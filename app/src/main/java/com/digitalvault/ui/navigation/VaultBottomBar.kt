package com.digitalvault.ui.navigation

import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.digitalvault.ui.theme.VaultTheme

@Composable
fun VaultBottomBar(
    current: VaultDestination,
    onSelect: (VaultDestination) -> Unit,
) {
    val colors = VaultTheme.colors

    NavigationBar(
        containerColor = colors.surface,
        contentColor = colors.textMuted,
    ) {
        VaultDestination.entries.forEach { destination ->
            NavigationBarItem(
                selected = destination == current,
                onClick = { onSelect(destination) },
                icon = { Icon(destination.icon, contentDescription = destination.label) },
                label = { Text(destination.label) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = colors.ink,
                    selectedTextColor = colors.brass,
                    indicatorColor = colors.brass,
                    unselectedIconColor = colors.textMuted,
                    unselectedTextColor = colors.textMuted,
                ),
            )
        }
    }
}
