package com.digitalvault.core.accessibility.matcher

import com.digitalvault.core.accessibility.ScreenNode

object InstagramSpeedMatcher : SurfaceMatcher {

    override val id = "instagram_speed"
    override val packageName = "com.instagram.android"
    override val surfaceLabel = "Long-press playback speed"

    private const val SPEED_LABEL_PREFIX = "Slide down to lock"

    override fun isTargetSurface(root: ScreenNode): Boolean =
        root.anyDescendantTextMatches { it.startsWith(SPEED_LABEL_PREFIX) } ||
            root.anyDescendantDescriptionMatches { it.startsWith(SPEED_LABEL_PREFIX) }
}
