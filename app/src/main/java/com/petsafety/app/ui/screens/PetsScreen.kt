package com.petsafety.app.ui.screens

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import com.petsafety.app.ui.viewmodel.VaccinationsViewModel
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.petsafety.app.R
import com.petsafety.app.ui.util.AdaptiveLayout
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel
import com.petsafety.app.ui.viewmodel.PetsViewModel
import com.petsafety.app.ui.viewmodel.SuccessStoriesViewModel

@Composable
fun PetsScreen(appStateViewModel: AppStateViewModel, authViewModel: AuthViewModel, modifier: Modifier = Modifier, onNavigateToSuccessStories: () -> Unit = {}, onScanTag: () -> Unit = {}, onExploreAccount: () -> Unit = {}) {
    val navController = rememberNavController()
    val viewModel: PetsViewModel = hiltViewModel()
    val successStoriesViewModel: SuccessStoriesViewModel = hiltViewModel()
    val isTablet = AdaptiveLayout.isTablet()
    var tabletSelectedPetId by rememberSaveable { mutableStateOf<String?>(null) }

    // ── Vaccination deep-link convergence (slice 4) ──────────────────────────
    // The SINGLE consume point for the home urgent-row tap, the VACCINATION_DUE
    // push, and (future) defect A's inbox tap — all of which funnel through the
    // coordinator (surfaced as vaccinationDeepLinkPetId). This lives at the
    // PetsScreen level (where navController is in scope) so it's stable across the
    // internal pets_list → pet_detail → vaccinations navigation, mirroring iOS's
    // PetsListView-level consume point.
    //
    // Gated on hasLoadedOnce (a COMPLETED fetch cycle — success, empty, OR
    // failure), NOT pets.isEmpty(): a cold-launch target resolves on any terminal
    // determination and is held only while genuinely loading. That, plus the
    // one-shot tab-switch in MainTabScaffold, is what stops a failed cold-launch
    // fetch from leaving a pending target lingering.
    val deepLinkPetId by appStateViewModel.vaccinationDeepLinkPetId.collectAsState()
    val petsLoaded by viewModel.hasLoadedOnce.collectAsState()
    LaunchedEffect(deepLinkPetId, petsLoaded) {
        val petId = deepLinkPetId ?: return@LaunchedEffect
        if (!petsLoaded) return@LaunchedEffect // genuinely still loading — wait (cold launch)
        // CONSUME-ONCE CORRECTNESS: this block MUST stay suspension-free. Clearing
        // the StateFlow re-fires this effect with a null key, but only AFTER this
        // synchronous block completes — so consume-then-navigate can't be torn into
        // consume-without-navigate. Do NOT slip a suspend call between consume() and
        // the navigate()s.
        appStateViewModel.consumeVaccinationsDeepLink()
        if (viewModel.pets.value.any { it.id == petId }) {
            // Build the real back stack: pets_list → pet_detail → pet_vaccinations.
            // The two sequential navigates put pet_detail in the stack, which BOTH
            // makes Back natural (vaccinations → pet_detail → list) AND satisfies
            // slice 3's getBackStackEntry("pet_detail/{petId}") shared-VM scoping
            // invariant. The naive single navigate("pet_vaccinations/...") crashes there.
            navController.navigate("pet_detail/$petId")
            navController.navigate("pet_vaccinations/$petId")
        }
        // else: pet gone between request and now → graceful no-op (stay on pets_list)
    }

    NavHost(
        navController = navController,
        startDestination = "pets_list",
        modifier = modifier,
        enterTransition = { slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
        exitTransition = { slideOutHorizontally(targetOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(150)) },
        popEnterTransition = { slideInHorizontally(initialOffsetX = { -it / 3 }, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300)) },
        popExitTransition = { slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(300)) + fadeOut(animationSpec = tween(150)) }
    ) {
        composable("pets_list") {
            if (isTablet) {
                // Tablet: side-by-side list + detail
                Row(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.weight(0.4f)) {
                        PetsListScreen(
                            viewModel = viewModel,
                            appStateViewModel = appStateViewModel,
                            authViewModel = authViewModel,
                            onPetSelected = { tabletSelectedPetId = it.id },
                            onAddPet = { navController.navigate("order_more_tags") },
                            onOrderTags = { navController.navigate("order_more_tags") },
                            onReplacementTag = { petId -> navController.navigate("order_replacement/$petId") },
                            onReferral = { navController.navigate("referral") },
                            onNotifications = { navController.navigate("notifications") },
                            onSuccessStories = onNavigateToSuccessStories,
                            onScanTag = onScanTag,
                            onExploreAccount = onExploreAccount,
                            onQuickMarkMissing = { navController.navigate("quick_mark_missing") }
                        )
                    }
                    VerticalDivider()
                    Box(modifier = Modifier.weight(0.6f)) {
                        val petId = tabletSelectedPetId
                        if (petId != null) {
                            key(petId) {
                                PetDetailScreen(
                                    viewModel = viewModel,
                                    successStoriesViewModel = successStoriesViewModel,
                                    authViewModel = authViewModel,
                                    petId = petId,
                                    onEditPet = { navController.navigate("pet_form/$petId") },
                                    onOpenPhotos = { navController.navigate("pet_photos/$petId") },
                                    onViewPublicProfile = {
                                        val pet = viewModel.pets.value.firstOrNull { it.id == petId }
                                        pet?.qrCode?.let { qrCode ->
                                            navController.navigate("public_profile/$qrCode")
                                        }
                                    },
                                    onMarkMissing = { navController.navigate("mark_missing/$petId") },
                                    onBack = { tabletSelectedPetId = null },
                                    appStateViewModel = appStateViewModel
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.Pets,
                                        contentDescription = null,
                                        modifier = Modifier.size(64.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                    Spacer(Modifier.height(16.dp))
                                    Text(
                                        text = stringResource(R.string.select_pet_header),
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Phone: existing full-screen list → detail push
                PetsListScreen(
                    viewModel = viewModel,
                    appStateViewModel = appStateViewModel,
                    authViewModel = authViewModel,
                    onPetSelected = { navController.navigate("pet_detail/${it.id}") },
                    onAddPet = { navController.navigate("order_more_tags") },
                    onOrderTags = { navController.navigate("order_more_tags") },
                    onReplacementTag = { petId -> navController.navigate("order_replacement/$petId") },
                    onReferral = { navController.navigate("referral") },
                    onNotifications = { navController.navigate("notifications") },
                    onSuccessStories = onNavigateToSuccessStories,
                    onScanTag = onScanTag,
                    onExploreAccount = onExploreAccount,
                    onQuickMarkMissing = { navController.navigate("quick_mark_missing") }
                )
            }
        }
        composable(
            route = "pet_detail/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
            val pet = viewModel.pets.value.firstOrNull { it.id == petId }
            PetDetailScreen(
                viewModel = viewModel,
                successStoriesViewModel = successStoriesViewModel,
                authViewModel = authViewModel,
                petId = petId,
                onEditPet = { navController.navigate("pet_form/$petId") },
                onOpenPhotos = { navController.navigate("pet_photos/$petId") },
                onOpenVaccinations = { navController.navigate("pet_vaccinations/$petId") },
                onAddVaccination = { navController.navigate("vaccination_form/$petId") },
                onViewPublicProfile = {
                    pet?.qrCode?.let { qrCode ->
                        navController.navigate("public_profile/$qrCode")
                    }
                },
                onMarkMissing = { navController.navigate("mark_missing/$petId") },
                onBack = { navController.popBackStack() },
                appStateViewModel = appStateViewModel
            )
        }
        // Vaccination screens (list / form / detail / edit) share ONE per-pet
        // VaccinationsViewModel, scoped to the pet_detail/{petId} back-stack entry
        // — the lowest entry always present across all of them (the section sits on
        // it; the rest sit above). So edits/deletes reflect everywhere instantly and
        // the detail's by-id lookup is always live, mirroring iOS's PetDetailView-
        // owned VM. INVARIANT: pet_detail/{petId} MUST be in the back stack — Compose
        // getBackStackEntry throws loudly if not, which makes the invariant self-
        // enforcing (slice 4's convergence builds pets→pet_detail→vaccinations).
        composable(
            route = "pet_vaccinations/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
            val parent = remember(petId) { navController.getBackStackEntry("pet_detail/$petId") }
            VaccinationsScreen(
                petId = petId,
                onBack = { navController.popBackStack() },
                onAdd = { navController.navigate("vaccination_form/$petId") },
                onOpenDetail = { id -> navController.navigate("vaccination_detail/$petId/$id") },
                viewModel = hiltViewModel<VaccinationsViewModel>(parent)
            )
        }
        composable(
            route = "vaccination_form/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
            val pet = viewModel.pets.value.firstOrNull { it.id == petId }
            val currentUser by authViewModel.currentUser.collectAsState()
            val parent = remember(petId) { navController.getBackStackEntry("pet_detail/$petId") }
            VaccinationFormScreen(
                petId = petId,
                species = pet?.species ?: "",
                country = currentUser?.country ?: "",
                onBack = { navController.popBackStack() },
                appStateViewModel = appStateViewModel,
                viewModel = hiltViewModel<VaccinationsViewModel>(parent)
            )
        }
        composable(
            route = "vaccination_detail/{petId}/{vaccinationId}",
            arguments = listOf(
                navArgument("petId") { type = NavType.StringType },
                navArgument("vaccinationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
            val vaccId = backStackEntry.arguments?.getString("vaccinationId") ?: return@composable
            val parent = remember(petId) { navController.getBackStackEntry("pet_detail/$petId") }
            VaccinationDetailScreen(
                petId = petId,
                vaccinationId = vaccId,
                onBack = { navController.popBackStack() },
                onEdit = { navController.navigate("vaccination_edit/$petId/$vaccId") },
                appStateViewModel = appStateViewModel,
                viewModel = hiltViewModel<VaccinationsViewModel>(parent)
            )
        }
        composable(
            route = "vaccination_edit/{petId}/{vaccinationId}",
            arguments = listOf(
                navArgument("petId") { type = NavType.StringType },
                navArgument("vaccinationId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
            val vaccId = backStackEntry.arguments?.getString("vaccinationId") ?: return@composable
            val parent = remember(petId) { navController.getBackStackEntry("pet_detail/$petId") }
            VaccinationEditScreen(
                petId = petId,
                vaccinationId = vaccId,
                onBack = { navController.popBackStack() },
                appStateViewModel = appStateViewModel,
                viewModel = hiltViewModel<VaccinationsViewModel>(parent)
            )
        }
        composable(
            route = "public_profile/{qrCode}",
            arguments = listOf(navArgument("qrCode") { type = NavType.StringType })
        ) { backStackEntry ->
            val qrCode = backStackEntry.arguments?.getString("qrCode") ?: return@composable
            PublicPetProfileScreen(
                qrCode = qrCode,
                appStateViewModel = appStateViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "pet_form/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
            PetFormScreen(
                viewModel = viewModel,
                petId = petId,
                appStateViewModel = appStateViewModel,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            route = "pet_photos/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
            val pet = viewModel.pets.value.firstOrNull { it.id == petId }
            PhotoGalleryScreen(
                petId = petId,
                petName = pet?.name ?: "",
                appStateViewModel = appStateViewModel,
                onBack = { navController.popBackStack() },
                onPrimaryPhotoChanged = { photoUrl ->
                    // Update local state immediately with the new primary photo URL
                    viewModel.updatePetProfileImage(petId, photoUrl)
                    // Also try to refresh from server (may fail if offline)
                    viewModel.refresh()
                }
            )
        }
        composable("order_more_tags") {
            OrderMoreTagsScreen(
                appStateViewModel = appStateViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            route = "order_replacement/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
            val pet = viewModel.pets.value.firstOrNull { it.id == petId } ?: return@composable
            OrderReplacementTagScreen(
                pet = pet,
                appStateViewModel = appStateViewModel,
                authViewModel = authViewModel,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }
        composable("referral") {
            ReferralScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("notifications") {
            NotificationsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable(
            route = "mark_missing/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
            val pet = viewModel.pets.value.firstOrNull { it.id == petId } ?: return@composable
            MarkAsMissingScreen(
                pet = pet,
                viewModel = viewModel,
                authViewModel = authViewModel,
                appStateViewModel = appStateViewModel,
                onDismiss = { navController.popBackStack() }
            )
        }
        composable("quick_mark_missing") {
            QuickMarkMissingScreen(
                viewModel = viewModel,
                onPetSelected = { pet -> navController.navigate("mark_missing/${pet.id}") },
                onDismiss = { navController.popBackStack() }
            )
        }
    }
}
