package com.petsafety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.R
import com.petsafety.app.data.network.model.CreatePetRequest
import com.petsafety.app.ui.viewmodel.ShelterPromoClaimViewModel

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

    // Form fields
    var petName by remember { mutableStateOf("") }
    var species by remember { mutableStateOf("") }
    var breed by remember { mutableStateOf("") }
    var color by remember { mutableStateOf("") }
    var sex by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.loadPets()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Claim Tag") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
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
                        "Tag Claimed!",
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
                                Text("Pet: ${state.response.pet?.name ?: ""}", fontWeight = FontWeight.Medium)
                                Text("Tag: ${state.response.tag?.qrCode ?: ""}", style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "Free Standard plan for ${details.promoDurationMonths} months",
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
                        Text("Done")
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
                                    "Welcome from $shelterName!",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    "Register your pet to get $promoDurationMonths months free Standard plan.",
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
                                label = { Text("New Pet") },
                                modifier = Modifier.weight(1f).padding(end = 4.dp)
                            )
                            FilterChip(
                                selected = useExistingPet,
                                onClick = { useExistingPet = true },
                                label = { Text("Existing Pet") },
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
                            Text("Claim Tag")
                        }
                    } else {
                        // New pet form
                        OutlinedTextField(
                            value = petName,
                            onValueChange = { petName = it },
                            label = { Text("Name *") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Species dropdown
                        var speciesExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = speciesExpanded,
                            onExpandedChange = { speciesExpanded = !speciesExpanded }
                        ) {
                            OutlinedTextField(
                                value = species.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Species *") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = speciesExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = speciesExpanded,
                                onDismissRequest = { speciesExpanded = false }
                            ) {
                                listOf("dog" to "Dog", "cat" to "Cat", "bird" to "Bird", "rabbit" to "Rabbit", "other" to "Other").forEach { (value, label) ->
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
                            label = { Text("Breed") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = color,
                            onValueChange = { color = it },
                            label = { Text("Color") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        // Sex dropdown
                        var sexExpanded by remember { mutableStateOf(false) }
                        ExposedDropdownMenuBox(
                            expanded = sexExpanded,
                            onExpandedChange = { sexExpanded = !sexExpanded }
                        ) {
                            OutlinedTextField(
                                value = sex.replaceFirstChar { it.uppercase() },
                                onValueChange = {},
                                readOnly = true,
                                label = { Text("Sex") },
                                trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sexExpanded) },
                                modifier = Modifier.menuAnchor().fillMaxWidth()
                            )
                            ExposedDropdownMenu(
                                expanded = sexExpanded,
                                onDismissRequest = { sexExpanded = false }
                            ) {
                                listOf("male" to "Male", "female" to "Female").forEach { (value, label) ->
                                    DropdownMenuItem(
                                        text = { Text(label) },
                                        onClick = { sex = value; sexExpanded = false }
                                    )
                                }
                            }
                        }

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
                                viewModel.claimWithNewPet(
                                    qrCode,
                                    CreatePetRequest(
                                        name = petName,
                                        species = species,
                                        breed = breed.ifBlank { null },
                                        color = color.ifBlank { null },
                                        sex = sex.ifBlank { null }
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
                            Text("Register Pet & Claim Tag")
                        }
                    }
                }
            }
        }
    }
}
