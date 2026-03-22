package com.example.monytix.foresight

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
import com.example.monytix.future.FutureScreen
import com.example.monytix.moneymoments.MoneyMomentsEmbedTab
import com.example.monytix.moneymoments.MoneyMomentsScreen

@Composable
fun ForesightScreen(
    modifier: Modifier = Modifier,
    onUploadStatement: () -> Unit = {}
) {
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    val tabs = listOf("Nudges", "Habits", "Future")

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
                0 -> MoneyMomentsScreen(
                    modifier = Modifier.fillMaxSize(),
                    embedTab = MoneyMomentsEmbedTab.NUDGES
                )
                1 -> MoneyMomentsScreen(
                    modifier = Modifier.fillMaxSize(),
                    embedTab = MoneyMomentsEmbedTab.HABITS
                )
                2 -> FutureScreen(
                    modifier = Modifier.fillMaxSize(),
                    onUploadStatement = onUploadStatement
                )
            }
        }
    }
}
