package com.digitalvault.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitalvault.core.data.SetupRepository
import com.digitalvault.core.data.VaultRepository
import com.digitalvault.core.data.vaultDataStore
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class VaultRootViewModel(application: Application) : AndroidViewModel(application) {

    private val setupRepository = SetupRepository(application.vaultDataStore)
    private val vaultRepository = VaultRepository(application.vaultDataStore)

    val onboardingComplete: StateFlow<Boolean?> = setupRepository.state
        .map { it.onboardingComplete }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isPasswordSet: StateFlow<Boolean?> = vaultRepository.config
        .map { it.isPasswordSet }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)
}
