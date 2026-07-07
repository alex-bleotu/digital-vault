package com.digitalvault.ui.onboarding

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.digitalvault.ui.theme.VaultTheme

@Composable
fun OnboardingProgressDial(
    steps: List<StepStatus>,
    currentIndex: Int,
    modifier: Modifier = Modifier,
) {
    val colors = VaultTheme.colors
    val completedCount = steps.count { it.isComplete }
    val gapDegrees = 6f
    val segmentSweep = (360f - gapDegrees * steps.size) / steps.size

    val infiniteTransition = rememberInfiniteTransition(label = "currentStepPulse")
    val currentStepAlpha by infiniteTransition.animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "currentStepAlpha",
    )

    Box(
        modifier = modifier.size(132.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(132.dp)) {
            val strokeWidth = 10.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            steps.forEachIndexed { index, status ->
                val startAngle = -90f + index * (segmentSweep + gapDegrees)
                val segmentColor = when {
                    status.isComplete -> colors.brass
                    index == currentIndex -> colors.brass.copy(alpha = currentStepAlpha)
                    else -> colors.surfaceRaised
                }
                drawArc(
                    color = segmentColor,
                    startAngle = startAngle,
                    sweepAngle = segmentSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Butt),
                )
            }
        }
        Text(
            text = "$completedCount / ${steps.size}",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
        )
    }
}
