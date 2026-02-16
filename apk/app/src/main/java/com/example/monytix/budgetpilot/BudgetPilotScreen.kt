package com.example.monytix.budgetpilot

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monytix.data.BudgetGoalImpact
import com.example.monytix.data.BudgetRecommendation
import com.example.monytix.data.BudgetStateResponse
import com.example.monytix.data.CommittedBudget
import com.example.monytix.data.BudgetVariance
import com.example.monytix.ui.theme.AccentPrimary
import com.example.monytix.ui.theme.BannerPurple
import com.example.monytix.ui.theme.ChartRed
import com.example.monytix.ui.theme.ChartBlue
import com.example.monytix.ui.theme.ChartGreen
import com.example.monytix.ui.theme.ChartOrange
import com.example.monytix.ui.theme.GlassCard

// Fintech layout constants
private val ScreenPaddingH = 20.dp
private val ScreenPaddingTop = 12.dp
private val SectionSpacing = 20.dp
private val CardSpacing = 12.dp
private val CardPadding = 16.dp
private val CardRadius = 20.dp
private val ChipRadius = 14.dp
private val ProgressBarRadius = 10.dp
private val CardBorderWidth = 1.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPilotScreen(
    viewModel: BudgetPilotViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    var showAddBudget by remember { mutableStateOf(false) }
    var showPlanToAchieve by remember { mutableStateOf<BudgetRecommendation?>(null) }

    LaunchedEffect(Unit) {
        BudgetUpdateCache.consume()?.let { viewModel.refresh() }
    }

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
        containerColor = colorScheme.background,
        floatingActionButton = {
            androidx.compose.material3.ExtendedFloatingActionButton(
                onClick = { showAddBudget = true },
                containerColor = AccentPrimary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                icon = { Icon(Icons.Default.Add, contentDescription = null) },
                text = { Text("Add Budget", style = MaterialTheme.typography.labelLarge) }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = uiState.isLoadingState,
            onRefresh = { viewModel.refresh() }
        ) {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(top = ScreenPaddingTop, start = ScreenPaddingH, end = ScreenPaddingH),
            verticalArrangement = Arrangement.spacedBy(CardSpacing)
        ) {
            // 1. Hero Status Card (dominant)
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
            // 2. Month Snapshot Card
            item {
                MonthSnapshotCard(
                    variance = uiState.variance,
                    month = uiState.selectedMonth
                )
            }
            // Chips row under snapshot
            if (uiState.deviation != null && uiState.committedBudget != null && uiState.variance != null) {
                item {
                    StatusChipRow(
                        deviation = uiState.deviation!!,
                        variance = uiState.variance,
                        autopilotSuggestion = uiState.autopilotSuggestion,
                        isApplying = uiState.isApplyingAdjustment,
                        onApply = { s -> viewModel.applyAdjustment(s.shiftFrom, s.shiftTo, s.pct) }
                    )
                }
            }
            // 3. Plan Card (target vs actual)
            item {
                CommittedBudgetSection(
                    committedBudget = uiState.committedBudget,
                    variance = uiState.variance,
                    budgetState = uiState.budgetState,
                    lastUpdatedAt = uiState.lastUpdatedAt,
                    isLoading = uiState.isLoadingCommitted,
                    isRecalculating = uiState.isRecalculating,
                    onRecalculate = { viewModel.recalculate() }
                )
            }
            // 4. Action Engine Card
            item {
                NextBestActionCard(
                    suggestion = uiState.autopilotSuggestion,
                    variance = uiState.variance,
                    committedBudget = uiState.committedBudget,
                    isApplying = uiState.isApplyingAdjustment,
                    hasRealData = hasRealData(uiState.variance),
                    onApply = { viewModel.applyAdjustment(it.shiftFrom, it.shiftTo, it.pct) }
                )
            }
            // 5. Goals Impact Strip (below forecast/plan)
            val goalImpact = uiState.budgetState?.goal_impact?.takeIf { it.isNotEmpty() }
            if (goalImpact != null) {
                item {
                    GoalsImpactStrip(goalImpact = goalImpact)
                }
            }
            // 6. Explore Alternatives (2 cards + See all)
            item {
                Text(
                    text = "Explore other plans",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            if (uiState.isLoadingRecommendations) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            } else if (uiState.error != null) {
                item {
                    ErrorState(
                        message = uiState.error!!,
                        onRetry = { viewModel.refresh() }
                    )
                }
            } else if (uiState.recommendations.isEmpty()) {
                item {
                    EmptyRecommendationsState()
                }
            } else {
                items(uiState.recommendations.take(2)) { rec ->
                    RecommendationCard(
                        recommendation = rec,
                        isCommitted = uiState.committedBudget?.plan_code == rec.plan_code,
                        isCommitting = uiState.isCommitting && uiState.committingPlanCode == rec.plan_code,
                        variance = uiState.variance,
                        onPreview = { showPlanToAchieve = rec },
                        onCommit = { viewModel.commitBudget(rec.plan_code) }
                    )
                }
                if (uiState.recommendations.size > 2) {
                    item {
                        TextButton(
                            onClick = { showAddBudget = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("See all (${uiState.recommendations.size} plans)", color = AccentPrimary)
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
        }
    }

    showPlanToAchieve?.let { plan ->
        PlanToAchieveBottomSheet(
            plan = plan,
            variance = uiState.variance,
            autopilotSuggestion = uiState.autopilotSuggestion,
            onDismiss = { showPlanToAchieve = null },
            onCommit = {
                viewModel.commitBudget(plan.plan_code)
                showPlanToAchieve = null
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

private fun hasRealData(variance: BudgetVariance?): Boolean {
    val income = variance?.income_amt ?: 0.0
    val spend = (variance?.needs_amt ?: 0.0) + (variance?.wants_amt ?: 0.0)
    return income >= 100 || spend >= 100
}

@Composable
private fun GoalsImpactStrip(
    goalImpact: List<BudgetGoalImpact>,
    onGoalTap: (String) -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(CardRadius),
        border = BorderStroke(CardBorderWidth, AccentPrimary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(
                "Goal Impact",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(12.dp))
            goalImpact.forEach { g ->
                val isAtRisk = g.status == "at_risk"
                val statusText = when (g.status) {
                    "at_risk" -> "At risk"
                    else -> "On track"
                }
                val detail = when {
                    isAtRisk && g.shortfall != null && g.shortfall > 0 ->
                        "Savings shortfall ${formatCurrency(g.shortfall)}"
                    !isAtRisk && g.planned_amount > 0 ->
                        "${formatCurrency(g.planned_amount)} planned this week"
                    else -> "On track"
                }
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(ChipRadius),
                    color = if (isAtRisk) ChartOrange.copy(alpha = 0.12f) else GlassCard,
                    onClick = { onGoalTap(g.goal_id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            if (isAtRisk) "⚠" else "✓",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isAtRisk) ChartOrange else AccentPrimary
                        )
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                g.goal_name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                "$statusText ($detail)",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                            )
                        }
                    }
                }
                if (g != goalImpact.last()) Spacer(Modifier.height(8.dp))
            }
        }
    }
}

@Composable
private fun MonthSnapshotCard(
    variance: BudgetVariance?,
    month: String,
    onAddTransactions: () -> Unit = {}
) {
    val income = variance?.income_amt ?: 0.0
    val spend = (variance?.needs_amt ?: 0.0) + (variance?.wants_amt ?: 0.0)
    val savings = variance?.assets_amt ?: 0.0
    val savingsPct = if (income > 0) (savings / income * 100) else 0.0
    val (dayOfMonth, daysInMonth) = try {
        val now = java.time.LocalDate.now()
        val monthStart = java.time.YearMonth.parse(month)
        val cur = if (month == "${now.year}-${now.monthValue.toString().padStart(2, '0')}") now.dayOfMonth else 15
        cur to monthStart.lengthOfMonth()
    } catch (_: Exception) { 15 to 30 }
    val progress = dayOfMonth.toFloat() / daysInMonth
    val hasData = hasRealData(variance)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(CardRadius),
        border = BorderStroke(CardBorderWidth, AccentPrimary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(
                "Month Snapshot",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            if (!hasData) {
                Text(
                    "No transactions yet this month",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Connect accounts or add transactions in SpendSense to see your budget.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(12.dp))
                TextButton(onClick = onAddTransactions) {
                    Text("Add transactions", color = AccentPrimary, style = MaterialTheme.typography.labelMedium)
                }
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("Income ${formatCurrency(income)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Spend ${formatCurrency(spend)}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
                    Text("Savings ${formatCurrency(savings)} (${savingsPct.toInt()}%)", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(8.dp)
                        .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(ProgressBarRadius))
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(progress.coerceIn(0f, 1f))
                            .background(AccentPrimary.copy(alpha = 0.6f), RoundedCornerShape(ProgressBarRadius))
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text("$dayOfMonth/$daysInMonth days", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        }
    }
}

@Composable
private fun StatusHeroCard(
    deviation: com.example.monytix.budgetpilot.BudgetDeviation,
    variance: BudgetVariance?,
    autopilotSuggestion: com.example.monytix.budgetpilot.AutopilotSuggestion?,
    isApplying: Boolean,
    onApply: (com.example.monytix.budgetpilot.AutopilotSuggestion) -> Unit
) {
    val hasIssue = deviation.needs > 5 || deviation.wants > 5 || deviation.savings < -5
    val statusColor = when {
        hasIssue && deviation.savings < -10 -> ChartRed
        hasIssue -> ChartOrange
        else -> AccentPrimary
    }
    val statusLabel = when {
        hasIssue && deviation.savings < -10 -> "At Risk"
        hasIssue -> "Slightly Off"
        else -> "On Track"
    }
    val statusEmoji = when {
        hasIssue && deviation.savings < -10 -> "🔴"
        hasIssue -> "🟡"
        else -> "🟢"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = statusColor.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(CardRadius),
        border = BorderStroke(CardBorderWidth, statusColor.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(CardPadding),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(statusEmoji, style = MaterialTheme.typography.titleLarge)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        statusLabel,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        if (hasIssue) "Budget needs attention" else "You're staying within plan",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            }
            if (hasIssue && autopilotSuggestion != null) {
                Button(
                    onClick = { onApply(autopilotSuggestion) },
                    enabled = !isApplying,
                    modifier = Modifier.height(36.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = statusColor,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isApplying) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (isApplying) "Applying…" else "Fix it", style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}

@Composable
private fun StatusChipRow(
    deviation: com.example.monytix.budgetpilot.BudgetDeviation,
    variance: BudgetVariance?,
    autopilotSuggestion: com.example.monytix.budgetpilot.AutopilotSuggestion?,
    isApplying: Boolean,
    onApply: (com.example.monytix.budgetpilot.AutopilotSuggestion) -> Unit
) {
    val needsStr = formatDeviation(deviation.needs)
    val wantsStr = formatDeviation(deviation.wants)
    val savingsStr = formatDeviation(deviation.savings)
    val hasIssue = deviation.needs > 5 || deviation.wants > 5 || deviation.savings < -5
    val projectedOverspend = variance?.let { v ->
        val shortfall = -v.variance_assets_amt
        if (shortfall > 0) formatCurrency(shortfall) else null
    }
    val accent = AccentPrimary

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(ChipRadius),
            color = GlassCard,
            tonalElevation = 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(if (hasIssue) "⚠" else "✓", style = MaterialTheme.typography.labelSmall, color = if (hasIssue) ChartOrange else accent)
                Spacer(Modifier.width(6.dp))
                Text(
                    if (hasIssue) "Off track" else "On track",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
            }
        }
        Surface(
            shape = RoundedCornerShape(ChipRadius),
            color = GlassCard,
            tonalElevation = 0.dp
        ) {
            Text(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                text = "Savings $savingsStr",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        }
        if (projectedOverspend != null) {
            Surface(
                shape = RoundedCornerShape(ChipRadius),
                color = GlassCard,
                tonalElevation = 0.dp
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    text = "Projected overspend $projectedOverspend",
                    style = MaterialTheme.typography.labelSmall,
                    color = ChartOrange.copy(alpha = 0.9f)
                )
            }
        }
        if (hasIssue && autopilotSuggestion != null) {
            Surface(
                shape = RoundedCornerShape(ChipRadius),
                color = accent.copy(alpha = 0.15f),
                tonalElevation = 0.dp,
                onClick = { if (!isApplying) onApply(autopilotSuggestion) }
            ) {
                Text(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    text = if (isApplying) "Applying…" else "Apply fix",
                    style = MaterialTheme.typography.labelSmall,
                    color = accent,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}

@Composable
private fun NextBestActionCard(
    suggestion: com.example.monytix.budgetpilot.AutopilotSuggestion?,
    variance: BudgetVariance?,
    committedBudget: CommittedBudget?,
    isApplying: Boolean,
    hasRealData: Boolean,
    onApply: (com.example.monytix.budgetpilot.AutopilotSuggestion) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(CardRadius),
        border = BorderStroke(CardBorderWidth, AccentPrimary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Text(
                "Next Best Actions",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(8.dp))
            if (suggestion != null && hasRealData && committedBudget != null) {
                val income = variance?.income_amt ?: 0.0
                val amount = (income * suggestion.pct / 100).toLong().coerceAtLeast(100)
                Text(
                    "Reduce ${suggestion.shiftFrom.replaceFirstChar { it.uppercase() }} by ${formatCurrency(amount.toDouble())} this week",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    "Tap to see where to cut • ${suggestion.message}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onApply(suggestion) },
                        enabled = !isApplying,
                        modifier = Modifier.height(36.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isApplying) {
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(if (isApplying) "Applying…" else "Apply", style = MaterialTheme.typography.labelMedium)
                    }
                }
            } else {
                Text(
                    "Add transactions in SpendSense to get personalized suggestions.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

private fun formatDeviation(d: Double): String {
    val sign = if (d >= 0) "+" else ""
    return "$sign${d.toInt()}%"
}

@Composable
private fun SuggestedReformCard(
    suggestion: com.example.monytix.budgetpilot.AutopilotSuggestion,
    isApplying: Boolean,
    onApply: (com.example.monytix.budgetpilot.AutopilotSuggestion) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, ChartGreen.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Suggested reform",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = suggestion.message,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = { onApply(suggestion) },
                enabled = !isApplying,
                modifier = Modifier.fillMaxWidth(),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = ChartGreen,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                if (isApplying) {
                    CircularProgressIndicator(modifier = Modifier.width(20.dp).height(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(if (isApplying) "Applying..." else "Apply update")
            }
        }
    }
}

@Composable
private fun WelcomeBanner(username: String?) {
    val displayName = username?.split("@")?.firstOrNull()?.takeIf { it.isNotBlank() }?.let {
        it.replaceFirstChar { c -> c.uppercase() }
    } ?: "User"
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(BannerPurple)
            .padding(16.dp)
    ) {
        Text(
            text = "Smart budget recommendations.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Commit to a plan and track your spending. Welcome back, $displayName!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
        )
    }
}

@Composable
private fun QuickStatsRow(variance: BudgetVariance) {
    val totalSpending = variance.needs_amt + variance.wants_amt
    val savingsRate = if (variance.income_amt > 0) {
        (variance.assets_amt / variance.income_amt * 100)
    } else 0.0

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        QuickStatCard(
            title = "Income",
            value = formatCurrency(variance.income_amt),
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            title = "Spending",
            value = formatCurrency(totalSpending),
            modifier = Modifier.weight(1f)
        )
        QuickStatCard(
            title = "Savings",
            value = "${savingsRate.toInt()}%",
            subtitle = formatCurrency(variance.assets_amt),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun QuickStatCard(
    title: String,
    value: String,
    subtitle: String? = null,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun defaultBudgetPlans(): List<BudgetRecommendation> = listOf(
    BudgetRecommendation(plan_code = "BAL_50_30_20", name = "Balanced 50/30/20", recommendation_reason = "Default balanced plan", score = 0.5),
    BudgetRecommendation(plan_code = "EMERGENCY_FIRST", name = "Emergency Priority", recommendation_reason = "Boost savings until emergency funded", score = 0.5),
    BudgetRecommendation(plan_code = "DEBT_FIRST", name = "Debt First", recommendation_reason = "Aggressive needs to repay debt", score = 0.5),
    BudgetRecommendation(plan_code = "GOAL_PRIORITY", name = "Top 3 Goals Priority", recommendation_reason = "Assets tilt to top-3 goals", score = 0.5),
    BudgetRecommendation(plan_code = "LEAN_BASICS", name = "Lean Basics", recommendation_reason = "Tighten wants, preserve savings", score = 0.5)
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
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.padding(24.dp)) {
                Text(
                    "Add Budget",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "Choose a budget plan to commit to. Allocations are tailored to your goals.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(16.dp))
                plans.forEach { rec ->
                    val isCommitted = committedPlanCode == rec.plan_code
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isCommitted) ChartGreen.copy(alpha = 0.15f) else GlassCard
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    rec.name,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                if (rec.recommendation_reason.isNotBlank()) {
                                    Text(
                                        rec.recommendation_reason,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                            if (isCommitted) {
                                Text("Committed", style = MaterialTheme.typography.labelMedium, color = ChartGreen)
                            } else if (!isCommitting) {
                                Button(
                                    onClick = { onCommit(rec.plan_code) },
                                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Commit")
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
        }
    }
}

private fun formatLastUpdated(iso: String?): String {
    if (iso == null) return ""
    return try {
        val t = java.time.Instant.parse(iso)
        val now = java.time.Instant.now()
        val mins = java.time.Duration.between(t, now).toMinutes()
        when {
            mins < 1 -> "Just now"
            mins < 60 -> "${mins}m ago"
            else -> "${mins / 60}h ago"
        }
    } catch (_: Exception) { "" }
}

@Composable
private fun CommittedBudgetSection(
    committedBudget: CommittedBudget?,
    variance: BudgetVariance?,
    budgetState: BudgetStateResponse?,
    lastUpdatedAt: String?,
    isLoading: Boolean,
    isRecalculating: Boolean,
    onRecalculate: () -> Unit
) {
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    } else if (committedBudget != null) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Your Plan",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                )
                Text(
                    text = "Last updated: ${formatLastUpdated(lastUpdatedAt)}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            CommittedBudgetCard(
                committedBudget = committedBudget,
                variance = variance,
                budgetState = budgetState
            )
        }
    } else {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GlassCard),
            shape = RoundedCornerShape(CardRadius),
            border = BorderStroke(CardBorderWidth, AccentPrimary.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(CardPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(text = "📊", style = MaterialTheme.typography.displaySmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No Budget Committed",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Commit to a budget plan below to start tracking your spending against your goals.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
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
    val actual = budgetState?.actual
    val deviation = budgetState?.deviation
    val income = variance?.income_amt ?: 0.0
    val hasRealData = hasRealData(variance)
    val targetNeeds = income * committedBudget.alloc_needs_pct
    val targetWants = income * committedBudget.alloc_wants_pct
    val targetSavings = income * committedBudget.alloc_assets_pct

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(CardRadius),
        border = BorderStroke(CardBorderWidth, AccentPrimary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = planCodeToName(committedBudget.plan_code),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(text = "✓", style = MaterialTheme.typography.titleMedium, color = AccentPrimary)
            }
            Spacer(modifier = Modifier.height(12.dp))
            BudgetAllocationBar(
                needsPct = committedBudget.alloc_needs_pct,
                wantsPct = committedBudget.alloc_wants_pct,
                savingsPct = committedBudget.alloc_assets_pct
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (hasRealData && variance != null) {
                Text("Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                Text("Needs ${formatCurrency(targetNeeds)} • Wants ${formatCurrency(targetWants)} • Savings ${formatCurrency(targetSavings)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                Spacer(Modifier.height(8.dp))
                Text("Actual", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                Spacer(Modifier.height(4.dp))
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf(
                        Triple("Needs", variance.needs_amt, targetNeeds),
                        Triple("Wants", variance.wants_amt, targetWants),
                        Triple("Savings", variance.assets_amt, targetSavings)
                    ).forEach { (label, actualAmt, plannedAmt) ->
                        val over = actualAmt > plannedAmt && plannedAmt > 0
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                "$label: ${formatCurrency(actualAmt)} / ${formatCurrency(plannedAmt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                            )
                            if (over && label != "Needs") {
                                Text("⚠", style = MaterialTheme.typography.labelSmall, color = ChartOrange)
                            }
                        }
                    }
                }
                if (deviation != null && (deviation.wants > 5 || deviation.savings < -5)) {
                    Spacer(Modifier.height(8.dp))
                    val shortfall = -variance.variance_assets_amt
                    if (shortfall > 0) {
                        Text(
                            "⚠ At this rate you'll miss savings target by ${formatCurrency(shortfall)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = ChartOrange
                        )
                    }
                }
                val (dayOfMonth, daysInMonth) = try {
                    val now = java.time.LocalDate.now()
                    val cur = now.dayOfMonth
                    cur to java.time.YearMonth.now().lengthOfMonth()
                } catch (_: Exception) { 15 to 30 }
                if (dayOfMonth > 5 && hasRealData && variance != null) {
                    Spacer(Modifier.height(12.dp))
                    val factor = daysInMonth.toDouble() / dayOfMonth.coerceAtLeast(1)
                    val projWants = variance.wants_amt * factor
                    val wantsOver = projWants > targetWants
                    Text("Projected month-end", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Needs ${formatCurrency(variance.needs_amt * factor)} • Wants ${formatCurrency(projWants)}${if (wantsOver) " ⚠" else ""} • Savings ${formatCurrency(variance.assets_amt * factor)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                }
            } else {
                Text(
                    "Add transactions to see your progress against this plan.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}

private fun planCodeToSavingsPct(code: String): Double = when (code) {
    "BAL_50_30_20" -> 0.20
    "EMERGENCY_FIRST" -> 0.30
    "DEBT_FIRST" -> 0.25
    "GOAL_PRIORITY" -> 0.30
    "LEAN_BASICS" -> 0.25
    else -> 0.20
}

private fun planCodeToName(code: String): String = when (code) {
    "BAL_50_30_20" -> "Balanced 50/30/20"
    "EMERGENCY_FIRST" -> "Emergency Priority"
    "DEBT_FIRST" -> "Debt First"
    "GOAL_PRIORITY" -> "Top 3 Goals"
    "LEAN_BASICS" -> "Lean Basics"
    else -> code
}

@Composable
private fun TargetVsActualBar(
    targetNeeds: Double,
    targetWants: Double,
    targetSavings: Double,
    actualNeeds: Double,
    actualWants: Double,
    actualSavings: Double
) {
    Column {
        Row(modifier = Modifier.fillMaxWidth().height(24.dp)) {
            listOf(
                Triple(targetNeeds, actualNeeds, ChartBlue),
                Triple(targetWants, actualWants, ChartOrange),
                Triple(targetSavings, actualSavings, ChartGreen)
            ).forEachIndexed { i, (targetFrac, actualFrac, color) ->
                val targetW = targetFrac.toFloat().coerceIn(0.01f, 1f)
                    Box(
                        modifier = Modifier
                            .weight(targetW)
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .padding(horizontal = 1.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                        )
                        val fillRatio = if (targetFrac > 0) (actualFrac / targetFrac).toFloat().coerceIn(0f, 1f) else 0f
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(fillRatio)
                                .fillMaxSize()
                                .background(color, RoundedCornerShape(4.dp))
                        )
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            listOf(
                Triple("Needs", (actualNeeds * 100).toInt(), ChartBlue),
                Triple("Wants", (actualWants * 100).toInt(), ChartOrange),
                Triple("Savings", (actualSavings * 100).toInt(), ChartGreen)
            ).forEach { (label, pct, color) ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(8.dp)
                            .height(8.dp)
                            .background(color, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$label $pct%",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
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
    variance: BudgetVariance?,
    onPreview: () -> Unit,
    onCommit: () -> Unit
) {
    val fitScore = (recommendation.score * 100).toInt()

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCommitted) GlassCard.copy(alpha = 0.5f) else GlassCard
        ),
        shape = RoundedCornerShape(CardRadius),
        border = if (isCommitted) BorderStroke(CardBorderWidth, AccentPrimary.copy(alpha = 0.5f)) else BorderStroke(CardBorderWidth, AccentPrimary.copy(alpha = 0.15f))
    ) {
        Column(modifier = Modifier.padding(CardPadding)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = recommendation.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "Fit Score ${fitScore}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = AccentPrimary
                    )
                    Text(
                        text = "Based on last 60 days spending",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
                if (isCommitted) {
                    Text("Active", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                text = recommendation.recommendation_reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(
                    onClick = onPreview,
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                ) {
                    Text("Preview plan", style = MaterialTheme.typography.labelMedium)
                }
                if (!isCommitted) {
                    Button(
                        onClick = onCommit,
                        enabled = !isCommitting,
                        modifier = Modifier.height(36.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = AccentPrimary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isCommitting) {
                            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(if (isCommitting) "Switching…" else "Switch", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlanToAchieveBottomSheet(
    plan: BudgetRecommendation,
    variance: BudgetVariance?,
    autopilotSuggestion: com.example.monytix.budgetpilot.AutopilotSuggestion?,
    onDismiss: () -> Unit,
    onCommit: () -> Unit,
    isCommitting: Boolean
) {
    val income = variance?.income_amt ?: 0.0
    val wantsOver = variance?.let { (it.wants_amt - it.planned_wants_amt).coerceAtLeast(0.0) } ?: 0.0
    val savingsShortfall = variance?.let { (it.planned_assets_amt - it.assets_amt).coerceAtLeast(0.0) } ?: 0.0
    val savingsPct = if (plan.savings_budget_pct > 0) plan.savings_budget_pct else planCodeToSavingsPct(plan.plan_code)
    val weeklySavings = (income * savingsPct) / 4

    androidx.compose.material3.ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(
                "Plan to achieve ${plan.name}",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(8.dp))
            Text(
                "To hit ${plan.name} this month",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(Modifier.height(16.dp))
            if (wantsOver > 0) {
                Text("• Cut Wants by ${formatCurrency(wantsOver)} (food delivery + entertainment)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                Spacer(Modifier.height(4.dp))
            }
            Text("• Add Savings ${formatCurrency(weeklySavings)}/week (auto-transfer every Monday)", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
            if (savingsShortfall > 0) {
                Spacer(Modifier.height(4.dp))
                Text("• Reduce non-essential transfers by ${formatCurrency(savingsShortfall)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
            }
            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onCommit,
                    enabled = !isCommitting,
                    modifier = Modifier.weight(1f),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = AccentPrimary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isCommitting) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                        Spacer(Modifier.width(6.dp))
                    }
                    Text(if (isCommitting) "Activating…" else "Start this plan")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun BudgetAllocationBar(
    needsPct: Double,
    wantsPct: Double,
    savingsPct: Double
) {
    var needsFrac = (needsPct * 100).coerceIn(0.0, 100.0) / 100.0
    var wantsFrac = (wantsPct * 100).coerceIn(0.0, 100.0) / 100.0
    var savingsFrac = (savingsPct * 100).coerceIn(0.0, 100.0) / 100.0
    val total = needsFrac + wantsFrac + savingsFrac
    if (total <= 0) {
        needsFrac = 0.5
        wantsFrac = 0.3
        savingsFrac = 0.2
    }

    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
        ) {
            if (needsFrac > 0) {
                Box(
                    modifier = Modifier
                        .weight(needsFrac.toFloat())
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(ChartBlue, RoundedCornerShape(topStart = 8.dp, bottomStart = 8.dp))
                )
            }
            if (wantsFrac > 0) {
                Box(
                    modifier = Modifier
                        .weight(wantsFrac.toFloat())
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(ChartOrange)
                )
            }
            if (savingsFrac > 0) {
                Box(
                    modifier = Modifier
                        .weight(savingsFrac.toFloat())
                        .fillMaxWidth()
                        .height(24.dp)
                        .background(ChartGreen, RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                )
            }
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(ChartBlue, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Needs ${(needsPct * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(ChartOrange, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Wants ${(wantsPct * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .width(8.dp)
                        .height(8.dp)
                        .background(ChartGreen, RoundedCornerShape(4.dp))
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Savings ${(savingsPct * 100).toInt()}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun ErrorState(message: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Unable to Load Recommendations",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.error
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onRetry, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
            Text("Retry")
        }
    }
}

@Composable
private fun EmptyRecommendationsState() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "📊", style = MaterialTheme.typography.displayMedium)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No Recommendations Available",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "Budget recommendations will appear here once you have spending data and goals set up.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
    }
}

private fun formatCurrency(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    val formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(abs.toLong())
    return if (amount < 0) "-₹$formatted" else "₹$formatted"
}
