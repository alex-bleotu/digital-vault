package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object TikTokSearchMatcher : SurfaceMatcher {

    override val id = "tiktok_search"
    override val packageName = "com.zhiliaoapp.musically"
    override val surfaceLabel = "Search (results and videos opened from it)"

    private val searchResultTabLabels = listOf("Videos", "Users", "Sounds")
    private const val RELATED_CONTENT_LABEL = "Find related content"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean =
        searchResultTabLabels.all { root.findVisibleNodesByText(it).isNotEmpty() } ||
            root.findVisibleNodesByText(RELATED_CONTENT_LABEL).isNotEmpty()
}
