package com.petsafety.app.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.petsafety.app.R
import com.petsafety.app.data.model.CommunityFoundPet
import com.petsafety.app.ui.theme.BrandOrange
import java.text.SimpleDateFormat
import java.util.Locale

private val DetailAmber = Color(0xFFF59E0B)

/**
 * Detail sheet for a community-submitted found-pet report. Mirrors the
 * iOS FoundPetDetailView: photo, species/sex header, description,
 * metadata block, optional finder-contact, and a primary "Open in maps"
 * CTA that hands off to Google Maps / Waze via Intent ACTION_VIEW.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoundPetDetailScreen(
    report: CommunityFoundPet,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    var showMapPicker by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.found_pet_detail_title)) },
                navigationIcon = {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                },
                actions = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.common_done))
                    }
                },
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Photo(report)
            StatusRow(report)
            if (!report.description.isNullOrBlank()) {
                Text(report.description, fontSize = 14.sp)
            }
            MetadataBlock(report)
            ContactBlock(report)
            Button(
                onClick = { showMapPicker = true },
                colors = ButtonDefaults.buttonColors(containerColor = BrandOrange),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth().height(52.dp),
            ) {
                Icon(Icons.Filled.Map, contentDescription = null)
                Text(
                    stringResource(R.string.found_pet_detail_open_in_maps),
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }

    if (showMapPicker) {
        MapAppPickerDialog(
            latitude = report.foundLatitude,
            longitude = report.foundLongitude,
            label = report.breed
                ?: report.foundAddress
                ?: stringResource(R.string.lost_and_found_status_community),
            onPick = { intent ->
                context.startActivity(intent)
                showMapPicker = false
            },
            onDismiss = { showMapPicker = false },
        )
    }
}

@Composable
private fun Photo(report: CommunityFoundPet) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(20.dp))
            .background(DetailAmber.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center,
    ) {
        if (!report.photoUrl.isNullOrBlank()) {
            AsyncImage(
                model = report.photoUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Icon(
                Icons.Filled.Pets,
                contentDescription = null,
                tint = DetailAmber,
                modifier = Modifier.size(64.dp),
            )
        }
    }
}

@Composable
private fun StatusRow(report: CommunityFoundPet) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(50))
                .background(DetailAmber)
                .padding(horizontal = 10.dp, vertical = 4.dp),
        ) {
            Text(
                stringResource(R.string.lost_and_found_status_community).uppercase(),
                color = Color.White,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.2.sp,
            )
        }
        Text(
            when (report.species) {
                CommunityFoundPet.Species.DOG -> stringResource(R.string.lost_and_found_species_dog_singular)
                CommunityFoundPet.Species.CAT -> stringResource(R.string.lost_and_found_species_cat_singular)
                CommunityFoundPet.Species.OTHER -> stringResource(R.string.lost_and_found_species_other_singular)
            },
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        Text(
            when (report.sex) {
                CommunityFoundPet.Sex.MALE -> stringResource(R.string.found_pet_form_sex_male)
                CommunityFoundPet.Sex.FEMALE -> stringResource(R.string.found_pet_form_sex_female)
                CommunityFoundPet.Sex.UNKNOWN -> stringResource(R.string.found_pet_form_sex_unknown)
            },
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetadataBlock(report: CommunityFoundPet) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        if (!report.breed.isNullOrBlank()) {
            MetadataRow(Icons.Filled.Tag, stringResource(R.string.found_pet_detail_breed), report.breed)
        }
        if (!report.color.isNullOrBlank()) {
            MetadataRow(Icons.Filled.Palette, stringResource(R.string.found_pet_detail_color), report.color)
        }
        val foundAtFormatted = remember(report.foundAt) {
            // Best-effort parse — backend sends ISO 8601 UTC.
            val parsers = listOf(
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
                "yyyy-MM-dd'T'HH:mm:ss'Z'",
            )
            val outFormatter = SimpleDateFormat("yyyy.MM.dd HH:mm", Locale.getDefault())
            parsers.firstNotNullOfOrNull { fmt ->
                runCatching {
                    SimpleDateFormat(fmt, Locale.US).apply {
                        timeZone = java.util.TimeZone.getTimeZone("UTC")
                    }.parse(report.foundAt)?.let { outFormatter.format(it) }
                }.getOrNull()
            } ?: report.foundAt
        }
        MetadataRow(Icons.Filled.CalendarToday, stringResource(R.string.found_pet_detail_found_at), foundAtFormatted)
        if (!report.foundAddress.isNullOrBlank()) {
            MetadataRow(Icons.Filled.LocationOn, stringResource(R.string.found_pet_detail_address), report.foundAddress)
        }
        report.distanceKm?.let {
            MetadataRow(
                Icons.Filled.LocationOn,
                stringResource(R.string.found_pet_detail_distance),
                String.format(Locale.getDefault(), "%.1f km", it),
            )
        }
    }
}

@Composable
private fun ContactBlock(report: CommunityFoundPet) {
    val anyContact = !report.reporterName.isNullOrBlank() ||
        !report.reporterEmail.isNullOrBlank() ||
        !report.reporterPhone.isNullOrBlank()
    if (!anyContact) {
        Text(
            stringResource(R.string.found_pet_detail_no_contact),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth(),
        )
        return
    }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surface)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Text(
            stringResource(R.string.found_pet_detail_finder_contact).uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.2.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        report.reporterName?.takeIf { it.isNotBlank() }?.let {
            MetadataRow(Icons.Filled.Person, stringResource(R.string.found_pet_detail_finder_name), it)
        }
        report.reporterEmail?.takeIf { it.isNotBlank() }?.let {
            MetadataRow(Icons.Filled.Email, stringResource(R.string.found_pet_detail_finder_email), it)
        }
        report.reporterPhone?.takeIf { it.isNotBlank() }?.let {
            MetadataRow(Icons.Filled.Phone, stringResource(R.string.found_pet_detail_finder_phone), it)
        }
    }
}

@Composable
private fun MetadataRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
) {
    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                label,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
    }
}

/**
 * Picker dialog offering Google Maps and Waze for a given lat/lng. The
 * iOS version uses MapAppPickerView; Android equivalent is just an
 * Intent into whichever map app the user picks.
 */
@Composable
private fun MapAppPickerDialog(
    latitude: Double,
    longitude: Double,
    label: String,
    onPick: (Intent) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.common_cancel))
            }
        },
        title = { Text(stringResource(R.string.map_open_in)) },
        text = {
            val packageManager = LocalContext.current.packageManager
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MapAppRow(
                    title = stringResource(R.string.map_google_maps),
                    onClick = {
                        val gmm = "geo:$latitude,$longitude?q=${Uri.encode(label)}"
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(gmm))
                            .setPackage("com.google.android.apps.maps")
                        // Fall back to default geo: handler if Google Maps isn't installed
                        val safe = if (intent.resolveActivity(packageManager) != null) {
                            intent
                        } else {
                            Intent(Intent.ACTION_VIEW, Uri.parse(gmm))
                        }
                        onPick(safe)
                    },
                )
                MapAppRow(
                    title = stringResource(R.string.map_waze),
                    onClick = {
                        val waze = "https://waze.com/ul?ll=$latitude,$longitude&navigate=yes"
                        onPick(Intent(Intent.ACTION_VIEW, Uri.parse(waze)))
                    },
                )
            }
        },
    )
}

@Composable
private fun MapAppRow(title: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .border(1.dp, Color.Black.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Filled.Map, contentDescription = null, tint = BrandOrange)
        Text(
            title,
            modifier = Modifier.padding(start = 12.dp),
            fontWeight = FontWeight.SemiBold,
        )
    }
}

