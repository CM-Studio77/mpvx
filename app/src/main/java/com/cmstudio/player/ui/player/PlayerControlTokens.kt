package com.cmstudio.player.ui.player

import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.roundToInt

/**
 * Metadata and helpers for all Media3-compatible player HUD controls, 5-band Equalizer presets,
 * audio channel matrix mixing math, subtitle speed timing, 3-zone gesture mapping, and orientation modes.
 */
data class PlayerControlSpec(
    val id: String,
    val label: String,
    val shortLabel: String,
    val slots: Set<String>
)

enum class GestureTapZone {
    LEFT, CENTER, RIGHT
}

data class VideoChapterItem(
    val title: String,
    val startMs: Long,
    val endMs: Long = 0L
)

data class SecondaryCueItem(
    val startMs: Long,
    val endMs: Long,
    val text: String
)

object PlayerControlCatalog {
    const val SLOT_TOP_RIGHT = "TOP_RIGHT"
    const val SLOT_BOTTOM_LEFT = "BOTTOM_LEFT"
    const val SLOT_BOTTOM_RIGHT = "BOTTOM_RIGHT"
    const val SLOT_PORTRAIT_BOTTOM = "PORTRAIT_BOTTOM"

    val ALL_CONTROLS: List<PlayerControlSpec> = listOf(
        PlayerControlSpec("DECODER", "Decoder Engine", "HW/SW", setOf(SLOT_TOP_RIGHT)),
        PlayerControlSpec("AUDIO_TRACK", "Audio Track & Delay", "Audio", setOf(SLOT_TOP_RIGHT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("SUBTITLES", "Subtitles & Sync", "Subs", setOf(SLOT_TOP_RIGHT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("BOOKMARKS_CHAPTERS", "Chapters", "Chap", setOf(SLOT_TOP_RIGHT, SLOT_BOTTOM_RIGHT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("CURRENT_CHAPTER", "Current Chapter Pill", "Now Ch", setOf(SLOT_BOTTOM_LEFT, SLOT_BOTTOM_RIGHT)),
        PlayerControlSpec("VIDEO_QUALITY", "Video Quality", "Qual", setOf(SLOT_TOP_RIGHT, SLOT_BOTTOM_RIGHT)),
        PlayerControlSpec("EQUALIZER", "Equalizer (5-Band)", "EQ", setOf(SLOT_TOP_RIGHT, SLOT_BOTTOM_LEFT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("TIME_NETWORK", "Clock & Battery Pill", "Clock", setOf(SLOT_TOP_RIGHT)),
        PlayerControlSpec("AMBIENT_MODE", "Ambient Aura Glow", "Aura", setOf(SLOT_TOP_RIGHT, SLOT_BOTTOM_RIGHT)),
        PlayerControlSpec("BACKGROUND_PLAYBACK", "Background Play", "BG", setOf(SLOT_TOP_RIGHT, SLOT_BOTTOM_LEFT)),
        PlayerControlSpec("MORE_OPTIONS", "Tools Hub / Drawer", "More", setOf(SLOT_TOP_RIGHT, SLOT_BOTTOM_RIGHT, SLOT_PORTRAIT_BOTTOM)),

        PlayerControlSpec("LOCK_CONTROLS", "Screen Lock", "Lock", setOf(SLOT_BOTTOM_LEFT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("SCREEN_ROTATION", "Orientation Toggle", "Rotate", setOf(SLOT_BOTTOM_LEFT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("PLAYBACK_SPEED", "Playback Speed", "Speed", setOf(SLOT_BOTTOM_LEFT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("REPEAT_MODE", "Repeat Mode", "Repeat", setOf(SLOT_BOTTOM_LEFT)),
        PlayerControlSpec("SHUFFLE", "Queue Shuffle", "Shuffle", setOf(SLOT_BOTTOM_LEFT, SLOT_BOTTOM_RIGHT)),
        PlayerControlSpec("AB_LOOP", "A-B Segment Loop", "A-B", setOf(SLOT_BOTTOM_LEFT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("CUSTOM_SKIP", "Custom Skip Button", "+Skip", setOf(SLOT_BOTTOM_LEFT, SLOT_BOTTOM_RIGHT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("SLEEP_TIMER", "Sleep Timer", "Timer", setOf(SLOT_BOTTOM_LEFT, SLOT_BOTTOM_RIGHT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("MIRROR", "Mirror Horizontal", "Mirror", setOf(SLOT_BOTTOM_LEFT, SLOT_BOTTOM_RIGHT)),
        PlayerControlSpec("VERTICAL_FLIP", "Vertical Flip", "Flip", setOf(SLOT_BOTTOM_LEFT, SLOT_BOTTOM_RIGHT)),

        PlayerControlSpec("FRAME_NAVIGATION", "Screenshot & Frame Step", "Frame", setOf(SLOT_BOTTOM_RIGHT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("ASPECT_RATIO", "Aspect Ratio", "Aspect", setOf(SLOT_BOTTOM_RIGHT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("VIDEO_ZOOM", "Zoom & Pan", "Zoom", setOf(SLOT_BOTTOM_RIGHT)),
        PlayerControlSpec("PICTURE_IN_PICTURE", "Picture in Picture", "PiP", setOf(SLOT_BOTTOM_RIGHT, SLOT_PORTRAIT_BOTTOM)),
        PlayerControlSpec("NONE", "Empty Spacer", "Spacer", setOf(SLOT_TOP_RIGHT, SLOT_BOTTOM_LEFT, SLOT_BOTTOM_RIGHT, SLOT_PORTRAIT_BOTTOM))
    )

    private val ALIASES: Map<String, String> = mapOf(
        "AUTO" to "DECODER",
        "MUSIC" to "AUDIO_TRACK",
        "MORE" to "MORE_OPTIONS",
        "LOCK" to "LOCK_CONTROLS",
        "ROTATE" to "SCREEN_ROTATION",
        "SPEED" to "PLAYBACK_SPEED",
        "REPEAT" to "REPEAT_MODE",
        "SCREENSHOT" to "FRAME_NAVIGATION",
        "ASPECT" to "ASPECT_RATIO",
        "ZOOM" to "VIDEO_ZOOM",
        "PIP" to "PICTURE_IN_PICTURE",
        "CHAPTERS" to "BOOKMARKS_CHAPTERS",
        "QUALITY" to "VIDEO_QUALITY",
        "EQ" to "EQUALIZER"
    )

    fun canonicalToken(raw: String): String {
        val upper = raw.trim().uppercase()
        if (upper.isEmpty()) return ""
        return ALIASES[upper] ?: upper
    }

    fun parseSlotCsv(csv: String): List<String> =
        csv.split(",")
            .map { canonicalToken(it) }
            .filter { it.isNotEmpty() && it != "CAST" && ALL_CONTROLS.any { c -> c.id == it } }
            .distinct()

    fun moveTokenInCsv(csv: String, token: String, direction: Int): String {
        val canonical = canonicalToken(token)
        val list = parseSlotCsv(csv).toMutableList()
        val idx = list.indexOf(canonical)
        if (idx < 0) return list.joinToString(",")
        val target = (idx + direction).coerceIn(0, list.lastIndex)
        if (target == idx) return list.joinToString(",")
        list.removeAt(idx)
        list.add(target, canonical)
        return list.joinToString(",")
    }

    fun toggleTokenInCsv(csv: String, token: String): String {
        val canonical = canonicalToken(token)
        val list = parseSlotCsv(csv).toMutableList()
        if (canonical in list) {
            if (list.size > 1) list.remove(canonical)
        } else {
            list.add(canonical)
        }
        return list.joinToString(",")
    }

    fun findDuplicateTokens(slots: List<String>): Set<String> {
        val seen = mutableSetOf<String>()
        val dupes = mutableSetOf<String>()
        slots.forEach { csv ->
            parseSlotCsv(csv).forEach { token ->
                if (token != "NONE" && !seen.add(token)) {
                    dupes.add(token)
                }
            }
        }
        return dupes
    }

    val ORIENTATION_MODES: List<String> = listOf(
        "Auto",
        "Landscape",
        "Reverse Landscape",
        "Sensor Landscape",
        "Portrait",
        "Reverse Portrait",
        "Sensor Portrait",
        "Match Video"
    )

    val SEEKBAR_STYLES: List<Pair<String, String>> = listOf(
        "NORMAL" to "Normal",
        "STANDARD" to "Standard",
        "WAVY" to "Wavy",
        "THICK" to "Thick",
        "SLIM" to "Slim",
        "EXPRESSIVE" to "Expressive Pill",
        "SEGMENTED" to "Segmented Blocks",
        "DUAL_GLOW" to "Neon Dual Glow",
        "PULSE_BAR" to "Waveform Pulse"
    )
}

object EqualizerPresetsCatalog {
    // 5-band ISO gains in dB (-12f..+12f) matching 60Hz, 230Hz, 910Hz, 3.6kHz, 14kHz
    val PRESETS: Map<String, List<Float>> = linkedMapOf(
        "Flat" to listOf(0f, 0f, 0f, 0f, 0f),
        "Cinema" to listOf(4f, 2f, 0f, 2f, 4f),
        "Rock" to listOf(5f, 3f, -1f, 3f, 5f),
        "Pop" to listOf(-1f, 3f, 5f, 2f, -1f),
        "Jazz" to listOf(4f, 2f, -2f, 3f, 4f),
        "Classical" to listOf(4f, 3f, 0f, 3f, 5f),
        "Electronic" to listOf(6f, 4f, -1f, 2f, 6f),
        "Bass Boost" to listOf(8f, 6f, 1f, 0f, 0f),
        "Treble Boost" to listOf(0f, 0f, 1f, 5f, 8f),
        "Voice Boost" to listOf(-2f, 1f, 6f, 5f, 1f),
        "Vocal Clarifier" to listOf(-3f, 0f, 5f, 7f, 3f),
        "Custom" to listOf(0f, 0f, 0f, 0f, 0f)
    )

    val BAND_LABELS: List<String> = listOf("60Hz", "230Hz", "910Hz", "3.6k", "14k")

    fun bandsForPreset(name: String, customCsv: String): List<Float> {
        if (name.equals("Custom", ignoreCase = true)) {
            val parsed = parseCustomBands(customCsv)
            if (parsed.size == 5) return parsed
        }
        return PRESETS[name] ?: PRESETS.getValue("Cinema")
    }

    fun parseCustomBands(csv: String): List<Float> {
        val raw = csv.split(",").mapNotNull { it.trim().toFloatOrNull()?.coerceIn(-12f, 12f) }
        return if (raw.size == 5) raw else listOf(0f, 0f, 0f, 0f, 0f)
    }

    fun serializeBands(bands: List<Float>): String =
        bands.take(5).joinToString(",") { String.format(java.util.Locale.US, "%.1f", it.coerceIn(-12f, 12f)) }
}

/**
 * Pure-JVM 16-bit PCM channel routing helper for Audio Channels (`AUTO`, `MONO`, `STEREO`, `REVERSE_STEREO`).
 */
object ChannelMixMath {
    const val MODE_AUTO = "AUTO"
    const val MODE_MONO = "MONO"
    const val MODE_STEREO = "STEREO"
    const val MODE_REVERSE_STEREO = "REVERSE_STEREO"

    fun normalizeMode(mode: String?): String = when (mode?.trim()?.uppercase()) {
        MODE_MONO -> MODE_MONO
        MODE_STEREO -> MODE_STEREO
        MODE_REVERSE_STEREO -> MODE_REVERSE_STEREO
        else -> MODE_AUTO
    }

    fun processPcm16Samples(samples: ShortArray, sampleCount: Int, channelCount: Int, mode: String) {
        if (channelCount <= 1 || sampleCount <= 0) return
        val normalized = normalizeMode(mode)
        if (normalized == MODE_AUTO || (normalized == MODE_STEREO && channelCount == 2)) return

        val frames = sampleCount / channelCount
        var idx = 0
        for (f in 0 until frames) {
            when (normalized) {
                MODE_MONO -> {
                    var sum = 0
                    for (c in 0 until channelCount) {
                        sum += samples[idx + c].toInt()
                    }
                    val mono = (sum / channelCount).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                    for (c in 0 until channelCount) {
                        samples[idx + c] = mono
                    }
                }
                MODE_REVERSE_STEREO -> {
                    val left = samples[idx]
                    val right = samples[idx + 1]
                    samples[idx] = right
                    samples[idx + 1] = left
                }
                MODE_STEREO -> {
                    if (channelCount > 2) {
                        // Gentle downmix preserving L/R plus center attenuation across extra surround channels
                        val center = if (channelCount >= 3) samples[idx + 2].toInt() else 0
                        val mixFactor = (center * 707) / 1000
                        val l = (samples[idx].toInt() + mixFactor).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        val r = (samples[idx + 1].toInt() + mixFactor).coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
                        samples[idx] = l
                        samples[idx + 1] = r
                    }
                }
            }
            idx += channelCount
        }
    }

    fun processLittleEndianBuffer(
        inputBuffer: ByteBuffer,
        outputBuffer: ByteBuffer,
        channelCount: Int,
        mode: String
    ) {
        val normalized = normalizeMode(mode)
        if (normalized == MODE_AUTO || channelCount <= 1 || (normalized == MODE_STEREO && channelCount == 2)) {
            outputBuffer.put(inputBuffer)
            return
        }
        val inOrder = inputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        outputBuffer.order(ByteOrder.LITTLE_ENDIAN)
        val remainingShorts = inOrder.remaining() / 2
        val scratch = ShortArray(remainingShorts)
        inOrder.asShortBuffer().get(scratch)
        inOrder.position(inOrder.limit())
        processPcm16Samples(scratch, remainingShorts, channelCount, normalized)
        val outShort = outputBuffer.asShortBuffer()
        outShort.put(scratch)
        outputBuffer.position(outputBuffer.position() + remainingShorts * 2)
    }
}

object SubtitleTimingMath {
    fun clampSpeedMilli(speedMilli: Int): Int = speedMilli.coerceIn(100, 10000)

    fun adjustedPositionUs(positionUs: Long, delayMs: Int, speedMilli: Int): Long {
        val clampedSpeed = clampSpeedMilli(speedMilli)
        val scaledUs = (positionUs * clampedSpeed) / 1000L
        return scaledUs - (delayMs * 1000L)
    }
}

object PlayerGestureZoneMath {
    fun zoneForTapX(x: Float, width: Float, zoneWidthPct: Int): GestureTapZone {
        if (width <= 0f) return GestureTapZone.CENTER
        val frac = (zoneWidthPct.coerceIn(10, 45) / 100f)
        val norm = (x / width).coerceIn(0f, 1f)
        return when {
            norm < frac -> GestureTapZone.LEFT
            norm > (1f - frac) -> GestureTapZone.RIGHT
            else -> GestureTapZone.CENTER
        }
    }

    fun normalizeSeekSensitivity(value: Float): Float = value.coerceIn(0.4f, 2.5f)
}

object DoubleTapActionCatalog {
    const val ACTION_SEEK = "SEEK"
    const val ACTION_PLAY_PAUSE = "PLAY_PAUSE"
    const val ACTION_CUSTOM_SKIP = "CUSTOM_SKIP"
    const val ACTION_NONE = "NONE"

    val ALL_ACTIONS = listOf(
        ACTION_SEEK to "Seek (+/- Step)",
        ACTION_PLAY_PAUSE to "Play / Pause",
        ACTION_CUSTOM_SKIP to "Custom Skip",
        ACTION_NONE to "Disabled"
    )

    fun normalizeAction(raw: String?, fallback: String): String = when (raw?.trim()?.uppercase()) {
        ACTION_SEEK -> ACTION_SEEK
        ACTION_PLAY_PAUSE -> ACTION_PLAY_PAUSE
        ACTION_CUSTOM_SKIP -> ACTION_CUSTOM_SKIP
        ACTION_NONE -> ACTION_NONE
        else -> fallback
    }
}

object ScreenshotTemplateFormatter {
    const val DEFAULT_TEMPLATE = "MPVX_{title}_{time}"

    fun formatFilename(
        template: String?,
        videoTitle: String?,
        timestampText: String,
        dateText: String,
        extension: String
    ): String {
        val rawTemplate = template?.trim()?.ifBlank { DEFAULT_TEMPLATE } ?: DEFAULT_TEMPLATE
        val safeTitle = (videoTitle ?: "Video")
            .substringBeforeLast('.')
            .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
            .trim('_')
            .take(40)
            .ifBlank { "Video" }
        val resolved = rawTemplate
            .replace("{title}", safeTitle)
            .replace("{time}", timestampText)
            .replace("{date}", dateText)
            .replace("/", "_")
            .replace("\\", "_")
            .replace("..", "")
            .trim()
            .ifBlank { "MPVX_$timestampText" }
        return "$resolved.$extension"
    }
}
