package com.cmstudio.player.ui.music

import android.app.Activity
import android.content.pm.ActivityInfo
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Lyrics
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Radio
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ViewWeek
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.player.EqualizerBottomSheet
import com.cmstudio.player.ui.player.EqualizerPresetsCatalog
import com.cmstudio.player.ui.player.MpvxSeekbarCanvas
import com.cmstudio.player.ui.player.SleepTimerBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MusicScreen(
    onTrackPlay: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var searchQuery by remember { mutableStateOf("") }
    var searchActive by remember { mutableStateOf(false) }

    val tabOrder by AppSettings.musicTabOrder.collectAsState()
    val hiddenTabs by AppSettings.musicHiddenTabs.collectAsState()
    val musicSortBy by AppSettings.musicSortBy.collectAsState()
    val musicSortAsc by AppSettings.musicSortAscending.collectAsState()
    val musicGridView by AppSettings.musicGridView.collectAsState()
    val musicArtSizeDp by AppSettings.musicArtSizeDp.collectAsState()

    val visibleTabs = remember(tabOrder, hiddenTabs) {
        tabOrder.filterNot { it in hiddenTabs }.ifEmpty { listOf("SONGS") }
    }
    val pagerState = rememberPagerState(pageCount = { visibleTabs.size })

    var showSortDialog by remember { mutableStateOf(false) }
    var showTabCustomizerDialog by remember { mutableStateOf(false) }
    var showMusicEqSheet by remember { mutableStateOf(false) }
    var showMusicSleepSheet by remember { mutableStateOf(false) }

    var scannedSongs by remember { mutableStateOf<List<RealAudioTrack>>(emptyList()) }
    var scannedAlbums by remember { mutableStateOf<List<RealAudioAlbum>>(emptyList()) }
    var scannedArtists by remember { mutableStateOf<List<RealAudioArtist>>(emptyList()) }
    var scannedFolders by remember { mutableStateOf<List<RealAudioFolder>>(emptyList()) }
    var selectedGroupTitle by remember { mutableStateOf<String?>(null) }
    var selectedGroupTracks by remember { mutableStateOf<List<RealAudioTrack>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    val activeQueue by AudioPlayerManager.playlist.collectAsState()
    val currentPlayingTrack by AudioPlayerManager.currentTrack.collectAsState()
    val isPlaying by AudioPlayerManager.isPlaying.collectAsState()
    val currentPositionMs by AudioPlayerManager.currentPositionMs.collectAsState()
    val durationMs by AudioPlayerManager.durationMs.collectAsState()
    val isShuffle by AudioPlayerManager.isShuffle.collectAsState()
    val isRepeat by AudioPlayerManager.isRepeat.collectAsState()
    val sleepRemainingMs by AudioPlayerManager.sleepTimerRemainingMs.collectAsState()

    var showNowPlayingSheet by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        AudioPlayerManager.init(context)
        withContext(Dispatchers.IO) {
            val songs = AudioLibrary.scanAudio(context)
            scannedSongs = songs
            scannedAlbums = AudioLibrary.groupAlbums(songs)
            scannedArtists = AudioLibrary.groupArtists(songs)
            scannedFolders = AudioLibrary.groupFolders(songs)
            isLoading = false
        }
    }

    LaunchedEffect(visibleTabs.size) {
        val maxPage = visibleTabs.lastIndex.coerceAtLeast(0)
        if (pagerState.currentPage > maxPage) {
            pagerState.scrollToPage(maxPage)
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        selectedGroupTitle = null
        selectedGroupTracks = emptyList()
    }

    val filteredSongs = remember(scannedSongs, searchQuery, musicSortBy, musicSortAsc) {
        val base = if (searchQuery.isBlank()) scannedSongs
        else scannedSongs.filter {
            it.title.contains(searchQuery, ignoreCase = true) ||
                    it.artist.contains(searchQuery, ignoreCase = true) ||
                    it.album.contains(searchQuery, ignoreCase = true)
        }
        sortMusicTracks(base, musicSortBy, musicSortAsc)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "Music",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surfaceContainerHigh
                    ) {
                        Text(
                            text = "Local Device",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                Text(
                    text = "${scannedSongs.size} Tracks \u2022 ${scannedAlbums.size} Albums",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showSortDialog = true }) {
                Icon(
                    imageVector = Icons.Rounded.Tune,
                    contentDescription = "Sort and Display",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { showTabCustomizerDialog = true }) {
                Icon(
                    imageVector = Icons.Rounded.ViewWeek,
                    contentDescription = "Customize Tabs",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = { searchActive = !searchActive }) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search Music",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (currentPlayingTrack != null) {
                IconButton(onClick = { showNowPlayingSheet = true }) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.QueueMusic,
                        contentDescription = "Now Playing",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        // Expandable Search Bar
        AnimatedVisibility(visible = searchActive) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search songs, artists, albums...") },
                singleLine = true,
                shape = CircleShape,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        // Swipe-synced Expressive Category Tabs
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            visibleTabs.forEachIndexed { index, tabId ->
                val label = when (tabId) {
                    "SONGS" -> "Songs (${filteredSongs.size})"
                    "QUEUE" -> "Queue (${activeQueue.size})"
                    else -> musicTabDisplayLabel(tabId)
                }
                val isSelected = pagerState.currentPage == index
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        selectedGroupTitle = null
                        selectedGroupTracks = emptyList()
                        coroutineScope.launch { pagerState.animateScrollToPage(index) }
                    },
                    label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                    ),
                    shape = CircleShape
                )
            }
        }

        HorizontalDivider(
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f),
            modifier = Modifier.padding(top = 4.dp)
        )

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        } else if (selectedGroupTitle != null) {
            Column(modifier = Modifier.fillMaxSize()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = {
                        selectedGroupTitle = null
                        selectedGroupTracks = emptyList()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = selectedGroupTitle.orEmpty(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${selectedGroupTracks.size} Tracks",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (selectedGroupTracks.isNotEmpty()) {
                        TextButton(onClick = {
                            AudioPlayerManager.playQueue(selectedGroupTracks, 0)
                            showNowPlayingSheet = true
                        }) {
                            Text("Play All", fontWeight = FontWeight.Bold)
                        }
                    }
                }
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    itemsIndexed(
                        items = selectedGroupTracks,
                        key = { _, item -> item.id }
                    ) { index, track ->
                        val isCurrent = currentPlayingTrack?.id == track.id
                        RealAudioTrackCard(
                            track = track,
                            isPlaying = isCurrent && isPlaying,
                            isCurrent = isCurrent,
                            artSizeDp = musicArtSizeDp,
                            onClick = {
                                AudioPlayerManager.playQueue(selectedGroupTracks, index)
                                showNowPlayingSheet = true
                            }
                        )
                    }
                }
            }
        } else {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                when (visibleTabs.getOrNull(pageIndex)) {
                    "SONGS" -> {
                        if (filteredSongs.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "No audio files found on device storage",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else if (musicGridView) {
                            AudioTrackGrid(
                                tracks = filteredSongs,
                                currentTrackId = currentPlayingTrack?.id,
                                isPlaying = isPlaying,
                                onTrackClick = { idx ->
                                    AudioPlayerManager.playQueue(filteredSongs, idx)
                                    showNowPlayingSheet = true
                                }
                            )
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items = filteredSongs,
                                    key = { _, item -> item.id }
                                ) { index, track ->
                                    val isCurrent = currentPlayingTrack?.id == track.id
                                    RealAudioTrackCard(
                                        track = track,
                                        isPlaying = isCurrent && isPlaying,
                                        isCurrent = isCurrent,
                                        artSizeDp = musicArtSizeDp,
                                        onClick = {
                                            AudioPlayerManager.playQueue(filteredSongs, index)
                                            showNowPlayingSheet = true
                                        }
                                    )
                                }
                            }
                        }
                    }
                    "ALBUMS" -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = scannedAlbums,
                                key = { _, item -> item.id }
                            ) { _, album ->
                                Surface(
                                    onClick = {
                                        val albumTracks = scannedSongs.filter { it.album == album.title }
                                        selectedGroupTitle = album.title
                                        selectedGroupTracks = sortMusicTracks(albumTracks, musicSortBy, musicSortAsc)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Album, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                                            if (album.artworkUri != null) {
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context).data(album.artworkUri).crossfade(true).build(),
                                                    contentDescription = null,
                                                    contentScale = ContentScale.Crop,
                                                    modifier = Modifier.fillMaxSize()
                                                )
                                            }
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(album.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${album.artist} \u2022 ${album.trackCount} Tracks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "ARTISTS" -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = scannedArtists,
                                key = { _, item -> item.name }
                            ) { _, artist ->
                                Surface(
                                    onClick = {
                                        val artistTracks = scannedSongs.filter { it.artist == artist.name }
                                        selectedGroupTitle = artist.name
                                        selectedGroupTracks = sortMusicTracks(artistTracks, musicSortBy, musicSortAsc)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(CircleShape)
                                                .background(MaterialTheme.colorScheme.primaryContainer),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Person, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(24.dp))
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(artist.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${artist.albumCount} Albums \u2022 ${artist.trackCount} Songs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "PLAYLISTS" -> {
                        MusicPlaylistsTabContent(
                            scannedSongs = scannedSongs,
                            onPlayQueue = { tracks, idx, shuffle ->
                                if (shuffle && !AudioPlayerManager.isShuffle.value) {
                                    AudioPlayerManager.toggleShuffle()
                                }
                                AudioPlayerManager.playQueue(tracks, idx)
                                showNowPlayingSheet = true
                            }
                        )
                    }
                    "FOLDERS" -> {
                        LazyColumn(
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(
                                items = scannedFolders,
                                key = { _, item -> item.path }
                            ) { _, folder ->
                                Surface(
                                    onClick = {
                                        val folderTracks = scannedSongs.filter { it.folderName == folder.name }
                                        selectedGroupTitle = folder.name
                                        selectedGroupTracks = sortMusicTracks(folderTracks, musicSortBy, musicSortAsc)
                                    },
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(14.dp),
                                    color = MaterialTheme.colorScheme.surfaceContainerLow
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Rounded.Folder, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(folder.name, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                            Text("${folder.trackCount} Audio Files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    "QUEUE" -> {
                        if (activeQueue.isEmpty()) {
                            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                Text(
                                    text = "Queue is empty. Tap any song to start playing.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        } else {
                            LazyColumn(
                                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                itemsIndexed(
                                    items = activeQueue,
                                    key = { index, item -> "${item.id}_$index" }
                                ) { index, track ->
                                    val isCurrent = currentPlayingTrack?.id == track.id
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Box(modifier = Modifier.weight(1f)) {
                                            RealAudioTrackCard(
                                                track = track,
                                                isPlaying = isCurrent && isPlaying,
                                                isCurrent = isCurrent,
                                                artSizeDp = musicArtSizeDp,
                                                onClick = {
                                                    AudioPlayerManager.playQueue(activeQueue, index)
                                                    showNowPlayingSheet = true
                                                }
                                            )
                                        }
                                        IconButton(onClick = { AudioPlayerManager.removeFromQueue(index) }) {
                                            Icon(
                                                imageVector = Icons.Rounded.Close,
                                                contentDescription = "Remove from queue",
                                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                                modifier = Modifier.size(18.dp)
                                            )
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

    if (showSortDialog) {
        MusicSortAndViewDialog(onDismiss = { showSortDialog = false })
    }
    if (showTabCustomizerDialog) {
        MusicTabCustomizerDialog(onDismiss = { showTabCustomizerDialog = false })
    }

    // Full-screen Now Playing Bottom Sheet
    if (showNowPlayingSheet && currentPlayingTrack != null) {
        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
        ModalBottomSheet(
            onDismissRequest = { showNowPlayingSheet = false },
            sheetState = sheetState,
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            FullNowPlayingContent(
                track = currentPlayingTrack!!,
                isPlaying = isPlaying,
                currentPosMs = currentPositionMs,
                durationMs = durationMs,
                isShuffle = isShuffle,
                isRepeat = isRepeat,
                sleepRemainingMs = sleepRemainingMs,
                onPlayPauseClick = { AudioPlayerManager.togglePlayPause() },
                onSeekMs = { AudioPlayerManager.seekTo(it) },
                onNextClick = { AudioPlayerManager.playNext() },
                onPrevClick = { AudioPlayerManager.playPrevious() },
                onShuffleClick = { AudioPlayerManager.toggleShuffle() },
                onRepeatClick = { AudioPlayerManager.toggleRepeat() },
                onOpenEqualizer = { showMusicEqSheet = true },
                onOpenSleepTimer = { showMusicSleepSheet = true },
                onMinimizeClick = { showNowPlayingSheet = false }
            )
        }
    }

    if (showMusicEqSheet) {
        EqualizerBottomSheet(
            sheetColor = Color(0xFF161622),
            accent = MaterialTheme.colorScheme.primary,
            onSelectPreset = { preset ->
                AppSettings.setEqualizerPreset(preset)
                AudioPlayerManager.applyAudioEffectsNow()
            },
            onCustomBandsChange = { bands ->
                AppSettings.setEqCustomBandsDb(EqualizerPresetsCatalog.serializeBands(bands))
                AppSettings.setEqualizerPreset("Custom")
                AudioPlayerManager.applyAudioEffectsNow()
            },
            onAudioChannelModeChange = { mode -> AppSettings.setAudioChannelMode(mode) },
            onDismiss = { showMusicEqSheet = false }
        )
    }

    if (showMusicSleepSheet) {
        val remTrackMins = ((durationMs - currentPositionMs) / 60_000L).toInt().coerceAtLeast(1)
        SleepTimerBottomSheet(
            remainingMs = sleepRemainingMs,
            untilEndOfVideo = false,
            sheetColor = Color(0xFF161622),
            accent = MaterialTheme.colorScheme.primary,
            onAccent = MaterialTheme.colorScheme.onPrimary,
            onSetMinutes = { AudioPlayerManager.setSleepTimerMinutes(it) },
            onSetEndOfVideo = { AudioPlayerManager.setSleepTimerMinutes(remTrackMins) },
            onCancel = { AudioPlayerManager.cancelSleepTimer() },
            onDismiss = { showMusicSleepSheet = false }
        )
    }
}

@Composable
fun RealAudioTrackCard(
    track: RealAudioTrack,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    artSizeDp: Int = 56
) {
    val context = LocalContext.current

    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isCurrent) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        } else {
            Color.Transparent
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Square Album Artwork
            Box(
                modifier = Modifier
                    .size(artSizeDp.coerceIn(48, 126).dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(if (isCurrent) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (track.isRadio) Icons.Rounded.Radio else Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                if (track.artworkUri != null) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(track.artworkUri)
                            .crossfade(true)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = track.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${track.artist} \u2022 ${track.durationText}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            IconButton(onClick = onClick) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FullNowPlayingContent(
    track: RealAudioTrack,
    isPlaying: Boolean,
    currentPosMs: Long,
    durationMs: Long,
    isShuffle: Boolean,
    isRepeat: Boolean,
    sleepRemainingMs: Long = 0L,
    onPlayPauseClick: () -> Unit,
    onSeekMs: (Long) -> Unit,
    onNextClick: () -> Unit,
    onPrevClick: () -> Unit,
    onShuffleClick: () -> Unit,
    onRepeatClick: () -> Unit,
    onOpenEqualizer: () -> Unit = {},
    onOpenSleepTimer: () -> Unit = {},
    onMinimizeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var isDraggingSlider by remember { mutableStateOf(false) }
    var dragFraction by remember { mutableFloatStateOf(0f) }

    val effectiveDurationMs = if (durationMs > 0L) durationMs else track.durationMs
    val effectiveFraction = if (isDraggingSlider) dragFraction else {
        if (effectiveDurationMs > 0) (currentPosMs.toFloat() / effectiveDurationMs).coerceIn(0f, 1f) else 0f
    }
    val infiniteTransition = rememberInfiniteTransition(label = "music_wave")
    val wavePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = (2.0 * Math.PI).toFloat(),
        animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
        label = "music_wave_phase"
    )

    val albumPaletteEnabled by AppSettings.albumPaletteBackground.collectAsState()
    val visualizerStyle by AppSettings.musicVisualizerStyle.collectAsState()
    val musicOrient by AppSettings.musicPlayerOrientation.collectAsState()
    var showLyricsDialog by remember { mutableStateOf(false) }
    val lyricsText = remember(track.path) { loadLocalSidecarLyrics(track.path) }

    DisposableEffect(musicOrient) {
        val activity = context as? Activity
        if (activity != null && musicOrient != "AUTO") {
            activity.requestedOrientation = if (musicOrient == "LANDSCAPE") {
                ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            } else {
                ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            }
        }
        onDispose {
            if (activity != null && musicOrient != "AUTO") {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
    }

    val bgBrush = if (albumPaletteEnabled) {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(
                MaterialTheme.colorScheme.primary.copy(alpha = 0.22f),
                MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.14f),
                Color.Transparent
            )
        )
    } else {
        androidx.compose.ui.graphics.Brush.verticalGradient(
            colors = listOf(Color.Transparent, Color.Transparent)
        )
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgBrush)
            .padding(horizontal = 24.dp, vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Drag Handle / Dismiss bar
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f))
                .clickable { onMinimizeClick() }
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Large Square Album Artwork (Never cropped / unwarped)
        Box(
            modifier = Modifier
                .size(240.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .border(1.5.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (track.isRadio) Icons.Rounded.Radio else Icons.Rounded.Album,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(96.dp)
            )
            if (track.artworkUri != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(track.artworkUri)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Album Art",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Track Title & Artist
        Text(
            text = track.title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "${track.artist} \u2022 ${track.album}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(20.dp))

        // Seekbar & Timestamp row
        if (!track.isRadio) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .pointerInput(effectiveDurationMs) {
                        detectTapGestures { offset ->
                            if (size.width > 0 && effectiveDurationMs > 0L) {
                                val frac = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                onSeekMs((frac * effectiveDurationMs).toLong())
                            }
                        }
                    }
                    .pointerInput(effectiveDurationMs) {
                        detectHorizontalDragGestures(
                            onDragStart = { offset ->
                                isDraggingSlider = true
                                if (size.width > 0) {
                                    dragFraction = (offset.x / size.width.toFloat()).coerceIn(0f, 1f)
                                }
                            },
                            onHorizontalDrag = { change, _ ->
                                change.consume()
                                if (size.width > 0) {
                                    dragFraction = (change.position.x / size.width.toFloat()).coerceIn(0f, 1f)
                                }
                            },
                            onDragEnd = {
                                isDraggingSlider = false
                                if (effectiveDurationMs > 0L) {
                                    onSeekMs((dragFraction * effectiveDurationMs).toLong())
                                }
                            },
                            onDragCancel = { isDraggingSlider = false }
                        )
                    },
                contentAlignment = Alignment.Center
            ) {
                MpvxSeekbarCanvas(
                    seekbarStyle = "WAVY",
                    progressFraction = effectiveFraction,
                    bufferedFraction = 1f,
                    durationMs = effectiveDurationMs,
                    accentColor = MaterialTheme.colorScheme.primary,
                    wavePhase = if (isPlaying) wavePhase else 0f,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val currentSec = (if (isDraggingSlider) (dragFraction * effectiveDurationMs).toLong() else currentPosMs) / 1000
                val totalSec = effectiveDurationMs / 1000
                Text(
                    text = String.format(Locale.US, "%02d:%02d", currentSec / 60, currentSec % 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = String.format(Locale.US, "%02d:%02d", totalSec / 60, totalSec % 60),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            // Live stream indicator
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF10B981).copy(alpha = 0.18f),
                modifier = Modifier.padding(vertical = 8.dp)
            ) {
                Text(
                    text = "\u26A1 LIVE AUDIO STREAM",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Transport Controls Pod
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onShuffleClick) {
                Icon(
                    imageVector = Icons.Rounded.Shuffle,
                    contentDescription = "Shuffle",
                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onPrevClick) {
                Icon(
                    imageVector = Icons.Rounded.SkipPrevious,
                    contentDescription = "Previous Track",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            // Big Play/Pause Button
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clickable { onPlayPauseClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(36.dp)
                )
            }

            IconButton(onClick = onNextClick) {
                Icon(
                    imageVector = Icons.Rounded.SkipNext,
                    contentDescription = "Next Track",
                    modifier = Modifier.size(36.dp),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }

            IconButton(onClick = onRepeatClick) {
                Icon(
                    imageVector = Icons.Rounded.Repeat,
                    contentDescription = "Repeat",
                    tint = if (isRepeat) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        if (visualizerStyle != "OFF") {
            val primaryColor = MaterialTheme.colorScheme.primary
            val secondaryColor = MaterialTheme.colorScheme.secondary
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(34.dp)
            ) {
                val w = size.width
                val h = size.height
                val cy = h / 2f
                when (visualizerStyle) {
                    "BARS" -> {
                        val bars = 24
                        val step = w / bars
                        for (i in 0 until bars) {
                            val amp = if (isPlaying) {
                                0.25f + 0.72f * kotlin.math.abs(kotlin.math.sin(wavePhase + i * 0.45f))
                            } else 0.16f
                            val barH = h * amp
                            drawRoundRect(
                                color = if (i % 2 == 0) primaryColor else secondaryColor,
                                topLeft = androidx.compose.ui.geometry.Offset(i * step + step * 0.2f, cy - barH / 2f),
                                size = androidx.compose.ui.geometry.Size(step * 0.55f, barH),
                                cornerRadius = androidx.compose.ui.geometry.CornerRadius(4f, 4f)
                            )
                        }
                    }
                    "RING" -> {
                        for (r in 1..4) {
                            val rad = (10f + r * 5f) + if (isPlaying) 3f * kotlin.math.sin(wavePhase + r) else 0f
                            drawCircle(
                                color = primaryColor.copy(alpha = (0.7f - r * 0.12f).coerceIn(0.15f, 0.8f)),
                                radius = rad,
                                center = androidx.compose.ui.geometry.Offset(w / 2f, cy),
                                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.5f)
                            )
                        }
                    }
                    else -> {
                        val path = androidx.compose.ui.graphics.Path()
                        val pts = 48
                        for (i in 0..pts) {
                            val x = (i / pts.toFloat()) * w
                            val env = kotlin.math.sin((i / pts.toFloat()) * Math.PI).toFloat()
                            val y = cy + (if (isPlaying) kotlin.math.sin(wavePhase * 1.5f + i * 0.35f) * (h * 0.38f) * env else 0f)
                            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
                        }
                        drawPath(path, color = primaryColor, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 3.2f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        MusicNowPlayingQuickToolsRow(
            sleepRemainingMs = sleepRemainingMs,
            onOpenEqualizer = onOpenEqualizer,
            onOpenSleepTimer = onOpenSleepTimer
        )

        Spacer(modifier = Modifier.height(8.dp))
        OutlinedButton(onClick = { showLyricsDialog = true }, shape = CircleShape) {
            Icon(Icons.Rounded.Lyrics, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(if (lyricsText != null) "Lyrics (Available)" else "Lyrics", style = MaterialTheme.typography.labelMedium)
        }

        Spacer(modifier = Modifier.height(16.dp))
    }

    if (showLyricsDialog) {
        AlertDialog(
            onDismissRequest = { showLyricsDialog = false },
            title = { Text("${track.title} — Lyrics", fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis) },
            text = {
                Box(modifier = Modifier.heightIn(max = 320.dp).verticalScroll(rememberScrollState())) {
                    Text(
                        text = lyricsText ?: "No sidecar .lrc or .txt lyrics file found next to this audio track.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showLyricsDialog = false }) { Text("Close") }
            }
        )
    }
}

private fun loadLocalSidecarLyrics(path: String): String? {
    if (path.isBlank() || path.startsWith("http", ignoreCase = true)) return null
    return runCatching {
        val f = java.io.File(path)
        val base = f.nameWithoutExtension
        val parent = f.parentFile ?: return@runCatching null
        val lrc = java.io.File(parent, "$base.lrc").takeIf { it.isFile }
            ?: java.io.File(parent, "$base.txt").takeIf { it.isFile }
            ?: return@runCatching null
        lrc.readText().lines().joinToString("\n") { line ->
            line.replace(Regex("""\[\d{1,2}:\d{2}(?:\.\d{1,3})?\]"""), "").trim()
        }.trim().ifBlank { null }
    }.getOrNull()
}
