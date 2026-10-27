package com.digitalvault.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class AppUsageRepository(private val dataStore: DataStore<Preferences>) {

    private val usageKey = stringSetPreferencesKey("app_usage")

    val usageSecondsToday: Flow<Map<String, Long>> = dataStore.data.map { preferences ->
        decodeFor(preferences[usageKey].orEmpty(), LocalDate.now())
    }

    suspend fun secondsFor(date: LocalDate): Map<String, Long> =
        decodeFor(dataStore.data.first()[usageKey].orEmpty(), date)

    suspend fun save(date: LocalDate, secondsByPackage: Map<String, Long>) {
        dataStore.edit { preferences ->
            preferences[usageKey] = secondsByPackage.map { (packageName, seconds) -> "$packageName|$date|$seconds" }.toSet()
        }
    }

    private fun decodeFor(raw: Set<String>, date: LocalDate): Map<String, Long> {
        val dateText = date.toString()

        return raw.mapNotNull { entry ->
            val parts = entry.split("|")
            if (parts.size != 3 || parts[1] != dateText) {
                return@mapNotNull null
            }
            val seconds = parts[2].toLongOrNull() ?: return@mapNotNull null

            parts[0] to seconds
        }.toMap()
    }
}
