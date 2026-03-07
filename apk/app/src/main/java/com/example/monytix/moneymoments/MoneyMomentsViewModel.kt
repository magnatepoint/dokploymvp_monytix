package com.example.monytix.moneymoments

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.data.BackendApi
import com.example.monytix.data.MoneyMoment
import com.example.monytix.data.Nudge
import com.example.monytix.auth.FirebaseAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MoneyMomentsUiState(
    val moments: List<MoneyMoment> = emptyList(),
    val nudges: List<Nudge> = emptyList(),
    val userEmail: String? = null,
    val isMomentsLoading: Boolean = false,
    val isNudgesLoading: Boolean = false,
    val momentsError: String? = null,
    val nudgesError: String? = null,
    val isEvaluating: Boolean = false,
    val isComputing: Boolean = false,
    val actionError: String? = null,
    val actionMessage: String? = null
)

data class ProgressMetrics(
    val streak: Int,
    val nudgesCount: Int,
    val habitsCount: Int,
    val savedAmount: Double
)

class MoneyMomentsViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(MoneyMomentsUiState())
    val uiState: StateFlow<MoneyMomentsUiState> = _uiState.asStateFlow()

    private var hasAutoRunPipelineThisSession = false

    private suspend fun getAccessToken(): String? =
        FirebaseAuthManager.getIdToken()

    init {
        loadSession()
        loadData()
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

    fun loadData() {
        viewModelScope.launch {
            val token = getAccessToken()
            if (token == null) {
                _uiState.update {
                    it.copy(
                        isMomentsLoading = false,
                        isNudgesLoading = false,
                        actionError = "Sign in to get personalized nudges and moments."
                    )
                }
                return@launch
            }
            _uiState.update {
                it.copy(
                    isMomentsLoading = true,
                    isNudgesLoading = true,
                    momentsError = null,
                    nudgesError = null
                )
            }
            val momentsResult = withContext(Dispatchers.IO) {
                BackendApi.getMoments(token, month = null, allMonths = false)
            }
            val nudgesResult = withContext(Dispatchers.IO) {
                BackendApi.getNudges(token, limit = 20)
            }
            val loadedNudges = nudgesResult.getOrNull()?.nudges ?: emptyList()
            _uiState.update {
                it.copy(
                    moments = momentsResult.getOrNull()?.moments ?: emptyList(),
                    nudges = loadedNudges,
                    isMomentsLoading = false,
                    isNudgesLoading = false,
                    momentsError = momentsResult.exceptionOrNull()?.message,
                    nudgesError = nudgesResult.exceptionOrNull()?.message
                )
            }
            if (loadedNudges.isEmpty()) {
                val diagnoseResult = withContext(Dispatchers.IO) {
                    BackendApi.getNudgeDiagnose(token)
                }
                diagnoseResult.getOrNull()?.let { d ->
                    val hint = when (d.suggestion) {
                        "no_signal" -> "Run Evaluate & Deliver after adding transactions to get nudges."
                        "no_rule_matched" -> "No rules matched. Add more transactions or goals, then try Evaluate & Deliver."
                        "run_evaluate_process" -> "Run Evaluate & Deliver to send pending nudges."
                        "ok" -> null
                        else -> "Run Evaluate & Deliver to generate nudges."
                    }
                    if (hint != null) {
                        _uiState.update { state ->
                            if (state.actionMessage == null) state.copy(actionMessage = hint) else state
                        }
                    }
                }
                if (!hasAutoRunPipelineThisSession) {
                    hasAutoRunPipelineThisSession = true
                    _uiState.update { it.copy(isEvaluating = true, actionError = null, actionMessage = null) }
                    try {
                        withContext(Dispatchers.IO) { BackendApi.computeSignal(token, null) }
                        withContext(Dispatchers.IO) { BackendApi.evaluateNudges(token, null) }
                        _uiState.update { it.copy(isEvaluating = false, isComputing = true) }
                        withContext(Dispatchers.IO) { BackendApi.processNudges(token, 10) }
                        _uiState.update { it.copy(isComputing = false) }
                        loadData()
                    } catch (e: Exception) {
                        _uiState.update {
                            it.copy(
                                isEvaluating = false,
                                isComputing = false,
                                actionError = e.message ?: "Failed to evaluate and deliver nudges"
                            )
                        }
                    }
                }
            }
        }
    }

    fun computeProgressMetrics(): ProgressMetrics {
        val moments = _uiState.value.moments
        val nudges = _uiState.value.nudges
        val months = moments.map { it.month }.distinct().sorted().reversed()
        var streak = 0
        if (months.isNotEmpty()) {
            val now = java.util.Calendar.getInstance()
            val currentMonth = "%04d-%02d".format(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH) + 1)
            if (currentMonth in months) {
                streak = 1
                for (i in 1 until months.size) {
                    val prev = months[i - 1]
                    val curr = months[i]
                    val (py, pm) = prev.split("-").map { it.toInt() }
                    val (cy, cm) = curr.split("-").map { it.toInt() }
                    val diff = (py - cy) * 12 + (pm - cm)
                    if (diff == 1) streak++ else break
                }
            }
        }
        val habitsCount = moments.map { it.habit_id }.distinct().size
        val savedAmount = moments
            .filter { it.habit_id.contains("savings", ignoreCase = true) || it.habit_id.contains("assets", ignoreCase = true) }
            .sumOf { if (it.value > 0) it.value else 0.0 }
        return ProgressMetrics(
            streak = streak,
            nudgesCount = nudges.size,
            habitsCount = habitsCount,
            savedAmount = savedAmount
        )
    }

    fun evaluateAndDeliverNudges() {
        viewModelScope.launch {
            val token = getAccessToken()
            if (token == null) {
                _uiState.update {
                    it.copy(actionError = "Sign in to get personalized nudges.")
                }
                return@launch
            }
            _uiState.update {
                it.copy(isEvaluating = true, actionError = null, actionMessage = null)
            }
            try {
                withContext(Dispatchers.IO) {
                    BackendApi.computeSignal(token, null)
                }
                val evalResult = withContext(Dispatchers.IO) {
                    BackendApi.evaluateNudges(token, null)
                }
                _uiState.update { it.copy(isEvaluating = false) }
                _uiState.update { it.copy(isComputing = true) }
                val processResult = withContext(Dispatchers.IO) {
                    BackendApi.processNudges(token, 10)
                }
                val evalBody = evalResult.getOrNull()
                val processBody = processResult.getOrNull()
                val deliveredCount = processBody?.count ?: 0
                val message = when {
                    deliveredCount > 0 -> "${deliveredCount} nudges delivered."
                    evalBody?.status == "no_candidates" -> when (evalBody.reason) {
                        "no_signal" -> "No activity data yet. Add or connect accounts to get personalized nudges."
                        "no_rule_matched" -> "No nudges matched your activity this week. Check back later."
                        else -> "No nudges this time. Try again later."
                    }
                    else -> null
                }
                _uiState.update {
                    it.copy(
                        isComputing = false,
                        actionMessage = message,
                        actionError = evalResult.exceptionOrNull()?.message ?: processResult.exceptionOrNull()?.message
                    )
                }
                loadData()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isEvaluating = false,
                        isComputing = false,
                        actionError = e.message ?: "Failed to evaluate and deliver nudges",
                        actionMessage = null
                    )
                }
            }
        }
    }

    fun computeMoments() {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            _uiState.update { it.copy(isComputing = true, actionError = null) }
            var successCount = 0
            val now = java.util.Calendar.getInstance()
            for (i in 0 until 12) {
                val cal = java.util.Calendar.getInstance().apply {
                    set(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH) - i, 1)
                }
                val monthStr = "%04d-%02d".format(cal.get(java.util.Calendar.YEAR), cal.get(java.util.Calendar.MONTH) + 1)
                try {
                    val result = withContext(Dispatchers.IO) {
                        BackendApi.computeMoments(token, monthStr)
                    }
                    if (result.isSuccess) successCount++
                } catch (_: Exception) {}
            }
            _uiState.update { it.copy(isComputing = false) }
            if (successCount > 0) {
                loadData()
            } else {
                _uiState.update {
                    it.copy(actionError = "Failed to compute moments. Upload transaction data first.")
                }
            }
        }
    }

    fun logNudgeInteraction(deliveryId: String, eventType: String) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            withContext(Dispatchers.IO) {
                BackendApi.logNudgeInteraction(token, deliveryId, eventType, null)
            }
        }
    }

    fun clearActionError() {
        _uiState.update { it.copy(actionError = null) }
    }

    fun clearActionMessage() {
        _uiState.update { it.copy(actionMessage = null) }
    }
}
