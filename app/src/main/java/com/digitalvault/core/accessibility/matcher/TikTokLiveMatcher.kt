package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object TikTokLiveMatcher : SurfaceMatcher {

    override val id = "tiktok_live"
    override val packageName = "com.zhiliaoapp.musically"
    override val surfaceLabel = "LIVE (discover hub and live streams)"

    private const val DISCOVER_LIVE_LABEL = "Discover LIVE"
    private const val GIFTER_SEAT_SUBSTRING = "gifter seat"
    private const val CHAT_INPUT_LABEL = "Type..."
    private const val VIEWS_SUBSTRING = "views"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean =
        root.hasVisibleNodeWithExactText(DISCOVER_LIVE_LABEL) ||
            root.anyDescendantDescriptionMatches { it.contains(GIFTER_SEAT_SUBSTRING, ignoreCase = true) } ||
            isWatchingLiveStream(root)

    private fun isWatchingLiveStream(root: AccessibilityNodeInfo): Boolean =
        root.hasVisibleNodeWithExactText(CHAT_INPUT_LABEL) &&
            root.anyDescendantDescriptionMatches { it.contains(VIEWS_SUBSTRING, ignoreCase = true) }
}
