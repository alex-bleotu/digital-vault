package com.digitalvault.ui.vault

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitalvault.core.data.HealthRepository
import com.digitalvault.core.data.VaultRepository
import com.digitalvault.core.data.vaultDataStore
import com.digitalvault.core.permissions.SetupPermissions
import java.time.Duration
import java.time.Instant
import kotlinx.coroutines.launch

data class VaultUiState(
    val isAccessibilityLive: Boolean = false,
    val isOverlayLive: Boolean = false,
    val isAdminLive: Boolean = false,
    val isBatteryLive: Boolean = false,
    val areNotificationsEnabled: Boolean = false,
    val isPasswordSet: Boolean = false,
    val lastCheckedAt: Instant? = null,
    val standDownUntil: Instant? = null,
)

class VaultViewModel(application: Application) : AndroidViewModel(application) {

    val permissions = SetupPermissions(application)
    private val vaultRepository = VaultRepository(application.vaultDataStore)

    var uiState by mutableStateOf(VaultUiState())
        private set

    init {
        refreshLiveChecks()
        viewModelScope.launch {
            vaultRepository.config.collect { config ->
                uiState = uiState.copy(
                    isPasswordSet = config.isPasswordSet,
                    standDownUntil = config.unlockedForSettingsUntil,
                )
            }
        }
        viewModelScope.launch {
            HealthRepository(getApplication<Application>().vaultDataStore).lastCheckedAt.collect {
                uiState = uiState.copy(lastCheckedAt = it)
            }
        }
    }

    fun refreshLiveChecks() {
        val application = getApplication<Application>()
        uiState = uiState.copy(
            isAccessibilityLive = permissions.isAccessibilityEnabled(),
            isOverlayLive = permissions.isOverlayGranted(),
            isAdminLive = permissions.isDeviceAdminActive(),
            isBatteryLive = permissions.isBatteryExempted(),
            areNotificationsEnabled = NotificationManagerCompat.from(application).areNotificationsEnabled(),
        )
    }

    fun standDown(seconds: Long = 60) {
        viewModelScope.launch {
            vaultRepository.unlockSettingsFor(Duration.ofSeconds(seconds))
        }
    }
}
