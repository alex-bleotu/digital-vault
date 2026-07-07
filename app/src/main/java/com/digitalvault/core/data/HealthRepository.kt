package com.digitalvault.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import java.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class HealthRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val LAST_CHECKED = longPreferencesKey("health_last_checked")
    }

    val lastCheckedAt: Flow<Instant?> = dataStore.data.map { preferences ->
        preferences[Keys.LAST_CHECKED]?.let(Instant::ofEpochMilli)
    }

    suspend fun recordCheck(timestamp: Instant) {
        dataStore.edit { preferences ->
            preferences[Keys.LAST_CHECKED] = timestamp.toEpochMilli()
        }
    }
}
