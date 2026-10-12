package com.example.projectandroid.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import androidx.constraintlayout.compose.Dimension
import com.example.projectandroid.R
import com.example.projectandroid.data.Track

@Composable
fun CatalogScreen(state: CatalogUiState, onTrackClick: (Long) -> Unit) {
    LazyColumn(
        state = rememberLazyListState(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize().testTag("track_list"),
    ) {
        item(key = "heading") {
            Column(Modifier.padding(bottom = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.catalog_eyebrow), style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary)
                Text(stringResource(R.string.music), style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.catalog_subtitle), style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item(key = "collection") {
            Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                Column(Modifier.fillMaxWidth().padding(22.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("DAFT PUNK", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Black)
                    Text(stringResource(R.string.collection_description), style = MaterialTheme.typography.bodyMedium)
                    Text(stringResource(R.string.track_count, state.tracks.size),
                        style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        item(key = "list_title") {
            Text(stringResource(R.string.all_tracks), Modifier.padding(top = 14.dp, bottom = 2.dp),
                style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        }
        items(state.tracks, key = { it.trackId }, contentType = { "track" }) { track ->
            TrackRow(track) { onTrackClick(track.trackId) }
        }
    }
}

@Composable
private fun TrackRow(track: Track, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth().testTag("track_${track.trackId}"),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            AlbumArtwork(track, Modifier.size(60.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(track.trackName, style = MaterialTheme.typography.titleSmall, maxLines = 2,
                    overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                Text(track.artistName, style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(track.collectionName, style = MaterialTheme.typography.labelSmall, maxLines = 1,
                    overflow = TextOverflow.Ellipsis, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(formatDuration(track.trackTimeMillis), style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Decorative local vinyl artwork: deliberately no remote image loading. */
@Composable
private fun AlbumArtwork(track: Track, modifier: Modifier = Modifier) {
    val colors = if (track.collectionName == "Discovery") {
        listOf(Color(0xFF263C78), Color(0xFF9861B0), Color(0xFFE0988C))
    } else {
        listOf(Color(0xFF182E32), Color(0xFF58777B), Color(0xFFD2B485))
    }
    Canvas(modifier.clip(RoundedCornerShape(16.dp)).background(Brush.linearGradient(colors))) {
        val radius = size.minDimension * .36f
        val center = Offset(size.width * .55f, size.height * .5f)
        drawCircle(Color(0xFF152126), radius, center)
        for (index in 1..5) {
            drawCircle(Color.White.copy(alpha = .14f), radius * (.4f + index * .1f),
                center, style = Stroke(size.minDimension * .004f))
        }
        drawCircle(colors.last(), radius * .27f, center)
        drawCircle(Color(0xFF152126), radius * .055f, center)
        drawLine(Color.White.copy(alpha = .45f), Offset(size.width * .1f, size.height * .86f),
            Offset(size.width * .42f, size.height * .86f), size.minDimension * .014f)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DetailsScreen(state: DetailsUiState, onBack: () -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TopAppBar(
            title = { Text(stringResource(R.string.track_details)) },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(painterResource(R.drawable.ic_back), stringResource(R.string.back))
                }
            },
            windowInsets = WindowInsets(0, 0, 0, 0),
        )
        when (state) {
            DetailsUiState.NotFound -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.track_not_found))
            }
            is DetailsUiState.Content -> TrackDetails(state.track)
        }
    }
}

@Composable
private fun TrackDetails(track: Track) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).testTag("track_details")
            .padding(start = 20.dp, end = 20.dp, bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        ConstraintLayout(Modifier.widthIn(max = 420.dp).fillMaxWidth().align(Alignment.CenterHorizontally)) {
            val (art, badge, title, artist, album) = createRefs()
            AlbumArtwork(track, Modifier
                .constrainAs(art) {
                    top.linkTo(parent.top, 12.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                }.aspectRatio(1.25f))
            Surface(
                color = MaterialTheme.colorScheme.tertiaryContainer,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.constrainAs(badge) {
                    bottom.linkTo(art.bottom, 12.dp)
                    end.linkTo(art.end, 12.dp)
                },
            ) {
                Text(stringResource(R.string.song_badge), Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    style = MaterialTheme.typography.labelMedium)
            }
            Text(track.trackName, style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold, modifier = Modifier.constrainAs(title) {
                    top.linkTo(art.bottom, 24.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                })
            Text(track.artistName, style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary, modifier = Modifier.constrainAs(artist) {
                    top.linkTo(title.bottom, 8.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                })
            Text(track.collectionName, style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.constrainAs(album) {
                    top.linkTo(artist.bottom, 6.dp)
                    start.linkTo(parent.start)
                    end.linkTo(parent.end)
                    width = Dimension.fillToConstraints
                })
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Metric(stringResource(R.string.duration), formatDuration(track.trackTimeMillis), Modifier.weight(1f))
            Metric(stringResource(R.string.price), formatPrice(track.trackPrice, track.currency), Modifier.weight(1f))
        }
        Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surfaceContainerLow) {
            Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(stringResource(R.string.about_track), style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold)
                Metadata(stringResource(R.string.genre), track.primaryGenreName ?: "—")
                HorizontalDivider()
                Metadata(stringResource(R.string.release_date), formatReleaseDate(track.releaseDate))
                HorizontalDivider()
                Metadata(stringResource(R.string.album_number),
                    stringResource(R.string.track_position, track.trackNumber, track.trackCount))
                HorizontalDivider()
                Metadata(stringResource(R.string.store_country), track.country ?: "—")
                HorizontalDivider()
                Metadata(stringResource(R.string.content_rating), when (track.trackExplicitness) {
                    "explicit" -> stringResource(R.string.explicit_content)
                    "notExplicit", "cleaned" -> stringResource(R.string.clean_content)
                    else -> "—"
                })
            }
        }
        Text(stringResource(R.string.demo_notice), style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier) {
    Surface(modifier, shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.secondaryContainer) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(label, style = MaterialTheme.typography.labelMedium)
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun Metadata(label: String, value: String) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun PlaceholderScreen(title: Int, description: Int, icon: Int) {
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(28.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Surface(shape = RoundedCornerShape(24.dp), color = MaterialTheme.colorScheme.primaryContainer) {
            Icon(painterResource(icon), null, Modifier.padding(24.dp).size(40.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer)
        }
        Spacer(Modifier.height(24.dp))
        Text(stringResource(title), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(stringResource(description), style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

