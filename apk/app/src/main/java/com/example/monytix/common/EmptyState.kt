package com.example.monytix.common

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.monytix.R

/**
 * Reusable empty state composables for production fintech screens.
 * Each educates the user and guides the next step.
 */

@Composable
fun EmptyStateNoTransactions(
    onAddTransaction: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        modifier = modifier,
        title = stringResource(R.string.empty_no_transactions_title),
        subtitle = stringResource(R.string.empty_no_transactions_hint),
        actionLabel = stringResource(R.string.empty_no_transactions_action),
        onAction = onAddTransaction
    )
}

@Composable
fun EmptyStateNoBankLinked(
    onLinkBank: () -> Unit,
    onManualUpload: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        modifier = modifier,
        title = stringResource(R.string.empty_no_bank_title),
        subtitle = stringResource(R.string.empty_no_bank_hint),
        actionLabel = stringResource(R.string.empty_no_bank_action),
        onAction = onLinkBank,
        secondaryActionLabel = stringResource(R.string.empty_no_bank_manual),
        onSecondaryAction = onManualUpload
    )
}

@Composable
fun EmptyStateNoGoals(
    onCreateGoal: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        modifier = modifier,
        title = stringResource(R.string.empty_no_goals_title),
        subtitle = stringResource(R.string.empty_no_goals_hint),
        actionLabel = stringResource(R.string.empty_no_goals_action),
        onAction = onCreateGoal
    )
}

@Composable
fun EmptyStateNoInsights(
    onAddData: () -> Unit,
    modifier: Modifier = Modifier
) {
    EmptyStateTemplate(
        modifier = modifier,
        title = stringResource(R.string.empty_no_insights_title),
        subtitle = stringResource(R.string.empty_no_insights_hint),
        actionLabel = stringResource(R.string.empty_no_insights_action),
        onAction = onAddData
    )
}

@Composable
private fun EmptyStateTemplate(
    title: String,
    subtitle: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    secondaryActionLabel: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (icon != null) {
            androidx.compose.material3.Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.height(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onAction) {
            Text(actionLabel)
        }
        if (secondaryActionLabel != null && onSecondaryAction != null) {
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.material3.TextButton(onClick = onSecondaryAction) {
                Text(secondaryActionLabel)
            }
        }
    }
}
