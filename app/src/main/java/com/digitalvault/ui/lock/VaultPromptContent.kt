package com.digitalvault.ui.lock

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.digitalvault.ui.theme.VaultTheme
import kotlinx.coroutines.launch

private val KEYBOARD_SHIFT_REDUCTION = 100.dp

@Composable
fun VaultPromptContent(
    message: String,
    onVerify: suspend (String) -> Boolean,
    onSuccess: () -> Unit,
    onLeave: () -> Unit,
) {
    val colors = VaultTheme.colors
    val scope = rememberCoroutineScope()
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    var input by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    var isBusy by remember { mutableStateOf(false) }

    fun submit() {
        if (isBusy || input.length < VaultLockViewModel.MIN_LENGTH) {
            return
        }
        isBusy = true
        error = null
        scope.launch {
            val matches = onVerify(input)
            isBusy = false
            if (matches) {
                onSuccess()
            } else {
                input = ""
                error = "Incorrect passcode."
            }
        }
    }

    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    val reducedImeBottom = (imeBottom - KEYBOARD_SHIFT_REDUCTION).coerceAtLeast(0.dp)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.ink)
            .padding(bottom = reducedImeBottom)
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            text = "Vault is guarding this screen",
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = message,
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
            PinDots(length = input.length, max = VaultLockViewModel.MAX_LENGTH)
            PasscodeKeyboardField(
                value = input,
                onValueChange = { newValue ->
                    input = newValue
                    error = null
                },
                maxLength = VaultLockViewModel.MAX_LENGTH,
                enabled = !isBusy,
                onDone = ::submit,
                focusRequester = focusRequester,
            )
        }

        Spacer(Modifier.height(12.dp))
        Box(modifier = Modifier.height(24.dp), contentAlignment = Alignment.Center) {
            if (error != null) {
                Text(
                    text = error.orEmpty(),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.rust,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Spacer(Modifier.height(12.dp))
        OutlinedButton(
            onClick = onLeave,
            modifier = Modifier.offset(y = (-16).dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.brass),
        ) {
            Text(text = "Go to Home Screen", color = colors.brass)
        }
    }
}
