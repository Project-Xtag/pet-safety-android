package com.petsafety.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapShader
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Shader
import android.graphics.drawable.BitmapDrawable
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.rememberCameraPositionState
import com.petsafety.app.R
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.MissingPetAlert
import com.petsafety.app.ui.components.ErrorRetryState
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.SuccessGreen
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.viewmodel.AlertsViewModel
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel
import android.content.res.Resources
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import com.petsafety.app.data.repository.LocationConsent
import com.petsafety.app.ui.viewmodel.QrScannerViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingAlertsScreen(
    viewModel: AlertsViewModel,
    appStateViewModel: AppStateViewModel,
    authViewModel: AuthViewModel,
    qrScannerViewModel: QrScannerViewModel,
    userLocation: LatLng? = null
) {
    val alerts by viewModel.missingAlerts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val currentUser by authViewModel.currentUser.collectAsState()
    var showMap by remember { mutableStateOf(false) }
    var selectedAlert by remember { mutableStateOf<MissingPetAlert?>(null) }
    var mapDetailAlert by remember { mutableStateOf<MissingPetAlert?>(null) }
    var showReport by remember { mutableStateOf(false) }

    val sightingReportedMessage = stringResource(R.string.sighting_reported)
    val reportFailedMessage = stringResource(R.string.report_failed)

    // Full detail screen when a map marker is tapped
    mapDetailAlert?.let { alert ->
        AlertDetailScreen(
            alert = alert,
            currentUserId = currentUser?.id,
            viewModel = viewModel,
            qrScannerViewModel = qrScannerViewModel,
            appStateViewModel = appStateViewModel,
            onBack = { mapDetailAlert = null }
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Toggle Button — always visible, even when alerts are empty
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

            when {
                isLoading && alerts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = BrandOrange)
                            Text(
                                text = stringResource(R.string.loading_alerts),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                error != null && alerts.isEmpty() -> {
                    ErrorRetryState(
                        message = error ?: stringResource(R.string.failed_load_alerts),
                        onRetry = { viewModel.refresh() }
                    )
                }
                alerts.isEmpty() -> {
                    if (showMap) {
                        AlertsMap(alerts = emptyList(), userLocation = userLocation, onAlertSelected = {})
                    } else {
                        EmptyAlertsState(
                            title = stringResource(R.string.no_active_alerts),
                            message = stringResource(R.string.no_active_alerts_message)
                        )
                    }
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.weight(1f)
                    ) {
                        if (showMap) {
                            AlertsMap(alerts = alerts, userLocation = userLocation, onAlertSelected = { mapDetailAlert = it })
                        } else {
                            AlertsList(alerts) { selectedAlert = it }
                        }
                    }
                }
            }
        }
    }

    // List view dialog (unchanged)
    selectedAlert?.let { alert ->
        AlertDetailDialog(
            alert = alert,
            onDismiss = { selectedAlert = null },
            onReportSighting = { showReport = true }
        )
        if (showReport) {
            ReportSightingDialog(
                alertId = alert.id,
                onDismiss = { showReport = false },
                onSubmit = { coordinate, locationText, notes, name, phone, email ->
                    viewModel.reportSighting(
                        alertId = alert.id,
                        reporterName = name,
                        reporterPhone = phone,
                        reporterEmail = email,
                        location = locationText,
                        coordinate = coordinate,
                        notes = notes
                    ) { success, message ->
                        if (success) appStateViewModel.showSuccess(sightingReportedMessage)
                        else appStateViewModel.showError(message ?: reportFailedMessage)
                        showReport = false
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoundAlertsScreen(
    viewModel: AlertsViewModel,
    appStateViewModel: AppStateViewModel
) {
    val alerts by viewModel.foundAlerts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    var showMap by remember { mutableStateOf(false) }
    var selectedAlert by remember { mutableStateOf<MissingPetAlert?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
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

            when {
                isLoading && alerts.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = SuccessGreen)
                            Text(
                                text = stringResource(R.string.loading_alerts),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                error != null && alerts.isEmpty() -> {
                    ErrorRetryState(
                        message = error ?: stringResource(R.string.failed_load_alerts),
                        onRetry = { viewModel.refresh() }
                    )
                }
                alerts.isEmpty() -> {
                    EmptyAlertsState(
                        title = stringResource(R.string.no_found_alerts),
                        message = stringResource(R.string.no_found_alerts_message)
                    )
                }
                else -> {
                    PullToRefreshBox(
                        isRefreshing = isRefreshing,
                        onRefresh = { viewModel.refresh() },
                        modifier = Modifier.weight(1f)
                    ) {
                        if (showMap) {
                            AlertsMap(alerts = alerts, onAlertSelected = { selectedAlert = it })
                        } else {
                            AlertsList(alerts) { selectedAlert = it }
                        }
                    }
                }
            }
        }
    }

    selectedAlert?.let { alert ->
        AlertDetailDialog(alert = alert, onDismiss = { selectedAlert = null }, onReportSighting = null)
    }
}

@Composable
private fun EmptyAlertsState(title: String, message: String) {
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
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = BrandOrange
                )
            }

            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 32.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertsList(alerts: List<MissingPetAlert>, onSelect: (MissingPetAlert) -> Unit) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        items(alerts) { alert ->
            AlertCard(alert = alert, onClick = { onSelect(alert) })
        }
        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertCard(
    alert: MissingPetAlert,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Top section: Photo + Info + Chevron
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.Top
            ) {
                // Pet Photo — 80dp to match iOS
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.Red.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (!alert.resolvedPetPhoto.isNullOrBlank()) {
                        AsyncImage(
                            model = alert.resolvedPetPhoto,
                            contentDescription = alert.resolvedPetName,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Pets,
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
                    Text(
                        text = alert.resolvedPetName ?: stringResource(R.string.unknown_pet),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Species + Breed
                    val speciesBreed = buildList {
                        alert.resolvedSpecies?.let { add(it) }
                        alert.breed?.let { add(it) }
                    }.joinToString(" \u2022 ")
                    if (speciesBreed.isNotBlank()) {
                        Text(
                            text = speciesBreed,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    // Missing Badge + Reward Badge
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            modifier = Modifier
                                .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                modifier = Modifier.size(12.dp),
                                tint = Color.Red
                            )
                            Text(
                                text = stringResource(R.string.alert_status_missing),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = Color.Red
                            )
                        }

                        if (alert.rewardAmount != null && alert.rewardAmount > 0) {
                            Text(
                                text = stringResource(R.string.reward_badge, alert.rewardAmount.toInt()),
                                modifier = Modifier
                                    .background(BrandOrange.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                color = BrandOrange
                            )
                        }
                    }

                    // Missing Since
                    alert.createdAt?.let { dateStr ->
                        val formattedDate = formatDateAbbreviated(dateStr)
                        if (formattedDate.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.alert_missing_since, formattedDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Location
                    alert.resolvedLastSeenLocation?.let { location ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = location,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Distance
                    alert.distanceKm?.let { distance ->
                        Text(
                            text = stringResource(R.string.distance_km, distance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Chevron
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Additional info / description (truncated)
            alert.additionalInfo?.let { info ->
                if (info.isNotBlank()) {
                    Text(
                        text = info,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Sightings count
            if (!alert.sightings.isNullOrEmpty()) {
                Text(
                    text = pluralStringResource(R.plurals.sighting_count, alert.sightings.size, alert.sightings.size),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = BrandOrange
                )
            }
        }
    }
}

@Composable
private fun AlertDetailDialog(
    alert: MissingPetAlert,
    onDismiss: () -> Unit,
    onReportSighting: (() -> Unit)?
) {
    val resources = LocalContext.current.resources
    AlertDialog(
        onDismissRequest = onDismiss,
        title = null,
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                // Photo + Name + Badge header
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.Red.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (!alert.resolvedPetPhoto.isNullOrBlank()) {
                            AsyncImage(
                                model = alert.resolvedPetPhoto,
                                contentDescription = alert.resolvedPetName,
                                modifier = Modifier.fillMaxSize(),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Pets,
                                contentDescription = null,
                                modifier = Modifier.size(36.dp),
                                tint = Color.White
                            )
                        }
                    }

                    Column(
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = alert.resolvedPetName ?: stringResource(R.string.unknown_pet),
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )

                        // Species + Breed
                        val speciesBreed = buildList {
                            alert.resolvedSpecies?.let { add(it) }
                            alert.breed?.let { add(it) }
                        }.joinToString(" \u2022 ")
                        if (speciesBreed.isNotBlank()) {
                            Text(
                                text = speciesBreed,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // Missing Badge + Reward Badge
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                modifier = Modifier
                                    .background(Color.Red.copy(alpha = 0.1f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = null,
                                    modifier = Modifier.size(12.dp),
                                    tint = Color.Red
                                )
                                Text(
                                    text = stringResource(R.string.alert_status_missing),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = Color.Red
                                )
                            }

                            if (alert.rewardAmount != null && alert.rewardAmount > 0) {
                                Text(
                                    text = stringResource(R.string.reward_badge, alert.rewardAmount.toInt()),
                                    modifier = Modifier
                                        .background(BrandOrange.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp),
                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                                    color = BrandOrange
                                )
                            }
                        }
                    }
                }

                // Missing Duration (prominent red text)
                alert.createdAt?.let { dateStr ->
                    val duration = computeMissingDuration(dateStr, resources)
                    Text(
                        text = stringResource(R.string.alert_missing_for, duration),
                        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = Color.Red
                    )
                }

                // Missing since date
                alert.createdAt?.let { dateStr ->
                    val formattedDate = formatDateAbbreviated(dateStr)
                    if (formattedDate.isNotBlank()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                modifier = Modifier.size(14.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = stringResource(R.string.alert_missing_since, formattedDate),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Location
                alert.resolvedLastSeenLocation?.let { location ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Distance
                alert.distanceKm?.let { distance ->
                    Text(
                        text = stringResource(R.string.distance_km, distance),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Description / Additional Info
                alert.additionalInfo?.let { info ->
                    if (info.isNotBlank()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = stringResource(R.string.details),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = info,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Sightings count
                if (!alert.sightings.isNullOrEmpty()) {
                    Text(
                        text = pluralStringResource(R.plurals.sighting_count, alert.sightings.size, alert.sightings.size),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                        color = BrandOrange
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onReportSighting != null && alert.status == "active") {
                    Button(
                        onClick = onReportSighting,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(stringResource(R.string.report_sighting), fontWeight = FontWeight.SemiBold)
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.close))
                }
            }
        }
    )
}

@SuppressLint("MissingPermission")
@Composable
private fun ReportSightingDialog(
    alertId: String,
    onDismiss: () -> Unit,
    onSubmit: (LocationCoordinate?, String?, String?, String?, String?, String?) -> Unit
) {
    val context = LocalContext.current
    val locationProvider = remember { LocationServices.getFusedLocationProviderClient(context) }

    var locationText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var capturedCoordinate by remember { mutableStateOf<LocationCoordinate?>(null) }
    var isCapturingLocation by remember { mutableStateOf(false) }
    var shareContact by remember { mutableStateOf(false) }
    var reporterName by remember { mutableStateOf("") }
    var reporterPhone by remember { mutableStateOf("") }
    var reporterEmail by remember { mutableStateOf("") }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            isCapturingLocation = true
            locationProvider.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    capturedCoordinate = LocationCoordinate(location.latitude, location.longitude)
                    locationText = "%.5f, %.5f".format(location.latitude, location.longitude)
                }
                isCapturingLocation = false
            }.addOnFailureListener {
                isCapturingLocation = false
            }
        }
    }

    fun captureLocation() {
        if (hasLocationPermission) {
            isCapturingLocation = true
            locationProvider.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    capturedCoordinate = LocationCoordinate(location.latitude, location.longitude)
                    locationText = "%.5f, %.5f".format(location.latitude, location.longitude)
                }
                isCapturingLocation = false
            }.addOnFailureListener {
                isCapturingLocation = false
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_sighting)) },
        text = {
            Column {
                Text(stringResource(R.string.report_sighting_prompt))
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedButton(
                    onClick = { captureLocation() },
                    enabled = !isCapturingLocation,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (isCapturingLocation) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.MyLocation,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.use_current_location))
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = locationText,
                    onValueChange = {
                        locationText = it
                        if (capturedCoordinate != null) capturedCoordinate = null
                    },
                    label = { Text(stringResource(R.string.sighting_location)) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = if (capturedCoordinate != null) {
                        { Text(stringResource(R.string.location_captured)) }
                    } else null
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.notes)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))

                // Share contact info toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.share_contact_info),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.share_contact_info_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = shareContact,
                        onCheckedChange = { shareContact = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = TealAccent,
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = MaterialTheme.colorScheme.outline
                        )
                    )
                }

                if (shareContact) {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reporterName,
                        onValueChange = { reporterName = it },
                        label = { Text(stringResource(R.string.reporter_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reporterPhone,
                        onValueChange = { reporterPhone = it },
                        label = { Text(stringResource(R.string.reporter_phone)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = reporterEmail,
                        onValueChange = { reporterEmail = it },
                        label = { Text(stringResource(R.string.reporter_email)) },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        capturedCoordinate,
                        locationText.ifBlank { null },
                        notes.ifBlank { null },
                        if (shareContact) reporterName.ifBlank { null } else null,
                        if (shareContact) reporterPhone.ifBlank { null } else null,
                        if (shareContact) reporterEmail.ifBlank { null } else null
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                Text(stringResource(R.string.submit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AlertsMap(
    alerts: List<MissingPetAlert>,
    userLocation: LatLng? = null,
    onAlertSelected: (MissingPetAlert) -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val markerSizePx = with(density) { 48.dp.roundToPx() }

    // Load circular photo markers for each alert
    val markerIcons = remember { mutableStateMapOf<String, BitmapDescriptor>() }
    LaunchedEffect(alerts) {
        alerts.forEach { alert ->
            val photoUrl = alert.resolvedPetPhoto
            if (photoUrl.isNullOrBlank()) return@forEach
            if (markerIcons.containsKey(alert.id)) return@forEach
            loadCircularAlertMarkerBitmap(context, photoUrl, markerSizePx)?.let {
                markerIcons[alert.id] = it
            }
        }
    }

    val first = alerts.firstOrNull()
    // Center on first alert, fallback to user location, fallback to London
    val centerLat = first?.resolvedLatitude ?: userLocation?.latitude ?: 51.5074
    val centerLng = first?.resolvedLongitude ?: userLocation?.longitude ?: -0.1278
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(centerLat, centerLng), 11f)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState
        ) {
            // User location marker (blue dot)
            userLocation?.let { loc ->
                com.google.maps.android.compose.Circle(
                    center = loc,
                    radius = 50.0,
                    fillColor = Color(0x400000FF),
                    strokeColor = Color(0xFF0000FF),
                    strokeWidth = 2f
                )
            }

            // Alert markers with pet photo icons
            alerts.forEach { alert ->
                val lat = alert.resolvedLatitude
                val lng = alert.resolvedLongitude
                if (lat != null && lng != null) {
                    Marker(
                        state = MarkerState(position = LatLng(lat, lng)),
                        title = alert.resolvedPetName ?: stringResource(R.string.alert_marker),
                        snippet = alert.resolvedLastSeenLocation,
                        icon = markerIcons[alert.id]
                            ?: BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                        onClick = {
                            onAlertSelected(alert)
                            true
                        }
                    )
                }
            }
        }

        // "No alerts nearby" overlay when empty
        if (alerts.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .align(Alignment.TopCenter)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.no_active_alerts_message),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun AlertDetailScreen(
    alert: MissingPetAlert,
    currentUserId: String?,
    viewModel: AlertsViewModel,
    qrScannerViewModel: QrScannerViewModel,
    appStateViewModel: AppStateViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val resources = context.resources
    val isOwner = currentUserId != null && alert.userId == currentUserId
    var showReportSighting by remember { mutableStateOf(false) }
    var showReportFound by remember { mutableStateOf(false) }
    var showMarkFoundConfirmation by remember { mutableStateOf(false) }
    var reverseGeocodedAddress by remember { mutableStateOf<String?>(null) }

    // Reverse-geocode the pin coordinates to get the actual address
    LaunchedEffect(alert.resolvedLatitude, alert.resolvedLongitude) {
        val lat = alert.resolvedLatitude
        val lng = alert.resolvedLongitude
        if (lat != null && lng != null) {
            @Suppress("DEPRECATION")
            try {
                val geocoder = android.location.Geocoder(context)
                val results = geocoder.getFromLocation(lat, lng, 1)
                results?.firstOrNull()?.let { addr ->
                    val parts = listOfNotNull(
                        addr.thoroughfare,
                        addr.locality,
                        addr.adminArea,
                        addr.countryName
                    ).filter { it.isNotBlank() }
                    if (parts.isNotEmpty()) {
                        reverseGeocodedAddress = parts.joinToString(", ")
                    }
                }
            } catch (_: Exception) {
                // Reverse geocoding failed, fall back to stored address
            }
        }
    }

    val sightingReportedMessage = stringResource(R.string.sighting_reported)
    val reportFailedMessage = stringResource(R.string.report_failed)
    val markedFoundMessage = stringResource(R.string.mark_as_found)
    val ownerNotifiedMessage = stringResource(R.string.share_owner_notified)
    val shareLocationFailedMessage = stringResource(R.string.share_location_failed)
    val petName = alert.resolvedPetName ?: stringResource(R.string.unknown_pet)
    val resolvedQrCode = alert.qrCode ?: alert.pet?.qrCode

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
    ) {
        // Back button overlay on photo
        Box(modifier = Modifier.fillMaxWidth()) {
            // Pet photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .background(Color.Red.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                if (!alert.resolvedPetPhoto.isNullOrBlank()) {
                    AsyncImage(
                        model = alert.resolvedPetPhoto,
                        contentDescription = petName,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(80.dp),
                        tint = Color.Red.copy(alpha = 0.3f)
                    )
                }
            }

            // Back button
            IconButton(
                onClick = onBack,
                modifier = Modifier
                    .padding(8.dp)
                    .align(Alignment.TopStart)
                    .background(
                        MaterialTheme.colorScheme.surface.copy(alpha = 0.8f),
                        CircleShape
                    )
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = stringResource(R.string.back),
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pet Name + Reward Badge
            Text(
                text = petName,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface
            )

            // Reward badge
            if (alert.rewardAmount != null && alert.rewardAmount > 0) {
                Text(
                    text = stringResource(R.string.reward_badge, alert.rewardAmount.toInt()),
                    modifier = Modifier
                        .background(BrandOrange.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = BrandOrange
                )
            }

            // Species, breed, age, color — info chips
            val infoItems = buildList {
                alert.resolvedSpecies?.let { add(it) }
                alert.breed?.let { add(it) }
                alert.pet?.age?.let { add(it) }
                (alert.color ?: alert.pet?.color)?.let { add(it) }
            }
            if (infoItems.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    infoItems.forEach { info ->
                        Text(
                            text = info,
                            modifier = Modifier
                                .background(
                                    MaterialTheme.colorScheme.surfaceContainerHigh,
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }

            // Status: Active — Missing since [date]
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Red.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = Color.Red
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    alert.createdAt?.let { dateStr ->
                        val duration = computeMissingDuration(dateStr, resources)
                        Text(
                            text = stringResource(R.string.alert_missing_for, duration),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = Color.Red
                        )
                    }
                    alert.createdAt?.let { dateStr ->
                        val formattedDate = formatDateAbbreviated(dateStr)
                        if (formattedDate.isNotBlank()) {
                            Text(
                                text = stringResource(R.string.alert_missing_since, formattedDate),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Last seen location section
            val lat = alert.resolvedLatitude
            val lng = alert.resolvedLongitude
            if (lat != null && lng != null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = stringResource(R.string.last_seen_location_header),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )

                    // Embedded map (display only)
                    val mapCameraState = rememberCameraPositionState {
                        position = CameraPosition.fromLatLngZoom(LatLng(lat, lng), 15f)
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(200.dp)
                            .clip(RoundedCornerShape(12.dp))
                    ) {
                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = mapCameraState,
                            uiSettings = MapUiSettings(
                                zoomControlsEnabled = false,
                                scrollGesturesEnabled = false,
                                zoomGesturesEnabled = false,
                                tiltGesturesEnabled = false,
                                rotationGesturesEnabled = false,
                                mapToolbarEnabled = false
                            )
                        ) {
                            Marker(
                                state = MarkerState(position = LatLng(lat, lng)),
                                icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED)
                            )
                        }
                    }

                    // Address text (prefer reverse-geocoded address from coordinates)
                    val displayAddress = reverseGeocodedAddress ?: alert.resolvedLastSeenLocation
                    displayAddress?.let { address ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = address,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    // Distance
                    alert.distanceKm?.let { distance ->
                        Text(
                            text = stringResource(R.string.distance_km, distance),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Additional info
            alert.additionalInfo?.let { info ->
                if (info.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.details),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = info,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Sightings details section
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = stringResource(R.string.sighting_details_title),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (alert.sightings.isNullOrEmpty()) {
                    Text(
                        text = stringResource(R.string.sighting_no_details),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    alert.sightings.forEach { sighting ->
                        SightingCard(sighting = sighting)
                    }
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

            // Action buttons
            if (isOwner) {
                Button(
                    onClick = { showMarkFoundConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.mark_as_found),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }
            } else {
                Button(
                    onClick = { showReportSighting = true },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.report_sighting),
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // Report Found button — only if pet has a QR code
                if (resolvedQrCode != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Button(
                        onClick = { showReportFound = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = stringResource(R.string.report_found),
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    if (showReportSighting) {
        ReportSightingDialog(
            alertId = alert.id,
            onDismiss = { showReportSighting = false },
            onSubmit = { coordinate, locationText, notes, name, phone, email ->
                viewModel.reportSighting(
                    alertId = alert.id,
                    reporterName = name,
                    reporterPhone = phone,
                    reporterEmail = email,
                    location = locationText,
                    coordinate = coordinate,
                    notes = notes
                ) { success, message ->
                    if (success) appStateViewModel.showSuccess(sightingReportedMessage)
                    else appStateViewModel.showError(message ?: reportFailedMessage)
                    showReportSighting = false
                }
            }
        )
    }

    if (showReportFound && resolvedQrCode != null) {
        ReportFoundDialog(
            petName = petName,
            qrCode = resolvedQrCode,
            qrScannerViewModel = qrScannerViewModel,
            onDismiss = { showReportFound = false },
            onSuccess = {
                appStateViewModel.showSuccess(ownerNotifiedMessage)
                showReportFound = false
            },
            onError = { message ->
                appStateViewModel.showError(message ?: shareLocationFailedMessage)
                showReportFound = false
            }
        )
    }

    if (showMarkFoundConfirmation) {
        AlertDialog(
            onDismissRequest = { showMarkFoundConfirmation = false },
            title = { Text(stringResource(R.string.mark_found_confirm_title, petName)) },
            text = { Text(stringResource(R.string.mark_found_confirm_message, petName)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateAlertStatus(alert.id, "found") { success, message ->
                            if (success) {
                                appStateViewModel.showSuccess(markedFoundMessage)
                                viewModel.refresh()
                                onBack()
                            } else {
                                appStateViewModel.showError(message ?: reportFailedMessage)
                            }
                        }
                        showMarkFoundConfirmation = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
                ) {
                    Text(stringResource(R.string.confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = { showMarkFoundConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun ReportFoundDialog(
    petName: String,
    qrCode: String,
    qrScannerViewModel: QrScannerViewModel,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onError: (String?) -> Unit
) {
    val context = LocalContext.current
    val locationProvider = remember { LocationServices.getFusedLocationProviderClient(context) }

    var shareExactLocation by remember { mutableStateOf(true) }
    var isSubmitting by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }
    var currentLocation by remember { mutableStateOf<android.location.Location?>(null) }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            locationProvider.lastLocation.addOnSuccessListener { location ->
                currentLocation = location
            }
        }
    }

    // Request location permission and fetch location on open
    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            locationProvider.lastLocation.addOnSuccessListener { location ->
                currentLocation = location
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Text(
                text = stringResource(R.string.report_found),
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = stringResource(R.string.share_location_help, petName),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Exact location toggle
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                    )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(24.dp),
                            tint = if (shareExactLocation) TealAccent else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.share_exact_location_toggle),
                                style = MaterialTheme.typography.bodyLarge.copy(
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp
                                ),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (shareExactLocation)
                                    stringResource(R.string.precise_location_desc)
                                else
                                    stringResource(R.string.approximate_location_desc),
                                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = shareExactLocation,
                            onCheckedChange = { shareExactLocation = it },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = TealAccent,
                                uncheckedThumbColor = Color.White,
                                uncheckedTrackColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    isSubmitting = true
                    val consent = if (shareExactLocation)
                        LocationConsent.PRECISE
                    else
                        LocationConsent.APPROXIMATE

                    val loc = currentLocation
                    if (loc != null) {
                        qrScannerViewModel.shareLocation(
                            qrCode, consent,
                            loc.latitude, loc.longitude, loc.accuracy.toDouble(),
                            onResult = { success, message ->
                                if (success) onSuccess() else onError(message)
                            }
                        )
                    } else {
                        // Try to fetch location one more time
                        locationProvider.lastLocation.addOnSuccessListener { location ->
                            if (location != null) {
                                qrScannerViewModel.shareLocation(
                                    qrCode, consent,
                                    location.latitude, location.longitude, location.accuracy.toDouble(),
                                    onResult = { success, message ->
                                        if (success) onSuccess() else onError(message)
                                    }
                                )
                            } else {
                                // Send with chosen consent but no coordinates
                                // — backend will handle gracefully
                                qrScannerViewModel.shareLocation(
                                    qrCode, consent,
                                    onResult = { success, message ->
                                        if (success) onSuccess() else onError(message)
                                    }
                                )
                            }
                        }.addOnFailureListener {
                            qrScannerViewModel.shareLocation(
                                qrCode, consent,
                                onResult = { success, message ->
                                    if (success) onSuccess() else onError(message)
                                }
                            )
                        }
                    }
                },
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent),
                shape = RoundedCornerShape(10.dp)
            ) {
                if (isSubmitting) {
                    Text(stringResource(R.string.sending), fontWeight = FontWeight.SemiBold)
                } else {
                    Text(stringResource(R.string.share_location), fontWeight = FontWeight.SemiBold)
                }
            }
        },
        dismissButton = {
            if (!isSubmitting) {
                TextButton(onClick = onDismiss) {
                    Text(stringResource(R.string.cancel))
                }
            }
        }
    )
}


@Composable
private fun SightingCard(sighting: com.petsafety.app.data.model.Sighting) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Location
            sighting.resolvedLocation?.let { location ->
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = BrandOrange
                    )
                    Text(
                        text = stringResource(R.string.sighting_at, location),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            // Notes
            sighting.resolvedNotes?.let { notes ->
                if (notes.isNotBlank()) {
                    Text(
                        text = notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Timestamp
            val formattedTime = formatDateAbbreviated(sighting.resolvedCreatedAt)
            if (formattedTime.isNotBlank()) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Schedule,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = stringResource(R.string.sighting_reported_at, formattedTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Reporter info
            val hasReporterInfo = !sighting.reporterName.isNullOrBlank() ||
                !sighting.reporterPhone.isNullOrBlank() ||
                !sighting.reporterEmail.isNullOrBlank()

            if (hasReporterInfo) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = TealAccent
                    )
                    val contactParts = buildList {
                        sighting.reporterName?.takeIf { it.isNotBlank() }?.let { add(it) }
                        sighting.reporterPhone?.takeIf { it.isNotBlank() }?.let { add(it) }
                        sighting.reporterEmail?.takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                    Text(
                        text = contactParts.joinToString(" \u2022 "),
                        style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                        color = TealAccent
                    )
                }
            } else {
                Text(
                    text = stringResource(R.string.sighting_anonymous),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }

            // Photo
            sighting.photoUrl?.takeIf { it.isNotBlank() }?.let { url ->
                AsyncImage(
                    model = url,
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            }
        }
    }
}

private suspend fun loadCircularAlertMarkerBitmap(
    context: android.content.Context,
    url: String,
    sizePx: Int
): BitmapDescriptor? {
    return try {
        val loader = ImageLoader(context)
        val request = ImageRequest.Builder(context)
            .data(url)
            .size(sizePx)
            .allowHardware(false)
            .build()
        val result = loader.execute(request)
        val bitmap = (result.drawable as? BitmapDrawable)?.bitmap ?: return null

        val scaled = Bitmap.createScaledBitmap(bitmap, sizePx, sizePx, true)
        val output = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(output)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // Red border circle (matching iOS missing pet marker)
        val borderWidth = sizePx * 0.05f
        paint.color = android.graphics.Color.parseColor("#FF0000")
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)

        // Circular pet photo
        paint.shader = BitmapShader(
            scaled,
            Shader.TileMode.CLAMP,
            Shader.TileMode.CLAMP
        )
        canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f - borderWidth, paint)

        BitmapDescriptorFactory.fromBitmap(output)
    } catch (_: Exception) {
        null
    }
}

private fun formatDateAbbreviated(dateString: String): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateString.take(19))
        if (date != null) {
            val outputFormat = SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
            outputFormat.format(date)
        } else {
            dateString.take(10)
        }
    } catch (e: Exception) {
        dateString.take(10)
    }
}

private fun computeMissingDuration(dateString: String, resources: Resources): String {
    return try {
        val inputFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.getDefault())
        val date = inputFormat.parse(dateString.take(19))
        if (date != null) {
            val diffMs = Date().time - date.time
            val diffDays = TimeUnit.MILLISECONDS.toDays(diffMs).toInt()
            val diffHours = TimeUnit.MILLISECONDS.toHours(diffMs).toInt()
            when {
                diffDays > 0 -> resources.getQuantityString(R.plurals.time_days_duration, diffDays, diffDays)
                diffHours > 0 -> resources.getQuantityString(R.plurals.time_hours_duration, maxOf(1, diffHours), maxOf(1, diffHours))
                else -> resources.getQuantityString(R.plurals.time_hours_duration, 1, 1)
            }
        } else {
            resources.getString(R.string.some_time)
        }
    } catch (e: Exception) {
        resources.getString(R.string.some_time)
    }
}
