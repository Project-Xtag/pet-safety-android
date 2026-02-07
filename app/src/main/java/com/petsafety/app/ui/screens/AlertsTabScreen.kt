package com.petsafety.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.maps.model.LatLng
import com.petsafety.app.R
import com.petsafety.app.ui.components.OfflineIndicator
import com.petsafety.app.ui.viewmodel.AlertsViewModel
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel
import com.petsafety.app.ui.viewmodel.SuccessStoriesViewModel

@Composable
fun AlertsTabScreen(
    appStateViewModel: AppStateViewModel,
    authViewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val alertsViewModel: AlertsViewModel = hiltViewModel()
    val successStoriesViewModel: SuccessStoriesViewModel = hiltViewModel()
    var selectedTab by remember { mutableStateOf(0) }
    var hasLocationPermission by remember { mutableStateOf(false) }
    var userLocation by remember { mutableStateOf<LatLng?>(null) }
    val context = androidx.compose.ui.platform.LocalContext.current
    val locationProvider = LocationServices.getFusedLocationProviderClient(context)
    val currentUser by authViewModel.currentUser.collectAsState()

    // Helper function to fetch location and alerts
    fun fetchLocationAndAlerts() {
        @Suppress("MissingPermission")
        locationProvider.lastLocation.addOnSuccessListener { location ->
            if (location != null) {
                userLocation = LatLng(location.latitude, location.longitude)
                alertsViewModel.fetchNearbyAlerts(location.latitude, location.longitude, 10.0)
            } else {
                // lastLocation is null, request a fresh location update
                val locationRequest = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                    .setMinUpdateIntervalMillis(2000L)
                    .setMaxUpdates(1)
                    .build()

                val locationCallback = object : LocationCallback() {
                    override fun onLocationResult(result: LocationResult) {
                        result.lastLocation?.let { freshLocation ->
                            userLocation = LatLng(freshLocation.latitude, freshLocation.longitude)
                            alertsViewModel.fetchNearbyAlerts(freshLocation.latitude, freshLocation.longitude, 10.0)
                        } ?: run {
                            // Still no location, fallback to registered address
                            fallbackToRegisteredAddress()
                        }
                        locationProvider.removeLocationUpdates(this)
                    }
                }

                @Suppress("MissingPermission")
                locationProvider.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            }
        }.addOnFailureListener {
            // Location fetch failed, fallback to registered address
            fallbackToRegisteredAddress()
        }
    }

    // Helper function to fallback to user's registered address
    fun fallbackToRegisteredAddress() {
        val user = currentUser
        if (user != null) {
            val address = listOfNotNull(user.address, user.city, user.postalCode, user.country)
                .filter { it.isNotBlank() }
                .joinToString(", ")
            if (address.isNotBlank()) {
                @Suppress("DEPRECATION")
                try {
                    val geocoder = android.location.Geocoder(context)
                    val results = geocoder.getFromLocationName(address, 1)
                    results?.firstOrNull()?.let {
                        userLocation = LatLng(it.latitude, it.longitude)
                        alertsViewModel.fetchNearbyAlerts(it.latitude, it.longitude, 10.0)
                    }
                } catch (_: Exception) {
                    // Geocoding failed, no fallback available
                }
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
        if (granted) {
            fetchLocationAndAlerts()
        } else {
            // Permission denied, fallback to registered address
            fallbackToRegisteredAddress()
        }
    }

    LaunchedEffect(Unit) {
        // Check if permission is already granted
        val permissionStatus = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
        if (permissionStatus == PackageManager.PERMISSION_GRANTED) {
            hasLocationPermission = true
            fetchLocationAndAlerts()
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Column(modifier = modifier.fillMaxWidth().padding(12.dp)) {
        val isConnected by appStateViewModel.isConnected.collectAsState()
        OfflineIndicator(appStateViewModel.syncService, isConnected)

        TabRow(selectedTabIndex = selectedTab) {
            Tab(selected = selectedTab == 0, onClick = { selectedTab = 0 }) {
                Text(stringResource(R.string.tab_missing))
            }
            Tab(selected = selectedTab == 1, onClick = { selectedTab = 1 }) {
                Text(stringResource(R.string.tab_success))
            }
        }

        when (selectedTab) {
            0 -> MissingAlertsScreen(alertsViewModel, appStateViewModel, userLocation)
            1 -> SuccessStoriesScreen(
                viewModel = successStoriesViewModel,
                appStateViewModel = appStateViewModel,
                userLatitude = userLocation?.latitude,
                userLongitude = userLocation?.longitude
            )
        }
    }
}
