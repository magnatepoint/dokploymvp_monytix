package com.example.monytix.moneymoments

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.LaunchedEffect
import com.example.monytix.R
import com.example.monytix.analytics.AnalyticsHelper
import com.example.monytix.data.MoneyMoment
import com.example.monytix.data.Nudge
import com.example.monytix.ui.MonytixSpinner
import com.example.monytix.ui.theme.AccentPrimary
import com.example.monytix.ui.theme.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyMomentsScreen(
    viewModel: MoneyMomentsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(MmTab.NUDGES) }

    LaunchedEffect(Unit) { AnalyticsHelper.logScreenView("money_moments") }
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            androidx.compose.material3.TopAppBar(
                title = { Text("MoneyMoments", color = colorScheme.onBackground) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onBackground
                ),
                actions = {
                    IconButton(
                        onClick = {
                            AnalyticsHelper.logEvent("refresh")
                            viewModel.loadData()
                        },
                        enabled = !uiState.isMomentsLoading && !uiState.isNudgesLoading
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh", tint = colorScheme.onBackground)
                    }
                }
            )
        },
        containerColor = colorScheme.background
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            WelcomeBanner(username = uiState.userEmail)
            TabBar(selectedTab = selectedTab, onTabSelected = {
                AnalyticsHelper.logEvent("tab_selected", mapOf("tab" to it.name.lowercase()))
                selectedTab = it
            })
            PullToRefreshBox(
                isRefreshing = uiState.isMomentsLoading || uiState.isNudgesLoading,
                onRefresh = { viewModel.loadData() }
            ) {
                when (selectedTab) {
                    MmTab.NUDGES -> NudgesTab(viewModel = viewModel)
                    MmTab.HABITS -> HabitsTab(viewModel = viewModel)
                    MmTab.AI_INSIGHTS -> AIInsightsTab(viewModel = viewModel)
                }
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
            .background(MaterialTheme.colorScheme.surface.copy(alpha = 0.5f))
            .padding(20.dp)
    ) {
        Text(
            text = "Gentle reminders for smarter habits.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.95f)
        )
        Text(
            text = "Smart nudges and personalized prompts. Welcome back, $displayName!",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
        )
    }
}

private enum class MmTab(val label: String, val icon: String) {
    NUDGES("Nudges", "🔔"),
    HABITS("Habits", "🔄"),
    AI_INSIGHTS("AI Insights", "💡")
}

@Composable
private fun TabBar(selectedTab: MmTab, onTabSelected: (MmTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        MmTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            TextButton(
                onClick = { onTabSelected(tab) },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.background(
                    if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent
                )
            ) {
                Text("${tab.icon} ${tab.label}", style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

@Composable
private fun NudgesTab(viewModel: MoneyMomentsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val metrics = viewModel.computeProgressMetrics()

    if (uiState.isNudgesLoading && uiState.nudges.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MonytixSpinner()
        }
        return
    }

    if (uiState.nudgesError != null && uiState.nudges.isEmpty()) {
        EmptyState(
            title = "Unable to Load Nudges",
            subtitle = uiState.nudgesError ?: "",
            onRetry = {
                AnalyticsHelper.logEvent("refresh")
                viewModel.loadData()
            }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text("Your Progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(modifier = Modifier.weight(1f), label = "Streak", value = "${metrics.streak} days", color = MaterialTheme.colorScheme.error)
                MetricCard(modifier = Modifier.weight(1f), label = "Nudges", value = "${metrics.nudgesCount}", color = com.example.monytix.ui.theme.Info)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(modifier = Modifier.weight(1f), label = "Habits", value = "${metrics.habitsCount}", color = com.example.monytix.ui.theme.Success)
                MetricCard(modifier = Modifier.weight(1f), label = "Saved", value = formatCurrency(metrics.savedAmount), color = MaterialTheme.colorScheme.tertiary)
            }
        }
        item {
            Text("Active Nudges", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        if (uiState.nudges.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text("🔔", style = MaterialTheme.typography.displayMedium)
                    Text("No Nudges Yet", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        "Nudges are personalized recommendations. Evaluate and deliver nudges to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    uiState.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
                    Button(
                        onClick = {
                            AnalyticsHelper.logEvent("evaluate_nudges")
                            viewModel.evaluateAndDeliverNudges()
                        },
                        enabled = !uiState.isEvaluating && !uiState.isComputing,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        if (uiState.isEvaluating || uiState.isComputing) {
                            MonytixSpinner(size = 20.dp, stroke = 2.dp)
                            Spacer(Modifier.padding(4.dp))
                        }
                        Text(if (uiState.isEvaluating || uiState.isComputing) "Processing..." else "Evaluate & Deliver Nudges")
                    }
                }
            }
        } else {
            items(uiState.nudges, key = { it.delivery_id }) { nudge ->
                NudgeCard(
                    nudge = nudge,
                    onView = { viewModel.logNudgeInteraction(nudge.delivery_id, "view") },
                    onCtaClick = {
                        AnalyticsHelper.logEvent("nudge_cta_tapped", mapOf("nudge_id" to nudge.delivery_id))
                        viewModel.logNudgeInteraction(nudge.delivery_id, "click")
                    }
                )
            }
        }
    }
}

@Composable
private fun MetricCard(
    label: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun NudgeCard(
    nudge: Nudge,
    onView: () -> Unit,
    onCtaClick: () -> Unit = {}
) {
    androidx.compose.runtime.LaunchedEffect(nudge.delivery_id) {
        onView()
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("✨ ${nudge.rule_name}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(nudge.sent_at.take(10), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
            Spacer(Modifier.height(8.dp))
            Text(
                nudge.title ?: nudge.title_template ?: "Nudge",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            (nudge.body ?: nudge.body_template)?.takeIf { it.isNotBlank() }?.let { body ->
                Spacer(Modifier.height(4.dp))
                Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), maxLines = 4)
            }
            nudge.cta_text?.takeIf { it.isNotBlank() }?.let { cta ->
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = onCtaClick,
                    modifier = Modifier.fillMaxWidth(),
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                ) {
                    Text(cta)
                }
            }
        }
    }
}

@Composable
private fun HabitsTab(viewModel: MoneyMomentsViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isMomentsLoading && uiState.moments.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MonytixSpinner()
        }
        return
    }

    if (uiState.momentsError != null && uiState.moments.isEmpty()) {
        EmptyState(
            title = "Unable to Load Habits",
            subtitle = uiState.momentsError ?: "",
            onRetry = {
                AnalyticsHelper.logEvent("refresh")
                viewModel.loadData()
            }
        )
        return
    }

    if (uiState.moments.isEmpty()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("📊", style = MaterialTheme.typography.displayLarge)
            Spacer(Modifier.height(16.dp))
            Text("No Habits Tracked Yet", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(
                "Habits are derived from spending moments. Compute moments to start tracking.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            uiState.actionError?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    AnalyticsHelper.logEvent("compute_moments")
                    viewModel.computeMoments()
                },
                enabled = !uiState.isComputing,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                if (uiState.isComputing) {
                    MonytixSpinner(size = 20.dp, stroke = 2.dp)
                    Spacer(Modifier.padding(4.dp))
                }
                Text(if (uiState.isComputing) "Computing..." else "Compute Moments for Past 12 Months")
            }
        }
        return
    }

    val behaviorScore = remember(uiState.moments) {
        if (uiState.moments.isEmpty()) 0 else (uiState.moments.map { it.confidence }.average() * 100).toInt().coerceIn(0, 100)
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                stringResource(R.string.habits_section_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        stringResource(R.string.habit_behavior_score),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "$behaviorScore / 100",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = AccentPrimary
                    )
                    Text(
                        stringResource(R.string.habit_based_on_count, uiState.moments.size),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
        }
        items(uiState.moments, key = { "${it.habit_id}-${it.month}" }) { moment ->
            MoneyMomentCard(
                moment = moment,
                allMoments = uiState.moments,
                display = moment.toHabitCardDisplay()
            )
        }
    }
}

@Composable
private fun MoneyMomentCard(
    moment: MoneyMoment,
    allMoments: List<MoneyMoment>,
    display: HabitCardDisplay
) {
    var expanded by remember { mutableStateOf(false) }
    val healthColor = when (display.health) {
        HabitHealth.Healthy -> com.example.monytix.ui.theme.Success
        HabitHealth.Moderate -> com.example.monytix.ui.theme.Warning
        HabitHealth.NeedsAttention -> com.example.monytix.ui.theme.Error
    }
    val healthText = when (display.health) {
        HabitHealth.Healthy -> stringResource(R.string.habit_health_healthy)
        HabitHealth.Moderate -> stringResource(R.string.habit_health_moderate)
        HabitHealth.NeedsAttention -> stringResource(R.string.habit_health_needs_attention)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(display.icon, style = MaterialTheme.typography.headlineMedium)
                    Spacer(Modifier.size(8.dp))
                    Text(
                        display.conversationalTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2
                    )
                }
                Text(
                    healthText,
                    style = MaterialTheme.typography.labelMedium,
                    color = healthColor,
                    modifier = Modifier
                        .background(healthColor.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            if (display.progressBarRatio != null && display.progressLabel != null) {
                Spacer(Modifier.height(12.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(com.example.monytix.ui.theme.BorderSubtle)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(display.progressBarRatio.coerceIn(0f, 1f))
                            .fillMaxHeight()
                            .background(AccentPrimary, RoundedCornerShape(4.dp))
                    )
                }
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        display.progressLabel,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    display.idealOrBenchmark?.let { ideal ->
                        Text(
                            ideal,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text(
                display.insightLine,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                display.actionLine,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = AccentPrimary
            )

            Spacer(Modifier.height(8.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    stringResource(R.string.habit_view_details),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (expanded) "Collapse" else "Expand",
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(),
                exit = shrinkVertically()
            ) {
                Column(Modifier.padding(top = 12.dp)) {
                    val sameHabitMonths = allMoments
                        .filter { it.habit_id == moment.habit_id }
                        .sortedByDescending { it.month }
                        .take(3)
                    if (sameHabitMonths.isNotEmpty()) {
                        Text(
                            "Last ${sameHabitMonths.size} months",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(4.dp))
                        sameHabitMonths.forEach { m ->
                            val v = when {
                                m.habit_id.contains("ratio") || m.habit_id.contains("share") -> "${(m.value * 100).toInt()}%"
                                m.habit_id.contains("count") -> "${m.value.toInt()}"
                                else -> formatCurrency(m.value)
                            }
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(m.month, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                                Text(v, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = AccentPrimary)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AIInsightsTab(viewModel: MoneyMomentsViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val moments = uiState.moments
    val nudges = uiState.nudges

    val insights = remember(moments, nudges) {
        val result = mutableListOf<AiInsight>()
        val highConf = moments.filter { it.confidence >= 0.7 }
        if (highConf.isNotEmpty()) {
            result.add(AiInsight("1", "progress", "Great progress! You have ${highConf.size} well-established spending patterns.", "🏆"))
        }
        moments.firstOrNull()?.let { m ->
            result.add(AiInsight("2", "suggestion", "Based on your ${m.label.lowercase()}, consider reviewing your ${m.habit_id.replace("_", " ")}.", "💡"))
        }
        if (nudges.isNotEmpty()) {
            result.add(AiInsight("3", "milestone", "You've received ${nudges.size} personalized recommendations. Keep up the great work!", "🎯"))
        }
        if (result.isEmpty()) {
            result.add(AiInsight("0", "suggestion", "AI insights will appear here based on your spending patterns and habits.", "✨"))
        }
        result
    }

    if (uiState.isMomentsLoading && uiState.isNudgesLoading) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MonytixSpinner()
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Recent Insights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        items(insights, key = { it.id }) { insight ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                    Text(insight.icon, style = MaterialTheme.typography.headlineSmall)
                    Spacer(Modifier.padding(8.dp))
                    Text(insight.message, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurface)
                }
            }
        }
    }
}

private data class AiInsight(val id: String, val type: String, val message: String, val icon: String)

@Composable
private fun EmptyState(
    title: String,
    subtitle: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
            Text("Retry")
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    val formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(abs.toLong())
    return if (amount < 0) "-₹$formatted" else "₹$formatted"
}

// Habit Card 2.0: diagnostic display model (client-side from MoneyMoment)
private enum class HabitHealth { Healthy, Moderate, NeedsAttention }

private data class HabitCardDisplay(
    val health: HabitHealth,
    val conversationalTitle: String,
    val idealOrBenchmark: String?,
    val progressBarRatio: Float?,
    val progressLabel: String?, // e.g. "92% by Day 15"
    val insightLine: String,
    val actionLine: String,
    val icon: String
)

private fun MoneyMoment.toHabitCardDisplay(): HabitCardDisplay {
    val id = habit_id.lowercase()
    val icon = when {
        id.contains("burn_rate") || id.contains("spend_to_income") -> "📈"
        id.contains("micro") || id.contains("cash") -> "ℹ️"
        else -> "⚠️"
    }
    return when {
        id.contains("burn_rate") || id.contains("early") -> {
            val ratio = value.coerceIn(0.0, 1.0).toFloat()
            val health = when {
                value > 0.75 -> HabitHealth.NeedsAttention
                value > 0.6 -> HabitHealth.Moderate
                else -> HabitHealth.Healthy
            }
            HabitCardDisplay(
                health = health,
                conversationalTitle = "You spend early in the month",
                idealOrBenchmark = "Ideal: < 60%",
                progressBarRatio = ratio,
                progressLabel = "${(value * 100).toInt()}% by Day 15",
                insightLine = insight_text.ifBlank { "You are spending most of your monthly budget within the first half of the month." },
                actionLine = "Try limiting first-week spending to 30% of your budget.",
                icon = icon
            )
        }
        id.contains("cash") || id.contains("cash_spend") -> {
            val health = when {
                value <= 0.2 -> HabitHealth.Healthy
                value <= 0.5 -> HabitHealth.Moderate
                else -> HabitHealth.NeedsAttention
            }
            HabitCardDisplay(
                health = health,
                conversationalTitle = "Cash vs digital mix",
                idealOrBenchmark = null,
                progressBarRatio = value.coerceIn(0.0, 1.0).toFloat(),
                progressLabel = "Digital ${(value * 100).toInt()}% | Cash ${(100 - value * 100).toInt()}%",
                insightLine = insight_text.ifBlank { "You rely ${if (value >= 0.8) "fully" else "mostly"} on digital payments." },
                actionLine = "Track small cash spends if any to improve accuracy.",
                icon = icon
            )
        }
        id.contains("micro") -> {
            val health = when {
                confidence >= 0.7 -> HabitHealth.Healthy
                confidence >= 0.5 -> HabitHealth.Moderate
                else -> HabitHealth.NeedsAttention
            }
            HabitCardDisplay(
                health = health,
                conversationalTitle = "Micro-spending pattern",
                idealOrBenchmark = null,
                progressBarRatio = null,
                progressLabel = null,
                insightLine = insight_text.ifBlank { "Frequent small transactions can silently increase monthly spend." },
                actionLine = "Set micro-spend alert above ₹3,000/month.",
                icon = icon
            )
        }
        else -> {
            val health = when {
                confidence >= 0.7 -> HabitHealth.Healthy
                confidence >= 0.5 -> HabitHealth.Moderate
                else -> HabitHealth.NeedsAttention
            }
            HabitCardDisplay(
                health = health,
                conversationalTitle = label.ifBlank { habit_id.replace("_", " ").replaceFirstChar { c -> c.uppercase() } },
                idealOrBenchmark = null,
                progressBarRatio = null,
                progressLabel = null,
                insightLine = insight_text.ifBlank { "Review this pattern for insights." },
                actionLine = "Review this pattern in SpendSense.",
                icon = icon
            )
        }
    }
}
