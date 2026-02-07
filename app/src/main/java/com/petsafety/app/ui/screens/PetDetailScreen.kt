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
import androidx.compose.material.icons.filled.MonitorWeight
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Photo
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Scale
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
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
import com.petsafety.app.data.model.User
import com.petsafety.app.data.network.model.CreateSuccessStoryRequest
import com.petsafety.app.ui.components.BrandButton
import com.petsafety.app.ui.components.NotificationCenterSource
import com.petsafety.app.ui.components.SuccessStoryDialog
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.SuccessGreen
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.util.AdaptiveLayout
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel
import com.petsafety.app.ui.viewmodel.PetsViewModel
import com.petsafety.app.ui.viewmodel.SuccessStoriesViewModel

@Composable
fun PetDetailScreen(
    viewModel: PetsViewModel,
    successStoriesViewModel: SuccessStoriesViewModel,
    authViewModel: AuthViewModel,
    petId: String,
    onEditPet: () -> Unit,
    onOpenPhotos: () -> Unit,
    onViewPublicProfile: () -> Unit = {},
    onBack: () -> Unit,
    appStateViewModel: AppStateViewModel
) {
    val pets by viewModel.pets.collectAsState()
    val pet = pets.firstOrNull { it.id == petId } ?: return
    val currentUser by authViewModel.currentUser.collectAsState()
    var showMissingDialog by remember { mutableStateOf(false) }
    var showSuccessStoryDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmation by remember { mutableStateOf(false) }
    var showCannotDeleteAlert by remember { mutableStateOf(false) }
    var showMarkFoundConfirmation by remember { mutableStateOf(false) }
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
            .background(MaterialTheme.colorScheme.background),
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
                    contentDescription = stringResource(R.string.back),
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
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
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
                        contentDescription = stringResource(R.string.pet_photo_placeholder),
                        modifier = Modifier
                            .size(80.dp)
                            .align(Alignment.Center),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
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
                    text = stringResource(R.string.view_pet_photos, pet.name),
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
                    onClick = { showMarkFoundConfirmation = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = SuccessGreen
                    ),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.mark_as_found), fontWeight = FontWeight.SemiBold)
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
                    Text(stringResource(R.string.mark_as_lost), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Info Cards
            Column(
                modifier = Modifier.padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                InfoCard(
                    title = stringResource(R.string.species),
                    value = pet.species.replaceFirstChar { it.uppercase() },
                    icon = Icons.Default.Pets
                )

                pet.breed?.let {
                    InfoCard(title = stringResource(R.string.breed), value = it, icon = Icons.Default.FormatListBulleted)
                }

                pet.color?.let {
                    InfoCard(title = stringResource(R.string.color), value = it, icon = Icons.Default.Palette)
                }

                pet.age?.let {
                    InfoCard(title = stringResource(R.string.age), value = it, icon = Icons.Default.CalendarMonth)
                }

                pet.weight?.let { weight ->
                    InfoCard(title = stringResource(R.string.weight), value = String.format("%.1f kg", weight), icon = Icons.Default.Scale)
                }

                pet.sex?.takeIf { it.lowercase() != "unknown" }?.let { sex ->
                    InfoCard(title = stringResource(R.string.sex), value = sex.replaceFirstChar { it.uppercase() }, icon = Icons.Default.Person)
                }

                pet.isNeutered?.takeIf { it }?.let {
                    InfoCard(title = stringResource(R.string.neutered_spayed), value = stringResource(R.string.yes), icon = Icons.Default.CheckCircle)
                }

                pet.microchipNumber?.let {
                    InfoCard(title = stringResource(R.string.microchip), value = it, icon = Icons.Default.Pin)
                }
            }

            // Health Information
            if (pet.medicalNotes?.isNotBlank() == true || pet.allergies?.isNotBlank() == true || pet.medications?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(16.dp))
                HealthInfoSection(
                    medicalNotes = pet.medicalNotes,
                    allergies = pet.allergies,
                    medications = pet.medications
                )
            }

            // Additional Information
            if (pet.uniqueFeatures?.isNotBlank() == true || pet.notes?.isNotBlank() == true) {
                Spacer(modifier = Modifier.height(16.dp))
                AdditionalInfoSection(
                    uniqueFeatures = pet.uniqueFeatures,
                    behaviorNotes = pet.notes
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
                        containerColor = SuccessGreen.copy(alpha = 0.1f),
                        contentColor = SuccessGreen
                    ),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.view_public_profile, pet.name),
                        fontWeight = FontWeight.SemiBold
                    )
                }

                // Edit Button
                BrandButton(
                    text = stringResource(R.string.edit_pet_profile, pet.name),
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
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    shape = RoundedCornerShape(10.dp),
                    enabled = !isDeleting
                ) {
                    if (isDeleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.error,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.delete_pet_name, pet.name), fontWeight = FontWeight.SemiBold)
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Dialogs
    if (showMissingDialog) {
        MarkMissingDialog(
            pet = pet,
            currentUser = currentUser,
            onDismiss = { showMissingDialog = false },
            onSubmit = { coordinate, locationText, description, notifSource, notifLocation, notifAddress ->
                viewModel.markPetMissing(
                    pet.id,
                    location = coordinate,
                    address = locationText,
                    description = description,
                    notificationCenterSource = notifSource,
                    notificationCenterLocation = notifLocation,
                    notificationCenterAddress = notifAddress
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
            title = { Text(stringResource(R.string.delete_pet_confirm_title, pet.name)) },
            text = {
                Text(stringResource(R.string.delete_pet_confirm_message, pet.name))
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
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showCannotDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showCannotDeleteAlert = false },
            title = { Text(stringResource(R.string.cannot_delete_missing_pet)) },
            text = {
                Text(stringResource(R.string.cannot_delete_missing_message, pet.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showCannotDeleteAlert = false
                        showMarkFoundConfirmation = true
                    }
                ) {
                    Text(stringResource(R.string.mark_as_found))
                }
            },
            dismissButton = {
                TextButton(onClick = { showCannotDeleteAlert = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showMarkFoundConfirmation) {
        AlertDialog(
            onDismissRequest = { showMarkFoundConfirmation = false },
            title = { Text(stringResource(R.string.mark_found_confirm_title, pet.name)) },
            text = {
                Text(stringResource(R.string.mark_found_confirm_message, pet.name))
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showMarkFoundConfirmation = false
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
                    Text(stringResource(R.string.mark_as_found))
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

@Composable
private fun InfoCard(
    title: String,
    value: String,
    icon: ImageVector
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
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
                    contentDescription = title,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
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
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = title,
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
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun HealthInfoSection(
    medicalNotes: String?,
    allergies: String?,
    medications: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.LocalHospital,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.health_information),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            medicalNotes?.takeIf { it.isNotBlank() }?.let {
                Column {
                    Text(
                        text = stringResource(R.string.medical_notes),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            allergies?.takeIf { it.isNotBlank() }?.let {
                Column {
                    Text(
                        text = stringResource(R.string.allergies),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            medications?.takeIf { it.isNotBlank() }?.let {
                Column {
                    Text(
                        text = stringResource(R.string.medications),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun AdditionalInfoSection(
    uniqueFeatures: String?,
    behaviorNotes: String?
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.additional_information),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            uniqueFeatures?.takeIf { it.isNotBlank() }?.let {
                Column {
                    Text(
                        text = stringResource(R.string.unique_features),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            behaviorNotes?.takeIf { it.isNotBlank() }?.let {
                Column {
                    Text(
                        text = stringResource(R.string.behavior_notes),
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@SuppressLint("MissingPermission")
@Composable
private fun MarkMissingDialog(
    pet: Pet,
    currentUser: User?,
    onDismiss: () -> Unit,
    onSubmit: (LocationCoordinate?, String?, String?, String?, LocationCoordinate?, String?) -> Unit
) {
    val context = LocalContext.current
    val locationProvider = remember { LocationServices.getFusedLocationProviderClient(context) }

    var selectedSource by remember { mutableStateOf(NotificationCenterSource.REGISTERED_ADDRESS) }
    var locationText by remember { mutableStateOf("") }
    var customAddress by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var capturedCoordinate by remember { mutableStateOf<LocationCoordinate?>(null) }
    var isCapturingLocation by remember { mutableStateOf(false) }
    var isGeocoding by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val registeredAddress = remember(currentUser) {
        listOfNotNull(currentUser?.address, currentUser?.city, currentUser?.postalCode, currentUser?.country)
            .filter { it.isNotBlank() }
            .joinToString(", ")
            .ifBlank { null }
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

    fun geocodeAndSubmit() {
        val geocoder = android.location.Geocoder(context)
        val addressToGeocode = when (selectedSource) {
            NotificationCenterSource.REGISTERED_ADDRESS -> registeredAddress
            NotificationCenterSource.CUSTOM_ADDRESS -> customAddress.ifBlank { null }
            NotificationCenterSource.CURRENT_LOCATION -> null
        }

        val notifSource = selectedSource.value

        if (selectedSource == NotificationCenterSource.CURRENT_LOCATION) {
            // GPS already captured — reverse-geocode for a readable address
            @Suppress("DEPRECATION")
            val reverseAddress = capturedCoordinate?.let { coord ->
                try {
                    geocoder.getFromLocation(coord.lat, coord.lng, 1)
                        ?.firstOrNull()
                        ?.getAddressLine(0)
                } catch (_: Exception) { null }
            } ?: locationText.ifBlank { "Current location" }
            onSubmit(
                capturedCoordinate,
                reverseAddress,
                notes.ifBlank { null },
                notifSource,
                capturedCoordinate,
                reverseAddress
            )
            return
        }

        if (addressToGeocode == null) {
            onSubmit(null, null, notes.ifBlank { null }, notifSource, null, null)
            return
        }

        isGeocoding = true
        @Suppress("DEPRECATION")
        try {
            val results = geocoder.getFromLocationName(addressToGeocode, 1)
            val coord = results?.firstOrNull()?.let {
                LocationCoordinate(it.latitude, it.longitude)
            }
            onSubmit(
                coord,
                addressToGeocode,
                notes.ifBlank { null },
                notifSource,
                coord,
                addressToGeocode
            )
        } catch (_: Exception) {
            // Geocoding failed, submit without coordinates — backend will handle it
            onSubmit(null, addressToGeocode, notes.ifBlank { null }, notifSource, null, addressToGeocode)
        } finally {
            isGeocoding = false
        }
    }

    val isSubmitDisabled = when (selectedSource) {
        NotificationCenterSource.CURRENT_LOCATION -> capturedCoordinate == null && !isCapturingLocation
        NotificationCenterSource.REGISTERED_ADDRESS -> registeredAddress == null
        NotificationCenterSource.CUSTOM_ADDRESS -> customAddress.isBlank()
    } || isGeocoding

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.report_missing_title, pet.name)) },
        text = {
            Column {
                // Location Source Picker
                Text(
                    text = stringResource(R.string.last_seen_location),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    NotificationCenterSource.entries.forEach { source ->
                        FilterChip(
                            selected = selectedSource == source,
                            onClick = {
                                selectedSource = source
                                if (source == NotificationCenterSource.CURRENT_LOCATION && capturedCoordinate == null) {
                                    captureLocation()
                                }
                            },
                            label = { Text(source.displayName, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))

                // Source-specific content
                when (selectedSource) {
                    NotificationCenterSource.CURRENT_LOCATION -> {
                        if (isCapturingLocation) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.tap_get_location),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (capturedCoordinate != null) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp),
                                    tint = SuccessGreen
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = locationText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            OutlinedButton(
                                onClick = { captureLocation() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    Icons.Default.MyLocation,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(stringResource(R.string.use_current_location))
                            }
                        }
                    }
                    NotificationCenterSource.REGISTERED_ADDRESS -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.LocationOn,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (registeredAddress != null) SuccessGreen else MaterialTheme.colorScheme.error
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = registeredAddress ?: stringResource(R.string.no_registered_address),
                                style = MaterialTheme.typography.bodySmall,
                                color = if (registeredAddress != null) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.error
                            )
                        }
                    }
                    NotificationCenterSource.CUSTOM_ADDRESS -> {
                        OutlinedTextField(
                            value = customAddress,
                            onValueChange = { customAddress = it },
                            label = { Text(stringResource(R.string.enter_address_notifications)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.additional_info)) },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.alerts_radius_info),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { geocodeAndSubmit() },
                enabled = !isSubmitDisabled
            ) {
                if (isGeocoding) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text(stringResource(R.string.report))
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) }
        }
    )
}
