package com.example.projectandroiditunes.presentation

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import coil3.compose.AsyncImage
import com.example.projectandroiditunes.domain.FailureReason
import com.example.projectandroiditunes.domain.Track
import java.util.Locale

@Composable
fun MusicScreen(viewModel: MusicViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MusicContent(
        state = state,
        onQueryChange = viewModel::updateQuery,
        onSearch = viewModel::search,
        onRetrySearch = viewModel::retrySearch,
        onOpenTrack = viewModel::openTrack,
        onBack = viewModel::closeDetails,
        onRetryDetails = viewModel::retryDetails,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MusicContent(
    state: MusicUiState,
    onQueryChange: (String) -> Unit,
    onSearch: () -> Unit,
    onRetrySearch: () -> Unit,
    onOpenTrack: (Long) -> Unit,
    onBack: () -> Unit,
    onRetryDetails: () -> Unit,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    // Kept outside the screen branch so returning from details preserves scroll position.
    val listState = rememberLazyListState()
    val inDetails = state.selectedId != null
    BackHandler(enabled = inDetails, onBack = onBack)
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (inDetails) "О треке" else "iTunes · Музыка") },
                navigationIcon = {
                    if (inDetails) TextButton(onClick = onBack) { Text("Назад") }
                },
            )
        },
    ) { padding ->
        if (inDetails) {
            Box(Modifier.padding(padding).fillMaxSize()) {
                when (val details = state.details) {
                    LoadState.Idle, LoadState.Loading -> Loading("Загружаем трек…")
                    is LoadState.Error -> ErrorPanel(details.reason, onRetryDetails)
                    is LoadState.Content -> TrackDetails(details.value)
                }
            }
        } else {
            Column(Modifier.padding(padding).fillMaxSize().imePadding()) {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Найдите любимую музыку", style = MaterialTheme.typography.headlineSmall)
                    Text("Треки, исполнители и альбомы в каталоге Apple", style = MaterialTheme.typography.bodyMedium)
                    OutlinedTextField(
                        value = state.query,
                        onValueChange = onQueryChange,
                        label = { Text("Трек или исполнитель") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = {
                            if (state.query.isNotBlank() && state.tracks != LoadState.Loading) {
                                keyboard?.hide()
                                onSearch()
                            }
                        }),
                    )
                    Button(
                        onClick = { keyboard?.hide(); onSearch() },
                        enabled = state.query.isNotBlank() && state.tracks != LoadState.Loading,
                        modifier = Modifier.fillMaxWidth(),
                    ) { Text("Найти") }
                }
                Box(Modifier.weight(1f).fillMaxWidth()) {
                    when (val tracks = state.tracks) {
                        LoadState.Idle -> MessagePanel("Начните поиск", "Введите название трека или имя исполнителя и нажмите «Найти».")
                        LoadState.Loading -> Loading("Ищем музыку…")
                        is LoadState.Error -> ErrorPanel(tracks.reason, onRetrySearch)
                        is LoadState.Content -> {
                            if (tracks.value.isEmpty()) {
                                MessagePanel("Ничего не найдено", "По запросу «${state.submittedQuery}» нет треков. Попробуйте другое название.")
                            } else {
                                LazyColumn(
                                    state = listState,
                                    contentPadding = PaddingValues(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(10.dp),
                                ) {
                                    item {
                                        Text("«${state.submittedQuery}» · ${tracks.value.size} треков", style = MaterialTheme.typography.labelLarge)
                                    }
                                    items(tracks.value, key = { it.id }) { track ->
                                        TrackCard(track) { onOpenTrack(track.id) }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TrackCard(track: Track, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Artwork(track, 64.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(track.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(track.artist, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                track.album?.let { Text(it, style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis) }
            }
            Text("›", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

@Composable
private fun Artwork(track: Track, size: Dp) {
    Surface(Modifier.size(size).clip(RoundedCornerShape(12.dp)), color = MaterialTheme.colorScheme.secondaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            Text("♫", style = MaterialTheme.typography.headlineLarge)
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = "Обложка: ${track.album ?: track.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}

@Composable
private fun TrackDetails(track: Track) {
    val uriHandler = LocalUriHandler.current
    val context = LocalContext.current
    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Artwork(track, 180.dp)
        Text(track.title, style = MaterialTheme.typography.headlineMedium)
        Text(track.artist, style = MaterialTheme.typography.titleLarge)
        HorizontalDivider()
        DetailRow("Альбом", track.album)
        DetailRow("Жанр", track.genre)
        DetailRow("Дата выхода", track.releaseDate)
        DetailRow("Длительность", track.durationMillis?.let {
            val seconds = it / 1000
            String.format(Locale.ROOT, "%d:%02d", seconds / 60, seconds % 60)
        })
        DetailRow("Цена", track.price?.let { String.format(Locale.ROOT, "%.2f %s", it, track.currency.orEmpty()) })
        track.storeUrl?.takeIf { it.startsWith("https://") }?.let { url ->
            Button(onClick = {
                try {
                    uriHandler.openUri(url)
                } catch (_: Exception) {
                    Toast.makeText(context, "Не удалось открыть ссылку", Toast.LENGTH_SHORT).show()
                }
            }, modifier = Modifier.fillMaxWidth()) { Text("Открыть в iTunes") }
        }
    }
}

@Composable
private fun DetailRow(label: String, value: String?) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
        Text(value?.takeIf { it.isNotBlank() } ?: "Нет данных", style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun Loading(message: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        CircularProgressIndicator()
        Spacer(Modifier.height(16.dp))
        Text(message)
    }
}

@Composable
private fun MessagePanel(title: String, message: String) {
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Spacer(Modifier.height(12.dp))
        Text(message)
    }
}

@Composable
private fun ErrorPanel(reason: FailureReason, onRetry: () -> Unit) {
    val message = when (reason) {
        FailureReason.NETWORK -> "Не удалось подключиться. Проверьте интернет и повторите запрос."
        FailureReason.TIMEOUT -> "Сервер долго не отвечает. Попробуйте ещё раз."
        FailureReason.RATE_LIMIT -> "Слишком много запросов. Подождите минуту и повторите."
        FailureReason.SERVER -> "iTunes временно недоступен. Попробуйте позже."
        FailureReason.INVALID_RESPONSE -> "Не удалось прочитать ответ iTunes. Повторите запрос."
        FailureReason.NOT_FOUND -> "Этот трек больше не доступен в каталоге."
        FailureReason.UNKNOWN -> "Не удалось загрузить данные. Попробуйте ещё раз."
    }
    Column(Modifier.fillMaxSize().padding(24.dp), verticalArrangement = Arrangement.Center, horizontalAlignment = Alignment.CenterHorizontally) {
        Text("Не удалось загрузить", style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.error)
        Spacer(Modifier.height(12.dp))
        Text(message)
        Spacer(Modifier.height(16.dp))
        Button(onClick = onRetry) { Text("Повторить") }
    }
}