package com.digitalvault.ui.dashboard

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitalvault.core.data.DnsRepository
import com.digitalvault.core.data.HealthRepository
import com.digitalvault.core.data.RulesRepository
import com.digitalvault.core.data.VaultRepository
import com.digitalvault.core.data.model.BlockMode
import com.digitalvault.core.data.vaultDataStore
import com.digitalvault.core.permissions.SetupPermissions
import java.time.Instant
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class DashboardUiState(
    val fullBlockCount: Int = 0,
    val surfaceBlockCount: Int = 0,
    val blockedDomainCount: Int = 0,
    val isPasswordSet: Boolean = false,
    val isAccessibilityLive: Boolean = false,
    val isOverlayLive: Boolean = false,
    val isAdminLive: Boolean = false,
    val isBatteryLive: Boolean = false,
    val lastCheckedAt: Instant? = null,
    val standDownUntil: Instant? = null,
) {
    val isStoodDown: Boolean
        get() = standDownUntil?.isAfter(Instant.now()) == true

    val activeCount: Int
        get() = listOf(isAccessibilityLive, isOverlayLive, isAdminLive, isBatteryLive, isPasswordSet)
            .count { it }

    val totalCount: Int
        get() = 5

    val protectionFraction: Float
        get() = activeCount.toFloat() / totalCount
}

class DashboardViewModel(application: Application) : AndroidViewModel(application) {

    private val permissions = SetupPermissions(application)

    var uiState by mutableStateOf(DashboardUiState())
        private set

    init {
        val dataStore = application.vaultDataStore
        viewModelScope.launch {
            combine(
                RulesRepository(dataStore).rules,
                DnsRepository(dataStore).config,
                VaultRepository(dataStore).config,
                HealthRepository(dataStore).lastCheckedAt,
            ) { rules, dnsConfig, vaultConfig, lastChecked ->
                uiState.copy(
                    fullBlockCount = rules.count { it.mode == BlockMode.FULL_BLOCK },
                    surfaceBlockCount = rules.count { it.mode == BlockMode.SURFACE_BLOCK },
                    blockedDomainCount = dnsConfig.blockedDomains.size,
                    isPasswordSet = vaultConfig.isPasswordSet,
                    lastCheckedAt = lastChecked,
                    standDownUntil = vaultConfig.unlockedForSettingsUntil,
                )
            }.collect { combined ->
                uiState = combined.copy(
                    isAccessibilityLive = uiState.isAccessibilityLive,
                    isOverlayLive = uiState.isOverlayLive,
                    isAdminLive = uiState.isAdminLive,
                    isBatteryLive = uiState.isBatteryLive,
                )
                refreshLiveChecks()
            }
        }
    }

    fun refreshLiveChecks() {
        uiState = uiState.copy(
            isAccessibilityLive = permissions.isAccessibilityEnabled(),
            isOverlayLive = permissions.isOverlayGranted(),
            isAdminLive = permissions.isDeviceAdminActive(),
            isBatteryLive = permissions.isBatteryExempted(),
        )
    }
}
