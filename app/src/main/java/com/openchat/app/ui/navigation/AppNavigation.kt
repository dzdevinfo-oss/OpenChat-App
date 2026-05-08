package com.openchat.app.ui.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Composable
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.openchat.app.ui.screens.*

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
                onNavigateToMemories = { navController.navigate("memory") }
            )
        }
        
        composable(
            route = "workspace/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            WorkspaceScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() },
                onNavigateToFileEditor = { workspaceId, fileId -> navController.navigate("file_editor/$workspaceId/$fileId") }
            )
        }
        
        composable(
            route = "file_editor/{workspaceId}/{fileId}",
            arguments = listOf(
                navArgument("workspaceId") { type = NavType.StringType },
                navArgument("fileId") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val workspaceId = backStackEntry.arguments?.getString("workspaceId") ?: return@composable
            val fileId = backStackEntry.arguments?.getString("fileId") ?: return@composable
            FileEditorScreen(
                workspaceId = workspaceId,
                fileId = fileId,
                onBack = { navController.popBackStack() }
            )
        }
        
        composable("settings") {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        
        composable("memory") {
            MemoriesScreen(onBack = { navController.popBackStack() })
        }
        
        composable(
            route = "recycle_bin/{sessionId}",
            arguments = listOf(navArgument("sessionId") { type = NavType.StringType })
        ) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: return@composable
            RecycleBinScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }
    }
}
