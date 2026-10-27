package com.digitalvault.core.accessibility.matcher

import com.digitalvault.core.accessibility.ScreenNode

object TikTokCommentsMatcher : SurfaceMatcher {

    override val id = "tiktok_comments"
    override val packageName = "com.zhiliaoapp.musically"
    override val surfaceLabel = "Comments panel"

    private const val ADD_COMMENT_PREFIX = "Add comment"
    private const val BACK_LABEL = "Back"

    override fun isTargetSurface(root: ScreenNode): Boolean {
        if (root.hasDescendantWithExactText(BACK_LABEL)) {
            return false
        }

        return root.anyDescendantTextMatches { it.startsWith(ADD_COMMENT_PREFIX) }
    }
}
