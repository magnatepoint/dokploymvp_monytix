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
import java.time.LocalDate

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
    val actionMessage: String? = null,
    val nudgeFromDate: String? = null,
    val nudgeToDate: String? = null
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

    private suspend fun getAccessToken(): String? =
        FirebaseAuthManager.getIdToken()

    /** Default nudge range: last 7 days (from = today-6, to = today). */
    private fun defaultNudgeFromDate(): String = LocalDate.now().minusDays(6).toString()
    private fun defaultNudgeToDate(): String = LocalDate.now().toString()

    /** Effective from/to for API calls; uses state range or default last 7 days. */
    private fun nudgeRangeFrom(state: MoneyMomentsUiState): String =
        state.nudgeFromDate ?: defaultNudgeFromDate()
    private fun nudgeRangeTo(state: MoneyMomentsUiState): String =
        state.nudgeToDate ?: defaultNudgeToDate()

    fun setNudgeDateRange(fromDate: String, toDate: String) {
        _uiState.update { it.copy(nudgeFromDate = fromDate, nudgeToDate = toDate) }
    }

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
            val state = _uiState.value
            val from = nudgeRangeFrom(state)
            val to = nudgeRangeTo(state)
            if (state.nudgeFromDate == null || state.nudgeToDate == null) {
                _uiState.update { it.copy(nudgeFromDate = from, nudgeToDate = to) }
            }
            val momentsResult = withContext(Dispatchers.IO) {
                BackendApi.getMoments(token, month = null, allMonths = false)
            }
            val nudgesResult = withContext(Dispatchers.IO) {
                BackendApi.getNudges(token, limit = 20, fromDate = from, toDate = to)
            }
            _uiState.update {
                it.copy(
                    moments = momentsResult.getOrNull()?.moments ?: emptyList(),
                    nudges = nudgesResult.getOrNull()?.nudges ?: emptyList(),
                    isMomentsLoading = false,
                    isNudgesLoading = false,
                    momentsError = momentsResult.exceptionOrNull()?.message,
                    nudgesError = nudgesResult.exceptionOrNull()?.message
                )
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
