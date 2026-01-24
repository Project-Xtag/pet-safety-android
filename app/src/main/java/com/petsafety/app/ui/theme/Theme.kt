package com.petsafety.app.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    // Primary colors (Brand Orange)
    primary = BrandOrange,
    onPrimary = Color.White,
    primaryContainer = PeachBackground,
    onPrimaryContainer = Color(0xFF3D2000),

    // Secondary colors (Teal Accent)
    secondary = TealAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD0E8E8),
    onSecondaryContainer = Color(0xFF002020),

    // Tertiary (for additional accents)
    tertiary = TealAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFD0E8E8),
    onTertiaryContainer = Color(0xFF002020),

    // Background and Surface
    background = BackgroundLight,
    onBackground = Color(0xFF1C1B1F),
    surface = Color.White,
    onSurface = Color(0xFF1C1B1F),
    surfaceVariant = CardBackgroundLight,
    onSurfaceVariant = MutedTextLight,

    // Container colors
    surfaceContainerLowest = Color.White,
    surfaceContainerLow = Color(0xFFFFFBFF),
    surfaceContainer = PeachBackground,
    surfaceContainerHigh = CardBackgroundLight,
    surfaceContainerHighest = CardBackgroundLight,

    // Error colors
    error = ErrorColor,
    onError = Color.White,
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),

    // Outline colors
    outline = BorderLight,
    outlineVariant = Color(0xFFCAC4D0),

    // Inverse colors
    inverseSurface = Color(0xFF313033),
    inverseOnSurface = Color(0xFFF4EFF4),
    inversePrimary = Color(0xFFFFB77C)
)

private val DarkColorScheme = darkColorScheme(
    // Primary colors (Brand Orange - adjusted for dark mode)
    primary = BrandOrangeDark,
    onPrimary = Color(0xFF4D2600),
    primaryContainer = Color(0xFF6B3A00),
    onPrimaryContainer = Color(0xFFFFDCC2),

    // Secondary colors (Teal Accent)
    secondary = TealAccent,
    onSecondary = Color(0xFF003737),
    secondaryContainer = Color(0xFF004F4F),
    onSecondaryContainer = Color(0xFFA0CFCF),

    // Tertiary
    tertiary = TealAccent,
    onTertiary = Color(0xFF003737),
    tertiaryContainer = Color(0xFF004F4F),
    onTertiaryContainer = Color(0xFFA0CFCF),

    // Background and Surface
    background = BackgroundDark,
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1C1B1F),
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = CardBackgroundDark,
    onSurfaceVariant = MutedTextDark,

    // Container colors
    surfaceContainerLowest = Color(0xFF0D0E11),
    surfaceContainerLow = Color(0xFF1C1B1F),
    surfaceContainer = CardBackgroundDark,
    surfaceContainerHigh = Color(0xFF2B2930),
    surfaceContainerHighest = Color(0xFF36343B),

    // Error colors
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),

    // Outline colors
    outline = BorderDark,
    outlineVariant = Color(0xFF49454F),

    // Inverse colors
    inverseSurface = Color(0xFFE6E1E5),
    inverseOnSurface = Color(0xFF313033),
    inversePrimary = BrandOrange
)

@Composable
fun PetSafetyTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}
