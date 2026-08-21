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
    private val REEL_DESCRIPTION_SUFFIXES = listOf(
        ". Double tap to play or pause.",
        ". Double-tap to play or pause.",
    )
    private const val GRID_TILE_CLASS_NAME = "android.widget.Button"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean {
        if (root.findVisibleNodesByText(DIRECT_MESSAGE_REPLY_PREFIX).isNotEmpty()) {
            return false
        }
        if (isTabBarShowing(root)) {
            return true
        }
        if (isLikesAndPlaysDropdown(root)) {
            return true
        }

        return isWatchingReel(root)
    }

    private fun isLikesAndPlaysDropdown(root: AccessibilityNodeInfo): Boolean =
        root.anyVisibleDescendantDescriptionMatches { it.endsWith(" views") } &&
            root.anyVisibleDescendantDescriptionMatches { it.endsWith(" likes") }

    private fun isTabBarShowing(root: AccessibilityNodeInfo): Boolean =
        root.hasVisibleNodeWithExactText(REELS_TAB_LABEL) && root.hasVisibleNodeWithExactText(FRIENDS_TAB_LABEL)

    private fun isWatchingReel(node: AccessibilityNodeInfo): Boolean {
        val description = node.contentDescription?.toString()
        if (node.isVisibleToUser &&
            description != null &&
            node.className?.toString() != GRID_TILE_CLASS_NAME &&
            description.startsWith(REEL_DESCRIPTION_PREFIX) &&
            REEL_DESCRIPTION_SUFFIXES.any { description.endsWith(it) }
        ) {
            return true
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            if (isWatchingReel(child)) {
                return true
            }
        }

        return false
    }
}
