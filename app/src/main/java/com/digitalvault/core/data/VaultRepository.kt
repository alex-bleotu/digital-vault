package com.digitalvault.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.digitalvault.core.data.model.VaultConfig
import com.digitalvault.core.security.PasswordHasher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.Duration
import java.time.Instant

class VaultRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val PASSWORD_HASH = stringPreferencesKey("password_hash")
        val PASSWORD_SALT = stringPreferencesKey("password_salt")
        val UNLOCKED_UNTIL = longPreferencesKey("unlocked_for_settings_until")
        val PASSCODE_RESET_REQUESTED_AT = longPreferencesKey("passcode_reset_requested_at")
    }

    val config: Flow<VaultConfig> = dataStore.data.map { preferences ->
        preferences.toVaultConfig()
    }

    suspend fun currentConfig(): VaultConfig = dataStore.data.first().toVaultConfig()

    suspend fun setPassword(password: String) {
        val salt = PasswordHasher.generateSalt()
        val hash = withContext(Dispatchers.Default) { PasswordHasher.hash(password, salt) }
        dataStore.edit { preferences ->
            preferences[Keys.PASSWORD_HASH] = hash
            preferences[Keys.PASSWORD_SALT] = salt
            preferences.remove(Keys.PASSCODE_RESET_REQUESTED_AT)
        }
    }

    suspend fun requestPasscodeReset() {
        dataStore.edit { preferences ->
            preferences[Keys.PASSCODE_RESET_REQUESTED_AT] = Instant.now().toEpochMilli()
        }
    }

    suspend fun cancelPasscodeReset() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.PASSCODE_RESET_REQUESTED_AT)
        }
    }

    suspend fun verifyPassword(password: String): Boolean {
        val preferences = dataStore.data.first()
        val hash = preferences[Keys.PASSWORD_HASH] ?: return false
        val salt = preferences[Keys.PASSWORD_SALT] ?: return false
        return withContext(Dispatchers.Default) { PasswordHasher.verify(password, salt, hash) }
    }

    suspend fun unlockSettingsFor(duration: Duration) {
        val until = Instant.now().plus(duration)
        dataStore.edit { preferences ->
            preferences[Keys.UNLOCKED_UNTIL] = until.toEpochMilli()
        }
    }

    suspend fun clearSettingsUnlock() {
        dataStore.edit { preferences ->
            preferences.remove(Keys.UNLOCKED_UNTIL)
        }
    }

    private fun Preferences.toVaultConfig(): VaultConfig = VaultConfig(
        passwordHash = this[Keys.PASSWORD_HASH],
        passwordSalt = this[Keys.PASSWORD_SALT],
        unlockedForSettingsUntil = this[Keys.UNLOCKED_UNTIL]?.let(Instant::ofEpochMilli),
        passcodeResetRequestedAt = this[Keys.PASSCODE_RESET_REQUESTED_AT]?.let(Instant::ofEpochMilli),
    )
}
