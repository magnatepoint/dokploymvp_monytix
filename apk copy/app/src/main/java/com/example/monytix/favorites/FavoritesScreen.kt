package com.example.monytix.favorites

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.monytix.common.EmptyStateNoGoals

/**
 * Favorites / Goals tab.
 * Shows empty state when no goals exist (goals feature coming soon).
 */
@Composable
fun FavoritesScreen(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        EmptyStateNoGoals(
            onCreateGoal = {
                // TODO: Navigate to create goal flow when available
            }
        )
    }
}
