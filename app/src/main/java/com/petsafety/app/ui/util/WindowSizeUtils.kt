package com.petsafety.app.ui.util

import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Utility object for responsive layouts based on screen size.
 * Provides adaptive values for grids, padding, and max widths.
 */
object AdaptiveLayout {

    /**
     * Maximum content width for forms and centered content on large screens.
     * Prevents content from stretching too wide on tablets.
     */
    val MaxContentWidth = 600.dp

    /**
     * Maximum content width for wider content like lists with details.
     */
    val MaxWideContentWidth = 900.dp

    /**
     * Determines if the current device should use tablet layout.
     */
    @Composable
    fun isTablet(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp >= 600
    }

    /**
     * Determines if the current device is in landscape orientation.
     */
    @Composable
    fun isLandscape(): Boolean {
        val configuration = LocalConfiguration.current
        return configuration.screenWidthDp > configuration.screenHeightDp
    }

    /**
     * Returns the number of columns for a grid based on screen width.
     * - Phones: 2 columns
     * - Tablets portrait: 3 columns
     * - Tablets landscape: 4 columns
     */
    @Composable
    fun gridColumns(): Int {
        val configuration = LocalConfiguration.current
        return when {
            configuration.screenWidthDp >= 900 -> 4
            configuration.screenWidthDp >= 600 -> 3
            else -> 2
        }
    }

    /**
     * Returns the number of columns for photo gallery grids.
     * - Phones: 3 columns
     * - Tablets portrait: 4 columns
     * - Tablets landscape: 5-6 columns
     */
    @Composable
    fun photoGridColumns(): Int {
        val configuration = LocalConfiguration.current
        return when {
            configuration.screenWidthDp >= 900 -> 5
            configuration.screenWidthDp >= 600 -> 4
            else -> 3
        }
    }

    /**
     * Returns horizontal padding based on screen size.
     * Larger screens get more padding to center content.
     */
    @Composable
    fun horizontalPadding(): Dp {
        val configuration = LocalConfiguration.current
        return when {
            configuration.screenWidthDp >= 900 -> 48.dp
            configuration.screenWidthDp >= 600 -> 32.dp
            else -> 16.dp
        }
    }

    /**
     * Returns adaptive padding for cards and list items.
     */
    @Composable
    fun cardPadding(): Dp {
        val configuration = LocalConfiguration.current
        return when {
            configuration.screenWidthDp >= 600 -> 20.dp
            else -> 16.dp
        }
    }

    /**
     * Returns whether to use NavigationRail (tablets) or BottomNavigation (phones).
     */
    @Composable
    fun useNavigationRail(): Boolean {
        val configuration = LocalConfiguration.current
        // Use rail for tablets (width >= 600dp) and landscape tablets
        return configuration.screenWidthDp >= 600
    }

    /**
     * Returns the width of the NavigationRail for tablets.
     */
    val NavigationRailWidth = 80.dp

    /**
     * Returns adaptive spacing for form elements.
     */
    @Composable
    fun formSpacing(): Dp {
        val configuration = LocalConfiguration.current
        return when {
            configuration.screenWidthDp >= 600 -> 20.dp
            else -> 16.dp
        }
    }

    /**
     * Returns adaptive bottom sheet max height fraction.
     * Tablets don't need full-height sheets.
     */
    @Composable
    fun bottomSheetMaxHeightFraction(): Float {
        val configuration = LocalConfiguration.current
        return when {
            configuration.screenWidthDp >= 600 -> 0.6f
            else -> 0.9f
        }
    }
}

/**
 * Extension to check if width size class is expanded (tablet).
 */
fun WindowSizeClass.isExpandedWidth(): Boolean {
    return widthSizeClass == WindowWidthSizeClass.Expanded
}

/**
 * Extension to check if width size class is medium (large phone/small tablet).
 */
fun WindowSizeClass.isMediumWidth(): Boolean {
    return widthSizeClass == WindowWidthSizeClass.Medium
}

/**
 * Extension to check if width size class is compact (phone).
 */
fun WindowSizeClass.isCompactWidth(): Boolean {
    return widthSizeClass == WindowWidthSizeClass.Compact
}
