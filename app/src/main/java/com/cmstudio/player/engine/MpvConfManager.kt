package com.cmstudio.player.engine

import android.content.Context
import java.io.File
import java.util.Locale

data class MpvConfEntry(
    val key: String,
    val value: String,
    val lineNumber: Int
)

data class MpvConfDiagnostic(
    val lineNumber: Int,
    val rawLine: String,
    val reason: String,
    val isError: Boolean
)

data class MpvConfValidationResult(
    val validEntries: List<MpvConfEntry>,
    val diagnostics: List<MpvConfDiagnostic>,
    val ownedKeys: Set<String>
) {
    val isClean: Boolean get() = diagnostics.none { it.isError }
}

/**
 * Validated, sandboxed `mpv.conf` manager for MPVX Player.
 *
 * Security & Stability Guarantees:
 * - Only permits known safe playback/video/audio/subtitle/cache keys.
 * - Rejects arbitrary scripts, IPC sockets, shell hooks, `vo=null`, or filesystem paths.
 * - Exposes option ownership so UI clearly indicates which settings `mpv.conf` overrides.
 */
object MpvConfManager {

    const val DEFAULT_TEMPLATE = """# MPVX Safe Playback Configuration
# Only verified playback options are applied; scripts and shell hooks are blocked.
# Example:
# scale=ewa_lanczossharp
# cscale=mitchell
# video-sync=display-resample
# interpolation=yes
"""

    val ALLOWED_OPTIONS: Set<String> = setOf(
        "profile",
        "hwdec",
        "hwdec-codecs",
        "vo",
        "gpu-api",
        "gpu-context",
        "gpu-dumb-mode",
        "scale",
        "cscale",
        "dscale",
        "tscale",
        "correct-downscaling",
        "linear-downscaling",
        "sigmoid-upscaling",
        "interpolation",
        "video-sync",
        "framedrop",
        "deband",
        "deband-iterations",
        "deband-threshold",
        "deband-range",
        "deband-grain",
        "dither-depth",
        "dither",
        "tone-mapping",
        "tone-mapping-param",
        "hdr-compute-peak",
        "target-peak",
        "gamut-mapping-mode",
        "audio-channels",
        "audio-pitch-correction",
        "volume-max",
        "audio-normalize-downmix",
        "sub-auto",
        "sub-ass-override",
        "sub-scale-by-window",
        "sub-scale",
        "sub-font-size",
        "sub-margin-y",
        "sub-bold",
        "sub-italic",
        "sub-fix-timing",
        "cache",
        "cache-secs",
        "demuxer-max-bytes",
        "demuxer-max-back-bytes",
        "demuxer-readahead-secs",
        "network-timeout",
        "keep-open",
        "hr-seek",
        "hr-seek-framedrop"
    )

    private val BLOCKED_KEYS: Set<String> = setOf(
        "script",
        "scripts",
        "script-opts",
        "load-scripts",
        "input-ipc-server",
        "ao",
        "log-file",
        "dump-stats",
        "screenshot-directory",
        "screenshot-template",
        "include",
        "ytdl",
        "ytdl-format",
        "ytdl-raw-options",
        "cookies-file",
        "tls-ca-file"
    )

    private val KEY_REGEX = Regex("""^[a-z0-9][a-z0-9-]{0,48}$""")
    private val UNSAFE_VALUE_REGEX = Regex("""[;&|`$\n\r]""")

    fun configFile(context: Context): File {
        val dir = File(context.filesDir, "mpv").apply { mkdirs() }
        return File(dir, "mpv.conf")
    }

    fun configStoragePath(context: Context): String = configFile(context).absolutePath

    fun readStoredText(context: Context): String {
        val file = configFile(context)
        return runCatching {
            if (file.isFile) file.readText() else ""
        }.getOrDefault("")
    }

    fun writeStoredText(context: Context, text: String): Boolean {
        val file = configFile(context)
        return runCatching {
            file.parentFile?.mkdirs()
            file.writeText(text)
            true
        }.getOrDefault(false)
    }

    fun validateAndParse(rawText: String): MpvConfValidationResult {
        val entries = linkedMapOf<String, MpvConfEntry>()
        val diagnostics = mutableListOf<MpvConfDiagnostic>()

        rawText.lines().forEachIndexed { idx, rawLine ->
            val lineNum = idx + 1
            val trimmed = rawLine.trim()
            if (trimmed.isEmpty() || trimmed.startsWith("#")) return@forEachIndexed
            if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
                diagnostics += MpvConfDiagnostic(
                    lineNumber = lineNum,
                    rawLine = rawLine,
                    reason = "Profile sections like $trimmed are ignored; put key=value lines directly.",
                    isError = true
                )
                return@forEachIndexed
            }

            val cleanLine = trimmed.substringBefore(" #").trim()
            val eqIdx = cleanLine.indexOf('=')
            val key: String
            val value: String
            if (eqIdx > 0) {
                key = cleanLine.substring(0, eqIdx).trim().lowercase(Locale.ROOT)
                value = cleanLine.substring(eqIdx + 1).trim().trim('"', '\'')
            } else {
                key = cleanLine.lowercase(Locale.ROOT)
                value = "yes"
            }

            if (!KEY_REGEX.matches(key)) {
                diagnostics += MpvConfDiagnostic(
                    lineNumber = lineNum,
                    rawLine = rawLine,
                    reason = "Invalid option syntax at line $lineNum.",
                    isError = true
                )
                return@forEachIndexed
            }

            if (key in BLOCKED_KEYS) {
                diagnostics += MpvConfDiagnostic(
                    lineNumber = lineNum,
                    rawLine = rawLine,
                    reason = "Option '$key' is blocked for playback safety and Play Store compliance.",
                    isError = true
                )
                return@forEachIndexed
            }

            if (key !in ALLOWED_OPTIONS) {
                diagnostics += MpvConfDiagnostic(
                    lineNumber = lineNum,
                    rawLine = rawLine,
                    reason = "Unsupported or unrecognized option '$key' (ignored).",
                    isError = true
                )
                return@forEachIndexed
            }

            if (value.isEmpty() || UNSAFE_VALUE_REGEX.containsMatchIn(value) || value.length > 160) {
                diagnostics += MpvConfDiagnostic(
                    lineNumber = lineNum,
                    rawLine = rawLine,
                    reason = "Unsafe or empty value for '$key'.",
                    isError = true
                )
                return@forEachIndexed
            }

            if (key == "vo" && value.equals("null", ignoreCase = true)) {
                diagnostics += MpvConfDiagnostic(
                    lineNumber = lineNum,
                    rawLine = rawLine,
                    reason = "'vo=null' disables video output and is not allowed.",
                    isError = true
                )
                return@forEachIndexed
            }

            if (key == "hwdec" && value !in setOf("auto", "auto-safe", "auto-copy", "mediacodec", "mediacodec-copy", "mediacodec-copy,mediacodec,no", "mediacodec,mediacodec-copy", "no")) {
                diagnostics += MpvConfDiagnostic(
                    lineNumber = lineNum,
                    rawLine = rawLine,
                    reason = "Unsupported hwdec value '$value' (use mediacodec-copy, mediacodec, auto, or no).",
                    isError = true
                )
                return@forEachIndexed
            }

            entries[key] = MpvConfEntry(key = key, value = value, lineNumber = lineNum)
        }

        val list = entries.values.toList()
        return MpvConfValidationResult(
            validEntries = list,
            diagnostics = diagnostics,
            ownedKeys = list.map { it.key }.toSet()
        )
    }

    /**
     * Maps `mpv.conf` keys to the AppSettings domain that owns/conflicts with them.
     */
    fun ownershipGroupForKey(key: String): String = when (key.lowercase(Locale.ROOT)) {
        "hwdec", "hwdec-codecs", "profile", "vo", "gpu-api", "gpu-context" -> "Decoder & Engine"
        "deband", "deband-iterations", "deband-threshold", "deband-range", "deband-grain" -> "Debanding Filter"
        "tone-mapping", "tone-mapping-param", "hdr-compute-peak", "target-peak", "gamut-mapping-mode" -> "HDR Tone Mapping"
        "audio-channels", "audio-pitch-correction", "volume-max", "audio-normalize-downmix" -> "Audio"
        "sub-auto", "sub-ass-override", "sub-scale-by-window", "sub-scale", "sub-font-size", "sub-margin-y", "sub-bold", "sub-italic" -> "Subtitles"
        else -> "MPV Rendering / Cache"
    }
}
