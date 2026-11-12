package com.cmstudio.player.ui.library

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import androidx.core.content.ContextCompat
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.FavoriteBorder
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.PlayCircleOutline
import androidx.compose.material.icons.rounded.Podcasts
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.cmstudio.player.PlaybackHistoryBridge
import com.cmstudio.player.R
import com.cmstudio.player.library.MediaQualityFilterType
import com.cmstudio.player.library.SmartCollectionsAndStats
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.model.MediaFolderItem
import com.cmstudio.player.ui.model.MediaVideoItem
import com.cmstudio.player.ui.model.WatchStreakGoal
import com.cmstudio.player.ui.servers.MediaServerKind
import com.cmstudio.player.ui.settings.ModernSettingsActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal fun <T> sortByMediaOption(
    items: List<T>,
    sortOption: String,
    sortAscending: Boolean,
    titleOf: (T) -> String,
    dateAddedOf: (T) -> Long,
    dateModifiedOf: (T) -> Long,
    durationOf: (T) -> Long,
    sizeOf: (T) -> Long,
    recentPlayedOf: (T) -> Long = { dateModifiedOf(it) }
): List<T> {
    val comparator = when (sortOption) {
        "DATE_ADDED" -> compareBy<T> { dateAddedOf(it) }.thenBy { titleOf(it).lowercase(Locale.US) }
        "DATE" -> compareBy<T> { dateModifiedOf(it) }.thenBy { titleOf(it).lowercase(Locale.US) }
        "RECENTLY_PLAYED" -> compareBy<T> { recentPlayedOf(it) }.thenBy { dateModifiedOf(it) }.thenBy { titleOf(it).lowercase(Locale.US) }
        "DURATION" -> compareBy<T> { durationOf(it) }.thenBy { titleOf(it).lowercase(Locale.US) }
        "SIZE" -> compareBy<T> { sizeOf(it) }.thenBy { titleOf(it).lowercase(Locale.US) }
        else -> compareBy { titleOf(it).lowercase(Locale.US) }
    }
    return if (sortAscending) items.sortedWith(comparator) else items.sortedWith(comparator.reversed())
}

internal fun sortVideoList(
    videos: List<MediaVideoItem>,
    sortOption: String,
    sortAscending: Boolean,
    recentPlayedById: Map<String, Long> = emptyMap()
): List<MediaVideoItem> = sortByMediaOption(
    items = videos,
    sortOption = sortOption,
    sortAscending = sortAscending,
    titleOf = { it.title },
    dateAddedOf = { it.dateAddedTimestamp },
    dateModifiedOf = { it.modifiedTimestamp },
    durationOf = { it.durationMs },
    sizeOf = { it.sizeBytes },
    recentPlayedOf = { recentPlayedById[it.id] ?: it.modifiedTimestamp }
)

internal fun sortFolderList(
    folders: List<MediaFolderItem>,
    pinnedIds: Set<String>,
    sortOption: String,
    sortAscending: Boolean
): List<MediaFolderItem> {
    val sorted = sortByMediaOption(
        items = folders,
        sortOption = sortOption,
        sortAscending = sortAscending,
        titleOf = { it.name },
        dateAddedOf = { it.dateAddedTimestamp },
        dateModifiedOf = { it.modifiedTimestamp },
        durationOf = { it.totalDurationMs },
        sizeOf = { it.totalSizeBytes }
    )
    return if (pinnedIds.isEmpty()) sorted else sorted.sortedByDescending { it.id in pinnedIds }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun LibraryScreen(
    continueWatchingVideos: List<MediaVideoItem> = emptyList(),
    recentlyAddedVideos: List<MediaVideoItem> = emptyList(),
    allVideos: List<MediaVideoItem> = emptyList(),
    folders: List<MediaFolderItem> = emptyList(),
    watchStreakGoal: WatchStreakGoal = WatchStreakGoal(),
    onVideoClick: (MediaVideoItem) -> Unit = {},
    onVideoQueuePlay: (MediaVideoItem, List<MediaVideoItem>) -> Unit = { v, _ -> onVideoClick(v) },
    onPlayNetworkStreamClick: (String) -> Unit = {},
    onRefreshClick: () -> Unit = {},
    onNavigateTab: (String) -> Unit = {},
    onDeleteVideoClick: (MediaVideoItem) -> Unit = {},
    onDeleteVideosBatch: (List<MediaVideoItem>) -> Unit = { list -> list.firstOrNull()?.let(onDeleteVideoClick) },
    onToggleVideoWatched: (MediaVideoItem) -> Unit = {},
    onToggleFolderWatched: (String) -> Unit = {},
    onOpenVaultClick: () -> Unit = { onNavigateTab("VAULT") },
    onSelectionModeChange: (Boolean) -> Unit = {},
    onOpenSystemPickerFile: (Uri) -> Unit = { uri -> onPlayNetworkStreamClick(uri.toString()) },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.screenWidthDp > configuration.screenHeightDp

    // AppSettings reactive state
    val darkModePref by AppSettings.darkMode.collectAsState()
    val isSystemDark = isSystemInDarkTheme()
    val isDarkTheme = when (darkModePref) {
        "DARK" -> true
        "LIGHT" -> false
        else -> isSystemDark
    }
    var themeToggleCenter by remember { mutableStateOf(0f to 0f) }
    val sortOption by AppSettings.sortOption.collectAsState()
    val sortAscending by AppSettings.sortAscending.collectAsState()
    val folderViewMode by AppSettings.folderViewMode.collectAsState() // "FOLDER", "TREE", "MEDIA"
    val layoutMode by AppSettings.viewMode.collectAsState() // "LIST", "GRID"
    val showFolderSize by AppSettings.showFolderSize.collectAsState()
    val showFolderPath by AppSettings.showFolderPath.collectAsState()
    val showDateModified by AppSettings.showDateModified.collectAsState()
    val showResolutionBadge by AppSettings.showResolutionBadge.collectAsState()
    val unlimitedNameLines by AppSettings.unlimitedNameLines.collectAsState()
    val showExtensionField by AppSettings.showExtensionField.collectAsState()
    val showDurationField by AppSettings.showDurationField.collectAsState()
    val showProgressBar by AppSettings.showProgressBar.collectAsState()
    val centerGridTitles by AppSettings.centerGridTitles.collectAsState()
    val showTotalVideosChip by AppSettings.showTotalVideosChip.collectAsState()
    val showTotalDurationChip by AppSettings.showTotalDurationChip.collectAsState()
    val showFolderThumbnails by AppSettings.showFolderThumbnails.collectAsState()
    val showNewBadge by AppSettings.showNewBadge.collectAsState()
    val showQuickPlayFab by AppSettings.showQuickPlayFab.collectAsState()
    val quickPlayFabDirect by AppSettings.quickPlayFabDirect.collectAsState()
    val dualPaneTablet by AppSettings.dualPaneTablet.collectAsState()
    val folderGridColsPortrait by AppSettings.folderGridColumnsPortrait.collectAsState()
    val folderGridColsLandscape by AppSettings.folderGridColumnsLandscape.collectAsState()
    val videoGridColsPortrait by AppSettings.videoGridColumnsPortrait.collectAsState()
    val videoGridColsLandscape by AppSettings.videoGridColumnsLandscape.collectAsState()
    val mediaQualityFilter by AppSettings.mediaQualityFilter.collectAsState()
    val onlyShowFoldersInHome by AppSettings.onlyShowFoldersInHome.collectAsState()
    val homeCompactGrid by AppSettings.homeCompactGrid.collectAsState()
    val mediaServers by AppSettings.mediaServers.collectAsState()
    val safTreeUris by AppSettings.safMediaTreeUris.collectAsState()
    val showHiddenMediaFolders by AppSettings.showHiddenMediaFolders.collectAsState()

    var hasObservedFilterInit by remember { mutableStateOf(false) }
    LaunchedEffect(showHiddenMediaFolders, safTreeUris) {
        if (hasObservedFilterInit) {
            onRefreshClick()
        } else {
            hasObservedFilterInit = true
        }
    }

    val activeFolderCols = run {
        val base = if (isLandscape) folderGridColsLandscape else folderGridColsPortrait
        if (homeCompactGrid && layoutMode == "GRID") (base + 1).coerceAtMost(if (isLandscape) 6 else 4) else base
    }
    val activeVideoCols = run {
        val base = if (isLandscape) videoGridColsLandscape else videoGridColsPortrait
        if (homeCompactGrid && layoutMode == "GRID") (base + 1).coerceAtMost(if (isLandscape) 6 else 4) else base
    }
    val activeQualityEnum = remember(mediaQualityFilter) {
        MediaQualityFilterType.entries.firstOrNull { it.id == mediaQualityFilter } ?: MediaQualityFilterType.ALL
    }
    val recentPlayedById = remember(allVideos, sortOption) {
        if (sortOption == "RECENTLY_PLAYED") {
            PlaybackHistoryBridge.loadHistory(context).associate { it.uri.toString() to it.lastPlayedAtMs }
        } else {
            emptyMap()
        }
    }

    val favoriteUris by AppSettings.favoriteVideoUris.collectAsState()
    val pinnedFolderIds by AppSettings.pinnedFolderIds.collectAsState()
    val userPlaylists by AppSettings.userPlaylists.collectAsState()

    var searchQuery by rememberSaveable { mutableStateOf("") }
    var isSearchExpanded by rememberSaveable { mutableStateOf(false) }
    var activeQuickFilter by rememberSaveable { mutableStateOf("NONE") } // "NONE", "ALL_VIDEOS", "FAVORITES"
    var showSortSheet by remember { mutableStateOf(false) }
    var showStreamDialog by remember { mutableStateOf(false) }
    var isFabMenuExpanded by remember { mutableStateOf(false) }
    var treeSegments by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }

    // Multi-selection state
    var selectedVideoIds by remember { mutableStateOf<Set<String>>(emptySet()) }
    val inSelectionMode = selectedVideoIds.isNotEmpty()
    LaunchedEffect(inSelectionMode) {
        onSelectionModeChange(inSelectionMode)
        if (inSelectionMode) isFabMenuExpanded = false
    }

    // Video / Folder action menus & dialogs
    var contextMenuVideo by remember { mutableStateOf<MediaVideoItem?>(null) }
    var infoDialogVideo by remember { mutableStateOf<MediaVideoItem?>(null) }
    var deleteConfirmVideos by remember { mutableStateOf<List<MediaVideoItem>>(emptyList()) }
    var addToPlaylistVideos by remember { mutableStateOf<List<MediaVideoItem>>(emptyList()) }
    var contextMenuFolder by remember { mutableStateOf<MediaFolderItem?>(null) }

    // Folder drill-down state
    var selectedFolder by remember { mutableStateOf<MediaFolderItem?>(null) }
    var tabletSelectedFolder by remember { mutableStateOf<MediaFolderItem?>(null) }

    // Open File system video picker for Quick Play FAB
    val openFilePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { pickedUri ->
        if (pickedUri != null) {
            runCatching {
                context.contentResolver.takePersistableUriPermission(
                    pickedUri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }
            onOpenSystemPickerFile(pickedUri)
        }
    }

    // Filter + sort videos cleanly with real numeric metrics
    val sortedVideos = remember(allVideos, sortOption, sortAscending, searchQuery, activeQuickFilter, favoriteUris, activeQualityEnum, recentPlayedById) {
        var list = SmartCollectionsAndStats.filterByQuality(allVideos, activeQualityEnum)
        if (activeQuickFilter == "FAVORITES") {
            list = list.filter { it.id in favoriteUris }
        }
        if (searchQuery.isNotBlank()) {
            val q = searchQuery.trim()
            list = list.filter {
                it.title.contains(q, ignoreCase = true) ||
                    it.folderName.contains(q, ignoreCase = true)
            }
        }
        sortVideoList(list, sortOption, sortAscending, recentPlayedById)
    }

    val sortedFolders = remember(folders, searchQuery, sortOption, sortAscending, pinnedFolderIds, activeQualityEnum, allVideos) {
        val allowedFolderIds = if (activeQualityEnum == MediaQualityFilterType.ALL) {
            null
        } else {
            SmartCollectionsAndStats.filterByQuality(allVideos, activeQualityEnum).map { it.folderId }.toHashSet()
        }
        val base = folders.filter { f ->
            val qualityOk = allowedFolderIds == null || f.id in allowedFolderIds
            val searchOk = searchQuery.isBlank() ||
                f.name.contains(searchQuery.trim(), ignoreCase = true) ||
                f.path.contains(searchQuery.trim(), ignoreCase = true)
            qualityOk && searchOk
        }
        sortFolderList(base, pinnedFolderIds, sortOption, sortAscending)
    }

    val treeLevel = remember(sortedVideos, treeSegments, folderViewMode) {
        if (folderViewMode == "TREE") {
            MediaTreeResolver.resolveDirectoryLevel(sortedVideos, treeSegments)
        } else {
            MediaTreeLevel(emptyList(), emptyList(), emptyList())
        }
    }

    val currentSelectableVideos = remember(selectedFolder, folderViewMode, treeLevel, sortedVideos, allVideos, sortOption, sortAscending) {
        when {
            selectedFolder != null -> sortVideoList(allVideos.filter { it.folderId == selectedFolder!!.id }, sortOption, sortAscending)
            folderViewMode == "TREE" -> treeLevel.videos
            else -> sortedVideos
        }
    }

    fun toggleVideoSelection(videoId: String) {
        selectedVideoIds = if (videoId in selectedVideoIds) selectedVideoIds - videoId else selectedVideoIds + videoId
    }

    fun handleVideoCardClick(video: MediaVideoItem, queue: List<MediaVideoItem>) {
        if (inSelectionMode) toggleVideoSelection(video.id) else onVideoQueuePlay(video, queue)
    }

    fun handleVideoCardLongClick(video: MediaVideoItem) {
        if (inSelectionMode) toggleVideoSelection(video.id) else selectedVideoIds = selectedVideoIds + video.id
    }

    BackHandler(enabled = inSelectionMode) {
        selectedVideoIds = emptySet()
    }
    BackHandler(enabled = !inSelectionMode && selectedFolder != null) {
        selectedFolder = null
    }
    BackHandler(enabled = !inSelectionMode && selectedFolder == null && folderViewMode == "TREE" && treeSegments.isNotEmpty()) {
        treeSegments = treeSegments.dropLast(1)
    }

    if (selectedFolder != null) {
        Box(modifier = modifier.fillMaxSize()) {
            FolderVideosDetailView(
                folder = selectedFolder!!,
                allVideos = allVideos,
                layoutMode = layoutMode,
                gridColumns = activeVideoCols,
                sortOption = sortOption,
                sortAscending = sortAscending,
                showResolutionBadge = showResolutionBadge,
                showDateModified = showDateModified,
                showSize = showFolderSize,
                showDuration = showDurationField,
                showExtension = showExtensionField,
                showProgressBar = showProgressBar,
                centerGridTitles = centerGridTitles,
                unlimitedLines = unlimitedNameLines,
                favoriteUris = favoriteUris,
                selectedVideoIds = selectedVideoIds,
                onBackClick = { selectedFolder = null },
                onOpenSortSheet = { showSortSheet = true },
                onVideoClick = { clicked, folderQueue -> handleVideoCardClick(clicked, folderQueue) },
                onVideoLongClick = { handleVideoCardLongClick(it) },
                onToggleVideoWatched = onToggleVideoWatched
            )
            if (inSelectionMode) {
                val chosen = currentSelectableVideos.filter { it.id in selectedVideoIds }
                FloatingBottomActionBar(
                    selectedCount = chosen.size,
                    onPlaySelected = {
                        chosen.firstOrNull()?.let { first -> onVideoQueuePlay(first, chosen) }
                        selectedVideoIds = emptySet()
                    },
                    onAddToPlaylist = { addToPlaylistVideos = chosen },
                    onFavoriteSelected = {
                        chosen.forEach { AppSettings.toggleFavoriteVideo(it.id) }
                        selectedVideoIds = emptySet()
                    },
                    onVaultSelected = {
                        chosen.forEach { AppSettings.addVaultVideo(it.id) }
                        selectedVideoIds = emptySet()
                        onRefreshClick()
                    },
                    onShareSelected = { shareVideoUris(context, chosen); selectedVideoIds = emptySet() },
                    onDeleteSelected = { deleteConfirmVideos = chosen },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            }
        }
    } else {
        val isTabletDualPane = dualPaneTablet &&
            configuration.smallestScreenWidthDp >= 600 &&
            folderViewMode == "FOLDER" &&
            !inSelectionMode &&
            searchQuery.isBlank() &&
            activeQuickFilter == "NONE"

        val activeDualFolder = remember(tabletSelectedFolder, sortedFolders) {
            sortedFolders.firstOrNull { it.id == tabletSelectedFolder?.id } ?: sortedFolders.firstOrNull()
        }

        val listState = rememberLazyListState()
        var isFabVisible by remember { mutableStateOf(true) }
        LaunchedEffect(listState) {
            var prevIndex = 0
            var prevOffset = 0
            androidx.compose.runtime.snapshotFlow {
                listState.firstVisibleItemIndex to listState.firstVisibleItemScrollOffset
            }.collect { (idx, off) ->
                val scrollingDown = idx > prevIndex || (idx == prevIndex && off > prevOffset + 14)
                val scrollingUp = idx < prevIndex || (idx == prevIndex && off < prevOffset - 14)
                if (idx == 0 && off == 0) isFabVisible = true
                else if (scrollingDown && !isFabMenuExpanded) isFabVisible = false
                else if (scrollingUp) isFabVisible = true
                prevIndex = idx
                prevOffset = off
            }
        }

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            Row(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .then(if (isTabletDualPane) Modifier.weight(0.42f) else Modifier.fillMaxSize()),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 104.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 1. Top Header OR Multi-Selection Top Bar
                    item(key = "home_top_header") {
                        if (inSelectionMode) {
                            SelectionTopBar(
                                selectedCount = selectedVideoIds.size,
                                totalCount = currentSelectableVideos.size,
                                onClearSelection = { selectedVideoIds = emptySet() },
                                onSelectAll = { selectedVideoIds = currentSelectableVideos.map { it.id }.toSet() },
                                onInvertSelection = {
                                    val allSet = currentSelectableVideos.map { it.id }.toSet()
                                    selectedVideoIds = allSet - selectedVideoIds
                                }
                            )
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .combinedClickable(
                                            onClick = {},
                                            onDoubleClick = onOpenVaultClick,
                                            onLongClick = onOpenVaultClick
                                        )
                                        .padding(vertical = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Image(
                                        painter = painterResource(id = R.drawable.ic_kr_splash_logo),
                                        contentDescription = "MPVX Player Logo",
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = "MPVX Player",
                                        style = MaterialTheme.typography.headlineSmall.copy(
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = (-0.5).sp
                                        ),
                                        color = MaterialTheme.colorScheme.onBackground,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    modifier = Modifier.onGloballyPositioned { coords ->
                                        val center = coords.localToWindow(
                                            Offset(coords.size.width / 2f, coords.size.height / 2f)
                                        )
                                        themeToggleCenter = center.x to center.y
                                    },
                                    onClick = {
                                        AppSettings.setThemeTransitionOrigin(themeToggleCenter)
                                        AppSettings.setDarkMode(if (isDarkTheme) "LIGHT" else "DARK")
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isDarkTheme) Icons.Rounded.LightMode else Icons.Rounded.DarkMode,
                                        contentDescription = if (isDarkTheme) "Switch to Light Mode" else "Switch to Dark Mode",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { isSearchExpanded = !isSearchExpanded }) {
                                    Icon(
                                        imageVector = Icons.Rounded.Search,
                                        contentDescription = "Search",
                                        tint = if (isSearchExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(onClick = { showSortSheet = true }) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Rounded.Sort,
                                        contentDescription = "Sort & View",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                IconButton(
                                    onClick = {
                                        context.startActivity(Intent(context, ModernSettingsActivity::class.java))
                                    }
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Settings,
                                        contentDescription = "Settings",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }

                    // 2. Expandable Search Bar
                    item(key = "home_search_bar") {
                        AnimatedVisibility(
                            visible = isSearchExpanded && !inSelectionMode,
                            enter = fadeIn() + expandVertically(),
                            exit = fadeOut() + shrinkVertically()
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                                ) {
                                    OutlinedTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        placeholder = {
                                            Text(
                                                text = "Search videos, folders, or paste URL...",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        },
                                        leadingIcon = {
                                            Icon(
                                                imageVector = Icons.Rounded.Search,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        },
                                        trailingIcon = {
                                            if (searchQuery.isNotEmpty()) {
                                                IconButton(onClick = { searchQuery = "" }) {
                                                    Icon(
                                                        imageVector = Icons.Rounded.Close,
                                                        contentDescription = "Clear",
                                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                                    )
                                                }
                                            }
                                        },
                                        singleLine = true,
                                        shape = CircleShape,
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedContainerColor = Color.Transparent,
                                            unfocusedContainerColor = Color.Transparent,
                                            focusedBorderColor = Color.Transparent,
                                            unfocusedBorderColor = Color.Transparent
                                        ),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }

                                val trimmedQuery = searchQuery.trim()
                                if (trimmedQuery.startsWith("http://", ignoreCase = true) ||
                                    trimmedQuery.startsWith("https://", ignoreCase = true) ||
                                    trimmedQuery.startsWith("rtsp://", ignoreCase = true)
                                ) {
                                    QuickStripChip(
                                        icon = Icons.Rounded.PlayCircleOutline,
                                        label = "Play URL: $trimmedQuery",
                                        onClick = { onPlayNetworkStreamClick(trimmedQuery) }
                                    )
                                }
                            }
                        }
                    }

                // 3. Home Dashboard Header (Continue Watching Hero + 2x2 Bento + Online Stream + Recents Carousel)
                val showDashboardHero = !inSelectionMode && searchQuery.isBlank() &&
                    activeQuickFilter == "NONE" && treeSegments.isEmpty()

                if (showDashboardHero && continueWatchingVideos.isNotEmpty()) {
                    item(key = "home_continue_watching_hero") {
                        val hero = continueWatchingVideos.first()
                        ContinueWatchingHeroCard(
                            video = hero,
                            showExtension = showExtensionField,
                            showProgressBar = showProgressBar,
                            onResumeClick = { onVideoQueuePlay(hero, continueWatchingVideos) }
                        )
                    }
                }

                if (!inSelectionMode && searchQuery.isBlank() && treeSegments.isEmpty()) {
                    item(key = "home_quick_access_bento") {
                        QuickAccessBentoSection(
                            allVideosCount = allVideos.size,
                            foldersCount = folders.size,
                            favoritesCount = allVideos.count { it.id in favoriteUris },
                            playlistsCount = userPlaylists.size,
                            activeQuickFilter = activeQuickFilter,
                            onAllVideosClick = {
                                activeQuickFilter = if (activeQuickFilter == "ALL_VIDEOS") "NONE" else "ALL_VIDEOS"
                            },
                            onFoldersClick = {
                                activeQuickFilter = "NONE"
                                AppSettings.setFolderViewMode("FOLDER")
                            },
                            onFavoritesClick = {
                                activeQuickFilter = if (activeQuickFilter == "FAVORITES") "NONE" else "FAVORITES"
                            },
                            onPlaylistsClick = { onNavigateTab("PLAYLISTS") },
                            onOnlineStreamClick = { onNavigateTab("STREAMS") }
                        )
                    }

                    item(key = "home_storage_locations_strip") {
                        val freeStorageGb = remember {
                            runCatching {
                                val stat = StatFs(Environment.getDataDirectory().path)
                                val avail = stat.availableBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                                val total = stat.totalBytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
                                String.format(Locale.US, "%.1f / %.0f GB free", avail, total)
                            }.getOrElse { "Internal Storage" }
                        }
                        val hasMediaPermission = remember(context) {
                            val perm = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                Manifest.permission.READ_MEDIA_VIDEO
                            } else {
                                Manifest.permission.READ_EXTERNAL_STORAGE
                            }
                            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
                        }
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            color = MaterialTheme.colorScheme.surfaceContainerLow,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        val intent = Intent(context, ModernSettingsActivity::class.java)
                                            .putExtra(ModernSettingsActivity.EXTRA_CATEGORY, "MEDIA_LIBRARY")
                                        context.startActivity(intent)
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Folder,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "Storage • $freeStorageGb",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = buildString {
                                            append("${allVideos.size} videos in ${folders.size} folders")
                                            if (safTreeUris.isNotEmpty()) append(" • ${safTreeUris.size} SAF folder(s)")
                                            if (showHiddenMediaFolders) append(" • Hidden shown")
                                            append(if (hasMediaPermission) " • Media permission OK" else " • Grant storage permission")
                                        },
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                                    contentDescription = "Manage Storage",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                if (showDashboardHero && mediaServers.isNotEmpty()) {
                    item(key = "home_configured_media_servers") {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(
                                text = "Configured Media Servers",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .horizontalScroll(rememberScrollState()),
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                mediaServers.forEach { srv ->
                                    val kind = srv.kind
                                    val destId = when (kind) {
                                        MediaServerKind.JELLYFIN -> "JELLYFIN"
                                        MediaServerKind.NAVIDROME -> "NAVIDROME"
                                        MediaServerKind.AUDIOBOOKSHELF -> "AUDIOBOOKS"
                                    }
                                    Surface(
                                        modifier = Modifier.clickable {
                                            AppSettings.setServerTabEnabled(destId, true)
                                            onNavigateTab(destId)
                                        },
                                        shape = RoundedCornerShape(14.dp),
                                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Icon(
                                                imageVector = when (kind) {
                                                    MediaServerKind.JELLYFIN -> Icons.Rounded.Movie
                                                    MediaServerKind.NAVIDROME -> Icons.Rounded.MusicNote
                                                    MediaServerKind.AUDIOBOOKSHELF -> Icons.Rounded.Podcasts
                                                },
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Column {
                                                Text(
                                                    text = srv.name.ifBlank { kind.label },
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                                Text(
                                                    text = kind.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.75f)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showDashboardHero) {
                    val homeRecentItems = (continueWatchingVideos + recentlyAddedVideos + allVideos)
                        .distinctBy { it.id }
                        .take(12)
                    if (homeRecentItems.isNotEmpty()) {
                        item(key = "home_recently_played_carousel") {
                            RecentlyPlayedCarousel(
                                items = homeRecentItems,
                                showExtension = showExtensionField,
                                showProgressBar = showProgressBar,
                                onItemClick = { item -> onVideoQueuePlay(item, homeRecentItems) },
                                onSeeAllClick = { onNavigateTab("RECENTS") }
                            )
                        }
                    }
                }

                // 4. Storage Section Ribbon + Content based on View Mode / Quick Filter
                val showFlatMediaOnly = activeQuickFilter != "NONE" || folderViewMode == "MEDIA"

                item(key = "storage_section_ribbon") {
                    StorageSectionRibbon(
                        title = when {
                            activeQuickFilter == "FAVORITES" -> "Favorite Videos"
                            activeQuickFilter == "ALL_VIDEOS" || folderViewMode == "MEDIA" -> "All Videos"
                            folderViewMode == "TREE" -> "Storage Tree"
                            else -> "Folders"
                        },
                        countBadge = when {
                            showFlatMediaOnly -> sortedVideos.size
                            folderViewMode == "TREE" -> treeLevel.directories.size + treeLevel.videos.size
                            else -> sortedFolders.size
                        },
                        viewMode = folderViewMode,
                        layoutMode = layoutMode,
                        onOpenSortSheet = { showSortSheet = true },
                        onCycleViewMode = {
                            activeQuickFilter = "NONE"
                            val next = when (folderViewMode) {
                                "FOLDER" -> "TREE"
                                "TREE" -> "MEDIA"
                                else -> "FOLDER"
                            }
                            AppSettings.setFolderViewMode(next)
                        },
                        onToggleLayout = {
                            AppSettings.setViewMode(if (layoutMode == "GRID") "LIST" else "GRID")
                        }
                    )
                }

                if (activeQualityEnum != MediaQualityFilterType.ALL) {
                    item(key = "active_quality_filter_banner") {
                        FilterChip(
                            selected = true,
                            onClick = { AppSettings.setMediaQualityFilter(MediaQualityFilterType.ALL.id) },
                            label = { Text("Quality: ${activeQualityEnum.label}  ✕") }
                        )
                    }
                }

                if (showFlatMediaOnly) {
                    renderVideoCollectionItems(
                        videos = sortedVideos,
                        layoutMode = layoutMode,
                        gridColumns = activeVideoCols,
                        favoriteUris = favoriteUris,
                        selectedVideoIds = selectedVideoIds,
                        showResolutionBadge = showResolutionBadge,
                        showDateModified = showDateModified,
                        showSize = showFolderSize,
                        showDuration = showDurationField,
                        showExtension = showExtensionField,
                        showProgressBar = showProgressBar,
                        centerGridTitles = centerGridTitles,
                        unlimitedLines = unlimitedNameLines,
                        onVideoClick = { v -> handleVideoCardClick(v, sortedVideos) },
                        onVideoLongClick = { v -> handleVideoCardLongClick(v) },
                        onVideoMoreClick = { v -> contextMenuVideo = v },
                        onToggleVideoWatched = onToggleVideoWatched
                    )
                } else if (folderViewMode == "TREE") {
                    item(key = "tree_breadcrumbs") {
                        TreeBreadcrumbBar(
                            segments = treeSegments,
                            onSegmentClick = { idx ->
                                treeSegments = if (idx < 0) emptyList() else treeSegments.take(idx + 1)
                            }
                        )
                    }

                    if (treeLevel.directories.isNotEmpty()) {
                        items(
                            items = treeLevel.directories,
                            key = { "tree_dir_${it.fullSegments.joinToString("/")}" }
                        ) { dir ->
                            val asFolder = MediaFolderItem(
                                id = dir.fullSegments.joinToString("/"),
                                name = dir.name,
                                path = dir.fullSegments.joinToString("/"),
                                videoCount = dir.videoCount,
                                sizeText = formatFileSizeCompact(dir.totalSizeBytes),
                                dateModifiedText = "",
                                latestVideoThumbnailUri = dir.latestThumbnailUri,
                                newCount = dir.newCount,
                                totalSizeBytes = dir.totalSizeBytes,
                                totalDurationMs = dir.totalDurationMs
                            )
                            FolderCardItem(
                                folder = asFolder,
                                isPinned = false,
                                showPath = false,
                                showSize = showFolderSize,
                                showDate = false,
                                showTotalVideos = showTotalVideosChip,
                                showTotalDuration = showTotalDurationChip,
                                showThumbnail = showFolderThumbnails,
                                showNewBadge = showNewBadge,
                                unlimitedLines = unlimitedNameLines,
                                onClick = { treeSegments = dir.fullSegments },
                                onLongClick = {}
                            )
                        }
                    }

                    if (treeLevel.videos.isNotEmpty()) {
                        renderVideoCollectionItems(
                            videos = treeLevel.videos,
                            layoutMode = layoutMode,
                            gridColumns = activeVideoCols,
                            favoriteUris = favoriteUris,
                            selectedVideoIds = selectedVideoIds,
                            showResolutionBadge = showResolutionBadge,
                            showDateModified = showDateModified,
                            showSize = showFolderSize,
                            showDuration = showDurationField,
                            showExtension = showExtensionField,
                            showProgressBar = showProgressBar,
                            centerGridTitles = centerGridTitles,
                            unlimitedLines = unlimitedNameLines,
                            onVideoClick = { v -> handleVideoCardClick(v, treeLevel.videos) },
                            onVideoLongClick = { v -> handleVideoCardLongClick(v) },
                            onVideoMoreClick = { v -> contextMenuVideo = v },
                            onToggleVideoWatched = onToggleVideoWatched
                        )
                    }
                } else {
                    if (sortedFolders.isNotEmpty()) {
                        renderFolderCollectionItems(
                            folders = sortedFolders,
                            layoutMode = layoutMode,
                            gridColumns = activeFolderCols,
                            pinnedFolderIds = pinnedFolderIds,
                            selectedFolderId = if (isTabletDualPane) activeDualFolder?.id else null,
                            showPath = showFolderPath,
                            showSize = showFolderSize,
                            showDate = showDateModified,
                            showTotalVideos = showTotalVideosChip,
                            showTotalDuration = showTotalDurationChip,
                            showThumbnail = showFolderThumbnails,
                            showNewBadge = showNewBadge,
                            unlimitedLines = unlimitedNameLines,
                            onFolderClick = { folder ->
                                if (isTabletDualPane) tabletSelectedFolder = folder else selectedFolder = folder
                            },
                            onFolderLongClick = { folder -> contextMenuFolder = folder },
                            onToggleFolderWatched = onToggleFolderWatched
                        )
                    }

                    if (!isTabletDualPane && !onlyShowFoldersInHome && sortedVideos.isNotEmpty()) {
                        item(key = "all_videos_header") {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 10.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "All Videos",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onBackground
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = MaterialTheme.colorScheme.surfaceVariant
                                    ) {
                                        Text(
                                            text = "${sortedVideos.size}",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                TextButton(
                                    onClick = onRefreshClick,
                                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                ) {
                                    Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(15.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Scan", style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }

                        renderVideoCollectionItems(
                            videos = sortedVideos,
                            layoutMode = layoutMode,
                            gridColumns = activeVideoCols,
                            favoriteUris = favoriteUris,
                            selectedVideoIds = selectedVideoIds,
                            showResolutionBadge = showResolutionBadge,
                            showDateModified = showDateModified,
                            showSize = showFolderSize,
                            showDuration = showDurationField,
                            showExtension = showExtensionField,
                            showProgressBar = showProgressBar,
                            centerGridTitles = centerGridTitles,
                            unlimitedLines = unlimitedNameLines,
                            onVideoClick = { v -> handleVideoCardClick(v, sortedVideos) },
                            onVideoLongClick = { v -> handleVideoCardLongClick(v) },
                            onVideoMoreClick = { v -> contextMenuVideo = v },
                            onToggleVideoWatched = onToggleVideoWatched
                        )
                    }
                }

                if (sortedVideos.isEmpty() && sortedFolders.isEmpty()) {
                    item(key = "empty_videos_card") {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(28.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Movie,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(44.dp)
                                )
                                Text(
                                    text = when {
                                        activeQuickFilter == "FAVORITES" -> "No Favorite Videos Yet"
                                        searchQuery.isNotBlank() -> "No Matches Found"
                                        else -> "No Local Videos Found"
                                    },
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = when {
                                        activeQuickFilter == "FAVORITES" -> "Tap the menu on any video to mark it as a favorite."
                                        searchQuery.isNotBlank() -> "Try a different keyword or paste an http(s):// stream URL."
                                        else -> "Grant media storage permission or tap Scan to index device videos."
                                    },
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                                    Button(onClick = onRefreshClick) {
                                        Text("Rescan Library")
                                    }
                                    if (activeQuickFilter != "NONE") {
                                        TextButton(onClick = { activeQuickFilter = "NONE" }) {
                                            Text("Show All")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "home_bottom_spacer") {
                    Spacer(modifier = Modifier.height(76.dp))
                }
            }

            if (isTabletDualPane && activeDualFolder != null) {
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
                Box(modifier = Modifier.weight(0.58f).fillMaxHeight()) {
                    FolderVideosDetailView(
                        folder = activeDualFolder,
                        allVideos = allVideos,
                        layoutMode = layoutMode,
                        gridColumns = activeVideoCols,
                        sortOption = sortOption,
                        sortAscending = sortAscending,
                        showResolutionBadge = showResolutionBadge,
                        showDateModified = showDateModified,
                        showSize = showFolderSize,
                        showDuration = showDurationField,
                        showExtension = showExtensionField,
                        showProgressBar = showProgressBar,
                        centerGridTitles = centerGridTitles,
                        unlimitedLines = unlimitedNameLines,
                        favoriteUris = favoriteUris,
                        selectedVideoIds = selectedVideoIds,
                        showHeaderBack = false,
                        onBackClick = {},
                        onOpenSortSheet = { showSortSheet = true },
                        onVideoClick = { clicked, folderQueue -> handleVideoCardClick(clicked, folderQueue) },
                        onVideoLongClick = { handleVideoCardLongClick(it) },
                        onToggleVideoWatched = onToggleVideoWatched
                    )
                }
            }
            }

            if (inSelectionMode) {
                val chosen = sortedVideos.filter { it.id in selectedVideoIds }
                    .ifEmpty { allVideos.filter { it.id in selectedVideoIds } }
                FloatingBottomActionBar(
                    selectedCount = chosen.size,
                    onPlaySelected = {
                        chosen.firstOrNull()?.let { first -> onVideoQueuePlay(first, chosen) }
                        selectedVideoIds = emptySet()
                    },
                    onAddToPlaylist = { addToPlaylistVideos = chosen },
                    onFavoriteSelected = {
                        chosen.forEach { AppSettings.toggleFavoriteVideo(it.id) }
                        selectedVideoIds = emptySet()
                    },
                    onVaultSelected = {
                        chosen.forEach { AppSettings.addVaultVideo(it.id) }
                        selectedVideoIds = emptySet()
                        onRefreshClick()
                    },
                    onShareSelected = { shareVideoUris(context, chosen); selectedVideoIds = emptySet() },
                    onDeleteSelected = { deleteConfirmVideos = chosen },
                    modifier = Modifier.align(Alignment.BottomCenter)
                )
            } else if (showQuickPlayFab) {
                ExpandableQuickPlayFab(
                    expanded = isFabMenuExpanded,
                    directMode = quickPlayFabDirect,
                    visible = isFabVisible || isFabMenuExpanded,
                    onToggleExpand = { isFabMenuExpanded = !isFabMenuExpanded },
                    onDirectPlay = {
                        val target = continueWatchingVideos.firstOrNull() ?: sortedVideos.firstOrNull()
                        if (target != null) {
                            onVideoQueuePlay(target, sortedVideos)
                        } else {
                            openFilePickerLauncher.launch(arrayOf("video/*"))
                        }
                    },
                    onOpenFile = { openFilePickerLauncher.launch(arrayOf("video/*")) },
                    onOpenRecents = { onNavigateTab("RECENTS") },
                    onPlayLink = { showStreamDialog = true },
                    onOpenNetwork = { onNavigateTab("NETWORK") },
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(end = 16.dp, bottom = 14.dp)
                )
            }
        }
    }

    // Sort & View Options Sheet
    if (showSortSheet) {
        SortViewOptionsSheet(
            sortOption = sortOption,
            sortAscending = sortAscending,
            viewMode = folderViewMode,
            layoutMode = layoutMode,
            showFolderSize = showFolderSize,
            showFolderPath = showFolderPath,
            showDate = showDateModified,
            showResolutionBadge = showResolutionBadge,
            onSortOptionChange = { AppSettings.setSortOption(it) },
            onSortAscendingChange = { AppSettings.setSortAscending(it) },
            onViewModeChange = { AppSettings.setFolderViewMode(it) },
            onLayoutModeChange = { AppSettings.setViewMode(it) },
            onShowFolderSizeChange = { AppSettings.setShowFolderSize(it) },
            onShowFolderPathChange = { AppSettings.setShowFolderPath(it) },
            onShowDateChange = { AppSettings.setShowDateModified(it) },
            onShowResolutionBadgeChange = { AppSettings.setShowResolutionBadge(it) },
            onDismiss = { showSortSheet = false }
        )
    }

    // Direct URL Stream Dialog (Real URL launcher + bookmark saving)
    if (showStreamDialog) {
        var urlText by remember { mutableStateOf("") }
        var titleText by remember { mutableStateOf("") }
        var urlError by remember { mutableStateOf<String?>(null) }

        AlertDialog(
            onDismissRequest = { showStreamDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Rounded.PlayCircleOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Open Network Stream", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Play HTTP, HTTPS, HLS (.m3u8), DASH (.mpd), or RTSP streams directly in MPVX Player:",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = {
                            urlText = it
                            urlError = null
                        },
                        label = { Text("Stream URL") },
                        placeholder = { Text("https://example.com/stream.m3u8") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = titleText,
                        onValueChange = { titleText = it },
                        label = { Text("Optional Bookmark Title") },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (urlError != null) {
                        Text(
                            text = urlError!!,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(
                        onClick = {
                            val clean = urlText.trim()
                            val parsed = runCatching { Uri.parse(clean) }.getOrNull()
                            val scheme = parsed?.scheme?.lowercase().orEmpty()
                            if (scheme !in listOf("http", "https", "rtsp", "rtmp")) {
                                urlError = "Enter a valid http://, https://, or rtsp:// URL"
                            } else {
                                AppSettings.addSavedStream(titleText, clean)
                                showStreamDialog = false
                                onPlayNetworkStreamClick(clean)
                            }
                        }
                    ) {
                        Text("Save & Play")
                    }
                    Button(
                        onClick = {
                            val clean = urlText.trim()
                            val parsed = runCatching { Uri.parse(clean) }.getOrNull()
                            val scheme = parsed?.scheme?.lowercase().orEmpty()
                            if (scheme !in listOf("http", "https", "rtsp", "rtmp")) {
                                urlError = "Enter a valid http://, https://, or rtsp:// URL"
                            } else {
                                showStreamDialog = false
                                onPlayNetworkStreamClick(clean)
                            }
                        },
                        enabled = urlText.isNotBlank()
                    ) {
                        Text("Play")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showStreamDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Long-Press Video Context Bottom Sheet
    contextMenuVideo?.let { video ->
        val isFav = video.id in favoriteUris
        ModalBottomSheet(
            onDismissRequest = { contextMenuVideo = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = video.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = listOfNotNull(
                        video.durationText.takeIf { it.isNotBlank() },
                        video.sizeText.takeIf { it.isNotBlank() },
                        video.resolutionLabel.takeIf { it.isNotBlank() }
                    ).joinToString(" • "),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

                ContextActionRow(
                    icon = Icons.Rounded.PlayArrow,
                    label = if (video.progress > 0f) "Resume Playback" else "Play Video",
                    onClick = {
                        contextMenuVideo = null
                        onVideoQueuePlay(video, allVideos)
                    }
                )
                ContextActionRow(
                    icon = if (isFav) Icons.Rounded.Favorite else Icons.Rounded.FavoriteBorder,
                    label = if (isFav) "Remove from Favorites" else "Add to Favorites",
                    onClick = {
                        AppSettings.toggleFavoriteVideo(video.id)
                        contextMenuVideo = null
                    }
                )
                ContextActionRow(
                    icon = Icons.AutoMirrored.Rounded.PlaylistAdd,
                    label = "Add to Playlist",
                    onClick = {
                        addToPlaylistVideos = listOf(video)
                        contextMenuVideo = null
                    }
                )
                ContextActionRow(
                    icon = Icons.Rounded.CheckCircle,
                    label = if (video.isWatched) "Mark Unwatched" else "Mark Watched",
                    onClick = {
                        onToggleVideoWatched(video)
                        contextMenuVideo = null
                    }
                )
                ContextActionRow(
                    icon = Icons.Rounded.GridView,
                    label = "Select Multiple",
                    onClick = {
                        selectedVideoIds = selectedVideoIds + video.id
                        contextMenuVideo = null
                    }
                )
                ContextActionRow(
                    icon = Icons.Rounded.Lock,
                    label = "Move to Privacy Vault",
                    onClick = {
                        AppSettings.addVaultVideo(video.id)
                        contextMenuVideo = null
                        onRefreshClick()
                    }
                )
                ContextActionRow(
                    icon = Icons.Rounded.Info,
                    label = "Media File Info",
                    onClick = {
                        infoDialogVideo = video
                        contextMenuVideo = null
                    }
                )
                ContextActionRow(
                    icon = Icons.Rounded.DeleteOutline,
                    label = "Delete from Device",
                    tint = MaterialTheme.colorScheme.error,
                    onClick = {
                        deleteConfirmVideos = listOf(video)
                        contextMenuVideo = null
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Long-Press Folder Context Sheet
    contextMenuFolder?.let { folder ->
        val isPinned = folder.id in pinnedFolderIds
        ModalBottomSheet(
            onDismissRequest = { contextMenuFolder = null },
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${folder.videoCount} Videos • ${folder.sizeText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                ContextActionRow(
                    icon = Icons.Rounded.PushPin,
                    label = if (isPinned) "Unpin Folder" else "Pin Folder to Top",
                    onClick = {
                        AppSettings.togglePinnedFolder(folder.id)
                        contextMenuFolder = null
                    }
                )
                ContextActionRow(
                    icon = Icons.Rounded.CheckCircle,
                    label = "Toggle Folder Watched",
                    onClick = {
                        onToggleFolderWatched(folder.id)
                        contextMenuFolder = null
                    }
                )
                ContextActionRow(
                    icon = Icons.Rounded.VisibilityOff,
                    label = "Exclude Folder from Library",
                    onClick = {
                        AppSettings.addExcludedFolder(folder.id)
                        contextMenuFolder = null
                        onRefreshClick()
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // Batch / Single Add to Playlist Dialog
    if (addToPlaylistVideos.isNotEmpty()) {
        val targetVideos = addToPlaylistVideos
        var newPlaylistName by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { addToPlaylistVideos = emptyList() },
            title = {
                Text(
                    text = if (targetVideos.size == 1) "Add to Playlist" else "Add ${targetVideos.size} Videos to Playlist",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (userPlaylists.isNotEmpty()) {
                        userPlaylists.forEach { pl ->
                            val allPresent = targetVideos.all { it.id in pl.itemUris }
                            Surface(
                                onClick = {
                                    targetVideos.forEach { v ->
                                        if (allPresent) AppSettings.removeVideoFromPlaylist(pl.id, v.id)
                                        else AppSettings.addVideoToPlaylist(pl.id, v.id)
                                    }
                                    addToPlaylistVideos = emptyList()
                                    selectedVideoIds = emptySet()
                                },
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerLow,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(pl.name, modifier = Modifier.weight(1f), fontWeight = FontWeight.SemiBold)
                                    if (allPresent) {
                                        Icon(Icons.Rounded.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                    OutlinedTextField(
                        value = newPlaylistName,
                        onValueChange = { newPlaylistName = it },
                        label = { Text("New Playlist Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPlaylistName.isNotBlank()) {
                            val created = AppSettings.createPlaylist(newPlaylistName, targetVideos.first().id)
                            targetVideos.drop(1).forEach { v ->
                                AppSettings.addVideoToPlaylist(created.id, v.id)
                            }
                            selectedVideoIds = emptySet()
                        }
                        addToPlaylistVideos = emptyList()
                    }
                ) {
                    Text(if (newPlaylistName.isNotBlank()) "Create & Add" else "Close")
                }
            },
            dismissButton = {
                TextButton(onClick = { addToPlaylistVideos = emptyList() }) { Text("Cancel") }
            }
        )
    }

    // File Info Dialog
    infoDialogVideo?.let { video ->
        val dateAddedStr = remember(video.dateAddedTimestamp) {
            if (video.dateAddedTimestamp > 0L) SimpleDateFormat("MMM d, yyyy HH:mm", Locale.US).format(Date(video.dateAddedTimestamp)) else "Unknown"
        }
        AlertDialog(
            onDismissRequest = { infoDialogVideo = null },
            title = { Text("Media File Info", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    InfoLine("Title", video.title)
                    InfoLine("Folder", video.relativePath.ifBlank { video.folderName }.ifBlank { "Internal Storage" })
                    InfoLine("Duration", video.durationText)
                    InfoLine("Size", video.sizeText)
                    if (video.resolutionLabel.isNotBlank()) InfoLine("Resolution", video.resolutionLabel)
                    if (video.dateModifiedText.isNotBlank()) InfoLine("Modified", video.dateModifiedText)
                    InfoLine("Date Added", dateAddedStr)
                    InfoLine("URI", video.uri.toString())
                }
            },
            confirmButton = {
                TextButton(onClick = { infoDialogVideo = null }) { Text("Close") }
            }
        )
    }

    // Confirmed Single / Batch Video Delete Dialog
    if (deleteConfirmVideos.isNotEmpty()) {
        val pendingDelete = deleteConfirmVideos
        AlertDialog(
            onDismissRequest = { deleteConfirmVideos = emptyList() },
            title = {
                Text(
                    if (pendingDelete.size == 1) "Delete Video?" else "Delete ${pendingDelete.size} Videos?",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    if (pendingDelete.size == 1) {
                        "Permanently remove \"${pendingDelete.first().title}\" from your device? This action cannot be undone."
                    } else {
                        "Permanently remove ${pendingDelete.size} selected videos from your device? This action cannot be undone."
                    }
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deleteConfirmVideos = emptyList()
                        onDeleteVideosBatch(pendingDelete)
                        selectedVideoIds = emptySet()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteConfirmVideos = emptyList() }) { Text("Cancel") }
            }
        )
    }
}

private fun shareVideoUris(context: android.content.Context, videos: List<MediaVideoItem>) {
    if (videos.isEmpty()) return
    val shareIntent = if (videos.size == 1) {
        Intent(Intent.ACTION_SEND).apply {
            type = "video/*"
            putExtra(Intent.EXTRA_STREAM, videos.first().uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    } else {
        Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "video/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, ArrayList(videos.map { it.uri }))
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }
    runCatching {
        context.startActivity(Intent.createChooser(shareIntent, "Share Video"))
    }
}

private fun formatFileSizeCompact(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = arrayOf("B", "KB", "MB", "GB", "TB")
    var v = bytes.toDouble()
    var idx = 0
    while (v >= 1024.0 && idx < units.lastIndex) {
        v /= 1024.0
        idx++
    }
    return String.format(Locale.US, if (idx >= 2) "%.1f %s" else "%.0f %s", v, units[idx])
}

private fun LazyListScope.renderVideoCollectionItems(
    videos: List<MediaVideoItem>,
    layoutMode: String,
    gridColumns: Int,
    favoriteUris: Set<String>,
    selectedVideoIds: Set<String>,
    showResolutionBadge: Boolean,
    showDateModified: Boolean,
    showSize: Boolean,
    showDuration: Boolean,
    showExtension: Boolean,
    showProgressBar: Boolean,
    centerGridTitles: Boolean,
    unlimitedLines: Boolean,
    onVideoClick: (MediaVideoItem) -> Unit,
    onVideoLongClick: (MediaVideoItem) -> Unit,
    onVideoMoreClick: (MediaVideoItem) -> Unit,
    onToggleVideoWatched: (MediaVideoItem) -> Unit
) {
    if (layoutMode == "GRID") {
        val cols = gridColumns.coerceIn(1, 6)
        val gridRows = videos.chunked(cols)
        items(
            items = gridRows,
            key = { row -> "vgrid_row_${row.joinToString("|") { it.id }}" }
        ) { rowVideos ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowVideos.forEach { video ->
                    VideoGridCard(
                        video = video,
                        isFavorite = video.id in favoriteUris,
                        isSelected = video.id in selectedVideoIds,
                        showResolutionBadge = showResolutionBadge,
                        showDate = showDateModified,
                        showSize = showSize,
                        showDuration = showDuration,
                        showExtension = showExtension,
                        showProgressBar = showProgressBar,
                        centerTitle = centerGridTitles,
                        unlimitedLines = unlimitedLines,
                        onClick = { onVideoClick(video) },
                        onLongClick = { onVideoLongClick(video) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat((cols - rowVideos.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    } else {
        items(
            items = videos,
            key = { "video_${it.id}" }
        ) { video ->
            SwipeToWatchWrapper(
                isWatched = video.isWatched,
                onToggleWatched = { onToggleVideoWatched(video) }
            ) {
                VideoListCard(
                    video = video,
                    isFavorite = video.id in favoriteUris,
                    isSelected = video.id in selectedVideoIds,
                    showResolutionBadge = showResolutionBadge,
                    showDate = showDateModified,
                    showSize = showSize,
                    showDuration = showDuration,
                    showExtension = showExtension,
                    showProgressBar = showProgressBar,
                    unlimitedLines = unlimitedLines,
                    onClick = { onVideoClick(video) },
                    onLongClick = { onVideoLongClick(video) },
                    onMoreClick = { onVideoMoreClick(video) }
                )
            }
        }
    }
}

private fun LazyListScope.renderFolderCollectionItems(
    folders: List<MediaFolderItem>,
    layoutMode: String,
    gridColumns: Int,
    pinnedFolderIds: Set<String>,
    selectedFolderId: String?,
    showPath: Boolean,
    showSize: Boolean,
    showDate: Boolean,
    showTotalVideos: Boolean,
    showTotalDuration: Boolean,
    showThumbnail: Boolean,
    showNewBadge: Boolean,
    unlimitedLines: Boolean,
    onFolderClick: (MediaFolderItem) -> Unit,
    onFolderLongClick: (MediaFolderItem) -> Unit,
    onToggleFolderWatched: (String) -> Unit
) {
    if (layoutMode == "GRID") {
        val cols = gridColumns.coerceIn(1, 6)
        val gridRows = folders.chunked(cols)
        items(
            items = gridRows,
            key = { row -> "fgrid_row_${row.joinToString("|") { it.id }}" }
        ) { rowFolders ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                rowFolders.forEach { folder ->
                    FolderCardItem(
                        folder = folder,
                        isPinned = folder.id in pinnedFolderIds,
                        showPath = false,
                        showSize = showSize,
                        showDate = showDate,
                        showTotalVideos = showTotalVideos,
                        showTotalDuration = showTotalDuration,
                        showThumbnail = showThumbnail,
                        showNewBadge = showNewBadge,
                        unlimitedLines = unlimitedLines,
                        selectedHighlight = selectedFolderId == folder.id,
                        onClick = { onFolderClick(folder) },
                        onLongClick = { onFolderLongClick(folder) },
                        modifier = Modifier.weight(1f)
                    )
                }
                repeat((cols - rowFolders.size).coerceAtLeast(0)) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    } else {
        items(
            items = folders,
            key = { "folder_${it.id}" }
        ) { folder ->
            SwipeToWatchWrapper(
                isWatched = folder.newCount == 0,
                onToggleWatched = { onToggleFolderWatched(folder.id) }
            ) {
                FolderCardItem(
                    folder = folder,
                    isPinned = folder.id in pinnedFolderIds,
                    showPath = showPath,
                    showSize = showSize,
                    showDate = showDate,
                    showTotalVideos = showTotalVideos,
                    showTotalDuration = showTotalDuration,
                    showThumbnail = showThumbnail,
                    showNewBadge = showNewBadge,
                    unlimitedLines = unlimitedNameLinesSafe(unlimitedLines),
                    selectedHighlight = selectedFolderId == folder.id,
                    onClick = { onFolderClick(folder) },
                    onLongClick = { onFolderLongClick(folder) }
                )
            }
        }
    }
}

private fun unlimitedNameLinesSafe(unlimited: Boolean): Boolean = unlimited

@Composable
private fun ContextActionRow(
    icon: ImageVector,
    label: String,
    tint: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color.Transparent,
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(22.dp))
            Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium, color = tint)
        }
    }
}

@Composable
private fun InfoLine(label: String, value: String) {
    Column {
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        Text(value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun QuickStripChip(
    icon: ImageVector,
    label: String,
    isSelected: Boolean = false,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun FolderCardItem(
    folder: MediaFolderItem,
    isPinned: Boolean,
    showPath: Boolean,
    showSize: Boolean,
    showDate: Boolean,
    showTotalVideos: Boolean = true,
    showTotalDuration: Boolean = false,
    showThumbnail: Boolean = false,
    showNewBadge: Boolean = true,
    unlimitedLines: Boolean = false,
    selectedHighlight: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selectedHighlight) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (selectedHighlight) {
            BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.55f))
        } else {
            null
        }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                val folderThumbReq = if (showThumbnail) {
                    buildVideoThumbnailRequest(LocalContext.current, folder.latestVideoThumbnailUri, folder.totalDurationMs)
                } else null
                if (folderThumbReq != null) {
                    AsyncImage(
                        model = folderThumbReq,
                        contentDescription = folder.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Folder,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(30.dp)
                    )
                }

                if (showTotalVideos) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                    ) {
                        Text(
                            text = "${folder.videoCount}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 9.sp,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    if (isPinned) {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = "Pinned",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = folder.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (unlimitedLines) Int.MAX_VALUE else 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (showNewBadge && folder.newCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = "${folder.newCount} NEW",
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                }

                if (showPath && folder.path.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = folder.path,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (unlimitedLines) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showTotalVideos) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = if (folder.videoCount == 1) "1 Video" else "${folder.videoCount} Videos",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (showTotalDuration && folder.totalDurationMs > 0L) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = formatCompactDuration(folder.totalDurationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (showSize && folder.sizeText.isNotBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = folder.sizeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }
                    if (showDate && folder.dateModifiedText.isNotBlank()) {
                        Text(
                            text = folder.dateModifiedText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Rounded.ArrowForwardIos,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f),
                modifier = Modifier.size(15.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoListCard(
    video: MediaVideoItem,
    isFavorite: Boolean,
    isSelected: Boolean = false,
    showResolutionBadge: Boolean,
    showDate: Boolean,
    showSize: Boolean,
    showDuration: Boolean = true,
    showExtension: Boolean = false,
    showProgressBar: Boolean = true,
    unlimitedLines: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayTitle = MediaTreeResolver.formatVideoTitle(video.title, video.fileNameWithExt, showExtension)
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .width(116.dp)
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                val thumbRequest = buildVideoThumbnailRequest(LocalContext.current, video.thumbnailUri, video.durationMs)
                if (thumbRequest != null) {
                    AsyncImage(
                        model = thumbRequest,
                        contentDescription = displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
                    )
                }

                if (showResolutionBadge && video.resolutionLabel.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Color.Black.copy(alpha = 0.72f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = video.resolutionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF70ECE4),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(20.dp)
                    )
                } else if (video.isWatched) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Watched",
                        tint = Color(0xFF22C55E),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(4.dp)
                            .size(16.dp)
                    )
                }

                if (showDuration && video.durationText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(5.dp),
                        color = Color.Black.copy(alpha = 0.76f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(4.dp)
                    ) {
                        Text(
                            text = video.durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (showProgressBar && video.progress > 0.01f) {
                    LinearProgressIndicator(
                        progress = { video.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isFavorite) {
                        Icon(
                            imageVector = Icons.Rounded.Favorite,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = displayTitle,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = if (unlimitedLines) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showSize && video.sizeText.isNotBlank()) {
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceContainerHigh
                        ) {
                            Text(
                                text = video.sizeText,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (showDate && video.dateModifiedText.isNotBlank()) {
                        Text(
                            text = video.dateModifiedText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            IconButton(onClick = onMoreClick, modifier = Modifier.size(32.dp)) {
                Icon(
                    imageVector = Icons.Rounded.MoreVert,
                    contentDescription = "More Options",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun VideoGridCard(
    video: MediaVideoItem,
    isFavorite: Boolean,
    isSelected: Boolean = false,
    showResolutionBadge: Boolean,
    showDate: Boolean,
    showSize: Boolean,
    showDuration: Boolean = true,
    showExtension: Boolean = false,
    showProgressBar: Boolean = true,
    centerTitle: Boolean = true,
    unlimitedLines: Boolean = false,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val displayTitle = MediaTreeResolver.formatVideoTitle(video.title, video.fileNameWithExt, showExtension)
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(onClick = onClick, onLongClick = onLongClick),
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = if (isSelected) BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary) else null
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
                    .background(Color(0xFF1E2430)),
                contentAlignment = Alignment.Center
            ) {
                val thumbRequest = buildVideoThumbnailRequest(LocalContext.current, video.thumbnailUri, video.durationMs)
                if (thumbRequest != null) {
                    AsyncImage(
                        model = thumbRequest,
                        contentDescription = displayTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = Icons.Rounded.Movie,
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.4f),
                        modifier = Modifier.size(28.dp)
                    )
                }

                if (showResolutionBadge && video.resolutionLabel.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.75f),
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = video.resolutionLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color(0xFF70ECE4),
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (isSelected) {
                    Icon(
                        imageVector = Icons.Rounded.CheckCircle,
                        contentDescription = "Selected",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(20.dp)
                    )
                } else if (isFavorite) {
                    Icon(
                        imageVector = Icons.Rounded.Favorite,
                        contentDescription = "Favorite",
                        tint = Color(0xFFF43F5E),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                            .size(16.dp)
                    )
                }

                if (showDuration && video.durationText.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color.Black.copy(alpha = 0.8f),
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            text = video.durationText,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontSize = 8.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                        )
                    }
                }

                if (showProgressBar && video.progress > 0.01f) {
                    LinearProgressIndicator(
                        progress = { video.progress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(3.dp)
                            .align(Alignment.BottomCenter),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Column(
                modifier = Modifier.padding(10.dp),
                horizontalAlignment = if (centerTitle) Alignment.CenterHorizontally else Alignment.Start
            ) {
                Text(
                    text = displayTitle,
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = if (unlimitedLines) Int.MAX_VALUE else 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = if (centerTitle) TextAlign.Center else TextAlign.Start
                )
                val metaParts = listOfNotNull(
                    video.sizeText.takeIf { showSize && it.isNotBlank() },
                    video.dateModifiedText.takeIf { showDate && it.isNotBlank() }
                )
                if (metaParts.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = metaParts.joinToString(" • "),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun FolderVideosDetailView(
    folder: MediaFolderItem,
    allVideos: List<MediaVideoItem>,
    layoutMode: String,
    gridColumns: Int,
    sortOption: String,
    sortAscending: Boolean,
    showResolutionBadge: Boolean,
    showDateModified: Boolean,
    showSize: Boolean,
    showDuration: Boolean,
    showExtension: Boolean,
    showProgressBar: Boolean,
    centerGridTitles: Boolean,
    unlimitedLines: Boolean,
    favoriteUris: Set<String>,
    selectedVideoIds: Set<String>,
    showHeaderBack: Boolean = true,
    onBackClick: () -> Unit,
    onOpenSortSheet: () -> Unit,
    onVideoClick: (MediaVideoItem, List<MediaVideoItem>) -> Unit,
    onVideoLongClick: (MediaVideoItem) -> Unit,
    onToggleVideoWatched: (MediaVideoItem) -> Unit
) {
    val folderVideos = remember(folder, allVideos, sortOption, sortAscending) {
        val strictMatches = allVideos.filter { it.folderId == folder.id }
        sortVideoList(strictMatches, sortOption, sortAscending)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (showHeaderBack) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = "Back to Folders",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(modifier = Modifier.width(6.dp))
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folder.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${folderVideos.size} Videos • ${folder.sizeText}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onOpenSortSheet) {
                Icon(
                    imageVector = Icons.AutoMirrored.Rounded.Sort,
                    contentDescription = "Sort & View",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))

        if (folderVideos.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No videos in this directory",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else if (layoutMode == "GRID") {
            LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumns.coerceIn(1, 6)),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = folderVideos,
                    key = { it.id }
                ) { video ->
                    VideoGridCard(
                        video = video,
                        isFavorite = video.id in favoriteUris,
                        isSelected = video.id in selectedVideoIds,
                        showResolutionBadge = showResolutionBadge,
                        showDate = showDateModified,
                        showSize = showSize,
                        showDuration = showDuration,
                        showExtension = showExtension,
                        showProgressBar = showProgressBar,
                        centerTitle = centerGridTitles,
                        unlimitedLines = unlimitedLines,
                        onClick = { onVideoClick(video, folderVideos) },
                        onLongClick = { onVideoLongClick(video) }
                    )
                }
            }
        } else {
            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(
                    items = folderVideos,
                    key = { it.id }
                ) { video ->
                    SwipeToWatchWrapper(
                        isWatched = video.isWatched,
                        onToggleWatched = { onToggleVideoWatched(video) }
                    ) {
                        VideoListCard(
                            video = video,
                            isFavorite = video.id in favoriteUris,
                            isSelected = video.id in selectedVideoIds,
                            showResolutionBadge = showResolutionBadge,
                            showDate = showDateModified,
                            showSize = showSize,
                            showDuration = showDuration,
                            showExtension = showExtension,
                            showProgressBar = showProgressBar,
                            unlimitedLines = unlimitedLines,
                            onClick = { onVideoClick(video, folderVideos) },
                            onLongClick = { onVideoLongClick(video) },
                            onMoreClick = { onVideoLongClick(video) }
                        )
                    }
                }
            }
        }
    }
}
