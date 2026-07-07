package com.digitalvault.ui.onboarding

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalvault.core.oem.OemAutostartRegistry
import com.digitalvault.core.permissions.SetupPermissions
import com.digitalvault.ui.theme.VaultTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val ONBOARDING_TAG = "Onboarding"

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OnboardingViewModel = viewModel(),
) {
    val colors = VaultTheme.colors
    val context = LocalContext.current
    val permissions = remember { SetupPermissions(context) }
    val state = viewModel.uiState
    val lifecycleOwner = LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
                coroutineScope.launch {
                    delay(400)
                    viewModel.refresh()
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun launch(intent: Intent, fallback: Intent? = null) {
        try {
            context.startActivity(intent)
        } catch (error: ActivityNotFoundException) {
            Log.w(ONBOARDING_TAG, "No activity for ${intent.action}", error)
            if (fallback != null) {
                try {
                    context.startActivity(fallback)
                } catch (fallbackError: ActivityNotFoundException) {
                    Log.w(ONBOARDING_TAG, "No fallback activity for ${fallback.action}", fallbackError)
                }
            }
        }
    }

    val notificationLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { viewModel.refresh() }

    val batteryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) {
        viewModel.refresh()
        coroutineScope.launch {
            delay(400)
            viewModel.refresh()
        }
    }

    fun onAction(step: OnboardingStep) {
        when (step) {
            OnboardingStep.AUTO_BLOCKER -> viewModel.confirmAutoBlocker()
            OnboardingStep.OVERLAY -> launch(permissions.overlayIntent(), permissions.appSettingsIntent())
            OnboardingStep.ACCESSIBILITY -> launch(permissions.accessibilityIntent())
            OnboardingStep.DEVICE_ADMIN ->
                launch(permissions.deviceAdminIntent(step.why), permissions.appSettingsIntent())
            OnboardingStep.BATTERY -> {
                try {
                    batteryLauncher.launch(permissions.batteryIntent())
                } catch (error: ActivityNotFoundException) {
                    Log.w(ONBOARDING_TAG, "No activity for battery intent", error)
                    launch(permissions.batterySettingsIntent())
                }
            }
            OnboardingStep.NOTIFICATIONS -> {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    launch(permissions.appSettingsIntent())
                }
            }
            OnboardingStep.AUTOSTART -> {
                val autostartIntent = OemAutostartRegistry.autostartIntent(context)
                if (autostartIntent != null) {
                    launch(autostartIntent, permissions.appSettingsIntent())
                } else {
                    launch(permissions.appSettingsIntent())
                }
                viewModel.confirmAutostart()
            }
        }
    }

    val currentStatus = state.currentStep

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ink)
            .systemBarsPadding()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        OnboardingProgressDial(steps = state.steps, currentIndex = state.currentIndex)

        Spacer(Modifier.height(32.dp))
        StepCard(
            step = currentStatus.step,
            isComplete = currentStatus.isComplete,
            modifier = Modifier.weight(1f),
        )

        Spacer(Modifier.height(24.dp))
        when (currentStatus.step) {
            OnboardingStep.AUTO_BLOCKER -> {
                TextButton(
                    onClick = { launch(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(text = "Open security settings", color = colors.textMuted)
                }
                Spacer(Modifier.height(8.dp))
            }

            else -> Unit
        }
        Button(
            onClick = { onAction(currentStatus.step) },
            enabled = !currentStatus.isComplete || currentStatus.step == OnboardingStep.AUTOSTART,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(
                containerColor = colors.brass,
                contentColor = colors.ink,
                disabledContainerColor = colors.surface,
                disabledContentColor = colors.sage,
            ),
        ) {
            Text(
                text = if (currentStatus.isComplete && currentStatus.step != OnboardingStep.AUTOSTART) {
                    "Enabled"
                } else {
                    currentStatus.step.actionLabel
                },
                style = MaterialTheme.typography.labelLarge,
            )
        }

        Spacer(Modifier.height(12.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = viewModel::goToPrevious,
                enabled = state.currentIndex > 0,
            ) {
                Text(text = "Back", color = colors.textMuted)
            }
            if (state.isLastStep) {
                OutlinedButton(
                    onClick = {
                        viewModel.completeOnboarding()
                        onComplete()
                    },
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brass),
                ) {
                    Text(text = "Enter the vault")
                }
            } else {
                TextButton(onClick = viewModel::goToNext) {
                    Text(text = "Next", color = colors.brass)
                }
            }
        }
    }
}

@Composable
private fun StepCard(
    step: OnboardingStep,
    isComplete: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = VaultTheme.colors

    Surface(
        shape = VaultTheme.shapes.large,
        color = colors.surface,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            StepIcon(icon = step.icon, isComplete = isComplete)

            Spacer(Modifier.height(20.dp))
            Text(
                text = step.title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(12.dp))
            Text(
                text = step.why,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
            )

            if (step.note != null) {
                Spacer(Modifier.height(20.dp))
                Surface(
                    shape = VaultTheme.shapes.medium,
                    color = colors.surfaceRaised,
                ) {
                    Text(
                        text = step.note,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted,
                        textAlign = TextAlign.Start,
                        modifier = Modifier.padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepIcon(
    icon: ImageVector,
    isComplete: Boolean,
) {
    val colors = VaultTheme.colors
    val tint = if (isComplete) colors.sage else colors.brass

    Box(
        modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(colors.surfaceRaised)
            .border(2.dp, tint, CircleShape),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = if (isComplete) Icons.Filled.Check else icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(32.dp),
        )
    }
}
