package com.example.monytix.spendsense.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.monytix.data.CategoryBreakdownItem
import com.example.monytix.ui.theme.AccentPrimary
import com.example.monytix.ui.theme.ChartBlue
import com.example.monytix.ui.theme.ChartGreen
import com.example.monytix.ui.theme.ChartOrange
import com.example.monytix.ui.theme.ChartPurple
import com.example.monytix.ui.theme.ChartRed
import com.example.monytix.ui.theme.GlassCard
import com.example.monytix.ui.theme.HeroCardGlow
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class PieSlice(
    val category: CategoryBreakdownItem,
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color,
    val index: Int
)

@Composable
fun InteractivePieChart(
    data: List<CategoryBreakdownItem>,
    totalAmount: Double,
    deltaByCategory: Map<String, Double?>,
    onCategorySelected: ((CategoryBreakdownItem) -> Unit)? = null,
    getTrendLabel: ((CategoryBreakdownItem) -> String?)? = null,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GlassCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No category data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
        return
    }

    val chartColors = remember {
        listOf(
            AccentPrimary,
            ChartRed,
            ChartPurple,
            ChartGreen,
            ChartBlue,
            ChartOrange,
            AccentPrimary.copy(alpha = 0.7f),
            ChartRed.copy(alpha = 0.7f),
            ChartPurple.copy(alpha = 0.7f),
            ChartGreen.copy(alpha = 0.7f),
            ChartBlue.copy(alpha = 0.7f),
            ChartOrange.copy(alpha = 0.7f)
        )
    }

    // Prepare slices with angles
    val slices = remember(data) {
        var currentAngle = -90f // Start from top
        data.mapIndexed { index, item ->
            val sweepAngle = (item.percentage / 100f) * 360f
            val slice = PieSlice(
                category = item,
                startAngle = currentAngle,
                sweepAngle = sweepAngle,
                color = chartColors[index % chartColors.size],
                index = index
            )
            currentAngle += sweepAngle
            slice
        }
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val animationProgress = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
    }

    val selectedSlice = selectedIndex?.let { slices.getOrNull(it) }

    Column(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GlassCard),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(280.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val radius = size.minDimension / 2f
                                val dx = tapOffset.x - center.x
                                val dy = tapOffset.y - center.y
                                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                                if (distance <= radius) {
                                    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    val normalizedAngle = (angle + 90f + 360f) % 360f

                                    val tappedIndex = slices.indexOfFirst { slice ->
                                        val start = (slice.startAngle + 90f + 360f) % 360f
                                        val end = (start + slice.sweepAngle) % 360f
                                        if (end > start) {
                                            normalizedAngle >= start && normalizedAngle <= end
                                        } else {
                                            normalizedAngle >= start || normalizedAngle <= end
                                        }
                                    }

                                    if (tappedIndex >= 0) {
                                        selectedIndex = if (selectedIndex == tappedIndex) null else tappedIndex
                                        selectedIndex?.let { onCategorySelected?.invoke(slices[it].category) }
                                    }
                                }
                            }
                        }
                ) {
                    PieChartCanvas(
                        slices = slices,
                        selectedIndex = selectedIndex,
                        animationProgress = animationProgress.value
                    )
                }

                // Tooltip/Category Details Card
                AnimatedVisibility(
                    visible = selectedSlice != null,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    selectedSlice?.let { slice ->
                        Spacer(Modifier.height(16.dp))
                        CategoryDetailsCard(
                            category = slice.category,
                            deltaPct = deltaByCategory[slice.category.category_code],
                            trendLabel = getTrendLabel?.invoke(slice.category)
                        )
                    }
                }
            }
        }

        // Legend
        Spacer(Modifier.height(12.dp))
        PieChartLegend(
            slices = slices,
            selectedIndex = selectedIndex,
            onLegendClick = { index ->
                selectedIndex = if (selectedIndex == index) null else index
                selectedIndex?.let { onCategorySelected?.invoke(slices[it].category) }
            }
        )
    }
}

@Composable
private fun PieChartCanvas(
    slices: List<PieSlice>,
    selectedIndex: Int?,
    animationProgress: Float
) {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension / 2f * 0.85f // Leave some padding
        val innerRadius = radius * 0.5f // Donut style

        // Draw inner circle background first (for donut effect)
        val innerRadius = radius * 0.5f
        drawCircle(
            color = MaterialTheme.colorScheme.surface,
            radius = innerRadius * animationProgress,
            center = center
        )

        slices.forEachIndexed { index, slice ->
            val isSelected = selectedIndex == index
            val scale = if (isSelected) 1.05f else 1f
            val currentRadius = radius * scale * animationProgress
            val currentInnerRadius = innerRadius * scale * animationProgress

            // Create path for donut slice
            val path = Path().apply {
                val startAngleRad = Math.toRadians(slice.startAngle.toDouble())
                val sweepAngleRad = Math.toRadians(slice.sweepAngle.toDouble())
                val endAngleRad = startAngleRad + sweepAngleRad

                // Start at outer arc start point
                val outerStartX = center.x + currentRadius * cos(startAngleRad).toFloat()
                val outerStartY = center.y + currentRadius * sin(startAngleRad).toFloat()
                moveTo(outerStartX, outerStartY)

                // Draw outer arc
                val outerRect = androidx.compose.ui.geometry.Rect(
                    left = center.x - currentRadius,
                    top = center.y - currentRadius,
                    right = center.x + currentRadius,
                    bottom = center.y + currentRadius
                )
                arcTo(
                    rect = outerRect,
                    startAngleDegrees = slice.startAngle,
                    sweepAngleDegrees = slice.sweepAngle,
                    forceMoveTo = false
                )

                // Line to inner arc end point
                val innerEndX = center.x + currentInnerRadius * cos(endAngleRad).toFloat()
                val innerEndY = center.y + currentInnerRadius * sin(endAngleRad).toFloat()
                lineTo(innerEndX, innerEndY)

                // Draw inner arc (reverse direction)
                val innerRect = androidx.compose.ui.geometry.Rect(
                    left = center.x - currentInnerRadius,
                    top = center.y - currentInnerRadius,
                    right = center.x + currentInnerRadius,
                    bottom = center.y + currentInnerRadius
                )
                arcTo(
                    rect = innerRect,
                    startAngleDegrees = slice.startAngle + slice.sweepAngle,
                    sweepAngleDegrees = -slice.sweepAngle,
                    forceMoveTo = false
                )

                close()
            }

            // Draw slice with color
            val sliceColor = if (isSelected) {
                slice.color.copy(alpha = 0.9f)
            } else {
                slice.color.copy(alpha = 0.85f * animationProgress)
            }
            drawPath(path = path, color = sliceColor)

            // Draw glow outline if selected
            if (isSelected) {
                drawPath(
                    path = path,
                    color = slice.color.copy(alpha = 0.5f),
                    style = Stroke(width = 4.dp.toPx())
                )
            }

            // Draw percentage labels for slices > 5%
            if (slice.category.percentage >= 5f && animationProgress > 0.5f) {
                val labelAngle = slice.startAngle + slice.sweepAngle / 2f
                val labelRadius = (currentRadius + currentInnerRadius) / 2f
                val labelAngleRad = Math.toRadians(labelAngle.toDouble())
                val labelX = center.x + labelRadius * cos(labelAngleRad).toFloat()
                val labelY = center.y + labelRadius * sin(labelAngleRad).toFloat()

                // Use native canvas for rotated text
                drawContext.canvas.nativeCanvas.apply {
                    save()
                    translate(labelX, labelY)
                    rotate(labelAngle + 90f)
                    val textPaint = android.graphics.Paint().apply {
                        color = android.graphics.Color.WHITE
                        textSize = 28f
                        textAlign = android.graphics.Paint.Align.CENTER
                        isAntiAlias = true
                        typeface = android.graphics.Typeface.create(
                            android.graphics.Typeface.DEFAULT,
                            android.graphics.Typeface.BOLD
                        )
                    }
                    drawText(
                        "${slice.category.percentage.toInt()}%",
                        0f,
                        0f,
                        textPaint
                    )
                    restore()
                }
            }
        }
    }
}

@Composable
private fun CategoryDetailsCard(
    category: CategoryBreakdownItem,
    deltaPct: Double?,
    trendLabel: String?
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f)
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Text(
                text = category.category_name,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatCurrency(category.amount),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${category.percentage.toInt()}% of total",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${category.transaction_count} transactions",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    deltaPct?.let { pct ->
                        Text(
                            text = "${if (pct >= 0) "↑" else "↓"} ${kotlin.math.abs(pct).toInt()}% vs last month",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (pct <= 0) com.example.monytix.ui.theme.Success else ChartRed
                        )
                    }
                    trendLabel?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (label.contains("⚠")) ChartOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PieChartLegend(
    slices: List<PieSlice>,
    selectedIndex: Int?,
    onLegendClick: (Int) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(8.dp)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            slices.chunked(2).forEach { rowSlices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    rowSlices.forEach { slice ->
                        LegendItem(
                            slice = slice,
                            isSelected = selectedIndex == slice.index,
                            onClick = { onLegendClick(slice.index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    // Fill empty space if odd number
                    if (rowSlices.size == 1) {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun LegendItem(
    slice: PieSlice,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .background(
                color = if (isSelected) {
                    slice.color.copy(alpha = 0.2f)
                } else {
                    Color.Transparent
                },
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .background(
                    color = slice.color,
                    shape = RoundedCornerShape(2.dp)
                )
        )
        Spacer(Modifier.width(8.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slice.category.category_name,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
            Text(
                text = "${slice.category.percentage.toInt()}%",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    val formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(abs.toLong())
    return if (amount < 0) "-₹$formatted" else "₹$formatted"
}
