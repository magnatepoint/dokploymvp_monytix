package com.example.monytix.budgetpilot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.data.BackendApi
import com.example.monytix.data.BudgetRecommendation
import com.example.monytix.data.CommittedBudget
import com.example.monytix.data.BudgetVariance
import com.example.monytix.data.Supabase
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.format.DateTimeFormatter

data class BudgetDeviation(
    val needs: Double,
    val wants: Double,
    val savings: Double
)

data class AutopilotSuggestion(
    val shiftFrom: String,
    val shiftTo: String,
    val pct: Double,
    val message: String
)

data class BudgetPilotUiState(
    val recommendations: List<BudgetRecommendation> = emptyList(),
    val committedBudget: CommittedBudget? = null,
    val variance: BudgetVariance? = null,
    val deviation: BudgetDeviation? = null,
    val autopilotSuggestion: AutopilotSuggestion? = null,
    val userEmail: String? = null,
    val selectedMonth: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE).substring(0, 7),
    val isLoadingRecommendations: Boolean = false,
    val isLoadingCommitted: Boolean = false,
    val isLoadingVariance: Boolean = false,
    val isCommitting: Boolean = false,
    val committingPlanCode: String? = null,
    val isApplyingAdjustment: Boolean = false,
    val error: String? = null
)

class BudgetPilotViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(BudgetPilotUiState())
    val uiState: StateFlow<BudgetPilotUiState> = _uiState.asStateFlow()

    private fun getAccessToken(): String? =
        Supabase.client.auth.currentSessionOrNull()?.accessToken

    init {
        loadSession()
        loadData()
        // If BudgetUpdateCache has data from a transaction, we'll refresh when screen is shown
        viewModelScope.launch {
            BudgetUpdateCache.consume()?.let { loadData() }
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

    fun setMonth(month: String) {
        _uiState.update { it.copy(selectedMonth = month) }
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val month = _uiState.value.selectedMonth
            val monthParam = if (month.isNotBlank()) "${month}-01" else null

            _uiState.update {
                it.copy(
                    isLoadingRecommendations = true,
                    isLoadingCommitted = true,
                    isLoadingVariance = true,
                    error = null
                )
            }

            val recResult = withContext(Dispatchers.IO) {
                BackendApi.getBudgetRecommendations(token, monthParam)
            }
            val committedResult = withContext(Dispatchers.IO) {
                BackendApi.getCommittedBudget(token, monthParam)
            }
            val varianceResult = withContext(Dispatchers.IO) {
                BackendApi.getBudgetVariance(token, monthParam)
            }

            val committed = committedResult.getOrNull()
            val committedBudget = when (committed?.status) {
                "committed" -> committed.budget
                else -> null
            }

            val varianceResp = varianceResult.getOrNull()
            val variance = when (varianceResp?.status) {
                "ok" -> varianceResp.aggregate
                else -> null
            }

            val (deviation, suggestion) = variance?.let { v -> computeDeviationAndSuggestion(v, committedBudget) } ?: (null to null)

            _uiState.update {
                it.copy(
                    recommendations = recResult.getOrNull()?.recommendations ?: emptyList(),
                    committedBudget = committedBudget,
                    variance = variance,
                    deviation = deviation,
                    autopilotSuggestion = suggestion,
                    isLoadingRecommendations = false,
                    isLoadingCommitted = false,
                    isLoadingVariance = false,
                    error = recResult.exceptionOrNull()?.message
                        ?: committedResult.exceptionOrNull()?.message
                        ?: varianceResult.exceptionOrNull()?.message
                )
            }
        }
    }

    fun commitBudget(planCode: String) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val month = _uiState.value.selectedMonth
            val monthParam = if (month.isNotBlank()) "${month}-01" else null

            _uiState.update {
                it.copy(isCommitting = true, committingPlanCode = planCode)
            }

            val result = withContext(Dispatchers.IO) {
                BackendApi.commitBudget(token, planCode, monthParam)
            }

            result.fold(
                onSuccess = { budget ->
                    _uiState.update {
                        it.copy(
                            committedBudget = budget,
                            isCommitting = false,
                            committingPlanCode = null
                        )
                    }
                    loadData()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isCommitting = false,
                            committingPlanCode = null,
                            error = e.message ?: "Failed to commit budget"
                        )
                    }
                }
            )
        }
    }

    fun refresh() {
        BudgetUpdateCache.consume() // clear any stale cache
        loadData()
    }

    fun applyAdjustment(shiftFrom: String, shiftTo: String, pct: Double) {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val month = _uiState.value.selectedMonth
            val monthParam = if (month.isNotBlank()) month else null

            _uiState.update { it.copy(isApplyingAdjustment = true) }

            val result = withContext(Dispatchers.IO) {
                BackendApi.applyBudgetAdjustment(token, shiftFrom, shiftTo, pct, monthParam)
            }

            result.fold(
                onSuccess = { resp ->
                    when (resp.status) {
                        "applied" -> loadData()
                        "rejected" -> _uiState.update {
                            it.copy(error = resp.reason ?: "Adjustment rejected")
                        }
                        else -> _uiState.update { it.copy(error = "Unknown response") }
                    }
                    _uiState.update { it.copy(isApplyingAdjustment = false) }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isApplyingAdjustment = false,
                            error = e.message ?: "Failed to apply adjustment"
                        )
                    }
                }
            )
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    private fun computeDeviationAndSuggestion(
        v: BudgetVariance,
        committed: CommittedBudget?
    ): Pair<BudgetDeviation?, AutopilotSuggestion?> {
        if (v.income_amt <= 0 || committed == null) return null to null
        val income = v.income_amt
        val actualNeedsPct = (v.needs_amt / income) * 100
        val actualWantsPct = (v.wants_amt / income) * 100
        val actualSavingsPct = (v.assets_amt / income) * 100
        val plannedNeedsPct = committed.alloc_needs_pct * 100
        val plannedWantsPct = committed.alloc_wants_pct * 100
        val plannedSavingsPct = committed.alloc_assets_pct * 100
        val deviation = BudgetDeviation(
            needs = actualNeedsPct - plannedNeedsPct,
            wants = actualWantsPct - plannedWantsPct,
            savings = actualSavingsPct - plannedSavingsPct
        )
        val suggestion = when {
            deviation.savings < -5 && deviation.wants > 5 ->
                AutopilotSuggestion("wants", "savings", minOf(5.0, -deviation.savings / 2, deviation.wants), "Move ${minOf(5.0, -deviation.savings / 2).toInt()}% from wants to savings")
            deviation.savings < -5 ->
                AutopilotSuggestion("wants", "savings", minOf(5.0, -deviation.savings / 2), "Move ${minOf(5.0, -deviation.savings / 2).toInt()}% from wants to savings")
            else -> null
        }
        return deviation to suggestion
    }
}
