package com.projectorbit.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val OrbitDarkColorScheme = darkColorScheme(
    primary = OrbitAccent,
    onPrimary = SpaceBlack,
    primaryContainer = OrbitAccentDim,
    onPrimaryContainer = StarWhite,
    secondary = StarYellow,
    onSecondary = SpaceBlack,
    background = SpaceBlack,
    onBackground = OrbitOnSurface,
    surface = OrbitSurface,
    onSurface = OrbitOnSurface,
    surfaceVariant = OrbitSurfaceVariant,
    onSurfaceVariant = OrbitOnSurfaceVariant,
    error = DeleteRed,
    onError = StarWhite
)

@Composable
fun OrbitTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = OrbitDarkColorScheme,
        typography = OrbitTypography,
        content = content
    )
}
