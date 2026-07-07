package com.digitalvault.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

@Immutable
data class VaultColors(
    val ink: Color,
    val surface: Color,
    val surfaceRaised: Color,
    val brass: Color,
    val rust: Color,
    val sage: Color,
    val textPrimary: Color,
    val textMuted: Color,
)

val vaultColors = VaultColors(
    ink = Ink,
    surface = Surface,
    surfaceRaised = SurfaceRaised,
    brass = Brass,
    rust = Rust,
    sage = Sage,
    textPrimary = TextPrimary,
    textMuted = TextMuted,
)

val LocalVaultColors = staticCompositionLocalOf { vaultColors }
