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
import com.petsafety.app.ui.theme.BackgroundLight
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.MutedTextLight
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.viewmodel.AlertsViewModel
import com.petsafety.app.ui.viewmodel.AppStateViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MissingAlertsScreen(
    viewModel: AlertsViewModel,
    appStateViewModel: AppStateViewModel
) {
    val alerts by viewModel.missingAlerts.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    var showMap by remember { mutableStateOf(false) }
    var selectedAlert by remember { mutableStateOf<MissingPetAlert?>(null) }
    var showReport by remember { mutableStateOf(false) }

    val sightingReportedMessage = stringResource(R.string.sighting_reported)
    val reportFailedMessage = stringResource(R.string.report_failed)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
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
                                text = "Loading alerts...",
                                color = MutedTextLight
                            )
                        }
                    }
                }
                alerts.isEmpty() -> {
                    EmptyAlertsState(
                        title = "No Active Alerts",
                        message = "You don't have any missing pet alerts at the moment"
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
    var showMap by remember { mutableStateOf(false) }
    var selectedAlert by remember { mutableStateOf<MissingPetAlert?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
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
                            CircularProgressIndicator(color = Color(0xFF34C759))
                            Text(
                                text = "Loading alerts...",
                                color = MutedTextLight
                            )
                        }
                    }
                }
                alerts.isEmpty() -> {
                    EmptyAlertsState(
                        title = "No Found Alerts",
                        message = "No pets have been found recently in your area"
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
                    .background(Color(0xFFF2F2F7), CircleShape),
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
                    color = MutedTextLight,
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
        "active" -> Color.Red
        "found" -> Color(0xFF34C759)
        else -> Color.Gray
    }

    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
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
                            contentDescription = null,
                            modifier = Modifier.size(28.dp),
                            tint = Color.White
                        )
                    }
                } ?: Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    modifier = Modifier.size(28.dp),
                    tint = Color.White
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
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MutedTextLight
                        )
                        Text(
                            text = location,
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedTextLight,
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
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color.Blue
                        )
                        Text(
                            text = "${alert.sightings.size} sighting${if (alert.sightings.size == 1) "" else "s"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.Blue
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
private fun AlertsMap(alerts: List<MissingPetAlert>) {
    val first = alerts.firstOrNull()
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(
            LatLng(
                first?.resolvedLatitude ?: 51.5074,
                first?.resolvedLongitude ?: -0.1278
            ),
            11f
        )
    }
    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState
    ) {
        alerts.forEach { alert ->
            val lat = alert.resolvedLatitude
            val lng = alert.resolvedLongitude
            if (lat != null && lng != null) {
                Marker(
                    state = MarkerState(position = LatLng(lat, lng)),
                    title = alert.pet?.name ?: "Alert",
                    snippet = alert.resolvedLastSeenLocation
                )
            }
        }
    }
}
