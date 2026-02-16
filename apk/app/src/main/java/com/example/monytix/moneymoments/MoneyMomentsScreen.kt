package com.example.monytix.moneymoments

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monytix.data.MoneyMoment
import com.example.monytix.data.Nudge
import com.example.monytix.ui.theme.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoneyMomentsScreen(
    viewModel: MoneyMomentsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(MmTab.NUDGES) }
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
                        onClick = { viewModel.loadData() },
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
            TabBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            when (selectedTab) {
                MmTab.NUDGES -> NudgesTab(viewModel = viewModel)
                MmTab.HABITS -> HabitsTab(viewModel = viewModel)
                MmTab.AI_INSIGHTS -> AIInsightsTab(viewModel = viewModel)
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
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (uiState.nudgesError != null && uiState.nudges.isEmpty()) {
        EmptyState(
            title = "Unable to Load Nudges",
            subtitle = uiState.nudgesError ?: "",
            onRetry = { viewModel.loadData() }
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
                        onClick = { viewModel.evaluateAndDeliverNudges() },
                        enabled = !uiState.isEvaluating && !uiState.isComputing,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        if (uiState.isEvaluating || uiState.isComputing) {
                            CircularProgressIndicator(modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onPrimary)
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
                    onCtaClick = { viewModel.logNudgeInteraction(nudge.delivery_id, "click") }
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
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (uiState.momentsError != null && uiState.moments.isEmpty()) {
        EmptyState(
            title = "Unable to Load Habits",
            subtitle = uiState.momentsError ?: "",
            onRetry = { viewModel.loadData() }
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
                onClick = { viewModel.computeMoments() },
                enabled = !uiState.isComputing,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                if (uiState.isComputing) {
                    CircularProgressIndicator(modifier = Modifier.padding(8.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(Modifier.padding(4.dp))
                }
                Text(if (uiState.isComputing) "Computing..." else "Compute Moments for Past 12 Months")
            }
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Your Habits", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        items(uiState.moments, key = { "${it.habit_id}-${it.month}" }) { moment ->
            MoneyMomentCard(moment = moment)
        }
    }
}

@Composable
private fun MoneyMomentCard(moment: MoneyMoment) {
    val icon = when {
        moment.habit_id.contains("burn_rate") || moment.habit_id.contains("spend_to_income") -> "📈"
        moment.habit_id.contains("micro") || moment.habit_id.contains("cash") -> "ℹ️"
        else -> "⚠️"
    }
    val confidenceColor = when {
        moment.confidence >= 0.7 -> com.example.monytix.ui.theme.Success
        moment.confidence >= 0.5 -> com.example.monytix.ui.theme.Warning
        else -> com.example.monytix.ui.theme.Warning
    }
    val valueStr = when {
        moment.habit_id.contains("ratio") || moment.habit_id.contains("share") -> "${(moment.value * 100).toInt()}%"
        moment.habit_id.contains("count") -> "${moment.value.toInt()}"
        else -> formatCurrency(moment.value)
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(icon, style = MaterialTheme.typography.headlineMedium)
                Text(
                    "${(moment.confidence * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.background(confidenceColor, RoundedCornerShape(8.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            Spacer(Modifier.height(8.dp))
            Text(moment.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(moment.insight_text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f), maxLines = 3)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(valueStr, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(moment.habit_id.replace("_", " ").uppercase(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
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
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
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
