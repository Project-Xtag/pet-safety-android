package com.petsafety.app.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Navigation
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp

/**
 * Map App Picker Dialog
 *
 * Presents options for opening a location in different map apps.
 * Shows a warning when location is approximate (~500m accuracy).
 */
@Composable
fun MapAppPickerDialog(
    latitude: Double,
    longitude: Double,
    label: String,
    isApproximate: Boolean = false,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("$label Location")
        },
        text = {
            Column {
                // Warning for approximate location
                if (isApproximate) {
                    Surface(
                        color = MaterialTheme.colorScheme.errorContainer,
                        shape = MaterialTheme.shapes.small,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "This is an approximate location (~500m). Search the surrounding area.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.padding(8.dp))
                }

                // Coordinates display
                Text(
                    text = "Coordinates: ${formatCoordinates(latitude, longitude, isApproximate)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.padding(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.padding(4.dp))

                // Map app options
                Text(
                    text = "Open in:",
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                MapAppOption(
                    icon = Icons.Default.Map,
                    label = "Google Maps",
                    onClick = {
                        openGoogleMaps(context, latitude, longitude, label)
                        onDismiss()
                    }
                )

                MapAppOption(
                    icon = Icons.Default.Navigation,
                    label = "Waze",
                    onClick = {
                        openWaze(context, latitude, longitude)
                        onDismiss()
                    }
                )

                MapAppOption(
                    icon = Icons.Default.Directions,
                    label = "Other Maps App",
                    onClick = {
                        openGenericMap(context, latitude, longitude, label)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MapAppOption(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

private fun formatCoordinates(lat: Double, lng: Double, isApproximate: Boolean): String {
    val precision = if (isApproximate) 3 else 6
    return String.format("%.${precision}f, %.${precision}f", lat, lng)
}

private fun openGoogleMaps(context: Context, lat: Double, lng: Double, label: String) {
    // Try Google Maps app first
    val gmmUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
    val mapIntent = Intent(Intent.ACTION_VIEW, gmmUri)
    mapIntent.setPackage("com.google.android.apps.maps")

    if (mapIntent.resolveActivity(context.packageManager) != null) {
        context.startActivity(mapIntent)
    } else {
        // Fallback to web
        val webUri = Uri.parse("https://www.google.com/maps/search/?api=1&query=$lat,$lng")
        context.startActivity(Intent(Intent.ACTION_VIEW, webUri))
    }
}

private fun openWaze(context: Context, lat: Double, lng: Double) {
    val wazeUri = Uri.parse("https://waze.com/ul?ll=$lat,$lng&navigate=yes")
    val intent = Intent(Intent.ACTION_VIEW, wazeUri)
    context.startActivity(intent)
}

private fun openGenericMap(context: Context, lat: Double, lng: Double, label: String) {
    // Generic geo: URI that opens the system chooser
    val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
    val intent = Intent(Intent.ACTION_VIEW, geoUri)
    val chooser = Intent.createChooser(intent, "Open with...")
    context.startActivity(chooser)
}
