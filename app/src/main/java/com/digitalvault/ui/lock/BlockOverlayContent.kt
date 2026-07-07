package com.digitalvault.ui.lock

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.digitalvault.ui.theme.VaultTheme
import kotlinx.coroutines.launch

private val MOTIVATIONAL_QUOTES = listOf(
    "Discipline is choosing between what you want now and what you want most.",
    "The urge is temporary. The version of you who resists it is permanent.",
    "You didn't come this far to stop for a scroll.",
    "Every time you resist, you get a little stronger.",
    "Boredom is not an emergency.",
    "Future you is watching. Don't let them down.",
    "This feeling passes in minutes. Regret lasts longer.",
    "You built this wall. Trust the version of you that did.",
    "Nothing on the other side of this app is worth what it costs you.",
    "Small no's build a life of big yeses.",
)

data class BlockOverlayRequest(
    val appLabel: String,
    val appIcon: ImageBitmap?,
    val dismissMinutes: Int,
    val holdMillis: Int,
    val reduceMotion: Boolean,
    val allowBreak: Boolean = true,
    val breakUsedToday: Boolean = false,
)

@Composable
fun BlockOverlayContent(
    request: BlockOverlayRequest,
    onDismissForBreak: () -> Unit,
    onExit: () -> Unit,
) {
    val colors = VaultTheme.colors
    val shutter = remember { Animatable(if (request.reduceMotion) 1f else 0f) }

    LaunchedEffect(Unit) {
        if (!request.reduceMotion) {
            shutter.animateTo(1f, tween(durationMillis = 460, easing = FastOutSlowInEasing))
        }
    }

    val shutterProgress = shutter.value
    val contentAlpha = ((shutterProgress - 0.65f) / 0.35f).coerceIn(0f, 1f)
    val quote = remember { MOTIVATIONAL_QUOTES.random() }

    Box(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.TopCenter)
                .graphicsLayer { translationY = -size.height * (1f - shutterProgress) }
                .background(colors.ink),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.5f)
                .align(Alignment.BottomCenter)
                .graphicsLayer { translationY = size.height * (1f - shutterProgress) }
                .background(colors.ink),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .align(Alignment.Center)
                .graphicsLayer { alpha = shutterProgress * (1f - contentAlpha) }
                .background(colors.brass),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer { alpha = contentAlpha },
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.TopCenter)
                    .padding(bottom = 28.dp),
                contentAlignment = Alignment.BottomCenter,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (request.appIcon != null) {
                        Image(
                            bitmap = request.appIcon,
                            contentDescription = null,
                            modifier = Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(14.dp)),
                        )
                        Spacer(Modifier.height(20.dp))
                    }

                    Text(
                        text = "Not now.",
                        style = MaterialTheme.typography.headlineMedium,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = "${request.appLabel} is locked.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    Text(
                        text = "“$quote”",
                        style = MaterialTheme.typography.bodyMedium.copy(fontStyle = FontStyle.Italic),
                        color = colors.brass,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            HorizontalDivider(
                color = colors.surfaceRaised,
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.5f)
                    .align(Alignment.BottomCenter)
                    .padding(top = 28.dp),
                contentAlignment = Alignment.TopCenter,
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    if (request.allowBreak) {
                        HoldToDismiss(
                            holdMillis = request.holdMillis,
                            onComplete = onDismissForBreak,
                        )
                        Spacer(Modifier.height(20.dp))
                        Text(
                            text = "HOLD TO OPEN FOR ${request.dismissMinutes} MINUTES",
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(40.dp),
                        )
                        Spacer(Modifier.height(16.dp))
                        Text(
                            text = if (request.breakUsedToday) {
                                "TODAY'S BREAK IS USED. TRY AGAIN TOMORROW"
                            } else {
                                "BREAKS ARE TURNED OFF FOR THIS APP"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = colors.textMuted,
                            textAlign = TextAlign.Center,
                        )
                    }

                    Spacer(Modifier.height(28.dp))
                    OutlinedButton(
                        onClick = onExit,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brass),
                    ) {
                        Text(text = "Go to Home Screen", color = colors.brass)
                    }
                }
            }
        }
    }
}

@Composable
private fun HoldToDismiss(
    holdMillis: Int,
    onComplete: () -> Unit,
) {
    val colors = VaultTheme.colors
    val haptics = LocalHapticFeedback.current
    val holdProgress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    var isCompleted by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .size(140.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val holdJob = scope.launch {
                        holdProgress.animateTo(
                            targetValue = 1f,
                            animationSpec = tween(durationMillis = holdMillis, easing = LinearEasing),
                        )
                        if (!isCompleted) {
                            isCompleted = true
                            haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                            onComplete()
                        }
                    }
                    waitForUpOrCancellation()
                    holdJob.cancel()
                    if (!isCompleted) {
                        scope.launch {
                            holdProgress.animateTo(0f, tween(durationMillis = 220, easing = LinearEasing))
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(140.dp)) {
            val strokeWidth = 10.dp.toPx()
            val inset = strokeWidth / 2f
            val arcSize = Size(size.width - strokeWidth, size.height - strokeWidth)
            val topLeft = Offset(inset, inset)

            drawArc(
                color = colors.surfaceRaised,
                startAngle = 0f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
            drawArc(
                color = colors.brass,
                startAngle = -90f,
                sweepAngle = 360f * holdProgress.value,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }
        Text(
            text = "HOLD",
            style = MaterialTheme.typography.labelLarge,
            color = colors.textPrimary,
        )
    }
}
