package com.petsafety.app.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import com.petsafety.app.ui.screens.AlertsTabScreen
import com.petsafety.app.ui.screens.PetsScreen
import com.petsafety.app.ui.screens.ProfileScreen
import com.petsafety.app.ui.screens.QrScannerScreen
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.NotificationImportant
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.QrCodeScanner
import com.petsafety.app.R
import com.petsafety.app.ui.components.PushNotificationPrompt
import androidx.annotation.StringRes
import androidx.compose.runtime.saveable.rememberSaveable

sealed class TabItem(@StringRes val labelRes: Int, val icon: ImageVector) {
    data object Pets : TabItem(R.string.my_pets, Icons.Default.Pets)
    data object Scan : TabItem(R.string.scan_qr, Icons.Default.QrCodeScanner)
    data object Alerts : TabItem(R.string.alerts, Icons.Default.NotificationImportant)
    data object Profile : TabItem(R.string.profile, Icons.Default.AccountCircle)
}

@Composable
fun MainTabScaffold(
    appStateViewModel: AppStateViewModel,
    authViewModel: AuthViewModel,
    pendingQrCode: String?,
    onQrCodeHandled: () -> Unit
) {
    val context = LocalContext.current
    val tabs = listOf(TabItem.Pets, TabItem.Scan, TabItem.Alerts, TabItem.Profile)
    var selectedTab by remember { mutableStateOf<TabItem>(TabItem.Pets) }

    // Custom pre-permission dialog state
    var showPushPrompt by rememberSaveable { mutableStateOf(false) }

    // Request notification permission on Android 13+
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        // Permission result - notifications will work if granted
        // No action needed on denial - app works without notifications
    }

    // Show custom dialog once on first composition (Android 13+)
    LaunchedEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val hasPermission = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

            if (!hasPermission) {
                // Show custom dialog first instead of system dialog directly
                val prefs = context.getSharedPreferences("push_prefs", android.content.Context.MODE_PRIVATE)
                val promptShown = prefs.getBoolean("push_prompt_shown", false)
                if (!promptShown) {
                    showPushPrompt = true
                } else {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            }
        }
    }

    // Custom pre-permission dialog
    if (showPushPrompt) {
        PushNotificationPrompt(
            onEnable = {
                showPushPrompt = false
                val prefs = context.getSharedPreferences("push_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putBoolean("push_prompt_shown", true).apply()
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                }
            },
            onDismiss = {
                showPushPrompt = false
                val prefs = context.getSharedPreferences("push_prefs", android.content.Context.MODE_PRIVATE)
                prefs.edit().putBoolean("push_prompt_shown", true).apply()
            }
        )
    }

    LaunchedEffect(pendingQrCode) {
        if (!pendingQrCode.isNullOrBlank()) {
            selectedTab = TabItem.Scan
        }
    }

    // Bottom navigation for both phones and tablets
    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = { Text(stringResource(tab.labelRes)) }
                    )
                }
            }
        }
    ) { innerPadding ->
        TabContent(
            selectedTab = selectedTab,
            appStateViewModel = appStateViewModel,
            authViewModel = authViewModel,
            pendingQrCode = pendingQrCode,
            onQrCodeHandled = onQrCodeHandled,
            modifier = Modifier.padding(innerPadding)
        )
    }
}

@Composable
private fun TabContent(
    selectedTab: TabItem,
    appStateViewModel: AppStateViewModel,
    authViewModel: AuthViewModel,
    pendingQrCode: String?,
    onQrCodeHandled: () -> Unit,
    modifier: Modifier = Modifier
) {
    Crossfade(targetState = selectedTab, animationSpec = tween(200)) {
        when (it) {
            TabItem.Pets -> PetsScreen(appStateViewModel, authViewModel, modifier)
            TabItem.Scan -> QrScannerScreen(
                appStateViewModel = appStateViewModel,
                pendingQrCode = pendingQrCode,
                onQrCodeHandled = onQrCodeHandled,
                modifier = modifier
            )
            TabItem.Alerts -> AlertsTabScreen(appStateViewModel, authViewModel, modifier)
            TabItem.Profile -> ProfileScreen(appStateViewModel, authViewModel, modifier)
        }
    }
}
