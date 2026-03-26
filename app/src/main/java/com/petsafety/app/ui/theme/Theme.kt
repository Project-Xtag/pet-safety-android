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

// Dark colors matched to iOS asset catalog dark appearances
private val DarkColorScheme = darkColorScheme(
    // Primary colors (Brand Orange — same in dark per iOS)
    primary = BrandOrangeDark,
    onPrimary = Color.White,
    primaryContainer = Color(0xFF1E1E1E), // iOS PeachBackground dark: 0.118
    onPrimaryContainer = Color(0xFFFFDCC2),

    // Secondary colors (Teal Accent — same in both modes per iOS)
    secondary = TealAccent,
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF004F4F),
    onSecondaryContainer = Color(0xFFA0CFCF),

    // Tertiary
    tertiary = TealAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFF004F4F),
    onTertiaryContainer = Color(0xFFA0CFCF),

    // Background and Surface — iOS: BackgroundColor dark = 0.122, 0.106, 0.180 → #1F1B2E
    background = BackgroundDark,             // #1F1B2E — matches iOS
    onBackground = Color(0xFFE6E1E5),
    surface = Color(0xFF1E1E1E),             // iOS PeachBackground dark
    onSurface = Color(0xFFE6E1E5),
    surfaceVariant = CardBackgroundDark,      // #262626 — matches iOS CardBackground dark
    onSurfaceVariant = MutedTextDark,         // #999999 — matches iOS MutedText dark

    // Container colors
    surfaceContainerLowest = Color(0xFF121212),
    surfaceContainerLow = Color(0xFF1C1B1F),
    surfaceContainer = Color(0xFF262626),     // iOS CardBackground dark
    surfaceContainerHigh = Color(0xFF2C2C2C),
    surfaceContainerHighest = Color(0xFF333333),

    // Error colors — iOS: #F75757 same in both modes
    error = ErrorColor,
    onError = Color.White,
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
    val typography = adaptiveTypography()

    MaterialTheme(
        colorScheme = colorScheme,
        typography = typography,
        shapes = Shapes,
        content = content
    )
}
