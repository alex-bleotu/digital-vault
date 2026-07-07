package com.digitalvault.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.digitalvault.core.data.model.AppRule
import com.digitalvault.core.data.model.BlockMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class RulesRepository(private val dataStore: DataStore<Preferences>) {

    private val rulesKey = stringSetPreferencesKey("app_rules")

    val rules: Flow<List<AppRule>> = dataStore.data.map { preferences ->
        preferences[rulesKey].orEmpty().mapNotNull(::decode).sortedBy { it.packageName }
    }

    suspend fun currentRules(): List<AppRule> =
        dataStore.data.first()[rulesKey].orEmpty().mapNotNull(::decode)

    suspend fun upsert(rule: AppRule) {
        dataStore.edit { preferences ->
            val byPackage = preferences[rulesKey].orEmpty()
                .mapNotNull(::decode)
                .associateBy { it.packageName }
                .toMutableMap()
            byPackage[rule.packageName] = rule
            preferences[rulesKey] = byPackage.values.map(::encode).toSet()
        }
    }

    suspend fun remove(packageName: String) {
        dataStore.edit { preferences ->
            val remaining = preferences[rulesKey].orEmpty()
                .mapNotNull(::decode)
                .filterNot { it.packageName == packageName }
            preferences[rulesKey] = remaining.map(::encode).toSet()
        }
    }

    private fun encode(rule: AppRule): String =
        listOf(
            rule.packageName,
            rule.mode.name,
            rule.graceSeconds.toString(),
            rule.targetSurfaces.joinToString(","),
            rule.allowBreak.toString(),
        ).joinToString("|")

    private fun decode(raw: String): AppRule? {
        val parts = raw.split("|")
        if (parts.size < 4) {
            return null
        }
        val mode = runCatching { BlockMode.valueOf(parts[1]) }.getOrNull() ?: return null
        val graceSeconds = parts[2].toIntOrNull() ?: 10
        val targetSurfaces = if (parts[3].isEmpty()) emptyList() else parts[3].split(",")
        val allowBreak = parts.getOrNull(4)?.toBooleanStrictOrNull() ?: true

        return AppRule(
            packageName = parts[0],
            mode = mode,
            graceSeconds = graceSeconds,
            targetSurfaces = targetSurfaces,
            allowBreak = allowBreak,
        )
    }
}
