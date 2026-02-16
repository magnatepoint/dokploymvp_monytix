package com.example.monytix.spendsense

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.data.AccountItemResponse
import com.example.monytix.data.BackendApi
import com.example.monytix.data.CategoryResponse
import com.example.monytix.data.CategorySpendKpi
import com.example.monytix.data.InsightsResponse
import com.example.monytix.data.KpiResponse
import com.example.monytix.data.SubcategoryResponse
import com.example.monytix.data.Supabase
import com.example.monytix.data.TransactionCreateRequest
import io.github.jan.supabase.auth.auth
import com.example.monytix.data.TransactionCreateResponse
import com.example.monytix.data.TransactionRecordResponse
import com.example.monytix.budgetpilot.BudgetUpdateCache
import com.example.monytix.goaltracker.GoalUpdateCache
import com.example.monytix.data.TransactionUpdateRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

data class SpendSenseUiState(
    val kpis: KpiResponse? = null,
    val availableMonths: List<String> = emptyList(),
    val selectedMonth: String? = null,
    val transactions: List<TransactionRecordResponse> = emptyList(),
    val transactionsTotal: Int = 0,
    val insights: InsightsResponse? = null,
    val transferBreakdownTopMerchants: List<Pair<String, Double>> = emptyList(),
    val transferBreakdownLoading: Boolean = false,
    val categories: List<CategoryResponse> = emptyList(),
    val subcategories: List<SubcategoryResponse> = emptyList(),
    val channels: List<String> = emptyList(),
    val accounts: List<AccountItemResponse> = emptyList(),
    val userEmail: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val transactionFilters: TransactionFilters = TransactionFilters(),
    val goalUpdatedToast: String? = null
)

data class TransactionFilters(
    val categoryCode: String? = null,
    val subcategoryCode: String? = null,
    val channel: String? = null,
    val direction: String? = null,
    val bankCode: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)

class SpendSenseViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(SpendSenseUiState())
    val uiState: StateFlow<SpendSenseUiState> = _uiState.asStateFlow()

    private fun getAccessToken(): String? =
        Supabase.client.auth.currentSessionOrNull()?.accessToken

    init {
        loadSession()
        loadCategories()
        loadChannels()
        loadAccounts()
    }

    fun loadAccounts() {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val result = withContext(Dispatchers.IO) { BackendApi.getAccounts(token) }
            result.getOrNull()?.let { resp ->
                _uiState.update { it.copy(accounts = resp.accounts) }
            }
        }
    }

    fun loadSession() {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val result = withContext(Dispatchers.IO) { BackendApi.getSession(token) }
            result.getOrNull()?.let { session ->
                _uiState.update { it.copy(userEmail = session.email) }
            }
        }
    }

    fun loadCategories() {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val result = withContext(Dispatchers.IO) { BackendApi.getCategories(token) }
            result.getOrNull()?.let { cats ->
                _uiState.update { it.copy(categories = cats) }
            }
        }
    }

    fun loadChannels() {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val result = withContext(Dispatchers.IO) { BackendApi.getChannels(token) }
            result.getOrNull()?.let { ch ->
                _uiState.update { it.copy(channels = ch) }
            }
        }
    }

    fun loadSubcategories(categoryCode: String?) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val result = withContext(Dispatchers.IO) {
                BackendApi.getSubcategories(token, categoryCode)
            }
            result.getOrNull()?.let { subs ->
                _uiState.update { it.copy(subcategories = subs) }
            }
        }
    }

    fun loadKpis(month: String? = null) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            val kpisResult = withContext(Dispatchers.IO) { BackendApi.getKpis(token, month) }
            val monthsResult = withContext(Dispatchers.IO) { BackendApi.getAvailableMonths(token) }
            kpisResult.getOrNull()?.let { kpis ->
                monthsResult.getOrNull()?.let { months ->
                    _uiState.update {
                        it.copy(
                            kpis = kpis,
                            availableMonths = months.data,
                            selectedMonth = month ?: it.selectedMonth,
                            isLoading = false,
                            error = null
                        )
                    }
                } ?: run {
                    _uiState.update {
                        it.copy(kpis = kpis, isLoading = false, error = null)
                    }
                }
            } ?: run {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = kpisResult.exceptionOrNull()?.message ?: "Failed to load KPIs"
                    )
                }
            }
        }
    }

    fun loadTransactions(page: Int = 1, append: Boolean = false) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val state = _uiState.value
            if (!append) _uiState.update { it.copy(isLoading = true, error = null) }
            val limit = 25
            val offset = (page - 1) * limit
            val result = withContext(Dispatchers.IO) {
                BackendApi.getTransactions(
                    token,
                    limit = limit,
                    offset = offset,
                    search = state.searchQuery.takeIf { it.isNotBlank() },
                    categoryCode = state.transactionFilters.categoryCode,
                    subcategoryCode = state.transactionFilters.subcategoryCode,
                    channel = state.transactionFilters.channel,
                    direction = state.transactionFilters.direction,
                    bankCode = state.transactionFilters.bankCode,
                    startDate = state.transactionFilters.startDate,
                    endDate = state.transactionFilters.endDate
                )
            }
            result.fold(
                onSuccess = { response ->
                    _uiState.update {
                        it.copy(
                            transactions = if (append) it.transactions + response.transactions else response.transactions,
                            transactionsTotal = response.total,
                            isLoading = false,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load transactions"
                        )
                    }
                }
            )
        }
    }

    fun loadInsights(startDate: String? = null, endDate: String? = null) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = withContext(Dispatchers.IO) {
                BackendApi.getInsights(token, startDate, endDate)
            }
            result.fold(
                onSuccess = { insights ->
                    _uiState.update {
                        it.copy(insights = insights, isLoading = false, error = null)
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to load insights"
                        )
                    }
                }
            )
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun setTransactionFilters(filters: TransactionFilters) {
        _uiState.update { it.copy(transactionFilters = filters) }
        loadTransactions(1, append = false)
    }

    fun applyFiltersAndLoad() {
        loadTransactions(1, append = false)
    }

    fun setSelectedMonth(month: String?) {
        _uiState.update { it.copy(selectedMonth = month) }
        loadKpis(month)
    }

    fun refreshKpis() {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val result = withContext(Dispatchers.IO) { BackendApi.refreshKpis(token) }
            result.getOrNull()?.let { loadKpis(_uiState.value.selectedMonth) }
        }
    }

    fun createTransaction(
        txnDate: String,
        merchantName: String,
        description: String?,
        amount: Double,
        direction: String,
        categoryCode: String?,
        subcategoryCode: String?,
        channel: String?
    ) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = withContext(Dispatchers.IO) {
                BackendApi.createTransaction(
                    token,
                    TransactionCreateRequest(
                        txn_date = txnDate,
                        merchant_name = merchantName,
                        description = description,
                        amount = amount,
                        direction = direction,
                        category_code = categoryCode,
                        subcategory_code = subcategoryCode,
                        channel = channel
                    )
                )
            }
            result.fold(
                onSuccess = { resp ->
                    GoalUpdateCache.setFromTransactionCreate(resp.updated_goals)
                    resp.budget_state?.let { BudgetUpdateCache.setFromTransactionCreate(it) }
                    val toast = resp.updated_goals.firstOrNull()?.let { g ->
                        "₹${g.delta.toInt()} added to ${g.goal_name}"
                    } ?: resp.budget_state?.autopilot_suggestion?.message?.takeIf {
                        resp.budget_state?.budget_state_updated == true && resp.budget_state?.alerts?.isNotEmpty() == true
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = null,
                            goalUpdatedToast = toast
                        )
                    }
                    loadTransactions(1, append = false)
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to add transaction"
                        )
                    }
                }
            )
        }
    }

    fun updateTransaction(
        txnId: String,
        categoryCode: String?,
        subcategoryCode: String?,
        merchantName: String?,
        channel: String?
    ) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val result = withContext(Dispatchers.IO) {
                BackendApi.updateTransaction(
                    token, txnId, categoryCode, subcategoryCode, merchantName, channel
                )
            }
            result.getOrNull()?.let { loadTransactions(1, append = false) }
        }
    }

    fun deleteTransaction(txnId: String) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val result = withContext(Dispatchers.IO) { BackendApi.deleteTransaction(token, txnId) }
            result.getOrNull()?.let { loadTransactions(1, append = false) }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearGoalUpdatedToast() {
        _uiState.update { it.copy(goalUpdatedToast = null) }
    }

    fun refresh() {
        loadKpis(_uiState.value.selectedMonth)
        loadTransactions(1, append = false)
        loadInsights()
    }

    fun financialHealthScore(): Int {
        val kpis = _uiState.value.kpis ?: return 50
        val income = kpis.income_amount ?: 0.0
        val needs = kpis.needs_amount ?: 0.0
        val wants = kpis.wants_amount ?: 0.0
        val expenses = needs + wants
        val savingsRate = if (income > 0) ((income - expenses) / income * 100) else 0.0
        val wantsRatio = if (expenses > 0) wants / expenses else 0.0
        val score = (savingsRate * 0.5 + (1 - wantsRatio) * 25 + kotlin.math.min(income / 50000, 1.0) * 25).toInt()
        return score.coerceIn(0, 100)
    }

    fun healthScoreVsLastMonth(): Double {
        val trends = _uiState.value.insights?.spending_trends ?: return 0.0
        if (trends.size < 2) return _uiState.value.kpis?.best_month?.delta_pct ?: 0.0
        val latest = trends.last()
        val previous = trends[trends.size - 2]
        val latestNet = latest.net
        val previousNet = previous.net
        return if (previousNet != 0.0) ((latestNet - previousNet) / kotlin.math.abs(previousNet) * 100) else 0.0
    }

    /** Projected monthly spending based on current month's daily rate. Returns null if insufficient data. */
    fun projectedMonthlySpending(): String? {
        val kpis = _uiState.value.kpis ?: return null
        val spentSoFar = kpis.total_debits_amount ?: (kpis.needs_amount ?: 0.0) + (kpis.wants_amount ?: 0.0)
        if (spentSoFar <= 0) return null
        val now = LocalDate.now()
        val daysElapsed = now.dayOfMonth.toDouble().coerceAtLeast(1.0)
        val daysInMonth = now.lengthOfMonth().toDouble()
        val dailyRate = spentSoFar / daysElapsed
        val projectedMonthly = dailyRate * 30
        return "At this rate, monthly spending will reach ₹${java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(projectedMonthly.toLong())}."
    }

    /** Projected savings rate for current month. Returns null if insufficient data. */
    fun projectedSavingsRate(): String? {
        val kpis = _uiState.value.kpis ?: return null
        val income = kpis.income_amount ?: return null
        if (income <= 0) return null
        val spentSoFar = kpis.total_debits_amount ?: (kpis.needs_amount ?: 0.0) + (kpis.wants_amount ?: 0.0)
        val daysElapsed = LocalDate.now().dayOfMonth.toDouble().coerceAtLeast(1.0)
        val projectedExpenses = (spentSoFar / daysElapsed) * 30
        val projectedSavings = income - projectedExpenses
        val projectedRate = (projectedSavings / income * 100).toInt()
        if (projectedRate < 0) return null
        return "You're on track to save $projectedRate% this month."
    }

    fun generateSpendSenseInsight(): String? =
        insightAlerts().firstOrNull()?.text ?: insightOpportunities().firstOrNull()?.text ?: "Your finances look on track. Keep monitoring to stay ahead."

    data class InsightItem(val text: String, val isExpandable: Boolean = false)

    fun insightAlerts(): List<InsightItem> {
        val kpis = _uiState.value.kpis ?: return emptyList()
        val insights = _uiState.value.insights ?: return emptyList()
        val list = mutableListOf<InsightItem>()
        val topCat = kpis.top_categories.maxByOrNull { it.spend_amount }
        topCat?.let { cat ->
            if (cat.spend_amount > 50000) {
                val pct = (cat.spend_amount / (kpis.income_amount ?: 1.0) * 100).toInt()
                list.add(InsightItem("You're spending $pct% of income on ${cat.category_name.lowercase()}. Consider setting a limit."))
            }
        }
        val transfers = insights.category_breakdown.find { it.category_name.contains("Transfer", ignoreCase = true) }
        transfers?.let { t ->
            val avg = insights.category_breakdown.map { it.amount }.average()
            if (t.amount > avg * 1.5) list.add(InsightItem("Transfers out unusually high (₹${t.amount.toInt()}).", isExpandable = true))
        }
        val food = insights.category_breakdown.find { it.category_name.contains("Food", ignoreCase = true) || it.category_name.contains("Dining", ignoreCase = true) }
        food?.let { f ->
            if (f.percentage > 25) list.add(InsightItem("Food spending is ${f.percentage.toInt()}% of total. Meal planning could save ₹${(f.amount * 0.15).toInt()}."))
        }
        return list
    }

    fun insightOpportunities(): List<InsightItem> {
        val kpis = _uiState.value.kpis ?: return emptyList()
        val income = kpis.income_amount ?: 0.0
        val expenses = (kpis.needs_amount ?: 0.0) + (kpis.wants_amount ?: 0.0)
        val savings = income - expenses
        val list = mutableListOf<InsightItem>()
        if (income > 0 && savings > 5000) {
            val safeSip = (savings * 0.3).toInt()
            if (safeSip >= 1000) list.add(InsightItem("You can increase SIP by ₹${safeSip / 1000 * 1000} safely."))
        }
        if (list.isEmpty() && income > 0 && expenses / income < 0.6) {
            list.add(InsightItem("Strong savings rate. Consider investing surplus."))
        }
        return list
    }

    fun insightPatterns(): List<InsightItem> {
        val insights = _uiState.value.insights ?: return emptyList()
        val list = mutableListOf<InsightItem>()
        val patterns = insights.spending_patterns
        if (patterns.isNotEmpty()) {
            val topDay = patterns.maxByOrNull { it.amount } ?: return list
            list.add(InsightItem("You spend most on ${topDay.day_of_week}s."))
        }
        val timeSeries = insights.time_series
        if (timeSeries.size >= 5) {
            val byWeek = timeSeries.chunked(7).map { chunk -> chunk.sumOf { it.value } }
            val peakWeek = byWeek.indexOf(byWeek.maxOrNull() ?: 0.0) + 1
            if (peakWeek in 1..4) list.add(InsightItem("You spend most between week $peakWeek (days ${(peakWeek - 1) * 7 + 1}–${peakWeek * 7})."))
        }
        return list
    }

    fun isRecurringTransaction(merchant: String?): Boolean =
        _uiState.value.insights?.recurring_transactions?.any { it.merchant_name.equals(merchant, ignoreCase = true) } == true

    fun isLargeAmount(amount: Double): Boolean = kotlin.math.abs(amount) >= 10000

    fun isUnusualTransaction(amount: Double, categoryCode: String?): Boolean {
        val insights = _uiState.value.insights ?: return false
        val cat = insights.category_breakdown.find { it.category_code == categoryCode } ?: return false
        val avg = cat.avg_transaction
        if (avg <= 0) return false
        return kotlin.math.abs(amount) > avg * 2
    }

    fun isTransferRelatedInsight(insight: String?): Boolean =
        insight != null && insight.contains("Transfer", ignoreCase = true)

    fun getTransferCategoryCode(): String? =
        _uiState.value.insights?.category_breakdown
            ?.find { it.category_name.contains("Transfer", ignoreCase = true) }
            ?.category_code

    fun loadTransferBreakdown() {
        val categoryCode = getTransferCategoryCode() ?: return
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            _uiState.update { it.copy(transferBreakdownLoading = true) }
            val result = withContext(Dispatchers.IO) {
                BackendApi.getTransactions(
                    token,
                    limit = 100,
                    offset = 0,
                    categoryCode = categoryCode,
                    direction = "debit"
                )
            }
            result.fold(
                onSuccess = { response ->
                    val byMerchant = response.transactions
                        .groupBy { it.merchant ?: "Unknown" }
                        .mapValues { (_, txns) -> txns.sumOf { kotlin.math.abs(it.amount) } }
                        .toList()
                        .sortedByDescending { it.second }
                        .take(3)
                    _uiState.update {
                        it.copy(
                            transferBreakdownTopMerchants = byMerchant,
                            transferBreakdownLoading = false
                        )
                    }
                },
                onFailure = {
                    _uiState.update {
                        it.copy(transferBreakdownLoading = false)
                    }
                }
            )
        }
    }

    fun clearTransferBreakdown() {
        _uiState.update { it.copy(transferBreakdownTopMerchants = emptyList()) }
    }

    fun riskLevel(score: Int): String = when {
        score >= 80 -> "Low"
        score >= 60 -> "Moderate"
        score >= 40 -> "Elevated"
        else -> "High"
    }

    fun optimizationPotential(score: Int): String = when {
        score >= 80 -> "Low"
        score >= 60 -> "Medium"
        score >= 40 -> "High"
        else -> "High"
    }

    fun scoreBreakdown(): List<Pair<String, String>> {
        val kpis = _uiState.value.kpis ?: return emptyList()
        val income = kpis.income_amount ?: 0.0
        val needs = kpis.needs_amount ?: 0.0
        val wants = kpis.wants_amount ?: 0.0
        val expenses = needs + wants
        val savingsRate = if (income > 0) ((income - expenses) / income * 100) else 0.0
        val wantsRatio = if (expenses > 0) wants / expenses else 0.0
        val list = mutableListOf<Pair<String, String>>()
        list.add("Savings rate" to "${savingsRate.toInt()}% (50% weight)")
        list.add("Wants ratio" to "${(wantsRatio * 100).toInt()}% (25% weight)")
        list.add("Income scale" to "25% weight")
        return list
    }

    fun projectionConfidence(): Int {
        val kpis = _uiState.value.kpis ?: return 0
        val daysElapsed = LocalDate.now().dayOfMonth.toDouble().coerceAtLeast(1.0)
        return when {
            daysElapsed >= 25 -> 92
            daysElapsed >= 14 -> 82
            daysElapsed >= 7 -> 72
            else -> 62
        }
    }

    fun projectionDaysBased(): Int = LocalDate.now().dayOfMonth.coerceAtLeast(1)

    fun categorizationConfidence(): Int {
        val txns = _uiState.value.transactions
        if (txns.isEmpty()) return 94
        val withConf = txns.filter { it.confidence != null }
        return if (withConf.isEmpty()) 94 else (withConf.map { (it.confidence ?: 0.0) * 100 }.average()).toInt().coerceIn(50, 99)
    }

    fun financialHealthConfidence(): Int {
        val kpis = _uiState.value.kpis ?: return 75
        val hasIncome = (kpis.income_amount ?: 0.0) > 0
        val hasExpenses = (kpis.needs_amount ?: 0.0) + (kpis.wants_amount ?: 0.0) > 0
        return when {
            hasIncome && hasExpenses -> 87
            hasIncome || hasExpenses -> 72
            else -> 62
        }
    }

    data class DailySummary(val credits: Int, val debits: Int, val net: Double)

    fun dailySummary(): DailySummary? {
        val today = LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        val txns = _uiState.value.transactions.filter { it.txn_date.startsWith(today) }
        if (txns.isEmpty()) return null
        val credits = txns.count { it.direction.equals("credit", ignoreCase = true) }
        val debits = txns.count { it.direction.equals("debit", ignoreCase = true) }
        val net = txns.sumOf { t ->
            when (t.direction.lowercase()) {
                "credit" -> t.amount
                else -> -t.amount
            }
        }
        return DailySummary(credits, debits, net)
    }

    fun projectionForecastData(): List<Float> {
        val kpis = _uiState.value.kpis ?: return emptyList()
        val spent = kpis.total_debits_amount ?: (kpis.needs_amount ?: 0.0) + (kpis.wants_amount ?: 0.0)
        if (spent <= 0) return emptyList()
        val daysElapsed = LocalDate.now().dayOfMonth.toDouble().coerceAtLeast(1.0)
        val dailyRate = spent / daysElapsed
        val daysInMonth = LocalDate.now().lengthOfMonth()
        return (1..daysInMonth).map { day ->
            if (day <= daysElapsed) (spent * day / daysElapsed).toFloat()
            else (spent + dailyRate * (day - daysElapsed)).toFloat()
        }
    }

    fun categoryTrendLabel(item: com.example.monytix.data.CategoryBreakdownItem): String? {
        val insights = _uiState.value.insights ?: return null
        val kpis = _uiState.value.kpis ?: return null
        val topCat = kpis.top_categories.find { it.category_code == item.category_code } ?: return null
        val delta = topCat.delta_pct ?: return null
        val breakdown = insights.category_breakdown
        val cat = breakdown.find { it.category_code == item.category_code } ?: return null
        val avg = breakdown.map { it.amount }.average()
        return when {
            delta > 15 -> "Spiking (⚠)"
            delta < -5 -> "Declining"
            kotlin.math.abs(delta) <= 5 -> "Stable trend"
            else -> "${if (delta >= 0) "+" else ""}${delta.toInt()}% vs 3-mo avg"
        }
    }

    fun isFirstTimeMerchant(merchant: String?): Boolean {
        if (merchant == null) return false
        return _uiState.value.transactions.count { it.merchant.equals(merchant, ignoreCase = true) } <= 1
    }

    fun isSubscriptionCategory(categoryCode: String?): Boolean {
        val code = (categoryCode ?: "").lowercase()
        return code.contains("subscription") || code.contains("ott") || (code.contains("entertainment") && _uiState.value.insights?.recurring_transactions?.any { it.merchant_name.isNotBlank() } == true)
    }

    fun isUnusualTiming(transaction: TransactionRecordResponse): Boolean {
        val dayOfMonth = transaction.txn_date.takeLast(2).toIntOrNull() ?: return false
        return dayOfMonth in 1..5 && transaction.direction.lowercase() == "debit"
    }

    fun transactionTags(transaction: TransactionRecordResponse): List<String> {
        val tags = mutableListOf<String>()
        if (isRecurringTransaction(transaction.merchant)) tags.add("Recurring")
        if (isLargeAmount(transaction.amount)) tags.add("Large")
        if (isUnusualTransaction(transaction.amount, transaction.category)) tags.add("Unusual")
        if (isFirstTimeMerchant(transaction.merchant)) tags.add("First-time")
        if (isUnusualTiming(transaction)) tags.add("Unusual timing")
        if (isSubscriptionCategory(transaction.category) && isRecurringTransaction(transaction.merchant)) tags.add("Subscription")
        return tags
    }

    fun uploadStatement(fileBytes: ByteArray, filename: String, pdfPassword: String? = null) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = withContext(Dispatchers.IO) {
                BackendApi.uploadStatement(token, fileBytes, filename, pdfPassword)
            }
            result.fold(
                onSuccess = { batch ->
                    _uiState.update { it.copy(isLoading = false, error = null) }
                    pollBatchAndRefresh(batch.upload_id)
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Upload failed"
                        )
                    }
                }
            )
        }
    }

    private suspend fun pollBatchAndRefresh(batchId: String) {
        val token = getAccessToken() ?: return
        var attempts = 0
        while (attempts < 60) {
            kotlinx.coroutines.delay(2000)
            val result = withContext(Dispatchers.IO) { BackendApi.getBatchStatus(token, batchId) }
            result.getOrNull()?.let { batch ->
                when (batch.status) {
                    "loaded" -> {
                        loadKpis(_uiState.value.selectedMonth)
                        loadTransactions(1, append = false)
                        loadInsights()
                        return
                    }
                    "failed" -> {
                        _uiState.update {
                            it.copy(error = batch.error_message ?: "Parsing failed")
                        }
                        return
                    }
                }
            }
            attempts++
        }
    }
}
