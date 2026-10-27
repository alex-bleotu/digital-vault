package com.digitalvault.core.accessibility.matcher

import com.digitalvault.core.accessibility.ScreenNode

object InstagramExplorePreviewMatcher : SurfaceMatcher {

    override val id = "instagram_explore_preview"
    override val packageName = "com.instagram.android"
    override val surfaceLabel = "Explore video preview (long-press)"

    private val requiredLabels = listOf("View profile", "Repost", "Not interested")

    override fun isTargetSurface(root: ScreenNode): Boolean =
        requiredLabels.all { root.hasVisibleNodeWithExactText(it) }
}
