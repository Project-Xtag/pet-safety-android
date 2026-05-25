package com.petsafety.app.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

// Spacing scale shared with redesign7 web + iOS AppSpacing.
// 4 / 8 / 12 / 16 / 24 / 32.
object AppSpacing {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
    val xxl = 32.dp
}

// Radius scale shared with iOS AppRadius. Pill = effectively infinite.
object AppRadius {
    val sm = 10.dp
    val md = 14.dp
    val lg = 20.dp   // card default
    val xl = 28.dp   // hero card
    val pill = 999.dp
}

// Brand gradient (warm → deep) for hero CTAs and section accents.
// Mirrors linear-gradient(135deg, BRAND, BRAND_DEEP) on redesign7 web.
@Composable
@ReadOnlyComposable
fun brandGradient(): Brush {
    val isDark = isSystemInDarkTheme()
    return Brush.linearGradient(
        colors = if (isDark) {
            listOf(BrandOrangeDark, BrandOrangeDeepDark)
        } else {
            listOf(BrandOrange, BrandOrangeDeep)
        }
    )
}

// Cream theme-aware accessors so screens don't have to branch.
val CreamSurface: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) CreamDark else Cream

val InkText: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) InkDark else Ink

val SoftBorderColor: Color
    @Composable
    @ReadOnlyComposable
    get() = if (isSystemInDarkTheme()) SoftBorderDark else SoftBorder

/**
 * Cream rounded card surface — soft warm shadow + hairline border,
 * continuous-style rounded corners. Mirrors iOS softCard() modifier.
 * Defaults match the redesign7 large-card spec (20.dp padding +
 * corner). Pass custom values for tighter / chunkier surfaces.
 */
fun Modifier.softCard(
    padding: androidx.compose.ui.unit.Dp = AppSpacing.xl,
    radius: androidx.compose.ui.unit.Dp = AppRadius.lg
): Modifier = composed {
    val shape = RoundedCornerShape(radius)
    this
        .shadow(
            elevation = 6.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.04f)
        )
        .clip(shape)
        .background(CreamSurface)
        .border(1.dp, SoftBorderColor, shape)
        .padding(padding)
}

/**
 * White / surface elevated card — same rounded soft-shadow shape but
 * sits "above" cream cards in visual hierarchy. iOS elevatedCard().
 */
fun Modifier.elevatedCard(
    padding: androidx.compose.ui.unit.Dp = AppSpacing.xl,
    radius: androidx.compose.ui.unit.Dp = AppRadius.lg
): Modifier = composed {
    val shape = RoundedCornerShape(radius)
    val bg = MaterialTheme.colorScheme.surface
    this
        .shadow(
            elevation = 10.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.06f),
            spotColor = Color.Black.copy(alpha = 0.06f)
        )
        .clip(shape)
        .background(bg)
        .border(1.dp, SoftBorderColor, shape)
        .padding(padding)
}
