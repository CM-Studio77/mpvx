package com.cmstudio.player.ui

import android.content.Context
import android.content.SharedPreferences
import com.cmstudio.player.engine.EngineRenderConfig
import com.cmstudio.player.engine.PlaybackEngineMode
import com.cmstudio.player.security.EncryptedCredentialStore
import com.cmstudio.player.ui.model.NetworkShareConfig
import com.cmstudio.player.ui.model.SavedStreamItem
import com.cmstudio.player.ui.model.UserPlaylist
import com.cmstudio.player.ui.servers.MediaServerKind
import com.cmstudio.player.ui.servers.MediaServerProfile
import com.cmstudio.player.ui.theme.ThemePreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom

object AppSettings {
    private const val PREFS_NAME = "mpvx_player_prefs"

    val ALL_NAV_TABS = listOf("HOME", "MUSIC", "RECENTS", "PLAYLISTS", "STREAMS", "NETWORK", "VAULT")
    const val DEFAULT_TOP_RIGHT_HUD = "AUTO,SUBTITLES,MORE"
    const val DEFAULT_BOTTOM_LEFT_HUD = "LOCK,ROTATE,SPEED,AUDIO_TRACK,REPEAT,AB_LOOP"
    const val DEFAULT_BOTTOM_RIGHT_HUD = "SCREENSHOT,ASPECT,ZOOM,PIP"

    // 1. Theme & Appearance
    private const val KEY_DARK_THEME = "pref_dark_theme"
    private const val KEY_THEME_PRESET = "pref_theme_preset"
    private const val KEY_AMOLED = "pref_amoled"
    private const val KEY_FROSTED_SHEETS = "pref_frosted_sheets"
    private const val KEY_LIQUID_GLASS = "pref_liquid_glass_surfaces"
    private const val KEY_GLASS_BLUR = "pref_glass_blur"
    private const val KEY_GLASS_OPACITY = "pref_glass_opacity"
    private const val KEY_3D_CONTROLS = "pref_3d_controls"
    private const val KEY_CUSTOM_THEME_COLOR = "pref_custom_theme_color"
    private const val KEY_CUSTOM_PRIMARY = "pref_custom_primary_color"
    private const val KEY_CUSTOM_ACCENT = "pref_custom_accent_color"
    private const val KEY_CUSTOM_BACKGROUND = "pref_custom_bg_color"
    private const val KEY_CUSTOM_SURFACE = "pref_custom_surface_color"
    private const val KEY_DARK_MODE = "pref_dark_mode" // SYSTEM, DARK, LIGHT
    private const val KEY_FAVORITE_THEMES = "pref_favorite_themes"
    private const val KEY_CUSTOM_WALLPAPER_URI = "pref_custom_wallpaper_uri"
    private const val KEY_CUSTOM_WALLPAPER_IS_VIDEO = "pref_custom_wallpaper_is_video"
    private const val KEY_CUSTOM_WALLPAPER_BLUR = "pref_custom_wallpaper_blur"
    private const val KEY_CUSTOM_WALLPAPER_BRIGHTNESS = "pref_custom_wallpaper_brightness"
    private const val KEY_CUSTOM_WALLPAPER_SATURATION = "pref_custom_wallpaper_saturation"
    private const val KEY_CUSTOM_WALLPAPER_SCRIM = "pref_custom_wallpaper_scrim"
    private const val KEY_CUSTOM_SURFACE_OPACITY = "pref_custom_surface_opacity"
    private const val KEY_SEEK_INTERVAL = "pref_seek_interval_sec"
    private const val KEY_PLAYER_CONTROL_SIZE = "pref_player_control_size"
    private const val KEY_SEEKBAR_STYLE = "pref_seekbar_style"
    private const val KEY_SHOW_HUD_TELEMETRY = "pref_show_hud_telemetry"
    private const val KEY_TOP_RIGHT_HUD_CONTROLS = "pref_hud_top_right"
    private const val KEY_BOTTOM_LEFT_HUD_CONTROLS = "pref_hud_bottom_left"
    private const val KEY_BOTTOM_RIGHT_HUD_CONTROLS = "pref_hud_bottom_right"
    private const val KEY_AUTO_PLAY_NEXT = "pref_auto_play_next"
    private const val KEY_REMEMBER_POSITION = "pref_remember_position"

    // 2. Playback Engine (Media3)
    private const val KEY_DEFAULT_ENGINE = "pref_default_engine"
    private const val KEY_DECODER_MODE = "pref_decoder_mode"
    private const val KEY_AUTO_ROUTING_DV = "pref_auto_routing_dv"
    private const val KEY_AUTO_ROUTING_STREAMS = "pref_auto_routing_streams"
    private const val KEY_HW_ACCELERATION = "pref_hw_acceleration"

    // 3. Performance & Smooth Motion
    private const val KEY_SMOOTH_MOTION = "pref_smooth_motion"
    private const val KEY_TARGET_FPS = "pref_target_fps"
    private const val KEY_UI_ANIMATION_SCALE = "pref_ui_anim_scale"

    // 4. Subtitles
    private const val KEY_DUAL_SUBTITLES = "pref_dual_subtitles"
    private const val KEY_SUBTITLE_FONT_SIZE = "pref_sub_font_size"
    private const val KEY_SUBTITLE_COLOR = "pref_sub_color"
    private const val KEY_SUBTITLE_BG = "pref_sub_bg"
    private const val KEY_SUBTITLE_DELAY = "pref_sub_delay"
    private const val KEY_SUBTITLE_LANG = "pref_sub_lang"
    private const val KEY_SUBTITLE_ENABLED = "pref_sub_enabled"

    // 5. Audio & Sound
    private const val KEY_AUDIO_LANG = "pref_audio_lang"
    private const val KEY_AUDIO_DELAY = "pref_audio_delay"
    private const val KEY_AUDIO_BOOST = "pref_audio_boost"
    private const val KEY_AUDIO_OFFLOAD = "pref_audio_offload"
    private const val KEY_NIGHT_COMPRESSOR = "pref_night_compressor"
    private const val KEY_EQ_PRESET = "pref_eq_preset"

    // 6. Gestures & Player Controls
    private const val KEY_VOLUME_GESTURE = "pref_vol_gesture"
    private const val KEY_BRIGHT_GESTURE = "pref_bright_gesture"
    private const val KEY_DOUBLE_TAP_SEC = "pref_double_tap_sec"
    private const val KEY_SEEK_GESTURE = "pref_seek_gesture"
    private const val KEY_HAPTIC_FEEDBACK = "pref_haptic_feedback"
    private const val KEY_LONG_PRESS_2X = "pref_long_press_2x"

    // 7. Video & Display
    private const val KEY_DEFAULT_ASPECT = "pref_default_aspect"
    private const val KEY_ORIENTATION = "pref_orientation"
    private const val KEY_KEEP_SCREEN_ON = "pref_keep_screen_on"
    private const val KEY_DEFAULT_SPEED = "pref_default_speed"

    // 8. Media3 LoadControl Buffers
    private const val KEY_MIN_BUFFER_MS = "pref_min_buffer_ms"
    private const val KEY_MAX_BUFFER_MS = "pref_max_buffer_ms"
    private const val KEY_BUFFER_PLAYBACK_MS = "pref_buffer_playback_ms"
    private const val KEY_BUFFER_REBUFFER_MS = "pref_buffer_rebuffer_ms"

    // 9. Library & Media Preferences
    private const val KEY_SORT_OPTION = "pref_sort_option"
    private const val KEY_SORT_ASC = "pref_sort_asc"
    private const val KEY_VIEW_MODE = "pref_view_mode" // "LIST", "GRID"
    private const val KEY_FOLDER_VIEW_MODE = "pref_folder_view_mode" // "FOLDER", "TREE", "MEDIA"
    private const val KEY_SHOW_FOLDER_SIZE = "pref_show_folder_size"
    private const val KEY_SHOW_FOLDER_PATH = "pref_show_folder_path"
    private const val KEY_SHOW_DATE_MODIFIED = "pref_show_date_modified"
    private const val KEY_SHOW_RESOLUTION_BADGE = "pref_show_res_badge"
    private const val KEY_FOLDER_GRID_COLS_PORTRAIT = "pref_folder_grid_cols_portrait"
    private const val KEY_FOLDER_GRID_COLS_LANDSCAPE = "pref_folder_grid_cols_landscape"
    private const val KEY_VIDEO_GRID_COLS_PORTRAIT = "pref_video_grid_cols_portrait"
    private const val KEY_VIDEO_GRID_COLS_LANDSCAPE = "pref_video_grid_cols_landscape"
    private const val KEY_UNLIMITED_NAME_LINES = "pref_unlimited_name_lines"
    private const val KEY_SHOW_EXTENSION = "pref_show_extension"
    private const val KEY_SHOW_DURATION = "pref_show_duration"
    private const val KEY_SHOW_PROGRESS_BAR = "pref_show_progress_bar"
    private const val KEY_CENTER_GRID_TITLES = "pref_center_grid_titles"
    private const val KEY_SHOW_TOTAL_VIDEOS = "pref_show_total_videos"
    private const val KEY_SHOW_TOTAL_DURATION = "pref_show_total_duration"
    private const val KEY_SHOW_FOLDER_THUMBNAILS = "pref_show_folder_thumbnails"
    private const val KEY_SHOW_NEW_BADGE = "pref_show_new_badge"
    private const val KEY_NEW_BADGE_DAYS = "pref_new_badge_days"
    private const val KEY_WATCHED_THRESHOLD_PCT = "pref_watched_threshold_pct"
    private const val KEY_SHOW_QUICK_PLAY_FAB = "pref_show_quick_play_fab"
    private const val KEY_QUICK_PLAY_FAB_DIRECT = "pref_quick_play_fab_direct"
    private const val KEY_DUAL_PANE_TABLET = "pref_dual_pane_tablet"
    private const val KEY_VAULT_ENTRY_HIDDEN = "pref_vault_entry_hidden"
    private const val KEY_VAULT_BIOMETRIC_ENABLED = "pref_vault_biometric_enabled"
    private const val KEY_VAULT_SEC_QUESTION = "pref_vault_sec_question"
    private const val KEY_VAULT_SEC_ANSWER_HASH = "pref_vault_sec_answer_hash"
    private const val KEY_VAULT_SEC_ANSWER_SALT = "pref_vault_sec_answer_salt"

    // 10. Background Playback & PiP
    private const val KEY_BACKGROUND_PLAYBACK = "pref_background_playback"
    private const val KEY_AUTO_PIP = "pref_auto_pip"

    private const val KEY_ANIME4K = "pref_anime4k"
    private const val KEY_ANIME4K_MODE = "pref_anime4k_mode"
    private const val KEY_ANIME4K_QUALITY = "pref_anime4k_quality"
    private const val KEY_HDR_TONEMAP = "pref_hdr_tonemap"
    private const val KEY_MPV_PROFILE = "pref_mpv_profile"
    private const val KEY_USE_GPU_NEXT = "pref_use_gpu_next"
    private const val KEY_USE_VULKAN = "pref_use_vulkan"
    private const val KEY_USE_YUV420P = "pref_use_yuv420p"
    private const val KEY_MPV_DEBANDING = "pref_mpv_debanding"
    private const val KEY_DEBAND_ITERATIONS = "pref_deband_iterations"
    private const val KEY_DEBAND_THRESHOLD = "pref_deband_threshold"
    private const val KEY_DEBAND_RANGE = "pref_deband_range"
    private const val KEY_DEBAND_GRAIN = "pref_deband_grain"
    private const val KEY_VIDEO_ENGINE_MAP_JSON = "pref_video_engine_map_json"
    private const val KEY_MANUAL_GRID_COLS = "pref_manual_grid_cols"
    private const val KEY_SEPARATE_FOLDER_VIDEO_LAYOUT = "pref_separate_folder_video_layout"
    private const val KEY_NETWORK_THUMBNAILS = "pref_network_thumbnails"
    private const val KEY_THUMBNAIL_CACHE_MB = "pref_thumbnail_cache_mb"
    private const val KEY_SHOW_ROTATION_OVERLAY = "pref_show_rotation_overlay"
    private const val KEY_SHOW_ASPECT_OVERLAY = "pref_show_aspect_overlay"
    private const val KEY_SHOW_SPEED_OVERLAY = "pref_show_speed_overlay"
    private const val KEY_SHOW_DECODER_OVERLAY = "pref_show_decoder_overlay"
    private const val KEY_SHOW_SHUFFLE_OVERLAY = "pref_show_shuffle_overlay"
    private const val KEY_SHOW_LOOP_OVERLAY = "pref_show_loop_overlay"
    private const val KEY_SHOW_PIP_OVERLAY = "pref_show_pip_overlay"
    private const val KEY_SHOW_SCREENSHOT_OVERLAY = "pref_show_screenshot_overlay"
    private const val KEY_SCREENSHOT_FORMAT = "pref_screenshot_format"
    private const val KEY_SCREENSHOT_QUALITY = "pref_screenshot_quality"
    private const val KEY_SCREENSHOT_FOLDER = "pref_screenshot_folder"
    private const val KEY_DOUBLE_TAP_PAUSE_RESUME = "pref_double_tap_pause_resume"
    private const val KEY_LONG_PRESS_SPEED_VAR = "pref_long_press_speed_var"
    private const val KEY_VAULT_PIN = "pref_vault_pin"
    private const val KEY_VAULT_PIN_HASH = "pref_vault_pin_hash"
    private const val KEY_VAULT_PIN_SALT = "pref_vault_pin_salt"
    private const val KEY_NAV_ENABLED_TABS = "pref_nav_enabled_tabs"
    private const val KEY_NAV_TAB_ORDER = "pref_nav_tab_order"
    private const val KEY_DECODER_PRIORITY = "pref_decoder_priority"
    private const val KEY_VIDEO_SURFACE_TYPE = "pref_video_surface_type"
    private const val KEY_AUDIO_PITCH_CORRECTION = "pref_audio_pitch_correction"
    private const val KEY_PLAYER_REPEAT_MODE = "pref_player_repeat_mode"
    private const val KEY_PINCH_ZOOM_ENABLED = "pref_pinch_zoom_enabled"
    private const val KEY_FAST_SEEK_KEYFRAMES = "pref_fast_seek_keyframes"
    private const val KEY_SUBTITLE_EDGE_TYPE = "pref_sub_edge_type"
    private const val KEY_SUBTITLE_BG_OPACITY = "pref_sub_bg_opacity"
    private const val KEY_SUBTITLE_BOTTOM_PADDING = "pref_sub_bottom_padding_dp"
    private const val KEY_SUBTITLE_BOLD = "pref_sub_bold"
    private const val KEY_WIFI_ONLY_NETWORK = "pref_wifi_only_network"
    private const val KEY_NETWORK_TIMEOUT_SEC = "pref_network_timeout_sec"
    private const val KEY_EXCLUDED_FOLDERS = "pref_excluded_folders"
    private const val KEY_FAVORITE_VIDEOS = "pref_favorite_video_uris"
    private const val KEY_PINNED_FOLDERS = "pref_pinned_folder_ids"
    private const val KEY_VAULT_VIDEOS = "pref_vault_video_uris"
    private const val KEY_USER_PLAYLISTS_JSON = "pref_user_playlists_json"
    private const val KEY_SAVED_STREAMS_JSON = "pref_saved_streams_json"
    private const val KEY_NETWORK_SHARES_JSON = "pref_network_shares_json"

    private lateinit var prefs: SharedPreferences
    private var appContext: Context? = null

    // 1. Theme State
    private val _isDarkTheme = MutableStateFlow(true)
    val isDarkTheme: StateFlow<Boolean> = _isDarkTheme.asStateFlow()

    private val _themePreset = MutableStateFlow(ThemePreset.MPVX_CYAN)
    val themePreset: StateFlow<ThemePreset> = _themePreset.asStateFlow()

    private val _isAmoled = MutableStateFlow(false)
    val isAmoled: StateFlow<Boolean> = _isAmoled.asStateFlow()

    private val _isFrostedPlayerSheets = MutableStateFlow(true)
    val isFrostedPlayerSheets: StateFlow<Boolean> = _isFrostedPlayerSheets.asStateFlow()

    // Strictly scoped to floating dock, player HUD, and player sheets (off by default)
    private val _isLiquidGlassEnabled = MutableStateFlow(false)
    val isLiquidGlassEnabled: StateFlow<Boolean> = _isLiquidGlassEnabled.asStateFlow()

    private val _glassBlurRadius = MutableStateFlow(16f)
    val glassBlurRadius: StateFlow<Float> = _glassBlurRadius.asStateFlow()

    private val _glassFrostOpacity = MutableStateFlow(0.78f)
    val glassFrostOpacity: StateFlow<Float> = _glassFrostOpacity.asStateFlow()

    private val _is3dControlsEnabled = MutableStateFlow(false)
    val is3dControlsEnabled: StateFlow<Boolean> = _is3dControlsEnabled.asStateFlow()

    private val _customThemeColor = MutableStateFlow(0xFF2563EB)
    val customThemeColor: StateFlow<Long> = _customThemeColor.asStateFlow()

    private val _customPrimaryColor = MutableStateFlow<Long?>(null)
    val customPrimaryColor: StateFlow<Long?> = _customPrimaryColor.asStateFlow()

    private val _customAccentColor = MutableStateFlow<Long?>(null)
    val customAccentColor: StateFlow<Long?> = _customAccentColor.asStateFlow()

    private val _customBackgroundColor = MutableStateFlow<Long?>(null)
    val customBackgroundColor: StateFlow<Long?> = _customBackgroundColor.asStateFlow()

    private val _customSurfaceColor = MutableStateFlow<Long?>(null)
    val customSurfaceColor: StateFlow<Long?> = _customSurfaceColor.asStateFlow()

    private val _darkMode = MutableStateFlow("SYSTEM") // SYSTEM, DARK, LIGHT
    val darkMode: StateFlow<String> = _darkMode.asStateFlow()

    private val _favoriteThemePresets = MutableStateFlow<Set<String>>(emptySet())
    val favoriteThemePresets: StateFlow<Set<String>> = _favoriteThemePresets.asStateFlow()

    private val _themeTransitionOrigin = MutableStateFlow<Pair<Float, Float>?>(null)
    val themeTransitionOrigin: StateFlow<Pair<Float, Float>?> = _themeTransitionOrigin.asStateFlow()

    private val _customThemeWallpaperUri = MutableStateFlow("")
    val customThemeWallpaperUri: StateFlow<String> = _customThemeWallpaperUri.asStateFlow()

    private val _customThemeWallpaperIsVideo = MutableStateFlow(false)
    val customThemeWallpaperIsVideo: StateFlow<Boolean> = _customThemeWallpaperIsVideo.asStateFlow()

    private val _customThemeWallpaperBlur = MutableStateFlow(0f)
    val customThemeWallpaperBlur: StateFlow<Float> = _customThemeWallpaperBlur.asStateFlow()

    private val _customThemeWallpaperBrightness = MutableStateFlow(1.0f)
    val customThemeWallpaperBrightness: StateFlow<Float> = _customThemeWallpaperBrightness.asStateFlow()

    private val _customThemeWallpaperSaturation = MutableStateFlow(1.0f)
    val customThemeWallpaperSaturation: StateFlow<Float> = _customThemeWallpaperSaturation.asStateFlow()

    private val _customThemeWallpaperScrimAlpha = MutableStateFlow(0.45f)
    val customThemeWallpaperScrimAlpha: StateFlow<Float> = _customThemeWallpaperScrimAlpha.asStateFlow()

    private val _customThemeSurfaceOpacity = MutableStateFlow(0.85f)
    val customThemeSurfaceOpacity: StateFlow<Float> = _customThemeSurfaceOpacity.asStateFlow()

    private val _seekIntervalSeconds = MutableStateFlow(10)
    val seekIntervalSeconds: StateFlow<Int> = _seekIntervalSeconds.asStateFlow()

    private val _playerControlSize = MutableStateFlow("NORMAL") // COMPACT, NORMAL, LARGE
    val playerControlSize: StateFlow<String> = _playerControlSize.asStateFlow()

    private val _seekbarStyle = MutableStateFlow("EXPRESSIVE") // EXPRESSIVE, WAVY, STANDARD
    val seekbarStyle: StateFlow<String> = _seekbarStyle.asStateFlow()

    private val _showHudTelemetry = MutableStateFlow(false)
    val showHudTelemetry: StateFlow<Boolean> = _showHudTelemetry.asStateFlow()

    private val _topRightHudControls = MutableStateFlow(DEFAULT_TOP_RIGHT_HUD)
    val topRightHudControls: StateFlow<String> = _topRightHudControls.asStateFlow()

    private val _bottomLeftHudControls = MutableStateFlow(DEFAULT_BOTTOM_LEFT_HUD)
    val bottomLeftHudControls: StateFlow<String> = _bottomLeftHudControls.asStateFlow()

    private val _bottomRightHudControls = MutableStateFlow(DEFAULT_BOTTOM_RIGHT_HUD)
    val bottomRightHudControls: StateFlow<String> = _bottomRightHudControls.asStateFlow()

    private val _enabledNavTabs = MutableStateFlow(ALL_NAV_TABS)
    val enabledNavTabs: StateFlow<List<String>> = _enabledNavTabs.asStateFlow()

    private val _navTabOrder = MutableStateFlow(ALL_NAV_TABS)
    val navTabOrder: StateFlow<List<String>> = _navTabOrder.asStateFlow()

    private val _decoderPriority = MutableStateFlow("AUTO") // AUTO, HW_ONLY, SW_PREFER
    val decoderPriority: StateFlow<String> = _decoderPriority.asStateFlow()

    private val _videoSurfaceType = MutableStateFlow("SURFACE_VIEW") // SURFACE_VIEW, TEXTURE_VIEW
    val videoSurfaceType: StateFlow<String> = _videoSurfaceType.asStateFlow()

    private val _audioPitchCorrection = MutableStateFlow(true)
    val audioPitchCorrection: StateFlow<Boolean> = _audioPitchCorrection.asStateFlow()

    private val _playerRepeatMode = MutableStateFlow("OFF") // OFF, ONE, ALL
    val playerRepeatMode: StateFlow<String> = _playerRepeatMode.asStateFlow()

    private val _pinchZoomEnabled = MutableStateFlow(true)
    val pinchZoomEnabled: StateFlow<Boolean> = _pinchZoomEnabled.asStateFlow()

    private val _fastSeekKeyframes = MutableStateFlow(true)
    val fastSeekKeyframes: StateFlow<Boolean> = _fastSeekKeyframes.asStateFlow()

    private val _subtitleEdgeType = MutableStateFlow("DROP_SHADOW")
    val subtitleEdgeType: StateFlow<String> = _subtitleEdgeType.asStateFlow()

    private val _subtitleBgOpacity = MutableStateFlow(0.5f)
    val subtitleBgOpacity: StateFlow<Float> = _subtitleBgOpacity.asStateFlow()

    private val _subtitleBottomPaddingDp = MutableStateFlow(24)
    val subtitleBottomPaddingDp: StateFlow<Int> = _subtitleBottomPaddingDp.asStateFlow()

    private val _subtitleBold = MutableStateFlow(false)
    val subtitleBold: StateFlow<Boolean> = _subtitleBold.asStateFlow()

    private val _wifiOnlyNetwork = MutableStateFlow(false)
    val wifiOnlyNetwork: StateFlow<Boolean> = _wifiOnlyNetwork.asStateFlow()

    private val _networkTimeoutSec = MutableStateFlow(15)
    val networkTimeoutSec: StateFlow<Int> = _networkTimeoutSec.asStateFlow()

    private val _excludedFolders = MutableStateFlow<Set<String>>(emptySet())
    val excludedFolders: StateFlow<Set<String>> = _excludedFolders.asStateFlow()

    private val _folderGridColumnsPortrait = MutableStateFlow(2)
    val folderGridColumnsPortrait: StateFlow<Int> = _folderGridColumnsPortrait.asStateFlow()

    private val _folderGridColumnsLandscape = MutableStateFlow(4)
    val folderGridColumnsLandscape: StateFlow<Int> = _folderGridColumnsLandscape.asStateFlow()

    private val _videoGridColumnsPortrait = MutableStateFlow(2)
    val videoGridColumnsPortrait: StateFlow<Int> = _videoGridColumnsPortrait.asStateFlow()

    private val _videoGridColumnsLandscape = MutableStateFlow(4)
    val videoGridColumnsLandscape: StateFlow<Int> = _videoGridColumnsLandscape.asStateFlow()

    private val _unlimitedNameLines = MutableStateFlow(false)
    val unlimitedNameLines: StateFlow<Boolean> = _unlimitedNameLines.asStateFlow()

    private val _showExtensionField = MutableStateFlow(false)
    val showExtensionField: StateFlow<Boolean> = _showExtensionField.asStateFlow()

    private val _showDurationField = MutableStateFlow(true)
    val showDurationField: StateFlow<Boolean> = _showDurationField.asStateFlow()

    private val _showProgressBar = MutableStateFlow(true)
    val showProgressBar: StateFlow<Boolean> = _showProgressBar.asStateFlow()

    private val _centerGridTitles = MutableStateFlow(true)
    val centerGridTitles: StateFlow<Boolean> = _centerGridTitles.asStateFlow()

    private val _showTotalVideosChip = MutableStateFlow(true)
    val showTotalVideosChip: StateFlow<Boolean> = _showTotalVideosChip.asStateFlow()

    private val _showTotalDurationChip = MutableStateFlow(false)
    val showTotalDurationChip: StateFlow<Boolean> = _showTotalDurationChip.asStateFlow()

    private val _showFolderThumbnails = MutableStateFlow(false)
    val showFolderThumbnails: StateFlow<Boolean> = _showFolderThumbnails.asStateFlow()

    private val _showNewBadge = MutableStateFlow(true)
    val showNewBadge: StateFlow<Boolean> = _showNewBadge.asStateFlow()

    private val _newBadgeDays = MutableStateFlow(7)
    val newBadgeDays: StateFlow<Int> = _newBadgeDays.asStateFlow()

    private val _watchedThresholdPct = MutableStateFlow(95)
    val watchedThresholdPct: StateFlow<Int> = _watchedThresholdPct.asStateFlow()

    private val _showQuickPlayFab = MutableStateFlow(true)
    val showQuickPlayFab: StateFlow<Boolean> = _showQuickPlayFab.asStateFlow()

    private val _quickPlayFabDirect = MutableStateFlow(false)
    val quickPlayFabDirect: StateFlow<Boolean> = _quickPlayFabDirect.asStateFlow()

    private val _dualPaneTablet = MutableStateFlow(true)
    val dualPaneTablet: StateFlow<Boolean> = _dualPaneTablet.asStateFlow()

    private val _vaultEntryHidden = MutableStateFlow(false)
    val vaultEntryHidden: StateFlow<Boolean> = _vaultEntryHidden.asStateFlow()

    private val _vaultBiometricEnabled = MutableStateFlow(true)
    val vaultBiometricEnabled: StateFlow<Boolean> = _vaultBiometricEnabled.asStateFlow()

    private val _vaultSecurityQuestion = MutableStateFlow("")
    val vaultSecurityQuestion: StateFlow<String> = _vaultSecurityQuestion.asStateFlow()

    private var inMemoryVaultAnswerSalt: String? = null
    private var inMemoryVaultAnswerHash: String? = null

    private val _favoriteVideoUris = MutableStateFlow<Set<String>>(emptySet())
    val favoriteVideoUris: StateFlow<Set<String>> = _favoriteVideoUris.asStateFlow()

    private val _pinnedFolderIds = MutableStateFlow<Set<String>>(emptySet())
    val pinnedFolderIds: StateFlow<Set<String>> = _pinnedFolderIds.asStateFlow()

    private val _vaultVideoUris = MutableStateFlow<Set<String>>(emptySet())
    val vaultVideoUris: StateFlow<Set<String>> = _vaultVideoUris.asStateFlow()

    private val _userPlaylists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val userPlaylists: StateFlow<List<UserPlaylist>> = _userPlaylists.asStateFlow()

    private val _savedStreams = MutableStateFlow<List<SavedStreamItem>>(emptyList())
    val savedStreams: StateFlow<List<SavedStreamItem>> = _savedStreams.asStateFlow()

    private val _networkShares = MutableStateFlow<List<NetworkShareConfig>>(emptyList())
    val networkShares: StateFlow<List<NetworkShareConfig>> = _networkShares.asStateFlow()

    private val sessionNetworkPasswords = mutableMapOf<String, String>()

    private val _autoPlayNext = MutableStateFlow(true)
    val autoPlayNext: StateFlow<Boolean> = _autoPlayNext.asStateFlow()

    private val _rememberPosition = MutableStateFlow(true)
    val rememberPosition: StateFlow<Boolean> = _rememberPosition.asStateFlow()

    // 2. Dual Playback Engine State (AUTO, MPV, NATIVE)
    private val _defaultEngine = MutableStateFlow("AUTO")
    val defaultEngine: StateFlow<String> = _defaultEngine.asStateFlow()

    private val _decoderMode = MutableStateFlow("HW+") // HW+, HW, SW
    val decoderMode: StateFlow<String> = _decoderMode.asStateFlow()

    private val _hwAcceleration = MutableStateFlow(true)
    val hwAcceleration: StateFlow<Boolean> = _hwAcceleration.asStateFlow()

    private val _autoRoutingDolbyVision = MutableStateFlow(true)
    val autoRoutingDolbyVision: StateFlow<Boolean> = _autoRoutingDolbyVision.asStateFlow()

    private val _autoRoutingHlsDash = MutableStateFlow(true)
    val autoRoutingHlsDash: StateFlow<Boolean> = _autoRoutingHlsDash.asStateFlow()

    // 3. Performance & Smooth Motion
    private val _smoothMotion120fps = MutableStateFlow(true)
    val smoothMotion120fps: StateFlow<Boolean> = _smoothMotion120fps.asStateFlow()

    private val _targetFps = MutableStateFlow(120) // 60, 90, 120
    val targetFps: StateFlow<Int> = _targetFps.asStateFlow()

    private val _uiAnimationScale = MutableStateFlow(1.0f)
    val uiAnimationScale: StateFlow<Float> = _uiAnimationScale.asStateFlow()

    // 4. Subtitles
    private val _subtitlesEnabled = MutableStateFlow(true)
    val subtitlesEnabled: StateFlow<Boolean> = _subtitlesEnabled.asStateFlow()

    private val _dualSubtitles = MutableStateFlow(false)
    val dualSubtitles: StateFlow<Boolean> = _dualSubtitles.asStateFlow()

    private val _subtitleFontSize = MutableStateFlow(22f)
    val subtitleFontSize: StateFlow<Float> = _subtitleFontSize.asStateFlow()

    private val _subtitleColor = MutableStateFlow(0xFFFFFFFF)
    val subtitleColor: StateFlow<Long> = _subtitleColor.asStateFlow()

    private val _subtitleBackground = MutableStateFlow(0x80000000)
    val subtitleBackground: StateFlow<Long> = _subtitleBackground.asStateFlow()

    private val _subtitleDelayMs = MutableStateFlow(0)
    val subtitleDelayMs: StateFlow<Int> = _subtitleDelayMs.asStateFlow()

    private val _subtitleLanguage = MutableStateFlow("Auto")
    val subtitleLanguage: StateFlow<String> = _subtitleLanguage.asStateFlow()

    // 5. Audio & Sound
    private val _audioLanguage = MutableStateFlow("Auto")
    val audioLanguage: StateFlow<String> = _audioLanguage.asStateFlow()

    private val _audioDelayMs = MutableStateFlow(0)
    val audioDelayMs: StateFlow<Int> = _audioDelayMs.asStateFlow()

    private val _audioBoost = MutableStateFlow(100) // 100% to 200%
    val audioBoost: StateFlow<Int> = _audioBoost.asStateFlow()

    private val _audioOffload = MutableStateFlow(true)
    val audioOffload: StateFlow<Boolean> = _audioOffload.asStateFlow()

    private val _nightModeCompressor = MutableStateFlow(false)
    val nightModeCompressor: StateFlow<Boolean> = _nightModeCompressor.asStateFlow()

    private val _eqPreset = MutableStateFlow("Cinema")
    val eqPreset: StateFlow<String> = _eqPreset.asStateFlow()

    // 6. Gestures & Player Controls
    private val _volumeGesture = MutableStateFlow(true)
    val volumeGesture: StateFlow<Boolean> = _volumeGesture.asStateFlow()

    private val _brightnessGesture = MutableStateFlow(true)
    val brightnessGesture: StateFlow<Boolean> = _brightnessGesture.asStateFlow()

    private val _doubleTapSeconds = MutableStateFlow(10)
    val doubleTapSeconds: StateFlow<Int> = _doubleTapSeconds.asStateFlow()

    private val _seekGesture = MutableStateFlow(true)
    val seekGesture: StateFlow<Boolean> = _seekGesture.asStateFlow()

    private val _hapticFeedback = MutableStateFlow(true)
    val hapticFeedback: StateFlow<Boolean> = _hapticFeedback.asStateFlow()

    private val _longPress2x = MutableStateFlow(true)
    val longPress2x: StateFlow<Boolean> = _longPress2x.asStateFlow()

    // 7. Video & Display
    private val _defaultAspectRatio = MutableStateFlow("Fit") // Fit, Fill, Stretch, 16:9, 4:3, 21:9
    val defaultAspectRatio: StateFlow<String> = _defaultAspectRatio.asStateFlow()

    private val _screenOrientation = MutableStateFlow("Auto") // Auto, Landscape, Portrait, Sensor
    val screenOrientation: StateFlow<String> = _screenOrientation.asStateFlow()

    private val _keepScreenOn = MutableStateFlow(true)
    val keepScreenOn: StateFlow<Boolean> = _keepScreenOn.asStateFlow()

    private val _defaultSpeed = MutableStateFlow(1.0f)
    val defaultSpeed: StateFlow<Float> = _defaultSpeed.asStateFlow()

    // 8. Media3 LoadControl Buffers
    private val _minBufferMs = MutableStateFlow(50000)
    val minBufferMs: StateFlow<Int> = _minBufferMs.asStateFlow()

    private val _maxBufferMs = MutableStateFlow(50000)
    val maxBufferMs: StateFlow<Int> = _maxBufferMs.asStateFlow()

    private val _bufferForPlaybackMs = MutableStateFlow(2500)
    val bufferForPlaybackMs: StateFlow<Int> = _bufferForPlaybackMs.asStateFlow()

    // Phase 3 & 4 Player HUD, Gestures, Equalizer, Channels, Subtitles & Orientation Preferences
    const val DEFAULT_PORTRAIT_BOTTOM_HUD = "LOCK,ROTATE,AUDIO_TRACK,SPEED,ASPECT,PIP"
    private const val KEY_PLAYER_CONTROLS_THEME = "pref_player_controls_theme"
    private const val KEY_HIDE_PLAYER_BTN_BG = "pref_hide_player_btn_bg"
    private const val KEY_FORCE_DARK_PLAYER_BTN_BG = "pref_force_dark_player_btn_bg"
    private const val KEY_SHOW_SEEKBAR_CONTAINER = "pref_show_seekbar_container"
    private const val KEY_PORTRAIT_CONTROLS_POS = "pref_portrait_controls_pos"
    private const val KEY_PLAYER_CLOCK_FORMAT = "pref_player_clock_format"
    private const val KEY_HUD_AUTO_HIDE_MS = "pref_hud_auto_hide_ms"
    private const val KEY_SHOW_SYS_STATUS_BAR = "pref_show_sys_status_bar"
    private const val KEY_SHOW_SYS_NAV_BAR = "pref_show_sys_nav_bar"
    private const val KEY_SAFE_AREA_WINDOW = "pref_safe_area_window"
    private const val KEY_SHOW_LOADING_CIRCLE = "pref_show_loading_circle"
    private const val KEY_PORTRAIT_BOTTOM_HUD = "pref_portrait_bottom_hud"
    private const val KEY_DOUBLE_TAP_AREA_PCT = "pref_double_tap_area_pct"
    private const val KEY_DOUBLE_TAP_CENTER_PP = "pref_double_tap_center_pp"
    private const val KEY_SINGLE_TAP_PAUSE = "pref_single_tap_pause"
    private const val KEY_DISPLAY_VOL_ON_RIGHT = "pref_display_vol_on_right"
    private const val KEY_REMEMBER_BRIGHTNESS = "pref_remember_brightness"
    private const val KEY_SAVED_BRIGHTNESS = "pref_saved_brightness"
    private const val KEY_HOLD_SPEED_MULT = "pref_hold_speed_mult"
    private const val KEY_H_SWIPE_SENSITIVITY = "pref_h_swipe_sensitivity"
    private const val KEY_SHOW_GESTURE_SEEK_HUD = "pref_show_gesture_seek_hud"
    private const val KEY_SHOW_GESTURE_VB_HUD = "pref_show_gesture_vb_hud"
    private const val KEY_SWIPE_UP_PLAYLIST = "pref_swipe_up_playlist"
    private const val KEY_CUSTOM_SKIP_SEC = "pref_custom_skip_sec"
    private const val KEY_AMBIENT_MODE_ENABLED = "pref_ambient_mode_enabled"
    private const val KEY_AUDIO_CHANNEL_MODE = "pref_audio_channel_mode"
    private const val KEY_EQ_CUSTOM_BANDS = "pref_eq_custom_bands"
    private const val KEY_SUB_SPEED_MILLI = "pref_sub_speed_milli"
    private const val KEY_SUB_ITALIC = "pref_sub_italic"
    private const val KEY_SUB_ALIGNMENT = "pref_sub_alignment"
    private const val KEY_SUB_CUSTOM_FONT_URI = "pref_sub_custom_font_uri"
    private const val KEY_SUB_CUSTOM_FONT_NAME = "pref_sub_custom_font_name"
    private const val KEY_SECONDARY_SUB_COLOR = "pref_secondary_sub_color"
    private const val KEY_PLAYER_SHUFFLE = "pref_player_shuffle"
    private const val KEY_VIDEO_ASPECT_MAP_JSON = "pref_video_aspect_map_json"

    // Phase 5, 6 & 7 Music, Recents, Playlists, Network, Font & Search History Preferences
    val ALL_MUSIC_TABS = listOf("SONGS", "ALBUMS", "ARTISTS", "PLAYLISTS", "FOLDERS", "QUEUE")
    private const val KEY_MUSIC_TAB_ORDER = "pref_music_tab_order"
    private const val KEY_MUSIC_HIDDEN_TABS = "pref_music_hidden_tabs"
    private const val KEY_MUSIC_SORT_BY = "pref_music_sort_by"
    private const val KEY_MUSIC_SORT_ASC = "pref_music_sort_asc"
    private const val KEY_MUSIC_GRID_VIEW = "pref_music_grid_view"
    private const val KEY_MUSIC_ART_SIZE_DP = "pref_music_art_size_dp"
    private const val KEY_MUSIC_PLAYLISTS_JSON = "pref_music_playlists_json"
    private const val KEY_RECENTS_FILTER = "pref_recents_filter"
    private const val KEY_RECENTS_GRID_VIEW = "pref_recents_grid_view"
    private const val KEY_NETWORK_BOOKMARKS_JSON = "pref_network_bookmarks_json"
    private const val KEY_NETWORK_SORT_BY = "pref_network_sort_by"
    private const val KEY_NETWORK_GRID_VIEW = "pref_network_grid_view"
    private const val KEY_USE_SYSTEM_FONT = "pref_use_system_font"
    private const val KEY_SETTINGS_SEARCH_HISTORY = "pref_settings_search_history"
    private const val KEY_REMEMBER_ASPECT_PER_VIDEO = "pref_remember_aspect_per_video"
    private const val KEY_CLOSE_AFTER_END = "pref_close_after_end_of_playback"
    private const val KEY_KEEP_SCREEN_ON_PAUSED = "pref_keep_screen_on_paused"
    private const val KEY_AUTOPLAY_AFTER_UNLOCK = "pref_autoplay_after_unlock"
    private const val KEY_PIP_ON_HOME_ONLY = "pref_pip_on_home_only"
    private const val KEY_VIDEO_MINI_PLAYER = "pref_video_mini_player"
    private const val KEY_CONTROLS_DRAWER_ENABLED = "pref_controls_drawer_enabled"
    private const val KEY_REDUCE_PLAYER_ANIMATION = "pref_reduce_player_animation"
    private const val KEY_ALLOW_GESTURES_IN_PANELS = "pref_allow_gestures_in_panels"
    private const val KEY_SHOW_SEEK_RIPPLE = "pref_show_seek_ripple"
    private const val KEY_SHOW_SEEK_TIME_HUD = "pref_show_seek_time_hud"
    private const val KEY_SHOW_BUFFERED_SEEKBAR = "pref_show_buffered_seekbar"
    private const val KEY_SHOW_CHAPTERS_SEEKBAR = "pref_show_chapters_seekbar"
    private const val KEY_DOUBLE_TAP_LEFT_ACTION = "pref_double_tap_left_action"
    private const val KEY_DOUBLE_TAP_CENTER_ACTION = "pref_double_tap_center_action"
    private const val KEY_DOUBLE_TAP_RIGHT_ACTION = "pref_double_tap_right_action"
    private const val KEY_SHOW_HOLD_SPEED_OVERLAY = "pref_show_hold_speed_overlay"
    private const val KEY_SHOW_ASPECT_FEEDBACK_OVERLAY = "pref_show_aspect_feedback_overlay"
    private const val KEY_SHOW_ZOOM_FEEDBACK_OVERLAY = "pref_show_zoom_feedback_overlay"
    private const val KEY_SHOW_REPEAT_SHUFFLE_OVERLAY = "pref_show_repeat_shuffle_overlay"
    private const val KEY_SHOW_ACTION_FEEDBACK_PILLS = "pref_show_action_feedback_pills"
    private const val KEY_INCLUDE_SUBS_SCREENSHOT = "pref_include_subs_screenshot"
    private const val KEY_PNG_COMPRESSION_LEVEL = "pref_png_compression_level"
    private const val KEY_SCREENSHOT_TEMPLATE = "pref_screenshot_template"
    private const val KEY_SUB_OVERRIDE_ASS = "pref_sub_override_ass_mode"
    private const val KEY_SUB_SCALE_BY_WINDOW = "pref_sub_scale_by_window"
    private const val KEY_SUB_FONTS_DIR_URI = "pref_sub_fonts_dir_uri"
    private const val KEY_SUB_FONTS_DIR_LABEL = "pref_sub_fonts_dir_label"
    private const val KEY_HOLD_DRAG_MOVES_SUBS = "pref_hold_drag_moves_subs"
    private const val KEY_SWIPE_SUBS_SEEK_DIALOG = "pref_swipe_subs_seek_dialog"
    private const val KEY_INVERT_SWIPE_SUBS_DIR = "pref_invert_swipe_subs_dir"
    private const val KEY_INCLUDE_AUDIO_IN_VIDEO_LIB = "pref_include_audio_in_video_lib"
    private const val KEY_MIN_AUDIO_DURATION_SEC = "pref_min_audio_duration_sec"
    private const val KEY_ALBUM_PALETTE_BG = "pref_album_palette_bg"
    private const val KEY_VOLUME_NORMALIZATION = "pref_volume_normalization"
    private const val KEY_VOLUME_BOOST_CAP = "pref_volume_boost_cap_pct"
    private const val KEY_EXTRA_SMART_COLLECTIONS = "pref_extra_smart_collections"
    private const val KEY_EXTRA_WATCH_STATS = "pref_extra_watch_stats"
    private const val KEY_EXTRA_PLAYBACK_DIAG = "pref_extra_playback_diag"
    private const val KEY_EXTRA_LIBRARY_HEALTH = "pref_extra_library_health"
    private const val KEY_EXTRA_QUALITY_FILTERS = "pref_extra_quality_filters"
    private const val KEY_EXTRA_ACTIVITY_LOG = "pref_extra_activity_log"

    // Master Prompt Extensions: mpv.conf, Hidden/SAF Folders, Intro/Outro, Online Subs, Audio Extras, Media Servers
    private const val KEY_MPV_CONF_ENABLED = "pref_mpv_conf_enabled"
    private const val KEY_MPV_CONF_TEXT = "pref_mpv_conf_text"
    private const val KEY_SHOW_HIDDEN_FOLDERS = "pref_show_hidden_folders"
    private const val KEY_SAF_MEDIA_TREES = "pref_saf_media_trees"
    private const val KEY_INTRO_OUTRO_ENABLED = "pref_intro_outro_enabled"
    private const val KEY_INTRO_PROVIDER = "pref_intro_provider"
    private const val KEY_DETECT_INTRO_CHAPTERS = "pref_detect_intro_chapters"
    private const val KEY_AUTO_SKIP_INTRO = "pref_auto_skip_intro"
    private const val KEY_AUTO_SKIP_OUTRO = "pref_auto_skip_outro"
    private const val KEY_CUSTOM_INTRO_KEYWORDS = "pref_custom_intro_keywords"
    private const val KEY_CUSTOM_OUTRO_KEYWORDS = "pref_custom_outro_keywords"
    private const val KEY_SHOW_PROVIDER_STATUS_OV = "pref_show_provider_status_ov"
    private const val KEY_ONLINE_SUB_MODE = "pref_online_sub_mode"
    private const val KEY_ONLINE_SUB_LANG = "pref_online_sub_lang"
    private const val KEY_AUDIO_STANDBY_SEC = "pref_audio_standby_sec"
    private const val KEY_MUSIC_VISUALIZER_STYLE = "pref_music_visualizer_style"
    private const val KEY_MUSIC_ORIENTATION = "pref_music_orientation"
    private const val KEY_MEDIA_SERVERS_JSON = "pref_media_servers_json"
    private const val KEY_ENABLED_SERVER_TABS = "pref_enabled_server_tabs"
    private const val KEY_MEDIA_QUALITY_FILTER = "pref_media_quality_filter"
    private const val KEY_ONLY_SHOW_FOLDERS = "pref_only_show_folders"
    private const val KEY_HOME_COMPACT_GRID = "pref_home_compact_grid"

    val OPTIONAL_SERVER_TABS = listOf("JELLYFIN", "NAVIDROME", "AUDIOBOOKS")

    private val _rememberAspectPerVideo = MutableStateFlow(true)
    val rememberAspectPerVideo: StateFlow<Boolean> = _rememberAspectPerVideo.asStateFlow()

    private val _closeAfterEndOfPlayback = MutableStateFlow(false)
    val closeAfterEndOfPlayback: StateFlow<Boolean> = _closeAfterEndOfPlayback.asStateFlow()

    private val _keepScreenOnWhenPaused = MutableStateFlow(false)
    val keepScreenOnWhenPaused: StateFlow<Boolean> = _keepScreenOnWhenPaused.asStateFlow()

    private val _autoplayAfterScreenUnlock = MutableStateFlow(true)
    val autoplayAfterScreenUnlock: StateFlow<Boolean> = _autoplayAfterScreenUnlock.asStateFlow()

    private val _pipOnHomeGestureOnly = MutableStateFlow(false)
    val pipOnHomeGestureOnly: StateFlow<Boolean> = _pipOnHomeGestureOnly.asStateFlow()

    private val _videoMiniPlayerEnabled = MutableStateFlow(false)
    val videoMiniPlayerEnabled: StateFlow<Boolean> = _videoMiniPlayerEnabled.asStateFlow()

    private val _controlsDrawerEnabled = MutableStateFlow(true)
    val controlsDrawerEnabled: StateFlow<Boolean> = _controlsDrawerEnabled.asStateFlow()

    private val _reducePlayerAnimation = MutableStateFlow(false)
    val reducePlayerAnimation: StateFlow<Boolean> = _reducePlayerAnimation.asStateFlow()

    private val _allowGesturesInPanels = MutableStateFlow(false)
    val allowGesturesInPanels: StateFlow<Boolean> = _allowGesturesInPanels.asStateFlow()

    private val _showSeekRipple = MutableStateFlow(true)
    val showSeekRipple: StateFlow<Boolean> = _showSeekRipple.asStateFlow()

    private val _showSeekTimeInHud = MutableStateFlow(true)
    val showSeekTimeInHud: StateFlow<Boolean> = _showSeekTimeInHud.asStateFlow()

    private val _showBufferedRangeOnSeekbar = MutableStateFlow(true)
    val showBufferedRangeOnSeekbar: StateFlow<Boolean> = _showBufferedRangeOnSeekbar.asStateFlow()

    private val _showChaptersOnSeekbar = MutableStateFlow(true)
    val showChaptersOnSeekbar: StateFlow<Boolean> = _showChaptersOnSeekbar.asStateFlow()

    private val _doubleTapLeftAction = MutableStateFlow("SEEK")
    val doubleTapLeftAction: StateFlow<String> = _doubleTapLeftAction.asStateFlow()

    private val _doubleTapCenterAction = MutableStateFlow("PLAY_PAUSE")
    val doubleTapCenterAction: StateFlow<String> = _doubleTapCenterAction.asStateFlow()

    private val _doubleTapRightAction = MutableStateFlow("SEEK")
    val doubleTapRightAction: StateFlow<String> = _doubleTapRightAction.asStateFlow()

    private val _showHoldSpeedOverlay = MutableStateFlow(true)
    val showHoldSpeedOverlay: StateFlow<Boolean> = _showHoldSpeedOverlay.asStateFlow()

    private val _showAspectFeedbackOverlay = MutableStateFlow(true)
    val showAspectFeedbackOverlay: StateFlow<Boolean> = _showAspectFeedbackOverlay.asStateFlow()

    private val _showZoomFeedbackOverlay = MutableStateFlow(true)
    val showZoomFeedbackOverlay: StateFlow<Boolean> = _showZoomFeedbackOverlay.asStateFlow()

    private val _showRepeatShuffleOverlay = MutableStateFlow(true)
    val showRepeatShuffleOverlay: StateFlow<Boolean> = _showRepeatShuffleOverlay.asStateFlow()

    private val _showActionFeedbackPills = MutableStateFlow(true)
    val showActionFeedbackPills: StateFlow<Boolean> = _showActionFeedbackPills.asStateFlow()

    private val _includeSubtitlesInScreenshot = MutableStateFlow(true)
    val includeSubtitlesInScreenshot: StateFlow<Boolean> = _includeSubtitlesInScreenshot.asStateFlow()

    private val _pngCompressionLevel = MutableStateFlow(6)
    val pngCompressionLevel: StateFlow<Int> = _pngCompressionLevel.asStateFlow()

    private val _screenshotFilenameTemplate = MutableStateFlow("MPVX_{title}_{time}")
    val screenshotFilenameTemplate: StateFlow<String> = _screenshotFilenameTemplate.asStateFlow()

    private val _subOverrideAssMode = MutableStateFlow("SCALE")
    val subOverrideAssMode: StateFlow<String> = _subOverrideAssMode.asStateFlow()

    private val _subScaleByWindow = MutableStateFlow(true)
    val subScaleByWindow: StateFlow<Boolean> = _subScaleByWindow.asStateFlow()

    private val _subFontsDirUri = MutableStateFlow("")
    val subFontsDirUri: StateFlow<String> = _subFontsDirUri.asStateFlow()

    private val _subFontsDirLabel = MutableStateFlow("App Default")
    val subFontsDirLabel: StateFlow<String> = _subFontsDirLabel.asStateFlow()

    private val _holdDragMovesSubtitles = MutableStateFlow(false)
    val holdDragMovesSubtitles: StateFlow<Boolean> = _holdDragMovesSubtitles.asStateFlow()

    private val _swipeSubtitlesToSeekDialog = MutableStateFlow(false)
    val swipeSubtitlesToSeekDialog: StateFlow<Boolean> = _swipeSubtitlesToSeekDialog.asStateFlow()

    private val _invertSwipeSubtitlesDirection = MutableStateFlow(false)
    val invertSwipeSubtitlesDirection: StateFlow<Boolean> = _invertSwipeSubtitlesDirection.asStateFlow()

    private val _includeAudioFilesInVideoLib = MutableStateFlow(false)
    val includeAudioFilesInVideoLib: StateFlow<Boolean> = _includeAudioFilesInVideoLib.asStateFlow()

    private val _minAudioDurationSec = MutableStateFlow(10)
    val minAudioDurationSec: StateFlow<Int> = _minAudioDurationSec.asStateFlow()

    private val _albumPaletteBackground = MutableStateFlow(true)
    val albumPaletteBackground: StateFlow<Boolean> = _albumPaletteBackground.asStateFlow()

    private val _volumeNormalization = MutableStateFlow(false)
    val volumeNormalization: StateFlow<Boolean> = _volumeNormalization.asStateFlow()

    private val _volumeBoostCapPct = MutableStateFlow(200)
    val volumeBoostCapPct: StateFlow<Int> = _volumeBoostCapPct.asStateFlow()

    private val _extraSmartCollections = MutableStateFlow(false)
    val extraSmartCollections: StateFlow<Boolean> = _extraSmartCollections.asStateFlow()

    private val _extraWatchStats = MutableStateFlow(false)
    val extraWatchStats: StateFlow<Boolean> = _extraWatchStats.asStateFlow()

    private val _extraPlaybackDiagnostics = MutableStateFlow(false)
    val extraPlaybackDiagnostics: StateFlow<Boolean> = _extraPlaybackDiagnostics.asStateFlow()

    private val _extraLibraryHealth = MutableStateFlow(false)
    val extraLibraryHealth: StateFlow<Boolean> = _extraLibraryHealth.asStateFlow()

    private val _extraQualityFilters = MutableStateFlow(false)
    val extraQualityFilters: StateFlow<Boolean> = _extraQualityFilters.asStateFlow()

    private val _extraActivityLog = MutableStateFlow(false)
    val extraActivityLog: StateFlow<Boolean> = _extraActivityLog.asStateFlow()

    private val _mpvConfEnabled = MutableStateFlow(false)
    val mpvConfEnabled: StateFlow<Boolean> = _mpvConfEnabled.asStateFlow()

    private val _mpvConfText = MutableStateFlow("")
    val mpvConfText: StateFlow<String> = _mpvConfText.asStateFlow()

    private val _showHiddenMediaFolders = MutableStateFlow(false)
    val showHiddenMediaFolders: StateFlow<Boolean> = _showHiddenMediaFolders.asStateFlow()

    private val _safMediaTreeUris = MutableStateFlow<Set<String>>(emptySet())
    val safMediaTreeUris: StateFlow<Set<String>> = _safMediaTreeUris.asStateFlow()

    private val _introOutroEnabled = MutableStateFlow(true)
    val introOutroEnabled: StateFlow<Boolean> = _introOutroEnabled.asStateFlow()

    private val _introProvider = MutableStateFlow("HYBRID")
    val introProvider: StateFlow<String> = _introProvider.asStateFlow()

    private val _detectIntroFromChapters = MutableStateFlow(true)
    val detectIntroFromChapters: StateFlow<Boolean> = _detectIntroFromChapters.asStateFlow()

    private val _autoSkipIntro = MutableStateFlow(false)
    val autoSkipIntro: StateFlow<Boolean> = _autoSkipIntro.asStateFlow()

    private val _autoSkipOutro = MutableStateFlow(false)
    val autoSkipOutro: StateFlow<Boolean> = _autoSkipOutro.asStateFlow()

    private val _customIntroKeywords = MutableStateFlow("")
    val customIntroKeywords: StateFlow<String> = _customIntroKeywords.asStateFlow()

    private val _customOutroKeywords = MutableStateFlow("")
    val customOutroKeywords: StateFlow<String> = _customOutroKeywords.asStateFlow()

    private val _showProviderStatusOverlay = MutableStateFlow(false)
    val showProviderStatusOverlay: StateFlow<Boolean> = _showProviderStatusOverlay.asStateFlow()

    private val _onlineSubSearchMode = MutableStateFlow("HYBRID")
    val onlineSubSearchMode: StateFlow<String> = _onlineSubSearchMode.asStateFlow()

    private val _onlineSubPreferredLang = MutableStateFlow("en")
    val onlineSubPreferredLang: StateFlow<String> = _onlineSubPreferredLang.asStateFlow()

    private val _audioStandbyDelaySec = MutableStateFlow(0)
    val audioStandbyDelaySec: StateFlow<Int> = _audioStandbyDelaySec.asStateFlow()

    private val _musicVisualizerStyle = MutableStateFlow("WAVE")
    val musicVisualizerStyle: StateFlow<String> = _musicVisualizerStyle.asStateFlow()

    private val _musicPlayerOrientation = MutableStateFlow("AUTO")
    val musicPlayerOrientation: StateFlow<String> = _musicPlayerOrientation.asStateFlow()

    private val _mediaServers = MutableStateFlow<List<MediaServerProfile>>(emptyList())
    val mediaServers: StateFlow<List<MediaServerProfile>> = _mediaServers.asStateFlow()

    private val _enabledServerTabs = MutableStateFlow<Set<String>>(emptySet())
    val enabledServerTabs: StateFlow<Set<String>> = _enabledServerTabs.asStateFlow()

    private val _mediaQualityFilter = MutableStateFlow("ALL")
    val mediaQualityFilter: StateFlow<String> = _mediaQualityFilter.asStateFlow()

    private val _onlyShowFoldersInHome = MutableStateFlow(false)
    val onlyShowFoldersInHome: StateFlow<Boolean> = _onlyShowFoldersInHome.asStateFlow()

    private val _homeCompactGrid = MutableStateFlow(false)
    val homeCompactGrid: StateFlow<Boolean> = _homeCompactGrid.asStateFlow()

    private val _playerControlsTheme = MutableStateFlow("GLASS")
    val playerControlsTheme: StateFlow<String> = _playerControlsTheme.asStateFlow()

    private val _hidePlayerButtonsBg = MutableStateFlow(false)
    val hidePlayerButtonsBg: StateFlow<Boolean> = _hidePlayerButtonsBg.asStateFlow()

    private val _forceDarkPlayerButtonsBg = MutableStateFlow(false)
    val forceDarkPlayerButtonsBg: StateFlow<Boolean> = _forceDarkPlayerButtonsBg.asStateFlow()

    private val _showSeekbarOuterContainer = MutableStateFlow(true)
    val showSeekbarOuterContainer: StateFlow<Boolean> = _showSeekbarOuterContainer.asStateFlow()

    private val _portraitControlsPosition = MutableStateFlow("CENTER")
    val portraitControlsPosition: StateFlow<String> = _portraitControlsPosition.asStateFlow()

    private val _playerClockFormat = MutableStateFlow("SYSTEM")
    val playerClockFormat: StateFlow<String> = _playerClockFormat.asStateFlow()

    private val _hudAutoHideMs = MutableStateFlow(4000)
    val hudAutoHideMs: StateFlow<Int> = _hudAutoHideMs.asStateFlow()

    private val _showSystemStatusBar = MutableStateFlow(false)
    val showSystemStatusBar: StateFlow<Boolean> = _showSystemStatusBar.asStateFlow()

    private val _showSystemNavBar = MutableStateFlow(false)
    val showSystemNavBar: StateFlow<Boolean> = _showSystemNavBar.asStateFlow()

    private val _safeAreaWindow = MutableStateFlow(false)
    val safeAreaWindow: StateFlow<Boolean> = _safeAreaWindow.asStateFlow()

    private val _showLoadingCircle = MutableStateFlow(true)
    val showLoadingCircle: StateFlow<Boolean> = _showLoadingCircle.asStateFlow()

    private val _portraitBottomHudControls = MutableStateFlow(DEFAULT_PORTRAIT_BOTTOM_HUD)
    val portraitBottomHudControls: StateFlow<String> = _portraitBottomHudControls.asStateFlow()

    private val _doubleTapAreaWidthPct = MutableStateFlow(28)
    val doubleTapAreaWidthPct: StateFlow<Int> = _doubleTapAreaWidthPct.asStateFlow()

    private val _doubleTapCenterPlayPause = MutableStateFlow(true)
    val doubleTapCenterPlayPause: StateFlow<Boolean> = _doubleTapCenterPlayPause.asStateFlow()

    private val _useSingleTapToPause = MutableStateFlow(false)
    val useSingleTapToPause: StateFlow<Boolean> = _useSingleTapToPause.asStateFlow()

    private val _displayVolumeOnRight = MutableStateFlow(true)
    val displayVolumeOnRight: StateFlow<Boolean> = _displayVolumeOnRight.asStateFlow()

    private val _rememberBrightness = MutableStateFlow(false)
    val rememberBrightness: StateFlow<Boolean> = _rememberBrightness.asStateFlow()

    private val _savedPlayerBrightness = MutableStateFlow(0.7f)
    val savedPlayerBrightness: StateFlow<Float> = _savedPlayerBrightness.asStateFlow()

    private val _holdSpeedMultiplier = MutableStateFlow(2.0f)
    val holdSpeedMultiplier: StateFlow<Float> = _holdSpeedMultiplier.asStateFlow()

    private val _horizontalSwipeSensitivity = MutableStateFlow(1.0f)
    val horizontalSwipeSensitivity: StateFlow<Float> = _horizontalSwipeSensitivity.asStateFlow()

    private val _showGestureSeekOverlay = MutableStateFlow(true)
    val showGestureSeekOverlay: StateFlow<Boolean> = _showGestureSeekOverlay.asStateFlow()

    private val _showGestureVolBrightOverlay = MutableStateFlow(true)
    val showGestureVolBrightOverlay: StateFlow<Boolean> = _showGestureVolBrightOverlay.asStateFlow()

    private val _swipeUpOpensPlaylist = MutableStateFlow(true)
    val swipeUpOpensPlaylist: StateFlow<Boolean> = _swipeUpOpensPlaylist.asStateFlow()

    private val _customSkipDurationSec = MutableStateFlow(90)
    val customSkipDurationSec: StateFlow<Int> = _customSkipDurationSec.asStateFlow()

    private val _ambientModeEnabled = MutableStateFlow(false)
    val ambientModeEnabled: StateFlow<Boolean> = _ambientModeEnabled.asStateFlow()

    private val _audioChannelMode = MutableStateFlow("AUTO")
    val audioChannelMode: StateFlow<String> = _audioChannelMode.asStateFlow()

    private val _eqCustomBandsDb = MutableStateFlow("0.0,0.0,0.0,0.0,0.0")
    val eqCustomBandsDb: StateFlow<String> = _eqCustomBandsDb.asStateFlow()

    private val _subtitleSpeedMilli = MutableStateFlow(1000)
    val subtitleSpeedMilli: StateFlow<Int> = _subtitleSpeedMilli.asStateFlow()

    private val _subtitleItalic = MutableStateFlow(false)
    val subtitleItalic: StateFlow<Boolean> = _subtitleItalic.asStateFlow()

    private val _subtitleAlignment = MutableStateFlow("CENTER")
    val subtitleAlignment: StateFlow<String> = _subtitleAlignment.asStateFlow()

    private val _subtitleCustomFontUri = MutableStateFlow("")
    val subtitleCustomFontUri: StateFlow<String> = _subtitleCustomFontUri.asStateFlow()

    private val _subtitleCustomFontName = MutableStateFlow("System Default")
    val subtitleCustomFontName: StateFlow<String> = _subtitleCustomFontName.asStateFlow()

    private val _secondarySubtitleColor = MutableStateFlow(0xFFFDE047)
    val secondarySubtitleColor: StateFlow<Long> = _secondarySubtitleColor.asStateFlow()

    private val _playerShuffleMode = MutableStateFlow(false)
    val playerShuffleMode: StateFlow<Boolean> = _playerShuffleMode.asStateFlow()

    private val _musicTabOrder = MutableStateFlow(ALL_MUSIC_TABS)
    val musicTabOrder: StateFlow<List<String>> = _musicTabOrder.asStateFlow()

    private val _musicHiddenTabs = MutableStateFlow<Set<String>>(emptySet())
    val musicHiddenTabs: StateFlow<Set<String>> = _musicHiddenTabs.asStateFlow()

    private val _musicSortBy = MutableStateFlow("TITLE")
    val musicSortBy: StateFlow<String> = _musicSortBy.asStateFlow()

    private val _musicSortAscending = MutableStateFlow(true)
    val musicSortAscending: StateFlow<Boolean> = _musicSortAscending.asStateFlow()

    private val _musicGridView = MutableStateFlow(false)
    val musicGridView: StateFlow<Boolean> = _musicGridView.asStateFlow()

    private val _musicArtSizeDp = MutableStateFlow(56)
    val musicArtSizeDp: StateFlow<Int> = _musicArtSizeDp.asStateFlow()

    private val _musicPlaylists = MutableStateFlow<List<UserPlaylist>>(emptyList())
    val musicPlaylists: StateFlow<List<UserPlaylist>> = _musicPlaylists.asStateFlow()

    private val _recentsFilter = MutableStateFlow("ALL") // ALL, VIDEO, AUDIO
    val recentsFilter: StateFlow<String> = _recentsFilter.asStateFlow()

    private val _recentsGridView = MutableStateFlow(false)
    val recentsGridView: StateFlow<Boolean> = _recentsGridView.asStateFlow()

    private val _networkBookmarks = MutableStateFlow<List<NetworkBookmark>>(emptyList())
    val networkBookmarks: StateFlow<List<NetworkBookmark>> = _networkBookmarks.asStateFlow()

    private val _networkSortBy = MutableStateFlow("FOLDERS_FIRST")
    val networkSortBy: StateFlow<String> = _networkSortBy.asStateFlow()

    private val _networkGridView = MutableStateFlow(false)
    val networkGridView: StateFlow<Boolean> = _networkGridView.asStateFlow()

    private val _useSystemFont = MutableStateFlow(false)
    val useSystemFont: StateFlow<Boolean> = _useSystemFont.asStateFlow()

    private val _settingsSearchHistory = MutableStateFlow<List<String>>(emptyList())
    val settingsSearchHistory: StateFlow<List<String>> = _settingsSearchHistory.asStateFlow()

    private val _bufferForPlaybackAfterRebufferMs = MutableStateFlow(5000)
    val bufferForPlaybackAfterRebufferMs: StateFlow<Int> = _bufferForPlaybackAfterRebufferMs.asStateFlow()

    // 9. Library & Media Preferences
    private val _sortOption = MutableStateFlow("TITLE") // TITLE, DATE, SIZE, DURATION
    val sortOption: StateFlow<String> = _sortOption.asStateFlow()

    private val _sortAscending = MutableStateFlow(true)
    val sortAscending: StateFlow<Boolean> = _sortAscending.asStateFlow()

    private val _viewMode = MutableStateFlow("LIST") // LIST, GRID
    val viewMode: StateFlow<String> = _viewMode.asStateFlow()

    private val _folderViewMode = MutableStateFlow("FOLDER") // FOLDER, TREE
    val folderViewMode: StateFlow<String> = _folderViewMode.asStateFlow()

    private val _showFolderSize = MutableStateFlow(true)
    val showFolderSize: StateFlow<Boolean> = _showFolderSize.asStateFlow()

    private val _showFolderPath = MutableStateFlow(true)
    val showFolderPath: StateFlow<Boolean> = _showFolderPath.asStateFlow()

    private val _showDateModified = MutableStateFlow(true)
    val showDateModified: StateFlow<Boolean> = _showDateModified.asStateFlow()

    private val _showResolutionBadge = MutableStateFlow(true)
    val showResolutionBadge: StateFlow<Boolean> = _showResolutionBadge.asStateFlow()

    // 10. Background Playback & PiP
    private val _backgroundPlayback = MutableStateFlow(false)
    val backgroundPlayback: StateFlow<Boolean> = _backgroundPlayback.asStateFlow()

    private val _autoPiP = MutableStateFlow(true)
    val autoPiP: StateFlow<Boolean> = _autoPiP.asStateFlow()

    // Dual-Engine MPV Renderer, Deband & Shader Config
    private val _anime4k = MutableStateFlow(false)
    val anime4k: StateFlow<Boolean> = _anime4k.asStateFlow()

    private val _anime4kMode = MutableStateFlow("OFF")
    val anime4kMode: StateFlow<String> = _anime4kMode.asStateFlow()

    private val _anime4kQuality = MutableStateFlow("FAST")
    val anime4kQuality: StateFlow<String> = _anime4kQuality.asStateFlow()

    private val _hdrTonemapping = MutableStateFlow(true)
    val hdrTonemapping: StateFlow<Boolean> = _hdrTonemapping.asStateFlow()

    private val _mpvProfile = MutableStateFlow("fast")
    val mpvProfile: StateFlow<String> = _mpvProfile.asStateFlow()

    private val _useGpuNext = MutableStateFlow(false)
    val useGpuNext: StateFlow<Boolean> = _useGpuNext.asStateFlow()

    private val _useVulkan = MutableStateFlow(false)
    val useVulkan: StateFlow<Boolean> = _useVulkan.asStateFlow()

    private val _useYuv420p = MutableStateFlow(false)
    val useYuv420p: StateFlow<Boolean> = _useYuv420p.asStateFlow()

    private val _mpvDebanding = MutableStateFlow("NONE")
    val mpvDebanding: StateFlow<String> = _mpvDebanding.asStateFlow()

    private val _debandIterations = MutableStateFlow(1)
    val debandIterations: StateFlow<Int> = _debandIterations.asStateFlow()

    private val _debandThreshold = MutableStateFlow(48)
    val debandThreshold: StateFlow<Int> = _debandThreshold.asStateFlow()

    private val _debandRange = MutableStateFlow(16)
    val debandRange: StateFlow<Int> = _debandRange.asStateFlow()

    private val _debandGrain = MutableStateFlow(32)
    val debandGrain: StateFlow<Int> = _debandGrain.asStateFlow()

    private val _manualGridColumnsEnabled = MutableStateFlow(false)
    val manualGridColumnsEnabled: StateFlow<Boolean> = _manualGridColumnsEnabled.asStateFlow()

    private val _separateFolderVideoLayout = MutableStateFlow(false)
    val separateFolderVideoLayout: StateFlow<Boolean> = _separateFolderVideoLayout.asStateFlow()

    private val _networkThumbnailsEnabled = MutableStateFlow(false)
    val networkThumbnailsEnabled: StateFlow<Boolean> = _networkThumbnailsEnabled.asStateFlow()

    private val _thumbnailCacheSizeMb = MutableStateFlow(128)
    val thumbnailCacheSizeMb: StateFlow<Int> = _thumbnailCacheSizeMb.asStateFlow()

    private val _showScreenRotationOverlay = MutableStateFlow(true)
    val showScreenRotationOverlay: StateFlow<Boolean> = _showScreenRotationOverlay.asStateFlow()

    private val _showAspectRatioOverlay = MutableStateFlow(true)
    val showAspectRatioOverlay: StateFlow<Boolean> = _showAspectRatioOverlay.asStateFlow()

    private val _showPlaybackSpeedOverlay = MutableStateFlow(true)
    val showPlaybackSpeedOverlay: StateFlow<Boolean> = _showPlaybackSpeedOverlay.asStateFlow()

    private val _showDecoderOverlay = MutableStateFlow(true)
    val showDecoderOverlay: StateFlow<Boolean> = _showDecoderOverlay.asStateFlow()

    private val _showShuffleOverlay = MutableStateFlow(true)
    val showShuffleOverlay: StateFlow<Boolean> = _showShuffleOverlay.asStateFlow()

    private val _showLoopOverlay = MutableStateFlow(true)
    val showLoopOverlay: StateFlow<Boolean> = _showLoopOverlay.asStateFlow()

    private val _showPipOverlay = MutableStateFlow(true)
    val showPipOverlay: StateFlow<Boolean> = _showPipOverlay.asStateFlow()

    private val _showScreenshotOverlay = MutableStateFlow(true)
    val showScreenshotOverlay: StateFlow<Boolean> = _showScreenshotOverlay.asStateFlow()

    private val _screenshotFormat = MutableStateFlow("PNG")
    val screenshotFormat: StateFlow<String> = _screenshotFormat.asStateFlow()

    private val _screenshotQuality = MutableStateFlow(95)
    val screenshotQuality: StateFlow<Int> = _screenshotQuality.asStateFlow()

    private val _screenshotFolder = MutableStateFlow("MPVX Screenshots")
    val screenshotFolder: StateFlow<String> = _screenshotFolder.asStateFlow()

    private val _doubleTapPauseResume = MutableStateFlow(true)
    val doubleTapPauseResume: StateFlow<Boolean> = _doubleTapPauseResume.asStateFlow()

    private val _longPressSpeedVariable = MutableStateFlow(false)
    val longPressSpeedVariable: StateFlow<Boolean> = _longPressSpeedVariable.asStateFlow()

    private val _hasCustomVaultPin = MutableStateFlow(false)
    val hasCustomVaultPin: StateFlow<Boolean> = _hasCustomVaultPin.asStateFlow()

    private var inMemoryVaultSalt: String? = null
    private var inMemoryVaultHash: String? = null

    private val _isVaultUnlocked = MutableStateFlow(false)
    val isVaultUnlocked: StateFlow<Boolean> = _isVaultUnlocked.asStateFlow()

    // Mini Player
    private val _showMiniPlayer = MutableStateFlow(false)
    val showMiniPlayer: StateFlow<Boolean> = _showMiniPlayer.asStateFlow()
    val isMiniPlayerActive: StateFlow<Boolean> = _showMiniPlayer.asStateFlow()

    private val _miniPlayerTitle = MutableStateFlow("Audio Playback")
    val miniPlayerTitle: StateFlow<String> = _miniPlayerTitle.asStateFlow()

    private val _miniPlayerSubtitle = MutableStateFlow("MPVX")
    val miniPlayerSubtitle: StateFlow<String> = _miniPlayerSubtitle.asStateFlow()

    private val _isMiniPlayerPlaying = MutableStateFlow(true)
    val isMiniPlayerPlaying: StateFlow<Boolean> = _isMiniPlayerPlaying.asStateFlow()

    private val _miniPlayerProgress = MutableStateFlow(0f)
    val miniPlayerProgress: StateFlow<Float> = _miniPlayerProgress.asStateFlow()

    fun init(context: Context) {
        appContext = context.applicationContext
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        loadFromPrefs(prefs)
    }

    internal fun loadFromPrefs(prefs: SharedPreferences) {
        _isDarkTheme.value = prefs.getBoolean(KEY_DARK_THEME, true)
        _darkMode.value = prefs.getString(
            KEY_DARK_MODE,
            if (prefs.contains(KEY_DARK_THEME)) {
                if (prefs.getBoolean(KEY_DARK_THEME, true)) "DARK" else "LIGHT"
            } else {
                "SYSTEM"
            }
        ) ?: "SYSTEM"
        _favoriteThemePresets.value = prefs.getStringSet(KEY_FAVORITE_THEMES, emptySet())?.toSet() ?: emptySet()
        _customThemeWallpaperUri.value = prefs.getString(KEY_CUSTOM_WALLPAPER_URI, "") ?: ""
        _customThemeWallpaperIsVideo.value = prefs.getBoolean(KEY_CUSTOM_WALLPAPER_IS_VIDEO, false)
        _customThemeWallpaperBlur.value = prefs.getFloat(KEY_CUSTOM_WALLPAPER_BLUR, 0f).coerceIn(0f, 40f)
        _customThemeWallpaperBrightness.value = prefs.getFloat(KEY_CUSTOM_WALLPAPER_BRIGHTNESS, 1.0f).coerceIn(0.2f, 1.6f)
        _customThemeWallpaperSaturation.value = prefs.getFloat(KEY_CUSTOM_WALLPAPER_SATURATION, 1.0f).coerceIn(0f, 2.0f)
        _customThemeWallpaperScrimAlpha.value = prefs.getFloat(KEY_CUSTOM_WALLPAPER_SCRIM, 0.45f).coerceIn(0f, 0.95f)
        _customThemeSurfaceOpacity.value = prefs.getFloat(KEY_CUSTOM_SURFACE_OPACITY, 0.85f).coerceIn(0.2f, 1.0f)
        val presetName = prefs.getString(KEY_THEME_PRESET, ThemePreset.MPVX_CYAN.name) ?: ThemePreset.MPVX_CYAN.name
        _themePreset.value = try { ThemePreset.valueOf(presetName) } catch (e: Exception) { ThemePreset.MPVX_CYAN }
        _isAmoled.value = prefs.getBoolean(KEY_AMOLED, false)
        _isFrostedPlayerSheets.value = prefs.getBoolean(KEY_FROSTED_SHEETS, true)
        _isLiquidGlassEnabled.value = prefs.getBoolean(KEY_LIQUID_GLASS, false)
        _glassBlurRadius.value = prefs.getFloat(KEY_GLASS_BLUR, 16f)
        _glassFrostOpacity.value = prefs.getFloat(KEY_GLASS_OPACITY, 0.78f)
        _is3dControlsEnabled.value = prefs.getBoolean(KEY_3D_CONTROLS, false)
        _customThemeColor.value = prefs.getLong(KEY_CUSTOM_THEME_COLOR, 0xFF2563EB)

        val storedEngine = prefs.getString(KEY_DEFAULT_ENGINE, "AUTO") ?: "AUTO"
        _defaultEngine.value = when {
            storedEngine.equals("MEDIA3", ignoreCase = true) -> "NATIVE"
            else -> PlaybackEngineMode.fromId(storedEngine).id
        }
        _decoderMode.value = prefs.getString(KEY_DECODER_MODE, "HW+") ?: "HW+"
        _hwAcceleration.value = prefs.getBoolean(KEY_HW_ACCELERATION, true)
        _autoRoutingDolbyVision.value = prefs.getBoolean(KEY_AUTO_ROUTING_DV, true)
        _autoRoutingHlsDash.value = prefs.getBoolean(KEY_AUTO_ROUTING_STREAMS, true)

        _smoothMotion120fps.value = prefs.getBoolean(KEY_SMOOTH_MOTION, true)
        _targetFps.value = prefs.getInt(KEY_TARGET_FPS, 120)
        _uiAnimationScale.value = prefs.getFloat(KEY_UI_ANIMATION_SCALE, 1.0f)

        _subtitlesEnabled.value = prefs.getBoolean(KEY_SUBTITLE_ENABLED, true)
        _dualSubtitles.value = prefs.getBoolean(KEY_DUAL_SUBTITLES, false)
        _subtitleFontSize.value = prefs.getFloat(KEY_SUBTITLE_FONT_SIZE, 22f)
        _subtitleColor.value = prefs.getLong(KEY_SUBTITLE_COLOR, 0xFFFFFFFF)
        _subtitleBackground.value = prefs.getLong(KEY_SUBTITLE_BG, 0x80000000)
        _subtitleDelayMs.value = prefs.getInt(KEY_SUBTITLE_DELAY, 0)
        _subtitleLanguage.value = prefs.getString(KEY_SUBTITLE_LANG, "Auto") ?: "Auto"

        _audioLanguage.value = prefs.getString(KEY_AUDIO_LANG, "Auto") ?: "Auto"
        _audioDelayMs.value = prefs.getInt(KEY_AUDIO_DELAY, 0)
        _audioBoost.value = prefs.getInt(KEY_AUDIO_BOOST, 100)
        _audioOffload.value = prefs.getBoolean(KEY_AUDIO_OFFLOAD, true)
        _nightModeCompressor.value = prefs.getBoolean(KEY_NIGHT_COMPRESSOR, false)
        _eqPreset.value = prefs.getString(KEY_EQ_PRESET, "Cinema") ?: "Cinema"

        _volumeGesture.value = prefs.getBoolean(KEY_VOLUME_GESTURE, true)
        _brightnessGesture.value = prefs.getBoolean(KEY_BRIGHT_GESTURE, true)
        _doubleTapSeconds.value = prefs.getInt(KEY_DOUBLE_TAP_SEC, 10)
        _seekGesture.value = prefs.getBoolean(KEY_SEEK_GESTURE, true)
        _hapticFeedback.value = prefs.getBoolean(KEY_HAPTIC_FEEDBACK, true)
        _longPress2x.value = prefs.getBoolean(KEY_LONG_PRESS_2X, true)

        _defaultAspectRatio.value = prefs.getString(KEY_DEFAULT_ASPECT, "Fit") ?: "Fit"
        _screenOrientation.value = prefs.getString(KEY_ORIENTATION, "Auto") ?: "Auto"
        _keepScreenOn.value = prefs.getBoolean(KEY_KEEP_SCREEN_ON, true)
        _defaultSpeed.value = prefs.getFloat(KEY_DEFAULT_SPEED, 1.0f)

        _minBufferMs.value = prefs.getInt(KEY_MIN_BUFFER_MS, 50000)
        _maxBufferMs.value = prefs.getInt(KEY_MAX_BUFFER_MS, 50000)
        _bufferForPlaybackMs.value = prefs.getInt(KEY_BUFFER_PLAYBACK_MS, 2500)
        _bufferForPlaybackAfterRebufferMs.value = prefs.getInt(KEY_BUFFER_REBUFFER_MS, 5000)

        _sortOption.value = prefs.getString(KEY_SORT_OPTION, "TITLE") ?: "TITLE"
        _sortAscending.value = prefs.getBoolean(KEY_SORT_ASC, true)
        _viewMode.value = prefs.getString(KEY_VIEW_MODE, "LIST") ?: "LIST"
        _folderViewMode.value = prefs.getString(KEY_FOLDER_VIEW_MODE, "FOLDER") ?: "FOLDER"
        _showFolderSize.value = prefs.getBoolean(KEY_SHOW_FOLDER_SIZE, true)
        _showFolderPath.value = prefs.getBoolean(KEY_SHOW_FOLDER_PATH, true)
        _showDateModified.value = prefs.getBoolean(KEY_SHOW_DATE_MODIFIED, true)
        _showResolutionBadge.value = prefs.getBoolean(KEY_SHOW_RESOLUTION_BADGE, true)
        _folderGridColumnsPortrait.value = prefs.getInt(KEY_FOLDER_GRID_COLS_PORTRAIT, 2).coerceIn(1, 4)
        _folderGridColumnsLandscape.value = prefs.getInt(KEY_FOLDER_GRID_COLS_LANDSCAPE, 4).coerceIn(2, 6)
        _videoGridColumnsPortrait.value = prefs.getInt(KEY_VIDEO_GRID_COLS_PORTRAIT, 2).coerceIn(1, 4)
        _videoGridColumnsLandscape.value = prefs.getInt(KEY_VIDEO_GRID_COLS_LANDSCAPE, 4).coerceIn(2, 6)
        _unlimitedNameLines.value = prefs.getBoolean(KEY_UNLIMITED_NAME_LINES, false)
        _showExtensionField.value = prefs.getBoolean(KEY_SHOW_EXTENSION, false)
        _showDurationField.value = prefs.getBoolean(KEY_SHOW_DURATION, true)
        _showProgressBar.value = prefs.getBoolean(KEY_SHOW_PROGRESS_BAR, true)
        _centerGridTitles.value = prefs.getBoolean(KEY_CENTER_GRID_TITLES, true)
        _showTotalVideosChip.value = prefs.getBoolean(KEY_SHOW_TOTAL_VIDEOS, true)
        _showTotalDurationChip.value = prefs.getBoolean(KEY_SHOW_TOTAL_DURATION, false)
        _showFolderThumbnails.value = prefs.getBoolean(KEY_SHOW_FOLDER_THUMBNAILS, false)
        _showNewBadge.value = prefs.getBoolean(KEY_SHOW_NEW_BADGE, true)
        _newBadgeDays.value = prefs.getInt(KEY_NEW_BADGE_DAYS, 7).coerceIn(0, 30)
        _watchedThresholdPct.value = prefs.getInt(KEY_WATCHED_THRESHOLD_PCT, 95).coerceIn(0, 100)
        _showQuickPlayFab.value = prefs.getBoolean(KEY_SHOW_QUICK_PLAY_FAB, true)
        _quickPlayFabDirect.value = prefs.getBoolean(KEY_QUICK_PLAY_FAB_DIRECT, false)
        _dualPaneTablet.value = prefs.getBoolean(KEY_DUAL_PANE_TABLET, true)
        _vaultEntryHidden.value = prefs.getBoolean(KEY_VAULT_ENTRY_HIDDEN, false)
        _vaultBiometricEnabled.value = prefs.getBoolean(KEY_VAULT_BIOMETRIC_ENABLED, true)
        _vaultSecurityQuestion.value = prefs.getString(KEY_VAULT_SEC_QUESTION, "") ?: ""
        inMemoryVaultAnswerSalt = prefs.getString(KEY_VAULT_SEC_ANSWER_SALT, null)
        inMemoryVaultAnswerHash = prefs.getString(KEY_VAULT_SEC_ANSWER_HASH, null)

        _backgroundPlayback.value = prefs.getBoolean(KEY_BACKGROUND_PLAYBACK, false)
        _autoPiP.value = prefs.getBoolean(KEY_AUTO_PIP, true)

        _anime4k.value = prefs.getBoolean(KEY_ANIME4K, false)
        _anime4kMode.value = prefs.getString(KEY_ANIME4K_MODE, "OFF") ?: "OFF"
        _anime4kQuality.value = prefs.getString(KEY_ANIME4K_QUALITY, "FAST") ?: "FAST"
        _hdrTonemapping.value = prefs.getBoolean(KEY_HDR_TONEMAP, true)
        _mpvProfile.value = prefs.getString(KEY_MPV_PROFILE, "fast") ?: "fast"
        _useGpuNext.value = prefs.getBoolean(KEY_USE_GPU_NEXT, false)
        _useVulkan.value = prefs.getBoolean(KEY_USE_VULKAN, false)
        _useYuv420p.value = prefs.getBoolean(KEY_USE_YUV420P, false)
        _mpvDebanding.value = prefs.getString(KEY_MPV_DEBANDING, "NONE") ?: "NONE"
        _debandIterations.value = prefs.getInt(KEY_DEBAND_ITERATIONS, 1).coerceIn(1, 4)
        _debandThreshold.value = prefs.getInt(KEY_DEBAND_THRESHOLD, 48).coerceIn(0, 256)
        _debandRange.value = prefs.getInt(KEY_DEBAND_RANGE, 16).coerceIn(1, 64)
        _debandGrain.value = prefs.getInt(KEY_DEBAND_GRAIN, 32).coerceIn(0, 256)

        _manualGridColumnsEnabled.value = prefs.getBoolean(KEY_MANUAL_GRID_COLS, false)
        _separateFolderVideoLayout.value = prefs.getBoolean(KEY_SEPARATE_FOLDER_VIDEO_LAYOUT, false)
        _networkThumbnailsEnabled.value = prefs.getBoolean(KEY_NETWORK_THUMBNAILS, false)
        _thumbnailCacheSizeMb.value = prefs.getInt(KEY_THUMBNAIL_CACHE_MB, 128).coerceIn(32, 512)
        _showScreenRotationOverlay.value = prefs.getBoolean(KEY_SHOW_ROTATION_OVERLAY, true)
        _showAspectRatioOverlay.value = prefs.getBoolean(KEY_SHOW_ASPECT_OVERLAY, true)
        _showPlaybackSpeedOverlay.value = prefs.getBoolean(KEY_SHOW_SPEED_OVERLAY, true)
        _showDecoderOverlay.value = prefs.getBoolean(KEY_SHOW_DECODER_OVERLAY, true)
        _showShuffleOverlay.value = prefs.getBoolean(KEY_SHOW_SHUFFLE_OVERLAY, true)
        _showLoopOverlay.value = prefs.getBoolean(KEY_SHOW_LOOP_OVERLAY, true)
        _showPipOverlay.value = prefs.getBoolean(KEY_SHOW_PIP_OVERLAY, true)
        _showScreenshotOverlay.value = prefs.getBoolean(KEY_SHOW_SCREENSHOT_OVERLAY, true)
        _screenshotFormat.value = prefs.getString(KEY_SCREENSHOT_FORMAT, "PNG") ?: "PNG"
        _screenshotQuality.value = prefs.getInt(KEY_SCREENSHOT_QUALITY, 95).coerceIn(60, 100)
        _screenshotFolder.value = prefs.getString(KEY_SCREENSHOT_FOLDER, "MPVX Screenshots") ?: "MPVX Screenshots"
        _doubleTapPauseResume.value = prefs.getBoolean(KEY_DOUBLE_TAP_PAUSE_RESUME, true)
        _longPressSpeedVariable.value = prefs.getBoolean(KEY_LONG_PRESS_SPEED_VAR, false)

        inMemoryVaultSalt = prefs.getString(KEY_VAULT_PIN_SALT, null)
        inMemoryVaultHash = prefs.getString(KEY_VAULT_PIN_HASH, null)
        _hasCustomVaultPin.value = !inMemoryVaultHash.isNullOrBlank() && !inMemoryVaultSalt.isNullOrBlank()

        if (prefs.contains(KEY_CUSTOM_PRIMARY)) {
            _customPrimaryColor.value = prefs.getLong(KEY_CUSTOM_PRIMARY, 0L)
        } else {
            _customPrimaryColor.value = null
        }
        if (prefs.contains(KEY_CUSTOM_ACCENT)) {
            _customAccentColor.value = prefs.getLong(KEY_CUSTOM_ACCENT, 0L)
        } else {
            _customAccentColor.value = null
        }
        if (prefs.contains(KEY_CUSTOM_BACKGROUND)) {
            _customBackgroundColor.value = prefs.getLong(KEY_CUSTOM_BACKGROUND, 0L)
        } else {
            _customBackgroundColor.value = null
        }
        if (prefs.contains(KEY_CUSTOM_SURFACE)) {
            _customSurfaceColor.value = prefs.getLong(KEY_CUSTOM_SURFACE, 0L)
        } else {
            _customSurfaceColor.value = null
        }

        _seekIntervalSeconds.value = prefs.getInt(KEY_SEEK_INTERVAL, 10)
        _playerControlSize.value = prefs.getString(KEY_PLAYER_CONTROL_SIZE, "NORMAL") ?: "NORMAL"
        _seekbarStyle.value = prefs.getString(KEY_SEEKBAR_STYLE, "EXPRESSIVE") ?: "EXPRESSIVE"
        _showHudTelemetry.value = prefs.getBoolean(KEY_SHOW_HUD_TELEMETRY, false)
        val rawTopRight = prefs.getString(KEY_TOP_RIGHT_HUD_CONTROLS, DEFAULT_TOP_RIGHT_HUD) ?: DEFAULT_TOP_RIGHT_HUD
        _topRightHudControls.value = rawTopRight
            .split(",")
            .map { it.trim() }
            .filter { it.isNotEmpty() && !it.equals("CAST", ignoreCase = true) }
            .joinToString(",")
            .ifEmpty { DEFAULT_TOP_RIGHT_HUD }
        _bottomLeftHudControls.value = prefs.getString(KEY_BOTTOM_LEFT_HUD_CONTROLS, DEFAULT_BOTTOM_LEFT_HUD) ?: DEFAULT_BOTTOM_LEFT_HUD
        _bottomRightHudControls.value = prefs.getString(KEY_BOTTOM_RIGHT_HUD_CONTROLS, DEFAULT_BOTTOM_RIGHT_HUD) ?: DEFAULT_BOTTOM_RIGHT_HUD
        _autoPlayNext.value = prefs.getBoolean(KEY_AUTO_PLAY_NEXT, true)
        _rememberPosition.value = prefs.getBoolean(KEY_REMEMBER_POSITION, true)

        val rawOrder = prefs.getString(KEY_NAV_TAB_ORDER, ALL_NAV_TABS.joinToString(",")) ?: ALL_NAV_TABS.joinToString(",")
        val parsedOrder = rawOrder.split(",").map { it.trim() }.filter { it in ALL_NAV_TABS }.distinct()
        _navTabOrder.value = parsedOrder + ALL_NAV_TABS.filter { it !in parsedOrder }

        val rawEnabled = prefs.getString(KEY_NAV_ENABLED_TABS, ALL_NAV_TABS.joinToString(",")) ?: ALL_NAV_TABS.joinToString(",")
        val parsedEnabled = rawEnabled.split(",").map { it.trim() }.filter { it in ALL_NAV_TABS }.distinct()
        _enabledNavTabs.value = parsedEnabled.ifEmpty { listOf("HOME") }

        _decoderPriority.value = prefs.getString(KEY_DECODER_PRIORITY, "AUTO") ?: "AUTO"
        _videoSurfaceType.value = prefs.getString(KEY_VIDEO_SURFACE_TYPE, "SURFACE_VIEW") ?: "SURFACE_VIEW"
        _audioPitchCorrection.value = prefs.getBoolean(KEY_AUDIO_PITCH_CORRECTION, true)
        _playerRepeatMode.value = prefs.getString(KEY_PLAYER_REPEAT_MODE, "OFF") ?: "OFF"
        _pinchZoomEnabled.value = prefs.getBoolean(KEY_PINCH_ZOOM_ENABLED, true)
        _fastSeekKeyframes.value = prefs.getBoolean(KEY_FAST_SEEK_KEYFRAMES, true)
        _subtitleEdgeType.value = prefs.getString(KEY_SUBTITLE_EDGE_TYPE, "DROP_SHADOW") ?: "DROP_SHADOW"
        _subtitleBgOpacity.value = prefs.getFloat(KEY_SUBTITLE_BG_OPACITY, 0.5f).coerceIn(0f, 1f)
        _subtitleBottomPaddingDp.value = prefs.getInt(KEY_SUBTITLE_BOTTOM_PADDING, 24).coerceIn(0, 120)
        _subtitleBold.value = prefs.getBoolean(KEY_SUBTITLE_BOLD, false)
        _wifiOnlyNetwork.value = prefs.getBoolean(KEY_WIFI_ONLY_NETWORK, false)
        _networkTimeoutSec.value = prefs.getInt(KEY_NETWORK_TIMEOUT_SEC, 15).coerceIn(5, 60)

        _playerControlsTheme.value = prefs.getString(KEY_PLAYER_CONTROLS_THEME, "GLASS") ?: "GLASS"
        _hidePlayerButtonsBg.value = prefs.getBoolean(KEY_HIDE_PLAYER_BTN_BG, false)
        _forceDarkPlayerButtonsBg.value = prefs.getBoolean(KEY_FORCE_DARK_PLAYER_BTN_BG, false)
        _showSeekbarOuterContainer.value = prefs.getBoolean(KEY_SHOW_SEEKBAR_CONTAINER, true)
        _portraitControlsPosition.value = prefs.getString(KEY_PORTRAIT_CONTROLS_POS, "CENTER") ?: "CENTER"
        _playerClockFormat.value = prefs.getString(KEY_PLAYER_CLOCK_FORMAT, "SYSTEM") ?: "SYSTEM"
        _hudAutoHideMs.value = prefs.getInt(KEY_HUD_AUTO_HIDE_MS, 4000).coerceIn(1500, 15000)
        _showSystemStatusBar.value = prefs.getBoolean(KEY_SHOW_SYS_STATUS_BAR, false)
        _showSystemNavBar.value = prefs.getBoolean(KEY_SHOW_SYS_NAV_BAR, false)
        _safeAreaWindow.value = prefs.getBoolean(KEY_SAFE_AREA_WINDOW, false)
        _showLoadingCircle.value = prefs.getBoolean(KEY_SHOW_LOADING_CIRCLE, true)
        _portraitBottomHudControls.value = prefs.getString(KEY_PORTRAIT_BOTTOM_HUD, DEFAULT_PORTRAIT_BOTTOM_HUD) ?: DEFAULT_PORTRAIT_BOTTOM_HUD
        _doubleTapAreaWidthPct.value = prefs.getInt(KEY_DOUBLE_TAP_AREA_PCT, 28).coerceIn(10, 45)
        _doubleTapCenterPlayPause.value = prefs.getBoolean(KEY_DOUBLE_TAP_CENTER_PP, true)
        _useSingleTapToPause.value = prefs.getBoolean(KEY_SINGLE_TAP_PAUSE, false)
        _displayVolumeOnRight.value = prefs.getBoolean(KEY_DISPLAY_VOL_ON_RIGHT, true)
        _rememberBrightness.value = prefs.getBoolean(KEY_REMEMBER_BRIGHTNESS, false)
        _savedPlayerBrightness.value = prefs.getFloat(KEY_SAVED_BRIGHTNESS, 0.7f).coerceIn(0.01f, 1.0f)
        _holdSpeedMultiplier.value = prefs.getFloat(KEY_HOLD_SPEED_MULT, 2.0f).coerceIn(1.5f, 4.0f)
        _horizontalSwipeSensitivity.value = prefs.getFloat(KEY_H_SWIPE_SENSITIVITY, 1.0f).coerceIn(0.4f, 2.5f)
        _showGestureSeekOverlay.value = prefs.getBoolean(KEY_SHOW_GESTURE_SEEK_HUD, true)
        _showGestureVolBrightOverlay.value = prefs.getBoolean(KEY_SHOW_GESTURE_VB_HUD, true)
        _swipeUpOpensPlaylist.value = prefs.getBoolean(KEY_SWIPE_UP_PLAYLIST, true)
        _customSkipDurationSec.value = prefs.getInt(KEY_CUSTOM_SKIP_SEC, 90).coerceIn(5, 300)
        _ambientModeEnabled.value = prefs.getBoolean(KEY_AMBIENT_MODE_ENABLED, false)
        _audioChannelMode.value = prefs.getString(KEY_AUDIO_CHANNEL_MODE, "AUTO") ?: "AUTO"
        _eqCustomBandsDb.value = prefs.getString(KEY_EQ_CUSTOM_BANDS, "0.0,0.0,0.0,0.0,0.0") ?: "0.0,0.0,0.0,0.0,0.0"
        _subtitleSpeedMilli.value = prefs.getInt(KEY_SUB_SPEED_MILLI, 1000).coerceIn(100, 10000)
        _subtitleItalic.value = prefs.getBoolean(KEY_SUB_ITALIC, false)
        _subtitleAlignment.value = prefs.getString(KEY_SUB_ALIGNMENT, "CENTER") ?: "CENTER"
        _subtitleCustomFontUri.value = prefs.getString(KEY_SUB_CUSTOM_FONT_URI, "") ?: ""
        _subtitleCustomFontName.value = prefs.getString(KEY_SUB_CUSTOM_FONT_NAME, "System Default") ?: "System Default"
        _secondarySubtitleColor.value = prefs.getLong(KEY_SECONDARY_SUB_COLOR, 0xFFFDE047)
        _playerShuffleMode.value = prefs.getBoolean(KEY_PLAYER_SHUFFLE, false)

        val rawMusicTabs = prefs.getString(KEY_MUSIC_TAB_ORDER, ALL_MUSIC_TABS.joinToString(",")) ?: ALL_MUSIC_TABS.joinToString(",")
        val parsedMusicTabs = rawMusicTabs.split(",").map { it.trim().uppercase() }.filter { it in ALL_MUSIC_TABS }.distinct()
        _musicTabOrder.value = parsedMusicTabs + ALL_MUSIC_TABS.filter { it !in parsedMusicTabs }
        _musicHiddenTabs.value = prefs.getStringSet(KEY_MUSIC_HIDDEN_TABS, emptySet())?.toSet() ?: emptySet()
        _musicSortBy.value = prefs.getString(KEY_MUSIC_SORT_BY, "TITLE") ?: "TITLE"
        _musicSortAscending.value = prefs.getBoolean(KEY_MUSIC_SORT_ASC, true)
        _musicGridView.value = prefs.getBoolean(KEY_MUSIC_GRID_VIEW, false)
        _musicArtSizeDp.value = prefs.getInt(KEY_MUSIC_ART_SIZE_DP, 56).coerceIn(56, 126)
        _musicPlaylists.value = deserializePlaylists(prefs.getString(KEY_MUSIC_PLAYLISTS_JSON, "") ?: "")
        _recentsFilter.value = prefs.getString(KEY_RECENTS_FILTER, "ALL") ?: "ALL"
        _recentsGridView.value = prefs.getBoolean(KEY_RECENTS_GRID_VIEW, false)
        _networkBookmarks.value = deserializeNetworkBookmarks(prefs.getString(KEY_NETWORK_BOOKMARKS_JSON, "") ?: "")
        _networkSortBy.value = prefs.getString(KEY_NETWORK_SORT_BY, "FOLDERS_FIRST") ?: "FOLDERS_FIRST"
        _networkGridView.value = prefs.getBoolean(KEY_NETWORK_GRID_VIEW, false)
        _useSystemFont.value = prefs.getBoolean(KEY_USE_SYSTEM_FONT, false)
        val rawSearchHist = prefs.getString(KEY_SETTINGS_SEARCH_HISTORY, "") ?: ""
        _settingsSearchHistory.value = rawSearchHist.split("\n").map { it.trim() }.filter { it.isNotEmpty() }.distinct().take(8)
        _rememberAspectPerVideo.value = prefs.getBoolean(KEY_REMEMBER_ASPECT_PER_VIDEO, true)
        _closeAfterEndOfPlayback.value = prefs.getBoolean(KEY_CLOSE_AFTER_END, false)
        _keepScreenOnWhenPaused.value = prefs.getBoolean(KEY_KEEP_SCREEN_ON_PAUSED, false)
        _autoplayAfterScreenUnlock.value = prefs.getBoolean(KEY_AUTOPLAY_AFTER_UNLOCK, true)
        _pipOnHomeGestureOnly.value = prefs.getBoolean(KEY_PIP_ON_HOME_ONLY, false)
        _videoMiniPlayerEnabled.value = prefs.getBoolean(KEY_VIDEO_MINI_PLAYER, false)
        _controlsDrawerEnabled.value = prefs.getBoolean(KEY_CONTROLS_DRAWER_ENABLED, true)
        _reducePlayerAnimation.value = prefs.getBoolean(KEY_REDUCE_PLAYER_ANIMATION, false)
        _allowGesturesInPanels.value = prefs.getBoolean(KEY_ALLOW_GESTURES_IN_PANELS, false)
        _showSeekRipple.value = prefs.getBoolean(KEY_SHOW_SEEK_RIPPLE, true)
        _showSeekTimeInHud.value = prefs.getBoolean(KEY_SHOW_SEEK_TIME_HUD, true)
        _showBufferedRangeOnSeekbar.value = prefs.getBoolean(KEY_SHOW_BUFFERED_SEEKBAR, true)
        _showChaptersOnSeekbar.value = prefs.getBoolean(KEY_SHOW_CHAPTERS_SEEKBAR, true)
        _doubleTapLeftAction.value = prefs.getString(KEY_DOUBLE_TAP_LEFT_ACTION, "SEEK") ?: "SEEK"
        _doubleTapCenterAction.value = prefs.getString(KEY_DOUBLE_TAP_CENTER_ACTION, "PLAY_PAUSE") ?: "PLAY_PAUSE"
        _doubleTapRightAction.value = prefs.getString(KEY_DOUBLE_TAP_RIGHT_ACTION, "SEEK") ?: "SEEK"
        _showHoldSpeedOverlay.value = prefs.getBoolean(KEY_SHOW_HOLD_SPEED_OVERLAY, true)
        _showAspectFeedbackOverlay.value = prefs.getBoolean(KEY_SHOW_ASPECT_FEEDBACK_OVERLAY, true)
        _showZoomFeedbackOverlay.value = prefs.getBoolean(KEY_SHOW_ZOOM_FEEDBACK_OVERLAY, true)
        _showRepeatShuffleOverlay.value = prefs.getBoolean(KEY_SHOW_REPEAT_SHUFFLE_OVERLAY, true)
        _showActionFeedbackPills.value = prefs.getBoolean(KEY_SHOW_ACTION_FEEDBACK_PILLS, true)
        _includeSubtitlesInScreenshot.value = prefs.getBoolean(KEY_INCLUDE_SUBS_SCREENSHOT, true)
        _pngCompressionLevel.value = prefs.getInt(KEY_PNG_COMPRESSION_LEVEL, 6).coerceIn(0, 9)
        _screenshotFilenameTemplate.value = prefs.getString(KEY_SCREENSHOT_TEMPLATE, "MPVX_{title}_{time}") ?: "MPVX_{title}_{time}"
        _subOverrideAssMode.value = prefs.getString(KEY_SUB_OVERRIDE_ASS, "SCALE") ?: "SCALE"
        _subScaleByWindow.value = prefs.getBoolean(KEY_SUB_SCALE_BY_WINDOW, true)
        _subFontsDirUri.value = prefs.getString(KEY_SUB_FONTS_DIR_URI, "") ?: ""
        _subFontsDirLabel.value = prefs.getString(KEY_SUB_FONTS_DIR_LABEL, "App Default") ?: "App Default"
        _holdDragMovesSubtitles.value = prefs.getBoolean(KEY_HOLD_DRAG_MOVES_SUBS, false)
        _swipeSubtitlesToSeekDialog.value = prefs.getBoolean(KEY_SWIPE_SUBS_SEEK_DIALOG, false)
        _invertSwipeSubtitlesDirection.value = prefs.getBoolean(KEY_INVERT_SWIPE_SUBS_DIR, false)
        _includeAudioFilesInVideoLib.value = prefs.getBoolean(KEY_INCLUDE_AUDIO_IN_VIDEO_LIB, false)
        _minAudioDurationSec.value = prefs.getInt(KEY_MIN_AUDIO_DURATION_SEC, 10).coerceIn(0, 300)
        _albumPaletteBackground.value = prefs.getBoolean(KEY_ALBUM_PALETTE_BG, true)
        _volumeNormalization.value = prefs.getBoolean(KEY_VOLUME_NORMALIZATION, false)
        _volumeBoostCapPct.value = prefs.getInt(KEY_VOLUME_BOOST_CAP, 200).coerceIn(100, 200)
        _extraSmartCollections.value = prefs.getBoolean(KEY_EXTRA_SMART_COLLECTIONS, false)
        _extraWatchStats.value = prefs.getBoolean(KEY_EXTRA_WATCH_STATS, false)
        _extraPlaybackDiagnostics.value = prefs.getBoolean(KEY_EXTRA_PLAYBACK_DIAG, false)
        _extraLibraryHealth.value = prefs.getBoolean(KEY_EXTRA_LIBRARY_HEALTH, false)
        _extraQualityFilters.value = prefs.getBoolean(KEY_EXTRA_QUALITY_FILTERS, false)
        _extraActivityLog.value = prefs.getBoolean(KEY_EXTRA_ACTIVITY_LOG, false)

        _mpvConfEnabled.value = prefs.getBoolean(KEY_MPV_CONF_ENABLED, false)
        _mpvConfText.value = prefs.getString(KEY_MPV_CONF_TEXT, "") ?: ""
        _showHiddenMediaFolders.value = prefs.getBoolean(KEY_SHOW_HIDDEN_FOLDERS, false)
        _safMediaTreeUris.value = prefs.getStringSet(KEY_SAF_MEDIA_TREES, emptySet())?.toSet() ?: emptySet()
        _introOutroEnabled.value = prefs.getBoolean(KEY_INTRO_OUTRO_ENABLED, true)
        _introProvider.value = prefs.getString(KEY_INTRO_PROVIDER, "HYBRID") ?: "HYBRID"
        _detectIntroFromChapters.value = prefs.getBoolean(KEY_DETECT_INTRO_CHAPTERS, true)
        _autoSkipIntro.value = prefs.getBoolean(KEY_AUTO_SKIP_INTRO, false)
        _autoSkipOutro.value = prefs.getBoolean(KEY_AUTO_SKIP_OUTRO, false)
        _customIntroKeywords.value = prefs.getString(KEY_CUSTOM_INTRO_KEYWORDS, "") ?: ""
        _customOutroKeywords.value = prefs.getString(KEY_CUSTOM_OUTRO_KEYWORDS, "") ?: ""
        _showProviderStatusOverlay.value = prefs.getBoolean(KEY_SHOW_PROVIDER_STATUS_OV, false)
        _onlineSubSearchMode.value = prefs.getString(KEY_ONLINE_SUB_MODE, "HYBRID") ?: "HYBRID"
        _onlineSubPreferredLang.value = prefs.getString(KEY_ONLINE_SUB_LANG, "en") ?: "en"
        _audioStandbyDelaySec.value = prefs.getInt(KEY_AUDIO_STANDBY_SEC, 0).coerceIn(0, 1800)
        _musicVisualizerStyle.value = prefs.getString(KEY_MUSIC_VISUALIZER_STYLE, "WAVE") ?: "WAVE"
        _musicPlayerOrientation.value = prefs.getString(KEY_MUSIC_ORIENTATION, "AUTO") ?: "AUTO"
        _mediaServers.value = deserializeMediaServers(prefs.getString(KEY_MEDIA_SERVERS_JSON, "") ?: "")
        _enabledServerTabs.value = prefs.getStringSet(KEY_ENABLED_SERVER_TABS, emptySet())?.toSet() ?: emptySet()
        _mediaQualityFilter.value = prefs.getString(KEY_MEDIA_QUALITY_FILTER, "ALL") ?: "ALL"
        _onlyShowFoldersInHome.value = prefs.getBoolean(KEY_ONLY_SHOW_FOLDERS, false)
        _homeCompactGrid.value = prefs.getBoolean(KEY_HOME_COMPACT_GRID, false)

        _excludedFolders.value = prefs.getStringSet(KEY_EXCLUDED_FOLDERS, emptySet())?.toSet() ?: emptySet()
        _favoriteVideoUris.value = prefs.getStringSet(KEY_FAVORITE_VIDEOS, emptySet())?.toSet() ?: emptySet()
        _pinnedFolderIds.value = prefs.getStringSet(KEY_PINNED_FOLDERS, emptySet())?.toSet() ?: emptySet()
        _vaultVideoUris.value = prefs.getStringSet(KEY_VAULT_VIDEOS, emptySet())?.toSet() ?: emptySet()
        _userPlaylists.value = deserializePlaylists(prefs.getString(KEY_USER_PLAYLISTS_JSON, "") ?: "")
        _savedStreams.value = deserializeStreams(prefs.getString(KEY_SAVED_STREAMS_JSON, "") ?: "")
        _networkShares.value = deserializeShares(prefs.getString(KEY_NETWORK_SHARES_JSON, "") ?: "")
    }

    // Setters with persistence
    fun setDarkTheme(enabled: Boolean) {
        _isDarkTheme.value = enabled
        val mode = if (enabled) "DARK" else "LIGHT"
        _darkMode.value = mode
        if (::prefs.isInitialized) {
            prefs.edit()
                .putBoolean(KEY_DARK_THEME, enabled)
                .putString(KEY_DARK_MODE, mode)
                .apply()
        }
    }

    fun setDarkMode(mode: String) {
        val norm = when (mode.trim().uppercase()) {
            "DARK" -> "DARK"
            "LIGHT" -> "LIGHT"
            else -> "SYSTEM"
        }
        _darkMode.value = norm
        if (norm == "DARK") _isDarkTheme.value = true
        else if (norm == "LIGHT") _isDarkTheme.value = false
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_DARK_MODE, norm)
                .apply {
                    if (norm != "SYSTEM") putBoolean(KEY_DARK_THEME, norm == "DARK")
                }
                .apply()
        }
    }

    fun setThemeTransitionOrigin(origin: Pair<Float, Float>?) {
        _themeTransitionOrigin.value = origin
    }

    fun toggleFavoriteThemePreset(presetName: String) {
        val cur = _favoriteThemePresets.value
        val next = if (presetName in cur) cur - presetName else cur + presetName
        _favoriteThemePresets.value = next
        if (::prefs.isInitialized) prefs.edit().putStringSet(KEY_FAVORITE_THEMES, next).apply()
    }

    fun setCustomThemeWallpaper(uri: String, isVideo: Boolean) {
        _customThemeWallpaperUri.value = uri
        _customThemeWallpaperIsVideo.value = isVideo
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_CUSTOM_WALLPAPER_URI, uri)
                .putBoolean(KEY_CUSTOM_WALLPAPER_IS_VIDEO, isVideo)
                .apply()
        }
    }

    fun setCustomThemeWallpaperBlur(blur: Float) {
        val v = blur.coerceIn(0f, 40f)
        _customThemeWallpaperBlur.value = v
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_CUSTOM_WALLPAPER_BLUR, v).apply()
    }

    fun setCustomThemeWallpaperBrightness(brightness: Float) {
        val v = brightness.coerceIn(0.2f, 1.6f)
        _customThemeWallpaperBrightness.value = v
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_CUSTOM_WALLPAPER_BRIGHTNESS, v).apply()
    }

    fun setCustomThemeWallpaperSaturation(saturation: Float) {
        val v = saturation.coerceIn(0f, 2.0f)
        _customThemeWallpaperSaturation.value = v
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_CUSTOM_WALLPAPER_SATURATION, v).apply()
    }

    fun setCustomThemeWallpaperScrimAlpha(alpha: Float) {
        val v = alpha.coerceIn(0f, 0.95f)
        _customThemeWallpaperScrimAlpha.value = v
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_CUSTOM_WALLPAPER_SCRIM, v).apply()
    }

    fun setCustomThemeSurfaceOpacity(opacity: Float) {
        val v = opacity.coerceIn(0.2f, 1.0f)
        _customThemeSurfaceOpacity.value = v
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_CUSTOM_SURFACE_OPACITY, v).apply()
    }

    fun setThemePreset(preset: ThemePreset) {
        _themePreset.value = preset
        if (::prefs.isInitialized) prefs.edit().putString(KEY_THEME_PRESET, preset.name).apply()
    }

    fun setAmoled(enabled: Boolean) {
        _isAmoled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_AMOLED, enabled).apply()
    }

    fun setFrostedPlayerSheets(enabled: Boolean) {
        _isFrostedPlayerSheets.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_FROSTED_SHEETS, enabled).apply()
    }

    fun setLiquidGlassEnabled(enabled: Boolean) {
        _isLiquidGlassEnabled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_LIQUID_GLASS, enabled).apply()
    }

    fun setGlassBlurRadius(radius: Float) {
        _glassBlurRadius.value = radius
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_GLASS_BLUR, radius).apply()
    }

    fun setGlassFrostOpacity(opacity: Float) {
        _glassFrostOpacity.value = opacity
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_GLASS_OPACITY, opacity).apply()
    }

    fun set3dControlsEnabled(enabled: Boolean) {
        _is3dControlsEnabled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_3D_CONTROLS, enabled).apply()
    }

    fun setCustomThemeColor(colorLong: Long) {
        _customThemeColor.value = colorLong
        if (::prefs.isInitialized) prefs.edit().putLong(KEY_CUSTOM_THEME_COLOR, colorLong).apply()
    }

    fun setSeekbarStyle(style: String) {
        val normalized = when (style.trim().uppercase()) {
            "NORMAL", "STANDARD", "WAVY", "THICK", "SLIM", "EXPRESSIVE" -> style.trim().uppercase()
            else -> "EXPRESSIVE"
        }
        _seekbarStyle.value = normalized
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SEEKBAR_STYLE, normalized).apply()
    }

    fun setShowHudTelemetry(enabled: Boolean) {
        _showHudTelemetry.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_HUD_TELEMETRY, enabled).apply()
    }

    fun setTopRightHudControls(controls: String) {
        _topRightHudControls.value = controls
        if (::prefs.isInitialized) prefs.edit().putString(KEY_TOP_RIGHT_HUD_CONTROLS, controls).apply()
    }

    fun setBottomLeftHudControls(controls: String) {
        _bottomLeftHudControls.value = controls
        if (::prefs.isInitialized) prefs.edit().putString(KEY_BOTTOM_LEFT_HUD_CONTROLS, controls).apply()
    }

    fun setBottomRightHudControls(controls: String) {
        _bottomRightHudControls.value = controls
        if (::prefs.isInitialized) prefs.edit().putString(KEY_BOTTOM_RIGHT_HUD_CONTROLS, controls).apply()
    }

    fun setPortraitBottomHudControls(controls: String) {
        _portraitBottomHudControls.value = controls
        if (::prefs.isInitialized) prefs.edit().putString(KEY_PORTRAIT_BOTTOM_HUD, controls).apply()
    }

    /**
     * Persists a reordered or modified control list for [slot], enforcing cross-slot exclusivity across
     * the 3 landscape slots (`TOP_RIGHT`, `BOTTOM_LEFT`, `BOTTOM_RIGHT`) when [enforceLandscapeExclusivity] is true.
     */
    fun assignHudSlotControls(
        slot: String,
        tokens: List<String>,
        enforceLandscapeExclusivity: Boolean = true
    ) {
        val clean = tokens.map { com.cmstudio.player.ui.player.PlayerControlCatalog.canonicalToken(it) }
            .filter { it.isNotEmpty() }
            .distinct()
        val csv = clean.joinToString(",")
        when (slot) {
            "TOP_RIGHT" -> setTopRightHudControls(csv)
            "BOTTOM_LEFT" -> setBottomLeftHudControls(csv)
            "BOTTOM_RIGHT" -> setBottomRightHudControls(csv)
            else -> {
                setPortraitBottomHudControls(csv)
                return
            }
        }
        if (enforceLandscapeExclusivity && clean.isNotEmpty()) {
            val addedSet = clean.filter { it != "NONE" }.toSet()
            if (addedSet.isNotEmpty()) {
                val otherSlots = listOf("TOP_RIGHT", "BOTTOM_LEFT", "BOTTOM_RIGHT").filter { it != slot }
                otherSlots.forEach { other ->
                    val cur = when (other) {
                        "TOP_RIGHT" -> _topRightHudControls.value
                        "BOTTOM_LEFT" -> _bottomLeftHudControls.value
                        else -> _bottomRightHudControls.value
                    }
                    val curTokens = cur.split(",")
                        .map { com.cmstudio.player.ui.player.PlayerControlCatalog.canonicalToken(it) }
                        .filter { it.isNotEmpty() }
                    val filtered = curTokens.filter { it !in addedSet }
                    if (filtered.size != curTokens.size) {
                        when (other) {
                            "TOP_RIGHT" -> setTopRightHudControls(filtered.joinToString(","))
                            "BOTTOM_LEFT" -> setBottomLeftHudControls(filtered.joinToString(","))
                            else -> setBottomRightHudControls(filtered.joinToString(","))
                        }
                    }
                }
            }
        }
    }

    fun moveHudControlToken(slot: String, token: String, direction: Int) {
        val current = when (slot) {
            "TOP_RIGHT" -> _topRightHudControls.value
            "BOTTOM_LEFT" -> _bottomLeftHudControls.value
            "BOTTOM_RIGHT" -> _bottomRightHudControls.value
            else -> _portraitBottomHudControls.value
        }
        val updated = com.cmstudio.player.ui.player.PlayerControlCatalog.moveTokenInCsv(current, token, direction)
        when (slot) {
            "TOP_RIGHT" -> setTopRightHudControls(updated)
            "BOTTOM_LEFT" -> setBottomLeftHudControls(updated)
            "BOTTOM_RIGHT" -> setBottomRightHudControls(updated)
            else -> setPortraitBottomHudControls(updated)
        }
    }

    fun toggleHudControlToken(slot: String, token: String) {
        val current = when (slot) {
            "TOP_RIGHT" -> _topRightHudControls.value
            "BOTTOM_LEFT" -> _bottomLeftHudControls.value
            "BOTTOM_RIGHT" -> _bottomRightHudControls.value
            else -> _portraitBottomHudControls.value
        }
        val updated = com.cmstudio.player.ui.player.PlayerControlCatalog.toggleTokenInCsv(current, token)
        when (slot) {
            "TOP_RIGHT" -> setTopRightHudControls(updated)
            "BOTTOM_LEFT" -> setBottomLeftHudControls(updated)
            "BOTTOM_RIGHT" -> setBottomRightHudControls(updated)
            else -> setPortraitBottomHudControls(updated)
        }
    }

    fun resetHudLayoutToDefault() {
        setTopRightHudControls(DEFAULT_TOP_RIGHT_HUD)
        setBottomLeftHudControls(DEFAULT_BOTTOM_LEFT_HUD)
        setBottomRightHudControls(DEFAULT_BOTTOM_RIGHT_HUD)
        setPortraitBottomHudControls(DEFAULT_PORTRAIT_BOTTOM_HUD)
        setSeekbarStyle("EXPRESSIVE")
        setShowHudTelemetry(false)
        setPlayerControlsTheme("GLASS")
        setHidePlayerButtonsBg(false)
        setForceDarkPlayerButtonsBg(false)
        setShowSeekbarOuterContainer(true)
        setPortraitControlsPosition("CENTER")
        setPlayerClockFormat("SYSTEM")
        setHudAutoHideMs(4000)
        setShowSystemStatusBar(false)
        setShowSystemNavBar(false)
        setSafeAreaWindow(false)
        setShowLoadingCircle(true)
    }

    fun setNavTabEnabled(tabId: String, enabled: Boolean) {
        if (tabId !in ALL_NAV_TABS) return
        val current = _enabledNavTabs.value.toMutableList()
        if (enabled && tabId !in current) {
            current.add(tabId)
        } else if (!enabled) {
            // Keep at least one enabled tab so navigation never becomes completely empty
            if (current.size <= 1 && tabId in current) return
            current.remove(tabId)
        }
        val ordered = _navTabOrder.value.filter { it in current }.ifEmpty { listOf(tabId) }
        _enabledNavTabs.value = ordered
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_NAV_ENABLED_TABS, ordered.joinToString(",")).apply()
        }
    }

    fun setNavTabOrder(order: List<String>) {
        val valid = order.filter { it in ALL_NAV_TABS }.distinct()
        val fullOrder = valid + ALL_NAV_TABS.filter { it !in valid }
        _navTabOrder.value = fullOrder
        val enabledSet = _enabledNavTabs.value.toSet()
        val reorderedEnabled = fullOrder.filter { it in enabledSet }
        _enabledNavTabs.value = reorderedEnabled
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_NAV_TAB_ORDER, fullOrder.joinToString(","))
                .putString(KEY_NAV_ENABLED_TABS, reorderedEnabled.joinToString(","))
                .apply()
        }
    }

    fun moveNavTab(tabId: String, direction: Int) {
        val list = _navTabOrder.value.toMutableList()
        val index = list.indexOf(tabId)
        if (index < 0) return
        val target = (index + direction).coerceIn(0, list.lastIndex)
        if (target == index) return
        list.removeAt(index)
        list.add(target, tabId)
        setNavTabOrder(list)
    }

    fun resetNavigationPreferences() {
        _navTabOrder.value = ALL_NAV_TABS
        _enabledNavTabs.value = ALL_NAV_TABS
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_NAV_TAB_ORDER, ALL_NAV_TABS.joinToString(","))
                .putString(KEY_NAV_ENABLED_TABS, ALL_NAV_TABS.joinToString(","))
                .apply()
        }
    }

    fun setDecoderPriority(priority: String) {
        val normalized = when (priority) {
            "HW_ONLY", "SW_PREFER" -> priority
            else -> "AUTO"
        }
        _decoderPriority.value = normalized
        if (::prefs.isInitialized) prefs.edit().putString(KEY_DECODER_PRIORITY, normalized).apply()
    }

    fun setVideoSurfaceType(surfaceType: String) {
        val normalized = if (surfaceType == "TEXTURE_VIEW") "TEXTURE_VIEW" else "SURFACE_VIEW"
        _videoSurfaceType.value = normalized
        if (::prefs.isInitialized) prefs.edit().putString(KEY_VIDEO_SURFACE_TYPE, normalized).apply()
    }

    fun setAudioPitchCorrection(enabled: Boolean) {
        _audioPitchCorrection.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_AUDIO_PITCH_CORRECTION, enabled).apply()
    }

    fun setPlayerRepeatMode(mode: String) {
        val normalized = when (mode) {
            "ONE", "ALL" -> mode
            else -> "OFF"
        }
        _playerRepeatMode.value = normalized
        if (::prefs.isInitialized) prefs.edit().putString(KEY_PLAYER_REPEAT_MODE, normalized).apply()
    }

    fun setPinchZoomEnabled(enabled: Boolean) {
        _pinchZoomEnabled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_PINCH_ZOOM_ENABLED, enabled).apply()
    }

    fun setFastSeekKeyframes(enabled: Boolean) {
        _fastSeekKeyframes.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_FAST_SEEK_KEYFRAMES, enabled).apply()
    }

    fun setSubtitleEdgeType(edgeType: String) {
        _subtitleEdgeType.value = edgeType
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SUBTITLE_EDGE_TYPE, edgeType).apply()
    }

    fun setSubtitleBgOpacity(opacity: Float) {
        val clamped = opacity.coerceIn(0f, 1f)
        _subtitleBgOpacity.value = clamped
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_SUBTITLE_BG_OPACITY, clamped).apply()
    }

    fun setSubtitleBottomPaddingDp(dp: Int) {
        val clamped = dp.coerceIn(0, 120)
        _subtitleBottomPaddingDp.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_SUBTITLE_BOTTOM_PADDING, clamped).apply()
    }

    fun setSubtitleBold(bold: Boolean) {
        _subtitleBold.value = bold
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SUBTITLE_BOLD, bold).apply()
    }

    fun setPlayerControlsTheme(theme: String) {
        val normalized = when (theme.trim().uppercase()) {
            "CLASSIC", "GLASS", "GLOSSY" -> theme.trim().uppercase()
            else -> "GLASS"
        }
        _playerControlsTheme.value = normalized
        if (::prefs.isInitialized) prefs.edit().putString(KEY_PLAYER_CONTROLS_THEME, normalized).apply()
    }

    fun setHidePlayerButtonsBg(hide: Boolean) {
        _hidePlayerButtonsBg.value = hide
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_HIDE_PLAYER_BTN_BG, hide).apply()
    }

    fun setForceDarkPlayerButtonsBg(force: Boolean) {
        _forceDarkPlayerButtonsBg.value = force
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_FORCE_DARK_PLAYER_BTN_BG, force).apply()
    }

    fun setShowSeekbarOuterContainer(show: Boolean) {
        _showSeekbarOuterContainer.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_SEEKBAR_CONTAINER, show).apply()
    }

    fun setPortraitControlsPosition(pos: String) {
        val normalized = if (pos.trim().uppercase() == "CENTER") "CENTER" else "BELOW_SEEKBAR"
        _portraitControlsPosition.value = normalized
        if (::prefs.isInitialized) prefs.edit().putString(KEY_PORTRAIT_CONTROLS_POS, normalized).apply()
    }

    fun setPlayerClockFormat(fmt: String) {
        val normalized = when (fmt.trim().uppercase()) {
            "12H", "24H" -> fmt.trim().uppercase()
            else -> "SYSTEM"
        }
        _playerClockFormat.value = normalized
        if (::prefs.isInitialized) prefs.edit().putString(KEY_PLAYER_CLOCK_FORMAT, normalized).apply()
    }

    fun setHudAutoHideMs(ms: Int) {
        val clamped = ms.coerceIn(1500, 15000)
        _hudAutoHideMs.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_HUD_AUTO_HIDE_MS, clamped).apply()
    }

    fun setShowSystemStatusBar(show: Boolean) {
        _showSystemStatusBar.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_SYS_STATUS_BAR, show).apply()
    }

    fun setShowSystemNavBar(show: Boolean) {
        _showSystemNavBar.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_SYS_NAV_BAR, show).apply()
    }

    fun setSafeAreaWindow(safe: Boolean) {
        _safeAreaWindow.value = safe
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SAFE_AREA_WINDOW, safe).apply()
    }

    fun setShowLoadingCircle(show: Boolean) {
        _showLoadingCircle.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_LOADING_CIRCLE, show).apply()
    }

    fun setDoubleTapAreaWidthPct(pct: Int) {
        val clamped = pct.coerceIn(10, 45)
        _doubleTapAreaWidthPct.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_DOUBLE_TAP_AREA_PCT, clamped).apply()
    }

    fun setDoubleTapCenterPlayPause(enabled: Boolean) {
        _doubleTapCenterPlayPause.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_DOUBLE_TAP_CENTER_PP, enabled).apply()
    }

    fun setUseSingleTapToPause(enabled: Boolean) {
        _useSingleTapToPause.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SINGLE_TAP_PAUSE, enabled).apply()
    }

    fun setDisplayVolumeOnRight(onRight: Boolean) {
        _displayVolumeOnRight.value = onRight
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_DISPLAY_VOL_ON_RIGHT, onRight).apply()
    }

    fun setRememberBrightness(remember: Boolean) {
        _rememberBrightness.value = remember
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_REMEMBER_BRIGHTNESS, remember).apply()
    }

    fun setSavedPlayerBrightness(brightness: Float) {
        val clamped = brightness.coerceIn(0.01f, 1.0f)
        _savedPlayerBrightness.value = clamped
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_SAVED_BRIGHTNESS, clamped).apply()
    }

    fun setHoldSpeedMultiplier(mult: Float) {
        val clamped = mult.coerceIn(1.5f, 4.0f)
        _holdSpeedMultiplier.value = clamped
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_HOLD_SPEED_MULT, clamped).apply()
    }

    fun setHorizontalSwipeSensitivity(sens: Float) {
        val clamped = sens.coerceIn(0.4f, 2.5f)
        _horizontalSwipeSensitivity.value = clamped
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_H_SWIPE_SENSITIVITY, clamped).apply()
    }

    fun setShowGestureSeekOverlay(show: Boolean) {
        _showGestureSeekOverlay.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_GESTURE_SEEK_HUD, show).apply()
    }

    fun setShowGestureVolBrightOverlay(show: Boolean) {
        _showGestureVolBrightOverlay.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_GESTURE_VB_HUD, show).apply()
    }

    fun setSwipeUpOpensPlaylist(enabled: Boolean) {
        _swipeUpOpensPlaylist.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SWIPE_UP_PLAYLIST, enabled).apply()
    }

    fun setCustomSkipDurationSec(sec: Int) {
        val clamped = sec.coerceIn(5, 300)
        _customSkipDurationSec.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_CUSTOM_SKIP_SEC, clamped).apply()
    }

    fun setAmbientModeEnabled(enabled: Boolean) {
        _ambientModeEnabled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_AMBIENT_MODE_ENABLED, enabled).apply()
    }

    fun setAudioChannelMode(mode: String) {
        val normalized = com.cmstudio.player.ui.player.ChannelMixMath.normalizeMode(mode)
        _audioChannelMode.value = normalized
        if (::prefs.isInitialized) prefs.edit().putString(KEY_AUDIO_CHANNEL_MODE, normalized).apply()
    }

    fun setEqCustomBandsDb(bandsCsv: String) {
        val clean = com.cmstudio.player.ui.player.EqualizerPresetsCatalog.serializeBands(
            com.cmstudio.player.ui.player.EqualizerPresetsCatalog.parseCustomBands(bandsCsv)
        )
        _eqCustomBandsDb.value = clean
        if (::prefs.isInitialized) prefs.edit().putString(KEY_EQ_CUSTOM_BANDS, clean).apply()
    }

    fun setSubtitleSpeedMilli(milli: Int) {
        val clamped = milli.coerceIn(100, 10000)
        _subtitleSpeedMilli.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_SUB_SPEED_MILLI, clamped).apply()
    }

    fun setSubtitleItalic(italic: Boolean) {
        _subtitleItalic.value = italic
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SUB_ITALIC, italic).apply()
    }

    fun setSubtitleAlignment(alignment: String) {
        val normalized = when (alignment.trim().uppercase()) {
            "LEFT", "RIGHT" -> alignment.trim().uppercase()
            else -> "CENTER"
        }
        _subtitleAlignment.value = normalized
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SUB_ALIGNMENT, normalized).apply()
    }

    fun setSubtitleCustomFont(uriString: String, fontName: String) {
        _subtitleCustomFontUri.value = uriString
        _subtitleCustomFontName.value = fontName.ifBlank { "System Default" }
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_SUB_CUSTOM_FONT_URI, uriString)
                .putString(KEY_SUB_CUSTOM_FONT_NAME, _subtitleCustomFontName.value)
                .apply()
        }
    }

    fun setSecondarySubtitleColor(argb: Long) {
        _secondarySubtitleColor.value = argb
        if (::prefs.isInitialized) prefs.edit().putLong(KEY_SECONDARY_SUB_COLOR, argb).apply()
    }

    fun setPlayerShuffleMode(shuffle: Boolean) {
        _playerShuffleMode.value = shuffle
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_PLAYER_SHUFFLE, shuffle).apply()
    }

    fun getVideoAspectOverride(videoKey: String): String? {
        if (!_rememberAspectPerVideo.value || !::prefs.isInitialized || videoKey.isBlank()) return null
        val json = prefs.getString(KEY_VIDEO_ASPECT_MAP_JSON, "") ?: ""
        if (json.isBlank()) return null
        return runCatching { JSONObject(json).optString(videoKey, "").takeIf { it.isNotBlank() } }.getOrNull()
    }

    fun setVideoAspectOverride(videoKey: String, mode: String?) {
        if (!_rememberAspectPerVideo.value || !::prefs.isInitialized || videoKey.isBlank()) return
        val json = prefs.getString(KEY_VIDEO_ASPECT_MAP_JSON, "") ?: ""
        val obj = runCatching { if (json.isBlank()) JSONObject() else JSONObject(json) }.getOrElse { JSONObject() }
        if (mode.isNullOrBlank()) obj.remove(videoKey) else obj.put(videoKey, mode)
        prefs.edit().putString(KEY_VIDEO_ASPECT_MAP_JSON, obj.toString()).apply()
    }

    fun getVideoEngineOverride(videoKey: String): PlaybackEngineMode? {
        if (!::prefs.isInitialized || videoKey.isBlank()) return null
        val json = prefs.getString(KEY_VIDEO_ENGINE_MAP_JSON, "") ?: ""
        if (json.isBlank()) return null
        val raw = runCatching { JSONObject(json).optString(videoKey, "") }.getOrDefault("")
        return if (raw.isBlank() || raw.equals("AUTO", ignoreCase = true)) null else PlaybackEngineMode.fromId(raw)
    }

    fun setVideoEngineOverride(videoKey: String, mode: PlaybackEngineMode?) {
        if (!::prefs.isInitialized || videoKey.isBlank()) return
        val json = prefs.getString(KEY_VIDEO_ENGINE_MAP_JSON, "") ?: ""
        val obj = runCatching { if (json.isBlank()) JSONObject() else JSONObject(json) }.getOrElse { JSONObject() }
        if (mode == null || mode == PlaybackEngineMode.AUTO) obj.remove(videoKey) else obj.put(videoKey, mode.id)
        prefs.edit().putString(KEY_VIDEO_ENGINE_MAP_JSON, obj.toString()).apply()
    }

    // Music, Playlist Reorder, Recents, Network Bookmarks, System Font & Search History Setters
    fun moveVideoInPlaylist(playlistId: String, index: Int, direction: Int) {
        val updated = _userPlaylists.value.map { pl ->
            if (pl.id != playlistId || index !in pl.itemUris.indices) pl
            else {
                val target = (index + direction).coerceIn(0, pl.itemUris.lastIndex)
                if (target == index) pl
                else {
                    val items = pl.itemUris.toMutableList()
                    val moved = items.removeAt(index)
                    items.add(target, moved)
                    pl.copy(itemUris = items)
                }
            }
        }
        _userPlaylists.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_USER_PLAYLISTS_JSON, serializePlaylists(updated)).apply()
    }

    fun toggleMusicTabVisibility(tabId: String) {
        if (tabId == "SONGS" || tabId !in ALL_MUSIC_TABS) return
        val current = _musicHiddenTabs.value.toMutableSet()
        if (tabId in current) current.remove(tabId) else current.add(tabId)
        _musicHiddenTabs.value = current
        if (::prefs.isInitialized) prefs.edit().putStringSet(KEY_MUSIC_HIDDEN_TABS, current).apply()
    }

    fun moveMusicTab(tabId: String, direction: Int) {
        val list = _musicTabOrder.value.toMutableList()
        val idx = list.indexOf(tabId)
        if (idx < 0) return
        val target = (idx + direction).coerceIn(0, list.lastIndex)
        if (target == idx) return
        list.removeAt(idx)
        list.add(target, tabId)
        _musicTabOrder.value = list
        if (::prefs.isInitialized) prefs.edit().putString(KEY_MUSIC_TAB_ORDER, list.joinToString(",")).apply()
    }

    fun setMusicSort(sortBy: String, ascending: Boolean) {
        _musicSortBy.value = sortBy
        _musicSortAscending.value = ascending
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_MUSIC_SORT_BY, sortBy).putBoolean(KEY_MUSIC_SORT_ASC, ascending).apply()
        }
    }

    fun setMusicGridView(grid: Boolean) {
        _musicGridView.value = grid
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_MUSIC_GRID_VIEW, grid).apply()
    }

    fun setMusicArtSizeDp(sizeDp: Int) {
        val clamped = sizeDp.coerceIn(56, 126)
        _musicArtSizeDp.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_MUSIC_ART_SIZE_DP, clamped).apply()
    }

    fun createMusicPlaylist(name: String): UserPlaylist {
        val clean = name.trim().ifBlank { "Music Mix" }
        val item = UserPlaylist(
            id = "mpl_${System.currentTimeMillis()}_${_musicPlaylists.value.size}",
            name = clean,
            itemUris = emptyList(),
            createdAtMs = System.currentTimeMillis()
        )
        val updated = _musicPlaylists.value + item
        _musicPlaylists.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_MUSIC_PLAYLISTS_JSON, serializePlaylists(updated)).apply()
        return item
    }

    fun addTrackToMusicPlaylist(playlistId: String, trackUri: String) {
        val updated = _musicPlaylists.value.map { pl ->
            if (pl.id == playlistId && trackUri !in pl.itemUris) pl.copy(itemUris = pl.itemUris + trackUri) else pl
        }
        _musicPlaylists.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_MUSIC_PLAYLISTS_JSON, serializePlaylists(updated)).apply()
    }

    fun removeTrackFromMusicPlaylist(playlistId: String, trackUri: String) {
        val updated = _musicPlaylists.value.map { pl ->
            if (pl.id == playlistId) pl.copy(itemUris = pl.itemUris.filterNot { it == trackUri }) else pl
        }
        _musicPlaylists.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_MUSIC_PLAYLISTS_JSON, serializePlaylists(updated)).apply()
    }

    fun deleteMusicPlaylist(playlistId: String) {
        val updated = _musicPlaylists.value.filterNot { it.id == playlistId }
        _musicPlaylists.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_MUSIC_PLAYLISTS_JSON, serializePlaylists(updated)).apply()
    }

    fun setRecentsFilter(filter: String) {
        val normalized = when (filter.trim().uppercase()) {
            "VIDEO", "AUDIO" -> filter.trim().uppercase()
            else -> "ALL"
        }
        _recentsFilter.value = normalized
        if (::prefs.isInitialized) prefs.edit().putString(KEY_RECENTS_FILTER, normalized).apply()
    }

    fun setRecentsGridView(grid: Boolean) {
        _recentsGridView.value = grid
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_RECENTS_GRID_VIEW, grid).apply()
    }

    fun addNetworkBookmark(shareId: String, title: String, url: String, isDirectory: Boolean) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty() || _networkBookmarks.value.any { it.url == cleanUrl }) return
        val bm = NetworkBookmark(
            id = "nbm_${System.currentTimeMillis()}",
            shareId = shareId,
            title = title.ifBlank { cleanUrl.substringAfterLast('/') },
            url = cleanUrl,
            isDirectory = isDirectory,
            createdAtMs = System.currentTimeMillis()
        )
        val updated = listOf(bm) + _networkBookmarks.value
        _networkBookmarks.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_NETWORK_BOOKMARKS_JSON, serializeNetworkBookmarks(updated)).apply()
    }

    fun removeNetworkBookmark(idOrUrl: String) {
        val updated = _networkBookmarks.value.filterNot { it.id == idOrUrl || it.url == idOrUrl }
        _networkBookmarks.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_NETWORK_BOOKMARKS_JSON, serializeNetworkBookmarks(updated)).apply()
    }

    fun setNetworkSortBy(sort: String) {
        val normalized = when (sort.trim().uppercase()) {
            "NAME", "TYPE" -> sort.trim().uppercase()
            else -> "FOLDERS_FIRST"
        }
        _networkSortBy.value = normalized
        if (::prefs.isInitialized) prefs.edit().putString(KEY_NETWORK_SORT_BY, normalized).apply()
    }

    fun setNetworkGridView(grid: Boolean) {
        _networkGridView.value = grid
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_NETWORK_GRID_VIEW, grid).apply()
    }

    fun setUseSystemFont(enabled: Boolean) {
        _useSystemFont.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_USE_SYSTEM_FONT, enabled).apply()
    }

    fun recordSettingsSearchQuery(query: String) {
        val clean = query.trim()
        if (clean.length < 2) return
        val updated = (listOf(clean) + _settingsSearchHistory.value.filterNot { it.equals(clean, ignoreCase = true) }).take(8)
        _settingsSearchHistory.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SETTINGS_SEARCH_HISTORY, updated.joinToString("\n")).apply()
    }

    fun clearSettingsSearchHistory() {
        _settingsSearchHistory.value = emptyList()
        if (::prefs.isInitialized) prefs.edit().remove(KEY_SETTINGS_SEARCH_HISTORY).apply()
    }

    private fun serializeNetworkBookmarks(list: List<NetworkBookmark>): String {
        val arr = JSONArray()
        list.forEach { b ->
            arr.put(JSONObject().apply {
                put("id", b.id)
                put("shareId", b.shareId)
                put("title", b.title)
                put("url", b.url)
                put("isDirectory", b.isDirectory)
                put("createdAtMs", b.createdAtMs)
            })
        }
        return arr.toString()
    }

    private fun deserializeNetworkBookmarks(json: String): List<NetworkBookmark> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    add(
                        NetworkBookmark(
                            id = o.getString("id"),
                            shareId = o.optString("shareId", ""),
                            title = o.optString("title", "Bookmark"),
                            url = o.getString("url"),
                            isDirectory = o.optBoolean("isDirectory", true),
                            createdAtMs = o.optLong("createdAtMs", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setWifiOnlyNetwork(enabled: Boolean) {
        _wifiOnlyNetwork.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_WIFI_ONLY_NETWORK, enabled).apply()
    }

    fun setNetworkTimeoutSec(seconds: Int) {
        val clamped = seconds.coerceIn(5, 60)
        _networkTimeoutSec.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_NETWORK_TIMEOUT_SEC, clamped).apply()
    }

    fun addExcludedFolder(folderPathOrId: String) {
        val trimmed = folderPathOrId.trim()
        if (trimmed.isEmpty()) return
        val updated = _excludedFolders.value + trimmed
        _excludedFolders.value = updated
        if (::prefs.isInitialized) prefs.edit().putStringSet(KEY_EXCLUDED_FOLDERS, updated).apply()
    }

    fun removeExcludedFolder(folderPathOrId: String) {
        val updated = _excludedFolders.value - folderPathOrId
        _excludedFolders.value = updated
        if (::prefs.isInitialized) prefs.edit().putStringSet(KEY_EXCLUDED_FOLDERS, updated).apply()
    }

    fun toggleFavoriteVideo(uriString: String) {
        val updated = if (uriString in _favoriteVideoUris.value) {
            _favoriteVideoUris.value - uriString
        } else {
            _favoriteVideoUris.value + uriString
        }
        _favoriteVideoUris.value = updated
        if (::prefs.isInitialized) prefs.edit().putStringSet(KEY_FAVORITE_VIDEOS, updated).apply()
    }

    fun togglePinnedFolder(folderId: String) {
        val updated = if (folderId in _pinnedFolderIds.value) {
            _pinnedFolderIds.value - folderId
        } else {
            _pinnedFolderIds.value + folderId
        }
        _pinnedFolderIds.value = updated
        if (::prefs.isInitialized) prefs.edit().putStringSet(KEY_PINNED_FOLDERS, updated).apply()
    }

    fun addVaultVideo(uriString: String) {
        val updated = _vaultVideoUris.value + uriString
        _vaultVideoUris.value = updated
        if (::prefs.isInitialized) prefs.edit().putStringSet(KEY_VAULT_VIDEOS, updated).apply()
    }

    fun removeVaultVideo(uriString: String) {
        val updated = _vaultVideoUris.value - uriString
        _vaultVideoUris.value = updated
        if (::prefs.isInitialized) prefs.edit().putStringSet(KEY_VAULT_VIDEOS, updated).apply()
    }

    fun createPlaylist(name: String, initialUri: String? = null): UserPlaylist {
        val cleanName = name.trim().ifEmpty { "Playlist ${_userPlaylists.value.size + 1}" }
        val created = UserPlaylist(
            id = "pl_${System.currentTimeMillis()}",
            name = cleanName,
            itemUris = listOfNotNull(initialUri),
            createdAtMs = System.currentTimeMillis()
        )
        val updated = _userPlaylists.value + created
        _userPlaylists.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_USER_PLAYLISTS_JSON, serializePlaylists(updated)).apply()
        return created
    }

    fun renamePlaylist(playlistId: String, newName: String) {
        val clean = newName.trim()
        if (clean.isEmpty()) return
        val updated = _userPlaylists.value.map { if (it.id == playlistId) it.copy(name = clean) else it }
        _userPlaylists.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_USER_PLAYLISTS_JSON, serializePlaylists(updated)).apply()
    }

    fun deletePlaylist(playlistId: String) {
        val updated = _userPlaylists.value.filterNot { it.id == playlistId }
        _userPlaylists.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_USER_PLAYLISTS_JSON, serializePlaylists(updated)).apply()
    }

    fun addVideoToPlaylist(playlistId: String, uriString: String) {
        val updated = _userPlaylists.value.map { pl ->
            if (pl.id == playlistId && uriString !in pl.itemUris) pl.copy(itemUris = pl.itemUris + uriString) else pl
        }
        _userPlaylists.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_USER_PLAYLISTS_JSON, serializePlaylists(updated)).apply()
    }

    fun removeVideoFromPlaylist(playlistId: String, uriString: String) {
        val updated = _userPlaylists.value.map { pl ->
            if (pl.id == playlistId) pl.copy(itemUris = pl.itemUris - uriString) else pl
        }
        _userPlaylists.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_USER_PLAYLISTS_JSON, serializePlaylists(updated)).apply()
    }

    fun addSavedStream(title: String, url: String, streamType: String? = null) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return
        val detectedType = streamType ?: when {
            cleanUrl.contains(".m3u8", ignoreCase = true) -> "HLS"
            cleanUrl.contains(".mpd", ignoreCase = true) -> "DASH"
            cleanUrl.startsWith("rtsp://", ignoreCase = true) -> "RTSP"
            else -> "Direct"
        }
        val cleanTitle = title.trim().ifEmpty { cleanUrl.substringAfterLast('/').substringBefore('?').ifEmpty { "Network Stream" } }
        val item = SavedStreamItem(
            id = "stream_${System.currentTimeMillis()}",
            title = cleanTitle,
            url = cleanUrl,
            streamType = detectedType
        )
        val updated = listOf(item) + _savedStreams.value.filterNot { it.url == cleanUrl }
        _savedStreams.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SAVED_STREAMS_JSON, serializeStreams(updated)).apply()
    }

    fun removeSavedStream(streamId: String) {
        val updated = _savedStreams.value.filterNot { it.id == streamId }
        _savedStreams.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SAVED_STREAMS_JSON, serializeStreams(updated)).apply()
    }

    fun saveNetworkShare(name: String, protocol: String, baseUrl: String, username: String, sessionPassword: String = "") {
        val cleanUrl = baseUrl.trim().trimEnd('/')
        if (cleanUrl.isEmpty()) return
        val share = NetworkShareConfig(
            id = "share_${System.currentTimeMillis()}",
            name = name.trim().ifEmpty { cleanUrl },
            protocol = protocol,
            baseUrl = cleanUrl,
            username = username.trim()
        )
        if (sessionPassword.isNotEmpty()) {
            setNetworkSharePassword(share.id, sessionPassword)
        }
        val updated = _networkShares.value + share
        _networkShares.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_NETWORK_SHARES_JSON, serializeShares(updated)).apply()
    }

    fun updateNetworkShare(shareId: String, name: String, protocol: String, baseUrl: String, username: String, newPassword: String?) {
        val cleanUrl = baseUrl.trim().trimEnd('/')
        if (cleanUrl.isEmpty()) return
        val updated = _networkShares.value.map { sh ->
            if (sh.id == shareId) {
                sh.copy(name = name.trim().ifEmpty { cleanUrl }, protocol = protocol, baseUrl = cleanUrl, username = username.trim())
            } else sh
        }
        _networkShares.value = updated
        if (newPassword != null) {
            setNetworkSharePassword(shareId, newPassword)
        }
        if (::prefs.isInitialized) prefs.edit().putString(KEY_NETWORK_SHARES_JSON, serializeShares(updated)).apply()
    }

    fun getNetworkSharePassword(shareId: String): String {
        val mem = sessionNetworkPasswords[shareId]
        if (!mem.isNullOrEmpty()) return mem
        return EncryptedCredentialStore.getSecret(appContext, "net_share_$shareId")
    }

    fun setNetworkSharePassword(shareId: String, password: String) {
        if (password.isEmpty()) {
            sessionNetworkPasswords.remove(shareId)
            EncryptedCredentialStore.removeSecret(appContext, "net_share_$shareId")
        } else {
            sessionNetworkPasswords[shareId] = password
            EncryptedCredentialStore.putSecret(appContext, "net_share_$shareId", password)
        }
    }

    fun removeNetworkShare(shareId: String) {
        sessionNetworkPasswords.remove(shareId)
        EncryptedCredentialStore.removeSecret(appContext, "net_share_$shareId")
        val updated = _networkShares.value.filterNot { it.id == shareId }
        _networkShares.value = updated
        if (::prefs.isInitialized) prefs.edit().putString(KEY_NETWORK_SHARES_JSON, serializeShares(updated)).apply()
    }

    fun resetAppearancePreferences() {
        setDarkTheme(true)
        setDarkMode("SYSTEM")
        setThemePreset(ThemePreset.MPVX_CYAN)
        setAmoled(false)
        setLiquidGlassEnabled(false)
        setFrostedPlayerSheets(true)
        setGlassBlurRadius(16f)
        setGlassFrostOpacity(0.78f)
        set3dControlsEnabled(false)
        setCustomPrimaryColor(null)
        setCustomAccentColor(null)
        setCustomBackgroundColor(null)
        setCustomSurfaceColor(null)
        setCustomThemeWallpaper("", false)
        setCustomThemeWallpaperBlur(0f)
        setCustomThemeWallpaperBrightness(1.0f)
        setCustomThemeWallpaperSaturation(1.0f)
        setCustomThemeWallpaperScrimAlpha(0.45f)
        setCustomThemeSurfaceOpacity(0.85f)
    }

    fun resetPlaybackAndPlayerPreferences() {
        setDefaultEngine("AUTO")
        setAutoPlayNext(true)
        setRememberPosition(true)
        setDefaultSpeed(1.0f)
        setDefaultAspectRatio("Fit")
        setSeekIntervalSeconds(10)
        setPlayerControlSize("NORMAL")
        setPlayerRepeatMode("OFF")
        setDecoderPriority("AUTO")
        setVideoSurfaceType("SURFACE_VIEW")
        setFastSeekKeyframes(true)
        setMpvProfile("fast")
        setUseGpuNext(false)
        setUseVulkan(false)
        setUseYuv420p(false)
        setMpvDebanding("NONE")
        setDebandIterations(1)
        setDebandThreshold(48)
        setDebandRange(16)
        setDebandGrain(32)
        setAnime4k(false)
        setAnime4kMode("OFF")
        setAnime4kQuality("FAST")
        setHdrTonemapping(true)
        setShowScreenRotationOverlay(true)
        setShowAspectRatioOverlay(true)
        setShowPlaybackSpeedOverlay(true)
        setShowDecoderOverlay(true)
        setShowShuffleOverlay(true)
        setShowLoopOverlay(true)
        setShowPipOverlay(true)
        setShowScreenshotOverlay(true)
        setScreenshotFormat("PNG")
        setScreenshotQuality(95)
        setScreenshotFolder("MPVX Screenshots")
        setRememberAspectPerVideo(true)
        setCloseAfterEndOfPlayback(false)
        setKeepScreenOnWhenPaused(false)
        setAutoplayAfterScreenUnlock(true)
        setPipOnHomeGestureOnly(false)
        setVideoMiniPlayerEnabled(false)
        setControlsDrawerEnabled(true)
        setReducePlayerAnimation(false)
        setAllowGesturesInPanels(false)
        setShowSeekRipple(true)
        setShowSeekTimeInHud(true)
        setShowBufferedRangeOnSeekbar(true)
        setShowChaptersOnSeekbar(true)
        setShowHoldSpeedOverlay(true)
        setShowAspectFeedbackOverlay(true)
        setShowZoomFeedbackOverlay(true)
        setShowRepeatShuffleOverlay(true)
        setShowActionFeedbackPills(true)
        setIncludeSubtitlesInScreenshot(true)
        setPngCompressionLevel(6)
        setScreenshotFilenameTemplate("MPVX_{title}_{time}")
        setIntroOutroEnabled(true)
        setIntroProvider("HYBRID")
        setDetectIntroFromChapters(true)
        setAutoSkipIntro(false)
        setAutoSkipOutro(false)
        setShowProviderStatusOverlay(false)
        resetHudLayoutToDefault()
    }

    fun resetSubtitlePreferences() {
        _subtitlesEnabled.value = true
        _dualSubtitles.value = false
        _subtitleFontSize.value = 22f
        _subtitleColor.value = 0xFFFFFFFF
        _subtitleBackground.value = 0x80000000
        _subtitleDelayMs.value = 0
        _subtitleLanguage.value = "Auto"
        setSubtitleEdgeType("DROP_SHADOW")
        setSubtitleBgOpacity(0.5f)
        setSubtitleBottomPaddingDp(24)
        setSubtitleBold(false)
        setSubtitleSpeedMilli(1000)
        setSubtitleItalic(false)
        setSubtitleAlignment("CENTER")
        setSubtitleCustomFont("", "System Default")
        setSecondarySubtitleColor(0xFFFDE047)
        setSubOverrideAssMode("SCALE")
        setSubScaleByWindow(true)
        setSubFontsDir("", "App Default")
        setHoldDragMovesSubtitles(false)
        setSwipeSubtitlesToSeekDialog(false)
        setInvertSwipeSubtitlesDirection(false)
        setOnlineSubSearchMode("HYBRID")
        setOnlineSubPreferredLang("en")
        if (::prefs.isInitialized) {
            prefs.edit()
                .putBoolean(KEY_SUBTITLE_ENABLED, true)
                .putBoolean(KEY_DUAL_SUBTITLES, false)
                .putFloat(KEY_SUBTITLE_FONT_SIZE, 22f)
                .putLong(KEY_SUBTITLE_COLOR, 0xFFFFFFFF)
                .putLong(KEY_SUBTITLE_BG, 0x80000000)
                .putInt(KEY_SUBTITLE_DELAY, 0)
                .putString(KEY_SUBTITLE_LANG, "Auto")
                .apply()
        }
    }

    fun resetAudioPreferences() {
        _audioLanguage.value = "Auto"
        _audioDelayMs.value = 0
        _audioBoost.value = 100
        _audioOffload.value = true
        _nightModeCompressor.value = false
        _eqPreset.value = "Cinema"
        setAudioPitchCorrection(true)
        setAudioChannelMode("AUTO")
        setEqCustomBandsDb("0.0,0.0,0.0,0.0,0.0")
        setIncludeAudioFilesInVideoLib(false)
        setMinAudioDurationSec(10)
        setAlbumPaletteBackground(true)
        setVolumeNormalization(false)
        setVolumeBoostCapPct(200)
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_AUDIO_LANG, "Auto")
                .putInt(KEY_AUDIO_DELAY, 0)
                .putInt(KEY_AUDIO_BOOST, 100)
                .putBoolean(KEY_AUDIO_OFFLOAD, true)
                .putBoolean(KEY_NIGHT_COMPRESSOR, false)
                .putString(KEY_EQ_PRESET, "Cinema")
                .apply()
        }
    }

    fun resetGestureAndDisplayPreferences() {
        _volumeGesture.value = true
        _brightnessGesture.value = true
        _doubleTapSeconds.value = 10
        _seekGesture.value = true
        _hapticFeedback.value = true
        _longPress2x.value = true
        setPinchZoomEnabled(true)
        setScreenOrientation("Auto")
        setKeepScreenOn(true)
        setDoubleTapAreaWidthPct(28)
        setDoubleTapCenterPlayPause(true)
        setDoubleTapLeftAction("SEEK")
        setDoubleTapCenterAction("PLAY_PAUSE")
        setDoubleTapRightAction("SEEK")
        setUseSingleTapToPause(false)
        setDisplayVolumeOnRight(true)
        setRememberBrightness(false)
        setSavedPlayerBrightness(0.7f)
        setHoldSpeedMultiplier(2.0f)
        setHorizontalSwipeSensitivity(1.0f)
        setShowGestureSeekOverlay(true)
        setShowGestureVolBrightOverlay(true)
        setSwipeUpOpensPlaylist(true)
        setCustomSkipDurationSec(90)
        setAmbientModeEnabled(false)
        setDoubleTapPauseResume(true)
        setLongPressSpeedVariable(false)
        setAllowGesturesInPanels(false)
        if (::prefs.isInitialized) {
            prefs.edit()
                .putBoolean(KEY_VOLUME_GESTURE, true)
                .putBoolean(KEY_BRIGHT_GESTURE, true)
                .putInt(KEY_DOUBLE_TAP_SEC, 10)
                .putBoolean(KEY_SEEK_GESTURE, true)
                .putBoolean(KEY_HAPTIC_FEEDBACK, true)
                .putBoolean(KEY_LONG_PRESS_2X, true)
                .apply()
        }
    }

    fun resetLibraryPreferences() {
        setSortOption("TITLE")
        setSortAscending(true)
        setViewMode("LIST")
        setFolderViewMode("FOLDER")
        setShowFolderSize(true)
        setShowFolderPath(true)
        setShowDateModified(true)
        setShowResolutionBadge(true)
        setFolderGridColumnsPortrait(2)
        setFolderGridColumnsLandscape(4)
        setVideoGridColumnsPortrait(2)
        setVideoGridColumnsLandscape(4)
        setUnlimitedNameLines(false)
        setShowExtensionField(false)
        setShowDurationField(true)
        setShowProgressBar(true)
        setCenterGridTitles(true)
        setShowTotalVideosChip(true)
        setShowTotalDurationChip(false)
        setShowFolderThumbnails(false)
        setShowNewBadge(true)
        setNewBadgeDays(7)
        setWatchedThresholdPct(95)
        setShowQuickPlayFab(true)
        setQuickPlayFabDirect(false)
        setDualPaneTablet(true)
        setManualGridColumnsEnabled(false)
        setSeparateFolderVideoLayout(false)
        setNetworkThumbnailsEnabled(false)
        setThumbnailCacheSizeMb(128)
        setShowHiddenMediaFolders(false)
        setMediaQualityFilter("ALL")
        setOnlyShowFoldersInHome(false)
        setHomeCompactGrid(false)
    }

    fun resetNetworkPreferences() {
        setWifiOnlyNetwork(false)
        setNetworkTimeoutSec(15)
        setAutoPiP(true)
        setBackgroundPlayback(false)
    }

    private fun serializePlaylists(list: List<UserPlaylist>): String {
        val arr = JSONArray()
        list.forEach { pl ->
            val uris = JSONArray().apply { pl.itemUris.forEach { put(it) } }
            arr.put(JSONObject().apply {
                put("id", pl.id)
                put("name", pl.name)
                put("createdAtMs", pl.createdAtMs)
                put("itemUris", uris)
            })
        }
        return arr.toString()
    }

    private fun deserializePlaylists(json: String): List<UserPlaylist> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    val urisArr = obj.optJSONArray("itemUris")
                    val uris = if (urisArr == null) emptyList() else (0 until urisArr.length()).map { urisArr.getString(it) }
                    add(
                        UserPlaylist(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            itemUris = uris,
                            createdAtMs = obj.optLong("createdAtMs", 0L)
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeStreams(list: List<SavedStreamItem>): String {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("title", s.title)
                put("url", s.url)
                put("streamType", s.streamType)
            })
        }
        return arr.toString()
    }

    private fun deserializeStreams(json: String): List<SavedStreamItem> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        SavedStreamItem(
                            id = obj.getString("id"),
                            title = obj.getString("title"),
                            url = obj.getString("url"),
                            streamType = obj.optString("streamType", "HLS")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun serializeShares(list: List<NetworkShareConfig>): String {
        val arr = JSONArray()
        list.forEach { sh ->
            arr.put(JSONObject().apply {
                put("id", sh.id)
                put("name", sh.name)
                put("protocol", sh.protocol)
                put("baseUrl", sh.baseUrl)
                put("username", sh.username)
            })
        }
        return arr.toString()
    }

    private fun deserializeShares(json: String): List<NetworkShareConfig> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.getJSONObject(i)
                    add(
                        NetworkShareConfig(
                            id = obj.getString("id"),
                            name = obj.getString("name"),
                            protocol = obj.optString("protocol", "WebDAV"),
                            baseUrl = obj.getString("baseUrl"),
                            username = obj.optString("username", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun setAutoPlayNext(enabled: Boolean) {
        _autoPlayNext.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_AUTO_PLAY_NEXT, enabled).apply()
    }

    fun setRememberPosition(enabled: Boolean) {
        _rememberPosition.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_REMEMBER_POSITION, enabled).apply()
    }

    fun setDefaultEngine(engine: String) {
        val norm = if (engine.trim().equals("MEDIA3", ignoreCase = true)) {
            "NATIVE"
        } else {
            PlaybackEngineMode.fromId(engine).id
        }
        _defaultEngine.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_DEFAULT_ENGINE, norm).apply()
    }

    fun currentEngineRenderConfig(): EngineRenderConfig {
        return EngineRenderConfig(
            mpvProfile = _mpvProfile.value,
            decoderPriority = _decoderPriority.value,
            useGpuNext = _useGpuNext.value,
            useVulkan = _useVulkan.value,
            useYuv420p = _useYuv420p.value,
            hdrTonemapping = _hdrTonemapping.value,
            debanding = _mpvDebanding.value,
            debandIterations = _debandIterations.value,
            debandThreshold = _debandThreshold.value,
            debandRange = _debandRange.value,
            debandGrain = _debandGrain.value,
            anime4kEnabled = _anime4k.value,
            anime4kMode = _anime4kMode.value,
            anime4kQuality = _anime4kQuality.value
        )
    }

    fun setDecoderMode(mode: String) {
        _decoderMode.value = mode
        if (::prefs.isInitialized) prefs.edit().putString(KEY_DECODER_MODE, mode).apply()
    }

    fun setHwAcceleration(enabled: Boolean) {
        _hwAcceleration.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_HW_ACCELERATION, enabled).apply()
    }

    fun setAutoRoutingDolbyVision(enabled: Boolean) {
        _autoRoutingDolbyVision.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_AUTO_ROUTING_DV, enabled).apply()
    }

    fun setAutoRoutingHlsDash(enabled: Boolean) {
        _autoRoutingHlsDash.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_AUTO_ROUTING_STREAMS, enabled).apply()
    }

    fun setSmoothMotion(enabled: Boolean) {
        _smoothMotion120fps.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SMOOTH_MOTION, enabled).apply()
    }

    fun setTargetFps(fps: Int) {
        _targetFps.value = fps
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_TARGET_FPS, fps).apply()
    }

    fun setUiAnimationScale(scale: Float) {
        _uiAnimationScale.value = scale
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_UI_ANIMATION_SCALE, scale).apply()
    }

    fun setSubtitlesEnabled(enabled: Boolean) {
        _subtitlesEnabled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SUBTITLE_ENABLED, enabled).apply()
    }

    fun setDualSubtitles(enabled: Boolean) {
        _dualSubtitles.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_DUAL_SUBTITLES, enabled).apply()
    }

    fun setSubtitleFontSize(sizeSp: Float) {
        _subtitleFontSize.value = sizeSp
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_SUBTITLE_FONT_SIZE, sizeSp).apply()
    }

    fun setSubtitleColor(colorLong: Long) {
        _subtitleColor.value = colorLong
        if (::prefs.isInitialized) prefs.edit().putLong(KEY_SUBTITLE_COLOR, colorLong).apply()
    }

    fun setSubtitleBackground(colorLong: Long) {
        _subtitleBackground.value = colorLong
        if (::prefs.isInitialized) prefs.edit().putLong(KEY_SUBTITLE_BG, colorLong).apply()
    }

    fun setSubtitleDelayMs(delay: Int) {
        _subtitleDelayMs.value = delay
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_SUBTITLE_DELAY, delay).apply()
    }

    fun setSubtitleLanguage(lang: String) {
        _subtitleLanguage.value = lang
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SUBTITLE_LANG, lang).apply()
    }

    fun setAudioLanguage(lang: String) {
        _audioLanguage.value = lang
        if (::prefs.isInitialized) prefs.edit().putString(KEY_AUDIO_LANG, lang).apply()
    }

    fun setAudioDelayMs(delay: Int) {
        _audioDelayMs.value = delay
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_AUDIO_DELAY, delay).apply()
    }

    fun setAudioBoost(boost: Int) {
        _audioBoost.value = boost
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_AUDIO_BOOST, boost).apply()
    }

    fun setAudioOffload(enabled: Boolean) {
        _audioOffload.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_AUDIO_OFFLOAD, enabled).apply()
    }

    fun setNightModeCompressor(enabled: Boolean) {
        _nightModeCompressor.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_NIGHT_COMPRESSOR, enabled).apply()
    }

    fun setEqualizerPreset(preset: String) {
        _eqPreset.value = preset
        if (::prefs.isInitialized) prefs.edit().putString(KEY_EQ_PRESET, preset).apply()
    }

    fun setVolumeGesture(enabled: Boolean) {
        _volumeGesture.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_VOLUME_GESTURE, enabled).apply()
    }

    fun setBrightnessGesture(enabled: Boolean) {
        _brightnessGesture.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_BRIGHT_GESTURE, enabled).apply()
    }

    fun setDoubleTapSeconds(seconds: Int) {
        _doubleTapSeconds.value = seconds
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_DOUBLE_TAP_SEC, seconds).apply()
    }

    fun setSeekGesture(enabled: Boolean) {
        _seekGesture.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SEEK_GESTURE, enabled).apply()
    }

    fun setHapticFeedback(enabled: Boolean) {
        _hapticFeedback.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_HAPTIC_FEEDBACK, enabled).apply()
    }

    fun setLongPress2x(enabled: Boolean) {
        _longPress2x.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_LONG_PRESS_2X, enabled).apply()
    }

    fun setDefaultAspectRatio(ratio: String) {
        _defaultAspectRatio.value = ratio
        if (::prefs.isInitialized) prefs.edit().putString(KEY_DEFAULT_ASPECT, ratio).apply()
    }

    fun setScreenOrientation(orientation: String) {
        _screenOrientation.value = orientation
        if (::prefs.isInitialized) prefs.edit().putString(KEY_ORIENTATION, orientation).apply()
    }

    fun setKeepScreenOn(keep: Boolean) {
        _keepScreenOn.value = keep
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON, keep).apply()
    }

    fun setDefaultSpeed(speed: Float) {
        _defaultSpeed.value = speed
        if (::prefs.isInitialized) prefs.edit().putFloat(KEY_DEFAULT_SPEED, speed).apply()
    }

    fun setMedia3Buffers(minBuffer: Int, maxBuffer: Int, bufferPlayback: Int, bufferRebuffer: Int) {
        _minBufferMs.value = minBuffer
        _maxBufferMs.value = maxBuffer
        _bufferForPlaybackMs.value = bufferPlayback
        _bufferForPlaybackAfterRebufferMs.value = bufferRebuffer
        if (::prefs.isInitialized) {
            prefs.edit()
                .putInt(KEY_MIN_BUFFER_MS, minBuffer)
                .putInt(KEY_MAX_BUFFER_MS, maxBuffer)
                .putInt(KEY_BUFFER_PLAYBACK_MS, bufferPlayback)
                .putInt(KEY_BUFFER_REBUFFER_MS, bufferRebuffer)
                .apply()
        }
    }

    fun setSortOption(option: String) {
        _sortOption.value = option
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SORT_OPTION, option).apply()
    }

    fun setSortAscending(asc: Boolean) {
        _sortAscending.value = asc
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SORT_ASC, asc).apply()
    }

    fun setViewMode(mode: String) {
        _viewMode.value = mode
        if (::prefs.isInitialized) prefs.edit().putString(KEY_VIEW_MODE, mode).apply()
    }

    fun setFolderViewMode(mode: String) {
        _folderViewMode.value = mode
        if (::prefs.isInitialized) prefs.edit().putString(KEY_FOLDER_VIEW_MODE, mode).apply()
    }

    fun setShowFolderSize(show: Boolean) {
        _showFolderSize.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_FOLDER_SIZE, show).apply()
    }

    fun setShowFolderPath(show: Boolean) {
        _showFolderPath.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_FOLDER_PATH, show).apply()
    }

    fun setShowDateModified(show: Boolean) {
        _showDateModified.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_DATE_MODIFIED, show).apply()
    }

    fun setShowResolutionBadge(show: Boolean) {
        _showResolutionBadge.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_RESOLUTION_BADGE, show).apply()
    }

    fun setFolderGridColumnsPortrait(cols: Int) {
        val clamped = cols.coerceIn(1, 4)
        _folderGridColumnsPortrait.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_FOLDER_GRID_COLS_PORTRAIT, clamped).apply()
    }

    fun setFolderGridColumnsLandscape(cols: Int) {
        val clamped = cols.coerceIn(2, 6)
        _folderGridColumnsLandscape.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_FOLDER_GRID_COLS_LANDSCAPE, clamped).apply()
    }

    fun setVideoGridColumnsPortrait(cols: Int) {
        val clamped = cols.coerceIn(1, 4)
        _videoGridColumnsPortrait.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_VIDEO_GRID_COLS_PORTRAIT, clamped).apply()
    }

    fun setVideoGridColumnsLandscape(cols: Int) {
        val clamped = cols.coerceIn(2, 6)
        _videoGridColumnsLandscape.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_VIDEO_GRID_COLS_LANDSCAPE, clamped).apply()
    }

    fun setUnlimitedNameLines(enabled: Boolean) {
        _unlimitedNameLines.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_UNLIMITED_NAME_LINES, enabled).apply()
    }

    fun setShowExtensionField(show: Boolean) {
        _showExtensionField.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_EXTENSION, show).apply()
    }

    fun setShowDurationField(show: Boolean) {
        _showDurationField.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_DURATION, show).apply()
    }

    fun setShowProgressBar(show: Boolean) {
        _showProgressBar.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_PROGRESS_BAR, show).apply()
    }

    fun setCenterGridTitles(center: Boolean) {
        _centerGridTitles.value = center
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_CENTER_GRID_TITLES, center).apply()
    }

    fun setShowTotalVideosChip(show: Boolean) {
        _showTotalVideosChip.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_TOTAL_VIDEOS, show).apply()
    }

    fun setShowTotalDurationChip(show: Boolean) {
        _showTotalDurationChip.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_TOTAL_DURATION, show).apply()
    }

    fun setShowFolderThumbnails(show: Boolean) {
        _showFolderThumbnails.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_FOLDER_THUMBNAILS, show).apply()
    }

    fun setShowNewBadge(show: Boolean) {
        _showNewBadge.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_NEW_BADGE, show).apply()
    }

    fun setNewBadgeDays(days: Int) {
        val clamped = days.coerceIn(0, 30)
        _newBadgeDays.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_NEW_BADGE_DAYS, clamped).apply()
    }

    fun setWatchedThresholdPct(pct: Int) {
        val clamped = pct.coerceIn(0, 100)
        _watchedThresholdPct.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_WATCHED_THRESHOLD_PCT, clamped).apply()
    }

    fun setShowQuickPlayFab(show: Boolean) {
        _showQuickPlayFab.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_QUICK_PLAY_FAB, show).apply()
    }

    fun setQuickPlayFabDirect(direct: Boolean) {
        _quickPlayFabDirect.value = direct
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_QUICK_PLAY_FAB_DIRECT, direct).apply()
    }

    fun setDualPaneTablet(enabled: Boolean) {
        _dualPaneTablet.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_DUAL_PANE_TABLET, enabled).apply()
    }

    fun setVaultEntryHidden(hidden: Boolean) {
        _vaultEntryHidden.value = hidden
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_VAULT_ENTRY_HIDDEN, hidden).apply()
    }

    fun setVaultBiometricEnabled(enabled: Boolean) {
        _vaultBiometricEnabled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_VAULT_BIOMETRIC_ENABLED, enabled).apply()
    }

    fun setBackgroundPlayback(enabled: Boolean) {
        _backgroundPlayback.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_BACKGROUND_PLAYBACK, enabled).apply()
    }

    fun setAutoPiP(enabled: Boolean) {
        _autoPiP.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_AUTO_PIP, enabled).apply()
    }

    fun setAnime4k(enabled: Boolean) {
        _anime4k.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_ANIME4K, enabled).apply()
    }

    fun setAnime4kMode(mode: String) {
        val norm = mode.trim().uppercase()
        _anime4kMode.value = norm
        _anime4k.value = norm != "OFF"
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_ANIME4K_MODE, norm)
                .putBoolean(KEY_ANIME4K, norm != "OFF")
                .apply()
        }
    }

    fun setAnime4kQuality(quality: String) {
        val norm = quality.trim().uppercase()
        _anime4kQuality.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_ANIME4K_QUALITY, norm).apply()
    }

    fun setHdrTonemapping(enabled: Boolean) {
        _hdrTonemapping.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_HDR_TONEMAP, enabled).apply()
    }

    fun setMpvProfile(profile: String) {
        val norm = profile.trim().lowercase().ifBlank { "fast" }
        _mpvProfile.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_MPV_PROFILE, norm).apply()
    }

    fun setUseGpuNext(enabled: Boolean) {
        _useGpuNext.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_USE_GPU_NEXT, enabled).apply()
    }

    fun setUseVulkan(enabled: Boolean) {
        _useVulkan.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_USE_VULKAN, enabled).apply()
    }

    fun setUseYuv420p(enabled: Boolean) {
        _useYuv420p.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_USE_YUV420P, enabled).apply()
    }

    fun setMpvDebanding(mode: String) {
        val norm = mode.trim().uppercase()
        _mpvDebanding.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_MPV_DEBANDING, norm).apply()
    }

    fun setDebandIterations(iterations: Int) {
        val v = iterations.coerceIn(1, 4)
        _debandIterations.value = v
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_DEBAND_ITERATIONS, v).apply()
    }

    fun setDebandThreshold(threshold: Int) {
        val v = threshold.coerceIn(0, 256)
        _debandThreshold.value = v
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_DEBAND_THRESHOLD, v).apply()
    }

    fun setDebandRange(range: Int) {
        val v = range.coerceIn(1, 64)
        _debandRange.value = v
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_DEBAND_RANGE, v).apply()
    }

    fun setDebandGrain(grain: Int) {
        val v = grain.coerceIn(0, 256)
        _debandGrain.value = v
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_DEBAND_GRAIN, v).apply()
    }

    fun setManualGridColumnsEnabled(enabled: Boolean) {
        _manualGridColumnsEnabled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_MANUAL_GRID_COLS, enabled).apply()
    }

    fun setSeparateFolderVideoLayout(enabled: Boolean) {
        _separateFolderVideoLayout.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SEPARATE_FOLDER_VIDEO_LAYOUT, enabled).apply()
    }

    fun setNetworkThumbnailsEnabled(enabled: Boolean) {
        _networkThumbnailsEnabled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_NETWORK_THUMBNAILS, enabled).apply()
    }

    fun setThumbnailCacheSizeMb(mb: Int) {
        val v = mb.coerceIn(32, 512)
        _thumbnailCacheSizeMb.value = v
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_THUMBNAIL_CACHE_MB, v).apply()
    }

    fun setShowScreenRotationOverlay(show: Boolean) {
        _showScreenRotationOverlay.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_ROTATION_OVERLAY, show).apply()
    }

    fun setShowAspectRatioOverlay(show: Boolean) {
        _showAspectRatioOverlay.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_ASPECT_OVERLAY, show).apply()
    }

    fun setShowPlaybackSpeedOverlay(show: Boolean) {
        _showPlaybackSpeedOverlay.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_SPEED_OVERLAY, show).apply()
    }

    fun setShowDecoderOverlay(show: Boolean) {
        _showDecoderOverlay.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_DECODER_OVERLAY, show).apply()
    }

    fun setShowShuffleOverlay(show: Boolean) {
        _showShuffleOverlay.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_SHUFFLE_OVERLAY, show).apply()
    }

    fun setShowLoopOverlay(show: Boolean) {
        _showLoopOverlay.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_LOOP_OVERLAY, show).apply()
    }

    fun setShowPipOverlay(show: Boolean) {
        _showPipOverlay.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_PIP_OVERLAY, show).apply()
    }

    fun setShowScreenshotOverlay(show: Boolean) {
        _showScreenshotOverlay.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_SCREENSHOT_OVERLAY, show).apply()
    }

    fun setScreenshotFormat(format: String) {
        val norm = when (format.trim().uppercase()) {
            "JPEG", "JPG" -> "JPEG"
            "WEBP" -> "WEBP"
            else -> "PNG"
        }
        _screenshotFormat.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SCREENSHOT_FORMAT, norm).apply()
    }

    fun setScreenshotQuality(quality: Int) {
        val v = quality.coerceIn(60, 100)
        _screenshotQuality.value = v
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_SCREENSHOT_QUALITY, v).apply()
    }

    fun setScreenshotFolder(folder: String) {
        val clean = folder.trim().ifBlank { "MPVX Screenshots" }
        _screenshotFolder.value = clean
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SCREENSHOT_FOLDER, clean).apply()
    }

    fun setDoubleTapPauseResume(enabled: Boolean) {
        _doubleTapPauseResume.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_DOUBLE_TAP_PAUSE_RESUME, enabled).apply()
    }

    fun setLongPressSpeedVariable(enabled: Boolean) {
        _longPressSpeedVariable.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_LONG_PRESS_SPEED_VAR, enabled).apply()
    }

    fun setRememberAspectPerVideo(enabled: Boolean) {
        _rememberAspectPerVideo.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_REMEMBER_ASPECT_PER_VIDEO, enabled).apply()
    }

    fun setCloseAfterEndOfPlayback(enabled: Boolean) {
        _closeAfterEndOfPlayback.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_CLOSE_AFTER_END, enabled).apply()
    }

    fun setKeepScreenOnWhenPaused(enabled: Boolean) {
        _keepScreenOnWhenPaused.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_KEEP_SCREEN_ON_PAUSED, enabled).apply()
    }

    fun setAutoplayAfterScreenUnlock(enabled: Boolean) {
        _autoplayAfterScreenUnlock.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_AUTOPLAY_AFTER_UNLOCK, enabled).apply()
    }

    fun setPipOnHomeGestureOnly(enabled: Boolean) {
        _pipOnHomeGestureOnly.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_PIP_ON_HOME_ONLY, enabled).apply()
    }

    fun setVideoMiniPlayerEnabled(enabled: Boolean) {
        _videoMiniPlayerEnabled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_VIDEO_MINI_PLAYER, enabled).apply()
    }

    fun setControlsDrawerEnabled(enabled: Boolean) {
        _controlsDrawerEnabled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_CONTROLS_DRAWER_ENABLED, enabled).apply()
    }

    fun setReducePlayerAnimation(enabled: Boolean) {
        _reducePlayerAnimation.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_REDUCE_PLAYER_ANIMATION, enabled).apply()
    }

    fun setAllowGesturesInPanels(enabled: Boolean) {
        _allowGesturesInPanels.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_ALLOW_GESTURES_IN_PANELS, enabled).apply()
    }

    fun setShowSeekRipple(enabled: Boolean) {
        _showSeekRipple.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_SEEK_RIPPLE, enabled).apply()
    }

    fun setShowSeekTimeInHud(enabled: Boolean) {
        _showSeekTimeInHud.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_SEEK_TIME_HUD, enabled).apply()
    }

    fun setShowBufferedRangeOnSeekbar(enabled: Boolean) {
        _showBufferedRangeOnSeekbar.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_BUFFERED_SEEKBAR, enabled).apply()
    }

    fun setShowChaptersOnSeekbar(enabled: Boolean) {
        _showChaptersOnSeekbar.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_CHAPTERS_SEEKBAR, enabled).apply()
    }

    fun setDoubleTapLeftAction(action: String) {
        val norm = com.cmstudio.player.ui.player.DoubleTapActionCatalog.normalizeAction(action, "SEEK")
        _doubleTapLeftAction.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_DOUBLE_TAP_LEFT_ACTION, norm).apply()
    }

    fun setDoubleTapCenterAction(action: String) {
        val norm = com.cmstudio.player.ui.player.DoubleTapActionCatalog.normalizeAction(action, "PLAY_PAUSE")
        _doubleTapCenterAction.value = norm
        setDoubleTapCenterPlayPause(norm == com.cmstudio.player.ui.player.DoubleTapActionCatalog.ACTION_PLAY_PAUSE)
        if (::prefs.isInitialized) prefs.edit().putString(KEY_DOUBLE_TAP_CENTER_ACTION, norm).apply()
    }

    fun setDoubleTapRightAction(action: String) {
        val norm = com.cmstudio.player.ui.player.DoubleTapActionCatalog.normalizeAction(action, "SEEK")
        _doubleTapRightAction.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_DOUBLE_TAP_RIGHT_ACTION, norm).apply()
    }

    fun setShowHoldSpeedOverlay(enabled: Boolean) {
        _showHoldSpeedOverlay.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_HOLD_SPEED_OVERLAY, enabled).apply()
    }

    fun setShowAspectFeedbackOverlay(enabled: Boolean) {
        _showAspectFeedbackOverlay.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_ASPECT_FEEDBACK_OVERLAY, enabled).apply()
    }

    fun setShowZoomFeedbackOverlay(enabled: Boolean) {
        _showZoomFeedbackOverlay.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_ZOOM_FEEDBACK_OVERLAY, enabled).apply()
    }

    fun setShowRepeatShuffleOverlay(enabled: Boolean) {
        _showRepeatShuffleOverlay.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_REPEAT_SHUFFLE_OVERLAY, enabled).apply()
    }

    fun setShowActionFeedbackPills(enabled: Boolean) {
        _showActionFeedbackPills.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_ACTION_FEEDBACK_PILLS, enabled).apply()
    }

    fun setIncludeSubtitlesInScreenshot(enabled: Boolean) {
        _includeSubtitlesInScreenshot.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_INCLUDE_SUBS_SCREENSHOT, enabled).apply()
    }

    fun setPngCompressionLevel(level: Int) {
        val v = level.coerceIn(0, 9)
        _pngCompressionLevel.value = v
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_PNG_COMPRESSION_LEVEL, v).apply()
    }

    fun setScreenshotFilenameTemplate(template: String) {
        val clean = template.trim().ifBlank { "MPVX_{title}_{time}" }
        _screenshotFilenameTemplate.value = clean
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SCREENSHOT_TEMPLATE, clean).apply()
    }

    fun setSubOverrideAssMode(mode: String) {
        val norm = when (mode.trim().uppercase()) {
            "NO", "YES", "FORCE", "STRIP" -> mode.trim().uppercase()
            else -> "SCALE"
        }
        _subOverrideAssMode.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_SUB_OVERRIDE_ASS, norm).apply()
    }

    fun setSubScaleByWindow(enabled: Boolean) {
        _subScaleByWindow.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SUB_SCALE_BY_WINDOW, enabled).apply()
    }

    fun setSubFontsDir(uriString: String, label: String) {
        val cleanUri = uriString.trim()
        val cleanLabel = label.trim().ifBlank { if (cleanUri.isBlank()) "App Default" else "Custom Folder" }
        _subFontsDirUri.value = cleanUri
        _subFontsDirLabel.value = cleanLabel
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_SUB_FONTS_DIR_URI, cleanUri)
                .putString(KEY_SUB_FONTS_DIR_LABEL, cleanLabel)
                .apply()
        }
    }

    fun setHoldDragMovesSubtitles(enabled: Boolean) {
        _holdDragMovesSubtitles.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_HOLD_DRAG_MOVES_SUBS, enabled).apply()
    }

    fun setSwipeSubtitlesToSeekDialog(enabled: Boolean) {
        _swipeSubtitlesToSeekDialog.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SWIPE_SUBS_SEEK_DIALOG, enabled).apply()
    }

    fun setInvertSwipeSubtitlesDirection(enabled: Boolean) {
        _invertSwipeSubtitlesDirection.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_INVERT_SWIPE_SUBS_DIR, enabled).apply()
    }

    fun setIncludeAudioFilesInVideoLib(enabled: Boolean) {
        _includeAudioFilesInVideoLib.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_INCLUDE_AUDIO_IN_VIDEO_LIB, enabled).apply()
    }

    fun setMinAudioDurationSec(seconds: Int) {
        val clamped = seconds.coerceIn(0, 300)
        _minAudioDurationSec.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_MIN_AUDIO_DURATION_SEC, clamped).apply()
    }

    fun setAlbumPaletteBackground(enabled: Boolean) {
        _albumPaletteBackground.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_ALBUM_PALETTE_BG, enabled).apply()
    }

    fun setVolumeNormalization(enabled: Boolean) {
        _volumeNormalization.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_VOLUME_NORMALIZATION, enabled).apply()
    }

    fun setVolumeBoostCapPct(capPct: Int) {
        val clamped = capPct.coerceIn(100, 200)
        _volumeBoostCapPct.value = clamped
        if (_audioBoost.value > clamped) {
            setAudioBoost(clamped)
        }
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_VOLUME_BOOST_CAP, clamped).apply()
    }

    fun setExtraSmartCollections(enabled: Boolean) {
        _extraSmartCollections.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_EXTRA_SMART_COLLECTIONS, enabled).apply()
    }

    fun setExtraWatchStats(enabled: Boolean) {
        _extraWatchStats.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_EXTRA_WATCH_STATS, enabled).apply()
    }

    fun setExtraPlaybackDiagnostics(enabled: Boolean) {
        _extraPlaybackDiagnostics.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_EXTRA_PLAYBACK_DIAG, enabled).apply()
    }

    fun setExtraLibraryHealth(enabled: Boolean) {
        _extraLibraryHealth.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_EXTRA_LIBRARY_HEALTH, enabled).apply()
    }

    fun setExtraQualityFilters(enabled: Boolean) {
        _extraQualityFilters.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_EXTRA_QUALITY_FILTERS, enabled).apply()
    }

    fun setExtraActivityLog(enabled: Boolean) {
        _extraActivityLog.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_EXTRA_ACTIVITY_LOG, enabled).apply()
    }

    fun setMpvConfEnabled(enabled: Boolean) {
        _mpvConfEnabled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_MPV_CONF_ENABLED, enabled).apply()
    }

    fun setMpvConfText(text: String) {
        _mpvConfText.value = text
        if (::prefs.isInitialized) prefs.edit().putString(KEY_MPV_CONF_TEXT, text).apply()
        appContext?.let { com.cmstudio.player.engine.MpvConfManager.writeStoredText(it, text) }
    }

    fun setShowHiddenMediaFolders(show: Boolean) {
        _showHiddenMediaFolders.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_HIDDEN_FOLDERS, show).apply()
    }

    fun addSafMediaTreeUri(uriString: String) {
        val clean = uriString.trim()
        if (clean.isEmpty()) return
        val updated = _safMediaTreeUris.value + clean
        _safMediaTreeUris.value = updated
        if (::prefs.isInitialized) prefs.edit().putStringSet(KEY_SAF_MEDIA_TREES, updated).apply()
    }

    fun removeSafMediaTreeUri(uriString: String) {
        val updated = _safMediaTreeUris.value - uriString
        _safMediaTreeUris.value = updated
        if (::prefs.isInitialized) prefs.edit().putStringSet(KEY_SAF_MEDIA_TREES, updated).apply()
    }

    fun setIntroOutroEnabled(enabled: Boolean) {
        _introOutroEnabled.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_INTRO_OUTRO_ENABLED, enabled).apply()
    }

    fun setIntroProvider(provider: String) {
        val norm = when (provider.trim().uppercase()) {
            "CHAPTERS", "INTRODB", "THEINTRODB", "ANISKIP", "ISKIP" -> provider.trim().uppercase()
            else -> "HYBRID"
        }
        _introProvider.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_INTRO_PROVIDER, norm).apply()
    }

    fun setDetectIntroFromChapters(enabled: Boolean) {
        _detectIntroFromChapters.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_DETECT_INTRO_CHAPTERS, enabled).apply()
    }

    fun setAutoSkipIntro(enabled: Boolean) {
        _autoSkipIntro.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_AUTO_SKIP_INTRO, enabled).apply()
    }

    fun setAutoSkipOutro(enabled: Boolean) {
        _autoSkipOutro.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_AUTO_SKIP_OUTRO, enabled).apply()
    }

    fun setCustomIntroKeywords(csv: String) {
        _customIntroKeywords.value = csv
        if (::prefs.isInitialized) prefs.edit().putString(KEY_CUSTOM_INTRO_KEYWORDS, csv).apply()
    }

    fun setCustomOutroKeywords(csv: String) {
        _customOutroKeywords.value = csv
        if (::prefs.isInitialized) prefs.edit().putString(KEY_CUSTOM_OUTRO_KEYWORDS, csv).apply()
    }

    fun setShowProviderStatusOverlay(show: Boolean) {
        _showProviderStatusOverlay.value = show
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_SHOW_PROVIDER_STATUS_OV, show).apply()
    }

    fun setOnlineSubSearchMode(mode: String) {
        val norm = when (mode.trim().uppercase()) {
            "WYZIE", "SUBHUB" -> mode.trim().uppercase()
            else -> "HYBRID"
        }
        _onlineSubSearchMode.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_ONLINE_SUB_MODE, norm).apply()
    }

    fun setOnlineSubPreferredLang(langCode: String) {
        val clean = langCode.trim().lowercase().ifBlank { "en" }
        _onlineSubPreferredLang.value = clean
        if (::prefs.isInitialized) prefs.edit().putString(KEY_ONLINE_SUB_LANG, clean).apply()
    }

    fun getSubdlApiKey(): String = EncryptedCredentialStore.getSecret(appContext, "subdl_api_key")

    fun setSubdlApiKey(key: String) {
        EncryptedCredentialStore.putSecret(appContext, "subdl_api_key", key.trim())
    }

    fun setAudioStandbyDelaySec(sec: Int) {
        val clamped = sec.coerceIn(0, 1800)
        _audioStandbyDelaySec.value = clamped
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_AUDIO_STANDBY_SEC, clamped).apply()
    }

    fun setMusicVisualizerStyle(style: String) {
        val norm = when (style.trim().uppercase()) {
            "BARS", "RING", "OFF" -> style.trim().uppercase()
            else -> "WAVE"
        }
        _musicVisualizerStyle.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_MUSIC_VISUALIZER_STYLE, norm).apply()
    }

    fun setMusicPlayerOrientation(orientation: String) {
        val norm = when (orientation.trim().uppercase()) {
            "PORTRAIT", "LANDSCAPE" -> orientation.trim().uppercase()
            else -> "AUTO"
        }
        _musicPlayerOrientation.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_MUSIC_ORIENTATION, norm).apply()
    }

    fun setMediaQualityFilter(filter: String) {
        val norm = filter.trim().uppercase().ifBlank { "ALL" }
        _mediaQualityFilter.value = norm
        if (::prefs.isInitialized) prefs.edit().putString(KEY_MEDIA_QUALITY_FILTER, norm).apply()
    }

    fun setOnlyShowFoldersInHome(enabled: Boolean) {
        _onlyShowFoldersInHome.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_ONLY_SHOW_FOLDERS, enabled).apply()
    }

    fun setHomeCompactGrid(enabled: Boolean) {
        _homeCompactGrid.value = enabled
        if (::prefs.isInitialized) prefs.edit().putBoolean(KEY_HOME_COMPACT_GRID, enabled).apply()
    }

    fun setServerTabEnabled(tabId: String, enabled: Boolean) {
        val norm = tabId.trim().uppercase()
        if (norm !in OPTIONAL_SERVER_TABS) return
        val updated = if (enabled) _enabledServerTabs.value + norm else _enabledServerTabs.value - norm
        _enabledServerTabs.value = updated
        if (::prefs.isInitialized) prefs.edit().putStringSet(KEY_ENABLED_SERVER_TABS, updated).apply()
    }

    fun saveMediaServer(profile: MediaServerProfile, secretTokenOrPassword: String) {
        val list = _mediaServers.value.filterNot { it.id == profile.id } + profile
        _mediaServers.value = list
        if (secretTokenOrPassword.isNotEmpty()) {
            EncryptedCredentialStore.putSecret(appContext, "srv_secret_${profile.id}", secretTokenOrPassword)
        }
        val tabId = when (profile.kind) {
            MediaServerKind.JELLYFIN -> "JELLYFIN"
            MediaServerKind.NAVIDROME -> "NAVIDROME"
            MediaServerKind.AUDIOBOOKSHELF -> "AUDIOBOOKS"
        }
        setServerTabEnabled(tabId, true)
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_MEDIA_SERVERS_JSON, serializeMediaServers(list)).apply()
        }
    }

    fun getMediaServerSecret(serverId: String): String =
        EncryptedCredentialStore.getSecret(appContext, "srv_secret_$serverId")

    fun removeMediaServer(serverId: String) {
        EncryptedCredentialStore.removeSecret(appContext, "srv_secret_$serverId")
        val updated = _mediaServers.value.filterNot { it.id == serverId }
        _mediaServers.value = updated
        if (::prefs.isInitialized) {
            prefs.edit().putString(KEY_MEDIA_SERVERS_JSON, serializeMediaServers(updated)).apply()
        }
    }

    private fun serializeMediaServers(list: List<MediaServerProfile>): String {
        val arr = JSONArray()
        list.forEach { s ->
            arr.put(JSONObject().apply {
                put("id", s.id)
                put("kind", s.kind.id)
                put("name", s.name)
                put("baseUrl", s.baseUrl)
                put("username", s.username)
                put("userId", s.userId)
            })
        }
        return arr.toString()
    }

    private fun deserializeMediaServers(json: String): List<MediaServerProfile> {
        if (json.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(json)
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val kindRaw = o.optString("kind", "JELLYFIN")
                    val kind = MediaServerKind.entries.firstOrNull { it.id == kindRaw } ?: MediaServerKind.JELLYFIN
                    add(
                        MediaServerProfile(
                            id = o.getString("id"),
                            kind = kind,
                            name = o.optString("name", kind.label),
                            baseUrl = o.getString("baseUrl"),
                            username = o.optString("username", ""),
                            userId = o.optString("userId", "")
                        )
                    )
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    private fun generateSaltHex(): String {
        val bytes = ByteArray(16)
        SecureRandom().nextBytes(bytes)
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun hashPin(pin: String, saltHex: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest("$saltHex:$pin".toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    fun setVaultPin(pin: String) {
        val clean = pin.trim()
        if (clean.length < 4) return
        val salt = generateSaltHex()
        val hash = hashPin(clean, salt)
        inMemoryVaultSalt = salt
        inMemoryVaultHash = hash
        _hasCustomVaultPin.value = true
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_VAULT_PIN_SALT, salt)
                .putString(KEY_VAULT_PIN_HASH, hash)
                .remove(KEY_VAULT_PIN)
                .apply()
        }
    }

    fun verifyVaultPin(pin: String): Boolean {
        val salt = if (::prefs.isInitialized) prefs.getString(KEY_VAULT_PIN_SALT, inMemoryVaultSalt) else inMemoryVaultSalt
        val storedHash = if (::prefs.isInitialized) prefs.getString(KEY_VAULT_PIN_HASH, inMemoryVaultHash) else inMemoryVaultHash
        if (salt.isNullOrBlank() || storedHash.isNullOrBlank()) return false
        val candidate = hashPin(pin.trim(), salt).toByteArray(Charsets.UTF_8)
        val expected = storedHash.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(candidate, expected)
    }

    fun clearVaultPin() {
        inMemoryVaultSalt = null
        inMemoryVaultHash = null
        _hasCustomVaultPin.value = false
        _isVaultUnlocked.value = false
        if (::prefs.isInitialized) {
            prefs.edit()
                .remove(KEY_VAULT_PIN_SALT)
                .remove(KEY_VAULT_PIN_HASH)
                .remove(KEY_VAULT_PIN)
                .apply()
        }
    }

    fun hasVaultSecurityQuestion(): Boolean {
        return _vaultSecurityQuestion.value.isNotBlank() &&
            !inMemoryVaultAnswerHash.isNullOrBlank() &&
            !inMemoryVaultAnswerSalt.isNullOrBlank()
    }

    fun setVaultSecurityQuestion(question: String, answer: String) {
        val cleanQ = question.trim()
        val normalizedA = answer.trim().lowercase(java.util.Locale.ROOT)
        if (cleanQ.isBlank() || normalizedA.isBlank()) return
        val salt = generateSaltHex()
        val hash = hashPin(normalizedA, salt)
        _vaultSecurityQuestion.value = cleanQ
        inMemoryVaultAnswerSalt = salt
        inMemoryVaultAnswerHash = hash
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_VAULT_SEC_QUESTION, cleanQ)
                .putString(KEY_VAULT_SEC_ANSWER_SALT, salt)
                .putString(KEY_VAULT_SEC_ANSWER_HASH, hash)
                .apply()
        }
    }

    fun verifyVaultSecurityAnswer(answer: String): Boolean {
        val salt = if (::prefs.isInitialized) prefs.getString(KEY_VAULT_SEC_ANSWER_SALT, inMemoryVaultAnswerSalt) else inMemoryVaultAnswerSalt
        val storedHash = if (::prefs.isInitialized) prefs.getString(KEY_VAULT_SEC_ANSWER_HASH, inMemoryVaultAnswerHash) else inMemoryVaultAnswerHash
        if (salt.isNullOrBlank() || storedHash.isNullOrBlank()) return false
        val normalized = answer.trim().lowercase(java.util.Locale.ROOT)
        val candidate = hashPin(normalized, salt).toByteArray(Charsets.UTF_8)
        val expected = storedHash.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(candidate, expected)
    }

    fun setVaultUnlocked(unlocked: Boolean) {
        _isVaultUnlocked.value = unlocked
    }

    fun setSeekIntervalSeconds(seconds: Int) {
        _seekIntervalSeconds.value = seconds
        if (::prefs.isInitialized) prefs.edit().putInt(KEY_SEEK_INTERVAL, seconds).apply()
    }

    fun setPlayerControlSize(size: String) {
        _playerControlSize.value = size
        if (::prefs.isInitialized) prefs.edit().putString(KEY_PLAYER_CONTROL_SIZE, size).apply()
    }

    fun setCustomPrimaryColor(color: Long?) {
        _customPrimaryColor.value = color
        if (::prefs.isInitialized) {
            if (color == null) prefs.edit().remove(KEY_CUSTOM_PRIMARY).apply()
            else prefs.edit().putLong(KEY_CUSTOM_PRIMARY, color).apply()
        }
    }

    fun setCustomAccentColor(color: Long?) {
        _customAccentColor.value = color
        if (::prefs.isInitialized) {
            if (color == null) prefs.edit().remove(KEY_CUSTOM_ACCENT).apply()
            else prefs.edit().putLong(KEY_CUSTOM_ACCENT, color).apply()
        }
    }

    fun setCustomBackgroundColor(color: Long?) {
        _customBackgroundColor.value = color
        if (::prefs.isInitialized) {
            if (color == null) prefs.edit().remove(KEY_CUSTOM_BACKGROUND).apply()
            else prefs.edit().putLong(KEY_CUSTOM_BACKGROUND, color).apply()
        }
    }

    fun setCustomSurfaceColor(color: Long?) {
        _customSurfaceColor.value = color
        if (::prefs.isInitialized) {
            if (color == null) prefs.edit().remove(KEY_CUSTOM_SURFACE).apply()
            else prefs.edit().putLong(KEY_CUSTOM_SURFACE, color).apply()
        }
    }

    fun restoreDefaultTheme() {
        _themePreset.value = ThemePreset.MPVX_CYAN
        _darkMode.value = "SYSTEM"
        _isDarkTheme.value = true
        _isAmoled.value = false
        _customPrimaryColor.value = null
        _customAccentColor.value = null
        _customBackgroundColor.value = null
        _customSurfaceColor.value = null
        setCustomThemeWallpaper("", false)
        setCustomThemeWallpaperBlur(0f)
        setCustomThemeWallpaperBrightness(1.0f)
        setCustomThemeWallpaperSaturation(1.0f)
        setCustomThemeWallpaperScrimAlpha(0.45f)
        setCustomThemeSurfaceOpacity(0.85f)
        if (::prefs.isInitialized) {
            prefs.edit()
                .putString(KEY_THEME_PRESET, ThemePreset.MPVX_CYAN.name)
                .putString(KEY_DARK_MODE, "SYSTEM")
                .putBoolean(KEY_DARK_THEME, true)
                .putBoolean(KEY_AMOLED, false)
                .remove(KEY_CUSTOM_PRIMARY)
                .remove(KEY_CUSTOM_ACCENT)
                .remove(KEY_CUSTOM_BACKGROUND)
                .remove(KEY_CUSTOM_SURFACE)
                .apply()
        }
    }

    fun resetPreferencesOnly() {
        resetAppearancePreferences()
        setUseSystemFont(false)
        resetNavigationPreferences()
        resetPlaybackAndPlayerPreferences()
        resetAudioPreferences()
        resetSubtitlePreferences()
        resetGestureAndDisplayPreferences()
        resetLibraryPreferences()
        resetNetworkPreferences()
    }

    private const val SETTINGS_BACKUP_FORMAT = "mpvx-settings-v1"
    private val SENSITIVE_BACKUP_KEYS = setOf(
        KEY_VAULT_PIN,
        KEY_VAULT_PIN_HASH,
        KEY_VAULT_PIN_SALT,
        KEY_VAULT_SEC_QUESTION,
        KEY_VAULT_SEC_ANSWER_HASH,
        KEY_VAULT_SEC_ANSWER_SALT,
        "pref_gemini_key",
        "pref_jellyfin_api_key",
        "pref_subdl_api_key"
    )

    /**
     * Serializes persistent MPVX settings cleanly to JSON, omitting Vault PIN/security hashes & API secrets.
     */
    fun exportSettingsJson(sourcePrefs: SharedPreferences? = if (::prefs.isInitialized) prefs else null): String {
        val entriesObj = JSONObject()
        sourcePrefs?.all?.forEach { (key, value) ->
            if (key in SENSITIVE_BACKUP_KEYS || value == null) return@forEach
            val item = JSONObject()
            when (value) {
                is Boolean -> {
                    item.put("t", "bool")
                    item.put("v", value)
                }
                is Int -> {
                    item.put("t", "int")
                    item.put("v", value)
                }
                is Long -> {
                    item.put("t", "long")
                    item.put("v", value)
                }
                is Float -> {
                    item.put("t", "float")
                    item.put("v", value.toDouble())
                }
                is String -> {
                    item.put("t", "str")
                    item.put("v", value)
                }
                is Set<*> -> {
                    val arr = JSONArray()
                    value.filterIsInstance<String>().forEach { arr.put(it) }
                    item.put("t", "set")
                    item.put("v", arr)
                }
                else -> return@forEach
            }
            entriesObj.put(key, item)
        }
        return JSONObject().apply {
            put("format", SETTINGS_BACKUP_FORMAT)
            put("version", 1)
            put("entries", entriesObj)
        }.toString(2)
    }

    /**
     * Restores settings from JSON created by [exportSettingsJson], commits them, and refreshes live StateFlows.
     * Returns count of restored entries, or -1 if invalid format.
     */
    fun importSettingsJson(
        json: String,
        targetPrefs: SharedPreferences? = if (::prefs.isInitialized) prefs else null
    ): Int {
        val activePrefs = targetPrefs ?: return -1
        return try {
            val root = JSONObject(json)
            if (root.optString("format") != SETTINGS_BACKUP_FORMAT) return -1
            val entries = root.optJSONObject("entries") ?: return -1
            val editor = activePrefs.edit()
            var count = 0
            val keys = entries.keys()
            while (keys.hasNext()) {
                val key = keys.next()
                if (key in SENSITIVE_BACKUP_KEYS) continue
                val item = entries.optJSONObject(key) ?: continue
                when (item.optString("t")) {
                    "bool" -> {
                        editor.putBoolean(key, item.optBoolean("v"))
                        count++
                    }
                    "int" -> {
                        editor.putInt(key, item.optInt("v"))
                        count++
                    }
                    "long" -> {
                        editor.putLong(key, item.optLong("v"))
                        count++
                    }
                    "float" -> {
                        editor.putFloat(key, item.optDouble("v").toFloat())
                        count++
                    }
                    "str" -> {
                        editor.putString(key, item.optString("v", ""))
                        count++
                    }
                    "set" -> {
                        val arr = item.optJSONArray("v") ?: JSONArray()
                        val set = linkedSetOf<String>()
                        for (i in 0 until arr.length()) {
                            set.add(arr.optString(i, ""))
                        }
                        editor.putStringSet(key, set)
                        count++
                    }
                }
            }
            editor.commit()
            loadFromPrefs(activePrefs)
            count
        } catch (_: Throwable) {
            -1
        }
    }

    @Deprecated("Use category-specific resets or resetPreferencesOnly() so user data is never wiped")
    fun resetToDefaults() = resetPreferencesOnly()

    fun updateMiniPlayer(title: String, subtitle: String, isPlaying: Boolean = true) {
        _miniPlayerTitle.value = title
        _miniPlayerSubtitle.value = subtitle
        _isMiniPlayerPlaying.value = isPlaying
        _showMiniPlayer.value = true
    }

    fun toggleMiniPlayerPlayback() {
        _isMiniPlayerPlaying.value = !_isMiniPlayerPlaying.value
    }

    fun dismissMiniPlayer() {
        _showMiniPlayer.value = false
    }

    fun setMiniPlayerActive(active: Boolean) {
        _showMiniPlayer.value = active
    }

    fun setMiniPlayerPlaying(playing: Boolean) {
        _isMiniPlayerPlaying.value = playing
    }

    fun setMiniPlayerTitle(title: String) {
        _miniPlayerTitle.value = title
    }

    fun setMiniPlayerSubtitle(subtitle: String) {
        _miniPlayerSubtitle.value = subtitle
    }

    fun setMiniPlayerProgress(progress: Float) {
        _miniPlayerProgress.value = progress
    }
}

data class NetworkBookmark(
    val id: String,
    val shareId: String,
    val title: String,
    val url: String,
    val isDirectory: Boolean,
    val createdAtMs: Long
)
