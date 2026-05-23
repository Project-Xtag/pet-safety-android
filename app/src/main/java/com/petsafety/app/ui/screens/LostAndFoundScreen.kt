package com.petsafety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldDefaults
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import com.petsafety.app.R
import com.petsafety.app.data.model.CommunityFoundPet
import com.petsafety.app.data.model.MissingPetAlert
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.viewmodel.LostAndFoundViewModel
import com.google.android.gms.maps.model.BitmapDescriptorFactory

private val MissingRed = Color(0xFFFF2D2D)
private val FoundAmber = Color(0xFFF59E0B)
private val FoundAmberTint = Color(0xFFFEF3C7)
private val MapRingBlue = Color(0xFF38BDF8)

/**
 * Redesigned Lost & Found board — Android counterpart of the web's
 * CommunityBoard.tsx and the iOS Lost & Found tab.
 *
 * Shows missing-pet alerts AND community-submitted found-pet reports in
 * one list/map, with search + species + status filters and an
 * "I found a stray pet?" CTA that opens the FoundPetFormScreen.
 */
@Composable
fun LostAndFoundScreen(
    userLocation: LatLng?,
    modifier: Modifier = Modifier,
) {
    val vm: LostAndFoundViewModel = hiltViewModel()
    val missing by vm.filteredMissing.collectAsState()
    val found by vm.filteredFound.collectAsState()
    val isLoading by vm.isLoading.collectAsState()
    val viewMode by vm.viewMode.collectAsState()
    val query by vm.query.collectAsState()
    val speciesFilter by vm.speciesFilter.collectAsState()
    val statusFilter by vm.statusFilter.collectAsState()
    val searchCenter by vm.searchCenter.collectAsState()

    var showFoundForm by remember { mutableStateOf(false) }
    var selectedFoundReport by remember { mutableStateOf<CommunityFoundPet?>(null) }

    androidx.compose.runtime.LaunchedEffect(userLocation) {
        userLocation?.let { vm.fetchNearby(it.latitude, it.longitude) }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item { Header() }
        item { ViewModeToggle(viewMode) { vm.viewMode.value = it } }
        item { SearchBar(query) { vm.query.value = it } }
        item {
            // Resolve the labels eagerly in this composable context; passing
            // a @Composable lambda down to FilterChipsRow's items() block
            // doesn't compile because items() is a non-composable call site.
            val speciesLabels = mapOf(
                LostAndFoundViewModel.SpeciesFilter.ALL to stringResource(R.string.lost_and_found_species_all),
                LostAndFoundViewModel.SpeciesFilter.DOG to stringResource(R.string.lost_and_found_species_dog),
                LostAndFoundViewModel.SpeciesFilter.CAT to stringResource(R.string.lost_and_found_species_cat),
            )
            FilterChipsRow(
                title = stringResource(R.string.lost_and_found_filter_species_label),
                items = LostAndFoundViewModel.SpeciesFilter.values().toList(),
                selected = speciesFilter,
                labelFor = { speciesLabels[it].orEmpty() },
                dotFor = { null },
                onSelect = { vm.speciesFilter.value = it },
            )
        }
        item {
            val statusLabels = mapOf(
                LostAndFoundViewModel.StatusFilter.ALL to stringResource(R.string.lost_and_found_status_all),
                LostAndFoundViewModel.StatusFilter.MISSING to stringResource(R.string.lost_and_found_status_missing),
                LostAndFoundViewModel.StatusFilter.COMMUNITY to stringResource(R.string.lost_and_found_status_community),
            )
            FilterChipsRow(
                title = stringResource(R.string.lost_and_found_filter_status_label),
                items = LostAndFoundViewModel.StatusFilter.values().toList(),
                selected = statusFilter,
                labelFor = { statusLabels[it].orEmpty() },
                dotFor = { statusDot(it) },
                onSelect = { vm.statusFilter.value = it },
            )
        }
        item { FoundCtaCard(onClick = { showFoundForm = true }) }
        if (isLoading && missing.isEmpty() && found.isEmpty()) {
            item {
                Box(Modifier.fillMaxWidth().height(160.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandOrange)
                }
            }
        } else if (viewMode == LostAndFoundViewModel.ViewMode.LIST) {
            if (missing.isEmpty() && found.isEmpty()) {
                item { EmptyState() }
            } else {
                items(missing, key = { "missing-${it.id}" }) { alert ->
                    MissingAlertCard(alert)
                }
                items(found, key = { "found-${it.id}" }) { report ->
                    CommunityFoundPetCard(report, onClick = { selectedFoundReport = report })
                }
            }
        } else {
            item {
                LostAndFoundMap(
                    missing = missing,
                    found = found,
                    searchCenter = searchCenter,
                    notificationRadiusKm = vm.notificationRadiusKm,
                    onSelectFound = { selectedFoundReport = it },
                )
            }
            item { MapLegend(vm.notificationRadiusKm.toInt()) }
        }
    }

    // Found-pet form sheet (replaces the chunk-3 placeholder). The
    // detail sheet is wired in chunk 5.
    if (showFoundForm) {
        androidx.compose.ui.window.Dialog(
            onDismissRequest = { showFoundForm = false },
            properties = androidx.compose.ui.window.DialogProperties(
                usePlatformDefaultWidth = false,
                dismissOnClickOutside = false,
            ),
        ) {
            FoundPetFormScreen(
                onDismiss = { showFoundForm = false },
                onSubmitted = { vm.prependLocalFoundReport(it) },
            )
        }
    }
    selectedFoundReport?.let { report ->
        PlaceholderSheet(
            text = report.breed
                ?: report.foundAddress
                ?: stringResource(R.string.found_pet_unknown_breed),
            onDismiss = { selectedFoundReport = null },
        )
    }
}

// MARK: - Header

@Composable
private fun Header() {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = stringResource(R.string.lost_and_found_eyebrow).uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 2.sp,
            color = Color(0xFFE0531F),
        )
        Text(
            text = stringResource(R.string.lost_and_found_title),
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
            text = stringResource(R.string.lost_and_found_description),
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

// MARK: - View mode toggle (Lista / Térkép)

@Composable
private fun ViewModeToggle(
    current: LostAndFoundViewModel.ViewMode,
    onSelect: (LostAndFoundViewModel.ViewMode) -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(50))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        ToggleButton(
            active = current == LostAndFoundViewModel.ViewMode.LIST,
            icon = Icons.Filled.ViewList,
            label = stringResource(R.string.lost_and_found_view_list),
            onClick = { onSelect(LostAndFoundViewModel.ViewMode.LIST) },
        )
        ToggleButton(
            active = current == LostAndFoundViewModel.ViewMode.MAP,
            icon = Icons.Filled.Map,
            label = stringResource(R.string.lost_and_found_view_map),
            onClick = { onSelect(LostAndFoundViewModel.ViewMode.MAP) },
        )
    }
}

@Composable
private fun ToggleButton(
    active: Boolean,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(50))
            .background(if (active) BrandOrange else Color.Transparent)
            .clickable(onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(16.dp),
        )
        Text(
            label,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}

// MARK: - Search bar

@Composable
private fun SearchBar(value: String, onChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        placeholder = { Text(stringResource(R.string.lost_and_found_search_placeholder)) },
        leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
        singleLine = true,
        shape = RoundedCornerShape(16.dp),
        keyboardOptions = KeyboardOptions(imeAction = androidx.compose.ui.text.input.ImeAction.Search),
        modifier = Modifier.fillMaxWidth(),
        colors = TextFieldDefaults.colors(
            focusedContainerColor = MaterialTheme.colorScheme.surface,
            unfocusedContainerColor = MaterialTheme.colorScheme.surface,
        ),
    )
}

// MARK: - Filter chips

@Composable
private fun <T> FilterChipsRow(
    title: String,
    items: List<T>,
    selected: T,
    labelFor: (T) -> String,
    dotFor: (T) -> Color?,
    onSelect: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = title,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(items) { item ->
                val active = item == selected
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (active) BrandOrange else Color.Transparent)
                        .border(
                            1.dp,
                            if (active) BrandOrange else Color.Black.copy(alpha = 0.15f),
                            RoundedCornerShape(50),
                        )
                        .clickable { onSelect(item) }
                        .padding(horizontal = 12.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    dotFor(item)?.let { dotColor ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(if (active) Color.White else dotColor),
                        )
                    }
                    Text(
                        labelFor(item),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (active) Color.White else MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }
    }
}

@Composable
private fun speciesLabel(filter: LostAndFoundViewModel.SpeciesFilter): String =
    when (filter) {
        LostAndFoundViewModel.SpeciesFilter.ALL -> stringResource(R.string.lost_and_found_species_all)
        LostAndFoundViewModel.SpeciesFilter.DOG -> stringResource(R.string.lost_and_found_species_dog)
        LostAndFoundViewModel.SpeciesFilter.CAT -> stringResource(R.string.lost_and_found_species_cat)
    }

@Composable
private fun statusLabel(filter: LostAndFoundViewModel.StatusFilter): String =
    when (filter) {
        LostAndFoundViewModel.StatusFilter.ALL -> stringResource(R.string.lost_and_found_status_all)
        LostAndFoundViewModel.StatusFilter.MISSING -> stringResource(R.string.lost_and_found_status_missing)
        LostAndFoundViewModel.StatusFilter.COMMUNITY -> stringResource(R.string.lost_and_found_status_community)
    }

private fun statusDot(filter: LostAndFoundViewModel.StatusFilter): Color? = when (filter) {
    LostAndFoundViewModel.StatusFilter.ALL -> null
    LostAndFoundViewModel.StatusFilter.MISSING -> MissingRed
    LostAndFoundViewModel.StatusFilter.COMMUNITY -> FoundAmber
}

// MARK: - Found-pet CTA

@Composable
private fun FoundCtaCard(onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(FoundAmberTint)
            .border(1.dp, FoundAmber.copy(alpha = 0.44f), RoundedCornerShape(20.dp))
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(FoundAmber),
            contentAlignment = Alignment.Center,
        ) {
            Icon(Icons.Filled.Pets, contentDescription = null, tint = Color.White)
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                stringResource(R.string.lost_and_found_cta_title),
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
            )
            Text(
                stringResource(R.string.lost_and_found_cta_body),
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

// MARK: - Cards

@Composable
private fun MissingAlertCard(alert: MissingPetAlert) {
    StatusCard(
        imageUrl = alert.pet?.profileImage,
        statusLabel = stringResource(R.string.lost_and_found_status_missing),
        statusColor = MissingRed,
        title = alert.pet?.name ?: stringResource(R.string.lost_and_found_unknown_pet),
        subtitle = alert.pet?.breed?.takeIf { it.isNotBlank() },
        location = alert.lastSeenLocation?.takeIf { it.isNotBlank() },
        placeholderIcon = Icons.Filled.WarningAmber,
    )
}

@Composable
private fun CommunityFoundPetCard(
    report: CommunityFoundPet,
    onClick: () -> Unit,
) {
    val species = when (report.species) {
        CommunityFoundPet.Species.DOG -> stringResource(R.string.lost_and_found_species_dog_singular)
        CommunityFoundPet.Species.CAT -> stringResource(R.string.lost_and_found_species_cat_singular)
        CommunityFoundPet.Species.OTHER -> stringResource(R.string.lost_and_found_species_other_singular)
    }
    val subtitle = listOfNotNull(report.breed, report.color).joinToString(" · ").ifBlank { null }
    Box(modifier = Modifier.clickable(onClick = onClick)) {
        StatusCard(
            imageUrl = report.photoUrl,
            statusLabel = stringResource(R.string.lost_and_found_status_community),
            statusColor = FoundAmber,
            title = species,
            subtitle = subtitle,
            location = report.foundAddress?.takeIf { it.isNotBlank() },
            placeholderIcon = Icons.Filled.Pets,
        )
    }
}

@Composable
private fun StatusCard(
    imageUrl: String?,
    statusLabel: String,
    statusColor: Color,
    title: String,
    subtitle: String?,
    location: String?,
    placeholderIcon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .border(1.dp, Color.Black.copy(alpha = 0.06f), RoundedCornerShape(20.dp)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(4f / 3f)
                .background(statusColor.copy(alpha = 0.15f)),
        ) {
            if (!imageUrl.isNullOrBlank()) {
                AsyncImage(
                    model = imageUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Icon(
                    placeholderIcon,
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(48.dp).align(Alignment.Center),
                )
            }
            Box(
                modifier = Modifier
                    .padding(10.dp)
                    .clip(RoundedCornerShape(50))
                    .background(statusColor)
                    .padding(horizontal = 10.dp, vertical = 4.dp),
            ) {
                Text(
                    statusLabel.uppercase(),
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                )
            }
        }
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            subtitle?.let {
                Text(it, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            location?.let {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Icon(
                        Icons.Filled.LocationOn,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(12.dp),
                    )
                    Text(
                        it,
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

// MARK: - Map

@Composable
private fun LostAndFoundMap(
    missing: List<MissingPetAlert>,
    found: List<CommunityFoundPet>,
    searchCenter: Pair<Double, Double>?,
    notificationRadiusKm: Double,
    onSelectFound: (CommunityFoundPet) -> Unit,
) {
    val center = searchCenter?.let { LatLng(it.first, it.second) } ?: LatLng(47.4979, 19.0402)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(center, 11f)
    }
    androidx.compose.runtime.LaunchedEffect(searchCenter) {
        searchCenter?.let {
            cameraPositionState.animate(
                CameraUpdateFactory.newLatLngZoom(LatLng(it.first, it.second), 11f),
            )
        }
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(420.dp)
            .clip(RoundedCornerShape(20.dp)),
    ) {
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
        ) {
            searchCenter?.let {
                Circle(
                    center = LatLng(it.first, it.second),
                    radius = notificationRadiusKm * 1000.0,
                    strokeColor = MapRingBlue,
                    strokeWidth = 2f,
                    fillColor = MapRingBlue.copy(alpha = 0.10f),
                )
            }
            missing.forEach { alert ->
                val lat = alert.lastSeenLatitude
                val lng = alert.lastSeenLongitude
                if (lat != null && lng != null) {
                    Marker(
                        state = MarkerState(position = LatLng(lat, lng)),
                        title = alert.pet?.name,
                        snippet = alert.lastSeenLocation,
                        icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_RED),
                    )
                }
            }
            found.forEach { report ->
                Marker(
                    state = MarkerState(position = LatLng(report.foundLatitude, report.foundLongitude)),
                    title = report.breed ?: report.foundAddress,
                    icon = BitmapDescriptorFactory.defaultMarker(BitmapDescriptorFactory.HUE_ORANGE),
                    onClick = { onSelectFound(report); false },
                )
            }
        }
    }
}

@Composable
private fun MapLegend(radiusKm: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        LegendDot(MissingRed, stringResource(R.string.lost_and_found_status_missing))
        LegendDot(FoundAmber, stringResource(R.string.lost_and_found_status_community))
        LegendDot(
            MapRingBlue,
            stringResource(R.string.lost_and_found_legend_radius, radiusKm),
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// MARK: - Empty state + placeholder sheet

@Composable
private fun EmptyState() {
    Box(
        modifier = Modifier.fillMaxWidth().height(200.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            stringResource(R.string.no_active_alerts_message),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun PlaceholderSheet(text: String, onDismiss: () -> Unit) {
    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_done))
            }
        },
        text = { Text(text) },
    )
}
