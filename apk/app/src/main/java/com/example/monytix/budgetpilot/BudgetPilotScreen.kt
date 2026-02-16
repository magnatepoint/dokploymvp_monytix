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
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.example.monytix.data.BudgetRecommendation
import com.example.monytix.data.BudgetStateResponse
import com.example.monytix.data.CommittedBudget
import com.example.monytix.data.BudgetVariance
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
    var showAddBudget by remember { mutableStateOf(false) }
    var showPlanPreview by remember { mutableStateOf<BudgetRecommendation?>(null) }

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
                        Text(
                            formatMonthLabel(uiState.selectedMonth),
                            style = MaterialTheme.typography.labelSmall,
                            fontSize = 12.sp,
                            color = colorScheme.onBackground.copy(alpha = 0.7f)
                        )
                    }
                },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onBackground
                ),
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colorScheme.onBackground)
                    }
                }
            )
        },
        containerColor = colorScheme.background
        // No FAB - primary actions are inline
    ) { innerPadding ->
        if (uiState.isLoadingState && isZeroState) {
            Box(
                modifier = modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentPrimary)
            }
        } else if (isZeroState) {
            ZeroStateContent(
                modifier = modifier.padding(innerPadding),
                goalsCount = uiState.goalsCount,
                hasTransactions = hasRealData(uiState.variance, uiState.budgetState),
                hasBudget = uiState.committedBudget != null,
                plans = defaultBudgetPlans().take(2),
                onConnectAccounts = { onNavigateTo(AppDestinations.DATA) },
                onAddTransaction = { onNavigateTo(AppDestinations.DATA) },
                onPlanTap = { showPlanPreview = it },
                onSelectPlan = { viewModel.commitBudget(it); showPlanPreview = null }
            )
        } else {
            DataStateContent(
                modifier = modifier.padding(innerPadding),
                uiState = uiState,
                viewModel = viewModel,
                onShowAddBudget = { showAddBudget = true },
                onShowPlanPreview = { showPlanPreview = it }
            )
        }
    }

    showPlanPreview?.let { plan ->
        PlanPreviewBottomSheet(
            plan = plan,
            sampleIncome = 50_000.0,
            onDismiss = { showPlanPreview = null },
            onSelect = {
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

// ─── Zero State: "We're ready. Connect data and we'll start managing your money." ───

@Composable
private fun ZeroStateContent(
    modifier: Modifier,
    goalsCount: Int,
    hasTransactions: Boolean,
    hasBudget: Boolean,
    plans: List<BudgetRecommendation>,
    onConnectAccounts: () -> Unit,
    onAddTransaction: () -> Unit,
    onPlanTap: (BudgetRecommendation) -> Unit,
    onSelectPlan: (String) -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(top = ScreenPaddingTop, start = ScreenPaddingH, end = ScreenPaddingH),
        verticalArrangement = Arrangement.spacedBy(SectionSpacing)
    ) {
        // 1. Hero: Smart Budget Engine
        HeroSmartEngineBanner(
            onConnectAccounts = onConnectAccounts,
            onAddTransaction = onAddTransaction
        )

        // 2. Budget Setup Progress
        SetupProgressTracker(
            goalsCreated = goalsCount > 0,
            transactionsSynced = hasTransactions,
            budgetSelected = hasBudget
        )

        // 3. Plan Previews (2 only)
        Text(
            "Plan preview",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
        plans.forEach { plan ->
            PlanPreviewCard(
                plan = plan,
                sampleIncome = 50_000.0,
                onClick = { onPlanTap(plan) }
            )
        }

        // 4. Education block
        Text(
            "Recommendations unlock after 10 transactions.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
            modifier = Modifier.padding(vertical = 8.dp)
        )

        Spacer(Modifier.height(24.dp))
    }
}

@Composable
private fun HeroSmartEngineBanner(
    onConnectAccounts: () -> Unit,
    onAddTransaction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AccentPrimary.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(CardRadius)
    ) {
        Column(
            modifier = Modifier.padding(CardPadding)
        ) {
            Text(
                "Smart Budget Engine",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Add transactions in SpendSense to see your allocation and get personalized plan recommendations.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
            )
            Spacer(Modifier.height(20.dp))
            Button(
                onClick = onAddTransaction,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Add Transaction in SpendSense")
            }
        }
    }
}

@Composable
private fun SetupProgressTracker(
    goalsCreated: Boolean,
    transactionsSynced: Boolean,
    budgetSelected: Boolean
) {
    val completed = listOf(goalsCreated, transactionsSynced, budgetSelected).count { it }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(CardRadius)
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(
                "Budget Setup Progress",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(12.dp))
            ProgressRow("Goals Created", goalsCreated)
            Spacer(Modifier.height(6.dp))
            ProgressRow("Transactions Synced", transactionsSynced)
            Spacer(Modifier.height(6.dp))
            ProgressRow("Budget Plan Selected", budgetSelected)
            Spacer(Modifier.height(12.dp))
            Text(
                "Progress: $completed/3 Complete",
                style = MaterialTheme.typography.labelMedium,
                color = AccentPrimary,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ProgressRow(label: String, done: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            if (done) "✔" else "✖",
            style = MaterialTheme.typography.bodyMedium,
            color = if (done) ChartGreen else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (done) 0.9f else 0.6f)
        )
    }
}

@Composable
private fun PlanPreviewCard(
    plan: BudgetRecommendation,
    sampleIncome: Double,
    onClick: () -> Unit
) {
    val needsPct = (plan.needs_budget_pct * 100).toInt().takeIf { it > 0 } ?: 55
    val wantsPct = (plan.wants_budget_pct * 100).toInt().takeIf { it > 0 } ?: 20
    val savingsPct = (plan.savings_budget_pct * 100).toInt().takeIf { it > 0 } ?: 25
    val desc = when (plan.plan_code) {
        "LEAN_BASICS" -> "Best for low-income stability"
        "BAL_50_30_20" -> "Best for steady income"
        else -> plan.recommendation_reason.take(40)
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(CardRadius)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    plan.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Needs $needsPct% | Wants $wantsPct% | Savings $savingsPct%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Text(
                    desc,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
            Text("Preview →", style = MaterialTheme.typography.labelMedium, color = AccentPrimary)
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
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
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

// ─── Data State: Full budget tracking UI ───

@Composable
private fun DataStateContent(
    modifier: Modifier,
    uiState: BudgetPilotUiState,
    viewModel: BudgetPilotViewModel,
    onShowAddBudget: () -> Unit,
    onShowPlanPreview: (BudgetRecommendation) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(top = ScreenPaddingTop, start = ScreenPaddingH, end = ScreenPaddingH),
        verticalArrangement = Arrangement.spacedBy(CardSpacing)
    ) {
        if (uiState.committedBudget != null && uiState.deviation != null) {
            item {
                StatusHeroCard(
                    deviation = uiState.deviation!!,
                    variance = uiState.variance,
                    autopilotSuggestion = uiState.autopilotSuggestion,
                    isApplying = uiState.isApplyingAdjustment,
                    onApply = { s -> viewModel.applyAdjustment(s.shiftFrom, s.shiftTo, s.pct) }
                )
            }
        }
        item {
            MonthSnapshotCard(variance = uiState.variance, budgetState = uiState.budgetState, month = uiState.selectedMonth)
        }
        item {
            CommittedBudgetSection(
                committedBudget = uiState.committedBudget,
                variance = uiState.variance,
                budgetState = uiState.budgetState,
                recommendations = uiState.recommendations,
                lastUpdatedAt = uiState.lastUpdatedAt,
                isLoading = uiState.isLoadingCommitted,
                onRecalculate = { viewModel.recalculate() }
            )
        }
        item {
            NextBestActionCard(
                suggestion = uiState.autopilotSuggestion,
                variance = uiState.variance,
                committedBudget = uiState.committedBudget,
                isApplying = uiState.isApplyingAdjustment,
                hasRealData = hasRealData(uiState.variance, uiState.budgetState),
                onApply = { viewModel.applyAdjustment(it.shiftFrom, it.shiftTo, it.pct) }
            )
        }
        val goalImpact = uiState.budgetState?.goal_impact?.takeIf { it.isNotEmpty() }
        if (goalImpact != null) {
            item {
                GoalsImpactStrip(goalImpact = goalImpact)
            }
        }
        item {
            Text(
                "Explore other plans",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        if (uiState.recommendations.isNotEmpty()) {
            items(uiState.recommendations.take(2)) { rec ->
                RecommendationCard(
                    recommendation = rec,
                    isCommitted = uiState.committedBudget?.plan_code == rec.plan_code,
                    isCommitting = uiState.isCommitting && uiState.committingPlanCode == rec.plan_code,
                    onPreview = { onShowPlanPreview(rec) },
                    onCommit = { viewModel.commitBudget(rec.plan_code) }
                )
            }
            if (uiState.recommendations.size > 2) {
                item {
                    TextButton(onClick = onShowAddBudget, modifier = Modifier.fillMaxWidth()) {
                        Text("See all (${uiState.recommendations.size} plans)", color = AccentPrimary)
                    }
                }
            }
        } else {
            item {
                Text(
                    "Recommendations unlock after 10 transactions.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            }
        }
        item { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun StatusHeroCard(
    deviation: BudgetDeviation,
    variance: BudgetVariance?,
    autopilotSuggestion: AutopilotSuggestion?,
    isApplying: Boolean,
    onApply: (AutopilotSuggestion) -> Unit
) {
    val hasIssue = deviation.needs > 5 || deviation.wants > 5 || deviation.savings < -5
    val statusColor = when {
        hasIssue && deviation.savings < -10 -> com.example.monytix.ui.theme.ChartRed
        hasIssue -> ChartOrange
        else -> AccentPrimary
    }
    val statusLabel = when {
        hasIssue && deviation.savings < -10 -> "At Risk"
        hasIssue -> "Slightly Off"
        else -> "On Track"
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(CardRadius)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(CardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(statusLabel, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text(if (hasIssue) "Budget needs attention" else "You're staying within plan", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
            if (hasIssue && autopilotSuggestion != null) {
                Button(onClick = { onApply(autopilotSuggestion) }, enabled = !isApplying, colors = ButtonDefaults.buttonColors(containerColor = statusColor, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                    if (isApplying) { CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(6.dp)) }
                    Text(if (isApplying) "Applying…" else "Fix it", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun MonthSnapshotCard(variance: BudgetVariance?, budgetState: BudgetStateResponse?, month: String) {
    val income = variance?.income_amt ?: budgetState?.income_amt ?: 0.0
    val spend = variance?.let { it.needs_amt + it.wants_amt }
        ?: budgetState?.actual?.let { it.needs_amt + it.wants_amt } ?: 0.0
    val savings = variance?.assets_amt ?: budgetState?.actual?.savings_amt ?: 0.0
    val hasData = hasRealData(variance, budgetState)
    val (dayOfMonth, daysInMonth) = try {
        val now = java.time.LocalDate.now()
        val cur = if (month == "${now.year}-${now.monthValue.toString().padStart(2, '0')}") now.dayOfMonth else 15
        cur to java.time.YearMonth.parse(month).lengthOfMonth()
    } catch (_: Exception) { 15 to 30 }
    val progress = dayOfMonth.toFloat() / daysInMonth

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GlassCard), shape = RoundedCornerShape(CardRadius)) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text("Month Snapshot", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            if (hasData) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text("Income ${formatCurrency(income)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Spend ${formatCurrency(spend)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Savings ${formatCurrency(savings)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            } else {
                Text("Add transactions in SpendSense to see your snapshot.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f).height(8.dp).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(10.dp))) {
                    Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(progress.coerceIn(0f, 1f)).background(AccentPrimary.copy(alpha = 0.6f), RoundedCornerShape(10.dp)))
                }
                Spacer(Modifier.width(8.dp))
                Text("$dayOfMonth/$daysInMonth days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun CommittedBudgetSection(
    committedBudget: CommittedBudget?,
    variance: BudgetVariance?,
    budgetState: BudgetStateResponse?,
    recommendations: List<BudgetRecommendation>,
    lastUpdatedAt: String?,
    isLoading: Boolean,
    onRecalculate: () -> Unit
) {
    if (isLoading) {
        Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = AccentPrimary)
        }
    } else if (committedBudget != null) {
        Column {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("Your Plan", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                Text("Last updated: ${formatLastUpdated(lastUpdatedAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(8.dp))
            CommittedBudgetCard(committedBudget = committedBudget, variance = variance, budgetState = budgetState)
        }
    } else {
        CurrentAllocationCard(variance = variance, budgetState = budgetState, recommendations = recommendations)
    }
}

@Composable
private fun CurrentAllocationCard(
    variance: BudgetVariance?,
    budgetState: BudgetStateResponse?,
    recommendations: List<BudgetRecommendation> = emptyList()
) {
    val actual = budgetState?.actual
    val income = variance?.income_amt ?: budgetState?.income_amt ?: 0.0
    val hasData = hasRealData(variance, budgetState)
    val closestPlan = recommendations.maxByOrNull { it.score } ?: defaultBudgetPlans().first()

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GlassCard), shape = RoundedCornerShape(CardRadius)) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text("Your Current Allocation", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
            Spacer(Modifier.height(12.dp))
            if (hasData && actual != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("Needs: ${actual.needs_pct.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = ChartBlue)
                    Text("Wants: ${actual.wants_pct.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = ChartOrange)
                    Text("Savings: ${actual.savings_pct.toInt()}%", style = MaterialTheme.typography.bodyMedium, color = ChartGreen)
                }
                Spacer(Modifier.height(8.dp))
                Text("Income ${formatCurrency(income)} • Spend ${formatCurrency(actual.needs_amt + actual.wants_amt)} • Savings ${formatCurrency(actual.savings_amt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Spacer(Modifier.height(8.dp))
                Text("Based on your behavior, you're closest to ${closestPlan.name}.", style = MaterialTheme.typography.labelMedium, color = AccentPrimary)
                Text("Choose a plan below to optimize.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            } else {
                Text("Add transactions in SpendSense to see your allocation.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Spacer(Modifier.height(4.dp))
                Text("Choose a plan below to start tracking.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
private fun CommittedBudgetCard(
    committedBudget: CommittedBudget,
    variance: BudgetVariance?,
    budgetState: BudgetStateResponse?
) {
    val income = variance?.income_amt ?: budgetState?.income_amt ?: 0.0
    val hasRealData = hasRealData(variance, budgetState)
    val targetNeeds = income * committedBudget.alloc_needs_pct
    val targetWants = income * committedBudget.alloc_wants_pct
    val targetSavings = income * committedBudget.alloc_assets_pct

    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GlassCard), shape = RoundedCornerShape(CardRadius)) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(planCodeToName(committedBudget.plan_code), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Text("✓", style = MaterialTheme.typography.titleMedium, color = AccentPrimary)
            }
            Spacer(Modifier.height(12.dp))
            if (hasRealData && variance != null) {
                Text("Target: Needs ${formatCurrency(targetNeeds)} • Wants ${formatCurrency(targetWants)} • Savings ${formatCurrency(targetSavings)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                Spacer(Modifier.height(8.dp))
                Text("Actual: Needs ${formatCurrency(variance.needs_amt)} • Wants ${formatCurrency(variance.wants_amt)} • Savings ${formatCurrency(variance.assets_amt)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                val shortfall = -variance.variance_assets_amt
                if (shortfall > 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("⚠ At this rate you'll miss savings target by ${formatCurrency(shortfall)}", style = MaterialTheme.typography.labelSmall, color = ChartOrange)
                }
            } else {
                Text("Add transactions to see your progress.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        }
    }
}

private fun planCodeToName(code: String): String = when (code) {
    "BAL_50_30_20" -> "Balanced 50/30/20"
    "EMERGENCY_FIRST" -> "Emergency Priority"
    "DEBT_FIRST" -> "Debt First"
    "GOAL_PRIORITY" -> "Top 3 Goals"
    "LEAN_BASICS" -> "Lean Basics"
    else -> code
}

private fun formatLastUpdated(iso: String?): String {
    if (iso == null) return ""
    return try {
        val t = java.time.Instant.parse(iso)
        val now = java.time.Instant.now()
        val mins = java.time.Duration.between(t, now).toMinutes()
        when { mins < 1 -> "Just now"; mins < 60 -> "${mins}m ago"; else -> "${mins / 60}h ago" }
    } catch (_: Exception) { "" }
}

@Composable
private fun NextBestActionCard(
    suggestion: AutopilotSuggestion?,
    variance: BudgetVariance?,
    committedBudget: CommittedBudget?,
    isApplying: Boolean,
    hasRealData: Boolean,
    onApply: (AutopilotSuggestion) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GlassCard), shape = RoundedCornerShape(CardRadius)) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text("Next Best Actions", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(8.dp))
            if (suggestion != null && hasRealData && committedBudget != null) {
                val income = variance?.income_amt ?: 0.0
                val amount = (income * suggestion.pct / 100).toLong().coerceAtLeast(100)
                Text("Reduce ${suggestion.shiftFrom.replaceFirstChar { it.uppercase() }} by ${formatCurrency(amount.toDouble())} this week", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.height(12.dp))
                Button(onClick = { onApply(suggestion) }, enabled = !isApplying, colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                    if (isApplying) { CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary); Spacer(Modifier.width(6.dp)) }
                    Text(if (isApplying) "Applying…" else "Apply")
                }
            } else {
                Text("Add transactions in SpendSense to get personalized suggestions.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun GoalsImpactStrip(goalImpact: List<com.example.monytix.data.BudgetGoalImpact>) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = GlassCard), shape = RoundedCornerShape(CardRadius)) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text("Goal Impact", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
            Spacer(Modifier.height(12.dp))
            goalImpact.forEach { g ->
                val isAtRisk = g.status == "at_risk"
                val detail = when {
                    isAtRisk && g.shortfall != null && g.shortfall > 0 -> "Savings shortfall ${formatCurrency(g.shortfall)}"
                    !isAtRisk && g.planned_amount > 0 -> "${formatCurrency(g.planned_amount)} planned"
                    else -> "On track"
                }
                Row(modifier = Modifier.padding(vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(if (isAtRisk) "⚠" else "✓", style = MaterialTheme.typography.labelSmall, color = if (isAtRisk) ChartOrange else AccentPrimary)
                    Spacer(Modifier.width(8.dp))
                    Text("${g.goal_name}: $detail", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
                }
            }
        }
    }
}

@Composable
private fun RecommendationCard(
    recommendation: BudgetRecommendation,
    isCommitted: Boolean,
    isCommitting: Boolean,
    onPreview: () -> Unit,
    onCommit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = if (isCommitted) GlassCard.copy(alpha = 0.5f) else GlassCard), shape = RoundedCornerShape(CardRadius)) {
        Row(modifier = Modifier.fillMaxWidth().padding(CardPadding), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(recommendation.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text("Fit: ${(recommendation.score * 100).toInt()}% • Based on last 60 days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onPreview) { Text("Preview") }
                if (!isCommitted && !isCommitting) {
                    Button(onClick = onCommit, colors = ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)) { Text("Switch") }
                }
            }
        }
    }
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
