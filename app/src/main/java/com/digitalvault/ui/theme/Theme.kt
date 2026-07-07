package com.digitalvault.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider

private val VaultColorScheme = darkColorScheme(
    primary = Brass,
    onPrimary = Ink,
    primaryContainer = SurfaceRaised,
    onPrimaryContainer = TextPrimary,
    secondary = Sage,
    onSecondary = Ink,
    tertiary = Brass,
    onTertiary = Ink,
    background = Ink,
    onBackground = TextPrimary,
    surface = Surface,
    onSurface = TextPrimary,
    surfaceVariant = SurfaceRaised,
    onSurfaceVariant = TextMuted,
    surfaceContainer = Surface,
    surfaceContainerHigh = SurfaceRaised,
    error = Rust,
    onError = TextPrimary,
    outline = TextMuted,
    outlineVariant = SurfaceRaised,
)

@Composable
fun DigitalVaultTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalVaultColors provides vaultColors) {
        MaterialTheme(
            colorScheme = VaultColorScheme,
            typography = VaultTypography,
            shapes = VaultShapes,
            content = content,
        )
    }
}

object VaultTheme {
    val colors: VaultColors
        @Composable get() = LocalVaultColors.current

    val shapes: androidx.compose.material3.Shapes
        @Composable get() = MaterialTheme.shapes
}
