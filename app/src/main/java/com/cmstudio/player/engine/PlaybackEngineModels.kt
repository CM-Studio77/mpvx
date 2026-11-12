package com.cmstudio.player.engine

import java.util.Locale

enum class PlaybackEngineMode(val id: String, val title: String, val shortLabel: String) {
    AUTO("AUTO", "Auto (Smart Routing)", "Auto"),
    MPV("MPV", "MPV (libmpv)", "MPV"),
    NATIVE("NATIVE", "Native (ExoPlayer)", "Exo");

    companion object {
        fun fromId(raw: String?): PlaybackEngineMode {
            return when (raw?.trim()?.uppercase(Locale.ROOT)) {
                "MPV", "LIBMPV" -> MPV
                "NATIVE", "MEDIA3", "EXO", "EXOPLAYER" -> NATIVE
                else -> AUTO
            }
        }
    }
}

data class EngineRenderConfig(
    val mpvProfile: String = "fast",
    val decoderPriority: String = "AUTO", // AUTO, HW_ONLY, SW_PREFER
    val useGpuNext: Boolean = false,
    val useVulkan: Boolean = false,
    val forceOpenGlFallback: Boolean = false,
    val useYuv420p: Boolean = false,
    val hdrTonemapping: Boolean = true,
    val debanding: String = "NONE", // NONE, CPU, GPU
    val debandIterations: Int = 1,
    val debandThreshold: Int = 48,
    val debandRange: Int = 16,
    val debandGrain: Int = 32,
    val anime4kEnabled: Boolean = false,
    val anime4kMode: String = "OFF", // OFF, MODE_A, MODE_B, MODE_C, MODE_AA, MODE_BB, MODE_CA
    val anime4kQuality: String = "FAST" // FAST, BALANCED, HQ
)

object PlaybackEngineResolver {
    private val hdrTokens = listOf(
        "dolby vision",
        "dolby-vision",
        "dolbyvision",
        "dovi",
        "dvhe",
        "dvh1",
        "hdr10+",
        "hdr10plus",
        "hdr10",
        "hlg",
        ".hdr."
    )

    fun isHdrOrDolbyVision(titleOrUri: String?, detectedHdrLabel: String = ""): Boolean {
        val hdrUpper = detectedHdrLabel.trim().uppercase(Locale.ROOT)
        if (hdrUpper in setOf("DOLBY VISION", "HDR10", "HDR10+", "HLG", "HDR")) {
            return true
        }
        val lower = titleOrUri?.trim()?.lowercase(Locale.ROOT).orEmpty()
        if (lower.isEmpty()) return false
        return hdrTokens.any { token -> lower.contains(token) } ||
            Regex("""\bhdr\b""", RegexOption.IGNORE_CASE).containsMatchIn(lower)
    }

    fun isNetworkStreamUri(uriString: String?): Boolean {
        val lower = uriString?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("rtsp://") ||
            lower.startsWith("rtmp://") ||
            lower.endsWith(".m3u8") ||
            lower.endsWith(".mpd")
    }

    /**
     * Resolves concrete runtime engine (`MPV` or `NATIVE`) cleanly:
     * - Per-video override wins if explicit (`MPV` or `NATIVE`).
     * - Explicit global setting wins (`MPV` or `NATIVE`).
     * - `AUTO` routes HDR / Dolby Vision (or network streams if auto-routing enabled) to `NATIVE` (ExoPlayer)
     *   for SurfaceFlinger HDR/DV passthrough & buffered streaming, and uses `MPV` for everything else.
     */
    fun resolveEffectiveEngine(
        configuredGlobal: PlaybackEngineMode,
        perVideoOverride: PlaybackEngineMode?,
        uriString: String?,
        title: String?,
        autoRouteDolbyVision: Boolean,
        autoRouteStreams: Boolean,
        detectedHdrLabel: String = ""
    ): PlaybackEngineMode {
        val selected = perVideoOverride ?: configuredGlobal
        return when (selected) {
            PlaybackEngineMode.MPV -> PlaybackEngineMode.MPV
            PlaybackEngineMode.NATIVE -> PlaybackEngineMode.NATIVE
            PlaybackEngineMode.AUTO -> {
                val hdrMatch = autoRouteDolbyVision &&
                    (isHdrOrDolbyVision(uriString, detectedHdrLabel) || isHdrOrDolbyVision(title, detectedHdrLabel))
                val streamMatch = autoRouteStreams && isNetworkStreamUri(uriString)
                if (hdrMatch || streamMatch) PlaybackEngineMode.NATIVE else PlaybackEngineMode.MPV
            }
        }
    }
}

object MpvBackendOptions {

    fun preferredHwdecMode(decoderPriority: String): String {
        return when (decoderPriority.trim().uppercase(Locale.ROOT)) {
            "HW_ONLY" -> "mediacodec,mediacodec-copy"
            "SW_PREFER" -> "no"
            else -> "mediacodec-copy,mediacodec,no"
        }
    }

    fun resolveVideoBackend(config: EngineRenderConfig): Triple<String, String, String> {
        val vulkanWanted = config.useVulkan && !config.forceOpenGlFallback
        val vo = if (config.useGpuNext && !(config.anime4kEnabled && !vulkanWanted)) "gpu-next" else "gpu"
        val gpuApi = if (vulkanWanted) "vulkan" else "opengl"
        val gpuContext = if (vulkanWanted) "androidvk" else "android"
        return Triple(vo, gpuApi, gpuContext)
    }

    fun buildAudioFilterChain(
        eqBandsDb: List<Float>,
        channelMode: String,
        nightCompressor: Boolean,
        normalizeVolume: Boolean = false
    ): String {
        val filters = mutableListOf<String>()
        val freqs = listOf(60, 230, 910, 3600, 14000)
        val hasEq = eqBandsDb.any { kotlin.math.abs(it) > 0.1f }
        if (hasEq) {
            freqs.forEachIndexed { idx, hz ->
                val db = eqBandsDb.getOrElse(idx) { 0f }.coerceIn(-12f, 12f)
                if (kotlin.math.abs(db) > 0.1f) {
                    filters += String.format(Locale.US, "equalizer=f=%d:t=q:w=1.2:g=%.1f", hz, db)
                }
            }
        }
        when (channelMode.trim().uppercase(Locale.ROOT)) {
            "MONO" -> filters += "pan=mono|c0=0.5*c0+0.5*c1"
            "REVERSE", "REVERSE_STEREO" -> filters += "pan=stereo|c0=c1|c1=c0"
            "STEREO" -> filters += "pan=stereo|c0=c0|c1=c1"
        }
        if (normalizeVolume) {
            filters += "dynaudnorm=f=250:g=15:p=0.92:m=6.0"
        }
        if (nightCompressor) {
            filters += "acompressor=threshold=-18dB:ratio=4:attack=10:release=120:makeup=4dB"
        }
        return if (filters.isEmpty()) "" else "lavfi=[${filters.joinToString(",")}]"
    }

    /**
     * Resolves only clean MIT / Public-Domain Anime4K shaders (excludes proprietary/NC assets).
     */
    fun resolveAnime4kShaderNames(enabled: Boolean, mode: String, quality: String): List<String> {
        if (!enabled) return emptyList()
        val normMode = mode.trim().uppercase(Locale.ROOT)
        if (normMode == "OFF") return emptyList()
        val suffix = when (quality.trim().uppercase(Locale.ROOT)) {
            "HQ" -> "L"
            "BALANCED" -> "M"
            else -> "S"
        }
        val upscaleSuffix = when (quality.trim().uppercase(Locale.ROOT)) {
            "HQ" -> "M"
            "BALANCED" -> "S"
            else -> "S"
        }
        return when (normMode) {
            "MODE_A" -> listOf(
                "Anime4K_Clamp_Highlights.glsl",
                "Anime4K_Restore_CNN_$suffix.glsl",
                "Anime4K_Upscale_CNN_x2_$upscaleSuffix.glsl",
                "Anime4K_AutoDownscalePre_x2.glsl"
            )
            "MODE_B" -> listOf(
                "Anime4K_Clamp_Highlights.glsl",
                "Anime4K_Restore_CNN_Soft_$suffix.glsl",
                "Anime4K_Upscale_CNN_x2_$upscaleSuffix.glsl"
            )
            "MODE_C" -> listOf(
                "Anime4K_Clamp_Highlights.glsl",
                "Anime4K_Upscale_Denoise_CNN_x2_$upscaleSuffix.glsl"
            )
            "MODE_AA" -> listOf(
                "Anime4K_Clamp_Highlights.glsl",
                "Anime4K_Restore_CNN_$suffix.glsl",
                "Anime4K_Upscale_CNN_x2_$upscaleSuffix.glsl",
                "Anime4K_Restore_CNN_M.glsl"
            )
            else -> listOf(
                "Anime4K_Clamp_Highlights.glsl",
                "Anime4K_Restore_CNN_S.glsl",
                "Anime4K_Upscale_CNN_x2_S.glsl"
            )
        }
    }

    fun buildHttpHeaderOption(headers: Map<String, String>): String {
        if (headers.isEmpty()) return ""
        return headers.entries
            .filter { it.key.isNotBlank() && it.value.isNotBlank() }
            .joinToString(",") { (k, v) ->
                "${k.trim()}: ${v.trim()}".replace(",", "\\,")
            }
    }
}
