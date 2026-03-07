package com.example.monytix.data

import android.util.Log
import com.example.monytix.BuildConfig
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.android.Android
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.forms.formData
import io.ktor.client.request.forms.submitFormWithBinaryData
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import io.ktor.http.content.TextContent
import io.ktor.http.contentType
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object BackendApi {

    private val client = HttpClient(Android) {
        install(HttpTimeout) {
            requestTimeoutMillis = 120_000  // 2 min for signal compute / nudge evaluate over date range
        }
        install(ContentNegotiation) {
            json(Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
            })
        }
    }

    private val baseUrl = BuildConfig.BACKEND_URL.trimEnd('/')
    private val backupBaseUrl = "https://api.monytix.ai".trimEnd('/')
    private val spendsenseBase = "$baseUrl/v1/spendsense"
    private val moneymomentsBase = "$baseUrl/v1/moneymoments"
    private val budgetBase = "$baseUrl/v1/budget"

    private fun isRetryable(e: Exception): Boolean {
        if (e is java.io.IOException) return true
        val msg = e.message ?: ""
        if (msg.contains("502") || msg.contains("503") || msg.contains("500") || msg.contains("timed out") || msg.contains("timeout")) return true
        return false
    }

    private fun logNetworkError(tag: String, url: String, e: Exception) {
        val msg = e.message ?: e.toString()
        Log.e(tag, "Failed to reach $url: $msg", e)
        when {
            msg.contains("Unable to resolve host", ignoreCase = true) -> Log.w(tag, "→ Likely DNS/ISP block. Try Private DNS (dns.google)")
            msg.contains("Connection timed out", ignoreCase = true) || msg.contains("timed out", ignoreCase = true) -> Log.w(tag, "→ Likely firewall/carrier block")
            msg.contains("Connection refused", ignoreCase = true) -> Log.w(tag, "→ Server unreachable or port blocked")
        }
    }

    private suspend fun <T> runWithFallback(primaryUrl: String, backupUrl: String, block: suspend (String) -> T): Result<T> {
        return try {
            Result.success(block(primaryUrl))
        } catch (e: Exception) {
            logNetworkError("BackendApi", primaryUrl, e)
            if (isRetryable(e) && primaryUrl != backupUrl) {
                Log.w("BackendApi", "Trying backup: $backupBaseUrl", e)
                try {
                    Result.success(block(backupUrl))
                } catch (e2: Exception) {
                    logNetworkError("BackendApi", backupUrl, e2)
                    Result.failure(e2)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    suspend fun healthCheck(): Result<HealthResponse> = withContext(Dispatchers.IO) {
        runWithFallback("$baseUrl/health", "$backupBaseUrl/health") { url ->
            client.get(url).body<HealthResponse>()
        }
    }

    suspend fun getSession(accessToken: String): Result<SessionResponse> = withContext(Dispatchers.IO) {
        runWithFallback("$baseUrl/auth/session", "$backupBaseUrl/auth/session") { url ->
            val response = client.get(url) { header("Authorization", "Bearer $accessToken") }
            if (response.status.value !in 200..299) {
                val msg = when (response.status.value) {
                    401 -> {
                        val body = runCatching { response.bodyAsText() }.getOrNull().orEmpty()
                        if (body.contains("\"detail\"")) {
                            runCatching {
                                val elem = Json.parseToJsonElement(body)
                                val obj = elem as? kotlinx.serialization.json.JsonObject ?: return@runCatching null
                                val d = obj["detail"]
                                when (d) {
                                    is kotlinx.serialization.json.JsonPrimitive -> d.content
                                    else -> null
                                }
                            }.getOrNull()?.takeIf { it.isNotBlank() } ?: "Unauthorized. Please sign out and sign in again."
                        } else "Unauthorized. Please sign out and sign in again."
                    }
                    else -> "Session failed: ${response.status}"
                }
                throw Exception(msg)
            }
            response.body<SessionResponse>()
        }
    }

    /** Legacy: proxy email/password login via backend (Supabase). Prefer Firebase Auth; use FirebaseAuthManager.getIdToken() for all other API calls. */
    suspend fun login(email: String, password: String): Result<LoginResponse> = withContext(Dispatchers.IO) {
        runWithFallback("$baseUrl/auth/login", "$backupBaseUrl/auth/login") { url ->
            client.post(url) {
                contentType(ContentType.Application.Json)
                setBody(LoginRequest(email = email, password = password))
            }.body<LoginResponse>()
        }
    }

    suspend fun getConfig(): Result<ConfigResponse> = withContext(Dispatchers.IO) {
        val r = runWithFallback("$baseUrl/config", "$backupBaseUrl/config") { url ->
            client.get(url).body<ConfigResponse>()
        }
        if (r.isSuccess) r else Result.success(
            ConfigResponse(
                min_version_code = 1,
                app_store_url = "https://play.google.com/store/apps/details?id=com.example.monytix",
                feature_flags = emptyMap()
            )
        )
    }

    suspend fun uploadStatement(
        accessToken: String,
        fileBytes: ByteArray,
        filename: String,
        pdfPassword: String? = null
    ): Result<UploadBatchResponse> = withContext(Dispatchers.IO) {
        Log.d("MonytixUpload", "BackendApi.uploadStatement: filename=$filename bytes=${fileBytes.size}")
        runWithFallback("$spendsenseBase/uploads/file", "$backupBaseUrl/v1/spendsense/uploads/file") { url ->
            val contentType = when {
                filename.endsWith(".pdf", ignoreCase = true) -> ContentType.parse("application/pdf")
                filename.endsWith(".csv", ignoreCase = true) -> ContentType.Text.CSV
                filename.endsWith(".xlsx", ignoreCase = true) -> ContentType.parse("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
                filename.endsWith(".xls", ignoreCase = true) -> ContentType.parse("application/vnd.ms-excel")
                else -> ContentType.Application.OctetStream
            }
            val parts = formData {
                append("file", fileBytes, Headers.build {
                    append(HttpHeaders.ContentType, contentType.toString())
                    append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$filename\"")
                })
                pdfPassword?.let { append("password", it) }
            }
            val response = client.submitFormWithBinaryData(url = url, formData = parts) {
                header("Authorization", "Bearer $accessToken")
            }
            if (response.status.value !in 200..299) {
                val msg = when (response.status.value) {
                    401 -> "Unauthorized. Please sign out and sign in again."
                    else -> "Upload failed: ${response.status}"
                }
                throw Exception(msg)
            }
            try {
                response.body<UploadBatchResponse>()
            } catch (e: Exception) {
                // Backend returned 2xx but body is not UploadBatchResponse (e.g. proxy returns 200 with error body)
                if (e.message?.contains("upload_id") == true || e.cause?.message?.contains("upload_id") == true) {
                    throw Exception("Upload failed. Please sign out and sign in again, then retry.")
                }
                throw e
            }
        }.also { r -> if (r.isSuccess) Log.d("MonytixUpload", "BackendApi.uploadStatement: success batch_id=${r.getOrNull()?.upload_id}") else Log.e("MonytixUpload", "BackendApi.uploadStatement: exception", r.exceptionOrNull()) }
    }

    suspend fun getBatchStatus(
        accessToken: String,
        batchId: String
    ): Result<UploadBatchResponse> = withContext(Dispatchers.IO) {
        runWithFallback("$spendsenseBase/batches/$batchId", "$backupBaseUrl/v1/spendsense/batches/$batchId") { url ->
            client.get(url) { header("Authorization", "Bearer $accessToken") }.body<UploadBatchResponse>()
        }
    }

    suspend fun createTransaction(
        accessToken: String,
        data: TransactionCreateRequest
    ): Result<TransactionCreateResponse> = withContext(Dispatchers.IO) {
        runWithFallback("$spendsenseBase/transactions", "$backupBaseUrl/v1/spendsense/transactions") { url ->
            client.post(url) {
                header("Authorization", "Bearer $accessToken")
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(data)
            }.body<TransactionCreateResponse>()
        }
    }

    suspend fun getKpis(
        accessToken: String,
        month: String? = null
    ): Result<KpiResponse> = withContext(Dispatchers.IO) {
        val path = if (month != null) "/v1/spendsense/kpis?month=$month" else "/v1/spendsense/kpis"
        runWithFallback("$baseUrl$path", "$backupBaseUrl$path") { url ->
            client.get(url) { header("Authorization", "Bearer $accessToken") }.body<KpiResponse>()
        }
    }

    suspend fun getAccounts(accessToken: String): Result<AccountsResponse> = withContext(Dispatchers.IO) {
        runWithFallback("$spendsenseBase/accounts", "$backupBaseUrl/v1/spendsense/accounts") { url ->
            client.get(url) { header("Authorization", "Bearer $accessToken") }.body<AccountsResponse>()
        }
    }

    suspend fun getInsights(
        accessToken: String,
        startDate: String? = null,
        endDate: String? = null
    ): Result<InsightsResponse> = withContext(Dispatchers.IO) {
        val params = buildList {
            startDate?.let { add("start_date=$it") }
            endDate?.let { add("end_date=$it") }
        }
        val path = if (params.isEmpty()) "/v1/spendsense/insights" else "/v1/spendsense/insights?${params.joinToString("&")}"
        runWithFallback("$baseUrl$path", "$backupBaseUrl$path") { url ->
            client.get(url) { header("Authorization", "Bearer $accessToken") }.body<InsightsResponse>()
        }
    }

    suspend fun getGoalsProgress(accessToken: String): Result<GoalsProgressResponse> = withContext(Dispatchers.IO) {
        runWithFallback("$baseUrl/v1/goals/progress", "$backupBaseUrl/v1/goals/progress") { url ->
            client.get(url) { header("Authorization", "Bearer $accessToken") }.body<GoalsProgressResponse>()
        }
    }

    suspend fun getForecast(accessToken: String): Result<ForecastResponse> = withContext(Dispatchers.IO) {
        runWithFallback("$baseUrl/v1/forecast", "$backupBaseUrl/v1/forecast") { url ->
            client.get(url) { header("Authorization", "Bearer $accessToken") }.body<ForecastResponse>()
        }
    }

    suspend fun getTopInsights(accessToken: String, limit: Int = 5): Result<TopInsightsResponse> = withContext(Dispatchers.IO) {
        runWithFallback("$baseUrl/v1/spendsense/insights/top?limit=$limit", "$backupBaseUrl/v1/spendsense/insights/top?limit=$limit") { url ->
            client.get(url) { header("Authorization", "Bearer $accessToken") }.body<TopInsightsResponse>()
        }
    }

    suspend fun postAssistantAsk(accessToken: String, prompt: String): Result<AskResponse> = withContext(Dispatchers.IO) {
        runWithFallback("$baseUrl/v1/assistant/ask", "$backupBaseUrl/v1/assistant/ask") { url ->
            client.post(url) {
                header("Authorization", "Bearer $accessToken")
                contentType(ContentType.Application.Json)
                setBody(AskRequest(prompt = prompt))
            }.body<AskResponse>()
        }
    }

    suspend fun getUserGoals(accessToken: String): Result<List<GoalResponse>> = withContext(Dispatchers.IO) {
        runWithFallback("$baseUrl/v1/goals", "$backupBaseUrl/v1/goals") { url ->
            client.get(url) { header("Authorization", "Bearer $accessToken") }.body<List<GoalResponse>>()
        }
    }

    suspend fun createGoal(
        accessToken: String,
        goalCategory: String,
        goalName: String,
        estimatedCost: Double,
        targetDate: String? = null,
        currentSavings: Double = 0.0,
        goalType: String? = null,
        importance: Int? = null
    ): Result<CreateGoalResponse> = withContext(Dispatchers.IO) {
        try {
            val body = buildJsonObject {
                put("goal_category", goalCategory)
                put("goal_name", goalName)
                put("estimated_cost", estimatedCost)
                put("current_savings", currentSavings)
                targetDate?.let { put("target_date", it) }
                goalType?.let { put("goal_type", it) }
                importance?.let { put("importance", it) }
            }
            runWithFallback("$baseUrl/v1/goals", "$backupBaseUrl/v1/goals") { url ->
                client.post(url) {
                    header("Authorization", "Bearer $accessToken")
                    contentType(io.ktor.http.ContentType.Application.Json)
                    setBody(TextContent(body.toString(), io.ktor.http.ContentType.Application.Json))
                }.body<CreateGoalResponse>()
            }
        } catch (e: Exception) {
            val msg = extractErrorMessage(e)
            Log.e("BackendApi", "createGoal failed: $msg", e)
            Result.failure(Exception(msg))
        }
    }

    private suspend fun extractErrorMessage(e: Exception): String {
        return try {
            val resp = (e as? io.ktor.client.plugins.ResponseException)?.response ?: return (e.message ?: "Unknown error")
            val body = resp.bodyAsText()
            if (body.contains("\"detail\"")) {
                val elem = kotlinx.serialization.json.Json.parseToJsonElement(body)
                val json = elem as? kotlinx.serialization.json.JsonObject ?: return body
                val detail = json["detail"]
                when (detail) {
                    is kotlinx.serialization.json.JsonPrimitive -> detail.content
                    is kotlinx.serialization.json.JsonArray -> (detail.firstOrNull() as? kotlinx.serialization.json.JsonObject)?.get("msg")?.toString()?.trim('"') ?: body
                    else -> body
                }.ifBlank { e.message ?: "Request failed" }
            } else body.ifBlank { e.message ?: "Request failed" }
        } catch (_: Exception) {
            e.message ?: "Failed to save goal"
        }
    }

    suspend fun updateGoal(
        accessToken: String,
        goalId: String,
        estimatedCost: Double? = null,
        targetDate: String? = null,
        currentSavings: Double? = null,
        goalType: String? = null,
        importance: Int? = null
    ): Result<GoalResponse> = withContext(Dispatchers.IO) {
        val body = buildJsonObject {
            estimatedCost?.let { put("estimated_cost", it) }
            targetDate?.let { put("target_date", it) }
            currentSavings?.let { put("current_savings", it) }
            goalType?.let { put("goal_type", it) }
            importance?.let { put("importance", it) }
        }
        runWithFallback("$baseUrl/v1/goals/$goalId", "$backupBaseUrl/v1/goals/$goalId") { url ->
            client.put(url) {
                header("Authorization", "Bearer $accessToken")
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(TextContent(body.toString(), io.ktor.http.ContentType.Application.Json))
            }.body<GoalResponse>()
        }
    }

    suspend fun deleteGoal(accessToken: String, goalId: String): Result<Unit> = withContext(Dispatchers.IO) {
        runWithFallback("$baseUrl/v1/goals/$goalId", "$backupBaseUrl/v1/goals/$goalId") { url ->
            client.delete(url) { header("Authorization", "Bearer $accessToken") }
            Unit
        }
    }

    suspend fun getTransactions(
        accessToken: String,
        limit: Int = 25,
        offset: Int = 0,
        search: String? = null,
        categoryCode: String? = null,
        subcategoryCode: String? = null,
        channel: String? = null,
        direction: String? = null,
        bankCode: String? = null,
        startDate: String? = null,
        endDate: String? = null
    ): Result<TransactionListResponse> = withContext(Dispatchers.IO) {
        try {
            val params = buildList {
                add("limit=$limit")
                add("offset=$offset")
                search?.let { add("search=${java.net.URLEncoder.encode(it, "UTF-8")}") }
                categoryCode?.let { add("category_code=$it") }
                subcategoryCode?.let { add("subcategory_code=$it") }
                channel?.let { add("channel=$it") }
                direction?.let { add("direction=$it") }
                bankCode?.let { add("bank_code=$it") }
                startDate?.let { add("start_date=$it") }
                endDate?.let { add("end_date=$it") }
            }
            val url = "$spendsenseBase/transactions?${params.joinToString("&")}"
            val response = client.get(url) {
                header("Authorization", "Bearer $accessToken")
            }.body<TransactionListResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getTransactionsSummary(
        accessToken: String,
        startDate: String? = null,
        endDate: String? = null,
        direction: String? = null,
        categoryCode: String? = null,
        subcategoryCode: String? = null,
        channel: String? = null,
        bankCode: String? = null,
        search: String? = null
    ): Result<TransactionSummaryResponse> = withContext(Dispatchers.IO) {
        try {
            val params = buildList {
                startDate?.let { add("start_date=$it") }
                endDate?.let { add("end_date=$it") }
                direction?.let { add("direction=$it") }
                categoryCode?.let { add("category_code=$it") }
                subcategoryCode?.let { add("subcategory_code=$it") }
                channel?.let { add("channel=$it") }
                bankCode?.let { add("bank_code=$it") }
                search?.let { add("search=${java.net.URLEncoder.encode(it, "UTF-8")}") }
            }
            val url = "$spendsenseBase/transactions/summary?${params.joinToString("&")}"
            val response = client.get(url) {
                header("Authorization", "Bearer $accessToken")
            }.body<TransactionSummaryResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTransaction(
        accessToken: String,
        txnId: String,
        categoryCode: String? = null,
        subcategoryCode: String? = null,
        merchantName: String? = null,
        channel: String? = null
    ): Result<TransactionRecordResponse> = withContext(Dispatchers.IO) {
        try {
            val updateBody = TransactionUpdateRequest(
                category_code = categoryCode,
                subcategory_code = subcategoryCode,
                merchant_name = merchantName,
                channel = channel
            )
            val resp = client.put("$spendsenseBase/transactions/$txnId") {
                header("Authorization", "Bearer $accessToken")
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(updateBody)
            }.body<TransactionRecordResponse>()
            Result.success(resp)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTransaction(
        accessToken: String,
        txnId: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.delete("$spendsenseBase/transactions/$txnId") {
                header("Authorization", "Bearer $accessToken")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCategories(accessToken: String): Result<List<CategoryResponse>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$spendsenseBase/categories") {
                header("Authorization", "Bearer $accessToken")
            }.body<List<CategoryResponse>>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getSubcategories(
        accessToken: String,
        categoryCode: String? = null
    ): Result<List<SubcategoryResponse>> = withContext(Dispatchers.IO) {
        try {
            val url = if (categoryCode != null) "$spendsenseBase/subcategories?category_code=$categoryCode" else "$spendsenseBase/subcategories"
            val response = client.get(url) {
                header("Authorization", "Bearer $accessToken")
            }.body<List<SubcategoryResponse>>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getChannels(accessToken: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$spendsenseBase/channels") {
                header("Authorization", "Bearer $accessToken")
            }.body<List<String>>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAvailableMonths(accessToken: String): Result<AvailableMonthsResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.get("$spendsenseBase/kpis/available-months") {
                header("Authorization", "Bearer $accessToken")
            }.body<AvailableMonthsResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun refreshKpis(accessToken: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            client.post("$spendsenseBase/kpis/refresh") {
                header("Authorization", "Bearer $accessToken")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAllData(accessToken: String): Result<DeleteDataResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.delete("$spendsenseBase/data") {
                header("Authorization", "Bearer $accessToken")
            }.body<DeleteDataResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // MoneyMoments API
    suspend fun getMoments(
        accessToken: String,
        month: String? = null,
        allMonths: Boolean = false
    ): Result<MoneyMomentsResponse> = withContext(Dispatchers.IO) {
        try {
            val params = buildList {
                month?.let { add("month=$it") }
                if (allMonths) add("all_months=true")
            }
            val url = if (params.isEmpty()) "$moneymomentsBase/moments" else "$moneymomentsBase/moments?${params.joinToString("&")}"
            val response = client.get(url) {
                header("Authorization", "Bearer $accessToken")
            }.body<MoneyMomentsResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun computeMoments(
        accessToken: String,
        targetMonth: String? = null
    ): Result<ComputeMomentsResponse> = withContext(Dispatchers.IO) {
        try {
            val url = if (targetMonth != null) "$moneymomentsBase/moments/compute?target_month=$targetMonth" else "$moneymomentsBase/moments/compute"
            val response = client.post(url) {
                header("Authorization", "Bearer $accessToken")
            }.body<ComputeMomentsResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNudges(
        accessToken: String,
        limit: Int = 20,
        fromDate: String? = null,
        toDate: String? = null
    ): Result<NudgesResponse> = withContext(Dispatchers.IO) {
        try {
            val query = buildString {
                append("limit=$limit")
                if (fromDate != null) append("&from_date=$fromDate")
                if (toDate != null) append("&to_date=$toDate")
            }
            val response = client.get("$moneymomentsBase/nudges?$query") {
                header("Authorization", "Bearer $accessToken")
            }.body<NudgesResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getNudgeDiagnose(
        accessToken: String,
        asOfDate: String? = null,
        fromDate: String? = null,
        toDate: String? = null
    ): Result<NudgeDiagnoseResponse> = withContext(Dispatchers.IO) {
        try {
            val query = buildString {
                if (asOfDate != null) append("as_of_date=$asOfDate")
                if (fromDate != null) {
                    if (length > 0) append("&")
                    append("from_date=$fromDate")
                }
                if (toDate != null) {
                    if (length > 0) append("&")
                    append("to_date=$toDate")
                }
            }
            val url = if (query.isNotEmpty()) "$moneymomentsBase/nudges/diagnose?$query" else "$moneymomentsBase/nudges/diagnose"
            val response = client.get(url) {
                header("Authorization", "Bearer $accessToken")
            }.body<NudgeDiagnoseResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun logNudgeInteraction(
        accessToken: String,
        deliveryId: String,
        eventType: String,
        metadata: Map<String, String>? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val body = NudgeInteractionRequest(event_type = eventType, metadata = metadata)
            client.post("$moneymomentsBase/nudges/$deliveryId/interact") {
                header("Authorization", "Bearer $accessToken")
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(body)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun evaluateNudges(
        accessToken: String,
        asOfDate: String? = null,
        fromDate: String? = null,
        toDate: String? = null
    ): Result<EvaluateNudgesResponse> = withContext(Dispatchers.IO) {
        try {
            val query = buildString {
                if (asOfDate != null) append("as_of_date=$asOfDate")
                if (fromDate != null) {
                    if (length > 0) append("&")
                    append("from_date=$fromDate")
                }
                if (toDate != null) {
                    if (length > 0) append("&")
                    append("to_date=$toDate")
                }
            }
            val url = if (query.isNotEmpty()) "$moneymomentsBase/nudges/evaluate?$query" else "$moneymomentsBase/nudges/evaluate"
            val response = client.post(url) {
                header("Authorization", "Bearer $accessToken")
            }.body<EvaluateNudgesResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun processNudges(
        accessToken: String,
        limit: Int = 10
    ): Result<ProcessNudgesResponse> = withContext(Dispatchers.IO) {
        try {
            val response = client.post("$moneymomentsBase/nudges/process?limit=$limit") {
                header("Authorization", "Bearer $accessToken")
            }.body<ProcessNudgesResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun computeSignal(
        accessToken: String,
        asOfDate: String? = null,
        fromDate: String? = null,
        toDate: String? = null
    ): Result<ComputeSignalResponse> = withContext(Dispatchers.IO) {
        try {
            val query = buildString {
                if (asOfDate != null) append("as_of_date=$asOfDate")
                if (fromDate != null) {
                    if (length > 0) append("&")
                    append("from_date=$fromDate")
                }
                if (toDate != null) {
                    if (length > 0) append("&")
                    append("to_date=$toDate")
                }
            }
            val url = if (query.isNotEmpty()) "$moneymomentsBase/signals/compute?$query" else "$moneymomentsBase/signals/compute"
            val response = client.post(url) {
                header("Authorization", "Bearer $accessToken")
            }.body<ComputeSignalResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // BudgetPilot API
    suspend fun getBudgetRecommendations(
        accessToken: String,
        month: String? = null
    ): Result<BudgetRecommendationsResponse> = withContext(Dispatchers.IO) {
        try {
            val url = if (month != null) "$budgetBase/recommendations?month=$month" else "$budgetBase/recommendations"
            val response = client.get(url) {
                header("Authorization", "Bearer $accessToken")
            }.body<BudgetRecommendationsResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCommittedBudget(
        accessToken: String,
        month: String? = null
    ): Result<CommittedBudgetResponse> = withContext(Dispatchers.IO) {
        try {
            val url = if (month != null) "$budgetBase/commit?month=$month" else "$budgetBase/commit"
            val response = client.get(url) {
                header("Authorization", "Bearer $accessToken")
            }.body<CommittedBudgetResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun commitBudget(
        accessToken: String,
        planCode: String,
        month: String? = null
    ): Result<CommittedBudget> = withContext(Dispatchers.IO) {
        try {
            Log.d("BackendApi", "commitBudget: POST $budgetBase/commit planCode=$planCode month=$month")
            val body = BudgetCommitRequest(plan_code = planCode, month = month)
            val response = client.post("$budgetBase/commit") {
                header("Authorization", "Bearer $accessToken")
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(body)
            }.body<BudgetCommitResponse>()
            Log.d("BackendApi", "commitBudget: success plan_code=${response.budget.plan_code}")
            Result.success(response.budget)
        } catch (e: Exception) {
            Log.e("BackendApi", "commitBudget: failed", e)
            Result.failure(e)
        }
    }

    suspend fun getBudgetState(
        accessToken: String,
        month: String? = null
    ): Result<BudgetStateResponse> = withContext(Dispatchers.IO) {
        try {
            val monthParam = if (month != null && month.isNotBlank()) "${month}-01" else null
            val url = if (monthParam != null) "$budgetBase/state?month=$monthParam" else "$budgetBase/state"
            val response = client.get(url) {
                header("Authorization", "Bearer $accessToken")
            }.body<BudgetStateResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun recalculateBudget(
        accessToken: String,
        month: String? = null
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val monthParam = if (month != null && month.isNotBlank()) "${month}-01" else null
            val url = if (monthParam != null) "$budgetBase/recalculate?month=$monthParam" else "$budgetBase/recalculate"
            client.post(url) {
                header("Authorization", "Bearer $accessToken")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getBudgetVariance(
        accessToken: String,
        month: String? = null
    ): Result<BudgetVarianceResponse> = withContext(Dispatchers.IO) {
        try {
            val url = if (month != null) "$budgetBase/variance?month=$month" else "$budgetBase/variance"
            val response = client.get(url) {
                header("Authorization", "Bearer $accessToken")
            }.body<BudgetVarianceResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun applyBudgetAdjustment(
        accessToken: String,
        shiftFrom: String,
        shiftTo: String,
        pct: Double,
        month: String? = null
    ): Result<BudgetApplyAdjustmentResponse> = withContext(Dispatchers.IO) {
        try {
            val monthVal = if (month != null && month.isNotBlank()) "${month}-01" else null
            val body = BudgetApplyAdjustmentRequest(
                shift_from = shiftFrom,
                shift_to = shiftTo,
                pct = pct,
                month = monthVal
            )
            val response = client.post("$budgetBase/apply-adjustment") {
                header("Authorization", "Bearer $accessToken")
                contentType(io.ktor.http.ContentType.Application.Json)
                setBody(body)
            }.body<BudgetApplyAdjustmentResponse>()
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

@kotlinx.serialization.Serializable
data class HealthResponse(val status: String = "ok")

@kotlinx.serialization.Serializable
data class ConfigResponse(
    val min_version_code: Int = 1,
    val app_store_url: String = "https://play.google.com/store/apps/details?id=com.example.monytix",
    val feature_flags: Map<String, Boolean> = emptyMap(),
    val maintenance_mode: Boolean = false
)

@kotlinx.serialization.Serializable
data class SessionResponse(
    val user_id: String,
    val email: String?,
    val role: String?
)

@kotlinx.serialization.Serializable
data class LoginRequest(
    val email: String,
    val password: String
)

@kotlinx.serialization.Serializable
data class LoginResponse(
    val access_token: String,
    val refresh_token: String,
    val token_type: String = "bearer",
    val expires_in: Int,
    val user: kotlinx.serialization.json.JsonObject? = null
)

@kotlinx.serialization.Serializable
data class UploadBatchResponse(
    val upload_id: String,
    val batch_id: String? = null,
    val user_id: String,
    val source_type: String,
    val status: String,
    val created_at: String,
    val error_message: String? = null
)

@kotlinx.serialization.Serializable
data class TransactionCreateRequest(
    val txn_date: String,
    val merchant_name: String,
    val description: String? = null,
    val amount: Double,
    val direction: String,
    val category_code: String? = null,
    val subcategory_code: String? = null,
    val channel: String? = null,
    val account_ref: String? = null
)

@kotlinx.serialization.Serializable
data class KpiResponse(
    val month: String? = null,
    val income_amount: Double = 0.0,
    val needs_amount: Double = 0.0,
    val wants_amount: Double = 0.0,
    val assets_amount: Double = 0.0,
    val total_debits_amount: Double = 0.0,
    val top_categories: List<CategorySpendKpi> = emptyList(),
    val wants_gauge: WantsGaugeResponse? = null,
    val best_month: BestMonthResponse? = null,
    val recent_loot_drop: LootDropResponse? = null
)

@kotlinx.serialization.Serializable
data class CategorySpendKpi(
    val category_code: String = "",
    val category_name: String = "",
    val txn_count: Int = 0,
    val spend_amount: Double = 0.0,
    val income_amount: Double = 0.0,
    val delta_pct: Double? = null
)

@kotlinx.serialization.Serializable
data class WantsGaugeResponse(
    val ratio: Double = 0.0,
    val label: String = "",
    val threshold_crossed: Boolean = false
)

@kotlinx.serialization.Serializable
data class BestMonthResponse(
    val month: String = "",
    val net_amount: Double = 0.0,
    val delta_pct: Double? = null,
    val is_current_best: Boolean = false
)

@kotlinx.serialization.Serializable
data class AccountsResponse(
    val accounts: List<AccountItemResponse> = emptyList()
)

@kotlinx.serialization.Serializable
data class AccountItemResponse(
    val id: String = "",
    val bank_code: String = "",
    val bank_name: String = "",
    val account_number: String? = null,
    val balance: Double = 0.0,
    val account_type: String = "SAVINGS",
    val transaction_count: Int = 0,
    val last_txn_date: String? = null
)

@kotlinx.serialization.Serializable
data class DailySpendItem(val date: String = "", val amount: Double = 0.0)

@kotlinx.serialization.Serializable
data class InsightsResponse(
    val time_series: List<TimeSeriesPoint> = emptyList(),
    val category_breakdown: List<CategoryBreakdownItem> = emptyList(),
    val spending_trends: List<SpendingTrendItem> = emptyList(),
    val recurring_transactions: List<RecurringItem> = emptyList(),
    val spending_patterns: List<SpendingPatternItem> = emptyList(),
    val top_merchants: List<TopMerchantItem> = emptyList(),
    val daily_spend: List<DailySpendItem> = emptyList()
)

@kotlinx.serialization.Serializable
data class TimeSeriesPoint(val date: String = "", val value: Double = 0.0, val label: String? = null)

@kotlinx.serialization.Serializable
data class CategoryBreakdownItem(
    val category_code: String = "",
    val category_name: String = "",
    val amount: Double = 0.0,
    val percentage: Double = 0.0,
    val transaction_count: Int = 0,
    val avg_transaction: Double = 0.0
)

@kotlinx.serialization.Serializable
data class SpendingTrendItem(
    val period: String = "",
    val income: Double = 0.0,
    val expenses: Double = 0.0,
    val net: Double = 0.0,
    val needs: Double = 0.0,
    val wants: Double = 0.0,
    val assets: Double = 0.0
)

@kotlinx.serialization.Serializable
data class RecurringItem(
    val merchant_name: String = "",
    val category_code: String = "",
    val category_name: String = "",
    val frequency: String = "",
    val avg_amount: Double = 0.0,
    val next_expected: String? = null
)

@kotlinx.serialization.Serializable
data class SpendingPatternItem(
    val day_of_week: String = "",
    val amount: Double = 0.0,
    val transaction_count: Int = 0
)

@kotlinx.serialization.Serializable
data class TopMerchantItem(
    val merchant_name: String = "",
    val total_spend: Double = 0.0,
    val transaction_count: Int = 0
)

@kotlinx.serialization.Serializable
data class LootDropResponse(
    val batch_id: String = "",
    val occurred_at: String = "",
    val transactions_unlocked: Int = 0,
    val rarity: String = "common"
)

@kotlinx.serialization.Serializable
data class GoalResponse(
    val goal_id: String = "",
    val goal_category: String = "",
    val goal_name: String = "",
    val goal_type: String = "",
    val linked_txn_type: String? = null,
    val estimated_cost: Double = 0.0,
    val target_date: String? = null,
    val current_savings: Double = 0.0,
    val importance: Int? = null,
    val priority_rank: Int? = null,
    val status: String = "active",
    val notes: String? = null,
    val created_at: String = "",
    val updated_at: String = ""
)

@kotlinx.serialization.Serializable
data class CreateGoalResponse(
    val status: String = "",
    val goal_id: String? = null
)

@kotlinx.serialization.Serializable
data class GoalsProgressResponse(
    val goals: List<GoalProgressItem> = emptyList()
)

@kotlinx.serialization.Serializable
data class GoalProgressItem(
    val goal_id: String = "",
    val goal_name: String = "",
    val progress_pct: Double = 0.0,
    val current_savings_close: Double = 0.0,
    val remaining_amount: Double = 0.0,
    val projected_completion_date: String? = null,
    val milestones: List<Int> = emptyList(),
    val monthly_required: Double? = null,
    val monthly_avg_contribution: Double? = null,
    val pace_description: String? = null,
    val days_to_target: Int? = null
)

@kotlinx.serialization.Serializable
data class ForecastRecommendationResponse(val title: String = "", val body: String = "")

@kotlinx.serialization.Serializable
data class ForecastResponse(
    val projection_points: List<List<Double>> = emptyList(),
    val confidence_label: String = "",
    val risk_strip_label: String? = null,
    val risk_strip_severity: String = "neutral",
    val savings_opportunity: String? = null,
    val recommendations: List<ForecastRecommendationResponse> = emptyList()
)

@kotlinx.serialization.Serializable
data class TopInsightItemResponse(
    val id: String = "",
    val title: String = "",
    val message: String = "",
    val type: String = "",
    val confidence: Double? = null
)

@kotlinx.serialization.Serializable
data class TopInsightsResponse(val insights: List<TopInsightItemResponse> = emptyList())

@kotlinx.serialization.Serializable
data class AskRequest(val prompt: String = "")

@kotlinx.serialization.Serializable
data class AskResponse(val answer: String = "")

@kotlinx.serialization.Serializable
data class UpdatedGoalItem(
    val goal_id: String = "",
    val goal_name: String = "",
    val delta: Double = 0.0,
    val prev_pct: Double = 0.0,
    val new_pct: Double = 0.0,
    val reason: String = ""
)

@kotlinx.serialization.Serializable
data class BudgetStateUpdate(
    val budget_state_updated: Boolean = false,
    val actual_split: Map<String, Double> = emptyMap(),
    val deviation: Map<String, Double> = emptyMap(),
    val autopilot_suggestion: AutopilotSuggestion? = null,
    val alerts: List<String> = emptyList()
)

@kotlinx.serialization.Serializable
data class AutopilotSuggestion(
    val shift_from: String = "",
    val shift_to: String = "",
    val pct: Double = 0.0,
    val message: String = ""
)

@kotlinx.serialization.Serializable
data class TransactionCreateResponse(
    val txn_id: String = "",
    val txn_date: String = "",
    val merchant: String? = null,
    val category: String? = null,
    val subcategory: String? = null,
    val bank_code: String? = null,
    val channel: String? = null,
    val amount: Double = 0.0,
    val direction: String = "",
    val confidence: Double? = null,
    val updated_goals: List<UpdatedGoalItem> = emptyList(),
    val budget_state: BudgetStateUpdate? = null
)

@kotlinx.serialization.Serializable
data class TransactionRecordResponse(
    val txn_id: String,
    val txn_date: String,
    val txn_time: String? = null,
    val recorded_at: String? = null,
    val merchant: String?,
    val category: String?,
    val subcategory: String?,
    val bank_code: String?,
    val channel: String?,
    val amount: Double,
    val direction: String,
    val confidence: Double? = null
)

@kotlinx.serialization.Serializable
data class TransactionListResponse(
    val transactions: List<TransactionRecordResponse> = emptyList(),
    val total: Int = 0,
    val page: Int = 1,
    val page_size: Int = 25
)

@kotlinx.serialization.Serializable
data class TransactionSummaryResponse(
    val debit_total: Double = 0.0,
    val credit_total: Double = 0.0,
    val debit_count: Int = 0,
    val credit_count: Int = 0
)

@kotlinx.serialization.Serializable
data class TransactionUpdateRequest(
    val category_code: String? = null,
    val subcategory_code: String? = null,
    val merchant_name: String? = null,
    val channel: String? = null
)

@kotlinx.serialization.Serializable
data class CategoryResponse(
    val category_code: String = "",
    val category_name: String = "",
    val is_custom: Boolean = false,
    val txn_type: String? = null
)

@kotlinx.serialization.Serializable
data class SubcategoryResponse(
    val subcategory_code: String = "",
    val subcategory_name: String = "",
    val category_code: String = "",
    val is_custom: Boolean = false
)

@kotlinx.serialization.Serializable
data class AvailableMonthsResponse(
    val data: List<String> = emptyList()
)

@kotlinx.serialization.Serializable
data class DeleteDataResponse(
    val transactions_deleted: Int = 0,
    val batches_deleted: Int = 0,
    val staging_deleted: Int = 0,
    val overrides_deleted: Int = 0
)

// MoneyMoments types
@kotlinx.serialization.Serializable
data class MoneyMoment(
    val user_id: String = "",
    val month: String = "",
    val habit_id: String = "",
    val value: Double = 0.0,
    val label: String = "",
    val insight_text: String = "",
    val confidence: Double = 0.0,
    val created_at: String = ""
)

@kotlinx.serialization.Serializable
data class Nudge(
    val delivery_id: String = "",
    val user_id: String = "",
    val rule_id: String = "",
    val template_code: String = "",
    val channel: String = "",
    val sent_at: String = "",
    val send_status: String = "",
    val metadata_json: Map<String, String>? = null,
    val title_template: String? = null,
    val body_template: String? = null,
    val title: String? = null,
    val body: String? = null,
    val cta_text: String? = null,
    val cta_deeplink: String? = null,
    val rule_name: String = ""
)

@kotlinx.serialization.Serializable
data class MoneyMomentsResponse(
    val moments: List<MoneyMoment> = emptyList()
)

@kotlinx.serialization.Serializable
data class ComputeMomentsResponse(
    val status: String = "",
    val moments: List<MoneyMoment> = emptyList(),
    val count: Int = 0,
    val message: String? = null
)

@kotlinx.serialization.Serializable
data class NudgesResponse(
    val nudges: List<Nudge> = emptyList()
)

@kotlinx.serialization.Serializable
data class NudgeDiagnoseResponse(
    val has_signal_today: Boolean = false,
    val pending_candidates: Int = 0,
    val delivered_count: Int = 0,
    val suggestion: String = "",
    val from_date: String? = null,
    val to_date: String? = null
)

@kotlinx.serialization.Serializable
data class NudgeInteractionRequest(
    val event_type: String,
    val metadata: Map<String, String>? = null
)

@kotlinx.serialization.Serializable
data class EvaluateNudgesResponse(
    val status: String = "",
    val count: Int = 0,
    val candidates: List<String>? = null,
    val reason: String? = null,
    val from_date: String? = null,
    val to_date: String? = null
)

@kotlinx.serialization.Serializable
data class ProcessNudgesResponse(
    val status: String = "",
    val delivered: List<Nudge> = emptyList(),
    val count: Int = 0
)

@kotlinx.serialization.Serializable
data class ComputeSignalResponse(
    val status: String = "",
    val signal: Map<String, String>? = null,
    val from_date: String? = null,
    val to_date: String? = null,
    val days_computed: Int? = null
)

// BudgetPilot types
@kotlinx.serialization.Serializable
data class GoalAllocationPreview(
    val goal_id: String = "",
    val goal_name: String = "",
    val allocation_pct: Double = 0.0,
    val allocation_amount: Double = 0.0
)

@kotlinx.serialization.Serializable
data class BudgetRecommendation(
    val plan_code: String = "",
    val name: String = "",
    val description: String? = null,
    val needs_budget_pct: Double = 0.0,
    val wants_budget_pct: Double = 0.0,
    val savings_budget_pct: Double = 0.0,
    val score: Double = 0.0,
    val recommendation_reason: String = "",
    val goal_preview: List<GoalAllocationPreview>? = null
)

@kotlinx.serialization.Serializable
data class BudgetRecommendationsResponse(
    val recommendations: List<BudgetRecommendation> = emptyList()
)

@kotlinx.serialization.Serializable
data class GoalAllocation(
    val ubcga_id: String = "",
    val user_id: String = "",
    val month: String = "",
    val goal_id: String = "",
    val goal_name: String? = null,
    val weight_pct: Double = 0.0,
    val planned_amount: Double = 0.0,
    val created_at: String = ""
)

@kotlinx.serialization.Serializable
data class CommittedBudget(
    val user_id: String = "",
    val month: String = "",
    val plan_code: String = "",
    val alloc_needs_pct: Double = 0.0,
    val alloc_wants_pct: Double = 0.0,
    val alloc_assets_pct: Double = 0.0,
    val notes: String? = null,
    val committed_at: String = "",
    val goal_allocations: List<GoalAllocation> = emptyList()
)

@kotlinx.serialization.Serializable
data class BudgetCommitRequest(
    val plan_code: String,
    val month: String? = null,
    val goal_allocations: Map<String, Double>? = null,
    val notes: String? = null
)

@kotlinx.serialization.Serializable
data class BudgetCommitResponse(
    val status: String = "",
    val budget: CommittedBudget = CommittedBudget()
)

@kotlinx.serialization.Serializable
data class CommittedBudgetResponse(
    val status: String = "",
    val budget: CommittedBudget? = null
)

@kotlinx.serialization.Serializable
data class BudgetVariance(
    val user_id: String = "",
    val month: String = "",
    val income_amt: Double = 0.0,
    val needs_amt: Double = 0.0,
    val planned_needs_amt: Double = 0.0,
    val variance_needs_amt: Double = 0.0,
    val wants_amt: Double = 0.0,
    val planned_wants_amt: Double = 0.0,
    val variance_wants_amt: Double = 0.0,
    val assets_amt: Double = 0.0,
    val planned_assets_amt: Double = 0.0,
    val variance_assets_amt: Double = 0.0,
    val computed_at: String = ""
)

@kotlinx.serialization.Serializable
data class BudgetGoalImpact(
    val goal_id: String = "",
    val goal_name: String = "",
    val status: String = "",
    val planned_amount: Double = 0.0,
    val shortfall: Double? = null
)

@kotlinx.serialization.Serializable
data class BudgetStateResponse(
    val month: String = "",
    val committed_plan: BudgetStateCommittedPlan? = null,
    val income_amt: Double = 0.0,
    val actual: BudgetStateActual? = null,
    val deviation: BudgetStateDeviation? = null,
    val plans: List<BudgetStatePlan> = emptyList(),
    val goal_impact: List<BudgetGoalImpact> = emptyList(),
    val last_updated_at: String? = null
)

@kotlinx.serialization.Serializable
data class BudgetStateCommittedPlan(
    val plan_id: String = "",
    val target: Map<String, Double> = emptyMap()
)

@kotlinx.serialization.Serializable
data class BudgetStateActual(
    val needs_pct: Double = 0.0,
    val wants_pct: Double = 0.0,
    val savings_pct: Double = 0.0,
    val needs_amt: Double = 0.0,
    val wants_amt: Double = 0.0,
    val savings_amt: Double = 0.0
)

@kotlinx.serialization.Serializable
data class BudgetStateDeviation(
    val needs: Double = 0.0,
    val wants: Double = 0.0,
    val savings: Double = 0.0
)

@kotlinx.serialization.Serializable
data class BudgetStatePlan(
    val plan_id: String = "",
    val name: String = "",
    val score: Double = 0.0,
    val reason: String = ""
)

@kotlinx.serialization.Serializable
data class BudgetVarianceResponse(
    val status: String = "",
    val aggregate: BudgetVariance? = null
)

@kotlinx.serialization.Serializable
data class BudgetApplyAdjustmentRequest(
    val shift_from: String,
    val shift_to: String = "savings",
    val pct: Double,
    val month: String? = null
)

@kotlinx.serialization.Serializable
data class BudgetApplyAdjustmentResponse(
    val status: String = "",
    val reason: String? = null,
    val budget: CommittedBudget? = null
)
