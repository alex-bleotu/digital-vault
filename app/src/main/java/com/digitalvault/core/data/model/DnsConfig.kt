package com.digitalvault.core.data.model

data class DnsConfig(
    val allowedDomains: Set<String> = emptySet(),
    val blockedDomains: Set<String> = emptySet(),
    val limitBlockedDomains: Map<String, String> = emptyMap(),
    val isAdultFilterEnabled: Boolean = false,
    val isVpnBlockingEnabled: Boolean = false,
    val isVpnRunning: Boolean = false,
)
