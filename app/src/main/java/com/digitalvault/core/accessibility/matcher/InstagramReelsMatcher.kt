package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object InstagramReelsMatcher : SurfaceMatcher {

    override val id = "instagram_reels_tab"
    override val packageName = "com.instagram.android"
    override val surfaceLabel = "Reels"

    private const val REELS_TAB_LABEL = "Reels"
    private const val FRIENDS_TAB_LABEL = "Friends"
    private const val DIRECT_MESSAGE_REPLY_PREFIX = "Reply to"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean {
        if (root.findVisibleNodesByText(DIRECT_MESSAGE_REPLY_PREFIX).isNotEmpty()) {
            return false
        }

        return root.findVisibleNodesByText(REELS_TAB_LABEL).isNotEmpty() &&
            root.findVisibleNodesByText(FRIENDS_TAB_LABEL).isNotEmpty()
    }
}
