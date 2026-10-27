package com.digitalvault.core.accessibility.matcher

import com.digitalvault.core.accessibility.ScreenNode

interface SurfaceMatcher {
    val id: String
    val packageName: String
    val surfaceLabel: String

    fun isTargetSurface(root: ScreenNode): Boolean
}

fun ScreenNode.findVisibleNodesByText(text: String): List<ScreenNode> =
    findNodesByText(text).filter { it.isVisibleToUser }

fun ScreenNode.hasVisibleNodeWithTextOrHintPrefix(prefix: String): Boolean {
    if (isVisibleToUser) {
        val text = text?.toString()
        val hint = hintText?.toString()
        if ((text != null && text.startsWith(prefix)) || (hint != null && hint.startsWith(prefix))) {
            return true
        }
    }
    for (index in 0 until childCount) {
        if (getChild(index)?.hasVisibleNodeWithTextOrHintPrefix(prefix) == true) {
            return true
        }
    }

    return false
}

fun ScreenNode.hasVisibleNodeWithViewId(viewId: String): Boolean {
    if (isVisibleToUser && viewIdResourceName == viewId) {
        return true
    }
    for (index in 0 until childCount) {
        if (getChild(index)?.hasVisibleNodeWithViewId(viewId) == true) {
            return true
        }
    }

    return false
}

fun ScreenNode.hasVisibleNodeWithExactText(target: String): Boolean {
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

fun ScreenNode.anyDescendantDescriptionMatches(predicate: (CharSequence) -> Boolean): Boolean {
    contentDescription?.let { if (predicate(it)) return true }
    for (index in 0 until childCount) {
        if (getChild(index)?.anyDescendantDescriptionMatches(predicate) == true) {
            return true
        }
    }

    return false
}

fun ScreenNode.anyDescendantTextMatches(predicate: (CharSequence) -> Boolean): Boolean {
    text?.let { if (predicate(it)) return true }
    for (index in 0 until childCount) {
        if (getChild(index)?.anyDescendantTextMatches(predicate) == true) {
            return true
        }
    }

    return false
}

fun ScreenNode.hasDescendantWithExactText(target: String): Boolean {
    if (text?.toString() == target || contentDescription?.toString() == target) {
        return true
    }
    for (index in 0 until childCount) {
        if (getChild(index)?.hasDescendantWithExactText(target) == true) {
            return true
        }
    }

    return false
}

fun ScreenNode.anyVisibleDescendantTextMatches(predicate: (CharSequence) -> Boolean): Boolean {
    if (isVisibleToUser) {
        text?.let { if (predicate(it)) return true }
    }
    for (index in 0 until childCount) {
        if (getChild(index)?.anyVisibleDescendantTextMatches(predicate) == true) {
            return true
        }
    }

    return false
}

fun ScreenNode.anyVisibleDescendantDescriptionMatches(predicate: (CharSequence) -> Boolean): Boolean {
    if (isVisibleToUser) {
        contentDescription?.let { if (predicate(it)) return true }
    }
    for (index in 0 until childCount) {
        if (getChild(index)?.anyVisibleDescendantDescriptionMatches(predicate) == true) {
            return true
        }
    }

    return false
}
