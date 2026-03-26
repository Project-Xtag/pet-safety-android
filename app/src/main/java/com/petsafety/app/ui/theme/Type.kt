package com.petsafety.app.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

// Typography matching iOS app design
// iOS uses system fonts with specific weights and sizes

private val PhoneTypography = Typography(
    // Display styles - for very large text
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 57.sp,
        lineHeight = 64.sp,
        letterSpacing = (-0.25).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 45.sp,
        lineHeight = 52.sp,
        letterSpacing = 0.sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 36.sp,
        lineHeight = 44.sp,
        letterSpacing = 0.sp
    ),

    // Headline styles - for section headers (iOS: 32pt bold for large titles)
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 32.sp,
        lineHeight = 40.sp,
        letterSpacing = 0.sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 36.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),

    // Title styles - for card titles, section names (iOS: ~22pt bold)
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 18.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),

    // Body styles - for main content
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 24.sp,
        letterSpacing = 0.5.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.4.sp
    ),

    // Label styles - for buttons, form labels
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 16.sp,
        lineHeight = 20.sp,
        letterSpacing = 0.1.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 15.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.5.sp
    )
)

// Tablet typography — ~30% larger fonts for readability on bigger screens
private val TabletTypography = Typography(
    displayLarge = PhoneTypography.displayLarge.copy(fontSize = 72.sp, lineHeight = 80.sp),
    displayMedium = PhoneTypography.displayMedium.copy(fontSize = 56.sp, lineHeight = 64.sp),
    displaySmall = PhoneTypography.displaySmall.copy(fontSize = 46.sp, lineHeight = 54.sp),

    headlineLarge = PhoneTypography.headlineLarge.copy(fontSize = 40.sp, lineHeight = 48.sp),
    headlineMedium = PhoneTypography.headlineMedium.copy(fontSize = 36.sp, lineHeight = 44.sp),
    headlineSmall = PhoneTypography.headlineSmall.copy(fontSize = 30.sp, lineHeight = 38.sp),

    titleLarge = PhoneTypography.titleLarge.copy(fontSize = 28.sp, lineHeight = 36.sp),
    titleMedium = PhoneTypography.titleMedium.copy(fontSize = 23.sp, lineHeight = 30.sp),
    titleSmall = PhoneTypography.titleSmall.copy(fontSize = 18.sp, lineHeight = 24.sp),

    bodyLarge = PhoneTypography.bodyLarge.copy(fontSize = 20.sp, lineHeight = 30.sp),
    bodyMedium = PhoneTypography.bodyMedium.copy(fontSize = 18.sp, lineHeight = 26.sp),
    bodySmall = PhoneTypography.bodySmall.copy(fontSize = 15.sp, lineHeight = 22.sp),

    labelLarge = PhoneTypography.labelLarge.copy(fontSize = 20.sp, lineHeight = 26.sp),
    labelMedium = PhoneTypography.labelMedium.copy(fontSize = 19.sp, lineHeight = 24.sp),
    labelSmall = PhoneTypography.labelSmall.copy(fontSize = 15.sp, lineHeight = 20.sp)
)

// Default for backward compat (used nowhere directly after this change)
val Typography = PhoneTypography

/**
 * Returns the appropriate Typography based on screen size.
 * Phone: standard sizes. Tablet (>=600dp): ~15% larger.
 */
@Composable
fun adaptiveTypography(): Typography {
    val configuration = LocalConfiguration.current
    // Use tablet typography for tablets and large phones (>=440dp covers 10" tablet emulators)
    return if (configuration.smallestScreenWidthDp >= 440) TabletTypography else PhoneTypography
}
