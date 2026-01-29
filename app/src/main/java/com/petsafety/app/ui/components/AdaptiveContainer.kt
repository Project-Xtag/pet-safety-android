package com.petsafety.app.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.petsafety.app.ui.util.AdaptiveLayout

/**
 * A container that centers content and constrains max width on tablets.
 * Used for forms, auth screens, and other content that shouldn't stretch too wide.
 */
@Composable
fun AdaptiveContentContainer(
    modifier: Modifier = Modifier,
    maxWidth: androidx.compose.ui.unit.Dp = AdaptiveLayout.MaxContentWidth,
    content: @Composable () -> Unit
) {
    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.TopCenter
    ) {
        Box(
            modifier = Modifier
                .widthIn(max = maxWidth)
                .fillMaxWidth()
        ) {
            content()
        }
    }
}

/**
 * A container for wider content like lists with details.
 * Uses a larger max width than AdaptiveContentContainer.
 */
@Composable
fun AdaptiveWideContainer(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    AdaptiveContentContainer(
        modifier = modifier,
        maxWidth = AdaptiveLayout.MaxWideContentWidth,
        content = content
    )
}
