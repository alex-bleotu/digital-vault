package com.digitalvault.core.data.model

enum class BlockMode {
    FULL_BLOCK,
    SURFACE_BLOCK,
    UNRESTRICTED,
}

data class AppRule(
    val packageName: String,
    val mode: BlockMode,
    val graceSeconds: Int = 10,
    val targetSurfaces: List<String> = emptyList(),
    val allowBreak: Boolean = true,
)
