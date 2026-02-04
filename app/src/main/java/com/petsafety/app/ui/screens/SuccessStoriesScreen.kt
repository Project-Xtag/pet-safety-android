package com.petsafety.app.ui.screens

import android.content.res.Resources
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.petsafety.app.R
import com.petsafety.app.data.model.SuccessStory
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.SuccessStoriesViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SuccessStoriesScreen(
    viewModel: SuccessStoriesViewModel,
    appStateViewModel: AppStateViewModel
) {
    val stories by viewModel.stories.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showMap by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Toggle Button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.End
            ) {
                Button(
                    onClick = { showMap = !showMap },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = TealAccent.copy(alpha = 0.1f),
                        contentColor = TealAccent
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = if (showMap) stringResource(R.string.show_list) else stringResource(R.string.show_map),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Content
            when {
                isLoading && stories.isEmpty() -> {
                    // Loading State
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = TealAccent
                            )
                            Text(
                                text = stringResource(R.string.loading_success_stories),
                                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                stories.isEmpty() -> {
                    // Empty State
                    EmptySuccessStoriesState()
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.weight(1f)
                    ) {
                        if (showMap) {
                            SuccessStoriesMap(stories)
                        } else {
                            SuccessStoriesList(stories)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptySuccessStoriesState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Favorite,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = TealAccent
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.no_success_stories),
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(R.string.no_success_stories_message),
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

@Composable
private fun SuccessStoriesList(stories: List<SuccessStory>) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            horizontal = 16.dp,
            vertical = 8.dp
        )
    ) {
        items(stories) { story ->
            SuccessStoryCard(story = story)
        }
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
private fun SuccessStoryCard(story: SuccessStory) {
    val resources = LocalContext.current.resources
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pet Photo
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF34C759).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!story.petPhotoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = story.petPhotoUrl,
                            contentDescription = story.petName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Favorite,
                            contentDescription = null,
                            modifier = Modifier.size(32.dp),
                            tint = Color.White
                        )
                    }
                }

                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Pet Name
                    story.petName?.let {
                        Text(
                            text = it,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Found Badge
                    Row(
                        modifier = Modifier
                            .background(
                                Color(0xFF34C759).copy(alpha = 0.1f),
                                RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color(0xFF34C759)
                        )
                        Text(
                            text = stringResource(R.string.found_and_reunited),
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontWeight = FontWeight.SemiBold
                            ),
                            color = Color(0xFF34C759)
                        )
                    }

                    // Location
                    story.reunionCity?.let { city ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = city,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Distance
                    story.distanceKm?.let { distance ->
                        Text(
                            text = stringResource(R.string.distance_km, distance),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Share Button
                IconButton(
                    onClick = { /* Share action */ },
                    modifier = Modifier
                        .size(36.dp)
                        .background(Color.Blue.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = stringResource(R.string.share),
                        modifier = Modifier.size(18.dp),
                        tint = Color.Blue
                    )
                }
            }

            // Story Text
            story.storyText?.let { text ->
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Time Info
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                story.missingSince?.let { missingSince ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Schedule,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = stringResource(R.string.missing_since, formatDateString(missingSince, resources)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.found_on, formatDateString(story.foundAt, resources)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun SuccessStoriesMap(stories: List<SuccessStory>) {
    val first = stories.firstOrNull()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(
                first?.reunionLatitude ?: 51.5074,
                first?.reunionLongitude ?: -0.1278
            ),
            11f
        )
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        stories.forEach { story ->
            val lat = story.reunionLatitude
            val lng = story.reunionLongitude
            if (lat != null && lng != null) {
                Marker(
                    state = MarkerState(position = LatLng(lat, lng)),
                    title = story.petName ?: stringResource(R.string.success_story_marker),
                    snippet = story.reunionCity
                )
            }
        }
    }
}

private fun formatDateString(dateString: String, resources: Resources): String {
    return try {
        // Try parsing ISO format
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateString.take(19))
        if (date != null) {
            val now = Date()
            val diffMs = now.time - date.time
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
            val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs).toInt()
            val diffMinutes = TimeUnit.MILLISECONDS.toMinutes(diffMs).toInt()

            when {
                diffDays > 0 -> resources.getQuantityString(R.plurals.time_ago_days, diffDays, diffDays)
                diffHours > 0 -> resources.getQuantityString(R.plurals.time_ago_hours, diffHours, diffHours)
                diffMinutes > 0 -> resources.getQuantityString(R.plurals.time_ago_minutes, diffMinutes, diffMinutes)
                else -> resources.getString(R.string.time_ago_just_now)
            }
        } else {
            dateString.take(10)
        }
    } catch (e: Exception) {
        dateString.take(10)
    }
}
