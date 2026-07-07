package com.digitalvault.ui.onboarding

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Accessibility
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Layers
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.ui.graphics.vector.ImageVector

enum class OnboardingStep(
    val icon: ImageVector,
    val title: String,
    val why: String,
    val actionLabel: String,
    val manualConfirm: Boolean = false,
    val note: String? = null,
) {
    AUTO_BLOCKER(
        icon = Icons.Filled.Security,
        title = "About Samsung Auto Blocker",
        why = "You turned Auto Blocker off to install Digital Vault. It's safe to turn it back on now. It only affects new installs, not apps already on the phone.",
        actionLabel = "Got it",
        manualConfirm = true,
        note = "Settings → Security and privacy → Auto Blocker. Finish this setup first, then re-enable it.",
    ),
    OVERLAY(
        icon = Icons.Filled.Layers,
        title = "Draw the block screen",
        why = "When you open a blocked app, Digital Vault needs to cover it with a block screen. This lets it draw over other apps.",
        actionLabel = "Grant overlay access",
    ),
    ACCESSIBILITY(
        icon = Icons.Filled.Accessibility,
        title = "Watch for blocked apps",
        why = "Digital Vault only watches the apps and feeds you choose to block, so it can close them when you land on one.",
        actionLabel = "Open accessibility settings",
        note = "Sideloaded apps are restricted the first time: if the toggle is greyed out, tap the three-dot menu and choose \"Allow restricted setting\", then turn it on.",
    ),
    DEVICE_ADMIN(
        icon = Icons.Filled.Shield,
        title = "Lock uninstalling",
        why = "This makes Digital Vault ask for your password before it can be uninstalled or disabled, so you can't remove it on impulse.",
        actionLabel = "Activate uninstall lock",
    ),
    BATTERY(
        icon = Icons.Filled.BatteryChargingFull,
        title = "Keep it running",
        why = "Android can shut down background apps to save power. This exemption keeps the blocking engine alive.",
        actionLabel = "Allow background running",
    ),
    NOTIFICATIONS(
        icon = Icons.Filled.Notifications,
        title = "Show the guard's status",
        why = "The protection engine shows one quiet, honest notification while it runs, and warns you if a protection breaks.",
        actionLabel = "Allow notifications",
    ),
    AUTOSTART(
        icon = Icons.Filled.RestartAlt,
        title = "Restart after reboot",
        why = "This phone needs you to allow Digital Vault to start on its own after a restart. Android can't confirm this for us.",
        actionLabel = "Enable autostart",
        manualConfirm = true,
    ),
}
