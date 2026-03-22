package com.example.monytix.state

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.monytix.AppDestinations
import com.example.monytix.budgetpilot.BudgetPilotScreen
import com.example.monytix.goaltracker.GoalTrackerScreen

@Composable
fun StateScreen(
    modifier: Modifier = Modifier,
    onNavigateTo: (AppDestinations) -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Budget", "Goals")

    Column(modifier = modifier.fillMaxSize()) {
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTabIndex == index,
                    onClick = { selectedTabIndex = index },
                    text = { Text(title) }
                )
            }
        }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(0.dp)
        ) {
            when (selectedTabIndex) {
                0 -> BudgetPilotScreen(
                    modifier = Modifier.fillMaxSize(),
                    onNavigateTo = onNavigateTo
                )
                1 -> GoalTrackerScreen(
                    modifier = Modifier.fillMaxSize(),
                    onNavigateTo = onNavigateTo
                )
            }
        }
    }
}
