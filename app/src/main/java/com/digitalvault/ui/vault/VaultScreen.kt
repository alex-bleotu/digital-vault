package com.digitalvault.ui.vault

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalvault.ui.lock.LockScreen
import com.digitalvault.ui.theme.VaultTheme
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private const val VAULT_TAG = "VaultScreen"
private val CheckFormatter = DateTimeFormatter.ofPattern("HH:mm · d MMM")

@Composable
fun VaultScreen(
    modifier: Modifier = Modifier,
    viewModel: VaultViewModel = viewModel(),
) {
    val colors = VaultTheme.colors
    val context = LocalContext.current
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

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refreshLiveChecks() }

    fun launch(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            Log.w(VAULT_TAG, "No activity for ${intent.action}", error)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ink)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
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
            HealthRow(
                label = "Accessibility engine",
                isHealthy = state.isAccessibilityLive,
                onFix = { launch(viewModel.permissions.accessibilityIntent()) },
            )
            HealthRow(
                label = "Block screen overlay",
                isHealthy = state.isOverlayLive,
                onFix = { launch(viewModel.permissions.overlayIntent()) },
            )
            HealthRow(
                label = "Uninstall lock",
                isHealthy = state.isAdminLive,
                onFix = { launch(viewModel.permissions.deviceAdminIntent("Digital Vault asks for your passcode before it can be removed.")) },
            )
            HealthRow(
                label = "Battery exemption",
                isHealthy = state.isBatteryLive,
                onFix = { launch(viewModel.permissions.batteryIntent()) },
            )
            HealthRow(
                label = "Notifications",
                isHealthy = state.areNotificationsEnabled,
                onFix = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        launch(viewModel.permissions.appSettingsIntent())
                    }
                },
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = state.lastCheckedAt?.let {
                    "LAST WATCHDOG CHECK · ${CheckFormatter.format(it.atZone(ZoneId.systemDefault())).uppercase()}"
                } ?: "WATCHDOG CHECK PENDING",
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
                onClick = { viewModel.standDown() },
                colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.rust),
            ) {
                Text(text = if (standDownActive) "Guard is standing down…" else "Stand down for 60 seconds")
            }
        }
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

@Composable
private fun HealthRow(
    label: String,
    isHealthy: Boolean,
    onFix: () -> Unit,
) {
    val colors = VaultTheme.colors

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .clip(CircleShape)
                    .background(if (isHealthy) colors.sage else colors.rust),
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textPrimary,
            )
        }
        if (!isHealthy) {
            TextButton(onClick = onFix) {
                Text(text = "Fix", color = colors.brass)
            }
        }
    }
}
