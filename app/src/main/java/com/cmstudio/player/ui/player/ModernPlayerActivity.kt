package com.cmstudio.player.ui.player

import android.app.PendingIntent
import android.app.PictureInPictureParams
import android.app.RemoteAction
import android.content.BroadcastReceiver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.drawable.Icon
import android.media.AudioManager
import android.media.audiofx.DynamicsProcessing
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.MediaStore
import android.provider.OpenableColumns
import android.text.Layout
import android.util.Rational
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.PixelCopy
import android.view.Surface
import android.view.SurfaceView
import android.view.TextureView
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DecoderReuseEvaluation
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.extractor.text.SubtitleParser
import androidx.media3.extractor.ts.DefaultTsPayloadReaderFactory
import androidx.media3.extractor.ts.TsExtractor
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import androidx.media3.ui.SubtitleView
import com.cmstudio.player.LaunchHeaders
import com.cmstudio.player.LaunchSubtitles
import com.cmstudio.player.MatroskaChapters
import com.cmstudio.player.PlaybackError
import com.cmstudio.player.PlaybackHistoryBridge
import com.cmstudio.player.QrCode
import com.cmstudio.player.R
import com.cmstudio.player.SubtitleConverter
import com.cmstudio.player.SubtitleFiles
import com.cmstudio.player.SubtitleUtils
import com.cmstudio.player.diagnostics.PlaybackDiagnosticsLog
import com.cmstudio.player.engine.EngineRenderConfig
import com.cmstudio.player.engine.MpvEngineController
import com.cmstudio.player.engine.MpvPlayerMedia3Adapter
import com.cmstudio.player.engine.PatientLoadControl
import com.cmstudio.player.engine.PlaybackEngineMode
import com.cmstudio.player.engine.PlaybackEngineResolver
import io.github.peerless2012.ass.media.AssHandler
import io.github.peerless2012.ass.media.kt.withAssMkvSupport
import io.github.peerless2012.ass.media.kt.withAssSupport
import io.github.peerless2012.ass.media.parser.AssSubtitleParserFactory
import io.github.peerless2012.ass.media.type.AssRenderType
import io.github.peerless2012.ass.media.widget.AssSubtitleView
import com.cmstudio.player.net.NetworkSpeed
import com.cmstudio.player.render.DelayRenderersFactory
import com.cmstudio.player.service.MediaPlaybackService
import com.cmstudio.player.subtitle.WebmVttExtractorsFactory
import com.cmstudio.player.subtitle.parser.EnhancedSubtitleParserFactory
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.network.NetworkBrowserClient
import com.cmstudio.player.ui.theme.MpvxTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

@androidx.annotation.OptIn(UnstableApi::class)
class ModernPlayerActivity : ComponentActivity() {

    private var exoPlayer: ExoPlayer? = null
    private var playerView: PlayerView? = null
    private var mpvSurfaceView: SurfaceView? = null
    private var mpvMedia3Adapter: MpvPlayerMedia3Adapter? = null
    private var assHandlerRef: AssHandler? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var loudnessEnhancer: LoudnessEnhancer? = null
    private var equalizer: Equalizer? = null
    private var dynamicsProcessing: DynamicsProcessing? = null
    private var activeAudioSessionId: Int = C.AUDIO_SESSION_ID_UNSET
    private var enhancedSubtitleParserFactory: EnhancedSubtitleParserFactory? = null

    private var activeEngineMode by mutableStateOf(PlaybackEngineMode.NATIVE)
    private var perVideoEngineOverride by mutableStateOf<PlaybackEngineMode?>(null)
    private var loadedMpvInitial: Boolean = false

    private val subtitleDelayAtomic = AtomicInteger(0)
    private val subtitleSpeedAtomic = AtomicInteger(1000)
    private val audioDelayAtomic = AtomicInteger(0)
    private val audioChannelModeAtomic = AtomicReference(ChannelMixMath.MODE_AUTO)

    // Compose-observed playback & telemetry states
    private var currentUriState by mutableStateOf<Uri?>(null)
    private var videoTitleState by mutableStateOf("MPVX Player")
    private var queueUrisState by mutableStateOf<List<String>>(emptyList())
    private var queueTitlesState by mutableStateOf<List<String>>(emptyList())
    private var currentQueueIndexState by mutableIntStateOf(0)
    private var isInPipState by mutableStateOf(false)

    private var isPlayingState by mutableStateOf(true)
    private var isNativeBufferingState by mutableStateOf(false)
    private var currentPositionMsState by mutableLongStateOf(0L)
    private var durationMsState by mutableLongStateOf(0L)
    private var bufferedPositionMsState by mutableLongStateOf(0L)
    private var activePlaybackError by mutableStateOf<String?>(null)
    private var activePlaybackErrorDetails by mutableStateOf<String?>(null)
    private var networkSpeedMonitor: NetworkSpeed? = null

    private var isHwDecoderState by mutableStateOf(true)
    private var activeDecoderNameState by mutableStateOf("")
    private var detectedFpsState by mutableFloatStateOf(24f)
    private var droppedFramesTotalState by mutableIntStateOf(0)
    private var hdrFormatTextState by mutableStateOf("SDR")
    private var bitrateTextState by mutableStateOf("")
    private var codecNameState by mutableStateOf("Media3")
    private var resolutionTextState by mutableStateOf("")
    private var channelLayoutTextState by mutableStateOf("Stereo")
    private var naturalVideoAspect by mutableFloatStateOf(16f / 9f)
    private var videoWidthPx by mutableIntStateOf(0)
    private var videoHeightPx by mutableIntStateOf(0)
    private var hasResolvedVideoSize by mutableStateOf(false)

    private var currentSpeedState by mutableFloatStateOf(1.0f)
    private var zoomScaleState by mutableFloatStateOf(1.0f)
    private var panOffsetXState by mutableFloatStateOf(0f)
    private var panOffsetYState by mutableFloatStateOf(0f)
    private var currentAspectRatioMode by mutableStateOf("Fit")
    private var isMirroredState by mutableStateOf(false)
    private var isFlippedVerticalState by mutableStateOf(false)
    private var ambientColorArgbState by mutableLongStateOf(0L)

    private var abLoopStartMsState by mutableLongStateOf(-1L)
    private var abLoopEndMsState by mutableLongStateOf(-1L)
    private var abLoopActiveState by mutableStateOf(false)

    private var videoTrackOptionsState by mutableStateOf<List<PlayerTrackOption>>(emptyList())
    private var audioTrackOptionsState by mutableStateOf<List<PlayerTrackOption>>(emptyList())
    private var subtitleTrackOptionsState by mutableStateOf<List<PlayerTrackOption>>(emptyList())
    private var subtitlesDisabledState by mutableStateOf(true)
    private var addedSubtitles by mutableStateOf<List<MediaItem.SubtitleConfiguration>>(emptyList())

    private var chaptersState by mutableStateOf<List<VideoChapterItem>>(emptyList())
    private var currentChapterIndexState by mutableIntStateOf(-1)

    private var secondaryCuesState by mutableStateOf<List<SecondaryCueItem>>(emptyList())
    private var activeSecondarySubTextState by mutableStateOf("")
    private var secondarySubTitleState by mutableStateOf("")

    private var sleepTimerRemainingMsState by mutableLongStateOf(0L)
    private var sleepTimerUntilEndState by mutableStateOf(false)

    private var screenshotEventCounter by mutableIntStateOf(0)
    private var screenshotSuccessText by mutableStateOf<String?>(null)

    private var pendingResumePositionMs: Long = -1L
    private var intentHttpHeadersState: Map<String, String> = emptyMap()

    private val subtitlePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            attachExternalSubtitleUri(pickedUri)
        }
    }

    private val secondarySubtitlePickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            loadSecondarySubtitleUri(pickedUri)
        }
    }

    private val customFontPickerLauncher = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { pickedUri: Uri? ->
        if (pickedUri != null) {
            importCustomSubtitleFont(pickedUri)
        }
    }

    private val pipActionReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            val isMpv = activeEngineMode == PlaybackEngineMode.MPV
            when (intent?.action) {
                ACTION_PIP_PLAY_PAUSE -> {
                    if (isMpv) {
                        MpvEngineController.togglePlayPause()
                    } else {
                        val player = exoPlayer ?: return
                        if (player.isPlaying) player.pause() else player.play()
                    }
                    updatePipAspectParams()
                }
                ACTION_PIP_REPLAY -> {
                    val step = AppSettings.seekIntervalSeconds.value * 1000L
                    if (isMpv) {
                        val pos = MpvEngineController.snapshot.value.positionMs
                        MpvEngineController.seekTo((pos - step).coerceAtLeast(0L))
                    } else {
                        val player = exoPlayer ?: return
                        player.seekTo((player.currentPosition - step).coerceAtLeast(0L))
                    }
                }
                ACTION_PIP_FORWARD -> {
                    val step = AppSettings.seekIntervalSeconds.value * 1000L
                    if (isMpv) {
                        val snap = MpvEngineController.snapshot.value
                        val dur = snap.durationMs.takeIf { it > 0L } ?: Long.MAX_VALUE
                        MpvEngineController.seekTo((snap.positionMs + step).coerceAtMost(dur))
                    } else {
                        val player = exoPlayer ?: return
                        val dur = player.duration.takeIf { it > 0L } ?: Long.MAX_VALUE
                        player.seekTo((player.currentPosition + step).coerceAtMost(dur))
                    }
                }
            }
        }
    }

    companion object {
        private const val ACTION_PIP_PLAY_PAUSE = "com.cmstudio.player.PIP_PLAY_PAUSE"
        private const val ACTION_PIP_REPLAY = "com.cmstudio.player.PIP_REPLAY"
        private const val ACTION_PIP_FORWARD = "com.cmstudio.player.PIP_FORWARD"
    }

    @Volatile
    private var pausedByScreenLock = false

    private val screenLockReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    if (isPlayingState && !AppSettings.backgroundPlayback.value) {
                        pausedByScreenLock = true
                    }
                }
                Intent.ACTION_USER_PRESENT -> {
                    if (pausedByScreenLock && AppSettings.autoplayAfterScreenUnlock.value && !isDestroyed) {
                        pausedByScreenLock = false
                        if (activeEngineMode == PlaybackEngineMode.MPV) {
                            MpvEngineController.play()
                        } else {
                            exoPlayer?.play()
                        }
                    } else {
                        pausedByScreenLock = false
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        hideSystemUI()
        applyOrientationPreference(AppSettings.screenOrientation.value)
        ContextCompat.registerReceiver(
            this,
            pipActionReceiver,
            IntentFilter().apply {
                addAction(ACTION_PIP_PLAY_PAUSE)
                addAction(ACTION_PIP_REPLAY)
                addAction(ACTION_PIP_FORWARD)
            },
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        ContextCompat.registerReceiver(
            this,
            screenLockReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_USER_PRESENT)
            },
            ContextCompat.RECEIVER_EXPORTED
        )

        subtitleDelayAtomic.set(AppSettings.subtitleDelayMs.value)
        subtitleSpeedAtomic.set(AppSettings.subtitleSpeedMilli.value)
        audioDelayAtomic.set(AppSettings.audioDelayMs.value)
        audioChannelModeAtomic.set(AppSettings.audioChannelMode.value)
        currentSpeedState = AppSettings.defaultSpeed.value.coerceIn(0.25f, 4.0f)
        currentAspectRatioMode = AppSettings.defaultAspectRatio.value

        parseAndLoadIntent(intent, rebuildPlayer = false)
        mpvMedia3Adapter = MpvPlayerMedia3Adapter(
            looper = Looper.getMainLooper(),
            titleProvider = { videoTitleState },
            uriProvider = { currentUriState }
        )
        currentUriState?.let { uri ->
            val rememberedAspect = AppSettings.getVideoAspectOverride(uri.toString())
            if (!rememberedAspect.isNullOrBlank()) currentAspectRatioMode = rememberedAspect
            perVideoEngineOverride = AppSettings.getVideoEngineOverride(uri.toString())
        }
        activeEngineMode = resolveEffectiveEngineForCurrentMedia()

        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager
        val initialVolPct = if (audioManager != null) {
            val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
            val cur = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            ((cur.toFloat() / max.toFloat()) * 100f).coerceIn(0f, 100f)
        } else {
            AppSettings.audioBoost.value.toFloat()
        }
        val initialBright = if (AppSettings.rememberBrightness.value) {
            AppSettings.savedPlayerBrightness.value.coerceIn(0.01f, 1.0f).also { b ->
                val lp = window.attributes
                lp.screenBrightness = b
                window.attributes = lp
            }
        } else {
            window.attributes.screenBrightness.let { if (it in 0.01f..1.0f) it else 0.7f }
        }

        val initialSheetArg = intent?.getStringExtra("initial_sheet").orEmpty()

        setContent {
            val darkMode by AppSettings.darkMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = when (darkMode.uppercase()) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }
            val themePreset by AppSettings.themePreset.collectAsState()
            val isAmoled by AppSettings.isAmoled.collectAsState()
            val customPrimary by AppSettings.customPrimaryColor.collectAsState()
            val customAccent by AppSettings.customAccentColor.collectAsState()
            val customBg by AppSettings.customBackgroundColor.collectAsState()
            val customSurface by AppSettings.customSurfaceColor.collectAsState()

            val decoderPriority by AppSettings.decoderPriority.collectAsState()
            val videoSurfaceType by AppSettings.videoSurfaceType.collectAsState()
            val keepScreenOn by AppSettings.keepScreenOn.collectAsState()
            val smoothMotionEnabled by AppSettings.smoothMotion120fps.collectAsState()
            val targetFps by AppSettings.targetFps.collectAsState()
            val repeatModeSetting by AppSettings.playerRepeatMode.collectAsState()
            val defaultEngineSetting by AppSettings.defaultEngine.collectAsState()
            val mpvProfile by AppSettings.mpvProfile.collectAsState()
            val useGpuNext by AppSettings.useGpuNext.collectAsState()
            val useVulkan by AppSettings.useVulkan.collectAsState()
            val useYuv420p by AppSettings.useYuv420p.collectAsState()
            val hdrTonemap by AppSettings.hdrTonemapping.collectAsState()
            val mpvDebanding by AppSettings.mpvDebanding.collectAsState()
            val debandIterations by AppSettings.debandIterations.collectAsState()
            val debandThreshold by AppSettings.debandThreshold.collectAsState()
            val debandRange by AppSettings.debandRange.collectAsState()
            val debandGrain by AppSettings.debandGrain.collectAsState()
            val anime4kEnabled by AppSettings.anime4k.collectAsState()
            val anime4kMode by AppSettings.anime4kMode.collectAsState()
            val anime4kQuality by AppSettings.anime4kQuality.collectAsState()
            val audioBoostSetting by AppSettings.audioBoost.collectAsState()
            val eqPresetSetting by AppSettings.eqPreset.collectAsState()
            val eqCustomBandsSetting by AppSettings.eqCustomBandsDb.collectAsState()
            val nightCompressorSetting by AppSettings.nightModeCompressor.collectAsState()
            val fastSeekSetting by AppSettings.fastSeekKeyframes.collectAsState()

            val subFontSize by AppSettings.subtitleFontSize.collectAsState()
            val subColor by AppSettings.subtitleColor.collectAsState()
            val subBgColor by AppSettings.subtitleBackground.collectAsState()
            val subBgOpacity by AppSettings.subtitleBgOpacity.collectAsState()
            val subEdgeType by AppSettings.subtitleEdgeType.collectAsState()
            val subBottomPadding by AppSettings.subtitleBottomPaddingDp.collectAsState()
            val subBold by AppSettings.subtitleBold.collectAsState()
            val subItalic by AppSettings.subtitleItalic.collectAsState()
            val subAlignment by AppSettings.subtitleAlignment.collectAsState()
            val subCustomFontUri by AppSettings.subtitleCustomFontUri.collectAsState()
            val subSpeedMilli by AppSettings.subtitleSpeedMilli.collectAsState()
            val subOverrideAss by AppSettings.subOverrideAssMode.collectAsState()
            val subScaleByWindow by AppSettings.subScaleByWindow.collectAsState()
            val volumeNormalization by AppSettings.volumeNormalization.collectAsState()
            val volumeBoostCap by AppSettings.volumeBoostCapPct.collectAsState()
            val keepScreenOnWhenPaused by AppSettings.keepScreenOnWhenPaused.collectAsState()
            val dualSubtitlesEnabled by AppSettings.dualSubtitles.collectAsState()
            val audioChannelMode by AppSettings.audioChannelMode.collectAsState()
            val ambientModeEnabled by AppSettings.ambientModeEnabled.collectAsState()
            val playerShuffle by AppSettings.playerShuffleMode.collectAsState()
            val showSysStatusBar by AppSettings.showSystemStatusBar.collectAsState()
            val showSysNavBar by AppSettings.showSystemNavBar.collectAsState()

            var isLocked by remember { mutableStateOf(false) }
            var orientationMode by remember {
                mutableStateOf(
                    when (AppSettings.screenOrientation.value) {
                        "Auto" -> "Auto-Rotate"
                        else -> AppSettings.screenOrientation.value
                    }
                )
            }

            // Live sync of subtitle speed, channel mixing & system bars without rebuilding ExoPlayer
            LaunchedEffect(subSpeedMilli) { subtitleSpeedAtomic.set(subSpeedMilli) }
            LaunchedEffect(audioChannelMode) { audioChannelModeAtomic.set(audioChannelMode) }
            LaunchedEffect(showSysStatusBar, showSysNavBar) { hideSystemUI() }
            LaunchedEffect(hasResolvedVideoSize, videoWidthPx, videoHeightPx, orientationMode) {
                applyOrientationPreference(if (orientationMode == "Auto-Rotate") "Auto" else orientationMode)
            }

            // Load Matroska chapters asynchronously whenever media URI or duration changes
            LaunchedEffect(currentUriState, durationMsState) {
                val uri = currentUriState
                if (uri == null) {
                    chaptersState = emptyList()
                } else {
                    val loaded = withContext(Dispatchers.IO) {
                        runCatching { MatroskaChapters.read(this@ModernPlayerActivity, uri) }
                            .getOrDefault(emptyList())
                    }
                    val totalDur = durationMsState
                    chaptersState = loaded.mapIndexed { idx, mark ->
                        val startMs = (mark.start * 1000.0).roundToInt().toLong().coerceAtLeast(0L)
                        val nextStartMs = loaded.getOrNull(idx + 1)?.let { (it.start * 1000.0).roundToInt().toLong() }
                        val endMs = nextStartMs ?: totalDur
                        VideoChapterItem(
                            title = mark.title.ifBlank { "Chapter ${idx + 1}" },
                            startMs = startMs,
                            endMs = endMs.coerceAtLeast(startMs)
                        )
                    }
                }
            }

            // Sleep timer countdown + final 5s audio fade
            LaunchedEffect(sleepTimerRemainingMsState, isPlayingState, activeEngineMode) {
                while (sleepTimerRemainingMsState > 0L) {
                    delay(1000L)
                    val next = (sleepTimerRemainingMsState - 1000L).coerceAtLeast(0L)
                    sleepTimerRemainingMsState = next
                    if (activeEngineMode == PlaybackEngineMode.MPV) {
                        if (next == 0L) MpvEngineController.pause()
                    } else {
                        val player = exoPlayer
                        if (player != null && next in 1L..5000L) {
                            player.volume = (next.toFloat() / 5000f).coerceIn(0.05f, 1f)
                        }
                        if (next == 0L) {
                            exoPlayer?.pause()
                            exoPlayer?.volume = 1.0f
                        }
                    }
                }
            }

            // Compose YouTube-style Ambient Aura color sampling when enabled
            LaunchedEffect(ambientModeEnabled, isPlayingState, isInPipState, playerView, activeEngineMode) {
                if (!ambientModeEnabled || isInPipState || activeEngineMode == PlaybackEngineMode.MPV) {
                    ambientColorArgbState = 0L
                    return@LaunchedEffect
                }
                while (ambientModeEnabled && !isInPipState && activeEngineMode == PlaybackEngineMode.NATIVE) {
                    val pv = playerView
                    if (pv != null) {
                        sampleAmbientAuraColor(pv)
                    }
                    delay(1200L)
                }
            }

            // Apply Keep Screen On dynamically (honors keepScreenOnWhenPaused across both engines)
            LaunchedEffect(keepScreenOn, keepScreenOnWhenPaused, isPlayingState) {
                if (keepScreenOn && (isPlayingState || keepScreenOnWhenPaused)) {
                    window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                } else {
                    window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                }
            }

            // Apply Repeat Mode & Fast Seek dynamically to active engine
            LaunchedEffect(repeatModeSetting, exoPlayer, activeEngineMode) {
                if (activeEngineMode == PlaybackEngineMode.MPV) {
                    MpvEngineController.setRepeatMode(repeatModeSetting)
                } else {
                    exoPlayer?.repeatMode = when (repeatModeSetting) {
                        "ONE" -> Player.REPEAT_MODE_ONE
                        "ALL" -> Player.REPEAT_MODE_ALL
                        else -> Player.REPEAT_MODE_OFF
                    }
                }
            }

            LaunchedEffect(fastSeekSetting, activeEngineMode) {
                if (activeEngineMode == PlaybackEngineMode.MPV) {
                    MpvEngineController.setFastSeek(fastSeekSetting)
                } else {
                    exoPlayer?.setSeekParameters(if (fastSeekSetting) SeekParameters.CLOSEST_SYNC else SeekParameters.EXACT)
                }
            }

            // Live MPV snapshot sync & MediaPlaybackService notification refresh
            val mpvSnap by MpvEngineController.snapshot.collectAsState()
            LaunchedEffect(mpvSnap, activeEngineMode, dualSubtitlesEnabled, secondaryCuesState, chaptersState, abLoopActiveState, abLoopStartMsState, abLoopEndMsState) {
                if (activeEngineMode == PlaybackEngineMode.MPV) {
                    isPlayingState = mpvSnap.isPlaying
                    currentPositionMsState = mpvSnap.positionMs
                    durationMsState = mpvSnap.durationMs
                    bufferedPositionMsState = mpvSnap.bufferedMs
                    if (mpvSnap.videoWidth > 0 && mpvSnap.videoHeight > 0) {
                        val (dispW, dispH) = PlayerControlCatalog.resolveDisplayDimensions(
                            mpvSnap.videoWidth,
                            mpvSnap.videoHeight,
                            mpvSnap.videoRotate
                        )
                        videoWidthPx = dispW
                        videoHeightPx = dispH
                        hasResolvedVideoSize = true
                        naturalVideoAspect = dispW.toFloat() / dispH.toFloat()
                        resolutionTextState = "${dispW}x${dispH}"
                        updatePipAspectParams()
                    }
                    codecNameState = mpvSnap.codecName.ifBlank { "MPV" }
                    activeDecoderNameState = mpvSnap.hwdecCurrent.ifBlank { mpvSnap.voName }
                    isHwDecoderState = mpvSnap.hwdecCurrent.isNotBlank() && !mpvSnap.hwdecCurrent.equals("no", ignoreCase = true)
                    detectedFpsState = mpvSnap.detectedFps
                    hdrFormatTextState = mpvSnap.hdrLabel
                    bitrateTextState = if (mpvSnap.bitrateBps > 0L) {
                        String.format(Locale.US, "%.1f Mbps", mpvSnap.bitrateBps / 1_000_000f)
                    } else ""
                    channelLayoutTextState = mpvSnap.channelLayout
                    if (mpvSnap.chapters.isNotEmpty()) {
                        chaptersState = mpvSnap.chapters
                        currentChapterIndexState = mpvSnap.currentChapterIndex
                    } else if (chaptersState.isNotEmpty()) {
                        currentChapterIndexState = chaptersState.indexOfLast { mpvSnap.positionMs >= it.startMs }
                    }
                    videoTrackOptionsState = mpvSnap.videoTracks
                    audioTrackOptionsState = mpvSnap.audioTracks
                    subtitleTrackOptionsState = mpvSnap.subtitleTracks
                    subtitlesDisabledState = mpvSnap.subtitleTracks.none { it.selected }
                    if (mpvSnap.lastError != null && activePlaybackError == null) {
                        activePlaybackError = mpvSnap.lastError
                        activePlaybackErrorDetails = "Engine: libmpv\nVO: ${mpvSnap.voName}\nError: ${mpvSnap.lastError}"
                    }
                    updateSecondarySubAndAbLoop(mpvSnap.positionMs)
                    mpvMedia3Adapter?.notifyStateChanged()
                }
            }

            // Live MPV render/shader/audio DSP synchronization
            val renderConfig = EngineRenderConfig(
                mpvProfile = mpvProfile,
                decoderPriority = decoderPriority,
                useGpuNext = useGpuNext,
                useVulkan = useVulkan,
                useYuv420p = useYuv420p,
                hdrTonemapping = hdrTonemap,
                debanding = mpvDebanding,
                debandIterations = debandIterations,
                debandThreshold = debandThreshold,
                debandRange = debandRange,
                debandGrain = debandGrain,
                anime4kEnabled = anime4kEnabled,
                anime4kMode = anime4kMode,
                anime4kQuality = anime4kQuality
            )
            LaunchedEffect(renderConfig, activeEngineMode) {
                if (activeEngineMode == PlaybackEngineMode.MPV) {
                    if (MpvEngineController.needsReinitForRenderConfig(renderConfig)) {
                        val pos = currentPositionMsState.coerceAtLeast(0L)
                        val play = isPlayingState
                        MpvEngineController.releaseEngine()
                        loadCurrentMediaIntoMpv(this@ModernPlayerActivity, pos, play)
                    } else {
                        MpvEngineController.applyRenderAndShaderConfig(this@ModernPlayerActivity, renderConfig)
                    }
                }
            }

            LaunchedEffect(audioBoostSetting, eqPresetSetting, eqCustomBandsSetting, nightCompressorSetting, audioChannelMode, volumeNormalization, volumeBoostCap, activeEngineMode) {
                applyAudioEffects(activeAudioSessionId)
            }

            // Apply SubtitleView / MPV styles in real time
            LaunchedEffect(subFontSize, subColor, subBgColor, subBgOpacity, subEdgeType, subBottomPadding, subBold, subItalic, subAlignment, subCustomFontUri, subOverrideAss, subScaleByWindow, playerView, activeEngineMode) {
                applySubtitleStyling(playerView?.subtitleView)
            }

            // Apply Aspect Ratio, Mirror/Flip & Zoom/Pan cleanly for both engines
            LaunchedEffect(currentAspectRatioMode, zoomScaleState, panOffsetXState, panOffsetYState, isMirroredState, isFlippedVerticalState, naturalVideoAspect, videoWidthPx, videoHeightPx, playerView, mpvSurfaceView, activeEngineMode) {
                applyAspectAndSurfaceZoom()
            }

            // ExoPlayer Polling Loop (when NATIVE engine is active)
            LaunchedEffect(exoPlayer, activeEngineMode, abLoopActiveState, abLoopStartMsState, abLoopEndMsState, dualSubtitlesEnabled, secondaryCuesState, chaptersState) {
                if (activeEngineMode != PlaybackEngineMode.NATIVE) return@LaunchedEffect
                while (activeEngineMode == PlaybackEngineMode.NATIVE) {
                    val player = exoPlayer
                    if (player != null) {
                        val pos = player.currentPosition.coerceAtLeast(0L)
                        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
                        val buf = player.bufferedPosition.coerceAtLeast(0L)
                        currentPositionMsState = pos
                        durationMsState = dur
                        bufferedPositionMsState = buf
                        isPlayingState = player.isPlaying

                        if (chaptersState.isNotEmpty()) {
                            currentChapterIndexState = chaptersState.indexOfLast { pos >= it.startMs }
                        } else {
                            currentChapterIndexState = -1
                        }

                        updateSecondarySubAndAbLoop(pos)
                    }
                    delay(180L)
                }
            }

            // Switch to TextureView automatically if Mirror or Vertical Flip is toggled on
            val effectiveSurfaceType = if (isMirroredState || isFlippedVerticalState) "TEXTURE_VIEW" else videoSurfaceType

            MpvxTheme(
                darkTheme = effectiveDark,
                themePreset = themePreset,
                isAmoled = isAmoled,
                customPrimaryArgb = customPrimary,
                customAccentArgb = customAccent,
                customBackgroundArgb = customBg,
                customSurfaceArgb = customSurface,
                showCustomThemeBackdrop = false
            ) {
                Box(modifier = Modifier.fillMaxSize()) {
                    key(activeEngineMode, if (activeEngineMode == PlaybackEngineMode.NATIVE) effectiveSurfaceType else "MPV_SURFACE") {
                        if (activeEngineMode == PlaybackEngineMode.MPV) {
                            AndroidView(
                                factory = { ctx ->
                                    val sv = MpvEngineController.createSurfaceView(ctx)
                                    mpvSurfaceView = sv
                                    if (!loadedMpvInitial) {
                                        loadedMpvInitial = true
                                        loadCurrentMediaIntoMpv(
                                            ctx = ctx,
                                            resumePosMs = pendingResumePositionMs.coerceAtLeast(0L),
                                            autoPlay = true
                                        )
                                    }
                                    sv
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            AndroidView(
                                factory = { ctx ->
                                    val layoutId = if (effectiveSurfaceType == "TEXTURE_VIEW") {
                                        R.layout.activity_player_textureview
                                    } else {
                                        R.layout.activity_player_surfaceview
                                    }
                                    val pv = LayoutInflater.from(ctx).inflate(layoutId, null) as PlayerView
                                    pv.useController = false
                                    pv.clipChildren = true
                                    pv.clipToPadding = true
                                    playerView = pv
                                    applySubtitleStyling(pv.subtitleView)

                                    if (exoPlayer == null) {
                                        createAndPreparePlayer(ctx, pv)
                                    } else {
                                        pv.player = exoPlayer
                                        assHandlerRef?.let { bindAssSubtitleOverlay(pv.subtitleView, it) }
                                        pv.post { applyAspectAndSurfaceZoom() }
                                    }
                                    pv
                                },
                                update = { pv ->
                                    if (pv.player !== exoPlayer && exoPlayer != null) {
                                        pv.player = exoPlayer
                                    }
                                    assHandlerRef?.let { bindAssSubtitleOverlay(pv.subtitleView, it) }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }

                    VideoPlayerScreen(
                        videoTitle = videoTitleState,
                        isPlaying = isPlayingState,
                        isBuffering = if (activeEngineMode == PlaybackEngineMode.MPV) mpvSnap.isBuffering else isNativeBufferingState,
                        currentPositionMs = currentPositionMsState,
                        durationMs = durationMsState,
                        bufferedPositionMs = bufferedPositionMsState,
                        isHwDecoder = isHwDecoderState,
                        decoderPriority = decoderPriority,
                        activeDecoderName = activeDecoderNameState,
                        isLocked = isLocked,
                        isInPipMode = isInPipState,
                        orientationMode = orientationMode,
                        onToggleOrientation = {
                            val next = when (orientationMode) {
                                "Auto-Rotate", "Auto" -> "Landscape"
                                "Landscape" -> "Portrait"
                                else -> "Auto-Rotate"
                            }
                            orientationMode = next
                            applyOrientationPreference(if (next == "Auto-Rotate") "Auto" else next)
                        },
                        activeEngine = activeEngineMode.id,
                        onEngineSwitch = { targetEngineId ->
                            switchEngineLive(PlaybackEngineMode.fromId(targetEngineId))
                        },
                        smoothMotionEnabled = smoothMotionEnabled,
                        targetFps = targetFps,
                        detectedFps = detectedFpsState,
                        droppedFrames = droppedFramesTotalState,
                        hdrFormatText = hdrFormatTextState,
                        bitrateText = bitrateTextState,
                        initialSheet = initialSheetArg,
                        zoomScale = zoomScaleState,
                        panOffsetX = panOffsetXState,
                        panOffsetY = panOffsetYState,
                        aspectRatioMode = currentAspectRatioMode,
                        codecName = codecNameState,
                        resolutionText = resolutionTextState,
                        channelLayoutText = channelLayoutTextState,
                        initialVolume = initialVolPct,
                        initialBrightness = initialBright,
                        currentSpeed = currentSpeedState,
                        repeatMode = repeatModeSetting,
                        hasPreviousTrack = queueUrisState.size > 1 && (currentQueueIndexState > 0 || playerShuffle),
                        hasNextTrack = queueUrisState.size > 1 && (currentQueueIndexState < queueUrisState.lastIndex || playerShuffle),
                        queueTitles = queueTitlesState,
                        currentQueueIndex = currentQueueIndexState,
                        abLoopStartMs = abLoopStartMsState,
                        abLoopEndMs = abLoopEndMsState,
                        abLoopActive = abLoopActiveState,
                        screenshotEventId = screenshotEventCounter,
                        screenshotSuccessMessage = screenshotSuccessText,
                        playbackErrorMessage = activePlaybackError,
                        playbackErrorDetails = activePlaybackErrorDetails,
                        onSharePlaybackError = {
                            val details = activePlaybackErrorDetails ?: activePlaybackError.orEmpty()
                            if (details.isNotBlank()) PlaybackError.share(this@ModernPlayerActivity, details)
                        },
                        onBuildPlaybackErrorQr = { sizePx ->
                            val details = activePlaybackErrorDetails ?: activePlaybackError.orEmpty()
                            if (details.isBlank()) null else QrCode.of(details, sizePx)
                        },
                        videoTrackOptions = videoTrackOptionsState,
                        audioTrackOptions = audioTrackOptionsState,
                        subtitleTrackOptions = subtitleTrackOptionsState,
                        subtitlesDisabled = subtitlesDisabledState,
                        chapters = chaptersState,
                        currentChapterIndex = currentChapterIndexState,
                        isShuffleEnabled = playerShuffle,
                        isMirrored = isMirroredState,
                        isFlippedVertical = isFlippedVerticalState,
                        ambientColorArgb = ambientColorArgbState,
                        sleepTimerRemainingMs = sleepTimerRemainingMsState,
                        sleepTimerUntilEnd = sleepTimerUntilEndState,
                        secondarySubtitleText = activeSecondarySubTextState,
                        secondarySubtitleTitle = secondarySubTitleState,
                        onSelectVideoQualityTrack = { g, t -> selectTrack(C.TRACK_TYPE_VIDEO, g, t) },
                        onResetVideoQualityAuto = { resetVideoQualityToAuto() },
                        onSelectAudioTrack = { g, t -> selectTrack(C.TRACK_TYPE_AUDIO, g, t) },
                        onSelectSubtitleTrack = { g, t -> selectTrack(C.TRACK_TYPE_TEXT, g, t) },
                        onDisableSubtitles = { disableTrackType(C.TRACK_TYPE_TEXT) },
                        onRequestSubtitleFile = { subtitlePickerLauncher.launch(arrayOf("*/*")) },
                        onAttachExternalSubtitleUri = { subUri -> attachExternalSubtitleUri(subUri) },
                        onRequestSecondarySubtitleFile = { secondarySubtitlePickerLauncher.launch(arrayOf("*/*")) },
                        onClearSecondarySubtitle = {
                            secondaryCuesState = emptyList()
                            activeSecondarySubTextState = ""
                            secondarySubTitleState = ""
                        },
                        onRequestCustomSubtitleFont = { customFontPickerLauncher.launch(arrayOf("*/*")) },
                        onClearCustomSubtitleFont = { AppSettings.setSubtitleCustomFont("", "System Default") },
                        onSubtitleDelayChange = { ms ->
                            subtitleDelayAtomic.set(ms)
                            AppSettings.setSubtitleDelayMs(ms)
                            if (activeEngineMode == PlaybackEngineMode.MPV) {
                                MpvEngineController.setSubtitleDelayMs(ms)
                            }
                        },
                        onSubtitleSpeedChange = { milli ->
                            subtitleSpeedAtomic.set(milli)
                            AppSettings.setSubtitleSpeedMilli(milli)
                            if (activeEngineMode == PlaybackEngineMode.MPV) {
                                MpvEngineController.setSubtitleSpeedMilli(milli)
                            }
                        },
                        onAudioDelayChange = { ms ->
                            audioDelayAtomic.set(ms)
                            AppSettings.setAudioDelayMs(ms)
                            if (activeEngineMode == PlaybackEngineMode.MPV) {
                                MpvEngineController.setAudioDelayMs(ms)
                            }
                        },
                        onEqualizerPresetChange = { preset ->
                            AppSettings.setEqualizerPreset(preset)
                            applyAudioEffects(activeAudioSessionId)
                        },
                        onCustomEqBandsChange = { bands ->
                            AppSettings.setEqCustomBandsDb(EqualizerPresetsCatalog.serializeBands(bands))
                            AppSettings.setEqualizerPreset("Custom")
                            applyAudioEffects(activeAudioSessionId)
                        },
                        onAudioChannelModeChange = { mode ->
                            AppSettings.setAudioChannelMode(mode)
                            audioChannelModeAtomic.set(mode)
                            applyAudioEffects(activeAudioSessionId)
                        },
                        onToggleShuffle = {
                            AppSettings.setPlayerShuffleMode(!AppSettings.playerShuffleMode.value)
                        },
                        onToggleMirror = { isMirroredState = !isMirroredState },
                        onToggleVerticalFlip = { isFlippedVerticalState = !isFlippedVerticalState },
                        onToggleAmbientMode = {
                            AppSettings.setAmbientModeEnabled(!AppSettings.ambientModeEnabled.value)
                        },
                        onSetSleepTimerMinutes = { mins ->
                            sleepTimerUntilEndState = false
                            sleepTimerRemainingMsState = (mins * 60_000L).coerceAtLeast(60_000L)
                            exoPlayer?.volume = 1.0f
                        },
                        onSetSleepTimerEndOfVideo = {
                            sleepTimerRemainingMsState = 0L
                            sleepTimerUntilEndState = true
                        },
                        onCancelSleepTimer = {
                            sleepTimerRemainingMsState = 0L
                            sleepTimerUntilEndState = false
                            exoPlayer?.volume = 1.0f
                        },
                        onToggleBackgroundPlayback = {
                            val next = !AppSettings.backgroundPlayback.value
                            AppSettings.setBackgroundPlayback(next)
                            val toastText = if (next) "Background audio playback enabled" else "Background playback disabled"
                            Toast.makeText(this@ModernPlayerActivity, toastText, Toast.LENGTH_SHORT).show()
                        },
                        onPlayPauseClick = {
                            if (activeEngineMode == PlaybackEngineMode.MPV) {
                                if (MpvEngineController.snapshot.value.isEnded) {
                                    MpvEngineController.seekTo(0L)
                                    MpvEngineController.play()
                                } else {
                                    MpvEngineController.togglePlayPause()
                                }
                            } else {
                                exoPlayer?.let { p ->
                                    if (p.isPlaying) {
                                        p.pause()
                                    } else {
                                        p.volume = 1.0f
                                        if (p.playbackState == Player.STATE_ENDED) p.seekTo(0L)
                                        if (p.playbackState == Player.STATE_IDLE) p.prepare()
                                        p.play()
                                    }
                                }
                            }
                        },
                        onPreviousTrack = { jumpToQueueIndex(currentQueueIndexState - 1) },
                        onNextTrack = { jumpToQueueIndex(currentQueueIndexState + 1) },
                        onSelectQueueIndex = { idx -> jumpToQueueIndex(idx) },
                        onSeek = { posMs ->
                            val target = posMs.coerceAtLeast(0L)
                            if (activeEngineMode == PlaybackEngineMode.MPV) {
                                MpvEngineController.seekTo(target)
                            } else {
                                exoPlayer?.seekTo(target)
                            }
                            currentPositionMsState = target
                        },
                        onFrameStep = { stepSign -> stepFrames(stepSign) },
                        onToggleDecoder = {
                            val next = when (decoderPriority) {
                                "AUTO" -> "HW_ONLY"
                                "HW_ONLY" -> "SW_PREFER"
                                else -> "AUTO"
                            }
                            switchDecoderPriorityAndRebuild(next)
                        },
                        onDecoderPriorityChange = { priority ->
                            switchDecoderPriorityAndRebuild(priority)
                        },
                        onToggleLock = { isLocked = !isLocked },
                        onToggleSmoothMotion = {
                            val next = !AppSettings.smoothMotion120fps.value
                            AppSettings.setSmoothMotion(next)
                            applyRefreshRateHint(detectedFpsState, next)
                        },
                        onSpeedChange = { speed ->
                            currentSpeedState = speed.coerceIn(0.25f, 4.0f)
                            val pitch = if (AppSettings.audioPitchCorrection.value) 1.0f else currentSpeedState
                            if (activeEngineMode == PlaybackEngineMode.MPV) {
                                MpvEngineController.setSpeed(currentSpeedState, AppSettings.audioPitchCorrection.value)
                            } else {
                                exoPlayer?.playbackParameters = PlaybackParameters(currentSpeedState, pitch)
                            }
                        },
                        onRepeatModeCycle = {
                            val next = when (repeatModeSetting) {
                                "OFF" -> "ONE"
                                "ONE" -> "ALL"
                                else -> "OFF"
                            }
                            AppSettings.setPlayerRepeatMode(next)
                        },
                        onSetAbStart = { pos -> abLoopStartMsState = pos },
                        onSetAbEnd = { pos ->
                            abLoopEndMsState = pos
                            if (abLoopStartMsState >= 0L && pos > abLoopStartMsState + 200L) {
                                abLoopActiveState = true
                            }
                        },
                        onToggleAbLoop = { enabled ->
                            abLoopActiveState = enabled
                            if (enabled && abLoopStartMsState >= 0L) {
                                val pos = currentPositionMsState
                                if (pos < abLoopStartMsState || pos > abLoopEndMsState) {
                                    if (activeEngineMode == PlaybackEngineMode.MPV) {
                                        MpvEngineController.seekTo(abLoopStartMsState)
                                    } else {
                                        exoPlayer?.seekTo(abLoopStartMsState)
                                    }
                                }
                            }
                        },
                        onClearAbLoop = {
                            abLoopActiveState = false
                            abLoopStartMsState = -1L
                            abLoopEndMsState = -1L
                        },
                        onAspectRatioChange = { mode ->
                            currentAspectRatioMode = mode
                            zoomScaleState = 1.0f
                            panOffsetXState = 0f
                            panOffsetYState = 0f
                            currentUriState?.let { u -> AppSettings.setVideoAspectOverride(u.toString(), mode) }
                        },
                        onZoomPanChange = { scale, panX, panY ->
                            zoomScaleState = scale.coerceIn(0.5f, 4.0f)
                            panOffsetXState = panX
                            panOffsetYState = panY
                        },
                        onBrightnessChange = { brightness ->
                            val clamped = brightness.coerceIn(0.01f, 1.0f)
                            val params = window.attributes
                            params.screenBrightness = clamped
                            window.attributes = params
                            if (AppSettings.rememberBrightness.value) {
                                AppSettings.setSavedPlayerBrightness(clamped)
                            }
                        },
                        onVolumeChange = { volPct -> applyVolumeAndBoost(volPct) },
                        onCaptureScreenshot = { captureLosslessScreenshot() },
                        onRetryPlayback = { rebuildAndPrepareAtCurrentPos() },
                        onSwitchToSoftwareAndRetry = { switchDecoderPriorityAndRebuild("SW_PREFER") },
                        onPipClick = { enterPiPMode() },
                        onBackClick = {
                            if (AppSettings.videoMiniPlayerEnabled.value &&
                                !AppSettings.pipOnHomeGestureOnly.value &&
                                isPlayingState && !isInPipState
                            ) {
                                enterPiPMode()
                            } else {
                                persistCurrentPlayback()
                                MediaPlaybackService.detachVideoPlayer(this@ModernPlayerActivity, activeVideoPlayerForService())
                                finish()
                            }
                        }
                    )
                }
            }
        }
    }

    private fun parseAndLoadIntent(intent: Intent?, rebuildPlayer: Boolean) {
        if (intent == null) return
        val extractedUri: Uri? = when (intent.action) {
            Intent.ACTION_SEND -> {
                val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    @Suppress("DEPRECATION")
                    intent.getParcelableExtra(Intent.EXTRA_STREAM)
                }
                streamUri ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.trim()?.let { txt ->
                    val token = txt.lineSequence().firstOrNull { it.contains("://") }?.trim() ?: txt
                    runCatching { Uri.parse(token) }.getOrNull()
                }
            }
            else -> {
                intent.data ?: intent.getStringExtra("video_uri")?.let { runCatching { Uri.parse(it) }.getOrNull() }
            }
        }

        if (extractedUri == null) {
            if (currentUriState == null) {
                activePlaybackError = "No playable media URI was provided."
            }
            return
        }

        val urisExtra = intent.getStringArrayListExtra("queue_uris")
        val titlesExtra = intent.getStringArrayListExtra("queue_titles")
        if (!urisExtra.isNullOrEmpty()) {
            queueUrisState = urisExtra
            queueTitlesState = titlesExtra ?: urisExtra.map { resolveTitleFromUri(Uri.parse(it), null) }
            currentQueueIndexState = intent.getIntExtra("queue_index", 0).coerceIn(0, urisExtra.lastIndex)
        } else {
            val resolvedTitle = resolveTitleFromUri(extractedUri, intent.getStringExtra("video_title"))
            queueUrisState = listOf(extractedUri.toString())
            queueTitlesState = listOf(resolvedTitle)
            currentQueueIndexState = 0
        }

        currentUriState = extractedUri
        videoTitleState = resolveTitleFromUri(extractedUri, intent.getStringExtra("video_title"))
        PlaybackDiagnosticsLog.info(
            engine = "PLAYER",
            category = "OPEN",
            message = "Opening '${videoTitleState}' (${extractedUri.scheme ?: "file"})"
        )
        val rawScheme = extractedUri.scheme?.lowercase(Locale.ROOT).orEmpty()
        if (rawScheme in setOf("smb", "ftp", "sftp")) {
            lifecycleScope.launch(Dispatchers.IO) {
                val shares = AppSettings.networkShares.value
                val rawStr = extractedUri.toString()
                val matched = shares.firstOrNull { rawStr.startsWith(it.baseUrl, ignoreCase = true) }
                    ?: shares.firstOrNull {
                        it.protocol.equals(rawScheme, ignoreCase = true) &&
                            !extractedUri.host.isNullOrBlank() &&
                            it.baseUrl.contains(extractedUri.host!!, ignoreCase = true)
                    }
                val shareCfg = matched ?: com.cmstudio.player.ui.model.NetworkShareConfig(
                    id = "intent_${rawScheme}",
                    name = extractedUri.host ?: "Remote",
                    protocol = rawScheme.uppercase(Locale.ROOT),
                    baseUrl = rawStr
                )
                val pwd = matched?.let { AppSettings.getNetworkSharePassword(it.id) }.orEmpty()
                val resolved = runCatching {
                    NetworkBrowserClient.resolvePlaybackUrl(shareCfg, rawStr, pwd)
                }.getOrDefault(rawStr)
                if (resolved != rawStr && currentUriState?.toString() == rawStr) {
                    withContext(Dispatchers.Main) {
                        currentUriState = Uri.parse(resolved)
                        if (activeEngineMode == PlaybackEngineMode.MPV) {
                            loadedMpvInitial = true
                            loadCurrentMediaIntoMpv(this@ModernPlayerActivity, pendingResumePositionMs.coerceAtLeast(0L), autoPlay = true)
                        } else {
                            exoPlayer?.let { p -> prepareCurrentMediaOnPlayer(p, pendingResumePositionMs.coerceAtLeast(0L)) }
                        }
                    }
                }
            }
        }
        val parsedHeaders = linkedMapOf<String, String>()
        intent.extras?.let { extras ->
            parsedHeaders.putAll(LaunchHeaders.read(extras, "headers"))
        }
        val extraUa = intent.getStringExtra("http_user_agent")?.trim().orEmpty()
        val extraAuth = intent.getStringExtra("http_authorization")?.trim().orEmpty()
        val extraReferer = intent.getStringExtra("http_referer")?.trim().orEmpty()
        if (extraUa.isNotEmpty()) parsedHeaders["User-Agent"] = extraUa
        if (extraAuth.isNotEmpty()) parsedHeaders["Authorization"] = extraAuth
        if (extraReferer.isNotEmpty()) parsedHeaders["Referer"] = extraReferer
        intentHttpHeadersState = parsedHeaders
        if (parsedHeaders.isNotEmpty()) {
            com.cmstudio.player.ui.network.NetworkPlaybackHeaders.registerForUrl(extractedUri.toString(), parsedHeaders)
        }
        activePlaybackError = null
        activePlaybackErrorDetails = null
        abLoopActiveState = false
        abLoopStartMsState = -1L
        abLoopEndMsState = -1L
        zoomScaleState = 1.0f
        panOffsetXState = 0f
        panOffsetYState = 0f
        droppedFramesTotalState = 0

        val explicitPos = intent.getLongExtra("start_position_ms", -1L)
        pendingResumePositionMs = when {
            explicitPos >= 0L -> explicitPos
            AppSettings.rememberPosition.value -> PlaybackHistoryBridge.savedPositionFor(this, extractedUri)
            else -> 0L
        }

        // Merge local sibling sidecar subtitles + third-party launcher intent subtitles (`subs` / `subs.enable`)
        val intentSubs = parseLaunchSubtitles(intent.extras)
        addedSubtitles = (discoverSidecarSubtitles(extractedUri) + intentSubs).distinctBy { it.uri }

        perVideoEngineOverride = AppSettings.getVideoEngineOverride(extractedUri.toString())
        loadedMpvInitial = false
        val nextMode = resolveEffectiveEngineForCurrentMedia()
        if (rebuildPlayer) {
            if (nextMode != activeEngineMode) {
                switchEngineInternal(nextMode, pendingResumePositionMs.coerceAtLeast(0L), autoPlay = true)
            } else if (activeEngineMode == PlaybackEngineMode.MPV) {
                loadedMpvInitial = true
                loadCurrentMediaIntoMpv(this, pendingResumePositionMs.coerceAtLeast(0L), autoPlay = true)
            } else {
                val pv = playerView
                if (exoPlayer != null) {
                    prepareCurrentMediaOnPlayer(exoPlayer!!, pendingResumePositionMs)
                } else if (pv != null) {
                    createAndPreparePlayer(this, pv)
                }
            }
        } else {
            activeEngineMode = nextMode
        }
    }

    private fun resolveEffectiveEngineForCurrentMedia(): PlaybackEngineMode {
        val uriStr = currentUriState?.toString()
        val globalMode = PlaybackEngineMode.fromId(AppSettings.defaultEngine.value)
        return PlaybackEngineResolver.resolveEffectiveEngine(
            configuredGlobal = globalMode,
            perVideoOverride = perVideoEngineOverride,
            uriString = uriStr,
            title = videoTitleState,
            autoRouteDolbyVision = AppSettings.autoRoutingDolbyVision.value,
            autoRouteStreams = AppSettings.autoRoutingHlsDash.value,
            detectedHdrLabel = hdrFormatTextState
        )
    }

    private fun activeVideoPlayerForService(): Player? {
        return if (activeEngineMode == PlaybackEngineMode.MPV) mpvMedia3Adapter else exoPlayer
    }

    private fun switchEngineLive(requested: PlaybackEngineMode) {
        val uriStr = currentUriState?.toString()
        if (requested == PlaybackEngineMode.AUTO) {
            perVideoEngineOverride = null
            if (!uriStr.isNullOrBlank()) AppSettings.setVideoEngineOverride(uriStr, null)
            AppSettings.setDefaultEngine("AUTO")
        } else {
            perVideoEngineOverride = requested
            if (!uriStr.isNullOrBlank()) AppSettings.setVideoEngineOverride(uriStr, requested)
        }
        val effective = resolveEffectiveEngineForCurrentMedia()
        if (effective == activeEngineMode) {
            Toast.makeText(this, "Engine preference updated (${requested.shortLabel})", Toast.LENGTH_SHORT).show()
            return
        }
        val curPos = currentPositionMsState.coerceAtLeast(0L)
        val wasPlaying = isPlayingState
        switchEngineInternal(effective, curPos, wasPlaying)
    }

    private fun switchEngineInternal(targetMode: PlaybackEngineMode, resumePosMs: Long, autoPlay: Boolean) {
        PlaybackDiagnosticsLog.info(
            engine = targetMode.name,
            category = "ENGINE",
            message = "Switching playback engine to ${targetMode.shortLabel} at ${resumePosMs}ms"
        )
        activePlaybackError = null
        activePlaybackErrorDetails = null
        isNativeBufferingState = false
        if (targetMode == PlaybackEngineMode.MPV) {
            MediaPlaybackService.detachVideoPlayer(this, exoPlayer)
            releaseEffectsAndPlayer()
            playerView = null
            pendingResumePositionMs = resumePosMs
            loadedMpvInitial = false
            activeEngineMode = PlaybackEngineMode.MPV
        } else {
            MediaPlaybackService.detachVideoPlayer(this, mpvMedia3Adapter)
            MpvEngineController.pause()
            MpvEngineController.detachSurface()
            mpvSurfaceView = null
            pendingResumePositionMs = resumePosMs
            activeEngineMode = PlaybackEngineMode.NATIVE
            playerView?.let { pv -> createAndPreparePlayer(this, pv, startPosOverride = resumePosMs) }
        }
        if (activeEngineMode == PlaybackEngineMode.MPV) {
            // loadCurrentMediaIntoMpv will also run cleanly in AndroidView factory once surface is ready;
            // pre-initialize now to catch device fallback early before showing an empty SurfaceView.
            val ok = MpvEngineController.ensureInitialized(this, AppSettings.currentEngineRenderConfig())
            if (!ok) {
                Toast.makeText(this, "MPV backend unavailable here; using Native ExoPlayer", Toast.LENGTH_SHORT).show()
                activeEngineMode = PlaybackEngineMode.NATIVE
            } else if (!loadedMpvInitial) {
                loadedMpvInitial = true
                loadCurrentMediaIntoMpv(this, resumePosMs, autoPlay)
            }
        }
    }

    private fun loadCurrentMediaIntoMpv(ctx: Context, resumePosMs: Long, autoPlay: Boolean) {
        val targetUri = currentUriState ?: return
        hasResolvedVideoSize = false
        videoWidthPx = 0
        videoHeightPx = 0
        activePlaybackError = null
        activePlaybackErrorDetails = null
        val renderCfg = AppSettings.currentEngineRenderConfig()
        val ok = MpvEngineController.ensureInitialized(ctx, renderCfg)
        if (!ok) {
            Toast.makeText(this, "MPV failed to start; falling back to Native ExoPlayer", Toast.LENGTH_SHORT).show()
            activeEngineMode = PlaybackEngineMode.NATIVE
            return
        }
        MpvEngineController.setOnPlaybackEndedListener {
            if (sleepTimerUntilEndState) {
                sleepTimerUntilEndState = false
                persistCurrentPlayback()
                return@setOnPlaybackEndedListener
            }
            if (AppSettings.playerRepeatMode.value == "OFF" &&
                AppSettings.autoPlayNext.value &&
                queueUrisState.size > 1
            ) {
                if (AppSettings.playerShuffleMode.value) {
                    val nextIdx = (queueUrisState.indices - currentQueueIndexState).randomOrNull()
                    if (nextIdx != null) {
                        jumpToQueueIndex(nextIdx)
                        return@setOnPlaybackEndedListener
                    }
                } else if (currentQueueIndexState < queueUrisState.lastIndex) {
                    jumpToQueueIndex(currentQueueIndexState + 1)
                    return@setOnPlaybackEndedListener
                }
            }
            persistCurrentPlayback()
            if (AppSettings.closeAfterEndOfPlayback.value && AppSettings.playerRepeatMode.value == "OFF") {
                finish()
            }
        }
        val activeHeaders = linkedMapOf<String, String>().apply {
            putAll(com.cmstudio.player.ui.network.NetworkPlaybackHeaders.getHeadersForUrl(targetUri.toString()))
            putAll(intentHttpHeadersState)
        }
        MpvEngineController.loadMedia(
            context = ctx,
            uri = targetUri,
            startPositionMs = resumePosMs,
            headers = activeHeaders,
            speed = currentSpeedState,
            pitchCorrection = AppSettings.audioPitchCorrection.value,
            fastSeek = AppSettings.fastSeekKeyframes.value,
            repeatMode = AppSettings.playerRepeatMode.value,
            initialSubUris = if (AppSettings.subtitlesEnabled.value) addedSubtitles.map { it.uri } else emptyList(),
            autoPlay = autoPlay
        )
        applySubtitleStyling(null)
        MpvEngineController.setAudioDelayMs(audioDelayAtomic.get())
        MpvEngineController.setSubtitleDelayMs(subtitleDelayAtomic.get())
        MpvEngineController.setSubtitleSpeedMilli(subtitleSpeedAtomic.get())
        val bands = EqualizerPresetsCatalog.bandsForPreset(AppSettings.eqPreset.value, AppSettings.eqCustomBandsDb.value)
        MpvEngineController.applyAudioEffects(
            boostPct = AppSettings.audioBoost.value,
            eqBandsDb = bands,
            channelMode = AppSettings.audioChannelMode.value,
            nightCompressor = AppSettings.nightModeCompressor.value,
            normalizeVolume = AppSettings.volumeNormalization.value,
            boostCapPct = AppSettings.volumeBoostCapPct.value
        )
        applyAspectAndSurfaceZoom()
        mpvMedia3Adapter?.notifyStateChanged()
    }

    private fun updateSecondarySubAndAbLoop(pos: Long) {
        if (AppSettings.dualSubtitles.value && secondaryCuesState.isNotEmpty()) {
            val adjMs = SubtitleTimingMath.adjustedPositionUs(
                pos * 1000L, subtitleDelayAtomic.get(), subtitleSpeedAtomic.get()
            ) / 1000L
            val activeCue = secondaryCuesState.firstOrNull { adjMs in it.startMs..it.endMs }
            activeSecondarySubTextState = activeCue?.text.orEmpty()
        } else if (activeSecondarySubTextState.isNotEmpty()) {
            activeSecondarySubTextState = ""
        }

        if (abLoopActiveState &&
            abLoopStartMsState >= 0L &&
            abLoopEndMsState > abLoopStartMsState + 200L &&
            pos >= abLoopEndMsState
        ) {
            if (activeEngineMode == PlaybackEngineMode.MPV) {
                MpvEngineController.seekTo(abLoopStartMsState)
            } else {
                exoPlayer?.seekTo(abLoopStartMsState)
            }
        }
    }

    private fun bindAssSubtitleOverlay(subtitleView: SubtitleView?, handler: AssHandler) {
        subtitleView ?: return
        for (i in subtitleView.childCount - 1 downTo 0) {
            if (subtitleView.getChildAt(i) is AssSubtitleView) {
                subtitleView.removeViewAt(i)
            }
        }
        subtitleView.withAssSupport(handler)
    }

    private fun createAndPreparePlayer(ctx: Context, pv: PlayerView, startPosOverride: Long? = null) {
        releaseEffectsAndPlayer()

        val selector = DefaultTrackSelector(ctx).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredAudioLanguage(AppSettings.audioLanguage.value.takeUnless { it == "Auto" })
                    .setPreferredTextLanguage(AppSettings.subtitleLanguage.value.takeUnless { it == "Auto" })
                    .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, !AppSettings.subtitlesEnabled.value)
                    .setAudioOffloadPreferences(
                        androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.Builder()
                            .setAudioOffloadMode(
                                if (AppSettings.audioOffload.value) {
                                    androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
                                } else {
                                    androidx.media3.common.TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
                                }
                            )
                            .build()
                    )
            )
        }
        trackSelector = selector

        val assHandler = AssHandler(renderType = AssRenderType.OVERLAY_CANVAS).also { assHandlerRef = it }
        val assParserFactory = AssSubtitleParserFactory(assHandler)
        val parserFactory = EnhancedSubtitleParserFactory(0.0).also { enhancedSubtitleParserFactory = it }
        val combinedSubtitleParserFactory = object : SubtitleParser.Factory {
            override fun supportsFormat(format: Format): Boolean =
                assParserFactory.supportsFormat(format) || parserFactory.supportsFormat(format)

            override fun getCueReplacementBehavior(format: Format): Int =
                if (assParserFactory.supportsFormat(format)) assParserFactory.getCueReplacementBehavior(format)
                else parserFactory.getCueReplacementBehavior(format)

            override fun create(format: Format): SubtitleParser =
                if (assParserFactory.supportsFormat(format)) assParserFactory.create(format)
                else parserFactory.create(format)
        }
        val defaultExtractors = DefaultExtractorsFactory()
            .setTsExtractorFlags(DefaultTsPayloadReaderFactory.FLAG_ENABLE_HDMV_DTS_AUDIO_STREAMS)
            .setTsExtractorTimestampSearchBytes(1500 * TsExtractor.TS_PACKET_SIZE)
        val extractorsFactory = WebmVttExtractorsFactory(
            defaultExtractors,
            combinedSubtitleParserFactory
        ).withAssMkvSupport(assParserFactory, assHandler)

        val extMode = when (AppSettings.decoderPriority.value) {
            "HW_ONLY" -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF
            "SW_PREFER" -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER
            else -> DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
        }

        @Suppress("WrongConstant")
        val renderersFactory = DelayRenderersFactory(
            ctx,
            subtitleDelayAtomic,
            subtitleSpeedAtomic,
            audioDelayAtomic,
            audioChannelModeAtomic
        )
            .setExtensionRendererMode(extMode)
            .setMapDV7ToHevc(AppSettings.autoRoutingDolbyVision.value)
            .withAssSupport(assHandler)

        val httpFactory = DataSource.Factory {
            val activeHeaders = linkedMapOf<String, String>().apply {
                putAll(com.cmstudio.player.ui.network.NetworkPlaybackHeaders.getHeadersForUrl(currentUriState?.toString()))
                putAll(intentHttpHeadersState)
            }
            val ua = activeHeaders.entries
                .firstOrNull { it.key.equals("User-Agent", ignoreCase = true) }
                ?.value
                ?.takeIf { it.isNotBlank() }
                ?: "MPVXPlayer/1.0"
            DefaultHttpDataSource.Factory()
                .setAllowCrossProtocolRedirects(true)
                .setUserAgent(ua)
                .apply {
                    val reqHeaders = activeHeaders.filterKeys { !it.equals("User-Agent", ignoreCase = true) }
                    if (reqHeaders.isNotEmpty()) setDefaultRequestProperties(reqHeaders)
                }
                .createDataSource()
        }
        val mediaSourceFactory = DefaultMediaSourceFactory(
            DefaultDataSource.Factory(ctx, httpFactory),
            extractorsFactory
        ).setSubtitleParserFactory(combinedSubtitleParserFactory)

        val bandwidthMeter = NetworkSpeed(
            androidx.media3.exoplayer.upstream.DefaultBandwidthMeter.Builder(ctx).build()
        ).also { networkSpeedMonitor = it }
        val loadControl = PatientLoadControl(
            DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    AppSettings.minBufferMs.value.coerceAtLeast(1500),
                    AppSettings.maxBufferMs.value.coerceAtLeast(5000),
                    AppSettings.bufferForPlaybackMs.value.coerceIn(250, 10000),
                    AppSettings.bufferForPlaybackAfterRebufferMs.value.coerceIn(500, 20000)
                )
                .build()
        )

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
            .build()

        val player = ExoPlayer.Builder(ctx, renderersFactory)
            .setTrackSelector(selector)
            .setMediaSourceFactory(mediaSourceFactory)
            .setBandwidthMeter(bandwidthMeter)
            .setLoadControl(loadControl)
            .setAudioAttributes(audioAttrs, true)
            .setHandleAudioBecomingNoisy(true)
            .setSeekParameters(
                if (AppSettings.fastSeekKeyframes.value) SeekParameters.CLOSEST_SYNC else SeekParameters.EXACT
            )
            .build()

        player.repeatMode = when (AppSettings.playerRepeatMode.value) {
            "ONE" -> Player.REPEAT_MODE_ONE
            "ALL" -> Player.REPEAT_MODE_ALL
            else -> Player.REPEAT_MODE_OFF
        }
        val pitch = if (AppSettings.audioPitchCorrection.value) 1.0f else currentSpeedState
        player.playbackParameters = PlaybackParameters(currentSpeedState, pitch)

        player.addListener(object : Player.Listener {
            override fun onPlayerError(error: PlaybackException) {
                isPlayingState = false
                activePlaybackError = PlaybackError.plainly(this@ModernPlayerActivity, error)
                PlaybackDiagnosticsLog.error(
                    engine = "EXO",
                    category = "ERROR",
                    message = "${error.errorCodeName}: ${activePlaybackError ?: error.localizedMessage ?: "unknown"}"
                )
                activePlaybackErrorDetails = PlaybackError.details(
                    this@ModernPlayerActivity,
                    error,
                    activeDecoderNameState.ifBlank { "Media3" },
                    currentUriState?.toString()
                )
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isNativeBufferingState = playbackState == Player.STATE_BUFFERING
                if (playbackState == Player.STATE_ENDED) {
                    if (sleepTimerUntilEndState) {
                        sleepTimerUntilEndState = false
                        persistCurrentPlayback()
                        return
                    }
                    if (AppSettings.playerRepeatMode.value == "OFF" &&
                        AppSettings.autoPlayNext.value &&
                        queueUrisState.size > 1
                    ) {
                        if (AppSettings.playerShuffleMode.value) {
                            val nextIdx = (queueUrisState.indices - currentQueueIndexState).randomOrNull()
                            if (nextIdx != null) {
                                jumpToQueueIndex(nextIdx)
                                return
                            }
                        } else if (currentQueueIndexState < queueUrisState.lastIndex) {
                            jumpToQueueIndex(currentQueueIndexState + 1)
                            return
                        }
                    }
                    persistCurrentPlayback()
                    if (AppSettings.closeAfterEndOfPlayback.value && AppSettings.playerRepeatMode.value == "OFF") {
                        finish()
                    }
                }
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                if (videoSize.width > 0 && videoSize.height > 0) {
                    @Suppress("DEPRECATION")
                    val unappliedRot = videoSize.unappliedRotationDegrees
                    val (dispW, dispH) = PlayerControlCatalog.resolveDisplayDimensions(
                        videoSize.width,
                        videoSize.height,
                        unappliedRot
                    )
                    videoWidthPx = dispW
                    videoHeightPx = dispH
                    hasResolvedVideoSize = true
                    naturalVideoAspect = (dispW.toFloat() / dispH.toFloat()) *
                        (if (dispW != videoSize.width && videoSize.pixelWidthHeightRatio > 0f) 1f / videoSize.pixelWidthHeightRatio else videoSize.pixelWidthHeightRatio)
                    resolutionTextState = "${dispW}x${dispH}"
                    updatePipAspectParams()
                }
            }

            override fun onTracksChanged(tracks: Tracks) {
                videoTrackOptionsState = collectTrackOptions(tracks, C.TRACK_TYPE_VIDEO)
                val audioList = collectTrackOptions(tracks, C.TRACK_TYPE_AUDIO)
                val subList = collectTrackOptions(tracks, C.TRACK_TYPE_TEXT)
                audioTrackOptionsState = audioList
                subtitleTrackOptionsState = subList
                subtitlesDisabledState = subList.none { it.selected }
                updatePipAspectParams()
            }
        })

        player.addAnalyticsListener(object : AnalyticsListener {
            override fun onVideoDecoderInitialized(
                eventTime: AnalyticsListener.EventTime,
                decoderName: String,
                initializedTimestampMs: Long,
                initializationDurationMs: Long
            ) {
                activeDecoderNameState = decoderName
                val lower = decoderName.lowercase(Locale.US)
                isHwDecoderState = !(lower.startsWith("c2.android.") ||
                    lower.startsWith("omx.google.") ||
                    lower.contains("ffmpeg") ||
                    lower.contains("dav1d") ||
                    lower.contains("gav1"))
                PlaybackDiagnosticsLog.info(
                    engine = "EXO",
                    category = "DECODER",
                    message = "Video decoder initialized: $decoderName (${if (isHwDecoderState) "HW" else "SW"}, ${initializationDurationMs}ms)"
                )
            }

            override fun onVideoInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                codecNameState = format.sampleMimeType
                    ?.substringAfter('/')
                    ?.uppercase(Locale.US)
                    ?: "Video"
                if (format.width > 0 && format.height > 0) {
                    val (dispW, dispH) = PlayerControlCatalog.resolveDisplayDimensions(
                        format.width,
                        format.height,
                        format.rotationDegrees
                    )
                    videoWidthPx = dispW
                    videoHeightPx = dispH
                    hasResolvedVideoSize = true
                    naturalVideoAspect = dispW.toFloat() / dispH.toFloat()
                    resolutionTextState = "${dispW}x${dispH}"
                }
                if (format.frameRate > 1f && format.frameRate != Format.NO_VALUE.toFloat()) {
                    detectedFpsState = format.frameRate
                    enhancedSubtitleParserFactory?.setFallbackFrameRate(format.frameRate.toDouble())
                    applyRefreshRateHint(format.frameRate, AppSettings.smoothMotion120fps.value)
                }
                hdrFormatTextState = when (format.colorInfo?.colorTransfer) {
                    C.COLOR_TRANSFER_ST2084 -> if (format.sampleMimeType == MimeTypes.VIDEO_DOLBY_VISION) "Dolby Vision" else "HDR10"
                    C.COLOR_TRANSFER_HLG -> "HLG"
                    else -> "SDR"
                }
                bitrateTextState = if (format.bitrate > 0) {
                    String.format(Locale.US, "%.1f Mbps", format.bitrate / 1_000_000f)
                } else ""
            }

            override fun onAudioInputFormatChanged(
                eventTime: AnalyticsListener.EventTime,
                format: Format,
                decoderReuseEvaluation: DecoderReuseEvaluation?
            ) {
                channelLayoutTextState = when (format.channelCount) {
                    1 -> "Mono"
                    2 -> "Stereo"
                    6 -> "5.1"
                    8 -> "7.1"
                    else -> if (format.channelCount > 0) "${format.channelCount}ch" else "Audio"
                }
            }

            override fun onDroppedVideoFrames(
                eventTime: AnalyticsListener.EventTime,
                droppedFrames: Int,
                elapsedMs: Long
            ) {
                droppedFramesTotalState += droppedFrames
                if (droppedFrames > 0) {
                    PlaybackDiagnosticsLog.warn(
                        engine = "EXO",
                        category = "DROPPED_FRAMES",
                        message = "Dropped $droppedFrames video frames in ${elapsedMs}ms (total=$droppedFramesTotalState)"
                    )
                }
            }

            override fun onAudioSessionIdChanged(
                eventTime: AnalyticsListener.EventTime,
                audioSessionId: Int
            ) {
                if (audioSessionId != C.AUDIO_SESSION_ID_UNSET && audioSessionId != 0) {
                    activeAudioSessionId = audioSessionId
                    applyAudioEffects(audioSessionId)
                }
            }
        })

        exoPlayer = player
        assHandler.init(player)
        pv.player = player
        bindAssSubtitleOverlay(pv.subtitleView, assHandler)
        prepareCurrentMediaOnPlayer(player, startPosOverride ?: pendingResumePositionMs)
    }

    private fun prepareCurrentMediaOnPlayer(player: ExoPlayer, resumePosMs: Long) {
        val targetUri = currentUriState ?: return
        hasResolvedVideoSize = false
        videoWidthPx = 0
        videoHeightPx = 0
        activePlaybackError = null
        val builder = MediaItem.Builder().setUri(targetUri)
        if (addedSubtitles.isNotEmpty()) {
            builder.setSubtitleConfigurations(addedSubtitles)
        }
        player.setMediaItem(builder.build())
        player.prepare()
        if (resumePosMs > 1500L) {
            player.seekTo(resumePosMs)
        }
        player.playWhenReady = true
    }

    private fun switchDecoderPriorityAndRebuild(priority: String) {
        AppSettings.setDecoderPriority(priority)
        activeDecoderNameState = ""
        activePlaybackError = null
        if (activeEngineMode == PlaybackEngineMode.MPV) {
            // `renderConfig` LaunchedEffect pushes new `hwdec` into MpvEngineController immediately
            return
        }
        val pos = exoPlayer?.currentPosition ?: currentPositionMsState
        val pv = playerView ?: return
        createAndPreparePlayer(this, pv, startPosOverride = pos)
    }

    private fun rebuildAndPrepareAtCurrentPos() {
        val pos = currentPositionMsState.coerceAtLeast(0L)
        activePlaybackError = null
        activePlaybackErrorDetails = null
        if (activeEngineMode == PlaybackEngineMode.MPV) {
            loadCurrentMediaIntoMpv(this, pos, autoPlay = true)
            return
        }
        val pv = playerView ?: return
        createAndPreparePlayer(this, pv, startPosOverride = pos)
    }

    private fun jumpToQueueIndex(targetIndex: Int) {
        if (targetIndex !in queueUrisState.indices) return
        persistCurrentPlayback()
        currentQueueIndexState = targetIndex
        val nextUri = Uri.parse(queueUrisState[targetIndex])
        currentUriState = nextUri
        videoTitleState = queueTitlesState.getOrNull(targetIndex) ?: resolveTitleFromUri(nextUri, null)
        abLoopActiveState = false
        abLoopStartMsState = -1L
        abLoopEndMsState = -1L
        droppedFramesTotalState = 0
        addedSubtitles = discoverSidecarSubtitles(nextUri)
        perVideoEngineOverride = AppSettings.getVideoEngineOverride(nextUri.toString())
        currentAspectRatioMode = AppSettings.getVideoAspectOverride(nextUri.toString()) ?: AppSettings.defaultAspectRatio.value
        val resumeMs = if (AppSettings.rememberPosition.value) PlaybackHistoryBridge.savedPositionFor(this, nextUri) else 0L
        val nextMode = resolveEffectiveEngineForCurrentMedia()
        if (nextMode != activeEngineMode) {
            switchEngineInternal(nextMode, resumeMs, autoPlay = true)
        } else if (activeEngineMode == PlaybackEngineMode.MPV) {
            loadCurrentMediaIntoMpv(this, resumeMs, autoPlay = true)
        } else {
            exoPlayer?.let { prepareCurrentMediaOnPlayer(it, resumeMs) }
        }
    }

    private fun stepFrames(stepSign: Int) {
        if (activeEngineMode == PlaybackEngineMode.MPV) {
            MpvEngineController.stepFrame(stepSign > 0)
            return
        }
        val player = exoPlayer ?: return
        player.pause()
        val fps = if (detectedFpsState > 1f) detectedFpsState else 24f
        val stepMs = max(16L, (1000.0 / fps).roundToInt().toLong())
        val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: Long.MAX_VALUE
        val target = (player.currentPosition + stepSign * stepMs).coerceIn(0L, dur)
        player.setSeekParameters(SeekParameters.EXACT)
        player.seekTo(target)
        player.setSeekParameters(if (AppSettings.fastSeekKeyframes.value) SeekParameters.CLOSEST_SYNC else SeekParameters.EXACT)
        currentPositionMsState = target
    }

    private fun attachExternalSubtitleUri(pickedUri: Uri) {
        try {
            val displayName = resolveDisplayName(pickedUri)
            val ext = displayName.substringAfterLast('.', "").lowercase(Locale.US)
            if (ext !in setOf("srt", "vtt", "ass", "ssa", "ttml", "xml", "smi", "sami", "sub")) {
                Toast.makeText(this, "Unsupported subtitle format: .$ext", Toast.LENGTH_SHORT).show()
                return
            }

            var subUri = SubtitleFiles.copy(this, pickedUri, displayName) ?: pickedUri
            if (activeEngineMode == PlaybackEngineMode.MPV) {
                AppSettings.setSubtitlesEnabled(true)
                val added = MpvEngineController.addExternalSubtitle(this, subUri)
                if (added) {
                    Toast.makeText(this, "Loaded subtitle: $displayName", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Failed to load subtitle in MPV", Toast.LENGTH_SHORT).show()
                }
                return
            }

            val converted = SubtitleConverter().convertSubtitles(this, listOf(subUri))
            if (converted.isNotEmpty() && converted[0] != null) {
                subUri = converted[0]
            }

            val config = SubtitleUtils.buildSubtitle(this, subUri, displayName, null, true)
            addedSubtitles = addedSubtitles.filterNot { it.uri == config.uri } + config

            // Ensure text tracks are NOT disabled when user explicitly loads external subtitles
            AppSettings.setSubtitlesEnabled(true)
            trackSelector?.let { ts ->
                ts.setParameters(
                    ts.parameters.buildUpon()
                        .setTrackTypeDisabled(C.TRACK_TYPE_TEXT, false)
                        .build()
                )
            }

            val pos = exoPlayer?.currentPosition ?: 0L
            val wasPlaying = exoPlayer?.playWhenReady ?: true
            exoPlayer?.let { p ->
                val mediaItem = MediaItem.Builder()
                    .setUri(currentUriState ?: return)
                    .setSubtitleConfigurations(addedSubtitles)
                    .build()
                p.setMediaItem(mediaItem, pos)
                p.prepare()
                p.playWhenReady = wasPlaying
            }
            Toast.makeText(this, "Loaded subtitle: $displayName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to load subtitle: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun discoverSidecarSubtitles(videoUri: Uri): List<MediaItem.SubtitleConfiguration> {
        val list = mutableListOf<MediaItem.SubtitleConfiguration>()
        try {
            val filePath = resolveLocalPath(videoUri) ?: return emptyList()
            val videoFile = File(filePath)
            val parentDir = videoFile.parentFile ?: return emptyList()
            val baseName = videoFile.nameWithoutExtension
            val exts = listOf("srt", "vtt", "ass", "ssa", "ttml")
            for (ext in exts) {
                val candidate = File(parentDir, "$baseName.$ext")
                if (candidate.exists() && candidate.canRead()) {
                    val subUri = Uri.fromFile(candidate)
                    list.add(SubtitleUtils.buildSubtitle(this, subUri, candidate.name, null, list.isEmpty()))
                }
            }
        } catch (_: Throwable) {
        }
        return list
    }

    private fun parseLaunchSubtitles(extras: Bundle?): List<MediaItem.SubtitleConfiguration> {
        if (extras == null || !LaunchSubtitles.present(extras)) return emptyList()
        val subs = LaunchSubtitles.urisByPosition(extras, *LaunchSubtitles.FILES)
        val enabled = LaunchSubtitles.uris(extras, *LaunchSubtitles.ENABLE)
        val names = LaunchSubtitles.strings(extras, *LaunchSubtitles.NAMES)
        val langs = LaunchSubtitles.strings(extras, *LaunchSubtitles.LANGUAGES)
        val out = mutableListOf<MediaItem.SubtitleConfiguration>()
        subs.forEachIndexed { idx, subUri ->
            if (subUri != null) {
                val label = names.getOrNull(idx)
                val lang = langs.getOrNull(idx)
                val select = enabled.contains(subUri) || (enabled.isEmpty() && idx == 0)
                out.add(SubtitleUtils.buildSubtitle(this, subUri, label, lang, select))
            }
        }
        return out
    }

    private fun resolveLocalPath(uri: Uri): String? {
        if ("file".equals(uri.scheme, ignoreCase = true)) return uri.path
        if ("content".equals(uri.scheme, ignoreCase = true)) {
            try {
                @Suppress("DEPRECATION")
                contentResolver.query(uri, arrayOf(MediaStore.Video.Media.DATA), null, null, null)?.use { c ->
                    if (c.moveToFirst()) {
                        val idx = c.getColumnIndex(MediaStore.Video.Media.DATA)
                        if (idx >= 0) return c.getString(idx)
                    }
                }
            } catch (_: Throwable) {
            }
        }
        return null
    }

    private fun resolveDisplayName(uri: Uri): String {
        try {
            contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) {
                        val name = cursor.getString(idx)
                        if (!name.isNullOrBlank()) return name
                    }
                }
            }
        } catch (_: Throwable) {
        }
        return uri.lastPathSegment ?: "subtitle.srt"
    }

    private fun resolveTitleFromUri(uri: Uri, fallback: String?): String {
        if (!fallback.isNullOrBlank()) return fallback
        return resolveDisplayName(uri)
    }

    private fun collectTrackOptions(tracks: Tracks, trackType: Int): List<PlayerTrackOption> {
        val result = mutableListOf<PlayerTrackOption>()
        var trackNum = 1
        tracks.groups.forEachIndexed { groupIndex, group ->
            if (group.type == trackType) {
                for (tIdx in 0 until group.length) {
                    if (!group.isTrackSupported(tIdx)) continue
                    val fmt = group.getTrackFormat(tIdx)
                    val fullLabel = if (trackType == C.TRACK_TYPE_VIDEO) {
                        val res = if (fmt.height > 0) "${fmt.height}p" else if (fmt.width > 0) "${fmt.width}w" else "Track $trackNum"
                        val mbps = if (fmt.bitrate > 0) String.format(Locale.US, " • %.1f Mbps", fmt.bitrate / 1_000_000f) else ""
                        "$res$mbps"
                    } else {
                        val label = fmt.label?.takeIf { it.isNotBlank() }
                            ?: fmt.language?.let { lang ->
                                val locale = Locale.forLanguageTag(lang.replace('_', '-'))
                                locale.getDisplayLanguage(Locale.ENGLISH).ifBlank { lang.uppercase(Locale.US) }
                            }
                            ?: "Track $trackNum"
                        val codecTag = fmt.sampleMimeType?.substringAfter('/')?.uppercase(Locale.US)
                        if (!codecTag.isNullOrBlank()) "$label ($codecTag)" else label
                    }
                    result.add(PlayerTrackOption(groupIndex, tIdx, fullLabel, group.isTrackSelected(tIdx)))
                    trackNum++
                }
            }
        }
        return result
    }

    private fun resetVideoQualityToAuto() {
        trackSelector?.let { ts ->
            ts.setParameters(ts.parameters.buildUpon().clearOverridesOfType(C.TRACK_TYPE_VIDEO).build())
        }
    }

    private fun loadSecondarySubtitleUri(pickedUri: Uri) {
        try {
            val displayName = resolveDisplayName(pickedUri)
            val ext = displayName.substringAfterLast('.', "").lowercase(Locale.US)
            if (ext !in setOf("srt", "vtt", "ass", "ssa", "ttml", "xml", "smi", "sami", "sub")) {
                Toast.makeText(this, "Unsupported subtitle format: .$ext", Toast.LENGTH_SHORT).show()
                return
            }
            val copied = SubtitleFiles.copy(this, pickedUri, "dual_$displayName") ?: pickedUri
            val mime = SubtitleUtils.getSubtitleMime(this, copied)
            val bytes = contentResolver.openInputStream(copied)?.use { it.readBytes() }
                ?: run {
                    Toast.makeText(this, "Unable to read subtitle file", Toast.LENGTH_SHORT).show()
                    return
                }
            val parser = EnhancedSubtitleParserFactory(detectedFpsState.toDouble()).create(
                Format.Builder().setSampleMimeType(mime).build()
            )
            val parsedCues = mutableListOf<SecondaryCueItem>()
            parser.parse(bytes, SubtitleParser.OutputOptions.allCues()) { cwt ->
                val joined = cwt.cues.mapNotNull { it.text?.toString()?.trim() }
                    .filter { it.isNotEmpty() }
                    .joinToString("\n")
                if (joined.isNotEmpty()) {
                    val startMs = (cwt.startTimeUs / 1000L).coerceAtLeast(0L)
                    val endMs = if (cwt.endTimeUs != C.TIME_UNSET && cwt.endTimeUs > cwt.startTimeUs) {
                        cwt.endTimeUs / 1000L
                    } else if (cwt.durationUs > 0L) {
                        startMs + (cwt.durationUs / 1000L)
                    } else {
                        startMs + 4000L
                    }
                    parsedCues.add(SecondaryCueItem(startMs, endMs, joined))
                }
            }
            secondaryCuesState = parsedCues
            secondarySubTitleState = displayName
            AppSettings.setDualSubtitles(true)
            Toast.makeText(this, "Secondary subtitle loaded (${parsedCues.size} cues)", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to parse secondary subtitle: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun importCustomSubtitleFont(pickedUri: Uri) {
        try {
            val name = resolveDisplayName(pickedUri)
            val fontDir = File(filesDir, "fonts").apply { mkdirs() }
            val dest = File(fontDir, "custom_subtitle_font.ttf")
            contentResolver.openInputStream(pickedUri)?.use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
            val tf = Typeface.createFromFile(dest)
            if (tf != null) {
                AppSettings.setSubtitleCustomFont(dest.absolutePath, name)
                Toast.makeText(this, "Applied custom font: $name", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Invalid font file: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sampleAmbientAuraColor(pv: PlayerView) {
        val tex = pv.videoSurfaceView as? TextureView
        if (tex != null && tex.isAvailable) {
            val bmp = tex.getBitmap(24, 14) ?: return
            ambientColorArgbState = averageDominantEdgeArgb(bmp)
            bmp.recycle()
            return
        }
        val sv = pv.videoSurfaceView as? SurfaceView ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && sv.holder.surface.isValid && sv.width > 0 && sv.height > 0) {
            val bmp = Bitmap.createBitmap(24, 14, Bitmap.Config.ARGB_8888)
            PixelCopy.request(
                sv,
                bmp,
                { res ->
                    if (res == PixelCopy.SUCCESS) {
                        ambientColorArgbState = averageDominantEdgeArgb(bmp)
                    }
                    bmp.recycle()
                },
                Handler(Looper.getMainLooper())
            )
        }
    }

    private fun averageDominantEdgeArgb(bmp: Bitmap): Long {
        var rSum = 0L; var gSum = 0L; var bSum = 0L; var count = 0L
        for (y in 0 until bmp.height step 2) {
            for (x in 0 until bmp.width step 2) {
                val px = bmp.getPixel(x, y)
                rSum += Color.red(px)
                gSum += Color.green(px)
                bSum += Color.blue(px)
                count++
            }
        }
        if (count <= 0L) return 0L
        val r = (rSum / count).toInt().coerceIn(0, 255)
        val g = (gSum / count).toInt().coerceIn(0, 255)
        val b = (bSum / count).toInt().coerceIn(0, 255)
        return (0xFFL shl 24) or (r.toLong() shl 16) or (g.toLong() shl 8) or b.toLong()
    }

    private fun selectTrack(trackType: Int, groupIndex: Int, trackIndex: Int) {
        if (activeEngineMode == PlaybackEngineMode.MPV) {
            when (trackType) {
                C.TRACK_TYPE_AUDIO -> MpvEngineController.selectAudioTrack(trackIndex)
                C.TRACK_TYPE_TEXT -> {
                    MpvEngineController.selectSubtitleTrack(trackIndex)
                    AppSettings.setSubtitlesEnabled(true)
                }
            }
            return
        }
        val selector = trackSelector ?: return
        val currentTracks = exoPlayer?.currentTracks ?: return
        val group = currentTracks.groups.getOrNull(groupIndex) ?: return
        val builder = selector.parameters.buildUpon()
            .setTrackTypeDisabled(trackType, false)
            .clearOverridesOfType(trackType)
            .addOverride(TrackSelectionOverride(group.mediaTrackGroup, trackIndex))
        selector.setParameters(builder.build())
        if (trackType == C.TRACK_TYPE_TEXT) {
            AppSettings.setSubtitlesEnabled(true)
        }
    }

    private fun disableTrackType(trackType: Int) {
        if (activeEngineMode == PlaybackEngineMode.MPV) {
            if (trackType == C.TRACK_TYPE_TEXT) {
                MpvEngineController.disableSubtitles()
                AppSettings.setSubtitlesEnabled(false)
            }
            return
        }
        val selector = trackSelector ?: return
        selector.setParameters(
            selector.parameters.buildUpon()
                .clearOverridesOfType(trackType)
                .setTrackTypeDisabled(trackType, true)
                .build()
        )
        if (trackType == C.TRACK_TYPE_TEXT) {
            AppSettings.setSubtitlesEnabled(false)
        }
    }

    private fun applySubtitleStyling(subtitleView: SubtitleView?) {
        val overrideAss = AppSettings.subOverrideAssMode.value
        val scaleByWindow = AppSettings.subScaleByWindow.value
        if (activeEngineMode == PlaybackEngineMode.MPV) {
            MpvEngineController.applySubtitleStyle(
                fontSizeSp = AppSettings.subtitleFontSize.value,
                colorArgb = AppSettings.subtitleColor.value,
                bgArgb = AppSettings.subtitleBackground.value,
                bgOpacity = AppSettings.subtitleBgOpacity.value,
                bottomPaddingDp = AppSettings.subtitleBottomPaddingDp.value,
                bold = AppSettings.subtitleBold.value,
                italic = AppSettings.subtitleItalic.value,
                alignment = AppSettings.subtitleAlignment.value,
                overrideAssMode = overrideAss,
                scaleByWindow = scaleByWindow
            )
        }
        subtitleView ?: return
        val fgArgb = (AppSettings.subtitleColor.value or 0xFF000000L).toInt()
        val baseBg = AppSettings.subtitleBackground.value.toInt() and 0x00FFFFFF
        val alphaByte = ((AppSettings.subtitleBgOpacity.value.coerceIn(0f, 1f) * 255f).roundToInt() and 0xFF) shl 24
        val bgArgb = alphaByte or baseBg
        val edgeType = when (AppSettings.subtitleEdgeType.value) {
            "OUTLINE" -> CaptionStyleCompat.EDGE_TYPE_OUTLINE
            "RAISED" -> CaptionStyleCompat.EDGE_TYPE_RAISED
            "DEPRESSED" -> CaptionStyleCompat.EDGE_TYPE_DEPRESSED
            "NONE" -> CaptionStyleCompat.EDGE_TYPE_NONE
            else -> CaptionStyleCompat.EDGE_TYPE_DROP_SHADOW
        }
        val baseTypeface = AppSettings.subtitleCustomFontUri.value
            .takeIf { it.isNotBlank() }
            ?.let { path -> runCatching { Typeface.createFromFile(File(path)) }.getOrNull() }
            ?: Typeface.DEFAULT
        val bold = AppSettings.subtitleBold.value
        val italic = AppSettings.subtitleItalic.value
        val style = when {
            bold && italic -> Typeface.BOLD_ITALIC
            bold -> Typeface.BOLD
            italic -> Typeface.ITALIC
            else -> Typeface.NORMAL
        }
        val typeface = Typeface.create(baseTypeface, style)
        subtitleView.textAlignment = when (AppSettings.subtitleAlignment.value) {
            "LEFT" -> View.TEXT_ALIGNMENT_TEXT_START
            "RIGHT" -> View.TEXT_ALIGNMENT_TEXT_END
            else -> View.TEXT_ALIGNMENT_CENTER
        }
        subtitleView.setApplyEmbeddedStyles(overrideAss == "NO")
        subtitleView.setStyle(
            CaptionStyleCompat(fgArgb, bgArgb, Color.TRANSPARENT, edgeType, Color.BLACK, typeface)
        )
        val windowScale = if (scaleByWindow) {
            (resources.displayMetrics.heightPixels.coerceAtLeast(480) / 1080f).coerceIn(0.75f, 1.85f)
        } else 1f
        subtitleView.setFixedTextSize(
            TypedValue.COMPLEX_UNIT_SP,
            (AppSettings.subtitleFontSize.value.coerceIn(12f, 44f) * windowScale).coerceIn(12f, 56f)
        )
        subtitleView.setBottomPaddingFraction((AppSettings.subtitleBottomPaddingDp.value / 400f).coerceIn(0.02f, 0.35f))
    }

    private fun applyAspectAndSurfaceZoom() {
        if (activeEngineMode == PlaybackEngineMode.MPV) {
            val sv = mpvSurfaceView
            val viewW = sv?.width?.takeIf { it > 0 } ?: 1080
            val viewH = sv?.height?.takeIf { it > 0 } ?: 1920
            val effectiveScale = if (currentAspectRatioMode == "Original" && abs(zoomScaleState - 1.0f) < 0.02f &&
                videoHeightPx > 0 && viewH > 0
            ) {
                (videoHeightPx.toFloat() / viewH.toFloat()).coerceIn(0.25f, 2.0f)
            } else {
                zoomScaleState
            }
            val normX = (panOffsetXState / viewW.toFloat()).coerceIn(-0.8f, 0.8f)
            val normY = (panOffsetYState / viewH.toFloat()).coerceIn(-0.8f, 0.8f)
            MpvEngineController.applyVideoGeometry(
                aspectMode = currentAspectRatioMode,
                zoomScale = effectiveScale,
                panNormX = normX,
                panNormY = normY,
                mirrored = isMirroredState,
                flippedVertical = isFlippedVerticalState
            )
            return
        }
        val pv = playerView ?: return
        val contentFrame = pv.findViewById<AspectRatioFrameLayout>(androidx.media3.ui.R.id.exo_content_frame)
        val normalized = currentAspectRatioMode
        when (normalized) {
            "Fill", "Crop", "Zoom" -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_ZOOM
                contentFrame?.setAspectRatio(naturalVideoAspect)
            }
            "Stretch" -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                contentFrame?.setAspectRatio(naturalVideoAspect)
            }
            "16:9" -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                contentFrame?.setAspectRatio(16f / 9f)
            }
            "4:3" -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                contentFrame?.setAspectRatio(4f / 3f)
            }
            "21:9" -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                contentFrame?.setAspectRatio(21f / 9f)
            }
            "16:10" -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                contentFrame?.setAspectRatio(16f / 10f)
            }
            "2.35:1" -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                contentFrame?.setAspectRatio(2.35f)
            }
            "2.39:1" -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                contentFrame?.setAspectRatio(2.39f)
            }
            "2:1" -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                contentFrame?.setAspectRatio(2f)
            }
            "5:4" -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FILL
                contentFrame?.setAspectRatio(5f / 4f)
            }
            else -> {
                pv.resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                contentFrame?.setAspectRatio(naturalVideoAspect)
            }
        }

        // Apply pinch/manual zoom strictly on videoSurfaceView so subtitles remain crisp and centered!
        val surface = pv.videoSurfaceView ?: return
        val effectiveScale = if (normalized == "Original" && abs(zoomScaleState - 1.0f) < 0.02f &&
            videoHeightPx > 0 && surface.height > 0
        ) {
            (videoHeightPx.toFloat() / surface.height.toFloat()).coerceIn(0.25f, 2.0f)
        } else {
            zoomScaleState
        }
        val flipX = if (isMirroredState) -1f else 1f
        val flipY = if (isFlippedVerticalState) -1f else 1f
        surface.scaleX = effectiveScale * flipX
        surface.scaleY = effectiveScale * flipY
        if (effectiveScale > 1.0f) {
            val maxPanX = surface.width * (effectiveScale - 1f) * 0.5f
            val maxPanY = surface.height * (effectiveScale - 1f) * 0.5f
            surface.translationX = panOffsetXState.coerceIn(-maxPanX, maxPanX)
            surface.translationY = panOffsetYState.coerceIn(-maxPanY, maxPanY)
        } else {
            surface.translationX = 0f
            surface.translationY = 0f
        }
    }

    private fun applyVolumeAndBoost(volPct: Float) {
        val maxCap = AppSettings.volumeBoostCapPct.value.coerceIn(100, 200).toFloat()
        val clamped = volPct.coerceIn(0f, maxCap)
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as? AudioManager ?: return
        val maxVol = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
        val systemRatio = (clamped / 100f).coerceIn(0f, 1f)
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (systemRatio * maxVol).roundToInt(), 0)
        AppSettings.setAudioBoost(clamped.roundToInt())
        applyAudioEffects(activeAudioSessionId, clamped)
    }

    private fun applyAudioEffects(sessionId: Int, overrideVolPct: Float? = null) {
        val boostCap = AppSettings.volumeBoostCapPct.value.coerceIn(100, 200)
        val normalize = AppSettings.volumeNormalization.value
        if (activeEngineMode == PlaybackEngineMode.MPV) {
            val boostPct = (overrideVolPct ?: AppSettings.audioBoost.value.toFloat()).roundToInt().coerceIn(0, boostCap)
            val eqBands = EqualizerPresetsCatalog.bandsForPreset(AppSettings.eqPreset.value, AppSettings.eqCustomBandsDb.value)
            MpvEngineController.applyAudioEffects(
                boostPct = boostPct,
                eqBandsDb = eqBands,
                channelMode = AppSettings.audioChannelMode.value,
                nightCompressor = AppSettings.nightModeCompressor.value,
                normalizeVolume = normalize,
                boostCapPct = boostCap
            )
            return
        }
        if (sessionId == C.AUDIO_SESSION_ID_UNSET || sessionId == 0) return

        val boostPct = (overrideVolPct ?: AppSettings.audioBoost.value.toFloat()).coerceIn(0f, boostCap.toFloat())
        try {
            if (boostPct > 100f) {
                val enhancer = loudnessEnhancer ?: LoudnessEnhancer(sessionId).also { loudnessEnhancer = it }
                val gainMb = (((boostPct - 100f) / 100f) * 1500f).roundToInt().coerceIn(0, 1500)
                enhancer.setTargetGain(gainMb)
                enhancer.enabled = true
            } else {
                loudnessEnhancer?.enabled = false
            }
        } catch (_: Throwable) {
        }

        val eqPreset = AppSettings.eqPreset.value
        val bandGainsDb = EqualizerPresetsCatalog.bandsForPreset(eqPreset, AppSettings.eqCustomBandsDb.value)
        try {
            val eq = equalizer ?: Equalizer(0, sessionId).also { equalizer = it }
            eq.enabled = true
            val bands = eq.numberOfBands.toInt()
            val minLv = eq.bandLevelRange[0]
            val maxLv = eq.bandLevelRange[1]
            for (b in 0 until bands) {
                val targetDb = bandGainsDb.getOrElse((b * 5) / bands.coerceAtLeast(1)) { 0f }
                val ratio = (targetDb / 12f).coerceIn(-1f, 1f)
                val level: Short = if (ratio >= 0f) {
                    (ratio * maxLv).roundToInt().toShort()
                } else {
                    (-ratio * minLv).roundToInt().toShort()
                }
                eq.setBandLevel(b.toShort(), level.coerceIn(minLv, maxLv))
            }
        } catch (_: Throwable) {
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val nightMode = AppSettings.nightModeCompressor.value
                val normMode = AppSettings.volumeNormalization.value
                if (nightMode || normMode) {
                    val cfg = DynamicsProcessing.Config.Builder(
                        DynamicsProcessing.VARIANT_FAVOR_FREQUENCY_RESOLUTION,
                        2, true, 0, true, 1, false, 0, true
                    ).build()
                    val dp = dynamicsProcessing ?: DynamicsProcessing(0, sessionId, cfg).also { dynamicsProcessing = it }
                    val limiter = if (nightMode) {
                        DynamicsProcessing.Limiter(true, true, 0, 10f, 60f, 4f, -4f, 4f)
                    } else {
                        DynamicsProcessing.Limiter(true, true, 0, 18f, 160f, 2.2f, -10f, 3f)
                    }
                    dp.setLimiterAllChannelsTo(limiter)
                    dp.enabled = true
                } else {
                    dynamicsProcessing?.enabled = false
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun applyRefreshRateHint(fps: Float, enabled: Boolean) {
        val surface = if (activeEngineMode == PlaybackEngineMode.MPV) {
            mpvSurfaceView?.holder?.surface
        } else {
            (playerView?.videoSurfaceView as? SurfaceView)?.holder?.surface
        } ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            try {
                if (enabled && fps > 1f) {
                    surface.setFrameRate(fps, Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE)
                } else {
                    surface.setFrameRate(0f, Surface.FRAME_RATE_COMPATIBILITY_DEFAULT)
                }
            } catch (_: Throwable) {
            }
        }
    }

    private fun captureLosslessScreenshot() {
        val includeSubs = AppSettings.includeSubtitlesInScreenshot.value
        if (activeEngineMode == PlaybackEngineMode.MPV) {
            if (!includeSubs) {
                val rawBmp = MpvEngineController.grabFrameBitmap()
                if (rawBmp != null) {
                    saveBitmapToGallery(rawBmp)
                    return
                }
            }
            val sv = mpvSurfaceView
            if (sv != null && sv.holder.surface.isValid && sv.width > 0 && sv.height > 0 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val bitmap = Bitmap.createBitmap(sv.width, sv.height, Bitmap.Config.ARGB_8888)
                PixelCopy.request(
                    sv,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            saveBitmapToGallery(bitmap)
                        } else {
                            val fallbackBmp = MpvEngineController.grabFrameBitmap()
                            if (fallbackBmp != null) saveBitmapToGallery(fallbackBmp)
                            else Toast.makeText(this, "Frame capture failed (code $copyResult)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            } else {
                val bmp = MpvEngineController.grabFrameBitmap()
                if (bmp != null) saveBitmapToGallery(bmp)
                else Toast.makeText(this, "Video surface is not ready", Toast.LENGTH_SHORT).show()
            }
            return
        }

        val pv = playerView ?: return
        val surfaceView = pv.videoSurfaceView as? SurfaceView
        val textureView = pv.videoSurfaceView as? TextureView

        val compositeSubtitlesIfRequested: (Bitmap) -> Unit = { bmp ->
            if (includeSubs) {
                pv.subtitleView?.takeIf { it.visibility == View.VISIBLE && it.width > 0 && it.height > 0 }?.let { sub ->
                    val canvas = android.graphics.Canvas(bmp)
                    val sx = bmp.width.toFloat() / sub.width.toFloat().coerceAtLeast(1f)
                    val sy = bmp.height.toFloat() / sub.height.toFloat().coerceAtLeast(1f)
                    canvas.scale(sx, sy)
                    sub.draw(canvas)
                }
            }
            saveBitmapToGallery(bmp)
        }

        if (surfaceView != null && surfaceView.holder.surface.isValid && surfaceView.width > 0 && surfaceView.height > 0) {
            val bitmap = Bitmap.createBitmap(surfaceView.width, surfaceView.height, Bitmap.Config.ARGB_8888)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                PixelCopy.request(
                    surfaceView,
                    bitmap,
                    { copyResult ->
                        if (copyResult == PixelCopy.SUCCESS) {
                            compositeSubtitlesIfRequested(bitmap)
                        } else {
                            Toast.makeText(this, "Frame capture failed (code $copyResult)", Toast.LENGTH_SHORT).show()
                        }
                    },
                    Handler(Looper.getMainLooper())
                )
            }
        } else if (textureView != null && textureView.isAvailable) {
            val bitmap = textureView.getBitmap()
            if (bitmap != null) {
                compositeSubtitlesIfRequested(bitmap)
            } else {
                Toast.makeText(this, "Unable to read video frame", Toast.LENGTH_SHORT).show()
            }
        } else {
            Toast.makeText(this, "Video surface is not ready", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBitmapToGallery(bitmap: Bitmap) {
        val nowDate = Date()
        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(nowDate)
        val dateText = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(nowDate)
        val formatPref = AppSettings.screenshotFormat.value
        val quality = if (formatPref == "PNG") {
            ((9 - AppSettings.pngCompressionLevel.value.coerceIn(0, 9)) * 10 + 10).coerceIn(10, 100)
        } else {
            AppSettings.screenshotQuality.value.coerceIn(60, 100)
        }
        val folderName = AppSettings.screenshotFolder.value
            .replace("/", "")
            .replace("..", "")
            .trim()
            .ifBlank { "MPVX Screenshots" }
        val (ext, mime, compressFmt) = when (formatPref) {
            "JPEG" -> Triple("jpg", "image/jpeg", Bitmap.CompressFormat.JPEG)
            "WEBP" -> Triple(
                "webp",
                "image/webp",
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Bitmap.CompressFormat.WEBP_LOSSY else @Suppress("DEPRECATION") Bitmap.CompressFormat.WEBP
            )
            else -> Triple("png", "image/png", Bitmap.CompressFormat.PNG)
        }
        val fileName = ScreenshotTemplateFormatter.formatFilename(
            template = AppSettings.screenshotFilenameTemplate.value,
            videoTitle = videoTitleState,
            timestampText = timestamp,
            dateText = dateText,
            extension = ext
        )
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                val values = ContentValues().apply {
                    put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                    put(MediaStore.Images.Media.MIME_TYPE, mime)
                    put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$folderName")
                    put(MediaStore.Images.Media.IS_PENDING, 1)
                }
                val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                    ?: throw IllegalStateException("MediaStore insert returned null")
                contentResolver.openOutputStream(uri)?.use { out ->
                    bitmap.compress(compressFmt, quality, out)
                }
                values.clear()
                values.put(MediaStore.Images.Media.IS_PENDING, 0)
                contentResolver.update(uri, values, null, null)
            } else {
                val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), folderName)
                dir.mkdirs()
                FileOutputStream(File(dir, fileName)).use { out ->
                    bitmap.compress(compressFmt, quality, out)
                }
            }
            screenshotSuccessText = "Saved: $fileName"
            screenshotEventCounter++
        } catch (e: Exception) {
            Toast.makeText(this, "Save failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun applyOrientationPreference(mode: String) {
        requestedOrientation = when (mode) {
            "Landscape" -> ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            "Reverse Landscape" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE
            "Sensor Landscape" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            "Portrait" -> ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            "Reverse Portrait" -> ActivityInfo.SCREEN_ORIENTATION_REVERSE_PORTRAIT
            "Sensor Portrait" -> ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
            "Auto", "Auto-Rotate", "Match Video" -> {
                when {
                    !hasResolvedVideoSize || videoWidthPx <= 0 || videoHeightPx <= 0 ->
                        ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    videoHeightPx > videoWidthPx ->
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                    videoWidthPx > videoHeightPx ->
                        ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                    else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                }
            }
            else -> ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    private fun updatePipAspectParams() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            try {
                setPictureInPictureParams(buildPipParams())
            } catch (_: Throwable) {
            }
        }
    }

    private fun buildPipParams(): PictureInPictureParams {
        val rawAspect = if (videoHeightPx > 0) videoWidthPx.toFloat() / videoHeightPx.toFloat() else (16f / 9f)
        // Android mandates PiP ratio strictly between 1/2.39 and 2.39/1
        val clamped = rawAspect.coerceIn(0.42f, 2.38f)
        val num = (clamped * 1000).roundToInt()
        val builder = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(num, 1000))
            .setActions(buildPipRemoteActions())

        val activeSurfaceView = if (activeEngineMode == PlaybackEngineMode.MPV) mpvSurfaceView else playerView?.videoSurfaceView
        activeSurfaceView?.let { surface ->
            if (surface.isAttachedToWindow && surface.width > 0 && surface.height > 0) {
                val rect = Rect()
                surface.getGlobalVisibleRect(rect)
                if (!rect.isEmpty) {
                    builder.setSourceRectHint(rect)
                }
            }
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            builder.setAutoEnterEnabled(AppSettings.autoPiP.value && isPlayingState)
        }
        return builder.build()
    }

    private fun buildPipRemoteActions(): List<RemoteAction> {
        val stepFlags = PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        val replayIntent = PendingIntent.getBroadcast(
            this, 201, Intent(ACTION_PIP_REPLAY).setPackage(packageName), stepFlags
        )
        val playPauseIntent = PendingIntent.getBroadcast(
            this, 202, Intent(ACTION_PIP_PLAY_PAUSE).setPackage(packageName), stepFlags
        )
        val forwardIntent = PendingIntent.getBroadcast(
            this, 203, Intent(ACTION_PIP_FORWARD).setPackage(packageName), stepFlags
        )
        val replayAction = RemoteAction(
            Icon.createWithResource(this, android.R.drawable.ic_media_rew),
            "Rewind",
            "Rewind",
            replayIntent
        )
        val playPauseAction = RemoteAction(
            Icon.createWithResource(
                this,
                if (isPlayingState) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
            ),
            if (isPlayingState) "Pause" else "Play",
            if (isPlayingState) "Pause" else "Play",
            playPauseIntent
        )
        val forwardAction = RemoteAction(
            Icon.createWithResource(this, android.R.drawable.ic_media_ff),
            "Forward",
            "Forward",
            forwardIntent
        )
        return listOf(replayAction, playPauseAction, forwardAction)
    }

    private fun enterPiPMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            try {
                isInPipState = true
                enterPictureInPictureMode(buildPipParams())
            } catch (_: Throwable) {
                isInPipState = false
            }
        }
    }

    override fun onStart() {
        super.onStart()
        MediaPlaybackService.detachVideoPlayer(this, activeVideoPlayerForService())
    }

    override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean, newConfig: Configuration) {
        super.onPictureInPictureModeChanged(isInPictureInPictureMode, newConfig)
        isInPipState = isInPictureInPictureMode
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (AppSettings.autoPiP.value && isPlayingState) {
            enterPiPMode()
        }
    }

    override fun onPause() {
        super.onPause()
        persistCurrentPlayback()
        val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode
        if (!inPip && !AppSettings.backgroundPlayback.value) {
            if (activeEngineMode == PlaybackEngineMode.MPV) {
                MpvEngineController.pause()
            } else {
                exoPlayer?.pause()
            }
        }
    }

    override fun onStop() {
        super.onStop()
        val servicePlayer = activeVideoPlayerForService()
        val inPip = Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && isInPictureInPictureMode
        if (!inPip && !isChangingConfigurations && !isFinishing &&
            AppSettings.backgroundPlayback.value && servicePlayer != null && isPlayingState
        ) {
            MediaPlaybackService.attachVideoPlayer(this, servicePlayer)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        persistCurrentPlayback()
        parseAndLoadIntent(intent, rebuildPlayer = true)
    }

    private fun persistCurrentPlayback() {
        val uri = currentUriState ?: return
        val pos: Long
        val dur: Long
        if (activeEngineMode == PlaybackEngineMode.MPV) {
            val snap = MpvEngineController.snapshot.value
            pos = snap.positionMs.coerceAtLeast(0L)
            dur = snap.durationMs.coerceAtLeast(0L)
        } else {
            val p = exoPlayer ?: return
            pos = p.currentPosition.coerceAtLeast(0L)
            dur = p.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: 0L
        }
        if (pos > 0L || dur > 0L) {
            PlaybackHistoryBridge.recordPlayback(
                context = this,
                uri = uri,
                title = videoTitleState,
                mimeType = "video/*",
                positionMs = pos,
                durationMs = dur,
                rememberPosition = AppSettings.rememberPosition.value
            )
        }
    }

    private fun releaseEffectsAndPlayer() {
        try { assHandlerRef?.release() } catch (_: Throwable) {}
        assHandlerRef = null
        try { dynamicsProcessing?.release() } catch (_: Throwable) {}
        dynamicsProcessing = null
        try { equalizer?.release() } catch (_: Throwable) {}
        equalizer = null
        try { loudnessEnhancer?.release() } catch (_: Throwable) {}
        loudnessEnhancer = null
        try { exoPlayer?.release() } catch (_: Throwable) {}
        exoPlayer = null
    }

    override fun onDestroy() {
        runCatching { unregisterReceiver(pipActionReceiver) }
        runCatching { unregisterReceiver(screenLockReceiver) }
        MediaPlaybackService.detachVideoPlayer(this, activeVideoPlayerForService())
        persistCurrentPlayback()
        releaseEffectsAndPlayer()
        if (isFinishing) {
            MpvEngineController.releaseEngine()
            loadedMpvInitial = false
        }
        mpvMedia3Adapter?.release()
        mpvMedia3Adapter = null
        mpvSurfaceView = null
        playerView = null
        super.onDestroy()
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUI() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (AppSettings.showSystemStatusBar.value) {
            controller.show(WindowInsetsCompat.Type.statusBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.statusBars())
        }
        if (AppSettings.showSystemNavBar.value) {
            controller.show(WindowInsetsCompat.Type.navigationBars())
        } else {
            controller.hide(WindowInsetsCompat.Type.navigationBars())
        }
    }
}
