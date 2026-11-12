package com.cmstudio.player.player

import com.cmstudio.player.BuildConfig
import com.cmstudio.player.ui.player.VideoChapterItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.Locale
import java.util.concurrent.TimeUnit

enum class IntroSegmentKind(val label: String, val buttonLabel: String) {
    INTRO("Intro", "Skip Intro"),
    OUTRO("Outro", "Skip Outro"),
    RECAP("Recap", "Skip Recap"),
    PREVIEW("Preview", "Skip Preview")
}

data class IntroOutroSegment(
    val kind: IntroSegmentKind,
    val startMs: Long,
    val endMs: Long,
    val title: String,
    val source: String
) {
    fun contains(positionMs: Long): Boolean =
        startMs >= 0L && endMs > startMs && positionMs in startMs until endMs
}

data class ParsedMediaEpisodeInfo(
    val cleanTitle: String,
    val season: Int? = null,
    val episode: Int? = null,
    val year: Int? = null,
    val imdbId: String? = null
)

data class IntroLookupResult(
    val segments: List<IntroOutroSegment>,
    val statusMessage: String,
    val fromOnlineProvider: Boolean
)

/**
 * Intro/Outro detection combining embedded Matroska/MPV chapter matching (100% offline-reliable)
 * with legitimate public community timestamp providers (`IntroDB`, `TheIntroDB`, `AniSkip`).
 *
 * Strictly avoids false positives: only marks chapters matching explicit keywords or verified
 * external show/episode matches.
 */
object IntroOutroManager {

    val DEFAULT_INTRO_KEYWORDS = listOf("intro", "opening", "op", "title sequence", "prologue", "recap")
    val DEFAULT_OUTRO_KEYWORDS = listOf("outro", "ending", "ed", "credits", "end credits", "preview", "next episode")

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(7, TimeUnit.SECONDS)
            .readTimeout(7, TimeUnit.SECONDS)
            .build()
    }

    private val imdbRegex = Regex("""\b(tt\d{7,9})\b""", RegexOption.IGNORE_CASE)
    private val sxeRegex = Regex("""(?i)\bS(\d{1,2})[\s._-]*E(\d{1,3})\b""")
    private val epOnlyRegex = Regex("""(?i)(?:\bEpisode\b|\bEp\.?\b|\s-\s)(\d{1,3})\b""")
    private val yearRegex = Regex("""\b(19\d{2}|20\d{2})\b""")

    fun parseEpisodeInfo(rawTitleOrName: String): ParsedMediaEpisodeInfo {
        val base = rawTitleOrName
            .substringBeforeLast('.')
            .replace('.', ' ')
            .replace('_', ' ')
            .trim()
        val imdb = imdbRegex.find(base)?.groupValues?.getOrNull(1)?.lowercase(Locale.ROOT)
        val year = yearRegex.find(base)?.groupValues?.getOrNull(1)?.toIntOrNull()
        val sxeMatch = sxeRegex.find(base)
        val season = sxeMatch?.groupValues?.getOrNull(1)?.toIntOrNull()
        val episode = sxeMatch?.groupValues?.getOrNull(2)?.toIntOrNull()
            ?: epOnlyRegex.find(base)?.groupValues?.getOrNull(1)?.toIntOrNull()

        var clean = base
        if (sxeMatch != null) clean = clean.substring(0, sxeMatch.range.first)
        if (imdb != null) clean = clean.replace(imdbRegex, "")
        clean = clean
            .replace(Regex("""\[[^\]]*\]"""), " ")
            .replace(Regex("""\([^\)]*\)"""), " ")
            .replace(Regex("""(?i)\b(1080p|2160p|720p|4k|x264|x265|hevc|hdr|bluray|web-dl|webrip)\b.*"""), "")
            .replace(Regex("""\s+"""), " ")
            .trim(' ', '-', ':')

        return ParsedMediaEpisodeInfo(
            cleanTitle = clean.ifBlank { base },
            season = season,
            episode = episode,
            year = year,
            imdbId = imdb
        )
    }

    fun parseKeywordCsv(raw: String, defaults: List<String>): List<String> {
        val custom = raw.split(',', ';', '\n')
            .map { it.trim().lowercase(Locale.ROOT) }
            .filter { it.length >= 2 }
        return (defaults + custom).distinct()
    }

    /**
     * Detects intro/outro intervals from embedded chapters using word/phrase boundary checks
     * so normal chapter names (e.g., "Top of the Hill") are never falsely matched by "op".
     */
    fun detectFromChapters(
        chapters: List<VideoChapterItem>,
        totalDurationMs: Long,
        customIntroCsv: String = "",
        customOutroCsv: String = ""
    ): List<IntroOutroSegment> {
        if (chapters.isEmpty()) return emptyList()
        val introKeywords = parseKeywordCsv(customIntroCsv, DEFAULT_INTRO_KEYWORDS)
        val outroKeywords = parseKeywordCsv(customOutroCsv, DEFAULT_OUTRO_KEYWORDS)
        val out = mutableListOf<IntroOutroSegment>()

        chapters.forEachIndexed { idx, ch ->
            val start = ch.startMs.coerceAtLeast(0L)
            val nextStart = chapters.getOrNull(idx + 1)?.startMs?.takeIf { it > start }
            val end = when {
                ch.endMs > start -> ch.endMs
                nextStart != null -> nextStart
                totalDurationMs > start -> totalDurationMs
                else -> 0L
            }
            if (end <= start) return@forEachIndexed
            val dur = end - start
            // Avoid matching giant 20-minute main feature chapters
            if (dur > 6L * 60L * 1000L) return@forEachIndexed

            val titleLower = ch.title.trim().lowercase(Locale.ROOT)
            val isIntro = matchesAnyKeyword(titleLower, introKeywords)
            val isOutro = !isIntro && matchesAnyKeyword(titleLower, outroKeywords)

            if (isIntro) {
                val kind = if (titleLower.contains("recap")) IntroSegmentKind.RECAP else IntroSegmentKind.INTRO
                out += IntroOutroSegment(kind, start, end, ch.title, "Chapters")
            } else if (isOutro) {
                val kind = if (titleLower.contains("preview")) IntroSegmentKind.PREVIEW else IntroSegmentKind.OUTRO
                out += IntroOutroSegment(kind, start, end, ch.title, "Chapters")
            }
        }
        return out
    }

    private fun matchesAnyKeyword(titleLower: String, keywords: List<String>): Boolean {
        if (titleLower.isBlank()) return false
        return keywords.any { kw ->
            val escaped = Regex.escape(kw)
            Regex("""(?:^|[^a-z0-9])$escaped(?:$|[^a-z0-9])""").containsMatchIn(titleLower)
        }
    }

    suspend fun lookupMarkers(
        titleOrFilename: String,
        chapters: List<VideoChapterItem>,
        totalDurationMs: Long,
        providerMode: String, // CHAPTERS, INTRODB, THEINTRODB, ANISKIP, HYBRID
        detectFromChaptersEnabled: Boolean,
        customIntroCsv: String,
        customOutroCsv: String
    ): IntroLookupResult = withContext(Dispatchers.IO) {
        val chapterMarkers = if (detectFromChaptersEnabled) {
            detectFromChapters(chapters, totalDurationMs, customIntroCsv, customOutroCsv)
        } else {
            emptyList()
        }

        val normProvider = providerMode.trim().uppercase(Locale.ROOT)
        if (normProvider == "CHAPTERS" || (chapterMarkers.isNotEmpty() && normProvider == "HYBRID")) {
            val msg = if (chapterMarkers.isNotEmpty()) {
                "Detected ${chapterMarkers.size} chapter marker(s)"
            } else {
                "No chapter intro/outro markers"
            }
            return@withContext IntroLookupResult(chapterMarkers, msg, fromOnlineProvider = false)
        }

        val parsed = parseEpisodeInfo(titleOrFilename)
        if (parsed.cleanTitle.length < 2) {
            return@withContext IntroLookupResult(chapterMarkers, "Offline chapters only (title unresolved)", false)
        }

        val onlineSegments = mutableListOf<IntroOutroSegment>()
        var providerNote = ""

        try {
            if (normProvider == "INTRODB" || normProvider == "THEINTRODB" || normProvider == "HYBRID") {
                val tidbSegments = queryTheIntroDb(parsed)
                if (tidbSegments.isNotEmpty()) {
                    onlineSegments += tidbSegments
                    providerNote = "TheIntroDB: ${tidbSegments.size} marker(s)"
                } else if (!parsed.imdbId.isNullOrBlank()) {
                    val idb = queryIntroDbApp(parsed.imdbId, parsed.season, parsed.episode)
                    if (idb.isNotEmpty()) {
                        onlineSegments += idb
                        providerNote = "IntroDB: ${idb.size} marker(s)"
                    }
                }
            }

            if (onlineSegments.isEmpty() && (normProvider == "ANISKIP" || normProvider == "ISKIP" || normProvider == "HYBRID") && parsed.episode != null) {
                val ani = queryAniSkip(parsed.cleanTitle, parsed.episode)
                if (ani.isNotEmpty()) {
                    onlineSegments += ani
                    providerNote = "AniSkip: ${ani.size} marker(s)"
                }
            }
        } catch (e: Exception) {
            return@withContext IntroLookupResult(
                segments = chapterMarkers,
                statusMessage = "Marker lookup unavailable (${e.javaClass.simpleName}); using chapters",
                fromOnlineProvider = false
            )
        }

        val merged = (onlineSegments + chapterMarkers).distinctBy { "${it.kind}_${it.startMs / 1000L}" }
        val status = when {
            onlineSegments.isNotEmpty() -> providerNote
            chapterMarkers.isNotEmpty() -> "Chapters: ${chapterMarkers.size} marker(s)"
            else -> "No intro/outro markers found"
        }
        IntroLookupResult(merged, status, fromOnlineProvider = onlineSegments.isNotEmpty())
    }

    private fun queryIntroDbApp(imdbId: String, season: Int?, episode: Int?): List<IntroOutroSegment> {
        val url = "https://api.introdb.app/segments".toHttpUrl().newBuilder()
            .addQueryParameter("imdb_id", imdbId)
            .apply {
                if (season != null) addQueryParameter("season", season.toString())
                if (episode != null) addQueryParameter("episode", episode.toString())
            }
            .build()
        val userAgent = "MPVXPlayer/${BuildConfig.VERSION_NAME}"
        val req = Request.Builder().url(url).header("User-Agent", userAgent).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body.string()
            if (body.isBlank()) return emptyList()
            return parseIntroJsonSegments(body, "IntroDB")
        }
    }

    private fun queryTheIntroDb(info: ParsedMediaEpisodeInfo): List<IntroOutroSegment> {
        val imdbId = info.imdbId ?: lookupTmdbOrImdbViaWyzie(info.cleanTitle) ?: return emptyList()
        val urlBuilder = "https://api.theintrodb.org/v2/media".toHttpUrl().newBuilder()
        if (imdbId.startsWith("tt", ignoreCase = true)) {
            urlBuilder.addQueryParameter("imdb_id", imdbId)
        } else {
            urlBuilder.addQueryParameter("tmdb_id", imdbId)
        }
        if (info.season != null) urlBuilder.addQueryParameter("season", info.season.toString())
        if (info.episode != null) urlBuilder.addQueryParameter("episode", info.episode.toString())
        val userAgent = "MPVXPlayer/${BuildConfig.VERSION_NAME}"
        val req = Request.Builder().url(urlBuilder.build()).header("User-Agent", userAgent).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val body = resp.body.string()
            if (body.isBlank()) return emptyList()
            return parseIntroJsonSegments(body, "TheIntroDB")
        }
    }

    private fun lookupTmdbOrImdbViaWyzie(title: String): String? {
        val encoded = URLEncoder.encode(title, "UTF-8")
        val req = Request.Builder()
            .url("https://sub.wyzie.io/api/tmdb/search?q=$encoded")
            .header("User-Agent", "MPVXPlayer/${BuildConfig.VERSION_NAME}")
            .get()
            .build()
        return runCatching {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val obj = JSONObject(resp.body.string())
                val arr = obj.optJSONArray("results") ?: return@use null
                if (arr.length() == 0) return@use null
                val first = arr.getJSONObject(0)
                val matchedTitle = first.optString("title").lowercase(Locale.ROOT)
                val qNorm = title.lowercase(Locale.ROOT)
                if (!matchedTitle.contains(qNorm) && !qNorm.contains(matchedTitle)) return@use null
                first.optInt("id", -1).takeIf { it > 0 }?.toString()
            }
        }.getOrNull()
    }

    private fun queryAniSkip(title: String, episode: Int): List<IntroOutroSegment> {
        val encoded = URLEncoder.encode(title, "UTF-8")
        val userAgent = "MPVXPlayer/${BuildConfig.VERSION_NAME}"
        val jikanReq = Request.Builder()
            .url("https://api.jikan.moe/v4/anime?q=$encoded&limit=3")
            .header("User-Agent", userAgent)
            .get()
            .build()
        val malId = client.newCall(jikanReq).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val data = JSONObject(resp.body.string()).optJSONArray("data") ?: return emptyList()
            if (data.length() == 0) return emptyList()
            data.getJSONObject(0).optInt("mal_id", -1).takeIf { it > 0 } ?: return emptyList()
        }

        val skipReq = Request.Builder()
            .url("https://api.aniskip.com/v2/skip-times/$malId/$episode?types[]=op&types[]=ed&types[]=recap&episodeLength=0")
            .header("User-Agent", userAgent)
            .get()
            .build()
        return client.newCall(skipReq).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            val root = JSONObject(resp.body.string())
            if (!root.optBoolean("found", false)) return emptyList()
            val results = root.optJSONArray("results") ?: return emptyList()
            val out = mutableListOf<IntroOutroSegment>()
            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val interval = item.optJSONObject("interval") ?: continue
                val startMs = (interval.optDouble("startTime", -1.0) * 1000.0).toLong()
                val endMs = (interval.optDouble("endTime", -1.0) * 1000.0).toLong()
                if (startMs < 0L || endMs <= startMs) continue
                val skipType = item.optString("skipType").lowercase(Locale.ROOT)
                val kind = when {
                    skipType.contains("ed") -> IntroSegmentKind.OUTRO
                    skipType.contains("recap") -> IntroSegmentKind.RECAP
                    else -> IntroSegmentKind.INTRO
                }
                out += IntroOutroSegment(kind, startMs, endMs, kind.label, "AniSkip")
            }
            out
        }
    }

    internal fun parseIntroJsonSegments(rawJson: String, sourceLabel: String): List<IntroOutroSegment> {
        val out = mutableListOf<IntroOutroSegment>()
        val trimmed = rawJson.trim()
        val arr = when {
            trimmed.startsWith("[") -> JSONArray(trimmed)
            trimmed.startsWith("{") -> {
                val obj = JSONObject(trimmed)
                obj.optJSONArray("segments")
                    ?: obj.optJSONArray("results")
                    ?: run {
                        // TheIntroDB v2 returns keys like intro/outro/recap directly or array
                        val direct = JSONArray()
                        listOf("intro" to "intro", "outro" to "outro", "recap" to "recap").forEach { (k, type) ->
                            obj.optJSONObject(k)?.let { segObj ->
                                segObj.put("segment_type", type)
                                direct.put(segObj)
                            }
                        }
                        direct
                    }
            }
            else -> return emptyList()
        }
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val startSec = obj.optDouble("start_sec", obj.optDouble("start", -1.0))
            val endSec = obj.optDouble("end_sec", obj.optDouble("end", -1.0))
            if (startSec < 0.0 || endSec <= startSec) continue
            val typeStr = obj.optString("segment_type", obj.optString("type", "intro")).lowercase(Locale.ROOT)
            val kind = when {
                "outro" in typeStr || "end" in typeStr || "credit" in typeStr -> IntroSegmentKind.OUTRO
                "recap" in typeStr -> IntroSegmentKind.RECAP
                "preview" in typeStr -> IntroSegmentKind.PREVIEW
                else -> IntroSegmentKind.INTRO
            }
            out += IntroOutroSegment(
                kind = kind,
                startMs = (startSec * 1000.0).toLong(),
                endMs = (endSec * 1000.0).toLong(),
                title = kind.label,
                source = sourceLabel
            )
        }
        return out
    }
}
