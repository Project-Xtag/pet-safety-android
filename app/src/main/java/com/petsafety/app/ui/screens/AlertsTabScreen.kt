package com.petsafety.app.ui.screens

import android.Manifest
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
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.location.LocationServices
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
    val context = androidx.compose.ui.platform.LocalContext.current
    val locationProvider = LocationServices.getFusedLocationProviderClient(context)

    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
        if (granted) {
            locationProvider.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    alertsViewModel.fetchNearbyAlerts(location.latitude, location.longitude, 10.0)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
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
            0 -> MissingAlertsScreen(alertsViewModel, appStateViewModel)
            1 -> SuccessStoriesScreen(successStoriesViewModel, appStateViewModel)
        }
    }
}
