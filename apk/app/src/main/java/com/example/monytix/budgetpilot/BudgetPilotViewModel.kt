package com.example.monytix.budgetpilot

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.data.BackendApi
import com.example.monytix.data.TransactionCreateRequest
import com.example.monytix.data.BudgetRecommendation
import com.example.monytix.data.CommittedBudget
import com.example.monytix.data.BudgetVariance
import com.example.monytix.data.Supabase
import com.example.monytix.goaltracker.GoalUpdateCache
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
    val budgetState: com.example.monytix.data.BudgetStateResponse? = null,
    val lastUpdatedAt: String? = null,
    val userEmail: String? = null,
    val selectedMonth: String = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE).substring(0, 7),
    val goalsCount: Int = 0,
    val isLoadingRecommendations: Boolean = false,
    val isLoadingCommitted: Boolean = false,
    val isLoadingVariance: Boolean = false,
    val isLoadingState: Boolean = false,
    val isCommitting: Boolean = false,
    val committingPlanCode: String? = null,
    val isApplyingAdjustment: Boolean = false,
    val isRecalculating: Boolean = false,
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
            val monthParam = if (month.isNotBlank()) month else null

            _uiState.update {
                it.copy(
                    isLoadingState = true,
                    isLoadingRecommendations = true,
                    isLoadingCommitted = true,
                    isLoadingVariance = true,
                    error = null
                )
            }

            val stateResult = withContext(Dispatchers.IO) { BackendApi.getBudgetState(token, monthParam) }
            val goalsResult = withContext(Dispatchers.IO) { BackendApi.getUserGoals(token) }
            val goalsCount = goalsResult.getOrNull()?.count { it.status.lowercase() == "active" } ?: 0

            stateResult.fold(
                onSuccess = { state ->
                    val committedPlan = state.committed_plan
                    val committedBudget = committedPlan?.let { cp ->
                        CommittedBudget(
                            plan_code = cp.plan_id,
                            alloc_needs_pct = (cp.target["needs"] ?: 0.0) / 100.0,
                            alloc_wants_pct = (cp.target["wants"] ?: 0.0) / 100.0,
                            alloc_assets_pct = (cp.target["savings"] ?: 0.0) / 100.0,
                            goal_allocations = emptyList()
                        )
                    }
                    val actual = state.actual
                    val income = state.income_amt
                    val variance = when {
                        committedPlan != null && income > 0 -> {
                            val targetNeeds = (committedPlan.target["needs"] ?: 0.0) / 100.0 * income
                            val targetWants = (committedPlan.target["wants"] ?: 0.0) / 100.0 * income
                            val targetSavings = (committedPlan.target["savings"] ?: 0.0) / 100.0 * income
                            BudgetVariance(
                                income_amt = income,
                                needs_amt = actual?.needs_amt ?: 0.0,
                                planned_needs_amt = targetNeeds,
                                variance_needs_amt = (actual?.needs_amt ?: 0.0) - targetNeeds,
                                wants_amt = actual?.wants_amt ?: 0.0,
                                planned_wants_amt = targetWants,
                                variance_wants_amt = (actual?.wants_amt ?: 0.0) - targetWants,
                                assets_amt = actual?.savings_amt ?: 0.0,
                                planned_assets_amt = targetSavings,
                                variance_assets_amt = (actual?.savings_amt ?: 0.0) - targetSavings
                            )
                        }
                        actual != null && (income > 0 || (actual.needs_amt + actual.wants_amt + actual.savings_amt) > 0) -> {
                            val effectiveIncome = if (income > 0) income else (actual.needs_amt + actual.wants_amt + actual.savings_amt)
                            BudgetVariance(
                                income_amt = effectiveIncome,
                                needs_amt = actual.needs_amt,
                                planned_needs_amt = 0.0,
                                variance_needs_amt = 0.0,
                                wants_amt = actual.wants_amt,
                                planned_wants_amt = 0.0,
                                variance_wants_amt = 0.0,
                                assets_amt = actual.savings_amt,
                                planned_assets_amt = 0.0,
                                variance_assets_amt = 0.0
                            )
                        }
                        else -> null
                    }
                    val deviation = state.deviation?.let { d ->
                        BudgetDeviation(needs = d.needs, wants = d.wants, savings = d.savings)
                    }
                    val suggestion = deviation?.let { d ->
                        when {
                            d.savings < -5 && d.wants > 5 ->
                                AutopilotSuggestion("wants", "savings", minOf(5.0, -d.savings / 2, d.wants), "Move ${minOf(5.0, -d.savings / 2).toInt()}% from wants to savings")
                            d.savings < -5 ->
                                AutopilotSuggestion("wants", "savings", minOf(5.0, -d.savings / 2), "Move ${minOf(5.0, -d.savings / 2).toInt()}% from wants to savings")
                            else -> null
                        }
                    }
                    val recommendations = state.plans.map { p ->
                        BudgetRecommendation(
                            plan_code = p.plan_id,
                            name = p.name,
                            recommendation_reason = p.reason,
                            score = p.score
                        )
                    }
                    _uiState.update {
                        it.copy(
                            budgetState = state,
                            committedBudget = committedBudget,
                            variance = variance,
                            deviation = deviation,
                            autopilotSuggestion = suggestion,
                            recommendations = recommendations,
                            lastUpdatedAt = state.last_updated_at,
                            goalsCount = goalsCount,
                            isLoadingState = false,
                            isLoadingRecommendations = false,
                            isLoadingCommitted = false,
                            isLoadingVariance = false
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingState = false,
                            isLoadingRecommendations = false,
                            isLoadingCommitted = false,
                            isLoadingVariance = false,
                            error = e.message ?: "Failed to load budget state"
                        )
                    }
                }
            )
        }
    }

    fun recalculate() {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            val month = _uiState.value.selectedMonth
            val monthParam = if (month.isNotBlank()) month else null

            _uiState.update { it.copy(isRecalculating = true) }
            val result = withContext(Dispatchers.IO) {
                BackendApi.recalculateBudget(token, monthParam)
            }
            result.fold(
                onSuccess = { loadData() },
                onFailure = { e -> _uiState.update { it.copy(error = e.message ?: "Recalculate failed") } }
            )
            _uiState.update { it.copy(isRecalculating = false) }
        }
    }

    fun commitBudget(planCode: String) {
        Log.d("BudgetPilot", "commitBudget called: planCode=$planCode")
        viewModelScope.launch {
            val token = getAccessToken()
            if (token == null) {
                Log.e("BudgetPilot", "commitBudget: No access token, user may not be logged in")
                _uiState.update {
                    it.copy(error = "Not logged in. Please sign in again.")
                }
                return@launch
            }
            val month = _uiState.value.selectedMonth
            val monthParam = if (month.isNotBlank()) "${month}-01" else null
            Log.d("BudgetPilot", "commitBudget: calling API planCode=$planCode month=$monthParam")

            _uiState.update {
                it.copy(isCommitting = true, committingPlanCode = planCode)
            }

            val result = withContext(Dispatchers.IO) {
                BackendApi.commitBudget(token, planCode, monthParam)
            }

            Log.d("BudgetPilot", "commitBudget: result isSuccess=${result.isSuccess}")
            result.fold(
                onSuccess = { budget ->
                    Log.d("BudgetPilot", "commitBudget: success plan_code=${budget.plan_code}")
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
                    Log.e("BudgetPilot", "commitBudget: failed", e)
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
            _uiState.update { it.copy(isLoadingState = true, error = null) }
            val result = withContext(Dispatchers.IO) {
                BackendApi.createTransaction(
                    token,
                    com.example.monytix.data.TransactionCreateRequest(
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
                    BudgetUpdateCache.setFromTransactionCreate(resp.budget_state)
                    GoalUpdateCache.setFromTransactionCreate(resp.updated_goals)
                    _uiState.update { it.copy(isLoadingState = false) }
                    loadData()
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoadingState = false,
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
