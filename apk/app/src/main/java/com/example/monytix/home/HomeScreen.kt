package com.example.monytix.home

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Brush
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monytix.R
import com.example.monytix.data.AccountItemResponse
import com.example.monytix.data.KpiResponse
import com.example.monytix.data.TransactionRecordResponse
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import com.example.monytix.ui.theme.AccentPrimary
import com.example.monytix.ui.theme.HeroCardGlow
import com.example.monytix.ui.theme.AccentSecondary
import com.example.monytix.ui.theme.BackgroundGradientBottom
import com.example.monytix.ui.theme.BackgroundGradientTop
import com.example.monytix.ui.theme.GlassCard
import com.example.monytix.ui.theme.SurfaceElevated

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onLaunchFilePicker: () -> Unit = {},
    onAddTransaction: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(ConsoleTab.OVERVIEW) }

    val colorScheme = MaterialTheme.colorScheme
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("MolyConsole", color = colorScheme.onBackground) },
                colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onBackground
                ),
                actions = {
                    val infiniteTransition = rememberInfiniteTransition(label = "refresh_spin")
                    val rotation by infiniteTransition.animateFloat(
                        initialValue = 0f,
                        targetValue = 360f,
                        animationSpec = infiniteRepeatable(
                            animation = tween(1000),
                            repeatMode = RepeatMode.Restart
                        ),
                        label = "refresh_rotation"
                    )
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = colorScheme.onBackground,
                            modifier = Modifier.rotate(if (uiState.isLoading) rotation else 0f)
                        )
                    }
                }
            )
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize().padding(innerPadding)) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawRect(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    AccentPrimary.copy(alpha = 0.08f),
                                    Color.Transparent
                                ),
                                center = Offset(size.width * 0.5f, size.height * 0.15f),
                                radius = size.width * 0.8f
                            )
                        )
                    }
            )
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(BackgroundGradientTop, BackgroundGradientBottom)
                        )
                    )
            ) {
            WelcomeBanner(username = uiState.userEmail ?: "User")
            TabBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            if (uiState.isLoading && uiState.kpis == null) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = colorScheme.primary)
                }
            } else {
                AnimatedContent(
                    targetState = selectedTab,
                    transitionSpec = { fadeIn(animationSpec = tween(200)) togetherWith fadeOut(animationSpec = tween(200)) },
                    label = "tab_content"
                ) { tab ->
                    when (tab) {
                        ConsoleTab.OVERVIEW -> OverviewTab(viewModel = viewModel, onTabSelected = { selectedTab = it })
                        ConsoleTab.ACCOUNTS -> AccountsTab(accounts = uiState.accounts, onRetry = { viewModel.refresh() })
                        ConsoleTab.SPENDING -> SpendingTab(viewModel = viewModel)
                        ConsoleTab.GOALS -> GoalsTab(viewModel = viewModel)
                        ConsoleTab.AI_INSIGHT -> AIInsightTab(viewModel = viewModel)
                    }
                }
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = onLaunchFilePicker,
                containerColor = SurfaceElevated,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Upload PDF")
            }
            FloatingActionButton(
                onClick = onAddTransaction,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    }
    }
}

@Composable
private fun WelcomeBanner(username: String) {
    val displayName = username.split("@").firstOrNull()?.takeIf { it.isNotBlank() } ?: "User"
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.surface.copy(alpha = 0.5f))
            .padding(20.dp)
    ) {
        Text(
            text = "AI Financial Command Center",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onBackground.copy(alpha = 0.95f)
        )
        Text(
            text = "Welcome back, $displayName • Your financial cockpit",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onBackground.copy(alpha = 0.6f)
        )
    }
}

private enum class ConsoleTab(val label: String, val icon: String) {
    OVERVIEW("Overview", "📊"),
    ACCOUNTS("Accounts", "💳"),
    SPENDING("Spending", "💰"),
    GOALS("Goals", "🎯"),
    AI_INSIGHT("AI Insight", "✨")
}

@Composable
private fun TabBar(selectedTab: ConsoleTab, onTabSelected: (ConsoleTab) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        ConsoleTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TextButton(
                    onClick = { onTabSelected(tab) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = if (selected) colorScheme.onPrimary else colorScheme.onBackground.copy(alpha = 0.6f)
                    ),
                    modifier = Modifier.background(
                        if (selected) AccentPrimary.copy(alpha = 0.25f) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(tab.icon, modifier = Modifier.padding(end = 4.dp), style = MaterialTheme.typography.labelMedium)
                    Text(tab.label, style = MaterialTheme.typography.labelMedium)
                }
                if (selected) {
                    Box(
                        modifier = Modifier
                            .width(24.dp)
                            .height(2.dp)
                            .background(AccentPrimary, RoundedCornerShape(1.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewTab(viewModel: HomeViewModel, onTabSelected: (ConsoleTab) -> Unit = {}) {
    val uiState by viewModel.uiState.collectAsState()
    if (viewModel.hasNoTransactionData()) {
        EmptyState(
            title = "No overview data available",
            subtitle = "Upload statements to see your financial overview"
        )
        return
    }
    val kpis = uiState.kpis ?: return
    val goals = viewModel.transformGoals()
    val monthName = java.text.SimpleDateFormat("MMMM", java.util.Locale.getDefault()).format(java.util.Date())
    val totalBalance = (kpis.income_amount ?: 0.0) - (kpis.total_debits_amount ?: (kpis.needs_amount ?: 0.0) + (kpis.wants_amount ?: 0.0))
    val thisMonthSpending = kpis.total_debits_amount ?: (kpis.needs_amount ?: 0.0) + (kpis.wants_amount ?: 0.0)
    val income = kpis.income_amount ?: 0.0
    val savingsRate = if (income > 0) {
        val expenses = (kpis.needs_amount ?: 0.0) + (kpis.wants_amount ?: 0.0)
        ((income - expenses) / income * 100)
    } else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AnimatedVisibility(
                visible = true,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(animationSpec = tween(400))
            ) {
                HeroBalanceCard(
                    netWorth = viewModel.totalNetWorth(),
                    trendPct = viewModel.netWorthTrendPct(),
                    sparklineData = viewModel.sparklineData(),
                    totalBalance = totalBalance,
                    onClick = { }
                )
            }
        }
        val todayItems = viewModel.todayIntelligence()
        if (todayItems.isNotEmpty()) {
            item {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(animationSpec = tween(400, delayMillis = 100))
                ) {
                    TodaysIntelligenceCard(items = todayItems)
                }
            }
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                DynamicChip(
                    label = if (viewModel.isCashFlowPositive()) "Cash Flow Positive" else "Cash Flow Negative",
                    isPositive = viewModel.isCashFlowPositive()
                )
                val atRisk = viewModel.goalsAtRiskCount()
                if (atRisk > 0) {
                    DynamicChip(label = "$atRisk Goals At Risk", isPositive = false)
                }
                val spikes = viewModel.spendingSpikeCount()
                if (spikes > 0) {
                    DynamicChip(label = "$spikes Spending Spike${if (spikes > 1) "s" else ""}", isPositive = false)
                }
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "$monthName Spending",
                    value = formatCurrency(thisMonthSpending),
                    color = com.example.monytix.ui.theme.ChartRed,
                    onClick = { onTabSelected(ConsoleTab.SPENDING) }
                )
                SavingsRateRingCard(
                    modifier = Modifier.weight(1f),
                    savingsRate = savingsRate,
                    onClick = { onTabSelected(ConsoleTab.AI_INSIGHT) }
                )
                SummaryCard(
                    modifier = Modifier.weight(1f),
                    title = "Active Goals",
                    value = goals.size.toString(),
                    color = MaterialTheme.colorScheme.primary,
                    onClick = { onTabSelected(ConsoleTab.GOALS) }
                )
            }
        }
        val recentTxns = uiState.recentTransactions
        if (recentTxns.isNotEmpty()) {
            item {
                Text("Recent Transactions", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            items(recentTxns.take(5)) { txn ->
                RecentTransactionRow(transaction = txn)
            }
        }
        val categories = viewModel.transformCategorySpending()
        if (categories.isNotEmpty()) {
            item {
                Text("Spending by Category", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
            }
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    shape = RoundedCornerShape(16.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
                ) {
                    Column(Modifier.padding(16.dp)) {
                        categories.take(5).forEachIndexed { index, cat ->
                            val maxAmount = categories.maxOfOrNull { it.amount } ?: 1.0
                            val fraction = if (maxAmount > 0) (cat.amount / maxAmount).toFloat() else 0f
                            CategoryBarRow(
                                category = cat.category,
                                amount = cat.amount,
                                fraction = fraction,
                                animationDelay = index * 80
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TodaysIntelligenceCard(items: List<HomeViewModel.TodayIntelligenceItem>) {
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.2f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                "TODAY",
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.SemiBold,
                color = AccentPrimary
            )
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items.forEach { item ->
                    Text(
                        "• ${item.label}",
                        style = MaterialTheme.typography.bodySmall,
                        color = when (item.type) {
                            "positive" -> com.example.monytix.ui.theme.Success
                            "risk" -> com.example.monytix.ui.theme.Warning
                            "spike" -> com.example.monytix.ui.theme.ChartRed
                            else -> colorScheme.onSurface
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun DynamicChip(label: String, isPositive: Boolean) {
    val color = if (isPositive) com.example.monytix.ui.theme.Success else com.example.monytix.ui.theme.ChartRed
    Text(
        label,
        style = MaterialTheme.typography.labelSmall,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    )
}

@Composable
private fun AiInsightBanner(insight: AiInsight) {
    val colorScheme = MaterialTheme.colorScheme
    val accentColor = when (insight.type) {
        "spending_alert" -> com.example.monytix.ui.theme.Warning
        "goal_progress" -> com.example.monytix.ui.theme.Success
        "budget_tip" -> com.example.monytix.ui.theme.Info
        "investment_recommendation" -> AccentSecondary
        else -> AccentSecondary
    }
    val infiniteTransition = rememberInfiniteTransition(label = "ai_icon")
    val iconAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ai_icon_alpha"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, accentColor.copy(alpha = 0.4f))
    ) {
        Row(
            Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Lightbulb,
                contentDescription = null,
                tint = accentColor.copy(alpha = iconAlpha),
                modifier = Modifier.size(24.dp)
            )
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        insight.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = accentColor
                    )
                    if (insight.confidence != null && insight.confidence > 0.8f) {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "High confidence",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(
                    insight.message,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun HeroBalanceCard(
    netWorth: Double,
    trendPct: Double,
    sparklineData: List<Float>,
    totalBalance: Double = 0.0,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val isProfitable = totalBalance >= 0
    val infiniteTransition = rememberInfiniteTransition(label = "hero_glow")
    val pulseAnim by infiniteTransition.animateFloat(
        initialValue = 0.2f,
        targetValue = 0.4f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )
    val pulseAlpha = if (isProfitable) pulseAnim else 0.25f
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.5.dp, (if (isProfitable) AccentPrimary else com.example.monytix.ui.theme.ChartRed).copy(alpha = pulseAlpha))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            HeroCardGlow,
                            GlassCard,
                            AccentPrimary.copy(alpha = 0.15f)
                        )
                    )
                )
                .drawBehind {
                    drawLine(
                        color = Color.White.copy(alpha = 0.06f),
                        start = Offset(0f, 0f),
                        end = Offset(size.width, 0f),
                        strokeWidth = 1f
                    )
                }
                .padding(20.dp)
        ) {
            Column {
                Text(
                    "Net Worth",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    AnimatedCurrencyText(
                        targetAmount = netWorth,
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            if (trendPct >= 0) "↑" else "↓",
                            style = MaterialTheme.typography.labelMedium,
                            color = if (trendPct >= 0) com.example.monytix.ui.theme.Success else com.example.monytix.ui.theme.ChartRed
                        )
                        Text(
                            "${kotlin.math.abs(trendPct).toInt()}% this month",
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant
                        )
                    }
                }
                if (sparklineData.isNotEmpty()) {
                    Spacer(Modifier.height(16.dp))
                    MiniSparkline(data = sparklineData)
                }
            }
        }
    }
}

@Composable
private fun AnimatedCurrencyText(
    targetAmount: Double,
    style: TextStyle,
    fontWeight: FontWeight,
    color: Color
) {
    val animatedValue = animateFloatAsState(
        targetValue = targetAmount.toFloat(),
        animationSpec = tween(800),
        label = "currency"
    ).value
    Text(
        text = formatCurrency(animatedValue.toDouble()),
        style = style,
        fontWeight = fontWeight,
        color = color
    )
}

@Composable
private fun SavingsRateRingCard(
    savingsRate: Double,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    val colorScheme = MaterialTheme.colorScheme
    val sweepAngle = (savingsRate / 100.0 * 360.0).coerceIn(-360.0, 360.0).toFloat()
    val animatedSweep by animateFloatAsState(
        targetValue = sweepAngle,
        animationSpec = tween(800),
        label = "savings_ring"
    )
    val isPositive = savingsRate >= 0
    val ringColor = if (isPositive) AccentPrimary else com.example.monytix.ui.theme.ChartRed
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(64.dp),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val strokeWidth = 5.dp.toPx()
                    val radius = (size.minDimension - strokeWidth) / 2
                    val center = Offset(size.width / 2, size.height / 2)
                    drawCircle(
                        color = colorScheme.outline.copy(alpha = 0.2f),
                        radius = radius,
                        center = center,
                        style = Stroke(width = strokeWidth)
                    )
                    drawArc(
                        color = ringColor,
                        startAngle = 270f,
                        sweepAngle = animatedSweep,
                        useCenter = false,
                        topLeft = center - Offset(radius, radius),
                        size = Size(radius * 2, radius * 2),
                        style = Stroke(width = strokeWidth)
                    )
                }
                Text(
                    "${savingsRate.toInt()}%",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isPositive) AccentPrimary else com.example.monytix.ui.theme.ChartRed
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                "Savings Rate",
                style = MaterialTheme.typography.labelSmall,
                color = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryBarRow(
    category: String,
    amount: Double,
    fraction: Float,
    animationDelay: Int
) {
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(400, delayMillis = animationDelay),
        label = "category_bar"
    )
    Row(
        Modifier.fillMaxWidth().padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            category,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(100.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(8.dp)
                .padding(horizontal = 8.dp)
                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedFraction)
                    .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
            )
        }
        Text(
            formatCurrency(amount),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(60.dp)
        )
    }
}

@Composable
private fun MiniSparkline(data: List<Float>) {
    val colorScheme = MaterialTheme.colorScheme
    val animatedProgress = androidx.compose.animation.core.animateFloatAsState(
        targetValue = 1f,
        animationSpec = androidx.compose.animation.core.tween(600),
        label = "sparkline"
    ).value
    Canvas(modifier = Modifier.fillMaxWidth().height(32.dp)) {
        val w = size.width
        val h = size.height
        val pts = data
        if (pts.size < 2) return@Canvas
        val min = pts.minOrNull() ?: 0f
        val max = pts.maxOrNull() ?: 1f
        val range = if (max - min > 0) max - min else 1f
        val step = w / (pts.size - 1)
        val pathProgress = (pts.size * animatedProgress).toInt().coerceAtLeast(2)
        for (i in 0 until pathProgress - 1) {
            val x1 = i * step
            val y1 = h - (pts[i] - min) / range * (h - 4.dp.toPx()) - 2.dp.toPx()
            val x2 = (i + 1) * step
            val y2 = h - (pts[i + 1] - min) / range * (h - 4.dp.toPx()) - 2.dp.toPx()
            drawLine(
                color = colorScheme.primary,
                start = Offset(x1, y1.coerceIn(2.dp.toPx(), h - 2.dp.toPx())),
                end = Offset(x2, y2.coerceIn(2.dp.toPx(), h - 2.dp.toPx())),
                strokeWidth = 2.dp.toPx()
            )
        }
    }
}

@Composable
private fun RecentTransactionRow(transaction: TransactionRecordResponse) {
    val isDebit = transaction.direction.lowercase() == "debit"
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Row(
            Modifier.padding(12.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.merchant ?: transaction.category ?: "Transaction",
                    style = MaterialTheme.typography.titleSmall,
                    color = colorScheme.onSurface
                )
                Text(
                    transaction.txn_date,
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
            Text(
                "${if (isDebit) "-" else "+"}${formatCurrency(kotlin.math.abs(transaction.amount))}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDebit) com.example.monytix.ui.theme.ChartRed else com.example.monytix.ui.theme.Success
            )
        }
    }
}

@Composable
private fun SummaryCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = modifier.clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun AccountsTab(
    accounts: List<AccountItemResponse>,
    onRetry: () -> Unit
) {
    if (accounts.isEmpty()) {
        EmptyState(
            title = "No Accounts",
            subtitle = "Upload bank statements to see your accounts",
            onRetry = onRetry
        )
        return
    }
    val totalBalance = accounts.sumOf { it.balance }
    val maxBalance = accounts.maxOfOrNull { kotlin.math.abs(it.balance) } ?: 1.0
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Total Portfolio", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(4.dp))
            AnimatedCurrencyText(
                targetAmount = totalBalance,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = if (totalBalance >= 0) com.example.monytix.ui.theme.Success else com.example.monytix.ui.theme.ChartRed
            )
        }
        item { Spacer(Modifier.height(8.dp)) }
        item {
            Text("Your Accounts", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        items(accounts) { account ->
            val isNegative = account.balance < 0
            val balanceFraction = if (maxBalance > 0) (kotlin.math.abs(account.balance) / maxBalance).toFloat().coerceIn(0f, 1f) else 0f
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = if (isNegative) BorderStroke(1.dp, com.example.monytix.ui.theme.ChartRed.copy(alpha = 0.4f)) else null
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = when (account.account_type.uppercase()) {
                                "SAVINGS" -> "🏦"
                                "CHECKING" -> "💳"
                                "INVESTMENT" -> "📈"
                                "CREDIT" -> "💳"
                                else -> "💳"
                            },
                            style = MaterialTheme.typography.headlineMedium
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(account.bank_name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                                if (isNegative) {
                                    Spacer(Modifier.width(8.dp))
                                    Text("⚠ Overdraft Risk", style = MaterialTheme.typography.labelSmall, color = com.example.monytix.ui.theme.ChartRed)
                                }
                            }
                            Text(
                                "${account.account_type}${account.account_number?.let { " • $it" } ?: ""}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(formatCurrency(account.balance), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = if (isNegative) com.example.monytix.ui.theme.ChartRed else MaterialTheme.colorScheme.onSurface)
                    }
                    Spacer(Modifier.height(8.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(balanceFraction)
                                .background(if (isNegative) com.example.monytix.ui.theme.ChartRed else AccentPrimary, RoundedCornerShape(2.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SpendingTab(viewModel: HomeViewModel) {
    val colorScheme = MaterialTheme.colorScheme
    val categories = viewModel.transformCategorySpending()
    val totalSpending = categories.sumOf { it.amount }
    val vsLastMonth = viewModel.spendingVsLastMonthPct()
    val projected = viewModel.projectedMonthEndSpending()
    val sparklineData = viewModel.sparklineData()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.15f))
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("This Month's Spending", style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurfaceVariant)
                    Spacer(Modifier.height(4.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        Text(formatCurrency(totalSpending), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = com.example.monytix.ui.theme.ChartRed)
                        vsLastMonth?.let { pct ->
                            Text(
                                "${if (pct >= 0) "↑" else "↓"} ${kotlin.math.abs(pct).toInt()}% vs last month",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pct >= 0) com.example.monytix.ui.theme.ChartRed else com.example.monytix.ui.theme.Success
                            )
                        }
                    }
                    projected?.let { proj ->
                        Spacer(Modifier.height(4.dp))
                        Text("Projected: ${formatCurrency(proj)} by month end", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                    }
                    if (sparklineData.isNotEmpty()) {
                        Spacer(Modifier.height(12.dp))
                        MiniSparkline(data = sparklineData)
                    }
                }
            }
        }
        if (categories.isEmpty()) {
            item {
                EmptyState(title = "No Spending Data", subtitle = "Upload statements to see your spending breakdown")
            }
        } else {
            item {
                Text("Spending by Category", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = colorScheme.onBackground)
            }
            items(categories) { cat ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surfaceVariant),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(cat.category, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(formatCurrency(cat.amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .background(MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((cat.percentage / 100.0).toFloat())
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                                )
                            }
                            Spacer(Modifier.width(8.dp))
                            Text("${cat.percentage.toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Text("${cat.transactionCount} transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalsTab(viewModel: HomeViewModel) {
    val goals = viewModel.transformGoals()
    if (goals.isEmpty()) {
        EmptyState(title = "No Goals", subtitle = "Create your first financial goal to get started")
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("Your Goals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        items(goals) { goal ->
            val progress = if (goal.targetAmount > 0) (goal.savedAmount / goal.targetAmount * 100).coerceIn(0.0, 100.0) else 0.0
            val remaining = (goal.targetAmount - goal.savedAmount).coerceAtLeast(0.0)
            val pctLeft = 100 - progress
            val goalHealthColor = when {
                progress >= 80 -> AccentPrimary
                progress >= 40 -> MaterialTheme.colorScheme.primary
                else -> com.example.monytix.ui.theme.Warning
            }
            val goalGlowColor = when {
                progress >= 80 -> AccentPrimary.copy(alpha = 0.15f)
                progress < 40 -> com.example.monytix.ui.theme.ChartRed.copy(alpha = 0.08f)
                else -> Color.Transparent
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, goalHealthColor.copy(alpha = 0.3f))
            ) {
                Box(modifier = Modifier.background(goalGlowColor)) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(goal.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            if (goal.isActive) {
                                Text("Active", style = MaterialTheme.typography.labelSmall, color = com.example.monytix.ui.theme.Success)
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            if (pctLeft > 0) "🔥 ${pctLeft.toInt()}% left to finish" else "✓ Complete",
                            style = MaterialTheme.typography.bodySmall,
                            color = goalHealthColor
                        )
                        Spacer(Modifier.height(8.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Progress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text("${progress.toInt()}%", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = goalHealthColor)
                        }
                        Spacer(Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(12.dp)
                                .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.3f), RoundedCornerShape(6.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .fillMaxWidth((progress / 100.0).toFloat())
                                    .background(goalHealthColor, RoundedCornerShape(6.dp))
                            )
                        }
                        Spacer(Modifier.height(12.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(goal.savedAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(goal.targetAmount), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text(formatCurrency(remaining), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AIInsightTab(viewModel: HomeViewModel) {
    val insights = viewModel.generateAiInsights()
    if (insights.isEmpty()) {
        AiAnalyzingPlaceholder()
        return
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("AI Insights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        }
        items(insights) { insight ->
            val (borderColor, glowColor) = when (insight.type) {
                "optimization" -> com.example.monytix.ui.theme.Success to com.example.monytix.ui.theme.Success.copy(alpha = 0.15f)
                "risk" -> com.example.monytix.ui.theme.Warning to com.example.monytix.ui.theme.ChartRed.copy(alpha = 0.1f)
                "pattern" -> com.example.monytix.ui.theme.Info to com.example.monytix.ui.theme.Info.copy(alpha = 0.1f)
                "goal_progress" -> com.example.monytix.ui.theme.Success to com.example.monytix.ui.theme.Success.copy(alpha = 0.15f)
                else -> AccentSecondary to AccentSecondary.copy(alpha = 0.1f)
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, borderColor.copy(alpha = 0.4f))
            ) {
                Box(modifier = Modifier.background(glowColor)) {
                    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.Top) {
                        Text(
                            when (insight.type) {
                                "optimization" -> "🟢"
                                "risk" -> "🟡"
                                "pattern" -> "🔵"
                                "spending_alert" -> "🟡"
                                "goal_progress" -> "🟢"
                                "budget_tip" -> "🟡"
                                "on_track" -> "✨"
                                else -> "✨"
                            },
                            style = MaterialTheme.typography.headlineSmall
                        )
                        Spacer(Modifier.width(16.dp))
                        Column(Modifier.weight(1f)) {
                            Text(insight.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = borderColor)
                            Spacer(Modifier.height(4.dp))
                            Text(insight.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                            if (insight.confidence != null && insight.confidence > 0.8f) {
                                Spacer(Modifier.height(6.dp))
                                Text("${(insight.confidence * 100).toInt()}% confidence", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AiAnalyzingPlaceholder() {
    val colorScheme = MaterialTheme.colorScheme
    val infiniteTransition = rememberInfiniteTransition(label = "shimmer")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shimmer_alpha"
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.Lightbulb,
            contentDescription = null,
            tint = AccentSecondary.copy(alpha = alpha),
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.height(16.dp))
        Text(
            "AI is analyzing your patterns",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Check back soon for personalized insights.",
            style = MaterialTheme.typography.bodyMedium,
            color = colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun EmptyState(
    title: String,
    subtitle: String,
    onRetry: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onBackground)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        onRetry?.let { retry ->
            Spacer(Modifier.height(16.dp))
            Button(onClick = retry, colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)) {
                Text("Retry")
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    val formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(abs.toLong())
    return if (amount < 0) "-₹$formatted" else "₹$formatted"
}
