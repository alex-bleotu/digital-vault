package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object InstagramHomeFeedMatcher : SurfaceMatcher {

    override val id = "instagram_home_feed_limit"
    override val packageName = "com.instagram.android"
    override val surfaceLabel = "Home feed (after 3 posts)"

    private const val MAX_POSTS_BEFORE_BLOCK = 3
    private const val CAPTION_CLASS_NAME = "com.instagram.ui.widget.textview.IgTextLayoutView"
    private const val SESSION_GAP_MILLIS = 60_000L
    private val otherSurfaceMarkers = listOf("Reply to", "Add comment", "Send to chat", "Messages", "Requests")

    private val seenCaptions = mutableSetOf<String>()
    private var lastSeenAtMillis = 0L

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean {
        val onOtherSurface = otherSurfaceMarkers.any { root.findVisibleNodesByText(it).isNotEmpty() } ||
            isOnReelsTab(root)
        if (onOtherSurface) {
            seenCaptions.clear()

            return false
        }

        val currentCaptions = collectCaptions(root)
        if (currentCaptions.isEmpty()) {
            return false
        }

        val now = System.currentTimeMillis()
        if (now - lastSeenAtMillis > SESSION_GAP_MILLIS) {
            seenCaptions.clear()
        }
        lastSeenAtMillis = now

        currentCaptions.forEach { seenCaptions.add(it) }

        return seenCaptions.size > MAX_POSTS_BEFORE_BLOCK
    }

    private fun isOnReelsTab(root: AccessibilityNodeInfo): Boolean =
        root.findVisibleNodesByText("Reels").isNotEmpty() && root.findVisibleNodesByText("Friends").isNotEmpty()

    private fun collectCaptions(root: AccessibilityNodeInfo): List<String> {
        val captions = mutableListOf<String>()
        collect(root, captions)

        return captions
    }

    private fun collect(node: AccessibilityNodeInfo?, out: MutableList<String>) {
        if (node == null) {
            return
        }
        if (node.className == CAPTION_CLASS_NAME && node.isVisibleToUser) {
            node.text?.toString()?.let(out::add)
        }
        for (index in 0 until node.childCount) {
            collect(node.getChild(index), out)
        }
    }
}
