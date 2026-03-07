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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monytix.common.EmptyStateNoForecast
import com.example.monytix.ui.MonytixSpinner
import com.example.monytix.ui.theme.AccentPrimary
import com.example.monytix.ui.theme.Background
import com.example.monytix.ui.theme.ErrorRed
import com.example.monytix.ui.theme.SuccessGreen

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

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(Background)
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Financial Future",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
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
                modifier = Modifier
                    .fillMaxWidth()
                    .height(180.dp)
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
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Text(
                        text = savingsText,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(16.dp)
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
    modifier: Modifier = Modifier
) {
    val drawProgress = remember { Animatable(0f) }
    LaunchedEffect(points.size) {
        drawProgress.snapTo(0f)
        drawProgress.animateTo(1f, animationSpec = tween(1000))
    }
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
            if (points.size < 2) {
                Text(
                    text = "Projected cash",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                val progress = drawProgress.value
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val w = size.width
                    val h = size.height
                    val padding = 4.dp.toPx()
                    val chartH = h - 2 * padding
                    val chartW = w - 2 * padding
                    val step = chartW / (points.size - 1).coerceAtLeast(1)
                    val endIdx = (progress * (points.size - 1)).toInt().coerceIn(0, points.size - 1)
                    val lineColor = AccentPrimary
                    val fillColor = AccentPrimary.copy(alpha = 0.2f)
                    // Line path (0 to endIdx for animation)
                    val linePath = Path().apply {
                        moveTo(padding + 0, padding + (1f - points[0].second) * chartH)
                        for (i in 1..endIdx) {
                            val x = padding + i * step
                            val y = padding + (1f - points[i].second) * chartH
                            lineTo(x, y)
                        }
                    }
                    // Fill path: line segment then down to bottom
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
                }
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = tint.copy(alpha = 0.15f)),
        border = BorderStroke(1.dp, tint.copy(alpha = 0.5f))
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = tint,
            modifier = Modifier.padding(12.dp)
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
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
