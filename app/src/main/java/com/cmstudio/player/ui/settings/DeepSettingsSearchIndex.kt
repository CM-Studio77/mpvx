package com.cmstudio.player.ui.settings

import android.app.ActivityManager
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.display.DisplayManager
import android.os.Build
import android.os.Process
import android.view.Display
import com.cmstudio.player.BuildConfig
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.Normalizer
import java.util.Locale
import kotlin.math.roundToInt

/**
 * Searchable entry indexing individual preferences and sections across all 18 MPVX Settings screens.
 */
data class IndexedSettingEntry(
    val key: String,
    val title: String,
    val description: String,
    val keywords: List<String>,
    val page: SettingsDestinationPage,
    val categoryName: String,
    val sectionIndex: Int = 0
)

data class DeviceSystemStats(
    val deviceModel: String,
    val androidVersion: String,
    val cpuAbis: String,
    val cpuCores: Int,
    val availRamBytes: Long,
    val totalRamBytes: Long,
    val openGlEsVersion: String,
    val vulkanSupported: Boolean,
    val screenSummary: String,
    val appSummary: String
) {
    fun formatReport(hardwareDecoders: Int = -1, softwareDecoders: Int = -1): String = buildString {
        appendLine("MPVX Player Hardware & System Report")
        appendLine("====================================")
        appendLine("App: $appSummary")
        appendLine("Device: $deviceModel")
        appendLine("OS: $androidVersion")
        appendLine("CPU ABIs: $cpuAbis ($cpuCores cores)")
        appendLine("Memory: ${formatBytesShort(availRamBytes)} free / ${formatBytesShort(totalRamBytes)} total")
        appendLine("Display: $screenSummary")
        appendLine("Graphics: OpenGL ES $openGlEsVersion | Vulkan: ${if (vulkanSupported) "Supported" else "Unavailable"}")
        if (hardwareDecoders >= 0 && softwareDecoders >= 0) {
            appendLine("Decoders: $hardwareDecoders HW / $softwareDecoders SW (MediaCodec)")
        }
        appendLine("Engine: AndroidX Media3 1.10.0 + libmpv + Bundled AV1/FFmpeg/IAMF/MPEG-H")
    }
}

private fun formatBytesShort(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val gb = bytes.toDouble() / (1024.0 * 1024.0 * 1024.0)
    if (gb >= 1.0) return String.format(Locale.US, "%.2f GB", gb)
    val mb = bytes.toDouble() / (1024.0 * 1024.0)
    return String.format(Locale.US, "%.0f MB", mb)
}

object DeepSettingsSearchIndex {

    val entries: List<IndexedSettingEntry> = listOf(
        // GENERAL (1-5)
        IndexedSettingEntry("remember_position", "Remember Playback Position", "Resume local videos and streams from saved timestamps", listOf("resume", "history", "progress", "timestamp"), SettingsDestinationPage.GENERAL, "General"),
        IndexedSettingEntry("auto_play_next", "Auto-Play Next Item", "Advance automatically through folder or playlist queues", listOf("queue", "next", "playlist", "continuous"), SettingsDestinationPage.GENERAL, "General"),
        IndexedSettingEntry("default_repeat_mode", "Default Repeat Behavior", "Off, Repeat One, or Repeat All playback loop", listOf("loop", "repeat", "one", "all"), SettingsDestinationPage.GENERAL, "General"),
        IndexedSettingEntry("watched_threshold", "Watched Threshold (%)", "Mark video watched after crossing completion percentage", listOf("watched", "95%", "complete", "seen"), SettingsDestinationPage.MEDIA_LIBRARY, "Media Library"),
        IndexedSettingEntry("quick_play_fab", "Quick Play FAB & Direct Play", "Show Quick Play button and direct-resume mode on Home", listOf("fab", "quick play", "resume", "button"), SettingsDestinationPage.MEDIA_LIBRARY, "Media Library"),

        // APPEARANCE (6-16)
        IndexedSettingEntry("dark_theme", "Dark Theme", "Use dark palette across all app screens", listOf("dark", "light", "night", "mode", "theme"), SettingsDestinationPage.APPEARANCE, "Appearance"),
        IndexedSettingEntry("amoled_black", "AMOLED Pure Black", "True #000000 surface contrast for OLED displays", listOf("amoled", "oled", "black", "battery"), SettingsDestinationPage.APPEARANCE, "Appearance"),
        IndexedSettingEntry("use_system_font", "Use System Font", "Inherit Android OEM typeface instead of sans-serif", listOf("font", "typeface", "typography", "system font"), SettingsDestinationPage.APPEARANCE, "Appearance"),
        IndexedSettingEntry("glass_effect", "Glass Effect", "Translucent frost treatment on floating dock and HUD", listOf("liquid glass", "blur", "frost", "translucent", "transparent"), SettingsDestinationPage.APPEARANCE, "Appearance"),
        IndexedSettingEntry("glass_opacity", "Glass Opacity", "Adjust frost alpha transparency for Liquid Glass", listOf("alpha", "opacity", "frost"), SettingsDestinationPage.APPEARANCE, "Appearance"),
        IndexedSettingEntry("frosted_sheets", "Frosted Player Sheets", "Translucent sheets inside the video player", listOf("sheets", "frosted", "modal"), SettingsDestinationPage.APPEARANCE, "Appearance"),
        IndexedSettingEntry("theme_presets", "Theme Presets & Accent Palettes", "Choose MPV-EX, Material Dynamic, Catppuccin, Nord, Gruvbox, Dracula", listOf("palette", "accent", "color", "material you", "dynamic", "catppuccin"), SettingsDestinationPage.APPEARANCE, "Appearance", 1),
        IndexedSettingEntry("dynamic_color", "Material You Dynamic Color", "Sync Android 12+ wallpaper colors to theme", listOf("wallpaper", "monet", "material"), SettingsDestinationPage.APPEARANCE, "Appearance", 1),
        IndexedSettingEntry("reset_appearance", "Reset Appearance Defaults", "Restore default MPV-EX theme without losing data", listOf("reset", "appearance", "default"), SettingsDestinationPage.APPEARANCE, "Appearance", 2),

        // NAVIGATION (15-20)
        IndexedSettingEntry("nav_tabs_visibility", "Show or Hide Navigation Tabs", "Toggle Music, Recents, Playlists, Streams, Network, Vault tabs", listOf("tabs", "bottom bar", "hide", "show"), SettingsDestinationPage.NAVIGATION, "Navigation"),
        IndexedSettingEntry("nav_tabs_order", "Reorder Bottom Navigation Tabs", "Move navigation tabs left or right", listOf("reorder", "move", "sort tabs"), SettingsDestinationPage.NAVIGATION, "Navigation"),
        IndexedSettingEntry("nav_stealth_vault", "Stealth Vault Tab Visibility", "Hide Vault from bottom bar while keeping PIN access", listOf("stealth", "privacy", "hidden vault"), SettingsDestinationPage.NAVIGATION, "Navigation"),
        IndexedSettingEntry("music_tabs_custom", "Music Tab Customization", "Reorder or hide Songs, Albums, Artists, Playlists, Folders, Queue", listOf("music tabs", "pager", "playlists"), SettingsDestinationPage.NAVIGATION, "Navigation"),
        IndexedSettingEntry("reset_navigation", "Restore Default Navigation", "Reset all bottom navigation tabs and order", listOf("reset", "nav", "tabs"), SettingsDestinationPage.NAVIGATION, "Navigation"),

        // VIDEO PLAYER HUD & LAYOUT EDITOR (21-38)
        IndexedSettingEntry("seekbar_style", "Seekbar Style (Live Preview)", "Switch between Standard, Expressive, Wavy, Thick, Slim & Normal", listOf("seekbar", "wavy", "thick", "slim", "timeline", "progress bar"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 0),
        IndexedSettingEntry("seekbar_container", "Show Seekbar Outer Container", "Toggle pill background around player timeline", listOf("container", "pill", "seekbar", "box"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("hud_theme", "HUD Visual Style", "Classic, Glass, or Glossy player button chrome", listOf("classic", "glass", "glossy", "controls theme", "hud"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("hide_hud_btn_bg", "Hide Player Buttons Background", "Render borderless icons without circular backing", listOf("transparent buttons", "hide background", "clean hud"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("force_dark_hud_bg", "Force Dark HUD Button Background", "Lock dark backing on HUD icons regardless of theme", listOf("dark buttons", "contrast", "hud"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("player_btn_size", "HUD Button Size", "Scale player control buttons between Compact, Medium & Large", listOf("button size", "dp", "large icons"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("portrait_hud_pos", "Portrait Controls Placement", "Place center transport controls in Center or Below Seekbar", listOf("portrait", "below seekbar", "center", "layout"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("clock_format", "HUD Clock Format", "Toggle 12-hour AM/PM or 24-hour clock in player top bar", listOf("time", "12h", "24h", "clock", "status"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("hud_timeout", "HUD Auto-Hide Timeout", "Configure seconds before player controls fade away", listOf("disappear", "timeout", "auto hide", "fade"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("custom_skip_step", "Custom Skip Duration (Intro/ED)", "Set custom fast-forward skip seconds (e.g. 90s anime OP)", listOf("anime", "opening", "intro", "85s", "90s", "skip"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("hud_telemetry", "Show HUD Telemetry & Live Network", "Display battery %, resolution, codec badge & speed chip", listOf("telemetry", "codec badge", "battery", "fps"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("controls_drawer", "Right-Edge Controls Drawer", "Show slide-out drawer for quick tools & speed", listOf("drawer", "edge", "panel"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("ambient_aura", "Ambient Aura Background", "Sample video colors to cast soft reactive glow behind bars", listOf("ambient", "glow", "aura", "ambilight"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("system_status_bar", "Show System Status Bar in Player", "Display Android status icons while HUD is visible", listOf("status bar", "notch", "system bars"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("system_nav_bar", "Show System Navigation Bar", "Keep system navigation pill visible in player", listOf("nav bar", "system bars", "bottom"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("safe_area_cutout", "Display Cutout Safe Area", "Respect notch/punch-hole margins inside player window", listOf("notch", "cutout", "safe area"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("loading_indicator", "Show Buffering Spinner", "Display circular loader when buffering stream or file", listOf("spinner", "loading", "buffer circle"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 1),
        IndexedSettingEntry("hud_slot_editor", "Reorderable HUD Control Editor", "Add, remove & reorder buttons across Top Right, Bottom Left, Bottom Right & Portrait", listOf("editor", "buttons", "lock", "rotate", "speed", "ab loop", "screenshot"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player", 2),

        // PLAYBACK (39-46)
        IndexedSettingEntry("default_speed", "Default Playback Speed", "Configure starting playback rate (0.5x to 2.0x+)", listOf("speed", "tempo", "1.5x", "2.0x", "rate"), SettingsDestinationPage.PLAYBACK, "Playback"),
        IndexedSettingEntry("pitch_correction", "Audio Pitch Correction", "Maintain natural voice pitch when changing playback speed", listOf("pitch", "speed", "tone", "sonic"), SettingsDestinationPage.PLAYBACK, "Playback"),
        IndexedSettingEntry("fast_seek", "Fast Seek (Nearest Keyframe)", "Scrub rapidly to keyframes or toggle frame-exact precision", listOf("keyframe", "exact seek", "scrubbing"), SettingsDestinationPage.PLAYBACK, "Playback"),
        IndexedSettingEntry("seek_interval", "HUD Rewind / Forward Interval", "Customize skip jump seconds on rewind/forward buttons", listOf("5s", "10s", "15s", "30s", "skip"), SettingsDestinationPage.PLAYBACK, "Playback"),
        IndexedSettingEntry("shuffle_queue", "Queue Shuffle Mode", "Shuffle next/previous order in video & audio queues", listOf("shuffle", "random", "queue"), SettingsDestinationPage.PLAYBACK, "Playback"),
        IndexedSettingEntry("ab_loop", "A-B Segment Loop", "Repeat section between Pin A and Pin B inside player", listOf("loop", "segment", "ab", "repeat section"), SettingsDestinationPage.PLAYBACK, "Playback"),
        IndexedSettingEntry("sleep_timer", "Sleep Timer (Fade & End of Video)", "Fade out volume over 5s and pause playback automatically", listOf("timer", "bedtime", "sleep", "fade"), SettingsDestinationPage.PLAYBACK, "Playback"),
        IndexedSettingEntry("frame_step", "Frame-by-Frame Stepping", "Step backward and forward single video frames", listOf("frame step", "next frame", "previous frame"), SettingsDestinationPage.PLAYBACK, "Playback"),

        // VIDEO RENDERING (47-53)
        IndexedSettingEntry("video_surface", "Video Output Surface (SurfaceView / TextureView)", "Choose SurfaceView for HDR/battery or TextureView for mirror/flip", listOf("surfaceview", "textureview", "mirror", "flip", "render"), SettingsDestinationPage.VIDEO_RENDERING, "Video Rendering"),
        IndexedSettingEntry("default_aspect", "Default Aspect Ratio", "Fit, Fill, Zoom, Crop, Stretch, 16:9, 4:3, 21:9", listOf("aspect ratio", "zoom", "crop", "16:9", "stretch"), SettingsDestinationPage.VIDEO_RENDERING, "Video Rendering"),
        IndexedSettingEntry("remember_aspect_per_video", "Remember Aspect Ratio Per Video", "Restore last Fit/Crop zoom ratio per video file", listOf("remember aspect", "per video", "zoom"), SettingsDestinationPage.VIDEO_RENDERING, "Video Rendering"),
        IndexedSettingEntry("smooth_motion", "Match Video Frame Rate (120Hz)", "Sync Android 11+ Surface refresh rate with video fps", listOf("fps", "refresh rate", "120hz", "24hz", "judder"), SettingsDestinationPage.VIDEO_RENDERING, "Video Rendering"),
        IndexedSettingEntry("dv7_fallback", "Map Dolby Vision Profile 7 to HEVC", "Fallback UHDBD DV7 layer cleanly on non-DV7 hardware", listOf("dolby vision", "dv", "hevc", "hdr", "profile 7"), SettingsDestinationPage.VIDEO_RENDERING, "Video Rendering"),
        IndexedSettingEntry("mirror_flip", "Video Mirror & Vertical Flip", "Flip video horizontally or vertically using TextureView", listOf("mirror", "flip", "rotate", "invert"), SettingsDestinationPage.VIDEO_RENDERING, "Video Rendering"),
        IndexedSettingEntry("screenshot_frame", "Instant Frame Screenshot", "Capture lossless PNG video frame via PixelCopy", listOf("screenshot", "capture", "snapshot", "png"), SettingsDestinationPage.VIDEO_RENDERING, "Video Rendering"),

        // DECODER & ENGINE (54-60)
        IndexedSettingEntry("media3_engine", "AndroidX Media3 1.10.0 Engine", "Inspect active ExoPlayer engine & FFmpeg/AV1/IAMF extensions", listOf("media3", "exoplayer", "ffmpeg", "av1", "dav1d", "iamf", "mpegh"), SettingsDestinationPage.DECODER_ENGINE, "Decoder & Engine"),
        IndexedSettingEntry("decoder_priority", "Decoder Priority (Auto / HW+ / SW)", "Prefer MediaCodec hardware decoders or FFmpeg software fallback", listOf("hardware", "software", "hw", "sw", "ffmpeg", "mediacodec"), SettingsDestinationPage.DECODER_ENGINE, "Decoder & Engine"),
        IndexedSettingEntry("loadcontrol_buffers", "LoadControl Buffers (Min / Max / Start)", "Tune ExoPlayer playback and rebuffer memory watermarks", listOf("buffer", "loadcontrol", "streaming", "rebuffering", "cache"), SettingsDestinationPage.DECODER_ENGINE, "Decoder & Engine"),
        IndexedSettingEntry("codec_inspector", "Hardware Codec Inspector", "Inspect supported device MediaCodec profiles, HDR & max resolution", listOf("codecs", "hevc", "av1", "h264", "inspector", "profiles"), SettingsDestinationPage.DECODER_ENGINE, "Decoder & Engine"),
        IndexedSettingEntry("dolby_routing", "Dolby Vision Safe Routing", "Automatic Media3 DV profile compatibility routing", listOf("dolby", "hdr", "routing"), SettingsDestinationPage.DECODER_ENGINE, "Decoder & Engine"),
        IndexedSettingEntry("hls_dash_routing", "HLS, DASH & RTSP Adaptive Streaming", "Adaptive bitrate selection and stream manifest parsing", listOf("hls", "dash", "rtsp", "m3u8", "mpd"), SettingsDestinationPage.DECODER_ENGINE, "Decoder & Engine"),

        // AUDIO (61-71)
        IndexedSettingEntry("audio_language", "Preferred Audio Track Language", "Auto-select English, Hindi, Japanese, Korean, Spanish audio", listOf("language", "dub", "japanese", "hindi", "english", "track"), SettingsDestinationPage.AUDIO, "Audio"),
        IndexedSettingEntry("audio_boost", "Default Volume Boost (Up to 200%)", "Amplify quiet audio using LoudnessEnhancer", listOf("loudness", "boost", "gain", "amplify", "200%"), SettingsDestinationPage.AUDIO, "Audio"),
        IndexedSettingEntry("audio_offload", "Audio Hardware Offload", "Offload supported compressed audio to DSP for battery savings", listOf("offload", "dsp", "battery", "pcm"), SettingsDestinationPage.AUDIO, "Audio"),
        IndexedSettingEntry("night_compressor", "Night Mode Limiter (Compressor)", "Tame loud explosions using DynamicsProcessing", listOf("night mode", "compressor", "drc", "loudness", "limiter"), SettingsDestinationPage.AUDIO, "Audio"),
        IndexedSettingEntry("audio_sync_delay", "Audio Sync Delay Offset (ms)", "Shift audio timing forward or backward up to +-5000ms", listOf("delay", "sync", "lip sync", "offset", "ms"), SettingsDestinationPage.AUDIO, "Audio"),
        IndexedSettingEntry("equalizer_presets", "11-Preset Equalizer (Cinema, Bass, Rock...)", "Select built-in acoustic curves in video & music players", listOf("eq", "equalizer", "bass", "cinema", "vocal", "flat"), SettingsDestinationPage.AUDIO, "Audio", 1),
        IndexedSettingEntry("equalizer_5band", "Custom 5-Band Parametric Equalizer", "Fine-tune 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz dB sliders", listOf("5 band", "60hz", "sliders", "eq", "equalizer"), SettingsDestinationPage.AUDIO, "Audio", 1),
        IndexedSettingEntry("audio_channels", "Audio Channel Mix (Auto / Mono / Stereo / Reverse)", "Downmix surround to Mono or swap L/R channels live", listOf("mono", "stereo", "reverse", "left right", "channels"), SettingsDestinationPage.AUDIO, "Audio", 1),
        IndexedSettingEntry("music_library_sort", "Music Track Sorting & Grid Art Size", "Sort audio library by Title/Artist/Album & size artwork", listOf("music", "album art", "sort", "audio library"), SettingsDestinationPage.AUDIO, "Audio"),

        // SUBTITLES (70-82)
        IndexedSettingEntry("subtitles_enable", "Enable Subtitles by Default", "Load embedded and matching sidecar SRT/ASS/VTT tracks", listOf("captions", "cc", "srt", "ass", "vtt", "auto load"), SettingsDestinationPage.SUBTITLES, "Subtitles"),
        IndexedSettingEntry("subtitle_language", "Preferred Subtitle Language", "Default caption language filter", listOf("language", "english", "sub", "cc"), SettingsDestinationPage.SUBTITLES, "Subtitles"),
        IndexedSettingEntry("subtitle_font_size", "Subtitle Font Size (sp)", "Scale primary subtitle text height", listOf("size", "sp", "big", "small", "captions"), SettingsDestinationPage.SUBTITLES, "Subtitles"),
        IndexedSettingEntry("subtitle_bg_opacity", "Subtitle Background Box Opacity", "Adjust translucent caption backdrop shading", listOf("background", "box", "opacity", "black"), SettingsDestinationPage.SUBTITLES, "Subtitles"),
        IndexedSettingEntry("subtitle_bottom_pad", "Subtitle Bottom Offset (dp)", "Adjust vertical distance above bottom screen edge", listOf("offset", "position", "margin", "padding", "bottom"), SettingsDestinationPage.SUBTITLES, "Subtitles"),
        IndexedSettingEntry("subtitle_edge_style", "Subtitle Edge Style", "None, Outline, Drop Shadow, Raised, Depressed", listOf("outline", "shadow", "border", "edge"), SettingsDestinationPage.SUBTITLES, "Subtitles"),
        IndexedSettingEntry("subtitle_bold", "Bold Subtitle Text", "Render caption cues with bold font weight", listOf("bold", "weight", "font"), SettingsDestinationPage.SUBTITLES, "Subtitles"),
        IndexedSettingEntry("subtitle_italic", "Italic Subtitle Style", "Render caption cues in italic style", listOf("italic", "slant", "font"), SettingsDestinationPage.SUBTITLES, "Subtitles", 1),
        IndexedSettingEntry("subtitle_alignment", "Subtitle Alignment (Left / Center / Right)", "Align caption text horizontally", listOf("align", "center", "left", "right"), SettingsDestinationPage.SUBTITLES, "Subtitles", 1),
        IndexedSettingEntry("subtitle_speed", "Subtitle Speed Scale (0.1x to 10.0x)", "Fix drifting FPS mismatch captions cleanly", listOf("speed", "drift", "23.976", "25fps", "timing"), SettingsDestinationPage.SUBTITLES, "Subtitles", 1),
        IndexedSettingEntry("subtitle_delay", "Subtitle Timing Delay & Tap-to-Sync", "Shift subtitle timing or use Heard/Saw stopwatch", listOf("sync", "delay", "ms", "stopwatch", "heard", "saw"), SettingsDestinationPage.SUBTITLES, "Subtitles"),
        IndexedSettingEntry("dual_subtitles", "Dual Subtitles Mode", "Display primary and secondary subtitle tracks together", listOf("dual", "bilingual", "two subtitles", "secondary"), SettingsDestinationPage.SUBTITLES, "Subtitles", 1),
        IndexedSettingEntry("custom_sub_font", "Custom .TTF / .OTF Subtitle Font", "Import user font file from storage via SAF picker", listOf("ttf", "otf", "font", "import", "typeface"), SettingsDestinationPage.SUBTITLES, "Subtitles", 1),

        // GESTURES (83-97)
        IndexedSettingEntry("vol_bright_swap", "Volume on Right Side (L/R Swap)", "Choose Left or Right vertical edge for Volume vs Brightness", listOf("volume", "brightness", "left", "right", "swap", "swipe"), SettingsDestinationPage.GESTURES, "Gestures"),
        IndexedSettingEntry("vol_bright_enable", "Vertical Volume & Brightness Swipes", "Toggle edge swipe gestures independently", listOf("vertical swipe", "volume gesture", "brightness gesture"), SettingsDestinationPage.GESTURES, "Gestures"),
        IndexedSettingEntry("remember_brightness", "Remember Player Brightness", "Persist last adjusted screen brightness across videos", listOf("brightness", "remember", "save brightness"), SettingsDestinationPage.GESTURES, "Gestures"),
        IndexedSettingEntry("horizontal_seek", "Horizontal Seek Scrub & Sensitivity", "Swipe left/right to scrub with adjustable multiplier", listOf("scrub", "horizontal swipe", "sensitivity"), SettingsDestinationPage.GESTURES, "Gestures"),
        IndexedSettingEntry("double_tap_step", "3-Zone Double-Tap & Edge Width %", "Double-tap left/right to seek, center to Play/Pause", listOf("double tap", "zones", "10s", "seek step"), SettingsDestinationPage.GESTURES, "Gestures"),
        IndexedSettingEntry("single_tap_pause", "Single-Tap Toggles Play/Pause", "Pause or resume immediately on single screen tap", listOf("single tap", "pause", "tap"), SettingsDestinationPage.GESTURES, "Gestures"),
        IndexedSettingEntry("hold_speed_mult", "Hold Speed & Swipe Up Lock", "Press & hold for 2x–4x playback; swipe up to lock", listOf("long press", "2x", "hold speed", "swipe up"), SettingsDestinationPage.GESTURES, "Gestures"),
        IndexedSettingEntry("swipe_up_queue", "Swipe Up Center to Open Queue", "Swipe upward in center to open up-next playlist sheet", listOf("swipe up", "playlist", "queue"), SettingsDestinationPage.GESTURES, "Gestures"),
        IndexedSettingEntry("pinch_zoom", "Two-Finger Pinch to Zoom & Pan", "Scale & pan video viewport up to 4x smoothly", listOf("pinch", "zoom", "pan", "multitouch"), SettingsDestinationPage.GESTURES, "Gestures"),
        IndexedSettingEntry("gesture_overlays", "Seek Card & Side Meter Overlays", "Show/hide seek preview delta card and volume/brightness bars", listOf("overlay", "hud", "bars", "delta"), SettingsDestinationPage.GESTURES, "Gestures"),
        IndexedSettingEntry("haptic_feedback", "Gesture & Control Haptic Feedback", "Subtle vibration confirmation on seeks & actions", listOf("haptic", "vibrate", "vibration"), SettingsDestinationPage.GESTURES, "Gestures"),

        // SCREEN ORIENTATION & BACKGROUND (94-99)
        IndexedSettingEntry("screen_orientation", "8 Screen Orientation Modes", "Auto, Landscape, Portrait, Reverse Landscape/Portrait, Sensor, Match Video", listOf("orientation", "rotate", "landscape", "portrait", "sensor", "match video"), SettingsDestinationPage.SCREEN_ORIENTATION, "Screen & Orientation"),
        IndexedSettingEntry("keep_screen_on", "Keep Screen On During Playback", "Prevent screen timeout while video plays", listOf("wake lock", "screen on", "sleep"), SettingsDestinationPage.SCREEN_ORIENTATION, "Screen & Orientation"),
        IndexedSettingEntry("bg_playback", "Continue Video Audio in Background", "Keep playing audio via MediaPlaybackService notification when leaving app", listOf("background", "audio", "notification", "minimized"), SettingsDestinationPage.BACKGROUND_PLAYBACK, "Background Playback"),
        IndexedSettingEntry("auto_pip", "Auto Picture-in-Picture (PiP)", "Enter floating Android PiP window when pressing Home during playback", listOf("pip", "picture in picture", "floating", "home"), SettingsDestinationPage.BACKGROUND_PLAYBACK, "Background Playback"),

        // NETWORK & CASTING (98-103)
        IndexedSettingEntry("wifi_only_network", "Browse Network Shares on Wi-Fi Only", "Block LAN/WebDAV/M3U browsing on metered cellular connections", listOf("wifi", "metered", "cellular", "mobile data"), SettingsDestinationPage.NETWORK_STREAMING, "Network & Streaming"),
        IndexedSettingEntry("network_timeout", "HTTP / WebDAV Request Timeout", "Configure network connection timeout seconds", listOf("timeout", "http", "webdav", "m3u", "seconds"), SettingsDestinationPage.NETWORK_STREAMING, "Network & Streaming"),
        IndexedSettingEntry("network_bookmarks", "Network Folder Bookmarks & Headers", "Bookmark HTTP/WebDAV folders & inject auth headers during playback", listOf("bookmarks", "star", "basic auth", "headers", "user agent"), SettingsDestinationPage.NETWORK_STREAMING, "Network & Streaming"),
        IndexedSettingEntry("casting_info", "Android System Cast & External Display", "Launch Android Cast settings & check mirroring limits", listOf("cast", "chromecast", "tv", "mirroring", "dlna"), SettingsDestinationPage.CASTING, "Casting"),

        // MEDIA LIBRARY (102-109)
        IndexedSettingEntry("folder_organization", "Folder View Mode (Flat / Directory Tree / All Videos)", "Switch between flat folders and nested breadcrumb directory tree", listOf("tree", "breadcrumbs", "folders", "browser"), SettingsDestinationPage.MEDIA_LIBRARY, "Media Library"),
        IndexedSettingEntry("video_layout_cols", "Grid Columns (Portrait & Landscape)", "Configure separate folder and video grid column counts", listOf("grid", "columns", "list", "portrait", "landscape"), SettingsDestinationPage.MEDIA_LIBRARY, "Media Library"),
        IndexedSettingEntry("dual_pane_tablet", "Tablet Dual-Pane Home Layout", "Side-by-side folders & videos split on wide screens", listOf("tablet", "dual pane", "split screen"), SettingsDestinationPage.MEDIA_LIBRARY, "Media Library"),
        IndexedSettingEntry("card_fields", "Media Card Fields & Badges", "Toggle Sizes, Paths, Date, Extension, Resolution, Progress Bar & Full Names", listOf("badges", "1080p", "4k", "new badge", "full names", "size"), SettingsDestinationPage.MEDIA_LIBRARY, "Media Library"),
        IndexedSettingEntry("excluded_folders", "Excluded Folders Manager", "Restore folders hidden from Home media scan", listOf("hide folder", "exclude", "nomedia", "restore"), SettingsDestinationPage.MEDIA_LIBRARY, "Media Library", 1),

        // DUAL-ENGINE MPV, SHADERS, WALLPAPER & OVERLAYS (110+)
        IndexedSettingEntry("default_engine_mode", "Playback Engine (Auto / MPV / Native ExoPlayer)", "Switch default backend between smart Auto, libmpv shaders, and ExoPlayer ASS", listOf("mpv", "libmpv", "exo", "exoplayer", "engine", "auto"), SettingsDestinationPage.DECODER_ENGINE, "Decoder & Engine"),
        IndexedSettingEntry("mpv_vulkan_gpunext", "MPV Profile, gpu-next & Vulkan Backend", "Configure libmpv quality profile, gpu-next renderer and Vulkan GPU API", listOf("vulkan", "opengl", "gpu-next", "mpv", "profile", "yuv420p"), SettingsDestinationPage.DECODER_ENGINE, "Decoder & Engine"),
        IndexedSettingEntry("mpv_anime4k", "Anime4K GLSL Shaders (MIT)", "Enable real-time Anime4K restoration and upscaling modes A/B/C/AA on MPV", listOf("anime4k", "shader", "glsl", "upscale", "hq"), SettingsDestinationPage.DECODER_ENGINE, "Decoder & Engine"),
        IndexedSettingEntry("mpv_debanding", "Video Debanding Filter (GPU / CPU)", "Smooth color banding gradients with iterations, threshold, range & grain tuning", listOf("deband", "banding", "grain", "filter", "mpv"), SettingsDestinationPage.DECODER_ENGINE, "Decoder & Engine"),
        IndexedSettingEntry("custom_wallpaper", "Custom Photo & Video Wallpaper", "Pick live looping video or photo backdrop with blur, brightness & saturation sliders", listOf("wallpaper", "photo", "video theme", "blur", "saturation", "backdrop"), SettingsDestinationPage.APPEARANCE, "Appearance"),
        IndexedSettingEntry("overlay_toggles", "Overlay Button Visibility (8 Toggles)", "Toggle Rotation, Aspect, Speed, Decoder, Shuffle, Loop, PiP & Screenshot overlays", listOf("overlay", "visibility", "pip", "rotation", "screenshot"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player"),
        IndexedSettingEntry("screenshot_prefs", "Screenshot Format, Quality & Folder", "Choose PNG, JPEG or WEBP capture format, compression & album folder", listOf("screenshot", "png", "jpeg", "webp", "quality", "folder"), SettingsDestinationPage.VIDEO_PLAYER, "Video Player"),
        IndexedSettingEntry("nav_home_hide", "Hide Any Nav Tab (Including Home) & Music Tabs", "Show, hide, reorder ALL bottom tabs & Music sub-tabs", listOf("hide home", "nav tabs", "music tabs", "quick play fab"), SettingsDestinationPage.NAVIGATION, "Navigation"),

        // ADVANCED, BACKUP, ABOUT & LICENSES (107-115)
        IndexedSettingEntry("cache_clear", "Clear Temporary & Subtitle Cache", "Free storage space used by cached subtitles and thumbnails", listOf("cache", "storage", "clean", "mb"), SettingsDestinationPage.ADVANCED, "Advanced"),
        IndexedSettingEntry("clear_history", "Clear Playback History", "Erase Recents and saved timestamps without deleting media files", listOf("clear history", "recents", "timestamps"), SettingsDestinationPage.ADVANCED, "Advanced"),
        IndexedSettingEntry("backup_export", "Export Settings Backup (JSON)", "Save all app preferences, playlists & bookmarks via Android Files", listOf("backup", "export", "json", "save settings", "saf"), SettingsDestinationPage.ADVANCED, "Advanced", 1),
        IndexedSettingEntry("backup_import", "Import / Restore Settings Backup", "Restore exported JSON backup without exposing Vault PIN hashes", listOf("restore", "import", "backup", "json", "load"), SettingsDestinationPage.ADVANCED, "Advanced", 1),
        IndexedSettingEntry("reset_prefs_only", "Reset Settings Preferences Only", "Reset UI preferences while keeping Vault PIN, playlists, favorites & history", listOf("reset", "factory", "defaults"), SettingsDestinationPage.ADVANCED, "Advanced"),
        IndexedSettingEntry("system_stats", "Hardware System Stats & Copy Report", "Inspect Android API, CPU ABI, Cores, RAM, OpenGL ES, Vulkan & Decoders", listOf("system stats", "ram", "cpu", "gpu", "opengl", "vulkan", "hardware"), SettingsDestinationPage.ABOUT, "About", 1),
        IndexedSettingEntry("diagnostics_logs", "App Diagnostics & Crash Logs", "View in-app logcat diagnostics and copy troubleshoot report", listOf("logs", "logcat", "crash", "debug", "diagnostics"), SettingsDestinationPage.ABOUT, "About", 1),
        IndexedSettingEntry("about_mpvx", "About MPVX Player", "Creative Mind Studio • Version, Application ID, libmpv & GNU GPL-3.0", listOf("version", "build", "package", "mpvx", "gpl", "creative mind studio"), SettingsDestinationPage.ABOUT, "About", 0),
        IndexedSettingEntry("extra_features", "Extra Features", "Smart collections, watch stats, library health, quality filters & activity log", listOf("smart collections", "watch stats", "diagnostics", "library health", "activity log"), SettingsDestinationPage.EXTRA_FEATURES, "Extra Features"),
        IndexedSettingEntry("supported_codecs", "Supported Codecs Explorer", "Hardware & software HEVC, AV1, VP9, Dolby Vision, Opus & FLAC decoder inspector", listOf("codecs", "hardware", "software", "hevc", "av1", "vp9", "hdr", "mediacodec"), SettingsDestinationPage.SUPPORTED_CODECS, "Supported Codecs"),
        IndexedSettingEntry("seekbar_styles_extra", "Segmented, Dual Glow & Pulse Seekbars", "Choose from 9 live animated player progress bar styles", listOf("seekbar", "segmented", "dual glow", "pulse bar", "wavy", "expressive"), SettingsDestinationPage.VIDEO_PLAYER, "Player Layout"),
        IndexedSettingEntry("screenshot_subtitles", "Include Subtitles in Screenshots", "Burn active captions & custom filenames into captured screenshots", listOf("screenshot", "subtitles", "png compression", "template"), SettingsDestinationPage.VIDEO_PLAYER, "Player Layout"),
        IndexedSettingEntry("playback_lifecycle_extras", "Close After End & Autoplay on Unlock", "Configure end-of-playback closing, aspect per video & unlock resume", listOf("close", "aspect", "unlock", "autoplay", "remember aspect"), SettingsDestinationPage.PLAYBACK, "Playback"),
        IndexedSettingEntry("subtitle_ass_override", "Override ASS/SSA & Scale by Window", "Configure sub-ass-override, window scaling & SAF fonts folder", listOf("ass", "ssa", "override", "scale by window", "fonts folder", "swipe subtitles"), SettingsDestinationPage.SUBTITLES, "Subtitles"),
        IndexedSettingEntry("audio_normalization_cap", "Volume Normalization & Boost Cap", "Level quiet/loud audio (dynaudnorm / DynamicsProcessing) across MPV & ExoPlayer", listOf("normalization", "dynaudnorm", "volume cap", "boost"), SettingsDestinationPage.AUDIO, "Audio"),
        IndexedSettingEntry("audio_standby_viz", "Audio Standby Delay, Visualizer & Orientation", "Idle release timeout, visualizer style & portrait/landscape music lock", listOf("standby", "visualizer", "music orientation", "wave", "bars"), SettingsDestinationPage.AUDIO, "Audio"),
        IndexedSettingEntry("intro_outro_skip", "Intro & Outro Detection (Chapters / IntroDB / AniSkip)", "Configure opening/ending markers, custom keywords & auto-skip", listOf("intro", "outro", "introdb", "theintrodb", "aniskip", "iskip", "auto skip", "opening", "ending"), SettingsDestinationPage.PLAYBACK, "Playback"),
        IndexedSettingEntry("online_sub_search", "Online Subtitle Search (Wyzie / SubHub)", "Configure online subtitle providers, preferred search language & optional key", listOf("online subtitles", "wyzie", "subhub", "subdl", "subtitle search", "download subtitles"), SettingsDestinationPage.SUBTITLES, "Subtitles"),
        IndexedSettingEntry("mpv_conf_editor", "MPV Configuration (mpv.conf)", "Safe validated mpv.conf editor, storage location & option ownership", listOf("mpv.conf", "config", "interpolation", "scale", "cscale", "hwdec"), SettingsDestinationPage.ADVANCED, "Advanced"),
        IndexedSettingEntry("hidden_saf_folders", "Hidden Media Folders & SAF Permissions", "Show dot-folders and manage Storage Access Framework directories", listOf("hidden folders", "nomedia", "saf", "permissions", "sd card", "usb"), SettingsDestinationPage.MEDIA_LIBRARY, "Media Library"),
        IndexedSettingEntry("media_server_tabs", "Media Server Destinations (Jellyfin, Navidrome, Audiobookshelf)", "Show or hide connected media server tabs in navigation", listOf("jellyfin", "navidrome", "audiobookshelf", "audiobooks", "media server", "smb", "ftp", "sftp"), SettingsDestinationPage.NAVIGATION, "Navigation"),
        IndexedSettingEntry("system_specs_about", "System Specifications & MPV/Media3 Info", "Inspect libmpv, ExoPlayer, libass, Anime4K shaders & device kernel", listOf("system", "kernel", "abi", "ram", "mpv", "about", "license"), SettingsDestinationPage.ABOUT, "About & System"),
        IndexedSettingEntry("open_source_licenses", "Open-Source Licenses Viewer", "Inspect bundled GPL-3.0, LGPL-2.1, Apache-2.0, MIT & ISC notices", listOf("license", "legal", "third party", "notices", "ffmpeg", "libmpv", "libass", "anime4k"), SettingsDestinationPage.OPEN_SOURCE_LICENSES, "Licenses")
    )

    internal fun normalizeForSearch(input: String): String {
        val decomposed = Normalizer.normalize(input.trim(), Normalizer.Form.NFD)
        return buildString(decomposed.length) {
            for (ch in decomposed) {
                val type = Character.getType(ch)
                if (type != Character.NON_SPACING_MARK.toInt() && type != Character.COMBINING_SPACING_MARK.toInt()) {
                    append(ch.lowercaseChar())
                }
            }
        }
    }

    fun search(query: String): List<IndexedSettingEntry> {
        val normQuery = normalizeForSearch(query)
        if (normQuery.isBlank()) return emptyList()
        val tokens = normQuery.split(Regex("\\s+")).filter { it.isNotEmpty() }

        return entries.mapNotNull { entry ->
            val titleNorm = normalizeForSearch(entry.title)
            val descNorm = normalizeForSearch(entry.description)
            val catNorm = normalizeForSearch(entry.categoryName)
            val kwNorm = entry.keywords.map(::normalizeForSearch)

            val allTokensMatch = tokens.all { tok ->
                titleNorm.contains(tok) || descNorm.contains(tok) || catNorm.contains(tok) || kwNorm.any { it.contains(tok) }
            }
            if (!allTokensMatch) return@mapNotNull null

            var score = 0
            if (titleNorm == normQuery) score += 1000
            if (titleNorm.startsWith(normQuery)) score += 400
            if (titleNorm.contains(normQuery)) score += 200
            if (kwNorm.any { it == normQuery }) score += 180
            if (kwNorm.any { it.contains(normQuery) }) score += 100
            if (descNorm.contains(normQuery)) score += 60
            if (catNorm.contains(normQuery)) score += 40
            tokens.forEach { tok ->
                if (titleNorm.contains(tok)) score += 50
                if (kwNorm.any { it.contains(tok) }) score += 30
            }
            score to entry
        }
            .sortedByDescending { it.first }
            .map { it.second }
    }

    fun collectSystemStats(context: Context): DeviceSystemStats {
        val app = context.applicationContext
        val am = app.getSystemService(Context.ACTIVITY_SERVICE) as? ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am?.getMemoryInfo(memInfo)

        val glVersion = am?.deviceConfigurationInfo?.glEsVersion ?: "Unknown"
        val vulkan = app.packageManager.hasSystemFeature(PackageManager.FEATURE_VULKAN_HARDWARE_VERSION)

        val dm = app.resources.displayMetrics
        val displayMgr = app.getSystemService(Context.DISPLAY_SERVICE) as? DisplayManager
        val refreshHz = displayMgr?.getDisplay(Display.DEFAULT_DISPLAY)?.refreshRate ?: 60f

        val mfr = Build.MANUFACTURER.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(Locale.US) else it.toString()
        }
        return DeviceSystemStats(
            deviceModel = "$mfr ${Build.MODEL} (${Build.DEVICE})",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            cpuAbis = Build.SUPPORTED_ABIS.joinToString(", ").ifBlank { "Unknown" },
            cpuCores = Runtime.getRuntime().availableProcessors().coerceAtLeast(1),
            availRamBytes = memInfo.availMem,
            totalRamBytes = memInfo.totalMem,
            openGlEsVersion = glVersion,
            vulkanSupported = vulkan,
            screenSummary = "${dm.widthPixels}x${dm.heightPixels} @ ${refreshHz.roundToInt()}Hz (${dm.densityDpi} dpi)",
            appSummary = "MPVX Player ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE}) - ${BuildConfig.APPLICATION_ID}"
        )
    }

    fun collectOwnProcessLogs(maxLines: Int = 100): String {
        return try {
            val pid = Process.myPid()
            val proc = ProcessBuilder("logcat", "-d", "--pid=$pid", "-t", maxLines.toString(), "-v", "time", "*:I")
                .redirectErrorStream(true)
                .start()
            val lines = BufferedReader(InputStreamReader(proc.inputStream)).use { reader ->
                reader.readLines()
            }
            proc.waitFor()
            if (lines.isEmpty()) {
                "No process log lines captured yet for PID $pid."
            } else {
                lines.takeLast(maxLines).joinToString("\n")
            }
        } catch (e: Throwable) {
            "Process log output unavailable on this device (${e.localizedMessage ?: "restricted"})."
        }
    }
}
