package com.cmstudio.player.ui.player

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.BatteryManager
import android.text.format.DateFormat
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.PlaylistPlay
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.BatteryStd
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.cmstudio.player.R
import com.cmstudio.player.player.IntroOutroSegment
import com.cmstudio.player.player.IntroSegmentKind
import kotlin.math.abs
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.cmstudio.player.ui.AppSettings
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Live clock, network type, and battery pill supporting System / 12H / 24H formats.
 */
@Composable
fun LiveTimeNetworkChip(
    glassFill: Color,
    glassBorder: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val clockFormatPref by AppSettings.playerClockFormat.collectAsState()
    var nowText by remember { mutableStateOf("") }
    var batteryPct by remember { mutableStateOf(100) }
    var netLabel by remember { mutableStateOf("") }

    LaunchedEffect(clockFormatPref) {
        while (true) {
            val pattern = when (clockFormatPref) {
                "12H" -> "h:mm a"
                "24H" -> "HH:mm"
                else -> if (DateFormat.is24HourFormat(context)) "HH:mm" else "h:mm a"
            }
            nowText = SimpleDateFormat(pattern, Locale.getDefault()).format(Date())
            batteryPct = queryBatteryPercent(context)
            netLabel = queryNetworkLabel(context)
            delay(15_000L)
        }
    }

    Surface(
        shape = CircleShape,
        color = glassFill,
        border = BorderStroke(1.dp, glassBorder),
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Text(
                text = nowText,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 1
            )
            if (netLabel.isNotBlank()) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = netLabel,
                    tint = accentColor,
                    modifier = Modifier.size(13.dp)
                )
            }
            Icon(
                imageVector = Icons.Rounded.BatteryStd,
                contentDescription = "Battery",
                tint = Color.White.copy(alpha = 0.85f),
                modifier = Modifier.size(13.dp)
            )
            Text(
                text = "$batteryPct%",
                style = MaterialTheme.typography.labelSmall,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 1
            )
        }
    }
}

private fun queryBatteryPercent(context: Context): Int {
    return try {
        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? BatteryManager ?: return 100
        bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
    } catch (_: Throwable) {
        100
    }
}

private fun queryNetworkLabel(context: Context): String {
    return try {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return ""
        val net = cm.activeNetwork ?: return ""
        val caps = cm.getNetworkCapabilities(net) ?: return ""
        when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> "Wi-Fi"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "Cell"
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> "ETH"
            else -> ""
        }
    } catch (_: Throwable) {
        ""
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChaptersBottomSheet(
    chapters: List<VideoChapterItem>,
    currentChapterIndex: Int,
    sheetColor: Color,
    accent: Color,
    onSelectChapter: (VideoChapterItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Text(
                text = "Chapters (${chapters.size})",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(10.dp))
            if (chapters.isEmpty()) {
                Text(
                    text = "No embedded Matroska chapters found in this file.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(vertical = 12.dp)
                )
            } else {
                chapters.forEachIndexed { idx, chap ->
                    val selected = idx == currentChapterIndex
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                        border = if (selected) BorderStroke(1.dp, accent) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onSelectChapter(chap)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 10.dp)) {
                                Text(
                                    text = chap.title,
                                    color = if (selected) accent else Color.White,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                if (chap.endMs > chap.startMs) {
                                    Text(
                                        text = "${formatSheetTime(chap.startMs)} – ${formatSheetTime(chap.endMs)}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.65f)
                                    )
                                }
                            }
                            Text(
                                text = formatSheetTime(chap.startMs),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (selected) accent else Color.White.copy(alpha = 0.8f)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoQualityBottomSheet(
    videoTrackOptions: List<PlayerTrackOption>,
    sheetColor: Color,
    accent: Color,
    onResetAuto: () -> Unit,
    onSelectTrack: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Text("Video Quality", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Select stream resolution or adaptive Auto quality", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(12.dp))

            val hasManual = videoTrackOptions.count { it.selected } == 1 && videoTrackOptions.size > 1
            OutlinedButton(
                onClick = {
                    onResetAuto()
                    onDismiss()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = if (!hasManual) "Auto (Adaptive) • Active" else "Reset to Auto (Adaptive)",
                    color = accent,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
            if (videoTrackOptions.isEmpty()) {
                Text("Single video stream active.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
            } else {
                videoTrackOptions.forEach { opt ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (opt.selected) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.06f),
                        border = if (opt.selected) BorderStroke(1.dp, accent) else null,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable {
                                onSelectTrack(opt.groupIndex, opt.trackIndex)
                                onDismiss()
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(opt.label, color = if (opt.selected) accent else Color.White, fontWeight = FontWeight.SemiBold)
                            if (opt.selected) {
                                Text("Active", style = MaterialTheme.typography.labelSmall, color = accent, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun SleepTimerBottomSheet(
    remainingMs: Long,
    untilEndOfVideo: Boolean,
    sheetColor: Color,
    accent: Color,
    onAccent: Color,
    onSetMinutes: (Int) -> Unit,
    onSetEndOfVideo: () -> Unit,
    onCancel: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sleep Timer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
                val statusLabel = when {
                    remainingMs > 0L -> formatSheetTime(remainingMs)
                    untilEndOfVideo -> "End of Video"
                    else -> "Off"
                }
                Text(statusLabel, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = accent)
            }
            Text("Gently fades audio and pauses playback when timer completes.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(14.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(10, 15, 30, 45, 60, 90).forEach { mins ->
                    FilterChip(
                        selected = false,
                        onClick = {
                            onSetMinutes(mins)
                            onDismiss()
                        },
                        label = { Text("$mins min") }
                    )
                }
                FilterChip(
                    selected = untilEndOfVideo,
                    onClick = {
                        onSetEndOfVideo()
                        onDismiss()
                    },
                    label = { Text("End of Video") }
                )
            }

            if (remainingMs > 0L || untilEndOfVideo) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {
                        onCancel()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Turn Off Sleep Timer")
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun EqualizerBottomSheet(
    sheetColor: Color,
    accent: Color,
    onSelectPreset: (String) -> Unit,
    onCustomBandsChange: (List<Float>) -> Unit,
    onAudioChannelModeChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val activePreset by AppSettings.eqPreset.collectAsState()
    val customCsv by AppSettings.eqCustomBandsDb.collectAsState()
    val channelMode by AppSettings.audioChannelMode.collectAsState()
    val currentBands = remember(activePreset, customCsv) {
        EqualizerPresetsCatalog.bandsForPreset(activePreset, customCsv)
    }
    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("5-Band Equalizer & Channels", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                TextButton(onClick = { onSelectPreset("Flat") }) {
                    Text("Reset Flat", color = accent)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                EqualizerPresetsCatalog.PRESETS.keys.forEach { preset ->
                    FilterChip(
                        selected = activePreset == preset,
                        onClick = { onSelectPreset(preset) },
                        label = { Text(preset) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            EqualizerPresetsCatalog.BAND_LABELS.forEachIndexed { idx, freq ->
                val bandDb = currentBands.getOrElse(idx) { 0f }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = freq,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.8f),
                        modifier = Modifier.width(46.dp)
                    )
                    Slider(
                        value = bandDb,
                        onValueChange = { v ->
                            val next = currentBands.toMutableList()
                            next[idx] = ((v * 2f).roundToInt() / 2f).coerceIn(-12f, 12f)
                            onCustomBandsChange(next)
                        },
                        valueRange = -12f..12f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent),
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = "${if (bandDb > 0f) "+" else ""}${bandDb.roundToInt()}dB",
                        style = MaterialTheme.typography.labelSmall,
                        color = accent,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.width(48.dp)
                    )
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.14f), modifier = Modifier.padding(vertical = 10.dp))
            Text("Audio Channels", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(8.dp))
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(
                    ChannelMixMath.MODE_AUTO to "Auto",
                    ChannelMixMath.MODE_MONO to "Mono",
                    ChannelMixMath.MODE_STEREO to "Stereo",
                    ChannelMixMath.MODE_REVERSE_STEREO to "Reverse Stereo"
                ).forEach { (id, label) ->
                    FilterChip(
                        selected = channelMode == id,
                        onClick = { onAudioChannelModeChange(id) },
                        label = { Text(label) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Interactive heard/seen subtitle sync helper + speed slider + dual subtitle + custom font controls
 * embedded cleanly inside the Subtitle bottom sheet.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SubtitleAdvancedControlsSection(
    accent: Color,
    secondarySubtitleTitle: String,
    onSubtitleDelayChange: (Int) -> Unit,
    onSubtitleSpeedChange: (Int) -> Unit,
    onRequestSecondarySubtitleFile: () -> Unit,
    onClearSecondarySubtitle: () -> Unit,
    onRequestCustomSubtitleFont: () -> Unit,
    onClearCustomSubtitleFont: () -> Unit
) {
    val speedMilli by AppSettings.subtitleSpeedMilli.collectAsState()
    val dualEnabled by AppSettings.dualSubtitles.collectAsState()
    val subItalic by AppSettings.subtitleItalic.collectAsState()
    val subAlign by AppSettings.subtitleAlignment.collectAsState()
    val customFontName by AppSettings.subtitleCustomFontName.collectAsState()
    val customFontUri by AppSettings.subtitleCustomFontUri.collectAsState()

    var heardTimestampMs by remember { mutableLongStateOf(0L) }
    var seenTimestampMs by remember { mutableLongStateOf(0L) }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Subtitle Speed", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = Color.White)
            Text(String.format(Locale.US, "%.2fx", speedMilli / 1000f), color = accent, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = speedMilli.toFloat(),
            onValueChange = { onSubtitleSpeedChange(((it / 50f).roundToInt() * 50).coerceIn(100, 10000)) },
            valueRange = 500f..2000f,
            colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf(900 to "0.9x", 1000 to "1.0x", 1050 to "1.05x", 1100 to "1.1x").forEach { (m, label) ->
                FilterChip(selected = speedMilli == m, onClick = { onSubtitleSpeedChange(m) }, label = { Text(label) })
            }
        }

        // Heard / Saw Sync Stopwatch
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = Color.White.copy(alpha = 0.08f),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tap-to-Sync Stopwatch", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { heardTimestampMs = android.os.SystemClock.elapsedRealtime() },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (heardTimestampMs > 0L) "Heard OK" else "1. Heard Voice") }
                    OutlinedButton(
                        onClick = { seenTimestampMs = android.os.SystemClock.elapsedRealtime() },
                        modifier = Modifier.weight(1f)
                    ) { Text(if (seenTimestampMs > 0L) "Saw Sub OK" else "2. Saw Text") }
                }
                if (heardTimestampMs > 0L && seenTimestampMs > 0L) {
                    val deltaMs = (heardTimestampMs - seenTimestampMs).toInt().coerceIn(-10000, 10000)
                    Button(
                        onClick = {
                            onSubtitleDelayChange(AppSettings.subtitleDelayMs.value + deltaMs)
                            heardTimestampMs = 0L
                            seenTimestampMs = 0L
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Apply Sync Shift (${if (deltaMs >= 0) "+" else ""}${deltaMs}ms)")
                    }
                }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Dual Subtitles", fontWeight = FontWeight.Bold, color = Color.White)
                Text(
                    text = if (secondarySubtitleTitle.isNotBlank()) "2nd: $secondarySubtitleTitle" else "Stack a secondary .srt/.vtt/.ass file",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.68f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Switch(checked = dualEnabled, onCheckedChange = { AppSettings.setDualSubtitles(it) })
        }
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onRequestSecondarySubtitleFile, modifier = Modifier.weight(1f)) {
                Text("Pick 2nd Subtitle")
            }
            if (secondarySubtitleTitle.isNotBlank()) {
                TextButton(onClick = onClearSecondarySubtitle) { Text("Clear") }
            }
        }

        HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("Italic Subtitles", color = Color.White, fontWeight = FontWeight.SemiBold)
            Switch(checked = subItalic, onCheckedChange = { AppSettings.setSubtitleItalic(it) })
        }
        Text("Alignment", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.8f))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("LEFT" to "Left", "CENTER" to "Center", "RIGHT" to "Right").forEach { (k, label) ->
                FilterChip(selected = subAlign == k, onClick = { AppSettings.setSubtitleAlignment(k) }, label = { Text(label) })
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Custom Font", color = Color.White, fontWeight = FontWeight.SemiBold)
                Text(customFontName, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
            }
            Row {
                OutlinedButton(onClick = onRequestCustomSubtitleFont) { Text("Load .ttf") }
                if (customFontUri.isNotBlank()) {
                    TextButton(onClick = onClearCustomSubtitleFont) { Text("Reset") }
                }
            }
        }
    }
}

@Composable
fun PlayerRightEdgeDrawer(
    visible: Boolean,
    glassFill: Color,
    accent: Color,
    isMirrored: Boolean,
    isFlippedVertical: Boolean,
    isShuffleEnabled: Boolean,
    customSkipSec: Int,
    sleepTimerRemainingMs: Long,
    onDismiss: () -> Unit,
    onOpenChapters: () -> Unit,
    onOpenQuality: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenSleepTimer: () -> Unit,
    onToggleMirror: () -> Unit,
    onToggleVerticalFlip: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleAmbient: () -> Unit,
    onToggleBackgroundPlay: () -> Unit,
    onCustomSkipForward: () -> Unit,
    activeEngineLabel: String = "Media3",
    aspectRatioMode: String = "Fit",
    currentSpeed: Float = 1.0f,
    repeatMode: String = "OFF",
    onOpenEngine: () -> Unit = {},
    onOpenAspect: () -> Unit = {},
    onOpenSpeed: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onCaptureScreenshot: () -> Unit = {},
    onOpenPip: () -> Unit = {}
) {
    val ambientEnabled by AppSettings.ambientModeEnabled.collectAsState()
    val bgEnabled by AppSettings.backgroundPlayback.collectAsState()
    val anime4kMode by AppSettings.anime4kMode.collectAsState()
    val mpvDeband by AppSettings.mpvDebanding.collectAsState()
    val showRotOv by AppSettings.showScreenRotationOverlay.collectAsState()
    val showAspectOv by AppSettings.showAspectRatioOverlay.collectAsState()
    val showSpeedOv by AppSettings.showPlaybackSpeedOverlay.collectAsState()
    val showDecoderOv by AppSettings.showDecoderOverlay.collectAsState()
    val showShuffleOv by AppSettings.showShuffleOverlay.collectAsState()
    val showLoopOv by AppSettings.showLoopOverlay.collectAsState()
    val showPipOv by AppSettings.showPipOverlay.collectAsState()
    val showShotOv by AppSettings.showScreenshotOverlay.collectAsState()

    AnimatedVisibility(
        visible = visible,
        enter = slideInHorizontally(initialOffsetX = { it }),
        exit = slideOutHorizontally(targetOffsetX = { it }),
        modifier = Modifier.fillMaxHeight()
    ) {
        Box(modifier = Modifier.fillMaxWidth().fillMaxHeight()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .background(Color.Black.copy(alpha = 0.34f))
                    .clickable(onClick = onDismiss)
            )
            Surface(
                shape = RoundedCornerShape(topStart = 24.dp, bottomStart = 24.dp),
                color = glassFill.copy(alpha = 0.94f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f)),
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .widthIn(min = 246.dp, max = 304.dp)
                    .fillMaxHeight()
                    .padding(vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Quick Control Drawer", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = Color.White)
                        IconButton(onClick = onDismiss, modifier = Modifier.size(30.dp)) {
                            Icon(Icons.Rounded.Close, contentDescription = "Close", tint = Color.White, modifier = Modifier.size(18.dp))
                        }
                    }
                    DrawerActionRow(
                        Icons.Rounded.Memory,
                        "Engine ($activeEngineLabel)",
                        activeEngineLabel.contains("MPV", ignoreCase = true),
                        accent
                    ) { onDismiss(); onOpenEngine() }
                    if (activeEngineLabel.contains("MPV", ignoreCase = true)) {
                        DrawerActionRow(
                            Icons.Rounded.AutoAwesome,
                            "Anime4K (${anime4kMode})",
                            anime4kMode != "OFF",
                            accent
                        ) {
                            val next = when (anime4kMode) {
                                "OFF" -> "MODE_A"
                                "MODE_A" -> "MODE_B"
                                "MODE_B" -> "MODE_C"
                                else -> "OFF"
                            }
                            AppSettings.setAnime4kMode(next)
                        }
                        DrawerActionRow(
                            Icons.Rounded.HighQuality,
                            "Debanding (${mpvDeband})",
                            mpvDeband != "NONE",
                            accent
                        ) {
                            val next = when (mpvDeband) {
                                "NONE" -> "GPU"
                                "GPU" -> "CPU"
                                else -> "NONE"
                            }
                            AppSettings.setMpvDebanding(next)
                        }
                    }
                    DrawerActionRow(Icons.Rounded.AspectRatio, "Aspect Ratio ($aspectRatioMode)", aspectRatioMode != "Fit", accent) { onDismiss(); onOpenAspect() }
                    DrawerActionRow(Icons.Rounded.Speed, String.format(Locale.US, "Speed (%.2gx)", currentSpeed), abs(currentSpeed - 1.0f) > 0.02f, accent) { onDismiss(); onOpenSpeed() }
                    DrawerActionRow(Icons.Rounded.Repeat, "Repeat ($repeatMode)", repeatMode != "OFF", accent, onCycleRepeat)
                    DrawerActionRow(Icons.Rounded.CameraAlt, "Capture Screenshot", false, accent) { onDismiss(); onCaptureScreenshot() }
                    DrawerActionRow(Icons.Rounded.PictureInPictureAlt, "Picture-in-Picture", false, accent) { onDismiss(); onOpenPip() }
                    DrawerActionRow(Icons.Rounded.Bookmarks, "Chapters", false, accent) { onDismiss(); onOpenChapters() }
                    DrawerActionRow(Icons.Rounded.HighQuality, "Video Quality", false, accent) { onDismiss(); onOpenQuality() }
                    DrawerActionRow(Icons.Rounded.GraphicEq, "5-Band Equalizer", false, accent) { onDismiss(); onOpenEqualizer() }
                    DrawerActionRow(
                        Icons.Rounded.Timer,
                        if (sleepTimerRemainingMs > 0L) "Sleep (${formatSheetTime(sleepTimerRemainingMs)})" else "Sleep Timer",
                        sleepTimerRemainingMs > 0L,
                        accent
                    ) { onDismiss(); onOpenSleepTimer() }
                    DrawerActionRow(Icons.Rounded.FastForward, "Custom Skip (+${customSkipSec}s)", false, accent) { onCustomSkipForward() }
                    DrawerActionRow(Icons.Rounded.Flip, "Mirror Horizontal", isMirrored, accent, onToggleMirror)
                    DrawerActionRow(Icons.Rounded.FlipCameraAndroid, "Vertical Flip", isFlippedVertical, accent, onToggleVerticalFlip)
                    DrawerActionRow(Icons.Rounded.Shuffle, "Queue Shuffle", isShuffleEnabled, accent, onToggleShuffle)
                    DrawerActionRow(Icons.Rounded.AutoAwesome, "Ambient Aura", ambientEnabled, accent, onToggleAmbient)
                    DrawerActionRow(Icons.Rounded.Headphones, "Background Audio", bgEnabled, accent, onToggleBackgroundPlay)

                    HorizontalDivider(color = Color.White.copy(alpha = 0.14f), modifier = Modifier.padding(vertical = 4.dp))
                    Text(
                        "HUD Overlay Buttons",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = accent
                    )
                    DrawerToggleRow("Screen Rotation", showRotOv, accent) { AppSettings.setShowScreenRotationOverlay(it) }
                    DrawerToggleRow("Aspect Ratio", showAspectOv, accent) { AppSettings.setShowAspectRatioOverlay(it) }
                    DrawerToggleRow("Playback Speed", showSpeedOv, accent) { AppSettings.setShowPlaybackSpeedOverlay(it) }
                    DrawerToggleRow("Decoder Button", showDecoderOv, accent) { AppSettings.setShowDecoderOverlay(it) }
                    DrawerToggleRow("Shuffle Button", showShuffleOv, accent) { AppSettings.setShowShuffleOverlay(it) }
                    DrawerToggleRow("Loop Button", showLoopOv, accent) { AppSettings.setShowLoopOverlay(it) }
                    DrawerToggleRow("PiP Button", showPipOv, accent) { AppSettings.setShowPipOverlay(it) }
                    DrawerToggleRow("Screenshot Button", showShotOv, accent) { AppSettings.setShowScreenshotOverlay(it) }
                }
            }
        }
    }
}

@Composable
private fun DrawerToggleRow(
    label: String,
    checked: Boolean,
    accent: Color,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable { onCheckedChange(!checked) }
            .padding(horizontal = 8.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.9f))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedTrackColor = accent)
        )
    }
}

@Composable
private fun DrawerActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    active: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (active) accent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
        border = if (active) BorderStroke(1.dp, accent) else null,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = if (active) accent else Color.White, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(10.dp))
            Text(label, style = MaterialTheme.typography.labelLarge, color = if (active) accent else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun formatSheetTime(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val h = totalSec / 3600L
    val m = (totalSec % 3600L) / 60L
    val s = totalSec % 60L
    return if (h > 0L) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
    else String.format(Locale.US, "%02d:%02d", m, s)
}

/**
 * Shared Seekbar Canvas supporting all 6 styles (`NORMAL`, `STANDARD`, `WAVY`, `THICK`, `SLIM`,
 * `EXPRESSIVE`), buffered track, chapter boundary notches, and A-B loop yellow pins/band.
 * Reused by both live `VideoPlayerScreen` and live animated `SettingsScreen` previews.
 */
@Composable
fun MpvxSeekbarCanvas(
    seekbarStyle: String,
    progressFraction: Float,
    bufferedFraction: Float,
    durationMs: Long,
    accentColor: Color,
    wavePhase: Float,
    modifier: Modifier = Modifier,
    chapters: List<VideoChapterItem> = emptyList(),
    introOutroSegments: List<IntroOutroSegment> = emptyList(),
    abLoopStartMs: Long = -1L,
    abLoopEndMs: Long = -1L,
    abLoopActive: Boolean = false,
    isScrubbing: Boolean = false
) {
    val scrubBreathScale by animateFloatAsState(
        targetValue = if (isScrubbing) 1.55f else 1.0f,
        animationSpec = tween(durationMillis = 160),
        label = "seekbarScrubBreath"
    )
    val trackColor = when (seekbarStyle.uppercase()) {
        "STANDARD", "WAVY" -> accentColor
        else -> Color.White
    }
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        if (w <= 1f) return@Canvas
        val centerY = h / 2f
        val normStyle = seekbarStyle.uppercase()
        val pFrac = progressFraction.coerceIn(0f, 1f)
        val bFrac = bufferedFraction.coerceIn(pFrac, 1f)
        val activeX = w * pFrac

        // Custom styles added beyond core 6 (share chapter notches & A-B overlay cleanly)
        if (normStyle == "SEGMENTED" || normStyle == "DUAL_GLOW" || normStyle == "PULSE_BAR") {
            when (normStyle) {
                "SEGMENTED" -> {
                    val segCount = 24
                    val gap = 3.dp.toPx()
                    val segW = ((w - gap * (segCount - 1)) / segCount).coerceAtLeast(2f)
                    val segH = (if (isScrubbing) 8.dp else 5.5.dp).toPx()
                    for (i in 0 until segCount) {
                        val segStart = i * (segW + gap)
                        val segFrac = (i + 0.5f) / segCount.toFloat()
                        val c = when {
                            segFrac <= pFrac -> accentColor
                            segFrac <= bFrac -> Color.White.copy(alpha = 0.44f)
                            else -> Color.White.copy(alpha = 0.18f)
                        }
                        drawRoundRect(
                            color = c,
                            topLeft = Offset(segStart, centerY - segH / 2f),
                            size = androidx.compose.ui.geometry.Size(segW, segH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(segH / 2f, segH / 2f)
                        )
                    }
                }
                "DUAL_GLOW" -> {
                    val coreW = (if (isScrubbing) 6.dp else 4.5.dp).toPx()
                    drawLine(
                        color = Color.White.copy(alpha = 0.22f),
                        start = Offset(0f, centerY),
                        end = Offset(w, centerY),
                        strokeWidth = coreW,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    if (bFrac > pFrac) {
                        drawLine(
                            color = accentColor.copy(alpha = 0.35f),
                            start = Offset(activeX, centerY),
                            end = Offset(w * bFrac, centerY),
                            strokeWidth = coreW,
                            cap = androidx.compose.ui.graphics.StrokeCap.Round
                        )
                    }
                    // Outer neon halo + bright core
                    drawLine(
                        color = accentColor.copy(alpha = 0.32f),
                        start = Offset(0f, centerY),
                        end = Offset(activeX, centerY),
                        strokeWidth = coreW * 2.6f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                    drawLine(
                        color = Color.White,
                        start = Offset(0f, centerY),
                        end = Offset(activeX, centerY),
                        strokeWidth = coreW * 0.85f,
                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                    )
                }
                else -> { // PULSE_BAR
                    val barCount = 42
                    val step = w / barCount.toFloat()
                    val barW = (step * 0.56f).coerceAtLeast(2f)
                    for (i in 0 until barCount) {
                        val frac = i / barCount.toFloat()
                        val amp = 0.32f + 0.65f * kotlin.math.abs(kotlin.math.sin(frac * 14f + wavePhase * 0.45f))
                        val barH = (h * 0.72f * amp).coerceIn(4f, h * 0.85f)
                        val x = i * step + (step - barW) / 2f
                        val c = when {
                            frac <= pFrac -> accentColor
                            frac <= bFrac -> Color.White.copy(alpha = 0.42f)
                            else -> Color.White.copy(alpha = 0.20f)
                        }
                        drawRoundRect(
                            color = c,
                            topLeft = Offset(x, centerY - barH / 2f),
                            size = androidx.compose.ui.geometry.Size(barW, barH),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(barW / 2f, barW / 2f)
                        )
                    }
                }
            }
            if (abLoopStartMs >= 0L && durationMs > 0L) {
                val ax = w * (abLoopStartMs.toFloat() / durationMs).coerceIn(0f, 1f)
                drawCircle(Color(0xFFFACC15), radius = 4.dp.toPx(), center = Offset(ax, centerY))
                if (abLoopActive && abLoopEndMs > abLoopStartMs) {
                    val bx = w * (abLoopEndMs.toFloat() / durationMs).coerceIn(0f, 1f)
                    drawLine(Color(0x66FACC15), Offset(ax, centerY), Offset(bx, centerY), strokeWidth = 8.dp.toPx())
                    drawCircle(Color(0xFFFACC15), radius = 4.dp.toPx(), center = Offset(bx, centerY))
                }
            }
            if (durationMs > 0L && chapters.size > 1) {
                chapters.drop(1).forEach { ch ->
                    val cx = w * (ch.startMs.toFloat() / durationMs).coerceIn(0f, 1f)
                    drawLine(Color.White.copy(alpha = 0.75f), Offset(cx, centerY - 5.dp.toPx()), Offset(cx, centerY + 5.dp.toPx()), strokeWidth = 1.5.dp.toPx())
                }
            }
            if (durationMs > 0L && introOutroSegments.isNotEmpty()) {
                introOutroSegments.forEach { seg ->
                    val sx = (seg.startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * w
                    val ex = (seg.endMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * w
                    if (ex > sx + 2f) {
                        val segColor = when (seg.kind) {
                            IntroSegmentKind.OUTRO, IntroSegmentKind.PREVIEW -> Color(0xFF38BDF8)
                            else -> Color(0xFF10B981)
                        }
                        drawLine(
                            color = segColor.copy(alpha = 0.85f),
                            start = Offset(sx, centerY - 6.dp.toPx()),
                            end = Offset(ex, centerY - 6.dp.toPx()),
                            strokeWidth = 3.dp.toPx(),
                            cap = StrokeCap.Round
                        )
                    }
                }
            }
            val thumbR = (if (isScrubbing) 8.dp else 6.dp).toPx()
            if (normStyle == "DUAL_GLOW") {
                drawCircle(accentColor.copy(alpha = 0.35f), radius = thumbR * 1.6f, center = Offset(activeX, centerY))
            }
            drawCircle(Color.White, radius = thumbR, center = Offset(activeX, centerY))
            drawCircle(accentColor, radius = thumbR * 0.55f, center = Offset(activeX, centerY))
            return@Canvas
        }

        val strokeW = when (normStyle) {
            "THICK" -> 9.dp.toPx()
            "SLIM" -> 2.dp.toPx()
            "STANDARD" -> 3.5.dp.toPx()
            "NORMAL" -> 4.5.dp.toPx()
            "WAVY" -> 5.dp.toPx()
            else -> 6.dp.toPx() // EXPRESSIVE
        } * scrubBreathScale

        // Background track
        drawLine(
            color = Color.White.copy(alpha = 0.22f),
            start = Offset(0f, centerY),
            end = Offset(w, centerY),
            strokeWidth = strokeW,
            cap = StrokeCap.Round
        )

        // Buffered track
        if (bufferedFraction > progressFraction) {
            drawLine(
                color = Color.White.copy(alpha = 0.42f),
                start = Offset(0f, centerY),
                end = Offset(w * bufferedFraction.coerceIn(0f, 1f), centerY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        // A-B Loop yellow range overlay
        if (durationMs > 0L && abLoopStartMs >= 0L) {
            val aX = (abLoopStartMs.toFloat() / durationMs).coerceIn(0f, 1f) * w
            val bX = if (abLoopEndMs > abLoopStartMs) {
                (abLoopEndMs.toFloat() / durationMs).coerceIn(0f, 1f) * w
            } else aX
            if (bX > aX + 2f) {
                drawLine(
                    color = Color(0xFFFACC15).copy(alpha = if (abLoopActive) 0.9f else 0.45f),
                    start = Offset(aX, centerY),
                    end = Offset(bX, centerY),
                    strokeWidth = strokeW + 3.dp.toPx(),
                    cap = StrokeCap.Round
                )
            }
            drawCircle(color = Color(0xFFFACC15), radius = 4.5.dp.toPx(), center = Offset(aX, centerY - 9.dp.toPx()))
            if (abLoopEndMs > abLoopStartMs) {
                drawCircle(color = Color(0xFFFACC15), radius = 4.5.dp.toPx(), center = Offset(bX, centerY - 9.dp.toPx()))
            }
        }

        // Active elapsed path (Sine wave for WAVY / EXPRESSIVE)
        val activeEndX = (w * progressFraction.coerceIn(0f, 1f)).coerceAtLeast(1f)
        val upperStyle = seekbarStyle.uppercase()
        if ((upperStyle == "WAVY" || upperStyle == "EXPRESSIVE") && activeEndX > 4f) {
            val path = Path()
            val amp = if (upperStyle == "WAVY") 3.6.dp.toPx() else 2.2.dp.toPx()
            val freq = 0.085f
            path.moveTo(0f, centerY)
            var x = 0f
            while (x <= activeEndX) {
                val y = centerY + kotlin.math.sin((x * freq) + wavePhase) * amp
                path.lineTo(x, y)
                x += 3f
            }
            drawPath(
                path = path,
                color = trackColor,
                style = Stroke(width = strokeW, cap = StrokeCap.Round)
            )
        } else {
            drawLine(
                color = trackColor,
                start = Offset(0f, centerY),
                end = Offset(activeEndX, centerY),
                strokeWidth = strokeW,
                cap = StrokeCap.Round
            )
        }

        // Chapter notches along timeline
        if (durationMs > 0L && chapters.isNotEmpty()) {
            chapters.forEach { chap ->
                if (chap.startMs in 1000L until durationMs) {
                    val cx = (chap.startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * w
                    drawLine(
                        color = Color(0xFF0A0F14),
                        start = Offset(cx, centerY - strokeW * 0.7f),
                        end = Offset(cx, centerY + strokeW * 0.7f),
                        strokeWidth = 2.dp.toPx()
                    )
                }
            }
        }

        // Intro / Outro marker bars slightly above track so they don't clash with playback progress
        if (durationMs > 0L && introOutroSegments.isNotEmpty()) {
            introOutroSegments.forEach { seg ->
                val sx = (seg.startMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * w
                val ex = (seg.endMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) * w
                if (ex > sx + 2f) {
                    val segColor = when (seg.kind) {
                        IntroSegmentKind.OUTRO, IntroSegmentKind.PREVIEW -> Color(0xFF38BDF8)
                        else -> Color(0xFF10B981)
                    }
                    drawLine(
                        color = segColor.copy(alpha = 0.85f),
                        start = Offset(sx, centerY - strokeW * 0.85f - 2.dp.toPx()),
                        end = Offset(ex, centerY - strokeW * 0.85f - 2.dp.toPx()),
                        strokeWidth = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                }
            }
        }

        // Playhead thumb
        val thumbRadius = when (upperStyle) {
            "SLIM" -> 5.dp.toPx()
            "STANDARD" -> 6.dp.toPx()
            "THICK" -> 9.dp.toPx()
            else -> 7.5.dp.toPx()
        } * scrubBreathScale
        drawCircle(
            color = Color.White,
            radius = thumbRadius,
            center = Offset(activeEndX, centerY)
        )
    }
}

@Composable
fun RenderHudActionToken(
    token: String,
    btnSizeDp: Dp,
    btnIconSizeDp: Dp,
    glassFill: Color,
    glassBorder: Color,
    accentColor: Color,
    decoderPriority: String,
    isHwDecoder: Boolean,
    activeDecoderName: String,
    subtitlesDisabled: Boolean,
    orientationMode: String,
    currentSpeed: Float,
    repeatMode: String,
    abLoopActive: Boolean,
    zoomScale: Float,
    aspectRatioMode: String,
    isMirrored: Boolean,
    isFlippedVertical: Boolean,
    isShuffleEnabled: Boolean,
    ambientEnabled: Boolean,
    bgPlaybackEnabled: Boolean,
    customSkipSec: Int,
    sleepTimerRemainingMs: Long,
    chapters: List<VideoChapterItem>,
    currentChapterIndex: Int,
    onAction: () -> Unit
) {
    when (PlayerControlCatalog.canonicalToken(token)) {
        "NONE" -> Spacer(modifier = Modifier.width(btnSizeDp * 0.5f))
        "TIME_NETWORK" -> LiveTimeNetworkChip(
            glassFill = glassFill,
            glassBorder = glassBorder,
            accentColor = accentColor
        )
        "DECODER" -> {
            val label = when (decoderPriority) {
                "HW_ONLY" -> "HW"
                "SW_PREFER" -> "SW"
                else -> if (activeDecoderName.isNotBlank()) (if (isHwDecoder) "HW+" else "SW") else "Auto"
            }
            HudPillButton(label, Color.White, btnSizeDp, glassFill, glassBorder, onAction)
        }
        "PLAYBACK_SPEED" -> {
            val active = abs(currentSpeed - 1.0f) > 0.02f
            HudPillButton(
                text = String.format(Locale.US, "%.2gx", currentSpeed),
                tint = if (active) accentColor else Color.White,
                height = btnSizeDp,
                glassFill = glassFill,
                glassBorder = glassBorder,
                onClick = onAction
            )
        }
        "CUSTOM_SKIP" -> HudPillButton(
            text = "+${customSkipSec}s",
            tint = accentColor,
            height = btnSizeDp,
            glassFill = glassFill,
            glassBorder = glassBorder,
            onClick = onAction
        )
        "CURRENT_CHAPTER" -> {
            val ch = chapters.getOrNull(currentChapterIndex)?.title ?: "Chapters"
            HudPillButton(
                text = ch.take(14),
                tint = if (currentChapterIndex >= 0) accentColor else Color.White,
                height = btnSizeDp,
                glassFill = glassFill,
                glassBorder = glassBorder,
                onClick = onAction
            )
        }
        "SCREEN_ROTATION" -> Surface(
            onClick = onAction,
            shape = CircleShape,
            color = glassFill,
            border = BorderStroke(1.dp, glassBorder),
            modifier = Modifier.size(btnSizeDp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(
                        when {
                            orientationMode.contains("Landscape") -> R.drawable.ic_screen_lock_landscape_24dp
                            orientationMode.contains("Portrait") -> R.drawable.ic_screen_lock_portrait_24dp
                            else -> R.drawable.ic_auto_rotate_24dp
                        }
                    ),
                    contentDescription = "Orientation: $orientationMode",
                    tint = if (orientationMode != "Auto-Rotate" && orientationMode != "Auto") accentColor else Color.White,
                    modifier = Modifier.size(btnIconSizeDp)
                )
            }
        }
        "AUDIO_TRACK" -> HudTokenIconBtn(Icons.Rounded.Audiotrack, "Audio", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, Color.White, onAction)
        "SUBTITLES" -> HudTokenIconBtn(Icons.Rounded.Subtitles, "Subtitles", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, if (!subtitlesDisabled) accentColor else Color.White, onAction)
        "MORE_OPTIONS" -> HudTokenIconBtn(Icons.Rounded.MoreVert, "More Tools", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, Color.White, onAction)
        "LOCK_CONTROLS" -> HudTokenIconBtn(Icons.Rounded.Lock, "Lock Controls", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, Color.White, onAction)
        "REPEAT_MODE" -> HudTokenIconBtn(
            if (repeatMode == "ONE") Icons.Rounded.RepeatOne else Icons.Rounded.Repeat,
            "Repeat", btnSizeDp, btnIconSizeDp, glassFill, glassBorder,
            if (repeatMode != "OFF") accentColor else Color.White, onAction
        )
        "AB_LOOP" -> HudTokenIconBtn(Icons.Rounded.Loop, "A-B Loop", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, if (abLoopActive) accentColor else Color.White, onAction)
        "FRAME_NAVIGATION" -> HudTokenIconBtn(Icons.Rounded.CameraAlt, "Screenshot & Frame", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, Color.White, onAction)
        "ASPECT_RATIO" -> HudTokenIconBtn(Icons.Rounded.AspectRatio, "Aspect Ratio", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, if (aspectRatioMode != "Fit") accentColor else Color.White, onAction)
        "VIDEO_ZOOM" -> HudTokenIconBtn(Icons.Rounded.ZoomIn, "Zoom & Pan", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, if (abs(zoomScale - 1.0f) > 0.04f) accentColor else Color.White, onAction)
        "PICTURE_IN_PICTURE" -> HudTokenIconBtn(Icons.Rounded.PictureInPictureAlt, "Picture in Picture", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, Color.White, onAction)
        "BOOKMARKS_CHAPTERS" -> HudTokenIconBtn(Icons.Rounded.Bookmarks, "Chapters", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, if (chapters.isNotEmpty()) accentColor else Color.White, onAction)
        "VIDEO_QUALITY" -> HudTokenIconBtn(Icons.Rounded.HighQuality, "Video Quality", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, Color.White, onAction)
        "EQUALIZER" -> HudTokenIconBtn(Icons.Rounded.GraphicEq, "Equalizer", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, accentColor, onAction)
        "SHUFFLE" -> HudTokenIconBtn(Icons.Rounded.Shuffle, "Shuffle", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, if (isShuffleEnabled) accentColor else Color.White, onAction)
        "MIRROR" -> HudTokenIconBtn(Icons.Rounded.Flip, "Mirror", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, if (isMirrored) accentColor else Color.White, onAction)
        "VERTICAL_FLIP" -> HudTokenIconBtn(Icons.Rounded.FlipCameraAndroid, "Vertical Flip", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, if (isFlippedVertical) accentColor else Color.White, onAction)
        "AMBIENT_MODE" -> HudTokenIconBtn(Icons.Rounded.AutoAwesome, "Ambient Aura", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, if (ambientEnabled) accentColor else Color.White, onAction)
        "BACKGROUND_PLAYBACK" -> HudTokenIconBtn(Icons.Rounded.Headphones, "Background Audio", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, if (bgPlaybackEnabled) accentColor else Color.White, onAction)
        "SLEEP_TIMER" -> HudTokenIconBtn(Icons.Rounded.Bedtime, "Sleep Timer", btnSizeDp, btnIconSizeDp, glassFill, glassBorder, if (sleepTimerRemainingMs > 0L) accentColor else Color.White, onAction)
    }
}

@Composable
private fun HudPillButton(
    text: String,
    tint: Color,
    height: Dp,
    glassFill: Color,
    glassBorder: Color,
    onClick: () -> Unit
) {
    val isFrameless = glassFill == Color.Transparent
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = if (isFrameless) Color.Black.copy(alpha = 0.28f) else glassFill,
        border = BorderStroke(1.dp, if (isFrameless) Color.White.copy(alpha = 0.30f) else glassBorder),
        modifier = Modifier.height(height)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 10.dp)) {
            Text(text = text, style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = tint, maxLines = 1)
        }
    }
}

@Composable
private fun HudTokenIconBtn(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    iconSize: Dp,
    glassFill: Color,
    glassBorder: Color,
    tint: Color,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = glassFill,
        border = BorderStroke(1.dp, glassBorder),
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = contentDescription, tint = tint, modifier = Modifier.size(iconSize))
        }
    }
}


