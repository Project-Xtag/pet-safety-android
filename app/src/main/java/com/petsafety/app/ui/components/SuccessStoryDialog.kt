package com.petsafety.app.ui.components

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import coil.compose.AsyncImage
import com.google.android.gms.location.LocationServices
import com.petsafety.app.R
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.data.model.Pet
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.TealAccent
import com.petsafety.app.ui.util.ShareCardGenerator
import com.petsafety.app.ui.util.SocialShareHelper
import kotlinx.coroutines.launch

@SuppressLint("MissingPermission")
@Composable
fun SuccessStoryDialog(
    pet: Pet,
    onDismiss: () -> Unit,
    onSubmit: (storyText: String, location: LocationCoordinate?) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val locationProvider = remember { LocationServices.getFusedLocationProviderClient(context) }

    var showForm by remember { mutableStateOf(false) }
    var storyText by remember { mutableStateOf("") }
    var capturedLocation by remember { mutableStateOf<LocationCoordinate?>(null) }
    var isCapturingLocation by remember { mutableStateOf(false) }
    var isGeneratingCard by remember { mutableStateOf(false) }
    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasLocationPermission = granted
        if (granted) {
            isCapturingLocation = true
            locationProvider.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    capturedLocation = LocationCoordinate(location.latitude, location.longitude)
                }
                isCapturingLocation = false
            }.addOnFailureListener {
                isCapturingLocation = false
            }
        }
    }

    // Auto-capture location when dialog opens
    LaunchedEffect(Unit) {
        if (hasLocationPermission) {
            isCapturingLocation = true
            locationProvider.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    capturedLocation = LocationCoordinate(location.latitude, location.longitude)
                }
                isCapturingLocation = false
            }.addOnFailureListener {
                isCapturingLocation = false
            }
        } else {
            permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            if (!showForm) {
                // Stage 1: Prompt
                PromptStage(
                    pet = pet,
                    isGeneratingCard = isGeneratingCard,
                    onShareStory = { showForm = true },
                    onShareGoodNews = {
                        isGeneratingCard = true
                        scope.launch {
                            try {
                                val bitmap = ShareCardGenerator.generate(
                                    context = context,
                                    petName = pet.name,
                                    petImageUrl = pet.profileImage,
                                    petSpecies = pet.species,
                                    city = pet.ownerCity
                                )
                                val caption = context.getString(R.string.story_social_caption, pet.name)
                                SocialShareHelper.shareImage(context, bitmap, caption)
                            } catch (_: Exception) { }
                            isGeneratingCard = false
                        }
                    },
                    onSkip = onSkip
                )
            } else {
                // Stage 2: Story Form
                FormStage(
                    pet = pet,
                    storyText = storyText,
                    onStoryTextChange = { storyText = it },
                    isCapturingLocation = isCapturingLocation,
                    capturedLocation = capturedLocation,
                    onSubmit = { onSubmit(storyText, capturedLocation) },
                    onBack = { showForm = false }
                )
            }
        }
    }
}

@Composable
private fun PromptStage(
    pet: Pet,
    isGeneratingCard: Boolean,
    onShareStory: () -> Unit,
    onShareGoodNews: () -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Animated circles + heart icon
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(TealAccent.copy(alpha = 0.15f))
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(TealAccent.copy(alpha = 0.25f))
            )
            Icon(
                Icons.Default.Favorite,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = TealAccent
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = stringResource(R.string.story_pet_is_home_title, pet.name),
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Pet photo
        if (pet.profileImage != null) {
            AsyncImage(
                model = pet.profileImage,
                contentDescription = pet.name,
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Pets,
                    contentDescription = null,
                    modifier = Modifier.size(40.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = stringResource(R.string.story_inspire_others_message, pet.name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Share My Story button (teal)
        Button(
            onClick = onShareStory,
            modifier = Modifier.fillMaxWidth(),
            colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
        ) {
            Icon(
                Icons.Default.Edit,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = stringResource(R.string.share_your_story),
                fontWeight = FontWeight.SemiBold
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Share the Good News button (orange)
        Button(
            onClick = onShareGoodNews,
            modifier = Modifier.fillMaxWidth(),
            enabled = !isGeneratingCard,
            colors = ButtonDefaults.buttonColors(containerColor = BrandOrange)
        ) {
            if (isGeneratingCard) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.dp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.story_generating_card),
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Icon(
                    Icons.Default.Share,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.story_share_good_news),
                    fontWeight = FontWeight.SemiBold
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Skip button
        TextButton(onClick = onSkip) {
            Text(
                text = stringResource(R.string.skip),
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun FormStage(
    pet: Pet,
    storyText: String,
    onStoryTextChange: (String) -> Unit,
    isCapturingLocation: Boolean,
    capturedLocation: LocationCoordinate?,
    onSubmit: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.share_your_story),
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = stringResource(R.string.success_story_prompt, pet.name),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = storyText,
            onValueChange = onStoryTextChange,
            label = { Text(stringResource(R.string.your_story)) },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
            maxLines = 6,
            placeholder = { Text(stringResource(R.string.story_placeholder)) }
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Location status
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (isCapturingLocation) {
                CircularProgressIndicator(
                    modifier = Modifier.size(16.dp),
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.getting_location),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else if (capturedLocation != null) {
                Icon(
                    Icons.Default.MyLocation,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = TealAccent
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = stringResource(R.string.location_captured),
                    style = MaterialTheme.typography.bodySmall,
                    color = TealAccent
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Buttons row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextButton(onClick = onBack) {
                Text(stringResource(R.string.back))
            }
            Button(
                onClick = onSubmit,
                enabled = storyText.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = TealAccent)
            ) {
                Text(stringResource(R.string.share_story))
            }
        }
    }
}
