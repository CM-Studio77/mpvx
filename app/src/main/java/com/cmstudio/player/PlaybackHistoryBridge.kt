package com.cmstudio.player

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import androidx.preference.PreferenceManager
import androidx.annotation.Keep
import com.cmstudio.player.ui.AppSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLDecoder
import java.util.Locale

@Keep
data class PlaybackHistoryEntry(
    val uri: Uri,
    val title: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAtMs: Long,
    val played: Boolean,
    val pinned: Boolean,
    val isNetwork: Boolean,
    val mimeType: String = "video/*"
) {
    val progress: Float
        get() = if (durationMs > 0L) (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) else 0f

    val isAudio: Boolean
        get() = mimeType.startsWith("audio/", ignoreCase = true)
}

/**
 * Pure-JVM representation of a recorded local or network playback history item.
 */
data class PlaybackHistoryRecord(
    val uriString: String,
    val title: String,
    val mimeType: String,
    val positionMs: Long,
    val durationMs: Long,
    val lastPlayedAtMs: Long,
    val played: Boolean,
    val pinned: Boolean,
    val isNetwork: Boolean
)

/**
 * Unified playback history bridge supporting BOTH local device videos (`content://`, `file://`, SAF)
 * and network streams (`http://`, `https://`, `rtsp://`, etc.).
 *
 * Legacy `History.java` ignores non-network URIs (`if (!isNetworkUri(uri)) return`), so this bridge
 * persists a complete local + network JSON timeline while keeping `History` & `Prefs` synchronized.
 */
object PlaybackHistoryBridge {

    internal const val KEY_UNIFIED_HISTORY = "mpvx_unified_history_json"
    private const val MAX_HISTORY_ITEMS = 120

    private val _historyRevision = MutableStateFlow(0L)
    val historyRevision: StateFlow<Long> = _historyRevision.asStateFlow()

    private fun defaultPrefs(context: Context): SharedPreferences =
        PreferenceManager.getDefaultSharedPreferences(context.applicationContext)

    fun isNetworkScheme(uriString: String): Boolean {
        val lower = uriString.trim().lowercase(Locale.ROOT)
        return lower.startsWith("http://") ||
            lower.startsWith("https://") ||
            lower.startsWith("rtsp://") ||
            lower.startsWith("rtmp://") ||
            lower.startsWith("udp://") ||
            lower.startsWith("rtp://")
    }

    /**
     * Normalizes `/sdcard/...` and `/storage/emulated/0/...` paths so `file://` launches and
     * MediaStore `content://` scans correlate accurately.
     */
    fun normalizeStoragePath(raw: String?): String {
        val trimmed = raw?.trim().orEmpty()
        if (trimmed.isEmpty()) return ""
        val decoded = try {
            URLDecoder.decode(trimmed, "UTF-8")
        } catch (_: Throwable) {
            trimmed
        }
        return decoded
            .removePrefix("file://")
            .replace("/sdcard/", "/storage/emulated/0/")
            .trimEnd('/')
            .lowercase(Locale.ROOT)
    }

    fun extractFileName(uriOrPath: String?): String {
        val clean = uriOrPath?.trim()?.substringBefore('?')?.substringBefore('#').orEmpty()
        if (clean.isEmpty()) return ""
        val decoded = try {
            URLDecoder.decode(clean, "UTF-8")
        } catch (_: Throwable) {
            clean
        }
        val segment = decoded.substringAfterLast('/')
        return if (segment.contains('.')) segment.lowercase(Locale.ROOT) else ""
    }

    fun isWatchedByThreshold(positionMs: Long, durationMs: Long, thresholdPct: Int): Boolean {
        if (thresholdPct <= 0 || durationMs <= 0L) return false
        val clamped = thresholdPct.coerceIn(50, 100)
        return positionMs >= (durationMs * clamped) / 100L
    }

    internal fun loadLocalRecordsFromPrefs(prefs: SharedPreferences): List<PlaybackHistoryRecord> {
        val raw = prefs.getString(KEY_UNIFIED_HISTORY, null).orEmpty()
        if (raw.isBlank()) return emptyList()
        return try {
            val arr = JSONArray(raw)
            buildList {
                for (i in 0 until arr.length()) {
                    val obj = arr.optJSONObject(i) ?: continue
                    val uriStr = obj.optString("uri", "")
                    if (uriStr.isBlank()) continue
                    add(
                        PlaybackHistoryRecord(
                            uriString = uriStr,
                            title = obj.optString("title", "").ifBlank {
                                extractFileName(uriStr).ifBlank { uriStr }
                            },
                            mimeType = obj.optString("mimeType", "video/*"),
                            positionMs = obj.optLong("positionMs", 0L).coerceAtLeast(0L),
                            durationMs = obj.optLong("durationMs", 0L).coerceAtLeast(0L),
                            lastPlayedAtMs = obj.optLong("lastPlayedAtMs", 0L).coerceAtLeast(0L),
                            played = obj.optBoolean("played", false),
                            pinned = obj.optBoolean("pinned", false),
                            isNetwork = obj.optBoolean("isNetwork", isNetworkScheme(uriStr))
                        )
                    )
                }
            }.sortedByDescending { it.lastPlayedAtMs }
        } catch (_: Throwable) {
            emptyList()
        }
    }

    private fun saveRecordsToPrefs(prefs: SharedPreferences, records: List<PlaybackHistoryRecord>) {
        val arr = JSONArray()
        records.sortedByDescending { it.lastPlayedAtMs }
            .take(MAX_HISTORY_ITEMS)
            .forEach { rec ->
                arr.put(JSONObject().apply {
                    put("uri", rec.uriString)
                    put("title", rec.title)
                    put("mimeType", rec.mimeType)
                    put("positionMs", rec.positionMs)
                    put("durationMs", rec.durationMs)
                    put("lastPlayedAtMs", rec.lastPlayedAtMs)
                    put("played", rec.played)
                    put("pinned", rec.pinned)
                    put("isNetwork", rec.isNetwork)
                })
            }
        prefs.edit().putString(KEY_UNIFIED_HISTORY, arr.toString()).apply()
    }

    internal fun recordEntryToPrefs(
        prefs: SharedPreferences,
        uriString: String,
        title: String?,
        mimeType: String?,
        positionMs: Long,
        durationMs: Long,
        rememberPosition: Boolean = true,
        watchedThresholdPct: Int = 95,
        playedOverride: Boolean? = null,
        timestampMs: Long = System.currentTimeMillis()
    ): List<PlaybackHistoryRecord> {
        val cleanUri = uriString.trim()
        if (cleanUri.isEmpty()) return loadLocalRecordsFromPrefs(prefs)
        val existing = loadLocalRecordsFromPrefs(prefs).toMutableList()
        val normPath = normalizeStoragePath(cleanUri)
        val fileName = extractFileName(cleanUri)
        val cleanTitleKey = title?.trim()?.lowercase(Locale.ROOT).orEmpty()

        val matchIdx = existing.indexOfFirst { item ->
            item.uriString == cleanUri ||
                (normPath.isNotEmpty() && normalizeStoragePath(item.uriString) == normPath) ||
                (!isNetworkScheme(cleanUri) && !item.isNetwork && (
                    (fileName.isNotEmpty() && !fileName.all(Char::isDigit) && extractFileName(item.uriString) == fileName) ||
                        (cleanTitleKey.isNotEmpty() && item.title.trim().lowercase(Locale.ROOT) == cleanTitleKey &&
                            (durationMs <= 0L || item.durationMs <= 0L || kotlin.math.abs(durationMs - item.durationMs) < 2000L))
                    ))
        }
        val prev = if (matchIdx >= 0) existing.removeAt(matchIdx) else null

        val safeDuration = durationMs.coerceAtLeast(prev?.durationMs ?: 0L).coerceAtLeast(0L)
        val safePosition = if (rememberPosition) positionMs.coerceAtLeast(0L) else (prev?.positionMs ?: 0L)
        val resolvedTitle = title?.trim()?.takeIf { it.isNotEmpty() }
            ?: prev?.title?.takeIf { it.isNotBlank() }
            ?: extractFileName(cleanUri).ifBlank { cleanUri }
        val resolvedPlayed = playedOverride ?: isWatchedByThreshold(safePosition, safeDuration, watchedThresholdPct)

        val updated = PlaybackHistoryRecord(
            uriString = cleanUri,
            title = resolvedTitle,
            mimeType = mimeType?.takeIf { it.isNotBlank() } ?: prev?.mimeType ?: "video/*",
            positionMs = safePosition,
            durationMs = safeDuration,
            lastPlayedAtMs = timestampMs,
            played = resolvedPlayed,
            pinned = prev?.pinned ?: false,
            isNetwork = isNetworkScheme(cleanUri)
        )
        existing.add(0, updated)
        saveRecordsToPrefs(prefs, existing)
        return existing
    }

    internal fun setWatchedStateInPrefs(
        prefs: SharedPreferences,
        uriString: String,
        title: String?,
        durationMs: Long,
        watched: Boolean,
        timestampMs: Long = System.currentTimeMillis()
    ) {
        val existing = loadLocalRecordsFromPrefs(prefs)
        val existingRec = findMatchingRecord(existing, uriString, title)
        val dur = durationMs.coerceAtLeast(existingRec?.durationMs ?: 0L)
        val targetPos = if (watched) dur else 0L
        recordEntryToPrefs(
            prefs = prefs,
            uriString = existingRec?.uriString ?: uriString,
            title = title ?: existingRec?.title,
            mimeType = existingRec?.mimeType ?: "video/*",
            positionMs = targetPos,
            durationMs = dur,
            rememberPosition = true,
            watchedThresholdPct = 95,
            playedOverride = watched,
            timestampMs = timestampMs
        )
    }

    internal fun findMatchingRecord(
        records: List<PlaybackHistoryRecord>,
        uriString: String,
        fallbackTitle: String? = null
    ): PlaybackHistoryRecord? {
        records.firstOrNull { it.uriString == uriString }?.let { return it }
        val normPath = normalizeStoragePath(uriString)
        if (normPath.isNotEmpty()) {
            records.firstOrNull { normalizeStoragePath(it.uriString) == normPath }?.let { return it }
        }
        val candidateName = extractFileName(uriString).ifBlank {
            extractFileName(fallbackTitle)
        }
        if (candidateName.isNotEmpty() && !isNetworkScheme(uriString)) {
            records.firstOrNull { !it.isNetwork && extractFileName(it.uriString) == candidateName }?.let { return it }
        }
        return null
    }

    internal fun findBestSavedPosition(
        prefs: SharedPreferences,
        uriString: String,
        fallbackTitle: String? = null
    ): Long {
        val records = loadLocalRecordsFromPrefs(prefs)
        val rec = findMatchingRecord(records, uriString, fallbackTitle) ?: return 0L
        return if (rec.played) 0L else rec.positionMs
    }

    internal fun removeEntryFromPrefs(prefs: SharedPreferences, uriString: String) {
        val normPath = normalizeStoragePath(uriString)
        val filtered = loadLocalRecordsFromPrefs(prefs).filterNot { item ->
            item.uriString == uriString ||
                (normPath.isNotEmpty() && normalizeStoragePath(item.uriString) == normPath)
        }
        saveRecordsToPrefs(prefs, filtered)
    }

    internal fun clearAllHistoryPrefs(prefs: SharedPreferences) {
        prefs.edit().remove(KEY_UNIFIED_HISTORY).apply()
    }

    fun savedPositionFor(context: Context, uri: Uri, fallbackTitle: String? = null): Long {
        val app = context.applicationContext
        val rec = findMatchingRecord(loadLocalRecordsFromPrefs(defaultPrefs(app)), uri.toString(), fallbackTitle)
        return if (rec != null && !rec.played) rec.positionMs else 0L
    }

    fun savedDurationFor(context: Context, uri: Uri, fallbackTitle: String? = null): Long {
        val app = context.applicationContext
        return findMatchingRecord(loadLocalRecordsFromPrefs(defaultPrefs(app)), uri.toString(), fallbackTitle)
            ?.durationMs ?: 0L
    }

    fun loadHistory(context: Context): List<PlaybackHistoryEntry> {
        val app = context.applicationContext
        val sharedPrefs = defaultPrefs(app)
        return loadLocalRecordsFromPrefs(sharedPrefs).map { rec ->
            PlaybackHistoryEntry(
                uri = Uri.parse(rec.uriString),
                title = rec.title,
                positionMs = rec.positionMs,
                durationMs = rec.durationMs,
                lastPlayedAtMs = rec.lastPlayedAtMs,
                played = rec.played,
                pinned = rec.pinned,
                isNetwork = rec.isNetwork,
                mimeType = rec.mimeType
            )
        }
    }

    fun recordPlayback(
        context: Context,
        uri: Uri,
        title: String?,
        mimeType: String?,
        positionMs: Long,
        durationMs: Long,
        rememberPosition: Boolean = true
    ) {
        val app = context.applicationContext
        val sharedPrefs = defaultPrefs(app)
        val threshold = AppSettings.watchedThresholdPct.value
        val safeDuration = durationMs.coerceAtLeast(0L)
        val safePos = positionMs.coerceAtLeast(0L)

        recordEntryToPrefs(
            prefs = sharedPrefs,
            uriString = uri.toString(),
            title = title,
            mimeType = mimeType,
            positionMs = safePos,
            durationMs = safeDuration,
            rememberPosition = rememberPosition,
            watchedThresholdPct = threshold
        )
        _historyRevision.value += 1L
    }

    fun setWatchedState(
        context: Context,
        uri: Uri,
        title: String?,
        durationMs: Long,
        watched: Boolean
    ) {
        val app = context.applicationContext
        val sharedPrefs = defaultPrefs(app)
        setWatchedStateInPrefs(
            prefs = sharedPrefs,
            uriString = uri.toString(),
            title = title,
            durationMs = durationMs,
            watched = watched
        )
        _historyRevision.value += 1L
    }

    fun removeHistoryEntry(context: Context, uri: Uri) {
        val app = context.applicationContext
        val sharedPrefs = defaultPrefs(app)
        removeEntryFromPrefs(sharedPrefs, uri.toString())
        _historyRevision.value += 1L
    }

    fun clearHistory(context: Context) {
        val app = context.applicationContext
        val sharedPrefs = defaultPrefs(app)
        clearAllHistoryPrefs(sharedPrefs)
        app.deleteFile("positions")
        app.deleteFile("durations")
        _historyRevision.value += 1L
    }
}
