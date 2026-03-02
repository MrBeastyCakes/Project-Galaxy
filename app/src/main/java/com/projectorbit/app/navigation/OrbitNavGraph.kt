package com.projectorbit.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.projectorbit.ui.GalaxyScreen
import com.projectorbit.ui.viewmodel.GalaxyViewModel

/**
 * Root Compose Navigation graph.
 *
 * Zoom-level navigation is handled within GalaxyScreen itself (surface editor
 * overlays on top of the canvas rather than navigating away), so the nav graph
 * is intentionally simple — one primary destination.
 */
@Composable
fun OrbitNavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()

    NavHost(
        navController = navController,
        startDestination = OrbitDestinations.GALAXY,
        modifier = modifier
    ) {
        composable(OrbitDestinations.GALAXY) {
            GalaxyScreen()
        }
    }
}

object OrbitDestinations {
    const val GALAXY = "galaxy"
}
