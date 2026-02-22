package com.example.monytix.spendsense

import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monytix.data.AccountItemResponse
import com.example.monytix.data.CategoryResponse
import com.example.monytix.data.CategorySpendKpi
import com.example.monytix.data.InsightsResponse
import com.example.monytix.data.TransactionRecordResponse
import com.example.monytix.ui.theme.AccentPrimary
import com.example.monytix.ui.theme.AccentSecondary
import com.example.monytix.ui.theme.ChartOrange
import com.example.monytix.ui.theme.ChartRed
import com.example.monytix.ui.theme.Success
import com.example.monytix.ui.theme.GlassCard
import com.example.monytix.ui.theme.HeroCardGlow
import com.example.monytix.ui.theme.SurfaceElevated
import com.example.monytix.ui.theme.Info
import com.example.monytix.ui.theme.Success
import com.example.monytix.ui.theme.TextSecondary
import com.example.monytix.spendsense.components.InteractivePieChart
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SpendSenseScreen(
    viewModel: SpendSenseViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onLaunchFilePicker: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(SpendSenseTab.CATEGORIES) }
    var showManualAddByRequest by remember { mutableStateOf(false) }
    val pendingUpload by PendingUploadHolder.state
    val pendingManualAdd by PendingManualAddHolder.state
    val colorScheme = MaterialTheme.colorScheme

    LaunchedEffect(pendingUpload) {
        if (pendingUpload != null) selectedTab = SpendSenseTab.TRANSACTIONS
    }
    LaunchedEffect(pendingManualAdd) {
        if (pendingManualAdd) {
            selectedTab = SpendSenseTab.TRANSACTIONS
            showManualAddByRequest = true
            PendingManualAddHolder.state.value = false
        }
    }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.goalUpdatedToast) {
        uiState.goalUpdatedToast?.let { msg ->
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearGoalUpdatedToast()
        }
    }
    LaunchedEffect(Unit) {
        viewModel.loadTransactions(1, append = false)
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        topBar = {
            TopAppBar(
                title = { Text("SpendSense", color = colorScheme.onBackground) },
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
    ) { innerPadding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            WelcomeBanner(username = uiState.userEmail ?: "User")
            TabBar(selectedTab = selectedTab, onTabSelected = { selectedTab = it })
            when (selectedTab) {
                SpendSenseTab.CATEGORIES -> CategoriesTab(
                    viewModel = viewModel,
                    onLaunchFilePicker = onLaunchFilePicker,
                    onSwitchToTransactions = { selectedTab = SpendSenseTab.TRANSACTIONS },
                    onShowManualAdd = { selectedTab = SpendSenseTab.TRANSACTIONS; showManualAddByRequest = true }
                )
                SpendSenseTab.TRANSACTIONS -> TransactionsTab(
                    viewModel = viewModel,
                    onLaunchFilePicker = onLaunchFilePicker,
                    showManualAddByRequest = showManualAddByRequest,
                    onClearManualAddRequest = { showManualAddByRequest = false }
                )
                SpendSenseTab.INSIGHTS -> InsightsTab(viewModel = viewModel)
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
            text = "See where your money really goes.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            color = colorScheme.onBackground.copy(alpha = 0.95f)
        )
        Text(
            text = "Automatically categorized insights. Welcome back, $displayName!",
            style = MaterialTheme.typography.bodySmall,
            color = colorScheme.onBackground.copy(alpha = 0.6f)
        )
        Text(
            text = "Powered by Monytix Intelligence",
            style = MaterialTheme.typography.labelSmall,
            color = TextSecondary
        )
    }
}

@Composable
private fun FilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    onClear: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .background(
                if (isSelected) colorScheme.primary else GlassCard,
                RoundedCornerShape(20.dp)
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (isSelected) colorScheme.onPrimary else colorScheme.onSurface.copy(alpha = 0.9f)
        )
        if (isSelected && onClear != null) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Clear",
                modifier = Modifier
                    .size(16.dp)
                    .clickable(onClick = onClear),
                tint = colorScheme.onPrimary
            )
        }
    }
}

private enum class SpendSenseTab(val label: String) {
    CATEGORIES("Categories"),
    TRANSACTIONS("Transactions"),
    INSIGHTS("Insights")
}

@Composable
private fun TabBar(selectedTab: SpendSenseTab, onTabSelected: (SpendSenseTab) -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorScheme.background)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        SpendSenseTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            Box(
                modifier = Modifier
                    .background(
                        if (selected) AccentPrimary.copy(alpha = 0.25f) else Color.Transparent,
                        RoundedCornerShape(12.dp)
                    )
            ) {
                TextButton(
                    onClick = { onTabSelected(tab) },
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = if (selected) colorScheme.primary else colorScheme.onBackground.copy(alpha = 0.6f)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(tab.label, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun CategoriesTab(
    viewModel: SpendSenseViewModel,
    onLaunchFilePicker: () -> Unit = {},
    onSwitchToTransactions: () -> Unit = {},
    onShowManualAdd: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadKpis(null)
        viewModel.loadInsights()
    }

    Box(Modifier.fillMaxSize()) {
        if (uiState.isLoading && uiState.kpis == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else {
            val kpis = uiState.kpis
            if (kpis == null || (kpis.month == null && kpis.top_categories.isEmpty())) {
                EmptyState(
                    title = "No KPI data available",
                    subtitle = "Upload transaction statements to see spending insights"
                )
            } else {
                categoriesTabContent(viewModel, uiState, kpis)
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = { onLaunchFilePicker(); onSwitchToTransactions() },
                containerColor = SurfaceElevated,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Upload PDF")
            }
            FloatingActionButton(
                onClick = onShowManualAdd,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    }
}

@Composable
private fun categoriesTabContent(
    viewModel: SpendSenseViewModel,
    uiState: SpendSenseUiState,
    kpis: com.example.monytix.data.KpiResponse
) {
    val income = kpis.income_amount ?: 0.0
    val needs = kpis.needs_amount ?: 0.0
    val wants = kpis.wants_amount ?: 0.0
    val expenses = needs + wants
    val savingsRate = if (income > 0) ((income - expenses) / income * 100) else 0.0

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            if (uiState.availableMonths.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Month:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Spacer(Modifier.width(12.dp))
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        uiState.availableMonths.forEach { month ->
                            val selected = month == uiState.selectedMonth
                            TextButton(
                                onClick = { viewModel.setSelectedMonth(if (selected) null else month) },
                                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                    contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                )
                            ) {
                                Text(formatMonthDisplay(month), style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    IconButton(onClick = { viewModel.refreshKpis() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh KPIs", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        item {
            Text("Financial Health", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .drawBehind {
                        val center = Offset(size.width / 2, size.height / 2)
                        val radius = size.maxDimension
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(AccentPrimary.copy(alpha = 0.06f), Color.Transparent),
                                center = center,
                                radius = radius
                            ),
                            center = center,
                            radius = radius
                        )
                    }
            ) {
                FinancialHealthScoreCard(
                    score = viewModel.financialHealthScore(),
                    vsLastMonth = viewModel.healthScoreVsLastMonth(),
                    aiInsight = viewModel.generateSpendSenseInsight(),
                    viewModel = viewModel
                )
            }
        }
        viewModel.dailySummary()?.let { summary ->
            item {
                DailySummaryCard(summary = summary)
            }
        }
        (viewModel.projectedSavingsRate() ?: viewModel.projectedMonthlySpending())?.let { text ->
            item {
                ProjectionCard(text = text, viewModel = viewModel)
            }
        }
        item {
            ConfidenceLayer(viewModel = viewModel)
        }
        item {
            Row(Modifier.fillMaxWidth().alpha(0.95f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedVisibility(visible = true, enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 100))) {
                    KpiCard(modifier = Modifier.weight(1f), title = "Savings Rate", value = "${savingsRate.toInt()}%", color = Success)
                }
                AnimatedVisibility(visible = true, enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 150))) {
                    KpiCard(modifier = Modifier.weight(1f), title = "Income", value = formatCurrency(income), color = Success)
                }
                AnimatedVisibility(visible = true, enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 200))) {
                    KpiCard(modifier = Modifier.weight(1f), title = "Expenses", value = formatCurrency(expenses), color = ChartRed)
                }
            }
        }
        item {
            Text("Key Metrics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.85f))
        }
        item {
            Row(Modifier.fillMaxWidth().alpha(0.9f), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                AnimatedVisibility(visible = true, enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 250))) {
                    KpiCard(modifier = Modifier.weight(1f), title = "Needs", value = formatCurrency(needs), color = ChartOrange)
                }
                AnimatedVisibility(visible = true, enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 300))) {
                    KpiCard(modifier = Modifier.weight(1f), title = "Wants", value = formatCurrency(wants), color = com.example.monytix.ui.theme.ChartPurple)
                }
            }
        }
        item {
            AnimatedVisibility(visible = true, enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 350))) {
                KpiCard(modifier = Modifier.fillMaxWidth().alpha(0.9f), title = "Assets", value = formatCurrency(kpis.assets_amount ?: 0.0), color = Info)
            }
        }
        if (kpis.top_categories.isNotEmpty()) {
            item {
                Text("Top Categories", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            items(kpis.top_categories, key = { it.category_code }) { cat ->
                CategoryCard(category = cat)
            }
        }
    }
}

@Composable
private fun FinancialHealthScoreCard(
    score: Int,
    vsLastMonth: Double,
    aiInsight: String?,
    viewModel: SpendSenseViewModel
) {
    var showScoreBreakdown by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    val ringColor = when {
        score >= 80 -> Success
        score >= 60 -> AccentPrimary
        score >= 40 -> ChartOrange
        else -> ChartRed
    }
    val statusText = when {
        score >= 80 -> "Strong position this month"
        score >= 60 -> "Good shape"
        score >= 40 -> "Room to improve"
        else -> "Needs attention"
    }
    val animScore = remember { Animatable(0f) }
    LaunchedEffect(score) {
        animScore.animateTo(score.toFloat(), animationSpec = tween(800))
    }
    val animatedScore = animScore.value
    val infiniteTransition = rememberInfiniteTransition(label = "ring")
    val strokeAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(1500), repeatMode = androidx.compose.animation.core.RepeatMode.Reverse),
        label = "stroke"
    )
    val emotionalModifier = when {
        score >= 80 -> Modifier.drawBehind {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.maxDimension
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(Success.copy(alpha = 0.08f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                center = center,
                radius = radius
            )
        }
        score < 50 -> Modifier.drawBehind {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.maxDimension
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(ChartRed.copy(alpha = 0.08f), Color.Transparent),
                    center = center,
                    radius = radius
                ),
                center = center,
                radius = radius
            )
        }
        else -> Modifier
    }
    Card(
        modifier = Modifier.fillMaxWidth().then(emotionalModifier),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(24.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.5.dp, ringColor.copy(alpha = strokeAlpha * 0.5f))
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(HeroCardGlow, Color(0x0FFFFFFF), ringColor.copy(alpha = 0.1f))
                    )
                )
                .padding(24.dp)
        ) {
            Column {
                Text("FINANCIAL HEALTH", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(80.dp)
                            .pointerInput(Unit) {
                                detectTapGestures(onLongPress = { showScoreBreakdown = true })
                            }
                    ) {
                        Canvas(modifier = Modifier.fillMaxSize()) {
                            val strokeWidth = 6.dp.toPx()
                            val radius = (size.minDimension - strokeWidth) / 2
                            val center = Offset(size.width / 2, size.height / 2)
                            drawCircle(
                                color = colorScheme.outline.copy(alpha = 0.2f),
                                radius = radius,
                                center = center,
                                style = Stroke(width = strokeWidth)
                            )
                            drawArc(
                                color = ringColor.copy(alpha = strokeAlpha),
                                startAngle = 270f,
                                sweepAngle = animatedScore / 100f * 360f,
                                useCenter = false,
                                topLeft = center - Offset(radius, radius),
                                size = Size(radius * 2, radius * 2),
                                style = Stroke(width = strokeWidth)
                            )
                        }
                        Text(
                            "${animatedScore.toInt()}",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text(statusText, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = ringColor)
                        Text("Risk: ${viewModel.riskLevel(score)} • Optimization: ${viewModel.optimizationPotential(score)}", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
                        AnimatedVisibility(
                            visible = true,
                            enter = fadeIn(animationSpec = tween(durationMillis = 400, delayMillis = 300))
                        ) {
                            Text(
                                "${if (vsLastMonth >= 0) "↑" else "↓"} ${kotlin.math.abs(vsLastMonth).toInt()}% vs last month",
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                aiInsight?.let { insight ->
                    Spacer(Modifier.height(16.dp))
                    AnimatedVisibility(
                        visible = true,
                        enter = slideInVertically(animationSpec = tween(400), initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(400))
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text("💡", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.width(8.dp))
                            Text(insight, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurface.copy(alpha = 0.9f))
                        }
                    }
                }
            }
        }
    }
    if (showScoreBreakdown) {
        AlertDialog(
            onDismissRequest = { showScoreBreakdown = false },
            title = { Text("Score breakdown") },
            text = {
                Column {
                    viewModel.scoreBreakdown().forEach { (label, value) ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Spacer(Modifier.height(4.dp))
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showScoreBreakdown = false }) { Text("Got it") } }
        )
    }
}

@Composable
private fun DailySummaryCard(summary: SpendSenseViewModel.DailySummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.15f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Today", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("• ${summary.credits} Credits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("• ${summary.debits} Debits", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Net: ${if (summary.net >= 0) "+" else ""}${formatCurrency(summary.net)}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium, color = if (summary.net >= 0) Success else ChartRed)
            }
        }
    }
}

@Composable
private fun ConfidenceLayer(viewModel: SpendSenseViewModel) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Financial Health: ${viewModel.financialHealthConfidence()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Text("Categorization: ${viewModel.categorizationConfidence()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
        Text("•", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f))
        Text("Projection: ${viewModel.projectionConfidence()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
    }
}

@Composable
private fun ProjectionCard(text: String, viewModel: SpendSenseViewModel) {
    var expanded by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { expanded = !expanded },
        colors = CardDefaults.cardColors(containerColor = AccentSecondary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, AccentSecondary.copy(alpha = 0.3f))
    ) {
        Column(Modifier.padding(12.dp)) {
            Text(text = text, style = MaterialTheme.typography.bodySmall, color = AccentSecondary)
            Spacer(Modifier.height(4.dp))
            Text("Based on last ${viewModel.projectionDaysBased()} days • Confidence: ${viewModel.projectionConfidence()}%", style = MaterialTheme.typography.labelSmall, color = colorScheme.onSurfaceVariant.copy(alpha = 0.8f))
            Text("Tap to see forecast graph", style = MaterialTheme.typography.labelSmall, color = AccentSecondary.copy(alpha = 0.8f))
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically(animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)),
                exit = shrinkVertically(animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
            ) {
                Column(Modifier.padding(top = 12.dp)) {
                    val data = viewModel.projectionForecastData()
                    if (data.isNotEmpty()) {
                        val maxVal = data.maxOrNull() ?: 1f
                        Box(modifier = Modifier.fillMaxWidth().height(80.dp)) {
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val path = Path()
                                val w = size.width
                                val h = size.height
                                val step = if (data.size > 1) w / (data.size - 1) else w
                                data.forEachIndexed { i, v ->
                                    val x = i * step
                                    val y = h - (v / maxVal * h * 0.9f)
                                    if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                                }
                                drawPath(path, AccentSecondary.copy(alpha = 0.8f), style = Stroke(width = 2.dp.toPx()))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun KpiCard(
    title: String,
    value: String,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun CategoryCard(category: CategorySpendKpi) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(category.category_name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                Text("${category.txn_count} transactions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(formatCurrency(category.spend_amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                category.delta_pct?.let { delta ->
                    Text(
                        "${if (delta >= 0) "+" else ""}${delta.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (delta >= 0) Color(0xFFE53935) else Color(0xFF4CAF50)
                    )
                }
            }
        }
    }
}

@Composable
private fun TransactionsTab(
    viewModel: SpendSenseViewModel,
    onLaunchFilePicker: () -> Unit = {},
    showManualAddByRequest: Boolean = false,
    onClearManualAddRequest: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()
    var pendingUpload by PendingUploadHolder.state
    var showManualAdd by remember { mutableStateOf(false) }

    LaunchedEffect(showManualAddByRequest) {
        if (showManualAddByRequest) {
            showManualAdd = true
            onClearManualAddRequest()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadTransactions(1, append = false)
        viewModel.loadInsights()
    }

    val filters = uiState.transactionFilters
    val hasAnyFilter = filters.categoryCode != null || filters.subcategoryCode != null ||
        filters.channel != null || filters.direction != null || filters.bankCode != null ||
        filters.startDate != null || filters.endDate != null

    var selectedTransaction by remember { mutableStateOf<TransactionRecordResponse?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var channelExpanded by remember { mutableStateOf(false) }
    var directionExpanded by remember { mutableStateOf(false) }
    var bankExpanded by remember { mutableStateOf(false) }
    var dateExpanded by remember { mutableStateOf(false) }

    Box(Modifier.fillMaxSize()) {
    Column(Modifier.fillMaxSize()) {
        Row(
            Modifier
                .horizontalScroll(rememberScrollState())
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilterChip(
                label = "Category",
                isSelected = filters.categoryCode != null || filters.subcategoryCode != null || categoryExpanded,
                onClick = { categoryExpanded = !categoryExpanded },
                onClear = {
                    viewModel.setTransactionFilters(filters.copy(categoryCode = null, subcategoryCode = null))
                    categoryExpanded = false
                }
            )
            FilterChip(
                label = "Channel",
                isSelected = filters.channel != null || channelExpanded,
                onClick = { channelExpanded = !channelExpanded },
                onClear = { viewModel.setTransactionFilters(filters.copy(channel = null)); channelExpanded = false }
            )
            FilterChip(
                label = "Direction",
                isSelected = filters.direction != null || directionExpanded,
                onClick = { directionExpanded = !directionExpanded },
                onClear = { viewModel.setTransactionFilters(filters.copy(direction = null)); directionExpanded = false }
            )
            FilterChip(
                label = "Bank",
                isSelected = filters.bankCode != null || bankExpanded,
                onClick = { bankExpanded = !bankExpanded },
                onClear = { viewModel.setTransactionFilters(filters.copy(bankCode = null)); bankExpanded = false }
            )
            FilterChip(
                label = "Date",
                isSelected = filters.startDate != null || filters.endDate != null || dateExpanded,
                onClick = { dateExpanded = !dateExpanded },
                onClear = {
                    viewModel.setTransactionFilters(filters.copy(startDate = null, endDate = null))
                    dateExpanded = false
                }
            )
            if (hasAnyFilter) {
                FilterChip(
                    label = "Clear All",
                    isSelected = false,
                    onClick = {
                        viewModel.setTransactionFilters(TransactionFilters())
                        categoryExpanded = false
                        channelExpanded = false
                        directionExpanded = false
                        bankExpanded = false
                        dateExpanded = false
                    }
                )
            }
        }

        if (categoryExpanded) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    label = "All",
                    isSelected = filters.categoryCode == null && filters.subcategoryCode == null,
                    onClick = {
                        viewModel.setTransactionFilters(filters.copy(categoryCode = null, subcategoryCode = null))
                    }
                )
                uiState.categories.forEach { cat ->
                    FilterChip(
                        label = cat.category_name,
                        isSelected = filters.categoryCode == cat.category_code,
                        onClick = {
                            viewModel.setTransactionFilters(filters.copy(categoryCode = cat.category_code, subcategoryCode = null))
                        }
                    )
                }
            }
        }

        if (channelExpanded) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    label = "All",
                    isSelected = filters.channel == null,
                    onClick = { viewModel.setTransactionFilters(filters.copy(channel = null)) }
                )
                uiState.channels.forEach { ch ->
                    FilterChip(
                        label = ch,
                        isSelected = filters.channel == ch,
                        onClick = { viewModel.setTransactionFilters(filters.copy(channel = ch)) }
                    )
                }
            }
        }

        if (directionExpanded) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(label = "All", isSelected = filters.direction == null, onClick = { viewModel.setTransactionFilters(filters.copy(direction = null)) })
                FilterChip(label = "Debit", isSelected = filters.direction == "debit", onClick = { viewModel.setTransactionFilters(filters.copy(direction = "debit")) })
                FilterChip(label = "Credit", isSelected = filters.direction == "credit", onClick = { viewModel.setTransactionFilters(filters.copy(direction = "credit")) })
            }
        }

        if (bankExpanded) {
            val uniqueBanks = uiState.accounts.distinctBy { it.bank_code }.filter { it.bank_code.isNotBlank() }
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(label = "All", isSelected = filters.bankCode == null, onClick = { viewModel.setTransactionFilters(filters.copy(bankCode = null)) })
                uniqueBanks.forEach { acc ->
                    FilterChip(
                        label = acc.bank_name.ifBlank { acc.bank_code },
                        isSelected = filters.bankCode == acc.bank_code,
                        onClick = { viewModel.setTransactionFilters(filters.copy(bankCode = acc.bank_code)) }
                    )
                }
            }
        }

        if (dateExpanded) {
            val today = LocalDate.now()
            val thisMonthStart = today.withDayOfMonth(1)
            val thisMonthEnd = today
            val lastMonth = today.minusMonths(1)
            val lastMonthStart = lastMonth.withDayOfMonth(1)
            val lastMonthEnd = lastMonth.withDayOfMonth(lastMonth.lengthOfMonth())
            Row(
                Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                FilterChip(
                    label = "All Time",
                    isSelected = filters.startDate == null && filters.endDate == null,
                    onClick = { viewModel.setTransactionFilters(filters.copy(startDate = null, endDate = null)) }
                )
                FilterChip(
                    label = "Today",
                    isSelected = filters.startDate == today.toString() && filters.endDate == today.toString(),
                    onClick = { viewModel.setTransactionFilters(filters.copy(startDate = today.toString(), endDate = today.toString())) }
                )
                FilterChip(
                    label = "This Month",
                    isSelected = filters.startDate == thisMonthStart.toString() && filters.endDate == thisMonthEnd.toString(),
                    onClick = { viewModel.setTransactionFilters(filters.copy(startDate = thisMonthStart.toString(), endDate = thisMonthEnd.toString())) }
                )
                FilterChip(
                    label = "Last Month",
                    isSelected = filters.startDate == lastMonthStart.toString() && filters.endDate == lastMonthEnd.toString(),
                    onClick = { viewModel.setTransactionFilters(filters.copy(startDate = lastMonthStart.toString(), endDate = lastMonthEnd.toString())) }
                )
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search...", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                    unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                    cursorColor = MaterialTheme.colorScheme.primary
                )
            )
            Button(
                onClick = { viewModel.loadTransactions(1, append = false) },
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Search")
            }
        }

        if (uiState.isLoading && uiState.transactions.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (uiState.transactions.isEmpty()) {
            EmptyState(
                title = "No transactions found",
                subtitle = "Upload statements or add manually",
                onRetry = { viewModel.loadTransactions(1, append = false) }
            )
        } else {
            // Deduplicate transactions by txn_id to avoid duplicate keys
            val uniqueTransactions = uiState.transactions.distinctBy { it.txn_id }
            val grouped = uniqueTransactions.groupBy { txn ->
                try {
                    val d = java.time.LocalDate.parse(txn.txn_date)
                    d.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()))
                } catch (_: Exception) {
                    txn.txn_date
                }
            }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "confidence_header") {
                    Row(
                        Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Auto-categorized with ${viewModel.categorizationConfidence()}% confidence",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentSecondary.copy(alpha = 0.9f)
                        )
                    }
                }
                grouped.forEach { (date, txns) ->
                    item(key = "date_header_$date") {
                        Text(
                            "$date (${txns.size})",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                    itemsIndexed(txns, key = { index, txn -> "txn_${txn.txn_id}_${date}_$index" }) { _, txn ->
                        TransactionRow(
                            transaction = txn,
                            viewModel = viewModel,
                            onClick = { selectedTransaction = txn }
                        )
                    }
                }
                if (uiState.transactionsTotal > uniqueTransactions.size) {
                    item(key = "load_more_button") {
                        Button(
                            onClick = { viewModel.loadTransactions((uniqueTransactions.size / 25) + 2, append = true) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = GlassCard, contentColor = MaterialTheme.colorScheme.onSurface)
                        ) {
                            Text("Load More")
                        }
                    }
                }
            }
        }
    }

        // Floating action buttons (Upload PDF + Add Transaction) - overlay at bottom-right
        Column(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = 16.dp, bottom = 80.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            FloatingActionButton(
                onClick = {
                    Log.d("MonytixUpload", "Upload FAB clicked, launching file picker")
                    onLaunchFilePicker()
                },
                containerColor = SurfaceElevated,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                Icon(Icons.Default.Upload, contentDescription = "Upload PDF")
            }
            FloatingActionButton(
                onClick = { showManualAdd = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Transaction")
            }
        }
    }

    if (showManualAdd) {
        ManualAddDialog(
            viewModel = viewModel,
            onDismiss = { showManualAdd = false },
            onSubmit = { date, merchant, desc, amount, direction, catCode, subCode, channel ->
                viewModel.createTransaction(date, merchant, desc, amount, direction, catCode, subCode, channel)
                showManualAdd = false
            }
        )
    }

    pendingUpload?.let { (bytes, filename) ->
        Log.d("MonytixUpload", "TransactionsTab: showing UploadStatementDialog filename=$filename bytes=${bytes.size}")
        UploadStatementDialog(
            filename = filename,
            fileBytes = bytes,
            viewModel = viewModel,
            onDismiss = { PendingUploadHolder.state.value = null },
            onSuccess = { PendingUploadHolder.state.value = null }
        )
    }

    selectedTransaction?.let { txn ->
        TransactionDetailDialog(
            transaction = txn,
            viewModel = viewModel,
            onDismiss = { selectedTransaction = null },
            onUpdated = { viewModel.loadTransactions(1, append = false); selectedTransaction = null },
            onDeleted = { viewModel.loadTransactions(1, append = false); selectedTransaction = null }
        )
    }
}

@Composable
private fun TransactionDetailDialog(
    transaction: TransactionRecordResponse,
    viewModel: SpendSenseViewModel,
    onDismiss: () -> Unit,
    onUpdated: () -> Unit,
    onDeleted: () -> Unit
) {
    var isEditing by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var merchant by remember { mutableStateOf(transaction.merchant ?: "") }
    var categoryCode by remember { mutableStateOf(transaction.category ?: "") }
    var subcategoryCode by remember { mutableStateOf(transaction.subcategory ?: "") }
    var channel by remember { mutableStateOf(transaction.channel ?: "") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var subcategoryExpanded by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(transaction.txn_id) {
        merchant = transaction.merchant ?: ""
        categoryCode = transaction.category ?: ""
        subcategoryCode = transaction.subcategory ?: ""
        channel = transaction.channel ?: ""
        viewModel.loadSubcategories(categoryCode.takeIf { it.isNotBlank() })
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete transaction?", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTransaction(transaction.txn_id)
                        showDeleteConfirm = false
                        onDeleted()
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = ChartRed, contentColor = Color.White)
                ) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel", color = MaterialTheme.colorScheme.onSurface) } }
        )
    }

    val colorScheme = MaterialTheme.colorScheme
    val isDebit = transaction.direction.lowercase() == "debit"

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(horizontal = 20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceElevated),
            shape = RoundedCornerShape(24.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.15f))
        ) {
            Column(Modifier.padding(24.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            if (isEditing) "Edit Transaction" else "Transaction Details",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                        if (isEditing) {
                            Text(
                                "Update category, merchant, or channel",
                                style = MaterialTheme.typography.labelSmall,
                                color = TextSecondary
                            )
                        }
                    }
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                val displayCategory = if (isEditing) {
                    uiState.categories.find { it.category_code == categoryCode }?.category_name ?: categoryCode.ifBlank { transaction.category } ?: "Uncategorized"
                } else transaction.category ?: "Uncategorized"
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .background(
                                (if (isDebit) ChartRed else Success).copy(alpha = 0.2f),
                                RoundedCornerShape(16.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            categoryToIcon(if (isEditing) categoryCode else transaction.category),
                            style = MaterialTheme.typography.headlineSmall
                        )
                    }
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (isEditing) merchant.ifBlank { "Unknown" } else (transaction.merchant ?: "Unknown"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.onSurface
                        )
                        Text(
                            displayCategory,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                        Text(
                            "${if (isDebit) "-" else "+"}${formatCurrency(kotlin.math.abs(transaction.amount))}",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isDebit) ChartRed else Success
                        )
                    }
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    transaction.confidence?.let { conf ->
                        Text(
                            "Auto-categorized ${(conf * 100).toInt()}%",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentSecondary,
                            modifier = Modifier
                                .background(AccentSecondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    if (viewModel.isRecurringTransaction(transaction.merchant)) {
                        Text(
                            "Recurring",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentSecondary,
                            modifier = Modifier
                                .background(AccentSecondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                    val catStr = (transaction.category ?: "").lowercase()
                    if (viewModel.isRecurringTransaction(transaction.merchant) && (catStr.contains("emi") || catStr.contains("loan"))) {
                        Text(
                            "EMI series",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentSecondary,
                            modifier = Modifier
                                .background(AccentSecondary.copy(alpha = 0.2f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
                Spacer(Modifier.height(20.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .then(if (isEditing) Modifier.heightIn(max = 380.dp) else Modifier),
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, colorScheme.outline.copy(alpha = 0.08f))
                ) {
                    if (!isEditing) {
                        Column(Modifier.padding(16.dp)) {
                            DetailRow("Date", transaction.txn_date)
                            DetailRow("Merchant", transaction.merchant ?: "-")
                            DetailRow("Category", transaction.category ?: "-")
                            DetailRow("Subcategory", transaction.subcategory ?: "-")
                            DetailRow("Channel", transaction.channel ?: "-")
                            DetailRow("Direction", transaction.direction)
                            DetailRow("Bank", transaction.bank_code ?: "-")
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp)
                        ) {
                            Text("Merchant", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            OutlinedTextField(
                                value = merchant,
                                onValueChange = { merchant = it },
                                modifier = Modifier.fillMaxWidth(),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = colorScheme.onSurface,
                                    unfocusedTextColor = colorScheme.onSurface,
                                    focusedBorderColor = AccentPrimary,
                                    unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(12.dp)
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Category", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = uiState.categories.find { it.category_code == categoryCode }?.category_name ?: categoryCode.ifBlank { "Select category" },
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { categoryExpanded = true },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Expand",
                                            modifier = Modifier.clickable { categoryExpanded = true }
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = colorScheme.onSurface,
                                        unfocusedTextColor = colorScheme.onSurface,
                                        disabledTextColor = colorScheme.onSurface,
                                        focusedBorderColor = AccentPrimary,
                                        unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                DropdownMenu(
                                    expanded = categoryExpanded,
                                    onDismissRequest = { categoryExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    uiState.categories.forEach { cat ->
                                        DropdownMenuItem(
                                            text = { Text(cat.category_name) },
                                            onClick = {
                                                categoryCode = cat.category_code
                                                viewModel.loadSubcategories(cat.category_code)
                                                subcategoryCode = ""
                                                categoryExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Subcategory", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                OutlinedTextField(
                                    value = uiState.subcategories.find { it.subcategory_code == subcategoryCode }?.subcategory_name ?: subcategoryCode.ifBlank { "Select subcategory" },
                                    onValueChange = {},
                                    readOnly = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable(enabled = uiState.subcategories.isNotEmpty()) { subcategoryExpanded = true },
                                    trailingIcon = {
                                        Icon(
                                            Icons.Default.ArrowDropDown,
                                            contentDescription = "Expand",
                                            modifier = Modifier.clickable(enabled = uiState.subcategories.isNotEmpty()) { subcategoryExpanded = true }
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = colorScheme.onSurface,
                                        unfocusedTextColor = colorScheme.onSurface,
                                        disabledTextColor = colorScheme.onSurface,
                                        focusedBorderColor = AccentPrimary,
                                        unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.3f)
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                DropdownMenu(
                                    expanded = subcategoryExpanded,
                                    onDismissRequest = { subcategoryExpanded = false },
                                    modifier = Modifier.fillMaxWidth(0.9f)
                                ) {
                                    if (uiState.subcategories.isEmpty()) {
                                        DropdownMenuItem(
                                            text = { Text("No subcategories", color = TextSecondary) },
                                            onClick = { subcategoryExpanded = false }
                                        )
                                    } else {
                                        uiState.subcategories.forEach { sub ->
                                            DropdownMenuItem(
                                                text = { Text(sub.subcategory_name) },
                                                onClick = {
                                                    subcategoryCode = sub.subcategory_code
                                                    subcategoryExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.height(12.dp))
                            Text("Channel", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                            Spacer(Modifier.height(6.dp))
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                uiState.channels.forEach { ch ->
                                    val selected = channel == ch
                                    Text(
                                        text = ch,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (selected) colorScheme.onPrimary else colorScheme.onSurface,
                                        modifier = Modifier
                                            .clickable { channel = ch }
                                            .background(
                                                if (selected) AccentPrimary else colorScheme.surface.copy(alpha = 0.5f),
                                                RoundedCornerShape(8.dp)
                                            )
                                            .padding(horizontal = 12.dp, vertical = 8.dp)
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(20.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    if (!isEditing) {
                        TextButton(onClick = { isEditing = true }) {
                            Text("Edit", color = AccentPrimary)
                        }
                        TextButton(onClick = { showDeleteConfirm = true }) {
                            Text("Delete", color = ChartRed)
                        }
                    } else {
                        TextButton(onClick = { isEditing = false }) {
                            Text("Cancel", color = TextSecondary)
                        }
                        Button(
                            onClick = {
                                viewModel.updateTransaction(transaction.txn_id, categoryCode.takeIf { it.isNotBlank() }, subcategoryCode.takeIf { it.isNotBlank() }, merchant.takeIf { it.isNotBlank() }, channel.takeIf { it.isNotBlank() })
                                onUpdated()
                            },
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = Color.White)
                        ) { Text("Save") }
                    }
                    Button(
                        onClick = onDismiss,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = GlassCard, contentColor = colorScheme.onSurface)
                    ) { Text("Close") }
                }
            }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = TextSecondary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun TransactionRow(
    transaction: TransactionRecordResponse,
    viewModel: SpendSenseViewModel,
    onClick: () -> Unit
) {
    val isDebit = transaction.direction.lowercase() == "debit"
    val tags = viewModel.transactionTags(transaction)
    val hasTags = tags.isNotEmpty()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(12.dp),
        border = if (hasTags) BorderStroke(1.dp, AccentSecondary.copy(alpha = 0.3f)) else null
    ) {
        Row(
            Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(
                    transaction.merchant ?: transaction.category ?: "Transaction",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        "${transaction.category ?: ""} ${transaction.channel ?: ""}".trim(),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                    tags.forEach { tag ->
                        Text(
                            tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentSecondary,
                            modifier = Modifier
                                .background(AccentSecondary.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Text(
                "${if (isDebit) "-" else "+"}${formatCurrency(kotlin.math.abs(transaction.amount))}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = if (isDebit) ChartRed else Success
            )
        }
    }
}

@Composable
private fun UploadStatementDialog(
    filename: String,
    fileBytes: ByteArray,
    viewModel: SpendSenseViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    var password by remember { mutableStateOf("") }
    var hasInitiatedUpload by remember { mutableStateOf(false) }
    val needsPassword = uiState.error?.lowercase()?.contains("password") == true

    LaunchedEffect(uiState.isLoading, uiState.error) {
        if (hasInitiatedUpload && !uiState.isLoading && uiState.error == null) {
            hasInitiatedUpload = false
            onSuccess()
        }
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = {
            viewModel.clearError()
            onDismiss()
        },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 480.dp),
            colors = CardDefaults.cardColors(containerColor = colorScheme.background),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text("Upload Statement", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                Text("PDF, Excel (XLS/XLSX), or CSV", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(16.dp))
                Text(filename, style = MaterialTheme.typography.bodyMedium, color = colorScheme.onSurface, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(16.dp))
                val isPdf = filename.endsWith(".pdf", ignoreCase = true)
                Text(
                    if (needsPassword) "PDF Password (required)" else if (isPdf) "PDF Password (if file is locked)" else "PDF Password (optional)",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (isPdf || needsPassword) FontWeight.SemiBold else FontWeight.Normal,
                    color = if (needsPassword) ChartOrange else if (isPdf) colorScheme.onSurface else TextSecondary
                )
                Spacer(Modifier.height(4.dp))
                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    placeholder = {
                        Text(
                            if (needsPassword) "Enter password to unlock PDF" else if (isPdf) "Enter password if PDF is locked" else "Enter password if file is encrypted",
                            color = TextSecondary
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        focusedBorderColor = if (needsPassword) ChartOrange else MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                if (needsPassword) {
                    Text(
                        "This file is password protected. Enter the password and tap Upload.",
                        style = MaterialTheme.typography.labelSmall,
                        color = ChartOrange,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                } else if (isPdf) {
                    Text(
                        "If your PDF is password-protected, enter it above before tapping Upload.",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
                uiState.error?.let { err ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = ChartRed.copy(alpha = 0.15f)),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(err, style = MaterialTheme.typography.bodySmall, color = ChartRed, modifier = Modifier.padding(12.dp))
                    }
                    Spacer(Modifier.height(12.dp))
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = { viewModel.clearError(); onDismiss() }) {
                        Text("Cancel", color = colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Button(
                        onClick = {
                            if (needsPassword && password.isBlank()) return@Button
                            Log.d("MonytixUpload", "UploadStatementDialog: Upload tapped filename=$filename hasPassword=${password.isNotBlank()}")
                            hasInitiatedUpload = true
                            viewModel.uploadStatement(fileBytes, filename, password.takeIf { it.isNotBlank() })
                        },
                        enabled = !uiState.isLoading && (!needsPassword || password.isNotBlank()),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (uiState.isLoading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                                Spacer(Modifier.width(8.dp))
                            }
                            Text(if (uiState.isLoading) "Uploading..." else "Upload")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualAddDialog(
    viewModel: SpendSenseViewModel,
    onDismiss: () -> Unit,
    onSubmit: (String, String, String?, Double, String, String?, String?, String?) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val colorScheme = MaterialTheme.colorScheme
    var date by remember { mutableStateOf(LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)) }
    var merchant by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var direction by remember { mutableStateOf("debit") }
    var categoryCode by remember { mutableStateOf("") }
    var subcategoryCode by remember { mutableStateOf("") }
    var channel by remember { mutableStateOf<String?>(null) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var subcategoryExpanded by remember { mutableStateOf(false) }
    var channelExpanded by remember { mutableStateOf(false) }

    LaunchedEffect(categoryCode) {
        if (categoryCode.isNotBlank()) viewModel.loadSubcategories(categoryCode)
        else subcategoryCode = ""
    }

    val channelOptions = if (uiState.channels.isNotEmpty()) uiState.channels else listOf("cash", "upi", "neft", "imps", "card", "atm", "ach", "nach", "other")
    val subcategoryRequired = categoryCode.isNotBlank() && uiState.subcategories.isNotEmpty()

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .heightIn(max = 560.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                Text("Add Transaction", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = colorScheme.onSurface)
                Text("Enter transaction details manually", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = date,
                    onValueChange = { date = it },
                    label = { Text("Date *", color = colorScheme.onSurface.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = merchant,
                    onValueChange = { merchant = it },
                    label = { Text("Merchant Name *", color = colorScheme.onSurface.copy(alpha = 0.7f)) },
                    placeholder = { Text("e.g., Amazon, Grocery Store", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = amount,
                    onValueChange = { amount = it },
                    label = { Text("Amount *", color = colorScheme.onSurface.copy(alpha = 0.7f)) },
                    placeholder = { Text("0.00", color = TextSecondary) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                Spacer(Modifier.height(8.dp))
                Text("Type *", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { direction = "debit" },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (direction == "debit") ChartRed.copy(alpha = 0.3f) else GlassCard,
                            contentColor = if (direction == "debit") ChartRed else colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    ) { Text("Debit (Expense)") }
                    Button(
                        onClick = { direction = "credit" },
                        modifier = Modifier.weight(1f),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = if (direction == "credit") Success.copy(alpha = 0.3f) else GlassCard,
                            contentColor = if (direction == "credit") Success else colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    ) { Text("Credit (Income)") }
                }
                Spacer(Modifier.height(12.dp))
                Text("Category *", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = uiState.categories.find { it.category_code == categoryCode }?.category_name ?: "Select category",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { categoryExpanded = true },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand", modifier = Modifier.clickable { categoryExpanded = true })
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            disabledTextColor = colorScheme.onSurface,
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(expanded = categoryExpanded, onDismissRequest = { categoryExpanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                        uiState.categories.forEach { cat ->
                            DropdownMenuItem(
                                text = { Text(cat.category_name) },
                                onClick = {
                                    categoryCode = cat.category_code
                                    subcategoryCode = ""
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }
                if (categoryCode.isNotBlank()) {
                    Spacer(Modifier.height(8.dp))
                    Text("Subcategory ${if (subcategoryRequired) "*" else ""}", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                    Spacer(Modifier.height(4.dp))
                    Box(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = uiState.subcategories.find { it.subcategory_code == subcategoryCode }?.subcategory_name ?: "Select subcategory",
                            onValueChange = {},
                            readOnly = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable(enabled = uiState.subcategories.isNotEmpty()) { subcategoryExpanded = true },
                            trailingIcon = {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand", modifier = Modifier.clickable(enabled = uiState.subcategories.isNotEmpty()) { subcategoryExpanded = true })
                            },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = colorScheme.onSurface,
                                unfocusedTextColor = colorScheme.onSurface,
                                disabledTextColor = colorScheme.onSurface,
                                focusedBorderColor = AccentPrimary,
                                unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        )
                        DropdownMenu(expanded = subcategoryExpanded, onDismissRequest = { subcategoryExpanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                            if (uiState.subcategories.isEmpty()) {
                                DropdownMenuItem(text = { Text("No subcategories", color = TextSecondary) }, onClick = { subcategoryExpanded = false })
                            } else {
                                uiState.subcategories.forEach { sub ->
                                    DropdownMenuItem(
                                        text = { Text(sub.subcategory_name) },
                                        onClick = { subcategoryCode = sub.subcategory_code; subcategoryExpanded = false }
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("Channel (optional)", style = MaterialTheme.typography.labelMedium, color = TextSecondary)
                Spacer(Modifier.height(4.dp))
                Box(modifier = Modifier.fillMaxWidth()) {
                    OutlinedTextField(
                        value = channel ?: "Select channel",
                        onValueChange = {},
                        readOnly = true,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { channelExpanded = true },
                        trailingIcon = {
                            Icon(Icons.Default.ArrowDropDown, contentDescription = "Expand", modifier = Modifier.clickable { channelExpanded = true })
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            disabledTextColor = colorScheme.onSurface,
                            focusedBorderColor = AccentPrimary,
                            unfocusedBorderColor = colorScheme.outline.copy(alpha = 0.3f)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    )
                    DropdownMenu(expanded = channelExpanded, onDismissRequest = { channelExpanded = false }, modifier = Modifier.fillMaxWidth(0.9f)) {
                        DropdownMenuItem(text = { Text("None", color = TextSecondary) }, onClick = { channel = null; channelExpanded = false })
                        channelOptions.forEach { ch ->
                            DropdownMenuItem(
                                text = { Text(ch.replaceFirstChar { c -> c.uppercase() }) },
                                onClick = { channel = ch; channelExpanded = false }
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text("Description (optional)", color = colorScheme.onSurface.copy(alpha = 0.7f)) },
                    placeholder = { Text("Additional notes...", color = TextSecondary) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp),
                    maxLines = 3,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = colorScheme.onSurface,
                        unfocusedTextColor = colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                Spacer(Modifier.height(16.dp))
                val amt = amount.toDoubleOrNull() ?: 0.0
                val canSubmit = merchant.isNotBlank() && amt > 0 && categoryCode.isNotBlank() &&
                    (!subcategoryRequired || subcategoryCode.isNotBlank())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Button(
                        onClick = {
                            if (canSubmit) {
                                onSubmit(
                                    date,
                                    merchant,
                                    description.takeIf { it.isNotBlank() },
                                    amt,
                                    direction,
                                    categoryCode.takeIf { it.isNotBlank() },
                                    subcategoryCode.takeIf { it.isNotBlank() },
                                    channel
                                )
                            }
                        },
                        enabled = canSubmit,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                    ) {
                        Text("Create Transaction")
                    }
                }
            }
        }
    }
}

@Composable
private fun InsightsTab(viewModel: SpendSenseViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(Unit) {
        viewModel.loadInsights()
        viewModel.loadKpis(null)
    }

    if (uiState.isLoading && uiState.insights == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    val insights = uiState.insights
    if (insights == null) {
        EmptyState(
            title = "No insights available",
            subtitle = "Upload statements to see spending insights"
        )
        return
    }

    val breakdown = insights.category_breakdown
    val total = breakdown.sumOf { it.amount }
    val trends = insights.spending_trends
    val deltaByCategory = uiState.kpis?.top_categories?.associate { it.category_code to it.delta_pct } ?: emptyMap()
    val momPct = if (trends.size >= 2) {
        val latest = trends.last().expenses
        val prev = trends[trends.size - 2].expenses
        if (prev != 0.0) ((latest - prev) / prev * 100) else 0.0
    } else 0.0

    val alerts = viewModel.insightAlerts()
    val opportunities = viewModel.insightOpportunities()
    val patterns = viewModel.insightPatterns()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (alerts.isNotEmpty()) {
            item {
                Text("Alerts", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ChartOrange)
            }
            itemsIndexed(alerts, key = { index, item -> "alert_${index}_${item.text.hashCode()}" }) { i, alert ->
                AnimatedVisibility(
                    visible = true,
                    enter = slideInVertically(animationSpec = tween(400), initialOffsetY = { it / 2 }) + fadeIn(animationSpec = tween(400))
                ) {
                    if (alert.isExpandable) {
                        ExpandableInsightBanner(insight = alert.text, viewModel = viewModel, insights = insights)
                    } else {
                        InsightBanner(insight = alert.text, color = ChartOrange, emoji = "🟡")
                    }
                }
            }
        }
        if (opportunities.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Optimization Opportunities", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = Success)
            }
            itemsIndexed(opportunities, key = { index, item -> "opportunity_${index}_${item.text.hashCode()}" }) { i, opportunity ->
                InsightBanner(insight = opportunity.text, color = Success, emoji = "🟢")
            }
        }
        if (patterns.isNotEmpty()) {
            item {
                Spacer(Modifier.height(8.dp))
                Text("Behavioral Patterns", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = AccentPrimary)
            }
            itemsIndexed(patterns, key = { index, item -> "pattern_${index}_${item.text.hashCode()}" }) { i, pattern ->
                InsightBanner(insight = pattern.text, color = AccentPrimary, emoji = "🔵")
            }
        }
        if (alerts.isEmpty() && opportunities.isEmpty() && patterns.isEmpty()) {
            item {
                viewModel.generateSpendSenseInsight()?.let { insight ->
                    ExpandableInsightBanner(insight = insight, viewModel = viewModel, insights = insights)
                }
            }
        }
        item {
            Spacer(Modifier.height(8.dp))
            Text("Spending Overview", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
        }
        item {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(formatCurrency(total), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = ChartRed)
                if (trends.size >= 2) {
                    Text(
                        "${if (momPct >= 0) "↑" else "↓"} ${kotlin.math.abs(momPct).toInt()}% vs last month",
                        style = MaterialTheme.typography.labelSmall,
                        color = if (momPct >= 0) ChartRed else Success
                    )
                }
            }
        }
        item {
            InteractivePieChart(
                data = breakdown,
                totalAmount = total,
                deltaByCategory = deltaByCategory,
                onCategorySelected = { category ->
                    // Load subcategory breakdown when category is selected
                    viewModel.loadSubcategoryBreakdown(category.category_code)
                },
                getTrendLabel = { item -> viewModel.categoryTrendLabel(item) },
                subcategoryBreakdown = uiState.subcategoryBreakdown,
                onLoadSubcategoryBreakdown = { categoryCode ->
                    viewModel.loadSubcategoryBreakdown(categoryCode)
                }
            )
        }
    }
}

@Composable
private fun InsightBanner(insight: String, color: Color, emoji: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.12f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, color.copy(alpha = 0.3f))
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(emoji, style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.width(12.dp))
            Text(insight, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun ExpandableInsightBanner(
    insight: String,
    viewModel: SpendSenseViewModel,
    insights: InsightsResponse?
) {
    var expanded by remember { mutableStateOf(false) }
    val uiState by viewModel.uiState.collectAsState()
    val isExpandable = viewModel.isTransferRelatedInsight(insight)

    LaunchedEffect(expanded) {
        if (expanded && isExpandable && uiState.transferBreakdownTopMerchants.isEmpty() && !uiState.transferBreakdownLoading) {
            viewModel.loadTransferBreakdown()
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (isExpandable) Modifier.clickable { expanded = !expanded } else Modifier),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        border = BorderStroke(1.dp, AccentSecondary.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("💡", style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(insight, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                    if (isExpandable) {
                        Text(
                            if (expanded) "Tap to collapse" else "Tap to see why",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
            if (expanded && isExpandable) {
                Spacer(Modifier.height(12.dp))
                if (uiState.transferBreakdownLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp),
                        color = AccentSecondary
                    )
                } else if (uiState.transferBreakdownTopMerchants.isNotEmpty()) {
                    Text("Top transfer sources:", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                    Spacer(Modifier.height(6.dp))
                    uiState.transferBreakdownTopMerchants.forEach { (merchant, amount) ->
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(merchant, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
                            Text(formatCurrency(amount), style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold, color = ChartRed)
                        }
                    }
                    insights?.spending_trends?.takeLast(6)?.let { trends ->
                        if (trends.size >= 2) {
                            Spacer(Modifier.height(12.dp))
                            MiniSparkline(values = trends.map { it.expenses })
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSparkline(values: List<Double>) {
    val maxVal = values.maxOrNull() ?: 1.0
    val minVal = values.minOrNull() ?: 0.0
    val range = (maxVal - minVal).coerceAtLeast(1.0)
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(32.dp)
    ) {
        val w = size.width
        val h = size.height
        val step = if (values.size > 1) w / (values.size - 1) else w
        val path = Path().apply {
            values.forEachIndexed { i, v ->
                val x = i * step
                val y = h - ((v - minVal) / range * h).toFloat().coerceIn(0f, h)
                if (i == 0) moveTo(x, y) else lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = AccentPrimary,
            style = Stroke(width = 2.dp.toPx())
        )
    }
}

@Composable
private fun InsightCategoryRow(
    categoryName: String,
    amount: Double,
    percentage: Double,
    transactionCount: Int,
    maxAmount: Double,
    staggerDelayMillis: Int = 0,
    deltaPct: Double? = null,
    trendLabel: String? = null
) {
    val fraction = if (maxAmount > 0) (amount / maxAmount).toFloat() else 0f
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 400, delayMillis = staggerDelayMillis),
        label = "bar"
    )
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text(categoryName, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurface)
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("$transactionCount txns • ${percentage.toInt()}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        deltaPct?.let { pct ->
                            Spacer(Modifier.width(8.dp))
                            Text(
                                "${if (pct >= 0) "↑" else "↓"} ${kotlin.math.abs(pct).toInt()}% vs last month",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (pct <= 0) Success else ChartRed
                            )
                        }
                    }
                    trendLabel?.let { label ->
                        Text(label, style = MaterialTheme.typography.labelSmall, color = if (label.contains("⚠")) ChartOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f))
                    }
                }
                Text(formatCurrency(amount), style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            }
            Spacer(Modifier.height(8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .background(MaterialTheme.colorScheme.outline.copy(alpha = 0.2f), RoundedCornerShape(3.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animatedFraction)
                        .background(AccentPrimary, RoundedCornerShape(3.dp))
                )
            }
        }
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
        Text(title, style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.onSurface)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        onRetry?.let { retry ->
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = retry,
                colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
            ) {
                Text("Retry")
            }
        }
    }
}

private fun categoryToIcon(categoryCode: String?): String {
    if (categoryCode == null || categoryCode.isBlank()) return "📋"
    val code = categoryCode.lowercase()
    return when {
        code.contains("food") || code.contains("dining") || code.contains("grocer") -> "🍽"
        code.contains("transfer") -> "↔"
        code.contains("emi") || code.contains("loan") -> "📅"
        code.contains("rent") || code.contains("housing") -> "🏠"
        code.contains("transport") || code.contains("fuel") || code.contains("uber") -> "🚗"
        code.contains("shopping") || code.contains("retail") -> "🛒"
        code.contains("subscription") || code.contains("entertainment") -> "📺"
        code.contains("health") || code.contains("medical") -> "💊"
        code.contains("utility") || code.contains("bill") -> "💡"
        else -> "📋"
    }
}

private fun formatMonthDisplay(yyyyMm: String): String {
    return try {
        YearMonth.parse(yyyyMm).format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.getDefault()))
    } catch (_: Exception) {
        yyyyMm
    }
}

private fun formatCurrency(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    val formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(abs.toLong())
    return if (amount < 0) "-₹$formatted" else "₹$formatted"
}
