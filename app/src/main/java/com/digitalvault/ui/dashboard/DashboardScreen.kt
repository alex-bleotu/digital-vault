package com.digitalvault.ui.dashboard

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlin.math.roundToInt
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalvault.ui.components.PermissionHealth
import com.digitalvault.ui.components.ScrollEdge
import com.digitalvault.ui.components.ScrollEdgeShadow
import com.digitalvault.ui.components.PermissionHealthRows
import com.digitalvault.ui.navigation.VaultDestination
import com.digitalvault.ui.theme.VaultTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.delay

private val HEADER_BLUR_RADIUS = 20.dp
private val HEADER_FADE_HEIGHT = 16.dp
private const val HEADER_TINT_ALPHA = 0.7f

private val LastCheckedFormatter = DateTimeFormatter.ofPattern("HH:mm · d MMM")

@Composable
fun DashboardScreen(
    onNavigate: (VaultDestination) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DashboardViewModel = viewModel(),
) {
    val colors = VaultTheme.colors
    val state = viewModel.uiState
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLiveChecks()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    var now by remember { mutableStateOf(Instant.now()) }
    var showRestartVpnDialog by remember { mutableStateOf(false) }

    LaunchedEffect(state.standDownUntil) {
        while (state.standDownUntil?.isAfter(Instant.now()) == true) {
            delay(1000)
            now = Instant.now()
        }
    }

    val hazeState = rememberHazeState()
    val density = LocalDensity.current
    var headerHeight by remember { mutableStateOf(0.dp) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ink),
    ) {
        val dashboardScrollState = rememberScrollState()

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .hazeSource(hazeState),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(dashboardScrollState)
                    .heightIn(min = maxHeight)
                    .padding(start = 24.dp, top = headerHeight, end = 24.dp, bottom = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                MasterDial(
                    fraction = state.protectionFraction,
                    activeCount = state.activeCount,
                    totalCount = state.totalCount,
                    isStoodDown = state.isStoodDown,
                )

                val health = PermissionHealth(
                    isAccessibilityLive = state.isAccessibilityLive,
                    isOverlayLive = state.isOverlayLive,
                    isAdminLive = state.isAdminLive,
                    isBatteryLive = state.isBatteryLive,
                    areNotificationsEnabled = state.areNotificationsEnabled,
                )
                if (health.hasIssues) {
                    Spacer(Modifier.height(24.dp))
                    Surface(
                        shape = VaultTheme.shapes.medium,
                        color = colors.surface,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Column(modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp)) {
                            Text(
                                text = "NEEDS ATTENTION",
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.rust,
                            )
                            Spacer(Modifier.height(8.dp))
                            PermissionHealthRows(
                                health = health,
                                onPermissionResult = viewModel::refreshLiveChecks,
                                showHealthy = false,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(32.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ModuleCard(
                        title = "Reels & Apps",
                        status = if (state.surfaceBlockCount + state.fullBlockCount > 0) {
                            "${state.surfaceBlockCount + state.fullBlockCount} armed"
                        } else {
                            "Not configured"
                        },
                        isActive = state.surfaceBlockCount + state.fullBlockCount > 0,
                        onClick = { onNavigate(VaultDestination.RULES) },
                        modifier = Modifier.weight(1f),
                    )
                    ModuleCard(
                        title = "Shield",
                        status = if (state.blockedDomainCount > 0) {
                            "${state.blockedDomainCount} ${if (state.blockedDomainCount == 1) "domain" else "domains"}"
                        } else {
                            "Not configured"
                        },
                        isActive = state.blockedDomainCount > 0,
                        onClick = { onNavigate(VaultDestination.SHIELD) },
                        modifier = Modifier.weight(1f),
                    )
                }
                Spacer(Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    ModuleCard(
                        title = "VPN",
                        status = if (state.isVpnBlockingEnabled) "Active" else "Inactive",
                        isActive = state.isVpnBlockingEnabled,
                        onClick = {
                            if (state.isVpnBlockingEnabled) {
                                showRestartVpnDialog = true
                            } else {
                                onNavigate(VaultDestination.SHIELD)
                            }
                        },
                        modifier = Modifier.weight(1f),
                    )
                    ModuleCard(
                        title = "Vault",
                        status = when {
                            state.isPasswordSet && state.isAdminLive -> "Sealed"
                            state.isPasswordSet -> "Passcode set"
                            else -> "Open"
                        },
                        isActive = state.isPasswordSet && state.isAdminLive,
                        onClick = { onNavigate(VaultDestination.VAULT) },
                        modifier = Modifier.weight(1f),
                    )
                }

                Spacer(Modifier.height(28.dp))
                Text(
                    text = state.lastCheckedAt?.let { "LAST HEALTH CHECK · ${formatChecked(it)}" }
                        ?: "HEALTH CHECK PENDING",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
            }
            ScrollEdgeShadow(
                edge = ScrollEdge.BOTTOM,
                scrollState = dashboardScrollState,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .onSizeChanged { size -> headerHeight = with(density) { size.height.toDp() } }
                .hazeEffect(
                    state = hazeState,
                    style = HazeStyle(
                        backgroundColor = colors.ink,
                        tints = listOf(HazeTint(colors.ink.copy(alpha = HEADER_TINT_ALPHA))),
                        blurRadius = HEADER_BLUR_RADIUS,
                        noiseFactor = 0f,
                    ),
                ) {
                    val headerHeightPx = with(density) { headerHeight.toPx() }
                    val fadeHeightPx = with(density) { HEADER_FADE_HEIGHT.toPx() }
                    progressive = HazeProgressive.verticalGradient(
                        startY = (headerHeightPx - fadeHeightPx).coerceAtLeast(0f),
                        startIntensity = 1f,
                        endY = headerHeightPx,
                        endIntensity = 0f,
                    )
                }
                .statusBarsPadding()
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = HEADER_FADE_HEIGHT),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = "Digital Vault",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = when {
                    state.isStoodDown && state.standDownUntil != null -> {
                        val remainingSeconds = Duration.between(now, state.standDownUntil).seconds.coerceAtLeast(0)
                        "GUARD IS DOWN · ${remainingSeconds}s LEFT"
                    }
                    state.isStoodDown -> "GUARD IS DOWN"
                    state.activeCount == state.totalCount -> "ALL SYSTEMS ARMED"
                    else -> "${state.activeCount} OF ${state.totalCount} PROTECTIONS ACTIVE"
                },
                style = MaterialTheme.typography.labelMedium,
                color = when {
                    state.isStoodDown -> colors.rust
                    state.activeCount == state.totalCount -> colors.brass
                    else -> colors.textMuted
                },
            )
        }

        if (showRestartVpnDialog) {
            AlertDialog(
                onDismissRequest = { showRestartVpnDialog = false },
                containerColor = colors.surface,
                title = {
                    Text(text = "Restart VPN?", color = colors.textPrimary)
                },
                text = {
                    Text(
                        text = "This briefly drops domain blocking while the VPN reconnects.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textMuted,
                    )
                },
                confirmButton = {
                    OutlinedButton(
                        onClick = {
                            viewModel.restartVpn()
                            showRestartVpnDialog = false
                        },
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brass),
                    ) {
                        Text(text = "Restart")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showRestartVpnDialog = false }) {
                        Text(text = "Cancel", color = colors.textMuted)
                    }
                },
            )
        }
    }
}

private fun formatChecked(instant: Instant): String =
    LastCheckedFormatter.format(instant.atZone(ZoneId.systemDefault())).uppercase()

@Composable
private fun MasterDial(
    fraction: Float,
    activeCount: Int,
    totalCount: Int,
    isStoodDown: Boolean,
) {
    val colors = VaultTheme.colors
    val accentColor = if (isStoodDown) colors.rust else colors.brass
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(durationMillis = 700),
        label = "dial",
    )

    Box(
        modifier = Modifier.size(224.dp),
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(224.dp)) {
            val tickCount = 50
            val tickLength = 10.dp.toPx()
            val tickWidth = 2.dp.toPx()
            val activeTicks = (animatedFraction * tickCount).roundToInt()

            repeat(tickCount) { index ->
                val isActiveTick = animatedFraction > 0f && index <= activeTicks
                rotate(degrees = index * (360f / tickCount), pivot = center) {
                    drawLine(
                        color = if (isActiveTick) accentColor else colors.surfaceRaised,
                        start = Offset(center.x, tickWidth / 2f),
                        end = Offset(center.x, tickLength),
                        strokeWidth = tickWidth,
                        cap = StrokeCap.Round,
                    )
                }
            }

            val ringStroke = 12.dp.toPx()
            val ringInset = tickLength + 14.dp.toPx()
            val arcSize = Size(size.width - ringInset * 2, size.height - ringInset * 2)
            val arcTopLeft = Offset(ringInset, ringInset)

            drawArc(
                color = colors.surfaceRaised,
                startAngle = -90f,
                sweepAngle = 360f,
                useCenter = false,
                topLeft = arcTopLeft,
                size = arcSize,
                style = Stroke(width = ringStroke, cap = StrokeCap.Round),
            )
            if (animatedFraction > 0f) {
                drawArc(
                    color = accentColor,
                    startAngle = -90f,
                    sweepAngle = 360f * animatedFraction,
                    useCenter = false,
                    topLeft = arcTopLeft,
                    size = arcSize,
                    style = Stroke(width = ringStroke, cap = StrokeCap.Round),
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = "$activeCount/$totalCount",
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
            )
            Text(
                text = if (isStoodDown) "GUARD DOWN" else "ARMED",
                style = MaterialTheme.typography.labelMedium,
                color = if (isStoodDown) colors.rust else colors.textMuted,
            )
        }
    }
}

@Composable
private fun ModuleCard(
    title: String,
    status: String,
    isActive: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = VaultTheme.colors

    Surface(
        onClick = onClick,
        shape = VaultTheme.shapes.medium,
        color = colors.surface,
        modifier = modifier,
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Canvas(modifier = Modifier.size(18.dp)) {
                val strokeWidth = 3.dp.toPx()
                drawArc(
                    color = if (isActive) colors.sage else colors.surfaceRaised,
                    startAngle = -90f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
                )
                if (isActive) {
                    drawCircle(color = colors.sage, radius = 3.dp.toPx())
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = status,
                style = MaterialTheme.typography.labelMedium,
                color = if (isActive) colors.sage else colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}
