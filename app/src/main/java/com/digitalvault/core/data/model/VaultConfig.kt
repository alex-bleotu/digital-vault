package com.digitalvault.core.data.model

import java.time.Instant

data class VaultConfig(
    val passwordHash: String? = null,
    val passwordSalt: String? = null,
    val unlockedForSettingsUntil: Instant? = null,
    val passcodeResetRequestedAt: Instant? = null,
) {
    val isPasswordSet: Boolean
        get() = !passwordHash.isNullOrEmpty() && !passwordSalt.isNullOrEmpty()

    fun isSettingsUnlockedAt(now: Instant): Boolean {
        val until = unlockedForSettingsUntil ?: return false
        return now.isBefore(until)
    }

    fun recoveryReadyAt(waitDuration: java.time.Duration): Instant? =
        passcodeResetRequestedAt?.plus(waitDuration)
}
