package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object InstagramSearchPostMatcher : SurfaceMatcher {

    override val id = "instagram_search_post"
    override val packageName = "com.instagram.android"
    override val surfaceLabel = "Posts opened from Search"

    private const val ADD_COMMENT_LABEL = "Add comment"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean =
        root.findVisibleNodesByText(ADD_COMMENT_LABEL).isNotEmpty()
}
