package com.digitalvault.core.accessibility

import android.view.accessibility.AccessibilityNodeInfo
import com.digitalvault.core.accessibility.matcher.findVisibleNodesByText
import com.digitalvault.core.accessibility.matcher.hasVisibleNodeWithExactText

object InstagramZoneGuard {

    const val PACKAGE_NAME = "com.instagram.android"

    private const val SETTINGS_MENU_LABEL = "Settings and activity"
    private const val EDIT_PROFILE_LABEL = "Edit profile"
    private const val ACTIVITY_FILTER_LABEL = "All content types"
    private const val REELS_TAB_LABEL = "Reels"
    private const val FRIENDS_TAB_LABEL = "Friends"
    private const val YOUR_STORY_LABEL = "Your story"

    fun isMainReelsTab(root: AccessibilityNodeInfo): Boolean =
        root.hasVisibleNodeWithExactText(REELS_TAB_LABEL) && root.hasVisibleNodeWithExactText(FRIENDS_TAB_LABEL)

    fun isHomeFeed(root: AccessibilityNodeInfo): Boolean =
        root.hasVisibleNodeWithExactText(YOUR_STORY_LABEL)

    fun isSettingsOrOwnProfile(root: AccessibilityNodeInfo): Boolean =
        root.findVisibleNodesByText(SETTINGS_MENU_LABEL).isNotEmpty() ||
            root.findVisibleNodesByText(EDIT_PROFILE_LABEL).isNotEmpty() ||
            root.findVisibleNodesByText(ACTIVITY_FILTER_LABEL).isNotEmpty()

    fun findReelIdentity(root: AccessibilityNodeInfo): String? {
        val description = root.contentDescription?.toString()
        if (root.isVisibleToUser &&
            description != null &&
            root.className?.toString() != GRID_TILE_CLASS_NAME &&
            description.startsWith(REEL_DESCRIPTION_PREFIX) &&
            REEL_DESCRIPTION_SUFFIXES.any { description.endsWith(it) }
        ) {
            return description
        }
        for (index in 0 until root.childCount) {
            val child = root.getChild(index) ?: continue
            val match = findReelIdentity(child)
            if (match != null) {
                return match
            }
        }

        return null
    }

    private const val REEL_DESCRIPTION_PREFIX = "Reel by "
    private val REEL_DESCRIPTION_SUFFIXES = listOf(
        ". Double tap to play or pause.",
        ". Double-tap to play or pause.",
    )
    private const val GRID_TILE_CLASS_NAME = "android.widget.Button"
}
