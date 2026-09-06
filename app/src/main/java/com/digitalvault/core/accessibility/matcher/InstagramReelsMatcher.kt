package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object InstagramReelsMatcher : SurfaceMatcher {

    override val id = "instagram_reels_tab"
    override val packageName = "com.instagram.android"
    override val surfaceLabel = "Reels"

    private const val REELS_TAB_LABEL = "Reels"
    private const val FRIENDS_TAB_LABEL = "Friends"
    private const val DIRECT_MESSAGE_REPLY_PREFIX = "Reply to"
    private const val DM_COMPOSER_VIEW_ID = "com.instagram.android:id/row_thread_composer_edittext"
    private const val REEL_DESCRIPTION_PREFIX = "Reel by "
    private val REEL_DESCRIPTION_SUFFIXES = listOf(
        ". Double tap to play or pause.",
        ". Double-tap to play or pause.",
    )
    private const val GRID_TILE_CLASS_NAME = "android.widget.Button"
    private const val COMMENT_INPUT_CLASS_NAME = "android.widget.AutoCompleteTextView"
    private const val LIKES_AND_PLAYS_HEADER = "Likes and plays"
    private const val REACTIONS_AND_PLAYS_HEADER = "Reactions and plays"
    private const val NOTE_QUICK_REPLY_SUFFIX = "liked this reel"
    private const val REPLY_MENU_ITEM_LABEL = "Reply"
    private const val VIEW_PROFILE_MENU_ITEM_LABEL = "View Profile"
    private const val MUTE_MENU_ITEM_LABEL = "Mute"
    private const val MEDIA_VIEWER_PHOTO_DESCRIPTION = "Photo"
    private const val MEDIA_VIEWER_VIDEO_DESCRIPTION = "Video"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean {
        if (root.hasVisibleNodeWithTextOrHintPrefix(DIRECT_MESSAGE_REPLY_PREFIX) ||
            root.hasVisibleNodeWithViewId(DM_COMPOSER_VIEW_ID)
        ) {
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
        root.hasVisibleNodeWithExactText(LIKES_AND_PLAYS_HEADER) ||
            root.hasVisibleNodeWithExactText(REACTIONS_AND_PLAYS_HEADER)

    fun isCommentsDrawer(root: AccessibilityNodeInfo): Boolean =
        hasVisibleDescendantOfClass(root, COMMENT_INPUT_CLASS_NAME)

    fun isNoteQuickReplyDrawer(root: AccessibilityNodeInfo): Boolean =
        root.anyVisibleDescendantDescriptionMatches { it.toString().endsWith(NOTE_QUICK_REPLY_SUFFIX) }

    fun isDmMediaViewer(root: AccessibilityNodeInfo): Boolean =
        root.hasVisibleNodeWithExactText(MEDIA_VIEWER_PHOTO_DESCRIPTION) ||
            root.hasVisibleNodeWithExactText(MEDIA_VIEWER_VIDEO_DESCRIPTION)

    fun isReplyContextMenu(root: AccessibilityNodeInfo): Boolean =
        root.hasVisibleNodeWithExactText(REPLY_MENU_ITEM_LABEL) &&
            root.hasVisibleNodeWithExactText(MUTE_MENU_ITEM_LABEL) &&
            root.anyVisibleDescendantTextMatches { it.toString().equals(VIEW_PROFILE_MENU_ITEM_LABEL, ignoreCase = true) }

    private fun hasVisibleDescendantOfClass(node: AccessibilityNodeInfo, className: String): Boolean {
        if (node.isVisibleToUser && node.className?.toString() == className) {
            return true
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            if (hasVisibleDescendantOfClass(child, className)) {
                return true
            }
        }

        return false
    }

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
