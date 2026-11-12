package com.cmstudio.player.subtitle

import android.content.Context
import android.net.Uri
import com.cmstudio.player.BuildConfig
import com.cmstudio.player.player.IntroOutroManager
import com.sigpwned.chardet4j.Chardet
import com.sigpwned.chardet4j.io.DecodedInputStreamReader
import kotlinx.coroutines.Dispatchers
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
    val error: String? = null
)

/**
 * Clean-room Online Subtitle search & downloader supporting:
 * - Wyzie (`https://sub.wyzie.io`, open keyless subtitle API)
 * - SubDL (`https://api.subdl.com`, when user supplies an optional SubDL API key)
 * - Hybrid mode combining available providers with transparent status & failure handling.
 */
object OnlineSubtitleClient {

    private const val WYZIE_BASE = "https://sub.wyzie.io"
    private const val SUBDL_API = "https://api.subdl.com/api/v1/subtitles"
    private const val SUBDL_DL_PREFIX = "https://dl.subdl.com"
    private const val MAX_SUB_BYTES = 4 * 1024 * 1024

    val SUPPORTED_LANGUAGES = listOf(
        "all" to "All",
        "en" to "English",
        "hi" to "Hindi",
        "es" to "Spanish",
        "fr" to "French",
        "de" to "German",
        "ja" to "Japanese",
        "ko" to "Korean",
        "pt" to "Portuguese",
        "ar" to "Arabic",
        "ru" to "Russian",
        "zh" to "Chinese",
        "id" to "Indonesian"
    )

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()
    }

    suspend fun search(
        query: String,
        languageCode: String = "en",
        searchMode: String = "HYBRID", // WYZIE, SUBHUB, HYBRID
        subdlApiKey: String = ""
    ): OnlineSubtitleSearchOutcome = withContext(Dispatchers.IO) {
        val rawQuery = query.trim()
        if (rawQuery.isEmpty()) {
            return@withContext OnlineSubtitleSearchOutcome(emptyList(), "Enter a title or episode to search.", searchMode, "Empty query")
        }

        val parsed = IntroOutroManager.parseEpisodeInfo(rawQuery)
        val cleanLang = languageCode.trim().lowercase(Locale.ROOT).ifBlank { "en" }
        val mode = searchMode.trim().uppercase(Locale.ROOT)
        val combined = mutableListOf<OnlineSubtitleResult>()
        val errors = mutableListOf<String>()

        if (mode == "WYZIE" || mode == "HYBRID" || (mode == "SUBHUB" && subdlApiKey.isBlank())) {
            runCatching {
                searchWyzie(parsed.cleanTitle, parsed.imdbId, parsed.season, parsed.episode, cleanLang)
            }.onSuccess { combined += it }
                .onFailure { errors += "Wyzie: ${it.message ?: "unavailable"}" }
        }

        if ((mode == "SUBHUB" || mode == "HYBRID") && subdlApiKey.isNotBlank()) {
            runCatching {
                searchSubDl(parsed.cleanTitle, parsed.season, parsed.episode, cleanLang, subdlApiKey.trim())
            }.onSuccess { combined += it }
                .onFailure { errors += "SubDL: ${it.message ?: "unavailable"}" }
        }

        val deduped = combined.distinctBy { "${it.downloadUrl}_${it.languageCode}" }
        val status = when {
            deduped.isNotEmpty() -> "Found ${deduped.size} subtitle(s) for \"${parsed.cleanTitle}\""
            errors.isNotEmpty() -> errors.joinToString(" • ")
            else -> "No subtitles found for \"${parsed.cleanTitle}\""
        }
        OnlineSubtitleSearchOutcome(
            results = deduped,
            statusMessage = status,
            providerUsed = mode,
            error = if (deduped.isEmpty() && errors.isNotEmpty()) errors.joinToString("; ") else null
        )
    }

    private fun searchWyzie(
        cleanTitle: String,
        imdbId: String?,
        season: Int?,
        episode: Int?,
        languageCode: String
    ): List<OnlineSubtitleResult> {
        val idParam = when {
            !imdbId.isNullOrBlank() -> imdbId
            cleanTitle.all { it.isDigit() } && cleanTitle.length >= 2 -> cleanTitle
            else -> lookupWyzieTmdbId(cleanTitle) ?: return emptyList()
        }

        val urlBuilder = "$WYZIE_BASE/search".toHttpUrl().newBuilder()
            .addQueryParameter("id", idParam)
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

    private fun lookupWyzieTmdbId(title: String): String? {
        val encoded = URLEncoder.encode(title, "UTF-8")
        val req = Request.Builder()
            .url("$WYZIE_BASE/api/tmdb/search?q=$encoded")
            .header("User-Agent", "MPVXPlayer/${BuildConfig.VERSION_NAME}")
            .get()
            .build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) return null
            val arr = JSONObject(resp.body.string()).optJSONArray("results") ?: return null
            if (arr.length() == 0) return null
            return arr.getJSONObject(0).optInt("id", -1).takeIf { it > 0 }?.toString()
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
            val lang = obj.optString("language").ifBlank { requestedLang }.lowercase(Locale.ROOT)
            val display = obj.optString("display").ifBlank {
                SUPPORTED_LANGUAGES.firstOrNull { it.first == lang }?.second ?: lang.uppercase(Locale.ROOT)
            }
            if (requestedLang != "all" &&
                !lang.equals(requestedLang, ignoreCase = true) &&
                !display.equals(SUPPORTED_LANGUAGES.firstOrNull { it.first == requestedLang }?.second.orEmpty(), ignoreCase = true)
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
            val lang = obj.optString("lang", "en").lowercase(Locale.ROOT)
            out += OnlineSubtitleResult(
                id = obj.optString("sd_id", fullUrl.hashCode().toString()),
                title = obj.optString("release_name").ifBlank { obj.optString("name", "SubDL Subtitle") },
                languageCode = lang,
                languageName = obj.optString("language", lang.uppercase(Locale.ROOT)),
                format = "srt",
                downloadUrl = fullUrl,
                source = "SubDL",
                isHearingImpaired = obj.optBoolean("hi", false)
            )
        }
        return out
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
                val bytes = resp.body.bytes()
                if (bytes.isEmpty()) error("Downloaded subtitle file is empty")
                if (bytes.size > MAX_SUB_BYTES) error("Subtitle file exceeds 4 MB safety cap")

                val dir = File(context.cacheDir, "online_subs").apply { mkdirs() }
                val ext = item.format.lowercase(Locale.ROOT).takeIf { it in setOf("srt", "ass", "ssa", "vtt") } ?: "srt"
                val safeName = item.title
                    .replace(Regex("""[^A-Za-z0-9._-]"""), "_")
                    .take(48)
                    .ifBlank { "online_sub" }
                val outFile = File(dir, "${safeName}_${item.languageCode}.$ext")

                // Normalize non-UTF-8 encodings via chardet4j so accented/CJK captions render cleanly
                DecodedInputStreamReader(ByteArrayInputStream(bytes), StandardCharsets.UTF_8).use { reader ->
                    val utf8Text = reader.readText()
                    outFile.writeText(utf8Text, StandardCharsets.UTF_8)
                }
                Uri.fromFile(outFile)
            }
        }
    }
}
