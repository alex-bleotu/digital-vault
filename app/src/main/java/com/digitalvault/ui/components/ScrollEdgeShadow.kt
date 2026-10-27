package com.digitalvault.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

private val FADE_DISTANCE = 48.dp
private val SHADOW_HEIGHT = 20.dp
private const val SHADOW_MAX_ALPHA = 0.28f
private val SHADOW_STOP_FRACTIONS = listOf(0f, 0.25f, 0.5f, 0.75f, 1f)
private val SHADOW_STOP_ALPHAS = listOf(1f, 0.55f, 0.25f, 0.08f, 0f)

enum class ScrollEdge { TOP, BOTTOM }

@Composable
fun ScrollEdgeShadow(
    edge: ScrollEdge,
    scrollState: ScrollState,
    modifier: Modifier = Modifier,
) {
    val fadeDistancePx = with(LocalDensity.current) { FADE_DISTANCE.toPx() }
    val progress = remember(edge, scrollState, fadeDistancePx) {
        derivedStateOf {
            val distanceFromEdge = when (edge) {
                ScrollEdge.TOP -> scrollState.value
                ScrollEdge.BOTTOM -> scrollState.maxValue - scrollState.value
            }
            (distanceFromEdge / fadeDistancePx).coerceIn(0f, 1f)
        }
    }

    EdgeShadow(edge = edge, progress = progress, modifier = modifier)
}

@Composable
fun ScrollEdgeShadow(
    edge: ScrollEdge,
    listState: LazyListState,
    modifier: Modifier = Modifier,
) {
    val fadeDistancePx = with(LocalDensity.current) { FADE_DISTANCE.toPx() }
    val progress = remember(edge, listState, fadeDistancePx) {
        derivedStateOf {
            val distanceFromEdge = when (edge) {
                ScrollEdge.TOP -> if (listState.firstVisibleItemIndex > 0) {
                    fadeDistancePx
                } else {
                    listState.firstVisibleItemScrollOffset.toFloat()
                }
                ScrollEdge.BOTTOM -> {
                    val layoutInfo = listState.layoutInfo
                    val lastItem = layoutInfo.visibleItemsInfo.lastOrNull()
                    if (lastItem == null || lastItem.index < layoutInfo.totalItemsCount - 1) {
                        if (lastItem == null) 0f else fadeDistancePx
                    } else {
                        (lastItem.offset + lastItem.size + layoutInfo.afterContentPadding - layoutInfo.viewportEndOffset).toFloat()
                    }
                }
            }
            (distanceFromEdge / fadeDistancePx).coerceIn(0f, 1f)
        }
    }

    EdgeShadow(edge = edge, progress = progress, modifier = modifier)
}

@Composable
private fun EdgeShadow(
    edge: ScrollEdge,
    progress: State<Float>,
    modifier: Modifier,
) {
    val shadowStops = SHADOW_STOP_FRACTIONS.zip(SHADOW_STOP_ALPHAS) { fraction, alpha ->
        val stopFraction = if (edge == ScrollEdge.TOP) fraction else 1f - fraction

        stopFraction to Color.Black.copy(alpha = SHADOW_MAX_ALPHA * alpha)
    }.sortedBy { it.first }.toTypedArray()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(SHADOW_HEIGHT)
            .graphicsLayer { alpha = progress.value }
            .background(Brush.verticalGradient(colorStops = shadowStops)),
    )
}
