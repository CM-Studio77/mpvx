package com.cmstudio.player.ui.library

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistAdd
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.automirrored.rounded.Sort
import androidx.compose.material.icons.automirrored.rounded.ViewList
import androidx.compose.material.icons.rounded.AccountTree
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Favorite
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Lan
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.RemoveDone
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.model.MediaVideoItem
import com.cmstudio.player.ui.theme.extendedColors
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Shared Coil [ImageRequest] builder ensuring every video card & carousel extracts an actual frame
 * reliably (avoiding black 0ms frames on long videos and out-of-range offsets on short clips).
 */
fun buildVideoThumbnailRequest(
    context: android.content.Context,
    uri: Uri?,
    durationMs: Long = 0L
): ImageRequest? {
    uri ?: return null
    val scheme = uri.scheme?.lowercase(Locale.ROOT).orEmpty()
    if ((scheme == "http" || scheme == "https") && !AppSettings.networkThumbnailsEnabled.value) {
        return null
    }
    val frameMs = when {
        durationMs in 1L..2500L -> (durationMs / 3L).coerceAtLeast(80L)
        durationMs in 2501L..15_000L -> (durationMs / 5L).coerceAtLeast(600L)
        else -> 1500L
    }
    return ImageRequest.Builder(context)
        .data(uri)
        .videoFrameMillis(frameMs)
        .crossfade(true)
        .build()
}

data class TreeDirectoryEntry(
    val name: String,
    val fullSegments: List<String>,
    val videoCount: Int,
    val totalSizeBytes: Long,
    val totalDurationMs: Long,
    val latestThumbnailUri: Uri?,
    val newCount: Int
)

data class MediaTreeLevel(
    val currentSegments: List<String>,
    val directories: List<TreeDirectoryEntry>,
    val videos: List<MediaVideoItem>
)

/**
 * Pure MediaStore-safe hierarchical directory builder derived from `relativePath` / `folderName`.
 * Enables nested Tree browsing + Breadcrumbs without requiring MANAGE_EXTERNAL_STORAGE.
 */
object MediaTreeResolver {
    fun splitPathSegments(relativePath: String, fallbackFolder: String): List<String> {
        val raw = relativePath.trim().ifBlank { fallbackFolder.trim() }
        if (raw.isBlank()) return emptyList()
        return raw.replace('\\', '/')
            .split('/')
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.startsWith("/storage", ignoreCase = true) }
    }

    fun resolveDirectoryLevel(
        videos: List<MediaVideoItem>,
        currentSegments: List<String>
    ): MediaTreeLevel {
        val childGroups = linkedMapOf<String, MutableList<MediaVideoItem>>()
        val directFiles = mutableListOf<MediaVideoItem>()

        videos.forEach { video ->
            val segs = splitPathSegments(video.relativePath, video.folderName)
            val prefixMatches = segs.size >= currentSegments.size &&
                currentSegments.indices.all { idx -> segs[idx].equals(currentSegments[idx], ignoreCase = true) }
            if (!prefixMatches) return@forEach

            if (segs.size == currentSegments.size) {
                directFiles.add(video)
            } else {
                val childName = segs[currentSegments.size]
                childGroups.getOrPut(childName) { mutableListOf() }.add(video)
            }
        }

        val dirs = childGroups.entries.map { (name, items) ->
            val latest = items.maxByOrNull { it.modifiedTimestamp }
            TreeDirectoryEntry(
                name = name,
                fullSegments = currentSegments + name,
                videoCount = items.size,
                totalSizeBytes = items.sumOf { it.sizeBytes },
                totalDurationMs = items.sumOf { it.durationMs },
                latestThumbnailUri = latest?.thumbnailUri,
                newCount = items.count { !it.isWatched && it.progress <= 0f }
            )
        }.sortedBy { it.name.lowercase(Locale.ROOT) }

        return MediaTreeLevel(
            currentSegments = currentSegments,
            directories = dirs,
            videos = directFiles
        )
    }

    fun formatVideoTitle(title: String, fileNameWithExt: String, showExtension: Boolean): String {
        val source = fileNameWithExt.ifBlank { title }
        if (showExtension) return source
        val dot = source.lastIndexOf('.')
        return if (dot > 0) source.substring(0, dot) else source
    }
}

fun formatCompactDuration(ms: Long): String {
    if (ms <= 0L) return "0:00"
    val totalSec = ms / 1000L
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%d:%02d", m, s)
}

@Composable
fun ContinueWatchingHeroCard(
    video: MediaVideoItem,
    showExtension: Boolean,
    showProgressBar: Boolean,
    onResumeClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val displayTitle = remember(video.id, video.title, video.fileNameWithExt, showExtension) {
        MediaTreeResolver.formatVideoTitle(video.title, video.fileNameWithExt, showExtension)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onResumeClick),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(112.dp)
                    .padding(10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Compact 16:10 thumbnail on the left of the hero resume card
                Box(
                    modifier = Modifier
                        .height(92.dp)
                        .aspectRatio(16f / 10f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFF0E141B))
                ) {
                    val heroThumbReq = remember(video.thumbnailUri, video.durationMs) {
                        buildVideoThumbnailRequest(context, video.thumbnailUri, video.durationMs)
                    }
                    if (heroThumbReq != null) {
                        AsyncImage(
                            model = heroThumbReq,
                            contentDescription = displayTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(Color.Transparent, Color(0x99000000))
                                )
                            )
                    )
                    if (video.resolutionLabel.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.68f),
                            modifier = Modifier
                                .align(Alignment.TopStart)
                                .padding(5.dp)
                        ) {
                            Text(
                                text = video.resolutionLabel,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                    if (video.durationText.isNotBlank()) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color.Black.copy(alpha = 0.72f),
                            modifier = Modifier
                                .align(Alignment.BottomEnd)
                                .padding(5.dp)
                        ) {
                            Text(
                                text = video.durationText,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }

                // Right column: badge + title + remaining + Resume >> pill
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxSize(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "CONTINUE WATCHING",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, letterSpacing = 0.6.sp),
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = displayTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = video.remainingText.ifBlank { video.sourceTag },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(end = 6.dp)
                        )
                        Surface(
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                Icon(
                                    Icons.Rounded.PlayArrow,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Resume",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                        }
                    }
                }
            }

            if (showProgressBar && video.progress > 0f) {
                LinearProgressIndicator(
                    progress = { video.progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(3.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }
        }
    }
}

@Composable
fun QuickAccessBentoSection(
    allVideosCount: Int,
    foldersCount: Int,
    favoritesCount: Int,
    playlistsCount: Int,
    activeQuickFilter: String, // "NONE", "ALL_VIDEOS", "FAVORITES"
    onAllVideosClick: () -> Unit,
    onFoldersClick: () -> Unit,
    onFavoritesClick: () -> Unit,
    onPlaylistsClick: () -> Unit,
    onOnlineStreamClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickBentoTile(
                modifier = Modifier.weight(1f),
                title = "All Videos",
                countText = "$allVideosCount ${if (allVideosCount == 1) "file" else "files"}",
                icon = Icons.Rounded.VideoLibrary,
                accentColor = MaterialTheme.colorScheme.primary,
                selected = activeQuickFilter == "ALL_VIDEOS",
                onClick = onAllVideosClick
            )
            QuickBentoTile(
                modifier = Modifier.weight(1f),
                title = "Folders",
                countText = "$foldersCount ${if (foldersCount == 1) "folder" else "folders"}",
                icon = Icons.Rounded.Folder,
                accentColor = Color(0xFF0EA5E9),
                selected = activeQuickFilter == "NONE",
                onClick = onFoldersClick
            )
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickBentoTile(
                modifier = Modifier.weight(1f),
                title = "Favorites",
                countText = "$favoritesCount starred",
                icon = Icons.Rounded.Favorite,
                accentColor = Color(0xFFF43F5E),
                selected = activeQuickFilter == "FAVORITES",
                onClick = onFavoritesClick
            )
            QuickBentoTile(
                modifier = Modifier.weight(1f),
                title = "Playlists",
                countText = "$playlistsCount ${if (playlistsCount == 1) "list" else "lists"}",
                icon = Icons.AutoMirrored.Rounded.PlaylistPlay,
                accentColor = Color(0xFF8B5CF6),
                selected = false,
                onClick = onPlaylistsClick
            )
        }

        // Full-width Online Stream quick action card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(18.dp))
                .clickable(onClick = onOnlineStreamClick),
            shape = RoundedCornerShape(18.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Public,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Online Stream",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Play HTTP, HLS (.m3u8), DASH (.mpd) or RTSP streams",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun QuickBentoTile(
    title: String,
    countText: String,
    icon: ImageVector,
    accentColor: Color,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(18.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(18.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
        } else {
            MaterialTheme.colorScheme.surfaceContainerLow
        },
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.65f)
            else MaterialTheme.extendedColors.cardBorder
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accentColor.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = accentColor, modifier = Modifier.size(20.dp))
            }
            Spacer(modifier = Modifier.width(10.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = countText,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}

@Composable
fun RecentlyPlayedCarousel(
    items: List<MediaVideoItem>,
    showExtension: Boolean,
    showProgressBar: Boolean,
    onItemClick: (MediaVideoItem) -> Unit,
    onSeeAllClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Recently Played",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            TextButton(onClick = onSeeAllClick, contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)) {
                Text("See All", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            contentPadding = PaddingValues(end = 4.dp)
        ) {
            items(items, key = { "recent_${it.id}" }) { item ->
                val titleText = MediaTreeResolver.formatVideoTitle(item.title, item.fileNameWithExt, showExtension)
                Card(
                    modifier = Modifier
                        .width(172.dp)
                        .clickable { onItemClick(item) },
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerLow),
                    border = BorderStroke(1.dp, MaterialTheme.extendedColors.cardBorder)
                ) {
                    Column {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(16f / 9f)
                                .background(Color(0xFF151922))
                        ) {
                            val itemThumbReq = remember(item.thumbnailUri, item.durationMs) {
                                buildVideoThumbnailRequest(context, item.thumbnailUri, item.durationMs)
                            }
                            if (itemThumbReq != null) {
                                AsyncImage(
                                    model = itemThumbReq,
                                    contentDescription = titleText,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            if (item.durationText.isNotBlank()) {
                                Surface(
                                    modifier = Modifier
                                        .align(Alignment.BottomEnd)
                                        .padding(6.dp),
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color.Black.copy(alpha = 0.7f)
                                ) {
                                    Text(
                                        text = item.durationText,
                                        style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                    )
                                }
                            }
                            if (showProgressBar && item.progress > 0f) {
                                LinearProgressIndicator(
                                    progress = { item.progress },
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .fillMaxWidth()
                                        .height(3.dp),
                                    color = MaterialTheme.colorScheme.primary,
                                    trackColor = Color.Black.copy(alpha = 0.3f)
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)) {
                            Text(
                                text = titleText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = item.remainingText.ifBlank { item.sourceTag },
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
    }
}

@Composable
fun StorageSectionRibbon(
    title: String,
    countBadge: Int,
    viewMode: String,
    layoutMode: String,
    onOpenSortSheet: () -> Unit,
    onCycleViewMode: () -> Unit,
    onToggleLayout: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.extendedColors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = countBadge.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Surface(
                    modifier = Modifier
                        .clip(CircleShape)
                        .clickable(onClick = onCycleViewMode),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = when (viewMode) {
                                "TREE" -> Icons.Rounded.AccountTree
                                "MEDIA" -> Icons.Rounded.VideoLibrary
                                else -> Icons.Rounded.Folder
                            },
                            contentDescription = "Cycle View Mode",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = when (viewMode) {
                                "TREE" -> "Tree"
                                "MEDIA" -> "Media"
                                else -> "Folder"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                IconButton(onClick = onOpenSortSheet, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.Sort,
                        contentDescription = "Sort & View",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(onClick = onToggleLayout, modifier = Modifier.size(34.dp)) {
                    Icon(
                        imageVector = if (layoutMode == "GRID") Icons.AutoMirrored.Rounded.ViewList else Icons.Rounded.GridView,
                        contentDescription = "Toggle List or Grid",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TreeBreadcrumbBar(
    segments: List<String>,
    onSegmentClick: (Int) -> Unit, // -1 = Storage Root
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.extendedColors.cardBorder)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 10.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val isRoot = segments.isEmpty()
            Surface(
                modifier = Modifier
                    .clip(RoundedCornerShape(10.dp))
                    .clickable { onSegmentClick(-1) },
                shape = RoundedCornerShape(10.dp),
                color = if (isRoot) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Home,
                        contentDescription = "Storage Root",
                        tint = if (isRoot) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Storage",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isRoot) FontWeight.Bold else FontWeight.Medium,
                        color = if (isRoot) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            segments.forEachIndexed { index, name ->
                Icon(
                    imageVector = Icons.Rounded.ChevronRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(16.dp)
                )
                val isCurrent = index == segments.lastIndex
                Surface(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSegmentClick(index) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isCurrent) MaterialTheme.colorScheme.primaryContainer else Color.Transparent
                ) {
                    Text(
                        text = name,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrent) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ExpandableQuickPlayFab(
    expanded: Boolean,
    directMode: Boolean,
    visible: Boolean = true,
    onToggleExpand: () -> Unit,
    onDirectPlay: () -> Unit,
    onOpenFile: () -> Unit,
    onOpenRecents: () -> Unit,
    onPlayLink: () -> Unit,
    onOpenNetwork: () -> Unit,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.8f),
        exit = fadeOut() + scaleOut(targetScale = 0.8f),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            AnimatedVisibility(
                visible = !directMode && expanded,
                enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
            ) {
                Column(
                    horizontalAlignment = Alignment.End,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickPlayMenuItem(
                        label = "Local Network",
                        icon = Icons.Rounded.Lan,
                        onClick = { onToggleExpand(); onOpenNetwork() }
                    )
                    QuickPlayMenuItem(
                        label = "Play Link",
                        icon = Icons.Rounded.Link,
                        onClick = { onToggleExpand(); onPlayLink() }
                    )
                    QuickPlayMenuItem(
                        label = "Recently Played",
                        icon = Icons.Rounded.History,
                        onClick = { onToggleExpand(); onOpenRecents() }
                    )
                    QuickPlayMenuItem(
                        label = "Open File",
                        icon = Icons.Rounded.FolderOpen,
                        onClick = { onToggleExpand(); onOpenFile() }
                    )
                }
            }

            FloatingActionButton(
                onClick = {
                    if (directMode) onDirectPlay() else onToggleExpand()
                },
                shape = CircleShape,
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer
            ) {
                Icon(
                    imageVector = if (!directMode && expanded) Icons.Rounded.Close else Icons.Rounded.PlayArrow,
                    contentDescription = if (directMode) "Quick Play Latest" else "Quick Play Menu"
                )
            }
        }
    }
}

@Composable
private fun QuickPlayMenuItem(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Surface(
            modifier = Modifier
                .clip(RoundedCornerShape(14.dp))
                .clickable(onClick = onClick),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            shadowElevation = 4.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
            )
        }
        SmallFloatingActionButton(
            onClick = onClick,
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ) {
            Icon(icon, contentDescription = label, modifier = Modifier.size(20.dp))
        }
    }
}

@Composable
fun SelectionTopBar(
    selectedCount: Int,
    totalCount: Int,
    onClearSelection: () -> Unit,
    onSelectAll: () -> Unit,
    onInvertSelection: () -> Unit,
    modifier: Modifier = Modifier
) {
    var dropdownExpanded by remember { mutableStateOf(false) }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onClearSelection) {
                    Icon(
                        Icons.Rounded.Close,
                        contentDescription = "Cancel Selection",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
                Box {
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { dropdownExpanded = true }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$selectedCount / $totalCount",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            Icons.Rounded.ExpandMore,
                            contentDescription = "Selection Options",
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    DropdownMenu(
                        expanded = dropdownExpanded,
                        onDismissRequest = { dropdownExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Select All ($totalCount)") },
                            onClick = { dropdownExpanded = false; onSelectAll() }
                        )
                        DropdownMenuItem(
                            text = { Text("Invert Selection") },
                            onClick = { dropdownExpanded = false; onInvertSelection() }
                        )
                        DropdownMenuItem(
                            text = { Text("Deselect All") },
                            onClick = { dropdownExpanded = false; onClearSelection() }
                        )
                    }
                }
            }

            TextButton(onClick = onInvertSelection) {
                Text(
                    text = "Invert",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }
    }
}

@Composable
fun FloatingBottomActionBar(
    selectedCount: Int,
    onPlaySelected: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onFavoriteSelected: () -> Unit,
    onVaultSelected: () -> Unit,
    onShareSelected: () -> Unit,
    onDeleteSelected: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 12.dp),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        tonalElevation = 8.dp,
        shadowElevation = 12.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 6.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomActionButton(Icons.Rounded.PlayArrow, "Play ($selectedCount)", onPlaySelected)
            BottomActionButton(Icons.AutoMirrored.Rounded.PlaylistAdd, "Playlist", onAddToPlaylist)
            BottomActionButton(Icons.Rounded.Favorite, "Favorite", onFavoriteSelected)
            BottomActionButton(Icons.Rounded.Lock, "Vault", onVaultSelected)
            BottomActionButton(Icons.Rounded.Share, "Share", onShareSelected)
            BottomActionButton(
                Icons.Rounded.Delete,
                "Delete",
                onDeleteSelected,
                tint = MaterialTheme.colorScheme.error
            )
        }
    }
}

@Composable
private fun BottomActionButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = label.substringBefore(" "),
            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
fun SwipeToWatchWrapper(
    isWatched: Boolean,
    onToggleWatched: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val triggerPx = with(density) { 64.dp.toPx() }
    var rawDragPx by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(
        targetValue = rawDragPx,
        animationSpec = spring(stiffness = 650f),
        label = "swipeToWatchOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
    ) {
        if (animatedOffset > 6f) {
            Surface(
                modifier = Modifier.matchParentSize(),
                color = if (isWatched) {
                    MaterialTheme.colorScheme.secondaryContainer
                } else {
                    MaterialTheme.colorScheme.primaryContainer
                },
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = if (isWatched) Icons.Rounded.RemoveDone else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (isWatched) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isWatched) "Mark Unwatched" else "Mark Watched",
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Bold,
                        color = if (isWatched) {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        } else {
                            MaterialTheme.colorScheme.onPrimaryContainer
                        }
                    )
                }
            }
        }

        Box(
            modifier = Modifier
                .offset { IntOffset(animatedOffset.roundToInt(), 0) }
                .pointerInput(isWatched) {
                    detectHorizontalDragGestures(
                        onHorizontalDrag = { _, dragAmount ->
                            rawDragPx = (rawDragPx + dragAmount).coerceIn(0f, triggerPx * 1.4f)
                        },
                        onDragEnd = {
                            if (rawDragPx >= triggerPx) {
                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                onToggleWatched()
                            }
                            rawDragPx = 0f
                        },
                        onDragCancel = { rawDragPx = 0f }
                    )
                }
        ) {
            content()
        }
    }
}
