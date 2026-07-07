package com.digitalvault.core.accessibility

import android.view.accessibility.AccessibilityNodeInfo

object SettingsGuard {

    val watchedPackages: Set<String> = setOf(
        "com.android.settings",
        "com.android.packageinstaller",
        "com.google.android.packageinstaller",
        "com.miui.packageinstaller",
        "com.miui.securitycenter",
        "com.samsung.android.packageinstaller",
        "com.samsung.android.lool",
    )

    private val dangerKeywords = listOf(
        "uninstall",
        "device admin",
        "deactivate",
        "force stop",
        "accessibility",
        "clear data",
        "clear storage",
    )
    private const val MAX_NODES = 120

    fun isProtectedScreen(root: AccessibilityNodeInfo, appLabel: String): Boolean {
        val texts = collectTexts(root)
        if (texts.isEmpty()) {
            return false
        }
        val label = appLabel.lowercase()
        val mentionsApp = texts.any { it.contains(label) }
        val mentionsDanger = texts.any { text -> dangerKeywords.any { text.contains(it) } }

        return mentionsApp && mentionsDanger
    }

    private fun collectTexts(root: AccessibilityNodeInfo): List<String> {
        val texts = mutableListOf<String>()
        val queue = ArrayDeque<AccessibilityNodeInfo>()
        queue.add(root)
        var visited = 0

        while (queue.isNotEmpty() && visited < MAX_NODES) {
            val node = queue.removeFirst()
            visited += 1
            node.text?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it.lowercase()) }
            node.contentDescription?.toString()?.takeIf { it.isNotBlank() }?.let { texts.add(it.lowercase()) }
            for (index in 0 until node.childCount) {
                val child = node.getChild(index) ?: continue
                queue.add(child)
            }
        }

        return texts
    }
}
