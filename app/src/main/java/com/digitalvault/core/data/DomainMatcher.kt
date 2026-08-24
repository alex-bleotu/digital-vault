package com.digitalvault.core.data

object DomainMatcher {

    fun matches(queriedDomain: String, blockedDomains: Set<String>): Boolean {
        val normalized = DnsRepository.normalizeDomain(queriedDomain)
        if (normalized.isEmpty()) {
            return false
        }

        return blockedDomains.any { blocked -> matchesHost(normalized, blocked) }
    }

    private fun matchesHost(text: String, domain: String): Boolean {
        val escaped = Regex.escape(domain)
        val boundaryPattern = Regex("(^|[./])" + escaped + "(\$|[/?#:])")

        return boundaryPattern.containsMatchIn(text)
    }
}
