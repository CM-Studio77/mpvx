package com.cmstudio.player.ui.player

import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.displayCutoutPadding
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.QueueMusic
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bedtime
import androidx.compose.material.icons.rounded.Bookmarks
import androidx.compose.material.icons.rounded.Flip
import androidx.compose.material.icons.rounded.FlipCameraAndroid
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.HighQuality
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Timer
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.automirrored.rounded.VolumeMute
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.AspectRatio
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.CameraAlt
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.DeveloperBoard
import androidx.compose.material.icons.rounded.ErrorOutline
import androidx.compose.material.icons.rounded.FastForward
import androidx.compose.material.icons.rounded.FastRewind
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Loop
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PictureInPictureAlt
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Remove
import androidx.compose.material.icons.rounded.Repeat
import androidx.compose.material.icons.rounded.RepeatOne
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.RestartAlt
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SkipNext
import androidx.compose.material.icons.rounded.SkipPrevious
import androidx.compose.material.icons.rounded.SlowMotionVideo
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material.icons.rounded.WbSunny
import androidx.compose.material.icons.rounded.ZoomIn
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cmstudio.player.R
import com.cmstudio.player.codec.CodecInspector
import com.cmstudio.player.player.IntroLookupResult
import com.cmstudio.player.player.IntroOutroManager
import com.cmstudio.player.player.IntroSegmentKind
import com.cmstudio.player.subtitle.OnlineSubtitleClient
import com.cmstudio.player.subtitle.OnlineSubtitleResult
import com.cmstudio.player.subtitle.OnlineSubtitleSearchOutcome
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.theme.LiquidGlassBorder
import com.cmstudio.player.ui.theme.LiquidGlassSurface
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin

data class PlayerTrackOption(
    val groupIndex: Int,
    val trackIndex: Int,
    val label: String,
    val selected: Boolean
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideoPlayerScreen(
    videoTitle: String,
    isPlaying: Boolean,
    isBuffering: Boolean = false,
    currentPositionMs: Long,
    durationMs: Long,
    bufferedPositionMs: Long = 0L,
    isHwDecoder: Boolean,
    decoderPriority: String = "AUTO",
    activeDecoderName: String = "",
    isLocked: Boolean,
    isInPipMode: Boolean = false,
    orientationMode: String = "Auto-Rotate",
    onToggleOrientation: () -> Unit = {},
    activeEngine: String = "Media3",
    smoothMotionEnabled: Boolean = false,
    targetFps: Int = 120,
    detectedFps: Float = 24f,
    droppedFrames: Int = 0,
    hdrFormatText: String = "SDR",
    bitrateText: String = "",
    initialSheet: String = "",
    zoomScale: Float = 1.0f,
    panOffsetX: Float = 0f,
    panOffsetY: Float = 0f,
    aspectRatioMode: String = "Fit",
    codecName: String = "Video",
    resolutionText: String = "",
    channelLayoutText: String = "Stereo",
    initialVolume: Float = 100f,
    initialBrightness: Float = 0.7f,
    currentSpeed: Float = 1.0f,
    repeatMode: String = "OFF",
    hasPreviousTrack: Boolean = false,
    hasNextTrack: Boolean = false,
    queueTitles: List<String> = emptyList(),
    currentQueueIndex: Int = 0,
    abLoopStartMs: Long = -1L,
    abLoopEndMs: Long = -1L,
    abLoopActive: Boolean = false,
    screenshotEventId: Int = 0,
    screenshotSuccessMessage: String? = null,
    playbackErrorMessage: String? = null,
    playbackErrorDetails: String? = null,
    onSharePlaybackError: () -> Unit = {},
    onBuildPlaybackErrorQr: (Int) -> android.graphics.Bitmap? = { null },
    audioTrackOptions: List<PlayerTrackOption> = emptyList(),
    subtitleTrackOptions: List<PlayerTrackOption> = emptyList(),
    subtitlesDisabled: Boolean = true,
    onSelectAudioTrack: (Int, Int) -> Unit = { _, _ -> },
    onSelectSubtitleTrack: (Int, Int) -> Unit = { _, _ -> },
    onDisableSubtitles: () -> Unit = {},
    onRequestSubtitleFile: () -> Unit = {},
    onAttachExternalSubtitleUri: (Uri) -> Unit = {},
    onSubtitleDelayChange: (Int) -> Unit = {},
    onAudioDelayChange: (Int) -> Unit = {},
    onEqualizerPresetChange: (String) -> Unit = {},
    chapters: List<VideoChapterItem> = emptyList(),
    currentChapterIndex: Int = -1,
    videoTrackOptions: List<PlayerTrackOption> = emptyList(),
    isShuffleEnabled: Boolean = false,
    isMirrored: Boolean = false,
    isFlippedVertical: Boolean = false,
    ambientColorArgb: Long = 0L,
    sleepTimerRemainingMs: Long = 0L,
    sleepTimerUntilEnd: Boolean = false,
    secondarySubtitleText: String = "",
    secondarySubtitleTitle: String = "",
    onSelectVideoQualityTrack: (Int, Int) -> Unit = { _, _ -> },
    onResetVideoQualityAuto: () -> Unit = {},
    onRequestSecondarySubtitleFile: () -> Unit = {},
    onClearSecondarySubtitle: () -> Unit = {},
    onRequestCustomSubtitleFont: () -> Unit = {},
    onClearCustomSubtitleFont: () -> Unit = {},
    onSubtitleSpeedChange: (Int) -> Unit = {},
    onCustomEqBandsChange: (List<Float>) -> Unit = {},
    onAudioChannelModeChange: (String) -> Unit = {},
    onToggleShuffle: () -> Unit = {},
    onToggleMirror: () -> Unit = {},
    onToggleVerticalFlip: () -> Unit = {},
    onToggleAmbientMode: () -> Unit = {},
    onSetSleepTimerMinutes: (Int) -> Unit = {},
    onSetSleepTimerEndOfVideo: () -> Unit = {},
    onCancelSleepTimer: () -> Unit = {},
    onToggleBackgroundPlayback: () -> Unit = { AppSettings.setBackgroundPlayback(!AppSettings.backgroundPlayback.value) },
    onPlayPauseClick: () -> Unit,
    onPreviousTrack: () -> Unit = {},
    onNextTrack: () -> Unit = {},
    onSelectQueueIndex: (Int) -> Unit = {},
    onSeek: (Long) -> Unit,
    onFrameStep: (Int) -> Unit = {},
    onToggleDecoder: () -> Unit,
    onDecoderPriorityChange: (String) -> Unit = {},
    onToggleLock: () -> Unit,
    onToggleSmoothMotion: () -> Unit = {},
    onEngineSwitch: (String) -> Unit = {},
    onSpeedChange: (Float) -> Unit = {},
    onRepeatModeCycle: () -> Unit = {},
    onSetAbStart: (Long) -> Unit = {},
    onSetAbEnd: (Long) -> Unit = {},
    onToggleAbLoop: (Boolean) -> Unit = {},
    onClearAbLoop: () -> Unit = {},
    onAspectRatioChange: (String) -> Unit = {},
    onZoomPanChange: (Float, Float, Float) -> Unit = { _, _, _ -> },
    onBrightnessChange: (Float) -> Unit = {},
    onVolumeChange: (Float) -> Unit = {},
    onCaptureScreenshot: () -> Unit = {},
    onRetryPlayback: () -> Unit = {},
    onSwitchToSoftwareAndRetry: () -> Unit = {},
    onPipClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haptic = LocalHapticFeedback.current

    val seekIntervalSec by AppSettings.doubleTapSeconds.collectAsState()
    val isVolGestureEnabled by AppSettings.volumeGesture.collectAsState()
    val isBrightGestureEnabled by AppSettings.brightnessGesture.collectAsState()
    val isSeekGestureEnabled by AppSettings.seekGesture.collectAsState()
    val isLongPress2xEnabled by AppSettings.longPress2x.collectAsState()
    val isPinchZoomEnabled by AppSettings.pinchZoomEnabled.collectAsState()
    val hapticEnabled by AppSettings.hapticFeedback.collectAsState()
    val isFrostedSheets by AppSettings.isFrostedPlayerSheets.collectAsState()
    val isLiquidGlass by AppSettings.isLiquidGlassEnabled.collectAsState()
    val isAmoled by AppSettings.isAmoled.collectAsState()
    val userFrostOpacity by AppSettings.glassFrostOpacity.collectAsState()
    val seekbarStyle by AppSettings.seekbarStyle.collectAsState()
    val showHudTelemetry by AppSettings.showHudTelemetry.collectAsState()
    val playerControlSize by AppSettings.playerControlSize.collectAsState()
    val topRightCsv by AppSettings.topRightHudControls.collectAsState()
    val bottomLeftCsv by AppSettings.bottomLeftHudControls.collectAsState()
    val bottomRightCsv by AppSettings.bottomRightHudControls.collectAsState()
    val showRotationOverlay by AppSettings.showScreenRotationOverlay.collectAsState()
    val showAspectOverlay by AppSettings.showAspectRatioOverlay.collectAsState()
    val showSpeedOverlay by AppSettings.showPlaybackSpeedOverlay.collectAsState()
    val showDecoderOverlay by AppSettings.showDecoderOverlay.collectAsState()
    val showShuffleOverlay by AppSettings.showShuffleOverlay.collectAsState()
    val showLoopOverlay by AppSettings.showLoopOverlay.collectAsState()
    val showPipOverlay by AppSettings.showPipOverlay.collectAsState()
    val showScreenshotOverlay by AppSettings.showScreenshotOverlay.collectAsState()

    val btnSizeDp = when (playerControlSize) {
        "COMPACT" -> 34.dp
        "LARGE" -> 44.dp
        else -> 38.dp
    }
    val btnIconSizeDp = when (playerControlSize) {
        "COMPACT" -> 16.dp
        "LARGE" -> 22.dp
        else -> 18.dp
    }
    val screenWidthDp = LocalConfiguration.current.screenWidthDp
    val isCompactWidth = screenWidthDp < 500
    val bottomBtnSizeDp = if (isCompactWidth) minOf(btnSizeDp, 32.dp) else btnSizeDp
    val bottomBtnIconSizeDp = if (isCompactWidth) minOf(btnIconSizeDp, 16.dp) else btnIconSizeDp
    val bottomClusterGapDp = if (isCompactWidth) 5.dp else 8.dp

    val activeSheetColor = when {
        isAmoled -> Color.Black
        isFrostedSheets || isLiquidGlass -> Color(0xFF141820).copy(alpha = userFrostOpacity.coerceIn(0.68f, 0.94f))
        else -> Color(0xFF141820)
    }
    val hudTheme by AppSettings.playerControlsTheme.collectAsState()
    val hideBtnBg by AppSettings.hidePlayerButtonsBg.collectAsState()
    val forceDarkBtnBg by AppSettings.forceDarkPlayerButtonsBg.collectAsState()
    val showSeekbarOuter by AppSettings.showSeekbarOuterContainer.collectAsState()
    val portraitCtrlPos by AppSettings.portraitControlsPosition.collectAsState()
    val hudAutoHideMs by AppSettings.hudAutoHideMs.collectAsState()
    val safeAreaWindow by AppSettings.safeAreaWindow.collectAsState()
    val showLoadingCircle by AppSettings.showLoadingCircle.collectAsState()
    val doubleTapAreaPct by AppSettings.doubleTapAreaWidthPct.collectAsState()
    val doubleTapCenterPP by AppSettings.doubleTapCenterPlayPause.collectAsState()
    val useSingleTapPause by AppSettings.useSingleTapToPause.collectAsState()
    val volOnRight by AppSettings.displayVolumeOnRight.collectAsState()
    val holdSpeedMult by AppSettings.holdSpeedMultiplier.collectAsState()
    val hSwipeSens by AppSettings.horizontalSwipeSensitivity.collectAsState()
    val showSeekHudPref by AppSettings.showGestureSeekOverlay.collectAsState()
    val showVbHudPref by AppSettings.showGestureVolBrightOverlay.collectAsState()
    val swipeUpPlaylistPref by AppSettings.swipeUpOpensPlaylist.collectAsState()
    val customSkipSec by AppSettings.customSkipDurationSec.collectAsState()
    val ambientEnabled by AppSettings.ambientModeEnabled.collectAsState()
    val dualSubEnabled by AppSettings.dualSubtitles.collectAsState()
    val secondarySubColor by AppSettings.secondarySubtitleColor.collectAsState()
    val subBottomPadDp by AppSettings.subtitleBottomPaddingDp.collectAsState()
    val portraitBottomCsv by AppSettings.portraitBottomHudControls.collectAsState()
    val controlsDrawerEnabled by AppSettings.controlsDrawerEnabled.collectAsState()
    val allowGesturesInPanels by AppSettings.allowGesturesInPanels.collectAsState()
    val showSeekRipple by AppSettings.showSeekRipple.collectAsState()
    val showSeekTimeInHud by AppSettings.showSeekTimeInHud.collectAsState()
    val showBufferedRangeOnSeekbar by AppSettings.showBufferedRangeOnSeekbar.collectAsState()
    val showChaptersOnSeekbar by AppSettings.showChaptersOnSeekbar.collectAsState()
    val doubleTapLeftAction by AppSettings.doubleTapLeftAction.collectAsState()
    val doubleTapCenterAction by AppSettings.doubleTapCenterAction.collectAsState()
    val doubleTapRightAction by AppSettings.doubleTapRightAction.collectAsState()
    val showHoldSpeedOverlay by AppSettings.showHoldSpeedOverlay.collectAsState()
    val showAspectFeedbackOverlay by AppSettings.showAspectFeedbackOverlay.collectAsState()
    val showZoomFeedbackOverlay by AppSettings.showZoomFeedbackOverlay.collectAsState()
    val showRepeatShuffleOverlay by AppSettings.showRepeatShuffleOverlay.collectAsState()
    val showActionFeedbackPills by AppSettings.showActionFeedbackPills.collectAsState()
    val holdDragMovesSubtitles by AppSettings.holdDragMovesSubtitles.collectAsState()
    val swipeSubtitlesToSeekDialog by AppSettings.swipeSubtitlesToSeekDialog.collectAsState()
    val invertSwipeSubtitlesDirection by AppSettings.invertSwipeSubtitlesDirection.collectAsState()
    val bgPlaybackEnabled by AppSettings.backgroundPlayback.collectAsState()
    val volumeBoostCap by AppSettings.volumeBoostCapPct.collectAsState()
    val introOutroEnabled by AppSettings.introOutroEnabled.collectAsState()
    val introProviderMode by AppSettings.introProvider.collectAsState()
    val autoSkipIntro by AppSettings.autoSkipIntro.collectAsState()
    val autoSkipOutro by AppSettings.autoSkipOutro.collectAsState()
    val detectIntroFromChapters by AppSettings.detectIntroFromChapters.collectAsState()
    val customIntroCsv by AppSettings.customIntroKeywords.collectAsState()
    val customOutroCsv by AppSettings.customOutroKeywords.collectAsState()
    val showProviderStatusOverlay by AppSettings.showProviderStatusOverlay.collectAsState()
    val onlineSubMode by AppSettings.onlineSubSearchMode.collectAsState()
    val onlineSubPreferredLang by AppSettings.onlineSubPreferredLang.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    var introLookupResult by remember { mutableStateOf(IntroLookupResult(emptyList(), "", false)) }
    val autoSkippedSegmentKeys = remember(videoTitle, durationMs) { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(
        videoTitle,
        chapters,
        durationMs,
        introOutroEnabled,
        introProviderMode,
        detectIntroFromChapters,
        customIntroCsv,
        customOutroCsv
    ) {
        if (!introOutroEnabled) {
            introLookupResult = IntroLookupResult(emptyList(), "", false)
        } else {
            introLookupResult = IntroOutroManager.lookupMarkers(
                titleOrFilename = videoTitle,
                chapters = chapters,
                totalDurationMs = durationMs,
                providerMode = introProviderMode,
                detectFromChaptersEnabled = detectIntroFromChapters,
                customIntroCsv = customIntroCsv,
                customOutroCsv = customOutroCsv
            )
        }
    }

    val activeIntroOutroSegment = remember(currentPositionMs, introLookupResult, introOutroEnabled) {
        if (!introOutroEnabled) null
        else introLookupResult.segments.firstOrNull { it.contains(currentPositionMs) }
    }

    val hudGlassFill = when {
        hideBtnBg -> Color.Transparent
        forceDarkBtnBg -> Color(0xCC06080C)
        hudTheme == "CLASSIC" -> Color(0x9E0B0E14)
        hudTheme == "GLOSSY" -> MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
        else -> Color(0xFF0E121A).copy(alpha = if (isLiquidGlass || isFrostedSheets) 0.62f else 0.78f)
    }
    val hudGlassBorder = when {
        hideBtnBg -> Color.Transparent
        hudTheme == "GLOSSY" -> Color.White.copy(alpha = 0.35f)
        hudTheme == "CLASSIC" -> Color.White.copy(alpha = 0.10f)
        else -> Color.White.copy(alpha = 0.20f)
    }

    var lastTapTimeMs by remember { mutableLongStateOf(0L) }
    var lastTapPosition by remember { mutableStateOf(Offset.Zero) }

    var controlsVisible by remember { mutableStateOf(true) }
    var showRemainingTime by remember { mutableStateOf(true) }
    var currentBrightness by remember(initialBrightness) { mutableFloatStateOf(initialBrightness.coerceIn(0.01f, 1.0f)) }
    var currentVolume by remember(initialVolume) { mutableFloatStateOf(initialVolume) }
    var isFastForwarding by remember { mutableStateOf(false) }

    // HUD overlays visibility
    var showBrightnessHud by remember { mutableStateOf(false) }
    var showVolumeHud by remember { mutableStateOf(false) }
    var seekPreviewDeltaMs by remember { mutableLongStateOf(0L) }
    var seekPreviewTargetMs by remember { mutableLongStateOf(0L) }
    var showSeekPreviewHud by remember { mutableStateOf(false) }
    var doubleTapRippleSide by remember { mutableStateOf<String?>(null) }
    var accumulatedDoubleTapSec by remember { mutableStateOf(0) }
    var doubleTapResetSeq by remember { mutableStateOf(0) }
    var actionFeedbackPillText by remember { mutableStateOf<String?>(null) }
    var visibleScreenshotMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(doubleTapResetSeq) {
        if (doubleTapResetSeq > 0) {
            kotlinx.coroutines.delay(750L)
            doubleTapRippleSide = null
            accumulatedDoubleTapSec = 0
        }
    }
    LaunchedEffect(actionFeedbackPillText) {
        if (actionFeedbackPillText != null) {
            kotlinx.coroutines.delay(1500L)
            actionFeedbackPillText = null
        }
    }

    // Modal Bottom Sheets
    var showEngineSheet by remember { mutableStateOf(initialSheet == "decoder") }
    var showSubtitlesSheet by remember { mutableStateOf(false) }
    var showAudioSheet by remember { mutableStateOf(false) }
    var showChaptersSheet by remember { mutableStateOf(false) }
    var showQualitySheet by remember { mutableStateOf(false) }
    var showSleepTimerSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showEdgeDrawer by remember { mutableStateOf(false) }
    var isSpeedLocked by remember { mutableStateOf(false) }
    var showSpeedSheet by remember { mutableStateOf(false) }
    var showAspectRatioSheet by remember { mutableStateOf(false) }
    var showZoomSheet by remember { mutableStateOf(false) }
    var showFrameNavSheet by remember { mutableStateOf(false) }
    var showAbLoopSheet by remember { mutableStateOf(false) }
    var showCodecInspectorSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showToolsSheet by remember { mutableStateOf(false) }

    val subtitleDelayMs by AppSettings.subtitleDelayMs.collectAsState()
    val subtitleFontSize by AppSettings.subtitleFontSize.collectAsState()
    val subtitleEdgeType by AppSettings.subtitleEdgeType.collectAsState()
    val subtitleBgOpacity by AppSettings.subtitleBgOpacity.collectAsState()
    val subtitleBottomPaddingDp by AppSettings.subtitleBottomPaddingDp.collectAsState()
    val subtitleBold by AppSettings.subtitleBold.collectAsState()
    val audioDelayMs by AppSettings.audioDelayMs.collectAsState()
    val selectedEqPreset by AppSettings.eqPreset.collectAsState()
    val audioPitchCorrection by AppSettings.audioPitchCorrection.collectAsState()
    var loudnessBoost by remember(initialVolume) { mutableFloatStateOf(initialVolume) }

    val latestSpeedMult by rememberUpdatedState(holdSpeedMult)
    val latestHSens by rememberUpdatedState(hSwipeSens)
    val latestVolOnRight by rememberUpdatedState(volOnRight)
    val latestDoubleTapPct by rememberUpdatedState(doubleTapAreaPct)
    val latestDoubleTapCenterPP by rememberUpdatedState(doubleTapCenterPP)
    val latestSingleTapPause by rememberUpdatedState(useSingleTapPause)
    val latestSwipeUpPlaylist by rememberUpdatedState(swipeUpPlaylistPref)
    val latestHasQueue by rememberUpdatedState(queueTitles.size > 1)

    val isAnySheetOpen = showEngineSheet || showSubtitlesSheet || showAudioSheet || showSpeedSheet ||
        showAspectRatioSheet || showZoomSheet || showFrameNavSheet || showAbLoopSheet ||
        showCodecInspectorSheet || showQueueSheet || showToolsSheet ||
        showChaptersSheet || showQualitySheet || showSleepTimerSheet || showEqualizerSheet || showEdgeDrawer

    // Always keep fresh values inside pointerInput gesture coroutine
    val latestPosMs by rememberUpdatedState(currentPositionMs)
    val latestDurationMs by rememberUpdatedState(durationMs)
    val latestZoom by rememberUpdatedState(zoomScale)
    val latestPanX by rememberUpdatedState(panOffsetX)
    val latestPanY by rememberUpdatedState(panOffsetY)
    val latestSpeed by rememberUpdatedState(currentSpeed)
    val latestSeekSec by rememberUpdatedState(seekIntervalSec)
    val latestVolEnabled by rememberUpdatedState(isVolGestureEnabled)
    val latestBrightEnabled by rememberUpdatedState(isBrightGestureEnabled)
    val latestSeekEnabled by rememberUpdatedState(isSeekGestureEnabled)
    val latestLongPress2x by rememberUpdatedState(isLongPress2xEnabled)
    val latestPinchEnabled by rememberUpdatedState(isPinchZoomEnabled)
    val latestHaptic by rememberUpdatedState(hapticEnabled)

    // Auto-hide transient HUD feedback
    LaunchedEffect(showBrightnessHud, currentBrightness) {
        if (showBrightnessHud) {
            delay(1300L)
            showBrightnessHud = false
        }
    }
    LaunchedEffect(showVolumeHud, currentVolume) {
        if (showVolumeHud) {
            delay(1300L)
            showVolumeHud = false
        }
    }
    LaunchedEffect(doubleTapRippleSide) {
        if (doubleTapRippleSide != null) {
            delay(650L)
            doubleTapRippleSide = null
        }
    }
    LaunchedEffect(screenshotEventId) {
        if (screenshotEventId > 0 && !screenshotSuccessMessage.isNullOrBlank()) {
            visibleScreenshotMsg = screenshotSuccessMessage
            delay(2200L)
            visibleScreenshotMsg = null
        }
    }
    // Auto-hide main controls during active playback or locked unlock pill
    LaunchedEffect(controlsVisible, isPlaying, isAnySheetOpen, isLocked, isInPipMode, hudAutoHideMs) {
        if (isInPipMode) {
            controlsVisible = false
            return@LaunchedEffect
        }
        if (controlsVisible && isLocked) {
            delay(3000L)
            controlsVisible = false
            return@LaunchedEffect
        }
        if (controlsVisible && isPlaying && !isAnySheetOpen && !isLocked) {
            delay(hudAutoHideMs.toLong().coerceIn(1500L, 15000L))
            controlsVisible = false
        }
    }

    var playButtonScaleTarget by remember { mutableFloatStateOf(1.0f) }
    val playButtonScale by animateFloatAsState(
        targetValue = playButtonScaleTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "playScale"
    )
    var rewindScaleTarget by remember { mutableFloatStateOf(1.0f) }
    val rewindScale by animateFloatAsState(
        targetValue = rewindScaleTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "rewindScale"
    )
    var forwardScaleTarget by remember { mutableFloatStateOf(1.0f) }
    val forwardScale by animateFloatAsState(
        targetValue = forwardScaleTarget,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "forwardScale"
    )

    val sheetState = rememberBottomSheetState(
        initialValue = SheetValue.Hidden,
        enabledValues = setOf(SheetValue.Hidden, SheetValue.Expanded)
    )

    // In Picture-in-Picture mode, keep overlay completely hidden so Android PiP window renders clean video
    if (isInPipMode) {
        Box(modifier = modifier.fillMaxSize())
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        // Optional YouTube-style Ambient Aura Glow around viewport edges
        if (ambientEnabled && ambientColorArgb != 0L) {
            val aura = Color(ambientColorArgb)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(aura.copy(alpha = 0.24f), aura.copy(alpha = 0.08f), Color.Transparent)
                        )
                    )
            )
        }

        // Dual Secondary Subtitle Display Overlay cleanly above primary captions
        if (dualSubEnabled && secondarySubtitleText.isNotBlank()) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xAA0A0D14),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 24.dp, vertical = (subBottomPadDp + 62).dp)
            ) {
                Text(
                    text = secondarySubtitleText,
                    color = Color(secondarySubColor),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                )
            }
        }

        // Sleep Timer or Speed-Lock Persistent Pill
        if (sleepTimerRemainingMs > 0L || sleepTimerUntilEnd || isSpeedLocked) {
            Row(
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 74.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (isSpeedLocked) {
                    Surface(
                        shape = CircleShape,
                        color = playerSheetAccent(),
                        modifier = Modifier.clickable {
                            isSpeedLocked = false
                            onSpeedChange(1.0f)
                        }
                    ) {
                        Text(
                            text = "${String.format(Locale.US, "%.1fx", holdSpeedMult)} Locked • Tap to 1x",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = playerOnAccentColor(playerSheetAccent()),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
                if (sleepTimerRemainingMs > 0L || sleepTimerUntilEnd) {
                    Surface(
                        shape = CircleShape,
                        color = hudGlassFill,
                        border = BorderStroke(1.dp, playerSheetAccent().copy(alpha = 0.7f)),
                        modifier = Modifier.clickable { showSleepTimerSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(5.dp)
                        ) {
                            Icon(Icons.Rounded.Bedtime, contentDescription = null, tint = playerSheetAccent(), modifier = Modifier.size(13.dp))
                            Text(
                                text = if (sleepTimerRemainingMs > 0L) formatDuration(sleepTimerRemainingMs) else "Sleep @ End",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }

        // Dedicated Background Gesture Layer (never blocks HUD button clicks; remembers updated states cleanly)
        val latestLeftAction by rememberUpdatedState(doubleTapLeftAction)
        val latestCenterAction by rememberUpdatedState(doubleTapCenterAction)
        val latestRightAction by rememberUpdatedState(doubleTapRightAction)
        val latestHoldMoveSub by rememberUpdatedState(holdDragMovesSubtitles)
        val latestSwipeSubDialog by rememberUpdatedState(swipeSubtitlesToSeekDialog)
        val latestInvertSubSwipe by rememberUpdatedState(invertSwipeSubtitlesDirection)
        val latestVolCap by rememberUpdatedState(volumeBoostCap)
        val latestAllowGesturesPanels by rememberUpdatedState(allowGesturesInPanels)
        val latestControlsDrawerEnabled by rememberUpdatedState(controlsDrawerEnabled)

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isLocked, isAnySheetOpen, allowGesturesInPanels) {
                    if (isLocked) {
                        awaitEachGesture {
                            awaitFirstDown(pass = PointerEventPass.Main).consume()
                            val downTime = SystemClock.uptimeMillis()
                            while (true) {
                                val ev = awaitPointerEvent(pass = PointerEventPass.Main)
                                val active = ev.changes.filter { it.pressed }
                                ev.changes.forEach { it.consume() }
                                if (active.isEmpty()) break
                            }
                            if (SystemClock.uptimeMillis() - downTime < 350L) {
                                controlsVisible = !controlsVisible
                            }
                        }
                        return@pointerInput
                    }
                    if (isAnySheetOpen && !latestAllowGesturesPanels) {
                        return@pointerInput
                    }

                    awaitEachGesture {
                        val down = awaitFirstDown(pass = PointerEventPass.Main)
                        val startTime = SystemClock.uptimeMillis()
                        val startPos = down.position
                        val screenWidth = size.width.toFloat().coerceAtLeast(1f)
                        val screenHeight = size.height.toFloat().coerceAtLeast(1f)

                        var isDragging = false
                        var isZooming = false
                        var dragDirection: String? = null
                        var currentPos = startPos
                        var localZoom = latestZoom
                        var localPanX = latestPanX
                        var localPanY = latestPanY

                        val initialBrightAtDown = currentBrightness
                        val initialVolAtDown = currentVolume
                        val initialSeekAtDown = latestPosMs
                        val initialSubPadAtDown = AppSettings.subtitleBottomPaddingDp.value

                        while (true) {
                            val event = awaitPointerEvent(pass = PointerEventPass.Main)
                            val activePointers = event.changes.filter { it.pressed }

                            if (activePointers.isEmpty()) {
                                if (isFastForwarding) {
                                    isFastForwarding = false
                                    if (!isSpeedLocked) {
                                        onSpeedChange(latestSpeed)
                                    }
                                }
                                if (dragDirection == "CENTER_UP" && !isSpeedLocked &&
                                    latestSwipeUpPlaylist && latestHasQueue &&
                                    (currentPos.y - startPos.y) < -110f
                                ) {
                                    showQueueSheet = true
                                }
                                if (dragDirection == "H" && showSeekPreviewHud) {
                                    onSeek(seekPreviewTargetMs)
                                    showSeekPreviewHud = false
                                }
                                if (dragDirection == "SUB_SEEK_H") {
                                    val dirSign = if (currentPos.x - startPos.x >= 0f) 1 else -1
                                    val finalSign = if (latestInvertSubSwipe) -dirSign else dirSign
                                    val stepMs = 4000L * finalSign
                                    val durMax = latestDurationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
                                    onSeek((latestPosMs + stepMs).coerceIn(0L, durMax))
                                    if (showActionFeedbackPills) {
                                        actionFeedbackPillText = if (finalSign > 0) "Next Dialogue (+4s)" else "Previous Dialogue (-4s)"
                                    }
                                }
                                if (!isDragging && !isZooming && !isFastForwarding) {
                                    val elapsed = SystemClock.uptimeMillis() - startTime
                                    if (elapsed < 320L) {
                                        val now = SystemClock.uptimeMillis()
                                        val dx = currentPos.x - lastTapPosition.x
                                        val dy = currentPos.y - lastTapPosition.y
                                        val tapDist = kotlin.math.sqrt(dx * dx + dy * dy)
                                        if (now - lastTapTimeMs < 350L && tapDist < 200f) {
                                            val zone = PlayerGestureZoneMath.zoneForTapX(currentPos.x, screenWidth, latestDoubleTapPct)
                                            val action = when (zone) {
                                                GestureTapZone.LEFT -> latestLeftAction
                                                GestureTapZone.RIGHT -> latestRightAction
                                                GestureTapZone.CENTER -> if (!latestDoubleTapCenterPP) {
                                                    DoubleTapActionCatalog.ACTION_NONE
                                                } else latestCenterAction
                                            }
                                            val intervalMs = latestSeekSec * 1000L
                                            val maxDur = latestDurationMs.coerceAtLeast(0L)
                                            when (action) {
                                                DoubleTapActionCatalog.ACTION_SEEK -> {
                                                    val sideStr = if (zone == GestureTapZone.LEFT) "LEFT" else "RIGHT"
                                                    accumulatedDoubleTapSec = if (doubleTapRippleSide == sideStr) {
                                                        accumulatedDoubleTapSec + latestSeekSec
                                                    } else latestSeekSec
                                                    doubleTapRippleSide = sideStr
                                                    doubleTapResetSeq++
                                                    if (zone == GestureTapZone.LEFT) {
                                                        onSeek((latestPosMs - intervalMs).coerceAtLeast(0L))
                                                    } else {
                                                        val target = if (maxDur > 0L) (latestPosMs + intervalMs).coerceAtMost(maxDur) else latestPosMs + intervalMs
                                                        onSeek(target)
                                                    }
                                                }
                                                DoubleTapActionCatalog.ACTION_CUSTOM_SKIP -> {
                                                    val target = if (maxDur > 0L) (latestPosMs + customSkipSec * 1000L).coerceAtMost(maxDur) else latestPosMs + customSkipSec * 1000L
                                                    onSeek(target)
                                                    if (showActionFeedbackPills) actionFeedbackPillText = "Skip +${customSkipSec}s"
                                                }
                                                DoubleTapActionCatalog.ACTION_PLAY_PAUSE -> onPlayPauseClick()
                                                else -> if (!isAnySheetOpen) controlsVisible = !controlsVisible
                                            }
                                            if (latestHaptic) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                            lastTapTimeMs = 0L
                                        } else {
                                            lastTapTimeMs = now
                                            lastTapPosition = currentPos
                                            if (latestSingleTapPause) {
                                                onPlayPauseClick()
                                            }
                                            if (!isAnySheetOpen) {
                                                controlsVisible = !controlsVisible
                                            }
                                        }
                                    }
                                }
                                break
                            }

                            if (activePointers.size >= 2 && latestPinchEnabled) {
                                isZooming = true
                                val zoomFactor = event.calculateZoom()
                                val pan = event.calculatePan()
                                localZoom = (localZoom * zoomFactor).coerceIn(0.5f, 4.0f)
                                if (localZoom > 1.0f) {
                                    val maxPanX = screenWidth * (localZoom - 1f) * 0.5f
                                    val maxPanY = screenHeight * (localZoom - 1f) * 0.5f
                                    localPanX = (localPanX + pan.x).coerceIn(-maxPanX, maxPanX)
                                    localPanY = (localPanY + pan.y).coerceIn(-maxPanY, maxPanY)
                                } else {
                                    localPanX = 0f
                                    localPanY = 0f
                                }
                                onZoomPanChange(localZoom, localPanX, localPanY)
                                event.changes.forEach { it.consume() }
                            } else if (activePointers.size == 1 && !isZooming) {
                                val change = activePointers[0]
                                currentPos = change.position
                                val dx = currentPos.x - startPos.x
                                val dy = currentPos.y - startPos.y
                                val distance = kotlin.math.sqrt(dx * dx + dy * dy)

                                val holdDuration = SystemClock.uptimeMillis() - startTime
                                if (latestLongPress2x && distance < 22f && holdDuration > 450L && !isFastForwarding && !isDragging) {
                                    isFastForwarding = true
                                    if (latestHaptic) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                    onSpeedChange(latestSpeedMult)
                                }
                                // Swipe upward during fast-forward hold locks speed until tapped off
                                if (isFastForwarding && dy < -110f && !isSpeedLocked) {
                                    isSpeedLocked = true
                                    if (latestHaptic) haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                }

                                if (distance > 24f && !isFastForwarding) {
                                    isDragging = true
                                    if (dragDirection == null) {
                                        val zone = PlayerGestureZoneMath.zoneForTapX(startPos.x, screenWidth, latestDoubleTapPct)
                                        val inSubZone = startPos.y > screenHeight * 0.78f
                                        dragDirection = when {
                                            latestControlsDrawerEnabled && startPos.x > screenWidth * 0.86f && abs(dx) > abs(dy) * 1.2f && dx < 0f -> "EDGE_DRAWER"
                                            inSubZone && latestSwipeSubDialog && abs(dx) > abs(dy) * 1.2f -> "SUB_SEEK_H"
                                            inSubZone && latestHoldMoveSub && abs(dy) > abs(dx) * 1.2f -> "SUB_MOVE_V"
                                            abs(dx) > abs(dy) * 1.2f -> "H"
                                            zone == GestureTapZone.CENTER && dy < -40f && latestSwipeUpPlaylist && latestHasQueue -> "CENTER_UP"
                                            startPos.x < screenWidth / 2f -> "V_LEFT"
                                            else -> "V_RIGHT"
                                        }
                                    }
                                    if (dragDirection == "EDGE_DRAWER" && dx < -65f) {
                                        showEdgeDrawer = true
                                    }

                                    when (dragDirection) {
                                        "SUB_MOVE_V" -> {
                                            val nextPad = (initialSubPadAtDown + (-dy / 4.5f).roundToInt()).coerceIn(0, 120)
                                            AppSettings.setSubtitleBottomPaddingDp(nextPad)
                                            if (showActionFeedbackPills) actionFeedbackPillText = "Sub Position: ${nextPad}dp"
                                            change.consume()
                                        }
                                        "H" -> if (latestSeekEnabled && latestDurationMs > 0L) {
                                            val deltaFraction = (dx / screenWidth) * PlayerGestureZoneMath.normalizeSeekSensitivity(latestHSens)
                                            val deltaMs = (deltaFraction * 90_000L).toLong()
                                            seekPreviewDeltaMs = deltaMs
                                            seekPreviewTargetMs = (initialSeekAtDown + deltaMs).coerceIn(0L, latestDurationMs)
                                            showSeekPreviewHud = showSeekHudPref
                                            change.consume()
                                        }
                                        "V_LEFT" -> {
                                            val isVolumeSide = !latestVolOnRight
                                            if (isVolumeSide && latestVolEnabled) {
                                                val capF = latestVolCap.coerceIn(100, 200).toFloat()
                                                val deltaY = -dy / (screenHeight * 0.7f) * 100f
                                                val next = (initialVolAtDown + deltaY).coerceIn(0f, capF)
                                                currentVolume = next
                                                loudnessBoost = next
                                                onVolumeChange(next)
                                                showVolumeHud = showVbHudPref
                                                change.consume()
                                            } else if (!isVolumeSide && latestBrightEnabled) {
                                                val deltaY = -dy / (screenHeight * 0.7f)
                                                val next = (initialBrightAtDown + deltaY).coerceIn(0.01f, 1.0f)
                                                currentBrightness = next
                                                onBrightnessChange(next)
                                                showBrightnessHud = showVbHudPref
                                                change.consume()
                                            }
                                        }
                                        "V_RIGHT" -> {
                                            val isVolumeSide = latestVolOnRight
                                            if (isVolumeSide && latestVolEnabled) {
                                                val capF = latestVolCap.coerceIn(100, 200).toFloat()
                                                val deltaY = -dy / (screenHeight * 0.7f) * 100f
                                                val next = (initialVolAtDown + deltaY).coerceIn(0f, capF)
                                                currentVolume = next
                                                loudnessBoost = next
                                                onVolumeChange(next)
                                                showVolumeHud = showVbHudPref
                                                change.consume()
                                            } else if (!isVolumeSide && latestBrightEnabled) {
                                                val deltaY = -dy / (screenHeight * 0.7f)
                                                val next = (initialBrightAtDown + deltaY).coerceIn(0.01f, 1.0f)
                                                currentBrightness = next
                                                onBrightnessChange(next)
                                                showBrightnessHud = showVbHudPref
                                                change.consume()
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        )

        // Gesture 1 HUD: Brightness
        AnimatedVisibility(
            visible = showBrightnessHud,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 28.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LiquidGlassSurface,
                border = BorderStroke(1.2.dp, LiquidGlassBorder),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(Icons.Rounded.WbSunny, contentDescription = "Brightness", tint = Color(0xFFFCD34D), modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(110.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight(currentBrightness)
                                .align(Alignment.BottomCenter)
                                .background(Color(0xFFFCD34D))
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("${(currentBrightness * 100).toInt()}%", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Gesture 2 HUD: Volume (0-200%)
        AnimatedVisibility(
            visible = showVolumeHud,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 28.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = LiquidGlassSurface,
                border = BorderStroke(1.2.dp, LiquidGlassBorder),
                shadowElevation = 12.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val isBoost = currentVolume > 100f
                    val volAccent = if (isBoost) Color(0xFFFF7A00) else playerSheetAccent()
                    Icon(
                        imageVector = if (currentVolume == 0f) Icons.AutoMirrored.Rounded.VolumeMute else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = "Volume",
                        tint = volAccent,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Box(
                        modifier = Modifier
                            .width(6.dp)
                            .height(110.dp)
                            .clip(RoundedCornerShape(3.dp))
                            .background(Color.White.copy(alpha = 0.2f))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .fillMaxHeight((currentVolume / 200f).coerceIn(0f, 1f))
                                .align(Alignment.BottomCenter)
                                .background(volAccent)
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (isBoost) "BOOST\n${currentVolume.toInt()}%" else "${currentVolume.toInt()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Gesture 3 HUD: Horizontal Scrub Preview
        AnimatedVisibility(
            visible = showSeekPreviewHud,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = LiquidGlassSurface,
                border = BorderStroke(1.2.dp, LiquidGlassBorder),
                shadowElevation = 16.dp
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (seekPreviewDeltaMs >= 0) Icons.Rounded.FastForward else Icons.Rounded.FastRewind,
                        contentDescription = null,
                        tint = playerSheetAccent(),
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "${if (seekPreviewDeltaMs >= 0) "+" else ""}${seekPreviewDeltaMs / 1000}s",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${formatDuration(seekPreviewTargetMs)} / ${formatDuration(durationMs)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }

        // Gesture 4 HUD: Double-Tap Circular Arc Seek Ripple
        if (doubleTapRippleSide != null && showSeekRipple) {
            DoubleTapSeekArcOverlay(
                side = doubleTapRippleSide!!,
                accumulatedSec = accumulatedDoubleTapSec.coerceAtLeast(seekIntervalSec),
                modifier = Modifier.align(if (doubleTapRippleSide == "LEFT") Alignment.CenterStart else Alignment.CenterEnd)
            )
        }

        // Gesture 5 HUD: Active Zoom Indicator Pill
        if (abs(zoomScale - 1.0f) > 0.04f && showZoomFeedbackOverlay) {
            val zoomAccent = playerSheetAccent()
            val zoomOnAccent = playerOnAccentColor(zoomAccent)
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = zoomAccent,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 76.dp)
                    .clickable { onZoomPanChange(1.0f, 0f, 0f) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.RestartAlt, contentDescription = "Reset Zoom", tint = zoomOnAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Zoom: ${(zoomScale * 100).roundToInt()}% • Reset",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = zoomOnAccent
                    )
                }
            }
        }

        // Gesture 6 HUD: Hold Speed Fast Forward Pulse Pill
        AnimatedVisibility(
            visible = isFastForwarding && showHoldSpeedOverlay,
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 76.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFFFF7A00),
                shadowElevation = 10.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Bolt, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("${String.format(Locale.US, "%.1fX", holdSpeedMult)} FAST-FORWARD >>", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }

        // Active A-B Loop Badge
        if (abLoopActive && abLoopEndMs > abLoopStartMs && !controlsVisible) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = playerSheetAccent().copy(alpha = 0.88f),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(top = 24.dp)
                    .clickable { showAbLoopSheet = true }
            ) {
                Text(
                    text = "A-B Loop: ${formatDuration(abLoopStartMs)} → ${formatDuration(abLoopEndMs)}",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = playerOnAccentColor(playerSheetAccent()),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                )
            }
        }

        // Verified Screenshot & Action Feedback Toasts
        AnimatedVisibility(
            visible = visibleScreenshotMsg != null || (actionFeedbackPillText != null && showActionFeedbackPills),
            enter = fadeIn() + scaleIn(),
            exit = fadeOut() + scaleOut(),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 80.dp)
        ) {
            val isShot = visibleScreenshotMsg != null
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = if (isShot) Color(0xFF059669) else Color(0xD912161E),
                border = if (!isShot) BorderStroke(1.dp, playerSheetAccent().copy(alpha = 0.65f)) else null,
                shadowElevation = 12.dp
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isShot) {
                        Icon(Icons.Rounded.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = visibleScreenshotMsg ?: actionFeedbackPillText.orEmpty(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        // Buffering Indicator
        AnimatedVisibility(
            visible = isBuffering && showLoadingCircle && playbackErrorMessage.isNullOrBlank(),
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.Center)
        ) {
            Surface(
                shape = CircleShape,
                color = Color(0xCC0D111A),
                border = BorderStroke(1.dp, playerSheetAccent().copy(alpha = 0.45f)),
                shadowElevation = 8.dp
            ) {
                CircularProgressIndicator(
                    color = playerSheetAccent(),
                    strokeWidth = 3.dp,
                    modifier = Modifier
                        .padding(14.dp)
                        .size(28.dp)
                )
            }
        }

        // Structured Playback Error Overlay (never silent black screen)
        if (!playbackErrorMessage.isNullOrBlank()) {
            var showQrError by remember(playbackErrorMessage) { mutableStateOf(false) }
            Surface(
                shape = RoundedCornerShape(22.dp),
                color = Color(0xFF141820).copy(alpha = 0.94f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.65f)),
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(horizontal = 24.dp)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(40.dp)
                    )
                    Text("Playback Error", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        text = playbackErrorMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center
                    )
                    if (showQrError) {
                        val qrBmp = remember(playbackErrorDetails) { onBuildPlaybackErrorQr(360) }
                        if (qrBmp != null) {
                            Surface(color = Color.White, shape = RoundedCornerShape(12.dp)) {
                                androidx.compose.foundation.Image(
                                    bitmap = qrBmp.asImageBitmap(),
                                    contentDescription = "Error Diagnostic QR",
                                    modifier = Modifier.size(160.dp).padding(8.dp)
                                )
                            }
                        } else if (!playbackErrorDetails.isNullOrBlank()) {
                            Text(
                                text = playbackErrorDetails,
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                textAlign = TextAlign.Start
                            )
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = onRetryPlayback) {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Retry")
                        }
                        if (decoderPriority != "SW_PREFER") {
                            OutlinedButton(onClick = onSwitchToSoftwareAndRetry) {
                                Text("Try SW Decoder", color = Color.White)
                            }
                        }
                    }
                    if (!playbackErrorDetails.isNullOrBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            TextButton(onClick = { showQrError = !showQrError }) {
                                Text(if (showQrError) "Hide Details" else "Details / QR", color = Color.White.copy(alpha = 0.85f))
                            }
                            TextButton(onClick = onSharePlaybackError) {
                                Text("Share", color = Color.White.copy(alpha = 0.85f))
                            }
                        }
                    }
                    TextButton(onClick = onBackClick) {
                        Text("Close Player", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }
        }

        // ==================== CINEMA HUD CONTROLS ====================
        AnimatedVisibility(
            visible = controlsVisible,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompactWidth) 130.dp else 135.dp)
                        .align(Alignment.TopCenter)
                        .background(Brush.verticalGradient(colors = listOf(Color.Black.copy(alpha = 0.72f), Color.Transparent)))
                )

                val isHudTokenEnabled: (String) -> Boolean = { raw ->
                    when (PlayerControlCatalog.canonicalToken(raw)) {
                        "SCREEN_ROTATION" -> showRotationOverlay
                        "ASPECT_RATIO" -> showAspectOverlay
                        "PLAYBACK_SPEED" -> showSpeedOverlay
                        "DECODER" -> showDecoderOverlay
                        "SHUFFLE" -> showShuffleOverlay
                        "REPEAT_MODE" -> showLoopOverlay
                        "PICTURE_IN_PICTURE" -> showPipOverlay
                        "FRAME_NAVIGATION" -> showScreenshotOverlay
                        else -> true
                    }
                }
                val topRightActions = remember(
                    topRightCsv,
                    showRotationOverlay, showAspectOverlay, showSpeedOverlay, showDecoderOverlay,
                    showShuffleOverlay, showLoopOverlay, showPipOverlay, showScreenshotOverlay
                ) { PlayerControlCatalog.parseSlotCsv(topRightCsv).filter(isHudTokenEnabled) }
                val bottomLeftActions = remember(
                    bottomLeftCsv,
                    showRotationOverlay, showAspectOverlay, showSpeedOverlay, showDecoderOverlay,
                    showShuffleOverlay, showLoopOverlay, showPipOverlay, showScreenshotOverlay
                ) { PlayerControlCatalog.parseSlotCsv(bottomLeftCsv).filter(isHudTokenEnabled) }
                val bottomRightActions = remember(
                    bottomRightCsv,
                    showRotationOverlay, showAspectOverlay, showSpeedOverlay, showDecoderOverlay,
                    showShuffleOverlay, showLoopOverlay, showPipOverlay, showScreenshotOverlay
                ) { PlayerControlCatalog.parseSlotCsv(bottomRightCsv).filter(isHudTokenEnabled) }
                val portraitBottomActions = remember(
                    portraitBottomCsv,
                    showRotationOverlay, showAspectOverlay, showSpeedOverlay, showDecoderOverlay,
                    showShuffleOverlay, showLoopOverlay, showPipOverlay, showScreenshotOverlay
                ) { PlayerControlCatalog.parseSlotCsv(portraitBottomCsv).filter(isHudTokenEnabled) }

                val dispatchActionSheet: (String) -> Unit = { action ->
                    when (PlayerControlCatalog.canonicalToken(action)) {
                        "DECODER" -> showEngineSheet = true
                        "AUDIO_TRACK" -> showAudioSheet = true
                        "SUBTITLES" -> showSubtitlesSheet = true
                        "MORE_OPTIONS" -> showToolsSheet = true
                        "BOOKMARKS_CHAPTERS", "CURRENT_CHAPTER" -> showChaptersSheet = true
                        "VIDEO_QUALITY" -> showQualitySheet = true
                        "EQUALIZER" -> showEqualizerSheet = true
                        "AMBIENT_MODE" -> onToggleAmbientMode()
                        "BACKGROUND_PLAYBACK" -> onToggleBackgroundPlayback()
                        "LOCK_CONTROLS" -> onToggleLock()
                        "SCREEN_ROTATION" -> onToggleOrientation()
                        "PLAYBACK_SPEED" -> showSpeedSheet = true
                        "REPEAT_MODE" -> onRepeatModeCycle()
                        "SHUFFLE" -> onToggleShuffle()
                        "AB_LOOP" -> showAbLoopSheet = true
                        "CUSTOM_SKIP" -> {
                            val stepMs = customSkipSec * 1000L
                            val target = if (durationMs > 0L) (currentPositionMs + stepMs).coerceAtMost(durationMs) else currentPositionMs + stepMs
                            onSeek(target)
                        }
                        "SLEEP_TIMER" -> showSleepTimerSheet = true
                        "MIRROR" -> onToggleMirror()
                        "VERTICAL_FLIP" -> onToggleVerticalFlip()
                        "FRAME_NAVIGATION" -> onCaptureScreenshot()
                        "ASPECT_RATIO" -> showAspectRatioSheet = true
                        "VIDEO_ZOOM" -> showZoomSheet = true
                        "PICTURE_IN_PICTURE" -> onPipClick()
                    }
                }

                // 1. Top Bar
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .displayCutoutPadding()
                        .statusBarsPadding()
                        .padding(
                            start = if (safeAreaWindow) 22.dp else 14.dp,
                            end = if (safeAreaWindow) 22.dp else 14.dp,
                            top = 16.dp,
                            bottom = 8.dp
                        )
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 6.dp else 8.dp)
                    ) {
                        Surface(
                            onClick = onBackClick,
                            shape = CircleShape,
                            color = hudGlassFill,
                            border = BorderStroke(1.dp, hudGlassBorder),
                            modifier = Modifier.size(btnSizeDp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back", tint = Color.White, modifier = Modifier.size(btnIconSizeDp))
                            }
                        }

                        Surface(
                            onClick = { showQueueSheet = true },
                            shape = CircleShape,
                            color = hudGlassFill,
                            border = BorderStroke(1.dp, hudGlassBorder),
                            modifier = Modifier.size(btnSizeDp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.AutoMirrored.Rounded.QueueMusic, contentDescription = "Playlist", tint = Color.White, modifier = Modifier.size(btnIconSizeDp))
                            }
                        }

                        if (hideBtnBg) {
                            Text(
                                text = videoTitle,
                                style = MaterialTheme.typography.titleSmall.copy(
                                    shadow = Shadow(
                                        color = Color.Black.copy(alpha = 0.80f),
                                        offset = Offset(0f, 1.5f),
                                        blurRadius = 6f
                                    )
                                ),
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { showCodecInspectorSheet = true }
                                    .padding(horizontal = 4.dp)
                            )
                        } else {
                            Surface(
                                shape = CircleShape,
                                color = hudGlassFill,
                                border = BorderStroke(1.dp, hudGlassBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(btnSizeDp)
                                    .clickable { showCodecInspectorSheet = true }
                            ) {
                                Box(
                                    contentAlignment = Alignment.CenterStart,
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                ) {
                                    Text(
                                        text = videoTitle,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 5.dp else 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = if (isCompactWidth && topRightActions.size > 3) {
                                Modifier.horizontalScroll(rememberScrollState())
                            } else Modifier
                        ) {
                            topRightActions.forEach { action ->
                                RenderHudActionToken(
                                    token = action,
                                    btnSizeDp = btnSizeDp,
                                    btnIconSizeDp = btnIconSizeDp,
                                    glassFill = hudGlassFill,
                                    glassBorder = hudGlassBorder,
                                    accentColor = playerSheetAccent(),
                                    decoderPriority = decoderPriority,
                                    isHwDecoder = isHwDecoder,
                                    activeDecoderName = activeDecoderName,
                                    subtitlesDisabled = subtitlesDisabled,
                                    orientationMode = orientationMode,
                                    currentSpeed = currentSpeed,
                                    repeatMode = repeatMode,
                                    abLoopActive = abLoopActive,
                                    zoomScale = zoomScale,
                                    aspectRatioMode = aspectRatioMode,
                                    isMirrored = isMirrored,
                                    isFlippedVertical = isFlippedVertical,
                                    isShuffleEnabled = isShuffleEnabled,
                                    ambientEnabled = ambientEnabled,
                                    bgPlaybackEnabled = AppSettings.backgroundPlayback.collectAsState().value,
                                    customSkipSec = customSkipSec,
                                    sleepTimerRemainingMs = sleepTimerRemainingMs,
                                    chapters = chapters,
                                    currentChapterIndex = currentChapterIndex,
                                    onAction = { dispatchActionSheet(action) }
                                )
                            }
                        }
                    }

                    // Runtime Telemetry Strip (real AnalyticsListener values!)
                    if (showHudTelemetry) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 4.dp, top = 8.dp, end = 4.dp)
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TelemetryBadge(text = codecName, borderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.6f), fillColor = hudGlassFill)
                            if (resolutionText.isNotBlank()) {
                                TelemetryBadge(text = resolutionText, borderColor = Color(0xFF0284C7).copy(alpha = 0.65f), fillColor = hudGlassFill)
                            }
                            if (hdrFormatText.isNotBlank() && hdrFormatText != "SDR") {
                                TelemetryBadge(text = hdrFormatText, borderColor = Color(0xFFF59E0B).copy(alpha = 0.7f), fillColor = hudGlassFill)
                            }
                            if (activeDecoderName.isNotBlank()) {
                                TelemetryBadge(
                                    text = "${if (isHwDecoder) "HW" else "SW"}: $activeDecoderName",
                                    borderColor = hudGlassBorder,
                                    fillColor = hudGlassFill,
                                    onClick = { showEngineSheet = true }
                                )
                            }
                            if (bitrateText.isNotBlank()) {
                                TelemetryBadge(text = bitrateText, borderColor = hudGlassBorder, fillColor = hudGlassFill)
                            }
                            if (detectedFps > 1f) {
                                TelemetryBadge(
                                    text = "${detectedFps.roundToInt()} FPS • Drops: $droppedFrames",
                                    borderColor = Color(0xFF10B981).copy(alpha = 0.65f),
                                    fillColor = hudGlassFill
                                )
                            }
                        }
                    }
                }

                // 2. Center Playback Transport Cluster (Portrait "CENTER" option only; Cinema default uses the Bottom Capsule)
                val showCenterTransport = !isLocked && isCompactWidth && portraitCtrlPos == "CENTER"
                if (showCenterTransport) {
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (hasPreviousTrack) {
                            HudCircleButton(
                                icon = Icons.Rounded.SkipPrevious,
                                contentDescription = "Previous Video",
                                size = 44.dp,
                                iconSize = 24.dp,
                                glassFill = hudGlassFill,
                                glassBorder = hudGlassBorder,
                                onClick = onPreviousTrack
                            )
                        }

                        Surface(
                            onClick = {
                                rewindScaleTarget = 0.85f
                                doubleTapRippleSide = "LEFT"
                                onSeek((currentPositionMs - seekIntervalSec * 1000L).coerceAtLeast(0L))
                            },
                            shape = CircleShape,
                            color = hudGlassFill,
                            border = BorderStroke(1.dp, hudGlassBorder),
                            modifier = Modifier
                                .size(50.dp)
                                .scale(rewindScale)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Replay10, contentDescription = "Rewind ${seekIntervalSec}s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                        LaunchedEffect(rewindScaleTarget) {
                            if (rewindScaleTarget != 1.0f) {
                                delay(120L)
                                rewindScaleTarget = 1.0f
                            }
                        }

                        Surface(
                            onClick = {
                                playButtonScaleTarget = 0.9f
                                onPlayPauseClick()
                            },
                            shape = CircleShape,
                            color = hudGlassFill,
                            border = BorderStroke(1.5.dp, Color.White.copy(alpha = if (isLiquidGlass) 0.32f else 0.14f)),
                            modifier = Modifier
                                .size(66.dp)
                                .scale(playButtonScale)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                    contentDescription = if (isPlaying) "Pause" else "Play",
                                    tint = Color.White,
                                    modifier = Modifier.size(36.dp)
                                )
                            }
                        }
                        LaunchedEffect(playButtonScaleTarget) {
                            if (playButtonScaleTarget != 1.0f) {
                                delay(120L)
                                playButtonScaleTarget = 1.0f
                            }
                        }

                        Surface(
                            onClick = {
                                forwardScaleTarget = 0.85f
                                doubleTapRippleSide = "RIGHT"
                                val target = if (durationMs > 0L) (currentPositionMs + seekIntervalSec * 1000L).coerceAtMost(durationMs) else currentPositionMs + seekIntervalSec * 1000L
                                onSeek(target)
                            },
                            shape = CircleShape,
                            color = hudGlassFill,
                            border = BorderStroke(1.dp, hudGlassBorder),
                            modifier = Modifier
                                .size(50.dp)
                                .scale(forwardScale)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Rounded.Forward10, contentDescription = "Forward ${seekIntervalSec}s", tint = Color.White, modifier = Modifier.size(28.dp))
                            }
                        }
                        LaunchedEffect(forwardScaleTarget) {
                            if (forwardScaleTarget != 1.0f) {
                                delay(120L)
                                forwardScaleTarget = 1.0f
                            }
                        }

                        if (hasNextTrack) {
                            HudCircleButton(
                                icon = Icons.Rounded.SkipNext,
                                contentDescription = "Next Video",
                                size = 44.dp,
                                iconSize = 24.dp,
                                glassFill = hudGlassFill,
                                glassBorder = hudGlassBorder,
                                onClick = onNextTrack
                            )
                        }
                    }
                }

                // Bottom Gradient Scrim
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(if (isCompactWidth) 215.dp else 220.dp)
                        .align(Alignment.BottomCenter)
                        .background(Brush.verticalGradient(colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.84f))))
                )

                // 3. Bottom Quick-Action Strip + Full-Width Seekbar + Floating Frosted Capsule
                if (!isLocked) {
                    val bgPlayEnabled by AppSettings.backgroundPlayback.collectAsState()
                    val sheetAccentColor = playerSheetAccent()
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                            .navigationBarsPadding()
                            .padding(
                                horizontal = if (safeAreaWindow) 22.dp else 16.dp,
                                vertical = if (isCompactWidth) 10.dp else 12.dp
                            )
                    ) {
                        if (isCompactWidth && portraitBottomActions.isNotEmpty()) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp)
                                    .then(if (portraitBottomActions.size > 6) Modifier.horizontalScroll(rememberScrollState()) else Modifier),
                                horizontalArrangement = if (portraitBottomActions.size <= 5) {
                                    Arrangement.spacedBy(8.dp, Alignment.End)
                                } else {
                                    Arrangement.SpaceEvenly
                                },
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                portraitBottomActions.forEach { action ->
                                    RenderHudActionToken(
                                        token = action,
                                        btnSizeDp = bottomBtnSizeDp,
                                        btnIconSizeDp = bottomBtnIconSizeDp,
                                        glassFill = hudGlassFill,
                                        glassBorder = hudGlassBorder,
                                        accentColor = sheetAccentColor,
                                        decoderPriority = decoderPriority,
                                        isHwDecoder = isHwDecoder,
                                        activeDecoderName = activeDecoderName,
                                        subtitlesDisabled = subtitlesDisabled,
                                        orientationMode = orientationMode,
                                        currentSpeed = currentSpeed,
                                        repeatMode = repeatMode,
                                        abLoopActive = abLoopActive,
                                        zoomScale = zoomScale,
                                        aspectRatioMode = aspectRatioMode,
                                        isMirrored = isMirrored,
                                        isFlippedVertical = isFlippedVertical,
                                        isShuffleEnabled = isShuffleEnabled,
                                        ambientEnabled = ambientEnabled,
                                        bgPlaybackEnabled = bgPlayEnabled,
                                        customSkipSec = customSkipSec,
                                        sleepTimerRemainingMs = sleepTimerRemainingMs,
                                        chapters = chapters,
                                        currentChapterIndex = currentChapterIndex,
                                        onAction = { dispatchActionSheet(action) }
                                    )
                                }
                            }
                        } else {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 6.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(bottomClusterGapDp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    bottomLeftActions.forEach { action ->
                                        RenderHudActionToken(
                                            token = action,
                                            btnSizeDp = bottomBtnSizeDp,
                                            btnIconSizeDp = bottomBtnIconSizeDp,
                                            glassFill = hudGlassFill,
                                            glassBorder = hudGlassBorder,
                                            accentColor = sheetAccentColor,
                                            decoderPriority = decoderPriority,
                                            isHwDecoder = isHwDecoder,
                                            activeDecoderName = activeDecoderName,
                                            subtitlesDisabled = subtitlesDisabled,
                                            orientationMode = orientationMode,
                                            currentSpeed = currentSpeed,
                                            repeatMode = repeatMode,
                                            abLoopActive = abLoopActive,
                                            zoomScale = zoomScale,
                                            aspectRatioMode = aspectRatioMode,
                                            isMirrored = isMirrored,
                                            isFlippedVertical = isFlippedVertical,
                                            isShuffleEnabled = isShuffleEnabled,
                                            ambientEnabled = ambientEnabled,
                                            bgPlaybackEnabled = bgPlayEnabled,
                                            customSkipSec = customSkipSec,
                                            sleepTimerRemainingMs = sleepTimerRemainingMs,
                                            chapters = chapters,
                                            currentChapterIndex = currentChapterIndex,
                                            onAction = { dispatchActionSheet(action) }
                                        )
                                    }
                                }
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(bottomClusterGapDp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    bottomRightActions.forEach { action ->
                                        RenderHudActionToken(
                                            token = action,
                                            btnSizeDp = bottomBtnSizeDp,
                                            btnIconSizeDp = bottomBtnIconSizeDp,
                                            glassFill = hudGlassFill,
                                            glassBorder = hudGlassBorder,
                                            accentColor = sheetAccentColor,
                                            decoderPriority = decoderPriority,
                                            isHwDecoder = isHwDecoder,
                                            activeDecoderName = activeDecoderName,
                                            subtitlesDisabled = subtitlesDisabled,
                                            orientationMode = orientationMode,
                                            currentSpeed = currentSpeed,
                                            repeatMode = repeatMode,
                                            abLoopActive = abLoopActive,
                                            zoomScale = zoomScale,
                                            aspectRatioMode = aspectRatioMode,
                                            isMirrored = isMirrored,
                                            isFlippedVertical = isFlippedVertical,
                                            isShuffleEnabled = isShuffleEnabled,
                                            ambientEnabled = ambientEnabled,
                                            bgPlaybackEnabled = bgPlayEnabled,
                                            customSkipSec = customSkipSec,
                                            sleepTimerRemainingMs = sleepTimerRemainingMs,
                                            chapters = chapters,
                                            currentChapterIndex = currentChapterIndex,
                                            onAction = { dispatchActionSheet(action) }
                                        )
                                    }
                                }
                            }
                        }

                        // Seekbar Capsule (All 9 styles, live scrub bubble, Chapter notches, A-B markers)
                        val progressFraction = (if (durationMs > 0L) currentPositionMs.toFloat() / durationMs else 0f).coerceIn(0f, 1f)
                        val bufferedFraction = (if (durationMs > 0L) bufferedPositionMs.toFloat() / durationMs else 0f).coerceIn(0f, 1f)
                        var seekBarWidthPx by remember { mutableFloatStateOf(1f) }
                        var isScrubbingSeekbar by remember { mutableStateOf(false) }
                        var scrubFraction by remember { mutableStateOf<Float?>(null) }
                        var lastScrubDispatchMs by remember { mutableLongStateOf(0L) }
                        val latestDurForSeekbar by rememberUpdatedState(durationMs)
                        val activeFraction = scrubFraction ?: progressFraction
                        val activeDisplayMs = if (scrubFraction != null && durationMs > 0L) {
                            (scrubFraction!! * durationMs).toLong().coerceIn(0L, durationMs)
                        } else {
                            currentPositionMs
                        }

                        val wavePhase by rememberInfiniteTransition(label = "wavySeekbar").animateFloat(
                            initialValue = 0f,
                            targetValue = (2f * PI).toFloat(),
                            animationSpec = infiniteRepeatable(
                                animation = tween(durationMillis = 1400, easing = LinearEasing),
                                repeatMode = RepeatMode.Restart
                            ),
                            label = "wavePhase"
                        )

                        AnimatedVisibility(
                            visible = isScrubbingSeekbar && durationMs > 0L,
                            enter = fadeIn(tween(120)) + scaleIn(initialScale = 0.92f),
                            exit = fadeOut(tween(120)) + scaleOut(targetScale = 0.92f),
                            modifier = Modifier
                                .align(Alignment.CenterHorizontally)
                                .padding(bottom = 6.dp)
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color(0xE60B0E14),
                                border = BorderStroke(1.dp, sheetAccentColor.copy(alpha = 0.7f)),
                                shadowElevation = 8.dp
                            ) {
                                Text(
                                    text = "${formatDuration(activeDisplayMs)} / ${formatDuration(durationMs)}",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                                )
                            }
                        }

                        val timeShadow = Shadow(
                            color = Color.Black.copy(alpha = 0.85f),
                            offset = Offset(0f, 1.5f),
                            blurRadius = 5f
                        )
                        val remainingMs = (durationMs - activeDisplayMs).coerceAtLeast(0L)
                        val leftTimeLabel: @Composable () -> Unit = {
                            if (showSeekTimeInHud) {
                                Text(
                                    text = formatDuration(activeDisplayMs),
                                    style = MaterialTheme.typography.labelMedium.copy(shadow = timeShadow),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false
                                )
                            }
                        }
                        val rightTimeLabel: @Composable () -> Unit = {
                            if (showSeekTimeInHud) {
                                Text(
                                    text = if (showRemainingTime && durationMs > 0L) "-${formatDuration(remainingMs)}" else formatDuration(durationMs),
                                    style = MaterialTheme.typography.labelMedium.copy(shadow = timeShadow),
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    softWrap = false,
                                    modifier = Modifier.clickable { showRemainingTime = !showRemainingTime }
                                )
                            }
                        }
                        val seekbarInteractiveBox: @Composable (Modifier) -> Unit = { boxModifier ->
                            Box(
                                modifier = boxModifier
                                    .height(26.dp)
                                    .onGloballyPositioned { seekBarWidthPx = it.size.width.toFloat().coerceAtLeast(1f) }
                                    .pointerInput(Unit) {
                                        detectTapGestures { offset ->
                                            val dur = latestDurForSeekbar
                                            if (dur > 0L) {
                                                val frac = (offset.x / seekBarWidthPx).coerceIn(0f, 1f)
                                                onSeek((frac * dur).toLong())
                                            }
                                        }
                                    }
                                    .pointerInput(Unit) {
                                        detectHorizontalDragGestures(
                                            onDragStart = { startOffset ->
                                                isScrubbingSeekbar = true
                                                scrubFraction = (startOffset.x / seekBarWidthPx).coerceIn(0f, 1f)
                                            },
                                            onDragEnd = {
                                                val finalFrac = scrubFraction
                                                isScrubbingSeekbar = false
                                                scrubFraction = null
                                                val dur = latestDurForSeekbar
                                                if (finalFrac != null && dur > 0L) {
                                                    onSeek((finalFrac * dur).toLong())
                                                }
                                            },
                                            onDragCancel = {
                                                isScrubbingSeekbar = false
                                                scrubFraction = null
                                            }
                                        ) { change, _ ->
                                            change.consume()
                                            val dur = latestDurForSeekbar
                                            if (dur > 0L) {
                                                val frac = (change.position.x / seekBarWidthPx).coerceIn(0f, 1f)
                                                scrubFraction = frac
                                                val now = SystemClock.uptimeMillis()
                                                if (now - lastScrubDispatchMs >= 180L) {
                                                    lastScrubDispatchMs = now
                                                    onSeek((frac * dur).toLong())
                                                }
                                            }
                                        }
                                    },
                                contentAlignment = Alignment.CenterStart
                            ) {
                                MpvxSeekbarCanvas(
                                    isScrubbing = isScrubbingSeekbar,
                                    seekbarStyle = seekbarStyle,
                                    progressFraction = activeFraction,
                                    bufferedFraction = if (showBufferedRangeOnSeekbar) bufferedFraction else 0f,
                                    durationMs = durationMs,
                                    accentColor = sheetAccentColor,
                                    wavePhase = wavePhase,
                                    chapters = if (showChaptersOnSeekbar) chapters else emptyList(),
                                    introOutroSegments = if (introOutroEnabled) introLookupResult.segments else emptyList(),
                                    abLoopStartMs = abLoopStartMs,
                                    abLoopEndMs = abLoopEndMs,
                                    abLoopActive = abLoopActive,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }

                        if (showSeekbarOuter) {
                            Surface(
                                shape = RoundedCornerShape(26.dp),
                                color = if (hudGlassFill == Color.Transparent) Color(0x8A0B0E14) else hudGlassFill,
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    leftTimeLabel()
                                    seekbarInteractiveBox(Modifier.weight(1f).padding(horizontal = 12.dp))
                                    rightTimeLabel()
                                }
                            }
                        } else {
                            Column(modifier = Modifier.fillMaxWidth()) {
                                seekbarInteractiveBox(Modifier.fillMaxWidth())
                                if (showSeekTimeInHud) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 2.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        leftTimeLabel()
                                        rightTimeLabel()
                                    }
                                }
                            }
                        }

                        // Centered Floating Frosted Transport Capsule (Matches Vertical & Horizontal Cinema HUD)
                        Spacer(modifier = Modifier.height(if (isCompactWidth) 6.dp else 4.dp))
                        val capsuleFill = when {
                            forceDarkBtnBg -> Color(0xDB080B10)
                            hudTheme == "GLOSSY" -> sheetAccentColor.copy(alpha = 0.24f)
                            hudTheme == "CLASSIC" -> Color(0x940D1219)
                            else -> Color(0x7810161F)
                        }
                        val capsuleBorderColor = when {
                            hudTheme == "GLOSSY" -> Color.White.copy(alpha = 0.30f)
                            else -> Color.White.copy(alpha = 0.20f)
                        }
                        val capBtnSize = if (isCompactWidth) 36.dp else 44.dp
                        val capIconSize = if (isCompactWidth) 20.dp else 22.dp

                        Surface(
                            shape = RoundedCornerShape(26.dp),
                            color = capsuleFill,
                            border = BorderStroke(1.dp, capsuleBorderColor),
                            shadowElevation = 6.dp,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Row(
                                modifier = Modifier.padding(
                                    horizontal = if (isCompactWidth) 10.dp else 16.dp,
                                    vertical = 4.dp
                                ),
                                horizontalArrangement = Arrangement.spacedBy(if (isCompactWidth) 4.dp else 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                IconButton(
                                    onClick = onToggleLock,
                                    modifier = Modifier.size(capBtnSize)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Lock,
                                        contentDescription = "Lock Controls",
                                        tint = Color.White,
                                        modifier = Modifier.size(capIconSize)
                                    )
                                }

                                IconButton(
                                    onClick = onToggleOrientation,
                                    modifier = Modifier.size(capBtnSize)
                                ) {
                                    Icon(
                                        painter = painterResource(
                                            when {
                                                orientationMode.contains("Landscape") -> R.drawable.ic_screen_lock_landscape_24dp
                                                orientationMode.contains("Portrait") -> R.drawable.ic_screen_lock_portrait_24dp
                                                else -> R.drawable.ic_auto_rotate_24dp
                                            }
                                        ),
                                        contentDescription = "Orientation: $orientationMode",
                                        tint = if (orientationMode != "Auto-Rotate" && orientationMode != "Auto") sheetAccentColor else Color.White,
                                        modifier = Modifier.size(capIconSize)
                                    )
                                }

                                if (!showCenterTransport) {
                                    IconButton(
                                        onClick = {
                                            if (hasPreviousTrack) {
                                                onPreviousTrack()
                                            } else {
                                                rewindScaleTarget = 0.85f
                                                doubleTapRippleSide = "LEFT"
                                                onSeek((currentPositionMs - seekIntervalSec * 1000L).coerceAtLeast(0L))
                                            }
                                        },
                                        modifier = Modifier
                                            .size(capBtnSize)
                                            .scale(rewindScale)
                                    ) {
                                        Icon(
                                            imageVector = if (hasPreviousTrack) Icons.Rounded.SkipPrevious else Icons.Rounded.Replay10,
                                            contentDescription = if (hasPreviousTrack) "Previous Video" else "Rewind ${seekIntervalSec}s",
                                            tint = Color.White,
                                            modifier = Modifier.size(capIconSize + 2.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            playButtonScaleTarget = 0.9f
                                            onPlayPauseClick()
                                        },
                                        modifier = Modifier
                                            .size(if (isCompactWidth) 44.dp else 50.dp)
                                            .scale(playButtonScale)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                                            contentDescription = if (isPlaying) "Pause" else "Play",
                                            tint = Color.White,
                                            modifier = Modifier.size(if (isCompactWidth) 28.dp else 32.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            if (hasNextTrack) {
                                                onNextTrack()
                                            } else {
                                                forwardScaleTarget = 0.85f
                                                doubleTapRippleSide = "RIGHT"
                                                val target = if (durationMs > 0L) (currentPositionMs + seekIntervalSec * 1000L).coerceAtMost(durationMs) else currentPositionMs + seekIntervalSec * 1000L
                                                onSeek(target)
                                            }
                                        },
                                        modifier = Modifier
                                            .size(capBtnSize)
                                            .scale(forwardScale)
                                    ) {
                                        Icon(
                                            imageVector = if (hasNextTrack) Icons.Rounded.SkipNext else Icons.Rounded.Forward10,
                                            contentDescription = if (hasNextTrack) "Next Video" else "Forward ${seekIntervalSec}s",
                                            tint = Color.White,
                                            modifier = Modifier.size(capIconSize + 2.dp)
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = {
                                        if (queueTitles.size > 1) onToggleShuffle() else showAbLoopSheet = true
                                    },
                                    modifier = Modifier.size(capBtnSize)
                                ) {
                                    Icon(
                                        imageVector = if (queueTitles.size > 1) Icons.Rounded.Shuffle else Icons.Rounded.Loop,
                                        contentDescription = if (queueTitles.size > 1) "Shuffle Queue" else "A-B Loop",
                                        tint = if ((queueTitles.size > 1 && isShuffleEnabled) || (queueTitles.size <= 1 && abLoopActive)) sheetAccentColor else Color.White,
                                        modifier = Modifier.size(capIconSize)
                                    )
                                }

                                IconButton(
                                    onClick = { showAspectRatioSheet = true },
                                    modifier = Modifier.size(capBtnSize)
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.AspectRatio,
                                        contentDescription = "Aspect Ratio ($aspectRatioMode)",
                                        tint = if (aspectRatioMode != "Fit") sheetAccentColor else Color.White,
                                        modifier = Modifier.size(capIconSize)
                                    )
                                }
                            }
                        }
                    }
                }

                // Clickable & Swipeable mid-right `<` edge handle for 3-column grid drawer
                if (!isLocked && !isInPipMode) {
                    PlayerControlEdgeHandle(
                        visible = controlsDrawerEnabled && !showEdgeDrawer,
                        glassFill = hudGlassFill,
                        glassBorder = hudGlassBorder,
                        onOpenDrawer = { showEdgeDrawer = true },
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                    )
                }

                // Interactive Slide-to-Unlock Pill
                if (isLocked) {
                    val lockAccent = playerSheetAccent()
                    val lockOnAccent = playerOnAccentColor(lockAccent)
                    SlideToUnlockPill(
                        accentColor = lockAccent,
                        onAccentColor = lockOnAccent,
                        onUnlock = onToggleLock,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 32.dp)
                    )
                }

                // Auto-skip trigger for active intro / outro segment (deduplicated per segment)
                LaunchedEffect(activeIntroOutroSegment, autoSkipIntro, autoSkipOutro) {
                    val seg = activeIntroOutroSegment ?: return@LaunchedEffect
                    val shouldAuto = when (seg.kind) {
                        IntroSegmentKind.INTRO, IntroSegmentKind.RECAP -> autoSkipIntro
                        IntroSegmentKind.OUTRO, IntroSegmentKind.PREVIEW -> autoSkipOutro
                    }
                    val segKey = "${seg.kind.name}_${seg.startMs}"
                    if (shouldAuto && segKey !in autoSkippedSegmentKeys.value) {
                        autoSkippedSegmentKeys.value = autoSkippedSegmentKeys.value + segKey
                        val target = if (durationMs > 0L) seg.endMs.coerceAtMost(durationMs) else seg.endMs
                        onSeek(target)
                        actionFeedbackPillText = "Auto-skipped ${seg.kind.label}"
                    }
                }

                // Floating non-blocking Skip Intro / Skip Outro Pill
                AnimatedVisibility(
                    visible = !isLocked && !isInPipMode && activeIntroOutroSegment != null,
                    enter = fadeIn(tween(180)) + scaleIn(initialScale = 0.92f),
                    exit = fadeOut(tween(150)) + scaleOut(targetScale = 0.92f),
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .navigationBarsPadding()
                        .padding(
                            end = 20.dp,
                            bottom = if (controlsVisible) 136.dp else 36.dp
                        )
                ) {
                    val seg = activeIntroOutroSegment
                    if (seg != null) {
                        Surface(
                            onClick = {
                                val target = if (durationMs > 0L) seg.endMs.coerceAtMost(durationMs) else seg.endMs
                                val segKey = "${seg.kind.name}_${seg.startMs}"
                                autoSkippedSegmentKeys.value = autoSkippedSegmentKeys.value + segKey
                                onSeek(target)
                                actionFeedbackPillText = "Skipped ${seg.kind.label}"
                            },
                            shape = CircleShape,
                            color = playerSheetAccent(),
                            shadowElevation = 6.dp
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.SkipNext,
                                    contentDescription = seg.kind.buttonLabel,
                                    tint = playerOnAccentColor(playerSheetAccent()),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = seg.kind.buttonLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = playerOnAccentColor(playerSheetAccent())
                                )
                            }
                        }
                    }
                }

                // Optional subtle Intro/Outro provider status indicator when enabled
                AnimatedVisibility(
                    visible = showProviderStatusOverlay && introOutroEnabled && !isInPipMode && controlsVisible && introLookupResult.statusMessage.isNotBlank(),
                    enter = fadeIn(tween(180)),
                    exit = fadeOut(tween(150)),
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .statusBarsPadding()
                        .padding(top = 62.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color(0xCC0D111A),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.16f))
                    ) {
                        Text(
                            text = introLookupResult.statusMessage,
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.88f),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }

    val sheetAccent = playerSheetAccent()
    val onSheetAccent = playerOnAccentColor(sheetAccent)

    // 1. Decoder & Playback Engine Sheet (Real HW/SW Priority + Active Decoder + 120Hz Refresh)
    if (showEngineSheet) {
        ModalBottomSheet(
            onDismissRequest = { showEngineSheet = false },
            sheetState = sheetState,
            containerColor = activeSheetColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isMpvActive = activeEngine.equals("MPV", ignoreCase = true)
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isMpvActive) "MPV Engine & Shaders" else "Native ExoPlayer & ASS",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = if (activeDecoderName.isNotBlank()) {
                                "${if (isMpvActive) "MPV" else "ExoPlayer"} • $activeDecoderName"
                            } else {
                                if (isMpvActive) "Engine: libmpv Dual Pipeline" else "Engine: AndroidX Media3 + libass"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = sheetAccent.copy(alpha = 0.2f)) {
                        Text(
                            text = "${if (isMpvActive) "MPV" else "EXO"} • ${if (isHwDecoder) "HW" else "SW"}",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = sheetAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Text("Switch Playback Engine", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))
                // Compact Segmented Switcher (`Auto | MPV | Native`)
                val defaultEnginePref by AppSettings.defaultEngine.collectAsState()
                val engineChoices = listOf(
                    "AUTO" to "Auto",
                    "MPV" to "MPV",
                    "NATIVE" to "Native"
                )
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        engineChoices.forEach { (engineKey, engineLabel) ->
                            val isEngineSelected = when (engineKey) {
                                "MPV" -> activeEngine.equals("MPV", ignoreCase = true) && defaultEnginePref != "AUTO"
                                "NATIVE" -> !activeEngine.equals("MPV", ignoreCase = true) && defaultEnginePref != "AUTO"
                                else -> defaultEnginePref == "AUTO"
                            }
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isEngineSelected) sheetAccent else Color.Transparent,
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        onEngineSwitch(engineKey)
                                        if (showActionFeedbackPills) {
                                            actionFeedbackPillText = "Engine: $engineLabel"
                                        }
                                        showEngineSheet = false
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 10.dp)) {
                                    Text(
                                        text = engineLabel,
                                        color = if (isEngineSelected) onSheetAccent else Color.White.copy(alpha = 0.85f),
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = if (isEngineSelected) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
                Text("Decoder Priority", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(8.dp))

                val priorities = listOf(
                    "AUTO" to "Auto (HW with FFmpeg/SW fallback)",
                    "HW_ONLY" to "Hardware Decoders Only",
                    "SW_PREFER" to "Prefer Software / Extension Decoders"
                )
                priorities.forEach { (key, desc) ->
                    val selected = decoderPriority == key
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable {
                                onDecoderPriorityChange(key)
                                showEngineSheet = false
                            }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = when (key) {
                                    "HW_ONLY" -> "HW Only"
                                    "SW_PREFER" -> "SW Prefer"
                                    else -> "Auto (HW+)"
                                },
                                color = if (selected) sheetAccent else Color.White,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                            )
                            Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.65f))
                        }
                        if (selected) Icon(Icons.Rounded.Check, contentDescription = null, tint = sheetAccent)
                    }
                }

                if (activeEngine.equals("MPV", ignoreCase = true)) {
                    val animeMode by AppSettings.anime4kMode.collectAsState()
                    val debandMode by AppSettings.mpvDebanding.collectAsState()
                    val vulkanEnabled by AppSettings.useVulkan.collectAsState()

                    Spacer(modifier = Modifier.height(14.dp))
                    Text("Anime4K Shader (MPV)", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("OFF" to "Off", "MODE_A" to "Mode A", "MODE_B" to "Mode B", "MODE_C" to "Mode C").forEach { (k, lbl) ->
                            val sel = animeMode == k
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (sel) sheetAccent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { AppSettings.setAnime4kMode(k) }
                            ) {
                                Text(
                                    text = lbl,
                                    color = if (sel) sheetAccent else Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Debanding", style = MaterialTheme.typography.labelSmall, fontWeight = FontWeight.Bold, color = Color.White.copy(alpha = 0.7f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("NONE" to "Off", "GPU" to "GPU", "CPU" to "CPU").forEach { (k, lbl) ->
                            val sel = debandMode == k
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (sel) sheetAccent.copy(alpha = 0.22f) else Color.White.copy(alpha = 0.08f),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { AppSettings.setMpvDebanding(k) }
                            ) {
                                Text(
                                    text = lbl,
                                    color = if (sel) sheetAccent else Color.White,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium,
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Vulkan GPU Backend", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                            Text("Auto-falls back to OpenGL if unsupported", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                        }
                        Switch(
                            checked = vulkanEnabled,
                            onCheckedChange = { AppSettings.setUseVulkan(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = onSheetAccent, checkedTrackColor = sheetAccent)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Match Display Frame Rate", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text(
                            text = if (smoothMotionEnabled) "Active ($targetFps Hz display cap)" else "System default refresh rate",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.6f)
                        )
                    }
                    Switch(
                        checked = smoothMotionEnabled,
                        onCheckedChange = { onToggleSmoothMotion() },
                        colors = SwitchDefaults.colors(checkedThumbColor = onSheetAccent, checkedTrackColor = sheetAccent)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // 2. Aspect Ratio Sheet
    if (showAspectRatioSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAspectRatioSheet = false },
            sheetState = sheetState,
            containerColor = activeSheetColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("Aspect Ratio & Framing", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Transform video container aspect ratio immediately", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(14.dp))

                val ratios = listOf(
                    "Fit" to "Best fit preserving natural ratio",
                    "Fill" to "Fill screen (crop excess edges)",
                    "Zoom" to "Zoom to fill viewport",
                    "Stretch" to "Stretch frame to fill screen bounds",
                    "Original" to "1:1 native video pixel dimensions",
                    "16:9" to "Standard Widescreen (16:9)",
                    "4:3" to "Classic Broadcast (4:3)",
                    "21:9" to "Ultrawide Cinema (21:9)",
                    "16:10" to "Display 16:10",
                    "2.35:1" to "Cinemascope Anamorphic"
                )

                LazyColumn(modifier = Modifier.fillMaxWidth().height(280.dp)) {
                    items(ratios) { (mode, desc) ->
                        val isSelected = aspectRatioMode.equals(mode, ignoreCase = true) ||
                            (mode == "Fit" && aspectRatioMode == "Default") ||
                            (mode == "Fill" && aspectRatioMode == "Crop")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onAspectRatioChange(mode)
                                    showAspectRatioSheet = false
                                }
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = mode,
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) sheetAccent else Color.White
                                )
                                Text(desc, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                            }
                            if (isSelected) {
                                Icon(Icons.Rounded.Check, contentDescription = null, tint = sheetAccent, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 2B. Dedicated Zoom & Pan Control Sheet (Fixes Zoom button opening wrong dialog!)
    if (showZoomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showZoomSheet = false },
            sheetState = sheetState,
            containerColor = activeSheetColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Video Zoom & Pan", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text("${(zoomScale * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = sheetAccent)
                }
                Text("Subtitles stay anchored in place while video zooms.", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))

                Slider(
                    value = zoomScale.coerceIn(0.5f, 4.0f),
                    onValueChange = { onZoomPanChange(it, if (it <= 1.0f) 0f else panOffsetX, if (it <= 1.0f) 0f else panOffsetY) },
                    valueRange = 0.5f..4.0f,
                    colors = SliderDefaults.colors(thumbColor = sheetAccent, activeTrackColor = sheetAccent)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    listOf(0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f).forEach { level ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (abs(zoomScale - level) < 0.05f) sheetAccent else Color.White.copy(alpha = 0.12f),
                            modifier = Modifier.clickable { onZoomPanChange(level, 0f, 0f) }
                        ) {
                            Text(
                                text = "${(level * 100).roundToInt()}%",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = if (abs(zoomScale - level) < 0.05f) onSheetAccent else Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Button(
                    onClick = {
                        onZoomPanChange(1.0f, 0f, 0f)
                        showZoomSheet = false
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Reset to 100% Fit")
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 3. Playback Speed Sheet (0.25x - 4.0x)
    if (showSpeedSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSpeedSheet = false },
            sheetState = sheetState,
            containerColor = activeSheetColor
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
                    Text("Playback Speed", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(String.format(Locale.US, "%.2fx", currentSpeed), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = sheetAccent)
                }

                Spacer(modifier = Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { onSpeedChange((currentSpeed - 0.1f).coerceIn(0.25f, 4.0f)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Remove, contentDescription = "Slower", tint = Color.White)
                    }
                    Slider(
                        value = currentSpeed.coerceIn(0.25f, 4.0f),
                        onValueChange = {
                            val snapped = ((it * 20f).roundToInt() / 20f).coerceIn(0.25f, 4.0f)
                            onSpeedChange(snapped)
                        },
                        valueRange = 0.25f..4.0f,
                        colors = SliderDefaults.colors(thumbColor = sheetAccent, activeTrackColor = sheetAccent),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = { onSpeedChange((currentSpeed + 0.1f).coerceIn(0.25f, 4.0f)) },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Add, contentDescription = "Faster", tint = Color.White)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                val presets = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 2.0f, 3.0f, 4.0f)
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    presets.take(5).forEach { sp ->
                        SpeedPresetPill(speed = sp, isSelected = abs(currentSpeed - sp) < 0.02f) { onSpeedChange(sp) }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    presets.drop(5).forEach { sp ->
                        SpeedPresetPill(speed = sp, isSelected = abs(currentSpeed - sp) < 0.02f) { onSpeedChange(sp) }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Audio Pitch Correction", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("Keep natural vocal pitch at fast/slow speeds", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
                    }
                    Switch(
                        checked = audioPitchCorrection,
                        onCheckedChange = {
                            AppSettings.setAudioPitchCorrection(it)
                            onSpeedChange(currentSpeed)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = onSheetAccent, checkedTrackColor = sheetAccent)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            AppSettings.setDefaultSpeed(currentSpeed)
                            if (showActionFeedbackPills) {
                                actionFeedbackPillText = "Default speed: ${String.format(Locale.US, "%.2fx", currentSpeed)}"
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Make Default", color = Color.White)
                    }
                    Button(
                        onClick = {
                            onSpeedChange(1.0f)
                            AppSettings.setDefaultSpeed(1.0f)
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Reset (1.0x)")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // 4. FPS-Accurate Frame Stepper Sheet
    if (showFrameNavSheet) {
        val effectiveFps = if (detectedFps > 1f) detectedFps else 24f
        val frameDurationMs = (1000f / effectiveFps)
        ModalBottomSheet(
            onDismissRequest = { showFrameNavSheet = false },
            sheetState = sheetState,
            containerColor = activeSheetColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("Frame-by-Frame Navigation", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Detected rate: ${String.format(Locale.US, "%.2f", effectiveFps)} fps (~${frameDurationMs.roundToInt()} ms/frame)", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(formatDuration(currentPositionMs), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Frame #${(currentPositionMs / frameDurationMs).toLong()}", style = MaterialTheme.typography.labelMedium, color = sheetAccent)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                    FrameNavButton("-5s") { onSeek((currentPositionMs - 5000L).coerceAtLeast(0L)) }
                    FrameNavButton("-1s") { onSeek((currentPositionMs - 1000L).coerceAtLeast(0L)) }
                    FrameNavButton("◀ 1F") { onFrameStep(-1) }
                    FrameNavButton("1F ▶") { onFrameStep(1) }
                    FrameNavButton("+1s") { onSeek((currentPositionMs + 1000L).coerceAtMost(durationMs.coerceAtLeast(0L))) }
                    FrameNavButton("+5s") { onSeek((currentPositionMs + 5000L).coerceAtMost(durationMs.coerceAtLeast(0L))) }
                }

                Spacer(modifier = Modifier.height(18.dp))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = sheetAccent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onCaptureScreenshot() }
                ) {
                    Row(
                        modifier = Modifier.padding(vertical = 12.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.CameraAlt, contentDescription = null, tint = onSheetAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Capture Current Frame (PNG)", style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.Bold, color = onSheetAccent)
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // 5. Real A-B Segment Loop Sheet (Replaces Fake Clip Cutter Export!)
    if (showAbLoopSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAbLoopSheet = false },
            sheetState = sheetState,
            containerColor = activeSheetColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("A-B Segment Loop", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Text("Continuously repeat a selected scene between points A and B", style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.7f))

                Spacer(modifier = Modifier.height(16.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSetAbStart(currentPositionMs) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Point A (Start)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (abLoopStartMs >= 0L) formatDuration(abLoopStartMs) else "Not Set",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = sheetAccent
                            )
                            Text("Tap to set current", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color.White.copy(alpha = 0.1f),
                        modifier = Modifier
                            .weight(1f)
                            .clickable { onSetAbEnd(currentPositionMs) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Point B (End)", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (abLoopEndMs >= 0L) formatDuration(abLoopEndMs) else "Not Set",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = sheetAccent
                            )
                            Text("Tap to set current", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.5f))
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                val isValidRange = abLoopStartMs >= 0L && abLoopEndMs > abLoopStartMs + 200L
                Text(
                    text = if (isValidRange) "Loop Segment: ${formatDuration(abLoopEndMs - abLoopStartMs)}" else "Set Point A before Point B to enable A-B Loop",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.75f)
                )

                Spacer(modifier = Modifier.height(14.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedButton(
                        onClick = { onClearAbLoop() },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text("Clear", color = Color.White)
                    }
                    Button(
                        onClick = { onToggleAbLoop(!abLoopActive) },
                        enabled = isValidRange,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(if (abLoopActive) "Disable Loop" else "Enable Loop")
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // 6. Runtime Telemetry & Hardware Codec Capabilities Inspector Sheet
    if (showCodecInspectorSheet) {
        val inspectionResult = remember { CodecInspector.inspect(context) }
        ModalBottomSheet(
            onDismissRequest = { showCodecInspectorSheet = false },
            sheetState = sheetState,
            containerColor = activeSheetColor
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
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Stream Telemetry & Codecs", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                        Text(
                            text = "${inspectionResult.hardwareCount} HW Codecs • ${inspectionResult.videoCount} Video Decoders",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                    Surface(shape = RoundedCornerShape(8.dp), color = sheetAccent.copy(alpha = 0.2f)) {
                        Text(
                            text = if (isHwDecoder) "HW" else "SW",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = sheetAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color.White.copy(alpha = 0.08f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Active Decoder: ${activeDecoderName.ifBlank { "Initializing" }}", color = Color.White, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.SemiBold)
                        Text(
                            text = listOfNotNull(
                                codecName.takeIf { it.isNotBlank() },
                                resolutionText.takeIf { it.isNotBlank() },
                                hdrFormatText.takeIf { it.isNotBlank() },
                                bitrateText.takeIf { it.isNotBlank() },
                                "Audio: $channelLayoutText"
                            ).joinToString(" • "),
                            color = Color.White.copy(alpha = 0.75f),
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text("FPS: ${String.format(Locale.US, "%.2f", detectedFps)} • Dropped Frames: $droppedFrames", color = sheetAccent, style = MaterialTheme.typography.labelSmall)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().height(220.dp)) {
                    items(inspectionResult.keySummaries) { summary ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color.White.copy(alpha = 0.08f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(summary.formatName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = Color.White)
                                    Surface(shape = RoundedCornerShape(6.dp), color = if (summary.hasHardware) Color(0xFF059669) else Color(0xFFE11D48)) {
                                        Text(
                                            text = if (summary.hasHardware) "HARDWARE" else "SOFTWARE",
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = Color.White,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Max: ${summary.maxResolution ?: "4K UHD"} • 10-Bit: ${if (summary.is10BitSupported) "Yes" else "No"} • HDR: ${if (summary.isHdrSupported) "Yes" else "No"}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.7f)
                                )
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 7. Audio Tracks, Sync Delay & Real Equalizer Sheet
    if (showAudioSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAudioSheet = false },
            sheetState = sheetState,
            containerColor = activeSheetColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("Audio Tracks & Equalizer", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                if (audioTrackOptions.isEmpty()) {
                    Text("No selectable audio tracks found", color = Color.White.copy(alpha = 0.65f))
                }
                audioTrackOptions.forEach { track ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectAudioTrack(track.groupIndex, track.trackIndex) }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Audiotrack, contentDescription = null, tint = sheetAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(track.label, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.weight(1f))
                        if (track.selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = sheetAccent)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
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
                        color = if (loudnessBoost > 100f) Color(0xFFFF7A00) else sheetAccent
                    )
                }

                Slider(
                    value = loudnessBoost.coerceIn(0f, 200f),
                    onValueChange = {
                        loudnessBoost = it
                        onVolumeChange(it)
                    },
                    valueRange = 0f..200f,
                    colors = SliderDefaults.colors(
                        thumbColor = if (loudnessBoost > 100f) Color(0xFFFF7A00) else sheetAccent,
                        activeTrackColor = if (loudnessBoost > 100f) Color(0xFFFF7A00) else sheetAccent
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                // Audio Delay Stepper
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Audio Sync Delay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("${audioDelayMs} ms", style = MaterialTheme.typography.bodySmall, color = sheetAccent)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DelayChip("-100ms") {
                            val next = (audioDelayMs - 100).coerceIn(-5000, 5000)
                            onAudioDelayChange(next)
                        }
                        DelayChip("0") { onAudioDelayChange(0) }
                        DelayChip("+100ms") {
                            val next = (audioDelayMs + 100).coerceIn(-5000, 5000)
                            onAudioDelayChange(next)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Text("Equalizer & Channels", style = MaterialTheme.typography.labelSmall, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("Flat", "Cinema", "Bass Boost", "Vocal Clarifier", "Rock").forEach { preset ->
                        val isSelected = selectedEqPreset == preset
                        FilterChip(
                            selected = isSelected,
                            onClick = {
                                AppSettings.setEqualizerPreset(preset)
                                onEqualizerPresetChange(preset)
                            },
                            label = { Text(preset, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = sheetAccent,
                                selectedLabelColor = onSheetAccent,
                                containerColor = Color.White.copy(alpha = 0.08f),
                                labelColor = Color.White.copy(alpha = 0.8f)
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = {
                        showAudioSheet = false
                        showEqualizerSheet = true
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Rounded.GraphicEq, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Open 5-Band Equalizer & Channels")
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // 8. Real Subtitles Sheet (Tracks, External File, Real Delay, Real Size/Bg/Edge/Position Styling)
    if (showSubtitlesSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSubtitlesSheet = false },
            sheetState = sheetState,
            containerColor = activeSheetColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("Subtitles & Sync", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onDisableSubtitles() }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = sheetAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Off", color = Color.White, modifier = Modifier.weight(1f))
                    if (subtitlesDisabled) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = sheetAccent)
                }

                subtitleTrackOptions.forEach { sub ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectSubtitleTrack(sub.groupIndex, sub.trackIndex) }
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = sheetAccent, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(sub.label, style = MaterialTheme.typography.bodyMedium, color = Color.White, modifier = Modifier.weight(1f))
                        if (sub.selected) Icon(Icons.Rounded.Check, contentDescription = "Selected", tint = sheetAccent)
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showSubtitlesSheet = false
                            onRequestSubtitleFile()
                        }
                        .padding(vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Rounded.Subtitles, contentDescription = null, tint = sheetAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Text("Load subtitle file (.srt, .ass, .ssa, .vtt, .ttml)", color = sheetAccent, fontWeight = FontWeight.SemiBold)
                }

                // Online Subtitle Search & Direct Attach (Wyzie + optional SubDL)
                var showOnlineSearchPanel by remember { mutableStateOf(false) }
                var onlineQuery by remember(videoTitle) {
                    mutableStateOf(IntroOutroManager.parseEpisodeInfo(videoTitle).cleanTitle.ifBlank { videoTitle })
                }
                var onlineLang by remember(onlineSubPreferredLang) { mutableStateOf(onlineSubPreferredLang) }
                var isSearchingOnlineSubs by remember { mutableStateOf(false) }
                var isDownloadingSubId by remember { mutableStateOf<String?>(null) }
                var onlineSearchOutcome by remember { mutableStateOf<OnlineSubtitleSearchOutcome?>(null) }

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color.White.copy(alpha = 0.06f),
                    border = BorderStroke(1.dp, sheetAccent.copy(alpha = if (showOnlineSearchPanel) 0.55f else 0.22f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showOnlineSearchPanel = !showOnlineSearchPanel },
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Rounded.Search, contentDescription = null, tint = sheetAccent, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Search Online Subtitles", color = Color.White, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                                Text("Mode: $onlineSubMode • Wyzie & SubDL", color = Color.White.copy(alpha = 0.65f), style = MaterialTheme.typography.labelSmall)
                            }
                            Text(if (showOnlineSearchPanel) "Hide" else "Open", color = sheetAccent, style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        }

                        AnimatedVisibility(visible = showOnlineSearchPanel) {
                            Column(modifier = Modifier.fillMaxWidth().padding(top = 10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = onlineQuery,
                                    onValueChange = { onlineQuery = it },
                                    label = { Text("Title / Episode (e.g. Show S01E02)", color = Color.White.copy(alpha = 0.7f)) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = sheetAccent,
                                        unfocusedBorderColor = Color.White.copy(alpha = 0.28f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState()),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    OnlineSubtitleClient.SUPPORTED_LANGUAGES.forEach { (code, label) ->
                                        FilterChip(
                                            selected = onlineLang.equals(code, ignoreCase = true),
                                            onClick = {
                                                onlineLang = code
                                                AppSettings.setOnlineSubPreferredLang(code)
                                            },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = sheetAccent,
                                                selectedLabelColor = onSheetAccent,
                                                containerColor = Color.White.copy(alpha = 0.08f),
                                                labelColor = Color.White.copy(alpha = 0.85f)
                                            )
                                        )
                                    }
                                }

                                Button(
                                    enabled = !isSearchingOnlineSubs && onlineQuery.isNotBlank(),
                                    onClick = {
                                        isSearchingOnlineSubs = true
                                        coroutineScope.launch {
                                            val outcome = OnlineSubtitleClient.search(
                                                query = onlineQuery,
                                                languageCode = onlineLang,
                                                searchMode = onlineSubMode,
                                                subdlApiKey = AppSettings.getSubdlApiKey()
                                            )
                                            onlineSearchOutcome = outcome
                                            isSearchingOnlineSubs = false
                                        }
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    if (isSearchingOnlineSubs) {
                                        CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = onSheetAccent)
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text("Searching...")
                                    } else {
                                        Icon(Icons.Rounded.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text("Search Subtitles")
                                    }
                                }

                                onlineSearchOutcome?.let { outcome ->
                                    Text(
                                        text = outcome.statusMessage,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (outcome.error != null && outcome.results.isEmpty()) Color(0xFFFCA5A5) else sheetAccent
                                    )
                                    outcome.results.take(15).forEach { subItem: OnlineSubtitleResult ->
                                        Surface(
                                            onClick = {
                                                if (isDownloadingSubId == null) {
                                                    isDownloadingSubId = subItem.id
                                                    coroutineScope.launch {
                                                        val dlResult = OnlineSubtitleClient.downloadSubtitleToCache(context, subItem)
                                                        isDownloadingSubId = null
                                                        dlResult.onSuccess { subUri ->
                                                            onAttachExternalSubtitleUri(subUri)
                                                            showSubtitlesSheet = false
                                                        }.onFailure { err ->
                                                            Toast.makeText(
                                                                context,
                                                                "Subtitle download failed: ${err.localizedMessage ?: "error"}",
                                                                Toast.LENGTH_SHORT
                                                            ).show()
                                                        }
                                                    }
                                                }
                                            },
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White.copy(alpha = 0.08f),
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth().padding(horizontal = 10.dp, vertical = 8.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(
                                                        text = subItem.title,
                                                        style = MaterialTheme.typography.labelMedium,
                                                        color = Color.White,
                                                        maxLines = 2,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${subItem.languageName} • ${subItem.format.uppercase(Locale.ROOT)} • ${subItem.source}${if (subItem.isHearingImpaired) " • HI" else ""}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        color = Color.White.copy(alpha = 0.65f)
                                                    )
                                                }
                                                if (isDownloadingSubId == subItem.id) {
                                                    CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp, color = sheetAccent)
                                                } else {
                                                    Text("Use", style = MaterialTheme.typography.labelMedium, color = sheetAccent, fontWeight = FontWeight.Bold)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                HorizontalDivider(color = Color.White.copy(alpha = 0.15f))
                Spacer(modifier = Modifier.height(10.dp))

                // Real Subtitle Delay Stepper
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Subtitle Sync Delay", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
                        Text("${subtitleDelayMs} ms", style = MaterialTheme.typography.bodySmall, color = sheetAccent)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        DelayChip("-100ms") { onSubtitleDelayChange((subtitleDelayMs - 100).coerceIn(-10000, 10000)) }
                        DelayChip("0") { onSubtitleDelayChange(0) }
                        DelayChip("+100ms") { onSubtitleDelayChange((subtitleDelayMs + 100).coerceIn(-10000, 10000)) }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Font Size", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text("${subtitleFontSize.toInt()} sp", color = sheetAccent, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = subtitleFontSize.coerceIn(12f, 44f),
                    onValueChange = { AppSettings.setSubtitleFontSize(it) },
                    valueRange = 12f..44f,
                    colors = SliderDefaults.colors(thumbColor = sheetAccent, activeTrackColor = sheetAccent)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Background Opacity", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text("${(subtitleBgOpacity * 100).roundToInt()}%", color = sheetAccent, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = subtitleBgOpacity.coerceIn(0f, 1f),
                    onValueChange = { AppSettings.setSubtitleBgOpacity(it) },
                    valueRange = 0f..1f,
                    colors = SliderDefaults.colors(thumbColor = sheetAccent, activeTrackColor = sheetAccent)
                )

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Bottom Offset", color = Color.White, style = MaterialTheme.typography.bodyMedium)
                    Text("${subtitleBottomPaddingDp} dp", color = sheetAccent, fontWeight = FontWeight.Bold)
                }
                Slider(
                    value = subtitleBottomPaddingDp.toFloat().coerceIn(0f, 120f),
                    onValueChange = { AppSettings.setSubtitleBottomPaddingDp(it.roundToInt()) },
                    valueRange = 0f..120f,
                    colors = SliderDefaults.colors(thumbColor = sheetAccent, activeTrackColor = sheetAccent)
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
                                selectedContainerColor = sheetAccent,
                                selectedLabelColor = onSheetAccent,
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
                        colors = SwitchDefaults.colors(checkedThumbColor = onSheetAccent, checkedTrackColor = sheetAccent)
                    )
                }
                SubtitleAdvancedControlsSection(
                    accent = sheetAccent,
                    secondarySubtitleTitle = secondarySubtitleTitle,
                    onSubtitleDelayChange = onSubtitleDelayChange,
                    onSubtitleSpeedChange = onSubtitleSpeedChange,
                    onRequestSecondarySubtitleFile = onRequestSecondarySubtitleFile,
                    onClearSecondarySubtitle = onClearSecondarySubtitle,
                    onRequestCustomSubtitleFont = onRequestCustomSubtitleFont,
                    onClearCustomSubtitleFont = onClearCustomSubtitleFont
                )
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // 9. Playlist Queue Sheet
    if (showQueueSheet) {
        ModalBottomSheet(
            onDismissRequest = { showQueueSheet = false },
            sheetState = sheetState,
            containerColor = activeSheetColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text("Up Next Queue (${queueTitles.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(12.dp))
                LazyColumn(modifier = Modifier.fillMaxWidth().height(260.dp)) {
                    itemsIndexed(queueTitles) { idx, title ->
                        val active = idx == currentQueueIndex
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectQueueIndex(idx)
                                    showQueueSheet = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("${idx + 1}. ", color = if (active) sheetAccent else Color.White.copy(alpha = 0.55f), fontWeight = FontWeight.Bold)
                            Text(title, color = if (active) sheetAccent else Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                            if (active) Icon(Icons.Rounded.PlayArrow, contentDescription = "Playing", tint = sheetAccent)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    // 10. More Player Tools Sheet (Zero fake Anime4K or fake Clip Cutter!)
    if (showToolsSheet) {
        val bgPlaybackEnabled by AppSettings.backgroundPlayback.collectAsState()
        ModalBottomSheet(
            onDismissRequest = { showToolsSheet = false },
            sheetState = sheetState,
            containerColor = activeSheetColor
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Player Tools", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = Color.White)
                    TextButton(onClick = {
                        showToolsSheet = false
                        showEdgeDrawer = true
                    }) {
                        Text("Edge Drawer", color = sheetAccent)
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))

                ToolsHubItem(
                    icon = Icons.Rounded.Bookmarks,
                    title = "Chapters (${chapters.size})",
                    subtitle = if (currentChapterIndex in chapters.indices) "Now: ${chapters[currentChapterIndex].title}" else "Jump to MKV chapters",
                    onClick = {
                        showToolsSheet = false
                        showChaptersSheet = true
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.HighQuality,
                    title = "Video Quality",
                    subtitle = if (videoTrackOptions.size > 1) "${videoTrackOptions.size} resolution streams" else "Auto stream",
                    onClick = {
                        showToolsSheet = false
                        showQualitySheet = true
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.GraphicEq,
                    title = "5-Band Equalizer & Channels",
                    subtitle = "Preset: $selectedEqPreset",
                    onClick = {
                        showToolsSheet = false
                        showEqualizerSheet = true
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.Bedtime,
                    title = "Sleep Timer",
                    subtitle = when {
                        sleepTimerRemainingMs > 0L -> "Active: ${formatDuration(sleepTimerRemainingMs)}"
                        sleepTimerUntilEnd -> "Pause at End of Video"
                        else -> "Auto-fade & pause playback"
                    },
                    onClick = {
                        showToolsSheet = false
                        showSleepTimerSheet = true
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.FastForward,
                    title = "Custom Skip (+${customSkipSec}s)",
                    subtitle = "One-tap intro/scene skip forward",
                    onClick = {
                        showToolsSheet = false
                        val target = if (durationMs > 0L) (currentPositionMs + customSkipSec * 1000L).coerceAtMost(durationMs) else currentPositionMs + customSkipSec * 1000L
                        onSeek(target)
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.Flip,
                    title = if (isMirrored) "Mirror Horizontal (ON)" else "Mirror Horizontal",
                    subtitle = "Flip video horizontally via TextureView",
                    onClick = {
                        onToggleMirror()
                        showToolsSheet = false
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.FlipCameraAndroid,
                    title = if (isFlippedVertical) "Vertical Flip (ON)" else "Vertical Flip",
                    subtitle = "Flip video vertically via TextureView",
                    onClick = {
                        onToggleVerticalFlip()
                        showToolsSheet = false
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.Shuffle,
                    title = if (isShuffleEnabled) "Queue Shuffle (ON)" else "Queue Shuffle",
                    subtitle = "Randomize next item in queue",
                    onClick = {
                        onToggleShuffle()
                        showToolsSheet = false
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.AutoAwesome,
                    title = if (ambientEnabled) "Ambient Aura Glow (ON)" else "Ambient Aura Glow",
                    subtitle = "Sample playback colors gently around viewport",
                    onClick = {
                        onToggleAmbientMode()
                        showToolsSheet = false
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.Headphones,
                    title = if (bgPlaybackEnabled) "Background Audio Playback (ON)" else "Background Audio Playback",
                    subtitle = "Keep audio playing with MediaSession notification when minimized",
                    onClick = {
                        onToggleBackgroundPlayback()
                        showToolsSheet = false
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.SlowMotionVideo,
                    title = "Frame-by-Frame Navigation",
                    subtitle = "Step exact frames & capture lossless PNG",
                    onClick = {
                        showToolsSheet = false
                        showFrameNavSheet = true
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.Loop,
                    title = "A-B Scene Loop",
                    subtitle = if (abLoopActive) "Loop active" else "Repeat any scene cleanly",
                    onClick = {
                        showToolsSheet = false
                        showAbLoopSheet = true
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.ZoomIn,
                    title = "Zoom & Pan Adjustment",
                    subtitle = "Current: ${(zoomScale * 100).roundToInt()}%",
                    onClick = {
                        showToolsSheet = false
                        showZoomSheet = true
                    }
                )
                ToolsHubItem(
                    icon = Icons.Rounded.DeveloperBoard,
                    title = "Stream Telemetry & Codecs",
                    subtitle = activeDecoderName.ifBlank { "Hardware & runtime format diagnostics" },
                    onClick = {
                        showToolsSheet = false
                        showCodecInspectorSheet = true
                    }
                )
                if (queueTitles.size > 1) {
                    ToolsHubItem(
                        icon = Icons.AutoMirrored.Rounded.QueueMusic,
                        title = "Playback Queue (${queueTitles.size} items)",
                        subtitle = "Jump between videos in queue",
                        onClick = {
                            showToolsSheet = false
                            showQueueSheet = true
                        }
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    if (showChaptersSheet) {
        ChaptersBottomSheet(
            chapters = chapters,
            currentChapterIndex = currentChapterIndex,
            sheetColor = activeSheetColor,
            accent = sheetAccent,
            onSelectChapter = { onSeek(it.startMs) },
            onDismiss = { showChaptersSheet = false }
        )
    }
    if (showQualitySheet) {
        VideoQualityBottomSheet(
            videoTrackOptions = videoTrackOptions,
            sheetColor = activeSheetColor,
            accent = sheetAccent,
            onResetAuto = onResetVideoQualityAuto,
            onSelectTrack = onSelectVideoQualityTrack,
            onDismiss = { showQualitySheet = false }
        )
    }
    if (showSleepTimerSheet) {
        SleepTimerBottomSheet(
            remainingMs = sleepTimerRemainingMs,
            untilEndOfVideo = sleepTimerUntilEnd,
            sheetColor = activeSheetColor,
            accent = sheetAccent,
            onAccent = onSheetAccent,
            onSetMinutes = onSetSleepTimerMinutes,
            onSetEndOfVideo = onSetSleepTimerEndOfVideo,
            onCancel = onCancelSleepTimer,
            onDismiss = { showSleepTimerSheet = false }
        )
    }
    if (showEqualizerSheet) {
        EqualizerBottomSheet(
            sheetColor = activeSheetColor,
            accent = sheetAccent,
            onSelectPreset = { preset ->
                AppSettings.setEqualizerPreset(preset)
                onEqualizerPresetChange(preset)
            },
            onCustomBandsChange = onCustomEqBandsChange,
            onAudioChannelModeChange = onAudioChannelModeChange,
            onDismiss = { showEqualizerSheet = false }
        )
    }
    PlayerControlGridDrawer(
        visible = showEdgeDrawer,
        accent = sheetAccent,
        isMirrored = isMirrored,
        isFlippedVertical = isFlippedVertical,
        isShuffleEnabled = isShuffleEnabled,
        customSkipSec = customSkipSec,
        sleepTimerRemainingMs = sleepTimerRemainingMs,
        activeEngineLabel = if (activeEngine.equals("MPV", ignoreCase = true)) "MPV" else "Exo",
        aspectRatioMode = aspectRatioMode,
        currentSpeed = currentSpeed,
        repeatMode = repeatMode,
        zoomScale = zoomScale,
        abLoopActive = abLoopActive,
        subtitlesDisabled = subtitlesDisabled,
        ambientEnabled = ambientEnabled,
        bgPlaybackEnabled = bgPlaybackEnabled,
        controlsDrawerEnabled = controlsDrawerEnabled,
        onDismiss = { showEdgeDrawer = false },
        onOpenEngine = { showEngineSheet = true },
        onOpenAspect = { showAspectRatioSheet = true },
        onOpenSpeed = { showSpeedSheet = true },
        onCycleRotation = onToggleOrientation,
        onOpenFrameNav = { showFrameNavSheet = true },
        onOpenZoomReset = {
            if (abs(zoomScale - 1.0f) > 0.04f) {
                onZoomPanChange(1.0f, 0f, 0f)
                if (showZoomFeedbackOverlay && showActionFeedbackPills) actionFeedbackPillText = "Zoom: 0% (Fit)"
            } else {
                onZoomPanChange(1.25f, 0f, 0f)
                if (showZoomFeedbackOverlay && showActionFeedbackPills) actionFeedbackPillText = "Zoom: 125%"
            }
        },
        onOpenPip = onPipClick,
        onToggleLock = onToggleLock,
        onOpenAudio = { showAudioSheet = true },
        onOpenSubtitles = { showSubtitlesSheet = true },
        onCycleRepeat = {
            onRepeatModeCycle()
            if (showRepeatShuffleOverlay && showActionFeedbackPills) {
                val nextRepeat = when (repeatMode) {
                    "OFF" -> "Repeat One"
                    "ONE" -> "Repeat All"
                    else -> "Repeat Off"
                }
                actionFeedbackPillText = nextRepeat
            }
        },
        onToggleShuffle = {
            onToggleShuffle()
            if (showRepeatShuffleOverlay && showActionFeedbackPills) {
                actionFeedbackPillText = if (!isShuffleEnabled) "Shuffle ON" else "Shuffle OFF"
            }
        },
        onOpenChapters = { showChaptersSheet = true },
        onOpenQuality = { showQualitySheet = true },
        onToggleMirror = {
            onToggleMirror()
            if (showActionFeedbackPills) actionFeedbackPillText = if (!isMirrored) "Mirror ON" else "Mirror OFF"
        },
        onToggleVerticalFlip = {
            onToggleVerticalFlip()
            if (showActionFeedbackPills) actionFeedbackPillText = if (!isFlippedVertical) "Vertical Flip ON" else "Vertical Flip OFF"
        },
        onOpenAbLoop = { showAbLoopSheet = true },
        onCustomSkipForward = {
            val target = if (durationMs > 0L) (currentPositionMs + customSkipSec * 1000L).coerceAtMost(durationMs) else currentPositionMs + customSkipSec * 1000L
            onSeek(target)
        },
        onToggleBackgroundPlay = onToggleBackgroundPlayback,
        onToggleAmbient = {
            onToggleAmbientMode()
            if (showActionFeedbackPills) actionFeedbackPillText = if (!ambientEnabled) "Ambience Mode ON" else "Ambience Mode OFF"
        },
        onOpenEqualizer = { showEqualizerSheet = true },
        onOpenSleepTimer = { showSleepTimerSheet = true },
        onCaptureScreenshot = onCaptureScreenshot,
        onOpenCast = {
            showEdgeDrawer = false
            try {
                context.startActivity(android.content.Intent(android.provider.Settings.ACTION_CAST_SETTINGS))
            } catch (_: Throwable) {
                try {
                    context.startActivity(android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS))
                } catch (_: Throwable) {
                    android.widget.Toast.makeText(context, "Cast settings unavailable", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
    )
}

@Composable
private fun SlideToUnlockPill(
    accentColor: Color,
    onAccentColor: Color,
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    var trackInnerWidthPx by remember { mutableFloatStateOf(0f) }
    var thumbWidthPx by remember { mutableFloatStateOf(0f) }
    var dragOffsetPx by remember { mutableFloatStateOf(0f) }
    var unlockedTriggered by remember { mutableStateOf(false) }

    val maxDragPx = (trackInnerWidthPx - thumbWidthPx).coerceAtLeast(1f)
    val animatedOffsetPx by animateFloatAsState(
        targetValue = dragOffsetPx,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "slideUnlockOffset"
    )
    val progress = (animatedOffsetPx / maxDragPx).coerceIn(0f, 1f)

    Surface(
        shape = RoundedCornerShape(28.dp),
        color = Color(0xD910141D),
        border = BorderStroke(1.2.dp, accentColor.copy(alpha = 0.55f)),
        shadowElevation = 16.dp,
        modifier = modifier
            .width(248.dp)
            .height(54.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(4.dp)
                .onSizeChanged { trackInnerWidthPx = it.width.toFloat() }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(
                        fraction = ((thumbWidthPx + animatedOffsetPx) / trackInnerWidthPx.coerceAtLeast(1f))
                            .coerceIn(0f, 1f)
                    )
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(24.dp))
                    .background(accentColor.copy(alpha = 0.28f))
            )

            Text(
                text = if (progress > 0.7f) "Release to unlock" else "Slide to unlock  >>>",
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = Color.White.copy(alpha = (0.92f - progress * 0.6f).coerceIn(0.3f, 0.92f)),
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(start = 32.dp)
            )

            Surface(
                shape = CircleShape,
                color = accentColor,
                shadowElevation = 6.dp,
                modifier = Modifier
                    .offset { IntOffset(animatedOffsetPx.roundToInt(), 0) }
                    .size(46.dp)
                    .align(Alignment.CenterStart)
                    .onSizeChanged { thumbWidthPx = it.width.toFloat() }
                    .clickable(onClickLabel = "Unlock screen") { onUnlock() }
                    .draggable(
                        orientation = Orientation.Horizontal,
                        state = rememberDraggableState { delta ->
                            if (!unlockedTriggered) {
                                val next = (dragOffsetPx + delta).coerceIn(0f, maxDragPx)
                                dragOffsetPx = next
                                if (next / maxDragPx >= 0.85f) {
                                    unlockedTriggered = true
                                    onUnlock()
                                }
                            }
                        },
                        onDragStopped = {
                            if (!unlockedTriggered) {
                                dragOffsetPx = 0f
                            }
                        }
                    )
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = if (progress > 0.5f) Icons.Rounded.LockOpen else Icons.Rounded.Lock,
                        contentDescription = "Slide or tap to unlock controls",
                        tint = onAccentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun TelemetryBadge(
    text: String,
    borderColor: Color,
    fillColor: Color,
    onClick: (() -> Unit)? = null
) {
    Surface(
        shape = CircleShape,
        color = fillColor,
        border = BorderStroke(1.dp, borderColor),
        modifier = if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
private fun DelayChip(label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(label, color = Color.White, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp), fontSize = 12.sp)
    }
}

@Composable
private fun SpeedPresetPill(
    speed: Float,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val pillAccent = playerSheetAccent()
    val onPillAccent = playerOnAccentColor(pillAccent)
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) pillAccent else Color.White.copy(alpha = 0.12f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = "${speed}x",
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = if (isSelected) onPillAccent else Color.White,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun FrameNavButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = Color.White.copy(alpha = 0.12f),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun HudCircleButton(
    icon: ImageVector,
    contentDescription: String,
    size: Dp,
    iconSize: Dp,
    glassFill: Color,
    glassBorder: Color,
    tint: Color = Color.White,
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
            Icon(
                imageVector = icon,
                contentDescription = contentDescription,
                tint = tint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}

@Composable
private fun ToolsHubItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    val toolAccent = playerSheetAccent()
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            shape = CircleShape,
            color = toolAccent.copy(alpha = 0.2f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(icon, contentDescription = null, tint = toolAccent, modifier = Modifier.size(20.dp))
            }
        }
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = Color.White)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(alpha = 0.6f))
        }
    }
}

// Backward-compatible alias until Phase 13 removes orphaned PlayerScreen
@Composable
fun PlayerOverlayHud(
    videoTitle: String,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    isHwDecoder: Boolean,
    isLocked: Boolean,
    orientationMode: String = "Auto-Rotate",
    onToggleOrientation: () -> Unit = {},
    activeEngine: String = "Media3",
    smoothMotionEnabled: Boolean = false,
    targetFps: Int = 120,
    initialSheet: String = "",
    onPlayPauseClick: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleDecoder: () -> Unit,
    onToggleLock: () -> Unit,
    onToggleSmoothMotion: () -> Unit = {},
    onEngineSwitch: (String) -> Unit = {},
    onPipClick: () -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) = VideoPlayerScreen(
    videoTitle = videoTitle,
    isPlaying = isPlaying,
    currentPositionMs = currentPositionMs,
    durationMs = durationMs,
    isHwDecoder = isHwDecoder,
    isLocked = isLocked,
    orientationMode = orientationMode,
    onToggleOrientation = onToggleOrientation,
    activeEngine = activeEngine,
    smoothMotionEnabled = smoothMotionEnabled,
    targetFps = targetFps,
    initialSheet = initialSheet,
    onPlayPauseClick = onPlayPauseClick,
    onSeek = onSeek,
    onToggleDecoder = onToggleDecoder,
    onToggleLock = onToggleLock,
    onToggleSmoothMotion = onToggleSmoothMotion,
    onEngineSwitch = onEngineSwitch,
    onPipClick = onPipClick,
    onBackClick = onBackClick,
    modifier = modifier
)

@Composable
private fun playerSheetAccent(): Color {
    val primary = MaterialTheme.colorScheme.primary
    return if (primary.luminance() >= 0.28f) primary else lerp(primary, Color.White, 0.45f)
}

private fun playerOnAccentColor(accent: Color): Color {
    return if (accent.luminance() > 0.4f) Color(0xFF061E1C) else Color.White
}

private fun formatDuration(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val hours = totalSec / 3600L
    val minutes = (totalSec % 3600L) / 60L
    val seconds = totalSec % 60L
    return if (hours > 0L) String.format(Locale.US, "%d:%02d:%02d", hours, minutes, seconds)
    else String.format(Locale.US, "%02d:%02d", minutes, seconds)
}
