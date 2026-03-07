package com.example.monytix.goaltracker

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.monytix.data.GoalProgressItem
import com.example.monytix.data.GoalResponse
import com.example.monytix.ui.theme.AccentPrimary
import com.example.monytix.ui.theme.ChartGreen
import com.example.monytix.ui.theme.ChartOrange
import com.example.monytix.ui.theme.GlassCard
import com.example.monytix.ui.theme.Success
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private fun formatCurrency(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    val formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(abs.toLong())
    return if (amount < 0) "-₹$formatted" else "₹$formatted"
}

// ─── Goal Detail Screen (Execution Mode) ───

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GoalDetailScreen(
    goal: GoalResponse,
    progress: GoalProgressItem?,
    viewModel: GoalTrackerViewModel,
    onDismiss: () -> Unit,
    onNavigateToBudget: () -> Unit = {}
) {
    val progressPct = progress?.progress_pct ?: 0.0
    val remaining = progress?.remaining_amount ?: (goal.estimated_cost - goal.current_savings).coerceAtLeast(0.0)
    val monthlyRequired = progress?.monthly_required ?: 0.0
    val daysToTarget = progress?.days_to_target ?: 0
    val monthsToFinish = if (daysToTarget > 0) daysToTarget / 30.0 else 0.0

    // Use actual avg from backend, or estimate if not available
    val currentAvg = progress?.monthly_avg_contribution?.takeIf { it > 0 }
        ?: if (monthlyRequired > 0 && progressPct < 95) {
            monthlyRequired * 0.85 // Fallback: assume 15% short
        } else monthlyRequired
    val gap = (monthlyRequired - currentAvg).coerceAtLeast(0.0)
    val isOnTrack = gap <= 0 || progressPct >= 95

    Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Hero Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = AccentPrimary.copy(alpha = 0.15f)),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "${viewModel.goalEmoji(goal)} ${goal.goal_name}",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        viewModel.goalEmotionalLabel(goal),
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentPrimary
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "Progress ${progressPct.toInt()}%",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(formatCurrency(goal.current_savings), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                            Text("saved", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                        Column(horizontalAlignment = Alignment.End, modifier = Modifier.weight(1f)) {
                            Text(formatCurrency(remaining), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = ChartOrange)
                            Text("remaining", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    progress?.pace_description?.let { pace ->
                        Text("🕒 $pace", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                    } ?: run {
                        if (monthsToFinish > 0) {
                            Text("🕒 On track to finish in ${monthsToFinish.toInt()} months", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f))
                        }
                    }
                    Text("⚡ Finish faster? See options below", style = MaterialTheme.typography.labelMedium, color = AccentPrimary)
                }
            }

            // 2. Smart Plan
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = GlassCard),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        "🧠 Your Current Plan",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    goal.target_date?.let { target ->
                        Spacer(Modifier.height(12.dp))
                        Text("Required to finish by $target:", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f))
                        Text(formatCurrency(monthlyRequired) + "/month", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        Spacer(Modifier.height(8.dp))
                        Text("Current average contribution:", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
                        Text(formatCurrency(currentAvg) + "/month", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                        if (gap > 0) {
                            Spacer(Modifier.height(8.dp))
                            Text("Gap: ${formatCurrency(gap)}/month short", style = MaterialTheme.typography.labelMedium, color = ChartOrange, fontWeight = FontWeight.Medium)
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                    Text(
                        "🔥 Action Plan",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.height(16.dp))

                    // Option A: Balanced
                    ActionOptionCard(
                        title = "Option A (Balanced)",
                        description = if (gap > 0) "Increase monthly contribution by ${formatCurrency(gap)}" else "You're on track. Keep current pace.",
                        subtext = if (gap > 0) "Reduce Wants by ₹${(gap * 0.6).toInt()} • Reduce variable by ₹${(gap * 0.4).toInt()}" else null,
                        onClick = { onNavigateToBudget() }
                    )
                    Spacer(Modifier.height(8.dp))

                    // Option B: Aggressive
                    val aggressiveAmount = (monthlyRequired * 1.5).toLong().coerceAtLeast(1000)
                    val monthsAggressive = if (aggressiveAmount > 0) (remaining / aggressiveAmount).toInt().coerceAtLeast(1) else 0
                    ActionOptionCard(
                        title = "Option B (Aggressive)",
                        description = "Increase contribution to ${formatCurrency(aggressiveAmount.toDouble())}/month",
                        subtext = "Finish in $monthsAggressive months",
                        onClick = { onNavigateToBudget() }
                    )
                    Spacer(Modifier.height(8.dp))

                    // Option C: Extend Timeline
                    goal.target_date?.let { target ->
                        val extendedDate = try {
                            val d = LocalDate.parse(target)
                            d.plusMonths(3).format(DateTimeFormatter.ISO_LOCAL_DATE)
                        } catch (_: Exception) { "March 2027" }
                        ActionOptionCard(
                            title = "Option C (Extend Timeline)",
                            description = "Move target to $extendedDate",
                            subtext = "Keep current pace",
                            onClick = { /* TODO: Update goal target date */ }
                        )
                    }
                }
            }

            // 3. Cashflow Impact
            if (monthlyRequired > 0 && gap > 0) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = GlassCard),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            "Cashflow Impact",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "If you increase contribution by ${formatCurrency(gap)}:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.9f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "• Savings rate: +${(gap / 50000 * 100).toInt()}% (estimated)",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Text(
                            "• Wants: reduce by ~₹${(gap * 0.6).toInt()}/month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "Link to BudgetPilot to see exact impact.",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary
                        )
                        Text(
                            "Tap to open BudgetPilot",
                            style = MaterialTheme.typography.labelSmall,
                            color = AccentPrimary,
                            modifier = Modifier.clickable { onNavigateToBudget() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }

@Composable
private fun ActionOptionCard(
    title: String,
    description: String,
    subtext: String?,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                title,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = AccentPrimary
            )
            Spacer(Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            subtext?.let {
                Spacer(Modifier.height(4.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
            }
        }
    }
}
