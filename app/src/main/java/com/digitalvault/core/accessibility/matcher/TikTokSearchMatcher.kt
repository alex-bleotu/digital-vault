package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object TikTokSearchMatcher : SurfaceMatcher {

    override val id = "tiktok_search"
    override val packageName = "com.zhiliaoapp.musically"
    override val surfaceLabel = "Search (before typing and results)"

    private val searchResultTabLabels = listOf("Videos", "Users", "Sounds")
    private const val CLOSE_LABEL = "Close"
    private const val RECENT_SEARCH_TIME_LABEL = "Time"
    private const val COMMENT_INPUT_PREFIX = "Add comment"
    private const val EDIT_TEXT_CLASS_NAME = "android.widget.EditText"
    private const val BACK_LABEL = "Back"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean {
        if (root.hasDescendantWithExactText(BACK_LABEL)) {
            return false
        }

        return searchResultTabLabels.all { root.findVisibleNodesByText(it).isNotEmpty() } ||
            isSearchEntryScreen(root) ||
            hasSearchEditText(root)
    }

    private fun isSearchEntryScreen(root: AccessibilityNodeInfo): Boolean =
        root.hasVisibleNodeWithExactText(CLOSE_LABEL) && root.hasVisibleNodeWithExactText(RECENT_SEARCH_TIME_LABEL)

    private fun hasSearchEditText(node: AccessibilityNodeInfo): Boolean {
        if (node.className == EDIT_TEXT_CLASS_NAME && node.text?.startsWith(COMMENT_INPUT_PREFIX) != true) {
            return true
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            if (hasSearchEditText(child)) {
                return true
            }
        }

        return false
    }
}
