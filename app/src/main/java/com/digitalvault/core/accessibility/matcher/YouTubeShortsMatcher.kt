package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

private fun isShortsPlayer(root: AccessibilityNodeInfo): Boolean {
    var hasComments = false
    var hasLike = false

    root.anyDescendantDescriptionMatches { description ->
        when {
            description.startsWith("View ") && description.endsWith("comments") -> hasComments = true
            description.startsWith("like this video along with") -> hasLike = true
        }
        hasComments && hasLike
    }

    return hasComments && hasLike
}

object YouTubeShortsMatcher : SurfaceMatcher {

    override val id = "youtube_shorts"
    override val packageName = "com.google.android.youtube"
    override val surfaceLabel = "Shorts"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean = isShortsPlayer(root)
}

object YouTubeRvxShortsMatcher : SurfaceMatcher {

    override val id = "youtube_shorts"
    override val packageName = "app.rvx.android.youtube"
    override val surfaceLabel = "Shorts"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean = isShortsPlayer(root)
}
