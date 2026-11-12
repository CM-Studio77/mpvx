package com.cmstudio.player.ui.servers

import com.cmstudio.player.BuildConfig
import com.cmstudio.player.ui.network.NetworkPlaybackHeaders
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import java.util.UUID
import java.util.concurrent.TimeUnit

enum class MediaServerKind(val id: String, val label: String) {
    JELLYFIN("JELLYFIN", "Jellyfin"),
    NAVIDROME("NAVIDROME", "Navidrome"),
    AUDIOBOOKSHELF("AUDIOBOOKSHELF", "Audiobookshelf")
}

data class MediaServerProfile(
    val id: String,
    val kind: MediaServerKind,
    val name: String,
    val baseUrl: String,
    val username: String,
    val userId: String = ""
)

sealed class ServerCallResult<out T> {
    data class Ok<T>(val data: T) : ServerCallResult<T>()
    data class Err(val message: String, val authExpired: Boolean = false) : ServerCallResult<Nothing>()
}

data class JellyfinMediaItem(
    val id: String,
    val name: String,
    val type: String, // CollectionFolder, Folder, Movie, Episode, Series, Audio
    val isFolder: Boolean,
    val overview: String = "",
    val runTimeMs: Long = 0L,
    val resumePositionMs: Long = 0L,
    val artworkUrl: String? = null,
    val streamUrl: String? = null
)

data class NavidromeAlbumItem(
    val id: String,
    val name: String,
    val artist: String,
    val songCount: Int = 0,
    val durationSec: Int = 0,
    val coverArtUrl: String? = null
)

data class NavidromeArtistItem(
    val id: String,
    val name: String,
    val albumCount: Int = 0
)

data class NavidromeSongItem(
    val id: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val streamUrl: String,
    val coverArtUrl: String? = null
)

data class NavidromePlaylistItem(
    val id: String,
    val name: String,
    val songCount: Int
)

data class AbsLibraryItem(
    val id: String,
    val name: String,
    val mediaType: String
)

data class AbsChapter(
    val title: String,
    val startMs: Long,
    val endMs: Long
)

data class AbsBookItem(
    val id: String,
    val title: String,
    val author: String,
    val durationMs: Long,
    val currentTimeMs: Long = 0L,
    val coverUrl: String? = null,
    val streamUrl: String? = null,
    val chapters: List<AbsChapter> = emptyList()
)

object MediaServerUrlUtils {
    fun normalizeBaseUrl(raw: String): String {
        val clean = raw.trim().trimEnd('/')
        if (clean.isEmpty()) return ""
        return if (clean.startsWith("http://", true) || clean.startsWith("https://", true)) {
            clean
        } else {
            "http://$clean"
        }
    }

    fun md5Hex(input: String): String {
        val digest = MessageDigest.getInstance("MD5").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}

/**
 * Genuine Jellyfin REST client (Auth, Library Views, Items, Artwork, Direct Stream URLs).
 */
object JellyfinClient {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .build()
    }

    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()
    private val DEVICE_ID = "mpvx-${UUID.nameUUIDFromBytes("mpvx-android".toByteArray())}"

    private fun authHeaderValue(token: String? = null): String {
        val base = """MediaBrowser Client="MPVX Player", Device="Android", DeviceId="$DEVICE_ID", Version="${BuildConfig.VERSION_NAME}""""
        return if (!token.isNullOrBlank()) """$base, Token="$token"""" else base
    }

    suspend fun authenticate(
        baseUrl: String,
        username: String,
        password: String
    ): ServerCallResult<Pair<String, String>> = withContext(Dispatchers.IO) {
        val cleanUrl = MediaServerUrlUtils.normalizeBaseUrl(baseUrl)
        if (cleanUrl.isEmpty()) return@withContext ServerCallResult.Err("Server URL required")
        try {
            val payload = JSONObject()
                .put("Username", username)
                .put("Pw", password)
                .toString()
            val req = Request.Builder()
                .url("$cleanUrl/Users/AuthenticateByName")
                .header("X-Emby-Authorization", authHeaderValue())
                .post(payload.toRequestBody(JSON_TYPE))
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) {
                    return@withContext ServerCallResult.Err("Invalid Jellyfin credentials (HTTP ${resp.code})", authExpired = true)
                }
                if (!resp.isSuccessful) {
                    return@withContext ServerCallResult.Err("Jellyfin login failed (HTTP ${resp.code})")
                }
                val obj = JSONObject(resp.body.string())
                val token = obj.optString("AccessToken")
                val userId = obj.optJSONObject("User")?.optString("Id").orEmpty()
                if (token.isBlank() || userId.isBlank()) {
                    return@withContext ServerCallResult.Err("Unexpected Jellyfin auth response")
                }
                NetworkPlaybackHeaders.registerForUrl(cleanUrl, mapOf("X-Emby-Token" to token))
                ServerCallResult.Ok(token to userId)
            }
        } catch (e: Exception) {
            ServerCallResult.Err(e.localizedMessage ?: "Connection failed")
        }
    }

    suspend fun fetchItems(
        baseUrl: String,
        userId: String,
        token: String,
        parentId: String? = null
    ): ServerCallResult<List<JellyfinMediaItem>> = withContext(Dispatchers.IO) {
        val cleanUrl = MediaServerUrlUtils.normalizeBaseUrl(baseUrl)
        if (cleanUrl.isEmpty() || token.isBlank()) {
            return@withContext ServerCallResult.Err("Authentication required", authExpired = true)
        }
        try {
            val url = if (parentId.isNullOrBlank()) {
                "$cleanUrl/Users/$userId/Views"
            } else {
                "$cleanUrl/Users/$userId/Items".toHttpUrl().newBuilder()
                    .addQueryParameter("ParentId", parentId)
                    .addQueryParameter("Fields", "Overview,PrimaryImageAspectRatio,UserData")
                    .addQueryParameter("SortBy", "IsFolder,SortName")
                    .addQueryParameter("SortOrder", "Ascending")
                    .build()
                    .toString()
            }
            val req = Request.Builder()
                .url(url)
                .header("X-Emby-Token", token)
                .header("X-Emby-Authorization", authHeaderValue(token))
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) {
                    return@withContext ServerCallResult.Err("Session expired (HTTP ${resp.code}). Please sign in again.", authExpired = true)
                }
                if (!resp.isSuccessful) {
                    return@withContext ServerCallResult.Err("Jellyfin returned HTTP ${resp.code}")
                }
                NetworkPlaybackHeaders.registerForUrl(cleanUrl, mapOf("X-Emby-Token" to token))
                ServerCallResult.Ok(parseItemsJson(resp.body.string(), cleanUrl, token))
            }
        } catch (e: Exception) {
            ServerCallResult.Err(e.localizedMessage ?: "Failed to browse Jellyfin")
        }
    }

    internal fun parseItemsJson(raw: String, baseUrl: String, token: String): List<JellyfinMediaItem> {
        val arr = JSONObject(raw).optJSONArray("Items") ?: return emptyList()
        val out = mutableListOf<JellyfinMediaItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("Id")
            if (id.isBlank()) continue
            val type = obj.optString("Type", "Folder")
            val isFolder = obj.optBoolean("IsFolder", type in setOf("CollectionFolder", "Folder", "Series", "Season"))
            val ticks = obj.optLong("RunTimeTicks", 0L)
            val resumeTicks = obj.optJSONObject("UserData")?.optLong("PlaybackPositionTicks", 0L) ?: 0L
            val art = "$baseUrl/Items/$id/Images/Primary?maxWidth=480&api_key=$token"
            val stream = if (!isFolder) {
                if (type.equals("Audio", ignoreCase = true)) {
                    "$baseUrl/Audio/$id/stream?static=true&api_key=$token"
                } else {
                    "$baseUrl/Videos/$id/stream?static=true&MediaSourceId=$id&api_key=$token"
                }
            } else null
            out += JellyfinMediaItem(
                id = id,
                name = obj.optString("Name", "Media"),
                type = type,
                isFolder = isFolder,
                overview = obj.optString("Overview"),
                runTimeMs = ticks / 10_000L,
                resumePositionMs = resumeTicks / 10_000L,
                artworkUrl = art,
                streamUrl = stream
            )
        }
        return out
    }
}

/**
 * Genuine Navidrome / Subsonic / OpenSubsonic client with salted MD5 token auth.
 */
object NavidromeClient {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    private fun subsonicUrl(
        baseUrl: String,
        endpoint: String,
        username: String,
        password: String,
        params: Map<String, String> = emptyMap()
    ): String {
        val cleanUrl = MediaServerUrlUtils.normalizeBaseUrl(baseUrl)
        val salt = UUID.randomUUID().toString().take(8)
        val token = MediaServerUrlUtils.md5Hex(password + salt)
        return "$cleanUrl/rest/$endpoint".toHttpUrl().newBuilder()
            .addQueryParameter("u", username)
            .addQueryParameter("t", token)
            .addQueryParameter("s", salt)
            .addQueryParameter("v", "1.16.1")
            .addQueryParameter("c", "MPVXPlayer")
            .addQueryParameter("f", "json")
            .apply { params.forEach { (k, v) -> addQueryParameter(k, v) } }
            .build()
            .toString()
    }

    suspend fun ping(baseUrl: String, username: String, password: String): ServerCallResult<Boolean> =
        withContext(Dispatchers.IO) {
            try {
                val url = subsonicUrl(baseUrl, "ping.view", username, password)
                client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                    if (!resp.isSuccessful) return@withContext ServerCallResult.Err("HTTP ${resp.code}")
                    val sub = JSONObject(resp.body.string()).optJSONObject("subsonic-response")
                        ?: return@withContext ServerCallResult.Err("Invalid Subsonic response")
                    if (sub.optString("status") == "ok") {
                        ServerCallResult.Ok(true)
                    } else {
                        val err = sub.optJSONObject("error")
                        val code = err?.optInt("code", 0) ?: 0
                        ServerCallResult.Err(err?.optString("message") ?: "Navidrome auth failed", authExpired = code == 40)
                    }
                }
            } catch (e: Exception) {
                ServerCallResult.Err(e.localizedMessage ?: "Navidrome connection failed")
            }
        }

    suspend fun fetchAlbums(
        baseUrl: String,
        username: String,
        password: String,
        type: String = "newest"
    ): ServerCallResult<List<NavidromeAlbumItem>> = withContext(Dispatchers.IO) {
        try {
            val url = subsonicUrl(baseUrl, "getAlbumList2.view", username, password, mapOf("type" to type, "size" to "40"))
            client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext ServerCallResult.Err("HTTP ${resp.code}")
                val body = resp.body.string()
                val sub = JSONObject(body).optJSONObject("subsonic-response")
                    ?: return@withContext ServerCallResult.Err("Invalid response")
                if (sub.optString("status") != "ok") {
                    val err = sub.optJSONObject("error")?.optString("message") ?: "Auth failed"
                    return@withContext ServerCallResult.Err(err, authExpired = true)
                }
                ServerCallResult.Ok(parseAlbumsJson(body, baseUrl, username, password))
            }
        } catch (e: Exception) {
            ServerCallResult.Err(e.localizedMessage ?: "Failed to load albums")
        }
    }

    suspend fun fetchAlbumSongs(
        baseUrl: String,
        username: String,
        password: String,
        albumId: String
    ): ServerCallResult<List<NavidromeSongItem>> = withContext(Dispatchers.IO) {
        try {
            val url = subsonicUrl(baseUrl, "getAlbum.view", username, password, mapOf("id" to albumId))
            client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext ServerCallResult.Err("HTTP ${resp.code}")
                ServerCallResult.Ok(parseSongsJson(resp.body.string(), "album", "song", baseUrl, username, password))
            }
        } catch (e: Exception) {
            ServerCallResult.Err(e.localizedMessage ?: "Failed to load album tracks")
        }
    }

    suspend fun fetchArtists(
        baseUrl: String,
        username: String,
        password: String
    ): ServerCallResult<List<NavidromeArtistItem>> = withContext(Dispatchers.IO) {
        try {
            val url = subsonicUrl(baseUrl, "getArtists.view", username, password)
            client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext ServerCallResult.Err("HTTP ${resp.code}")
                val sub = JSONObject(resp.body.string()).optJSONObject("subsonic-response")
                val indices = sub?.optJSONObject("artists")?.optJSONArray("index") ?: JSONArray()
                val out = mutableListOf<NavidromeArtistItem>()
                for (i in 0 until indices.length()) {
                    val artists = indices.optJSONObject(i)?.optJSONArray("artist") ?: continue
                    for (j in 0 until artists.length()) {
                        val a = artists.optJSONObject(j) ?: continue
                        out += NavidromeArtistItem(
                            id = a.optString("id"),
                            name = a.optString("name", "Artist"),
                            albumCount = a.optInt("albumCount", 0)
                        )
                    }
                }
                ServerCallResult.Ok(out)
            }
        } catch (e: Exception) {
            ServerCallResult.Err(e.localizedMessage ?: "Failed to load artists")
        }
    }

    suspend fun fetchPlaylists(
        baseUrl: String,
        username: String,
        password: String
    ): ServerCallResult<List<NavidromePlaylistItem>> = withContext(Dispatchers.IO) {
        try {
            val url = subsonicUrl(baseUrl, "getPlaylists.view", username, password)
            client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext ServerCallResult.Err("HTTP ${resp.code}")
                val sub = JSONObject(resp.body.string()).optJSONObject("subsonic-response")
                val arr = sub?.optJSONObject("playlists")?.optJSONArray("playlist") ?: JSONArray()
                val out = mutableListOf<NavidromePlaylistItem>()
                for (i in 0 until arr.length()) {
                    val p = arr.optJSONObject(i) ?: continue
                    out += NavidromePlaylistItem(
                        id = p.optString("id"),
                        name = p.optString("name", "Playlist"),
                        songCount = p.optInt("songCount", 0)
                    )
                }
                ServerCallResult.Ok(out)
            }
        } catch (e: Exception) {
            ServerCallResult.Err(e.localizedMessage ?: "Failed to load playlists")
        }
    }

    suspend fun fetchPlaylistSongs(
        baseUrl: String,
        username: String,
        password: String,
        playlistId: String
    ): ServerCallResult<List<NavidromeSongItem>> = withContext(Dispatchers.IO) {
        try {
            val url = subsonicUrl(baseUrl, "getPlaylist.view", username, password, mapOf("id" to playlistId))
            client.newCall(Request.Builder().url(url).get().build()).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext ServerCallResult.Err("HTTP ${resp.code}")
                ServerCallResult.Ok(parseSongsJson(resp.body.string(), "playlist", "entry", baseUrl, username, password))
            }
        } catch (e: Exception) {
            ServerCallResult.Err(e.localizedMessage ?: "Failed to load playlist")
        }
    }

    internal fun parseAlbumsJson(
        raw: String,
        baseUrl: String,
        username: String,
        password: String
    ): List<NavidromeAlbumItem> {
        val sub = JSONObject(raw).optJSONObject("subsonic-response") ?: return emptyList()
        val arr = sub.optJSONObject("albumList2")?.optJSONArray("album") ?: return emptyList()
        val out = mutableListOf<NavidromeAlbumItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id")
            val coverId = obj.optString("coverArt", id)
            out += NavidromeAlbumItem(
                id = id,
                name = obj.optString("name", "Album"),
                artist = obj.optString("artist", "Unknown Artist"),
                songCount = obj.optInt("songCount", 0),
                durationSec = obj.optInt("duration", 0),
                coverArtUrl = coverId.takeIf { it.isNotBlank() }?.let {
                    subsonicUrl(baseUrl, "getCoverArt.view", username, password, mapOf("id" to it, "size" to "320"))
                }
            )
        }
        return out
    }

    internal fun parseSongsJson(
        raw: String,
        parentObjKey: String,
        arrayKey: String,
        baseUrl: String,
        username: String,
        password: String
    ): List<NavidromeSongItem> {
        val sub = JSONObject(raw).optJSONObject("subsonic-response") ?: return emptyList()
        val arr = sub.optJSONObject(parentObjKey)?.optJSONArray(arrayKey) ?: return emptyList()
        val out = mutableListOf<NavidromeSongItem>()
        for (i in 0 until arr.length()) {
            val obj = arr.optJSONObject(i) ?: continue
            val id = obj.optString("id")
            if (id.isBlank()) continue
            val coverId = obj.optString("coverArt")
            out += NavidromeSongItem(
                id = id,
                title = obj.optString("title", "Track"),
                artist = obj.optString("artist", "Unknown Artist"),
                album = obj.optString("album", ""),
                durationMs = obj.optLong("duration", 0L) * 1000L,
                streamUrl = subsonicUrl(baseUrl, "stream.view", username, password, mapOf("id" to id)),
                coverArtUrl = coverId.takeIf { it.isNotBlank() }?.let {
                    subsonicUrl(baseUrl, "getCoverArt.view", username, password, mapOf("id" to it, "size" to "320"))
                }
            )
        }
        return out
    }
}

/**
 * Genuine Audiobookshelf REST client (Login, Libraries, Books, Chapters, Resume & Progress Sync).
 */
object AudiobookshelfClient {

    private val client by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(18, TimeUnit.SECONDS)
            .build()
    }
    private val JSON_TYPE = "application/json; charset=utf-8".toMediaType()

    suspend fun login(
        baseUrl: String,
        username: String,
        password: String
    ): ServerCallResult<Pair<String, String>> = withContext(Dispatchers.IO) {
        val cleanUrl = MediaServerUrlUtils.normalizeBaseUrl(baseUrl)
        if (cleanUrl.isEmpty()) return@withContext ServerCallResult.Err("Server URL required")
        try {
            val body = JSONObject().put("username", username).put("password", password).toString()
            val req = Request.Builder()
                .url("$cleanUrl/login")
                .post(body.toRequestBody(JSON_TYPE))
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) {
                    return@withContext ServerCallResult.Err("Invalid Audiobookshelf credentials", authExpired = true)
                }
                if (!resp.isSuccessful) {
                    return@withContext ServerCallResult.Err("Audiobookshelf login failed (HTTP ${resp.code})")
                }
                val user = JSONObject(resp.body.string()).optJSONObject("user")
                    ?: return@withContext ServerCallResult.Err("Missing user payload")
                val token = user.optString("token")
                val userId = user.optString("id")
                if (token.isBlank()) return@withContext ServerCallResult.Err("Missing access token")
                NetworkPlaybackHeaders.registerForUrl(cleanUrl, mapOf("Authorization" to "Bearer $token"))
                ServerCallResult.Ok(token to userId)
            }
        } catch (e: Exception) {
            ServerCallResult.Err(e.localizedMessage ?: "Audiobookshelf connection failed")
        }
    }

    suspend fun fetchLibraries(baseUrl: String, token: String): ServerCallResult<List<AbsLibraryItem>> =
        withContext(Dispatchers.IO) {
            val cleanUrl = MediaServerUrlUtils.normalizeBaseUrl(baseUrl)
            try {
                val req = Request.Builder()
                    .url("$cleanUrl/api/libraries")
                    .header("Authorization", "Bearer $token")
                    .get()
                    .build()
                client.newCall(req).execute().use { resp ->
                    if (resp.code == 401 || resp.code == 403) {
                        return@withContext ServerCallResult.Err("Session expired", authExpired = true)
                    }
                    if (!resp.isSuccessful) return@withContext ServerCallResult.Err("HTTP ${resp.code}")
                    val arr = JSONObject(resp.body.string()).optJSONArray("libraries") ?: JSONArray()
                    val out = mutableListOf<AbsLibraryItem>()
                    for (i in 0 until arr.length()) {
                        val lib = arr.optJSONObject(i) ?: continue
                        out += AbsLibraryItem(
                            id = lib.optString("id"),
                            name = lib.optString("name", "Library"),
                            mediaType = lib.optString("mediaType", "book")
                        )
                    }
                    ServerCallResult.Ok(out)
                }
            } catch (e: Exception) {
                ServerCallResult.Err(e.localizedMessage ?: "Failed to load libraries")
            }
        }

    suspend fun fetchLibraryBooks(
        baseUrl: String,
        token: String,
        libraryId: String
    ): ServerCallResult<List<AbsBookItem>> = withContext(Dispatchers.IO) {
        val cleanUrl = MediaServerUrlUtils.normalizeBaseUrl(baseUrl)
        try {
            val req = Request.Builder()
                .url("$cleanUrl/api/libraries/$libraryId/items?limit=60")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) {
                    return@withContext ServerCallResult.Err("Session expired", authExpired = true)
                }
                if (!resp.isSuccessful) return@withContext ServerCallResult.Err("HTTP ${resp.code}")
                NetworkPlaybackHeaders.registerForUrl(cleanUrl, mapOf("Authorization" to "Bearer $token"))
                ServerCallResult.Ok(parseBooksJson(resp.body.string(), cleanUrl, token))
            }
        } catch (e: Exception) {
            ServerCallResult.Err(e.localizedMessage ?: "Failed to load audiobooks")
        }
    }

    suspend fun fetchBookDetail(
        baseUrl: String,
        token: String,
        itemId: String
    ): ServerCallResult<AbsBookItem> = withContext(Dispatchers.IO) {
        val cleanUrl = MediaServerUrlUtils.normalizeBaseUrl(baseUrl)
        try {
            val req = Request.Builder()
                .url("$cleanUrl/api/items/$itemId?expanded=1&include=progress")
                .header("Authorization", "Bearer $token")
                .get()
                .build()
            client.newCall(req).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) {
                    return@withContext ServerCallResult.Err("Session expired", authExpired = true)
                }
                if (!resp.isSuccessful) return@withContext ServerCallResult.Err("HTTP ${resp.code}")
                NetworkPlaybackHeaders.registerForUrl(cleanUrl, mapOf("Authorization" to "Bearer $token"))
                ServerCallResult.Ok(parseSingleBookDetail(JSONObject(resp.body.string()), cleanUrl, token))
            }
        } catch (e: Exception) {
            ServerCallResult.Err(e.localizedMessage ?: "Failed to load book details")
        }
    }

    suspend fun syncProgress(
        baseUrl: String,
        token: String,
        itemId: String,
        currentTimeSec: Double,
        durationSec: Double
    ): Boolean = withContext(Dispatchers.IO) {
        val cleanUrl = MediaServerUrlUtils.normalizeBaseUrl(baseUrl)
        runCatching {
            val payload = JSONObject()
                .put("currentTime", currentTimeSec.coerceAtLeast(0.0))
                .put("duration", durationSec.coerceAtLeast(0.0))
                .put("progress", if (durationSec > 0.0) (currentTimeSec / durationSec).coerceIn(0.0, 1.0) else 0.0)
                .toString()
            val req = Request.Builder()
                .url("$cleanUrl/api/me/progress/$itemId")
                .header("Authorization", "Bearer $token")
                .patch(payload.toRequestBody(JSON_TYPE))
                .build()
            client.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    internal fun parseBooksJson(raw: String, baseUrl: String, token: String): List<AbsBookItem> {
        val arr = JSONObject(raw).optJSONArray("results") ?: return emptyList()
        val out = mutableListOf<AbsBookItem>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            out += parseSingleBookDetail(item, baseUrl, token)
        }
        return out
    }

    internal fun parseSingleBookDetail(item: JSONObject, baseUrl: String, token: String): AbsBookItem {
        val id = item.optString("id")
        val media = item.optJSONObject("media") ?: JSONObject()
        val meta = media.optJSONObject("metadata") ?: JSONObject()
        val durationSec = media.optDouble("duration", 0.0)
        val userProg = item.optJSONObject("userMediaProgress")
        val currSec = userProg?.optDouble("currentTime", 0.0) ?: 0.0

        val chaptersJson = media.optJSONArray("chapters") ?: JSONArray()
        val chapters = mutableListOf<AbsChapter>()
        for (i in 0 until chaptersJson.length()) {
            val ch = chaptersJson.optJSONObject(i) ?: continue
            val start = (ch.optDouble("start", 0.0) * 1000.0).toLong()
            val end = (ch.optDouble("end", 0.0) * 1000.0).toLong()
            chapters += AbsChapter(ch.optString("title", "Chapter ${i + 1}"), start, end)
        }

        val tracks = media.optJSONArray("tracks") ?: JSONArray()
        val firstTrackUrl = tracks.optJSONObject(0)?.optString("contentUrl")?.takeIf { it.isNotBlank() }?.let { rel ->
            if (rel.startsWith("http")) rel else "$baseUrl${if (rel.startsWith("/")) rel else "/$rel"}?token=$token"
        } ?: media.optJSONArray("audioFiles")?.optJSONObject(0)?.optString("ino")?.takeIf { it.isNotBlank() }?.let { ino ->
            "$baseUrl/api/items/$id/file/$ino?token=$token"
        }

        return AbsBookItem(
            id = id,
            title = meta.optString("title", "Audiobook"),
            author = meta.optString("authorName", "Unknown Author"),
            durationMs = (durationSec * 1000.0).toLong(),
            currentTimeMs = (currSec * 1000.0).toLong(),
            coverUrl = "$baseUrl/api/items/$id/cover?token=$token",
            streamUrl = firstTrackUrl,
            chapters = chapters
        )
    }
}
