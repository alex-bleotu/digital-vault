package com.digitalvault.ui.lock

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitalvault.core.data.VaultRepository
import com.digitalvault.core.data.vaultDataStore
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.launch

data class LockUiState(
    val mode: LockMode = LockMode.UNLOCK,
    val input: String = "",
    val error: String? = null,
    val isBusy: Boolean = false,
    val unlocked: Boolean = false,
    val isRecoveryVisible: Boolean = false,
    val passcodeResetRequestedAt: Instant? = null,
    val showRecoveryConfirmDialog: Boolean = false,
) {
    val recoveryReadyAt: Instant?
        get() = passcodeResetRequestedAt?.plus(VaultLockViewModel.RECOVERY_WAIT)

    fun isRecoveryReady(now: Instant = Instant.now()): Boolean =
        recoveryReadyAt?.let { now.isAfter(it) } ?: false
}

class VaultLockViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VaultRepository(application.vaultDataStore)

    var uiState by mutableStateOf(LockUiState())
        private set

    private var firstEntry: String? = null
    private var isForcedCreate = false

    init {
        viewModelScope.launch {
            val config = repository.currentConfig()
            if (!isForcedCreate) {
                uiState = uiState.copy(
                    mode = if (config.isPasswordSet) LockMode.UNLOCK else LockMode.CREATE,
                )
            }
        }
    }

    fun startCreateFlow() {
        isForcedCreate = true
        firstEntry = null
        uiState = LockUiState(mode = LockMode.CREATE)
    }

    fun setInput(value: String) {
        if (uiState.isBusy) {
            return
        }
        uiState = uiState.copy(input = value, error = null)
    }

    fun onSubmit() {
        if (uiState.isBusy || uiState.input.length < MIN_LENGTH) {
            return
        }
        when (uiState.mode) {
            LockMode.CREATE -> {
                firstEntry = uiState.input
                uiState = uiState.copy(mode = LockMode.CONFIRM, input = "", error = null)
            }

            LockMode.CONFIRM -> {
                if (uiState.input == firstEntry) {
                    persistNewPassword(uiState.input)
                } else {
                    firstEntry = null
                    uiState = uiState.copy(
                        mode = LockMode.CREATE,
                        input = "",
                        error = "Passcodes didn't match. Start again.",
                    )
                }
            }

            LockMode.UNLOCK -> attemptUnlock(uiState.input)
        }
    }

    private fun persistNewPassword(password: String) {
        uiState = uiState.copy(isBusy = true, error = null)
        viewModelScope.launch {
            repository.setPassword(password)
            firstEntry = null
            uiState = uiState.copy(isBusy = false, input = "", unlocked = true)
        }
    }

    private fun attemptUnlock(password: String) {
        uiState = uiState.copy(isBusy = true, error = null)
        viewModelScope.launch {
            val matches = repository.verifyPassword(password)
            uiState = if (matches) {
                uiState.copy(isBusy = false, input = "", unlocked = true)
            } else {
                uiState.copy(isBusy = false, input = "", error = "Incorrect passcode.")
            }
        }
    }

    fun showRecoveryScreen() {
        viewModelScope.launch {
            val config = repository.currentConfig()
            uiState = uiState.copy(
                isRecoveryVisible = true,
                passcodeResetRequestedAt = config.passcodeResetRequestedAt,
            )
        }
    }

    fun hideRecoveryScreen() {
        uiState = uiState.copy(isRecoveryVisible = false, showRecoveryConfirmDialog = false)
    }

    fun requestRecoveryConfirmation() {
        uiState = uiState.copy(showRecoveryConfirmDialog = true)
    }

    fun dismissRecoveryConfirmation() {
        uiState = uiState.copy(showRecoveryConfirmDialog = false)
    }

    fun confirmPasscodeReset() {
        viewModelScope.launch {
            repository.requestPasscodeReset()
            uiState = uiState.copy(
                passcodeResetRequestedAt = Instant.now(),
                showRecoveryConfirmDialog = false,
            )
        }
    }

    fun cancelPasscodeReset() {
        viewModelScope.launch {
            repository.cancelPasscodeReset()
            uiState = uiState.copy(passcodeResetRequestedAt = null)
        }
    }

    fun completeRecovery() {
        isForcedCreate = true
        firstEntry = null
        uiState = uiState.copy(
            mode = LockMode.CREATE,
            input = "",
            error = null,
            isRecoveryVisible = false,
        )
    }

    companion object {
        const val MIN_LENGTH = 6
        const val MAX_LENGTH = 6
        val RECOVERY_WAIT: Duration = Duration.ofHours(24)
    }
}
