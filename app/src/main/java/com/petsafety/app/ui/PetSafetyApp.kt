package com.petsafety.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.FragmentActivity
import com.petsafety.app.R
import com.petsafety.app.data.local.BiometricHelper
import com.petsafety.app.ui.components.AppSnackbarHost
import com.petsafety.app.ui.components.LoadingOverlay
import com.petsafety.app.ui.navigation.MainTabScaffold
import com.petsafety.app.ui.viewmodel.AuthViewModel
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun PetSafetyApp(
    pendingQrCode: String?,
    onQrCodeHandled: () -> Unit
) {
    val appStateViewModel: AppStateViewModel = hiltViewModel()
    val authViewModel: AuthViewModel = hiltViewModel()
    val isAuthenticated by authViewModel.isAuthenticated.collectAsState()
    val isLoading by appStateViewModel.isLoading.collectAsState()
    val showBiometricPrompt by authViewModel.showBiometricPrompt.collectAsState()

    val context = LocalContext.current
    val biometricHelper = remember { BiometricHelper(context) }
    val activity = context as? FragmentActivity

    val biometricTitle = stringResource(R.string.biometric_login_title)
    val biometricSubtitle = stringResource(R.string.biometric_login_subtitle)
    val biometricNegative = stringResource(R.string.use_password)

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
}
