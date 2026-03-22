package com.petsafety.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import coil.compose.AsyncImage
import com.petsafety.app.R
import com.petsafety.app.data.model.Pet
import com.petsafety.app.data.model.UnactivatedOrderItem
import com.petsafety.app.ui.components.BrandButton
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.viewmodel.ActivationState
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.TagActivationViewModel
import com.petsafety.app.ui.viewmodel.PetsViewModel
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.BorderStroke
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

@Composable
fun TagActivationScreen(
    qrCode: String,
    appStateViewModel: AppStateViewModel,
    onActivationComplete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val viewModel: TagActivationViewModel = hiltViewModel()
    val petsViewModel: PetsViewModel = hiltViewModel()
    val pets by viewModel.pets.collectAsState()
    val isLoadingPets by viewModel.isLoadingPets.collectAsState()
    val activationState by viewModel.activationState.collectAsState()
    val selectedPetId by viewModel.selectedPetId.collectAsState()

    val orderItems by viewModel.orderItems.collectAsState()

    var showCreatePetForm by remember { mutableStateOf(false) }
    var petIdsBeforeCreate by remember { mutableStateOf(emptySet<String>()) }
    var selectedPetNameForCreate by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadActivationData(qrCode)
    }

    // Show inline pet creation form
    if (showCreatePetForm) {
        PetFormScreen(
            viewModel = petsViewModel,
            appStateViewModel = appStateViewModel,
            initialPetName = selectedPetNameForCreate,
            onBack = { showCreatePetForm = false },
            onDone = {
                showCreatePetForm = false
                // Refresh pets and auto-activate the newly created pet
                viewModel.refreshAndAutoActivate(qrCode, petIdsBeforeCreate)
            }
        )
        return
    }

    // Pre-resolve localized error messages for use in LaunchedEffect
    val errorQrNotFound = stringResource(R.string.activation_error_qr_not_found)
    val errorAlreadyActivated = stringResource(R.string.activation_error_already_activated)
    val errorNotLinked = stringResource(R.string.activation_error_not_linked)
    val errorNotShipped = stringResource(R.string.activation_error_not_shipped)
    val errorNotOwner = stringResource(R.string.activation_error_not_owner)

    LaunchedEffect(activationState) {
        when (val state = activationState) {
            is ActivationState.Error -> {
                val msg = state.message
                val classified = when {
                    msg.contains("not found", ignoreCase = true) && msg.contains("QR", ignoreCase = true) -> errorQrNotFound
                    msg.contains("already activated", ignoreCase = true) -> errorAlreadyActivated
                    msg.contains("not been linked", ignoreCase = true) -> errorNotLinked
                    msg.contains("not been shipped", ignoreCase = true) -> errorNotShipped
                    msg.contains("do not own", ignoreCase = true) -> errorNotOwner
                    else -> msg
                }
                appStateViewModel.showError(classified)
                viewModel.resetActivation()
            }
            else -> {}
        }
    }

    if (activationState is ActivationState.Success) {
        val successState = activationState as ActivationState.Success
        val selectedPet = pets.firstOrNull { it.id == selectedPetId }
        val remaining = orderItems.filter { it.tagStatus != "active" }
        val context = LocalContext.current

        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))

            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = TealAccent
            )

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = stringResource(R.string.tag_activated_for, successState.petName),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = stringResource(R.string.pet_now_protected),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            selectedPet?.let { pet ->
                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(TealAccent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
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
                            contentDescription = pet.name,
                            modifier = Modifier.size(40.dp),
                            tint = TealAccent
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Remaining tags section
            if (remaining.isNotEmpty()) {
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

                remaining.forEach { item ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                // TODO: Navigate to activate this specific tag
                                viewModel.resetActivation()
                                onActivationComplete()
                            },
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
                                text = stringResource(R.string.setup_pet_tag, item.petName ?: "Pet"),
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
                // All done — show next steps
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

                NextStepCard(
                    text = stringResource(R.string.choose_subscription_plan),
                    onClick = { viewModel.resetActivation(); onActivationComplete() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                NextStepCard(
                    text = stringResource(R.string.register_your_vet),
                    onClick = { viewModel.resetActivation(); onActivationComplete() }
                )
                Spacer(modifier = Modifier.height(8.dp))
                NextStepCard(
                    text = stringResource(R.string.update_contact_details),
                    onClick = { viewModel.resetActivation(); onActivationComplete() }
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            BrandButton(
                text = stringResource(R.string.go_to_home),
                onClick = {
                    viewModel.resetActivation()
                    onActivationComplete()
                },
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    } else {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.tag_activate_title),
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(8.dp))

            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
                )
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.tag_code_label) + ": ",
                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = qrCode,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.tag_select_pet),
                style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            when {
                isLoadingPets -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = BrandOrange)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = stringResource(R.string.tag_loading_pets),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                pets.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxWidth().weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Pets,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = stringResource(R.string.tag_no_pets),
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(R.string.tag_no_pets_message),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            BrandButton(
                                text = stringResource(R.string.create_profile_first),
                                onClick = {
                                    petIdsBeforeCreate = pets.map { it.id }.toSet()
                                    selectedPetNameForCreate = null
                                    showCreatePetForm = true
                                },
                                modifier = Modifier.padding(horizontal = 40.dp)
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        if (viewModel.hasOrderContext()) {
                            val matchingPets = viewModel.getMatchingPets()
                            val unmatchedNames = viewModel.getUnmatchedOrderNames()
                            val otherPets = viewModel.getOtherPets()

                            // Section: Pets from order
                            if (matchingPets.isNotEmpty() || unmatchedNames.isNotEmpty()) {
                                item {
                                    Text(
                                        text = stringResource(R.string.pets_from_order),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                items(matchingPets, key = { it.id }) { pet ->
                                    PetSelectionCard(
                                        pet = pet,
                                        isSelected = selectedPetId == pet.id,
                                        badge = stringResource(R.string.from_your_order),
                                        onClick = { viewModel.selectPet(pet.id) }
                                    )
                                }
                                items(unmatchedNames, key = { it }) { name ->
                                    UnmatchedPetCard(
                                        petName = name,
                                        onCreateProfile = {
                                            petIdsBeforeCreate = pets.map { it.id }.toSet()
                                            selectedPetNameForCreate = name
                                            showCreatePetForm = true
                                        }
                                    )
                                }
                            }

                            // Section: Other pets
                            if (otherPets.isNotEmpty()) {
                                item {
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = stringResource(R.string.other_pets),
                                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                items(otherPets, key = { it.id }) { pet ->
                                    PetSelectionCard(
                                        pet = pet,
                                        isSelected = selectedPetId == pet.id,
                                        onClick = { viewModel.selectPet(pet.id) }
                                    )
                                }
                            }
                        } else {
                            // No order context - flat list
                            items(pets, key = { it.id }) { pet ->
                                PetSelectionCard(
                                    pet = pet,
                                    isSelected = selectedPetId == pet.id,
                                    onClick = { viewModel.selectPet(pet.id) }
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (pets.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.tag_link_message),
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                Spacer(modifier = Modifier.height(12.dp))

                BrandButton(
                    text = if (activationState is ActivationState.Loading)
                        stringResource(R.string.tag_activating)
                    else
                        stringResource(R.string.tag_activate_button),
                    onClick = { viewModel.activateTag(qrCode) },
                    enabled = selectedPetId != null && activationState !is ActivationState.Loading,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun PetSelectionCard(
    pet: Pet,
    isSelected: Boolean,
    badge: String? = null,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .then(
                if (isSelected) Modifier.border(
                    width = 2.dp,
                    color = BrandOrange,
                    shape = RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                BrandOrange.copy(alpha = 0.08f)
            else
                MaterialTheme.colorScheme.surfaceContainerHigh
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(TealAccent.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
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
                        contentDescription = pet.name,
                        modifier = Modifier.size(28.dp),
                        tint = TealAccent
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = pet.name,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                pet.breed?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                badge?.let {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                        color = BrandOrange,
                        modifier = Modifier
                            .background(BrandOrange.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = BrandOrange
                )
            }
        }
    }
}

@Composable
private fun UnmatchedPetCard(petName: String, onCreateProfile: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(1.dp, BrandOrange.copy(alpha = 0.3f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(BrandOrange.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = petName,
                    modifier = Modifier.size(28.dp),
                    tint = BrandOrange
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = petName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.needs_profile),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Medium),
                    color = BrandOrange,
                    modifier = Modifier
                        .background(BrandOrange.copy(alpha = 0.12f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.create_profile_first),
                    style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                    color = BrandOrange,
                    modifier = Modifier
                        .clickable(onClick = onCreateProfile)
                        .padding(vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun NextStepCard(
    text: String,
    onClick: () -> Unit
) {
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
