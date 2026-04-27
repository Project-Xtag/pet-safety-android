package com.petsafety.app.ui.components

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.petsafety.app.R

/**
 * Map App Picker Dialog
 *
 * Presents options for opening a location in different map apps.
 * Shows a warning when location is approximate.
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
            Text(stringResource(R.string.location_title, label))
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
                                text = stringResource(R.string.approximate_location_note),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }

                    Spacer(modifier = Modifier.padding(8.dp))
                }

                // Coordinates display
                Text(
                    text = stringResource(R.string.coordinates_label, formatCoordinates(latitude, longitude, isApproximate)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.padding(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.padding(4.dp))

                // Map app options
                Text(
                    text = stringResource(R.string.open_in),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                MapAppOption(
                    icon = Icons.Default.Map,
                    label = stringResource(R.string.google_maps),
                    onClick = {
                        openGoogleMaps(context, latitude, longitude, label)
                        onDismiss()
                    }
                )

                MapAppOption(
                    icon = Icons.Default.Navigation,
                    label = stringResource(R.string.waze),
                    onClick = {
                        openWaze(context, latitude, longitude)
                        onDismiss()
                    }
                )

                MapAppOption(
                    icon = Icons.Default.Directions,
                    label = stringResource(R.string.other_maps_app),
                    onClick = {
                        openGenericMap(context, latitude, longitude, label)
                        onDismiss()
                    }
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
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

/**
 * Pure helper deciding which Waze intent to fire based on what's installed.
 * Exposed `internal` so unit tests can exercise it without a Context (audit H46).
 *
 * Order of preference:
 *   1. waze:// deep link — lands directly in the in-app navigator
 *   2. https://waze.com/ul universal link — handled by Waze if installed,
 *      else by the default browser (a Waze landing page that re-prompts
 *      install — graceful, no crash)
 *   3. nothing resolvable (no Waze, no browser) — caller must show a toast
 */
internal sealed class WazeIntent {
    data class Resolved(val intent: Intent) : WazeIntent()
    object NoneAvailable : WazeIntent()
}

internal fun chooseWazeIntent(
    lat: Double,
    lng: Double,
    canResolve: (Intent) -> Boolean,
): WazeIntent {
    val deepLink = Intent(Intent.ACTION_VIEW, Uri.parse("waze://?ll=$lat,$lng&navigate=yes"))
    if (canResolve(deepLink)) return WazeIntent.Resolved(deepLink)

    val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://waze.com/ul?ll=$lat,$lng&navigate=yes"))
    if (canResolve(web)) return WazeIntent.Resolved(web)

    return WazeIntent.NoneAvailable
}

private fun openWaze(context: Context, lat: Double, lng: Double) {
    val choice = chooseWazeIntent(lat, lng) { intent ->
        intent.resolveActivity(context.packageManager) != null
    }
    when (choice) {
        is WazeIntent.Resolved -> {
            try {
                context.startActivity(choice.intent)
            } catch (e: ActivityNotFoundException) {
                // resolveActivity said yes but startActivity disagreed — rare
                // (uninstall race after package query). Fall through to toast.
                Toast.makeText(
                    context,
                    context.getString(R.string.no_browser_for_navigation),
                    Toast.LENGTH_LONG,
                ).show()
            }
        }
        WazeIntent.NoneAvailable -> {
            Toast.makeText(
                context,
                context.getString(R.string.no_browser_for_navigation),
                Toast.LENGTH_LONG,
            ).show()
        }
    }
}

private fun openGenericMap(context: Context, lat: Double, lng: Double, label: String) {
    // Generic geo: URI that opens the system chooser
    val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
    val intent = Intent(Intent.ACTION_VIEW, geoUri)
    val chooser = Intent.createChooser(intent, context.getString(R.string.open_with))
    context.startActivity(chooser)
}
