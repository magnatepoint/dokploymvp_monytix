package com.example.monytix.datasupport

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.data.BackendApi
import com.example.monytix.data.KpiResponse
import com.example.monytix.data.Supabase
import io.github.jan.supabase.auth.auth
import com.example.monytix.data.TransactionCreateRequest
import com.example.monytix.data.UploadBatchResponse
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class DataSetupStep {
    data object UploadOrManual : DataSetupStep()
    data object UploadStatement : DataSetupStep()
    data object ParsingProgress : DataSetupStep()
    data object DataImportedSuccess : DataSetupStep()
    data object Analyzing : DataSetupStep()
    data object FirstInsightReveal : DataSetupStep()
    data object ManualAdd : DataSetupStep()
}

data class DataSetupUiState(
    val step: DataSetupStep = DataSetupStep.UploadOrManual,
    val isLoading: Boolean = false,
    val error: String? = null,
    val selectedFile: String? = null,
    val batchId: String? = null,
    val parsedCount: Int = 0,
    val manualAddType: ManualAddType? = null,
    val kpis: KpiResponse? = null,
    val analyzingProgress: Float = 0f,
    val pendingRetryFile: Pair<ByteArray, String>? = null
)

enum class ManualAddType { INCOME, EXPENSE, LOAN, INVESTMENT }

class DataSetupViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(DataSetupUiState())
    val uiState: StateFlow<DataSetupUiState> = _uiState.asStateFlow()

    private fun getAccessToken(): String? =
        Supabase.client.auth.currentSessionOrNull()?.accessToken

    fun showUploadStatement() {
        _uiState.update {
            it.copy(step = DataSetupStep.UploadStatement, error = null, selectedFile = null, pendingRetryFile = null)
        }
    }

    fun clearPendingRetry() {
        _uiState.update { it.copy(pendingRetryFile = null) }
    }

    fun showManualAdd(type: ManualAddType? = null) {
        _uiState.update {
            it.copy(step = DataSetupStep.ManualAdd, manualAddType = type, error = null)
        }
    }

    fun uploadFile(fileBytes: ByteArray, filename: String, pdfPassword: String? = null) {
        Log.d("MonytixUpload", "DataSetupViewModel.uploadFile: filename=$filename bytes=${fileBytes.size}")
        val token = getAccessToken()
        if (token == null) {
            _uiState.update { it.copy(error = "Not signed in") }
            return
        }
        val ext = filename.substringAfterLast('.', "").lowercase()
        if (ext !in listOf("pdf", "csv", "xls", "xlsx")) {
            _uiState.update { it.copy(error = "Unsupported format. Use PDF, CSV, or Excel.") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, pendingRetryFile = Pair(fileBytes, filename)) }
            val result = BackendApi.uploadStatement(token, fileBytes, filename, pdfPassword)
            result.fold(
                onSuccess = { batch ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            step = DataSetupStep.ParsingProgress,
                            batchId = batch.upload_id,
                            selectedFile = filename,
                            error = null
                        )
                    }
                    pollBatchStatus(batch.upload_id)
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

    private fun pollBatchStatus(batchId: String) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            while (true) {
                delay(2000)
                val result = BackendApi.getBatchStatus(token, batchId)
                result.fold(
                    onSuccess = { batch ->
                        when (batch.status) {
                            "loaded" -> {
                                _uiState.update {
                                    it.copy(
                                        step = DataSetupStep.DataImportedSuccess,
                                        parsedCount = 0,
                                        pendingRetryFile = null
                                    )
                                }
                                return@launch
                            }
                            "failed" -> {
                                _uiState.update {
                                    it.copy(
                                        step = DataSetupStep.UploadStatement,
                                        error = batch.error_message ?: "Parsing failed"
                                    )
                                }
                                return@launch
                            }
                            else -> {
                                _uiState.update { it.copy(parsedCount = it.parsedCount + 1) }
                            }
                        }
                    },
                    onFailure = { exc ->
                        _uiState.update { it.copy(error = exc.message ?: "Status check failed") }
                        return@launch
                    }
                )
            }
        }
    }

    fun onDataImportedDone() {
        _uiState.update {
            it.copy(
                step = DataSetupStep.Analyzing,
                analyzingProgress = 0f,
                kpis = null
            )
        }
        startAnalyzing()
    }

    private fun startAnalyzing() {
        viewModelScope.launch {
            val token = getAccessToken() ?: run {
                _uiState.update { it.copy(step = DataSetupStep.UploadOrManual) }
                return@launch
            }
            var progress = 0f
            while (progress < 0.9f) {
                delay(150)
                progress += 0.02f
                _uiState.update { it.copy(analyzingProgress = progress.coerceAtMost(0.9f)) }
            }
            val result = BackendApi.getKpis(token)
            result.fold(
                onSuccess = { kpis ->
                    _uiState.update {
                        it.copy(
                            step = DataSetupStep.FirstInsightReveal,
                            kpis = kpis,
                            analyzingProgress = 1f
                        )
                    }
                },
                onFailure = { exc ->
                    _uiState.update { state ->
                        state.copy(
                            step = DataSetupStep.UploadOrManual,
                            error = exc.message ?: "Could not load insights"
                        )
                    }
                }
            )
        }
    }

    fun onFirstInsightDone() {
        _uiState.update {
            it.copy(step = DataSetupStep.UploadOrManual, kpis = null, error = null)
        }
    }

    fun createManualTransaction(
        txnDate: String,
        merchantName: String,
        description: String?,
        amount: Double,
        direction: String,
        categoryCode: String?
    ) {
        val token = getAccessToken()
        if (token == null) {
            _uiState.update { it.copy(error = "Not signed in") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = BackendApi.createTransaction(
                token,
                TransactionCreateRequest(
                    txn_date = txnDate,
                    merchant_name = merchantName,
                    description = description,
                    amount = amount,
                    direction = direction,
                    category_code = categoryCode
                )
            )
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(isLoading = false, error = null, step = DataSetupStep.UploadOrManual)
                    }
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

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun backToUploadOrManual() {
        _uiState.update {
            it.copy(step = DataSetupStep.UploadOrManual, error = null, manualAddType = null)
        }
    }
}
