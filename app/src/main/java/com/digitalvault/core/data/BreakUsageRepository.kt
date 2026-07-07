package com.digitalvault.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class BreakUsageRepository(private val dataStore: DataStore<Preferences>) {

    private val usageKey = stringSetPreferencesKey("break_usage")

    val lastUsedAtMillisByPackage: Flow<Map<String, Long>> = dataStore.data.map { preferences ->
        preferences[usageKey].orEmpty().mapNotNull(::decode).toMap()
    }

    suspend fun recordUsed(packageName: String, atMillis: Long) {
        dataStore.edit { preferences ->
            val byPackage = preferences[usageKey].orEmpty()
                .mapNotNull(::decode)
                .toMap()
                .toMutableMap()
            byPackage[packageName] = atMillis
            preferences[usageKey] = byPackage.map { (pkg, millis) -> encode(pkg, millis) }.toSet()
        }
    }

    private fun encode(packageName: String, atMillis: Long): String = "$packageName|$atMillis"

    private fun decode(raw: String): Pair<String, Long>? {
        val parts = raw.split("|")
        if (parts.size != 2) {
            return null
        }
        val millis = parts[1].toLongOrNull() ?: return null

        return parts[0] to millis
    }
}
