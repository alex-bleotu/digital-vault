package com.digitalvault.ui.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SpaceDashboard
import androidx.compose.ui.graphics.vector.ImageVector

enum class VaultDestination(
    val label: String,
    val icon: ImageVector,
) {
    DASHBOARD("Dashboard", Icons.Filled.SpaceDashboard),
    RULES("Rules", Icons.Filled.Apps),
    SHIELD("Shield", Icons.Filled.Shield),
    VAULT("Vault", Icons.Filled.Lock),
}
