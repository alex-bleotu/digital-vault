package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object TikTokSpeedMatcher : SurfaceMatcher {

    override val id = "tiktok_speed"
    override val packageName = "com.zhiliaoapp.musically"
    override val surfaceLabel = "Long-press playback speed"

    private const val SPEED_TEXT_PREFIX = "Speed: "
    private const val BACK_LABEL = "Back"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean {
        if (root.hasDescendantWithExactText(BACK_LABEL)) {
            return false
        }

        return root.anyDescendantTextMatches { it.startsWith(SPEED_TEXT_PREFIX) }
    }
}
