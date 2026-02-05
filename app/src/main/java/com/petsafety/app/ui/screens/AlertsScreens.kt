package com.petsafety.app.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.petsafety.app.R
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.MissingPetAlert
import com.petsafety.app.ui.components.ErrorRetryState
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.InfoBlue
import com.petsafety.app.ui.theme.SuccessGreen
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.viewmodel.AlertsViewModel
import com.petsafety.app.ui.viewmodel.AppStateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingAlertsScreen(
    viewModel: AlertsViewModel,
    appStateViewModel: AppStateViewModel,
    userLocation: LatLng? = null
) {
    val alerts by viewModel.missingAlerts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    var showMap by remember { mutableStateOf(false) }
    var selectedAlert by remember { mutableStateOf<MissingPetAlert?>(null) }
    var showReport by remember { mutableStateOf(false) }

    val sightingReportedMessage = stringResource(R.string.sighting_reported)
    val reportFailedMessage = stringResource(R.string.report_failed)

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
                        // Show map centered on user location even with no alerts
                        AlertsMap(alerts = emptyList(), userLocation = userLocation)
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
                            AlertsMap(alerts = alerts, userLocation = userLocation)
                        } else {
                            AlertsList(alerts) { selectedAlert = it }
                        }
                    }
                }
            }
        }
    }

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
                onSubmit = { coordinate, locationText, notes ->
                    viewModel.reportSighting(
                        alertId = alert.id,
                        reporterName = null,
                        reporterPhone = null,
                        reporterEmail = null,
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
                            AlertsMap(alerts)
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
    val statusColor = when (alert.status) {
        "active" -> MaterialTheme.colorScheme.error
        "found" -> SuccessGreen
        else -> MaterialTheme.colorScheme.outline
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Pet Photo
            Box(
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                alert.pet?.let { pet ->
                    if (!pet.profileImage.isNullOrBlank()) {
                        AsyncImage(
                            model = pet.profileImage,
                            contentDescription = pet.name,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = stringResource(R.string.pet_photo),
                            modifier = Modifier.size(28.dp),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                } ?: Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = stringResource(R.string.pet_photo),
                    modifier = Modifier.size(28.dp),
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                // Pet Name
                Text(
                    text = alert.pet?.name ?: stringResource(R.string.unknown_pet),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                // Status Badge
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, CircleShape)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = alert.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.bodyMedium,
                        color = statusColor
                    )
                }

                // Location
                alert.resolvedLastSeenLocation?.let { location ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = stringResource(R.string.last_seen_location),
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

                // Sightings Count
                if (!alert.sightings.isNullOrEmpty()) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Visibility,
                            contentDescription = stringResource(R.string.sighting_reports_desc),
                            modifier = Modifier.size(14.dp),
                            tint = InfoBlue
                        )
                        Text(
                            text = pluralStringResource(R.plurals.sighting_count, alert.sightings.size, alert.sightings.size),
                            style = MaterialTheme.typography.bodySmall,
                            color = InfoBlue
                        )
                    }
                }
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
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(alert.pet?.name ?: stringResource(R.string.pet_alert)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.alert_status, alert.status))
                alert.resolvedLastSeenLocation?.let { Text(stringResource(R.string.alert_last_seen, it)) }
                alert.additionalInfo?.let { Text(stringResource(R.string.alert_info, it)) }
                if (!alert.sightings.isNullOrEmpty()) {
                    Text(stringResource(R.string.alert_sightings_count, alert.sightings.size))
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                if (onReportSighting != null && alert.status == "active") {
                    Button(
                        onClick = onReportSighting,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Text(stringResource(R.string.report_sighting))
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
    onSubmit: (LocationCoordinate?, String?, String?) -> Unit
) {
    val context = LocalContext.current
    val locationProvider = remember { LocationServices.getFusedLocationProviderClient(context) }

    var locationText by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var capturedCoordinate by remember { mutableStateOf<LocationCoordinate?>(null) }
    var isCapturingLocation by remember { mutableStateOf(false) }
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
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSubmit(
                        capturedCoordinate,
                        locationText.ifBlank { null },
                        notes.ifBlank { null }
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

@Composable
private fun AlertsMap(alerts: List<MissingPetAlert>, userLocation: LatLng? = null) {
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
                Marker(
                    state = MarkerState(position = loc),
                    title = stringResource(R.string.current_location),
                    alpha = 0.8f
                )
            }

            // Alert markers
            alerts.forEach { alert ->
                val lat = alert.resolvedLatitude
                val lng = alert.resolvedLongitude
                if (lat != null && lng != null) {
                    Marker(
                        state = MarkerState(position = LatLng(lat, lng)),
                        title = alert.pet?.name ?: stringResource(R.string.alert_marker),
                        snippet = alert.resolvedLastSeenLocation
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
