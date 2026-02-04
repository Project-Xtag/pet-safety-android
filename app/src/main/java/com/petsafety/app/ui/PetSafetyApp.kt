package com.petsafety.app.ui

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.tween
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
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
import com.petsafety.app.ui.components.BrandButton
import com.petsafety.app.ui.components.LoadingOverlay
import com.petsafety.app.ui.components.MapAppPickerDialog
import com.petsafety.app.ui.navigation.MainTabScaffold
import com.petsafety.app.ui.screens.OrderMoreTagsScreen
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
    val defaultMapLabel = stringResource(R.string.map_label_pet_location)
    var mapPickerLabel by remember { mutableStateOf(defaultMapLabel) }

    // State for showing order tags screen from auth screen
    var showOrderTagsScreen by remember { mutableStateOf(false) }

    // State for showing register screen
    var showRegisterScreen by remember { mutableStateOf(false) }

    // State for session expired dialog
    var showSessionExpiredDialog by remember { mutableStateOf(false) }

    val context = LocalContext.current
    val biometricHelper = remember { BiometricHelper(context) }
    val activity = context as? FragmentActivity

    val biometricTitle = stringResource(R.string.biometric_login_title)
    val biometricSubtitle = stringResource(R.string.biometric_login_subtitle)
    val biometricNegative = stringResource(R.string.use_password)
    val mapLabelTagScanned = stringResource(R.string.map_label_tag_scanned)
    val mapLabelSighting = stringResource(R.string.map_label_sighting)
    val mapLabelPetLocation = stringResource(R.string.map_label_pet_location)

    // Listen for session expiration events
    LaunchedEffect(Unit) {
        authViewModel.sessionExpiredEvent.collect {
            showSessionExpiredDialog = true
        }
    }

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
                            NotificationHelper.TYPE_TAG_SCANNED -> mapLabelTagScanned
                            NotificationHelper.TYPE_SIGHTING -> mapLabelSighting
                            else -> mapLabelPetLocation
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
        val screenKey = when {
            isAuthenticated -> "main"
            showOrderTagsScreen -> "order_tags"
            showRegisterScreen -> "register"
            else -> "auth"
        }

        AnimatedContent(
            targetState = screenKey,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "screen_transition"
        ) { target ->
            when (target) {
                "main" -> MainTabScaffold(
                    appStateViewModel = appStateViewModel,
                    authViewModel = authViewModel,
                    pendingQrCode = pendingQrCode,
                    onQrCodeHandled = onQrCodeHandled
                )
                "order_tags" -> OrderMoreTagsScreen(
                    appStateViewModel = appStateViewModel,
                    onDone = { showOrderTagsScreen = false }
                )
                "register" -> RegisterScreen(
                    appStateViewModel = appStateViewModel,
                    authViewModel = authViewModel,
                    onNavigateToLogin = { showRegisterScreen = false }
                )
                else -> AuthScreen(
                    appStateViewModel = appStateViewModel,
                    authViewModel = authViewModel,
                    onNavigateToOrderTags = { showOrderTagsScreen = true },
                    onNavigateToRegister = { showRegisterScreen = true }
                )
            }
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

    // Session Expired Dialog - non-dismissible
    if (showSessionExpiredDialog) {
        AlertDialog(
            onDismissRequest = { /* Non-dismissible */ },
            title = {
                Text(
                    text = stringResource(R.string.session_expired_title),
                    style = MaterialTheme.typography.headlineSmall
                )
            },
            text = {
                Text(
                    text = stringResource(R.string.session_expired_message),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                BrandButton(
                    text = stringResource(R.string.session_expired_log_in),
                    onClick = { showSessionExpiredDialog = false }
                )
            }
        )
    }
}
