package com.cmstudio.player.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForwardIos
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.ArrowUpward
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.DeleteOutline
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Gesture
import androidx.compose.material.icons.rounded.GridView
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Language
import androidx.compose.material.icons.rounded.Memory
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.Palette
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.ScreenRotation
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Speed
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Tv
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmstudio.player.BuildConfig
import com.cmstudio.player.PlaybackHistoryBridge
import com.cmstudio.player.R
import com.cmstudio.player.SubtitleUtils
import com.cmstudio.player.codec.CodecCapabilitiesActivity
import com.cmstudio.player.codec.CodecInspector
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.theme.CustomThemeWallpaperSection
import com.cmstudio.player.ui.theme.DarkModeSegmentedRow
import com.cmstudio.player.ui.theme.ThemePreset
import com.cmstudio.player.ui.theme.ThemePreviewCarousel
import com.cmstudio.player.ui.theme.extendedColors
import java.io.File
import java.util.Locale
import kotlin.math.roundToInt

enum class SettingsDestinationPage {
    DASHBOARD,
    GENERAL,
    APPEARANCE,
    NAVIGATION,
    VIDEO_PLAYER,
    PLAYBACK,
    VIDEO_RENDERING,
    DECODER_ENGINE,
    AUDIO,
    SUBTITLES,
    GESTURES,
    SCREEN_ORIENTATION,
    BACKGROUND_PLAYBACK,
    NETWORK_STREAMING,
    CASTING,
    MEDIA_LIBRARY,
    EXTRA_FEATURES,
    SUPPORTED_CODECS,
    ADVANCED,
    ABOUT,
    OPEN_SOURCE_LICENSES
}

data class SearchableSettingItem(
    val title: String,
    val description: String,
    val page: SettingsDestinationPage,
    val categoryName: String
)

private data class LicenseEntry(val title: String, val fileName: String)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    initialCategory: String? = null
) {
    var currentPage by remember(initialCategory) {
        mutableStateOf(
            SettingsDestinationPage.entries.firstOrNull {
                it.name.equals(initialCategory, ignoreCase = true)
            } ?: SettingsDestinationPage.DASHBOARD
        )
    }
    var searchQuery by remember { mutableStateOf("") }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        val isTwoPane = maxWidth >= 600.dp
        val activeDetailPage = if (currentPage == SettingsDestinationPage.DASHBOARD) {
            SettingsDestinationPage.GENERAL
        } else {
            currentPage
        }

        if (!isTwoPane && currentPage != SettingsDestinationPage.DASHBOARD) {
            BackHandler {
                currentPage = SettingsDestinationPage.DASHBOARD
            }
        }

        if (isTwoPane) {
            Row(modifier = Modifier.fillMaxSize()) {
                SettingsDashboardScreen(
                    searchQuery = searchQuery,
                    onSearchQueryChange = { searchQuery = it },
                    onNavigate = { dest -> currentPage = dest },
                    onBackClick = onBackClick,
                    modifier = Modifier.weight(0.4f).fillMaxHeight()
                )
                VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f))
                Box(modifier = Modifier.weight(0.6f).fillMaxHeight()) {
                    SettingsDetailRouter(
                        target = activeDetailPage,
                        onNavigate = { currentPage = it },
                        onBack = { currentPage = SettingsDestinationPage.DASHBOARD }
                    )
                }
            }
        } else {
            androidx.compose.animation.Crossfade(
                targetState = currentPage,
                label = "SettingsNavigation"
            ) { target ->
                if (target == SettingsDestinationPage.DASHBOARD) {
                    SettingsDashboardScreen(
                        searchQuery = searchQuery,
                        onSearchQueryChange = { searchQuery = it },
                        onNavigate = { dest ->
                            if (searchQuery.isNotBlank()) AppSettings.recordSettingsSearchQuery(searchQuery)
                            currentPage = dest
                        },
                        onBackClick = onBackClick
                    )
                } else {
                    SettingsDetailRouter(
                        target = target,
                        onNavigate = { currentPage = it },
                        onBack = { currentPage = SettingsDestinationPage.DASHBOARD }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsDetailRouter(
    target: SettingsDestinationPage,
    onNavigate: (SettingsDestinationPage) -> Unit,
    onBack: () -> Unit
) {
    when (target) {
        SettingsDestinationPage.DASHBOARD, SettingsDestinationPage.GENERAL -> GeneralDetailScreen(onBack)
        SettingsDestinationPage.APPEARANCE -> AppearanceDetailScreen(onBack)
        SettingsDestinationPage.NAVIGATION -> NavigationDetailScreen(onBack)
        SettingsDestinationPage.VIDEO_PLAYER -> VideoPlayerDetailScreen(onBack)
        SettingsDestinationPage.PLAYBACK -> PlaybackDetailScreen(onBack)
        SettingsDestinationPage.VIDEO_RENDERING -> VideoRenderingDetailScreen(onBack)
        SettingsDestinationPage.DECODER_ENGINE -> DecoderEngineDetailScreen(onBack)
        SettingsDestinationPage.AUDIO -> AudioDetailScreen(onBack)
        SettingsDestinationPage.SUBTITLES -> SubtitlesDetailScreen(onBack)
        SettingsDestinationPage.GESTURES -> GesturesDetailScreen(onBack)
        SettingsDestinationPage.SCREEN_ORIENTATION -> ScreenOrientationDetailScreen(onBack)
        SettingsDestinationPage.BACKGROUND_PLAYBACK -> BackgroundPlaybackDetailScreen(onBack)
        SettingsDestinationPage.NETWORK_STREAMING -> NetworkStreamingDetailScreen(onBack)
        SettingsDestinationPage.CASTING -> CastingDetailScreen(onBack)
        SettingsDestinationPage.MEDIA_LIBRARY -> MediaLibraryDetailScreen(onBack)
        SettingsDestinationPage.EXTRA_FEATURES -> ExtraFeaturesDetailScreen(onBack)
        SettingsDestinationPage.SUPPORTED_CODECS -> SupportedCodecsDetailScreen(onBack)
        SettingsDestinationPage.ADVANCED -> AdvancedDetailScreen(onBack)
        SettingsDestinationPage.ABOUT -> AboutAndSystemDetailScreen(
            onOpenLicenses = { onNavigate(SettingsDestinationPage.OPEN_SOURCE_LICENSES) },
            onBack = onBack
        )
        SettingsDestinationPage.OPEN_SOURCE_LICENSES -> OpenSourceLicensesDetailScreen(onBack)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SettingsDashboardScreen(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    onNavigate: (SettingsDestinationPage) -> Unit,
    onBackClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val searchHistory by AppSettings.settingsSearchHistory.collectAsState()
    val filteredSearchResults = remember(searchQuery) {
        if (searchQuery.isBlank()) emptyList()
        else DeepSettingsSearchIndex.search(searchQuery)
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (onBackClick != null) {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = MaterialTheme.colorScheme.onBackground)
                    }
                }
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            }
        }

        item {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceContainerHigh
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = onSearchQueryChange,
                    placeholder = { Text("Search ${DeepSettingsSearchIndex.entries.size}+ settings & options...", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { onSearchQueryChange("") }) {
                                Icon(Icons.Rounded.Close, contentDescription = "Clear")
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
        }

        if (searchQuery.isBlank() && searchHistory.isNotEmpty()) {
            item {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    searchHistory.take(5).forEach { recent ->
                        FilterChip(
                            selected = false,
                            onClick = { onSearchQueryChange(recent) },
                            label = { Text(recent) }
                        )
                    }
                    TextButton(onClick = { AppSettings.clearSettingsSearchHistory() }) {
                        Text("Clear")
                    }
                }
            }
        }

        if (searchQuery.isNotBlank()) {
            item { SettingsSectionLabel("MATCHES (${filteredSearchResults.size})") }
            if (filteredSearchResults.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No matching settings found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            } else {
                items(filteredSearchResults) { result ->
                    Surface(
                        onClick = { onNavigate(result.page) },
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(result.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                Text("${result.categoryName} • ${result.description}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
        } else {
            // Core
            item { SettingsSectionLabel("APP & APPEARANCE") }
            item {
                PreferenceGroupCard {
                    CategoryNavigationRow("General", "Resume behavior and library startup", Icons.Rounded.Tune, Color(0xFF3B82F6)) {
                        onNavigate(SettingsDestinationPage.GENERAL)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Appearance", "Themes, AMOLED black, Glass Effect (OFF by default)", Icons.Rounded.Palette, Color(0xFFEC4899)) {
                        onNavigate(SettingsDestinationPage.APPEARANCE)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Navigation", "Show, hide, and reorder bottom navigation tabs", Icons.Rounded.Menu, Color(0xFF8B5CF6)) {
                        onNavigate(SettingsDestinationPage.NAVIGATION)
                    }
                }
            }

            // Video & Playback
            item { SettingsSectionLabel("PLAYER & PLAYBACK") }
            item {
                PreferenceGroupCard {
                    CategoryNavigationRow("Video Player", "HUD buttons, control size, seekbar style & telemetry", Icons.Rounded.Tv, Color(0xFF10B981)) {
                        onNavigate(SettingsDestinationPage.VIDEO_PLAYER)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Playback", "Speed, pitch correction, repeat, auto-next, seek precision", Icons.Rounded.PlayCircle, Color(0xFF06B6D4)) {
                        onNavigate(SettingsDestinationPage.PLAYBACK)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Video Rendering", "SurfaceView/TextureView, aspect ratio, 120Hz frame pacing", Icons.Rounded.AspectRatio, Color(0xFFF59E0B)) {
                        onNavigate(SettingsDestinationPage.VIDEO_RENDERING)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Decoder & Engine", "Dual Engine (MPV + ExoPlayer), Anime4K, Vulkan & codecs", Icons.Rounded.DeveloperBoard, Color(0xFF0EA5E9)) {
                        onNavigate(SettingsDestinationPage.DECODER_ENGINE)
                    }
                }
            }

            // Audio & Subtitles
            item { SettingsSectionLabel("AUDIO & SUBTITLES") }
            item {
                PreferenceGroupCard {
                    CategoryNavigationRow("Audio", "Boost, equalizer, night compressor & audio sync delay", Icons.AutoMirrored.Rounded.VolumeUp, Color(0xFF22C55E)) {
                        onNavigate(SettingsDestinationPage.AUDIO)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Subtitles", "Font size, edge style, background opacity & sync delay", Icons.Rounded.Subtitles, Color(0xFFEAB308)) {
                        onNavigate(SettingsDestinationPage.SUBTITLES)
                    }
                }
            }

            // Interaction & Display
            item { SettingsSectionLabel("CONTROLS & SYSTEM") }
            item {
                PreferenceGroupCard {
                    CategoryNavigationRow("Gestures", "Brightness/volume swipes, double-tap, pinch zoom, haptics", Icons.Rounded.Gesture, Color(0xFFF43F5E)) {
                        onNavigate(SettingsDestinationPage.GESTURES)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Screen & Orientation", "Rotation lock, Match Video orientation & Keep Screen On", Icons.Rounded.ScreenRotation, Color(0xFF6366F1)) {
                        onNavigate(SettingsDestinationPage.SCREEN_ORIENTATION)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Background Playback", "Audio continuation & Auto Picture-in-Picture", Icons.Rounded.PictureInPictureAlt, Color(0xFF14B8A6)) {
                        onNavigate(SettingsDestinationPage.BACKGROUND_PLAYBACK)
                    }
                }
            }

            // Network, Casting & Library
            item { SettingsSectionLabel("NETWORK & LIBRARY") }
            item {
                PreferenceGroupCard {
                    CategoryNavigationRow("Network & Streaming", "Wi-Fi only policy, timeouts & WebDAV/HTTP settings", Icons.Rounded.Language, Color(0xFF0284C7)) {
                        onNavigate(SettingsDestinationPage.NETWORK_STREAMING)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Casting", "External display and Android system cast options", Icons.Rounded.Cast, Color(0xFF78716C)) {
                        onNavigate(SettingsDestinationPage.CASTING)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Media Library", "Tree/Grid display, metadata badges & excluded folders", Icons.Rounded.Folder, Color(0xFFA855F7)) {
                        onNavigate(SettingsDestinationPage.MEDIA_LIBRARY)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Extra Features", "Smart collections, watch stats & library health", Icons.Rounded.AutoAwesome, Color(0xFFEC4899)) {
                        onNavigate(SettingsDestinationPage.EXTRA_FEATURES)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Supported Codecs", "Hardware/Software HEVC, AV1, HDR decoders & search", Icons.Rounded.Memory, Color(0xFF06B6D4)) {
                        onNavigate(SettingsDestinationPage.SUPPORTED_CODECS)
                    }
                }
            }

            // Advanced, About & Licenses
            item { SettingsSectionLabel("ADVANCED & INFO") }
            item {
                PreferenceGroupCard {
                    CategoryNavigationRow("Advanced", "Cache management, history clear & preference resets", Icons.Rounded.Build, Color(0xFF64748B)) {
                        onNavigate(SettingsDestinationPage.ADVANCED)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("About & System", "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) • MPV + Media3 + Specs", Icons.Rounded.Info, Color(0xFF2563EB)) {
                        onNavigate(SettingsDestinationPage.ABOUT)
                    }
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.25f))
                    CategoryNavigationRow("Open-Source Licenses", "View GPL-3.0 and third-party dependency notices", Icons.Rounded.Description, Color(0xFF059669)) {
                        onNavigate(SettingsDestinationPage.OPEN_SOURCE_LICENSES)
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(24.dp)) }
        }
    }
}

// 1. General
@Composable
private fun GeneralDetailScreen(onBack: () -> Unit) {
    val rememberPos by AppSettings.rememberPosition.collectAsState()
    val autoNext by AppSettings.autoPlayNext.collectAsState()
    val repeatMode by AppSettings.playerRepeatMode.collectAsState()

    SettingsSubScreen(title = "General", subtitle = "Startup and playback continuity", onBack = onBack) {
        item {
            SettingsCard {
                SwitchPreferenceRow(
                    title = "Remember Playback Position",
                    subtitle = "Automatically resume local videos and streams where you stopped",
                    checked = rememberPos,
                    onCheckedChange = { AppSettings.setRememberPosition(it) }
                )
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow(
                    title = "Auto-Play Next Item",
                    subtitle = "Advance automatically when playing folder or playlist queues",
                    checked = autoNext,
                    onCheckedChange = { AppSettings.setAutoPlayNext(it) }
                )
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("Default Repeat Behavior", fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("OFF" to "Off", "ONE" to "Repeat One", "ALL" to "Repeat All").forEach { (k, label) ->
                        FilterChip(
                            selected = repeatMode == k,
                            onClick = { AppSettings.setPlayerRepeatMode(k) },
                            label = { Text(label) }
                        )
                    }
                }
            }
        }
    }
}

// 2. Appearance (Glass Effect OFF by default; full 3-way Dark Mode, 40+ Themes, Custom Wallpaper, Browser & Thumbnails)
@Composable
private fun AppearanceDetailScreen(onBack: () -> Unit) {
    val darkMode by AppSettings.darkMode.collectAsState()
    val isAmoled by AppSettings.isAmoled.collectAsState()
    val currentPreset by AppSettings.themePreset.collectAsState()
    val favoritePresets by AppSettings.favoriteThemePresets.collectAsState()
    val glassEffect by AppSettings.isLiquidGlassEnabled.collectAsState()
    val frostedSheets by AppSettings.isFrostedPlayerSheets.collectAsState()
    val useSystemFont by AppSettings.useSystemFont.collectAsState()
    val glassOpacity by AppSettings.glassFrostOpacity.collectAsState()
    val uiAnimScale by AppSettings.uiAnimationScale.collectAsState()
    val showExtension by AppSettings.showExtensionField.collectAsState()
    val showDuration by AppSettings.showDurationField.collectAsState()
    val showProgressBar by AppSettings.showProgressBar.collectAsState()
    val centerGridTitles by AppSettings.centerGridTitles.collectAsState()
    val unlimitedLines by AppSettings.unlimitedNameLines.collectAsState()
    val showTotalVideos by AppSettings.showTotalVideosChip.collectAsState()
    val showTotalDuration by AppSettings.showTotalDurationChip.collectAsState()
    val showNewBadge by AppSettings.showNewBadge.collectAsState()
    val manualCols by AppSettings.manualGridColumnsEnabled.collectAsState()
    val separateLayout by AppSettings.separateFolderVideoLayout.collectAsState()
    val showFolderThumb by AppSettings.showFolderThumbnails.collectAsState()
    val netThumbs by AppSettings.networkThumbnailsEnabled.collectAsState()
    val thumbCacheMb by AppSettings.thumbnailCacheSizeMb.collectAsState()

    SettingsSubScreen(title = "Appearance", subtitle = "Themes, Wallpapers, Browser UI & Thumbnails", onBack = onBack) {
        // Card 1: Theme & Dark Mode + Carousel + Custom Wallpaper
        item {
            SettingsCard {
                Text("Theme Mode", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(6.dp))
                DarkModeSegmentedRow(
                    currentMode = darkMode,
                    onModeSelected = { mode, tapOrigin ->
                        AppSettings.setThemeTransitionOrigin(tapOrigin.x to tapOrigin.y)
                        AppSettings.setDarkMode(mode)
                    }
                )
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("AMOLED Pure Black", "True #000000 backgrounds for OLED displays", isAmoled) { AppSettings.setAmoled(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("Theme Palette (${ThemePreset.entries.size} Themes)", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(4.dp))
                ThemePreviewCarousel(
                    currentPreset = currentPreset,
                    darkMode = darkMode,
                    isAmoled = isAmoled,
                    favoritePresets = favoritePresets,
                    onToggleFavorite = { AppSettings.toggleFavoriteThemePreset(it.name) },
                    onSelectTheme = { preset, tapOrigin ->
                        AppSettings.setThemeTransitionOrigin(tapOrigin.x to tapOrigin.y)
                        AppSettings.setThemePreset(preset)
                    }
                )
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                CustomThemeWallpaperSection()
            }
        }

        // Card 2: Typography & Surfaces (Liquid Glass + Frosted Sheets)
        item {
            SettingsCard {
                Text("Typography & Glass", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SwitchPreferenceRow("Use System Font", "Use system sans-serif instead of Google Sans Rounded", useSystemFont) { AppSettings.setUseSystemFont(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow(
                    title = "Glass Effect",
                    subtitle = "Apply translucent glass blur to floating docks & player HUD (OFF by default)",
                    checked = glassEffect,
                    onCheckedChange = { AppSettings.setLiquidGlassEnabled(it) }
                )
                if (glassEffect) {
                    HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Glass Opacity", fontWeight = FontWeight.Medium)
                        Text("${(glassOpacity * 100).roundToInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                    Slider(value = glassOpacity.coerceIn(0.45f, 0.92f), onValueChange = { AppSettings.setGlassFrostOpacity(it) }, valueRange = 0.45f..0.92f)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow(
                    title = "Frosted Player Sheets",
                    subtitle = "Translucent background for in-player modal bottom sheets",
                    checked = frostedSheets,
                    onCheckedChange = { AppSettings.setFrostedPlayerSheets(it) }
                )
            }
        }

        // Card 3: Motion & Animations
        item {
            SettingsCard {
                Text("Animations", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("UI Transition Speed", fontWeight = FontWeight.Medium)
                    Text(if (uiAnimScale <= 0.05f) "Reduced" else "${String.format(Locale.US, "%.1fx", uiAnimScale)}", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Slider(value = uiAnimScale.coerceIn(0f, 1.5f), onValueChange = { AppSettings.setUiAnimationScale(it) }, valueRange = 0f..1.5f)
            }
        }

        // Card 4: File Browser Appearance
        item {
            SettingsCard {
                Text("File Browser Layout & Badges", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SwitchPreferenceRow("Separate Folder & Video Layout", "Configure folders and video grids independently", separateLayout) { AppSettings.setSeparateFolderVideoLayout(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Manual Grid Columns", "Override adaptive screen width columns with fixed counts", manualCols) { AppSettings.setManualGridColumnsEnabled(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Show File Extension", "Display .mkv, .mp4, .webm suffix on file titles", showExtension) { AppSettings.setShowExtensionField(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Show Duration Badge", "Show runtime timestamp chip on video cards", showDuration) { AppSettings.setShowDurationField(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Show Playback Progress Bar", "Render watch progress indicator under videos", showProgressBar) { AppSettings.setShowProgressBar(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Center Grid Titles", "Center align video titles in grid cards", centerGridTitles) { AppSettings.setCenterGridTitles(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Unlimited Title Lines", "Wrap long filenames instead of truncating to 2 lines", unlimitedLines) { AppSettings.setUnlimitedNameLines(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Show Total Videos Chip", "Show total video count badge in folder header", showTotalVideos) { AppSettings.setShowTotalVideosChip(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Show Total Duration Chip", "Show cumulative playback time in folder header", showTotalDuration) { AppSettings.setShowTotalDurationChip(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Show NEW Badge", "Highlight recently added files on Home & Folder screens", showNewBadge) { AppSettings.setShowNewBadge(it) }
            }
        }

        // Card 5: Thumbnails
        item {
            SettingsCard {
                Text("Thumbnails", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SwitchPreferenceRow("Folder Collage Thumbnails", "Preview cover frames on folder cards", showFolderThumb) { AppSettings.setShowFolderThumbnails(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Generate Network Stream Thumbnails", "Sample frames over HTTP/WebDAV (uses bandwidth)", netThumbs) { AppSettings.setNetworkThumbnailsEnabled(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Thumbnail Cache Limit", fontWeight = FontWeight.Medium)
                    Text("$thumbCacheMb MB", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Slider(value = thumbCacheMb.toFloat(), onValueChange = { AppSettings.setThumbnailCacheSizeMb(it.roundToInt()) }, valueRange = 32f..512f)
            }
        }

        item {
            OutlinedButton(
                onClick = { AppSettings.resetAppearancePreferences() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Reset Appearance Defaults")
            }
        }
    }
}

// 3. Navigation & Library Interface (Show/Hide & Reorder ALL bottom nav + Music tabs, FAB & Watched Threshold)
@Composable
private fun NavigationDetailScreen(onBack: () -> Unit) {
    val orderedIds by AppSettings.navTabOrder.collectAsState()
    val enabledIds by AppSettings.enabledNavTabs.collectAsState()
    val musicTabOrder by AppSettings.musicTabOrder.collectAsState()
    val musicHiddenTabs by AppSettings.musicHiddenTabs.collectAsState()
    val showQuickPlayFab by AppSettings.showQuickPlayFab.collectAsState()
    val quickPlayFabDirect by AppSettings.quickPlayFabDirect.collectAsState()
    val dualPaneTablet by AppSettings.dualPaneTablet.collectAsState()
    val watchedThreshold by AppSettings.watchedThresholdPct.collectAsState()

    val tabMeta = remember {
        mapOf(
            "HOME" to ("Home" to "Video library, folders and tree browser"),
            "MUSIC" to ("Music" to "Local music player, queue & notification controls"),
            "RECENTS" to ("Recents" to "Playback history with resume positions"),
            "PLAYLISTS" to ("Playlists" to "Custom persistent video queues"),
            "STREAMS" to ("Streams" to "Direct HLS, DASH, HTTP & RTSP streams"),
            "NETWORK" to ("Network" to "WebDAV, HTTP Directory & M3U LAN browser"),
            "VAULT" to ("Vault" to "PIN-protected private video list")
        )
    }

    SettingsSubScreen(title = "Navigation & Home", subtitle = "Toggle ANY tab, reorder rails, Music tabs & Quick Play", onBack = onBack) {
        item {
            SettingsCard {
                Text("Bottom Navigation Tabs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("At least one destination remains active.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(4.dp))
                orderedIds.forEachIndexed { index, tabId ->
                    val (label, desc) = tabMeta[tabId] ?: (tabId to "")
                    val isEnabled = tabId in enabledIds
                    val canDisable = !isEnabled || enabledIds.size > 1
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, fontWeight = FontWeight.SemiBold)
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(
                            onClick = { AppSettings.moveNavTab(tabId, -1) },
                            enabled = index > 0
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move Up")
                        }
                        IconButton(
                            onClick = { AppSettings.moveNavTab(tabId, 1) },
                            enabled = index < orderedIds.lastIndex
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move Down")
                        }
                        Switch(
                            checked = isEnabled,
                            enabled = canDisable,
                            onCheckedChange = { AppSettings.setNavTabEnabled(tabId, it) }
                        )
                    }
                    if (index < orderedIds.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                    }
                }
            }
        }

        item {
            SettingsCard {
                Text("Music Sub-Tabs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                musicTabOrder.forEachIndexed { index, mTab ->
                    val visible = mTab !in musicHiddenTabs
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = mTab.lowercase().replaceFirstChar { it.uppercase() },
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { AppSettings.moveMusicTab(mTab, -1) },
                            enabled = index > 0
                        ) {
                            Icon(Icons.Rounded.ArrowUpward, contentDescription = "Move Up")
                        }
                        IconButton(
                            onClick = { AppSettings.moveMusicTab(mTab, 1) },
                            enabled = index < musicTabOrder.lastIndex
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, contentDescription = "Move Down")
                        }
                        Switch(
                            checked = visible,
                            enabled = mTab != "SONGS",
                            onCheckedChange = { AppSettings.toggleMusicTabVisibility(mTab) }
                        )
                    }
                    if (index < musicTabOrder.lastIndex) {
                        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                    }
                }
            }
        }

        item {
            SettingsCard {
                val enabledServers by AppSettings.enabledServerTabs.collectAsState()
                val configuredServers by AppSettings.mediaServers.collectAsState()
                Text("Optional Media Server Tabs", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text(
                    "Shown in bottom navigation only when configured or explicitly enabled. Folders remain inside the Home browser.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                listOf(
                    "JELLYFIN" to "Jellyfin (Movies, Shows & Libraries)",
                    "NAVIDROME" to "Navidrome (Music & Playlists)",
                    "AUDIOBOOKS" to "Audiobookshelf (Books & Chapters)"
                ).forEach { (srvId, srvLabel) ->
                    val hasProfile = configuredServers.any {
                        (srvId == "JELLYFIN" && it.kind == com.cmstudio.player.ui.servers.MediaServerKind.JELLYFIN) ||
                            (srvId == "NAVIDROME" && it.kind == com.cmstudio.player.ui.servers.MediaServerKind.NAVIDROME) ||
                            (srvId == "AUDIOBOOKS" && it.kind == com.cmstudio.player.ui.servers.MediaServerKind.AUDIOBOOKSHELF)
                    }
                    SwitchPreferenceRow(
                        title = srvLabel,
                        subtitle = if (hasProfile) "Server configured" else "Enable to add & connect a server",
                        checked = hasProfile || srvId in enabledServers,
                        onCheckedChange = { AppSettings.setServerTabEnabled(srvId, it) }
                    )
                }
            }
        }

        item {
            SettingsCard {
                Text("Home Quick Actions & Progress", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SwitchPreferenceRow("Show Floating Quick Play Button", "Display expandable Quick Play FAB on Home screen", showQuickPlayFab) { AppSettings.setShowQuickPlayFab(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Direct Quick Play", "Resume last video immediately on tap without opening menu", quickPlayFabDirect) { AppSettings.setQuickPlayFabDirect(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Tablet Dual-Pane Split View", "Show folder videos beside folder rail on large screens", dualPaneTablet) { AppSettings.setDualPaneTablet(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Mark Watched Threshold", fontWeight = FontWeight.Medium)
                    Text("$watchedThreshold%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = watchedThreshold.toFloat(),
                    onValueChange = { AppSettings.setWatchedThresholdPct(it.roundToInt()) },
                    valueRange = 50f..100f
                )
            }
        }

        item {
            OutlinedButton(
                onClick = { AppSettings.resetNavigationPreferences() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Restore Default Navigation")
            }
        }
    }
}

// 4. Player Layout & HUD (Visual Landscape/Portrait Summary, Drag Editor, Live Seekbar Previews, Overlays)
@Composable
private fun VideoPlayerDetailScreen(onBack: () -> Unit) {
    var selectedSlot by remember { mutableStateOf(com.cmstudio.player.ui.player.PlayerControlCatalog.SLOT_TOP_RIGHT) }

    SettingsSubScreen(title = "Player Layout", subtitle = "Controls layout, seekbar styles, overlays & screenshots", onBack = onBack) {
        item {
            PlayerControlsVisualSummaryCard(
                activeSlot = selectedSlot,
                onSelectSlotToEdit = { selectedSlot = it }
            )
        }
        item {
            ControlLayoutEditorCard(
                activeSlot = selectedSlot,
                onActiveSlotChange = { selectedSlot = it }
            )
        }
        item {
            SeekbarStyleLivePreviewCard()
        }
        item {
            PlayerHudAppearanceCard()
        }
        item {
            OverlayAndScreenshotSettingsCards()
        }
        item {
            OutlinedButton(
                onClick = { AppSettings.resetHudLayoutToDefault() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Reset All Player HUD Preferences")
            }
        }
    }
}

// 5. Playback
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PlaybackDetailScreen(onBack: () -> Unit) {
    val speed by AppSettings.defaultSpeed.collectAsState()
    val pitchCorrection by AppSettings.audioPitchCorrection.collectAsState()
    val seekInterval by AppSettings.seekIntervalSeconds.collectAsState()
    val fastSeek by AppSettings.fastSeekKeyframes.collectAsState()
    val autoNext by AppSettings.autoPlayNext.collectAsState()
    val rememberAspect by AppSettings.rememberAspectPerVideo.collectAsState()
    val closeAfterEnd by AppSettings.closeAfterEndOfPlayback.collectAsState()
    val autoplayAfterUnlock by AppSettings.autoplayAfterScreenUnlock.collectAsState()
    val introOutroEnabled by AppSettings.introOutroEnabled.collectAsState()
    val introProvider by AppSettings.introProvider.collectAsState()
    val detectIntroChapters by AppSettings.detectIntroFromChapters.collectAsState()
    val autoSkipIntro by AppSettings.autoSkipIntro.collectAsState()
    val autoSkipOutro by AppSettings.autoSkipOutro.collectAsState()
    val customIntroKw by AppSettings.customIntroKeywords.collectAsState()
    val customOutroKw by AppSettings.customOutroKeywords.collectAsState()
    val showProviderStatus by AppSettings.showProviderStatusOverlay.collectAsState()

    SettingsSubScreen(title = "Playback", subtitle = "Speed, seeking & queue progression", onBack = onBack) {
        item {
            SettingsCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Default Speed", fontWeight = FontWeight.SemiBold)
                    Text(String.format(Locale.US, "%.2fx", speed), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = speed.coerceIn(0.5f, 2.5f),
                    onValueChange = { AppSettings.setDefaultSpeed((it * 20f).roundToInt() / 20f) },
                    valueRange = 0.5f..2.5f
                )
                SwitchPreferenceRow("Audio Pitch Correction", "Preserve vocal pitch when playing faster or slower", pitchCorrection) {
                    AppSettings.setAudioPitchCorrection(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Fast Seek (Nearest Keyframe)", "Seek faster along keyframes (OFF = frame-exact seek)", fastSeek) {
                    AppSettings.setFastSeekKeyframes(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Auto-Play Next Video", "Play next item automatically when current file finishes", autoNext) {
                    AppSettings.setAutoPlayNext(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Remember Aspect Ratio per Video", "Save custom aspect choices per file or stream", rememberAspect) {
                    AppSettings.setRememberAspectPerVideo(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Close After End of Playback", "Exit player cleanly when playback reaches the end", closeAfterEnd) {
                    AppSettings.setCloseAfterEndOfPlayback(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Autoplay After Screen Unlock", "Resume playback automatically after unlocking screen", autoplayAfterUnlock) {
                    AppSettings.setAutoplayAfterScreenUnlock(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("HUD Rewind / Forward Interval", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 30).forEach { sec ->
                        FilterChip(selected = seekInterval == sec, onClick = { AppSettings.setSeekIntervalSeconds(sec) }, label = { Text("${sec}s") })
                    }
                }
            }
        }
        item {
            SettingsCard {
                Text("Intro & Outro Detection", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                SwitchPreferenceRow("Enable Intro / Outro Skipper", "Show Skip Intro & Skip Outro buttons during detected segments", introOutroEnabled) {
                    AppSettings.setIntroOutroEnabled(it)
                }
                if (introOutroEnabled) {
                    HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                    Text("Marker Provider", fontWeight = FontWeight.SemiBold)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf(
                            "HYBRID" to "Hybrid",
                            "CHAPTERS" to "Chapters Only",
                            "INTRODB" to "IntroDB",
                            "THEINTRODB" to "TheIntroDB",
                            "ANISKIP" to "AniSkip"
                        ).forEach { (id, lbl) ->
                            FilterChip(selected = introProvider == id, onClick = { AppSettings.setIntroProvider(id) }, label = { Text(lbl) })
                        }
                    }
                    SwitchPreferenceRow("Chapter-Title Detection", "Detect Intro/OP/Outro/ED from embedded MKV chapters", detectIntroChapters) {
                        AppSettings.setDetectIntroFromChapters(it)
                    }
                    SwitchPreferenceRow("Auto-Skip Intro", "Automatically jump past opening/intro markers", autoSkipIntro) {
                        AppSettings.setAutoSkipIntro(it)
                    }
                    SwitchPreferenceRow("Auto-Skip Outro", "Automatically jump past ending/credit markers", autoSkipOutro) {
                        AppSettings.setAutoSkipOutro(it)
                    }
                    SwitchPreferenceRow("Show Marker Provider Status Pill", "Display lookup feedback when online markers load or fall back", showProviderStatus) {
                        AppSettings.setShowProviderStatusOverlay(it)
                    }
                    OutlinedTextField(
                        value = customIntroKw,
                        onValueChange = { AppSettings.setCustomIntroKeywords(it) },
                        label = { Text("Custom Opening Keywords (comma-separated)") },
                        placeholder = { Text("opening, op, cold open") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = customOutroKw,
                        onValueChange = { AppSettings.setCustomOutroKeywords(it) },
                        label = { Text("Custom Ending Keywords (comma-separated)") },
                        placeholder = { Text("ending, ed, credits") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        item {
            OutlinedButton(onClick = { AppSettings.resetPlaybackAndPlayerPreferences() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("Reset Playback Preferences")
            }
        }
    }
}

// 6. Video Rendering
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun VideoRenderingDetailScreen(onBack: () -> Unit) {
    val surfaceType by AppSettings.videoSurfaceType.collectAsState()
    val aspect by AppSettings.defaultAspectRatio.collectAsState()
    val smoothMotion by AppSettings.smoothMotion120fps.collectAsState()
    val dv7Map by AppSettings.autoRoutingDolbyVision.collectAsState()

    SettingsSubScreen(title = "Video Rendering", subtitle = "Surface output, aspect ratio & frame pacing", onBack = onBack) {
        item {
            SettingsCard {
                Text("Video Output Surface", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = surfaceType == "SURFACE_VIEW",
                        onClick = { AppSettings.setVideoSurfaceType("SURFACE_VIEW") },
                        label = { Text("SurfaceView (Recommended)") }
                    )
                    FilterChip(
                        selected = surfaceType == "TEXTURE_VIEW",
                        onClick = { AppSettings.setVideoSurfaceType("TEXTURE_VIEW") },
                        label = { Text("TextureView") }
                    )
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("Default Aspect Ratio", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Fit", "Fill", "Zoom", "Stretch", "Original", "16:9", "4:3").forEach { mode ->
                        FilterChip(selected = aspect == mode, onClick = { AppSettings.setDefaultAspectRatio(mode) }, label = { Text(mode) })
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Match Video Frame Rate", "Hint Android 11+ Surface to match source frame rate for smoother playback", smoothMotion) {
                    AppSettings.setSmoothMotion(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Map Dolby Vision Profile 7 to HEVC", "Fallback cleanly to HEVC 10-bit base layer on devices without DV7", dv7Map) {
                    AppSettings.setAutoRoutingDolbyVision(it)
                }
            }
        }
    }
}

// 7. Decoder & Dual Engine (MPV libmpv + Media3 ExoPlayer w/ libass, Shaders & Codec Inspector)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DecoderEngineDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val defaultEngine by AppSettings.defaultEngine.collectAsState()
    val autoDv by AppSettings.autoRoutingDolbyVision.collectAsState()
    val autoStreams by AppSettings.autoRoutingHlsDash.collectAsState()
    val decoderPriority by AppSettings.decoderPriority.collectAsState()
    val mpvProfile by AppSettings.mpvProfile.collectAsState()
    val useGpuNext by AppSettings.useGpuNext.collectAsState()
    val useVulkan by AppSettings.useVulkan.collectAsState()
    val useYuv420p by AppSettings.useYuv420p.collectAsState()
    val hdrTonemap by AppSettings.hdrTonemapping.collectAsState()
    val debandMode by AppSettings.mpvDebanding.collectAsState()
    val debandIterations by AppSettings.debandIterations.collectAsState()
    val debandThreshold by AppSettings.debandThreshold.collectAsState()
    val debandRange by AppSettings.debandRange.collectAsState()
    val debandGrain by AppSettings.debandGrain.collectAsState()
    val animeMode by AppSettings.anime4kMode.collectAsState()
    val animeQuality by AppSettings.anime4kQuality.collectAsState()
    val minBuf by AppSettings.minBufferMs.collectAsState()
    val maxBuf by AppSettings.maxBufferMs.collectAsState()
    val bufPlay by AppSettings.bufferForPlaybackMs.collectAsState()
    var showBufferDialog by remember { mutableStateOf(false) }
    val codecReport = remember { runCatching { CodecInspector.inspect(context) }.getOrNull() }

    SettingsSubScreen(title = "Decoder & Dual Engine", subtitle = "libmpv shaders, Native ExoPlayer ASS, routing & hardware codecs", onBack = onBack) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Dual Playback Pipeline Active", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                    Text(
                        "MPVX pairs libmpv (GPU shaders, Anime4K, debanding) with AndroidX Media3 1.10.0 ExoPlayer (ass-media Canvas ASS + platform HDR/DV & adaptive streams).",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        // Card 1: Engine Selection & Smart Routing
        item {
            SettingsCard {
                Text("Default Playback Engine", fontWeight = FontWeight.Bold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "AUTO" to "Auto (Smart Routing)",
                        "MPV" to "MPV (libmpv)",
                        "NATIVE" to "Native (ExoPlayer)"
                    ).forEach { (id, label) ->
                        FilterChip(selected = defaultEngine == id, onClick = { AppSettings.setDefaultEngine(id) }, label = { Text(label) })
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Auto-Route HDR / Dolby Vision to ExoPlayer", "Uses Native SurfaceFlinger HDR/DV passthrough in Auto mode", autoDv) {
                    AppSettings.setAutoRoutingDolbyVision(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Auto-Route Network Streams to ExoPlayer", "Uses buffered HLS/DASH/RTSP Media3 sources in Auto mode", autoStreams) {
                    AppSettings.setAutoRoutingHlsDash(it)
                }
            }
        }

        // Card 2: MPV Renderer, Vulkan, Debanding & MIT Anime4K
        item {
            SettingsCard {
                Text("MPV Rendering & Backend", fontWeight = FontWeight.Bold)
                Text("Quality Profile", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("fast" to "Fast", "high-quality" to "High Quality", "gpu-hq" to "GPU-HQ").forEach { (id, lbl) ->
                        FilterChip(selected = mpvProfile == id, onClick = { AppSettings.setMpvProfile(id) }, label = { Text(lbl) })
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Use gpu-next Renderer", "Enable modern experimental libplacebo-based video output", useGpuNext) { AppSettings.setUseGpuNext(it) }
                SwitchPreferenceRow("Vulkan GPU Backend (Opt-in)", "Use Vulkan androidvk context with automatic OpenGL fallback", useVulkan) { AppSettings.setUseVulkan(it) }
                SwitchPreferenceRow("Force YUV420P Pixel Format", "Workaround green/washed frames on specific Mali GPUs", useYuv420p) { AppSettings.setUseYuv420p(it) }
                SwitchPreferenceRow("HDR Tone Mapping (bt.2446a)", "Dynamic HDR-to-SDR mapping on standard displays", hdrTonemap) { AppSettings.setHdrTonemapping(it) }

                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("Debanding Filter", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("NONE" to "Off", "GPU" to "GPU", "CPU" to "CPU").forEach { (id, lbl) ->
                        FilterChip(selected = debandMode == id, onClick = { AppSettings.setMpvDebanding(id) }, label = { Text(lbl) })
                    }
                }
                if (debandMode != "NONE") {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Deband Iterations", style = MaterialTheme.typography.bodySmall)
                        Text("$debandIterations", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(value = debandIterations.toFloat(), onValueChange = { AppSettings.setDebandIterations(it.roundToInt()) }, valueRange = 1f..4f, steps = 2)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Deband Threshold", style = MaterialTheme.typography.bodySmall)
                        Text("$debandThreshold", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(value = debandThreshold.toFloat(), onValueChange = { AppSettings.setDebandThreshold(it.roundToInt()) }, valueRange = 0f..256f)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Deband Range", style = MaterialTheme.typography.bodySmall)
                        Text("$debandRange", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(value = debandRange.toFloat(), onValueChange = { AppSettings.setDebandRange(it.roundToInt()) }, valueRange = 1f..64f)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Deband Grain", style = MaterialTheme.typography.bodySmall)
                        Text("$debandGrain", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Slider(value = debandGrain.toFloat(), onValueChange = { AppSettings.setDebandGrain(it.roundToInt()) }, valueRange = 0f..256f)
                }

                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("Anime4K GLSL Shaders (MIT)", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("OFF" to "Off", "MODE_A" to "Mode A", "MODE_B" to "Mode B", "MODE_C" to "Mode C", "MODE_AA" to "Mode A+A").forEach { (id, lbl) ->
                        FilterChip(selected = animeMode == id, onClick = { AppSettings.setAnime4kMode(id) }, label = { Text(lbl) })
                    }
                }
                if (animeMode != "OFF") {
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        listOf("FAST" to "Fast (S)", "BALANCED" to "Balanced (M)", "HQ" to "High Quality (L)").forEach { (q, lbl) ->
                            FilterChip(selected = animeQuality == q, onClick = { AppSettings.setAnime4kQuality(q) }, label = { Text(lbl) })
                        }
                    }
                }
            }
        }

        // Card 3: Decoder Priority & LoadControl Buffers
        item {
            SettingsCard {
                Text("Decoder Priority", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("AUTO" to "Auto (HW+)", "HW_ONLY" to "HW Only", "SW_PREFER" to "SW Prefer").forEach { (id, label) ->
                        FilterChip(selected = decoderPriority == id, onClick = { AppSettings.setDecoderPriority(id) }, label = { Text(label) })
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("LoadControl Buffers", fontWeight = FontWeight.SemiBold)
                Text("Min: ${minBuf / 1000}s • Max: ${maxBuf / 1000}s • Start: ${bufPlay / 1000}s", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Button(onClick = { showBufferDialog = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Configure Playback Buffers")
                }
            }
        }

        // Card 4: Inline Device Codec Summary & Detailed Inspector
        if (codecReport != null) {
            item {
                SettingsCard {
                    Text("Device Hardware Codecs", fontWeight = FontWeight.Bold)
                    Text(
                        "${codecReport.hardwareCount} Hardware • ${codecReport.softwareCount} Software (${codecReport.videoCount} Video, ${codecReport.audioCount} Audio)",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                    codecReport.keySummaries.take(5).forEach { summary ->
                        HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(summary.formatName, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    "${summary.defaultDecoderName ?: "Software"} • Max ${summary.maxResolution ?: "Unknown"}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (summary.hasHardware) "HW" else "SW",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (summary.hasHardware) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }
            }
        }
        item {
            OutlinedButton(
                onClick = { context.startActivity(Intent(context, CodecCapabilitiesActivity::class.java)) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Open Full Hardware Codec Inspector")
            }
        }
    }

    if (showBufferDialog) {
        var localMin by remember { mutableStateOf((minBuf / 1000).toString()) }
        var localMax by remember { mutableStateOf((maxBuf / 1000).toString()) }
        var localPlay by remember { mutableStateOf((bufPlay / 1000).toString()) }
        AlertDialog(
            onDismissRequest = { showBufferDialog = false },
            title = { Text("LoadControl Buffers (Seconds)", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(value = localMin, onValueChange = { localMin = it }, label = { Text("Min Buffer (sec)") }, singleLine = true)
                    OutlinedTextField(value = localMax, onValueChange = { localMax = it }, label = { Text("Max Buffer (sec)") }, singleLine = true)
                    OutlinedTextField(value = localPlay, onValueChange = { localPlay = it }, label = { Text("Start Playback (sec)") }, singleLine = true)
                }
            },
            confirmButton = {
                Button(onClick = {
                    val mMin = (localMin.toIntOrNull() ?: 50).coerceIn(2, 120) * 1000
                    val mMax = (localMax.toIntOrNull() ?: 50).coerceIn(5, 180) * 1000
                    val mPlay = (localPlay.toIntOrNull() ?: 2).coerceIn(1, 30) * 1000
                    AppSettings.setMedia3Buffers(mMin, maxOf(mMin, mMax), mPlay, mPlay * 2)
                    showBufferDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showBufferDialog = false }) { Text("Cancel") }
            }
        )
    }
}

// 8. Audio
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AudioDetailScreen(onBack: () -> Unit) {
    val audioLang by AppSettings.audioLanguage.collectAsState()
    val boost by AppSettings.audioBoost.collectAsState()
    val offload by AppSettings.audioOffload.collectAsState()
    val nightMode by AppSettings.nightModeCompressor.collectAsState()
    val eqPreset by AppSettings.eqPreset.collectAsState()
    val delayMs by AppSettings.audioDelayMs.collectAsState()
    val standbySec by AppSettings.audioStandbyDelaySec.collectAsState()
    val vizStyle by AppSettings.musicVisualizerStyle.collectAsState()
    val musicOrient by AppSettings.musicPlayerOrientation.collectAsState()

    SettingsSubScreen(title = "Audio", subtitle = "Language, equalizer, boost & audio sync", onBack = onBack) {
        item {
            SettingsCard {
                Text("Preferred Audio Language", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Auto" to "Auto", "en" to "English", "hi" to "Hindi", "ja" to "Japanese", "es" to "Spanish").forEach { (code, label) ->
                        FilterChip(selected = audioLang == code, onClick = { AppSettings.setAudioLanguage(code) }, label = { Text(label) })
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Default Volume Boost", fontWeight = FontWeight.SemiBold)
                    Text("$boost%", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(value = boost.toFloat().coerceIn(50f, 200f), onValueChange = { AppSettings.setAudioBoost(it.roundToInt()) }, valueRange = 50f..200f)
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Audio Offload", "Offload supported PCM/compressed audio to DSP for battery efficiency", offload) {
                    AppSettings.setAudioOffload(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Night Mode Limiter", "Compress sudden loud peaks (Android 9+ DynamicsProcessing)", nightMode) {
                    AppSettings.setNightModeCompressor(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("Equalizer Preset", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Flat", "Cinema", "Bass Boost", "Vocal Clarifier", "Rock").forEach { preset ->
                        FilterChip(selected = eqPreset == preset, onClick = { AppSettings.setEqualizerPreset(preset) }, label = { Text(preset) })
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Default Audio Sync Offset", fontWeight = FontWeight.SemiBold)
                        Text("$delayMs ms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { AppSettings.setAudioDelayMs((delayMs - 100).coerceIn(-5000, 5000)) }) { Text("-100ms") }
                        OutlinedButton(onClick = { AppSettings.setAudioDelayMs(0) }) { Text("0") }
                        OutlinedButton(onClick = { AppSettings.setAudioDelayMs((delayMs + 100).coerceIn(-5000, 5000)) }) { Text("+100ms") }
                    }
                }
            }
        }
        item {
            SettingsCard {
                Text("Music Player Behavior & Appearance", fontWeight = FontWeight.SemiBold)
                Text("Visualizer Style", fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("WAVE" to "Wave", "BARS" to "Bars", "RING" to "Pulse Ring", "OFF" to "Off").forEach { (id, lbl) ->
                        FilterChip(selected = vizStyle == id, onClick = { AppSettings.setMusicVisualizerStyle(id) }, label = { Text(lbl) })
                    }
                }
                Text("Audio Player Orientation", fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("AUTO" to "Auto", "PORTRAIT" to "Portrait", "LANDSCAPE" to "Landscape").forEach { (id, lbl) ->
                        FilterChip(selected = musicOrient == id, onClick = { AppSettings.setMusicPlayerOrientation(id) }, label = { Text(lbl) })
                    }
                }
                Text("Audio Standby Auto-Release Delay", fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "Never", 60 to "1 min", 300 to "5 min", 900 to "15 min").forEach { (sec, lbl) ->
                        FilterChip(selected = standbySec == sec, onClick = { AppSettings.setAudioStandbyDelaySec(sec) }, label = { Text(lbl) })
                    }
                }
            }
        }
        item {
            AdvancedAudioSettingsExtrasCard()
        }
        item {
            OutlinedButton(onClick = { AppSettings.resetAudioPreferences() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("Reset Audio Preferences")
            }
        }
    }
}

// 9. Subtitles
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SubtitlesDetailScreen(onBack: () -> Unit) {
    val enabled by AppSettings.subtitlesEnabled.collectAsState()
    val lang by AppSettings.subtitleLanguage.collectAsState()
    val fontSize by AppSettings.subtitleFontSize.collectAsState()
    val edgeType by AppSettings.subtitleEdgeType.collectAsState()
    val bgOpacity by AppSettings.subtitleBgOpacity.collectAsState()
    val bottomPadding by AppSettings.subtitleBottomPaddingDp.collectAsState()
    val bold by AppSettings.subtitleBold.collectAsState()
    val delayMs by AppSettings.subtitleDelayMs.collectAsState()
    val onlineMode by AppSettings.onlineSubSearchMode.collectAsState()
    val onlineLang by AppSettings.onlineSubPreferredLang.collectAsState()
    var subdlKeyInput by remember { mutableStateOf(AppSettings.getSubdlApiKey()) }

    SettingsSubScreen(title = "Subtitles", subtitle = "Caption rendering, styling & delay offset", onBack = onBack) {
        item {
            SettingsCard {
                SwitchPreferenceRow("Enable Subtitles by Default", "Load embedded and matching sidecar subtitles automatically", enabled) {
                    AppSettings.setSubtitlesEnabled(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("Preferred Subtitle Language", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("Auto" to "Auto", "en" to "English", "hi" to "Hindi", "ja" to "Japanese", "es" to "Spanish").forEach { (code, label) ->
                        FilterChip(selected = lang == code, onClick = { AppSettings.setSubtitleLanguage(code) }, label = { Text(label) })
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Font Size", fontWeight = FontWeight.SemiBold)
                    Text("${fontSize.toInt()} sp", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Slider(value = fontSize.coerceIn(12f, 44f), onValueChange = { AppSettings.setSubtitleFontSize(it) }, valueRange = 12f..44f)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Background Opacity", fontWeight = FontWeight.SemiBold)
                    Text("${(bgOpacity * 100).roundToInt()}%", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Slider(value = bgOpacity.coerceIn(0f, 1f), onValueChange = { AppSettings.setSubtitleBgOpacity(it) }, valueRange = 0f..1f)

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bottom Offset", fontWeight = FontWeight.SemiBold)
                    Text("${bottomPadding} dp", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                }
                Slider(value = bottomPadding.toFloat().coerceIn(0f, 120f), onValueChange = { AppSettings.setSubtitleBottomPaddingDp(it.roundToInt()) }, valueRange = 0f..120f)

                Text("Edge Style", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("DROP_SHADOW" to "Shadow", "OUTLINE" to "Outline", "NONE" to "None", "RAISED" to "Raised").forEach { (k, label) ->
                        FilterChip(selected = edgeType == k, onClick = { AppSettings.setSubtitleEdgeType(k) }, label = { Text(label) })
                    }
                }
                SwitchPreferenceRow("Bold Subtitle Text", "Render cues in bold weight", bold) { AppSettings.setSubtitleBold(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Default Subtitle Sync", fontWeight = FontWeight.SemiBold)
                        Text("$delayMs ms", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { AppSettings.setSubtitleDelayMs((delayMs - 100).coerceIn(-10000, 10000)) }) { Text("-100ms") }
                        OutlinedButton(onClick = { AppSettings.setSubtitleDelayMs(0) }) { Text("0") }
                        OutlinedButton(onClick = { AppSettings.setSubtitleDelayMs((delayMs + 100).coerceIn(-10000, 10000)) }) { Text("+100ms") }
                    }
                }
            }
        }
        item {
            SettingsCard {
                Text("Online Subtitle Search", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                Text("Search Mode", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("HYBRID" to "Hybrid", "WYZIE" to "Wyzie", "SUBHUB" to "SubHub (SubDL)").forEach { (id, lbl) ->
                        FilterChip(selected = onlineMode == id, onClick = { AppSettings.setOnlineSubSearchMode(id) }, label = { Text(lbl) })
                    }
                }
                Text("Default Search Language", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("en" to "English", "hi" to "Hindi", "es" to "Spanish", "ja" to "Japanese", "all" to "All").forEach { (code, lbl) ->
                        FilterChip(selected = onlineLang == code, onClick = { AppSettings.setOnlineSubPreferredLang(code) }, label = { Text(lbl) })
                    }
                }
                OutlinedTextField(
                    value = subdlKeyInput,
                    onValueChange = {
                        subdlKeyInput = it
                        AppSettings.setSubdlApiKey(it)
                    },
                    label = { Text("SubDL API Key (Optional, encrypted)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
        item {
            AdvancedSubtitleSettingsExtrasCard()
        }
        item {
            OutlinedButton(onClick = { AppSettings.resetSubtitlePreferences() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("Reset Subtitle Preferences")
            }
        }
    }
}

// 10. Gestures
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun GesturesDetailScreen(onBack: () -> Unit) {
    val vol by AppSettings.volumeGesture.collectAsState()
    val bright by AppSettings.brightnessGesture.collectAsState()
    val seek by AppSettings.seekGesture.collectAsState()
    val doubleTap by AppSettings.doubleTapSeconds.collectAsState()
    val doubleTapPct by AppSettings.doubleTapAreaWidthPct.collectAsState()
    val doubleTapCenterPP by AppSettings.doubleTapCenterPlayPause.collectAsState()
    val doubleTapPauseResume by AppSettings.doubleTapPauseResume.collectAsState()
    val singleTapPause by AppSettings.useSingleTapToPause.collectAsState()
    val volOnRight by AppSettings.displayVolumeOnRight.collectAsState()
    val rememberBright by AppSettings.rememberBrightness.collectAsState()
    val longPress2x by AppSettings.longPress2x.collectAsState()
    val longPressVar by AppSettings.longPressSpeedVariable.collectAsState()
    val holdMult by AppSettings.holdSpeedMultiplier.collectAsState()
    val hSens by AppSettings.horizontalSwipeSensitivity.collectAsState()
    val showSeekHud by AppSettings.showGestureSeekOverlay.collectAsState()
    val showVbHud by AppSettings.showGestureVolBrightOverlay.collectAsState()
    val swipeUpPl by AppSettings.swipeUpOpensPlaylist.collectAsState()
    val pinchZoom by AppSettings.pinchZoomEnabled.collectAsState()
    val haptics by AppSettings.hapticFeedback.collectAsState()
    val allowGesturesInPanels by AppSettings.allowGesturesInPanels.collectAsState()
    val dtLeftAction by AppSettings.doubleTapLeftAction.collectAsState()
    val dtCenterAction by AppSettings.doubleTapCenterAction.collectAsState()
    val dtRightAction by AppSettings.doubleTapRightAction.collectAsState()

    SettingsSubScreen(title = "Gestures", subtitle = "3-zone touch controls, swipe sensitivity & speed hold", onBack = onBack) {
        item {
            SettingsCard {
                SwitchPreferenceRow("Volume on Right Side", "Off swaps Volume to Left and Brightness to Right", volOnRight) {
                    AppSettings.setDisplayVolumeOnRight(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow(
                    if (volOnRight) "Brightness Swipe (Left Side)" else "Brightness Swipe (Right Side)",
                    "Swipe vertically to adjust display brightness",
                    bright
                ) { AppSettings.setBrightnessGesture(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow(
                    if (volOnRight) "Volume Swipe (Right Side)" else "Volume Swipe (Left Side)",
                    "Swipe vertically to adjust media volume & boost",
                    vol
                ) { AppSettings.setVolumeGesture(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Remember Brightness", "Persist last manually adjusted brightness across videos", rememberBright) {
                    AppSettings.setRememberBrightness(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Horizontal Seek Scrub", "Drag horizontally across player to scrub timeline", seek) { AppSettings.setSeekGesture(it) }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Seek Scrub Sensitivity", fontWeight = FontWeight.Medium)
                    Text(String.format(Locale.US, "%.1fx", hSens), fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = hSens.coerceIn(0.4f, 2.5f),
                    onValueChange = { AppSettings.setHorizontalSwipeSensitivity(((it * 10f).roundToInt() / 10f).coerceIn(0.4f, 2.5f)) },
                    valueRange = 0.4f..2.5f
                )
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Double-Tap Center Play/Pause", "Double-tap center zone to toggle Play/Pause", doubleTapCenterPP) {
                    AppSettings.setDoubleTapCenterPlayPause(it)
                }
                SwitchPreferenceRow("Double-Tap Pause/Resume Fallback", "Allow pause/resume when double-tapping outside skip zones", doubleTapPauseResume) {
                    AppSettings.setDoubleTapPauseResume(it)
                }
                SwitchPreferenceRow("Single-Tap Toggles Play/Pause", "Pause or resume on single screen tap", singleTapPause) {
                    AppSettings.setUseSingleTapToPause(it)
                }
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Edge Double-Tap Zone Width", fontWeight = FontWeight.Medium)
                    Text("$doubleTapPct% each side", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }
                Slider(
                    value = doubleTapPct.toFloat().coerceIn(10f, 45f),
                    onValueChange = { AppSettings.setDoubleTapAreaWidthPct(it.roundToInt()) },
                    valueRange = 10f..45f
                )
                Text("Double-Tap Seek Step", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 10, 15, 20, 30).forEach { sec ->
                        FilterChip(selected = doubleTap == sec, onClick = { AppSettings.setDoubleTapSeconds(sec) }, label = { Text("${sec}s") })
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Hold Speed Playback (Swipe Up to Lock)", "Press & hold for temporary fast speed; swipe up to lock", longPress2x) {
                    AppSettings.setLongPress2x(it)
                }
                SwitchPreferenceRow("Variable Hold Speed Slide", "Slide vertically while holding to vary 1.5x–4.0x speed dynamically", longPressVar) {
                    AppSettings.setLongPressSpeedVariable(it)
                }
                Text("Hold Speed Multiplier (${String.format(Locale.US, "%.1fx", holdMult)})", fontWeight = FontWeight.Medium)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(1.5f, 2.0f, 2.5f, 3.0f, 4.0f).forEach { mult ->
                        FilterChip(
                            selected = kotlin.math.abs(holdMult - mult) < 0.05f,
                            onClick = { AppSettings.setHoldSpeedMultiplier(mult) },
                            label = { Text("${mult}x") }
                        )
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Swipe Up Center to Open Queue", "Swipe upward in center zone to browse video playlist", swipeUpPl) {
                    AppSettings.setSwipeUpOpensPlaylist(it)
                }
                SwitchPreferenceRow("Allow Gestures in Panels", "Keep video swipe & tap gestures active while sheets are open", allowGesturesInPanels) {
                    AppSettings.setAllowGesturesInPanels(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("Per-Zone Double-Tap Actions", fontWeight = FontWeight.SemiBold)
                Text("Left Zone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.cmstudio.player.ui.player.DoubleTapActionCatalog.ALL_ACTIONS.forEach { (id, lbl) ->
                        FilterChip(selected = dtLeftAction == id, onClick = { AppSettings.setDoubleTapLeftAction(id) }, label = { Text(lbl) })
                    }
                }
                Text("Center Zone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.cmstudio.player.ui.player.DoubleTapActionCatalog.ALL_ACTIONS.forEach { (id, lbl) ->
                        FilterChip(selected = dtCenterAction == id, onClick = { AppSettings.setDoubleTapCenterAction(id) }, label = { Text(lbl) })
                    }
                }
                Text("Right Zone", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.cmstudio.player.ui.player.DoubleTapActionCatalog.ALL_ACTIONS.forEach { (id, lbl) ->
                        FilterChip(selected = dtRightAction == id, onClick = { AppSettings.setDoubleTapRightAction(id) }, label = { Text(lbl) })
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Two-Finger Pinch to Zoom", "Pinch & pan inside the video viewport", pinchZoom) { AppSettings.setPinchZoomEnabled(it) }
                SwitchPreferenceRow("Show Seek Preview Card", "Display seek delta box when scrubbing", showSeekHud) { AppSettings.setShowGestureSeekOverlay(it) }
                SwitchPreferenceRow("Show Volume/Brightness Bars", "Display side meters during vertical swipes", showVbHud) { AppSettings.setShowGestureVolBrightOverlay(it) }
                SwitchPreferenceRow("Haptic Feedback", "Subtle tactile confirmation on seek and nav interactions", haptics) { AppSettings.setHapticFeedback(it) }
            }
        }
        item {
            OutlinedButton(onClick = { AppSettings.resetGestureAndDisplayPreferences() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("Reset Gesture Preferences")
            }
        }
    }
}

// 11. Screen & Orientation (All 8 Modes)
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ScreenOrientationDetailScreen(onBack: () -> Unit) {
    val orientation by AppSettings.screenOrientation.collectAsState()
    val keepOn by AppSettings.keepScreenOn.collectAsState()
    val keepOnPaused by AppSettings.keepScreenOnWhenPaused.collectAsState()

    SettingsSubScreen(title = "Screen & Orientation", subtitle = "Auto-matches vertical/horizontal videos & 8 locks", onBack = onBack) {
        item {
            SettingsCard {
                Text("Default Player Orientation", fontWeight = FontWeight.SemiBold)
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    com.cmstudio.player.ui.player.PlayerControlCatalog.ORIENTATION_MODES.forEach { mode ->
                        FilterChip(selected = orientation == mode, onClick = { AppSettings.setScreenOrientation(mode) }, label = { Text(mode) })
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Keep Screen On During Playback", "Prevent display sleep while video is actively playing", keepOn) {
                    AppSettings.setKeepScreenOn(it)
                }
                SwitchPreferenceRow("Keep Screen On When Paused", "Prevent screen timeout even while playback is paused", keepOnPaused) {
                    AppSettings.setKeepScreenOnWhenPaused(it)
                }
            }
        }
    }
}

// 12. Background Playback
@Composable
private fun BackgroundPlaybackDetailScreen(onBack: () -> Unit) {
    val bgPlay by AppSettings.backgroundPlayback.collectAsState()
    val autoPip by AppSettings.autoPiP.collectAsState()
    val pipHomeOnly by AppSettings.pipOnHomeGestureOnly.collectAsState()
    val miniPlayer by AppSettings.videoMiniPlayerEnabled.collectAsState()

    SettingsSubScreen(title = "Background Playback", subtitle = "Picture-in-Picture & background audio", onBack = onBack) {
        item {
            SettingsCard {
                SwitchPreferenceRow("Continue Video Audio in Background", "Keep audio playing when leaving video player without PiP", bgPlay) {
                    AppSettings.setBackgroundPlayback(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Auto Picture-in-Picture", "Enter Android PiP floating window when pressing Home while playing", autoPip) {
                    AppSettings.setAutoPiP(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("PiP on Home Gesture Only", "Only trigger Picture-in-Picture via Home / task switch", pipHomeOnly) {
                    AppSettings.setPipOnHomeGestureOnly(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Video Mini Player on Back", "Shrink active video to floating window when pressing Back", miniPlayer) {
                    AppSettings.setVideoMiniPlayerEnabled(it)
                }
            }
        }
    }
}

// 13. Network & Streaming
@Composable
private fun NetworkStreamingDetailScreen(onBack: () -> Unit) {
    val wifiOnly by AppSettings.wifiOnlyNetwork.collectAsState()
    val timeoutSec by AppSettings.networkTimeoutSec.collectAsState()

    SettingsSubScreen(title = "Network & Streaming", subtitle = "HTTP/WebDAV timeouts & data controls", onBack = onBack) {
        item {
            SettingsCard {
                SwitchPreferenceRow("Browse Network Shares on Wi-Fi Only", "Prevent metered mobile data usage inside Network File Browser", wifiOnly) {
                    AppSettings.setWifiOnlyNetwork(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("HTTP / WebDAV Request Timeout", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(5, 15, 30, 45).forEach { sec ->
                        FilterChip(selected = timeoutSec == sec, onClick = { AppSettings.setNetworkTimeoutSec(sec) }, label = { Text("${sec}s") })
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { AppSettings.resetNetworkPreferences() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("Reset Network Preferences")
            }
        }
    }
}

// 14. Casting (Honest platform explanation + System Cast Settings shortcut)
@Composable
private fun CastingDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current

    SettingsSubScreen(title = "Casting & External Displays", subtitle = "Wireless display and screencast options", onBack = onBack) {
        item {
            SettingsCard {
                Text("Built-in Chromecast / DLNA", fontWeight = FontWeight.Bold)
                Text(
                    "Google Cast SDK is not bundled inside this offline-first Media3 build. You can mirror or cast your Android screen directly using Android System Cast or HDMI / DisplayPort output.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        try {
                            context.startActivity(Intent(Settings.ACTION_CAST_SETTINGS))
                        } catch (_: Exception) {
                            try {
                                context.startActivity(Intent(Settings.ACTION_DISPLAY_SETTINGS))
                            } catch (_: Exception) {
                                Toast.makeText(context, "Cast settings not available on this device", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Rounded.Cast, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open Android System Cast Settings")
                }
            }
        }
    }
}

// 15. Media Library
@Composable
private fun MediaLibraryDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val folderMode by AppSettings.folderViewMode.collectAsState()
    val layoutMode by AppSettings.viewMode.collectAsState()
    val showSize by AppSettings.showFolderSize.collectAsState()
    val showPath by AppSettings.showFolderPath.collectAsState()
    val showDate by AppSettings.showDateModified.collectAsState()
    val showRes by AppSettings.showResolutionBadge.collectAsState()
    val excludedFolders by AppSettings.excludedFolders.collectAsState()
    val showHidden by AppSettings.showHiddenMediaFolders.collectAsState()
    val safTrees by AppSettings.safMediaTreeUris.collectAsState()
    val includeAudio by AppSettings.includeAudioFilesInVideoLib.collectAsState()
    val minAudioDur by AppSettings.minAudioDurationSec.collectAsState()
    val albumPalette by AppSettings.albumPaletteBackground.collectAsState()

    val safFolderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri: Uri? ->
        uri ?: return@rememberLauncherForActivityResult
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        AppSettings.addSafMediaTreeUri(uri.toString())
        Toast.makeText(context, "SAF folder access granted & saved", Toast.LENGTH_SHORT).show()
    }

    SettingsSubScreen(title = "Media Library", subtitle = "Folders, grid/list layout, audio scan & hidden folders", onBack = onBack) {
        item {
            SettingsCard {
                Text("Folder Organization", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = folderMode == "FOLDER", onClick = { AppSettings.setFolderViewMode("FOLDER") }, label = { Text("Flat Folders") })
                    FilterChip(selected = folderMode == "TREE", onClick = { AppSettings.setFolderViewMode("TREE") }, label = { Text("Directory Tree") })
                }
                Text("Video Display Layout", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = layoutMode == "LIST", onClick = { AppSettings.setViewMode("LIST") }, label = { Text("List") })
                    FilterChip(selected = layoutMode == "GRID", onClick = { AppSettings.setViewMode("GRID") }, label = { Text("Grid (2-Column)") })
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Show File Sizes", "Show media file and folder storage sizes", showSize) { AppSettings.setShowFolderSize(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Show Folder Paths", "Display storage path beneath folder titles", showPath) { AppSettings.setShowFolderPath(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Show Date Modified", "Display file modification date on cards", showDate) { AppSettings.setShowDateModified(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Show Resolution Badges", "Show actual video resolution tag (1080p, 4K)", showRes) { AppSettings.setShowResolutionBadge(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Include Audio Files in Library", "Index scanned music tracks alongside folders", includeAudio) { AppSettings.setIncludeAudioFilesInVideoLib(it) }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Album Artwork Palette Background", "Tint Now Playing music background with album accent tones", albumPalette) { AppSettings.setAlbumPaletteBackground(it) }
                Text("Minimum Music Track Duration", fontWeight = FontWeight.SemiBold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(0 to "All", 10 to "10s+", 30 to "30s+", 60 to "60s+").forEach { (sec, lbl) ->
                        FilterChip(selected = minAudioDur == sec, onClick = { AppSettings.setMinAudioDurationSec(sec) }, label = { Text(lbl) })
                    }
                }
            }
        }
        item {
            SettingsCard {
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                SwitchPreferenceRow("Show Hidden Media Folders", "Include dot-folders (e.g. .nomedia / .hidden) during scans", showHidden) {
                    AppSettings.setShowHiddenMediaFolders(it)
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Text("Excluded Folders (${excludedFolders.size})", fontWeight = FontWeight.Bold)
                if (excludedFolders.isEmpty()) {
                    Text("Long-press any folder on Home to hide it from the video library.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    excludedFolders.forEach { folderId ->
                        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                            Text(folderId, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            TextButton(onClick = { AppSettings.removeExcludedFolder(folderId) }) { Text("Restore") }
                        }
                    }
                }
            }
        }
        item {
            SettingsCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("SAF Media Directories (${safTrees.size})", fontWeight = FontWeight.Bold)
                        Text("Grant persistent Storage Access Framework read permissions for SD cards & USB folders", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = { safFolderPicker.launch(null) }) { Text("Add Folder") }
                }
                safTrees.forEach { tree ->
                    val label = Uri.decode(tree).substringAfterLast(':').ifBlank { tree }
                    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodySmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        TextButton(onClick = {
                            runCatching {
                                context.contentResolver.releasePersistableUriPermission(Uri.parse(tree), Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            AppSettings.removeSafMediaTreeUri(tree)
                        }) { Text("Revoke") }
                    }
                }
            }
        }
        item {
            OutlinedButton(onClick = { AppSettings.resetLibraryPreferences() }, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Text("Reset Library Display Preferences")
            }
        }
    }
}

// 16. Advanced (Cache Clear + Confirmed History Clear + Non-destructive Preferences Reset)
@Composable
private fun AdvancedDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    var cacheBytes by remember { mutableStateOf(computeDirSizeBytes(context.cacheDir)) }
    var showClearHistoryDialog by remember { mutableStateOf(false) }
    var showResetPrefsDialog by remember { mutableStateOf(false) }

    SettingsSubScreen(title = "Advanced", subtitle = "mpv.conf, JSON backups, cache & preference resets", onBack = onBack) {
        item {
            SafeMpvConfCard()
        }
        item {
            JsonBackupAndRestoreCard()
        }
        item {
            SettingsCard {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column {
                        Text("Temporary & Subtitle Cache", fontWeight = FontWeight.SemiBold)
                        Text("${formatBytes(cacheBytes)} used", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    OutlinedButton(onClick = {
                        SubtitleUtils.clearCache(context)
                        context.cacheDir?.listFiles()?.forEach { file -> file.deleteRecursively() }
                        cacheBytes = computeDirSizeBytes(context.cacheDir)
                        Toast.makeText(context, "Cache cleared", Toast.LENGTH_SHORT).show()
                    }) {
                        Text("Clear Cache")
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Clear Playback History", fontWeight = FontWeight.SemiBold)
                        Text("Remove saved resume timestamps & Recents without deleting files", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { showClearHistoryDialog = true }) {
                        Text("Clear", color = MaterialTheme.colorScheme.error)
                    }
                }
                HorizontalDivider(color = MaterialTheme.extendedColors.cardBorder)
                Button(
                    onClick = { showResetPrefsDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.DeleteOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Reset Settings Preferences Only")
                }
                Text(
                    "Note: Resetting preferences keeps your watch history, playlists, saved streams, network shares, and Privacy Vault PIN intact.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }

    if (showClearHistoryDialog) {
        AlertDialog(
            onDismissRequest = { showClearHistoryDialog = false },
            title = { Text("Clear Playback History?", fontWeight = FontWeight.Bold) },
            text = { Text("All entries in Recents and saved resume positions will be cleared.") },
            confirmButton = {
                Button(
                    onClick = {
                        PlaybackHistoryBridge.clearHistory(context)
                        showClearHistoryDialog = false
                        Toast.makeText(context, "Playback history cleared", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Clear History") }
            },
            dismissButton = { TextButton(onClick = { showClearHistoryDialog = false }) { Text("Cancel") } }
        )
    }

    if (showResetPrefsDialog) {
        AlertDialog(
            onDismissRequest = { showResetPrefsDialog = false },
            title = { Text("Reset UI & Playback Preferences?", fontWeight = FontWeight.Bold) },
            text = { Text("Restores theme, HUD layout, subtitle styling, audio, gesture, and navigation settings to defaults. Your history, playlists, and Vault will NOT be erased.") },
            confirmButton = {
                Button(
                    onClick = {
                        AppSettings.resetPreferencesOnly()
                        showResetPrefsDialog = false
                        Toast.makeText(context, "Preferences restored to defaults", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Reset Preferences") }
            },
            dismissButton = { TextButton(onClick = { showResetPrefsDialog = false }) { Text("Cancel") } }
        )
    }
}

// Open-Source Licenses Viewer (reads genuine bundled assets/licenses/index.txt)
@Composable
private fun OpenSourceLicensesDetailScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val licenseEntries = remember {
        runCatching {
            context.assets.open("licenses/index.txt").bufferedReader().useLines { lines ->
                lines.mapNotNull { line ->
                    val idx = line.indexOf('|')
                    if (idx > 0 && idx < line.length - 1) {
                        LicenseEntry(line.substring(0, idx), line.substring(idx + 1))
                    } else null
                }.toList()
            }
        }.getOrDefault(emptyList())
    }

    var selectedEntry by remember { mutableStateOf<LicenseEntry?>(null) }

    if (selectedEntry != null) {
        BackHandler { selectedEntry = null }
        val textContent = remember(selectedEntry) {
            runCatching {
                context.assets.open("licenses/${selectedEntry!!.fileName}").bufferedReader().readText()
            }.getOrElse { "Unable to load license file." }
        }
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(16.dp)
        ) {
            SubScreenHeader(selectedEntry!!.title, "License Text", onBack = { selectedEntry = null })
            Spacer(modifier = Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxSize(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
            ) {
                Text(
                    text = textContent,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(14.dp)
                )
            }
        }
        return
    }

    SettingsSubScreen(title = "Open-Source Licenses", subtitle = "MPVX Player (GPL v3.0), libmpv / FFmpeg & open-source notices", onBack = onBack) {
        if (licenseEntries.isEmpty()) {
            item {
                Text("License notices bundled in APK assets/licenses.", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        } else {
            items(licenseEntries, key = { it.fileName }) { entry ->
                Surface(
                    onClick = { selectedEntry = entry },
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Description, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(entry.title, modifier = Modifier.weight(1f), fontWeight = FontWeight.Medium)
                        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }
    }
}

// Reusable UI helpers
@Composable
internal fun SettingsSubScreen(
    title: String,
    subtitle: String,
    onBack: () -> Unit,
    content: androidx.compose.foundation.lazy.LazyListScope.() -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { SubScreenHeader(title, subtitle, onBack) }
        content()
        item { Spacer(modifier = Modifier.height(24.dp)) }
    }
}

@Composable
internal fun SettingsCard(content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.extendedColors.cardBackground)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
    }
}

@Composable
internal fun SwitchPreferenceRow(
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun SettingsSectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 6.dp, top = 4.dp, bottom = 2.dp)
    )
}

@Composable
private fun PreferenceGroupCard(
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow
    ) {
        Column(modifier = Modifier.fillMaxWidth(), content = content)
    }
}

@Composable
private fun CategoryNavigationRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(iconTint.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(24.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(2.dp))
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }

        Spacer(modifier = Modifier.width(8.dp))
        Icon(Icons.AutoMirrored.Rounded.ArrowForwardIos, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.55f), modifier = Modifier.size(14.dp))
    }
}

@Composable
private fun SubScreenHeader(
    title: String,
    subtitle: String,
    onBack: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack) {
            Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back to Settings", tint = MaterialTheme.colorScheme.onBackground)
        }
        Spacer(modifier = Modifier.width(4.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

private fun computeDirSizeBytes(dir: File?): Long {
    if (dir == null || !dir.exists()) return 0L
    return dir.walkBottomUp().filter { it.isFile }.sumOf { it.length() }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 KB"
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return if (mb >= 1.0) String.format(Locale.US, "%.1f MB", mb)
    else "${(bytes / 1024L).coerceAtLeast(1L)} KB"
}
