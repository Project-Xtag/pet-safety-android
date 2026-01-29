package com.petsafety.app.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.petsafety.app.R
import com.petsafety.app.data.fcm.NotificationLocation
import com.petsafety.app.ui.MainActivity
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for creating and showing notifications
 *
 * Supports multiple notification channels:
 * - Tag scans (high priority)
 * - Missing pet alerts
 * - Sighting reports
 * - General notifications
 */
@Singleton
class NotificationHelper @Inject constructor(private val context: Context) {

    companion object {
        const val CHANNEL_TAG_SCANS = "tag_scans"
        const val CHANNEL_ALERTS = "missing_alerts"
        const val CHANNEL_SIGHTINGS = "sightings"
        const val CHANNEL_GENERAL = "pet_safety_general"

        const val EXTRA_NOTIFICATION_TYPE = "notification_type"
        const val EXTRA_PET_ID = "pet_id"
        const val EXTRA_ALERT_ID = "alert_id"
        const val EXTRA_SCAN_ID = "scan_id"
        const val EXTRA_SIGHTING_ID = "sighting_id"
        const val EXTRA_LATITUDE = "latitude"
        const val EXTRA_LONGITUDE = "longitude"
        const val EXTRA_IS_APPROXIMATE = "is_approximate"

        const val TYPE_TAG_SCANNED = "tag_scanned"
        const val TYPE_MISSING_ALERT = "missing_alert"
        const val TYPE_PET_FOUND = "pet_found"
        const val TYPE_SIGHTING = "sighting"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Tag scans channel - high priority
        val tagScansChannel = NotificationChannel(
            CHANNEL_TAG_SCANS,
            context.getString(R.string.notification_channel_tag_scans),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_tag_scans_desc)
            enableVibration(true)
        }

        // Missing pet alerts channel
        val alertsChannel = NotificationChannel(
            CHANNEL_ALERTS,
            context.getString(R.string.notification_channel_alerts),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_alerts_desc)
            enableVibration(true)
        }

        // Sightings channel
        val sightingsChannel = NotificationChannel(
            CHANNEL_SIGHTINGS,
            context.getString(R.string.notification_channel_sightings),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_sightings_desc)
            enableVibration(true)
        }

        // General channel
        val generalChannel = NotificationChannel(
            CHANNEL_GENERAL,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        )

        manager.createNotificationChannels(
            listOf(tagScansChannel, alertsChannel, sightingsChannel, generalChannel)
        )
    }

    /**
     * Show a simple notification
     */
    fun showNotification(title: String, body: String) {
        if (!hasNotificationPermission()) return

        val intent = createMainActivityIntent()
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_GENERAL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(generateNotificationId(), notification)
    }

    /**
     * Show tag scanned notification with optional location action
     */
    fun showTagScannedNotification(
        title: String,
        body: String,
        petId: String?,
        scanId: String?,
        petName: String,
        location: NotificationLocation?
    ) {
        if (!hasNotificationPermission()) return

        val intent = createMainActivityIntent().apply {
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_TAG_SCANNED)
            putExtra(EXTRA_PET_ID, petId)
            putExtra(EXTRA_SCAN_ID, scanId)
            location?.let {
                putExtra(EXTRA_LATITUDE, it.latitude)
                putExtra(EXTRA_LONGITUDE, it.longitude)
                putExtra(EXTRA_IS_APPROXIMATE, it.isApproximate)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context, generateNotificationId(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_TAG_SCANS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Add "View on Map" action if location is available
        location?.let { loc ->
            val mapIntent = createMapIntent(loc.latitude, loc.longitude, petName)
            val mapPendingIntent = PendingIntent.getActivity(
                context, generateNotificationId(), mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_notification,
                if (loc.isApproximate) "View Area (~500m)" else "View on Map",
                mapPendingIntent
            )
        }

        NotificationManagerCompat.from(context).notify(generateNotificationId(), builder.build())
    }

    /**
     * Show missing pet alert notification
     */
    fun showMissingPetAlert(
        title: String,
        body: String,
        alertId: String?,
        petName: String
    ) {
        if (!hasNotificationPermission()) return

        val intent = createMainActivityIntent().apply {
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_MISSING_ALERT)
            putExtra(EXTRA_ALERT_ID, alertId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, generateNotificationId(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(generateNotificationId(), notification)
    }

    /**
     * Show pet found notification
     */
    fun showPetFoundNotification(
        title: String,
        body: String,
        petId: String?,
        alertId: String?,
        petName: String
    ) {
        if (!hasNotificationPermission()) return

        val intent = createMainActivityIntent().apply {
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_PET_FOUND)
            putExtra(EXTRA_PET_ID, petId)
            putExtra(EXTRA_ALERT_ID, alertId)
        }

        val pendingIntent = PendingIntent.getActivity(
            context, generateNotificationId(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        NotificationManagerCompat.from(context).notify(generateNotificationId(), notification)
    }

    /**
     * Show sighting reported notification
     */
    fun showSightingNotification(
        title: String,
        body: String,
        alertId: String?,
        sightingId: String?,
        petName: String,
        location: NotificationLocation?
    ) {
        if (!hasNotificationPermission()) return

        val intent = createMainActivityIntent().apply {
            putExtra(EXTRA_NOTIFICATION_TYPE, TYPE_SIGHTING)
            putExtra(EXTRA_ALERT_ID, alertId)
            putExtra(EXTRA_SIGHTING_ID, sightingId)
            location?.let {
                putExtra(EXTRA_LATITUDE, it.latitude)
                putExtra(EXTRA_LONGITUDE, it.longitude)
            }
        }

        val pendingIntent = PendingIntent.getActivity(
            context, generateNotificationId(), intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_SIGHTINGS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        // Add "Get Directions" action if location is available
        location?.let { loc ->
            val mapIntent = createNavigationIntent(loc.latitude, loc.longitude, "$petName sighting")
            val mapPendingIntent = PendingIntent.getActivity(
                context, generateNotificationId(), mapIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                R.drawable.ic_notification,
                "Get Directions",
                mapPendingIntent
            )
        }

        NotificationManagerCompat.from(context).notify(generateNotificationId(), builder.build())
    }

    private fun createMainActivityIntent(): Intent {
        return Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
    }

    /**
     * Create intent to view location on map (geo: URI for chooser)
     */
    private fun createMapIntent(lat: Double, lng: Double, label: String): Intent {
        val geoUri = Uri.parse("geo:$lat,$lng?q=$lat,$lng(${Uri.encode(label)})")
        return Intent(Intent.ACTION_VIEW, geoUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
    }

    /**
     * Create intent for navigation to location
     */
    private fun createNavigationIntent(lat: Double, lng: Double, label: String): Intent {
        // Use google.navigation for turn-by-turn directions, fallback to geo:
        val navUri = Uri.parse("google.navigation:q=$lat,$lng")
        val intent = Intent(Intent.ACTION_VIEW, navUri)
        intent.setPackage("com.google.android.apps.maps")

        // Check if Google Maps is available
        if (intent.resolveActivity(context.packageManager) != null) {
            return intent.apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK }
        }

        // Fallback to generic geo: URI
        return createMapIntent(lat, lng, label)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun generateNotificationId(): Int {
        return System.currentTimeMillis().toInt()
    }
}
