package com.petsafety.app.ui.screens

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.AuthViewModel
import com.petsafety.app.ui.viewmodel.PetsViewModel
import com.petsafety.app.ui.viewmodel.SuccessStoriesViewModel

@Composable
fun PetsScreen(appStateViewModel: AppStateViewModel, authViewModel: AuthViewModel, modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val viewModel: PetsViewModel = hiltViewModel()
    val successStoriesViewModel: SuccessStoriesViewModel = hiltViewModel()

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
            PetsListScreen(
                viewModel = viewModel,
                appStateViewModel = appStateViewModel,
                authViewModel = authViewModel,
                onPetSelected = { navController.navigate("pet_detail/${it.id}") },
                onAddPet = { navController.navigate("pet_form") },
                onOrderTags = { navController.navigate("order_more_tags") },
                onReplacementTag = { petId -> navController.navigate("order_replacement/$petId") }
            )
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
                onViewPublicProfile = {
                    pet?.qrCode?.let { qrCode ->
                        navController.navigate("public_profile/$qrCode")
                    }
                },
                onBack = { navController.popBackStack() },
                appStateViewModel = appStateViewModel
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
        composable("pet_form") {
            PetFormScreen(
                viewModel = viewModel,
                appStateViewModel = appStateViewModel,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
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
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() }
            )
        }
    }
}
