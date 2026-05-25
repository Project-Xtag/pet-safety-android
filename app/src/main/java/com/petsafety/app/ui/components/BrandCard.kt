package com.petsafety.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.petsafety.app.ui.theme.AppRadius
import com.petsafety.app.ui.theme.AppSpacing
import com.petsafety.app.ui.theme.CreamSurface
import com.petsafety.app.ui.theme.PeachBackground
import com.petsafety.app.ui.theme.SoftBorderColor

/**
 * Soft card — cream surface, hairline border, warm shadow,
 * continuous-feel rounded corners. Mirrors iOS softCard() modifier
 * and the redesign7 web rounded-3xl border-stone-200 pattern.
 */
@Composable
fun BrandCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(AppRadius.lg)
    val border = SoftBorderColor
    val bg = CreamSurface
    val base = modifier
        .fillMaxWidth()
        .shadow(
            elevation = 6.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.04f),
            spotColor = Color.Black.copy(alpha = 0.04f)
        )
        .clip(shape)
        .background(bg)
        .border(1.dp, border, shape)

    val withClick = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(
        modifier = withClick.padding(AppSpacing.xl),
        content = content
    )
}

/**
 * Peach header section — kept for screens that intentionally need the
 * accent wash (e.g. promotional banners). Headers on regular content
 * surfaces now use cream via softCard.
 */
@Composable
fun PeachHeader(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = AppRadius.lg, bottomEnd = AppRadius.lg))
            .background(PeachBackground)
            .padding(AppSpacing.lg)
    ) {
        content()
    }
}

/**
 * Elevated card — white / surface-color, slightly deeper shadow.
 * Use sparingly for the dominant card on a screen; cream cards sit
 * "below" elevated ones in visual hierarchy. iOS elevatedCard().
 */
@Composable
fun ElevatedBrandCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(AppRadius.lg)
    val base = modifier
        .fillMaxWidth()
        .shadow(
            elevation = 10.dp,
            shape = shape,
            ambientColor = Color.Black.copy(alpha = 0.06f),
            spotColor = Color.Black.copy(alpha = 0.06f)
        )
        .clip(shape)
        .background(MaterialTheme.colorScheme.surface)
        .border(1.dp, SoftBorderColor, shape)

    val withClick = if (onClick != null) base.clickable(onClick = onClick) else base
    Column(
        modifier = withClick.padding(AppSpacing.xl),
        content = content
    )
}
