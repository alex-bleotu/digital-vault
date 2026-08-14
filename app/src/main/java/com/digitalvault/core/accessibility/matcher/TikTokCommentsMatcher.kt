package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

object TikTokCommentsMatcher : SurfaceMatcher {

    override val id = "tiktok_comments"
    override val packageName = "com.zhiliaoapp.musically"
    override val surfaceLabel = "Comments panel"

    private const val ADD_COMMENT_PREFIX = "Add comment"
    private const val BACK_LABEL = "Back"

    override fun isTargetSurface(root: AccessibilityNodeInfo): Boolean {
        if (root.hasDescendantWithExactText(BACK_LABEL)) {
            return false
        }

        return root.anyDescendantTextMatches { it.startsWith(ADD_COMMENT_PREFIX) }
    }
}
