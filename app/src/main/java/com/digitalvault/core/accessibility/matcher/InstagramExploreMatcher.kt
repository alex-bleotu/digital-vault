package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object InstagramExploreMatcher : SurfaceMatcher {

    override val id = "instagram_explore_grid"
    override val packageName = "com.instagram.android"
    override val surfaceLabel = "Explore grid"

    private const val SETTINGS_LABEL = "Settings"
    private const val BACK_LABEL = "Back"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean {
        if (root.findVisibleNodesByText(BACK_LABEL).isNotEmpty()) {
            return false
        }

        return root.hasVisibleNodeWithExactText(SETTINGS_LABEL)
    }
}
