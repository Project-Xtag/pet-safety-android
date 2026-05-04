package com.petsafety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.R
import com.petsafety.app.data.network.model.CreatePetRequest
import com.petsafety.app.ui.viewmodel.ShelterPromoClaimViewModel
import com.petsafety.app.util.InputValidators

private val BrandOrange = Color(0xFFFF914D)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShelterPromoClaimScreen(
    qrCode: String,
    shelterName: String,
    promoDurationMonths: Int,
    onDone: () -> Unit,
    onBack: () -> Unit
) {
    val viewModel: ShelterPromoClaimViewModel = hiltViewModel()
    val claimState by viewModel.claimState.collectAsState()
    val pets by viewModel.pets.collectAsState()
    val isLoadingPets by viewModel.isLoadingPets.collectAsState()

    var useExistingPet by remember { mutableStateOf(false) }
    var selectedPetId by remember { mutableStateOf<String?>(null) }

    // Form fields. Pre-fix this screen only collected name/species/breed/
    // color/sex; the iOS PetFormView and the standard Android PetFormScreen
    // both capture the full pet model on creation, so a shelter user
    // claiming via QR was forced into a create-then-immediately-edit flow
    // to capture microchip / weight / DOB / medical / behavioural fields.
    // M1 — bring this screen to the same field set the backend's
    // claimPromoSchema accepts.
    var petName by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }
    var dateOfBirth by remember { mutableStateOf("") }
    var dobIsApproximate by remember { mutableStateOf(false) }
    var weight by remember { mutableStateOf("") }
    var microchipNumber by remember { mutableStateOf("") }
    var isNeutered by remember { mutableStateOf(false) }
    var medicalNotes by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var medications by remember { mutableStateOf("") }
    var uniqueFeatures by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadPets()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.shelter_promo_claim_tag_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.back))
                    }
                }
            )
        }
    ) { padding ->
        when (val state = claimState) {
            is ShelterPromoClaimViewModel.ClaimState.Success -> {
                // Success screen
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF4CAF50),
                        modifier = Modifier.size(72.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.shelter_promo_claimed_title),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        state.response.message,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    state.response.promoDetails?.let { details ->
                        Spacer(modifier = Modifier.height(16.dp))
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFFE8F5E9))
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    stringResource(R.string.shelter_promo_pet_label_format, state.response.pet?.name ?: ""),
                                    fontWeight = FontWeight.Medium
                                )
                                Text(
                                    stringResource(R.string.shelter_promo_tag_label_format, state.response.tag?.qrCode ?: ""),
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    stringResource(R.string.shelter_promo_free_plan_format, details.promoDurationMonths),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = BrandOrange
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = onDone,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                    ) {
                        Text(stringResource(R.string.shelter_promo_done))
                    }
                }
            }
            else -> {
                // Claim form
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp)
                ) {
                    // Welcome banner
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandOrange.copy(alpha = 0.1f))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Favorite, contentDescription = null, tint = BrandOrange)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    stringResource(R.string.shelter_promo_welcome_format, shelterName),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    stringResource(R.string.shelter_promo_register_subtitle_format, promoDurationMonths),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Toggle new/existing pet
                    if (pets.isNotEmpty()) {
                        Row(modifier = Modifier.fillMaxWidth()) {
                            FilterChip(
                                selected = !useExistingPet,
                                onClick = { useExistingPet = false },
                                label = { Text(stringResource(R.string.shelter_promo_new_pet)) },
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            )
                            FilterChip(
                                selected = useExistingPet,
                                onClick = { useExistingPet = true },
                                label = { Text(stringResource(R.string.shelter_promo_existing_pet)) },
                                modifier = Modifier.weight(1f).padding(start = 4.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    if (useExistingPet) {
                        // Pet selection
                        pets.forEach { pet ->
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                                    .clickable { selectedPetId = pet.id },
                                colors = CardDefaults.cardColors(
                                    containerColor = if (selectedPetId == pet.id) BrandOrange.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(16.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Default.Pets, contentDescription = null)
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(pet.name, fontWeight = FontWeight.Medium)
                                        Text(pet.species ?: "", style = MaterialTheme.typography.bodySmall)
                                    }
                                    if (selectedPetId == pet.id) {
                                        Icon(Icons.Default.CheckCircle, contentDescription = null, tint = BrandOrange)
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = {
                                selectedPetId?.let { viewModel.claimWithExistingPet(qrCode, it) }
                            },
                            enabled = selectedPetId != null && state !is ShelterPromoClaimViewModel.ClaimState.Loading,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                        ) {
                            if (state is ShelterPromoClaimViewModel.ClaimState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Text(stringResource(R.string.shelter_promo_claim_tag_button))
                        }
                    } else {
                        // New pet form
                        OutlinedTextField(
                            value = petName,
                            onValueChange = { petName = it },
                            label = { Text(stringResource(R.string.shelter_promo_pet_name_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Species dropdown
                        var speciesExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = speciesExpanded,
                            onExpandedChange = { speciesExpanded = !speciesExpanded }
                        ) {
                            val speciesDogLabel = stringResource(R.string.species_dog)
                            val speciesCatLabel = stringResource(R.string.species_cat)
                            val speciesBirdLabel = stringResource(R.string.species_bird)
                            val speciesRabbitLabel = stringResource(R.string.species_rabbit)
                            val speciesOtherLabel = stringResource(R.string.species_other)
                            val speciesDisplay = when (species) {
                                "dog" -> speciesDogLabel
                                "cat" -> speciesCatLabel
                                "bird" -> speciesBirdLabel
                                "rabbit" -> speciesRabbitLabel
                                "other" -> speciesOtherLabel
                                else -> ""
                            }
                            OutlinedTextField(
                                value = speciesDisplay,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.shelter_promo_species_label)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = speciesExpanded,
                                onDismissRequest = { speciesExpanded = false }
                            ) {
                                listOf(
                                    "dog" to speciesDogLabel,
                                    "cat" to speciesCatLabel,
                                    "bird" to speciesBirdLabel,
                                    "rabbit" to speciesRabbitLabel,
                                    "other" to speciesOtherLabel
                                ).forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { species = value; speciesExpanded = false }
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = breed,
                            onValueChange = { breed = it },
                            label = { Text(stringResource(R.string.shelter_promo_breed_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = { Text(stringResource(R.string.shelter_promo_color_label)) },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Sex dropdown
                        var sexExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = sexExpanded,
                            onExpandedChange = { sexExpanded = !sexExpanded }
                        ) {
                            val sexMaleLabel = stringResource(R.string.sex_male)
                            val sexFemaleLabel = stringResource(R.string.sex_female)
                            val sexDisplay = when (sex) {
                                "male" -> sexMaleLabel
                                "female" -> sexFemaleLabel
                                else -> ""
                            }
                            OutlinedTextField(
                                value = sexDisplay,
                                onValueChange = {},
                                readOnly = true,
                                label = { Text(stringResource(R.string.shelter_promo_sex_label)) },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = sexExpanded,
                                onDismissRequest = { sexExpanded = false }
                            ) {
                                listOf(
                                    "male" to sexMaleLabel,
                                    "female" to sexFemaleLabel
                                ).forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { sex = value; sexExpanded = false }
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Date of birth — text input (YYYY-MM-DD). The
                        // standard PetFormScreen uses a DatePickerDialog;
                        // keeping this lighter here so the shelter-claim
                        // path stays low-friction. Backend accepts both.
                        OutlinedTextField(
                            value = dateOfBirth,
                            onValueChange = { dateOfBirth = it.take(10) },
                            label = { Text(stringResource(R.string.date_of_birth)) },
                            placeholder = { Text("YYYY-MM-DD") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = dobIsApproximate,
                                onCheckedChange = { dobIsApproximate = it }
                            )
                            Text(stringResource(R.string.dob_approximate))
                        }
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = weight,
                            onValueChange = { new -> weight = new.filter { it.isDigit() || it == '.' || it == ',' } },
                            label = { Text(stringResource(R.string.weight)) },
                            placeholder = { Text(stringResource(R.string.weight_optional)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = microchipNumber,
                            onValueChange = { microchipNumber = it.take(InputValidators.MAX_MICROCHIP) },
                            label = { Text(stringResource(R.string.microchip)) },
                            placeholder = { Text(stringResource(R.string.microchip_optional)) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Checkbox(
                                checked = isNeutered,
                                onCheckedChange = { isNeutered = it }
                            )
                            Text(stringResource(R.string.neutered_spayed))
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        // Health information
                        OutlinedTextField(
                            value = medicalNotes,
                            onValueChange = { medicalNotes = it.take(InputValidators.MAX_MEDICAL_NOTES) },
                            label = { Text(stringResource(R.string.medical_notes)) },
                            placeholder = { Text(stringResource(R.string.medical_notes_hint)) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = allergies,
                            onValueChange = { allergies = it.take(InputValidators.MAX_ALLERGIES) },
                            label = { Text(stringResource(R.string.allergies)) },
                            placeholder = { Text(stringResource(R.string.allergies_hint)) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = medications,
                            onValueChange = { medications = it.take(InputValidators.MAX_MEDICATIONS) },
                            label = { Text(stringResource(R.string.medications)) },
                            placeholder = { Text(stringResource(R.string.medications_hint)) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = uniqueFeatures,
                            onValueChange = { uniqueFeatures = it.take(InputValidators.MAX_UNIQUE_FEATURES) },
                            label = { Text(stringResource(R.string.unique_features)) },
                            placeholder = { Text(stringResource(R.string.unique_features_hint)) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = notes,
                            onValueChange = { notes = it.take(InputValidators.MAX_NOTES) },
                            label = { Text(stringResource(R.string.behavior_notes)) },
                            placeholder = { Text(stringResource(R.string.behavior_notes_hint)) },
                            modifier = Modifier.fillMaxWidth().heightIn(min = 80.dp)
                        )

                        Spacer(modifier = Modifier.height(16.dp))

                        // Error
                        if (state is ShelterPromoClaimViewModel.ClaimState.Error) {
                            Text(
                                state.message,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }

                        Button(
                            onClick = {
                                val parsedWeight = weight.replace(',', '.').toDoubleOrNull()
                                viewModel.claimWithNewPet(
                                    qrCode,
                                    CreatePetRequest(
                                        name = petName,
                                        species = species,
                                        breed = breed.ifBlank { null },
                                        color = color.ifBlank { null },
                                        weight = parsedWeight,
                                        microchipNumber = microchipNumber.ifBlank { null },
                                        sex = sex.ifBlank { null },
                                        isNeutered = if (isNeutered) true else null,
                                        medicalNotes = medicalNotes.ifBlank { null },
                                        allergies = allergies.ifBlank { null },
                                        medications = medications.ifBlank { null },
                                        uniqueFeatures = uniqueFeatures.ifBlank { null },
                                        notes = notes.ifBlank { null },
                                        dateOfBirth = dateOfBirth.ifBlank { null },
                                        dobIsApproximate = if (dateOfBirth.isNotBlank() && dobIsApproximate) true else null
                                    )
                                )
                            },
                            enabled = petName.isNotBlank() && species.isNotBlank() && state !is ShelterPromoClaimViewModel.ClaimState.Loading,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
                        ) {
                            if (state is ShelterPromoClaimViewModel.ClaimState.Loading) {
                                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(8.dp))
                            }
                            Icon(Icons.Default.Pets, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.shelter_promo_register_and_claim_button))
                        }
                    }
                }
            }
        }
    }
}
