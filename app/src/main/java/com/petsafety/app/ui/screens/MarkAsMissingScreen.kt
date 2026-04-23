package com.petsafety.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.location.Geocoder
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import com.petsafety.app.R
import com.petsafety.app.util.InputValidators
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.Pet
import com.petsafety.app.ui.components.BrandButton
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.util.PetLocalizer
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel
import com.petsafety.app.ui.viewmodel.PetsViewModel
import com.petsafety.app.ui.viewmodel.SubscriptionViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.util.Locale
import kotlin.coroutines.resume

private enum class LocationSource { CURRENT, REGISTERED, CUSTOM }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkAsMissingScreen(
    pet: Pet,
    viewModel: PetsViewModel,
    authViewModel: AuthViewModel,
    appStateViewModel: AppStateViewModel,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val subscriptionViewModel: SubscriptionViewModel = hiltViewModel()
    val user by authViewModel.currentUser.collectAsState()
    val isOnStarterPlan by remember { derivedStateOf { subscriptionViewModel.isOnStarterPlan } }

    var locationSource by remember { mutableStateOf(LocationSource.REGISTERED) }
    var customAddress by remember { mutableStateOf("") }
    var additionalInfo by remember { mutableStateOf("") }
    var rewardAmount by remember { mutableStateOf("") }
    var isGeocoding by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var capturedLocation by remember { mutableStateOf<LocationCoordinate?>(null) }
    var capturedLocationAddress by remember { mutableStateOf<String?>(null) }
    var isReverseGeocodingCurrent by remember { mutableStateOf(false) }
    var isGettingLocation by remember { mutableStateOf(false) }

    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    // Build registered address from user profile
    val registeredAddress = remember(user) {
        val parts = listOfNotNull(user?.address, user?.addressLine2, user?.city, user?.postalCode, user?.country)
            .filter { it.isNotBlank() }
        parts.joinToString(", ").ifEmpty { null }
    }

    // Form validity
    val isFormValid = remember(locationSource, capturedLocation, registeredAddress, customAddress, isGeocoding, isSubmitting) {
        if (isGeocoding || isSubmitting) return@remember false
        when (locationSource) {
            LocationSource.CURRENT -> capturedLocation != null
            LocationSource.REGISTERED -> registeredAddress != null
            LocationSource.CUSTOM -> customAddress.isNotBlank()
        }
    }

    // Location permission
    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            isGettingLocation = true
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            capturedLocation = LocationCoordinate(location.latitude, location.longitude)
                        }
                        isGettingLocation = false
                    }
                    .addOnFailureListener { isGettingLocation = false }
            } catch (_: SecurityException) {
                isGettingLocation = false
            }
        }
    }

    fun requestLocation() {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            isGettingLocation = true
            try {
                fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
                    .addOnSuccessListener { location ->
                        if (location != null) {
                            capturedLocation = LocationCoordinate(location.latitude, location.longitude)
                        }
                        isGettingLocation = false
                    }
                    .addOnFailureListener { isGettingLocation = false }
            } catch (_: SecurityException) {
                isGettingLocation = false
            }
        } else {
            locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    // Request location when selecting current location source
    LaunchedEffect(locationSource) {
        if (locationSource == LocationSource.CURRENT && capturedLocation == null) {
            requestLocation()
        }
    }

    // Load subscription data
    LaunchedEffect(Unit) {
        subscriptionViewModel.refreshIfStale()
    }

    suspend fun geocodeAddress(address: String): LocationCoordinate? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= 33) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocationName(address, 1) { addresses ->
                        val first = addresses.firstOrNull()
                        cont.resume(first?.let { LocationCoordinate(it.latitude, it.longitude) })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocationName(address, 1)
                results?.firstOrNull()?.let { LocationCoordinate(it.latitude, it.longitude) }
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun reverseGeocode(coord: LocationCoordinate): String? = withContext(Dispatchers.IO) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            if (Build.VERSION.SDK_INT >= 33) {
                suspendCancellableCoroutine { cont ->
                    geocoder.getFromLocation(coord.lat, coord.lng, 1) { addresses ->
                        val place = addresses.firstOrNull()
                        val parts = listOfNotNull(place?.featureName, place?.locality, place?.adminArea, place?.countryName)
                        cont.resume(parts.joinToString(", ").ifEmpty { null })
                    }
                }
            } else {
                @Suppress("DEPRECATION")
                val results = geocoder.getFromLocation(coord.lat, coord.lng, 1)
                val place = results?.firstOrNull()
                val parts = listOfNotNull(place?.featureName, place?.locality, place?.adminArea, place?.countryName)
                parts.joinToString(", ").ifEmpty { null }
            }
        } catch (_: Exception) {
            null
        }
    }

    // Reverse-geocode the captured location for display whenever it changes
    LaunchedEffect(capturedLocation) {
        val loc = capturedLocation
        if (loc != null) {
            isReverseGeocodingCurrent = true
            capturedLocationAddress = reverseGeocode(loc)
            isReverseGeocodingCurrent = false
        } else {
            capturedLocationAddress = null
        }
    }

    fun submitReport() {
        scope.launch {
            isGeocoding = true
            isSubmitting = true

            var coordinate: LocationCoordinate? = null
            var addressText: String? = null
            val notifSource = when (locationSource) {
                LocationSource.CURRENT -> "current_location"
                LocationSource.REGISTERED -> "registered_address"
                LocationSource.CUSTOM -> "custom_address"
            }

            when (locationSource) {
                LocationSource.CURRENT -> {
                    capturedLocation?.let { loc ->
                        coordinate = loc
                        addressText = reverseGeocode(loc) ?: context.getString(R.string.current_location)
                    }
                }
                LocationSource.REGISTERED -> {
                    addressText = registeredAddress
                    registeredAddress?.let { addr ->
                        coordinate = geocodeAddress(addr)
                    }
                }
                LocationSource.CUSTOM -> {
                    val addr = customAddress.ifBlank { null }
                    addressText = addr
                    addr?.let { coordinate = geocodeAddress(it) }
                }
            }

            isGeocoding = false

            if (addressText.isNullOrBlank()) {
                appStateViewModel.showError(context.getString(R.string.mark_lost_address_required))
                isSubmitting = false
                return@launch
            }

            viewModel.markPetMissing(
                petId = pet.id,
                location = coordinate,
                address = addressText,
                description = additionalInfo.ifBlank { null },
                rewardAmount = rewardAmount.ifBlank { null },
                notificationCenterSource = notifSource,
                notificationCenterLocation = coordinate,
                notificationCenterAddress = addressText
            ) { success, error ->
                isSubmitting = false
                if (success) {
                    val msg = if (coordinate != null) {
                        context.getString(R.string.mark_lost_success_with_alerts, pet.name)
                    } else {
                        context.getString(R.string.mark_lost_success_no_location, pet.name)
                    }
                    appStateViewModel.showSuccess(msg)
                    viewModel.refresh()
                    onDismiss()
                } else {
                    // Check for offline queued
                    if (error?.contains("offline", ignoreCase = true) == true || error?.contains("queued", ignoreCase = true) == true) {
                        appStateViewModel.showSuccess(error ?: "")
                        viewModel.refresh()
                        onDismiss()
                    } else {
                        appStateViewModel.showError(error ?: context.getString(R.string.mark_missing_failed))
                    }
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.mark_lost_title, pet.name)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    navigationIconContentColor = BrandOrange
                )
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Section 1: Pet Info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.mark_lost_pet_info),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (pet.profileImage != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context)
                                    .data(pet.profileImage)
                                    .crossfade(true)
                                    .build(),
                                contentDescription = pet.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(CircleShape)
                            )
                        } else {
                            Surface(
                                modifier = Modifier.size(60.dp),
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.primaryContainer
                            ) {
                                Icon(
                                    Icons.Default.Pets,
                                    contentDescription = null,
                                    modifier = Modifier.padding(14.dp),
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(pet.name, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Text(
                                PetLocalizer.localizeSpecies(context, pet.species),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }

            // Section 2: Last Seen Location
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.mark_lost_last_seen),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    // Location source chips
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.height(IntrinsicSize.Min)
                    ) {
                        LocationSourceChip(
                            selected = locationSource == LocationSource.CURRENT,
                            label = stringResource(R.string.location_current),
                            onClick = { locationSource = LocationSource.CURRENT },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        LocationSourceChip(
                            selected = locationSource == LocationSource.REGISTERED,
                            label = stringResource(R.string.location_my_address),
                            onClick = { locationSource = LocationSource.REGISTERED },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                        LocationSourceChip(
                            selected = locationSource == LocationSource.CUSTOM,
                            label = stringResource(R.string.location_custom),
                            onClick = { locationSource = LocationSource.CUSTOM },
                            modifier = Modifier.weight(1f).fillMaxHeight()
                        )
                    }

                    // Conditional content based on source
                    when (locationSource) {
                        LocationSource.CURRENT -> {
                            if (capturedLocation != null) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                    Spacer(modifier = Modifier.width(8.dp))
                                    val addr = capturedLocationAddress
                                    val display = when {
                                        !addr.isNullOrBlank() -> addr
                                        isReverseGeocodingCurrent -> stringResource(R.string.mark_lost_getting_location)
                                        else -> String.format(Locale.US, "%.6f, %.6f", capturedLocation!!.lat, capturedLocation!!.lng)
                                    }
                                    Text(
                                        display,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else if (isGettingLocation) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        stringResource(R.string.mark_lost_getting_location),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            } else {
                                TextButton(onClick = { requestLocation() }) {
                                    Icon(Icons.Default.MyLocation, contentDescription = null, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(stringResource(R.string.tap_get_location))
                                }
                            }
                        }
                        LocationSource.REGISTERED -> {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Home, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                if (registeredAddress != null) {
                                    Text(registeredAddress, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                } else {
                                    Text(stringResource(R.string.mark_lost_no_address), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        LocationSource.CUSTOM -> {
                            OutlinedTextField(
                                value = customAddress,
                                onValueChange = { customAddress = it },
                                placeholder = { Text(stringResource(R.string.mark_lost_address_placeholder)) },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                        }
                    }

                    // Footer
                    Text(
                        stringResource(R.string.mark_lost_alerts_footer),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Section 3: Additional Info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.mark_lost_additional_info),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = additionalInfo,
                        onValueChange = { additionalInfo = it },
                        placeholder = { Text(stringResource(R.string.mark_lost_additional_placeholder)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = 100.dp),
                        maxLines = 5
                    )
                }
            }

            // Section 4: Reward
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = stringResource(R.string.reward_amount_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = rewardAmount,
                        onValueChange = { raw ->
                            // Keep only digits and one separator; cap at
                            // MAX_REWARD_AMOUNT (20). The char-length
                            // cap alone used to accept 20 digits of "9"
                            // (99_999_999_999_999_999_999) which is way
                            // beyond our server bound of 1_000_000.
                            val cleaned = raw.filter { it.isDigit() || it == '.' || it == ',' }
                                .take(InputValidators.MAX_REWARD_AMOUNT)
                            rewardAmount = cleaned
                        },
                        placeholder = { Text(stringResource(R.string.reward_amount_placeholder)) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        isError = rewardAmount.isNotBlank() && !InputValidators.isValidRewardAmount(rewardAmount),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                    )
                }
            }

            // Section 5: Subscription Plan Info
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isOnStarterPlan) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Info, contentDescription = null, tint = BrandOrange, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                stringResource(R.string.mark_lost_starter_notice),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = BrandOrange
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.mark_lost_upgrade_prompt),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            stringResource(R.string.mark_lost_sends_to),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.mark_lost_nearby_owners), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.mark_lost_vet_clinics), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Home, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.mark_lost_shelters), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Section 6: Submit Button
            if (isGeocoding || isSubmitting) {
                BrandButton(
                    text = "",
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                // Overlay spinner
                Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(24.dp).offset(y = (-52).dp),
                        color = MaterialTheme.colorScheme.onPrimary,
                        strokeWidth = 2.dp
                    )
                }
            } else {
                BrandButton(
                    text = stringResource(R.string.mark_lost_report),
                    onClick = { submitReport() },
                    enabled = isFormValid,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun LocationSourceChip(
    selected: Boolean,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.heightIn(min = 56.dp),
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.secondaryContainer else MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp).fillMaxHeight(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (selected) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(4.dp))
            }
            Text(
                text = label,
                fontSize = 12.sp,
                lineHeight = 14.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
                color = if (selected) MaterialTheme.colorScheme.onSecondaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
