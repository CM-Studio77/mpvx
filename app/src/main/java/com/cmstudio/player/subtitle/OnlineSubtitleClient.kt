package com.cmstudio.player.subtitle

import android.content.Context
import android.net.Uri
import com.cmstudio.player.BuildConfig
import com.cmstudio.player.player.IntroOutroManager
import com.sigpwned.chardet4j.io.DecodedInputStreamReader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.zip.GZIPInputStream
import java.util.zip.ZipInputStream

data class OnlineMediaMatch(
    val id: String,
    val title: String,
    val year: String,
    val mediaType: String, // "movie" or "series"
    val imdbId: String? = null,
    val tmdbId: String? = null,
    val posterUrl: String? = null
)

data class OnlineSubtitleResult(
    val id: String,
    val title: String,
    val languageCode: String,
    val languageName: String,
    val format: String,
    val downloadUrl: String,
    val source: String,
    val isHearingImpaired: Boolean = false,
    val downloadCount: Int = 0
)

data class OnlineSubtitleSearchOutcome(
    val results: List<OnlineSubtitleResult>,
    val statusMessage: String,
    val providerUsed: String,
    val error: String? = null,
    val mediaMatches: List<OnlineMediaMatch> = emptyList(),
    val selectedMedia: OnlineMediaMatch? = null
)

/**
 * Online Subtitle Search, Poster Catalog Identification, ZIP/GZIP Unpacker & Live Cue Translator.
 *
 * Keyless open providers:
 * - Cinemeta (`v3-cinemeta.strem.io`) & Wyzie Catalog (`sub.wyzie.io/api/tmdb/search`) for Movie/Series posters & IDs
 * - OpenSubtitles v3 (`opensubtitles-v3.strem.io`) for direct keyless subtitle downloads
 * - Optional Wyzie (`sub.wyzie.io/search`) & SubDL (`api.subdl.com`) when user configures API keys.
 */
object OnlineSubtitleClient {

    private const val WYZIE_BASE = "https://sub.wyzie.io"
    private const val CINEMETA_BASE = "https://v3-cinemeta.strem.io"
    private const val OPENSUBS_V3_BASE = "https://opensubtitles-v3.strem.io"
    private const val SUBDL_API = "https://api.subdl.com/api/v1/subtitles"
    private const val SUBDL_DL_PREFIX = "https://dl.subdl.com"
    private const val MAX_SUB_BYTES = 8 * 1024 * 1024
    private const val MAX_TRANSLATE_CACHE = 300

    val SUPPORTED_LANGUAGES = listOf(
        "all" to "All",
        "en" to "English",
        "hi" to "Hindi",
        "ta" to "Tamil",
        "te" to "Telugu",
        "bn" to "Bengali",
        "ml" to "Malayalam",
        "ur" to "Urdu",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "ja" to "Japanese",
        "ko" to "Korean",
        "pt" to "Portuguese",
        "ar" to "Arabic",
        "ru" to "Russian",
        "zh" to "Chinese",
        "id" to "Indonesian",
        "it" to "Italian",
        "tr" to "Turkish"
    )

    val TRANSLATION_LANGUAGES = listOf(
        "hi" to "Hindi",
        "en" to "English",
        "ta" to "Tamil",
        "te" to "Telugu",
        "bn" to "Bengali",
        "es" to "Spanish",
        "fr" to "French",
        "ar" to "Arabic",
        "ja" to "Japanese",
        "ko" to "Korean"
    )

    private val ISO639_TO_SHORT = mapOf(
        "eng" to "en", "en" to "en",
        "hin" to "hi", "hi" to "hi",
        "tam" to "ta", "ta" to "ta",
        "tel" to "te", "te" to "te",
        "ben" to "bn", "bn" to "bn",
        "mal" to "ml", "ml" to "ml",
        "mar" to "mr", "mr" to "mr",
        "kan" to "kn", "kn" to "kn",
        "urd" to "ur", "ur" to "ur",
        "spa" to "es", "es" to "es",
        "fre" to "fr", "fra" to "fr", "fr" to "fr",
        "deu" to "de", "ger" to "de", "de" to "de",
        "jpn" to "ja", "ja" to "ja",
        "kor" to "ko", "ko" to "ko",
        "por" to "pt", "pob" to "pt", "pt" to "pt",
        "ara" to "ar", "ar" to "ar",
        "rus" to "ru", "ru" to "ru",
        "chi" to "zh", "zho" to "zh", "zh" to "zh",
        "ind" to "id", "id" to "id",
        "ita" to "it", "it" to "it",
        "tur" to "tr", "tr" to "tr"
    )

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    private val translateClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(6, TimeUnit.SECONDS)
            .build()
    }

    private val translationCache = object : LinkedHashMap<String, String>(128, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String>?): Boolean =
            size > MAX_TRANSLATE_CACHE
    }

    fun normalizeLanguageCode(raw: String): String {
        val clean = raw.trim().lowercase(Locale.ROOT).substringBefore('-').substringBefore('_')
        return ISO639_TO_SHORT[clean] ?: clean.take(2).ifBlank { "en" }
    }

    fun languageDisplayName(code: String): String {
        val norm = normalizeLanguageCode(code)
        return SUPPORTED_LANGUAGES.firstOrNull { it.first == norm }?.second
            ?: Locale.forLanguageTag(norm).getDisplayLanguage(Locale.ENGLISH).ifBlank { norm.uppercase(Locale.ROOT) }
    }

    suspend fun search(
        query: String,
        languageCode: String = "en",
        searchMode: String = "HYBRID", // OPENSUBS, WYZIE, SUBHUB, HYBRID
        subdlApiKey: String = "",
        wyzieApiKey: String = ""
    ): OnlineSubtitleSearchOutcome = withContext(Dispatchers.IO) {
        val rawQuery = query.trim()
        if (rawQuery.isEmpty()) {
            return@withContext OnlineSubtitleSearchOutcome(emptyList(), "Enter a movie or episode title to search.", searchMode, "Empty query")
        }

        val parsed = IntroOutroManager.parseEpisodeInfo(rawQuery)
        val cleanLang = normalizeLanguageCode(languageCode).let {
            if (languageCode.trim().equals("all", ignoreCase = true)) "all" else it
        }
        val matches = searchMediaCatalog(
            cleanTitle = parsed.cleanTitle,
            preferredYear = parsed.year,
            isSeriesHint = parsed.season != null || parsed.episode != null
        )

        val chosen = when {
            !parsed.imdbId.isNullOrBlank() -> OnlineMediaMatch(
                id = parsed.imdbId,
                title = parsed.cleanTitle,
                year = parsed.year?.toString().orEmpty(),
                mediaType = if (parsed.season != null) "series" else "movie",
                imdbId = parsed.imdbId
            )
            matches.isNotEmpty() -> matches.first()
            else -> null
        }

        fetchSubtitlesForSelection(
            cleanTitle = parsed.cleanTitle,
            selectedMedia = chosen,
            allMatches = matches,
            season = parsed.season,
            episode = parsed.episode,
            requestedLang = cleanLang,
            searchMode = searchMode,
            subdlApiKey = subdlApiKey,
            wyzieApiKey = wyzieApiKey
        )
    }

    suspend fun searchForMedia(
        media: OnlineMediaMatch,
        allMatches: List<OnlineMediaMatch>,
        season: Int? = null,
        episode: Int? = null,
        languageCode: String = "en",
        searchMode: String = "HYBRID",
        subdlApiKey: String = "",
        wyzieApiKey: String = ""
    ): OnlineSubtitleSearchOutcome = withContext(Dispatchers.IO) {
        val cleanLang = if (languageCode.trim().equals("all", ignoreCase = true)) "all" else normalizeLanguageCode(languageCode)
        fetchSubtitlesForSelection(
            cleanTitle = media.title,
            selectedMedia = media,
            allMatches = allMatches,
            season = season,
            episode = episode,
            requestedLang = cleanLang,
            searchMode = searchMode,
            subdlApiKey = subdlApiKey,
            wyzieApiKey = wyzieApiKey
        )
    }

    private suspend fun fetchSubtitlesForSelection(
        cleanTitle: String,
        selectedMedia: OnlineMediaMatch?,
        allMatches: List<OnlineMediaMatch>,
        season: Int?,
        episode: Int?,
        requestedLang: String,
        searchMode: String,
        subdlApiKey: String,
        wyzieApiKey: String
    ): OnlineSubtitleSearchOutcome = coroutineScope {
        val mode = searchMode.trim().uppercase(Locale.ROOT)
        val combined = mutableListOf<OnlineSubtitleResult>()
        val errors = mutableListOf<String>()
        var fellBackToAllLangs = false

        val imdbId = selectedMedia?.imdbId
        val tmdbId = selectedMedia?.tmdbId

        // 1. Keyless OpenSubtitles v3 (works out-of-the-box without any API key)
        if (!imdbId.isNullOrBlank()) {
            runCatching {
                searchOpenSubtitlesV3(
                    imdbId = imdbId,
                    mediaType = selectedMedia.mediaType,
                    season = season,
                    episode = episode,
                    requestedLang = requestedLang
                )
            }.onSuccess { (filtered, unfiltered) ->
                if (filtered.isNotEmpty()) {
                    combined += filtered
                } else if (requestedLang != "all" && unfiltered.isNotEmpty()) {
                    fellBackToAllLangs = true
                    combined += unfiltered
                }
            }.onFailure { errors += "OpenSubtitles: ${it.message ?: "unavailable"}" }
        }

        // 2. Wyzie (only if user configured a Wyzie API key)
        val effectiveWyzieId = imdbId ?: tmdbId ?: cleanTitle.takeIf { it.all(Char::isDigit) && it.length >= 2 }
        if ((mode == "WYZIE" || mode == "HYBRID") && wyzieApiKey.isNotBlank() && !effectiveWyzieId.isNullOrBlank()) {
            runCatching {
                searchWyzie(effectiveWyzieId, season, episode, requestedLang, wyzieApiKey.trim())
            }.onSuccess { combined += it }
                .onFailure { errors += "Wyzie: ${it.message ?: "unavailable"}" }
        }

        // 3. SubDL (when user configures optional SubDL API key)
        if ((mode == "SUBHUB" || mode == "HYBRID") && subdlApiKey.isNotBlank()) {
            runCatching {
                searchSubDl(cleanTitle, season, episode, requestedLang, subdlApiKey.trim())
            }.onSuccess { combined += it }
                .onFailure { errors += "SubDL: ${it.message ?: "unavailable"}" }
        }

        val deduped = combined.distinctBy { "${it.downloadUrl}_${it.languageCode}" }
        val targetName = selectedMedia?.let {
            if (it.year.isNotBlank()) "${it.title} (${it.year})" else it.title
        } ?: cleanTitle

        val status = when {
            deduped.isNotEmpty() && fellBackToAllLangs ->
                "No ${languageDisplayName(requestedLang)} subs yet for \"$targetName\"; showing ${deduped.size} available"
            deduped.isNotEmpty() ->
                "Found ${deduped.size} subtitle(s) for \"$targetName\""
            errors.isNotEmpty() -> errors.joinToString(" • ")
            else -> "No subtitles found for \"$targetName\""
        }

        OnlineSubtitleSearchOutcome(
            results = deduped,
            statusMessage = status,
            providerUsed = if (imdbId != null) "OpenSubtitles v3" else mode,
            error = if (deduped.isEmpty() && errors.isNotEmpty()) errors.joinToString("; ") else null,
            mediaMatches = allMatches,
            selectedMedia = selectedMedia
        )
    }

    suspend fun searchMediaCatalog(
        cleanTitle: String,
        preferredYear: Int? = null,
        isSeriesHint: Boolean = false
    ): List<OnlineMediaMatch> = coroutineScope {
        if (cleanTitle.isBlank()) return@coroutineScope emptyList()
        val encoded = URLEncoder.encode(cleanTitle, "UTF-8").replace("+", "%20")

        val movieJob = async(Dispatchers.IO) {
            runCatching { queryCinemetaCatalog("movie", encoded) }.getOrDefault(emptyList())
        }
        val seriesJob = async(Dispatchers.IO) {
            runCatching { queryCinemetaCatalog("series", encoded) }.getOrDefault(emptyList())
        }
        val tmdbJob = async(Dispatchers.IO) {
            runCatching { queryWyzieCatalog(cleanTitle) }.getOrDefault(emptyList())
        }

        val all = movieJob.await() + seriesJob.await() + tmdbJob.await()
        val normTarget = cleanTitle.lowercase(Locale.ROOT).trim()

        // Prefer entries that have an IMDb ID (works keyless on OpenSubtitles v3), plus exact title/year matches
        val merged = linkedMapOf<String, OnlineMediaMatch>()
        all.forEach { item ->
            val key = item.imdbId ?: "${item.title.lowercase(Locale.ROOT)}_${item.year}_${item.mediaType}"
            val existing = merged[key]
            if (existing == null) {
                merged[key] = item
            } else if (existing.posterUrl.isNullOrBlank() && !item.posterUrl.isNullOrBlank()) {
                merged[key] = existing.copy(posterUrl = item.posterUrl, tmdbId = existing.tmdbId ?: item.tmdbId)
            }
        }

        merged.values.sortedByDescending { item ->
            var score = 0
            val itemLower = item.title.lowercase(Locale.ROOT).trim()
            if (itemLower == normTarget) score += 100
            else if (itemLower.contains(normTarget)) score += 40
            if (preferredYear != null && item.year.startsWith(preferredYear.toString())) score += 80
            if (!item.imdbId.isNullOrBlank()) score += 50
            if (!item.posterUrl.isNullOrBlank()) score += 15
            if (isSeriesHint && item.mediaType == "series") score += 25
            if (!isSeriesHint && item.mediaType == "movie") score += 15
            score
        }.take(12)
    }

    private fun queryCinemetaCatalog(type: String, encodedTitle: String): List<OnlineMediaMatch> {
        val url = "$CINEMETA_BASE/catalog/$type/top/search=$encodedTitle.json"
        val req = Request.Builder()
            .url(url)
            .header("User-Agent", "MPVXPlayer/${BuildConfig.VERSION_NAME}")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            return parseCinemetaJson(resp.body.string(), type)
        }
    }

    private fun queryWyzieCatalog(title: String): List<OnlineMediaMatch> {
        val encoded = URLEncoder.encode(title, "UTF-8")
        val req = Request.Builder()
            .url("$WYZIE_BASE/api/tmdb/search?q=$encoded")
            .header("User-Agent", "MPVXPlayer/${BuildConfig.VERSION_NAME}")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return emptyList()
            return parseWyzieCatalogJson(resp.body.string())
        }
    }

    internal fun parseCinemetaJson(raw: String, fallbackType: String): List<OnlineMediaMatch> {
        val metas = JSONObject(raw).optJSONArray("metas") ?: return emptyList()
        val out = mutableListOf<OnlineMediaMatch>()
        for (i in 0 until metas.length()) {
            val obj = metas.optJSONObject(i) ?: continue
            val imdb = obj.optString("imdb_id").ifBlank { obj.optString("id") }.takeIf { it.startsWith("tt") }
                ?: continue
            val title = obj.optString("name").trim().ifBlank { continue }
            val year = obj.optString("releaseInfo").trim().take(4)
            val poster = obj.optString("poster").trim().ifBlank { null }
            val mType = obj.optString("type").ifBlank { fallbackType }
            out += OnlineMediaMatch(
                id = imdb,
                title = title,
                year = year,
                mediaType = if (mType == "series" || mType == "tv") "series" else "movie",
                imdbId = imdb,
                posterUrl = poster
            )
        }
        return out
    }

    internal fun parseWyzieCatalogJson(raw: String): List<OnlineMediaMatch> {
        val arr = JSONObject(raw).optJSONArray("results") ?: return emptyList()
        val out = mutableListOf<OnlineMediaMatch>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val tmdbId = obj.optInt("id", -1).takeIf { it > 0 }?.toString() ?: continue
            val title = obj.optString("title").ifBlank { obj.optString("name") }.trim().ifBlank { continue }
            val year = obj.optString("releaseYear").ifBlank {
                obj.optString("release_date").ifBlank { obj.optString("first_air_date") }.take(4)
            }
            val posterRaw = obj.optString("poster").ifBlank { obj.optString("poster_path") }.trim()
            val posterUrl = when {
                posterRaw.startsWith("http") -> posterRaw
                posterRaw.startsWith("/") -> "https://image.tmdb.org/t/p/w342$posterRaw"
                else -> null
            }
            val mType = obj.optString("mediaType").ifBlank { obj.optString("media_type") }
            out += OnlineMediaMatch(
                id = "tmdb:$tmdbId",
                title = title,
                year = year,
                mediaType = if (mType == "tv" || mType == "series") "series" else "movie",
                tmdbId = tmdbId,
                posterUrl = posterUrl
            )
        }
        return out
    }

    private fun searchOpenSubtitlesV3(
        imdbId: String,
        mediaType: String,
        season: Int?,
        episode: Int?,
        requestedLang: String
    ): Pair<List<OnlineSubtitleResult>, List<OnlineSubtitleResult>> {
        val cleanImdb = imdbId.trim().lowercase(Locale.ROOT)
        val path = if (mediaType == "series" || (season != null && episode != null)) {
            val s = season ?: 1
            val e = episode ?: 1
            "$OPENSUBS_V3_BASE/subtitles/series/$cleanImdb:$s:$e.json"
        } else {
            "$OPENSUBS_V3_BASE/subtitles/movie/$cleanImdb.json"
        }

        val req = Request.Builder()
            .url(path)
            .header("User-Agent", "MPVXPlayer/${BuildConfig.VERSION_NAME}")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            val body = resp.body.string()
            val all = parseOpenSubtitlesV3Json(body, "all")
            val filtered = if (requestedLang == "all") all else all.filter {
                it.languageCode.equals(requestedLang, ignoreCase = true)
            }
            return filtered to all
        }
    }

    internal fun parseOpenSubtitlesV3Json(raw: String, requestedLang: String): List<OnlineSubtitleResult> {
        val arr = JSONObject(raw).optJSONArray("subtitles") ?: return emptyList()
        val out = mutableListOf<OnlineSubtitleResult>()
        val targetLang = if (requestedLang.equals("all", ignoreCase = true)) "all" else normalizeLanguageCode(requestedLang)
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val url = obj.optString("url").trim()
            if (url.isEmpty()) continue
            val rawLang = obj.optString("lang").ifBlank { "eng" }
            val shortLang = normalizeLanguageCode(rawLang)
            if (targetLang != "all" && !shortLang.equals(targetLang, ignoreCase = true)) {
                continue
            }
            val fileName = obj.optString("subtitleFileName")
                .ifBlank { obj.optString("movieReleaseName") }
                .ifBlank { "OpenSubtitles_${i + 1}.srt" }
            val ext = fileName.substringAfterLast('.', "srt").lowercase(Locale.ROOT)
                .takeIf { it in setOf("srt", "ass", "ssa", "vtt", "sub") } ?: "srt"
            out += OnlineSubtitleResult(
                id = obj.optString("id").ifBlank { url.hashCode().toString() },
                title = fileName,
                languageCode = shortLang,
                languageName = languageDisplayName(shortLang),
                format = ext,
                downloadUrl = url,
                source = "OpenSubtitles",
                isHearingImpaired = obj.optString("m").equals("hi", ignoreCase = true),
                downloadCount = obj.optInt("g", 0)
            )
        }
        return out
    }

    private fun searchWyzie(
        idParam: String,
        season: Int?,
        episode: Int?,
        languageCode: String,
        apiKey: String
    ): List<OnlineSubtitleResult> {
        val urlBuilder = "$WYZIE_BASE/search".toHttpUrl().newBuilder()
            .addQueryParameter("id", idParam)
            .addQueryParameter("key", apiKey)
            .addQueryParameter("unzip", "true")
        if (season != null && episode != null) {
            urlBuilder.addQueryParameter("season", season.toString())
            urlBuilder.addQueryParameter("episode", episode.toString())
        }
        if (languageCode != "all") {
            urlBuilder.addQueryParameter("language", languageCode)
        }

        val req = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", "MPVXPlayer/${BuildConfig.VERSION_NAME}")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            val body = resp.body.string()
            if (resp.code == 400 && body.contains("No subtitles found", ignoreCase = true)) {
                return emptyList()
            }
            if (!resp.isSuccessful) {
                throw IllegalStateException("HTTP ${resp.code}")
            }
            return parseWyzieJson(body, languageCode)
        }
    }

    private fun searchSubDl(
        cleanTitle: String,
        season: Int?,
        episode: Int?,
        languageCode: String,
        apiKey: String
    ): List<OnlineSubtitleResult> {
        val urlBuilder = SUBDL_API.toHttpUrl().newBuilder()
            .addQueryParameter("api_key", apiKey)
            .addQueryParameter("film_name", cleanTitle)
        if (season != null) urlBuilder.addQueryParameter("season_number", season.toString())
        if (episode != null) urlBuilder.addQueryParameter("episode_number", episode.toString())
        if (languageCode != "all") urlBuilder.addQueryParameter("languages", languageCode.uppercase(Locale.ROOT))

        val req = Request.Builder()
            .url(urlBuilder.build())
            .header("User-Agent", "MPVXPlayer/${BuildConfig.VERSION_NAME}")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) throw IllegalStateException("HTTP ${resp.code}")
            return parseSubDlJson(resp.body.string())
        }
    }

    internal fun parseWyzieJson(raw: String, requestedLang: String): List<OnlineSubtitleResult> {
        val trimmed = raw.trim()
        if (!trimmed.startsWith("[")) return emptyList()
        val arr = JSONArray(trimmed)
        val out = mutableListOf<OnlineSubtitleResult>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val url = obj.optString("url").trim()
            if (url.isEmpty()) continue
            val lang = normalizeLanguageCode(obj.optString("language").ifBlank { requestedLang })
            val display = obj.optString("display").ifBlank { languageDisplayName(lang) }
            if (requestedLang != "all" &&
                !lang.equals(requestedLang, ignoreCase = true) &&
                !display.equals(languageDisplayName(requestedLang), ignoreCase = true)
            ) {
                continue
            }
            val title = obj.optString("fileName")
                .ifBlank { obj.optString("release") }
                .ifBlank { obj.optString("media") }
                .ifBlank { "Subtitle ${i + 1}" }
            out += OnlineSubtitleResult(
                id = obj.optString("id").ifBlank { url.hashCode().toString() },
                title = title,
                languageCode = lang,
                languageName = display,
                format = obj.optString("format").ifBlank { "srt" }.lowercase(Locale.ROOT),
                downloadUrl = url,
                source = "Wyzie/${obj.optString("source").ifBlank { "online" }}",
                isHearingImpaired = obj.optBoolean("isHearingImpaired", false),
                downloadCount = obj.optInt("downloadCount", 0)
            )
        }
        return out
    }

    internal fun parseSubDlJson(raw: String): List<OnlineSubtitleResult> {
        val root = JSONObject(raw)
        if (!root.optBoolean("status", true)) return emptyList()
        val arr = root.optJSONArray("subtitles") ?: return emptyList()
        val out = mutableListOf<OnlineSubtitleResult>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val relUrl = obj.optString("url").trim()
            if (relUrl.isEmpty()) continue
            val fullUrl = if (relUrl.startsWith("http")) relUrl else "$SUBDL_DL_PREFIX$relUrl"
            val lang = normalizeLanguageCode(obj.optString("lang", "en"))
            out += OnlineSubtitleResult(
                id = obj.optString("sd_id", fullUrl.hashCode().toString()),
                title = obj.optString("release_name").ifBlank { obj.optString("name", "SubDL Subtitle") },
                languageCode = lang,
                languageName = obj.optString("language", languageDisplayName(lang)),
                format = if (fullUrl.endsWith(".zip", ignoreCase = true)) "zip" else "srt",
                downloadUrl = fullUrl,
                source = "SubDL",
                isHearingImpaired = obj.optBoolean("hi", false)
            )
        }
        return out
    }

    internal fun extractSubtitleBytesIfArchive(rawBytes: ByteArray, hintExt: String): Pair<ByteArray, String> {
        if (rawBytes.size >= 4 && rawBytes[0] == 0x50.toByte() && rawBytes[1] == 0x4B.toByte()) {
            var bestBytes: ByteArray? = null
            var bestExt = "srt"
            ZipInputStream(ByteArrayInputStream(rawBytes)).use { zis ->
                var entry = zis.nextEntry
                while (entry != null) {
                    if (!entry.isDirectory) {
                        val ext = entry.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                        if (ext in setOf("srt", "ass", "ssa", "vtt", "ttml", "sub")) {
                            val data = zis.readBytes()
                            if (bestBytes == null || data.size > bestBytes!!.size) {
                                bestBytes = data
                                bestExt = ext
                            }
                        }
                    }
                    entry = zis.nextEntry
                }
            }
            if (bestBytes != null) return bestBytes!! to bestExt
            error("ZIP archive contains no supported subtitle file")
        }
        if (rawBytes.size >= 2 && rawBytes[0] == 0x1F.toByte() && rawBytes[1] == 0x8B.toByte()) {
            val unzipped = GZIPInputStream(ByteArrayInputStream(rawBytes)).use { it.readBytes() }
            val ext = hintExt.takeIf { it in setOf("srt", "ass", "ssa", "vtt") } ?: "srt"
            return unzipped to ext
        }
        val cleanExt = hintExt.lowercase(Locale.ROOT).takeIf { it in setOf("srt", "ass", "ssa", "vtt") } ?: "srt"
        return rawBytes to cleanExt
    }

    suspend fun downloadSubtitleToCache(
        context: Context,
        item: OnlineSubtitleResult
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val req = Request.Builder()
                .url(item.downloadUrl)
                .header("User-Agent", "MPVXPlayer/${BuildConfig.VERSION_NAME}")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) error("Download failed (HTTP ${resp.code})")
                val rawBytes = resp.body.bytes()
                if (rawBytes.isEmpty()) error("Downloaded subtitle file is empty")
                if (rawBytes.size > MAX_SUB_BYTES) error("Subtitle file exceeds 8 MB safety cap")

                val (subBytes, resolvedExt) = extractSubtitleBytesIfArchive(rawBytes, item.format)
                val dir = File(context.cacheDir, "online_subs").apply { mkdirs() }
                val safeName = item.title
                    .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
                    .take(48)
                    .ifBlank { "online_sub" }
                val outFile = File(dir, "${safeName}_${item.languageCode}.$resolvedExt")

                // Normalize non-UTF-8 encodings via chardet4j so accented/CJK/Indic captions render cleanly
                DecodedInputStreamReader(ByteArrayInputStream(subBytes), StandardCharsets.UTF_8).use { reader ->
                    val utf8Text = reader.readText()
                    outFile.writeText(utf8Text, StandardCharsets.UTF_8)
                }
                Uri.fromFile(outFile)
            }
        }
    }

    suspend fun translateCueCached(text: String, targetLang: String): String = withContext(Dispatchers.IO) {
        val clean = text.trim()
        if (clean.isEmpty()) return@withContext ""
        val lang = normalizeLanguageCode(targetLang)
        val cacheKey = "$lang|$clean"
        synchronized(translationCache) {
            translationCache[cacheKey]?.let { return@withContext it }
        }
        val translated = runCatching {
            val encoded = URLEncoder.encode(clean, "UTF-8")
            val url = "https://translate.googleapis.com/translate_a/single?client=gtx&sl=auto&tl=$lang&dt=t&q=$encoded"
            val req = Request.Builder()
                .url(url)
                .header("User-Agent", "Mozilla/5.0")
                .get()
                .build()
            translateClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use clean
                val outer = JSONArray(resp.body.string())
                val sentences = outer.optJSONArray(0) ?: return@use clean
                val sb = StringBuilder()
                for (i in 0 until sentences.length()) {
                    val part = sentences.optJSONArray(i)?.optString(0).orEmpty()
                    sb.append(part)
                }
                sb.toString().ifBlank { clean }
            }
        }.getOrDefault(clean)

        if (translated.isNotBlank()) {
            synchronized(translationCache) {
                translationCache[cacheKey] = translated
            }
        }
        translated
    }
}
