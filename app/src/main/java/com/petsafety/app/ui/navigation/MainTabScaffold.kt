package com.petsafety.app.ui.navigation

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import com.petsafety.app.ui.AuthScreen
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
import androidx.annotation.StringRes

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
    val tabs = listOf(TabItem.Pets, TabItem.Scan, TabItem.Alerts, TabItem.Profile)
    var selectedTab by remember { mutableStateOf<TabItem>(TabItem.Pets) }

    LaunchedEffect(pendingQrCode) {
        if (!pendingQrCode.isNullOrBlank()) {
            selectedTab = TabItem.Scan
        }
    }

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
    ) {
        when (selectedTab) {
            TabItem.Pets -> PetsScreen(appStateViewModel)
            TabItem.Scan -> QrScannerScreen(
                appStateViewModel = appStateViewModel,
                pendingQrCode = pendingQrCode,
                onQrCodeHandled = onQrCodeHandled
            )
            TabItem.Alerts -> AlertsTabScreen(appStateViewModel, authViewModel)
            TabItem.Profile -> ProfileScreen(appStateViewModel, authViewModel)
        }
    }
}
