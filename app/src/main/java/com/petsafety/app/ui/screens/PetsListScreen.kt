package com.petsafety.app.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import com.petsafety.app.ui.util.AdaptiveLayout
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.petsafety.app.R
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.Pet
import com.petsafety.app.ui.components.BrandButton
import com.petsafety.app.ui.components.MarkFoundSheet
import com.petsafety.app.ui.components.OfflineIndicator
import com.petsafety.app.ui.components.PetsListSkeleton
import com.petsafety.app.ui.components.ReportMissingSheet
import com.petsafety.app.ui.theme.BackgroundLight
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.MutedTextLight
import com.petsafety.app.ui.theme.PeachBackground
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel
import com.petsafety.app.ui.viewmodel.PetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetsListScreen(
    viewModel: PetsViewModel,
    appStateViewModel: AppStateViewModel,
    authViewModel: AuthViewModel? = null,
    onPetSelected: (Pet) -> Unit,
    onAddPet: () -> Unit,
    onOrderTags: () -> Unit,
    onReplacementTag: (String) -> Unit,
    onSuccessStories: () -> Unit = {}
) {
    val pets by viewModel.pets.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val error by viewModel.errorMessage.collectAsState()
    val isConnected by appStateViewModel.isConnected.collectAsState()
    val currentUser by authViewModel?.currentUser?.collectAsState() ?: remember { mutableStateOf(null) }

    val hasMissingPets = pets.any { it.isMissing }
    val missingPets = pets.filter { it.isMissing }
    val availablePets = pets.filter { !it.isMissing }
    val addPetFirstMessage = stringResource(R.string.add_pet_first_replacement)

    // Bottom sheet states
    var showMarkFoundSheet by remember { mutableStateOf(false) }
    var showReportMissingSheet by remember { mutableStateOf(false) }

    // Location handling
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    var locationCallback by remember { mutableStateOf<((LocationCoordinate?) -> Unit)?>(null) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            // Permission granted, try to get location
            try {
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    val coordinate = location?.let { LocationCoordinate(it.latitude, it.longitude) }
                    locationCallback?.invoke(coordinate)
                }.addOnFailureListener {
                    locationCallback?.invoke(null)
                }
            } catch (e: SecurityException) {
                locationCallback?.invoke(null)
            }
        } else {
            locationCallback?.invoke(null)
        }
    }

    // Success/error messages
    val markedFoundMessage = stringResource(R.string.marked_found_message, "%s")
    val markedMissingMessage = stringResource(R.string.marked_missing_message, "%s")
    val markFoundFailed = stringResource(R.string.mark_found_failed)
    val markMissingFailed = stringResource(R.string.mark_missing_failed)
    val noMissingPetsMessage = stringResource(R.string.no_missing_pets_message)
    val noPetsToReportMessage = stringResource(R.string.no_pets_to_report_message)

    LaunchedEffect(Unit) { viewModel.fetchPets() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            OfflineIndicator(appStateViewModel.syncService, isConnected)

            if (pets.isEmpty() && !isLoading) {
                EmptyStateView(
                    onAddPet = onAddPet
                )
            } else {
                PullToRefreshBox(
                    isRefreshing = isRefreshing,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Header Section
                        HeaderSection(userName = currentUser?.firstName ?: "Pet Owner")

                        Spacer(modifier = Modifier.height(24.dp))

                        // My Pets Section
                        PetsSection(
                            pets = pets,
                            onPetSelected = onPetSelected
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Quick Actions Section
                        QuickActionsSection(
                            hasMissingPets = hasMissingPets,
                            onAddPet = onAddPet,
                            onMarkLostOrFound = {
                                if (hasMissingPets) {
                                    // Show mark found sheet
                                    if (missingPets.isNotEmpty()) {
                                        showMarkFoundSheet = true
                                    } else {
                                        appStateViewModel.showError(noMissingPetsMessage)
                                    }
                                } else {
                                    // Show report missing sheet
                                    if (availablePets.isNotEmpty()) {
                                        showReportMissingSheet = true
                                    } else {
                                        appStateViewModel.showError(noPetsToReportMessage)
                                    }
                                }
                            },
                            onOrderTags = onOrderTags,
                            onReplaceTag = {
                                val firstPetId = pets.firstOrNull()?.id
                                if (firstPetId != null) {
                                    onReplacementTag(firstPetId)
                                } else {
                                    appStateViewModel.showError(addPetFirstMessage)
                                }
                            }
                        )

                        Spacer(modifier = Modifier.height(24.dp))

                        // Success Stories Section
                        SuccessStoriesSection(onClick = onSuccessStories)

                        Spacer(modifier = Modifier.height(100.dp))
                    }
                }
            }
        }

        // Show skeleton loading on initial load
        if (isLoading && pets.isEmpty()) {
            PetsListSkeleton(
                columns = 2,
                itemCount = 4
            )
        }
    }

    // Mark Found Bottom Sheet
    if (showMarkFoundSheet) {
        MarkFoundSheet(
            missingPets = missingPets,
            onDismiss = { showMarkFoundSheet = false },
            onMarkFound = { pet ->
                viewModel.markPetFound(pet.id) { success, error ->
                    showMarkFoundSheet = false
                    if (success) {
                        viewModel.refresh()
                        appStateViewModel.showSuccess(markedFoundMessage.replace("%s", pet.name))
                    } else {
                        appStateViewModel.showError(error ?: markFoundFailed)
                    }
                }
            }
        )
    }

    // Report Missing Bottom Sheet
    if (showReportMissingSheet) {
        ReportMissingSheet(
            availablePets = availablePets,
            onDismiss = { showReportMissingSheet = false },
            onReportMissing = { pet, location, address, description, notifSource, notifLocation, notifAddress ->
                viewModel.markPetMissing(
                    petId = pet.id,
                    location = location,
                    address = address,
                    description = description,
                    notificationCenterSource = notifSource,
                    notificationCenterLocation = notifLocation,
                    notificationCenterAddress = notifAddress
                ) { success, error ->
                    showReportMissingSheet = false
                    if (success) {
                        viewModel.refresh()
                        appStateViewModel.showSuccess(markedMissingMessage.replace("%s", pet.name))
                    } else {
                        appStateViewModel.showError(error ?: markMissingFailed)
                    }
                }
            },
            onRequestLocation = { callback ->
                locationCallback = callback
                val hasPermission = ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED

                if (hasPermission) {
                    try {
                        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                            val coordinate = location?.let { LocationCoordinate(it.latitude, it.longitude) }
                            callback(coordinate)
                        }.addOnFailureListener {
                            callback(null)
                        }
                    } catch (e: SecurityException) {
                        callback(null)
                    }
                } else {
                    locationPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                }
            }
        )
    }
}

@Composable
private fun HeaderSection(userName: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(PeachBackground)
            .padding(horizontal = 24.dp, vertical = 16.dp)
    ) {
        Text(
            text = "Welcome back,",
            style = MaterialTheme.typography.bodyMedium.copy(
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MutedTextLight
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = userName,
            style = MaterialTheme.typography.headlineSmall.copy(
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun PetsSection(
    pets: List<Pet>,
    onPetSelected: (Pet) -> Unit
) {
    Column {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "My Pets",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            if (pets.size > 4) {
                Text(
                    text = "View All",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = BrandOrange,
                    modifier = Modifier.clickable { /* View all */ }
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Pet Cards Grid - adaptive columns for tablets
        val gridColumns = AdaptiveLayout.gridColumns()
        val displayPets = pets.take(if (gridColumns >= 3) 6 else 4)
        val rowCount = (displayPets.size + gridColumns - 1) / gridColumns
        LazyVerticalGrid(
            columns = GridCells.Fixed(gridColumns),
            modifier = Modifier
                .padding(horizontal = AdaptiveLayout.horizontalPadding())
                .height((rowCount * 180).dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            userScrollEnabled = false
        ) {
            items(displayPets) { pet ->
                PetCardView(pet = pet, onClick = { onPetSelected(pet) })
            }
        }
    }
}

@Composable
private fun PetCardView(pet: Pet, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(16.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            )
            .then(
                if (pet.isMissing) {
                    Modifier.border(2.dp, Color.Red, RoundedCornerShape(16.dp))
                } else {
                    Modifier
                }
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Column {
            // Pet Photo with Missing Badge
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (!pet.profileImage.isNullOrBlank()) {
                    AsyncImage(
                        model = pet.profileImage,
                        contentDescription = pet.name,
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color(0xFFF2F2F7))
                            .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (pet.species.lowercase() == "dog") Icons.Default.Pets else Icons.Default.Pets,
                            contentDescription = null,
                            modifier = Modifier.size(48.dp),
                            tint = MutedTextLight
                        )
                    }
                }

                // Missing Badge
                if (pet.isMissing) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                            .background(Color.Red, RoundedCornerShape(8.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(10.dp),
                            tint = Color.White
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "MISSING",
                            style = MaterialTheme.typography.labelSmall.copy(
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = Color.White
                        )
                    }
                }
            }

            // Pet Name
            Text(
                text = pet.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = if (pet.isMissing) Color.Red else MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp)
            )
        }
    }
}

@Composable
private fun QuickActionsSection(
    hasMissingPets: Boolean,
    onAddPet: () -> Unit,
    onMarkLostOrFound: () -> Unit,
    onOrderTags: () -> Unit,
    onReplaceTag: () -> Unit
) {
    Column {
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                icon = Icons.Default.Add,
                title = "ADD PET",
                color = TealAccent,
                onClick = onAddPet,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = if (hasMissingPets) Icons.Default.CheckCircle else Icons.Default.Warning,
                title = if (hasMissingPets) "MARK FOUND" else "REPORT MISSING",
                color = if (hasMissingPets) Color(0xFF34C759) else Color.Red,
                onClick = onMarkLostOrFound,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Default.ShoppingCart,
                title = "ORDER TAGS",
                color = TealAccent,
                onClick = onOrderTags,
                modifier = Modifier.weight(1f)
            )
            QuickActionButton(
                icon = Icons.Default.Refresh,
                title = "REPLACE TAG",
                color = BrandOrange,
                onClick = onReplaceTag,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
private fun QuickActionButton(
    icon: ImageVector,
    title: String,
    color: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .shadow(
                elevation = 8.dp,
                shape = RoundedCornerShape(20.dp),
                ambientColor = Color.Black.copy(alpha = 0.06f),
                spotColor = Color.Black.copy(alpha = 0.06f)
            ),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        onClick = onClick
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(color.copy(alpha = 0.1f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = color
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                ),
                color = MutedTextLight,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun SuccessStoriesSection(onClick: () -> Unit) {
    Column {
        Text(
            text = "Success Stories",
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(16.dp))

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .shadow(
                    elevation = 8.dp,
                    shape = RoundedCornerShape(16.dp),
                    ambientColor = Color.Black.copy(alpha = 0.06f),
                    spotColor = Color.Black.copy(alpha = 0.06f)
                ),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            onClick = onClick
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icon
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(Color(0xFF34C759).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        modifier = Modifier.size(30.dp),
                        tint = Color(0xFF34C759)
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Text
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Found Pets",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "See happy reunions near you",
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MutedTextLight
                    )
                }

                // Chevron
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    modifier = Modifier.size(14.dp),
                    tint = MutedTextLight
                )
            }
        }
    }
}

@Composable
private fun EmptyStateView(onAddPet: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(100.dp)
                .background(Color(0xFFF2F2F7), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Pets,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = TealAccent
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = stringResource(R.string.empty_pets_title),
            style = MaterialTheme.typography.titleLarge.copy(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.empty_pets_message),
            style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
            color = MutedTextLight,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        BrandButton(
            text = stringResource(R.string.add_pet),
            onClick = onAddPet,
            modifier = Modifier.padding(horizontal = 60.dp)
        )
    }
}
