package com.example.monytix.ui

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.monytix.ui.theme.CyanPrimary
import com.example.monytix.ui.theme.CyanSecondary
import com.example.monytix.ui.theme.Warning

@Composable
fun MonytixRing(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    stroke: Dp = 10.dp,
    gapDegrees: Float = 12f,
    rotationDegrees: Float = 0f,
    colors: List<Color> = listOf(
        CyanPrimary,
        Warning,
        CyanSecondary,
        Color(0xFFE7A93B)
    ),
    alpha: Float = 1f,
    progress: Float? = null,
) {
    Canvas(modifier = modifier.size(size)) {
        val strokePx = stroke.toPx()
        val diameter = this.size.minDimension
        val topLeft = Offset(strokePx / 2, strokePx / 2)
        val arcSize = Size(diameter - strokePx, diameter - strokePx)

        val segmentCount = colors.size
        val segmentFull = 360f / segmentCount
        val segmentSweep = segmentFull - gapDegrees

        val totalAvailableSweep = segmentCount * segmentSweep
        val allowedSweep = progress?.coerceIn(0f, 1f)?.let { it * totalAvailableSweep } ?: totalAvailableSweep

        var drawn = 0f

        rotate(rotationDegrees) {
            for (i in 0 until segmentCount) {
                if (drawn >= allowedSweep) break

                val remaining = allowedSweep - drawn
                val sweep = minOf(segmentSweep, remaining)

                val start = i * segmentFull + gapDegrees / 2

                drawArc(
                    color = colors[i].copy(alpha = alpha),
                    startAngle = start,
                    sweepAngle = sweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )

                drawn += sweep
            }
        }
    }
}

@Composable
fun MonytixSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    stroke: Dp = 10.dp,
    durationMs: Int = 900
) {
    val infinite = rememberInfiniteTransition(label = "monytix_spin")
    val rotation by infinite.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMs, easing = LinearEasing)
        ),
        label = "rotation"
    )

    MonytixRing(
        modifier = modifier,
        size = size,
        stroke = stroke,
        rotationDegrees = rotation
    )
}

@Composable
fun MonytixProgressRing(
    progress: Float,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    stroke: Dp = 10.dp
) {
    MonytixRing(
        modifier = modifier,
        size = size,
        stroke = stroke,
        rotationDegrees = -90f,
        progress = progress
    )
}

@Composable
fun PremiumMonytixSpinner(
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    stroke: Dp = 10.dp
) {
    val infinite = rememberInfiniteTransition(label = "premium")
    val scale by infinite.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Box(
        modifier = modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
        },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 1.7f)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        listOf(CyanPrimary.copy(alpha = 0.14f), Color.Transparent)
                    )
                )
        )
        MonytixSpinner(size = size, stroke = stroke)
    }
}
