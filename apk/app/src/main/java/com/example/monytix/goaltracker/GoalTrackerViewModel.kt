package com.example.monytix.goaltracker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.data.BackendApi
import com.example.monytix.data.GoalProgressItem
import com.example.monytix.data.GoalResponse
import com.example.monytix.goaltracker.GoalUpdateCache
import com.example.monytix.analytics.AnalyticsHelper
import com.example.monytix.auth.FirebaseAuthManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class CreateGoalResult {
    data object Success : CreateGoalResult()
    data class Failure(val message: String) : CreateGoalResult()
}

data class GoalTrackerUiState(
    val goals: List<GoalResponse> = emptyList(),
    val progress: List<GoalProgressItem> = emptyList(),
    val userEmail: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val createGoalResult: CreateGoalResult? = null,
    val selectedFilter: String? = null,
    val recentlyUpdatedGoalPrevPct: Map<String, Float> = emptyMap(),
    val lastSyncTime: Long = 0L
)

class GoalTrackerViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(GoalTrackerUiState())
    val uiState: StateFlow<GoalTrackerUiState> = _uiState.asStateFlow()

    private suspend fun getAccessToken(): String? =
        FirebaseAuthManager.getIdToken()

    init {
        loadSession()
        loadData()
        consumeGoalUpdateCache()
    }

    private fun consumeGoalUpdateCache() {
        val updated = GoalUpdateCache.consume()
        if (updated.isNotEmpty()) {
            val map = updated.associate { it.goal_id to it.prev_pct.toFloat() }
            _uiState.update { it.copy(recentlyUpdatedGoalPrevPct = map) }
            viewModelScope.launch {
                delay(2000)
                _uiState.update { it.copy(recentlyUpdatedGoalPrevPct = emptyMap()) }
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

    fun loadData() {
        viewModelScope.launch {
            consumeGoalUpdateCache()
            val token = getAccessToken() ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            val goalsResult = withContext(Dispatchers.IO) { BackendApi.getUserGoals(token) }
            val progressResult = withContext(Dispatchers.IO) { BackendApi.getGoalsProgress(token) }
            _uiState.update {
                it.copy(
                    goals = goalsResult.getOrNull() ?: emptyList(),
                    progress = progressResult.getOrNull()?.goals ?: emptyList(),
                    isLoading = false,
                    error = goalsResult.exceptionOrNull()?.message ?: progressResult.exceptionOrNull()?.message,
                    lastSyncTime = System.currentTimeMillis()
                )
            }
        }
    }

    fun setFilter(filter: String?) {
        _uiState.update { it.copy(selectedFilter = filter) }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    fun clearCreateGoalResult() {
        _uiState.update { it.copy(createGoalResult = null) }
    }

    fun createGoal(
        goalCategory: String,
        goalName: String,
        estimatedCost: Double,
        targetDate: String?,
        currentSavings: Double,
        goalType: String? = null,
        importance: Int? = null
    ) {
        viewModelScope.launch {
            val token = getAccessToken()
            if (token == null) {
                _uiState.update {
                    it.copy(
                        createGoalResult = CreateGoalResult.Failure("Please sign in to add goals")
                    )
                }
                return@launch
            }
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = withContext(Dispatchers.IO) {
                BackendApi.createGoal(token, goalCategory, goalName, estimatedCost, targetDate, currentSavings, goalType, importance)
            }
            result.fold(
                onSuccess = {
                    AnalyticsHelper.logEvent("goal_created", mapOf(
                        "goal_category" to goalCategory,
                        "goal_type" to (goalType ?: "default"),
                        "has_target_date" to (targetDate != null).toString()
                    ))
                    _uiState.update { it.copy(isLoading = false, createGoalResult = CreateGoalResult.Success) }
                    loadData()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to add goal",
                            createGoalResult = CreateGoalResult.Failure(e.message ?: "Failed to add goal")
                        )
                    }
                }
            )
        }
    }

    fun updateGoal(
        goalId: String,
        estimatedCost: Double? = null,
        targetDate: String? = null,
        currentSavings: Double? = null,
        goalType: String? = null,
        importance: Int? = null
    ) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = withContext(Dispatchers.IO) {
                BackendApi.updateGoal(token, goalId, estimatedCost, targetDate, currentSavings, goalType, importance)
            }
            result.fold(
                onSuccess = {
                    AnalyticsHelper.logEvent("goal_edited")
                    _uiState.update { it.copy(isLoading = false) }
                    loadData()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to update goal"
                        )
                    }
                }
            )
        }
    }

    fun deleteGoal(goalId: String) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            val result = withContext(Dispatchers.IO) {
                BackendApi.deleteGoal(token, goalId)
            }
            result.fold(
                onSuccess = {
                    AnalyticsHelper.logEvent("goal_deleted")
                    _uiState.update { it.copy(isLoading = false) }
                    loadData()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = e.message ?: "Failed to delete goal"
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        AnalyticsHelper.logEvent("refresh")
        loadData()
    }

    data class GoalHealthSummary(val score: Int, val onTrack: Int, val atRisk: Int)

    fun goalHealthSummary(): GoalHealthSummary {
        val goals = _uiState.value.goals.filter { it.status.lowercase() == "active" }
        val progress = _uiState.value.progress
        if (goals.isEmpty()) return GoalHealthSummary(0, 0, 0)
        var onTrack = 0
        var atRisk = 0
        for (g in goals) {
            val prog = progress.find { it.goal_id == g.goal_id }
            val pct = prog?.progress_pct ?: 0.0
            val daysLeft = prog?.days_to_target ?: 999
            val monthlyReq = prog?.monthly_required ?: 0.0
            when {
                pct >= 100 -> onTrack++
                daysLeft <= 30 && pct < 50 -> atRisk++
                monthlyReq > 15000 -> atRisk++
                else -> onTrack++
            }
        }
        val score = if (goals.isEmpty()) 0 else ((onTrack.toDouble() / goals.size) * 60 + (1 - atRisk.toDouble() / goals.size.coerceAtLeast(1)) * 40).toInt().coerceIn(0, 100)
        return GoalHealthSummary(score, onTrack, atRisk)
    }

    fun upcomingDeadlines(): List<Pair<GoalResponse, GoalProgressItem?>> {
        val goals = _uiState.value.goals.filter { it.status.lowercase() == "active" }
        val progress = _uiState.value.progress
        return goals
            .map { g -> g to progress.find { it.goal_id == g.goal_id } }
            .filter { (_, prog) -> (prog?.days_to_target ?: 999) in 1..365 }
            .sortedBy { (_, prog) -> prog?.days_to_target ?: 999 }
            .take(5)
    }

    fun goalEmoji(goal: GoalResponse): String = when {
        goal.goal_name.contains("Retirement", ignoreCase = true) -> "🎯"
        goal.goal_name.contains("Home", ignoreCase = true) || goal.goal_name.contains("Down Payment", ignoreCase = true) -> "🏠"
        goal.goal_name.contains("Credit", ignoreCase = true) || goal.goal_name.contains("Debt", ignoreCase = true) || goal.goal_name.contains("Paydown", ignoreCase = true) -> "💳"
        goal.goal_name.contains("Emergency", ignoreCase = true) -> "🛡️"
        goal.goal_name.contains("Education", ignoreCase = true) -> "📚"
        goal.goal_name.contains("Travel", ignoreCase = true) || goal.goal_name.contains("Vacation", ignoreCase = true) -> "✈️"
        else -> "🎯"
    }

    fun goalEmotionalLabel(goal: GoalResponse): String = when {
        goal.goal_name.contains("Retirement", ignoreCase = true) -> "Long-term freedom"
        goal.goal_name.contains("Home", ignoreCase = true) || goal.goal_name.contains("Down Payment", ignoreCase = true) -> "Dream Home Fund"
        goal.goal_name.contains("Credit", ignoreCase = true) || goal.goal_name.contains("Debt", ignoreCase = true) || goal.goal_name.contains("Paydown", ignoreCase = true) -> "Debt Freedom"
        goal.goal_name.contains("Emergency", ignoreCase = true) -> "Safety Net"
        goal.goal_name.contains("Education", ignoreCase = true) -> "Future Ready"
        goal.goal_name.contains("Travel", ignoreCase = true) || goal.goal_name.contains("Vacation", ignoreCase = true) -> "Adventure Fund"
        else -> "Your goal"
    }
}
