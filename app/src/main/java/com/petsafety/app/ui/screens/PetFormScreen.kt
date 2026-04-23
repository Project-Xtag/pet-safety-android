package com.petsafety.app.ui.screens

import com.petsafety.app.util.InputValidators
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
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
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import com.petsafety.app.R
import com.petsafety.app.data.model.BreedData
import com.petsafety.app.data.model.LocalBreed
import com.petsafety.app.data.network.model.CreatePetRequest
import com.petsafety.app.data.network.model.UpdatePetRequest
import com.petsafety.app.ui.components.PhotoCaptureSheet
import com.petsafety.app.ui.components.SearchableDropdown
import com.petsafety.app.ui.components.SimpleDropdown
import androidx.compose.ui.text.style.TextAlign
import com.petsafety.app.ui.components.BrandButton
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.util.AdaptiveLayout
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.PetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PetFormScreen(
    viewModel: PetsViewModel,
    appStateViewModel: AppStateViewModel,
    petId: String? = null,
    initialPetName: String? = null,
    remainingPetNames: List<String> = emptyList(),
    onRegisterNextPet: ((String) -> Unit)? = null,
    onAllDone: (() -> Unit)? = null,
    onBack: () -> Unit = {},
    onDone: () -> Unit
) {
    val pets by viewModel.pets.collectAsState()
    val upgradePromptInfo by viewModel.showUpgradePrompt.collectAsState()
    val existing = pets.firstOrNull { it.id == petId }
    val context = LocalContext.current
    val isEditMode = petId != null
    val defaultSpecies = stringResource(R.string.default_species_dog)
    val speciesOptions = listOf(
        stringResource(R.string.default_species_dog),
        stringResource(R.string.species_cat)
    )

    var name by remember { mutableStateOf(existing?.name ?: initialPetName ?: "") }
    var species by remember(existing?.species, defaultSpecies) {
        mutableStateOf(existing?.species ?: defaultSpecies)
    }
    var selectedBreed by remember { mutableStateOf<LocalBreed?>(null) }
    var breedText by remember { mutableStateOf(existing?.breed ?: "") }
    var hasDob by remember { mutableStateOf(existing?.dateOfBirth != null) }
    var dobMillis by remember {
        mutableStateOf(
            existing?.dateOfBirth?.let {
                try { java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).parse(it)?.time } catch (_: Exception) { null }
            }
        )
    }
    var dobIsApproximate by remember { mutableStateOf(existing?.dobIsApproximate ?: false) }
    var showDatePicker by remember { mutableStateOf(false) }
    var color by remember { mutableStateOf(existing?.color ?: "") }
    var microchipNumber by remember { mutableStateOf(existing?.microchipNumber ?: "") }
    var medicalNotes by remember { mutableStateOf(existing?.medicalNotes ?: "") }
    var allergies by remember { mutableStateOf(existing?.allergies ?: "") }
    var medications by remember { mutableStateOf(existing?.medications ?: "") }
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

    val localBreeds = remember(species) {
        BreedData.breedsFor(species, context)
    }

    LaunchedEffect(species) {
        if (existing == null || existing.species != species) {
            selectedBreed = null
            breedText = ""
        }
    }

    var showPostSaveScreen by remember { mutableStateOf(false) }
    var savedPetName by remember { mutableStateOf("") }

    if (showPostSaveScreen) {
        PostSaveScreen(
            petName = savedPetName,
            remainingPetNames = remainingPetNames,
            onRegisterNextPet = onRegisterNextPet,
            onAllDone = onAllDone ?: onDone
        )
        return
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .imePadding()
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

            // Profile completion warning
            if (isEditMode && existing != null && existing.qrCode != null &&
                existing.color.isNullOrBlank() && existing.weight == null) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFFBEB)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = Color(0xFFD97706)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.profile_incomplete_warning),
                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = Color(0xFF92400E)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(R.string.profile_incomplete_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = Color(0xFFB45309)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Identity fields info banner (warning on create, locked notice on edit)
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isEditMode) Color(0xFFEFF6FF) else Color(0xFFFFF7ED)
                )
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Lock else Icons.Default.Info,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                        tint = if (isEditMode) Color(0xFF2563EB) else Color(0xFFD97706)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = stringResource(if (isEditMode) R.string.identity_locked_title else R.string.identity_will_lock_title),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = if (isEditMode) Color(0xFF1E40AF) else Color(0xFF92400E)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(if (isEditMode) R.string.identity_locked_desc else R.string.identity_will_lock_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = if (isEditMode) Color(0xFF1D4ED8) else Color(0xFFB45309)
                        )
                    }
                }
            }
            // Tag activation context banner
            if (!initialPetName.isNullOrBlank()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BrandOrange.copy(alpha = 0.1f), RoundedCornerShape(12.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = BrandOrange,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.setting_up_tag_for, initialPetName),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

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
                                val bitmap = remember(capturedPhotoBytes) {
                                    android.graphics.BitmapFactory.decodeByteArray(
                                        capturedPhotoBytes, 0, capturedPhotoBytes!!.size
                                    )
                                }
                                if (bitmap != null) {
                                    Image(
                                        bitmap = bitmap.asImageBitmap(),
                                        contentDescription = stringResource(R.string.photo_selected_hint),
                                        modifier = Modifier.fillMaxSize(),
                                        contentScale = ContentScale.Crop
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = null,
                                        modifier = Modifier.size(40.dp),
                                        tint = TealAccent
                                    )
                                }
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
                if (isEditMode) {
                    // Name locked after registration
                    FormReadOnlyField(
                        label = stringResource(R.string.name),
                        value = name
                    )
                } else {
                    FormTextField(
                        label = stringResource(R.string.name),
                        value = name,
                        onValueChange = { name = it.take(InputValidators.MAX_PET_NAME) },
                        placeholder = stringResource(R.string.pet_name)
                    )
                }

                if (isEditMode) {
                    // Species locked after registration
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

                if (isEditMode) {
                    // Breed locked after registration
                    FormReadOnlyField(
                        label = stringResource(R.string.breed),
                        value = breedText.ifBlank { "-" }
                    )
                } else {
                    FormBreedField(
                        breeds = localBreeds,
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
                                breedText = breed.localizedName
                            }
                        },
                        otherBreedLabel = otherBreedLabel
                    )

                    if (isOtherBreed) {
                        FormTextField(
                            label = stringResource(R.string.breed_custom_label),
                            value = customBreedText,
                            onValueChange = { customBreedText = it.take(InputValidators.MAX_BREED) },
                            placeholder = stringResource(R.string.breed_custom_hint)
                        )
                    }
                }

                // Date of Birth toggle + picker
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.dob_set_date), style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = hasDob, onCheckedChange = { hasDob = it })
                }

                if (hasDob) {
                    val dobDisplay = dobMillis?.let {
                        java.text.SimpleDateFormat("dd MMM yyyy", java.util.Locale.getDefault()).format(java.util.Date(it))
                    } ?: stringResource(R.string.dob_select_date)

                    OutlinedButton(
                        onClick = { showDatePicker = true },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(dobDisplay)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = dobIsApproximate, onCheckedChange = { dobIsApproximate = it })
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.dob_approximate), style = MaterialTheme.typography.bodySmall)
                    }
                }

                if (showDatePicker) {
                    // Lower-bound the DOB picker to 100 years ago. Previously
                    // only upper-bounded at "now", which meant users could
                    // scroll back to the 1800s — the backend accepts the
                    // date but downstream age calculations produce absurd
                    // values and the "pet too old" sanity heuristic trips.
                    val maxMillis = System.currentTimeMillis()
                    val minMillis = maxMillis - (100L * 365L * 24L * 60L * 60L * 1000L)
                    val datePickerState = rememberDatePickerState(
                        initialSelectedDateMillis = dobMillis ?: maxMillis,
                        selectableDates = object : SelectableDates {
                            override fun isSelectableDate(utcTimeMillis: Long): Boolean =
                                utcTimeMillis in minMillis..maxMillis
                        }
                    )
                    DatePickerDialog(
                        onDismissRequest = { showDatePicker = false },
                        confirmButton = {
                            TextButton(onClick = {
                                dobMillis = datePickerState.selectedDateMillis
                                showDatePicker = false
                            }) { Text(stringResource(R.string.ok)) }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDatePicker = false }) { Text(stringResource(R.string.cancel)) }
                        }
                    ) {
                        DatePicker(state = datePickerState)
                    }
                }

                FormTextField(
                    label = stringResource(R.string.colour),
                    value = color,
                    onValueChange = { color = it.take(InputValidators.MAX_COLOR) },
                    placeholder = stringResource(R.string.colour_optional)
                )

                FormTextField(
                    label = stringResource(R.string.microchip),
                    value = microchipNumber,
                    onValueChange = { microchipNumber = it.take(InputValidators.MAX_MICROCHIP) },
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
                    onValueChange = { medicalNotes = it.take(InputValidators.MAX_MEDICAL_NOTES) },
                    placeholder = stringResource(R.string.medical_notes_hint)
                )

                Spacer(modifier = Modifier.height(16.dp))

                FormTextArea(
                    value = allergies,
                    onValueChange = { allergies = it.take(InputValidators.MAX_ALLERGIES) },
                    placeholder = stringResource(R.string.allergies_hint)
                )

                Spacer(modifier = Modifier.height(16.dp))

                FormTextArea(
                    value = medications,
                    onValueChange = { medications = it.take(InputValidators.MAX_MEDICATIONS) },
                    placeholder = stringResource(R.string.medications_hint)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Additional Information Section
            FormSection(title = stringResource(R.string.additional_information)) {
                FormTextArea(
                    value = uniqueFeatures,
                    onValueChange = { uniqueFeatures = it.take(InputValidators.MAX_UNIQUE_FEATURES) },
                    placeholder = stringResource(R.string.unique_features_hint)
                )

                Spacer(modifier = Modifier.height(12.dp))

                FormTextArea(
                    value = notes,
                    onValueChange = { notes = it.take(InputValidators.MAX_NOTES) },
                    placeholder = stringResource(R.string.behavior_notes_hint)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Save Button
            Button(
                onClick = {
                    val breedValue = if (isOtherBreed) customBreedText.ifBlank { null } else selectedBreed?.englishName ?: breedText.ifBlank { null }
                    if (petId == null) {
                        val dobStr = if (hasDob && dobMillis != null) java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(dobMillis!!)) else null
                        viewModel.createPet(
                            CreatePetRequest(
                                name = name,
                                species = species,
                                breed = breedValue,
                                color = color.ifBlank { null },
                                weight = weight.toDoubleOrNull(),
                                microchipNumber = microchipNumber.ifBlank { null },
                                medicalNotes = medicalNotes.ifBlank { null },
                                allergies = allergies.ifBlank { null },
                                medications = medications.ifBlank { null },
                                notes = notes.ifBlank { null },
                                uniqueFeatures = uniqueFeatures.ifBlank { null },
                                sex = sex.ifBlank { null },
                                isNeutered = isNeutered,
                                dateOfBirth = dobStr,
                                dobIsApproximate = if (hasDob) dobIsApproximate else null
                            )
                        ) { pet, message ->
                            if (pet != null) {
                                uploadPhotoIfNeeded(viewModel, capturedPhotoBytes, pet.id)
                                appStateViewModel.showSuccess(petCreatedMessage)
                                // Show post-save screen in tag activation context, otherwise just dismiss
                                if (onRegisterNextPet != null || onAllDone != null) {
                                    savedPetName = name
                                    showPostSaveScreen = true
                                } else {
                                    onDone()
                                }
                            } else if (message != null) {
                                appStateViewModel.showError(message)
                            }
                            // If both null, pet limit dialog handles it
                        }
                    } else {
                        val editDobStr = if (hasDob && dobMillis != null) java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.US).format(java.util.Date(dobMillis!!)) else null
                        viewModel.updatePet(
                            petId,
                            UpdatePetRequest(
                                name = name,
                                breed = breedValue,
                                color = color.ifBlank { null },
                                weight = weight.toDoubleOrNull(),
                                microchipNumber = microchipNumber.ifBlank { null },
                                medicalNotes = medicalNotes.ifBlank { null },
                                allergies = allergies.ifBlank { null },
                                medications = medications.ifBlank { null },
                                notes = notes.ifBlank { null },
                                uniqueFeatures = uniqueFeatures.ifBlank { null },
                                sex = sex.ifBlank { null },
                                isNeutered = isNeutered,
                                dateOfBirth = editDobStr,
                                dobIsApproximate = if (hasDob) dobIsApproximate else null
                            )
                        ) { success, message ->
                            if (success) {
                                uploadPhotoIfNeeded(viewModel, capturedPhotoBytes, petId)
                                viewModel.fetchPets() // Refresh pet list so detail view shows updated data
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
            title = { Text(stringResource(R.string.delete_pet_warning_title, existing.name)) },
            text = {
                Text(stringResource(R.string.delete_pet_warning_message, existing.name))
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
                    Text(stringResource(R.string.delete_pet_warning_confirm))
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
        AlertDialog(
            onDismissRequest = { viewModel.dismissUpgradePrompt() },
            title = { Text(stringResource(R.string.pet_limit_reached)) },
            text = {
                Text(
                    stringResource(
                        R.string.pet_limit_reached_message,
                        info.currentPlan.replaceFirstChar { it.uppercase() },
                        info.maxPets,
                        info.upgradeTo.replaceFirstChar { it.uppercase() },
                        info.upgradePrice
                    )
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
                    Text(stringResource(R.string.upgrade_to_plan, info.upgradeTo.replaceFirstChar { it.uppercase() }))
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
    breeds: List<LocalBreed>,
    selectedBreed: LocalBreed?,
    breedText: String,
    onBreedSelected: (LocalBreed) -> Unit,
    otherBreedLabel: String = "Other"
) {
    val breedsWithOther = remember(breeds) {
        breeds + LocalBreed(id = "other", localizedName = otherBreedLabel, englishName = "Other", species = "")
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
            itemToString = { it.localizedName },
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

// MARK: - Post-Save Screen (tag activation flow)
@Composable
private fun PostSaveScreen(
    petName: String,
    remainingPetNames: List<String>,
    onRegisterNextPet: ((String) -> Unit)?,
    onAllDone: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = null,
            modifier = Modifier.size(70.dp),
            tint = TealAccent
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = stringResource(R.string.tag_activated_for, petName),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.pet_now_protected),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 24.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))

        if (remainingPetNames.isNotEmpty()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = BrandOrange
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.more_tags_to_setup),
                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            remainingPetNames.forEach { nextName ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onRegisterNextPet?.invoke(nextName) },
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandOrange.copy(alpha = 0.08f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = BrandOrange
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.setup_pet_tag, nextName),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            imageVector = Icons.Default.ChevronRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        } else {
            Text(
                text = stringResource(R.string.thank_you_senra),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.whats_next),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

            NextStepActionCard(text = stringResource(R.string.choose_subscription_plan), onClick = onAllDone)
            Spacer(modifier = Modifier.height(8.dp))
            NextStepActionCard(text = stringResource(R.string.update_contact_details), onClick = onAllDone)
        }

        Spacer(modifier = Modifier.height(24.dp))

        BrandButton(
            text = stringResource(R.string.go_to_home),
            onClick = onAllDone,
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(32.dp))
    }
}

@Composable
private fun NextStepActionCard(text: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.ChevronRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
