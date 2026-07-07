package com.digitalvault.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalvault.ui.dashboard.DashboardScreen
import com.digitalvault.ui.lock.LockScreen
import com.digitalvault.ui.navigation.VaultBottomBar
import com.digitalvault.ui.navigation.VaultDestination
import com.digitalvault.ui.rules.RulesScreen
import com.digitalvault.ui.rules.RulesViewModel
import com.digitalvault.ui.shield.ShieldScreen
import com.digitalvault.ui.shield.ShieldViewModel
import com.digitalvault.ui.theme.VaultTheme
import com.digitalvault.ui.update.UpdateDialog
import com.digitalvault.ui.vault.VaultScreen

@Composable
fun VaultApp() {
    var current by rememberSaveable { mutableStateOf(VaultDestination.DASHBOARD) }
    var isSessionUnlocked by rememberSaveable { mutableStateOf(false) }

    val rulesViewModel: RulesViewModel = viewModel()
    LaunchedEffect(Unit) {
        rulesViewModel.loadInstalledApps()
    }
    val shieldViewModel: ShieldViewModel = viewModel()

    UpdateDialog()

    Scaffold(
        containerColor = VaultTheme.colors.ink,
        bottomBar = {
            VaultBottomBar(
                current = current,
                onSelect = { current = it },
            )
        },
    ) { innerPadding ->
        val layoutDirection = LocalLayoutDirection.current
        val isDashboard = current == VaultDestination.DASHBOARD
        val contentPadding = if (isDashboard) {
            PaddingValues(
                top = 0.dp,
                bottom = innerPadding.calculateBottomPadding(),
                start = innerPadding.calculateStartPadding(layoutDirection),
                end = innerPadding.calculateEndPadding(layoutDirection),
            )
        } else {
            innerPadding
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(contentPadding),
        ) {
            val isProtectedTab = current != VaultDestination.DASHBOARD

            if (isProtectedTab && !isSessionUnlocked) {
                LockScreen(onUnlocked = { isSessionUnlocked = true })
            } else {
                when (current) {
                    VaultDestination.DASHBOARD -> DashboardScreen(onNavigate = { current = it })
                    VaultDestination.RULES -> RulesScreen()
                    VaultDestination.SHIELD -> ShieldScreen()
                    VaultDestination.VAULT -> VaultScreen()
                }
            }
        }
    }
}
