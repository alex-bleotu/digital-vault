package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object TikTokFeedMatcher : SurfaceMatcher {

    override val id = "tiktok_feed"
    override val packageName = "com.zhiliaoapp.musically"
    override val surfaceLabel = "Explore, Following and For You feeds"

    private val discoveryTabLabels = listOf("Explore", "Community")
    private val requiredTabLabels = listOf("Following", "For You")

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean =
        discoveryTabLabels.any { root.findVisibleNodesByText(it).isNotEmpty() } &&
            requiredTabLabels.all { root.findVisibleNodesByText(it).isNotEmpty() }
}
