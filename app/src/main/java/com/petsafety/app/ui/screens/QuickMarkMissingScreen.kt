package com.petsafety.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.petsafety.app.R
import com.petsafety.app.data.model.Pet
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.util.PetLocalizer
import com.petsafety.app.ui.viewmodel.PetsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickMarkMissingScreen(
    viewModel: PetsViewModel,
    onPetSelected: (Pet) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val allPets by viewModel.pets.collectAsState()
    val availablePets = allPets.filter { !it.isMissing }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.quick_lost_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cancel))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    navigationIconContentColor = BrandOrange
                )
            )
        }
    ) { padding ->
        if (availablePets.isEmpty()) {
            // Empty state
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(32.dp)) {
                    Icon(
                        Icons.Default.Pets,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        stringResource(R.string.quick_lost_all_missing),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    Text(
                        stringResource(R.string.quick_lost_select_pet),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
                items(availablePets, key = { it.id }) { pet ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPetSelected(pet) }
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (pet.profileImage != null) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(pet.profileImage)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = pet.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                )
                            } else {
                                Surface(
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer
                                ) {
                                    Icon(
                                        Icons.Default.Pets,
                                        contentDescription = null,
                                        modifier = Modifier.padding(10.dp),
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pet.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    PetLocalizer.localizeSpecies(context, pet.species) +
                                        (pet.breed?.let { " • ${PetLocalizer.localizeBreed(context, it, pet.species)}" } ?: ""),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Icon(
                                Icons.Default.ChevronRight,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }
}
