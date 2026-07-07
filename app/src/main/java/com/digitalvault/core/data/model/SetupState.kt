package com.digitalvault.core.data.model

data class SetupState(
    val accessibilityGranted: Boolean = false,
    val deviceAdminActive: Boolean = false,
    val overlayGranted: Boolean = false,
    val batteryExempted: Boolean = false,
    val autostartConfirmedByUser: Boolean = false,
    val autoBlockerReviewed: Boolean = false,
    val onboardingComplete: Boolean = false,
)
