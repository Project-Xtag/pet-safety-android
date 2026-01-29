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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FormatListBulleted
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.petsafety.app.R
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.network.model.CreateSuccessStoryRequest
import com.petsafety.app.ui.components.BrandButton
import com.petsafety.app.ui.components.SuccessStoryDialog
import com.petsafety.app.ui.theme.BackgroundLight
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.MutedTextLight
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.util.AdaptiveLayout
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.PetsViewModel
import com.petsafety.app.ui.viewmodel.SuccessStoriesViewModel

@Composable
fun PetDetailScreen(
    viewModel: PetsViewModel,
    successStoriesViewModel: SuccessStoriesViewModel,
    petId: String,
    onEditPet: () -> Unit,
    onOpenPhotos: () -> Unit,
    onViewPublicProfile: () -> Unit = {},
    onBack: () -> Unit,
    appStateViewModel: AppStateViewModel
) {
    val pets by viewModel.pets.collectAsState()
    val pet = pets.firstOrNull { it.id == petId } ?: return
    var showMissingDialog by remember { mutableStateOf(false) }
    var showSuccessStoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showCannotDeleteAlert by remember { mutableStateOf(false) }
    var isDeleting by remember { mutableStateOf(false) }
    var foundPetName by remember { mutableStateOf("") }

    // Extract string resources outside lambdas
    val markedFoundMessage = stringResource(R.string.marked_found_message, pet.name)
    val markFoundFailedMessage = stringResource(R.string.mark_found_failed)
    val markedMissingMessage = stringResource(R.string.marked_missing_message, pet.name)
    val markMissingFailedMessage = stringResource(R.string.mark_missing_failed)
    val storySharedMessage = stringResource(R.string.story_shared)
    val storyShareFailedMessage = stringResource(R.string.story_share_failed)
    val petDeletedMessage = stringResource(R.string.pet_deleted, pet.name)
    val deleteFailedMessage = stringResource(R.string.delete_pet_failed)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AdaptiveLayout.MaxContentWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 16.dp)
        ) {
            // Back Button
            IconButton(
                onClick = onBack,
                modifier = Modifier.padding(start = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pet Photo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
                    .height(300.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFF2F2F7))
            ) {
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
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.Center),
                        tint = MutedTextLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // View Photos Button
            Button(
                onClick = onOpenPhotos,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = TealAccent.copy(alpha = 0.1f),
                    contentColor = TealAccent
                ),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Photo,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "View ${pet.name}'s Photos",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Pet Name
            Text(
                text = pet.name,
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Mark as Lost/Found Button
            if (pet.isMissing) {
                Button(
                    onClick = {
                        viewModel.markPetFound(pet.id) { success, message ->
                            if (success) {
                                appStateViewModel.showSuccess(markedFoundMessage)
                                foundPetName = pet.name
                                showSuccessStoryDialog = true
                            } else {
                                appStateViewModel.showError(message ?: markFoundFailedMessage)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C759)
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Found", fontWeight = FontWeight.SemiBold)
                }
            } else {
                Button(
                    onClick = { showMissingDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandOrange
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Warning, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Mark as Lost", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info Cards
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoCard(
                    title = "Species",
                    value = pet.species.replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.Pets
                )

                pet.breed?.let {
                    InfoCard(title = "Breed", value = it, icon = Icons.Default.FormatListBulleted)
                }

                pet.color?.let {
                    InfoCard(title = "Color", value = it, icon = Icons.Default.Palette)
                }

                pet.age?.let {
                    InfoCard(title = "Age", value = it, icon = Icons.Default.CalendarMonth)
                }

                pet.microchipNumber?.let {
                    InfoCard(title = "Microchip", value = it, icon = Icons.Default.Pin)
                }
            }

            // Medical Info
            pet.medicalNotes?.takeIf { it.isNotBlank() }?.let { medicalNotes ->
                Spacer(modifier = Modifier.height(16.dp))
                InfoSection(
                    title = "Medical Information",
                    content = medicalNotes,
                    icon = Icons.Default.LocalHospital
                )
            }

            // Additional Information
            pet.notes?.takeIf { it.isNotBlank() }?.let { notes ->
                Spacer(modifier = Modifier.height(16.dp))
                InfoSection(
                    title = "Additional Information",
                    content = notes,
                    icon = Icons.Default.Info
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Action Buttons
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // View Public Profile Button
                Button(
                    onClick = onViewPublicProfile,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF34C759).copy(alpha = 0.1f),
                        contentColor = Color(0xFF34C759)
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "View ${pet.name}'s Public Profile",
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Edit Button
                BrandButton(
                    text = "Edit ${pet.name}'s Profile",
                    onClick = onEditPet,
                    icon = Icons.Default.Edit
                )

                // Delete Button
                Button(
                    onClick = {
                        if (pet.isMissing) {
                            showCannotDeleteAlert = true
                        } else {
                            showDeleteConfirmation = true
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.1f),
                        contentColor = Color.Red
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = Color.Red,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Delete ${pet.name}", fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Dialogs
    if (showMissingDialog) {
        MarkMissingDialog(
            pet = pet,
            onDismiss = { showMissingDialog = false },
            onSubmit = { coordinate, locationText, description ->
                viewModel.markPetMissing(
                    pet.id,
                    location = coordinate,
                    address = locationText,
                    description = description
                ) { success, msg ->
                    if (success) {
                        appStateViewModel.showSuccess(markedMissingMessage)
                    } else {
                        appStateViewModel.showError(msg ?: markMissingFailedMessage)
                    }
                    showMissingDialog = false
                }
            }
        )
    }

    if (showSuccessStoryDialog) {
        SuccessStoryDialog(
            petName = foundPetName,
            onDismiss = {
                showSuccessStoryDialog = false
                onBack()
            },
            onSubmit = { storyText ->
                successStoriesViewModel.createStory(
                    CreateSuccessStoryRequest(
                        petId = petId,
                        storyText = storyText,
                        autoConfirm = true
                    )
                ) { success, message ->
                    if (success) {
                        appStateViewModel.showSuccess(storySharedMessage)
                    } else {
                        appStateViewModel.showError(message ?: storyShareFailedMessage)
                    }
                    showSuccessStoryDialog = false
                    onBack()
                }
            },
            onSkip = {
                showSuccessStoryDialog = false
                onBack()
            }
        )
    }

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete ${pet.name}?") },
            text = {
                Text("This action cannot be undone. All data for ${pet.name} will be permanently deleted.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        isDeleting = true
                        viewModel.deletePet(pet.id) { success, message ->
                            isDeleting = false
                            if (success) {
                                appStateViewModel.showSuccess(petDeletedMessage)
                                onBack()
                            } else {
                                appStateViewModel.showError(message ?: deleteFailedMessage)
                            }
                            showDeleteConfirmation = false
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (showCannotDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showCannotDeleteAlert = false },
            title = { Text("Cannot Delete Missing Pet") },
            text = {
                Text("${pet.name} is currently marked as missing. Please mark them as found before deleting.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCannotDeleteAlert = false
                        viewModel.markPetFound(pet.id) { success, message ->
                            if (success) {
                                appStateViewModel.showSuccess(markedFoundMessage)
                                foundPetName = pet.name
                                showSuccessStoryDialog = true
                            } else {
                                appStateViewModel.showError(message ?: markFoundFailedMessage)
                            }
                        }
                    }
                ) {
                    Text("Mark as Found")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCannotDeleteAlert = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun InfoCard(
    title: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.width(120.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MutedTextLight
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MutedTextLight
                )
            }
            Spacer(modifier = Modifier.weight(1f))
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun InfoSection(
    title: String,
    content: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF2F2F7))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = content,
                style = MaterialTheme.typography.bodyMedium,
                color = MutedTextLight
            )
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun MarkMissingDialog(
    pet: Pet,
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
        title = { Text(stringResource(R.string.report_missing_title, pet.name)) },
        text = {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { captureLocation() },
                        enabled = !isCapturingLocation,
                        modifier = Modifier.weight(1f)
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
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.use_current_location))
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = locationText,
                    onValueChange = {
                        locationText = it
                        if (capturedCoordinate != null) {
                            capturedCoordinate = null
                        }
                    },
                    label = { Text(stringResource(R.string.last_seen_location)) },
                    modifier = Modifier.fillMaxWidth(),
                    supportingText = if (capturedCoordinate != null) {
                        { Text(stringResource(R.string.location_captured)) }
                    } else null
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.additional_info)) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(onClick = {
                onSubmit(
                    capturedCoordinate,
                    locationText.ifBlank { null },
                    notes.ifBlank { null }
                )
            }) {
                Text(stringResource(R.string.report))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
