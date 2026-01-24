package com.petsafety.app.ui.screens

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.petsafety.app.ui.viewmodel.AppStateViewModel
import com.petsafety.app.ui.viewmodel.PetsViewModel
import com.petsafety.app.ui.viewmodel.SuccessStoriesViewModel

@Composable
fun PetsScreen(appStateViewModel: AppStateViewModel) {
    val navController = rememberNavController()
    val viewModel: PetsViewModel = hiltViewModel()
    val successStoriesViewModel: SuccessStoriesViewModel = hiltViewModel()

    NavHost(navController = navController, startDestination = "pets_list") {
        composable("pets_list") {
            PetsListScreen(
                viewModel = viewModel,
                appStateViewModel = appStateViewModel,
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
            PetDetailScreen(
                viewModel = viewModel,
                successStoriesViewModel = successStoriesViewModel,
                petId = petId,
                onEditPet = { navController.navigate("pet_form/$petId") },
                onOpenPhotos = { navController.navigate("pet_photos/$petId") },
                onBack = { navController.popBackStack() },
                appStateViewModel = appStateViewModel
            )
        }
        composable("pet_form") {
            PetFormScreen(
                viewModel = viewModel,
                appStateViewModel = appStateViewModel,
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
                onDone = { navController.popBackStack() }
            )
        }
        composable(
            route = "pet_photos/{petId}",
            arguments = listOf(navArgument("petId") { type = NavType.StringType })
        ) { backStackEntry ->
            val petId = backStackEntry.arguments?.getString("petId") ?: return@composable
            PhotoGalleryScreen(
                petId = petId,
                appStateViewModel = appStateViewModel,
                onBack = { navController.popBackStack() }
            )
        }
        composable("order_more_tags") {
            OrderMoreTagsScreen(
                appStateViewModel = appStateViewModel,
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
                onDone = { navController.popBackStack() }
            )
        }
    }
}
