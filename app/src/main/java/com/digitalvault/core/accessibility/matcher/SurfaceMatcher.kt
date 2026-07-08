package com.digitalvault.core.accessibility.matcher

import android.view.accessibility.AccessibilityNodeInfo

interface SurfaceMatcher {
    val id: String
    val packageName: String
    val surfaceLabel: String

    fun isTargetSurface(root: AccessibilityNodeInfo): Boolean
}

fun AccessibilityNodeInfo.findVisibleNodesByText(text: String): List<AccessibilityNodeInfo> =
    findAccessibilityNodeInfosByText(text).filter { it.isVisibleToUser }

fun AccessibilityNodeInfo.hasVisibleNodeWithExactText(target: String): Boolean {
    if (isVisibleToUser && (text?.toString() == target || contentDescription?.toString() == target)) {
        return true
    }
    for (index in 0 until childCount) {
        if (getChild(index)?.hasVisibleNodeWithExactText(target) == true) {
            return true
        }
    }

    return false
}

fun AccessibilityNodeInfo.anyDescendantDescriptionMatches(predicate: (CharSequence) -> Boolean): Boolean {
    contentDescription?.let { if (predicate(it)) return true }
    for (index in 0 until childCount) {
        if (getChild(index)?.anyDescendantDescriptionMatches(predicate) == true) {
            return true
        }
    }

    return false
}
