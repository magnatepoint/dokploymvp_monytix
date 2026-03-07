package com.example.monytix.assistant

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import com.example.monytix.auth.FirebaseAuthManager
import com.example.monytix.data.BackendApi
import com.example.monytix.ui.theme.AccentPrimary
import kotlinx.coroutines.launch

private val PROMPT_CHIPS = listOf(
    "Can I afford this?",
    "Will I run short?",
    "Why did spending increase?",
    "How do I save faster?",
    "What should I reduce this week?"
)

private fun mockAnswerFor(prompt: String): String = when {
    prompt.contains("afford") -> "Based on your current cash flow and goals, you're on track. Check the Future tab for a 14-day projection. For big purchases, we recommend keeping 3 months of expenses as buffer."
    prompt.contains("run short") -> "Your forecast shows a dip around days 8–10. Consider delaying non-essential spend until after payday, or top up your Emergency goal. See the Financial Future tab for details."
    prompt.contains("spending increase") -> "This month's spending is up mainly in dining and shopping. We've flagged this in Insights. Small cuts there can free ₹2–3K without changing essentials."
    prompt.contains("save faster") -> "• Top up one goal by ₹2K this month.\n• Trim dining by 10% to unlock ~₹3,200.\n• Use BudgetPilot to set a Wants cap.\nYou're already ahead of many—small steps will compound."
    prompt.contains("reduce") -> "Focus on: dining out, subscriptions, and impulse buys. Even 5–10% less in those categories can add ₹2–4K to savings. We can suggest a weekly limit in BudgetPilot."
    else -> "Your finances look on track. Use the Future tab for projections and Goals for targets. If you have a specific question, try one of the prompts above."
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AssistantSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var answer by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = null
    ) {
        AssistantSheetContent(
            query = query,
            onQueryChange = { query = it },
            answer = answer,
            isLoading = isLoading,
            onDismiss = onDismiss,
            onChipClick = { chip ->
                if (isLoading) return@AssistantSheetContent
                answer = null
                isLoading = true
                scope.launch {
                    val token = FirebaseAuthManager.getIdToken()
                    if (token != null) {
                        val result = BackendApi.postAssistantAsk(token, chip)
                        answer = result.getOrNull()?.answer ?: mockAnswerFor(chip)
                    } else {
                        answer = mockAnswerFor(chip)
                    }
                    isLoading = false
                }
            }
        )
    }
}

@Composable
private fun AssistantSheetContent(
    query: String,
    onQueryChange: (String) -> Unit,
    answer: String?,
    isLoading: Boolean,
    onDismiss: () -> Unit,
    onChipClick: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .padding(bottom = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Ask MONYTIX",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Close")
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            text = "Quick prompts or type your question.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.weight(1f),
                placeholder = { Text("Ask anything...") },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = AccentPrimary,
                    cursorColor = AccentPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onSurface
                ),
                singleLine = true
            )
        }
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Suggestions",
            style = MaterialTheme.typography.labelMedium,
            color = AccentPrimary
        )
        Spacer(Modifier.height(8.dp))
        PromptChipsGrid(onChipClick = onChipClick)
        if (isLoading) {
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CircularProgressIndicator(modifier = Modifier.height(24.dp), strokeWidth = 2.dp)
                Spacer(Modifier.height(8.dp))
                Text(
                    text = "Asking MONYTIX…",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (answer != null) {
            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(12.dp))
            Text(
                text = "MONYTIX",
                style = MaterialTheme.typography.labelSmall,
                color = AccentPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = answer ?: "",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            )
        }
    }
}

@Composable
private fun PromptChipsGrid(onChipClick: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        for (rowChips in PROMPT_CHIPS.chunked(2)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (chip in rowChips) {
                    Text(
                        text = chip,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onChipClick(chip) }
                            .background(
                                AccentPrimary.copy(alpha = 0.12f),
                                RoundedCornerShape(12.dp)
                            )
                            .padding(12.dp)
                    )
                }
                if (rowChips.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}
