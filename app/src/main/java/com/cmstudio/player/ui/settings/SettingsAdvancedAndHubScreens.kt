package com.cmstudio.player.ui.settings

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CleaningServices
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Terminal
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmstudio.player.PlaybackHistoryBridge
import com.cmstudio.player.PlaybackHistoryEntry
import com.cmstudio.player.diagnostics.PlaybackDiagnosticsLog
import com.cmstudio.player.engine.MpvConfManager
import com.cmstudio.player.home.Library
import com.cmstudio.player.library.MediaQualityFilterType
import com.cmstudio.player.library.SmartCollectionType
import com.cmstudio.player.library.SmartCollectionsAndStats
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.library.formatCompactDuration
import com.cmstudio.player.ui.model.MediaVideoItem
import com.cmstudio.player.ui.player.ModernPlayerActivity
import com.cmstudio.player.ui.theme.extendedColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

@Composable
internal fun JsonBackupAndRestoreCard() {
    val context = LocalContext.current

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val json = AppSettings.exportSettingsJson()
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(json.toByteArray(Charsets.UTF_8))
            }
            Toast.makeText(context, "Settings backup exported cleanly (secrets redacted)", Toast.LENGTH_SHORT).show()
        }.onFailure {
            Toast.makeText(context, "Export failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            val text = context.contentResolver.openInputStream(uri)?.bufferedReader()?.readText().orEmpty()
            val count = AppSettings.importSettingsJson(text)
            if (count >= 0) {
                Toast.makeText(context, "Restored $count setting(s)", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Invalid MPVX backup JSON file", Toast.LENGTH_SHORT).show()
            }
        }.onFailure {
            Toast.makeText(context, "Import failed: ${it.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Backup, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("Configuration Backup & Restore", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "Export or restore all MPVX preferences, HUD layout, and theme settings as JSON (Vault PINs & server tokens are always excluded).",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = { exportLauncher.launch("mpvx-settings-backup.json") },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Backup, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Export JSON")
            }
            OutlinedButton(
                onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Restore JSON")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SafeMpvConfCard() {
    val context = LocalContext.current
    val enabled by AppSettings.mpvConfEnabled.collectAsState()
    val storedText by AppSettings.mpvConfText.collectAsState()
    var draftText by remember(storedText) { mutableStateOf(storedText.ifBlank { MpvConfManager.DEFAULT_TEMPLATE }) }
    val validation = remember(draftText) { MpvConfManager.validateAndParse(draftText) }
    val storagePath = remember(context) { MpvConfManager.configStoragePath(context) }

    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.Terminal, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
            Text("MPV Configuration (mpv.conf)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "Storage: $storagePath",
            style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        SwitchPreferenceRow(
            title = "Apply Validated mpv.conf Overrides",
            subtitle = "Applies verified playback options after built-in MPV settings; unsafe scripts & shell hooks are blocked",
            checked = enabled
        ) { AppSettings.setMpvConfEnabled(it) }

        OutlinedTextField(
            value = draftText,
            onValueChange = { draftText = it },
            label = { Text("Safe mpv.conf Options") },
            textStyle = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
            minLines = 5,
            maxLines = 9,
            modifier = Modifier.fillMaxWidth()
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            FilterChip(
                selected = false,
                onClick = {
                    draftText = "# High Quality Scaling\nscale=ewa_lanczossharp\ncscale=mitchell\ndscale=mitchell\n"
                },
                label = { Text("+ HQ Scaling") }
            )
            FilterChip(
                selected = false,
                onClick = {
                    draftText = "# Smooth Motion\nvideo-sync=display-resample\ninterpolation=yes\ntscale=oversample\n"
                },
                label = { Text("+ Interpolation") }
            )
            FilterChip(
                selected = false,
                onClick = {
                    draftText = MpvConfManager.DEFAULT_TEMPLATE
                    AppSettings.setMpvConfText("")
                    Toast.makeText(context, "mpv.conf reset to safe default", Toast.LENGTH_SHORT).show()
                },
                label = { Text("Reset") }
            )
        }

        // Live validation & option ownership feedback
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = if (validation.isClean) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f)
            else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.7f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(
                        imageVector = if (validation.isClean) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                        contentDescription = null,
                        tint = if (validation.isClean) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = if (validation.isClean) {
                            "${validation.validEntries.size} validated option(s) ready"
                        } else {
                            "${validation.diagnostics.size} blocked/invalid line(s) will be safely skipped"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                if (validation.ownedKeys.isNotEmpty()) {
                    val groups = validation.ownedKeys.map { MpvConfManager.ownershipGroupForKey(it) }.distinct()
                    Text(
                        text = "Option ownership: ${groups.joinToString(", ")}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                validation.diagnostics.take(3).forEach { diag ->
                    Text(
                        text = "Line ${diag.lineNumber}: ${diag.reason}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Button(
            onClick = {
                AppSettings.setMpvConfText(draftText)
                Toast.makeText(
                    context,
                    "Saved ${validation.validEntries.size} valid option(s)",
                    Toast.LENGTH_SHORT
                ).show()
            },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("Validate & Save mpv.conf")
        }
    }
}

private data class GenuineExtraHubData(
    val videos: List<MediaVideoItem> = emptyList(),
    val history: List<PlaybackHistoryEntry> = emptyList(),
    val refreshKey: Int = 0
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun GenuineExtraHubCards() {
    val context = LocalContext.current
    var reloadCounter by remember { mutableStateOf(0) }
    val data by produceState(GenuineExtraHubData(), reloadCounter) {
        value = withContext(Dispatchers.IO) {
            val history = runCatching { PlaybackHistoryBridge.loadHistory(context) }.getOrDefault(emptyList())
            val rawVideos = runCatching { Library.videos(context) }.getOrDefault(emptyList())
            val items = rawVideos.map { v ->
                val uriStr = v.uri.toString()
                val matched = history.firstOrNull { it.uri.toString() == uriStr }
                val dur = v.duration.takeIf { it > 0L } ?: matched?.durationMs ?: 0L
                val pos = matched?.positionMs ?: 0L
                MediaVideoItem(
                    id = uriStr,
                    title = v.name ?: "Video",
                    uri = v.uri,
                    folderId = v.folderId ?: "",
                    durationText = formatCompactDuration(dur),
                    progress = if (dur > 0L && pos > 0L) (pos.toFloat() / dur).coerceIn(0f, 1f) else 0f,
                    durationMs = dur,
                    sizeBytes = v.size,
                    modifiedTimestamp = v.modified * 1000L,
                    dateAddedTimestamp = v.modified * 1000L,
                    folderName = v.folderName ?: "",
                    isWatched = matched?.played ?: false,
                    fileNameWithExt = v.name ?: "Video"
                )
            }
            GenuineExtraHubData(items, history, reloadCounter)
        }
    }

    var activeCollection by remember { mutableStateOf<SmartCollectionType?>(null) }
    var activeQuality by remember { mutableStateOf(MediaQualityFilterType.ALL) }
    val diagEntries by PlaybackDiagnosticsLog.entries.collectAsState()

    val stats = remember(data) {
        SmartCollectionsAndStats.computeWatchStatistics(data.history)
    }
    val health = remember(data) {
        SmartCollectionsAndStats.computeLibraryHealth(data.videos, data.history)
    }

    // 1. Smart Collections & Media Quality Filters (Interactive + Playable!)
    SettingsCard {
        Text("Smart Collections & Quality Explorer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            SmartCollectionType.entries.forEach { col ->
                val items = remember(data, col) { SmartCollectionsAndStats.resolveSmartCollection(col, data.videos, data.history) }
                FilterChip(
                    selected = activeCollection == col,
                    onClick = { activeCollection = if (activeCollection == col) null else col },
                    label = { Text("${col.title} (${items.size})") }
                )
            }
        }
        Text("Quality Filter", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            MediaQualityFilterType.entries.forEach { qf ->
                FilterChip(
                    selected = activeQuality == qf,
                    onClick = {
                        activeQuality = qf
                        AppSettings.setMediaQualityFilter(qf.id)
                    },
                    label = { Text(qf.label) }
                )
            }
        }
        val previewList = remember(data, activeCollection, activeQuality) {
            val base = activeCollection?.let { SmartCollectionsAndStats.resolveSmartCollection(it, data.videos, data.history) } ?: data.videos
            SmartCollectionsAndStats.filterByQuality(base, activeQuality)
        }
        if (activeCollection != null || activeQuality != MediaQualityFilterType.ALL) {
            HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
            if (previewList.isEmpty()) {
                Text("No matching videos in this collection/filter.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            } else {
                previewList.take(6).forEach { vid ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable {
                                val intent = Intent(context, ModernPlayerActivity::class.java).apply {
                                    setData(vid.uri)
                                    putExtra("video_title", vid.title)
                                }
                                context.startActivity(intent)
                            }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.PlayArrow, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(vid.title, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(vid.durationText, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }

    // 2. Watch Statistics Dashboard (Weekly/Monthly + Daily Breakdown + Most Played)
    SettingsCard {
        Text("Watch Statistics", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            StatMiniBox("Total", formatDurationShort(stats.totalWatchTimeMs), Modifier.weight(1f))
            StatMiniBox("7 Days", formatDurationShort(stats.weeklyWatchTimeMs), Modifier.weight(1f))
            StatMiniBox("30 Days", formatDurationShort(stats.monthlyWatchTimeMs), Modifier.weight(1f))
            StatMiniBox("Done", "${stats.completedCount}/${stats.videosPlayedCount}", Modifier.weight(1f))
        }
        val maxDayMin = (stats.dailyBreakdown.maxOfOrNull { it.minutes } ?: 1).coerceAtLeast(1)
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Bottom
        ) {
            stats.dailyBreakdown.forEach { d ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                    val barH = ((d.minutes.toFloat() / maxDayMin.toFloat()) * 34f).coerceIn(4f, 34f)
                    Box(
                        modifier = Modifier
                            .width(14.dp)
                            .height(barH.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (d.minutes > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                    Spacer(Modifier.height(3.dp))
                    Text(d.dayLabel, style = MaterialTheme.typography.labelSmall, fontSize = 10.sp)
                }
            }
        }
        if (stats.mostPlayed.isNotEmpty()) {
            HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
            Text("Most & Recently Played", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.SemiBold)
            stats.mostPlayed.take(3).forEach { e ->
                Text("• ${e.title} (${(e.progress * 100f).toInt()}%)", style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }

    // 3. Library Health Report
    SettingsCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Icon(Icons.Rounded.HealthAndSafety, contentDescription = null, tint = if (health.isHealthy) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error)
            Text("Library Health Report", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
        }
        Text(
            "Indexed: ${health.totalIndexedVideos} videos • History: ${health.totalHistoryEntries} • Missing refs: ${health.missingFileEntries.size} • Duplicate names: ${health.duplicatePathCount}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (health.missingFileEntries.isNotEmpty()) {
            OutlinedButton(
                onClick = {
                    health.missingFileEntries.forEach { PlaybackHistoryBridge.removeHistoryEntry(context, it.uri) }
                    reloadCounter++
                    Toast.makeText(context, "Cleaned ${health.missingFileEntries.size} stale history entry(s)", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Rounded.CleaningServices, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("Remove Missing History References (Files Untouched)")
            }
        }
    }

    // 4. Playback Diagnostics & Logs Ring Buffer
    SettingsCard {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Playback Logs (${diagEntries.size})", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Row {
                IconButton(onClick = {
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                    cm?.setPrimaryClip(ClipData.newPlainText("MPVX Playback Logs", PlaybackDiagnosticsLog.formatReport()))
                    Toast.makeText(context, "Diagnostics copied", Toast.LENGTH_SHORT).show()
                }) { Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Logs", modifier = Modifier.size(18.dp)) }
                IconButton(onClick = { PlaybackDiagnosticsLog.clear() }) {
                    Icon(Icons.Rounded.DeleteSweep, contentDescription = "Clear Logs", modifier = Modifier.size(18.dp))
                }
            }
        }
        if (diagEntries.isEmpty()) {
            Text("Play a video to inspect live engine, decoder, resolution & buffering logs.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                diagEntries.take(8).forEach { log ->
                    Text(
                        text = "[${log.engine}/${log.category}] ${log.message}",
                        style = MaterialTheme.typography.labelSmall.copy(fontFamily = FontFamily.Monospace),
                        color = if (log.level == "ERROR") MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun StatMiniBox(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, MaterialTheme.extendedColors.cardBorder)
    ) {
        Column(modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.ExtraBold, color = MaterialTheme.colorScheme.primary)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun formatDurationShort(ms: Long): String {
    val totalMin = (ms / 60_000L).coerceAtLeast(0L)
    val h = totalMin / 60L
    val m = totalMin % 60L
    return if (h > 0L) "${h}h ${m}m" else "${m}m"
}
