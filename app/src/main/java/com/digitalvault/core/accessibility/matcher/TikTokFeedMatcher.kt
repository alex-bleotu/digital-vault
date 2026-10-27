package com.digitalvault.core.accessibility.matcher

import com.digitalvault.core.accessibility.ScreenNode

object TikTokFeedMatcher : SurfaceMatcher {

    override val id = "tiktok_feed"
    override val packageName = "com.zhiliaoapp.musically"
    override val surfaceLabel = "Explore, Following and For You feeds"

    private val discoveryTabLabels = listOf("Explore", "Community")
    private val requiredTabLabels = listOf("Following", "For You")
    private val videoActionDescriptionPrefixes = listOf(
        "Like video",
        "Read or add comments",
        "Share video",
    )
    private const val DM_COMPOSER_TEXT_PREFIX = "Message "

    override fun isTargetSurface(root: ScreenNode): Boolean {
        if (isDmComposerVisible(root)) {
            return false
        }
        if (isTabBarShowing(root)) {
            return true
        }

        return isWatchingFeedVideo(root)
    }

    fun isDmComposerVisible(root: ScreenNode): Boolean =
        root.hasVisibleNodeWithTextOrHintPrefix(DM_COMPOSER_TEXT_PREFIX)

    private fun isTabBarShowing(root: ScreenNode): Boolean =
        discoveryTabLabels.any { root.findVisibleNodesByText(it).isNotEmpty() } &&
            requiredTabLabels.all { root.findVisibleNodesByText(it).isNotEmpty() }

    fun isWatchingFeedVideo(root: ScreenNode): Boolean =
        videoActionDescriptionPrefixes.all { prefix ->
            root.anyVisibleDescendantDescriptionMatches { it.startsWith(prefix) }
        }
}
