package com.digitalvault.core.accessibility.matcher

import com.digitalvault.core.accessibility.ScreenNode

object InstagramExploreMatcher : SurfaceMatcher {

    override val id = "instagram_explore_grid"
    override val packageName = "com.instagram.android"
    override val surfaceLabel = "Explore grid"

    private val headerLabels = listOf("Settings", "Search")
    private const val BACK_LABEL = "Back"
    private const val GRID_TILE_DESCRIPTION_MARKER = " at row "

    override fun isTargetSurface(root: ScreenNode): Boolean {
        if (root.findVisibleNodesByText(BACK_LABEL).isNotEmpty()) {
            return false
        }
        if (headerLabels.none { root.hasVisibleNodeWithExactText(it) }) {
            return false
        }

        return countGridTiles(root) >= 2
    }

    private fun countGridTiles(node: ScreenNode): Int {
        val description = node.contentDescription?.toString()
        var count = if (node.isVisibleToUser && description != null && description.contains(GRID_TILE_DESCRIPTION_MARKER)) {
            1
        } else {
            0
        }
        for (index in 0 until node.childCount) {
            val child = node.getChild(index) ?: continue
            count += countGridTiles(child)
            if (count >= 2) {
                return count
            }
        }

        return count
    }
}
