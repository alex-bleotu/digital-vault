package com.digitalvault.core.accessibility

import com.digitalvault.core.accessibility.matcher.findVisibleNodesByText
import com.digitalvault.core.accessibility.matcher.hasVisibleNodeWithExactText
import com.digitalvault.core.accessibility.matcher.hasVisibleSelectedNodeWithDescription

object InstagramZoneGuard {

    const val PACKAGE_NAME = "com.instagram.android"

    private const val SETTINGS_MENU_LABEL = "Settings and activity"
    private const val EDIT_PROFILE_LABEL = "Edit profile"
    private const val ACTIVITY_FILTER_LABEL = "All content types"
    private const val REELS_TAB_LABEL = "Reels"
    private const val FRIENDS_TAB_LABEL = "Friends"
    private const val YOUR_STORY_LABEL = "Your story"
    private const val STORY_TRAY_LABEL = "reels tray container"
    private const val BACK_BUTTON_LABEL = "Back"
    private const val HOME_TAB_LABEL = "Home"
    private const val PROFILE_TAB_LABEL = "Profile"
    private const val MESSAGE_TAB_LABEL = "Message"
    private const val SEARCH_TAB_LABEL = "Search and explore"
    private val NON_HOME_TAB_LABELS = listOf(MESSAGE_TAB_LABEL, PROFILE_TAB_LABEL, REELS_TAB_LABEL, SEARCH_TAB_LABEL)

    fun isMainReelsTab(root: ScreenNode): Boolean =
        root.hasVisibleNodeWithExactText(REELS_TAB_LABEL) &&
            root.hasVisibleNodeWithExactText(FRIENDS_TAB_LABEL) &&
            !root.hasVisibleNodeWithExactText(BACK_BUTTON_LABEL)

    fun isHomeFeed(root: ScreenNode): Boolean =
        root.findVisibleNodesByText(YOUR_STORY_LABEL).any { it.text?.toString() == YOUR_STORY_LABEL } ||
            root.hasVisibleNodeWithExactText(STORY_TRAY_LABEL)

    fun isHomeFeedShowing(root: ScreenNode): Boolean =
        root.hasVisibleNodeWithExactText(HOME_TAB_LABEL) &&
            root.hasVisibleNodeWithExactText(PROFILE_TAB_LABEL) &&
            !root.hasVisibleNodeWithExactText(BACK_BUTTON_LABEL) &&
            !isMainReelsTab(root) &&
            !isSettingsOrOwnProfile(root) &&
            NON_HOME_TAB_LABELS.none { root.hasVisibleSelectedNodeWithDescription(it) }

    fun isOwnProfileShowing(root: ScreenNode): Boolean =
        root.hasVisibleSelectedNodeWithDescription(PROFILE_TAB_LABEL) &&
            !root.hasVisibleNodeWithExactText(BACK_BUTTON_LABEL)

    fun isSettingsOrOwnProfile(root: ScreenNode): Boolean =
        root.findVisibleNodesByText(SETTINGS_MENU_LABEL).isNotEmpty() ||
            root.findVisibleNodesByText(EDIT_PROFILE_LABEL).isNotEmpty() ||
            root.findVisibleNodesByText(ACTIVITY_FILTER_LABEL).isNotEmpty()

    fun findReelIdentity(root: ScreenNode): String? {
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
