package com.petsafety.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.petsafety.app.R
import com.petsafety.app.data.local.BiometricHelper
import com.petsafety.app.data.notifications.NotificationHelper
import com.petsafety.app.ui.components.AppSnackbarHost
import com.petsafety.app.ui.components.LoadingOverlay
import com.petsafety.app.ui.components.MapAppPickerDialog
import com.petsafety.app.ui.navigation.MainTabScaffold
import com.petsafety.app.ui.viewmodel.AuthViewModel
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PetSafetyApp(
    pendingQrCode: String?,
    onQrCodeHandled: () -> Unit,
    pendingNotification: NotificationData? = null,
    onNotificationHandled: () -> Unit = {}
) {
    val appStateViewModel: AppStateViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val isLoading by appStateViewModel.isLoading.collectAsState()
    val showBiometricPrompt by authViewModel.showBiometricPrompt.collectAsState()

    // State for showing map picker dialog from notification
    var showMapPicker by remember { mutableStateOf(false) }
    var mapPickerLocation by remember { mutableStateOf<Triple<Double, Double, Boolean>?>(null) }
    var mapPickerLabel by remember { mutableStateOf("Pet Location") }

    val context = LocalContext.current
    val biometricHelper = remember { BiometricHelper(context) }
    val activity = context as? FragmentActivity

    val biometricTitle = stringResource(R.string.biometric_login_title)
    val biometricSubtitle = stringResource(R.string.biometric_login_subtitle)
    val biometricNegative = stringResource(R.string.use_password)

    // Handle notification tap
    LaunchedEffect(pendingNotification) {
        pendingNotification?.let { notification ->
            when (notification.type) {
                NotificationHelper.TYPE_TAG_SCANNED,
                NotificationHelper.TYPE_SIGHTING -> {
                    // Show map picker if location is available
                    if (notification.latitude != null && notification.longitude != null) {
                        mapPickerLocation = Triple(
                            notification.latitude,
                            notification.longitude,
                            notification.isApproximate
                        )
                        mapPickerLabel = when (notification.type) {
                            NotificationHelper.TYPE_TAG_SCANNED -> "Pet Tag Scanned"
                            NotificationHelper.TYPE_SIGHTING -> "Pet Sighting"
                            else -> "Pet Location"
                        }
                        showMapPicker = true
                    }
                    onNotificationHandled()
                }
                NotificationHelper.TYPE_MISSING_ALERT,
                NotificationHelper.TYPE_PET_FOUND -> {
                    // Navigate to alerts tab (handled by MainTabScaffold)
                    // For now, just clear the notification
                    onNotificationHandled()
                }
            }
        }
    }

    LaunchedEffect(showBiometricPrompt) {
        if (showBiometricPrompt && activity != null && biometricHelper.canUseBiometric()) {
            biometricHelper.showBiometricPrompt(
                activity = activity,
                title = biometricTitle,
                subtitle = biometricSubtitle,
                negativeButtonText = biometricNegative,
                onSuccess = { authViewModel.onBiometricSuccess() },
                onFailure = { authViewModel.onBiometricCancelled() },
                onCancel = { authViewModel.onBiometricCancelled() }
            )
        } else if (showBiometricPrompt) {
            // Biometric not available, dismiss prompt
            authViewModel.dismissBiometricPrompt()
        }
    }

    LaunchedEffect(isAuthenticated) {
        if (isAuthenticated) {
            appStateViewModel.connectSse()
        } else {
            appStateViewModel.disconnectSse()
        }
    }

    Scaffold(
        snackbarHost = {
            AppSnackbarHost(appStateViewModel)
        },
        containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0f)
    ) { padding ->
        if (isAuthenticated) {
            MainTabScaffold(
                appStateViewModel = appStateViewModel,
                authViewModel = authViewModel,
                pendingQrCode = pendingQrCode,
                onQrCodeHandled = onQrCodeHandled
            )
        } else {
            AuthScreen(
                appStateViewModel = appStateViewModel,
                authViewModel = authViewModel
            )
        }

        if (isLoading) {
            LoadingOverlay()
        }
    }

    // Map App Picker Dialog (shown when notification with location is tapped)
    if (showMapPicker && mapPickerLocation != null) {
        val (lat, lng, isApproximate) = mapPickerLocation!!
        MapAppPickerDialog(
            latitude = lat,
            longitude = lng,
            label = mapPickerLabel,
            isApproximate = isApproximate,
            onDismiss = {
                showMapPicker = false
                mapPickerLocation = null
            }
        )
    }
}
