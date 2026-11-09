package com.digitalvault.core.data.model

data class DnsConfig(
    val allowedDomains: Set<String> = emptySet(),
    val blockedDomains: Set<String> = emptySet(),
    val isAdultFilterEnabled: Boolean = false,
    val isVpnBlockingEnabled: Boolean = false,
)
