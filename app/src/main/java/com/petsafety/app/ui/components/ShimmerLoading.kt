package com.petsafety.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme

/**
 * Creates a shimmer effect brush for loading placeholders.
 */
@Composable
fun shimmerBrush(
    targetValue: Float = 1000f,
    showShimmer: Boolean = true,
    baseColor: Color = Color(0xFFE0E0E0),
    highlightColor: Color = Color(0xFFF5F5F5)
): Brush {
    return if (showShimmer) {
        val transition = rememberInfiniteTransition(label = "shimmer")
        val translateAnimation = transition.animateFloat(
            initialValue = 0f,
            targetValue = targetValue,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "shimmer_translate"
        )

        Brush.linearGradient(
            colors = listOf(baseColor, highlightColor, baseColor),
            start = Offset(translateAnimation.value - 200f, 0f),
            end = Offset(translateAnimation.value, 0f)
        )
    } else {
        Brush.linearGradient(
            colors = listOf(baseColor, baseColor),
            start = Offset.Zero,
            end = Offset.Zero
        )
    }
}

/**
 * A basic shimmer box placeholder.
 */
@Composable
fun ShimmerBox(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(8.dp)
) {
    Box(
        modifier = modifier
            .clip(shape)
            .background(shimmerBrush())
    )
}

/**
 * Shimmer skeleton for a pet card in grid view.
 */
@Composable
fun PetCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp)
    ) {
        // Pet image placeholder
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Pet name
        ShimmerBox(
            modifier = Modifier
                .width(100.dp)
                .height(16.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Species/breed
        ShimmerBox(
            modifier = Modifier
                .width(80.dp)
                .height(12.dp)
        )
    }
}

/**
 * Loading skeleton grid for the pets list screen.
 */
@Composable
fun PetsListSkeleton(
    columns: Int = 2,
    itemCount: Int = 4
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            PetCardSkeleton(
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/**
 * Shimmer skeleton for an alert card in list view.
 */
@Composable
fun AlertCardSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pet image placeholder
        ShimmerBox(
            modifier = Modifier.size(60.dp),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Pet name
            ShimmerBox(
                modifier = Modifier
                    .width(120.dp)
                    .height(16.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Location
            ShimmerBox(
                modifier = Modifier
                    .width(180.dp)
                    .height(12.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Status
            ShimmerBox(
                modifier = Modifier
                    .width(80.dp)
                    .height(12.dp)
            )
        }
    }
}

/**
 * Loading skeleton list for the alerts screen.
 */
@Composable
fun AlertsListSkeleton(itemCount: Int = 5) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            AlertCardSkeleton()
        }
    }
}

/**
 * Shimmer skeleton for an order row.
 */
@Composable
fun OrderRowSkeleton(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Order icon placeholder
        ShimmerBox(
            modifier = Modifier.size(40.dp),
            shape = RoundedCornerShape(8.dp)
        )

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            // Order number
            ShimmerBox(
                modifier = Modifier
                    .width(100.dp)
                    .height(14.dp)
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Order total
            ShimmerBox(
                modifier = Modifier
                    .width(70.dp)
                    .height(12.dp)
            )
        }

        // Status badge
        ShimmerBox(
            modifier = Modifier
                .width(60.dp)
                .height(24.dp),
            shape = RoundedCornerShape(12.dp)
        )
    }
}

/**
 * Loading skeleton list for orders.
 */
@Composable
fun OrdersListSkeleton(itemCount: Int = 3) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(itemCount) {
            OrderRowSkeleton()
        }
    }
}

/**
 * Shimmer skeleton for a success story card.
 */
@Composable
fun SuccessStoryCardSkeleton(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // User avatar
            ShimmerBox(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(20.dp)
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column {
                // User name
                ShimmerBox(
                    modifier = Modifier
                        .width(100.dp)
                        .height(14.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                // Date
                ShimmerBox(
                    modifier = Modifier
                        .width(80.dp)
                        .height(12.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Story image
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            shape = RoundedCornerShape(12.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Story text lines
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        ShimmerBox(
            modifier = Modifier
                .width(200.dp)
                .height(14.dp)
        )
    }
}

/**
 * Loading skeleton for success stories list.
 */
@Composable
fun SuccessStoriesListSkeleton(itemCount: Int = 3) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(itemCount) {
            SuccessStoryCardSkeleton()
        }
    }
}

/**
 * Pet detail screen skeleton.
 */
@Composable
fun PetDetailSkeleton() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Main image
        ShimmerBox(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp),
            shape = RoundedCornerShape(16.dp)
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Pet name
        ShimmerBox(
            modifier = Modifier
                .width(150.dp)
                .height(28.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Species/breed
        ShimmerBox(
            modifier = Modifier
                .width(120.dp)
                .height(16.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Info cards
        repeat(3) {
            ShimmerBox(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp),
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
