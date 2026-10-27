package com.digitalvault.ui.components

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.digitalvault.core.permissions.SetupPermissions
import com.digitalvault.ui.theme.VaultTheme

private const val PRESSED_ALPHA = 0.5f

data class PermissionHealth(
    val isAccessibilityLive: Boolean,
    val isOverlayLive: Boolean,
    val isAdminLive: Boolean,
    val isBatteryLive: Boolean,
    val areNotificationsEnabled: Boolean,
) {
    val hasIssues: Boolean
        get() = !isAccessibilityLive || !isOverlayLive || !isAdminLive || !isBatteryLive || !areNotificationsEnabled
}

@Composable
fun PermissionHealthRows(
    health: PermissionHealth,
    onPermissionResult: () -> Unit,
    showHealthy: Boolean = true,
) {
    val context = LocalContext.current
    val permissions = remember(context) { SetupPermissions(context) }
    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { onPermissionResult() }

    fun launch(intent: Intent) {
        try {
            context.startActivity(intent)
        } catch (_: ActivityNotFoundException) {
        }
    }

    Column {
        if (showHealthy || !health.isAccessibilityLive) {
            HealthRow(
                label = "Accessibility engine",
                isHealthy = health.isAccessibilityLive,
                onFix = { launch(permissions.accessibilityIntent()) },
            )
        }
        if (showHealthy || !health.isOverlayLive) {
            HealthRow(
                label = "Block screen overlay",
                isHealthy = health.isOverlayLive,
                onFix = { launch(permissions.overlayIntent()) },
            )
        }
        if (showHealthy || !health.isAdminLive) {
            HealthRow(
                label = "Uninstall lock",
                isHealthy = health.isAdminLive,
                onFix = { launch(permissions.deviceAdminIntent("Digital Vault asks for your passcode before it can be removed.")) },
            )
        }
        if (showHealthy || !health.isBatteryLive) {
            HealthRow(
                label = "Battery exemption",
                isHealthy = health.isBatteryLive,
                onFix = { launch(permissions.batteryIntent()) },
            )
        }
        if (showHealthy || !health.areNotificationsEnabled) {
            HealthRow(
                label = "Notifications",
                isHealthy = health.areNotificationsEnabled,
                onFix = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    } else {
                        launch(permissions.appSettingsIntent())
                    }
                },
            )
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
            val fixInteractionSource = remember { MutableInteractionSource() }
            val isFixPressed by fixInteractionSource.collectIsPressedAsState()

            Text(
                text = "Fix",
                color = colors.brass,
                modifier = Modifier
                    .alpha(if (isFixPressed) PRESSED_ALPHA else 1f)
                    .clickable(
                        interactionSource = fixInteractionSource,
                        indication = null,
                        role = Role.Button,
                        onClick = onFix,
                    ),
            )
        }
    }
}
