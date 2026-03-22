package com.example.monytix.future

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monytix.R
import com.example.monytix.common.EmptyStateNoForecast
import com.example.monytix.ui.MonytixSpinner
import androidx.compose.material3.Button
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import com.example.monytix.ui.theme.AccentPrimary
import com.example.monytix.ui.theme.Background
import com.example.monytix.ui.theme.ErrorRed
import com.example.monytix.ui.theme.SuccessGreen
import com.example.monytix.ui.theme.MonytixSpacing
import com.example.monytix.ui.theme.MonytixRadius
import com.example.monytix.ui.theme.Warning

@Composable
fun FutureScreen(
    viewModel: FutureViewModel = viewModel(),
    modifier: Modifier = Modifier,
    onUploadStatement: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    if (uiState.isLoading && !uiState.hasData) {
        FutureSkeleton(modifier = modifier.fillMaxSize().background(Background))
        return
    }

    if (!uiState.hasData && uiState.errorMessage == null) {
        Box(
            modifier = modifier.fillMaxSize().background(Background),
            contentAlignment = Alignment.Center
        ) {
            EmptyStateNoForecast(onUploadStatement = onUploadStatement)
        }
        return
    }

    if (uiState.errorMessage != null && !uiState.hasData) {
        FutureErrorState(
            modifier = modifier.fillMaxSize().background(Background),
            message = uiState.errorMessage!!,
            onRetry = { viewModel.refresh() }
        )
        return
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = MonytixSpacing.cardPaddingCompact, vertical = MonytixSpacing.betweenCards),
        verticalArrangement = Arrangement.spacedBy(MonytixSpacing.betweenCards)
    ) {
        item {
            FutureHero(
                message = uiState.heroMessage.ifEmpty { "On track — no shortfall in the next 30 days" },
                isRisk = uiState.heroIsRisk
            )
        }
        item {
            Text(
                text = uiState.confidenceLabel,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            ForecastChartPlaceholder(
                points = uiState.projectionPoints,
                lowPointIndex = uiState.lowPointDayIndex,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
            )
        }
        uiState.riskStripLabel?.let { label ->
            item {
                RiskStripCard(
                    label = label,
                    severity = uiState.riskStripSeverity
                )
            }
        }
        uiState.savingsOpportunity?.let { savingsText ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(MonytixRadius.secondary),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = savingsText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(MonytixSpacing.cardPaddingCompact)
                    )
                }
            }
        }
        if (uiState.recommendations.isNotEmpty()) {
            item {
                Text(
                    text = "Recommendations",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
            items(uiState.recommendations) { rec ->
                RecommendationCard(
                    title = rec.title,
                    body = rec.body
                )
            }
        }
    }
}

@Composable
private fun FutureHero(
    message: String,
    isRisk: Boolean,
    modifier: Modifier = Modifier
) {
    val color = if (isRisk) Warning else AccentPrimary
    Text(
        text = message,
        style = MaterialTheme.typography.headlineSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = modifier
    )
}

@Composable
private fun FutureErrorState(
    modifier: Modifier = Modifier,
    message: String,
    onRetry: () -> Unit
) {
    Column(
        modifier = modifier.padding(MonytixSpacing.cardPaddingPrimary),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(MonytixSpacing.betweenCards))
        Button(
            onClick = onRetry,
            colors = androidx.compose.material3.ButtonDefaults.buttonColors(containerColor = AccentPrimary, contentColor = MaterialTheme.colorScheme.onPrimary)
        ) {
            Text("Retry")
        }
    }
}

@Composable
private fun FutureSkeleton(modifier: Modifier = Modifier) {
    val infiniteTransition = rememberInfiniteTransition(label = "future_skeleton")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.7f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "skeleton_alpha"
    )
    val colorScheme = MaterialTheme.colorScheme
    val skeletonColor = colorScheme.onSurface.copy(alpha = alpha)
    LazyColumn(
        modifier = modifier.padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = stringResource(R.string.loading_forecast),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            Box(
                modifier = Modifier
                    .height(28.dp)
                    .fillMaxWidth(0.5f)
                    .background(skeletonColor, RoundedCornerShape(6.dp))
            )
        }
        item {
            Box(
                modifier = Modifier
                    .height(18.dp)
                    .fillMaxWidth(0.4f)
                    .background(skeletonColor, RoundedCornerShape(4.dp))
            )
        }
        item {
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .background(skeletonColor, RoundedCornerShape(16.dp))
                )
            }
        }
        repeat(2) {
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Box(
                            modifier = Modifier
                                .height(18.dp)
                                .fillMaxWidth(0.6f)
                                .background(skeletonColor, RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .height(14.dp)
                                .fillMaxWidth()
                                .background(skeletonColor, RoundedCornerShape(4.dp))
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ForecastChartPlaceholder(
    points: List<Pair<Float, Float>>,
    lowPointIndex: Int? = null,
    modifier: Modifier = Modifier
) {
    val drawProgress = remember { Animatable(0f) }
    LaunchedEffect(points.size) {
        drawProgress.snapTo(0f)
        drawProgress.animateTo(1f, animationSpec = tween(1000))
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(MonytixRadius.secondary),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(MonytixSpacing.cardPaddingCompact)) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                if (points.size < 2) {
                    Text(
                        text = "Projected cash",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                } else {
                    val progress = drawProgress.value
                    val todayLineColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val w = size.width
                        val h = size.height
                        val padding = 8.dp.toPx()
                        val chartH = h - 2 * padding
                        val chartW = w - 2 * padding
                        val step = chartW / (points.size - 1).coerceAtLeast(1)
                        val endIdx = (progress * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                        val lineColor = AccentPrimary
                        val fillColor = AccentPrimary.copy(alpha = 0.18f)
                        val linePath = Path().apply {
                            moveTo(padding + 0, padding + (1f - points[0].second) * chartH)
                            for (i in 1..endIdx) {
                                val x = padding + i * step
                                val y = padding + (1f - points[i].second) * chartH
                                lineTo(x, y)
                            }
                        }
                        val fillPath = Path().apply {
                            moveTo(padding + 0, padding + (1f - points[0].second) * chartH)
                            for (i in 1..endIdx) {
                                val x = padding + i * step
                                val y = padding + (1f - points[i].second) * chartH
                                lineTo(x, y)
                            }
                            val lastX = padding + endIdx * step
                            lineTo(lastX, padding + chartH)
                            lineTo(padding, padding + chartH)
                            close()
                        }
                        drawPath(fillPath, fillColor)
                        drawPath(linePath, lineColor, style = Stroke(width = 2.dp.toPx()))
                        // Today marker (vertical line at x=0)
                        drawLine(
                            color = todayLineColor,
                            start = Offset(padding, padding),
                            end = Offset(padding, padding + chartH),
                            strokeWidth = 1.dp.toPx()
                        )
                        // Low point marker (dot)
                        lowPointIndex?.takeIf { it < points.size }?.let { idx ->
                            val lx = padding + idx * step
                            val ly = padding + (1f - points[idx].second) * chartH
                            drawCircle(
                                color = Warning,
                                radius = 5.dp.toPx(),
                                center = Offset(lx, ly)
                            )
                        }
                    }
                }
            }
            // X-axis labels: Today | Day 15 | Day 30
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Today", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Day 15", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Day 30", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun RiskStripCard(
    label: String,
    severity: String,
    modifier: Modifier = Modifier
) {
    val tint = when (severity) {
        "warning" -> Color(0xFFFF9F43)
        "danger" -> ErrorRed
        else -> SuccessGreen
    }
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MonytixRadius.compact),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.5f))
    ) {
        Text(
            text = if (label.startsWith("⚠")) label else "⚠ $label",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = tint,
            modifier = Modifier.padding(MonytixSpacing.cardPaddingCompact)
        )
    }
}

@Composable
private fun RecommendationCard(
    title: String,
    body: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MonytixRadius.secondary),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(MonytixSpacing.cardPaddingCompact)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = body,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
