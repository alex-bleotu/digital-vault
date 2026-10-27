package com.digitalvault.core.accessibility.matcher

import com.digitalvault.core.accessibility.ScreenNode

object TikTokShareMatcher : SurfaceMatcher {

    override val id = "tiktok_share"
    override val packageName = "com.zhiliaoapp.musically"
    override val surfaceLabel = "Share panel"

    private val requiredLabels = listOf("Repost", "Send to")

    override fun isTargetSurface(root: ScreenNode): Boolean =
        requiredLabels.all { root.hasVisibleNodeWithExactText(it) }
}
