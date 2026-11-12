package com.cmstudio.player.ui

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.BookmarkAdd
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.LibraryMusic
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarOutline
import androidx.compose.material.icons.rounded.UploadFile
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.ViewAgenda
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.SheetValue
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cmstudio.player.PlaybackHistoryEntry
import com.cmstudio.player.ui.library.LibraryScreen
import com.cmstudio.player.ui.model.MediaFolderItem
import com.cmstudio.player.ui.model.MediaVideoItem
import com.cmstudio.player.ui.model.NetworkRemoteEntry
import com.cmstudio.player.ui.model.NetworkShareConfig
import com.cmstudio.player.ui.model.UserPlaylist
import com.cmstudio.player.ui.model.WatchStreakGoal
import com.cmstudio.player.ui.music.AudioPlayerManager
import com.cmstudio.player.ui.music.FourArtMosaicBox
import com.cmstudio.player.ui.music.FullNowPlayingContent
import com.cmstudio.player.ui.music.MusicScreen
import com.cmstudio.player.ui.network.NetworkBrowseResult
import com.cmstudio.player.ui.network.NetworkBrowserClient
import com.cmstudio.player.ui.network.NetworkConnectionTestResult
import com.cmstudio.player.ui.servers.MediaServerKind
import com.cmstudio.player.ui.servers.MediaServerTabContent
import com.cmstudio.player.ui.player.EqualizerBottomSheet
import com.cmstudio.player.ui.player.EqualizerPresetsCatalog
import com.cmstudio.player.ui.player.SleepTimerBottomSheet
import com.cmstudio.player.ui.theme.ElectricIndigo
import com.cmstudio.player.ui.theme.GlassSurface
import com.cmstudio.player.ui.theme.extendedColors
import com.cmstudio.player.ui.vault.VaultScreen
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class MainNavDestination(
    val title: String,
    val icon: ImageVector,
    val activeWidth: Dp
) {
    HOME("Home", Icons.Rounded.Home, 96.dp),
    MUSIC("Music", Icons.Rounded.MusicNote, 96.dp),
    RECENTS("Recents", Icons.Rounded.History, 108.dp),
    PLAYLISTS("Playlists", Icons.AutoMirrored.Rounded.QueueMusic, 112.dp),
    STREAMS("Streams", Icons.Rounded.Podcasts, 108.dp),
    NETWORK("Network", Icons.Rounded.Lan, 108.dp),
    JELLYFIN("Jellyfin", Icons.Rounded.Movie, 108.dp),
    NAVIDROME("Navidrome", Icons.Rounded.LibraryMusic, 116.dp),
    AUDIOBOOKS("Audiobooks", Icons.Rounded.Book, 120.dp),
    VAULT("Vault", Icons.Rounded.Lock, 96.dp)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    continueWatchingVideos: List<MediaVideoItem>,
    recentlyAddedVideos: List<MediaVideoItem>,
    allVideos: List<MediaVideoItem> = emptyList(),
    allScannedVideos: List<MediaVideoItem> = allVideos,
    folders: List<MediaFolderItem>,
    historyEntries: List<PlaybackHistoryEntry> = emptyList(),
    watchStreakGoal: WatchStreakGoal,
    requestedDestinationId: String? = null,
    onConsumeRequestedDestination: () -> Unit = {},
    onVideoClick: (MediaVideoItem) -> Unit,
    onVideoQueuePlay: (MediaVideoItem, List<MediaVideoItem>) -> Unit = { v, _ -> onVideoClick(v) },
    onPlayNetworkStreamClick: (String) -> Unit = {},
    onPlayUriWithTitle: (Uri, String?, Long) -> Unit = { uri, _, _ -> onPlayNetworkStreamClick(uri.toString()) },
    onRefreshLibrary: () -> Unit = {},
    onRemoveHistoryEntry: (Uri) -> Unit = {},
    onClearHistory: () -> Unit = {},
    onDeleteHistoryFiles: (List<PlaybackHistoryEntry>) -> Unit = {},
    onDeleteVideoClick: (MediaVideoItem) -> Unit = {},
    onDeleteVideosBatch: (List<MediaVideoItem>) -> Unit = { list -> list.firstOrNull()?.let(onDeleteVideoClick) },
    onToggleVideoWatched: (MediaVideoItem) -> Unit = {},
    onToggleFolderWatched: (String) -> Unit = {},
    onOpenSystemPickerFile: (Uri) -> Unit = { uri -> onPlayUriWithTitle(uri, uri.lastPathSegment, -1L) },
    modifier: Modifier = Modifier
) {
    val enabledTabIds by AppSettings.enabledNavTabs.collectAsState()
    val orderedTabIds by AppSettings.navTabOrder.collectAsState()
    val vaultEntryHidden by AppSettings.vaultEntryHidden.collectAsState()
    val mediaServers by AppSettings.mediaServers.collectAsState()
    val enabledServerTabs by AppSettings.enabledServerTabs.collectAsState()

    val destinations = remember(enabledTabIds, orderedTabIds, vaultEntryHidden, mediaServers, enabledServerTabs) {
        val enabledSet = enabledTabIds.toSet()
        val core = orderedTabIds.filter { it in enabledSet && !(vaultEntryHidden && it == "VAULT") }
            .mapNotNull { id -> MainNavDestination.entries.firstOrNull { it.name == id } }
        val serverDests = buildList {
            if (mediaServers.any { it.kind == MediaServerKind.JELLYFIN } || "JELLYFIN" in enabledServerTabs) {
                add(MainNavDestination.JELLYFIN)
            }
            if (mediaServers.any { it.kind == MediaServerKind.NAVIDROME } || "NAVIDROME" in enabledServerTabs) {
                add(MainNavDestination.NAVIDROME)
            }
            if (mediaServers.any { it.kind == MediaServerKind.AUDIOBOOKSHELF } || "AUDIOBOOKS" in enabledServerTabs) {
                add(MainNavDestination.AUDIOBOOKS)
            }
        }
        (core + serverDests).ifEmpty { listOf(MainNavDestination.HOME) }
    }

    var selectedDestination by rememberSaveable { mutableStateOf(MainNavDestination.HOME) }
    var isExpandedMusicPlayerOpen by rememberSaveable { mutableStateOf(false) }
    var isHomeSelectionMode by remember { mutableStateOf(false) }
    var showStealthVaultOverlay by rememberSaveable { mutableStateOf(false) }

    val pagerState = rememberPagerState(
        initialPage = destinations.indexOf(selectedDestination).coerceAtLeast(0),
        pageCount = { destinations.size }
    )
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(destinations) {
        if (selectedDestination !in destinations) {
            selectedDestination = destinations.first()
        }
        val idx = destinations.indexOf(selectedDestination).coerceAtLeast(0)
        if (pagerState.currentPage != idx) {
            pagerState.scrollToPage(idx)
        }
    }

    LaunchedEffect(pagerState.currentPage, destinations) {
        destinations.getOrNull(pagerState.currentPage)?.let {
            selectedDestination = it
        }
        if (destinations.getOrNull(pagerState.currentPage) != MainNavDestination.HOME) {
            isHomeSelectionMode = false
        }
    }

    LaunchedEffect(requestedDestinationId, destinations) {
        if (!requestedDestinationId.isNullOrBlank()) {
            if (requestedDestinationId == "VAULT" && destinations.none { it == MainNavDestination.VAULT }) {
                showStealthVaultOverlay = true
            } else {
                val match = destinations.firstOrNull { it.name == requestedDestinationId }
                if (match != null) {
                    selectedDestination = match
                    val idx = destinations.indexOf(match).coerceAtLeast(0)
                    pagerState.animateScrollToPage(idx)
                }
            }
            onConsumeRequestedDestination()
        }
    }

    // Mini-player reactive state
    val isMiniPlayerActive by AppSettings.isMiniPlayerActive.collectAsState()
    val miniPlayerTitle by AppSettings.miniPlayerTitle.collectAsState()
    val miniPlayerSubtitle by AppSettings.miniPlayerSubtitle.collectAsState()
    val isMiniPlayerPlaying by AppSettings.isMiniPlayerPlaying.collectAsState()
    val miniPlayerProgress by AppSettings.miniPlayerProgress.collectAsState()
    val currentAudioTrack by AudioPlayerManager.currentTrack.collectAsState()
    var showMiniEqSheet by remember { mutableStateOf(false) }
    var showMiniSleepSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = Color.Transparent,
        contentColor = MaterialTheme.colorScheme.onBackground,
        bottomBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AnimatedVisibility(
                    visible = isMiniPlayerActive && !isHomeSelectionMode && !showStealthVaultOverlay,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(280)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(220))
                ) {
                    SwipeableMiniPlayerDock(
                        title = miniPlayerTitle,
                        subtitle = miniPlayerSubtitle,
                        isPlaying = isMiniPlayerPlaying,
                        progress = miniPlayerProgress,
                        artworkUri = currentAudioTrack?.artworkUri,
                        onPlayPauseClick = { AudioPlayerManager.togglePlayPause() },
                        onNextClick = { AudioPlayerManager.playNext() },
                        onCloseClick = { AudioPlayerManager.release() },
                        onExpandClick = { isExpandedMusicPlayerOpen = true }
                    )
                }

                AnimatedVisibility(
                    visible = destinations.size > 1 && !isHomeSelectionMode && !showStealthVaultOverlay,
                    enter = slideInVertically(initialOffsetY = { it }, animationSpec = tween(240)),
                    exit = slideOutVertically(targetOffsetY = { it }, animationSpec = tween(180))
                ) {
                    val pageProgress = (pagerState.currentPage + pagerState.currentPageOffsetFraction)
                        .coerceIn(0f, (destinations.size - 1).coerceAtLeast(0).toFloat())
                    ExpressivePillNavigationBar(
                        destinations = destinations,
                        selectedDestination = selectedDestination,
                        pageProgress = pageProgress,
                        onDestinationSelected = { dest ->
                            selectedDestination = dest
                            val targetIdx = destinations.indexOf(dest)
                            if (targetIdx >= 0) {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(targetIdx)
                                }
                            }
                        },
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color.Transparent)
        ) {
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isHomeSelectionMode && !showStealthVaultOverlay,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                val destination = destinations.getOrNull(pageIndex) ?: MainNavDestination.HOME
                when (destination) {
                    MainNavDestination.HOME -> {
                        LibraryScreen(
                            continueWatchingVideos = continueWatchingVideos,
                            recentlyAddedVideos = recentlyAddedVideos,
                            allVideos = allVideos,
                            folders = folders,
                            watchStreakGoal = watchStreakGoal,
                            onVideoClick = onVideoClick,
                            onVideoQueuePlay = onVideoQueuePlay,
                            onPlayNetworkStreamClick = onPlayNetworkStreamClick,
                            onRefreshClick = onRefreshLibrary,
                            onNavigateTab = { tabName ->
                                if (tabName == "VAULT" && destinations.none { it == MainNavDestination.VAULT }) {
                                    showStealthVaultOverlay = true
                                } else {
                                    val target = destinations.firstOrNull { it.name == tabName }
                                    if (target != null) {
                                        selectedDestination = target
                                        val idx = destinations.indexOf(target)
                                        if (idx >= 0) {
                                            coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                                        }
                                    }
                                }
                            },
                            onDeleteVideoClick = onDeleteVideoClick,
                            onDeleteVideosBatch = onDeleteVideosBatch,
                            onToggleVideoWatched = onToggleVideoWatched,
                            onToggleFolderWatched = onToggleFolderWatched,
                            onOpenVaultClick = {
                                val vaultDest = destinations.firstOrNull { it == MainNavDestination.VAULT }
                                if (vaultDest != null) {
                                    selectedDestination = vaultDest
                                    val idx = destinations.indexOf(vaultDest)
                                    if (idx >= 0) coroutineScope.launch { pagerState.animateScrollToPage(idx) }
                                } else {
                                    showStealthVaultOverlay = true
                                }
                            },
                            onSelectionModeChange = { active -> isHomeSelectionMode = active },
                            onOpenSystemPickerFile = onOpenSystemPickerFile
                        )
                    }

                    MainNavDestination.MUSIC -> {
                        MusicScreen(
                            onTrackPlay = { title, artist ->
                                AppSettings.updateMiniPlayer(title, artist, true)
                            }
                        )
                    }

                    MainNavDestination.RECENTS -> {
                        RecentsHistoryModule(
                            historyEntries = historyEntries,
                            allVideos = allVideos,
                            onResumeEntry = { entry ->
                                val localMatch = allVideos.firstOrNull { it.uri == entry.uri }
                                if (localMatch != null) {
                                    onVideoClick(localMatch)
                                } else {
                                    onPlayUriWithTitle(entry.uri, entry.title, entry.positionMs)
                                }
                            },
                            onRemoveEntry = { entry -> onRemoveHistoryEntry(entry.uri) },
                            onClearAll = onClearHistory,
                            onDeleteLocalFiles = onDeleteHistoryFiles
                        )
                    }

                    MainNavDestination.PLAYLISTS -> {
                        PlaylistsModule(
                            allVideos = allVideos,
                            onPlayQueue = { firstVideo, playlistQueue ->
                                onVideoQueuePlay(firstVideo, playlistQueue)
                            },
                            onPlayStreamUrl = onPlayNetworkStreamClick
                        )
                    }

                    MainNavDestination.STREAMS -> {
                        WebStreamsModule(
                            onPlayStream = onPlayNetworkStreamClick
                        )
                    }

                    MainNavDestination.NETWORK -> {
                        NetworkSharesModule(
                            onPlayStream = onPlayNetworkStreamClick
                        )
                    }

                    MainNavDestination.JELLYFIN -> {
                        MediaServerTabContent(
                            kind = MediaServerKind.JELLYFIN,
                            onPlayVideoUrl = onPlayUriWithTitle
                        )
                    }

                    MainNavDestination.NAVIDROME -> {
                        MediaServerTabContent(
                            kind = MediaServerKind.NAVIDROME,
                            onPlayVideoUrl = onPlayUriWithTitle
                        )
                    }

                    MainNavDestination.AUDIOBOOKS -> {
                        MediaServerTabContent(
                            kind = MediaServerKind.AUDIOBOOKSHELF,
                            onPlayVideoUrl = onPlayUriWithTitle
                        )
                    }

                    MainNavDestination.VAULT -> {
                        VaultScreen(
                            allVideos = allScannedVideos,
                            onVideoClick = onVideoClick,
                            onPlayVideo = onVideoClick,
                            onVaultMembershipChanged = onRefreshLibrary
                        )
                    }
                }
            }

            if (showStealthVaultOverlay) {
                BackHandler { showStealthVaultOverlay = false }
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background)
                ) {
                    VaultScreen(
                        allVideos = allScannedVideos,
                        onVideoClick = onVideoClick,
                        onPlayVideo = onVideoClick,
                        onCloseClick = { showStealthVaultOverlay = false },
                        onVaultMembershipChanged = onRefreshLibrary
                    )
                    IconButton(
                        onClick = { showStealthVaultOverlay = false },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Close Vault",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }

    // Expanded Music Player Sheet
    if (isExpandedMusicPlayerOpen) {
        val currentTrack by AudioPlayerManager.currentTrack.collectAsState()
        val sleepRemainingMs by AudioPlayerManager.sleepTimerRemainingMs.collectAsState()
        val sheetState = rememberBottomSheetState(
            initialValue = SheetValue.Hidden,
            enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
        )
        ModalBottomSheet(
            onDismissRequest = { isExpandedMusicPlayerOpen = false },
            sheetState = sheetState,
            dragHandle = null,
            containerColor = MaterialTheme.colorScheme.surface
        ) {
            if (currentTrack != null) {
                val isMusicPlaying by AudioPlayerManager.isPlaying.collectAsState()
                val currentPosMs by AudioPlayerManager.currentPositionMs.collectAsState()
                val durationMs by AudioPlayerManager.durationMs.collectAsState()
                val isShuffle by AudioPlayerManager.isShuffle.collectAsState()
                val isRepeat by AudioPlayerManager.isRepeat.collectAsState()

                FullNowPlayingContent(
                    track = currentTrack!!,
                    isPlaying = isMusicPlaying,
                    currentPosMs = currentPosMs,
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
                    onOpenEqualizer = { showMiniEqSheet = true },
                    onOpenSleepTimer = { showMiniSleepSheet = true },
                    onMinimizeClick = { isExpandedMusicPlayerOpen = false }
                )
            } else {
                isExpandedMusicPlayerOpen = false
            }
        }
    }

    if (showMiniEqSheet) {
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
            onDismiss = { showMiniEqSheet = false }
        )
    }

    if (showMiniSleepSheet) {
        val sleepRemainingMs by AudioPlayerManager.sleepTimerRemainingMs.collectAsState()
        val curPos by AudioPlayerManager.currentPositionMs.collectAsState()
        val curDur by AudioPlayerManager.durationMs.collectAsState()
        SleepTimerBottomSheet(
            remainingMs = sleepRemainingMs,
            untilEndOfVideo = false,
            sheetColor = Color(0xFF161622),
            accent = MaterialTheme.colorScheme.primary,
            onAccent = MaterialTheme.colorScheme.onPrimary,
            onSetMinutes = { AudioPlayerManager.setSleepTimerMinutes(it) },
            onSetEndOfVideo = {
                AudioPlayerManager.setSleepTimerMinutes(((curDur - curPos) / 60_000L).toInt().coerceAtLeast(1))
            },
            onCancel = { AudioPlayerManager.cancelSleepTimer() },
            onDismiss = { showMiniSleepSheet = false }
        )
    }
}

@Composable
private fun ExpressivePillNavigationBar(
    destinations: List<MainNavDestination>,
    selectedDestination: MainNavDestination,
    pageProgress: Float = 0f,
    onDestinationSelected: (MainNavDestination) -> Unit,
    modifier: Modifier = Modifier
) {
    val haptic = LocalHapticFeedback.current
    val hapticEnabled by AppSettings.hapticFeedback.collectAsState()
    val isLiquidGlass by AppSettings.isLiquidGlassEnabled.collectAsState()
    val glassOpacity by AppSettings.glassFrostOpacity.collectAsState()
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        shape = CircleShape,
        color = if (isLiquidGlass) {
            colorScheme.surfaceContainerHigh.copy(alpha = glassOpacity.coerceIn(0.45f, 0.92f))
        } else {
            colorScheme.surfaceContainerHigh
        },
        border = if (isLiquidGlass) BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.55f)) else null,
        shadowElevation = if (isLiquidGlass) 8.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            destinations.forEachIndexed { index, dest ->
                val activeFraction = (1f - kotlin.math.abs(pageProgress - index.toFloat())).coerceIn(0f, 1f)
                val isSelected = selectedDestination == dest || activeFraction > 0.5f
                val targetWidth = androidx.compose.ui.unit.lerp(44.dp, dest.activeWidth, activeFraction)
                val pillAlpha = activeFraction

                val pillBgColor = colorScheme.primaryContainer.copy(alpha = pillAlpha)
                val contentTint = androidx.compose.ui.graphics.lerp(
                    colorScheme.onSurfaceVariant,
                    colorScheme.onPrimaryContainer,
                    activeFraction
                )

                Box(
                    modifier = Modifier
                        .width(targetWidth)
                        .height(44.dp)
                        .clip(CircleShape)
                        .background(pillBgColor)
                        .clickable {
                            if (hapticEnabled) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                            onDestinationSelected(dest)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = dest.icon,
                            contentDescription = dest.title,
                            tint = contentTint,
                            modifier = Modifier.size(22.dp)
                        )
                        if (activeFraction > 0.35f) {
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = dest.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = contentTint.copy(alpha = ((activeFraction - 0.35f) / 0.65f).coerceIn(0f, 1f)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RecentsHistoryModule(
    historyEntries: List<PlaybackHistoryEntry>,
    allVideos: List<MediaVideoItem>,
    onResumeEntry: (PlaybackHistoryEntry) -> Unit,
    onRemoveEntry: (PlaybackHistoryEntry) -> Unit,
    onClearAll: () -> Unit,
    onDeleteLocalFiles: (List<PlaybackHistoryEntry>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    val activeFilter by AppSettings.recentsFilter.collectAsState()
    val isGridView by AppSettings.recentsGridView.collectAsState()
    var showClearConfirm by remember { mutableStateOf(false) }
    var deleteLocalOnClear by remember { mutableStateOf(false) }
    var pendingRemoveEntry by remember { mutableStateOf<PlaybackHistoryEntry?>(null) }
    var deleteLocalOnSingle by remember { mutableStateOf(false) }
    val localMap = remember(allVideos) { allVideos.associateBy { it.uri } }

    val filteredEntries = remember(historyEntries, activeFilter) {
        when (activeFilter) {
            "VIDEO" -> historyEntries.filterNot { it.isAudio }
            "AUDIO" -> historyEntries.filter { it.isAudio }
            else -> historyEntries
        }
    }

    LazyVerticalGrid(
        columns = GridCells.Fixed(if (isGridView) 2 else 1),
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item(key = "recents_header", span = { GridItemSpan(maxLineSpan) }) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.extendedColors.badgeBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.History, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Playback History", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${filteredEntries.size} recently played items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { AppSettings.setRecentsGridView(!isGridView) }) {
                        Icon(
                            imageVector = if (isGridView) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                            contentDescription = "Toggle Layout",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (historyEntries.isNotEmpty()) {
                        TextButton(onClick = {
                            deleteLocalOnClear = false
                            showClearConfirm = true
                        }) {
                            Text("Clear All", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ALL" to "All", "VIDEO" to "Video", "AUDIO" to "Audio").forEach { (id, label) ->
                        FilterChip(
                            selected = activeFilter == id,
                            onClick = { AppSettings.setRecentsFilter(id) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }

        if (filteredEntries.isEmpty()) {
            item(key = "empty_recents", span = { GridItemSpan(maxLineSpan) }) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            Icons.Rounded.History,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text("No Recents Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            "Videos, audio tracks, and streams save their resume timestamps here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        } else {
            items(filteredEntries, key = { it.uri.toString() }) { entry ->
                val localVideo = localMap[entry.uri]
                val thumbUri = localVideo?.thumbnailUri ?: if (!entry.isNetwork && !entry.isAudio) entry.uri else null
                val dateStr = remember(entry.lastPlayedAtMs) {
                    if (entry.lastPlayedAtMs > 0L) {
                        SimpleDateFormat("MMM d, HH:mm", Locale.US).format(Date(entry.lastPlayedAtMs))
                    } else ""
                }
                val posLabel = when {
                    entry.played -> "Watched"
                    entry.positionMs > 0L -> "At ${formatDurationMs(entry.positionMs)}" +
                        if (entry.durationMs > 0L) " / ${formatDurationMs(entry.durationMs)}" else ""
                    entry.durationMs > 0L -> formatDurationMs(entry.durationMs)
                    entry.isNetwork -> "Network Stream"
                    else -> "Saved"
                }

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onResumeEntry(entry) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                ) {
                    if (isGridView) {
                        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(92.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF141820)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (thumbUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current).data(thumbUri).crossfade(true).build(),
                                        contentDescription = entry.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (entry.isAudio) Icons.Rounded.MusicNote else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(entry.title, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(posLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
                                }
                                IconButton(
                                    onClick = {
                                        deleteLocalOnSingle = false
                                        pendingRemoveEntry = entry
                                    },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(Icons.Rounded.Close, contentDescription = "Remove from Recents", modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 92.dp, height = 54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF141820)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (thumbUri != null) {
                                    AsyncImage(
                                        model = ImageRequest.Builder(LocalContext.current)
                                            .data(thumbUri)
                                            .crossfade(true)
                                            .build(),
                                        contentDescription = entry.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Icon(
                                        imageVector = if (entry.isAudio) Icons.Rounded.MusicNote else Icons.Rounded.PlayArrow,
                                        contentDescription = null,
                                        tint = Color.White
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    entry.title,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = listOfNotNull(posLabel, dateStr.takeIf { it.isNotBlank() }).joinToString(" • "),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (entry.progress > 0.01f && !entry.played) {
                                    Spacer(modifier = Modifier.height(6.dp))
                                    LinearProgressIndicator(
                                        progress = { entry.progress },
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(4.dp)
                                            .clip(RoundedCornerShape(2.dp)),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }

                            IconButton(onClick = {
                                deleteLocalOnSingle = false
                                pendingRemoveEntry = entry
                            }) {
                                Icon(
                                    imageVector = Icons.Rounded.Close,
                                    contentDescription = "Remove from Recents",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    pendingRemoveEntry?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingRemoveEntry = null },
            title = { Text("Remove History Entry?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(target.title, fontWeight = FontWeight.SemiBold, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    if (!target.isNetwork) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { deleteLocalOnSingle = !deleteLocalOnSingle }
                        ) {
                            Checkbox(checked = deleteLocalOnSingle, onCheckedChange = { deleteLocalOnSingle = it })
                            Text("Also delete original local file from device", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteLocalOnSingle && !target.isNetwork) {
                            onDeleteLocalFiles(listOf(target))
                        }
                        onRemoveEntry(target)
                        pendingRemoveEntry = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Remove") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRemoveEntry = null }) { Text("Cancel") }
            }
        )
    }

    if (showClearConfirm) {
        val localCount = historyEntries.count { !it.isNetwork }
        AlertDialog(
            onDismissRequest = { showClearConfirm = false },
            title = { Text("Clear Playback History?", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("This removes all saved history entries and resume timestamps.")
                    if (localCount > 0) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { deleteLocalOnClear = !deleteLocalOnClear }
                        ) {
                            Checkbox(checked = deleteLocalOnClear, onCheckedChange = { deleteLocalOnClear = it })
                            Text("Also delete $localCount original local media file(s)", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (deleteLocalOnClear) {
                            onDeleteLocalFiles(historyEntries.filter { !it.isNetwork })
                        }
                        showClearConfirm = false
                        onClearAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Clear History")
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearConfirm = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun PlaylistsModule(
    allVideos: List<MediaVideoItem>,
    onPlayQueue: (MediaVideoItem, List<MediaVideoItem>) -> Unit,
    onPlayStreamUrl: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val playlists by AppSettings.userPlaylists.collectAsState()
    val favoriteUris by AppSettings.favoriteVideoUris.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showImportM3uDialog by remember { mutableStateOf(false) }
    var selectedPlaylistId by remember { mutableStateOf<String?>(null) }
    var selectedVirtualId by remember { mutableStateOf<String?>(null) }
    var showAddItemsDialog by remember { mutableStateOf(false) }
    var renameTarget by remember { mutableStateOf<UserPlaylist?>(null) }

    val videoMap = remember(allVideos) { allVideos.associateBy { it.id } }
    val favoriteVideos = remember(allVideos, favoriteUris) {
        allVideos.filter { it.id in favoriteUris || it.uri.toString() in favoriteUris }
    }
    val selectedPlaylist = playlists.firstOrNull { it.id == selectedPlaylistId }

    val m3uFilePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { pickedUri ->
        if (pickedUri != null) {
            scope.launch {
                val lines = runCatching {
                    context.contentResolver.openInputStream(pickedUri)?.bufferedReader()?.use { it.readLines() } ?: emptyList()
                }.getOrDefault(emptyList())
                val entries = NetworkBrowserClient.parseM3uLines("", lines)
                if (entries.isEmpty()) {
                    Toast.makeText(context, "No stream entries found in selected M3U file", Toast.LENGTH_SHORT).show()
                } else {
                    val plName = pickedUri.lastPathSegment?.substringAfterLast('/')?.substringBeforeLast('.')?.ifBlank { "Imported M3U" } ?: "Imported M3U"
                    val target = selectedPlaylist ?: AppSettings.createPlaylist(plName)
                    entries.forEach { AppSettings.addVideoToPlaylist(target.id, it.url) }
                    selectedPlaylistId = target.id
                    Toast.makeText(context, "Imported ${entries.size} entries into ${target.name}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    if (selectedPlaylist != null || selectedVirtualId != null) {
        BackHandler {
            selectedPlaylistId = null
            selectedVirtualId = null
        }
        val isVirtual = selectedVirtualId != null
        val activeTitle = when {
            selectedPlaylist != null -> selectedPlaylist.name
            selectedVirtualId == "FAVORITES" -> "Favorites"
            else -> "All Videos"
        }
        val resolvedVideos = remember(selectedPlaylist, selectedVirtualId, videoMap, allVideos, favoriteVideos) {
            when {
                selectedPlaylist != null -> selectedPlaylist.itemUris.mapNotNull { uriStr ->
                    videoMap[uriStr] ?: run {
                        val uri = Uri.parse(uriStr)
                        val title = uri.lastPathSegment?.ifBlank { uriStr } ?: uriStr
                        MediaVideoItem(id = uriStr, title = title, uri = uri, sourceTag = "Playlist")
                    }
                }
                selectedVirtualId == "FAVORITES" -> favoriteVideos
                else -> allVideos
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = {
                    selectedPlaylistId = null
                    selectedVirtualId = null
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(activeTitle, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("${resolvedVideos.size} items", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (!isVirtual && selectedPlaylist != null) {
                    IconButton(onClick = { m3uFilePicker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = "Import Local M3U")
                    }
                    IconButton(onClick = { showImportM3uDialog = true }) {
                        Icon(Icons.Rounded.Link, contentDescription = "Import M3U URL")
                    }
                    IconButton(onClick = { renameTarget = selectedPlaylist }) {
                        Icon(Icons.Rounded.Edit, contentDescription = "Rename")
                    }
                    IconButton(onClick = { showAddItemsDialog = true }) {
                        Icon(Icons.Rounded.Add, contentDescription = "Add Videos", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            if (resolvedVideos.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onPlayQueue(resolvedVideos.first(), resolvedVideos) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play All (${resolvedVideos.size})")
                    }
                    OutlinedButton(
                        onClick = {
                            val shuffled = resolvedVideos.shuffled()
                            onPlayQueue(shuffled.first(), shuffled)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Rounded.Shuffle, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle")
                    }
                }
            }

            if (resolvedVideos.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            if (selectedVirtualId == "FAVORITES") "No Favorites marked yet" else "Playlist is empty",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        if (!isVirtual) {
                            OutlinedButton(onClick = { showAddItemsDialog = true }) {
                                Icon(Icons.Rounded.Add, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Add Local Videos")
                            }
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 100.dp)
                ) {
                    itemsIndexed(resolvedVideos, key = { idx, item -> "${idx}_${item.id}" }) { index, video ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (video.id.startsWith("http", ignoreCase = true) && video.id !in videoMap) {
                                        onPlayStreamUrl(video.id)
                                    } else {
                                        onPlayQueue(video, resolvedVideos)
                                    }
                                },
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(video.title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    if (video.durationText.isNotBlank()) {
                                        Text("${video.durationText} • ${video.sizeText}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                                if (!isVirtual && selectedPlaylist != null) {
                                    IconButton(
                                        onClick = { AppSettings.moveVideoInPlaylist(selectedPlaylist.id, index, -1) },
                                        enabled = index > 0,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move Up", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { AppSettings.moveVideoInPlaylist(selectedPlaylist.id, index, +1) },
                                        enabled = index < resolvedVideos.lastIndex,
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move Down", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(onClick = { AppSettings.removeVideoFromPlaylist(selectedPlaylist.id, video.id) }) {
                                        Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "playlist_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.extendedColors.badgeBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Playlists", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(
                            "${playlists.size} saved playlists",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { m3uFilePicker.launch(arrayOf("*/*")) }) {
                        Icon(Icons.Rounded.UploadFile, contentDescription = "Open Local M3U File")
                    }
                    IconButton(onClick = { showImportM3uDialog = true }) {
                        Icon(Icons.Rounded.Link, contentDescription = "Import M3U URL")
                    }
                    Button(
                        onClick = { showCreateDialog = true },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("New")
                    }
                }
            }

            item(key = "virtual_playlists") {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedVirtualId = "ALL_VIDEOS" },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.VideoLibrary, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("All Videos", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${allVideos.size} items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .clickable { selectedVirtualId = "FAVORITES" },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Rounded.Favorite, contentDescription = null, tint = Color(0xFFE11D48))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text("Favorites", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyMedium)
                                Text("${favoriteVideos.size} items", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (playlists.isEmpty()) {
                item(key = "empty_playlists") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(48.dp))
                            Text("No Custom Playlists Yet", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Create a playlist or import an .m3u/.m3u8 URL or local file.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { showCreateDialog = true }) { Text("Create Playlist") }
                        }
                    }
                }
            } else {
                items(playlists, key = { it.id }) { playlist ->
                    val mosaicModels = remember(playlist, videoMap) {
                        playlist.itemUris.mapNotNull { key ->
                            videoMap[key]?.thumbnailUri ?: Uri.parse(key).takeIf { it.scheme == "content" || it.scheme == "file" }
                        }.distinct().take(4)
                    }
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { selectedPlaylistId = playlist.id },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            FourArtMosaicBox(models = mosaicModels, size = 54.dp)
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(playlist.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                Text(
                                    if (playlist.itemUris.size == 1) "1 video" else "${playlist.itemUris.size} videos",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(onClick = { AppSettings.deletePlaylist(playlist.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Playlist", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
                item { Spacer(modifier = Modifier.height(80.dp)) }
            }
        }
    }

    if (showImportM3uDialog) {
        var m3uUrl by remember { mutableStateOf("") }
        var m3uUserAgent by remember { mutableStateOf("MPVXPlayer/1.0") }
        var isFetching by remember { mutableStateOf(false) }
        var fetchErr by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { if (!isFetching) showImportM3uDialog = false },
            title = { Text("Import M3U Playlist URL", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = m3uUrl,
                        onValueChange = { m3uUrl = it; fetchErr = null },
                        label = { Text("M3U / M3U8 URL") },
                        placeholder = { Text("https://example.com/playlist.m3u") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = m3uUserAgent,
                        onValueChange = { m3uUserAgent = it },
                        label = { Text("Custom User-Agent") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (fetchErr != null) {
                        Text(fetchErr!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = {
                Button(
                    enabled = m3uUrl.isNotBlank() && !isFetching,
                    onClick = {
                        scope.launch {
                            isFetching = true
                            fetchErr = null
                            when (val res = NetworkBrowserClient.fetchM3uWithHeaders(m3uUrl.trim(), userAgent = m3uUserAgent.trim().ifBlank { "MPVXPlayer/1.0" })) {
                                is NetworkBrowseResult.Success -> {
                                    if (res.entries.isEmpty()) {
                                        fetchErr = "No stream links found inside M3U."
                                    } else {
                                        val plName = Uri.parse(m3uUrl.trim()).lastPathSegment?.substringBeforeLast('.')?.ifBlank { "M3U Stream" } ?: "M3U Stream"
                                        val target = selectedPlaylist ?: AppSettings.createPlaylist(plName)
                                        res.entries.forEach { AppSettings.addVideoToPlaylist(target.id, it.url) }
                                        selectedPlaylistId = target.id
                                        showImportM3uDialog = false
                                        Toast.makeText(context, "Imported ${res.entries.size} M3U links", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                is NetworkBrowseResult.Failure -> {
                                    fetchErr = res.message
                                }
                            }
                            isFetching = false
                        }
                    }
                ) {
                    Text(if (isFetching) "Importing..." else "Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { if (!isFetching) showImportM3uDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showCreateDialog) {
        var name by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            title = { Text("Create Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Watch Later, Action Clips...") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            val created = AppSettings.createPlaylist(name)
                            showCreateDialog = false
                            selectedPlaylistId = created.id
                        }
                    },
                    enabled = name.isNotBlank()
                ) { Text("Create") }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) { Text("Cancel") }
            }
        )
    }

    renameTarget?.let { target ->
        var newName by remember { mutableStateOf(target.name) }
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("Rename Playlist", fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(value = newName, onValueChange = { newName = it }, singleLine = true, modifier = Modifier.fillMaxWidth())
            },
            confirmButton = {
                Button(onClick = {
                    AppSettings.renamePlaylist(target.id, newName)
                    renameTarget = null
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (showAddItemsDialog && selectedPlaylist != null) {
        AlertDialog(
            onDismissRequest = { showAddItemsDialog = false },
            title = { Text("Add Videos to ${selectedPlaylist.name}", fontWeight = FontWeight.Bold) },
            text = {
                if (allVideos.isEmpty()) {
                    Text("No local videos available.")
                } else {
                    LazyColumn(modifier = Modifier.height(260.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        items(allVideos, key = { it.id }) { video ->
                            val included = video.id in selectedPlaylist.itemUris
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (included) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f) else Color.Transparent)
                                    .clickable {
                                        if (included) AppSettings.removeVideoFromPlaylist(selectedPlaylist.id, video.id)
                                        else AppSettings.addVideoToPlaylist(selectedPlaylist.id, video.id)
                                    }
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(video.title, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                if (included) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = { showAddItemsDialog = false }) { Text("Done") }
            }
        )
    }
}

@Composable
fun WebStreamsModule(
    onPlayStream: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val savedStreams by AppSettings.savedStreams.collectAsState()
    var streamUrl by remember { mutableStateOf("") }
    var streamTitle by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    fun validateUrl(raw: String): String? {
        val clean = raw.trim()
        val scheme = runCatching { Uri.parse(clean).scheme?.lowercase() }.getOrNull()
        return if (clean.isBlank() || scheme !in listOf("http", "https", "rtsp", "rtmp")) {
            validationError = "Enter a valid http://, https://, or rtsp:// media URL"
            null
        } else {
            validationError = null
            clean
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "streams_header") {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.extendedColors.badgeBlueBg),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Podcasts, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Network Streams", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Direct HLS (.m3u8), DASH (.mpd), RTSP, and HTTP/HTTPS playback", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item(key = "stream_input_card") {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Open Direct Stream URL", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = streamUrl,
                        onValueChange = {
                            streamUrl = it
                            validationError = null
                        },
                        placeholder = { Text("https://example.com/playlist.m3u8") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = streamTitle,
                        onValueChange = { streamTitle = it },
                        placeholder = { Text("Optional stream label") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (validationError != null) {
                        Text(validationError!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        OutlinedButton(
                            onClick = {
                                validateUrl(streamUrl)?.let { valid ->
                                    AppSettings.addSavedStream(streamTitle, valid)
                                    streamUrl = ""
                                    streamTitle = ""
                                }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.BookmarkAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Save")
                        }
                        Button(
                            onClick = {
                                validateUrl(streamUrl)?.let { valid -> onPlayStream(valid) }
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Play")
                        }
                    }
                }
            }
        }

        if (savedStreams.isEmpty()) {
            item(key = "streams_empty") {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("No Bookmarked Streams", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("Save any HTTP/HLS/DASH stream URL above for quick playback.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        } else {
            items(savedStreams, key = { it.id }) { stream ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onPlayStream(stream.url) },
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Rounded.Podcasts, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stream.title, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(stream.url, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                        Surface(shape = RoundedCornerShape(8.dp), color = MaterialTheme.extendedColors.badgeBlueBg) {
                            Text(
                                stream.streamType,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.extendedColors.badgeBlueText,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                            )
                        }
                        IconButton(onClick = { AppSettings.removeSavedStream(stream.id) }) {
                            Icon(Icons.Rounded.DeleteOutline, contentDescription = "Delete Bookmark", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(80.dp)) }
    }
}

@Composable
fun NetworkSharesModule(
    onPlayStream: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val savedShares by AppSettings.networkShares.collectAsState()
    val networkBookmarks by AppSettings.networkBookmarks.collectAsState()
    val networkSortBy by AppSettings.networkSortBy.collectAsState()
    val networkGridView by AppSettings.networkGridView.collectAsState()

    var showAddShareDialog by remember { mutableStateOf(false) }
    var editingShare by remember { mutableStateOf<NetworkShareConfig?>(null) }
    var activeShare by remember { mutableStateOf<NetworkShareConfig?>(null) }
    val urlHistoryStack = remember { mutableStateListOf<String>() }
    var remoteEntries by remember { mutableStateOf<List<NetworkRemoteEntry>>(emptyList()) }
    var shareSearchQuery by remember { mutableStateOf("") }
    var isBrowsing by remember { mutableStateOf(false) }
    var browseError by remember { mutableStateOf<String?>(null) }
    var isAuthPromptVisible by remember { mutableStateOf(false) }

    fun loadShareUrl(share: NetworkShareConfig, url: String, pushHistory: Boolean) {
        scope.launch {
            isBrowsing = true
            browseError = null
            val pwd = AppSettings.getNetworkSharePassword(share.id)
            when (val res = NetworkBrowserClient.browse(context, share, url, pwd)) {
                is NetworkBrowseResult.Success -> {
                    if (pushHistory) urlHistoryStack.add(res.resolvedUrl)
                    remoteEntries = res.entries
                    isBrowsing = false
                }
                is NetworkBrowseResult.Failure -> {
                    isBrowsing = false
                    browseError = res.message
                    if (res.isAuthError) {
                        isAuthPromptVisible = true
                    }
                }
            }
        }
    }

    if (activeShare != null) {
        val share = activeShare!!
        val currentUrl = urlHistoryStack.lastOrNull() ?: share.baseUrl
        val isCurrentBookmarked = networkBookmarks.any { it.url == currentUrl }
        val displayedEntries = remember(remoteEntries, shareSearchQuery, networkSortBy) {
            val searched = if (shareSearchQuery.isBlank()) remoteEntries
            else remoteEntries.filter { it.name.contains(shareSearchQuery, ignoreCase = true) }
            when (networkSortBy) {
                "NAME" -> searched.sortedBy { it.name.lowercase() }
                "TYPE" -> searched.sortedWith(compareBy<NetworkRemoteEntry> { !it.isDirectory }.thenBy { it.name.substringAfterLast('.', "").lowercase() }.thenBy { it.name.lowercase() })
                else -> searched.sortedWith(compareBy<NetworkRemoteEntry> { !it.isDirectory }.thenBy { it.name.lowercase() })
            }
        }

        BackHandler {
            if (urlHistoryStack.size > 1) {
                urlHistoryStack.removeAt(urlHistoryStack.lastIndex)
                loadShareUrl(share, urlHistoryStack.last(), pushHistory = false)
            } else {
                activeShare = null
                urlHistoryStack.clear()
                remoteEntries = emptyList()
                shareSearchQuery = ""
                browseError = null
            }
        }

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = {
                    if (urlHistoryStack.size > 1) {
                        urlHistoryStack.removeAt(urlHistoryStack.lastIndex)
                        loadShareUrl(share, urlHistoryStack.last(), pushHistory = false)
                    } else {
                        activeShare = null
                        urlHistoryStack.clear()
                        remoteEntries = emptyList()
                        shareSearchQuery = ""
                        browseError = null
                    }
                }) {
                    Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back")
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(share.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(currentUrl, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                IconButton(onClick = {
                    if (isCurrentBookmarked) {
                        AppSettings.removeNetworkBookmark(currentUrl)
                    } else {
                        val title = Uri.parse(currentUrl).lastPathSegment?.ifBlank { share.name } ?: share.name
                        AppSettings.addNetworkBookmark(share.id, title, currentUrl, true)
                    }
                }) {
                    Icon(
                        imageVector = if (isCurrentBookmarked) Icons.Rounded.Star else Icons.Rounded.StarOutline,
                        contentDescription = "Bookmark Folder",
                        tint = if (isCurrentBookmarked) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(onClick = { AppSettings.setNetworkGridView(!networkGridView) }) {
                    Icon(
                        imageVector = if (networkGridView) Icons.Rounded.ViewAgenda else Icons.Rounded.GridView,
                        contentDescription = "Toggle Grid"
                    )
                }
                IconButton(onClick = { loadShareUrl(share, currentUrl, pushHistory = false) }) {
                    Icon(Icons.Rounded.Refresh, contentDescription = "Refresh")
                }
            }

            OutlinedTextField(
                value = shareSearchQuery,
                onValueChange = { shareSearchQuery = it },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                placeholder = { Text("Search current folder...") },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("FOLDERS_FIRST" to "Folders First", "NAME" to "Name", "TYPE" to "Type").forEach { (id, label) ->
                    FilterChip(
                        selected = networkSortBy == id,
                        onClick = { AppSettings.setNetworkSortBy(id) },
                        label = { Text(label) }
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 6.dp))

            when {
                isBrowsing -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                browseError != null -> {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.45f))
                    ) {
                        Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text("Connection Issue", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text(browseError!!, style = MaterialTheme.typography.bodySmall)
                            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                Button(onClick = { loadShareUrl(share, currentUrl, pushHistory = false) }) {
                                    Text("Retry")
                                }
                                OutlinedButton(onClick = { isAuthPromptVisible = true }) {
                                    Text("Credentials")
                                }
                            }
                        }
                    }
                }
                displayedEntries.isEmpty() -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("No playable media or subfolders match in this directory.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                else -> {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(if (networkGridView) 2 else 1),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 100.dp)
                    ) {
                        items(displayedEntries, key = { it.url }) { entry ->
                            val cleanEntryUrl = entry.url.substringBefore('?')
                            val isM3u = entry.name.endsWith(".m3u", true) || entry.name.endsWith(".m3u8", true) ||
                                cleanEntryUrl.endsWith(".m3u", true) || cleanEntryUrl.endsWith(".m3u8", true)
                            val isHlsManifest = entry.name.endsWith(".m3u8", true) || cleanEntryUrl.endsWith(".m3u8", true)

                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        if (entry.isDirectory || isM3u) {
                                            loadShareUrl(share, entry.url, pushHistory = true)
                                        } else {
                                            scope.launch {
                                                val pwd = AppSettings.getNetworkSharePassword(share.id)
                                                val resolved = NetworkBrowserClient.resolvePlaybackUrl(share, entry.url, pwd)
                                                onPlayStream(resolved)
                                            }
                                        }
                                    },
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = when {
                                            entry.isDirectory -> Icons.Rounded.Folder
                                            isM3u -> Icons.AutoMirrored.Rounded.PlaylistPlay
                                            else -> Icons.Rounded.PlayArrow
                                        },
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        entry.name,
                                        modifier = Modifier.weight(1f),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = if (networkGridView) 2 else 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (isHlsManifest) {
                                        IconButton(onClick = { onPlayStream(entry.url) }, modifier = Modifier.size(28.dp)) {
                                            Icon(Icons.Rounded.PlayArrow, contentDescription = "Play HLS Stream", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                                        }
                                    } else if (entry.isDirectory || isM3u) {
                                        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    } else {
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item(key = "network_header") {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.extendedColors.badgeBlueBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Lan, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Network Browser", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text("Browse SMB, FTP, SFTP, WebDAV, HTTP, and M3U media servers", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Button(
                        onClick = { showAddShareDialog = true },
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                        shape = CircleShape
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add")
                    }
                }
            }

            if (networkBookmarks.isNotEmpty()) {
                item(key = "network_bookmarks_header") {
                    Text(
                        "Bookmarks (${networkBookmarks.size})",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                items(networkBookmarks, key = { "bm_${it.id}" }) { bm ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val targetShare = savedShares.firstOrNull { it.id == bm.shareId }
                                    ?: NetworkShareConfig(id = bm.shareId, name = bm.title, protocol = "HTTP", baseUrl = bm.url)
                                activeShare = targetShare
                                urlHistoryStack.clear()
                                loadShareUrl(targetShare, bm.url, pushHistory = true)
                            },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Star, contentDescription = null, tint = Color(0xFFF59E0B))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(bm.title, fontWeight = FontWeight.SemiBold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text(bm.url, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = { AppSettings.removeNetworkBookmark(bm.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove Bookmark", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            if (savedShares.isEmpty()) {
                item(key = "network_empty") {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(28.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Rounded.Lan, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(44.dp))
                            Text("No Network Servers Added", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("Connect to an SMB, FTP, SFTP, WebDAV, HTTP directory, or M3U playlist server on your LAN.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Button(onClick = { showAddShareDialog = true }) { Text("Add Network Server") }
                        }
                    }
                }
            } else {
                items(savedShares, key = { it.id }) { share ->
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                activeShare = share
                                urlHistoryStack.clear()
                                loadShareUrl(share, share.baseUrl, pushHistory = true)
                            },
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF10B981).copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Rounded.Lan, contentDescription = null, tint = Color(0xFF10B981))
                            }
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(share.name, style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Bold)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Surface(shape = RoundedCornerShape(6.dp), color = MaterialTheme.extendedColors.badgeBlueBg) {
                                        Text(share.protocol, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.extendedColors.badgeBlueText, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                                Text(share.baseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = {
                                editingShare = share
                                showAddShareDialog = true
                            }) {
                                Icon(Icons.Rounded.Edit, contentDescription = "Edit Server", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(onClick = { AppSettings.removeNetworkShare(share.id) }) {
                                Icon(Icons.Rounded.DeleteOutline, contentDescription = "Remove Server", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(80.dp)) }
        }
    }

    if (showAddShareDialog) {
        val targetEdit = editingShare
        var name by remember(targetEdit?.id) { mutableStateOf(targetEdit?.name.orEmpty()) }
        var protocol by remember(targetEdit?.id) { mutableStateOf(targetEdit?.protocol ?: "SMB") }
        var url by remember(targetEdit?.id) { mutableStateOf(targetEdit?.baseUrl.orEmpty()) }
        var username by remember(targetEdit?.id) { mutableStateOf(targetEdit?.username.orEmpty()) }
        var password by remember(targetEdit?.id) {
            mutableStateOf(
                if (targetEdit != null) AppSettings.getNetworkSharePassword(targetEdit.id) else ""
            )
        }
        var error by remember(targetEdit?.id) { mutableStateOf<String?>(null) }
        var isTesting by remember(targetEdit?.id) { mutableStateOf(false) }
        var testResult by remember(targetEdit?.id) { mutableStateOf<NetworkConnectionTestResult?>(null) }

        fun normalizeShareUrl(proto: String, raw: String): String {
            val trimmed = raw.trim()
            if (trimmed.isEmpty() || trimmed.contains("://")) return trimmed
            val scheme = when (proto.uppercase(Locale.ROOT)) {
                "SMB" -> "smb://"
                "FTP" -> "ftp://"
                "SFTP" -> "sftp://"
                else -> "http://"
            }
            return "$scheme$trimmed"
        }

        fun validateShareUrl(proto: String, candidate: String): String? {
            if (candidate.isBlank()) return "Server address is required"
            return when (proto.uppercase(Locale.ROOT)) {
                "SMB" -> if (!candidate.startsWith("smb://", ignoreCase = true)) {
                    "SMB URL should start with smb:// (e.g. smb://192.168.1.50/Share)"
                } else null
                "FTP" -> if (!candidate.startsWith("ftp://", ignoreCase = true)) {
                    "FTP URL should start with ftp:// (e.g. ftp://192.168.1.50:21)"
                } else null
                "SFTP" -> if (!candidate.startsWith("sftp://", ignoreCase = true)) {
                    "SFTP URL should start with sftp:// (e.g. sftp://192.168.1.50:22/media)"
                } else null
                else -> if (!candidate.startsWith("http://", ignoreCase = true) && !candidate.startsWith("https://", ignoreCase = true)) {
                    "URL must start with http:// or https://"
                } else null
            }
        }

        val urlPlaceholder = when (protocol.uppercase(Locale.ROOT)) {
            "SMB" -> "smb://192.168.1.50/Media"
            "FTP" -> "ftp://192.168.1.50:21/videos"
            "SFTP" -> "sftp://192.168.1.50:22/home/media"
            else -> "http://192.168.1.50:8080"
        }

        AlertDialog(
            onDismissRequest = {
                showAddShareDialog = false
                editingShare = null
            },
            title = { Text(if (targetEdit != null) "Edit Network Server" else "Add Network Server", fontWeight = FontWeight.Bold) },
            text = {
                Column(
                    modifier = Modifier.verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("SMB", "FTP", "SFTP", "WebDAV", "HTTP").forEach { proto ->
                            FilterChip(
                                selected = protocol.equals(proto, ignoreCase = true),
                                onClick = {
                                    protocol = proto
                                    error = null
                                    testResult = null
                                },
                                label = { Text(proto) }
                            )
                        }
                    }
                    OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Server Label") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = url,
                        onValueChange = {
                            url = it
                            error = null
                            testResult = null
                        },
                        label = { Text("$protocol Server URL") },
                        placeholder = { Text(urlPlaceholder) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(value = username, onValueChange = { username = it }, label = { Text("Username (Optional)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password (Encrypted Keystore Storage)") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            enabled = !isTesting,
                            onClick = {
                                val normalized = normalizeShareUrl(protocol, url)
                                val validation = validateShareUrl(protocol, normalized)
                                if (validation != null) {
                                    error = validation
                                } else {
                                    url = normalized
                                    error = null
                                    isTesting = true
                                    testResult = null
                                    scope.launch {
                                        val probeConfig = NetworkShareConfig(
                                            id = targetEdit?.id ?: "probe_${System.currentTimeMillis()}",
                                            name = name.ifBlank { normalized },
                                            protocol = protocol,
                                            baseUrl = normalized,
                                            username = username.trim()
                                        )
                                        testResult = NetworkBrowserClient.testConnection(context, probeConfig, password)
                                        isTesting = false
                                    }
                                }
                            }
                        ) {
                            if (isTesting) {
                                CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Testing...")
                            } else {
                                Text("Test Connection")
                            }
                        }
                        testResult?.let { res ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (res.ok) Color(0xFF10B981).copy(alpha = 0.16f) else MaterialTheme.colorScheme.errorContainer
                            ) {
                                Text(
                                    text = res.statusText,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = if (res.ok) Color(0xFF10B981) else MaterialTheme.colorScheme.onErrorContainer,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }
                    if (error != null) {
                        Text(error!!, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val normalized = normalizeShareUrl(protocol, url)
                    val validation = validateShareUrl(protocol, normalized)
                    if (validation != null) {
                        error = validation
                    } else {
                        if (targetEdit != null) {
                            AppSettings.updateNetworkShare(
                                shareId = targetEdit.id,
                                name = name,
                                protocol = protocol,
                                baseUrl = normalized,
                                username = username,
                                newPassword = password
                            )
                            if (activeShare?.id == targetEdit.id) {
                                activeShare = targetEdit.copy(
                                    name = name.ifBlank { normalized },
                                    protocol = protocol,
                                    baseUrl = normalized.trimEnd('/'),
                                    username = username.trim()
                                )
                            }
                        } else {
                            AppSettings.saveNetworkShare(name, protocol, normalized, username, password)
                        }
                        showAddShareDialog = false
                        editingShare = null
                    }
                }) { Text(if (targetEdit != null) "Update" else "Save") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddShareDialog = false
                    editingShare = null
                }) { Text("Cancel") }
            }
        )
    }

    if (isAuthPromptVisible && activeShare != null) {
        var passwordInput by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { isAuthPromptVisible = false },
            title = { Text("Authentication Required", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Enter password for ${activeShare!!.username.ifBlank { activeShare!!.name }}:", style = MaterialTheme.typography.bodySmall)
                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { passwordInput = it },
                        label = { Text("Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(onClick = {
                    val share = activeShare!!
                    AppSettings.setNetworkSharePassword(share.id, passwordInput)
                    isAuthPromptVisible = false
                    val target = urlHistoryStack.lastOrNull() ?: share.baseUrl
                    loadShareUrl(share, target, pushHistory = false)
                }) { Text("Connect") }
            },
            dismissButton = {
                TextButton(onClick = { isAuthPromptVisible = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun SwipeableMiniPlayerDock(
    title: String,
    subtitle: String,
    isPlaying: Boolean,
    progress: Float,
    artworkUri: Uri? = null,
    onPlayPauseClick: () -> Unit,
    onNextClick: (() -> Unit)? = null,
    onCloseClick: () -> Unit,
    onExpandClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isLiquidGlass by AppSettings.isLiquidGlassEnabled.collectAsState()
    val glassOpacity by AppSettings.glassFrostOpacity.collectAsState()
    val cardModifier = modifier
        .fillMaxWidth()
        .padding(horizontal = 12.dp, vertical = 4.dp)
        .clickable { onExpandClick() }
        .pointerInput(Unit) {
            detectVerticalDragGestures { _, dragAmount ->
                if (dragAmount > 25f) {
                    onCloseClick()
                } else if (dragAmount < -25f) {
                    onExpandClick()
                }
            }
        }

    val content: @Composable () -> Unit = {
        Column(modifier = Modifier.fillMaxWidth()) {
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.5.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.MusicNote,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(24.dp)
                    )
                    if (artworkUri != null) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(artworkUri).crossfade(true).build(),
                            contentDescription = title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                        contentDescription = "Play/Pause",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (onNextClick != null) {
                    IconButton(onClick = onNextClick) {
                        Icon(
                            imageVector = Icons.Rounded.SkipNext,
                            contentDescription = "Next Track",
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close Mini-Player",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }

    if (isLiquidGlass) {
        GlassSurface(
            modifier = cardModifier,
            shape = RoundedCornerShape(18.dp),
            opacity = glassOpacity.coerceIn(0.45f, 0.92f),
            elevation = 8.dp
        ) {
            content()
        }
    } else {
        Surface(
            modifier = cardModifier,
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp
        ) {
            content()
        }
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}
