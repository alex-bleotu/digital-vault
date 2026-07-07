package com.digitalvault.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.digitalvault.core.data.model.SetupState
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class SetupRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val ACCESSIBILITY_GRANTED = booleanPreferencesKey("accessibility_granted")
        val DEVICE_ADMIN_ACTIVE = booleanPreferencesKey("device_admin_active")
        val OVERLAY_GRANTED = booleanPreferencesKey("overlay_granted")
        val BATTERY_EXEMPTED = booleanPreferencesKey("battery_exempted")
        val AUTOSTART_CONFIRMED = booleanPreferencesKey("autostart_confirmed_by_user")
        val AUTO_BLOCKER_REVIEWED = booleanPreferencesKey("auto_blocker_reviewed")
        val ONBOARDING_COMPLETE = booleanPreferencesKey("onboarding_complete")
    }

    val state: Flow<SetupState> = dataStore.data.map { preferences ->
        SetupState(
            accessibilityGranted = preferences[Keys.ACCESSIBILITY_GRANTED] ?: false,
            deviceAdminActive = preferences[Keys.DEVICE_ADMIN_ACTIVE] ?: false,
            overlayGranted = preferences[Keys.OVERLAY_GRANTED] ?: false,
            batteryExempted = preferences[Keys.BATTERY_EXEMPTED] ?: false,
            autostartConfirmedByUser = preferences[Keys.AUTOSTART_CONFIRMED] ?: false,
            autoBlockerReviewed = preferences[Keys.AUTO_BLOCKER_REVIEWED] ?: false,
            onboardingComplete = preferences[Keys.ONBOARDING_COMPLETE] ?: false,
        )
    }

    suspend fun setAccessibilityGranted(granted: Boolean) = put(Keys.ACCESSIBILITY_GRANTED, granted)

    suspend fun setDeviceAdminActive(active: Boolean) = put(Keys.DEVICE_ADMIN_ACTIVE, active)

    suspend fun setOverlayGranted(granted: Boolean) = put(Keys.OVERLAY_GRANTED, granted)

    suspend fun setBatteryExempted(exempted: Boolean) = put(Keys.BATTERY_EXEMPTED, exempted)

    suspend fun setAutostartConfirmed(confirmed: Boolean) = put(Keys.AUTOSTART_CONFIRMED, confirmed)

    suspend fun setAutoBlockerReviewed(reviewed: Boolean) = put(Keys.AUTO_BLOCKER_REVIEWED, reviewed)

    suspend fun setOnboardingComplete(complete: Boolean) = put(Keys.ONBOARDING_COMPLETE, complete)

    private suspend fun put(key: Preferences.Key<Boolean>, value: Boolean) {
        dataStore.edit { preferences ->
            preferences[key] = value
        }
    }
}
