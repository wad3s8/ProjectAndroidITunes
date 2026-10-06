package com.example.projectandroid.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.example.projectandroid.R
import com.example.projectandroid.data.MockTrackRepository

private enum class Tab(val route: String, val label: Int, val icon: Int) {
    Music("music", R.string.music, R.drawable.ic_music),
    Favorites("favorites", R.string.favorites, R.drawable.ic_heart),
    Settings("settings", R.string.settings, R.drawable.ic_settings),
}

@Composable
fun MusicApp() {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    Scaffold(
        bottomBar = {
            NavigationBar {
                Tab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = entry?.destination?.hierarchy?.any { it.route == tab.route } == true,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(painterResource(tab.icon), contentDescription = null) },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Tab.Music.route, modifier = Modifier.padding(padding)) {
            navigation(startDestination = "catalog", route = Tab.Music.route) {
                composable("catalog") {
                    val model: CatalogViewModel = viewModel(factory = viewModelFactory {
                        initializer { CatalogViewModel(MockTrackRepository) }
                    })
                    val state by model.uiState.collectAsStateWithLifecycle()
                    CatalogScreen(state) { id -> navController.navigate("track/$id") }
                }
                composable(
                    "track/{trackId}",
                    arguments = listOf(navArgument("trackId") { type = NavType.LongType }),
                ) {
                    val model: DetailsViewModel = viewModel(factory = viewModelFactory {
                        initializer { DetailsViewModel(MockTrackRepository, createSavedStateHandle()) }
                    })
                    val state by model.uiState.collectAsStateWithLifecycle()
                    DetailsScreen(state, onBack = { navController.popBackStack() })
                }
            }
            composable(Tab.Favorites.route) {
                PlaceholderScreen(R.string.favorites, R.string.favorites_later, R.drawable.ic_heart)
            }
            composable(Tab.Settings.route) {
                PlaceholderScreen(R.string.settings, R.string.settings_later, R.drawable.ic_settings)
            }
        }
    }
}

