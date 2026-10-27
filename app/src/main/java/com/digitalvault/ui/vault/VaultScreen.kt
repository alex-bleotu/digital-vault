package com.digitalvault.ui.vault

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalvault.BuildConfig
import com.digitalvault.ui.components.PermissionHealth
import com.digitalvault.ui.components.PermissionHealthRows
import com.digitalvault.ui.lock.LockScreen
import com.digitalvault.ui.theme.VaultTheme
import com.digitalvault.ui.components.ScrollEdgeShadow
import com.digitalvault.ui.components.ScrollEdge
import androidx.compose.foundation.layout.Box
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val CheckFormatter = DateTimeFormatter.ofPattern("HH:mm · d MMM")

@Composable
fun VaultScreen(
    modifier: Modifier = Modifier,
    viewModel: VaultViewModel = viewModel(),
) {
    val colors = VaultTheme.colors
    val state = viewModel.uiState
    val lifecycleOwner = LocalLifecycleOwner.current
    var isChangingPasscode by remember { mutableStateOf(false) }
    var isRecoveringPasscode by remember { mutableStateOf(false) }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLiveChecks()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (isChangingPasscode) {
        LockScreen(
            onUnlocked = { isChangingPasscode = false },
            modifier = modifier,
            forceCreate = true,
            onBack = { isChangingPasscode = false },
            viewModel = viewModel(key = "change_passcode"),
        )

        return
    }

    if (isRecoveringPasscode) {
        LockScreen(
            onUnlocked = { isRecoveringPasscode = false },
            modifier = modifier,
            startInRecovery = true,
            onBack = { isRecoveringPasscode = false },
            viewModel = viewModel(key = "recovery_passcode"),
        )

        return
    }

    val screenScrollState = rememberScrollState()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ink),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(screenScrollState)
                .padding(start = 24.dp, top = 24.dp, end = 24.dp, bottom = 28.dp),
        ) {
            Text(
                text = "Vault",
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "SECURITY · HEALTH · RECOVERY",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )

            Spacer(Modifier.height(24.dp))
            SectionCard(title = "PERMISSION HEALTH") {
                PermissionHealthRows(
                    health = PermissionHealth(
                        isAccessibilityLive = state.isAccessibilityLive,
                        isOverlayLive = state.isOverlayLive,
                        isAdminLive = state.isAdminLive,
                        isBatteryLive = state.isBatteryLive,
                        areNotificationsEnabled = state.areNotificationsEnabled,
                    ),
                    onPermissionResult = viewModel::refreshLiveChecks,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = state.lastCheckedAt?.let {
                        "LAST CHECK · ${CheckFormatter.format(it.atZone(ZoneId.systemDefault())).uppercase()}"
                    } ?: "CHECK PENDING",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textMuted,
                )
            }

            Spacer(Modifier.height(16.dp))
            SectionCard(title = "PASSCODE") {
                Text(
                    text = "Gates the Rules, Shield and Vault screens, protected system settings, and uninstalling.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { isChangingPasscode = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brass),
                ) {
                    Text("Change passcode")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = { isRecoveringPasscode = true },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textMuted),
                ) {
                    Text("Forgot passcode?")
                }
            }

            Spacer(Modifier.height(16.dp))
            SectionCard(title = "DANGER ZONE") {
                val standDownActive = state.standDownUntil?.isAfter(Instant.now()) == true

                Text(
                    text = "Stand the guard down for 60 seconds to change protected settings (device admin, accessibility) and pause Rules and Shield enforcement.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { if (standDownActive) viewModel.armGuard() else viewModel.standDown() },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.rust),
                ) {
                    Text(text = if (standDownActive) "Guard is standing down…" else "Stand down for 60 seconds")
                }
            }

            Spacer(Modifier.height(28.dp))
            Text(
                text = "VERSION ${BuildConfig.VERSION_NAME}",
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
        }
        ScrollEdgeShadow(
            edge = ScrollEdge.BOTTOM,
            scrollState = screenScrollState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

@Composable
private fun SectionCard(
    title: String,
    content: @Composable () -> Unit,
) {
    val colors = VaultTheme.colors

    Surface(
        shape = VaultTheme.shapes.medium,
        color = colors.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
            )
            Spacer(Modifier.height(12.dp))
            content()
        }
    }
}
