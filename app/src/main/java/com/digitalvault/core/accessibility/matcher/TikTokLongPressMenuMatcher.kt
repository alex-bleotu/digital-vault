package com.digitalvault.core.accessibility.matcher

import com.digitalvault.core.accessibility.ScreenNode

object TikTokLongPressMenuMatcher : SurfaceMatcher {

    override val id = "tiktok_long_press_menu"
    override val packageName = "com.zhiliaoapp.musically"
    override val surfaceLabel = "Long-press video options menu"

    private val requiredLabels = listOf("Auto scroll", "Captions and translation")

    override fun isTargetSurface(root: ScreenNode): Boolean =
        requiredLabels.all { root.hasVisibleNodeWithExactText(it) }
}
