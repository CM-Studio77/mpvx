package com.cmstudio.player.ui.music

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.model.UserPlaylist
import com.cmstudio.player.ui.theme.extendedColors
import java.util.Locale
import kotlin.math.roundToInt

data class AudioTrackSortSeed(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long
)

/**
 * Pure-JVM sorter supporting Title, Artist, Album, Duration, and Id/Date.
 */
fun <T> sortAudioItems(
    items: List<T>,
    sortBy: String,
    ascending: Boolean,
    idOf: (T) -> String,
    titleOf: (T) -> String,
    artistOf: (T) -> String,
    albumOf: (T) -> String,
    durationOf: (T) -> Long
): List<T> {
    val comparator: Comparator<T> = when (sortBy.trim().uppercase()) {
        "ARTIST" -> compareBy(String.CASE_INSENSITIVE_ORDER, artistOf)
        "ALBUM" -> compareBy(String.CASE_INSENSITIVE_ORDER, albumOf)
        "DURATION" -> compareBy(durationOf)
        "DATE" -> compareBy(idOf)
        else -> compareBy(String.CASE_INSENSITIVE_ORDER, titleOf)
    }
    val sorted = items.sortedWith(comparator.thenBy(String.CASE_INSENSITIVE_ORDER, titleOf))
    return if (ascending) sorted else sorted.reversed()
}

fun sortMusicTracks(
    tracks: List<RealAudioTrack>,
    sortBy: String,
    ascending: Boolean
): List<RealAudioTrack> = sortAudioItems(
    items = tracks,
    sortBy = sortBy,
    ascending = ascending,
    idOf = { it.id },
    titleOf = { it.title },
    artistOf = { it.artist },
    albumOf = { it.album },
    durationOf = { it.durationMs }
)

fun musicTabDisplayLabel(tabId: String): String = when (tabId) {
    "SONGS" -> "Songs"
    "ALBUMS" -> "Albums"
    "ARTISTS" -> "Artists"
    "PLAYLISTS" -> "Playlists"
    "FOLDERS" -> "Folders"
    "QUEUE" -> "Queue"
    else -> tabId
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MusicSortAndViewDialog(onDismiss: () -> Unit) {
    val sortBy by AppSettings.musicSortBy.collectAsState()
    val sortAsc by AppSettings.musicSortAscending.collectAsState()
    val gridView by AppSettings.musicGridView.collectAsState()
    val artSizeDp by AppSettings.musicArtSizeDp.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Music Sort & Display", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Sort Songs By", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    listOf("TITLE" to "Title", "ARTIST" to "Artist", "ALBUM" to "Album", "DURATION" to "Duration", "DATE" to "Added").forEach { (id, label) ->
                        FilterChip(selected = sortBy == id, onClick = { AppSettings.setMusicSort(id, sortAsc) }, label = { Text(label) })
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = sortAsc, onClick = { AppSettings.setMusicSort(sortBy, true) }, label = { Text("Ascending") })
                    FilterChip(selected = !sortAsc, onClick = { AppSettings.setMusicSort(sortBy, false) }, label = { Text("Descending") })
                }

                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("View Layout", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = !gridView,
                        onClick = { AppSettings.setMusicGridView(false) },
                        leadingIcon = { Icon(Icons.Rounded.ViewAgenda, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("List") }
                    )
                    FilterChip(
                        selected = gridView,
                        onClick = { AppSettings.setMusicGridView(true) },
                        leadingIcon = { Icon(Icons.Rounded.GridView, contentDescription = null, modifier = Modifier.size(16.dp)) },
                        label = { Text("Grid") }
                    )
                }

                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Album Art Size", fontWeight = FontWeight.SemiBold)
                    Text("${artSizeDp} dp", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = artSizeDp.toFloat().coerceIn(56f, 126f),
                    onValueChange = { AppSettings.setMusicArtSizeDp(it.roundToInt()) },
                    valueRange = 56f..126f
                )
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun MusicTabCustomizerDialog(onDismiss: () -> Unit) {
    val tabOrder by AppSettings.musicTabOrder.collectAsState()
    val hiddenTabs by AppSettings.musicHiddenTabs.collectAsState()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Customize Music Tabs", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Show, hide, or reorder Music tabs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                tabOrder.forEachIndexed { idx, tabId ->
                    val visible = tabId !in hiddenTabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = visible,
                            enabled = tabId != "SONGS",
                            onCheckedChange = { AppSettings.toggleMusicTabVisibility(tabId) }
                        )
                        Text(
                            text = musicTabDisplayLabel(tabId),
                            modifier = Modifier.weight(1f),
                            fontWeight = FontWeight.SemiBold
                        )
                        IconButton(onClick = { AppSettings.moveMusicTab(tabId, -1) }, enabled = idx > 0, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                        }
                        IconButton(onClick = { AppSettings.moveMusicTab(tabId, +1) }, enabled = idx < tabOrder.lastIndex, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) { Text("Done") }
        }
    )
}

@Composable
fun AudioTrackGrid(
    tracks: List<RealAudioTrack>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onTrackClick: (Int) -> Unit
) {
    val context = LocalContext.current
    LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 148.dp),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        itemsIndexed(tracks, key = { _, item -> item.id }) { index, track ->
            val active = track.id == currentTrackId
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onTrackClick(index) },
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (active) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.extendedColors.cardBackground
                ),
                border = BorderStroke(
                    1.dp,
                    if (active) MaterialTheme.colorScheme.primary else MaterialTheme.extendedColors.cardBorder
                )
            ) {
                Column(modifier = Modifier.padding(10.dp)) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(34.dp))
                        if (track.artworkUri != null) {
                            AsyncImage(
                                model = ImageRequest.Builder(context).data(track.artworkUri).crossfade(true).build(),
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(track.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text("${track.artist} • ${track.durationText}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun MusicPlaylistsTabContent(
    scannedSongs: List<RealAudioTrack>,
    onPlayQueue: (List<RealAudioTrack>, Int, Boolean) -> Unit
) {
    val playlists by AppSettings.musicPlaylists.collectAsState()
    var activePlaylistId by remember { mutableStateOf<String?>(null) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var showAddTracksDialog by remember { mutableStateOf(false) }

    val trackByIdOrUri = remember(scannedSongs) {
        scannedSongs.associateBy { it.uri.toString() } + scannedSongs.associateBy { it.id }
    }
    val activePlaylist = playlists.firstOrNull { it.id == activePlaylistId }

    if (activePlaylist != null) {
        val resolved = remember(activePlaylist, trackByIdOrUri) {
            activePlaylist.itemUris.mapNotNull { key -> trackByIdOrUri[key] }
        }
        Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { activePlaylistId = null }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(activePlaylist.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("${resolved.size} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { showAddTracksDialog = true }) {
                    Icon(Icons.Rounded.Add, contentDescription = "Add Songs", tint = MaterialTheme.colorScheme.primary)
                }
            }

            if (resolved.isNotEmpty()) {
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onPlayQueue(resolved, 0, false) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All")
                    }
                    OutlinedButton(
                        onClick = { onPlayQueue(resolved.shuffled(), 0, true) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }
            }

            if (resolved.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    OutlinedButton(onClick = { showAddTracksDialog = true }) {
                        Icon(Icons.AutoMirrored.Rounded.PlaylistAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Add Songs to Playlist")
                    }
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp), contentPadding = PaddingValues(bottom = 100.dp)) {
                    items(resolved, key = { it.id }) { track ->
                        val uriStr = track.uri.toString()
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Box(modifier = Modifier.weight(1f)) {
                                RealAudioTrackCard(
                                    track = track,
                                    isPlaying = false,
                                    isCurrent = false,
                                    onClick = { onPlayQueue(resolved, resolved.indexOf(track).coerceAtLeast(0), false) }
                                )
                            }
                            IconButton(onClick = { AppSettings.removeTrackFromMusicPlaylist(activePlaylist.id, uriStr) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(bottom = 100.dp)
        ) {
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Music Playlists (${playlists.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Button(onClick = { showCreateDialog = true }, shape = CircleShape, contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New")
                    }
                }
            }
            if (playlists.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(28.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(40.dp))
                            Text("Create Your First Music Mix", fontWeight = FontWeight.Bold)
                            Text("Organize songs into custom audio playlists with 4-cover artwork collages.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { showCreateDialog = true }) { Text("Create Playlist") }
                        }
                    }
                }
            } else {
                items(playlists, key = { it.id }) { pl ->
                    val artModels = remember(pl, trackByIdOrUri) {
                        pl.itemUris.mapNotNull { trackByIdOrUri[it]?.artworkUri }.distinct().take(4)
                    }
                    Card(
                        modifier = Modifier.fillMaxWidth().clickable { activePlaylistId = pl.id },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            FourArtMosaicBox(models = artModels, size = 54.dp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(pl.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                                Text("${pl.itemUris.size} tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            IconButton(onClick = { AppSettings.deleteMusicPlaylist(pl.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("New Music Playlist", fontWeight = FontWeight.Bold) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it }, placeholder = { Text("Workout, Focus...") }, singleLine = true, modifier = Modifier.fillMaxWidth()) },
            confirmButton = {
                Button(onClick = {
                    if (name.isNotBlank()) {
                        val created = AppSettings.createMusicPlaylist(name)
                        showCreateDialog = false
                        activePlaylistId = created.id
                    }
                }, enabled = name.isNotBlank()) { Text("Create") }
            },
            dismissButton = { TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") } }
        )
    }

    if (showAddTracksDialog && activePlaylist != null) {
        AlertDialog(
            onDismissRequest = { showAddTracksDialog = false },
            title = { Text("Add Songs to ${activePlaylist.name}", fontWeight = FontWeight.Bold) },
            text = {
                LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    items(scannedSongs, key = { it.id }) { song ->
                        val uriStr = song.uri.toString()
                        val included = uriStr in activePlaylist.itemUris
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (included) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                .clickable {
                                    if (included) AppSettings.removeTrackFromMusicPlaylist(activePlaylist.id, uriStr)
                                    else AppSettings.addTrackToMusicPlaylist(activePlaylist.id, uriStr)
                                }
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(song.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(song.artist, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                            }
                            if (included) Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                }
            },
            confirmButton = { Button(onClick = { showAddTracksDialog = false }) { Text("Done") } }
        )
    }
}

/**
 * Shared 2x2 Mosaic Collage Box displaying up to 4 item artwork/thumbnails cleanly.
 */
@Composable
fun FourArtMosaicBox(
    models: List<Any>,
    size: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        if (models.isEmpty()) {
            Icon(Icons.AutoMirrored.Rounded.PlaylistPlay, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
        } else if (models.size < 4) {
            AsyncImage(
                model = ImageRequest.Builder(context).data(models.first()).crossfade(true).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            val half = size / 2
            Column(modifier = Modifier.fillMaxSize()) {
                Row(modifier = Modifier.fillMaxWidth().height(half)) {
                    AsyncImage(model = ImageRequest.Builder(context).data(models[0]).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(half))
                    AsyncImage(model = ImageRequest.Builder(context).data(models[1]).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(half))
                }
                Row(modifier = Modifier.fillMaxWidth().height(half)) {
                    AsyncImage(model = ImageRequest.Builder(context).data(models[2]).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(half))
                    AsyncImage(model = ImageRequest.Builder(context).data(models[3]).crossfade(true).build(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.size(half))
                }
            }
        }
    }
}

@Composable
fun MusicNowPlayingQuickToolsRow(
    sleepRemainingMs: Long,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        OutlinedButton(onClick = onOpenEqualizer, shape = CircleShape) {
            Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text("Equalizer", style = MaterialTheme.typography.labelMedium)
        }
        Spacer(modifier = Modifier.width(10.dp))
        OutlinedButton(onClick = onOpenSleepTimer, shape = CircleShape) {
            Icon(Icons.Rounded.Bedtime, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = if (sleepRemainingMs > 0L) {
                    val sec = sleepRemainingMs / 1000L
                    String.format(Locale.US, "%02d:%02d", sec / 60L, sec % 60L)
                } else "Sleep Timer",
                style = MaterialTheme.typography.labelMedium
            )
        }
    }
}
