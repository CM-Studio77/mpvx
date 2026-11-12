package com.cmstudio.player.diagnostics

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class PlaybackDiagnosticEntry(
    val timestampMs: Long,
    val level: String, // INFO, WARN, ERROR
    val engine: String,
    val category: String,
    val message: String
)

/**
 * Thread-safe bounded ring buffer capturing real playback diagnostics, decoder initialization,
 * engine switches, buffering events, marker lookups, and playback errors.
 */
object PlaybackDiagnosticsLog {

    private const val MAX_ENTRIES = 200
    private val lock = Any()
    private val buffer = ArrayDeque<PlaybackDiagnosticEntry>(MAX_ENTRIES)

    private val _entries = MutableStateFlow<List<PlaybackDiagnosticEntry>>(emptyList())
    val entries: StateFlow<List<PlaybackDiagnosticEntry>> = _entries.asStateFlow()

    fun record(
        level: String,
        engine: String,
        category: String,
        message: String,
        timestampMs: Long = System.currentTimeMillis()
    ) {
        val entry = PlaybackDiagnosticEntry(
            timestampMs = timestampMs,
            level = level.uppercase(Locale.ROOT),
            engine = engine,
            category = category,
            message = message.trim()
        )
        synchronized(lock) {
            if (buffer.size >= MAX_ENTRIES) {
                buffer.removeFirst()
            }
            buffer.addLast(entry)
            _entries.value = buffer.toList().asReversed()
        }
    }

    fun info(engine: String, category: String, message: String) =
        record("INFO", engine, category, message)

    fun warn(engine: String, category: String, message: String) =
        record("WARN", engine, category, message)

    fun error(engine: String, category: String, message: String) =
        record("ERROR", engine, category, message)

    fun clear() {
        synchronized(lock) {
            buffer.clear()
            _entries.value = emptyList()
        }
    }

    fun formatReport(): String {
        val snapshot = _entries.value
        if (snapshot.isEmpty()) return "No playback diagnostic events recorded in this session."
        val fmt = SimpleDateFormat("HH:mm:ss", Locale.US)
        return buildString {
            appendLine("MPVX Playback Diagnostics (${snapshot.size} events)")
            appendLine("==========================================")
            snapshot.forEach { e ->
                appendLine("[${fmt.format(Date(e.timestampMs))}] ${e.level} [${e.engine}/${e.category}] ${e.message}")
            }
        }
    }
}
