package com.cmstudio.player.ui.player

import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Code
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Translate
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.SheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.cmstudio.player.player.IntroOutroManager
import com.cmstudio.player.subtitle.OnlineMediaMatch
import com.cmstudio.player.subtitle.OnlineSubtitleClient
import com.cmstudio.player.subtitle.OnlineSubtitleResult
import com.cmstudio.player.subtitle.OnlineSubtitleSearchOutcome
import com.cmstudio.player.ui.AppSettings
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Clean frosted Subtitles sheet featuring external track picker, live online search trigger,
 * embedded translation switch, P/S dual-subtitle selectors, appearance styling & sync delay.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostedSubtitlesSheet(
    sheetState: SheetState,
    sheetColor: Color,
    accent: Color,
    onAccent: Color,
    subtitleTrackOptions: List<PlayerTrackOption>,
    subtitlesDisabled: Boolean,
    secondarySubtitleTitle: String,
    subtitleDelayMs: Int,
    subtitleFontSize: Float,
    subtitleBgOpacity: Float,
    subtitleBottomPaddingDp: Int,
    subtitleEdgeType: String,
    subtitleBold: Boolean,
    onSelectSubtitleTrack: (Int, Int) -> Unit,
    onSelectSecondarySubtitleTrack: (PlayerTrackOption) -> Unit,
    onDisableSubtitles: () -> Unit,
    onRequestSubtitleFile: () -> Unit,
    onOpenOnlineSearch: () -> Unit,
    onSubtitleDelayChange: (Int) -> Unit,
    onSubtitleSpeedChange: (Int) -> Unit,
    onRequestSecondarySubtitleFile: () -> Unit,
    onClearSecondarySubtitle: () -> Unit,
    onRequestCustomSubtitleFont: () -> Unit,
    onClearCustomSubtitleFont: () -> Unit,
    onDismiss: () -> Unit
) {
    var activeTab by remember { mutableIntStateOf(0) } // 0: Tracks, 1: Appearance, 2: Sync/Timing
    val translateEnabled by AppSettings.translateSubtitlesEnabled.collectAsState()
    val translateLang by AppSettings.translateSubtitlesLang.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top bar: + Add external subtitles + quick action icons (Search, Tracks, Style, Sync)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            onRequestSubtitleFile()
                        }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add external subtitles",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        onDismiss()
                        onOpenOnlineSearch()
                    }, modifier = Modifier.size(38.dp)) {
                        Icon(Icons.Rounded.Search, contentDescription = "Search Online Subtitles", tint = accent, modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = { activeTab = 0 }, modifier = Modifier.size(38.dp)) {
                        Icon(
                            Icons.Rounded.Subtitles,
                            contentDescription = "Subtitle Tracks",
                            tint = if (activeTab == 0) accent else Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { activeTab = 1 }, modifier = Modifier.size(38.dp)) {
                        Icon(
                            Icons.Rounded.Palette,
                            contentDescription = "Subtitle Style",
                            tint = if (activeTab == 1) accent else Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = { activeTab = 2 }, modifier = Modifier.size(38.dp)) {
                        Icon(
                            Icons.Rounded.Timer,
                            contentDescription = "Subtitle Timing",
                            tint = if (activeTab == 2) accent else Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            when (activeTab) {
                0 -> {
                    // Translate subtitles row + quick language selector
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Translate, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Translate embedded subtitles",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = Color.White
                            )
                        }
                        Switch(
                            checked = translateEnabled,
                            onCheckedChange = { AppSettings.setTranslateSubtitlesEnabled(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = onAccent, checkedTrackColor = accent)
                        )
                    }

                    if (translateEnabled) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            OnlineSubtitleClient.TRANSLATION_LANGUAGES.forEach { (code, name) ->
                                FilterChip(
                                    selected = translateLang.equals(code, ignoreCase = true),
                                    onClick = { AppSettings.setTranslateSubtitlesLang(code) },
                                    label = { Text(name, fontSize = 11.sp) },
                                    colors = FilterChipDefaults.filterChipColors(
                                        selectedContainerColor = accent,
                                        selectedLabelColor = onAccent,
                                        containerColor = Color.White.copy(alpha = 0.08f),
                                        labelColor = Color.White.copy(alpha = 0.85f)
                                    )
                                )
                            }
                        }
                    }

                    // Off row
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { onDisableSubtitles() }
                            .padding(horizontal = 6.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (subtitlesDisabled) accent else Color.Transparent,
                            border = BorderStroke(1.5.dp, if (subtitlesDisabled) accent else Color.White.copy(alpha = 0.4f)),
                            modifier = Modifier.size(20.dp)
                        ) {
                            if (subtitlesDisabled) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Rounded.Check, contentDescription = null, tint = onAccent, modifier = Modifier.size(14.dp))
                                }
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Off", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }

                    val embeddedTracks = subtitleTrackOptions.filter { !it.isExternal }
                    val externalTracks = subtitleTrackOptions.filter { it.isExternal }

                    if (embeddedTracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Embedded Subtitles",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        embeddedTracks.forEachIndexed { idx, sub ->
                            SubtitleDualTrackRow(
                                indexNum = idx + 1,
                                track = sub,
                                accent = accent,
                                onAccent = onAccent,
                                onSelectPrimary = { onSelectSubtitleTrack(sub.groupIndex, sub.trackIndex) },
                                onSelectSecondary = { onSelectSecondarySubtitleTrack(sub) }
                            )
                        }
                    }

                    if (externalTracks.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "External Subtitles",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
                        )
                        externalTracks.forEachIndexed { idx, sub ->
                            SubtitleDualTrackRow(
                                indexNum = idx + 1,
                                track = sub,
                                accent = accent,
                                onAccent = onAccent,
                                onSelectPrimary = { onSelectSubtitleTrack(sub.groupIndex, sub.trackIndex) },
                                onSelectSecondary = { onSelectSecondarySubtitleTrack(sub) }
                            )
                        }
                    }

                    if (subtitleTrackOptions.isEmpty()) {
                        Text(
                            text = "No embedded subtitle tracks — tap Search (🔍) above to find subtitles online.",
                            color = Color.White.copy(alpha = 0.65f),
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 8.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            onDismiss()
                            onOpenOnlineSearch()
                        },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Search & Download Online Subtitles")
                    }
                }

                1 -> {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Font Size", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Text("${subtitleFontSize.toInt()} sp", color = accent, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = subtitleFontSize.coerceIn(12f, 44f),
                        onValueChange = { AppSettings.setSubtitleFontSize(it) },
                        valueRange = 12f..44f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Background Opacity", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Text("${(subtitleBgOpacity * 100).roundToInt()}%", color = accent, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = subtitleBgOpacity.coerceIn(0f, 1f),
                        onValueChange = { AppSettings.setSubtitleBgOpacity(it) },
                        valueRange = 0f..1f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Bottom Offset", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Text("${subtitleBottomPaddingDp} dp", color = accent, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = subtitleBottomPaddingDp.toFloat().coerceIn(0f, 120f),
                        onValueChange = { AppSettings.setSubtitleBottomPaddingDp(it.roundToInt()) },
                        valueRange = 0f..120f,
                        colors = SliderDefaults.colors(thumbColor = accent, activeTrackColor = accent)
                    )

                    Text("Edge Style", color = Color.White, style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("DROP_SHADOW" to "Shadow", "OUTLINE" to "Outline", "NONE" to "None", "RAISED" to "Raised").forEach { (key, label) ->
                            FilterChip(
                                selected = subtitleEdgeType == key,
                                onClick = { AppSettings.setSubtitleEdgeType(key) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = accent,
                                    selectedLabelColor = onAccent,
                                    containerColor = Color.White.copy(alpha = 0.08f),
                                    labelColor = Color.White.copy(alpha = 0.8f)
                                )
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Bold Text", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                        Switch(
                            checked = subtitleBold,
                            onCheckedChange = { AppSettings.setSubtitleBold(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = onAccent, checkedTrackColor = accent)
                        )
                    }
                }

                else -> {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Subtitle Sync Delay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("${subtitleDelayMs} ms", style = MaterialTheme.typography.bodySmall, color = accent)
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            DelayChip("-100ms") { onSubtitleDelayChange((subtitleDelayMs - 100).coerceIn(-10000, 10000)) }
                            DelayChip("0") { onSubtitleDelayChange(0) }
                            DelayChip("+100ms") { onSubtitleDelayChange((subtitleDelayMs + 100).coerceIn(-10000, 10000)) }
                        }
                    }

                    SubtitleAdvancedControlsSection(
                        accent = accent,
                        secondarySubtitleTitle = secondarySubtitleTitle,
                        onSubtitleDelayChange = onSubtitleDelayChange,
                        onSubtitleSpeedChange = onSubtitleSpeedChange,
                        onRequestSecondarySubtitleFile = onRequestSecondarySubtitleFile,
                        onClearSecondarySubtitle = onClearSecondarySubtitle,
                        onRequestCustomSubtitleFont = onRequestCustomSubtitleFont,
                        onClearCustomSubtitleFont = onClearCustomSubtitleFont
                    )
                }
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun SubtitleDualTrackRow(
    indexNum: Int,
    track: PlayerTrackOption,
    accent: Color,
    onAccent: Color,
    onSelectPrimary: () -> Unit,
    onSelectSecondary: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onSelectPrimary)
            .padding(horizontal = 6.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = RoundedCornerShape(6.dp),
            color = if (track.selected) accent else Color.Transparent,
            border = BorderStroke(1.5.dp, if (track.selected) accent else Color.White.copy(alpha = 0.4f)),
            modifier = Modifier.size(20.dp)
        ) {
            if (track.selected) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Rounded.Check, contentDescription = null, tint = onAccent, modifier = Modifier.size(14.dp))
                }
            }
        }
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = "#$indexNum: ${track.label}",
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (track.selected || track.secondarySelected) FontWeight.SemiBold else FontWeight.Normal,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        // Primary (P) & Secondary (S) dual-subtitle badges matching screenshot
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                onClick = onSelectPrimary,
                shape = CircleShape,
                color = if (track.selected) Color.White else Color.White.copy(alpha = 0.14f),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "P",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (track.selected) Color(0xFF0E141B) else Color.White.copy(alpha = 0.8f)
                    )
                }
            }
            Surface(
                onClick = onSelectSecondary,
                shape = CircleShape,
                color = if (track.secondarySelected) accent else Color.White.copy(alpha = 0.12f),
                modifier = Modifier.size(26.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "S",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (track.secondarySelected) onAccent else Color.White.copy(alpha = 0.72f)
                    )
                }
            }
        }
    }
}

/**
 * Dedicated Online Subtitle Search Sheet with movie/series poster identification carousel,
 * multi-language filters, live progress bar, ZIP extraction, and one-tap player attachment.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineSubtitleSearchSheet(
    sheetState: SheetState,
    sheetColor: Color,
    accent: Color,
    onAccent: Color,
    videoTitle: String,
    onAttachExternalSubtitleUri: (Uri) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val parsedHint = remember(videoTitle) { IntroOutroManager.parseEpisodeInfo(videoTitle) }
    val onlineSubMode by AppSettings.onlineSubSearchMode.collectAsState()
    val onlineSubPreferredLang by AppSettings.onlineSubPreferredLang.collectAsState()

    var queryText by remember(videoTitle) { mutableStateOf(parsedHint.cleanTitle.ifBlank { videoTitle }) }
    var selectedLang by remember(onlineSubPreferredLang) { mutableStateOf(onlineSubPreferredLang) }
    var isSearching by remember { mutableStateOf(false) }
    var downloadingSubId by remember { mutableStateOf<String?>(null) }
    var outcome by remember { mutableStateOf<OnlineSubtitleSearchOutcome?>(null) }

    fun runSearch(customMedia: OnlineMediaMatch? = null) {
        if (queryText.isBlank()) return
        isSearching = true
        scope.launch {
            val res = if (customMedia != null && outcome != null) {
                OnlineSubtitleClient.searchForMedia(
                    media = customMedia,
                    allMatches = outcome!!.mediaMatches,
                    season = parsedHint.season,
                    episode = parsedHint.episode,
                    languageCode = selectedLang,
                    searchMode = onlineSubMode,
                    subdlApiKey = AppSettings.getSubdlApiKey(),
                    wyzieApiKey = AppSettings.getWyzieApiKey()
                )
            } else {
                OnlineSubtitleClient.search(
                    query = queryText,
                    languageCode = selectedLang,
                    searchMode = onlineSubMode,
                    subdlApiKey = AppSettings.getSubdlApiKey(),
                    wyzieApiKey = AppSettings.getWyzieApiKey()
                )
            }
            outcome = res
            isSearching = false
        }
    }

    LaunchedEffect(videoTitle) {
        if (queryText.isNotBlank()) runSearch()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 10.dp)
        ) {
            val detectedBadge = buildString {
                append(parsedHint.cleanTitle.ifBlank { videoTitle })
                if (parsedHint.year != null) append(" (${parsedHint.year})")
                if (parsedHint.season != null && parsedHint.episode != null) {
                    append(" • S${parsedHint.season.toString().padStart(2, '0')}E${parsedHint.episode.toString().padStart(2, '0')}")
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(14.dp))
                Text(
                    text = detectedBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedTextField(
                value = queryText,
                onValueChange = { queryText = it },
                singleLine = true,
                placeholder = { Text("Search movie or episode title...", color = Color.White.copy(alpha = 0.5f)) },
                leadingIcon = {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
                },
                trailingIcon = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (queryText.isNotEmpty()) {
                            IconButton(onClick = { queryText = "" }, modifier = Modifier.size(34.dp)) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.7f), modifier = Modifier.size(18.dp))
                            }
                        }
                        IconButton(onClick = { runSearch() }, modifier = Modifier.size(34.dp)) {
                            Icon(Icons.Rounded.Refresh, contentDescription = "Refresh", tint = Color.White.copy(alpha = 0.85f), modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { runSearch() }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Rounded.Search, contentDescription = "Search", tint = accent, modifier = Modifier.size(20.dp))
                        }
                    }
                },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { runSearch() }),
                shape = RoundedCornerShape(16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = accent,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.28f)
                ),
                modifier = Modifier.fillMaxWidth()
            )

            if (isSearching) {
                Spacer(modifier = Modifier.height(6.dp))
                LinearProgressIndicator(
                    color = accent,
                    trackColor = Color.White.copy(alpha = 0.14f),
                    modifier = Modifier.fillMaxWidth().height(2.5.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Language chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OnlineSubtitleClient.SUPPORTED_LANGUAGES.forEach { (code, label) ->
                    FilterChip(
                        selected = selectedLang.equals(code, ignoreCase = true),
                        onClick = {
                            selectedLang = code
                            AppSettings.setOnlineSubPreferredLang(code)
                            runSearch(outcome?.selectedMedia)
                        },
                        label = { Text(label, fontSize = 11.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = onAccent,
                            containerColor = Color.White.copy(alpha = 0.08f),
                            labelColor = Color.White.copy(alpha = 0.85f)
                        )
                    )
                }
            }

            // Poster Identification Carousel (matches Screenshot 4)
            val mediaList = outcome?.mediaMatches.orEmpty()
            if (mediaList.isNotEmpty()) {
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Found ${mediaList.size} title match(es)",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White.copy(alpha = 0.72f)
                )
                Spacer(modifier = Modifier.height(6.dp))
                LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(mediaList, key = { it.id }) { media ->
                        val isChosen = outcome?.selectedMedia?.id == media.id
                        Surface(
                            onClick = { runSearch(media) },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(
                                width = if (isChosen) 2.dp else 1.dp,
                                color = if (isChosen) accent else Color.White.copy(alpha = 0.14f)
                            ),
                            color = Color(0xFF151C24),
                            modifier = Modifier.width(102.dp)
                        ) {
                            Box(modifier = Modifier.aspectRatio(0.68f)) {
                                if (!media.posterUrl.isNullOrBlank()) {
                                    AsyncImage(
                                        model = media.posterUrl,
                                        contentDescription = media.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                } else {
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.fillMaxSize().background(Color(0xFF1C2530))
                                    ) {
                                        Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = Color.White.copy(alpha = 0.4f))
                                    }
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xE6080B10))))
                                        .padding(6.dp)
                                ) {
                                    Column {
                                        Text(
                                            text = media.title,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        if (media.year.isNotBlank()) {
                                            Text(
                                                text = media.year,
                                                fontSize = 10.sp,
                                                color = if (isChosen) accent else Color.White.copy(alpha = 0.75f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            val resultsList = outcome?.results.orEmpty()
            Text(
                text = if (outcome != null) "Online Results (${resultsList.size})" else "Searching providers...",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            outcome?.statusMessage?.let { msg ->
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = msg,
                    style = MaterialTheme.typography.labelSmall,
                    color = if (outcome?.error != null && resultsList.isEmpty()) Color(0xFFFCA5A5) else Color.White.copy(alpha = 0.68f)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                resultsList.take(30).forEach { item: OnlineSubtitleResult ->
                    Surface(
                        onClick = {
                            if (downloadingSubId == null) {
                                downloadingSubId = item.id
                                scope.launch {
                                    val dl = OnlineSubtitleClient.downloadSubtitleToCache(context, item)
                                    downloadingSubId = null
                                    dl.onSuccess { subUri ->
                                        onAttachExternalSubtitleUri(subUri)
                                        onDismiss()
                                    }.onFailure { err ->
                                        Toast.makeText(
                                            context,
                                            "Download failed: ${err.localizedMessage ?: "error"}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                                }
                            }
                        },
                        shape = RoundedCornerShape(14.dp),
                        color = Color.White.copy(alpha = 0.09f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.title,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(3.dp))
                                Text(
                                    text = "${item.languageName} • ${item.source}${if (item.isHearingImpaired) " • HI" else ""}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.68f)
                                )
                            }
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = Color.White.copy(alpha = 0.12f)
                            ) {
                                Text(
                                    text = item.format.uppercase(Locale.ROOT),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                                )
                            }
                            if (downloadingSubId == item.id) {
                                CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = accent)
                            } else {
                                Icon(Icons.Rounded.Download, contentDescription = "Download Subtitle", tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}

/**
 * Clean frosted Audio Tracks sheet with external audio loader, channel/codec info,
 * volume/loudness boost slider, audio sync delay & equalizer shortcuts.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostedAudioTracksSheet(
    sheetState: SheetState,
    sheetColor: Color,
    accent: Color,
    onAccent: Color,
    audioTrackOptions: List<PlayerTrackOption>,
    channelLayoutText: String,
    loudnessBoost: Float,
    audioDelayMs: Int,
    selectedEqPreset: String,
    onSelectAudioTrack: (Int, Int) -> Unit,
    onRequestExternalAudioFile: () -> Unit,
    onVolumeChange: (Float) -> Unit,
    onAudioDelayChange: (Int) -> Unit,
    onSelectEqPreset: (String) -> Unit,
    onOpenFullEqualizer: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            onRequestExternalAudioFile()
                        }
                        .padding(horizontal = 6.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Add, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add external audio track",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }
                if (channelLayoutText.isNotBlank()) {
                    Surface(shape = CircleShape, color = accent.copy(alpha = 0.2f)) {
                        Text(
                            text = channelLayoutText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Audio Tracks",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = 0.72f),
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp)
            )
            if (audioTrackOptions.isEmpty()) {
                Text(
                    text = "No selectable audio tracks found",
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
            audioTrackOptions.forEachIndexed { idx, track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onSelectAudioTrack(track.groupIndex, track.trackIndex) }
                        .padding(horizontal = 6.dp, vertical = 9.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (track.selected) accent else Color.Transparent,
                        border = BorderStroke(1.5.dp, if (track.selected) accent else Color.White.copy(alpha = 0.4f)),
                        modifier = Modifier.size(20.dp)
                    ) {
                        if (track.selected) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = onAccent, modifier = Modifier.size(14.dp))
                            }
                        }
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Icon(Icons.Rounded.Audiotrack, contentDescription = null, tint = if (track.selected) accent else Color.White.copy(alpha = 0.65f), modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "#${idx + 1}: ${track.label}",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (track.selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = Color.White,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            Spacer(modifier = Modifier.height(10.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = if (loudnessBoost > 100f) "Loudness Boost" else "Master Volume",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    text = "${loudnessBoost.toInt()}%",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (loudnessBoost > 100f) Color(0xFFFF7A00) else accent
                )
            }
            Slider(
                value = loudnessBoost.coerceIn(0f, 200f),
                onValueChange = onVolumeChange,
                valueRange = 0f..200f,
                colors = SliderDefaults.colors(
                    thumbColor = if (loudnessBoost > 100f) Color(0xFFFF7A00) else accent,
                    activeTrackColor = if (loudnessBoost > 100f) Color(0xFFFF7A00) else accent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Audio Sync Delay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                    Text("${audioDelayMs} ms", style = MaterialTheme.typography.bodySmall, color = accent)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    DelayChip("-100ms") { onAudioDelayChange((audioDelayMs - 100).coerceIn(-5000, 5000)) }
                    DelayChip("0") { onAudioDelayChange(0) }
                    DelayChip("+100ms") { onAudioDelayChange((audioDelayMs + 100).coerceIn(-5000, 5000)) }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text("Equalizer Presets", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf("Flat", "Cinema", "Bass Boost", "Vocal Clarifier", "Rock").forEach { preset ->
                    FilterChip(
                        selected = selectedEqPreset == preset,
                        onClick = { onSelectEqPreset(preset) },
                        label = { Text(preset, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = accent,
                            selectedLabelColor = onAccent,
                            containerColor = Color.White.copy(alpha = 0.08f),
                            labelColor = Color.White.copy(alpha = 0.85f)
                        )
                    )
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedButton(
                onClick = {
                    onDismiss()
                    onOpenFullEqualizer()
                },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Open 5-Band Equalizer & Channels")
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

/**
 * Clean frosted More sheet (Screenshot 3): quick chips, live Engine switcher card,
 * Default statistics page selector (Off..Page 4) & full Player Tools list.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FrostedMoreToolsSheet(
    sheetState: SheetState,
    sheetColor: Color,
    accent: Color,
    onAccent: Color,
    activeEngine: String,
    activeDecoderName: String,
    statsPage: Int,
    sleepTimerRemainingMs: Long,
    sleepTimerUntilEnd: Boolean,
    selectedEqPreset: String,
    chaptersCount: Int,
    currentChapterTitle: String?,
    videoTracksCount: Int,
    customSkipSec: Int,
    isMirrored: Boolean,
    isFlippedVertical: Boolean,
    isShuffleEnabled: Boolean,
    ambientEnabled: Boolean,
    bgPlaybackEnabled: Boolean,
    abLoopActive: Boolean,
    zoomScale: Float,
    queueSize: Int,
    onOpenSleepTimer: () -> Unit,
    onOpenEqualizer: () -> Unit,
    onOpenEngineSheet: () -> Unit,
    onOpenEdgeDrawer: () -> Unit,
    onOpenOnlineSubs: () -> Unit,
    onSelectStatsPage: (Int) -> Unit,
    onOpenChapters: () -> Unit,
    onOpenQuality: () -> Unit,
    onCustomSkip: () -> Unit,
    onToggleMirror: () -> Unit,
    onToggleVerticalFlip: () -> Unit,
    onToggleShuffle: () -> Unit,
    onToggleAmbientMode: () -> Unit,
    onToggleBackgroundPlayback: () -> Unit,
    onOpenFrameNav: () -> Unit,
    onOpenAbLoop: () -> Unit,
    onOpenZoom: () -> Unit,
    onOpenCodecInspector: () -> Unit,
    onOpenQueue: () -> Unit,
    onDismiss: () -> Unit
) {
    val isMpv = activeEngine.equals("MPV", ignoreCase = true)
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = sheetColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 520.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp)
        ) {
            // Top "More" + quick pills (Sleep timer, Equalizer, Filters/Shaders, Drawer, Download Subs)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("More", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = Color.White)
                QuickHeaderAction(Icons.Rounded.Bedtime, "Sleep timer") { onDismiss(); onOpenSleepTimer() }
                QuickHeaderAction(Icons.Rounded.GraphicEq, "Equalizer") { onDismiss(); onOpenEqualizer() }
                QuickHeaderAction(Icons.Rounded.Tune, "Filters") { onDismiss(); onOpenEngineSheet() }
                QuickHeaderAction(Icons.Rounded.Code, "Tools Grid") { onDismiss(); onOpenEdgeDrawer() }
                QuickHeaderAction(Icons.Rounded.Download, "Subtitles") { onDismiss(); onOpenOnlineSubs() }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Highlighted Engine card (matches Screenshot 3)
            Surface(
                onClick = { onDismiss(); onOpenEngineSheet() },
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.14f),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.22f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        text = if (isMpv) "Engine: MPV" else "Engine: Media3 ExoPlayer",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = if (isMpv) "Output: MPV renderer • Tap to switch engine" else "Output: ${activeDecoderName.ifBlank { "MediaCodec" }} • Tap to switch",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.72f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            Text(
                text = "Default statistics page",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(0 to "Off", 1 to "Page 1", 2 to "Page 2", 3 to "Page 3", 4 to "Page 4").forEach { (idx, label) ->
                    val selected = statsPage == idx
                    Surface(
                        onClick = { onSelectStatsPage(idx) },
                        shape = RoundedCornerShape(10.dp),
                        color = if (selected) accent else Color.White.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, if (selected) accent else Color.White.copy(alpha = 0.22f))
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = if (selected) onAccent else Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
            HorizontalDivider(color = Color.White.copy(alpha = 0.14f))
            Spacer(modifier = Modifier.height(6.dp))

            ToolsHubItem(
                icon = Icons.Rounded.Bookmarks,
                title = "Chapters ($chaptersCount)",
                subtitle = if (!currentChapterTitle.isNullOrBlank()) "Now: $currentChapterTitle" else "Jump to MKV chapters",
                onClick = { onDismiss(); onOpenChapters() }
            )
            ToolsHubItem(
                icon = Icons.Rounded.HighQuality,
                title = "Video Quality",
                subtitle = if (videoTracksCount > 1) "$videoTracksCount resolution streams" else "Auto stream",
                onClick = { onDismiss(); onOpenQuality() }
            )
            ToolsHubItem(
                icon = Icons.Rounded.FastForward,
                title = "Custom Skip (+${customSkipSec}s)",
                subtitle = "One-tap intro/scene skip forward",
                onClick = { onDismiss(); onCustomSkip() }
            )
            ToolsHubItem(
                icon = Icons.Rounded.Flip,
                title = if (isMirrored) "Mirror Horizontal (ON)" else "Mirror Horizontal",
                subtitle = "Flip video horizontally",
                onClick = { onToggleMirror(); onDismiss() }
            )
            ToolsHubItem(
                icon = Icons.Rounded.FlipCameraAndroid,
                title = if (isFlippedVertical) "Vertical Flip (ON)" else "Vertical Flip",
                subtitle = "Flip video vertically",
                onClick = { onToggleVerticalFlip(); onDismiss() }
            )
            ToolsHubItem(
                icon = Icons.Rounded.Shuffle,
                title = if (isShuffleEnabled) "Queue Shuffle (ON)" else "Queue Shuffle",
                subtitle = "Randomize next item in queue",
                onClick = { onToggleShuffle(); onDismiss() }
            )
            ToolsHubItem(
                icon = Icons.Rounded.AutoAwesome,
                title = if (ambientEnabled) "Ambient Aura Glow (ON)" else "Ambient Aura Glow",
                subtitle = "Sample playback colors gently around viewport",
                onClick = { onToggleAmbientMode(); onDismiss() }
            )
            ToolsHubItem(
                icon = Icons.Rounded.Headphones,
                title = if (bgPlaybackEnabled) "Background Audio Playback (ON)" else "Background Audio Playback",
                subtitle = "Keep audio playing when minimized ($selectedEqPreset EQ)",
                onClick = { onToggleBackgroundPlayback(); onDismiss() }
            )
            ToolsHubItem(
                icon = Icons.Rounded.SlowMotionVideo,
                title = "Frame-by-Frame Navigation",
                subtitle = "Step exact frames & capture lossless PNG",
                onClick = { onDismiss(); onOpenFrameNav() }
            )
            ToolsHubItem(
                icon = Icons.Rounded.Loop,
                title = "A-B Scene Loop",
                subtitle = if (abLoopActive) "Loop active" else "Repeat any scene cleanly",
                onClick = { onDismiss(); onOpenAbLoop() }
            )
            ToolsHubItem(
                icon = Icons.Rounded.ZoomIn,
                title = "Zoom & Pan Adjustment",
                subtitle = "Current: ${(zoomScale * 100).roundToInt()}%",
                onClick = { onDismiss(); onOpenZoom() }
            )
            ToolsHubItem(
                icon = Icons.Rounded.DeveloperBoard,
                title = "Stream Telemetry & Codecs",
                subtitle = activeDecoderName.ifBlank {
                    when {
                        sleepTimerRemainingMs > 0L -> "Sleep Timer: ${formatDuration(sleepTimerRemainingMs)}"
                        sleepTimerUntilEnd -> "Sleep at End"
                        else -> "Hardware & runtime diagnostics"
                    }
                },
                onClick = { onDismiss(); onOpenCodecInspector() }
            )
            if (queueSize > 1) {
                ToolsHubItem(
                    icon = Icons.AutoMirrored.Rounded.QueueMusic,
                    title = "Playback Queue ($queueSize items)",
                    subtitle = "Jump between videos in queue",
                    onClick = { onDismiss(); onOpenQueue() }
                )
            }
            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}

@Composable
private fun QuickHeaderAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 6.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(5.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = Color.White, fontWeight = FontWeight.Medium)
    }
}

/**
 * Live floating Statistics HUD Overlay controlled via "Default statistics page" (Off / Page 1..4).
 */
@Composable
fun LiveStatsHudOverlayCard(
    page: Int,
    activeEngine: String,
    isHwDecoder: Boolean,
    activeDecoderName: String,
    codecName: String,
    resolutionText: String,
    hdrFormatText: String,
    bitrateText: String,
    detectedFps: Float,
    droppedFrames: Int,
    channelLayoutText: String,
    currentSpeed: Float,
    audioDelayMs: Int,
    subtitleDelayMs: Int,
    currentPositionMs: Long,
    bufferedPositionMs: Long,
    durationMs: Long,
    aspectRatioMode: String,
    orientationMode: String,
    audioTracksCount: Int,
    subtitleTracksCount: Int,
    accent: Color,
    modifier: Modifier = Modifier
) {
    val lines = when (page) {
        1 -> listOf(
            "Page 1 • Video & Render",
            "Engine: $activeEngine (${if (isHwDecoder) "HW" else "SW"})",
            "Decoder: ${activeDecoderName.ifBlank { codecName }}",
            "Video: ${resolutionText.ifBlank { "Auto" }} • $hdrFormatText • ${if (bitrateText.isNotBlank()) bitrateText else "VBR"}",
            "Timing: ${if (detectedFps > 1f) "${detectedFps.roundToInt()} FPS" else "Auto FPS"} • Dropped: $droppedFrames"
        )
        2 -> listOf(
            "Page 2 • Audio & Sync",
            "Channels: $channelLayoutText • Speed: ${String.format(Locale.US, "%.2fx", currentSpeed)}",
            "A-Sync Delay: ${audioDelayMs} ms",
            "Sub Sync Delay: ${subtitleDelayMs} ms"
        )
        3 -> {
            val cacheAheadSec = ((bufferedPositionMs - currentPositionMs).coerceAtLeast(0L) / 1000L)
            listOf(
                "Page 3 • Buffer & Frame",
                "Pos: ${formatDuration(currentPositionMs)} / ${formatDuration(durationMs)}",
                "Cache Ahead: ${cacheAheadSec}s",
                "Aspect: $aspectRatioMode • Orient: $orientationMode"
            )
        }
        else -> listOf(
            "Page 4 • Streams & Tracks",
            "Audio Tracks: $audioTracksCount",
            "Subtitle Tracks: $subtitleTracksCount",
            "Codec: $codecName • HDR: $hdrFormatText"
        )
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color(0xBD090D13),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.45f)),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            lines.forEachIndexed { idx, line ->
                Text(
                    text = line,
                    fontSize = if (idx == 0) 11.sp else 10.sp,
                    fontWeight = if (idx == 0) FontWeight.Bold else FontWeight.Medium,
                    color = if (idx == 0) accent else Color.White.copy(alpha = 0.9f)
                )
            }
        }
    }
}
