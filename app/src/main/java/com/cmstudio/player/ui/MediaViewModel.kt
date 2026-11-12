package com.cmstudio.player.ui

import android.app.Application
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.documentfile.provider.DocumentFile
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cmstudio.player.PlaybackHistoryBridge
import com.cmstudio.player.PlaybackHistoryEntry
import com.cmstudio.player.home.Library
import com.cmstudio.player.ui.model.MediaFolderItem
import com.cmstudio.player.ui.model.MediaVideoItem
import com.cmstudio.player.ui.model.WatchStreakGoal
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.log10
import kotlin.math.pow

class MediaViewModel(application: Application) : AndroidViewModel(application) {

    private data class ExtraVideoMeta(
        val dateAddedSec: Long,
        val width: Int,
        val height: Int,
        val relativePath: String = ""
    )

    private val _continueWatching = MutableStateFlow<List<MediaVideoItem>>(emptyList())
    val continueWatching: StateFlow<List<MediaVideoItem>> = _continueWatching.asStateFlow()

    private val _recentlyAdded = MutableStateFlow<List<MediaVideoItem>>(emptyList())
    val recentlyAdded: StateFlow<List<MediaVideoItem>> = _recentlyAdded.asStateFlow()

    private val _folders = MutableStateFlow<List<MediaFolderItem>>(emptyList())
    val folders: StateFlow<List<MediaFolderItem>> = _folders.asStateFlow()

    private val _allVideos = MutableStateFlow<List<MediaVideoItem>>(emptyList())
    val allVideos: StateFlow<List<MediaVideoItem>> = _allVideos.asStateFlow()

    private val _allScannedVideos = MutableStateFlow<List<MediaVideoItem>>(emptyList())
    val allScannedVideos: StateFlow<List<MediaVideoItem>> = _allScannedVideos.asStateFlow()

    private val _historyEntries = MutableStateFlow<List<PlaybackHistoryEntry>>(emptyList())
    val historyEntries: StateFlow<List<PlaybackHistoryEntry>> = _historyEntries.asStateFlow()

    private val _watchStreakGoal = MutableStateFlow(WatchStreakGoal(currentStreakDays = 0, dailyGoalMinutes = 45, watchedTodayMinutes = 0))
    val watchStreakGoal: StateFlow<WatchStreakGoal> = _watchStreakGoal.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    init {
        loadMedia()
        viewModelScope.launch {
            PlaybackHistoryBridge.historyRevision.collect { rev ->
                if (rev > 0L) {
                    val freshHistory = withContext(Dispatchers.IO) {
                        PlaybackHistoryBridge.loadHistory(getApplication())
                    }
                    _historyEntries.value = freshHistory
                }
            }
        }
    }

    fun loadMedia() {
        viewModelScope.launch {
            _isLoading.value = true
            val app = getApplication<Application>()
            val excludedFolders = AppSettings.excludedFolders.value
            val showHiddenFolders = AppSettings.showHiddenMediaFolders.value
            val safTreeUris = AppSettings.safMediaTreeUris.value
            val vaultUris = AppSettings.vaultVideoUris.value

            val result = withContext(Dispatchers.IO) {
                val rawVideos = Library.videos(app)
                val extraMetaMap = queryExtraVideoMeta(app)
                val history = PlaybackHistoryBridge.loadHistory(app)
                val historyByUri = history.associateBy { it.uri.toString() }
                val historyByNormPath = history
                    .mapNotNull { entry ->
                        PlaybackHistoryBridge.normalizeStoragePath(entry.uri.toString())
                            .takeIf { it.isNotEmpty() }
                            ?.let { it to entry }
                    }
                    .toMap()
                val historyByFileName = history
                    .filter { !it.isNetwork }
                    .mapNotNull { entry ->
                        PlaybackHistoryBridge.extractFileName(entry.uri.toString())
                            .takeIf { it.isNotEmpty() }
                            ?.let { it to entry }
                    }
                    .toMap()

                val allItems = rawVideos.map { v ->
                    val mediaId = v.uri.lastPathSegment?.toLongOrNull()
                    val extra = if (mediaId != null) extraMetaMap[mediaId] else null
                    val uriStr = v.uri.toString()
                    val nameKey = PlaybackHistoryBridge.extractFileName(v.name)
                    val matchedHistory = historyByUri[uriStr]
                        ?: historyByNormPath[PlaybackHistoryBridge.normalizeStoragePath(uriStr)]
                        ?: nameKey.takeIf { it.isNotEmpty() }?.let { historyByFileName[it] }

                    val savedPos = matchedHistory?.positionMs?.takeIf { it > 0L }
                        ?: PlaybackHistoryBridge.savedPositionFor(app, v.uri, v.name)
                    val savedDur = (matchedHistory?.durationMs ?: 0L).takeIf { it > 0L }
                        ?: PlaybackHistoryBridge.savedDurationFor(app, v.uri, v.name).takeIf { it > 0L }
                        ?: v.duration
                    val realProgress = if (savedDur > 0L && savedPos > 0L) {
                        (savedPos.toFloat() / savedDur.toFloat()).coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val remainingMs = if (savedPos > 0L && savedDur > savedPos) savedDur - savedPos else 0L
                    val modifiedMs = normalizeEpochMs(v.modified)
                    val dateAddedMs = normalizeEpochMs(extra?.dateAddedSec ?: v.modified)
                    val resolutionLabel = formatResolution(extra?.width ?: 0, extra?.height ?: 0)
                    val relFolderPath = extra?.relativePath?.trim()?.trimEnd('/')
                        ?.takeIf { it.isNotEmpty() }
                        ?: (v.folderName ?: "")

                    MediaVideoItem(
                        id = v.uri.toString(),
                        title = v.name ?: "Video",
                        uri = v.uri,
                        folderId = v.folderId ?: "",
                        durationText = formatDuration(v.duration),
                        remainingText = if (remainingMs > 0L) "${formatDuration(remainingMs)} left" else "",
                        progress = realProgress,
                        sizeText = formatFileSize(v.size),
                        dateModifiedText = formatDate(modifiedMs),
                        sourceTag = if (v.folderName.isNullOrBlank()) "Local File" else v.folderName,
                        thumbnailUri = v.uri,
                        durationMs = v.duration,
                        sizeBytes = v.size,
                        modifiedTimestamp = modifiedMs,
                        dateAddedTimestamp = dateAddedMs,
                        folderName = v.folderName ?: "",
                        resolutionLabel = resolutionLabel,
                        isWatched = matchedHistory?.played ?: false,
                        relativePath = relFolderPath,
                        fileNameWithExt = v.name ?: "Video"
                    )
                }

                val audioExtras = if (AppSettings.includeAudioFilesInVideoLib.value) {
                    runCatching {
                        com.cmstudio.player.ui.music.AudioLibrary.scanAudio(app).map { track ->
                            MediaVideoItem(
                                id = track.uri.toString(),
                                title = track.title,
                                uri = track.uri,
                                folderId = track.folderName.ifBlank { "audio_lib" },
                                durationText = track.durationText,
                                sizeText = track.sizeText,
                                sourceTag = "Audio • ${track.artist}",
                                thumbnailUri = track.artworkUri,
                                durationMs = track.durationMs,
                                sizeBytes = track.sizeBytes,
                                folderName = track.folderName.ifBlank { "Audio" },
                                resolutionLabel = "AUDIO",
                                relativePath = track.folderName.ifBlank { "Audio" },
                                fileNameWithExt = track.title
                            )
                        }
                    }.getOrDefault(emptyList())
                } else emptyList()

                // Filter out Vault-hidden items, user-excluded folders, and dot-hidden folders unless enabled
                val existingIds = (allItems + audioExtras).map { it.id }.toHashSet()
                val safExtras = scanSafMediaItems(
                    app = app,
                    treeUris = safTreeUris,
                    includeHidden = showHiddenFolders,
                    historyByUri = historyByUri,
                    historyByFileName = historyByFileName
                ).filterNot { it.id in existingIds }

                val visibleItems = (allItems + audioExtras + safExtras).filter { item ->
                    val inVault = item.id in vaultUris
                    val inExcluded = excludedFolders.any { ex ->
                        item.folderId.equals(ex, ignoreCase = true) ||
                            item.folderName.equals(ex, ignoreCase = true) ||
                            item.relativePath.startsWith(ex, ignoreCase = true)
                    }
                    val hiddenDotFolder = !showHiddenFolders && isDotHiddenItem(item)
                    !inVault && !inExcluded && !hiddenDotFolder
                }

                val recent = visibleItems.sortedByDescending { it.dateAddedTimestamp }

                val visibleMap = visibleItems.associateBy { it.id }
                val visibleByFileName = visibleItems
                    .mapNotNull { item ->
                        PlaybackHistoryBridge.extractFileName(item.fileNameWithExt)
                            .takeIf { it.isNotEmpty() }
                            ?.let { it to item }
                    }
                    .toMap()

                val continueList = history
                    .filter { entry ->
                        if (entry.isAudio) return@filter false
                        val key = entry.uri.toString()
                        val matchedLocal = visibleMap[key]
                            ?: PlaybackHistoryBridge.extractFileName(key)
                                .takeIf { it.isNotEmpty() }
                                ?.let { visibleByFileName[it] }
                        (matchedLocal != null || entry.isNetwork) && entry.positionMs > 0L && !entry.played
                    }
                    .mapNotNull { entry ->
                        val key = entry.uri.toString()
                        val local = visibleMap[key]
                            ?: PlaybackHistoryBridge.extractFileName(key)
                                .takeIf { it.isNotEmpty() }
                                ?.let { visibleByFileName[it] }
                        local ?: if (entry.durationMs > 0L || entry.positionMs > 0L) {
                            MediaVideoItem(
                                id = entry.uri.toString(),
                                title = entry.title,
                                uri = entry.uri,
                                durationText = if (entry.durationMs > 0L) formatDuration(entry.durationMs) else formatDuration(entry.positionMs),
                                remainingText = if (entry.durationMs > entry.positionMs) "${formatDuration(entry.durationMs - entry.positionMs)} left" else "",
                                progress = entry.progress,
                                sizeText = if (entry.isNetwork) "Network Stream" else "Local File",
                                dateModifiedText = formatDate(entry.lastPlayedAtMs),
                                sourceTag = if (entry.isNetwork) "Stream" else "Local File",
                                thumbnailUri = if (entry.isNetwork) null else entry.uri,
                                durationMs = entry.durationMs,
                                modifiedTimestamp = entry.lastPlayedAtMs,
                                dateAddedTimestamp = entry.lastPlayedAtMs,
                                isWatched = entry.played
                            )
                        } else null
                    }

                // Group visible items back into folders accurately (respecting hidden/excluded videos!)
                val groupedByFolder = visibleItems.groupBy { it.folderId }
                val rawFolderLookup = Library.folders(rawVideos).associateBy { it.id }
                val nowMs = System.currentTimeMillis()
                val badgeDays = AppSettings.newBadgeDays.value
                val recentWindowMs = if (badgeDays <= 0) Long.MAX_VALUE else badgeDays.toLong() * 24L * 3600L * 1000L

                val folderItems = groupedByFolder.entries.map { (folderId, videosInFolder) ->
                    val rawFolder = rawFolderLookup[folderId]
                    val latestVideo = videosInFolder.maxByOrNull { it.modifiedTimestamp }
                    val newestAdded = videosInFolder.maxOfOrNull { it.dateAddedTimestamp } ?: 0L
                    val totalSize = videosInFolder.sumOf { it.sizeBytes }
                    val totalDuration = videosInFolder.sumOf { it.durationMs }
                    val modifiedMs = latestVideo?.modifiedTimestamp ?: normalizeEpochMs(rawFolder?.modified ?: 0L)
                    val recentCount = videosInFolder.count {
                        !it.isWatched && (badgeDays <= 0 || (nowMs - it.dateAddedTimestamp in 0..recentWindowMs))
                    }

                    MediaFolderItem(
                        id = folderId,
                        name = rawFolder?.name ?: latestVideo?.folderName?.ifBlank { "Folder" } ?: "Folder",
                        path = latestVideo?.relativePath?.ifBlank { folderId } ?: folderId,
                        videoCount = videosInFolder.size,
                        sizeText = formatFileSize(totalSize),
                        dateModifiedText = formatDate(modifiedMs),
                        latestVideoThumbnailUri = latestVideo?.uri,
                        newCount = recentCount,
                        totalSizeBytes = totalSize,
                        modifiedTimestamp = modifiedMs,
                        dateAddedTimestamp = newestAdded,
                        totalDurationMs = totalDuration
                    )
                }

                // Compute honest local watch activity from history entries in the last 24h
                val dayAgoMs = nowMs - 24L * 3600L * 1000L
                val watchedTodayMs = history
                    .filter { it.lastPlayedAtMs >= dayAgoMs }
                    .sumOf { entry ->
                        if (entry.played && entry.durationMs > 0L) entry.durationMs else entry.positionMs
                    }
                val watchedTodayMin = (watchedTodayMs / 60_000L).toInt().coerceAtLeast(0)
                val distinctDays = history
                    .filter { it.lastPlayedAtMs > 0L }
                    .map { it.lastPlayedAtMs / (24L * 3600L * 1000L) }
                    .distinct()
                    .size

                LoadedLibraryState(
                    allScanned = allItems,
                    visibleVideos = visibleItems,
                    recentlyAdded = recent,
                    continueWatching = continueList,
                    folders = folderItems,
                    history = history,
                    streakGoal = WatchStreakGoal(
                        currentStreakDays = distinctDays,
                        dailyGoalMinutes = 45,
                        watchedTodayMinutes = watchedTodayMin
                    )
                )
            }

            _allScannedVideos.value = result.allScanned
            _allVideos.value = result.visibleVideos
            _recentlyAdded.value = result.recentlyAdded
            _continueWatching.value = result.continueWatching
            _folders.value = result.folders
            _historyEntries.value = result.history
            _watchStreakGoal.value = result.streakGoal
            _isLoading.value = false
        }
    }

    fun removeHistoryEntry(uri: Uri) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PlaybackHistoryBridge.removeHistoryEntry(getApplication(), uri)
            }
            loadMedia()
        }
    }

    fun clearPlaybackHistory() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                PlaybackHistoryBridge.clearHistory(getApplication())
            }
            loadMedia()
        }
    }

    fun toggleVideoWatched(video: MediaVideoItem) {
        viewModelScope.launch {
            val targetWatched = !video.isWatched
            withContext(Dispatchers.IO) {
                PlaybackHistoryBridge.setWatchedState(
                    context = getApplication(),
                    uri = video.uri,
                    title = video.title,
                    durationMs = video.durationMs,
                    watched = targetWatched
                )
            }
            loadMedia()
        }
    }

    fun toggleFolderWatched(folderId: String) {
        viewModelScope.launch {
            val folderVideos = _allVideos.value.filter { it.folderId == folderId }
            if (folderVideos.isEmpty()) return@launch
            val allWatched = folderVideos.all { it.isWatched }
            withContext(Dispatchers.IO) {
                folderVideos.forEach { video ->
                    PlaybackHistoryBridge.setWatchedState(
                        context = getApplication(),
                        uri = video.uri,
                        title = video.title,
                        durationMs = video.durationMs,
                        watched = !allWatched
                    )
                }
            }
            loadMedia()
        }
    }

    private data class LoadedLibraryState(
        val allScanned: List<MediaVideoItem>,
        val visibleVideos: List<MediaVideoItem>,
        val recentlyAdded: List<MediaVideoItem>,
        val continueWatching: List<MediaVideoItem>,
        val folders: List<MediaFolderItem>,
        val history: List<PlaybackHistoryEntry>,
        val streakGoal: WatchStreakGoal
    )

    private fun queryExtraVideoMeta(app: Application): Map<Long, ExtraVideoMeta> {
        val map = HashMap<Long, ExtraVideoMeta>()
        val projection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.RELATIVE_PATH
            )
        } else {
            arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT
            )
        }
        val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
        } else {
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI
        }
        try {
            app.contentResolver.query(uri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndex(MediaStore.Video.Media._ID)
                val addedCol = cursor.getColumnIndex(MediaStore.Video.Media.DATE_ADDED)
                val widthCol = cursor.getColumnIndex(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndex(MediaStore.Video.Media.HEIGHT)
                val relPathCol = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    cursor.getColumnIndex(MediaStore.Video.Media.RELATIVE_PATH)
                } else {
                    -1
                }
                while (cursor.moveToNext()) {
                    if (idCol < 0) continue
                    val id = cursor.getLong(idCol)
                    val dateAdded = if (addedCol >= 0) cursor.getLong(addedCol) else 0L
                    val width = if (widthCol >= 0) cursor.getInt(widthCol) else 0
                    val height = if (heightCol >= 0) cursor.getInt(heightCol) else 0
                    val relPath = if (relPathCol >= 0) cursor.getString(relPathCol).orEmpty() else ""
                    map[id] = ExtraVideoMeta(dateAdded, width, height, relPath)
                }
            }
        } catch (_: Exception) {
        }
        return map
    }

    private fun normalizeEpochMs(raw: Long): Long {
        if (raw <= 0L) return 0L
        return if (raw < 10_000_000_000L) raw * 1000L else raw
    }

    private fun formatResolution(width: Int, height: Int): String {
        val minDim = minOf(width, height)
        val maxDim = maxOf(width, height)
        return when {
            maxDim >= 3840 || minDim >= 2160 -> "4K"
            maxDim >= 2560 || minDim >= 1440 -> "1440p"
            maxDim >= 1920 || minDim >= 1080 -> "1080p"
            maxDim >= 1280 || minDim >= 720 -> "720p"
            minDim >= 480 -> "480p"
            minDim > 0 -> "${minDim}p"
            else -> ""
        }
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB")
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, 4)
        return String.format(Locale.US, "%.1f %s", bytes / 1024.0.pow(digitGroups.toDouble()), units[digitGroups])
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = (ms / 1000L).coerceAtLeast(0L)
        val hours = totalSec / 3600L
        val min = (totalSec % 3600L) / 60L
        val sec = totalSec % 60L
        return if (hours > 0L) {
            String.format(Locale.US, "%d:%02d:%02d", hours, min, sec)
        } else {
            String.format(Locale.US, "%02d:%02d", min, sec)
        }
    }

    private fun formatDate(epochMs: Long): String {
        if (epochMs <= 0L) return ""
        return SimpleDateFormat("MMM d, yyyy", Locale.US).format(Date(epochMs))
    }

    private fun isDotHiddenItem(item: MediaVideoItem): Boolean {
        if (item.folderName.startsWith(".") || item.title.startsWith(".")) return true
        val relSegHidden = item.relativePath.split('/', '\\').any { it.startsWith(".") && it.length > 1 }
        if (relSegHidden) return true
        val folderSegHidden = item.folderId.split('/', '\\').any { it.startsWith(".") && it.length > 1 }
        return folderSegHidden
    }

    private fun scanSafMediaItems(
        app: Application,
        treeUris: Set<String>,
        includeHidden: Boolean,
        historyByUri: Map<String, PlaybackHistoryEntry>,
        historyByFileName: Map<String, PlaybackHistoryEntry>
    ): List<MediaVideoItem> {
        if (treeUris.isEmpty()) return emptyList()
        val videoExts = setOf("mp4", "mkv", "webm", "avi", "mov", "flv", "ts", "m4v", "3gp", "wmv", "mpg", "mpeg")
        val collected = mutableListOf<MediaVideoItem>()
        val maxTotal = 400

        fun walkDir(dir: DocumentFile, relPath: String, rootId: String, rootName: String, depth: Int) {
            if (depth > 5 || collected.size >= maxTotal) return
            val children = runCatching { dir.listFiles() }.getOrNull() ?: return
            for (child in children) {
                if (collected.size >= maxTotal) break
                val name = child.name ?: continue
                if (!includeHidden && name.startsWith(".")) continue
                if (child.isDirectory) {
                    walkDir(child, "$relPath/$name", rootId, rootName, depth + 1)
                } else if (child.isFile) {
                    val mime = child.type.orEmpty().lowercase(Locale.ROOT)
                    val ext = name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                    if (!mime.startsWith("video/") && ext !in videoExts) continue
                    val fileUri = child.uri
                    val uriStr = fileUri.toString()
                    val nameKey = PlaybackHistoryBridge.extractFileName(name)
                    val matched = historyByUri[uriStr]
                        ?: nameKey.takeIf { it.isNotEmpty() }?.let { historyByFileName[it] }
                    val durMs = matched?.durationMs ?: 0L
                    val posMs = matched?.positionMs ?: 0L
                    val prog = if (durMs > 0L && posMs > 0L) (posMs.toFloat() / durMs.toFloat()).coerceIn(0f, 1f) else 0f
                    val modMs = child.lastModified().takeIf { it > 0L } ?: System.currentTimeMillis()
                    val sizeBytes = child.length().coerceAtLeast(0L)
                    collected += MediaVideoItem(
                        id = uriStr,
                        title = name.substringBeforeLast('.', name),
                        uri = fileUri,
                        folderId = rootId,
                        durationText = if (durMs > 0L) formatDuration(durMs) else "--:--",
                        remainingText = if (durMs > posMs && posMs > 0L) "${formatDuration(durMs - posMs)} left" else "",
                        progress = prog,
                        sizeText = formatFileSize(sizeBytes),
                        dateModifiedText = formatDate(modMs),
                        sourceTag = "SAF • $rootName",
                        thumbnailUri = fileUri,
                        durationMs = durMs,
                        sizeBytes = sizeBytes,
                        modifiedTimestamp = modMs,
                        dateAddedTimestamp = modMs,
                        folderName = dir.name ?: rootName,
                        resolutionLabel = "",
                        isWatched = matched?.played ?: false,
                        relativePath = relPath,
                        fileNameWithExt = name
                    )
                }
            }
        }

        treeUris.forEach { rawUri ->
            val parsed = runCatching { Uri.parse(rawUri) }.getOrNull() ?: return@forEach
            val root = runCatching { DocumentFile.fromTreeUri(app, parsed) }.getOrNull() ?: return@forEach
            if (!root.exists() || !root.isDirectory) return@forEach
            val rootName = root.name ?: parsed.lastPathSegment ?: "SAF Folder"
            walkDir(root, "SAF/$rootName", "saf:$rawUri", rootName, 0)
        }
        return collected
    }
}
