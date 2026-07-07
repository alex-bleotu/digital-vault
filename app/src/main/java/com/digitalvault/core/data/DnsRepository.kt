package com.digitalvault.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.digitalvault.core.data.model.DnsConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DnsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val BLOCKED_DOMAINS = stringSetPreferencesKey("dns_blocked_domains")
    }

    val config: Flow<DnsConfig> = dataStore.data.map { preferences ->
        DnsConfig(
            blockedDomains = preferences[Keys.BLOCKED_DOMAINS].orEmpty(),
        )
    }

    suspend fun addBlockedDomain(domain: String) {
        val normalized = normalizeDomain(domain)
        if (normalized.isEmpty()) {
            return
        }
        dataStore.edit { preferences ->
            preferences[Keys.BLOCKED_DOMAINS] = preferences[Keys.BLOCKED_DOMAINS].orEmpty() + normalized
        }
    }

    suspend fun removeBlockedDomain(domain: String) {
        dataStore.edit { preferences ->
            preferences[Keys.BLOCKED_DOMAINS] = preferences[Keys.BLOCKED_DOMAINS].orEmpty() - domain
        }
    }

    companion object {
        fun normalizeDomain(raw: String): String =
            raw.trim()
                .lowercase()
                .removePrefix("https://")
                .removePrefix("http://")
                .removePrefix("www.")
                .substringBefore("/")
                .substringBefore(":")
                .trim()
    }
}
