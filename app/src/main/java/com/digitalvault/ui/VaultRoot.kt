package com.digitalvault.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.foundation.layout.Box
import com.digitalvault.ui.lock.LockScreen
import com.digitalvault.ui.onboarding.OnboardingScreen
import com.digitalvault.ui.theme.VaultTheme

@Composable
fun VaultRoot(viewModel: VaultRootViewModel = viewModel()) {
    val onboardingComplete by viewModel.onboardingComplete.collectAsState()
    val isPasswordSet by viewModel.isPasswordSet.collectAsState()

    when {
        onboardingComplete == null || (onboardingComplete == true && isPasswordSet == null) -> Box(
            modifier = Modifier
                .fillMaxSize()
                .background(VaultTheme.colors.ink),
        )

        onboardingComplete == false -> OnboardingScreen(onComplete = {})

        isPasswordSet == false -> LockScreen(onUnlocked = {}, forceCreate = true)

        else -> VaultApp()
    }
}
