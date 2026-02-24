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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.material3.IconButton
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
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.util.AdaptiveLayout
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.PetsViewModel

@Composable
fun PetFormScreen(
    viewModel: PetsViewModel,
    appStateViewModel: AppStateViewModel,
    petId: String? = null,
    onBack: () -> Unit = {},
    onDone: () -> Unit
) {
    val pets by viewModel.pets.collectAsState()
    val breeds by viewModel.breeds.collectAsState()
    val upgradePromptInfo by viewModel.showUpgradePrompt.collectAsState()
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
    var sex by remember { mutableStateOf(existing?.sex ?: "") }
    var isNeutered by remember { mutableStateOf(existing?.isNeutered ?: false) }
    var weight by remember { mutableStateOf(existing?.weight?.let { String.format("%.1f", it) } ?: "") }
    var uniqueFeatures by remember { mutableStateOf(existing?.uniqueFeatures ?: "") }
    val sexOptions = listOf("", "Male", "Female")
    val notSpecifiedLabel = stringResource(R.string.sex_optional)
    val maleLabel = stringResource(R.string.male)
    val femaleLabel = stringResource(R.string.female)

    val petCreatedMessage = stringResource(R.string.pet_created)
    val petCreateFailedMessage = stringResource(R.string.pet_create_failed)
    val petUpdatedMessage = stringResource(R.string.pet_updated)
    val petUpdateFailedMessage = stringResource(R.string.pet_update_failed)
    val petDeletedMessage = stringResource(R.string.pet_deleted, existing?.name ?: stringResource(R.string.pet_default_name))
    val deleteFailedMessage = stringResource(R.string.delete_pet_failed)
    val cannotDeleteMissingTitle = stringResource(R.string.cannot_delete_missing_pet)
    val cannotDeleteMissingMessage = stringResource(R.string.cannot_delete_missing_pet_message)
    var showCannotDeleteAlert by remember { mutableStateOf(false) }
    var customBreedText by remember { mutableStateOf("") }
    var isOtherBreed by remember { mutableStateOf(false) }
    val otherBreedLabel = stringResource(R.string.breed_other)

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
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = AdaptiveLayout.MaxContentWidth)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Title with back button
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = stringResource(R.string.back)
                    )
                }
                Text(
                    text = if (isEditMode) stringResource(R.string.edit_pet) else stringResource(R.string.add_pet),
                    style = MaterialTheme.typography.headlineMedium.copy(
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Photo Section
            FormSection(title = stringResource(R.string.photo)) {
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
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
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
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEditMode) stringResource(R.string.edit_photo) else stringResource(R.string.select_photo),
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = if (capturedPhotoBytes != null) stringResource(R.string.photo_selected_hint) else stringResource(R.string.choose_photo_hint),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Icon(
                        imageVector = Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Basic Information Section
            FormSection(title = stringResource(R.string.basic_information)) {
                FormTextField(
                    label = stringResource(R.string.name),
                    value = name,
                    onValueChange = { name = it },
                    placeholder = stringResource(R.string.pet_name)
                )

                if (isEditMode) {
                    FormReadOnlyField(
                        label = stringResource(R.string.species),
                        value = species.replaceFirstChar { it.uppercase() }
                    )
                } else {
                    FormDropdown(
                        label = stringResource(R.string.species),
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
                        if (breed.id == "other") {
                            isOtherBreed = true
                            selectedBreed = breed
                            breedText = ""
                        } else {
                            isOtherBreed = false
                            selectedBreed = breed
                            breedText = breed.name
                        }
                    },
                    otherBreedLabel = otherBreedLabel
                )

                if (isOtherBreed) {
                    FormTextField(
                        label = stringResource(R.string.breed_custom_label),
                        value = customBreedText,
                        onValueChange = { customBreedText = it },
                        placeholder = stringResource(R.string.breed_custom_hint)
                    )
                }

                FormTextField(
                    label = stringResource(R.string.colour),
                    value = color,
                    onValueChange = { color = it },
                    placeholder = stringResource(R.string.colour_optional)
                )

                FormTextField(
                    label = stringResource(R.string.microchip),
                    value = microchipNumber,
                    onValueChange = { microchipNumber = it },
                    placeholder = stringResource(R.string.microchip_optional)
                )

                FormTextField(
                    label = stringResource(R.string.weight),
                    value = weight,
                    onValueChange = { weight = it },
                    placeholder = stringResource(R.string.weight_optional)
                )

                FormDropdown(
                    label = stringResource(R.string.sex),
                    items = sexOptions,
                    selectedItem = sex,
                    onItemSelected = { sex = it },
                    displayTransform = { when (it) { "" -> notSpecifiedLabel; "Male" -> maleLabel; "Female" -> femaleLabel; else -> it } }
                )

                FormToggle(
                    label = stringResource(R.string.neutered_spayed),
                    checked = isNeutered,
                    onCheckedChange = { isNeutered = it }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Health Information Section
            FormSection(title = stringResource(R.string.health_information)) {
                FormTextArea(
                    value = medicalNotes,
                    onValueChange = { medicalNotes = it },
                    placeholder = stringResource(R.string.medical_notes_hint)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Additional Information Section
            FormSection(title = stringResource(R.string.additional_information)) {
                FormTextArea(
                    value = uniqueFeatures,
                    onValueChange = { uniqueFeatures = it },
                    placeholder = stringResource(R.string.unique_features_hint)
                )

                Spacer(modifier = Modifier.height(12.dp))

                FormTextArea(
                    value = notes,
                    onValueChange = { notes = it },
                    placeholder = stringResource(R.string.behavior_notes_hint)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    val breedValue = if (isOtherBreed) customBreedText.ifBlank { null } else selectedBreed?.name ?: breedText.ifBlank { null }
                    if (petId == null) {
                        viewModel.createPet(
                            CreatePetRequest(
                                name = name,
                                species = species,
                                breed = breedValue,
                                color = color.ifBlank { null },
                                weight = weight.toDoubleOrNull(),
                                microchipNumber = microchipNumber.ifBlank { null },
                                medicalNotes = medicalNotes.ifBlank { null },
                                notes = notes.ifBlank { null },
                                uniqueFeatures = uniqueFeatures.ifBlank { null },
                                sex = sex.ifBlank { null },
                                isNeutered = isNeutered
                            )
                        ) { pet, message ->
                            if (pet != null) {
                                uploadPhotoIfNeeded(viewModel, capturedPhotoBytes, pet.id)
                                appStateViewModel.showSuccess(petCreatedMessage)
                                onDone()
                            } else if (message != null) {
                                appStateViewModel.showError(message)
                            }
                            // If both null, pet limit dialog handles it
                        }
                    } else {
                        viewModel.updatePet(
                            petId,
                            UpdatePetRequest(
                                name = name,
                                breed = breedValue,
                                color = color.ifBlank { null },
                                weight = weight.toDoubleOrNull(),
                                microchipNumber = microchipNumber.ifBlank { null },
                                medicalNotes = medicalNotes.ifBlank { null },
                                notes = notes.ifBlank { null },
                                uniqueFeatures = uniqueFeatures.ifBlank { null },
                                sex = sex.ifBlank { null },
                                isNeutered = isNeutered
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
                    text = stringResource(R.string.save_changes),
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            // Delete Button (edit mode only)
            if (isEditMode && existing != null) {
                Spacer(modifier = Modifier.height(24.dp))

                Button(
                    onClick = {
                        if (existing.isMissing) {
                            showCannotDeleteAlert = true
                        } else {
                            showDeleteDialog = true
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f),
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.delete_pet_button),
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
            title = { Text(stringResource(R.string.delete_pet_dialog_title, existing.name)) },
            text = {
                Text(stringResource(R.string.delete_pet_dialog_message))
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
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    // Cannot Delete Missing Pet Dialog
    if (showCannotDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showCannotDeleteAlert = false },
            title = { Text(cannotDeleteMissingTitle) },
            text = { Text(cannotDeleteMissingMessage) },
            confirmButton = {
                TextButton(onClick = { showCannotDeleteAlert = false }) {
                    Text(stringResource(R.string.ok))
                }
            }
        )
    }

    // Pet Limit Upgrade Dialog
    upgradePromptInfo?.let { info ->
        val context = androidx.compose.ui.platform.LocalContext.current
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpgradePrompt() },
            title = { Text("Pet Limit Reached") },
            text = {
                Text(
                    "Your ${info.currentPlan.replaceFirstChar { it.uppercase() }} plan allows ${info.maxPets} pet. " +
                    "Upgrade to ${info.upgradeTo.replaceFirstChar { it.uppercase() }} for ${info.upgradePrice} to add unlimited pets."
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.dismissUpgradePrompt()
                    val intent = android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        android.net.Uri.parse("https://senra.pet/manage-subscription")
                    )
                    context.startActivity(intent)
                }) {
                    Text("Upgrade to ${info.upgradeTo.replaceFirstChar { it.uppercase() }}")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.dismissUpgradePrompt() }) {
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
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 16.dp, bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
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
            placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
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
    onItemSelected: (String) -> Unit,
    displayTransform: ((String) -> String)? = null
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
            modifier = Modifier.weight(1f),
            displayTransform = displayTransform
        )
    }
}

@Composable
private fun FormBreedField(
    breeds: List<Breed>,
    selectedBreed: Breed?,
    breedText: String,
    onBreedSelected: (Breed) -> Unit,
    otherBreedLabel: String = "Other"
) {
    val breedsWithOther = remember(breeds) {
        breeds + Breed(id = "other", name = otherBreedLabel, species = "")
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.breed),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.width(90.dp)
        )
        SearchableDropdown(
            items = breedsWithOther,
            selectedItem = selectedBreed,
            onItemSelected = onBreedSelected,
            itemToString = { it.name },
            label = "",
            modifier = Modifier.weight(1f),
            placeholder = stringResource(R.string.search_breed)
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
        placeholder = { Text(placeholder, color = MaterialTheme.colorScheme.onSurfaceVariant) },
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp),
        colors = OutlinedTextFieldDefaults.colors(
            unfocusedBorderColor = MaterialTheme.colorScheme.outline,
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
