package com.digitalvault.core.accessibility.matcher

object SurfaceMatchers {

    val all: List<SurfaceMatcher> = listOf(
        InstagramReelsMatcher,
        InstagramSuggestedReelMatcher,
        InstagramSearchPostMatcher,
        InstagramExplorePreviewMatcher,
        InstagramExploreMatcher,
        InstagramSpeedMatcher,
        TikTokFeedMatcher,
        TikTokSearchMatcher,
        TikTokSpeedMatcher,
        TikTokLongPressMenuMatcher,
        TikTokCommentsMatcher,
        TikTokShareMatcher,
        TikTokLiveMatcher,
        ChromeIncognitoMatcher,
        YouTubeShortsMatcher,
        YouTubeRvxShortsMatcher,
    )

    fun forPackage(packageName: String): List<SurfaceMatcher> =
        all.filter { it.packageName == packageName }

    fun supportsSurfaceBlock(packageName: String): Boolean =
        all.any { it.packageName == packageName }

    fun defaultSurfaceIds(packageName: String): List<String> =
        forPackage(packageName).map { it.id }
}
