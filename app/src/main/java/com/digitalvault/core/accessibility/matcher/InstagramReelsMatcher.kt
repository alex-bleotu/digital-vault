package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object InstagramReelsMatcher : SurfaceMatcher {

    override val id = "instagram_reels_tab"
    override val packageName = "com.instagram.android"
    override val surfaceLabel = "Reels"

    private const val REELS_TAB_LABEL = "Reels"
    private const val FRIENDS_TAB_LABEL = "Friends"
    private const val DIRECT_MESSAGE_REPLY_PREFIX = "Reply to"
    private const val REEL_DESCRIPTION_PREFIX = "Reel by "
    private const val REEL_DESCRIPTION_SUFFIX = ". Double tap to play or pause."
    private const val BACK_LABEL = "Back"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean {
        if (root.findVisibleNodesByText(DIRECT_MESSAGE_REPLY_PREFIX).isNotEmpty()) {
            return false
        }
        if (isTabBarShowing(root)) {
            return true
        }
        if (root.hasVisibleNodeWithExactText(BACK_LABEL)) {
            return false
        }

        return isWatchingReel(root)
    }

    private fun isTabBarShowing(root: AccessibilityNodeInfo): Boolean =
        root.hasVisibleNodeWithExactText(REELS_TAB_LABEL) && root.hasVisibleNodeWithExactText(FRIENDS_TAB_LABEL)

    private fun isWatchingReel(root: AccessibilityNodeInfo): Boolean =
        root.anyDescendantDescriptionMatches {
            it.startsWith(REEL_DESCRIPTION_PREFIX) && it.endsWith(REEL_DESCRIPTION_SUFFIX)
        }
}
