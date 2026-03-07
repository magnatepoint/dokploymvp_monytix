package com.example.monytix.budgetpilot

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monytix.AppDestinations
import com.example.monytix.analytics.AnalyticsHelper
import com.example.monytix.data.BudgetRecommendation
import com.example.monytix.data.BudgetStateResponse
import com.example.monytix.data.CommittedBudget
import com.example.monytix.data.BudgetVariance
import com.example.monytix.ui.MonytixSpinner
import com.example.monytix.ui.theme.AccentPrimary
import com.example.monytix.ui.theme.ChartBlue
import com.example.monytix.ui.theme.ChartGreen
import com.example.monytix.ui.theme.ChartOrange
import com.example.monytix.ui.theme.GlassCard

private val ScreenPaddingH = 20.dp
private val ScreenPaddingTop = 12.dp
private val SectionSpacing = 20.dp
private val CardSpacing = 12.dp
private val CardPadding = 16.dp
private val CardRadius = 20.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPilotScreen(
    viewModel: BudgetPilotViewModel = viewModel(),
    onNavigateTo: (AppDestinations) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(Unit) { AnalyticsHelper.logScreenView("budget_pilot") }
    var showAddBudget by remember { mutableStateOf(false) }
    var showPlanPreview by remember { mutableStateOf<BudgetRecommendation?>(null) }
    var dismissedSuggestion by remember { mutableStateOf(false) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        BudgetUpdateCache.consume()?.let { viewModel.refresh() }
    }

    val isZeroState = !hasRealData(uiState.variance, uiState.budgetState)

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = {
                    Column {
                        Text(
                            "BudgetPilot",
                            color = colorScheme.onBackground,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 22.sp
                            )
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { monthDropdownExpanded = true }
                        ) {
                            Text(
                                formatMonthLabel(uiState.selectedMonth),
                                style = MaterialTheme.typography.labelSmall,
                                fontSize = 12.sp,
                                color = colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                            Icon(
                                Icons.Default.ArrowDropDown,
                                contentDescription = "Select month",
                                modifier = Modifier.size(18.dp),
                                tint = colorScheme.onBackground.copy(alpha = 0.7f)
                            )
                        }
                        DropdownMenu(
                            expanded = monthDropdownExpanded,
                            onDismissRequest = { monthDropdownExpanded = false }
                        ) {
                            budgetMonthOptions().forEach { monthValue ->
                                DropdownMenuItem(
                                    text = { Text(formatMonthLabel(monthValue)) },
                                    onClick = {
                                        viewModel.setMonth(monthValue)
                                        monthDropdownExpanded = false
                                    }
                                )
                            }
                        }
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = {
                        dismissedSuggestion = false
                        viewModel.refresh()
                    }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colorScheme.onBackground)
                    }
                }
            )
        },
        containerColor = colorScheme.background
        // No FAB - primary actions are inline
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoadingState,
            onRefresh = { viewModel.refresh() }
        ) {
            if (uiState.isLoadingState && isZeroState) {
                Box(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentAlignment = Alignment.Center
                ) {
                    MonytixSpinner()
                }
            } else if (isZeroState) {
                ZeroStateCard(
                    modifier = modifier.padding(innerPadding),
                    onAddTransaction = { onNavigateTo(AppDestinations.DATA) }
                )
            } else {
                DataStateContent(
                    modifier = modifier.padding(innerPadding),
                    uiState = uiState,
                    viewModel = viewModel,
                    dismissedSuggestion = dismissedSuggestion,
                    onDismissSuggestion = { dismissedSuggestion = true },
                    onShowAddBudget = { showAddBudget = true },
                    onShowPlanPreview = { showPlanPreview = it }
                )
            }
        }
    }

    showPlanPreview?.let { plan ->
        PlanPreviewBottomSheet(
            plan = plan,
            sampleIncome = 50_000.0,
            onDismiss = { showPlanPreview = null },
            onSelect = {
                AnalyticsHelper.logEvent("plan_selected", mapOf("plan_code" to plan.plan_code))
                viewModel.commitBudget(plan.plan_code)
                showPlanPreview = null
            },
            isCommitting = uiState.isCommitting
        )
    }
    if (showAddBudget) {
        AddBudgetDialog(
            plans = uiState.recommendations.ifEmpty { defaultBudgetPlans() },
            committedPlanCode = uiState.committedBudget?.plan_code,
            isCommitting = uiState.isCommitting,
            onDismiss = { showAddBudget = false },
            onCommit = { planCode ->
                AnalyticsHelper.logEvent("plan_selected", mapOf("plan_code" to planCode))
                Log.d("BudgetPilot", "AddBudgetDialog onCommit: planCode=$planCode")
                viewModel.commitBudget(planCode)
                showAddBudget = false
            }
        )
    }
}

private fun hasRealData(variance: BudgetVariance?, budgetState: BudgetStateResponse?): Boolean {
    val income = variance?.income_amt ?: budgetState?.income_amt ?: 0.0
    val spend = variance?.let { it.needs_amt + it.wants_amt }
        ?: budgetState?.actual?.let { it.needs_amt + it.wants_amt + it.savings_amt } ?: 0.0
    return income >= 100 || spend >= 100
}

/** Returns YYYY-MM for the last 24 months (newest first) for the month selector. */
private fun budgetMonthOptions(): List<String> {
    val now = java.time.LocalDate.now()
    return (0 until 24).map { i ->
        val d = now.minusMonths(i.toLong())
        d.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
    }
}

private fun formatMonthLabel(month: String): String {
    if (month.isBlank()) return ""
    return try {
        val parts = month.split("-")
        if (parts.size >= 2) {
            val months = listOf("", "Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
            val m = parts[1].toIntOrNull()?.coerceIn(1, 12) ?: 0
            val y = parts[0].take(4)
            if (m > 0) "${months[m]} $y" else month
        } else month
    } catch (_: Exception) { month }
}

private fun formatCurrency(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    val formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(abs.toLong())
    return if (amount < 0) "-₹$formatted" else "₹$formatted"
}

// ─── Zero State: Single focused card, "waiting to activate" ───

@Composable
private fun ZeroStateCard(
    modifier: Modifier,
    onAddTransaction: () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = ScreenPaddingH),
            colors = CardDefaults.cardColors(containerColor = AccentPrimary.copy(alpha = 0.12f)),
            shape = RoundedCornerShape(CardRadius)
        ) {
            Column(
                modifier = Modifier.padding(CardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    "Your financial engine is ready.",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "BudgetPilot uses your SpendSense data automatically—no extra permission. No budget data for this month yet. Open SpendSense to check or add transactions, or set a budget plan from the menu.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onAddTransaction,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text("Open SpendSense")
                }
            }
        }
    }
}

@Composable
private fun PlanPreviewBottomSheet(
    plan: BudgetRecommendation,
    sampleIncome: Double,
    onDismiss: () -> Unit,
    onSelect: () -> Unit,
    isCommitting: Boolean
) {
    val needsPct = (plan.needs_budget_pct * 100).toInt().takeIf { it > 0 } ?: 55
    val wantsPct = (plan.wants_budget_pct * 100).toInt().takeIf { it > 0 } ?: 20
    val savingsPct = (plan.savings_budget_pct * 100).toInt().takeIf { it > 0 } ?: 25
    val needsAmt = sampleIncome * needsPct / 100
    val wantsAmt = sampleIncome * wantsPct / 100
    val savingsAmt = sampleIncome * savingsPct / 100

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(20.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "What this would look like with ${formatCurrency(sampleIncome)} income",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    plan.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(16.dp))
                Text("Needs: ${formatCurrency(needsAmt)} ($needsPct%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                Text("Wants: ${formatCurrency(wantsAmt)} ($wantsPct%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                Text("Savings: ${formatCurrency(savingsAmt)} ($savingsPct%)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(24.dp))
                Button(
                    onClick = onSelect,
                    enabled = !isCommitting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    if (isCommitting) {
                        MonytixSpinner(size = 16.dp, stroke = 2.dp)
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(if (isCommitting) "Activating…" else "Start this plan")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}

// ─── Data State: Active financial decision engine ───

@Composable
private fun DataStateContent(
    modifier: Modifier,
    uiState: BudgetPilotUiState,
    viewModel: BudgetPilotViewModel,
    dismissedSuggestion: Boolean,
    onDismissSuggestion: () -> Unit,
    onShowAddBudget: () -> Unit,
    onShowPlanPreview: (BudgetRecommendation) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = ScreenPaddingTop, start = ScreenPaddingH, end = ScreenPaddingH),
        verticalArrangement = Arrangement.spacedBy(CardSpacing)
    ) {
        item {
            AutopilotStatusCard(
                budgetState = uiState.budgetState,
                variance = uiState.variance,
                autopilotStatus = uiState.autopilotStatus,
                lastUpdatedAt = uiState.lastUpdatedAt
            )
        }
        item {
            RequiredAllocationCard(
                goalRequiredSavingsRate = uiState.goalRequiredSavingsRate,
                actualSavingsRate = uiState.budgetState?.actual?.let { a ->
                    val inc = uiState.budgetState?.income_amt ?: 0.0
                    if (inc > 0) (a.savings_amt / inc) * 100 else 0.0
                } ?: 0.0,
                goalsProgressList = uiState.goalsProgressList,
                hasGoals = uiState.goalsProgressList.isNotEmpty() || uiState.goalRequiredSavingsRate > 0
            )
        }
        if (uiState.autopilotSuggestion != null && hasRealData(uiState.variance, uiState.budgetState) && !dismissedSuggestion) {
            item {
                OptimizationSuggestionCard(
                    suggestion = uiState.autopilotSuggestion!!,
                    variance = uiState.variance,
                    isApplying = uiState.isApplyingAdjustment,
                    onApply = { viewModel.applyAdjustment(it.shiftFrom, it.shiftTo, it.pct) },
                    onDismiss = onDismissSuggestion
                )
            }
        }
        item {
            AdaptivePlanCard(
                committedBudget = uiState.committedBudget,
                budgetState = uiState.budgetState,
                variance = uiState.variance,
                adaptivePlanReason = uiState.adaptivePlanReason,
                recommendations = uiState.recommendations,
                selectedMonth = uiState.selectedMonth,
                lastUpdatedAt = uiState.lastUpdatedAt
            )
        }
        item {
            Text(
                "Explore Standard Frameworks",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                defaultBudgetPlans().take(3).forEach { plan ->
                    FrameworkChip(
                        plan = plan,
                        isCommitted = uiState.committedBudget?.plan_code == plan.plan_code,
                        onClick = { onShowPlanPreview(plan) }
                    )
                }
            }
        }
        if (defaultBudgetPlans().size > 3) {
            item {
                TextButton(onClick = onShowAddBudget, modifier = Modifier.fillMaxWidth()) {
                    Text("See all plans", color = AccentPrimary)
                }
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun AnimatedAllocationBar(
    needsPct: Float,
    wantsPct: Float,
    savingsPct: Float
) {
    val animNeeds by animateFloatAsState(needsPct, animationSpec = tween(500), label = "needs")
    val animWants by animateFloatAsState(wantsPct, animationSpec = tween(500), label = "wants")
    val animSavings by animateFloatAsState(savingsPct, animationSpec = tween(500), label = "savings")
    val total = (animNeeds + animWants + animSavings).coerceAtLeast(0.001f)
    val n = (animNeeds / total).coerceIn(0f, 1f)
    val w = (animWants / total).coerceIn(0f, 1f)
    val s = (animSavings / total).coerceIn(0f, 1f)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp)),
        horizontalArrangement = Arrangement.Start
    ) {
        if (n > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(n)
                    .background(ChartBlue, RoundedCornerShape(10.dp))
            )
        }
        if (w > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(w)
                    .background(ChartOrange, RoundedCornerShape(10.dp))
            )
        }
        if (s > 0.005f) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(s)
                    .background(ChartGreen, RoundedCornerShape(10.dp))
            )
        }
    }
}

@Composable
private fun AutopilotStatusCard(
    budgetState: BudgetStateResponse?,
    variance: BudgetVariance?,
    autopilotStatus: String,
    lastUpdatedAt: String?
) {
    val income = variance?.income_amt ?: budgetState?.income_amt ?: 0.0
    val actual = budgetState?.actual
    val spend = variance?.let { it.needs_amt + it.wants_amt }
        ?: actual?.let { it.needs_amt + it.wants_amt } ?: 0.0
    val savings = variance?.assets_amt ?: actual?.savings_amt ?: 0.0
    val savingsPct = if (income > 0) (savings / income) * 100 else 0.0
    val needsPct = actual?.needs_pct ?: 0.0
    val wantsPct = actual?.wants_pct ?: 0.0
    val hasData = income >= 100 || spend >= 100 || savings >= 100

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AccentPrimary.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(CardRadius)
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Smart Budget Engine",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    "Status: $autopilotStatus",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Your allocation is dynamically adjusted based on goals and spending.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            if (hasData) {
                Spacer(Modifier.height(16.dp))
                Text(
                    "Income: ${formatCurrency(income)} • Spend: ${formatCurrency(spend)} • Savings: ${formatCurrency(savings)} (${savingsPct.toInt()}%)",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))
                AnimatedAllocationBar(needsPct = (needsPct / 100).toFloat(), wantsPct = (wantsPct / 100).toFloat(), savingsPct = (savingsPct / 100).toFloat())
                Spacer(Modifier.height(8.dp))
                Text(
                    "Needs: ${needsPct.toInt()}% | Wants: ${wantsPct.toInt()}% | Savings: ${savingsPct.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            } else {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Status: Waiting for data",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (lastUpdatedAt != null && lastUpdatedAt.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Last recalculated: ${formatLastUpdated(lastUpdatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
        }
    }
}

@Composable
private fun RequiredAllocationCard(
    goalRequiredSavingsRate: Double,
    actualSavingsRate: Double,
    goalsProgressList: List<GoalProgressForBudget>,
    hasGoals: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(CardRadius)
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            if (hasGoals) {
                val gap = actualSavingsRate - goalRequiredSavingsRate
                Text(
                    "Required Savings Rate: ${goalRequiredSavingsRate.toInt()}%",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "You're currently saving: ${actualSavingsRate.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Text(
                    "Gap: ${if (gap >= 0) "+" else ""}${gap.toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (gap >= 0) ChartGreen else ChartOrange
                )
                if (goalsProgressList.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    goalsProgressList.forEach { g ->
                        Row(
                            modifier = Modifier.padding(vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "${g.goal_name}: ${formatCurrency(g.monthly_required)}/month",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                        }
                    }
                }
            } else {
                Text(
                    "Set goals in GoalTracker to see required allocation",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun OptimizationSuggestionCard(
    suggestion: AutopilotSuggestion,
    variance: BudgetVariance?,
    isApplying: Boolean,
    onApply: (AutopilotSuggestion) -> Unit,
    onDismiss: () -> Unit
) {
    val income = variance?.income_amt ?: 0.0
    val amount = if (income > 0) (income * suggestion.pct / 100).toLong().coerceAtLeast(100) else 0
    val currentWants = (variance?.wants_amt ?: 0.0) / income.coerceAtLeast(1.0) * 100
    val currentSavings = (variance?.assets_amt ?: 0.0) / income.coerceAtLeast(1.0) * 100
    val targetWants = (currentWants - suggestion.pct).coerceAtLeast(0.0)
    val targetSavings = currentSavings + suggestion.pct.toFloat()
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = ChartOrange.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(CardRadius)
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(
                "Suggested Adjustment",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "To stay on track, reduce ${suggestion.shiftFrom.replaceFirstChar { it.uppercase() }} by ${formatCurrency(amount.toDouble())} and increase ${suggestion.shiftTo.replaceFirstChar { it.uppercase() }} by ${formatCurrency(amount.toDouble())}.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
            if (income > 0 && suggestion.shiftFrom == "wants" && suggestion.shiftTo == "savings") {
                Spacer(Modifier.height(12.dp))
                Text("Current", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                AnimatedAllocationBar(
                    needsPct = ((variance?.needs_amt ?: 0.0) / income).toFloat(),
                    wantsPct = (currentWants / 100).toFloat(),
                    savingsPct = (currentSavings / 100).toFloat()
                )
                Spacer(Modifier.height(4.dp))
                Text("Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                AnimatedAllocationBar(
                    needsPct = ((variance?.needs_amt ?: 0.0) / income).toFloat(),
                    wantsPct = (targetWants / 100).toFloat(),
                    savingsPct = (targetSavings / 100).toFloat()
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onApply(suggestion) },
                    enabled = !isApplying,
                    colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    if (isApplying) {
                        MonytixSpinner(size = 14.dp, stroke = 2.dp)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (isApplying) "Applying…" else "Apply")
                }
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
private fun AdaptivePlanCard(
    committedBudget: CommittedBudget?,
    budgetState: BudgetStateResponse?,
    variance: BudgetVariance?,
    adaptivePlanReason: List<String>,
    recommendations: List<BudgetRecommendation>,
    selectedMonth: String,
    lastUpdatedAt: String?
) {
    val income = variance?.income_amt ?: budgetState?.income_amt ?: 0.0
    val actual = budgetState?.actual
    val hasData = hasRealData(variance, budgetState)
    val (needsPct, wantsPct, savingsPct) = when {
        committedBudget != null -> Triple(
            (committedBudget.alloc_needs_pct * 100).toInt(),
            (committedBudget.alloc_wants_pct * 100).toInt(),
            (committedBudget.alloc_assets_pct * 100).toInt()
        )
        actual != null && hasData -> Triple(actual.needs_pct.toInt(), actual.wants_pct.toInt(), actual.savings_pct.toInt())
        else -> {
            val top = recommendations.maxByOrNull { it.score } ?: defaultBudgetPlans().first()
            Triple(
                (top.needs_budget_pct * 100).toInt().takeIf { it > 0 } ?: 55,
                (top.wants_budget_pct * 100).toInt().takeIf { it > 0 } ?: 20,
                (top.savings_budget_pct * 100).toInt().takeIf { it > 0 } ?: 25
            )
        }
    }
    val monthLabel = formatMonthLabel(selectedMonth)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(CardRadius)
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(
                "Your Adaptive Plan (Generated for $monthLabel)",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "Needs: $needsPct% | Wants: $wantsPct% | Savings: $savingsPct%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
            if (adaptivePlanReason.isNotEmpty()) {
                Spacer(Modifier.height(8.dp))
                Text("Reason:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                adaptivePlanReason.forEach { reason ->
                    Text("• $reason", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                }
            } else if (!hasData) {
                Spacer(Modifier.height(8.dp))
                Text(
                    "Based on your goals and spending, we recommend the allocation above.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
            if (lastUpdatedAt != null && lastUpdatedAt.isNotBlank()) {
                Spacer(Modifier.height(4.dp))
                Text("Last updated: ${formatLastUpdated(lastUpdatedAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun FrameworkChip(
    plan: BudgetRecommendation,
    isCommitted: Boolean,
    onClick: () -> Unit
) {
    val needsPct = (plan.needs_budget_pct * 100).toInt().takeIf { it > 0 } ?: 55
    val wantsPct = (plan.wants_budget_pct * 100).toInt().takeIf { it > 0 } ?: 20
    val savingsPct = (plan.savings_budget_pct * 100).toInt().takeIf { it > 0 } ?: 25
    Card(
        modifier = Modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = if (isCommitted) AccentPrimary.copy(alpha = 0.2f) else GlassCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                plan.name,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                "$needsPct/$wantsPct/$savingsPct",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
        }
    }
}

private fun formatLastUpdated(iso: String?): String {
    if (iso == null) return ""
    return try {
        val t = java.time.Instant.parse(iso)
        val now = java.time.Instant.now()
        val secs = java.time.Duration.between(t, now).seconds
        when {
            secs < 60 -> "${secs} seconds ago"
            secs < 3600 -> "${secs / 60}m ago"
            else -> "${secs / 3600}h ago"
        }
    } catch (_: Exception) { "" }
}

private fun defaultBudgetPlans(): List<BudgetRecommendation> = listOf(
    BudgetRecommendation(plan_code = "LEAN_BASICS", name = "Lean Basics", needs_budget_pct = 0.55, wants_budget_pct = 0.20, savings_budget_pct = 0.25, recommendation_reason = "Best for low-income stability", score = 0.5),
    BudgetRecommendation(plan_code = "BAL_50_30_20", name = "Balanced 50/30/20", needs_budget_pct = 0.50, wants_budget_pct = 0.30, savings_budget_pct = 0.20, recommendation_reason = "Best for steady income", score = 0.5),
    BudgetRecommendation(plan_code = "EMERGENCY_FIRST", name = "Emergency Priority", needs_budget_pct = 0.50, wants_budget_pct = 0.20, savings_budget_pct = 0.30, recommendation_reason = "Boost savings", score = 0.5),
    BudgetRecommendation(plan_code = "DEBT_FIRST", name = "Debt First", needs_budget_pct = 0.55, wants_budget_pct = 0.20, savings_budget_pct = 0.25, recommendation_reason = "Aggressive debt payoff", score = 0.5),
    BudgetRecommendation(plan_code = "GOAL_PRIORITY", name = "Top 3 Goals", needs_budget_pct = 0.50, wants_budget_pct = 0.20, savings_budget_pct = 0.30, recommendation_reason = "Assets tilt to goals", score = 0.5)
)

@Composable
private fun AddBudgetDialog(
    plans: List<BudgetRecommendation>,
    committedPlanCode: String?,
    isCommitting: Boolean,
    onDismiss: () -> Unit,
    onCommit: (String) -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), shape = RoundedCornerShape(16.dp)) {
            Column(Modifier.padding(24.dp)) {
                Text("Add Budget", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(8.dp))
                Text("Choose a budget plan. Allocations are tailored to your goals.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Spacer(Modifier.height(16.dp))
                plans.forEach { rec ->
                    val isCommitted = committedPlanCode == rec.plan_code
                    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp), colors = CardDefaults.cardColors(containerColor = if (isCommitted) ChartGreen.copy(alpha = 0.15f) else GlassCard), shape = RoundedCornerShape(12.dp)) {
                        Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rec.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                if (rec.recommendation_reason.isNotBlank()) Text(rec.recommendation_reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                            }
                            if (isCommitted) Text("Committed", style = MaterialTheme.typography.labelMedium, color = ChartGreen)
                            else if (!isCommitting) Button(onClick = { onCommit(rec.plan_code) }, colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text("Commit") }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) }
            }
        }
    }
}
