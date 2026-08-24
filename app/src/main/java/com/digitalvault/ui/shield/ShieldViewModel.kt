package com.digitalvault.ui.shield

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitalvault.core.data.DnsRepository
import com.digitalvault.core.data.vaultDataStore
import com.digitalvault.core.vpn.VaultVpnService
import kotlinx.coroutines.launch

data class ShieldUiState(
    val blockedDomains: List<String> = emptyList(),
    val newDomainDraft: String = "",
    val isVpnBlockingEnabled: Boolean = false,
)

class ShieldViewModel(application: Application) : AndroidViewModel(application) {

    private val dnsRepository = DnsRepository(application.vaultDataStore)

    var uiState by mutableStateOf(ShieldUiState())
        private set

    init {
        viewModelScope.launch {
            dnsRepository.config.collect { config ->
                uiState = uiState.copy(
                    blockedDomains = config.blockedDomains.sorted(),
                    isVpnBlockingEnabled = config.isVpnBlockingEnabled,
                )
            }
        }
    }

    fun updateNewDomainDraft(value: String) {
        uiState = uiState.copy(newDomainDraft = value)
    }

    fun addBlockedDomain() {
        val domain = uiState.newDomainDraft
        viewModelScope.launch {
            dnsRepository.addBlockedDomain(domain)
            uiState = uiState.copy(newDomainDraft = "")
        }
    }

    fun removeBlockedDomain(domain: String) {
        viewModelScope.launch {
            dnsRepository.removeBlockedDomain(domain)
        }
    }

    fun disableVpnBlocking() {
        VaultVpnService.stop(getApplication())
        viewModelScope.launch {
            dnsRepository.setVpnBlockingEnabled(false)
        }
    }

    fun onVpnPermissionResult(granted: Boolean) {
        viewModelScope.launch {
            dnsRepository.setVpnBlockingEnabled(granted)
        }
        if (granted) {
            VaultVpnService.start(getApplication())
        }
    }
}
