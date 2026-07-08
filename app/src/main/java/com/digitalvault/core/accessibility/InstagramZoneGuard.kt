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

    fun isMainReelsTab(root: AccessibilityNodeInfo): Boolean =
        root.hasVisibleNodeWithExactText(REELS_TAB_LABEL) && root.hasVisibleNodeWithExactText(FRIENDS_TAB_LABEL)

    fun isSettingsOrOwnProfile(root: AccessibilityNodeInfo): Boolean =
        root.findVisibleNodesByText(SETTINGS_MENU_LABEL).isNotEmpty() ||
            root.findVisibleNodesByText(EDIT_PROFILE_LABEL).isNotEmpty() ||
            root.findVisibleNodesByText(ACTIVITY_FILTER_LABEL).isNotEmpty()
}
