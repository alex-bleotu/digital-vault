package com.digitalvault.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.clickable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.digitalvault.ui.theme.VaultTheme
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.delay

private val KEYBOARD_SHIFT_REDUCTION = 100.dp

@Composable
fun LockScreen(
    onUnlocked: () -> Unit,
    modifier: Modifier = Modifier,
    forceCreate: Boolean = false,
    startInRecovery: Boolean = false,
    onBack: (() -> Unit)? = null,
    viewModel: VaultLockViewModel = viewModel(),
) {
    val colors = VaultTheme.colors
    val state = viewModel.uiState
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(forceCreate, startInRecovery) {
        if (forceCreate) {
            viewModel.startCreateFlow()
        }
        if (startInRecovery) {
            viewModel.showRecoveryScreen()
        }
    }

    LaunchedEffect(state.unlocked) {
        if (state.unlocked) {
            onUnlocked()
        }
    }

    val title = when (state.mode) {
        LockMode.CREATE -> "Set your vault passcode"
        LockMode.CONFIRM -> "Re-enter to confirm"
        LockMode.UNLOCK -> "Enter your passcode"
    }
    val subtitle = when (state.mode) {
        LockMode.CREATE -> "${VaultLockViewModel.MAX_LENGTH} DIGITS · THIS GATES THE VAULT"
        LockMode.CONFIRM -> "ENTER THE SAME PASSCODE AGAIN"
        LockMode.UNLOCK -> "UNLOCK PROTECTED SETTINGS"
    }

    val effectiveOnBack = when {
        state.isRecoveryVisible && startInRecovery -> onBack
        state.isRecoveryVisible -> viewModel::hideRecoveryScreen
        else -> onBack
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.ink)
            .systemBarsPadding(),
    ) {
        if (effectiveOnBack != null) {
            IconButton(
                onClick = effectiveOnBack,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = 8.dp, y = (-36).dp),
            ) {
                Icon(
                    Icons.Filled.ChevronLeft,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(36.dp),
                )
            }
        }

        if (state.isRecoveryVisible) {
            RecoveryContent(
                state = state,
                onRequestReset = viewModel::requestRecoveryConfirmation,
                onCancelReset = viewModel::cancelPasscodeReset,
                onCompleteRecovery = viewModel::completeRecovery,
            )

            if (state.showRecoveryConfirmDialog) {
                AlertDialog(
                    onDismissRequest = viewModel::dismissRecoveryConfirmation,
                    containerColor = colors.surface,
                    title = { Text("Start 24-hour recovery wait?") },
                    text = {
                        Text(
                            "There is no way to speed this up or skip it. " +
                                "Only start this if you are certain you've forgotten your passcode.",
                        )
                    },
                    confirmButton = {
                        OutlinedButton(
                            onClick = viewModel::confirmPasscodeReset,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.rust),
                        ) {
                            Text("Start wait")
                        }
                    },
                    dismissButton = {
                        OutlinedButton(
                            onClick = viewModel::dismissRecoveryConfirmation,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textMuted),
                        ) {
                            Text("Cancel")
                        }
                    },
                )
            }

            return@Box
        }

        val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
        val reducedImeBottom = (imeBottom - KEYBOARD_SHIFT_REDUCTION).coerceAtLeast(0.dp)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = reducedImeBottom)
                .padding(horizontal = 24.dp, vertical = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.labelMedium,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(28.dp))
            Box(
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    focusRequester.requestFocus()
                    keyboardController?.show()
                },
                contentAlignment = Alignment.Center,
            ) {
                PinDots(length = state.input.length, max = VaultLockViewModel.MAX_LENGTH)
                PasscodeKeyboardField(
                    value = state.input,
                    onValueChange = viewModel::setInput,
                    maxLength = VaultLockViewModel.MAX_LENGTH,
                    enabled = !state.isBusy,
                    onDone = viewModel::onSubmit,
                    focusRequester = focusRequester,
                )
            }

            Spacer(Modifier.height(4.dp))
            Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
                if (state.error != null) {
                    Text(
                        text = state.error,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.rust,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            if (state.mode == LockMode.UNLOCK) {
                TextButton(
                    onClick = viewModel::showRecoveryScreen,
                    modifier = Modifier.offset(y = (-16).dp),
                ) {
                    Text(text = "Forgot passcode?", color = colors.textMuted)
                }
            }
        }
    }
}

@Composable
private fun RecoveryContent(
    state: LockUiState,
    onRequestReset: () -> Unit,
    onCancelReset: () -> Unit,
    onCompleteRecovery: () -> Unit,
) {
    val colors = VaultTheme.colors
    var now by remember { mutableStateOf(Instant.now()) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            now = Instant.now()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Passcode recovery",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(12.dp))

        when {
            state.passcodeResetRequestedAt == null -> {
                Text(
                    text = "This is a last resort. Starting recovery begins a mandatory " +
                        "24-hour wait with no way to speed it up. Only use this if you've " +
                        "truly forgotten your passcode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onRequestReset,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.rust),
                ) {
                    Text("Start 24-hour recovery wait")
                }
            }

            !state.isRecoveryReady(now) -> {
                val remaining = Duration.between(now, state.recoveryReadyAt).let {
                    if (it.isNegative) Duration.ZERO else it
                }
                val hours = remaining.toHours()
                val minutes = remaining.toMinutes() % 60

                Text(
                    text = "You can set a new passcode in:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = "${hours}h ${minutes}m",
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.rust,
                )
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onCancelReset,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textMuted),
                ) {
                    Text("Cancel recovery")
                }
            }

            else -> {
                Text(
                    text = "The wait is over. You can now set a new passcode.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textMuted,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    onClick = onCompleteRecovery,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brass),
                ) {
                    Text("Set new passcode")
                }
            }
        }
    }
}
