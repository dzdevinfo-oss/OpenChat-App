package com.openchat.app.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NamedNavArgument
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openchat.app.ui.screens.ChatScreen

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    
    NavHost(
        navController = navController,
        startDestination = "chat/new",
        enterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
        },
        exitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Left, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        },
        popEnterTransition = {
            slideIntoContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) + fadeIn(animationSpec = tween(300))
        },
        popExitTransition = {
            slideOutOfContainer(AnimatedContentTransitionScope.SlideDirection.Right, animationSpec = tween(300)) + fadeOut(animationSpec = tween(300))
        }
    ) {
        composable(
            route = "chat/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: "new"
            ChatScreen(
                sessionId = sessionId,
                onNavigateToWorkspace = { id -> navController.navigate("workspace/$id") },
                onNavigateToSettings = { navController.navigate("settings") },
                onNavigateToApiConfig = { navController.navigate("api_config") },
                onNavigateToCustomModels = { navController.navigate("custom_models") },
                onNavigateToMemories = { navController.navigate("memories") },
                onNavigateToChat = { id -> 
                    navController.navigate("chat/$id") {
                        popUpTo("chat") { inclusive = true }
                    }
                }
            )
        }
        
        composable(
            route = "workspace/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            com.openchat.app.ui.screens.WorkspaceScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }
        composable("settings") {
            com.openchat.app.ui.screens.SettingsScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("api_config") {
            com.openchat.app.ui.screens.ApiConfigBottomSheet(
                onDismiss = { navController.popBackStack() }
            )
        }
        composable("custom_models") {
            com.openchat.app.ui.screens.CustomModelsBottomSheet(
                onDismiss = { navController.popBackStack() }
            )
        }
        composable("memories") {
            com.openchat.app.ui.screens.MemoriesScreen(
                onBack = { navController.popBackStack() }
            )
        }
    }
}
