package com.cmstudio.player.ui.model

import android.net.Uri
import androidx.compose.runtime.Immutable

@Immutable
data class MediaVideoItem(
    val id: String,
    val title: String,
    val uri: Uri,
    val folderId: String = "",
    val durationText: String = "",
    val remainingText: String = "",
    val progress: Float = 0f,
    val sizeText: String = "",
    val dateModifiedText: String = "",
    val sourceTag: String = "Local File",
    val thumbnailUri: Uri? = null,
    val durationMs: Long = 0L,
    val sizeBytes: Long = 0L,
    val modifiedTimestamp: Long = 0L,
    val dateAddedTimestamp: Long = 0L,
    val folderName: String = "",
    val resolutionLabel: String = "",
    val isWatched: Boolean = false,
    val relativePath: String = "",
    val fileNameWithExt: String = title,
    val fps: Float = 0f
) {
    val remainingTimeText: String
        get() = if (remainingText.isNotBlank()) remainingText else "00:00 left"

    val fileSize: String
        get() = sizeText

    val durationFormatted: String
        get() = durationText
}

@Immutable
data class MediaFolderItem(
    val id: String,
    val name: String,
    val path: String = "",
    val videoCount: Int = 0,
    val sizeText: String = "",
    val dateModifiedText: String = "",
    val latestVideoThumbnailUri: Uri? = null,
    val newCount: Int = 0,
    val totalSizeBytes: Long = 0L,
    val modifiedTimestamp: Long = 0L,
    val dateAddedTimestamp: Long = 0L,
    val totalDurationMs: Long = 0L
) {
    val thumbnailUri: Uri?
        get() = latestVideoThumbnailUri

    val newVideosCount: Int
        get() = newCount

    val modifiedDateText: String
        get() = dateModifiedText

    val lastModified: Long
        get() = modifiedTimestamp

    val totalSizeFormatted: String
        get() = sizeText

    val lastModifiedFormatted: String
        get() = dateModifiedText

    val isNew: Boolean
        get() = newCount > 0
}

@Immutable
data class WatchStreakGoal(
    val currentStreakDays: Int = 7,
    val dailyGoalMinutes: Int = 45,
    val watchedTodayMinutes: Int = 30
) {
    val todayWatchMinutes: Int
        get() = watchedTodayMinutes

    val todayWatchedMinutes: Int
        get() = watchedTodayMinutes

    val progress: Float
        get() = if (dailyGoalMinutes > 0) {
            (watchedTodayMinutes.toFloat() / dailyGoalMinutes).coerceIn(0f, 1f)
        } else 0f
}

// Custom Theme Palette Model
@Immutable
data class UserPlaylist(
    val id: String,
    val name: String,
    val itemUris: List<String> = emptyList(),
    val createdAtMs: Long = 0L
)

@Immutable
data class SavedStreamItem(
    val id: String,
    val title: String,
    val url: String,
    val streamType: String = "HLS"
)

@Immutable
data class NetworkShareConfig(
    val id: String,
    val name: String,
    val protocol: String = "WebDAV",
    val baseUrl: String,
    val username: String = ""
)

@Immutable
data class NetworkRemoteEntry(
    val name: String,
    val url: String,
    val isDirectory: Boolean,
    val sizeBytes: Long = 0L,
    val contentType: String = ""
)
