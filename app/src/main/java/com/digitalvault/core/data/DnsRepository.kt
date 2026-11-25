package com.digitalvault.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import com.digitalvault.core.data.model.DnsConfig
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DnsRepository(private val dataStore: DataStore<Preferences>) {

    private object Keys {
        val ADULT_FILTER_ENABLED = booleanPreferencesKey("dns_adult_filter_enabled")
        val ALLOWED_DOMAINS = stringSetPreferencesKey("dns_allowed_domains")
        val BLOCKED_DOMAINS = stringSetPreferencesKey("dns_blocked_domains")
        val LIMIT_BLOCKED_DOMAINS = stringSetPreferencesKey("dns_limit_blocked_domains")
        val VPN_BLOCKING_ENABLED =booleanPreferencesKey("dns_vpn_blocking_enabled")
        val VPN_RUNNING = booleanPreferencesKey("dns_vpn_running")
    }

    val config: Flow<DnsConfig> = dataStore.data.map { preferences ->
        DnsConfig(
            allowedDomains = preferences[Keys.ALLOWED_DOMAINS].orEmpty(),
            blockedDomains = preferences[Keys.BLOCKED_DOMAINS].orEmpty(),
            limitBlockedDomains = preferences[Keys.LIMIT_BLOCKED_DOMAINS].orEmpty().associate { entry ->
                entry.substringBefore(LIMIT_ENTRY_SEPARATOR) to entry.substringAfter(LIMIT_ENTRY_SEPARATOR)
            },
            isAdultFilterEnabled = preferences[Keys.ADULT_FILTER_ENABLED] ?: false,
            isVpnBlockingEnabled = preferences[Keys.VPN_BLOCKING_ENABLED] ?: false,
            isVpnRunning = preferences[Keys.VPN_RUNNING] ?: false,
        )
    }

    suspend fun setVpnBlockingEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.VPN_BLOCKING_ENABLED] = enabled
        }
    }

    suspend fun setVpnRunning(isRunning: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.VPN_RUNNING] = isRunning
        }
    }

    suspend fun setLimitBlockedDomains(domains: Map<String, String>) {
        val entries = domains.map { (domain, label) -> "$domain$LIMIT_ENTRY_SEPARATOR$label" }.toSet()
        dataStore.edit { preferences ->
            if (preferences[Keys.LIMIT_BLOCKED_DOMAINS].orEmpty() != entries) {
                preferences[Keys.LIMIT_BLOCKED_DOMAINS] = entries
            }
        }
    }

    suspend fun setAdultFilterEnabled(enabled: Boolean) {
        dataStore.edit { preferences ->
            preferences[Keys.ADULT_FILTER_ENABLED] = enabled
        }
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

    suspend fun addAllowedDomain(domain: String) {
        val normalized = normalizeDomain(domain)
        if (normalized.isEmpty()) {
            return
        }
        dataStore.edit { preferences ->
            preferences[Keys.ALLOWED_DOMAINS] = preferences[Keys.ALLOWED_DOMAINS].orEmpty() + normalized
        }
    }

    suspend fun removeAllowedDomain(domain: String) {
        dataStore.edit { preferences ->
            preferences[Keys.ALLOWED_DOMAINS] = preferences[Keys.ALLOWED_DOMAINS].orEmpty() - domain
        }
    }

    companion object {
        private const val LIMIT_ENTRY_SEPARATOR = "|"

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
