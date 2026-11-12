package com.cmstudio.player.engine

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import android.view.SurfaceHolder
import android.view.SurfaceView
import com.cmstudio.player.Utils
import com.cmstudio.player.diagnostics.PlaybackDiagnosticsLog
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.player.PlayerTrackOption
import com.cmstudio.player.ui.player.VideoChapterItem
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.math.ln
import kotlin.math.roundToInt
import kotlin.math.roundToLong

data class MpvRuntimeSnapshot(
    val initialized: Boolean = false,
    val isPlaying: Boolean = false,
    val isBuffering: Boolean = false,
    val isFileLoaded: Boolean = false,
    val isEnded: Boolean = false,
    val positionMs: Long = 0L,
    val durationMs: Long = 0L,
    val bufferedMs: Long = 0L,
    val videoWidth: Int = 0,
    val videoHeight: Int = 0,
    val detectedFps: Float = 24f,
    val bitrateBps: Long = 0L,
    val codecName: String = "MPV",
    val hwdecCurrent: String = "",
    val voName: String = "gpu",
    val hdrLabel: String = "SDR",
    val channelLayout: String = "Stereo",
    val chapters: List<VideoChapterItem> = emptyList(),
    val currentChapterIndex: Int = -1,
    val videoTracks: List<PlayerTrackOption> = emptyList(),
    val audioTracks: List<PlayerTrackOption> = emptyList(),
    val subtitleTracks: List<PlayerTrackOption> = emptyList(),
    val lastError: String? = null
)

/**
 * Clean-room process-safe `libmpv` playback engine controller backed by `is.xyz.mpv.MPVLib`.
 * Manages native initialization, Vulkan -> OpenGL fallback, surface attach/detach without tearing down
 * playback on rotation, FD URI resolution (`fd://`), track selection, dual subtitles, shaders, and audio DSP.
 */
object MpvEngineController : MPVLib.EventObserver {

    private const val TAG = "MpvEngineController"
    private val nativeLock = ReentrantLock(true)
    private val mainHandler = Handler(Looper.getMainLooper())

    private val _snapshot = MutableStateFlow(MpvRuntimeSnapshot())
    val snapshot: StateFlow<MpvRuntimeSnapshot> = _snapshot.asStateFlow()

    @Volatile
    private var appContext: Context? = null
    @Volatile
    private var isInitialized = false
    @Volatile
    private var observerRegistered = false
    @Volatile
    private var activeSurface: Surface? = null
    @Volatile
    private var activeRenderConfig = EngineRenderConfig()
    @Volatile
    private var activeVoInUse = "gpu"
    @Volatile
    private var pendingStartSeconds: Double? = null
    @Volatile
    private var pendingAutoPlay: Boolean = true
    @Volatile
    private var pendingInitialSubtitles: List<String> = emptyList()
    @Volatile
    private var endCallback: (() -> Unit)? = null

    fun setOnPlaybackEndedListener(listener: (() -> Unit)?) {
        endCallback = listener
    }

    fun ensureInitialized(context: Context, renderConfig: EngineRenderConfig): Boolean {
        val ctx = context.applicationContext
        appContext = ctx
        activeRenderConfig = renderConfig
        return nativeLock.withLock {
            if (isInitialized) {
                true
            } else {
                initLocked(ctx, renderConfig)
            }
        }
    }

    private fun initLocked(ctx: Context, config: EngineRenderConfig): Boolean {
        val (configDir, cacheDir, shaderDir) = prepareMpvDirectories(ctx)
        val (vo, gpuApi, gpuContext) = MpvBackendOptions.resolveVideoBackend(config)
        activeVoInUse = vo

        return try {
            MPVLib.create(ctx)
            MPVLib.setOptionString("config", "yes")
            MPVLib.setOptionString("config-dir", configDir.absolutePath)
            MPVLib.setOptionString("gpu-shader-cache-dir", cacheDir.absolutePath)
            MPVLib.setOptionString("icc-cache-dir", cacheDir.absolutePath)
            MPVLib.setOptionString("keep-open", "yes")
            MPVLib.setOptionString("osd-level", "0")
            MPVLib.setOptionString("osd-bar", "no")
            MPVLib.setOptionString("osd-on-seek", "no")
            MPVLib.setOptionString("input-default-bindings", "no")
            MPVLib.setOptionString("osc", "no")
            MPVLib.setOptionString("ytdl", "no")
            MPVLib.setOptionString("profile", config.mpvProfile.ifBlank { "fast" })
            MPVLib.setOptionString("vo", vo)
            MPVLib.setOptionString("gpu-api", gpuApi)
            MPVLib.setOptionString("gpu-context", gpuContext)
            MPVLib.setOptionString("hwdec", MpvBackendOptions.preferredHwdecMode(config.decoderPriority))
            if (config.useYuv420p) {
                MPVLib.setOptionString("vf", "format=yuv420p")
            }
            MPVLib.setOptionString("vd-lavc-threads", "0")
            MPVLib.setOptionString("framedrop", "decoder")
            MPVLib.setOptionString("video-sync", "audio")
            MPVLib.setOptionString("ao", "audiotrack")
            MPVLib.setOptionString("volume-max", "200")
            MPVLib.setOptionString("sub-auto", "fuzzy")
            MPVLib.setOptionString("sub-fix-timing", "yes")
            MPVLib.setOptionString("sub-ass-override", "scale")
            MPVLib.setOptionString("embeddedfonts", "yes")
            MPVLib.setOptionString("sub-fonts-dir", File(ctx.filesDir, "fonts").apply { mkdirs() }.absolutePath)
            val caFile = File(configDir, "cacert.pem")
            if (caFile.isFile) {
                MPVLib.setOptionString("tls-verify", "yes")
                MPVLib.setOptionString("tls-ca-file", caFile.absolutePath)
            }
            MPVLib.setOptionString("network-timeout", "20")
            MPVLib.setOptionString("cache", "auto")
            MPVLib.setOptionString("cache-on-disk", "no")
            MPVLib.setOptionString("demuxer-max-bytes", "256MiB")
            MPVLib.setOptionString("demuxer-max-back-bytes", "64MiB")

            if (AppSettings.mpvConfEnabled.value) {
                val parsed = MpvConfManager.validateAndParse(AppSettings.mpvConfText.value)
                parsed.validEntries.forEach { entry ->
                    runCatching { MPVLib.setOptionString(entry.key, entry.value) }
                }
            }

            MPVLib.init()

            MPVLib.setPropertyString("force-window", "no")
            MPVLib.setPropertyString("idle", "yes")
            if (!observerRegistered) {
                MPVLib.addObserver(this)
                observerRegistered = true
            }
            observeCoreProperties()
            isInitialized = true
            _snapshot.value = _snapshot.value.copy(
                initialized = true,
                voName = "$vo/$gpuApi",
                lastError = null
            )
            applyPostInitRenderOptionsLocked(config, shaderDir)
            PlaybackDiagnosticsLog.info(
                engine = "MPV",
                category = "INIT",
                message = "Initialized libmpv ($vo/$gpuApi, hwdec=${MpvBackendOptions.preferredHwdecMode(config.decoderPriority)})"
            )
            true
        } catch (t: Throwable) {
            Log.e(TAG, "MPV init failed (vulkan=${config.useVulkan}): ${t.message}", t)
            PlaybackDiagnosticsLog.warn(
                engine = "MPV",
                category = "INIT",
                message = "MPV init failed (vulkan=${config.useVulkan}): ${t.localizedMessage ?: "unknown"}"
            )
            runCatching { MPVLib.destroy() }
            isInitialized = false
            // Automatic Vulkan -> OpenGL fallback
            if (config.useVulkan && !config.forceOpenGlFallback) {
                val glFallback = config.copy(useVulkan = false, forceOpenGlFallback = true)
                activeRenderConfig = glFallback
                return initLocked(ctx, glFallback)
            }
            _snapshot.value = _snapshot.value.copy(
                initialized = false,
                lastError = "MPV engine failed to start: ${t.localizedMessage ?: "unknown error"}"
            )
            false
        }
    }

    private fun observeCoreProperties() {
        val boolProps = listOf("pause", "paused-for-cache", "eof-reached")
        val longProps = listOf("video-params/w", "video-params/h", "chapter", "aid", "sid", "secondary-sid")
        val doubleProps = listOf("time-pos", "duration", "demuxer-cache-duration", "container-fps", "video-bitrate")
        val strProps = listOf("video-codec", "hwdec-current", "video-params/gamma", "video-params/primaries")
        val nodeProps = listOf("track-list", "chapter-list")

        boolProps.forEach { MPVLib.observeProperty(it, MPVLib.MpvFormat.MPV_FORMAT_FLAG) }
        longProps.forEach { MPVLib.observeProperty(it, MPVLib.MpvFormat.MPV_FORMAT_INT64) }
        doubleProps.forEach { MPVLib.observeProperty(it, MPVLib.MpvFormat.MPV_FORMAT_DOUBLE) }
        strProps.forEach { MPVLib.observeProperty(it, MPVLib.MpvFormat.MPV_FORMAT_STRING) }
        nodeProps.forEach { MPVLib.observeProperty(it, MPVLib.MpvFormat.MPV_FORMAT_NODE) }
    }

    private fun prepareMpvDirectories(ctx: Context): Triple<File, File, File> {
        val configDir = File(ctx.filesDir, "mpv").apply { mkdirs() }
        val cacheDir = File(ctx.cacheDir, "mpv_cache").apply { mkdirs() }
        val shaderDir = File(configDir, "shaders/anime4k").apply { mkdirs() }

        copyAssetIfMissing(ctx, "cacert.pem", File(configDir, "cacert.pem"))
        copyAssetIfMissing(ctx, "subfont.ttf", File(configDir, "subfont.ttf"))
        copyAssetDirectory(ctx, "shaders/anime4k", shaderDir)
        return Triple(configDir, cacheDir, shaderDir)
    }

    private fun copyAssetIfMissing(ctx: Context, assetPath: String, dest: File) {
        if (dest.isFile && dest.length() > 0L) return
        try {
            ctx.assets.open(assetPath).use { input ->
                FileOutputStream(dest).use { output -> input.copyTo(output) }
            }
        } catch (_: Throwable) {
        }
    }

    private fun copyAssetDirectory(ctx: Context, assetDir: String, destDir: File) {
        try {
            val entries = ctx.assets.list(assetDir) ?: return
            for (name in entries) {
                copyAssetIfMissing(ctx, "$assetDir/$name", File(destDir, name))
            }
        } catch (_: Throwable) {
        }
    }

    fun attachSurface(surface: Surface, width: Int, height: Int) {
        nativeLock.withLock {
            activeSurface = surface
            if (!isInitialized) return
            MPVLib.attachSurface(surface)
            if (width > 0 && height > 0) {
                MPVLib.setPropertyString("android-surface-size", "${width}x${height}")
            }
            MPVLib.setPropertyString("vo", activeVoInUse)
            MPVLib.setPropertyString("force-window", "yes")
        }
    }

    fun resizeSurface(width: Int, height: Int) {
        if (width <= 0 || height <= 0) return
        nativeLock.withLock {
            if (!isInitialized) return
            MPVLib.setPropertyString("android-surface-size", "${width}x${height}")
        }
    }

    fun detachSurface() {
        nativeLock.withLock {
            activeSurface = null
            if (!isInitialized) return
            runCatching {
                MPVLib.setPropertyString("vo", "null")
                MPVLib.setPropertyString("force-window", "no")
                MPVLib.detachSurface()
            }
        }
    }

    fun loadMedia(
        context: Context,
        uri: Uri,
        startPositionMs: Long = 0L,
        headers: Map<String, String> = emptyMap(),
        speed: Float = 1.0f,
        pitchCorrection: Boolean = true,
        fastSeek: Boolean = false,
        repeatMode: String = "OFF",
        initialSubUris: List<Uri> = emptyList(),
        autoPlay: Boolean = true
    ) {
        val ctx = context.applicationContext
        val playable = Utils.resolvePlayableUriForMpv(ctx, uri) ?: uri.toString()
        nativeLock.withLock {
            if (!isInitialized && !ensureInitialized(ctx, activeRenderConfig)) return
            val headerField = MpvBackendOptions.buildHttpHeaderOption(headers)
            if (headerField.isNotEmpty()) {
                MPVLib.setOptionString("http-header-fields", headerField)
            } else {
                MPVLib.setOptionString("http-header-fields", "")
            }
            MPVLib.setPropertyBoolean("audio-pitch-correction", pitchCorrection)
            MPVLib.setPropertyDouble("speed", speed.toDouble().coerceIn(0.25, 4.0))
            MPVLib.setOptionString("hr-seek", if (fastSeek) "no" else "yes")
            MPVLib.setOptionString("loop-file", if (repeatMode == "ONE") "inf" else "no")

            pendingAutoPlay = autoPlay
            pendingStartSeconds = if (startPositionMs > 1200L) startPositionMs / 1000.0 else null
            pendingInitialSubtitles = initialSubUris.mapNotNull { Utils.resolvePlayableUriForMpv(ctx, it) ?: it.toString() }

            _snapshot.value = _snapshot.value.copy(
                isFileLoaded = false,
                isEnded = false,
                positionMs = startPositionMs.coerceAtLeast(0L),
                lastError = null
            )
            MPVLib.setPropertyBoolean("pause", !autoPlay)
            MPVLib.command("loadfile", playable, "replace")
        }
    }

    fun play() = setPaused(false)
    fun pause() = setPaused(true)
    fun togglePlayPause() {
        val pausedNow = !_snapshot.value.isPlaying
        setPaused(!pausedNow)
    }

    fun setPaused(paused: Boolean) {
        pendingAutoPlay = !paused
        nativeLock.withLock {
            if (!isInitialized) return
            MPVLib.setPropertyBoolean("pause", paused)
        }
    }

    fun seekTo(positionMs: Long) {
        val sec = (positionMs.coerceAtLeast(0L)) / 1000.0
        nativeLock.withLock {
            if (!isInitialized) return
            MPVLib.command("seek", String.format(Locale.US, "%.3f", sec), "absolute")
        }
        _snapshot.value = _snapshot.value.copy(positionMs = positionMs.coerceAtLeast(0L))
    }

    fun stepFrame(forward: Boolean) {
        nativeLock.withLock {
            if (!isInitialized) return
            MPVLib.setPropertyBoolean("pause", true)
            MPVLib.command(if (forward) "frame-step" else "frame-back-step")
        }
    }

    fun setSpeed(speed: Float, pitchCorrection: Boolean) {
        nativeLock.withLock {
            if (!isInitialized) return
            MPVLib.setPropertyBoolean("audio-pitch-correction", pitchCorrection)
            MPVLib.setPropertyDouble("speed", speed.toDouble().coerceIn(0.25, 4.0))
        }
    }

    fun setRepeatMode(mode: String) {
        nativeLock.withLock {
            if (!isInitialized) return
            MPVLib.setOptionString("loop-file", if (mode == "ONE") "inf" else "no")
        }
    }

    fun setFastSeek(fast: Boolean) {
        nativeLock.withLock {
            if (!isInitialized) return
            MPVLib.setOptionString("hr-seek", if (fast) "no" else "yes")
        }
    }

    fun setAudioDelayMs(delayMs: Int) {
        nativeLock.withLock {
            if (!isInitialized) return
            MPVLib.setPropertyDouble("audio-delay", delayMs / 1000.0)
        }
    }

    fun setSubtitleDelayMs(delayMs: Int) {
        nativeLock.withLock {
            if (!isInitialized) return
            MPVLib.setPropertyDouble("sub-delay", delayMs / 1000.0)
        }
    }

    fun setSubtitleSpeedMilli(speedMilli: Int) {
        nativeLock.withLock {
            if (!isInitialized) return
            val speed = (speedMilli / 1000.0).coerceIn(0.1, 10.0)
            MPVLib.setPropertyDouble("sub-speed", speed)
        }
    }

    fun setSubtitleFontsDir(path: String) {
        nativeLock.withLock {
            if (!isInitialized || path.isBlank()) return
            MPVLib.setPropertyString("sub-fonts-dir", path)
        }
    }

    fun selectAudioTrack(mpvTrackId: Int) {
        nativeLock.withLock {
            if (!isInitialized) return
            MPVLib.setPropertyInt("aid", mpvTrackId)
        }
        refreshTrackLists()
    }

    fun selectSubtitleTrack(mpvTrackId: Int) {
        nativeLock.withLock {
            if (!isInitialized) return
            MPVLib.setPropertyString("sub-visibility", "yes")
            MPVLib.setPropertyInt("sid", mpvTrackId)
        }
        refreshTrackLists()
    }

    fun selectSecondarySubtitleTrack(mpvTrackId: Int?) {
        nativeLock.withLock {
            if (!isInitialized) return
            if (mpvTrackId == null || mpvTrackId <= 0) {
                MPVLib.setPropertyString("secondary-sid", "no")
            } else {
                MPVLib.setPropertyInt("secondary-sid", mpvTrackId)
            }
        }
        refreshTrackLists()
    }

    fun disableSubtitles() {
        nativeLock.withLock {
            if (!isInitialized) return
            MPVLib.setPropertyString("sid", "no")
            MPVLib.setPropertyString("secondary-sid", "no")
        }
        refreshTrackLists()
    }

    fun addExternalSubtitle(context: Context, uri: Uri): Boolean {
        val path = Utils.resolvePlayableUriForMpv(context, uri) ?: return false
        nativeLock.withLock {
            if (!isInitialized) return false
            MPVLib.setPropertyString("sub-visibility", "yes")
            MPVLib.command("sub-add", path, "select")
        }
        mainHandler.postDelayed({ refreshTrackLists() }, 250L)
        return true
    }

    fun applyAudioEffects(
        boostPct: Int,
        eqBandsDb: List<Float>,
        channelMode: String,
        nightCompressor: Boolean,
        normalizeVolume: Boolean = false,
        boostCapPct: Int = 200
    ) {
        nativeLock.withLock {
            if (!isInitialized) return
            val clampedCap = boostCapPct.coerceIn(100, 200)
            val vol = boostPct.coerceIn(0, clampedCap).toDouble()
            MPVLib.setPropertyDouble("volume", vol)
            val chain = MpvBackendOptions.buildAudioFilterChain(eqBandsDb, channelMode, nightCompressor, normalizeVolume)
            MPVLib.setPropertyString("af", chain)
        }
    }

    fun applySubtitleStyle(
        fontSizeSp: Float,
        colorArgb: Long,
        bgArgb: Long,
        bgOpacity: Float,
        bottomPaddingDp: Int,
        bold: Boolean,
        italic: Boolean,
        alignment: String,
        overrideAssMode: String = "SCALE",
        scaleByWindow: Boolean = true
    ) {
        nativeLock.withLock {
            if (!isInitialized) return
            val scale = (fontSizeSp / 22f).coerceIn(0.4f, 3.0f).toDouble()
            MPVLib.setPropertyDouble("sub-scale", scale)
            MPVLib.setPropertyString("sub-scale-by-window", if (scaleByWindow) "yes" else "no")
            val normalizedAss = when (overrideAssMode.trim().uppercase(Locale.ROOT)) {
                "NO", "YES", "FORCE", "STRIP" -> overrideAssMode.trim().lowercase(Locale.ROOT)
                else -> "scale"
            }
            MPVLib.setPropertyString("sub-ass-override", normalizedAss)
            MPVLib.setPropertyInt("sub-margin-y", (bottomPaddingDp * 1.6f).roundToInt().coerceIn(8, 220))
            MPVLib.setPropertyBoolean("sub-bold", bold)
            MPVLib.setPropertyBoolean("sub-italic", italic)
            MPVLib.setPropertyString("sub-align-x", when (alignment) {
                "LEFT" -> "left"
                "RIGHT" -> "right"
                else -> "center"
            })
            val fg = String.format(Locale.US, "#FF%06X", (colorArgb.toInt() and 0x00FFFFFF))
            val alphaHex = ((bgOpacity.coerceIn(0f, 1f) * 255f).roundToInt() and 0xFF)
            val bg = String.format(Locale.US, "#%02X%06X", alphaHex, (bgArgb.toInt() and 0x00FFFFFF))
            MPVLib.setPropertyString("sub-color", fg)
            MPVLib.setPropertyString("sub-back-color", bg)
        }
    }

    fun applyVideoGeometry(
        aspectMode: String,
        zoomScale: Float,
        panNormX: Float,
        panNormY: Float,
        mirrored: Boolean,
        flippedVertical: Boolean
    ) {
        nativeLock.withLock {
            if (!isInitialized) return
            val ratioStr = when (aspectMode) {
                "Fill", "Crop", "Zoom" -> "-1"
                "Stretch" -> "-1"
                "16:9" -> "1.7778"
                "4:3" -> "1.3333"
                "21:9" -> "2.3333"
                "16:10" -> "1.6"
                "2.35:1" -> "2.35"
                "2.39:1" -> "2.39"
                "2:1" -> "2.0"
                "5:4" -> "1.25"
                else -> "-1"
            }
            MPVLib.setPropertyString("video-aspect-override", ratioStr)
            val panScan = if (aspectMode in setOf("Fill", "Crop", "Zoom")) 1.0 else 0.0
            MPVLib.setPropertyDouble("panscan", panScan)
            // MPV video-zoom is base-2 log scale (`2^video-zoom == linear scale`)
            val mpvZoom = if (zoomScale > 0.01f) ln(zoomScale.toDouble()) / ln(2.0) else 0.0
            MPVLib.setPropertyDouble("video-zoom", mpvZoom.coerceIn(-1.5, 2.5))
            MPVLib.setPropertyDouble("video-pan-x", panNormX.toDouble().coerceIn(-0.8, 0.8))
            MPVLib.setPropertyDouble("video-pan-y", panNormY.toDouble().coerceIn(-0.8, 0.8))

            val filters = mutableListOf<String>()
            if (activeRenderConfig.useYuv420p) filters += "format=yuv420p"
            if (mirrored) filters += "hflip"
            if (flippedVertical) filters += "vflip"
            MPVLib.setPropertyString("vf", filters.joinToString(","))
        }
    }

    fun needsReinitForRenderConfig(config: EngineRenderConfig): Boolean {
        if (!isInitialized) return false
        return MpvBackendOptions.resolveVideoBackend(activeRenderConfig) !=
            MpvBackendOptions.resolveVideoBackend(config) ||
            activeRenderConfig.mpvProfile != config.mpvProfile
    }

    fun applyRenderAndShaderConfig(context: Context, config: EngineRenderConfig) {
        activeRenderConfig = config
        nativeLock.withLock {
            if (!isInitialized) return
            val shaderDir = File(context.applicationContext.filesDir, "mpv/shaders/anime4k")
            applyPostInitRenderOptionsLocked(config, shaderDir)
        }
    }

    fun grabFrameBitmap(): android.graphics.Bitmap? {
        return nativeLock.withLock {
            if (!isInitialized) return@withLock null
            runCatching { MPVLib.grabThumbnail(1080) }.getOrNull()
        }
    }

    fun captureScreenshotToFile(outFile: File, includeSubtitles: Boolean): Boolean {
        return nativeLock.withLock {
            if (!isInitialized) return@withLock false
            runCatching {
                MPVLib.command(
                    "screenshot-to-file",
                    outFile.absolutePath,
                    if (includeSubtitles) "subtitles" else "video"
                )
                true
            }.getOrDefault(false)
        }
    }

    private fun applyPostInitRenderOptionsLocked(config: EngineRenderConfig, shaderDir: File) {
        MPVLib.setPropertyString("hwdec", MpvBackendOptions.preferredHwdecMode(config.decoderPriority))
        if (config.hdrTonemapping) {
            MPVLib.setPropertyString("tone-mapping", "bt.2446a")
            MPVLib.setPropertyString("target-colorspace-hint", "auto")
        } else {
            MPVLib.setPropertyString("tone-mapping", "clip")
            MPVLib.setPropertyString("target-colorspace-hint", "no")
        }
        when (config.debanding.uppercase(Locale.ROOT)) {
            "GPU", "CPU" -> {
                MPVLib.setPropertyString("deband", "yes")
                MPVLib.setPropertyInt("deband-iterations", config.debandIterations.coerceIn(1, 4))
                MPVLib.setPropertyInt("deband-threshold", config.debandThreshold.coerceIn(0, 256))
                MPVLib.setPropertyInt("deband-range", config.debandRange.coerceIn(1, 64))
                MPVLib.setPropertyInt("deband-grain", config.debandGrain.coerceIn(0, 256))
            }
            else -> MPVLib.setPropertyString("deband", "no")
        }

        val shaderFiles = MpvBackendOptions.resolveAnime4kShaderNames(
            enabled = config.anime4kEnabled,
            mode = config.anime4kMode,
            quality = config.anime4kQuality
        ).mapNotNull { name ->
            val file = File(shaderDir, name)
            if (file.isFile) file.absolutePath else null
        }
        if (shaderFiles.isEmpty()) {
            MPVLib.command("change-list", "glsl-shaders", "clr", "")
        } else {
            MPVLib.command("change-list", "glsl-shaders", "set", shaderFiles.joinToString(":"))
        }

        if (AppSettings.mpvConfEnabled.value) {
            val parsed = MpvConfManager.validateAndParse(AppSettings.mpvConfText.value)
            var appliedCount = 0
            parsed.validEntries.forEach { entry ->
                runCatching {
                    MPVLib.setPropertyString(entry.key, entry.value)
                    appliedCount++
                }.onFailure { err ->
                    PlaybackDiagnosticsLog.warn(
                        engine = "MPV",
                        category = "MPV_CONF",
                        message = "Skipped runtime mpv.conf option '${entry.key}=${entry.value}': ${err.localizedMessage ?: "unsupported"}"
                    )
                }
            }
            if (appliedCount > 0) {
                PlaybackDiagnosticsLog.info(
                    engine = "MPV",
                    category = "MPV_CONF",
                    message = "Applied $appliedCount validated mpv.conf option(s)"
                )
            }
        }
    }

    fun releaseEngine() {
        nativeLock.withLock {
            if (!isInitialized) return
            runCatching {
                MPVLib.command("stop")
                MPVLib.setPropertyString("vo", "null")
                MPVLib.detachSurface()
                if (observerRegistered) {
                    MPVLib.removeObserver(this)
                    observerRegistered = false
                }
                MPVLib.destroy()
            }
            isInitialized = false
            activeSurface = null
            _snapshot.value = MpvRuntimeSnapshot(initialized = false)
        }
    }

    fun refreshTrackLists() {
        nativeLock.withLock {
            if (!isInitialized) return
            val node = runCatching { MPVLib.getPropertyNode("track-list") }.getOrNull() ?: return
            parseTracksNode(node)
        }
    }

    private fun parseTracksNode(node: MPVNode) {
        val arr = node.asArray() ?: return
        val vList = mutableListOf<PlayerTrackOption>()
        val aList = mutableListOf<PlayerTrackOption>()
        val sList = mutableListOf<PlayerTrackOption>()

        for (entry in arr) {
            val type = entry.get("type")?.asString().orEmpty()
            val id = entry.get("id")?.asInt()?.toInt() ?: continue
            val selected = entry.get("selected")?.asBoolean() ?: false
            val title = entry.get("title")?.asString()?.takeIf { it.isNotBlank() }
            val lang = entry.get("lang")?.asString()?.takeIf { it.isNotBlank() }
            val codec = entry.get("codec")?.asString()?.uppercase(Locale.US)
            val labelParts = listOfNotNull(
                title ?: lang?.let {
                    Locale.forLanguageTag(it.replace('_', '-')).getDisplayLanguage(Locale.ENGLISH).takeIf { d -> d.isNotBlank() }
                        ?: it.uppercase(Locale.US)
                } ?: "Track $id",
                if (title != null && lang != null) lang.uppercase(Locale.US) else null,
                codec
            )
            val option = PlayerTrackOption(
                groupIndex = 0,
                trackIndex = id,
                label = labelParts.joinToString(" • "),
                selected = selected
            )
            when (type) {
                "video" -> vList += option
                "audio" -> {
                    aList += option
                    if (selected) {
                        val ch = entry.get("demux-channel-count")?.asInt()?.toInt() ?: 0
                        val layout = when (ch) {
                            1 -> "Mono"
                            2 -> "Stereo"
                            6 -> "5.1"
                            8 -> "7.1"
                            else -> if (ch > 0) "${ch}ch" else "Stereo"
                        }
                        _snapshot.value = _snapshot.value.copy(channelLayout = layout)
                    }
                }
                "sub" -> sList += option
            }
        }
        _snapshot.value = _snapshot.value.copy(
            videoTracks = vList,
            audioTracks = aList,
            subtitleTracks = sList
        )
    }

    private fun parseChaptersNode(node: MPVNode) {
        val arr = node.asArray() ?: return
        val durMs = _snapshot.value.durationMs
        val starts = mutableListOf<Pair<String, Long>>()
        arr.forEachIndexed { idx, item ->
            val title = item.get("title")?.asString()?.takeIf { it.isNotBlank() } ?: "Chapter ${idx + 1}"
            val timeSec = item.get("time")?.asDouble() ?: 0.0
            starts += title to (timeSec * 1000.0).roundToLong().coerceAtLeast(0L)
        }
        val mapped = starts.mapIndexed { idx, (title, startMs) ->
            val endMs = starts.getOrNull(idx + 1)?.second ?: durMs
            VideoChapterItem(title = title, startMs = startMs, endMs = endMs.coerceAtLeast(startMs))
        }
        _snapshot.value = _snapshot.value.copy(chapters = mapped)
    }

    override fun eventProperty(property: String) {}

    override fun eventProperty(property: String, value: Long) {
        when (property) {
            "video-params/w" -> _snapshot.value = _snapshot.value.copy(videoWidth = value.toInt().coerceAtLeast(0))
            "video-params/h" -> _snapshot.value = _snapshot.value.copy(videoHeight = value.toInt().coerceAtLeast(0))
            "chapter" -> _snapshot.value = _snapshot.value.copy(currentChapterIndex = value.toInt())
            "aid", "sid", "secondary-sid" -> mainHandler.post { refreshTrackLists() }
        }
    }

    override fun eventProperty(property: String, value: Boolean) {
        when (property) {
            "pause" -> _snapshot.value = _snapshot.value.copy(isPlaying = !value)
            "paused-for-cache" -> _snapshot.value = _snapshot.value.copy(isBuffering = value)
            "eof-reached" -> if (value) {
                _snapshot.value = _snapshot.value.copy(isEnded = true, isPlaying = false)
                mainHandler.post { endCallback?.invoke() }
            }
        }
    }

    override fun eventProperty(property: String, value: String) {
        val snap = _snapshot.value
        when (property) {
            "video-codec" -> _snapshot.value = snap.copy(codecName = value.ifBlank { "MPV" }.uppercase(Locale.US))
            "hwdec-current" -> _snapshot.value = snap.copy(hwdecCurrent = value)
            "video-params/gamma" -> {
                val hdr = when (value.lowercase(Locale.ROOT)) {
                    "pq", "st2084" -> "HDR10"
                    "hlg" -> "HLG"
                    else -> if (snap.hdrLabel == "SDR") "SDR" else snap.hdrLabel
                }
                _snapshot.value = snap.copy(hdrLabel = hdr)
            }
        }
    }

    override fun eventProperty(property: String, value: Double) {
        when (property) {
            "time-pos" -> _snapshot.value = _snapshot.value.copy(positionMs = (value * 1000.0).roundToLong().coerceAtLeast(0L))
            "duration" -> _snapshot.value = _snapshot.value.copy(durationMs = (value * 1000.0).roundToLong().coerceAtLeast(0L))
            "demuxer-cache-duration" -> {
                val cur = _snapshot.value.positionMs
                _snapshot.value = _snapshot.value.copy(bufferedMs = cur + (value * 1000.0).roundToLong().coerceAtLeast(0L))
            }
            "container-fps" -> if (value > 1.0) _snapshot.value = _snapshot.value.copy(detectedFps = value.toFloat())
            "video-bitrate" -> _snapshot.value = _snapshot.value.copy(bitrateBps = value.roundToLong().coerceAtLeast(0L))
        }
    }

    override fun eventProperty(property: String, value: MPVNode) {
        when (property) {
            "track-list" -> parseTracksNode(value)
            "chapter-list" -> parseChaptersNode(value)
        }
    }

    override fun event(eventId: Int, data: MPVNode) {
        when (eventId) {
            MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> {
                _snapshot.value = _snapshot.value.copy(isFileLoaded = true, isEnded = false)
                PlaybackDiagnosticsLog.info("MPV", "PLAYBACK", "File loaded (${_snapshot.value.voName})")
                mainHandler.post {
                    nativeLock.withLock {
                        if (!isInitialized) return@withLock
                        pendingStartSeconds?.let { sec ->
                            MPVLib.command("seek", String.format(Locale.US, "%.3f", sec), "absolute")
                            pendingStartSeconds = null
                        }
                        pendingInitialSubtitles.forEach { path ->
                            MPVLib.command("sub-add", path, "auto")
                        }
                        pendingInitialSubtitles = emptyList()
                        MPVLib.setPropertyBoolean("pause", !pendingAutoPlay)
                    }
                    refreshTrackLists()
                }
            }
            MPVLib.MpvEvent.MPV_EVENT_END_FILE -> {
                val reason = runCatching { data.get("reason")?.asInt()?.toInt() }.getOrNull()
                if (reason == 4 /* error */) {
                    PlaybackDiagnosticsLog.error("MPV", "ERROR", "MPV could not decode or open this media source (reason=$reason)")
                    _snapshot.value = _snapshot.value.copy(
                        isPlaying = false,
                        lastError = "MPV could not decode or open this media source."
                    )
                }
            }
        }
    }

    fun createSurfaceView(context: Context): SurfaceView {
        return SurfaceView(context).apply {
            holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    attachSurface(holder.surface, width, height)
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, w: Int, h: Int) {
                    resizeSurface(w, h)
                }

                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    detachSurface()
                }
            })
        }
    }
}
