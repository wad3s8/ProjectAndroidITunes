package com.example.projectandroid.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.example.projectandroid.R
import com.example.projectandroid.data.CatalogFilters
import com.example.projectandroid.domain.Track

@Composable
internal fun FiltersScreen(applied: CatalogFilters, onDone: (CatalogFilters) -> Unit) {
    var genre by rememberSaveable(applied.genre) { mutableStateOf(applied.genre) }
    var hideExplicit by rememberSaveable(applied.hideExplicit) { mutableStateOf(applied.hideExplicit) }
    Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).imePadding().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.filters), style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(genre, { genre = it }, Modifier.fillMaxWidth(), singleLine = true,
            label = { Text(stringResource(R.string.genre)) })
        Row(Modifier.fillMaxWidth().toggleable(hideExplicit, role = Role.Switch) { hideExplicit = it },
            verticalAlignment = Alignment.CenterVertically) {
            Text(stringResource(R.string.hide_explicit), Modifier.weight(1f))
            Switch(hideExplicit, onCheckedChange = null)
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = { onDone(CatalogFilters(genre, hideExplicit)) }) { Text(stringResource(R.string.done)) }
            TextButton(onClick = { genre = ""; hideExplicit = false }) { Text(stringResource(R.string.reset)) }
        }
    }
}

@Composable
internal fun FavoritesScreen(tracks: List<Track>?, onTrackClick: (Track) -> Unit) {
    LazyColumn(Modifier.fillMaxSize(), contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item { Text(stringResource(R.string.favorites), style = MaterialTheme.typography.headlineLarge) }
        if (tracks == null) item { CircularProgressIndicator() }
        else if (tracks.isEmpty()) item { Text(stringResource(R.string.empty_favorites)) }
        else items(tracks, key = { it.trackId }) { track -> TrackRow(track) { onTrackClick(track) } }
    }
}
