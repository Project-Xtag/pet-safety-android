package com.petsafety.app.ui.components

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import com.petsafety.app.R
import com.petsafety.app.data.model.LocationCoordinate
import com.petsafety.app.ui.theme.TealAccent

@SuppressLint("MissingPermission")
@Composable
fun SuccessStoryDialog(
    petName: String,
    onDismiss: () -> Unit,
    onSubmit: (storyText: String, location: LocationCoordinate?) -> Unit,
    onSkip: () -> Unit
) {
    val context = LocalContext.current
    val locationProvider = remember { LocationServices.getFusedLocationProviderClient(context) }

    var storyText by remember { mutableStateOf("") }
    var capturedLocation by remember { mutableStateOf<LocationCoordinate?>(null) }
    var isCapturingLocation by remember { mutableStateOf(false) }
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

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.share_your_story)) },
        text = {
            Column {
                Text(stringResource(R.string.success_story_prompt, petName))
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = storyText,
                    onValueChange = { storyText = it },
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
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(storyText, capturedLocation) },
                enabled = storyText.isNotBlank()
            ) {
                Text(stringResource(R.string.share_story))
            }
        },
        dismissButton = {
            TextButton(onClick = onSkip) {
                Text(stringResource(R.string.skip))
            }
        }
    )
}
