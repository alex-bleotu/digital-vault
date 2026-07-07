package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object InstagramSuggestedReelMatcher : SurfaceMatcher {

    override val id = "instagram_reels_dm_next"
    override val packageName = "com.instagram.android"
    override val surfaceLabel = "Reels (after a shared one)"

    private const val SEND_TO_CHAT_LABEL = "Send to chat"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean =
        root.findVisibleNodesByText(SEND_TO_CHAT_LABEL).isNotEmpty()
}
