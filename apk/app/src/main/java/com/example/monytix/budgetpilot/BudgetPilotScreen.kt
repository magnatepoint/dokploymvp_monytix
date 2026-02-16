package com.example.monytix.budgetpilot

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
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
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monytix.data.BudgetRecommendation
import com.example.monytix.data.BudgetStateResponse
import com.example.monytix.data.CommittedBudget
import com.example.monytix.data.BudgetVariance
import com.example.monytix.ui.theme.BannerPurple
import com.example.monytix.ui.theme.ChartRed
import com.example.monytix.ui.theme.ChartBlue
import com.example.monytix.ui.theme.ChartGreen
import com.example.monytix.ui.theme.ChartOrange
import com.example.monytix.ui.theme.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BudgetPilotScreen(
    viewModel: BudgetPilotViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    var showAddBudget by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        BudgetUpdateCache.consume()?.let { viewModel.refresh() }
    }

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("BudgetPilot", color = colorScheme.onBackground) },
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
            FloatingActionButton(
                onClick = { showAddBudget = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add budget")
            }
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
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                WelcomeBanner(username = uiState.userEmail)
            }
            item {
                Spacer(modifier = Modifier.height(8.dp))
            }
            item {
                Text(
                    text = "Smart budget recommendations tailored to your spending patterns and goals",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                )
            }
            if (uiState.committedBudget != null && uiState.variance != null) {
                item {
                    QuickStatsRow(variance = uiState.variance!!)
                }
            }
            if (uiState.deviation != null && uiState.committedBudget != null) {
                item {
                    DeviationStrip(
                        deviation = uiState.deviation!!,
                        suggestion = uiState.autopilotSuggestion,
                        isApplying = uiState.isApplyingAdjustment,
                        onFixClick = { s -> viewModel.applyAdjustment(s.shiftFrom, s.shiftTo, s.pct) }
                    )
                }
            }
            if (uiState.autopilotSuggestion != null && uiState.committedBudget != null) {
                item {
                    SuggestedReformCard(
                        suggestion = uiState.autopilotSuggestion!!,
                        isApplying = uiState.isApplyingAdjustment,
                        onApply = { viewModel.applyAdjustment(it.shiftFrom, it.shiftTo, it.pct) }
                    )
                }
            }
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
            item {
                Text(
                    text = if (uiState.committedBudget != null) "Other Recommendations" else "Recommended Budget Plans",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
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
                items(uiState.recommendations) { rec ->
                    RecommendationCard(
                        recommendation = rec,
                        isCommitted = uiState.committedBudget?.plan_code == rec.plan_code,
                        isCommitting = uiState.isCommitting && uiState.committingPlanCode == rec.plan_code,
                        onCommit = { viewModel.commitBudget(rec.plan_code) }
                    )
                }
            }
            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
        }
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

@Composable
private fun DeviationStrip(
    deviation: com.example.monytix.budgetpilot.BudgetDeviation,
    suggestion: com.example.monytix.budgetpilot.AutopilotSuggestion?,
    isApplying: Boolean,
    onFixClick: (com.example.monytix.budgetpilot.AutopilotSuggestion) -> Unit
) {
    val needsStr = formatDeviation(deviation.needs)
    val wantsStr = formatDeviation(deviation.wants)
    val savingsStr = formatDeviation(deviation.savings)
    val hasIssue = deviation.needs > 5 || deviation.wants > 5 || deviation.savings < -5
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (hasIssue) ChartRed.copy(alpha = 0.12f)
                else ChartGreen.copy(alpha = 0.2f),
                RoundedCornerShape(8.dp)
            )
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Needs $needsStr", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("Wants $wantsStr", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
            Text("Savings $savingsStr", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface)
        }
        if (hasIssue && suggestion != null) {
            Button(
                onClick = { onFixClick(suggestion) },
                enabled = !isApplying,
                modifier = Modifier.height(32.dp),
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Fix", style = MaterialTheme.typography.labelMedium)
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
                    text = "Your Committed Budget",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (lastUpdatedAt != null) {
                    Text(
                        text = "Last updated: ${formatLastUpdated(lastUpdatedAt)}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onRecalculate,
                    enabled = !isRecalculating,
                    modifier = Modifier.height(28.dp),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    if (isRecalculating) {
                        CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(if (isRecalculating) "Recalculating..." else "Recalculate now", style = MaterialTheme.typography.labelSmall)
                }
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
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
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
    val hasActuals = actual != null && (actual.needs_pct > 0 || actual.wants_pct > 0 || actual.savings_pct > 0)

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = committedBudget.plan_code,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(text = "✓", style = MaterialTheme.typography.titleLarge, color = ChartGreen)
            }
            Spacer(modifier = Modifier.height(12.dp))
            if (hasActuals && actual != null && deviation != null) {
                TargetVsActualBar(
                    targetNeeds = committedBudget.alloc_needs_pct,
                    targetWants = committedBudget.alloc_wants_pct,
                    targetSavings = committedBudget.alloc_assets_pct,
                    actualNeeds = actual.needs_pct / 100,
                    actualWants = actual.wants_pct / 100,
                    actualSavings = actual.savings_pct / 100
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This month: Needs ${actual.needs_pct.toInt()}% (${formatDeviation(deviation.needs)}) • Wants ${actual.wants_pct.toInt()}% (${formatDeviation(deviation.wants)}) • Savings ${actual.savings_pct.toInt()}% (${formatDeviation(deviation.savings)})",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f)
                )
            } else {
                BudgetAllocationBar(
                    needsPct = committedBudget.alloc_needs_pct,
                    wantsPct = committedBudget.alloc_wants_pct,
                    savingsPct = committedBudget.alloc_assets_pct
                )
                if (variance != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Needs: ${formatCurrency(variance.needs_amt)} / ${formatCurrency(variance.planned_needs_amt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "Wants: ${formatCurrency(variance.wants_amt)} / ${formatCurrency(variance.planned_wants_amt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }
    }
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
    onCommit: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isCommitted) GlassCard.copy(alpha = 0.3f) else GlassCard
        ),
        shape = RoundedCornerShape(12.dp),
        border = if (isCommitted) BorderStroke(2.dp, ChartGreen) else null
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = recommendation.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isCommitted) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "✓ Active",
                                style = MaterialTheme.typography.labelSmall,
                                color = ChartGreen,
                                modifier = Modifier
                                    .background(ChartGreen.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                    recommendation.description?.let { desc ->
                        Text(
                            text = desc,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            maxLines = 2
                        )
                    }
                }
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Score",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "%.2f".format(recommendation.score),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            BudgetAllocationBar(
                needsPct = recommendation.needs_budget_pct,
                wantsPct = recommendation.wants_budget_pct,
                savingsPct = recommendation.savings_budget_pct
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = recommendation.recommendation_reason,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            if (isCommitted) {
                Text(
                    text = "✓ Committed",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = ChartGreen
                )
            } else {
                Button(
                    onClick = onCommit,
                    enabled = !isCommitting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (isCommitting) {
                        CircularProgressIndicator(
                            modifier = Modifier.width(20.dp).height(20.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                    }
                    Text(
                        text = if (isCommitting) "Committing..." else "Commit to This Plan",
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
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
