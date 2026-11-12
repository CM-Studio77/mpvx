package com.cmstudio.player.library

import com.cmstudio.player.PlaybackHistoryEntry
import com.cmstudio.player.ui.model.MediaVideoItem
import java.io.File
import java.util.Locale

data class SmartMediaEntry(
    val id: String,
    val title: String,
    val progress: Float = 0f,
    val isWatched: Boolean = false,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val dateAddedTimestamp: Long = 0L,
    val resolutionLabel: String = "",
    val fileNameWithExt: String = title
)

data class SmartHistoryEntry(
    val uriString: String,
    val title: String,
    val mimeType: String = "video/mp4",
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val lastPlayedAtMs: Long = 0L,
    val played: Boolean = false
)

private fun MediaVideoItem.toSmartEntry() = SmartMediaEntry(
    id = id,
    title = title,
    progress = progress,
    isWatched = isWatched,
    durationMs = durationMs,
    sizeBytes = sizeBytes,
    dateAddedTimestamp = dateAddedTimestamp,
    resolutionLabel = resolutionLabel,
    fileNameWithExt = fileNameWithExt
)

private fun PlaybackHistoryEntry.toSmartHistory() = SmartHistoryEntry(
    uriString = uri.toString(),
    title = title,
    mimeType = mimeType,
    positionMs = positionMs,
    durationMs = durationMs,
    lastPlayedAtMs = lastPlayedAtMs,
    played = played
)

enum class SmartCollectionType(val id: String, val title: String, val subtitle: String) {
    UNWATCHED("UNWATCHED", "Unwatched", "Videos you haven't started or finished yet"),
    RECENTLY_ADDED("RECENTLY_ADDED", "Recently Added", "Newest additions to your local library"),
    RECENTLY_PLAYED("RECENTLY_PLAYED", "Recently Played", "Latest watched videos & streams"),
    PARTIALLY_WATCHED("PARTIALLY_WATCHED", "Partially Watched", "In-progress videos ready to resume"),
    COMPLETED("COMPLETED", "Completed", "Videos marked as watched"),
    LONG_VIDEOS("LONG_VIDEOS", "Long Videos", "20 minutes or longer"),
    SHORT_VIDEOS("SHORT_VIDEOS", "Short Videos", "Under 5 minutes")
}

enum class MediaQualityFilterType(val id: String, val label: String) {
    ALL("ALL", "All"),
    UHD_4K("UHD_4K", "4K UHD"),
    FHD_1080P("FHD_1080P", "1080p"),
    HD_720P("HD_720P", "720p"),
    MKV("MKV", "MKV"),
    MP4("MP4", "MP4")
}

data class DailyWatchStat(
    val dayLabel: String,
    val minutes: Int
)

data class WatchStatisticsSummary(
    val totalWatchTimeMs: Long,
    val videosPlayedCount: Int,
    val completedCount: Int,
    val weeklyWatchTimeMs: Long,
    val monthlyWatchTimeMs: Long,
    val mostPlayed: List<PlaybackHistoryEntry>,
    val recentlyWatched: List<PlaybackHistoryEntry>,
    val dailyBreakdown: List<DailyWatchStat>
)

data class LibraryHealthSummary(
    val totalIndexedVideos: Int,
    val totalHistoryEntries: Int,
    val missingFileEntries: List<PlaybackHistoryEntry>,
    val zeroDurationCount: Int,
    val duplicatePathCount: Int
) {
    val isHealthy: Boolean get() = missingFileEntries.isEmpty() && duplicatePathCount == 0
}

object SmartCollectionsAndStats {

    private const val LONG_VIDEO_MIN_MS = 20L * 60L * 1000L
    private const val SHORT_VIDEO_MAX_MS = 5L * 60L * 1000L
    private const val DAY_MS = 24L * 3600L * 1000L

    fun resolveSmartCollection(
        type: SmartCollectionType,
        videos: List<MediaVideoItem>,
        history: List<PlaybackHistoryEntry>,
        nowMs: Long = System.currentTimeMillis()
    ): List<MediaVideoItem> {
        val seedMap = videos.associateBy { it.id }
        val selectedIds = resolveSmartCollectionIds(
            type = type,
            entries = videos.map { it.toSmartEntry() },
            history = history.map { it.toSmartHistory() },
            nowMs = nowMs
        )
        return selectedIds.mapNotNull { seedMap[it] }
    }

    internal fun resolveSmartCollectionIds(
        type: SmartCollectionType,
        entries: List<SmartMediaEntry>,
        history: List<SmartHistoryEntry>,
        nowMs: Long = System.currentTimeMillis()
    ): List<String> {
        val recentThresholdMs = nowMs - 14L * DAY_MS
        val selected = when (type) {
            SmartCollectionType.UNWATCHED ->
                entries.filter { !it.isWatched && it.progress <= 0.02f }
            SmartCollectionType.RECENTLY_ADDED ->
                entries.filter { it.dateAddedTimestamp >= recentThresholdMs || entries.size <= 20 }
                    .sortedByDescending { it.dateAddedTimestamp }
                    .take(50)
            SmartCollectionType.RECENTLY_PLAYED -> {
                val byUri = entries.associateBy { it.id }
                history.asSequence()
                    .filter { !it.mimeType.startsWith("audio/", ignoreCase = true) }
                    .mapNotNull { entry ->
                        byUri[entry.uriString] ?: entries.firstOrNull {
                            it.title.equals(entry.title, ignoreCase = true)
                        }
                    }
                    .distinctBy { it.id }
                    .take(50)
                    .toList()
            }
            SmartCollectionType.PARTIALLY_WATCHED ->
                entries.filter { !it.isWatched && it.progress > 0.02f }
                    .sortedByDescending { it.progress }
            SmartCollectionType.COMPLETED ->
                entries.filter { it.isWatched }
            SmartCollectionType.LONG_VIDEOS ->
                entries.filter { it.durationMs >= LONG_VIDEO_MIN_MS }
                    .sortedByDescending { it.durationMs }
            SmartCollectionType.SHORT_VIDEOS ->
                entries.filter { it.durationMs in 1L until SHORT_VIDEO_MAX_MS }
                    .sortedBy { it.durationMs }
        }
        return selected.map { it.id }
    }

    fun matchesQualityFilter(
        resolutionLabel: String,
        fileNameWithExt: String,
        filter: MediaQualityFilterType
    ): Boolean = when (filter) {
        MediaQualityFilterType.ALL -> true
        MediaQualityFilterType.UHD_4K ->
            resolutionLabel.equals("4K", ignoreCase = true) ||
                fileNameWithExt.contains("2160", ignoreCase = true) ||
                fileNameWithExt.contains("4k", ignoreCase = true)
        MediaQualityFilterType.FHD_1080P ->
            resolutionLabel.equals("1080p", ignoreCase = true) ||
                fileNameWithExt.contains("1080", ignoreCase = true)
        MediaQualityFilterType.HD_720P ->
            resolutionLabel.equals("720p", ignoreCase = true) ||
                fileNameWithExt.contains("720", ignoreCase = true)
        MediaQualityFilterType.MKV ->
            fileNameWithExt.endsWith(".mkv", ignoreCase = true)
        MediaQualityFilterType.MP4 ->
            fileNameWithExt.endsWith(".mp4", ignoreCase = true)
    }

    fun filterByQuality(
        videos: List<MediaVideoItem>,
        filter: MediaQualityFilterType
    ): List<MediaVideoItem> {
        if (filter == MediaQualityFilterType.ALL) return videos
        return videos.filter { matchesQualityFilter(it.resolutionLabel, it.fileNameWithExt, filter) }
    }

    fun computeWatchStatistics(
        history: List<PlaybackHistoryEntry>,
        playCountsByKey: Map<String, Int> = emptyMap(),
        nowMs: Long = System.currentTimeMillis()
    ): WatchStatisticsSummary {
        val byUri = history.associateBy { it.uri.toString() }
        val pureStats = computeWatchStatisticsFromRecords(
            records = history.map { it.toSmartHistory() },
            playCountsByKey = playCountsByKey,
            nowMs = nowMs
        )
        return WatchStatisticsSummary(
            totalWatchTimeMs = pureStats.totalWatchTimeMs,
            videosPlayedCount = pureStats.videosPlayedCount,
            completedCount = pureStats.completedCount,
            weeklyWatchTimeMs = pureStats.weeklyWatchTimeMs,
            monthlyWatchTimeMs = pureStats.monthlyWatchTimeMs,
            mostPlayed = pureStats.mostPlayedIds.mapNotNull { byUri[it] },
            recentlyWatched = pureStats.recentlyWatchedIds.mapNotNull { byUri[it] },
            dailyBreakdown = pureStats.dailyBreakdown
        )
    }

    internal data class PureWatchStats(
        val totalWatchTimeMs: Long,
        val videosPlayedCount: Int,
        val completedCount: Int,
        val weeklyWatchTimeMs: Long,
        val monthlyWatchTimeMs: Long,
        val mostPlayedIds: List<String>,
        val recentlyWatchedIds: List<String>,
        val dailyBreakdown: List<DailyWatchStat>
    )

    internal fun computeWatchStatisticsFromRecords(
        records: List<SmartHistoryEntry>,
        playCountsByKey: Map<String, Int> = emptyMap(),
        nowMs: Long = System.currentTimeMillis()
    ): PureWatchStats {
        val videoEntries = records.filter { !it.mimeType.startsWith("audio/", ignoreCase = true) }
        val totalMs = records.sumOf { watchedMsForRecord(it) }
        val completed = videoEntries.count { it.played }
        val weekAgo = nowMs - 7L * DAY_MS
        val monthAgo = nowMs - 30L * DAY_MS
        val weeklyMs = records.filter { it.lastPlayedAtMs >= weekAgo }.sumOf { watchedMsForRecord(it) }
        val monthlyMs = records.filter { it.lastPlayedAtMs >= monthAgo }.sumOf { watchedMsForRecord(it) }

        val mostPlayed = videoEntries.sortedWith(
            compareByDescending<SmartHistoryEntry> {
                playCountsByKey[it.uriString] ?: if (it.played) 2 else 1
            }.thenByDescending { watchedMsForRecord(it) }
        ).take(8).map { it.uriString }

        val dayFormat = java.text.SimpleDateFormat("EEE", Locale.US)
        val todayDayIdx = nowMs / DAY_MS
        val daily = (6 downTo 0).map { offset ->
            val targetDay = todayDayIdx - offset
            val dayStart = targetDay * DAY_MS
            val dayEnd = dayStart + DAY_MS
            val mins = (records.filter { it.lastPlayedAtMs in dayStart until dayEnd }
                .sumOf { watchedMsForRecord(it) } / 60_000L).toInt().coerceAtLeast(0)
            DailyWatchStat(dayLabel = dayFormat.format(java.util.Date(dayStart)), minutes = mins)
        }

        return PureWatchStats(
            totalWatchTimeMs = totalMs,
            videosPlayedCount = videoEntries.size,
            completedCount = completed,
            weeklyWatchTimeMs = weeklyMs,
            monthlyWatchTimeMs = monthlyMs,
            mostPlayedIds = mostPlayed,
            recentlyWatchedIds = videoEntries.sortedByDescending { it.lastPlayedAtMs }.take(8).map { it.uriString },
            dailyBreakdown = daily
        )
    }

    fun computeLibraryHealth(
        videos: List<MediaVideoItem>,
        history: List<PlaybackHistoryEntry>
    ): LibraryHealthSummary {
        val byUri = history.associateBy { it.uri.toString() }
        val pure = computeLibraryHealthFromRecords(
            entries = videos.map { it.toSmartEntry() },
            records = history.map { it.toSmartHistory() },
            fileExists = { path -> path.isNotBlank() && File(path).exists() }
        )
        return LibraryHealthSummary(
            totalIndexedVideos = pure.totalIndexedVideos,
            totalHistoryEntries = pure.totalHistoryEntries,
            missingFileEntries = pure.missingFileUriStrings.mapNotNull { byUri[it] },
            zeroDurationCount = pure.zeroDurationCount,
            duplicatePathCount = pure.duplicatePathCount
        )
    }

    internal data class PureLibraryHealth(
        val totalIndexedVideos: Int,
        val totalHistoryEntries: Int,
        val missingFileUriStrings: List<String>,
        val zeroDurationCount: Int,
        val duplicatePathCount: Int
    )

    internal fun computeLibraryHealthFromRecords(
        entries: List<SmartMediaEntry>,
        records: List<SmartHistoryEntry>,
        fileExists: (String) -> Boolean = { path -> path.isNotBlank() && File(path).exists() }
    ): PureLibraryHealth {
        val missing = records.filter { rec ->
            val raw = rec.uriString
            if (raw.startsWith("file://")) {
                val path = raw.removePrefix("file://")
                path.isBlank() || !fileExists(path)
            } else {
                false
            }
        }.map { it.uriString }
        val zeroDuration = entries.count { it.durationMs <= 0L }
        val duplicateCount = entries.size - entries.distinctBy {
            "${it.fileNameWithExt.lowercase(Locale.ROOT)}:${it.sizeBytes}"
        }.size

        return PureLibraryHealth(
            totalIndexedVideos = entries.size,
            totalHistoryEntries = records.size,
            missingFileUriStrings = missing,
            zeroDurationCount = zeroDuration,
            duplicatePathCount = duplicateCount.coerceAtLeast(0)
        )
    }

    private fun watchedMsForRecord(entry: SmartHistoryEntry): Long {
        return when {
            entry.played && entry.durationMs > 0L -> entry.durationMs
            entry.positionMs > 0L -> entry.positionMs
            else -> 0L
        }
    }
}
