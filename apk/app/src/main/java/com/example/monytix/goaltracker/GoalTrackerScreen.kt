package com.example.monytix.goaltracker

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monytix.analytics.AnalyticsHelper
import com.example.monytix.data.GoalProgressItem
import com.example.monytix.data.GoalResponse
import com.example.monytix.ui.MonytixSpinner
import com.example.monytix.ui.theme.AccentPrimary
import com.example.monytix.ui.theme.BannerPurple
import com.example.monytix.ui.theme.ChartOrange
import com.example.monytix.ui.theme.ChartPurple
import com.example.monytix.ui.theme.GlassCard
import com.example.monytix.ui.theme.Success
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalTrackerScreen(
    viewModel: GoalTrackerViewModel = viewModel(),
    onNavigateTo: (com.example.monytix.AppDestinations) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedTab by remember { mutableStateOf(GtTab.OVERVIEW) }

    LaunchedEffect(Unit) { AnalyticsHelper.logScreenView("goal_tracker") }
    val colorScheme = MaterialTheme.colorScheme
    var showAddGoal by remember { mutableStateOf(false) }
    var selectedGoal by remember { mutableStateOf<Pair<GoalResponse, GoalProgressItem?>?>(null) }
    var showEditGoal by remember { mutableStateOf<GoalResponse?>(null) }
    var showDeleteGoal by remember { mutableStateOf<GoalResponse?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.error, uiState.goals.isEmpty()) {
        if (uiState.error != null && uiState.goals.isNotEmpty()) {
            snackbarHostState.showSnackbar(
                uiState.error ?: "Something went wrong",
                duration = SnackbarDuration.Long
            )
            viewModel.clearError()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } },
        topBar = {
            when (val sel = selectedGoal) {
                null -> androidx.compose.material3.TopAppBar(
                    title = { Text("GoalTracker", color = colorScheme.onBackground) },
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
                else -> {
                    val (goal, _) = sel
                    var showGoalMenu by remember { mutableStateOf(false) }
                    androidx.compose.material3.TopAppBar(
                        title = { Text(goal.goal_name, color = colorScheme.onBackground, maxLines = 1) },
                        navigationIcon = {
                            IconButton(onClick = { selectedGoal = null }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Back",
                                    tint = colorScheme.onBackground
                                )
                            }
                        },
                        colors = androidx.compose.material3.TopAppBarDefaults.topAppBarColors(
                            containerColor = colorScheme.background,
                            titleContentColor = colorScheme.onBackground
                        ),
                        actions = {
                            Box {
                                IconButton(onClick = { showGoalMenu = true }) {
                                    Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = colorScheme.onBackground)
                                }
                                androidx.compose.material3.DropdownMenu(
                                    expanded = showGoalMenu,
                                    onDismissRequest = { showGoalMenu = false }
                                ) {
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Edit") },
                                        onClick = {
                                            showGoalMenu = false
                                            showEditGoal = goal
                                        },
                                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) }
                                    )
                                    androidx.compose.material3.DropdownMenuItem(
                                        text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                                        onClick = {
                                            showGoalMenu = false
                                            showDeleteGoal = goal
                                        },
                                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                                    )
                                }
                            }
                        }
                    )
                }
            }
        },
        containerColor = colorScheme.background,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    AnalyticsHelper.logEvent("add_goal")
                    showAddGoal = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add goal")
            }
        }
    ) { innerPadding ->
        Box(modifier = modifier.fillMaxSize().padding(innerPadding)) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            WelcomeBanner(username = uiState.userEmail)
            if (uiState.lastSyncTime > 0) {
                val mins = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis() - uiState.lastSyncTime)
                val syncLabel = when {
                    mins < 1 -> "Just now"
                    mins == 1L -> "1 min ago"
                    mins < 60 -> "${mins} mins ago"
                    else -> "${TimeUnit.MILLISECONDS.toHours(System.currentTimeMillis() - uiState.lastSyncTime)} hrs ago"
                }
                Text(
                    "Last updated: $syncLabel • From transactions sync",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
            TabBar(selectedTab = selectedTab, onTabSelected = {
                AnalyticsHelper.logEvent("tab_selected", mapOf("tab" to it.name.lowercase()))
                selectedTab = it
            })
            PullToRefreshBox(
                isRefreshing = uiState.isLoading,
                onRefresh = { viewModel.refresh() }
            ) {
                when (selectedTab) {
                    GtTab.OVERVIEW -> OverviewTab(viewModel = viewModel, onGoalClick = { g, p ->
                        AnalyticsHelper.logEvent("goal_tapped", mapOf("goal_id" to g.goal_id))
                        selectedGoal = g to p
                    }, onAddGoal = { showAddGoal = true })
                    GtTab.GOALS -> GoalsListTab(viewModel = viewModel, onGoalClick = { g, p ->
                        AnalyticsHelper.logEvent("goal_tapped", mapOf("goal_id" to g.goal_id))
                        selectedGoal = g to p
                    }, onAddGoal = { showAddGoal = true })
                    GtTab.AI_INSIGHTS -> AIInsightsTab(viewModel = viewModel)
                }
            }
        }
        }
        selectedGoal?.let { (goal, prog) ->
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
                GoalDetailScreen(
                    goal = goal,
                    progress = prog,
                    viewModel = viewModel,
                    onDismiss = { selectedGoal = null },
                    onNavigateToBudget = { onNavigateTo(com.example.monytix.AppDestinations.BUDGET) }
                )
            }
        }
    }
    LaunchedEffect(uiState.createGoalResult) {
        when (val r = uiState.createGoalResult) {
            is CreateGoalResult.Success -> {
                showAddGoal = false
                viewModel.clearCreateGoalResult()
            }
            is CreateGoalResult.Failure -> {
                snackbarHostState.showSnackbar(r.message, duration = SnackbarDuration.Long)
                viewModel.clearCreateGoalResult()
            }
            null -> { }
        }
    }
    if (showAddGoal) {
        AddGoalDialog(
            onDismiss = {
                showAddGoal = false
                viewModel.clearCreateGoalResult()
            },
            onSubmit = { cat, name, cost, targetDate, savings, goalType, importance ->
                viewModel.createGoal(cat, name, cost, targetDate, savings, goalType, importance)
            },
            isLoading = uiState.isLoading
        )
    }
    showEditGoal?.let { goal ->
        EditGoalDialog(
            goal = goal,
            onDismiss = { showEditGoal = null },
            onSubmit = { cost, targetDate, savings, goalType, importance ->
                viewModel.updateGoal(goal.goal_id, cost, targetDate, savings, goalType, importance)
                showEditGoal = null
                selectedGoal = null
            }
        )
    }
    showDeleteGoal?.let { goal ->
        AlertDialog(
            onDismissRequest = { showDeleteGoal = null },
            title = { Text("Delete Goal?", color = MaterialTheme.colorScheme.onSurface) },
            text = { Text("Are you sure you want to delete \"${goal.goal_name}\"? This cannot be undone.", color = MaterialTheme.colorScheme.onSurface) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteGoal(goal.goal_id)
                        showDeleteGoal = null
                        selectedGoal = null
                    },
                    colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error, contentColor = MaterialTheme.colorScheme.onError)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteGoal = null }) {
                    Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
                }
            }
        )
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
            text = "Track your financial goals.",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Set targets, monitor progress, and achieve more. Welcome back, $displayName!",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.9f)
        )
    }
}

private enum class GtTab(val label: String, val icon: String) {
    OVERVIEW("Overview", "📊"),
    GOALS("Goals", "🎯"),
    AI_INSIGHTS("AI Insights", "✨")
}

@Composable
private fun TabBar(selectedTab: GtTab, onTabSelected: (GtTab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        GtTab.entries.forEach { tab ->
            val selected = tab == selectedTab
            TextButton(
                onClick = { onTabSelected(tab) },
                colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                    contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
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
private fun OverviewTab(
    viewModel: GoalTrackerViewModel,
    onGoalClick: (GoalResponse, GoalProgressItem?) -> Unit = { _, _ -> },
    onAddGoal: () -> Unit = { }
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading && uiState.goals.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MonytixSpinner()
        }
        return
    }

    if (uiState.error != null && uiState.goals.isEmpty()) {
        EmptyState(
            title = "Unable to Load Data",
            subtitle = uiState.error ?: "",
            onRetry = { viewModel.loadData() }
        )
        return
    }

    val activeGoals = uiState.goals.filter { it.status.lowercase() == "active" }
    val completedGoals = uiState.goals.filter { it.status.lowercase() == "completed" }
    val totalProgress = if (uiState.progress.isNotEmpty()) {
        uiState.progress.sumOf { it.progress_pct.toDouble() } / uiState.progress.size
    } else 0.0
    val achieverLevel = when {
        uiState.goals.isEmpty() -> "Beginner"
        completedGoals.size.toDouble() / uiState.goals.size >= 0.8 && totalProgress >= 80 -> "Expert"
        completedGoals.size.toDouble() / uiState.goals.size >= 0.6 && totalProgress >= 60 -> "Advanced"
        completedGoals.size.toDouble() / uiState.goals.size >= 0.4 && totalProgress >= 40 -> "Intermediate"
        else -> "Beginner"
    }

    if (uiState.goals.isEmpty()) {
        NoGoalsEmptyState(onAddGoal = onAddGoal)
        return
    }

    val health = viewModel.goalHealthSummary()
    val deadlines = viewModel.upcomingDeadlines()

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            GoalHealthCard(health = health)
        }
        if (deadlines.isNotEmpty()) {
            item {
                Text("Upcoming Deadlines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = ChartOrange)
            }
            items(deadlines.size) { i ->
                val (goal, prog) = deadlines[i]
                UpcomingDeadlineCard(goal = goal, progress = prog, viewModel = viewModel, onClick = { onGoalClick(goal, prog) })
            }
        }
        item {
            Text("Your Progress", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(modifier = Modifier.weight(1f), label = "Active Goals", value = "${activeGoals.size}", color = ChartPurple)
                MetricCard(modifier = Modifier.weight(1f), label = "Completed", value = "${completedGoals.size}", color = Success)
            }
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricCard(modifier = Modifier.weight(1f), label = "Total Progress", value = "${totalProgress.toInt()}%", color = MaterialTheme.colorScheme.primary)
                MetricCard(modifier = Modifier.weight(1f), label = "Goal Achiever", value = achieverLevel, color = MaterialTheme.colorScheme.error)
            }
        }
        item {
            Text("Active Goals", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
        if (activeGoals.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("🎯", style = MaterialTheme.typography.displayMedium)
                    Text("No Active Goals", style = MaterialTheme.typography.titleMedium, color = Color.White)
                    Text("Create your first goal to start tracking.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                }
            }
        } else {
            items(activeGoals, key = { it.goal_id }) { goal ->
                val prog = uiState.progress.find { it.goal_id == goal.goal_id }
                val animateFromPct = uiState.recentlyUpdatedGoalPrevPct[goal.goal_id]
                GoalCard(goal = goal, progress = prog, viewModel = viewModel, animateFromPct = animateFromPct, onClick = { onGoalClick(goal, prog) })
            }
        }
    }
}

@Composable
private fun GoalsListTab(
    viewModel: GoalTrackerViewModel,
    onGoalClick: (GoalResponse, GoalProgressItem?) -> Unit = { _, _ -> },
    onAddGoal: () -> Unit = { }
) {
    val uiState by viewModel.uiState.collectAsState()
    val filter = uiState.selectedFilter
    val filteredGoals = when (filter?.lowercase()) {
        "active" -> uiState.goals.filter { it.status.lowercase() == "active" }
        "completed" -> uiState.goals.filter { it.status.lowercase() == "completed" }
        else -> uiState.goals
    }

    if (uiState.isLoading && uiState.goals.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            MonytixSpinner()
        }
        return
    }

    if (uiState.error != null && uiState.goals.isEmpty()) {
        EmptyState(
            title = "Unable to Load Goals",
            subtitle = uiState.error ?: "",
            onRetry = { viewModel.loadData() }
        )
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(title = "All", selected = filter == null) { viewModel.setFilter(null) }
                FilterChip(title = "Active", selected = filter == "active") { viewModel.setFilter("active") }
                FilterChip(title = "Completed", selected = filter == "completed") { viewModel.setFilter("completed") }
            }
        }
        if (filteredGoals.isEmpty()) {
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(if (filter == "completed") "✅" else "🎯", style = MaterialTheme.typography.displayLarge)
                    Text(
                        if (filter == "completed") "No Completed Goals" else "No Goals",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        if (filter == "completed") "Complete your first goal to see it here." else "Create your first goal to start tracking.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
                    )
                    if (filter != "completed") {
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = onAddGoal,
                            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
                            Text("Set Up Goals")
                        }
                    }
                }
            }
        } else {
            items(filteredGoals, key = { it.goal_id }) { goal ->
                val prog = uiState.progress.find { it.goal_id == goal.goal_id }
                val animateFromPct = uiState.recentlyUpdatedGoalPrevPct[goal.goal_id]
                GoalCard(goal = goal, progress = prog, viewModel = viewModel, animateFromPct = animateFromPct, onClick = { onGoalClick(goal, prog) })
            }
        }
    }
}

@Composable
private fun FilterChip(title: String, selected: Boolean, onClick: () -> Unit) {
    TextButton(
        onClick = onClick,
        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
            contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        ),
        modifier = Modifier.background(
            if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
            RoundedCornerShape(8.dp)
        )
    ) {
        Text(title, style = MaterialTheme.typography.labelLarge)
    }
}

private data class AiInsightSection(val title: String, val color: Color, val emoji: String, val items: List<AiInsight>)

@Composable
private fun AIInsightsTab(viewModel: GoalTrackerViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val goals = uiState.goals
    val progress = uiState.progress

    val sections = remember(goals, progress) {
        val milestones = mutableListOf<AiInsight>()
        val risks = mutableListOf<AiInsight>()
        val optimizations = mutableListOf<AiInsight>()
        val completed = goals.filter { it.status.lowercase() == "completed" }
        val active = goals.filter { it.status.lowercase() == "active" }

        progress.filter { it.progress_pct >= 70 && it.progress_pct < 100 }.forEach { p ->
            milestones.add(AiInsight("m-${p.goal_id}", "Milestone", "You've reached ${p.progress_pct.toInt()}% of your ${p.goal_name.lowercase()} goal.", "milestone"))
        }
        if (completed.isNotEmpty() && goals.isNotEmpty()) {
            val rate = (completed.size.toDouble() / goals.size * 100).toInt()
            milestones.add(AiInsight("rate", "Achievement Rate", "You're on track to complete $rate% of your goals this year!", "rate"))
        }

        progress.filter { prog ->
            val daysLeft = prog.days_to_target ?: 999
            val pct = prog.progress_pct
            daysLeft <= 90 && pct < 50
        }.forEach { p ->
            val goal = goals.find { it.goal_id == p.goal_id }
            val shortfall = p.remaining_amount
            risks.add(AiInsight("r-${p.goal_id}", "Risk", "${goal?.goal_name ?: "Goal"} is underfunded by ${formatCurrency(shortfall)} this quarter.", "risk"))
        }
        active.firstOrNull()?.let { g ->
            val prog = progress.find { it.goal_id == g.goal_id }
            if (prog != null && prog.remaining_amount > 50000 && prog.monthly_required != null) {
                val redirect = (prog.monthly_required * 0.2).toInt()
                if (redirect >= 1000) {
                    optimizations.add(AiInsight("o-${g.goal_id}", "Optimization", "You can redirect ${formatCurrency(redirect.toDouble())} from unused budget to ${g.goal_name}.", "optimization"))
                }
            }
        }
        if (optimizations.isEmpty() && active.isNotEmpty()) {
            active.firstOrNull()?.let { g ->
                val prog = progress.find { it.goal_id == g.goal_id }
                if (prog != null && prog.remaining_amount > 20000) {
                    optimizations.add(AiInsight("o2", "Optimization", "Consider increasing your ${g.goal_name.lowercase()} contribution to reach your goal faster.", "optimization"))
                }
            }
        }

        buildList {
            if (milestones.isNotEmpty()) add(AiInsightSection("Milestones", Success, "🟢", milestones))
            if (risks.isNotEmpty()) add(AiInsightSection("Risk", ChartOrange, "🟡", risks))
            if (optimizations.isNotEmpty()) add(AiInsightSection("Optimization", AccentPrimary, "🔵", optimizations))
            if (isEmpty()) add(AiInsightSection("Getting Started", AccentPrimary, "✨", listOf(AiInsight("0", "Tip", "Set up your first goal to start tracking your financial progress!", "tip"))))
        }
    }

    if (uiState.isLoading) {
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
            Text("AI Insights", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
        }
        item {
            Text("Personalized recommendations based on your goals", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        }
        sections.forEach { section ->
            item {
                Text(section.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = section.color)
            }
            items(section.items, key = { it.id }) { insight ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = section.color.copy(alpha = 0.12f)),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, section.color.copy(alpha = 0.3f))
                ) {
                    Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(section.emoji, style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(insight.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = section.color)
                            Text(insight.message, style = MaterialTheme.typography.bodySmall, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GoalHealthCard(health: GoalTrackerViewModel.GoalHealthSummary) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = AccentPrimary.copy(alpha = 0.15f)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, AccentPrimary.copy(alpha = 0.4f))
    ) {
        Column(Modifier.padding(20.dp)) {
            Text("GOAL HEALTH", style = MaterialTheme.typography.labelSmall, color = AccentPrimary)
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("${health.score} / 100", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Column(horizontalAlignment = Alignment.End) {
                    Text("${health.onTrack} goals on track", style = MaterialTheme.typography.bodySmall, color = Success)
                    Text("${health.atRisk} at risk", style = MaterialTheme.typography.bodySmall, color = if (health.atRisk > 0) ChartOrange else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                }
            }
        }
    }
}

@Composable
private fun UpcomingDeadlineCard(
    goal: GoalResponse,
    progress: GoalProgressItem?,
    viewModel: GoalTrackerViewModel,
    onClick: () -> Unit = {}
) {
    val days = progress?.days_to_target ?: 0
    val monthlyReq = progress?.monthly_required ?: 0.0
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("⚠ ${goal.goal_name}", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = Color.White)
                Text("Target in $days days", style = MaterialTheme.typography.labelSmall, color = ChartOrange)
            }
            if (monthlyReq > 0) {
                Text("Need ${formatCurrency(monthlyReq)}/month to stay on track", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
            }
        }
    }
}

@Composable
private fun GoalCard(
    goal: GoalResponse,
    progress: GoalProgressItem?,
    viewModel: GoalTrackerViewModel,
    animateFromPct: Float? = null,
    onClick: () -> Unit = {}
) {
    val progressPct = progress?.progress_pct ?: 0.0
    val remaining = progress?.remaining_amount ?: (goal.estimated_cost - goal.current_savings).coerceAtLeast(0.0)
    val animProgress = remember { Animatable(animateFromPct?.div(100f) ?: 0f) }
    val targetProgress = (progressPct / 100.0).toFloat().coerceIn(0f, 1f)
    LaunchedEffect(progressPct, animateFromPct) {
        animProgress.animateTo(targetProgress, animationSpec = tween(600))
    }
    val showGlow = animateFromPct != null
    val glowAlpha = remember { Animatable(if (showGlow) 0.4f else 0f) }
    LaunchedEffect(showGlow) {
        if (showGlow) {
            glowAlpha.animateTo(0.4f, animationSpec = tween(150))
            glowAlpha.animateTo(0f, animationSpec = tween(450))
        }
    }
    val statusColor = when (goal.status.lowercase()) {
        "completed" -> Success
        "archived" -> Color.Gray
        else -> Success
    }
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp),
        border = if (showGlow) BorderStroke(2.dp, AccentPrimary.copy(alpha = glowAlpha.value)) else null
    ) {
        Column(Modifier.padding(16.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(Modifier.weight(1f)) {
                    Text("${viewModel.goalEmoji(goal)} ${goal.goal_name}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(viewModel.goalEmotionalLabel(goal), style = MaterialTheme.typography.labelSmall, color = AccentPrimary.copy(alpha = 0.9f))
                    Text(goal.goal_category, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                }
                Text(
                    goal.status.replaceFirstChar { it.uppercase() },
                    style = MaterialTheme.typography.labelSmall,
                    color = statusColor
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text("Progress", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
                Text("${progressPct.toInt()}%", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
            Spacer(Modifier.height(4.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(animProgress.value)
                        .background(MaterialTheme.colorScheme.primary, RoundedCornerShape(4.dp))
                )
            }
            progress?.pace_description?.let { pace ->
                Spacer(Modifier.height(4.dp))
                Text("At current pace: $pace", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
            Spacer(Modifier.height(12.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Saved", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Text(formatCurrency(goal.current_savings), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Target", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Text(formatCurrency(goal.estimated_cost), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Remaining", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
                    Text(formatCurrency(remaining), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
                }
            }
            if (progress?.monthly_required != null && progress.monthly_required > 0 && goal.target_date != null) {
                Spacer(Modifier.height(6.dp))
                Text("To finish by ${goal.target_date}: ${formatCurrency(progress.monthly_required)}/month required", style = MaterialTheme.typography.labelSmall, color = AccentPrimary.copy(alpha = 0.9f))
            }
            goal.target_date?.let { date ->
                if (progress?.monthly_required == null || progress.monthly_required <= 0) {
                    Spacer(Modifier.height(8.dp))
                    Text("Target: $date", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                }
            }
        }
    }
}

@Composable
private fun MetricCard(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
            Spacer(Modifier.height(4.dp))
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = color)
        }
    }
}

@Composable
private fun NoGoalsEmptyState(onAddGoal: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("🎯", style = MaterialTheme.typography.displayLarge)
        Spacer(Modifier.height(16.dp))
        Text("No Goals Yet", style = MaterialTheme.typography.titleLarge, color = Color.White)
        Text(
            "Set up your financial goals first to start tracking progress.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onAddGoal,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.padding(end = 8.dp))
            Text("Set Up Goals")
        }
    }
}

@Composable
private fun EmptyState(title: String, subtitle: String, onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(title, style = MaterialTheme.typography.titleLarge, color = Color.White)
        Spacer(Modifier.height(8.dp))
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f))
        Spacer(Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
            Text("Retry")
        }
    }
}

private data class AiInsight(val id: String, val title: String, val message: String, val type: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AddGoalDialog(
    onDismiss: () -> Unit,
    onSubmit: (String, String, Double, String?, Double, String?, Int) -> Unit,
    isLoading: Boolean = false
) {
    val categories = listOf(
        "Emergency" to "Emergency Fund",
        "Travel" to "Vacation / Travel",
        "Debt" to "Credit Card Paydown",
        "Insurance" to "Term Insurance",
        "Housing" to "Home Down Payment",
        "Education" to "Children Education",
        "Retirement" to "Retirement Corpus",
        "Lifestyle" to "New Smartphone",
        "Custom" to "Custom Goal (Medium)"
    )
    val goalTypes = listOf("short_term" to "Short", "medium_term" to "Medium", "long_term" to "Long")
    val priorityLabels = listOf(1 to "Low", 2 to "Med-Low", 3 to "Medium", 4 to "Med-High", 5 to "High")
    var selectedCategoryIndex by remember { mutableStateOf(0) }
    var goalName by remember { mutableStateOf(categories[0].second) }
    var estimatedCost by remember { mutableStateOf("") }
    var selectedGoalTypeIndex by remember { mutableStateOf(1) }
    var selectedPriority by remember { mutableStateOf(3) }
    var targetDate by remember { mutableStateOf("") }
    var currentSavings by remember { mutableStateOf("0") }
    var showDatePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val initialDateMillis = remember {
        LocalDate.now().plusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        yearRange = IntRange(LocalDate.now().year, LocalDate.now().year + 20)
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.ofEpochMilli(millis)
                            val localDate = LocalDate.ofInstant(instant, ZoneId.systemDefault())
                            targetDate = localDate.format(dateFormatter)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Add Goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    "Category",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEachIndexed { idx, (cat, name) ->
                        val selected = selectedCategoryIndex == idx
                        TextButton(
                            onClick = {
                                selectedCategoryIndex = idx
                                goalName = name
                            },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        ) {
                            Text(cat, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = goalName,
                    onValueChange = { goalName = it },
                    label = { Text("Goal name", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = estimatedCost,
                    onValueChange = { estimatedCost = it },
                    label = { Text("Target amount (₹)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Timeline",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goalTypes.forEachIndexed { idx, (_, label) ->
                        val selected = selectedGoalTypeIndex == idx
                        TextButton(
                            onClick = { selectedGoalTypeIndex = idx },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Priority",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priorityLabels.forEach { (value, label) ->
                        val selected = selectedPriority == value
                        TextButton(
                            onClick = { selectedPriority = value },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Target date (optional)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Pick date", color = MaterialTheme.colorScheme.primary)
                    }
                    if (targetDate.isNotBlank()) {
                        TextButton(onClick = { targetDate = "" }) {
                            Text("Clear", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentSavings,
                    onValueChange = { currentSavings = it },
                    label = { Text("Current savings (₹)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val cost = estimatedCost.toDoubleOrNull() ?: 0.0
                            val savings = currentSavings.toDoubleOrNull() ?: 0.0
                            val (cat, _) = categories[selectedCategoryIndex]
                            if (goalName.isNotBlank() && cost > 0 && !isLoading) {
                                val dateStr = targetDate.takeIf { it.isNotBlank() }
                                val goalType = goalTypes[selectedGoalTypeIndex].first
                                onSubmit(cat, goalName, cost, dateStr, savings, goalType, selectedPriority)
                            }
                        },
                        enabled = !isLoading,
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isLoading) {
                            MonytixSpinner(size = 20.dp, stroke = 2.dp)
                        } else {
                            Text("Add")
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EditGoalDialog(
    goal: GoalResponse,
    onDismiss: () -> Unit,
    onSubmit: (Double?, String?, Double?, String?, Int?) -> Unit
) {
    val goalTypes = listOf("short_term" to "Short", "medium_term" to "Medium", "long_term" to "Long")
    val priorityLabels = listOf(1 to "Low", 2 to "Med-Low", 3 to "Medium", 4 to "Med-High", 5 to "High")
    var estimatedCost by remember { mutableStateOf(goal.estimated_cost.toString()) }
    var targetDate by remember { mutableStateOf(goal.target_date ?: "") }
    var currentSavings by remember { mutableStateOf(goal.current_savings.toString()) }
    var selectedGoalTypeIndex by remember {
        mutableStateOf(goalTypes.indexOfFirst { it.first == goal.goal_type }.takeIf { it >= 0 } ?: 1)
    }
    var selectedPriority by remember { mutableStateOf(goal.importance ?: 3) }
    var showDatePicker by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current
    val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    val initialDateMillis = remember(targetDate) {
        targetDate.takeIf { it.isNotBlank() }?.let {
            try {
                LocalDate.parse(it, dateFormatter).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            } catch (_: Exception) {
                LocalDate.now().plusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
            }
        } ?: LocalDate.now().plusYears(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDateMillis,
        yearRange = IntRange(LocalDate.now().year, LocalDate.now().year + 20)
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val instant = Instant.ofEpochMilli(millis)
                            val localDate = LocalDate.ofInstant(instant, ZoneId.systemDefault())
                            targetDate = localDate.format(dateFormatter)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("OK", color = MaterialTheme.colorScheme.primary)
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.background),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                Modifier
                    .padding(24.dp)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    "Edit Goal",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    goal.goal_name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                )
                Spacer(Modifier.height(16.dp))
                OutlinedTextField(
                    value = estimatedCost,
                    onValueChange = { estimatedCost = it },
                    label = { Text("Target amount (₹)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    "Timeline",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    goalTypes.forEachIndexed { idx, (_, label) ->
                        val selected = selectedGoalTypeIndex == idx
                        TextButton(
                            onClick = { selectedGoalTypeIndex = idx },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                Text(
                    "Priority",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    priorityLabels.forEach { (value, label) ->
                        val selected = selectedPriority == value
                        TextButton(
                            onClick = { selectedPriority = value },
                            colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                contentColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            ),
                            modifier = Modifier.background(
                                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color.Transparent,
                                RoundedCornerShape(8.dp)
                            )
                        ) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = targetDate,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text("Target date (optional)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = { showDatePicker = true }) {
                        Text("Pick date", color = MaterialTheme.colorScheme.primary)
                    }
                    if (targetDate.isNotBlank()) {
                        TextButton(onClick = { targetDate = "" }) {
                            Text("Clear", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value = currentSavings,
                    onValueChange = { currentSavings = it },
                    label = { Text("Current savings (₹)", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MaterialTheme.colorScheme.onSurface,
                        unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        unfocusedBorderColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                )
                Spacer(Modifier.height(16.dp))
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                    }
                    Button(
                        onClick = {
                            focusManager.clearFocus()
                            val cost = estimatedCost.toDoubleOrNull()
                            val savings = currentSavings.toDoubleOrNull()
                            val dateStr = targetDate.takeIf { it.isNotBlank() }
                            val goalType = goalTypes[selectedGoalTypeIndex].first
                            if (cost != null && cost > 0) {
                                onSubmit(cost, dateStr, savings, goalType, selectedPriority)
                            }
                        },
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text("Save")
                    }
                }
            }
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    val formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(abs.toLong())
    return if (amount < 0) "-₹$formatted" else "₹$formatted"
}
