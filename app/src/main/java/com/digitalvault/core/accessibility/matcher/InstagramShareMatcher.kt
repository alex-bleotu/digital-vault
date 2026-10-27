package com.digitalvault.core.accessibility.matcher

import com.digitalvault.core.accessibility.ScreenNode

object InstagramShareMatcher : SurfaceMatcher {

    override val id = "instagram_share"
    override val packageName = "com.instagram.android"
    override val surfaceLabel = "Share/more panel"

    private val moreLabels = listOf("Not interested", "Report")
    private val collectionLabels = listOf("Collections", "New collection")
    private const val FEED_POST_MORE_MENU_LABEL = "Why you're seeing this"

    override fun isTargetSurface(root: ScreenNode): Boolean {
        if (root.hasVisibleNodeWithExactText(FEED_POST_MORE_MENU_LABEL)) {
            return false
        }

        return root.hasVisibleNodeWithExactText("Copy link") ||
            moreLabels.all { root.hasVisibleNodeWithExactText(it) } ||
            collectionLabels.all { root.hasVisibleNodeWithExactText(it) }
    }
}
