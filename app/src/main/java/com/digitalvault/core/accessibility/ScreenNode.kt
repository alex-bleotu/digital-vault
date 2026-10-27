package com.digitalvault.core.accessibility

import android.view.accessibility.AccessibilityNodeInfo

private const val MAX_CAPTURED_NODES = 4_000

class ScreenNode(
    val text: String?,
    val contentDescription: String?,
    val hintText: String?,
    val isVisibleToUser: Boolean,
    val className: String?,
    val viewIdResourceName: String?,
    val packageName: String?,
    private val children: List<ScreenNode>,
) {
    val childCount: Int
        get() = children.size

    fun getChild(index: Int): ScreenNode? = children.getOrNull(index)

    fun findNodesByText(query: String): List<ScreenNode> {
        val matches = mutableListOf<ScreenNode>()
        collectNodesByText(query, matches)

        return matches
    }

    private fun collectNodesByText(query: String, matches: MutableList<ScreenNode>) {
        if (text?.contains(query, ignoreCase = true) == true || contentDescription?.contains(query, ignoreCase = true) == true) {
            matches.add(this)
        }
        children.forEach { it.collectNodesByText(query, matches) }
    }

    companion object {
        fun capture(root: AccessibilityNodeInfo): ScreenNode {
            var remainingNodes = MAX_CAPTURED_NODES

            fun captureNode(node: AccessibilityNodeInfo): ScreenNode {
                remainingNodes -= 1
                val children = mutableListOf<ScreenNode>()
                for (index in 0 until node.childCount) {
                    if (remainingNodes <= 0) {
                        break
                    }
                    val child = node.getChild(index) ?: continue
                    children.add(captureNode(child))
                }

                return ScreenNode(
                    text = node.text?.toString(),
                    contentDescription = node.contentDescription?.toString(),
                    hintText = node.hintText?.toString(),
                    isVisibleToUser = node.isVisibleToUser,
                    className = node.className?.toString(),
                    viewIdResourceName = node.viewIdResourceName,
                    packageName = node.packageName?.toString(),
                    children = children,
                )
            }

            return captureNode(root)
        }
    }
}
