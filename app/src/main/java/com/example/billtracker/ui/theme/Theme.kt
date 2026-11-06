package com.example.billtracker.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import com.example.billtracker.domain.model.ThemeMode

// 1. Midnight Dark Color Scheme
val MidnightColorScheme = darkColorScheme(
    primary = MidnightPrimary,
    onPrimary = MidnightOnPrimary,
    primaryContainer = MidnightPrimaryContainer,
    onPrimaryContainer = MidnightOnPrimaryContainer,
    secondary = MidnightSecondary,
    onSecondary = MidnightOnSecondary,
    background = MidnightBackground,
    onBackground = MidnightOnBackground,
    surface = MidnightSurface,
    onSurface = MidnightOnSurface,
    surfaceVariant = MidnightSurfaceVariant,
    onSurfaceVariant = MidnightOnSurfaceVariant,
    outline = MidnightOutline,
    error = ErrorRed,
    onError = OnErrorWhite
)

// 2. Graphite Dark Color Scheme
val GraphiteColorScheme = darkColorScheme(
    primary = GraphitePrimary,
    onPrimary = GraphiteOnPrimary,
    primaryContainer = GraphitePrimaryContainer,
    onPrimaryContainer = GraphiteOnPrimaryContainer,
    secondary = GraphiteSecondary,
    onSecondary = GraphiteOnSecondary,
    background = GraphiteBackground,
    onBackground = GraphiteOnBackground,
    surface = GraphiteSurface,
    onSurface = GraphiteOnSurface,
    surfaceVariant = GraphiteSurfaceVariant,
    onSurfaceVariant = GraphiteOnSurfaceVariant,
    outline = GraphiteOutline,
    error = ErrorRed,
    onError = OnErrorWhite
)

// 3. Clean Light Color Scheme
val CleanColorScheme = lightColorScheme(
    primary = CleanPrimary,
    onPrimary = CleanOnPrimary,
    primaryContainer = CleanPrimaryContainer,
    onPrimaryContainer = CleanOnPrimaryContainer,
    secondary = CleanSecondary,
    onSecondary = CleanOnSecondary,
    background = CleanBackground,
    onBackground = CleanOnBackground,
    surface = CleanSurface,
    onSurface = CleanOnSurface,
    surfaceVariant = CleanSurfaceVariant,
    onSurfaceVariant = CleanOnSurfaceVariant,
    outline = CleanOutline,
    error = ErrorRed,
    onError = OnErrorWhite,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed
)

// 4. Warm Light Color Scheme
val WarmColorScheme = lightColorScheme(
    primary = WarmPrimary,
    onPrimary = WarmOnPrimary,
    primaryContainer = WarmPrimaryContainer,
    onPrimaryContainer = WarmOnPrimaryContainer,
    secondary = WarmSecondary,
    onSecondary = WarmOnSecondary,
    background = WarmBackground,
    onBackground = WarmOnBackground,
    surface = WarmSurface,
    onSurface = WarmOnSurface,
    surfaceVariant = WarmSurfaceVariant,
    onSurfaceVariant = WarmOnSurfaceVariant,
    outline = WarmOutline,
    error = ErrorRed,
    onError = OnErrorWhite,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed
)

@Composable
fun BillTrackerTheme(
    themeMode: ThemeMode = ThemeMode.CLEAN,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when (themeMode) {
        ThemeMode.MIDNIGHT -> MidnightColorScheme
        ThemeMode.GRAPHITE -> GraphiteColorScheme
        ThemeMode.CLEAN -> CleanColorScheme
        ThemeMode.WARM -> WarmColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
