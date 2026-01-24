package com.petsafety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.petsafety.app.R
import com.petsafety.app.data.model.Breed
import com.petsafety.app.data.network.model.CreatePetRequest
import com.petsafety.app.data.network.model.UpdatePetRequest
import com.petsafety.app.ui.components.PhotoCaptureSheet
import com.petsafety.app.ui.components.SearchableDropdown
import com.petsafety.app.ui.components.SimpleDropdown
import com.petsafety.app.ui.theme.BackgroundLight
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.MutedTextLight
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.PetsViewModel

@Composable
fun PetFormScreen(
    viewModel: PetsViewModel,
    appStateViewModel: AppStateViewModel,
    petId: String? = null,
    onDone: () -> Unit
) {
    val pets by viewModel.pets.collectAsState()
    val breeds by viewModel.breeds.collectAsState()
    val existing = pets.firstOrNull { it.id == petId }
    val isEditMode = petId != null
    val defaultSpecies = stringResource(R.string.default_species_dog)
    val speciesOptions = listOf(
        stringResource(R.string.default_species_dog),
        stringResource(R.string.species_cat)
    )

    var name by remember { mutableStateOf(existing?.name ?: "") }
    var species by remember(existing?.species, defaultSpecies) {
        mutableStateOf(existing?.species ?: defaultSpecies)
    }
    var selectedBreed by remember { mutableStateOf<Breed?>(null) }
    var breedText by remember { mutableStateOf(existing?.breed ?: "") }
    var color by remember { mutableStateOf(existing?.color ?: "") }
    var microchipNumber by remember { mutableStateOf(existing?.microchipNumber ?: "") }
    var medicalNotes by remember { mutableStateOf(existing?.medicalNotes ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var capturedPhotoBytes by remember { mutableStateOf<ByteArray?>(null) }
    var showPhotoSheet by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var isNeutered by remember { mutableStateOf(false) }

    val petCreatedMessage = stringResource(R.string.pet_created)
    val petCreateFailedMessage = stringResource(R.string.pet_create_failed)
    val petUpdatedMessage = stringResource(R.string.pet_updated)
    val petUpdateFailedMessage = stringResource(R.string.pet_update_failed)
    val petDeletedMessage = stringResource(R.string.pet_deleted, existing?.name ?: "Pet")
    val deleteFailedMessage = stringResource(R.string.delete_pet_failed)

    LaunchedEffect(species) {
        viewModel.fetchBreeds(species)
        if (existing == null || existing.species != species) {
            selectedBreed = null
            breedText = ""
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundLight)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Title
            Text(
                text = if (isEditMode) stringResource(R.string.edit_pet) else stringResource(R.string.add_pet),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Photo Section
            FormSection(title = "Photo") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showPhotoSheet = true }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF2F2F7)),
                        contentAlignment = Alignment.Center
                    ) {
                        when {
                            capturedPhotoBytes != null -> {
                                // Show captured photo preview would need bitmap conversion
                                Icon(
                                    imageVector = Icons.Default.CameraAlt,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = TealAccent
                                )
                            }
                            existing?.profileImage != null -> {
                                AsyncImage(
                                    model = existing.profileImage,
                                    contentDescription = existing.name,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }
                            else -> {
                                Icon(
                                    imageVector = Icons.Default.Pets,
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MutedTextLight
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditMode) "Edit Photo" else "Select Photo",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (capturedPhotoBytes != null) "Photo selected" else "Choose a photo of your pet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MutedTextLight
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = Color(0xFFC7C7CC)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Basic Information Section
            FormSection(title = "Basic Information") {
                FormTextField(
                    label = "Name",
                    value = name,
                    onValueChange = { name = it },
                    placeholder = "Pet Name"
                )

                if (isEditMode) {
                    FormReadOnlyField(
                        label = "Species",
                        value = species.replaceFirstChar { it.uppercase() }
                    )
                } else {
                    FormDropdown(
                        label = "Species",
                        items = speciesOptions,
                        selectedItem = species,
                        onItemSelected = { species = it }
                    )
                }

                FormBreedField(
                    breeds = breeds,
                    selectedBreed = selectedBreed,
                    breedText = breedText,
                    onBreedSelected = { breed ->
                        selectedBreed = breed
                        breedText = breed.name
                    }
                )

                FormTextField(
                    label = "Colour",
                    value = color,
                    onValueChange = { color = it },
                    placeholder = "Colour (optional)"
                )

                FormTextField(
                    label = "Microchip",
                    value = microchipNumber,
                    onValueChange = { microchipNumber = it },
                    placeholder = "Microchip Number (optional)"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Physical Details Section
            FormSection(title = "Physical Details") {
                FormToggle(
                    label = "Neutered/Spayed",
                    checked = isNeutered,
                    onCheckedChange = { isNeutered = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Health Information Section
            FormSection(title = "Health Information") {
                FormTextArea(
                    value = medicalNotes,
                    onValueChange = { medicalNotes = it },
                    placeholder = "Medical notes (conditions, surgeries, etc.)"
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Additional Information Section
            FormSection(title = "Additional Information") {
                FormTextArea(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = "Behavior notes (temperament, training, etc.)"
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    val breedValue = selectedBreed?.name ?: breedText.ifBlank { null }
                    if (petId == null) {
                        viewModel.createPet(
                            CreatePetRequest(
                                name = name,
                                species = species,
                                breed = breedValue,
                                color = color.ifBlank { null },
                                microchipNumber = microchipNumber.ifBlank { null },
                                medicalNotes = medicalNotes.ifBlank { null },
                                notes = notes.ifBlank { null }
                            )
                        ) { pet, message ->
                            if (pet != null) {
                                uploadPhotoIfNeeded(viewModel, capturedPhotoBytes, pet.id)
                                appStateViewModel.showSuccess(petCreatedMessage)
                                onDone()
                            } else {
                                appStateViewModel.showError(message ?: petCreateFailedMessage)
                            }
                        }
                    } else {
                        viewModel.updatePet(
                            petId,
                            UpdatePetRequest(
                                name = name,
                                breed = breedValue,
                                color = color.ifBlank { null },
                                microchipNumber = microchipNumber.ifBlank { null },
                                medicalNotes = medicalNotes.ifBlank { null },
                                notes = notes.ifBlank { null }
                            )
                        ) { success, message ->
                            if (success) {
                                uploadPhotoIfNeeded(viewModel, capturedPhotoBytes, petId)
                                appStateViewModel.showSuccess(petUpdatedMessage)
                                onDone()
                            } else {
                                appStateViewModel.showError(message ?: petUpdateFailedMessage)
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .shadow(
                        elevation = 8.dp,
                        shape = RoundedCornerShape(16.dp),
                        ambientColor = BrandOrange.copy(alpha = 0.3f),
                        spotColor = BrandOrange.copy(alpha = 0.3f)
                    ),
                enabled = name.isNotBlank(),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
            ) {
                Text(
                    text = "Save Changes",
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cancel Button
            TextButton(
                onClick = onDone,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = stringResource(R.string.cancel),
                    color = MutedTextLight
                )
            }

            // Delete Button (edit mode only)
            if (isEditMode && existing != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = { showDeleteDialog = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.Red.copy(alpha = 0.1f),
                        contentColor = Color.Red
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Delete Pet",
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                    )
                }
            }

            Spacer(modifier = Modifier.height(100.dp))
        }
    }

    // Photo Capture Sheet
    if (showPhotoSheet) {
        PhotoCaptureSheet(
            onDismiss = { showPhotoSheet = false },
            onPhotoSelected = { bytes ->
                capturedPhotoBytes = bytes
                showPhotoSheet = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (showDeleteDialog && existing != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete ${existing.name}?") },
            text = {
                Text("Are you sure you want to delete this pet? This action cannot be undone.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deletePet(existing.id) { success, message ->
                            if (success) {
                                appStateViewModel.showSuccess(petDeletedMessage)
                                onDone()
                            } else {
                                appStateViewModel.showError(message ?: deleteFailedMessage)
                            }
                        }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = Color.Red)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun FormSection(
    title: String,
    content: @Composable () -> Unit
) {
    Column {
        Text(
            text = title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.5.sp
            ),
            color = MutedTextLight,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                content()
            }
        }
    }
}

@Composable
private fun FormTextField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(90.dp)
        )
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, color = MutedTextLight) },
            modifier = Modifier.weight(1f),
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                unfocusedBorderColor = Color.Transparent,
                focusedBorderColor = Color.Transparent
            )
        )
    }
}

@Composable
private fun FormReadOnlyField(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(90.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun FormDropdown(
    label: String,
    items: List<String>,
    selectedItem: String,
    onItemSelected: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(90.dp)
        )
        SimpleDropdown(
            items = items,
            selectedItem = selectedItem,
            onItemSelected = onItemSelected,
            label = "",
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun FormBreedField(
    breeds: List<Breed>,
    selectedBreed: Breed?,
    breedText: String,
    onBreedSelected: (Breed) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Breed",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(90.dp)
        )
        SearchableDropdown(
            items = breeds,
            selectedItem = selectedBreed,
            onItemSelected = onBreedSelected,
            itemToString = { it.name },
            label = "",
            modifier = Modifier.weight(1f),
            placeholder = "Search breed"
        )
    }
}

@Composable
private fun FormToggle(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = TealAccent
            )
        )
    }
}

@Composable
private fun FormTextArea(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, color = MutedTextLight) },
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = Color(0xFFE5E5EA),
            focusedBorderColor = TealAccent
        ),
        shape = RoundedCornerShape(8.dp)
    )
}

private fun uploadPhotoIfNeeded(
    viewModel: PetsViewModel,
    photoBytes: ByteArray?,
    petId: String?
) {
    if (photoBytes == null || petId == null) return
    viewModel.uploadPhoto(petId, photoBytes) { _, _ -> }
}
