package com.petsafety.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.petsafety.app.R
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.Pet
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.MutedTextLight
import com.petsafety.app.ui.theme.TealAccent

/**
 * Notification center source options
 */
enum class NotificationCenterSource(val value: String, val displayName: String) {
    CURRENT_LOCATION("current_location", "Current Location"),
    REGISTERED_ADDRESS("registered_address", "My Address"),
    CUSTOM_ADDRESS("custom_address", "Custom")
}

/**
 * Bottom sheet for marking a pet as found.
 * Shows a list of missing pets to select from.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MarkFoundSheet(
    missingPets: List<Pet>,
    onDismiss: () -> Unit,
    onMarkFound: (Pet) -> Unit
) {
    val sheetState = rememberModalBottomSheetState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color(0xFF34C759).copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = Color(0xFF34C759),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.mark_pet_found),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(R.string.select_pet_to_mark_found),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MutedTextLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Pet List
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(missingPets) { pet ->
                    PetSelectionRow(
                        pet = pet,
                        onClick = { onMarkFound(pet) }
                    )
                }
            }
        }
    }
}

/**
 * Bottom sheet for reporting a pet as missing.
 * First shows pet selection, then location/description input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReportMissingSheet(
    availablePets: List<Pet>,
    onDismiss: () -> Unit,
    onReportMissing: (
        pet: Pet,
        lastSeenLocation: LocationCoordinate?,
        lastSeenAddress: String?,
        description: String?,
        notificationCenterSource: String,
        notificationCenterLocation: LocationCoordinate?,
        notificationCenterAddress: String?
    ) -> Unit,
    onRequestLocation: ((LocationCoordinate?) -> Unit) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var selectedPet by remember { mutableStateOf<Pet?>(null) }
    var lastSeenAddress by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var currentLocation by remember { mutableStateOf<LocationCoordinate?>(null) }

    // Notification center state
    var notificationCenterSource by remember { mutableStateOf(NotificationCenterSource.REGISTERED_ADDRESS) }
    var customNotificationAddress by remember { mutableStateOf("") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = 16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .background(Color.Red.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color.Red,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = stringResource(R.string.report_missing_pet),
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (selectedPet == null)
                            stringResource(R.string.select_pet_to_report)
                        else
                            stringResource(R.string.add_missing_details),
                        style = MaterialTheme.typography.bodyMedium.copy(fontSize = 14.sp),
                        color = MutedTextLight
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (selectedPet == null) {
                // Step 1: Pet Selection
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(availablePets) { pet ->
                        PetSelectionRow(
                            pet = pet,
                            onClick = { selectedPet = pet }
                        )
                    }
                }
            } else {
                // Step 2: Location and Description
                val pet = selectedPet ?: return@Column

                // Selected Pet Preview
                PetSelectionRow(
                    pet = pet,
                    onClick = { selectedPet = null },
                    showChangeButton = true
                )

                Spacer(modifier = Modifier.height(20.dp))

                // Last Seen Location
                Text(
                    text = stringResource(R.string.last_seen_location),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Text input for address
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFFF2F2F7))
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        BasicTextField(
                            value = lastSeenAddress,
                            onValueChange = { lastSeenAddress = it },
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = MaterialTheme.typography.bodyMedium.copy(
                                color = MaterialTheme.colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(BrandOrange),
                            decorationBox = { innerTextField ->
                                if (lastSeenAddress.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.enter_address_or_location),
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MutedTextLight
                                    )
                                }
                                innerTextField()
                            }
                        )
                    }

                    // Use Current Location Button
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TealAccent)
                            .clickable {
                                onRequestLocation { location ->
                                    currentLocation = location
                                    if (location != null) {
                                        lastSeenAddress = "Current location"
                                    }
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = stringResource(R.string.use_current_location),
                            tint = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Description
                Text(
                    text = stringResource(R.string.additional_details),
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(8.dp))

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF2F2F7))
                        .padding(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    BasicTextField(
                        value = description,
                        onValueChange = { description = it },
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = MaterialTheme.colorScheme.onSurface
                        ),
                        cursorBrush = SolidColor(BrandOrange),
                        decorationBox = { innerTextField ->
                            if (description.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.describe_circumstances),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MutedTextLight
                                )
                            }
                            innerTextField()
                        }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Notification Center Selection
                Text(
                    text = "Notification Center",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Alerts will be sent within 10km of this location",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    color = MutedTextLight
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    NotificationCenterSource.entries.forEach { source ->
                        FilterChip(
                            selected = notificationCenterSource == source,
                            onClick = {
                                notificationCenterSource = source
                                if (source == NotificationCenterSource.CURRENT_LOCATION && currentLocation == null) {
                                    onRequestLocation { location ->
                                        currentLocation = location
                                    }
                                }
                            },
                            label = {
                                Text(
                                    text = source.displayName,
                                    fontSize = 11.sp
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = TealAccent.copy(alpha = 0.15f),
                                selectedLabelColor = TealAccent
                            )
                        )
                    }
                }

                // Show content based on notification center source
                when (notificationCenterSource) {
                    NotificationCenterSource.CURRENT_LOCATION -> {
                        if (currentLocation != null) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.LocationOn,
                                    contentDescription = null,
                                    tint = TealAccent,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Lat: %.6f, Lng: %.6f".format(currentLocation!!.lat, currentLocation!!.lng),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MutedTextLight
                                )
                            }
                        } else {
                            Text(
                                text = "Tap to get current location...",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedTextLight,
                                modifier = Modifier.padding(top = 8.dp)
                            )
                        }
                    }
                    NotificationCenterSource.REGISTERED_ADDRESS -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Home,
                                contentDescription = null,
                                tint = Color(0xFF34C759),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Using your registered address",
                                style = MaterialTheme.typography.bodySmall,
                                color = MutedTextLight
                            )
                        }
                    }
                    NotificationCenterSource.CUSTOM_ADDRESS -> {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFFF2F2F7))
                                .padding(horizontal = 16.dp, vertical = 14.dp)
                        ) {
                            BasicTextField(
                                value = customNotificationAddress,
                                onValueChange = { customNotificationAddress = it },
                                modifier = Modifier.fillMaxWidth(),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(BrandOrange),
                                decorationBox = { innerTextField ->
                                    if (customNotificationAddress.isEmpty()) {
                                        Text(
                                            text = "Enter address for notifications",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MutedTextLight
                                        )
                                    }
                                    innerTextField()
                                }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Submit Button
                BrandButton(
                    text = stringResource(R.string.report_missing),
                    onClick = {
                        val notifLocation = if (notificationCenterSource == NotificationCenterSource.CURRENT_LOCATION) {
                            currentLocation
                        } else null

                        val notifAddress = if (notificationCenterSource == NotificationCenterSource.CUSTOM_ADDRESS) {
                            customNotificationAddress.takeIf { it.isNotBlank() }
                        } else null

                        onReportMissing(
                            pet,
                            currentLocation,
                            lastSeenAddress.takeIf { it.isNotBlank() && it != "Current location" },
                            description.takeIf { it.isNotBlank() },
                            notificationCenterSource.value,
                            notifLocation,
                            notifAddress
                        )
                    }
                )
            }
        }
    }
}

@Composable
private fun PetSelectionRow(
    pet: Pet,
    onClick: () -> Unit,
    showChangeButton: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFFF2F2F7))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Pet Photo
        if (!pet.profileImage.isNullOrBlank()) {
            AsyncImage(
                model = pet.profileImage,
                contentDescription = pet.name,
                modifier = Modifier
                    .size(50.dp)
                    .clip(RoundedCornerShape(10.dp)),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(50.dp)
                    .background(Color.White, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pets,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MutedTextLight
                )
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Pet Info
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = pet.name,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${pet.species} ${pet.breed?.let { "- $it" } ?: ""}",
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                color = MutedTextLight,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        if (showChangeButton) {
            Text(
                text = stringResource(R.string.change),
                style = MaterialTheme.typography.bodyMedium.copy(
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = BrandOrange
            )
        }
    }
}
