package com.example.monytix.spendsense.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.monytix.data.CategoryBreakdownItem
import com.example.monytix.data.SubcategoryResponse
import com.example.monytix.ui.theme.AccentPrimary
import com.example.monytix.ui.theme.ChartBlue
import com.example.monytix.ui.theme.ChartGreen
import com.example.monytix.ui.theme.ChartOrange
import com.example.monytix.ui.theme.ChartPurple
import com.example.monytix.ui.theme.ChartRed
import com.example.monytix.ui.theme.GlassCard
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin

data class PieSlice(
    val category: CategoryBreakdownItem,
    val startAngle: Float,
    val sweepAngle: Float,
    val color: Color,
    val gradientColors: List<Color>,
    val index: Int
)

data class SubcategoryBreakdownItem(
    val subcategory_code: String,
    val subcategory_name: String,
    val amount: Double,
    val percentage: Double,
    val transaction_count: Int
)

@Composable
fun InteractivePieChart(
    data: List<CategoryBreakdownItem>,
    totalAmount: Double,
    deltaByCategory: Map<String, Double?>,
    onCategorySelected: ((CategoryBreakdownItem) -> Unit)? = null,
    getTrendLabel: ((CategoryBreakdownItem) -> String?)? = null,
    subcategoryBreakdown: List<SubcategoryBreakdownItem> = emptyList(),
    onLoadSubcategoryBreakdown: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    if (data.isEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GlassCard),
            shape = RoundedCornerShape(16.dp)
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

    // Enhanced color palette with gradients
    val chartColorSchemes = remember {
        listOf(
            listOf(AccentPrimary, AccentPrimary.copy(alpha = 0.7f)), // Teal gradient
            listOf(ChartRed, ChartRed.copy(alpha = 0.7f)), // Pink gradient
            listOf(ChartPurple, ChartPurple.copy(alpha = 0.6f)), // Purple gradient
            listOf(ChartGreen, ChartGreen.copy(alpha = 0.7f)), // Green gradient
            listOf(ChartBlue, ChartBlue.copy(alpha = 0.7f)), // Blue gradient
            listOf(ChartOrange, ChartOrange.copy(alpha = 0.7f)), // Orange gradient
            listOf(AccentPrimary.copy(alpha = 0.8f), AccentPrimary.copy(alpha = 0.5f)),
            listOf(ChartRed.copy(alpha = 0.8f), ChartRed.copy(alpha = 0.5f)),
            listOf(ChartPurple.copy(alpha = 0.8f), ChartPurple.copy(alpha = 0.5f)),
            listOf(ChartGreen.copy(alpha = 0.8f), ChartGreen.copy(alpha = 0.5f)),
            listOf(ChartBlue.copy(alpha = 0.8f), ChartBlue.copy(alpha = 0.5f)),
            listOf(ChartOrange.copy(alpha = 0.8f), ChartOrange.copy(alpha = 0.5f))
        )
    }

    // Prepare slices with angles and gradients
    val slices = remember(data, chartColorSchemes) {
        var currentAngle = -90f // Start from top
        data.mapIndexed { index, item ->
            val sweepAngle = ((item.percentage.toFloat() / 100f) * 360f)
            val colors = chartColorSchemes[index % chartColorSchemes.size]
            val slice = PieSlice(
                category = item,
                startAngle = currentAngle,
                sweepAngle = sweepAngle,
                color = colors[0],
                gradientColors = colors,
                index = index
            )
            currentAngle += sweepAngle
            slice
        }
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    var showSubcategories by remember { mutableStateOf(false) }
    val animationProgress = remember { Animatable(0f) }
    val selectedScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(1000, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(selectedIndex) {
        if (selectedIndex != null) {
            selectedScale.animateTo(
                targetValue = 1.08f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        } else {
            selectedScale.animateTo(1f)
        }
    }

    val selectedSlice = selectedIndex?.let { slices.getOrNull(it) }

    Column(modifier = modifier) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = GlassCard),
            shape = RoundedCornerShape(20.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(300.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val radius = kotlin.math.min(size.width, size.height) / 2f
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
                                        val wasSelected = selectedIndex == tappedIndex
                                        selectedIndex = if (wasSelected) null else tappedIndex
                                        if (selectedIndex != null && !wasSelected) {
                                            onCategorySelected?.invoke(slices[selectedIndex!!].category)
                                            onLoadSubcategoryBreakdown?.invoke(slices[selectedIndex!!].category.category_code)
                                            showSubcategories = false
                                        } else {
                                            showSubcategories = false
                                        }
                                    }
                                }
                            }
                        }
                ) {
                    PieChartCanvas(
                        slices = slices,
                        selectedIndex = selectedIndex,
                        animationProgress = animationProgress.value,
                        selectedScale = selectedScale.value
                    )
                }

                // Enhanced Category Details Card
                AnimatedVisibility(
                    visible = selectedSlice != null && !showSubcategories,
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f),
                    exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f)
                ) {
                    selectedSlice?.let { slice ->
                        Spacer(Modifier.height(20.dp))
                        CategoryDetailsCard(
                            category = slice.category,
                            deltaPct = deltaByCategory[slice.category.category_code],
                            trendLabel = getTrendLabel?.invoke(slice.category),
                            hasSubcategories = subcategoryBreakdown.isNotEmpty(),
                            onShowSubcategories = { 
                                onLoadSubcategoryBreakdown?.invoke(slice.category.category_code)
                                showSubcategories = true 
                            }
                        )
                    }
                }

                // Subcategories Pie Chart View
                AnimatedVisibility(
                    visible = selectedSlice != null && showSubcategories && subcategoryBreakdown.isNotEmpty(),
                    enter = fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.9f),
                    exit = fadeOut(animationSpec = tween(200)) + scaleOut(targetScale = 0.9f)
                ) {
                    selectedSlice?.let { slice ->
                        Spacer(Modifier.height(20.dp))
                        SubcategoryPieChart(
                            categoryName = slice.category.category_name,
                            categoryAmount = slice.category.amount,
                            subcategoryBreakdown = subcategoryBreakdown,
                            onBack = { showSubcategories = false }
                        )
                    }
                }
            }
        }

        // Enhanced Legend
        Spacer(Modifier.height(16.dp))
        PieChartLegend(
            slices = slices,
            selectedIndex = selectedIndex,
            onLegendClick = { index ->
                val wasSelected = selectedIndex == index
                selectedIndex = if (wasSelected) null else index
                if (selectedIndex != null && !wasSelected) {
                    onCategorySelected?.invoke(slices[selectedIndex!!].category)
                    onLoadSubcategoryBreakdown?.invoke(slices[selectedIndex!!].category.category_code)
                    showSubcategories = false
                } else {
                    showSubcategories = false
                }
            }
        )
    }
}

@Composable
private fun PieChartCanvas(
    slices: List<PieSlice>,
    selectedIndex: Int?,
    animationProgress: Float,
    selectedScale: Float
) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    Canvas(modifier = Modifier.fillMaxSize()) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val minSize = kotlin.math.min(size.width, size.height)
        val radius = minSize / 2f * 0.82f
        val innerRadius = radius * 0.48f

        // Draw inner circle with subtle gradient (ensure radius > 0)
        // Only draw if animation has progressed enough to avoid RadialGradient crash
        if (animationProgress > 0.01f) {
            val animatedInnerRadius = (innerRadius * animationProgress).coerceAtLeast(5f)
            val gradientRadius = animatedInnerRadius.coerceAtLeast(5f) // Minimum 5px for RadialGradient
            if (gradientRadius > 0f && animatedInnerRadius > 0f) {
                drawCircle(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            surfaceColor.copy(alpha = 0.95f),
                            surfaceColor.copy(alpha = 0.85f)
                        ),
                        center = center,
                        radius = gradientRadius
                    ),
                    radius = animatedInnerRadius,
                    center = center
                )
            }
        }

        slices.forEachIndexed { index, slice ->
            val isSelected = selectedIndex == index
            val scale = if (isSelected) selectedScale else 1f
            val baseRadius = radius * scale * animationProgress
            val baseInnerRadius = innerRadius * scale * animationProgress

            // Skip drawing if radius is too small (avoids RadialGradient crash)
            if (baseRadius < 1f || animationProgress <= 0.01f) {
                return@forEachIndexed
            }

            val currentRadius = baseRadius.coerceAtLeast(1f)
            val currentInnerRadius = baseInnerRadius.coerceAtLeast(0.5f)

            // Calculate center point for gradient
            val midAngle = slice.startAngle + slice.sweepAngle / 2f
            val midAngleRad = Math.toRadians(midAngle.toDouble())
            val gradientCenter = Offset(
                center.x + currentRadius * 0.6f * cos(midAngleRad).toFloat(),
                center.y + currentRadius * 0.6f * sin(midAngleRad).toFloat()
            )

            // Create path for donut slice
            val path = Path().apply {
                val startAngleRad = Math.toRadians(slice.startAngle.toDouble())
                val sweepAngleRad = Math.toRadians(slice.sweepAngle.toDouble())
                val endAngleRad = startAngleRad + sweepAngleRad

                val outerStartX = center.x + currentRadius * cos(startAngleRad).toFloat()
                val outerStartY = center.y + currentRadius * sin(startAngleRad).toFloat()
                moveTo(outerStartX, outerStartY)

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

                val innerEndX = center.x + currentInnerRadius * cos(endAngleRad).toFloat()
                val innerEndY = center.y + currentInnerRadius * sin(endAngleRad).toFloat()
                lineTo(innerEndX, innerEndY)

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

            // Draw slice with radial gradient (ensure radius > 0)
            val alpha = if (isSelected) 1f else 0.88f * animationProgress
            val gradientRadius = (currentRadius * 1.2f).coerceAtLeast(10f) // Minimum 10px to avoid crash
            if (gradientRadius > 0f) {
                drawPath(
                    path = path,
                    brush = Brush.radialGradient(
                        colors = slice.gradientColors.map { it.copy(alpha = alpha) },
                        center = gradientCenter,
                        radius = gradientRadius
                    )
                )
            } else {
                // Fallback to solid color if gradient fails
                drawPath(path = path, color = slice.color.copy(alpha = alpha))
            }

            // Enhanced glow effect for selected slice
            if (isSelected) {
                val glowRadius = (currentRadius * 1.5f).coerceAtLeast(10f) // Minimum 10px
                if (glowRadius > 0f) {
                    // Outer glow
                    drawPath(
                        path = path,
                        brush = Brush.radialGradient(
                            colors = listOf(
                                slice.color.copy(alpha = 0.3f),
                                slice.color.copy(alpha = 0.0f)
                            ),
                            center = gradientCenter,
                            radius = glowRadius
                        ),
                        style = Stroke(width = 6.dp.toPx())
                    )
                }
                // Inner highlight
                drawPath(
                    path = path,
                    color = Color.White.copy(alpha = 0.15f),
                    style = Stroke(width = 2.dp.toPx())
                )
            }
        }
    }
}

@Composable
private fun CategoryDetailsCard(
    category: CategoryBreakdownItem,
    deltaPct: Double?,
    trendLabel: String?,
    hasSubcategories: Boolean,
    onShowSubcategories: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.category_name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (hasSubcategories) {
                    Text(
                        text = "View Subcategories →",
                        style = MaterialTheme.typography.labelMedium,
                        color = AccentPrimary,
                        modifier = Modifier
                            .background(
                                color = AccentPrimary.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                            .pointerInput(Unit) {
                                detectTapGestures { onShowSubcategories() }
                            }
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = formatCurrency(category.amount),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "${category.percentage.toInt()}% of total",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "${category.transaction_count} transactions",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                    deltaPct?.let { pct ->
                        Text(
                            text = "${if (pct >= 0) "↑" else "↓"} ${kotlin.math.abs(pct).toInt()}% vs last month",
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                            color = if (pct <= 0) com.example.monytix.ui.theme.Success else ChartRed
                        )
                    }
                    trendLabel?.let { label ->
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = if (label.contains("⚠")) ChartOrange else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubcategoryPieChart(
    categoryName: String,
    categoryAmount: Double,
    subcategoryBreakdown: List<SubcategoryBreakdownItem>,
    onBack: () -> Unit
) {
    val subcategoryColors = remember {
        listOf(
            listOf(AccentPrimary, AccentPrimary.copy(alpha = 0.7f)),
            listOf(ChartRed, ChartRed.copy(alpha = 0.7f)),
            listOf(ChartPurple, ChartPurple.copy(alpha = 0.6f)),
            listOf(ChartGreen, ChartGreen.copy(alpha = 0.7f)),
            listOf(ChartBlue, ChartBlue.copy(alpha = 0.7f)),
            listOf(ChartOrange, ChartOrange.copy(alpha = 0.7f)),
            listOf(AccentPrimary.copy(alpha = 0.8f), AccentPrimary.copy(alpha = 0.5f)),
            listOf(ChartRed.copy(alpha = 0.8f), ChartRed.copy(alpha = 0.5f))
        )
    }

    val subcategorySlices = remember(subcategoryBreakdown, subcategoryColors) {
        var currentAngle = -90f
        subcategoryBreakdown.mapIndexed { index, item ->
            val sweepAngle = ((item.percentage.toFloat() / 100f) * 360f)
            val colors = subcategoryColors[index % subcategoryColors.size]
            val slice = PieSlice(
                category = CategoryBreakdownItem(
                    category_code = item.subcategory_code,
                    category_name = item.subcategory_name,
                    amount = item.amount,
                    percentage = item.percentage,
                    transaction_count = item.transaction_count,
                    avg_transaction = if (item.transaction_count > 0) item.amount / item.transaction_count else 0.0
                ),
                startAngle = currentAngle,
                sweepAngle = sweepAngle,
                color = colors[0],
                gradientColors = colors,
                index = index
            )
            currentAngle += sweepAngle
            slice
        }
    }

    var selectedSubcategoryIndex by remember { mutableStateOf<Int?>(null) }
    val subcategoryAnimationProgress = remember { Animatable(0f) }
    val subcategorySelectedScale = remember { Animatable(1f) }

    LaunchedEffect(Unit) {
        subcategoryAnimationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(800, easing = FastOutSlowInEasing)
        )
    }

    LaunchedEffect(selectedSubcategoryIndex) {
        if (selectedSubcategoryIndex != null) {
            subcategorySelectedScale.animateTo(
                targetValue = 1.08f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        } else {
            subcategorySelectedScale.animateTo(1f)
        }
    }

    val selectedSubcategory = selectedSubcategoryIndex?.let { subcategorySlices.getOrNull(it) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subcategories: $categoryName",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = formatCurrency(categoryAmount),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                    )
                }
            }
            
            if (subcategoryBreakdown.isEmpty()) {
                Spacer(Modifier.height(20.dp))
                Text(
                    text = "No subcategory data available",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            } else {
                Spacer(Modifier.height(16.dp))
                Box(
                    modifier = Modifier
                        .size(250.dp)
                        .pointerInput(Unit) {
                            detectTapGestures { tapOffset ->
                                val center = Offset(size.width / 2f, size.height / 2f)
                                val radius = kotlin.math.min(size.width, size.height) / 2f
                                val dx = tapOffset.x - center.x
                                val dy = tapOffset.y - center.y
                                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                                if (distance <= radius) {
                                    val angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                    val normalizedAngle = (angle + 90f + 360f) % 360f

                                    val tappedIndex = subcategorySlices.indexOfFirst { slice ->
                                        val start = (slice.startAngle + 90f + 360f) % 360f
                                        val end = (start + slice.sweepAngle) % 360f
                                        if (end > start) {
                                            normalizedAngle >= start && normalizedAngle <= end
                                        } else {
                                            normalizedAngle >= start || normalizedAngle <= end
                                        }
                                    }

                                    if (tappedIndex >= 0) {
                                        selectedSubcategoryIndex = if (selectedSubcategoryIndex == tappedIndex) null else tappedIndex
                                    }
                                }
                            }
                        }
                ) {
                    PieChartCanvas(
                        slices = subcategorySlices,
                        selectedIndex = selectedSubcategoryIndex,
                        animationProgress = subcategoryAnimationProgress.value,
                        selectedScale = subcategorySelectedScale.value
                    )
                }

                // Selected subcategory details
                AnimatedVisibility(
                    visible = selectedSubcategory != null,
                    enter = fadeIn(animationSpec = tween(300)),
                    exit = fadeOut(animationSpec = tween(200))
                ) {
                    selectedSubcategory?.let { slice ->
                        Spacer(Modifier.height(16.dp))
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = slice.color.copy(alpha = 0.15f)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp)
                            ) {
                                Text(
                                    text = slice.category.category_name,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = formatCurrency(slice.category.amount),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = "${slice.category.percentage.toInt()}% • ${slice.category.transaction_count} txns",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                                    )
                                }
                            }
                        }
                    }
                }

                // Subcategory legend
                Spacer(Modifier.height(12.dp))
                PieChartLegend(
                    slices = subcategorySlices,
                    selectedIndex = selectedSubcategoryIndex,
                    onLegendClick = { index ->
                        selectedSubcategoryIndex = if (selectedSubcategoryIndex == index) null else index
                    }
                )
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
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            slices.chunked(2).forEach { rowSlices ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    rowSlices.forEach { slice ->
                        LegendItem(
                            slice = slice,
                            isSelected = selectedIndex == slice.index,
                            onClick = { onLegendClick(slice.index) },
                            modifier = Modifier.weight(1f)
                        )
                    }
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
                brush = if (isSelected) {
                    Brush.horizontalGradient(
                        colors = listOf(
                            slice.color.copy(alpha = 0.25f),
                            slice.color.copy(alpha = 0.1f)
                        )
                    )
                } else {
                    Brush.horizontalGradient(
                        colors = listOf(Color.Transparent, Color.Transparent)
                    )
                },
                shape = RoundedCornerShape(10.dp)
            )
            .padding(horizontal = 10.dp, vertical = 8.dp)
            .pointerInput(Unit) {
                detectTapGestures { onClick() }
            },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start
    ) {
        Box(
            modifier = Modifier
                .size(14.dp)
                .background(
                    brush = Brush.radialGradient(
                        colors = slice.gradientColors,
                        center = Offset(7f, 7f),
                        radius = 7f
                    ),
                    shape = RoundedCornerShape(3.dp)
                )
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = slice.category.category_name,
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
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
