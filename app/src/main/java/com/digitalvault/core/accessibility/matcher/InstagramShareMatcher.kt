package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object InstagramShareMatcher : SurfaceMatcher {

    override val id = "instagram_share"
    override val packageName = "com.instagram.android"
    override val surfaceLabel = "Share/more panel"

    private val moreLabels = listOf("Not interested", "Report")
    private val collectionLabels = listOf("Collections", "New collection")

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean =
        root.hasVisibleNodeWithExactText("Copy link") ||
            moreLabels.all { root.hasVisibleNodeWithExactText(it) } ||
            collectionLabels.all { root.hasVisibleNodeWithExactText(it) }
}
