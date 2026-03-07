package com.example.monytix.spendsense

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import com.example.monytix.data.CategoryBreakdownItem
import com.example.monytix.data.DailySpendItem
import com.example.monytix.spendsense.components.SubcategoryBreakdownItem
import com.example.monytix.ui.theme.AccentPrimary
import com.example.monytix.ui.theme.ChartRed
import com.example.monytix.ui.theme.Success
import com.example.monytix.ui.theme.TextSecondary
import java.time.LocalDate
import java.time.YearMonth

private fun formatCurrency(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    val formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(abs.toLong())
    return if (amount < 0) "-₹$formatted" else "₹$formatted"
}

/** Hero card: total spend (never naked ₹0), vs last month, highest category, peak day. */
@Composable
fun SmartSummaryCard(
    totalSpend: Double,
    momPct: Double?,
    highestCategoryName: String?,
    highestCategoryPct: Int?,
    peakDayLine: String?,
    modifier: Modifier = Modifier
) {
    val isZero = totalSpend <= 0.0
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(Modifier.padding(20.dp)) {
            if (isZero) {
                Text(
                    text = "No spending yet",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "Upload statements or add transactions to see insights.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            } else {
                Text(
                    text = formatCurrency(totalSpend),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = ChartRed
                )
                if (momPct != null) {
                    Text(
                        text = "${if (momPct >= 0) "↑" else "↓"} ${kotlin.math.abs(momPct).toInt()}% vs last month",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (momPct >= 0) ChartRed else Success
                    )
                }
                if (highestCategoryName != null) {
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "Highest spend: $highestCategoryName${if (highestCategoryPct != null) " ($highestCategoryPct%)" else ""}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
                if (peakDayLine != null) {
                    Text(
                        text = "Peak day: $peakDayLine",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun RankedCategoryBars(
    categoryBreakdown: List<CategoryBreakdownItem>,
    totalSpend: Double,
    deltaByCategory: Map<String, Double?>,
    onCategoryClick: (CategoryBreakdownItem) -> Unit,
    onViewAllCategories: () -> Unit,
    modifier: Modifier = Modifier
) {
    val top6 = categoryBreakdown.take(6)
    val maxAmount = top6.maxOfOrNull { it.amount } ?: 1.0
    val isEmpty = top6.isEmpty()

    Column(modifier = modifier) {
        if (isEmpty) {
            Text(
                text = "No categories yet",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = "Upload statements to see top categories by spend.",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )
            Spacer(Modifier.height(12.dp))
        } else {
        top6.forEach { item ->
            val delta = deltaByCategory[item.category_code]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoryClick(item) }
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = item.category_name,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = formatCurrency(item.amount),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    val sharePct = if (totalSpend > 0) (item.amount / totalSpend * 100).toInt() else 0
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                                Modifier
                                    .weight(1f)
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxWidth((item.amount / maxAmount).toFloat().coerceIn(0f, 1f))
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(AccentPrimary)
                            )
                        }
                        Text(
                            text = "$sharePct%",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                    if (delta != null) {
                        Text(
                            text = "${if (delta >= 0) "↑" else "↓"} ${kotlin.math.abs(delta).toInt()}% vs last month",
                            style = MaterialTheme.typography.labelSmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
        }
        TextButton(onClick = onViewAllCategories) {
            Text("View all categories", color = AccentPrimary)
        }
    }
}

@Composable
fun WeekdayMatrix(
    dailySpend: List<DailySpendItem>,
    month: YearMonth,
    onDayClick: ((LocalDate) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")

    // Build grid: row = week of month (1-5), col = Mon=0..Sun=6
    val grid = Array(6) { DoubleArray(7) }
    dailySpend.forEach { d ->
        val date = try {
            LocalDate.parse(d.date)
        } catch (_: Exception) {
            return@forEach
        }
        if (date.year != month.year || date.monthValue != month.monthValue) return@forEach
        val weekOfMonth = ((date.dayOfMonth - 1) / 7).coerceIn(0, 5)
        val dayOfWeek = date.dayOfWeek.value - 1 // Mon=0
        grid[weekOfMonth][dayOfWeek] += d.amount
    }
    val maxCell = grid.flatMap { it.toList() }.maxOrNull() ?: 1.0
    // Light cyan (0.08) → dark cyan (0.92) so grid is always visible; empty cells read as "no spend"
    val cellAlphaRange = 0.08f to 0.92f

    Column(modifier = modifier) {
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            dayNames.forEach { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
            }
        }
        Spacer(Modifier.height(4.dp))
        for (row in 0..4) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                for (col in 0..6) {
                    val value = grid[row][col]
                    val intensity = if (maxCell > 0) (value / maxCell).toFloat().coerceIn(0f, 1f) else 0f
                    val alpha = cellAlphaRange.first + intensity * (cellAlphaRange.second - cellAlphaRange.first)
                    val weekday = col + 1
                    val datesWithWeekday = (1..month.lengthOfMonth()).map { month.atDay(it) }.filter { it.dayOfWeek.value == weekday }
                    val dateForCell = datesWithWeekday.getOrNull(row)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .padding(2.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(AccentPrimary.copy(alpha = alpha))
                            .then(
                                if (onDayClick != null && dateForCell != null) {
                                    Modifier.clickable { onDayClick(dateForCell) }
                                } else Modifier
                            ),
                        contentAlignment = Alignment.Center
                    ) {}
                }
            }
        }
    }
}

@Composable
fun CostlyDayLine(
    spendingPatterns: List<com.example.monytix.data.SpendingPatternItem>,
    modifier: Modifier = Modifier
) {
    val totalByDay = spendingPatterns.sumOf { it.amount }
    val avg = totalByDay / 7
    val top = spendingPatterns.maxByOrNull { it.amount }
    if (spendingPatterns.isEmpty() || avg <= 0 || top == null) {
        Text(
            text = "Your money rhythm will appear here once you have enough data.",
            style = MaterialTheme.typography.bodySmall,
            color = TextSecondary,
            modifier = modifier
        )
        return
    }
    val multiplier = (top.amount / avg)
    Text(
        text = "You spend ${"%.1f".format(multiplier)}× more on ${top.day_of_week}s.",
        style = MaterialTheme.typography.bodySmall,
        color = TextSecondary,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryDetailBottomSheet(
    category: CategoryBreakdownItem,
    deltaPct: Double?,
    subcategoryBreakdown: List<SubcategoryBreakdownItem>,
    topMerchants: List<Pair<String, Double>>,
    onDismiss: () -> Unit,
    trendLabel: String? = null,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
                .padding(bottom = 32.dp)
        ) {
            Text(
                text = category.category_name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = formatCurrency(category.amount),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val trend = trendLabel ?: deltaPct?.let {
                "${if (it >= 0) "↑" else "↓"} ${kotlin.math.abs(it).toInt()}% vs last month"
            }
            if (trend != null) {
                Text(
                    text = trend,
                    style = MaterialTheme.typography.labelMedium,
                    color = TextSecondary
                )
            }

            if (subcategoryBreakdown.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "By subcategory",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                val subTotal = subcategoryBreakdown.sumOf { it.amount }
                val subMax = subcategoryBreakdown.maxOfOrNull { it.amount } ?: 1.0
                subcategoryBreakdown.forEach { item ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(
                                text = item.subcategory_name,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp))
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            ) {
                                Box(
                                    Modifier
                                        .fillMaxWidth((item.amount / subMax).toFloat().coerceIn(0f, 1f))
                                        .height(6.dp)
                                        .clip(RoundedCornerShape(3.dp))
                                        .background(AccentPrimary)
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = formatCurrency(item.amount),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            if (topMerchants.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "Biggest merchants",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                topMerchants.take(8).forEach { (name, amount) ->
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1
                        )
                        Text(
                            text = formatCurrency(amount),
                            style = MaterialTheme.typography.bodySmall,
                            color = TextSecondary
                        )
                    }
                }
            }
        }
    }
}
