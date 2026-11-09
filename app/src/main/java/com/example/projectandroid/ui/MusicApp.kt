package com.example.projectandroid.ui

import android.database.sqlite.SQLiteException
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import com.example.projectandroid.AppContainer
import com.example.projectandroid.domain.GetTrackUseCase
import com.example.projectandroid.domain.SearchTracksUseCase
import com.example.projectandroid.domain.Track
import com.example.projectandroid.MusicApplication
import com.example.projectandroid.R
import kotlinx.coroutines.launch

private enum class Tab(val route: String, val label: Int, val icon: Int) {
    Music("music", R.string.music, R.drawable.ic_music),
    Favorites("favorites", R.string.favorites, R.drawable.ic_heart),
    Settings("settings", R.string.settings, R.drawable.ic_settings),
}

@Composable
fun MusicApp(
    searchTracks: SearchTracksUseCase = AppContainer.searchTracks,
    getTrack: GetTrackUseCase = AppContainer.getTrack,
) {
    val navController = rememberNavController()
    val entry by navController.currentBackStackEntryAsState()
    val application = LocalContext.current.applicationContext as MusicApplication
    val cache = application.filters
    val filters by cache.applied.collectAsStateWithLifecycle()
    val catalog: CatalogViewModel = viewModel(factory = viewModelFactory {
        initializer { CatalogViewModel(searchTracks, createSavedStateHandle(), cache.applied.value) }
    })
    val favorites by application.favorites.tracks.collectAsStateWithLifecycle(initialValue = null)
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val saveError = stringResource(R.string.favorite_write_error)
    val changeFavorite: (Track, Boolean) -> Unit = { track, selected ->
        scope.launch {
            try {
                application.favorites.setFavorite(track, selected)
            } catch (error: SQLiteException) {
                snackbar.showSnackbar(saveError)
            }
        }
    }
    Scaffold(
        snackbarHost = { SnackbarHost(snackbar) },
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
                        icon = {
                            BadgedBox(badge = {
                                if (tab == Tab.Settings && filters.active) {
                                    val description = stringResource(R.string.active_filters)
                                    Badge(modifier = Modifier.semantics { contentDescription = description },
                                        containerColor = Color(0xFFF58A12))
                                }
                            }) { Icon(painterResource(tab.icon), contentDescription = null) }
                        },
                        label = { Text(stringResource(tab.label)) },
                    )
                }
            }
        },
    ) { padding ->
        NavHost(navController, startDestination = Tab.Music.route, modifier = Modifier.padding(padding)) {
            navigation(startDestination = "catalog", route = Tab.Music.route) {
                composable("catalog") {
                    val state by catalog.uiState.collectAsStateWithLifecycle()
                    CatalogScreen(state, catalog::changeQuery, catalog::search, catalog::retry) { id -> navController.navigate("track/$id") }
                }
                composable(
                    "track/{trackId}",
                    arguments = listOf(navArgument("trackId") { type = NavType.LongType }),
                ) {
                    val model: DetailsViewModel = viewModel(factory = viewModelFactory {
                        initializer { DetailsViewModel(getTrack, createSavedStateHandle()) }
                    })
                    val state by model.uiState.collectAsStateWithLifecycle()
                    val id = (state as? DetailsUiState.Content)?.track?.trackId
                    val saved = favorites?.any { it.trackId == id }
                    DetailsScreen(state, onBack = { navController.popBackStack() }, onRetry = model::retry,
                        isFavorite = saved, onFavoriteClick = { changeFavorite(it, saved != true) })
                }
            }
            navigation(startDestination = "favorite_list", route = Tab.Favorites.route) {
                composable("favorite_list") {
                    FavoritesScreen(favorites) { navController.navigate("favorite/${it.trackId}") }
                }
                composable("favorite/{trackId}", arguments = listOf(navArgument("trackId") { type = NavType.LongType })) { entry ->
                    val id = entry.arguments?.getLong("trackId")
                    val track = favorites?.find { it.trackId == id }
                    val state = when {
                        favorites == null -> DetailsUiState.Loading
                        track == null -> DetailsUiState.NotFound
                        else -> DetailsUiState.Content(track)
                    }
                    DetailsScreen(state, onBack = { navController.popBackStack() }, onRetry = {},
                        isFavorite = true, onFavoriteClick = {
                            changeFavorite(it, false)
                            navController.popBackStack()
                        })
                }
            }
            composable(Tab.Settings.route) {
                FiltersScreen(filters) { draft ->
                    cache.apply(draft)
                    catalog.applyFilters(cache.applied.value)
                    navController.navigate("catalog") {
                        popUpTo("catalog")
                        launchSingleTop = true
                    }
                }
            }
        }
    }
}
