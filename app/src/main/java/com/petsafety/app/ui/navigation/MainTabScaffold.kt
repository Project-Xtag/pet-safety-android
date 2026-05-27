package com.petsafety.app.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.Crossfade
import com.petsafety.app.ui.util.AdaptiveLayout
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import com.petsafety.app.ui.theme.BrandOrange
import com.petsafety.app.ui.theme.CreamSurface
import com.petsafety.app.ui.theme.InkText
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.petsafety.app.ui.screens.AlertsTabScreen
import com.petsafety.app.ui.screens.PetsScreen
import com.petsafety.app.ui.screens.ProfileScreen
import com.petsafety.app.ui.screens.QrScannerScreen
import com.petsafety.app.ui.screens.PetSetupWizardScreen
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
    var alertsInitialTab by remember { mutableStateOf(0) }
    var activationQrCode by remember { mutableStateOf<String?>(null) }
    var promoClaimData by remember { mutableStateOf<Triple<String, String, Int>?>(null) } // (qrCode, shelterName, months)

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

    // Promo tags route into the SAME PetSetupWizardScreen the ordered
    // flow uses, with mode=PROMO. Strict web parity (web's PetSetup.tsx).
    // The legacy ShelterPromoClaimScreen + its VM have been deleted.
    if (promoClaimData != null) {
        val (qrCode, _, _) = promoClaimData!!
        PetSetupWizardScreen(
            qrCode = qrCode,
            mode = com.petsafety.app.ui.viewmodel.PetSetupWizardMode.PROMO,
            onDone = { promoClaimData = null },
            // Step 11's "Következő biléta beolvasása" needs a handler
            // that actually opens the scanner. Pre-fix the param was
            // omitted, defaulted to null, and the button fell through
            // to onDone — closing the wizard with no way for the user
            // to register the next pet without re-navigating manually.
            onScanNextTag = {
                promoClaimData = null
                selectedTab = TabItem.Scan
            },
        )
        return
    }

    // Show the guided pet-setup wizard if a QR code needs activation
    if (activationQrCode != null) {
        PetSetupWizardScreen(
            qrCode = activationQrCode!!,
            onDone = { activationQrCode = null },
            onScanNextTag = {
                activationQrCode = null
                selectedTab = TabItem.Scan
            },
        )
        return
    }

    val showRail = AdaptiveLayout.useNavigationRail()

    val tabContentParams: @Composable (Modifier) -> Unit = { mod ->
        TabContent(
            selectedTab = selectedTab,
            appStateViewModel = appStateViewModel,
            authViewModel = authViewModel,
            pendingQrCode = pendingQrCode,
            onQrCodeHandled = onQrCodeHandled,
            alertsInitialTab = alertsInitialTab,
            onNavigateToSuccessStories = {
                alertsInitialTab = 1
                selectedTab = TabItem.Alerts
            },
            onNavigateToActivation = { qrCode -> activationQrCode = qrCode },
            onNavigateToPromoClaim = { qrCode, shelter, months -> promoClaimData = Triple(qrCode, shelter, months) },
            onScanTag = { selectedTab = TabItem.Scan },
            onExploreAccount = { selectedTab = TabItem.Profile },
            onNavigateToPets = { selectedTab = TabItem.Pets },
            modifier = mod
        )
    }

    // 2026-05-25 redesign7-aligned tab bar: cream wash for the bar
    // itself, brand-orange pill indicator under the selected tab,
    // ink-toned label/icon for the selection. Mirrors iOS
    // ContentView.CustomTabBar.
    val railColors = NavigationRailItemDefaults.colors(
        selectedIconColor = InkText,
        selectedTextColor = InkText,
        unselectedIconColor = InkText.copy(alpha = 0.6f),
        unselectedTextColor = InkText.copy(alpha = 0.6f),
        indicatorColor = BrandOrange.copy(alpha = 0.18f)
    )
    val barColors = NavigationBarItemDefaults.colors(
        selectedIconColor = InkText,
        selectedTextColor = InkText,
        unselectedIconColor = InkText.copy(alpha = 0.6f),
        unselectedTextColor = InkText.copy(alpha = 0.6f),
        indicatorColor = BrandOrange.copy(alpha = 0.18f)
    )

    if (showRail) {
        // Tablet: NavigationRail on the left + content on the right
        Row(modifier = Modifier.fillMaxSize()) {
            NavigationRail(
                modifier = Modifier.fillMaxHeight(),
                containerColor = CreamSurface
            ) {
                tabs.forEach { tab ->
                    NavigationRailItem(
                        selected = tab == selectedTab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = null) },
                        label = {
                            Text(
                                text = stringResource(tab.labelRes),
                                fontSize = AdaptiveLayout.scaledSp(10),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        },
                        colors = railColors
                    )
                }
            }
            VerticalDivider()
            tabContentParams(Modifier.weight(1f))
        }
    } else {
        // Phone: bottom navigation bar (existing layout)
        Scaffold(
            bottomBar = {
                NavigationBar(containerColor = CreamSurface) {
                    tabs.forEach { tab ->
                        NavigationBarItem(
                            selected = tab == selectedTab,
                            onClick = { selectedTab = tab },
                            icon = { Icon(tab.icon, contentDescription = null) },
                            label = {
                                Text(
                                    text = stringResource(tab.labelRes),
                                    fontSize = AdaptiveLayout.scaledSp(10),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            colors = barColors
                        )
                    }
                }
            }
        ) { innerPadding ->
            tabContentParams(Modifier.padding(innerPadding))
        }
    }
}

@Composable
private fun TabContent(
    selectedTab: TabItem,
    appStateViewModel: AppStateViewModel,
    authViewModel: AuthViewModel,
    pendingQrCode: String?,
    onQrCodeHandled: () -> Unit,
    alertsInitialTab: Int = 0,
    onNavigateToSuccessStories: () -> Unit = {},
    onNavigateToActivation: (String) -> Unit = {},
    onNavigateToPromoClaim: (String, String, Int) -> Unit = { _, _, _ -> },
    onScanTag: () -> Unit = {},
    onExploreAccount: () -> Unit = {},
    onNavigateToPets: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Crossfade(targetState = selectedTab, animationSpec = tween(200)) {
        when (it) {
            TabItem.Pets -> PetsScreen(appStateViewModel, authViewModel, modifier, onNavigateToSuccessStories, onScanTag, onExploreAccount)
            TabItem.Scan -> QrScannerScreen(
                appStateViewModel = appStateViewModel,
                pendingQrCode = pendingQrCode,
                onQrCodeHandled = onQrCodeHandled,
                onNavigateToActivation = onNavigateToActivation,
                onNavigateToPromoClaim = onNavigateToPromoClaim,
                modifier = modifier
            )
            TabItem.Alerts -> AlertsTabScreen(appStateViewModel, authViewModel, modifier, alertsInitialTab)
            TabItem.Profile -> ProfileScreen(
                appStateViewModel, authViewModel, modifier,
                onScanTag = onScanTag,
                onNavigateToPets = onNavigateToPets
            )
        }
    }
}
