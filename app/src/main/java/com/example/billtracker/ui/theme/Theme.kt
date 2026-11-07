package com.example.billtracker.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.billtracker.domain.model.ThemeMode

// Theme-specific styling tokens to achieve genuinely distinct visual identities
data class ThemeStyleConfig(
    val cardShape: Shape,
    val buttonShape: Shape,
    val cardBorder: BorderStroke? = null,
    val elevationDp: Dp = 2.dp,
    val headerGradient: Brush? = null,
    val isGlassOrClay: Boolean = false
)

val LocalThemeStyleConfig = staticCompositionLocalOf {
    ThemeStyleConfig(
        cardShape = RoundedCornerShape(16.dp),
        buttonShape = RoundedCornerShape(12.dp)
    )
}

// 1. Futuristic Dark Color Scheme
val FuturisticColorScheme = darkColorScheme(
    primary = FuturisticPrimary,
    onPrimary = FuturisticOnPrimary,
    primaryContainer = FuturisticPrimaryContainer,
    onPrimaryContainer = FuturisticOnPrimaryContainer,
    secondary = FuturisticSecondary,
    onSecondary = FuturisticOnSecondary,
    background = FuturisticBackground,
    onBackground = FuturisticOnBackground,
    surface = FuturisticSurface,
    onSurface = FuturisticOnSurface,
    surfaceVariant = FuturisticSurfaceVariant,
    onSurfaceVariant = FuturisticOnSurfaceVariant,
    outline = FuturisticOutline,
    error = ErrorRed,
    onError = OnErrorWhite
)

// 2. Liquid Glass Dark Color Scheme
val LiquidGlassColorScheme = darkColorScheme(
    primary = LiquidGlassPrimary,
    onPrimary = LiquidGlassOnPrimary,
    primaryContainer = LiquidGlassPrimaryContainer,
    onPrimaryContainer = LiquidGlassOnPrimaryContainer,
    secondary = LiquidGlassSecondary,
    onSecondary = LiquidGlassOnSecondary,
    background = LiquidGlassBackground,
    onBackground = LiquidGlassOnBackground,
    surface = LiquidGlassSurface,
    onSurface = LiquidGlassOnSurface,
    surfaceVariant = LiquidGlassSurfaceVariant,
    onSurfaceVariant = LiquidGlassOnSurfaceVariant,
    outline = LiquidGlassOutline,
    error = ErrorRed,
    onError = OnErrorWhite
)

// 3. Claymorphism Light Color Scheme
val ClaymorphismColorScheme = lightColorScheme(
    primary = ClaymorphismPrimary,
    onPrimary = ClaymorphismOnPrimary,
    primaryContainer = ClaymorphismPrimaryContainer,
    onPrimaryContainer = ClaymorphismOnPrimaryContainer,
    secondary = ClaymorphismSecondary,
    onSecondary = ClaymorphismOnSecondary,
    background = ClaymorphismBackground,
    onBackground = ClaymorphismOnBackground,
    surface = ClaymorphismSurface,
    onSurface = ClaymorphismOnSurface,
    surfaceVariant = ClaymorphismSurfaceVariant,
    onSurfaceVariant = ClaymorphismOnSurfaceVariant,
    outline = ClaymorphismOutline,
    error = ErrorRed,
    onError = OnErrorWhite,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed
)

// 4. Glassmorphism Light Color Scheme
val GlassmorphismColorScheme = lightColorScheme(
    primary = GlassmorphismPrimary,
    onPrimary = GlassmorphismOnPrimary,
    primaryContainer = GlassmorphismPrimaryContainer,
    onPrimaryContainer = GlassmorphismOnPrimaryContainer,
    secondary = GlassmorphismSecondary,
    onSecondary = GlassmorphismOnSecondary,
    background = GlassmorphismBackground,
    onBackground = GlassmorphismOnBackground,
    surface = GlassmorphismSurface,
    onSurface = GlassmorphismOnSurface,
    surfaceVariant = GlassmorphismSurfaceVariant,
    onSurfaceVariant = GlassmorphismOnSurfaceVariant,
    outline = GlassmorphismOutline,
    error = ErrorRed,
    onError = OnErrorWhite,
    errorContainer = ErrorContainerRed,
    onErrorContainer = OnErrorContainerRed
)

// Theme-specific shapes
val FuturisticShapes = Shapes(
    small = RoundedCornerShape(4.dp),
    medium = RoundedCornerShape(8.dp),
    large = RoundedCornerShape(12.dp),
    extraLarge = RoundedCornerShape(16.dp)
)

val LiquidGlassShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(18.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

val ClaymorphismShapes = Shapes(
    small = RoundedCornerShape(16.dp),
    medium = RoundedCornerShape(22.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

val GlassmorphismShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(20.dp),
    extraLarge = RoundedCornerShape(24.dp)
)

@Composable
fun BillTrackerTheme(
    themeMode: ThemeMode = ThemeMode.GLASSMORPHISM,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val (colorScheme, shapes, styleConfig) = when (themeMode) {
        ThemeMode.FUTURISTIC -> Triple(
            FuturisticColorScheme,
            FuturisticShapes,
            ThemeStyleConfig(
                cardShape = RoundedCornerShape(10.dp),
                buttonShape = RoundedCornerShape(6.dp),
                cardBorder = BorderStroke(1.dp, FuturisticOutline.copy(alpha = 0.4f)),
                elevationDp = 0.dp,
                headerGradient = Brush.horizontalGradient(
                    listOf(Color(0xFF004D5A), Color(0xFF240046))
                )
            )
        )
        ThemeMode.LIQUID_GLASS -> Triple(
            LiquidGlassColorScheme,
            LiquidGlassShapes,
            ThemeStyleConfig(
                cardShape = RoundedCornerShape(22.dp),
                buttonShape = RoundedCornerShape(18.dp),
                cardBorder = BorderStroke(1.dp, LiquidGlassOutline.copy(alpha = 0.3f)),
                elevationDp = 4.dp,
                headerGradient = Brush.verticalGradient(
                    listOf(Color(0xFF0C4A6E), Color(0xFF081220))
                ),
                isGlassOrClay = true
            )
        )
        ThemeMode.CLAYMORPHISM -> Triple(
            ClaymorphismColorScheme,
            ClaymorphismShapes,
            ThemeStyleConfig(
                cardShape = RoundedCornerShape(28.dp),
                buttonShape = RoundedCornerShape(24.dp),
                cardBorder = BorderStroke(1.5.dp, Color(0xFFFFFFFF).copy(alpha = 0.8f)),
                elevationDp = 6.dp,
                headerGradient = Brush.horizontalGradient(
                    listOf(Color(0xFFDED9F7), Color(0xFFC7F9CC))
                ),
                isGlassOrClay = true
            )
        )
        ThemeMode.GLASSMORPHISM -> Triple(
            GlassmorphismColorScheme,
            GlassmorphismShapes,
            ThemeStyleConfig(
                cardShape = RoundedCornerShape(18.dp),
                buttonShape = RoundedCornerShape(14.dp),
                cardBorder = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                elevationDp = 2.dp,
                headerGradient = Brush.linearGradient(
                    listOf(Color(0xFFDBEAFE), Color(0xFFE0E7FF))
                )
            )
        )
    }

    CompositionLocalProvider(LocalThemeStyleConfig provides styleConfig) {
        MaterialTheme(
            colorScheme = colorScheme,
            shapes = shapes,
            typography = Typography,
            content = content
        )
    }
}
