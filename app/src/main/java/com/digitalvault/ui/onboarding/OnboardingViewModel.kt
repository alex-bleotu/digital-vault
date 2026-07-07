package com.digitalvault.ui.onboarding

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationManagerCompat
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.digitalvault.core.data.SetupRepository
import com.digitalvault.core.data.vaultDataStore
import com.digitalvault.core.oem.OemAutostartRegistry
import com.digitalvault.core.permissions.SetupPermissions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

data class StepStatus(
    val step: OnboardingStep,
    val isComplete: Boolean,
)

data class OnboardingUiState(
    val steps: List<StepStatus>,
    val currentIndex: Int = 0,
) {
    val currentStep: StepStatus
        get() = steps[currentIndex]

    val completedCount: Int
        get() = steps.count { it.isComplete }

    val isLastStep: Boolean
        get() = currentIndex == steps.lastIndex
}

class OnboardingViewModel(application: Application) : AndroidViewModel(application) {

    private val permissions = SetupPermissions(application)
    private val setupRepository = SetupRepository(application.vaultDataStore)

    private val includedSteps: List<OnboardingStep> = buildList {
        if (OemAutostartRegistry.isSamsung()) {
            add(OnboardingStep.AUTO_BLOCKER)
        }
        add(OnboardingStep.OVERLAY)
        add(OnboardingStep.ACCESSIBILITY)
        add(OnboardingStep.DEVICE_ADMIN)
        add(OnboardingStep.BATTERY)
        add(OnboardingStep.NOTIFICATIONS)
        if (OemAutostartRegistry.needsAutostartStep(application)) {
            add(OnboardingStep.AUTOSTART)
        }
    }

    var uiState by mutableStateOf(
        OnboardingUiState(steps = includedSteps.map { StepStatus(it, false) }),
    )
        private set

    private var hasAutoAdvanced = false

    init {
        refresh()
    }

    fun refresh() {
        viewModelScope.launch {
            val setupState = setupRepository.state.first()
            val overlayGranted = permissions.isOverlayGranted()
            val accessibilityEnabled = permissions.isAccessibilityEnabled()
            val deviceAdminActive = permissions.isDeviceAdminActive()
            val batteryExempted = permissions.isBatteryExempted()
            val notificationsEnabled =
                NotificationManagerCompat.from(getApplication()).areNotificationsEnabled()

            setupRepository.setOverlayGranted(overlayGranted)
            setupRepository.setAccessibilityGranted(accessibilityEnabled)
            setupRepository.setDeviceAdminActive(deviceAdminActive)
            setupRepository.setBatteryExempted(batteryExempted)

            val steps = includedSteps.map { step ->
                val isComplete = when (step) {
                    OnboardingStep.AUTO_BLOCKER -> setupState.autoBlockerReviewed
                    OnboardingStep.OVERLAY -> overlayGranted
                    OnboardingStep.ACCESSIBILITY -> accessibilityEnabled
                    OnboardingStep.DEVICE_ADMIN -> deviceAdminActive
                    OnboardingStep.BATTERY -> batteryExempted
                    OnboardingStep.NOTIFICATIONS -> notificationsEnabled
                    OnboardingStep.AUTOSTART -> setupState.autostartConfirmedByUser
                }

                StepStatus(step, isComplete)
            }

            val newIndex = if (!hasAutoAdvanced) {
                hasAutoAdvanced = true
                steps.indexOfFirst { !it.isComplete }.let { if (it == -1) steps.lastIndex else it }
            } else {
                uiState.currentIndex
            }
            uiState = uiState.copy(steps = steps, currentIndex = newIndex)
        }
    }

    fun goToNext() {
        if (!uiState.isLastStep) {
            uiState = uiState.copy(currentIndex = uiState.currentIndex + 1)
        }
    }

    fun goToPrevious() {
        if (uiState.currentIndex > 0) {
            uiState = uiState.copy(currentIndex = uiState.currentIndex - 1)
        }
    }

    fun confirmAutostart() {
        viewModelScope.launch {
            setupRepository.setAutostartConfirmed(true)
            refresh()
        }
    }

    fun confirmAutoBlocker() {
        viewModelScope.launch {
            setupRepository.setAutoBlockerReviewed(true)
            refresh()
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            setupRepository.setOnboardingComplete(true)
        }
    }
}
