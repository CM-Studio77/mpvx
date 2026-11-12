package com.cmstudio.player.ui.settings

import android.app.ActivityManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.ExpandLess
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import com.cmstudio.player.BuildConfig
import com.cmstudio.player.PlaybackHistoryBridge
import com.cmstudio.player.PlaybackHistoryEntry
import com.cmstudio.player.codec.CodecCapabilitiesInfo
import com.cmstudio.player.codec.CodecInspector
import com.cmstudio.player.home.Library
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.theme.extendedColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private data class ExtraFeaturesSnapshot(
    val history: List<PlaybackHistoryEntry> = emptyList(),
    val localVideosCount: Int = 0,
    val missingFilesCount: Int = 0,
    val totalWatchedMs: Long = 0L,
    val completedCount: Int = 0,
    val hdPlusCount: Int = 0
)

/**
 * Play-Store-safe offline Smart Library & Diagnostics enhancements hub (no AI or piracy modules).
 */
@Composable
internal fun ExtraFeaturesDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val smartCollections by AppSettings.extraSmartCollections.collectAsState()
    val watchStats by AppSettings.extraWatchStats.collectAsState()
    val diagnostics by AppSettings.extraPlaybackDiagnostics.collectAsState()
    val libraryHealth by AppSettings.extraLibraryHealth.collectAsState()
    val qualityFilters by AppSettings.extraQualityFilters.collectAsState()
    val activityLog by AppSettings.extraActivityLog.collectAsState()

    val anyEnabled = smartCollections || watchStats || diagnostics || libraryHealth || qualityFilters || activityLog
    val snapshot by produceState(ExtraFeaturesSnapshot(), anyEnabled, smartCollections, watchStats, libraryHealth, qualityFilters, activityLog) {
        if (!anyEnabled) {
            value = ExtraFeaturesSnapshot()
            return@produceState
        }
        value = withContext(Dispatchers.IO) {
            val history = runCatching { PlaybackHistoryBridge.loadHistory(context) }.getOrDefault(emptyList())
            val videos = runCatching { Library.videos(context) }.getOrDefault(emptyList())
            val missing = history.count { entry ->
                val uriStr = entry.uri.toString()
                uriStr.startsWith("file://") && !File(Uri.parse(uriStr).path.orEmpty()).exists()
            }
            val totalMs = history.sumOf { e -> if (e.played && e.durationMs > 0L) e.durationMs else e.positionMs }
            val completed = history.count { it.played }
            val hdCount = videos.count { v ->
                val n = v.name.lowercase(Locale.ROOT)
                n.contains("1080") || n.contains("2160") || n.contains("4k") || n.contains("720") || n.endsWith(".mkv")
            }
            ExtraFeaturesSnapshot(
                history = history,
                localVideosCount = videos.size,
                missingFilesCount = missing,
                totalWatchedMs = totalMs,
                completedCount = completed,
                hdPlusCount = hdCount
            )
        }
    }

    SettingsSubScreen(
        title = "Extra Features",
        subtitle = "Offline smart tools & library diagnostics (Play-safe)",
        onBack = onBack
    ) {
        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text(
                        "100% On-Device Enhancement Modules",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    "Local analytics and library health checks processed privately on your device without external servers.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            SettingsCard {
                SwitchPreferenceRow(
                    title = "Smart Collections",
                    subtitle = "Track Continue Watching & Recent queues automatically",
                    checked = smartCollections
                ) { AppSettings.setExtraSmartCollections(it) }
                if (smartCollections) {
                    ExtraFeaturePillStatus("${snapshot.history.size} tracked items • ${snapshot.localVideosCount} indexed local videos")
                }

                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow(
                    title = "Watch Statistics",
                    subtitle = "Summarize total watch time and completed episodes locally",
                    checked = watchStats
                ) { AppSettings.setExtraWatchStats(it) }
                if (watchStats) {
                    val mins = (snapshot.totalWatchedMs / 60_000L).coerceAtLeast(0L)
                    ExtraFeaturePillStatus("${mins / 60}h ${mins % 60}m watched • ${snapshot.completedCount} completed videos")
                }

                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow(
                    title = "Playback Diagnostics",
                    subtitle = "Enable detailed runtime codec, HDR, FPS & engine status strip",
                    checked = diagnostics
                ) {
                    AppSettings.setExtraPlaybackDiagnostics(it)
                    AppSettings.setShowHudTelemetry(it)
                }
                if (diagnostics) {
                    ExtraFeaturePillStatus("Runtime telemetry enabled for both MPV (libmpv) & ExoPlayer")
                }

                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow(
                    title = "Library Health Report",
                    subtitle = "Detect moved or missing file references inside Recents",
                    checked = libraryHealth
                ) { AppSettings.setExtraLibraryHealth(it) }
                if (libraryHealth) {
                    ExtraFeaturePillStatus(
                        text = if (snapshot.missingFilesCount == 0) "All recent file references healthy" else "${snapshot.missingFilesCount} missing local file reference(s) detected",
                        isWarning = snapshot.missingFilesCount > 0
                    )
                }

                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow(
                    title = "Media Quality Filters",
                    subtitle = "Highlight HD/4K and MKV/MP4 badges across Home & Folders",
                    checked = qualityFilters
                ) {
                    AppSettings.setExtraQualityFilters(it)
                    AppSettings.setShowResolutionBadge(it || AppSettings.showResolutionBadge.value)
                }
                if (qualityFilters) {
                    ExtraFeaturePillStatus("${snapshot.hdPlusCount} HD/MKV candidates found across ${snapshot.localVideosCount} videos")
                }

                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow(
                    title = "Library Activity Log",
                    subtitle = "View recent local playback activity log below",
                    checked = activityLog
                ) { AppSettings.setExtraActivityLog(it) }
                if (activityLog) {
                    if (snapshot.history.isEmpty()) {
                        ExtraFeaturePillStatus("No playback activity recorded yet")
                    } else {
                        snapshot.history.take(6).forEach { entry ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "• ${entry.title}",
                                    style = MaterialTheme.typography.bodySmall,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${(entry.progress * 100f).toInt()}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            GenuineExtraHubCards()
        }
    }
}

@Composable
private fun ExtraFeaturePillStatus(text: String, isWarning: Boolean = false) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isWarning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isWarning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

private enum class CodecTabFilter(val label: String) {
    ALL("All"),
    HARDWARE("Hardware"),
    SOFTWARE("Software"),
    VIDEO("Video"),
    AUDIO("Audio")
}

/**
 * Modern Compose Codec Explorer showcasing live Android MediaCodec HW/SW decoders & Core Codecs.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun SupportedCodecsDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val result by produceState<CodecCapabilitiesResultState>(CodecCapabilitiesResultState()) {
        value = withContext(Dispatchers.IO) {
            val inspected = CodecInspector.inspect(context)
            CodecCapabilitiesResultState(loaded = true, inspection = inspected)
        }
    }

    var searchQuery by remember { mutableStateOf("") }
    var activeFilter by remember { mutableStateOf(CodecTabFilter.ALL) }
    var expandedCodecName by remember { mutableStateOf<String?>(null) }

    val filteredCodecs = remember(result, searchQuery, activeFilter) {
        val list = result.inspection?.codecs ?: emptyList()
        list.filter { c ->
            val matchesFilter = when (activeFilter) {
                CodecTabFilter.ALL -> true
                CodecTabFilter.HARDWARE -> c.isHardware
                CodecTabFilter.SOFTWARE -> !c.isHardware
                CodecTabFilter.VIDEO -> c.isVideo
                CodecTabFilter.AUDIO -> !c.isVideo
            }
            val matchesSearch = searchQuery.isBlank() ||
                c.name.contains(searchQuery, ignoreCase = true) ||
                c.mimeType.contains(searchQuery, ignoreCase = true) ||
                c.formatName.contains(searchQuery, ignoreCase = true)
            matchesFilter && matchesSearch
        }
    }

    SettingsSubScreen(
        title = "Supported Codecs",
        subtitle = "Device MediaCodec hardware & software capabilities",
        onBack = onBack
    ) {
        result.inspection?.let { info ->
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CodecStatPill("${info.hardwareCount}", "HW", MaterialTheme.colorScheme.primary, Modifier.weight(1f))
                    CodecStatPill("${info.softwareCount}", "SW", MaterialTheme.colorScheme.secondary, Modifier.weight(1f))
                    CodecStatPill("${info.videoCount}", "Video", MaterialTheme.colorScheme.tertiary, Modifier.weight(1f))
                    CodecStatPill("${info.audioCount}", "Audio", Color(0xFF10B981), Modifier.weight(1f))
                }
            }

            item {
                SettingsCard {
                    Text("Core Codecs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        info.keySummaries.filter { it.hasHardware || it.hasSoftware }.forEach { summary ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (summary.hasHardware) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, MaterialTheme.extendedColors.cardBorder)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(5.dp)
                                ) {
                                    Text(
                                        text = summary.formatName,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = if (summary.hasHardware) "HW" else "SW",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (summary.hasHardware) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search codec (e.g. hevc, av1, opus, exynos)") },
                leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                singleLine = true,
                shape = CircleShape,
                modifier = Modifier.fillMaxWidth()
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                CodecTabFilter.entries.forEach { tab ->
                    FilterChip(
                        selected = activeFilter == tab,
                        onClick = { activeFilter = tab },
                        label = { Text(tab.label, fontSize = 11.sp) }
                    )
                }
            }
        }

        items(filteredCodecs, key = { "${it.name}_${it.mimeType}" }) { codec ->
            val expanded = expandedCodecName == "${codec.name}_${codec.mimeType}"
            CodecDetailCard(
                codec = codec,
                expanded = expanded,
                onClick = { expandedCodecName = if (expanded) null else "${codec.name}_${codec.mimeType}" }
            )
        }

        result.inspection?.let { info ->
            item {
                OutlinedButton(
                    onClick = {
                        val report = buildString {
                            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL} (Android ${Build.VERSION.RELEASE})")
                            appendLine("Total: ${info.totalCount} (HW=${info.hardwareCount}, SW=${info.softwareCount})")
                            info.codecs.forEach { c ->
                                appendLine("- ${c.name} [${if (c.isHardware) "HW" else "SW"} | ${c.mimeType}] max=${c.maxResolution ?: "-"}")
                            }
                        }
                        copyTextToClipboard(context, "MPVX Codec Capabilities", report)
                        Toast.makeText(context, "Codec report copied", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Icon(Icons.Rounded.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Copy Full Codec Diagnostics")
                }
            }
        }
    }
}

private data class CodecCapabilitiesResultState(
    val loaded: Boolean = false,
    val inspection: CodecInspector.InspectionResult? = null
)

@Composable
private fun CodecStatPill(count: String, label: String, color: Color, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = color.copy(alpha = 0.14f),
        border = BorderStroke(1.dp, color.copy(alpha = 0.35f))
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(count, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.ExtraBold, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CodecDetailCard(
    codec: CodecCapabilitiesInfo,
    expanded: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, MaterialTheme.extendedColors.cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(codec.formatName.ifBlank { codec.mimeType }, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleSmall)
                    Text(
                        text = codec.name,
                        style = MaterialTheme.typography.bodySmall.copy(fontFamily = FontFamily.Monospace),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (codec.isHardware) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerHigh
                ) {
                    Text(
                        text = if (codec.isHardware) "HW" else "SW",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (codec.isHardware) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
                Spacer(Modifier.width(4.dp))
                Icon(
                    imageVector = if (expanded) Icons.Rounded.ExpandLess else Icons.Rounded.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.padding(top = 6.dp)) {
                    HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                    Text("MIME: ${codec.mimeType}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    codec.maxResolution?.let { Text("Max Resolution: $it", style = MaterialTheme.typography.labelSmall) }
                    codec.bitrateRange?.let { Text("Bitrate Range: $it", style = MaterialTheme.typography.labelSmall) }
                    if (codec.isHdrSupported || codec.is10BitSupported) {
                        Text(
                            text = buildList {
                                if (codec.is10BitSupported) add("10-bit Color")
                                if (codec.isHdrSupported) add("HDR Capable")
                            }.joinToString(" • "),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (codec.profilesAndLevels.isNotEmpty()) {
                        Text(
                            text = "Profiles: ${codec.profilesAndLevels.take(5).joinToString(", ")}",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

/**
 * Upgraded About + Live Dual-Engine + System Hardware Specifications Screen.
 */
@Composable
internal fun AboutAndSystemDetailScreen(
    onOpenLicenses: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val useVulkan by AppSettings.useVulkan.collectAsState()
    val useGpuNext by AppSettings.useGpuNext.collectAsState()

    val sysInfo = remember(context) { collectSystemSpecs(context) }

    SettingsSubScreen(title = "About & System", subtitle = "Engines, hardware & open-source licenses", onBack = onBack) {
        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.Verified, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(30.dp))
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text("MPVX Player", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.ExtraBold)
                        Text(
                            "v${BuildConfig.VERSION_NAME} (Build ${BuildConfig.VERSION_CODE}) • Creative Mind Studio",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            BuildConfig.APPLICATION_ID,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text(
                    "Open-source dual-engine media player by Creative Mind Studio, powered by libmpv (mpv v0.41.0, LGPL-2.1 / GPL-2.0) and AndroidX Media3 ExoPlayer. Distributed under GNU GPL v3.0.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = onOpenLicenses,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.Description, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Open-Source Licenses")
                }
            }
        }

        item {
            SettingsCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Rounded.Memory, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Text("Engines & Rendering Pipeline", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                }
                SpecInfoRow("MPV Engine", "libmpv (mpv v0.41.0) • FFmpeg SW/HW")
                SpecInfoRow("Native Engine", "AndroidX Media3 1.10.0 ExoPlayer")
                SpecInfoRow("ASS/SSA Subtitles", "ass-media 0.5.1 (Canvas libass) + libass")
                SpecInfoRow("Video Output", if (useVulkan) "Vulkan (androidvk) w/ OpenGL fallback" else if (useGpuNext) "libplacebo gpu-next / OpenGL ES" else "OpenGL ES / SurfaceView")
                SpecInfoRow("Shaders", "Anime4K v4.x (22 MIT GLSL shaders)")
            }
        }

        item {
            SettingsCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Rounded.DeveloperBoard, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Text("System Specifications", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                    IconButton(onClick = {
                        val diag = buildString {
                            appendLine("MPVX Player • Creative Mind Studio v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
                            sysInfo.forEach { (k, v) -> appendLine("$k: $v") }
                        }
                        copyTextToClipboard(context, "MPVX System Specs", diag)
                        Toast.makeText(context, "System diagnostics copied", Toast.LENGTH_SHORT).show()
                    }) {
                        Icon(Icons.Rounded.ContentCopy, contentDescription = "Copy Specs", modifier = Modifier.size(18.dp))
                    }
                }
                sysInfo.forEach { (label, valText) ->
                    SpecInfoRow(label, valText)
                }
            }
        }
    }
}

@Composable
private fun SpecInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(120.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}

private fun collectSystemSpecs(context: Context): List<Pair<String, String>> {
    val am = context.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
    val memInfo = ActivityManager.MemoryInfo()
    am?.getMemoryInfo(memInfo)
    val ramGb = String.format(Locale.US, "%.1f GB", memInfo.totalMem / (1024.0 * 1024.0 * 1024.0))
    val kernel = runCatching {
        File("/proc/version").takeIf { it.canRead() }?.bufferedReader()?.readLine()?.take(72)
    }.getOrNull() ?: (System.getProperty("os.version") ?: "Linux")

    return listOf(
        "Device" to "${Build.MANUFACTURER} ${Build.MODEL}",
        "Android" to "${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
        "ABIs" to Build.SUPPORTED_ABIS.joinToString(", "),
        "Hardware" to "${Build.HARDWARE} / ${Build.BOARD}",
        "Total RAM" to ramGb,
        "Kernel" to kernel
    )
}

private fun copyTextToClipboard(context: Context, label: String, text: String) {
    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return
    cm.setPrimaryClip(ClipData.newPlainText(label, text))
}
