package com.digitalvault.core.data.model

data class DnsConfig(
    val blockedDomains: Set<String> = emptySet(),
    val isVpnBlockingEnabled: Boolean = false,
)
