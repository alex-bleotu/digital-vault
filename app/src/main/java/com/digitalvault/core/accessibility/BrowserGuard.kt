package com.digitalvault.core.accessibility

import android.view.accessibility.AccessibilityNodeInfo

object BrowserGuard {

    val watchedPackages: Set<String> = setOf(
        "com.android.chrome",
        "com.chrome.beta",
        "com.sec.android.app.sbrowser",
        "org.mozilla.firefox",
        "org.mozilla.firefox_beta",
        "com.microsoft.emmx",
        "com.opera.browser",
        "com.opera.mini.native",
        "com.brave.browser",
        "com.duckduckgo.mobile.android",
        "com.mi.globalbrowser",
        "com.android.browser",
        "com.UCMobile.intl",
        "com.kiwibrowser.browser",
        "com.vivo.browser",
        "com.heytap.browser",
        "com.google.android.googlequicksearchbox",
    )

    private val editTextOnlyPackages: Set<String> = setOf(
        "com.android.chrome",
        "com.chrome.beta",
    )

    private const val MAX_NODES = 200
    private const val EDIT_TEXT_CLASS_NAME = "android.widget.EditText"
    private val incognitoMarkers = listOf(
        "you've gone incognito",
        "search your incognito tabs",
    )

    fun findBlockedDomain(root: AccessibilityNodeInfo, blockedDomains: Set<String>, packageName: String): String? {
        if (blockedDomains.isEmpty()) {
            return null
        }
        val texts = if (packageName in editTextOnlyPackages) {
            collectAddressBarTexts(root)
        } else {
            collectAddressBarTexts(root) + collectVisibleTexts(root)
        }

        return blockedDomains.firstOrNull { domain -> texts.any { matchesHost(it, domain) } }
    }

    fun isIncognitoActive(root: AccessibilityNodeInfo): Boolean {
        val texts = collectVisibleTexts(root)

        return incognitoMarkers.any { marker -> texts.any { it.contains(marker) } }
    }

    private fun matchesHost(text: String, domain: String): Boolean {
        val escaped = Regex.escape(domain)
        val boundaryPattern = Regex("(^|[./])" + escaped + "(\$|[/?#:])")

        return boundaryPattern.containsMatchIn(text)
    }

    private fun collectAddressBarTexts(root: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited += 1
            if (node.className == EDIT_TEXT_CLASS_NAME && node.isVisibleToUser) {
                node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(normalize(it)) }
            }
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                queue.add(child)
            }
        }

        return texts
    }

    private fun collectVisibleTexts(root: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited += 1
            if (node.isVisibleToUser) {
                node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(normalize(it)) }
                node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(normalize(it)) }
            }
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                queue.add(child)
            }
        }

        return texts
    }

    private fun normalize(text: String): String =
        text.lowercase()
            .replace('’', '\'')
            .replace('‘', '\'')
}
