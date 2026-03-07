package com.example.monytix.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.data.BackendApi
import com.example.monytix.data.GoalProgressItem
import com.example.monytix.data.InsightsResponse
import com.example.monytix.data.KpiResponse
import com.example.monytix.analytics.AnalyticsHelper
import com.example.monytix.auth.FirebaseAuthManager
import com.example.monytix.data.TransactionRecordResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class HomeUiState(
    val kpis: KpiResponse? = null,
    val accounts: List<com.example.monytix.data.AccountItemResponse> = emptyList(),
    val insights: InsightsResponse? = null,
    val goalsProgress: List<GoalProgressItem> = emptyList(),
    val recentTransactions: List<TransactionRecordResponse> = emptyList(),
    val topInsightsFromApi: List<AiInsight>? = null,
    val backendStatus: String? = null,
    val backendError: String? = null,
    val userEmail: String? = null,
    val isLoading: Boolean = false
)

data class ConsoleGoal(
    val id: String,
    val name: String,
    val targetAmount: Double,
    val savedAmount: Double,
    val targetDate: String?,
    val isActive: Boolean
)

data class ConsoleCategorySpending(
    val category: String,
    val amount: Double,
    val percentage: Double,
    val transactionCount: Int
)

data class AiInsight(
    val id: String,
    val title: String,
    val message: String,
    val type: String,
    val confidence: Float? = null
)

/** First-fold state for command center home. */
data class HealthState(
    val score: Int,
    val trend: String,
    val subtext: String
)

data class RiskState(
    val label: String,
    val reason: String
)

data class NextAction(
    val type: String,
    val label: String,
    val payload: String?
)

class HomeViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        loadDashboard()
        checkBackend()
    }

    private suspend fun getAccessToken(): String? =
        FirebaseAuthManager.getIdToken()

    fun loadDashboard() {
        viewModelScope.launch {
            val token = getAccessToken() ?: return@launch
            _uiState.update { it.copy(isLoading = true) }
            val now = java.util.Calendar.getInstance()
            val startOfMonth = java.util.Calendar.getInstance().apply {
                set(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH), 1)
            }
            val endOfMonth = java.util.Calendar.getInstance().apply {
                set(now.get(java.util.Calendar.YEAR), now.get(java.util.Calendar.MONTH) + 1, 0)
            }
            val startDate = "%04d-%02d-%02d".format(
                startOfMonth.get(java.util.Calendar.YEAR),
                startOfMonth.get(java.util.Calendar.MONTH) + 1,
                startOfMonth.get(java.util.Calendar.DAY_OF_MONTH)
            )
            val endDate = "%04d-%02d-%02d".format(
                endOfMonth.get(java.util.Calendar.YEAR),
                endOfMonth.get(java.util.Calendar.MONTH) + 1,
                endOfMonth.get(java.util.Calendar.DAY_OF_MONTH)
            )
            val kpisResult = withContext(Dispatchers.IO) { BackendApi.getKpis(token) }
            val accountsResult = withContext(Dispatchers.IO) { BackendApi.getAccounts(token) }
            val insightsResult = withContext(Dispatchers.IO) { BackendApi.getInsights(token, startDate, endDate) }
            val goalsResult = withContext(Dispatchers.IO) { BackendApi.getGoalsProgress(token) }
            val sessionResult = withContext(Dispatchers.IO) { BackendApi.getSession(token) }
            val transactionsResult = withContext(Dispatchers.IO) { BackendApi.getTransactions(token, limit = 5) }
            val topInsightsResult = withContext(Dispatchers.IO) { BackendApi.getTopInsights(token, limit = 5) }
            val topInsightsFromApi = topInsightsResult.getOrNull()?.insights?.map { i ->
                AiInsight(i.id, i.title, i.message, i.type, i.confidence?.toFloat())
            }
            _uiState.update {
                it.copy(
                    kpis = kpisResult.getOrNull(),
                    accounts = accountsResult.getOrNull()?.accounts ?: emptyList(),
                    insights = insightsResult.getOrNull(),
                    goalsProgress = goalsResult.getOrNull()?.goals ?: emptyList(),
                    recentTransactions = transactionsResult.getOrNull()?.transactions ?: emptyList(),
                    topInsightsFromApi = topInsightsFromApi,
                    userEmail = sessionResult.getOrNull()?.email,
                    isLoading = false
                )
            }
        }
    }

    fun transformGoals(): List<ConsoleGoal> = _uiState.value.goalsProgress.map { g ->
        ConsoleGoal(
            id = g.goal_id,
            name = g.goal_name,
            targetAmount = g.current_savings_close + g.remaining_amount,
            savedAmount = g.current_savings_close,
            targetDate = g.projected_completion_date,
            isActive = true
        )
    }

    fun transformCategorySpending(): List<ConsoleCategorySpending> {
        val breakdown = _uiState.value.insights?.category_breakdown ?: return emptyList()
        return breakdown.map { c ->
            ConsoleCategorySpending(
                category = c.category_name,
                amount = c.amount,
                percentage = c.percentage,
                transactionCount = c.transaction_count
            )
        }
    }

    fun totalNetWorth(): Double =
        _uiState.value.accounts.sumOf { it.balance }

    fun netWorthTrendPct(): Double {
        val trends = _uiState.value.insights?.spending_trends ?: return 0.0
        if (trends.size < 2) return _uiState.value.kpis?.best_month?.delta_pct ?: 0.0
        val latest = trends.last().net
        val previous = trends[trends.size - 2].net
        return if (previous != 0.0) ((latest - previous) / kotlin.math.abs(previous) * 100) else 0.0
    }

    fun spendingVsLastMonthPct(): Double? {
        val trends = _uiState.value.insights?.spending_trends ?: return null
        if (trends.size < 2) return null
        val latest = trends.last().expenses
        val prev = trends[trends.size - 2].expenses
        return if (prev != 0.0) ((latest - prev) / prev * 100) else null
    }

    fun projectedMonthEndSpending(): Double? {
        val kpis = _uiState.value.kpis ?: return null
        val spent = kpis.total_debits_amount ?: (kpis.needs_amount ?: 0.0) + (kpis.wants_amount ?: 0.0)
        if (spent <= 0) return null
        val now = java.util.Calendar.getInstance()
        val dayOfMonth = now.get(java.util.Calendar.DAY_OF_MONTH).toDouble().coerceAtLeast(1.0)
        val daysInMonth = now.getActualMaximum(java.util.Calendar.DAY_OF_MONTH).toDouble()
        return (spent / dayOfMonth) * daysInMonth
    }

    fun sparklineData(): List<Float> {
        val ts = _uiState.value.insights?.time_series
        if (!ts.isNullOrEmpty()) return ts.map { it.value.toFloat() }
        val trends = _uiState.value.insights?.spending_trends ?: return emptyList()
        return trends.map { it.net.toFloat() }
    }

    /** Top 3 insights for command center: from API if available, else from generateAiInsights(). */
    fun topInsightsForCommandCenter(): List<AiInsight> =
        _uiState.value.topInsightsFromApi?.take(3) ?: generateAiInsights().take(3)

    fun generateAiInsights(): List<AiInsight> {
        val kpis = _uiState.value.kpis ?: return emptyList()
        val insights = _uiState.value.insights
        val goals = transformGoals()
        val list = mutableListOf<AiInsight>()
        val income = kpis.income_amount ?: 0.0
        val expenses = kpis.total_debits_amount ?: (kpis.needs_amount ?: 0.0) + (kpis.wants_amount ?: 0.0)
        val emiCat = insights?.category_breakdown?.find { it.category_name.contains("EMI", ignoreCase = true) || it.category_name.contains("Loan", ignoreCase = true) }
        val emiPct = if (income > 0 && emiCat != null) (emiCat.amount / income * 100) else 0.0
        if (emiPct >= 30) {
            list.add(AiInsight("risk_emi", "🟡 Risk",
                "EMIs consume ${emiPct.toInt()}% of your income. Consider refinancing or prepayment.",
                "risk", 0.9f))
        }
        val surplus = income - expenses
        if (surplus > 5000 && income > 0) {
            val safeSip = (surplus * 0.3).toInt()
            list.add(AiInsight("opt_sip", "🟢 Optimization",
                "You can increase SIP by ₹${java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(safeSip)} safely.",
                "optimization", 0.85f))
        }
        val topCat = kpis.top_categories.firstOrNull()
        if (topCat != null && topCat.spend_amount > 50000) {
            list.add(AiInsight("1", "🟡 Risk",
                "Your spending on ${topCat.category_name.lowercase()} is high. Consider setting a limit.",
                "risk", 0.8f))
        }
        val transferCat = insights?.category_breakdown?.find { it.category_name.contains("Transfer", ignoreCase = true) }
        if (transferCat != null && insights.category_breakdown.isNotEmpty()) {
            val avg = insights.category_breakdown.map { it.amount }.average()
            if (transferCat.amount > avg * 1.3) {
                list.add(AiInsight("pattern", "🔵 Pattern",
                    "You spend most on transfers between 1st–5th. Plan ahead for those dates.",
                    "pattern", 0.75f))
            }
        }
        goals.firstOrNull()?.let { g ->
            val progress = if (g.targetAmount > 0) g.savedAmount / g.targetAmount else 0.0
            if (progress > 0.8) {
                list.add(AiInsight("2", "🟢 Good News",
                    "You're on track to reach your ${g.name.lowercase()} goal soon.", "goal_progress", 0.9f))
            }
        }
        val foodCat = insights?.category_breakdown?.find { it.category_name.contains("Food", ignoreCase = true) || it.category_name.contains("Dining", ignoreCase = true) }
        if (foodCat != null && foodCat.percentage > 25) {
            list.add(AiInsight("3", "🟡 Budget Tip",
                "You're spending ${foodCat.percentage.toInt()}% on ${foodCat.category_name.lowercase()}. Meal planning could save ₹${(foodCat.amount * 0.15).toInt()}.",
                "budget_tip", 0.8f))
        }
        if ((kpis.assets_amount ?: 0.0) > 1000000) {
            list.add(AiInsight("4", "🟢 Optimization",
                "Your portfolio shows strong growth. Consider increasing SIP contributions.",
                "optimization", 0.85f))
        }
        if (list.isEmpty()) {
            list.add(AiInsight("0", "✨ On Track",
                "Your finances look on track this month. Keep monitoring to stay ahead.",
                "on_track", 0.7f))
        }
        return list
    }

    fun isCashFlowPositive(): Boolean {
        val kpis = _uiState.value.kpis ?: return false
        val income = kpis.income_amount ?: 0.0
        val expenses = kpis.total_debits_amount ?: (kpis.needs_amount ?: 0.0) + (kpis.wants_amount ?: 0.0)
        return income >= expenses
    }

    fun goalsAtRiskCount(): Int = transformGoals().count { g ->
        val progress = if (g.targetAmount > 0) g.savedAmount / g.targetAmount * 100 else 0.0
        progress < 40
    }

    fun spendingSpikeCount(): Int {
        val insights = _uiState.value.insights ?: return 0
        val kpis = _uiState.value.kpis ?: return 0
        val topCats = kpis.top_categories
        if (topCats.isEmpty()) return 0
        val breakdown = insights.category_breakdown
        var spikes = 0
        topCats.forEach { kpi ->
            val cat = breakdown.find { it.category_code == kpi.category_code } ?: return@forEach
            val avg = cat.avg_transaction
            if (avg > 0 && kpi.spend_amount > avg * 1.5) spikes++
        }
        return spikes
    }

    data class TodayIntelligenceItem(val label: String, val type: String)
    fun todayIntelligence(): List<TodayIntelligenceItem> {
        val items = mutableListOf<TodayIntelligenceItem>()
        if (isCashFlowPositive()) items.add(TodayIntelligenceItem("Cash flow positive", "positive"))
        val atRisk = goalsAtRiskCount()
        if (atRisk > 0) items.add(TodayIntelligenceItem("$atRisk goal${if (atRisk > 1) "s" else ""} at risk", "risk"))
        val spikes = spendingSpikeCount()
        if (spikes > 0) items.add(TodayIntelligenceItem("$spikes spending spike${if (spikes > 1) "s" else ""}", "spike"))
        val recurringCount = _uiState.value.insights?.recurring_transactions?.size ?: 0
        if (recurringCount > 0) items.add(TodayIntelligenceItem("$recurringCount recurring payments", "recurring"))
        val largeDebits = _uiState.value.recentTransactions.count { it.direction.lowercase() == "debit" && kotlin.math.abs(it.amount) >= 10000 }
        if (largeDebits > 0) items.add(TodayIntelligenceItem("$largeDebits large debit${if (largeDebits > 1) "s" else ""}", "large"))
        return items.take(4)
    }

    fun hasNoTransactionData(): Boolean {
        val kpis = _uiState.value.kpis ?: return true
        if (kpis.month == null) return true
        val hasIncome = (kpis.income_amount ?: 0.0) > 0
        val hasNeeds = (kpis.needs_amount ?: 0.0) > 0
        val hasWants = (kpis.wants_amount ?: 0.0) > 0
        val hasAssets = (kpis.assets_amount ?: 0.0) > 0
        val hasCategories = kpis.top_categories.isNotEmpty()
        return !hasIncome && !hasNeeds && !hasWants && !hasAssets && !hasCategories
    }

    /** Financial health score 0–100 and trend for first-fold card. */
    fun healthState(): HealthState {
        if (hasNoTransactionData()) return HealthState(0, "neutral", "Add data to see your score")
        val income = _uiState.value.kpis?.income_amount ?: 0.0
        val expenses = _uiState.value.kpis?.total_debits_amount ?: 0.0
        val goals = transformGoals()
        val surplus = income - expenses
        var score = 50
        if (income > 0) {
            val savingsRate = (surplus / income * 100).toInt().coerceIn(-100, 100)
            score = (50 + savingsRate / 2).coerceIn(0, 100)
        }
        val atRisk = goalsAtRiskCount()
        if (atRisk > 0) score = (score - atRisk * 5).coerceIn(0, 100)
        val trend = when {
            surplus > 0 && atRisk == 0 -> "up"
            surplus < 0 || atRisk > 0 -> "down"
            else -> "neutral"
        }
        return HealthState(score, trend, "Based on cash, goals, and spending")
    }

    /** Current risk for first-fold card. */
    fun riskState(): RiskState {
        if (hasNoTransactionData()) return RiskState("Add your data", "Upload a statement to see risk and opportunities.")
        if (!isCashFlowPositive()) return RiskState("Spending ahead of income", "This month's outgo exceeds income. Review expenses.")
        val atRisk = goalsAtRiskCount()
        if (atRisk > 0) return RiskState("$atRisk goal${if (atRisk > 1) "s" else ""} at risk", "Consider topping up to stay on track.")
        return RiskState("You're on track", "Cash flow is positive and goals are in good shape.")
    }

    /** Next best action for CTA card. */
    fun nextAction(): NextAction {
        if (hasNoTransactionData()) return NextAction("upload", "Upload this week's statement", null)
        val insights = generateAiInsights()
        if (insights.size >= 2) return NextAction("insights", "Review ${insights.size} insights", null)
        val goals = transformGoals()
        if (goals.any { g -> g.targetAmount > 0 && (g.savedAmount / g.targetAmount) < 0.5 }) return NextAction("goal", "Top up a goal", null)
        return NextAction("forecast", "See your financial future", null)
    }

    fun checkBackend() {
        viewModelScope.launch {
            val healthResult = withContext(Dispatchers.IO) { BackendApi.healthCheck() }
            if (healthResult.isSuccess) {
                _uiState.update { it.copy(backendStatus = "Connected", backendError = null) }
            } else {
                _uiState.update {
                    it.copy(
                        backendStatus = null,
                        backendError = healthResult.exceptionOrNull()?.message ?: "Backend unreachable"
                    )
                }
            }
        }
    }

    fun refresh() {
        AnalyticsHelper.logEvent("refresh")
        loadDashboard()
        checkBackend()
    }
}
