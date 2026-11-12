package com.cmstudio.player.ui.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.model.NetworkRemoteEntry
import com.cmstudio.player.ui.model.NetworkShareConfig
import com.hierynomus.msfscc.FileAttributes
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.io.StringReader
import java.net.URLDecoder
import java.util.Locale
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

sealed class NetworkBrowseResult {
    data class Success(val entries: List<NetworkRemoteEntry>, val resolvedUrl: String) : NetworkBrowseResult()
    data class Failure(val message: String, val isAuthError: Boolean = false) : NetworkBrowseResult()
}

data class NetworkConnectionTestResult(
    val ok: Boolean,
    val statusText: String,
    val latencyMs: Long
)

/**
 * Genuine HTTP / WebDAV / M3U / SMB / FTP / SFTP browser and connection tester.
 */
object NetworkBrowserClient {

    private val HREF_PATTERN = Pattern.compile("""href\s*=\s*["']([^"']+)["']""", Pattern.CASE_INSENSITIVE)

    fun isWifiConnected(context: Context): Boolean {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager ?: return true
        val network = cm.activeNetwork ?: return false
        val caps = cm.getNetworkCapabilities(network) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
    }

    suspend fun testConnection(
        context: Context,
        share: NetworkShareConfig,
        password: String
    ): NetworkConnectionTestResult = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        when (val res = browse(context, share, share.baseUrl, password)) {
            is NetworkBrowseResult.Success -> {
                val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                NetworkConnectionTestResult(
                    ok = true,
                    statusText = "Connected (${res.entries.size} items, ${elapsed}ms)",
                    latencyMs = elapsed
                )
            }
            is NetworkBrowseResult.Failure -> {
                val elapsed = (System.currentTimeMillis() - start).coerceAtLeast(1L)
                NetworkConnectionTestResult(
                    ok = false,
                    statusText = res.message,
                    latencyMs = elapsed
                )
            }
        }
    }

    suspend fun resolvePlaybackUrl(
        share: NetworkShareConfig,
        entryUrl: String,
        password: String
    ): String = withContext(Dispatchers.IO) {
        val proto = share.protocol.trim().uppercase(Locale.ROOT)
        if (proto !in setOf("SMB", "FTP", "SFTP") &&
            !entryUrl.startsWith("smb://", ignoreCase = true) &&
            !entryUrl.startsWith("ftp://", ignoreCase = true) &&
            !entryUrl.startsWith("sftp://", ignoreCase = true)
        ) {
            return@withContext entryUrl
        }

        val uri = runCatching { java.net.URI(entryUrl.trim()) }.getOrNull()
        val host = uri?.host ?: return@withContext entryUrl
        val defaultPort = when (proto) {
            "SMB" -> 445
            "SFTP" -> 22
            else -> 21
        }
        val port = uri.port.takeIf { it > 0 } ?: defaultPort
        val rawPath = uri.path?.trimStart('/').orEmpty()
        val shareName = if (proto == "SMB") rawPath.substringBefore('/', "") else ""
        val remotePath = if (proto == "SMB") rawPath.substringAfter('/', "") else "/$rawPath"
        val mime = guessMime(entryUrl)

        NetworkStreamProxy.registerTarget(
            RemoteStreamTarget(
                protocol = proto,
                host = host,
                port = port,
                username = share.username,
                password = password,
                shareName = shareName,
                remotePath = remotePath,
                mimeType = mime
            )
        )
    }

    private fun guessMime(nameOrUrl: String): String {
        val lower = nameOrUrl.substringBefore('?').lowercase(Locale.ROOT)
        return when {
            lower.endsWith(".mkv") -> "video/x-matroska"
            lower.endsWith(".webm") -> "video/webm"
            lower.endsWith(".mp3") -> "audio/mpeg"
            lower.endsWith(".flac") -> "audio/flac"
            lower.endsWith(".m4a") || lower.endsWith(".aac") -> "audio/mp4"
            else -> "video/mp4"
        }
    }

    suspend fun browse(
        context: Context,
        share: NetworkShareConfig,
        targetUrl: String,
        password: String
    ): NetworkBrowseResult = withContext(Dispatchers.IO) {
        if (AppSettings.wifiOnlyNetwork.value && !isWifiConnected(context)) {
            return@withContext NetworkBrowseResult.Failure(
                "Wi-Fi-only streaming is enabled in Network settings. Connect to Wi-Fi or turn off the toggle."
            )
        }

        val cleanUrl = targetUrl.trim()
        val proto = share.protocol.trim().uppercase(Locale.ROOT)
        when {
            proto == "SMB" || cleanUrl.startsWith("smb://", ignoreCase = true) ->
                return@withContext browseSmb(cleanUrl, share.username, password)
            proto == "FTP" || cleanUrl.startsWith("ftp://", ignoreCase = true) ->
                return@withContext browseFtp(cleanUrl, share.username, password)
            proto == "SFTP" || cleanUrl.startsWith("sftp://", ignoreCase = true) ->
                return@withContext browseSftp(cleanUrl, share.username, password)
        }

        if (!cleanUrl.startsWith("http://", ignoreCase = true) &&
            !cleanUrl.startsWith("https://", ignoreCase = true)
        ) {
            return@withContext NetworkBrowseResult.Failure(
                "Unsupported URL scheme for '${share.protocol}'. Use http://, https://, smb://, ftp://, or sftp://."
            )
        }

        val timeout = AppSettings.networkTimeoutSec.value.toLong().coerceIn(5L, 60L)
        val client = OkHttpClient.Builder()
            .connectTimeout(timeout, TimeUnit.SECONDS)
            .readTimeout(timeout, TimeUnit.SECONDS)
            .followRedirects(true)
            .build()

        val authHeader = if (share.username.isNotBlank() || password.isNotBlank()) {
            Credentials.basic(share.username, password).also {
                val headers = buildPlaybackHeaders(share.username, password)
                NetworkPlaybackHeaders.registerForUrl(cleanUrl, headers)
                NetworkPlaybackHeaders.registerForUrl(share.baseUrl, headers)
            }
        } else {
            null
        }

        if (cleanUrl.endsWith(".m3u", ignoreCase = true) || cleanUrl.endsWith(".m3u8", ignoreCase = true)) {
            return@withContext fetchM3u(client, cleanUrl, authHeader)
        }

        if (share.protocol.equals("WebDAV", ignoreCase = true)) {
            val webDavResult = tryPropfind(client, cleanUrl, authHeader)
            if (webDavResult != null) return@withContext webDavResult
        }

        return@withContext fetchHtmlDirectory(client, cleanUrl, authHeader)
    }

    private fun browseSmb(url: String, username: String, password: String): NetworkBrowseResult {
        return try {
            val normalized = if (url.startsWith("smb://", ignoreCase = true)) url else "smb://$url"
            val parsed = java.net.URI(normalized)
            val host = parsed.host ?: return NetworkBrowseResult.Failure("SMB URL missing host (e.g. smb://192.168.1.50/Media)")
            val port = parsed.port.takeIf { it > 0 } ?: 445
            val rawPath = parsed.path?.trim('/')?.trim().orEmpty()
            val shareName = rawPath.substringBefore('/', "")
            val subPath = rawPath.substringAfter('/', "")
            if (shareName.isBlank()) {
                return NetworkBrowseResult.Failure("Include share name in SMB URL, e.g. smb://$host/Videos")
            }

            val client = SMBClient()
            client.use { smb ->
                smb.connect(host, port).use { conn ->
                    val auth = if (username.isBlank()) AuthenticationContext.guest()
                    else AuthenticationContext(username, password.toCharArray(), "")
                    val session = conn.authenticate(auth)
                    session.use { s ->
                        val share = s.connectShare(shareName) as DiskShare
                        share.use { disk ->
                            val listPath = subPath.replace('/', '\\')
                            val items = disk.list(listPath)
                            val entries = items.mapNotNull { info ->
                                val name = info.fileName
                                if (name == "." || name == "..") return@mapNotNull null
                                val isDir = (info.fileAttributes and FileAttributes.FILE_ATTRIBUTE_DIRECTORY.value) != 0L
                                if (!isDir && !isMediaOrDirectory(name)) return@mapNotNull null
                                val rel = if (subPath.isBlank()) name else "$subPath/$name"
                                val childUrl = "smb://$host${if (port != 445) ":$port" else ""}/$shareName/$rel"
                                NetworkRemoteEntry(
                                    name = name,
                                    url = childUrl,
                                    isDirectory = isDir,
                                    sizeBytes = info.endOfFile
                                )
                            }.sortedWith(compareByDescending<NetworkRemoteEntry> { it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) })
                            NetworkBrowseResult.Success(entries, normalized)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "SMB connection failed"
            val authErr = msg.contains("STATUS_LOGON_FAILURE", ignoreCase = true) || msg.contains("ACCESS_DENIED", ignoreCase = true)
            NetworkBrowseResult.Failure(msg, isAuthError = authErr)
        }
    }

    private fun browseFtp(url: String, username: String, password: String): NetworkBrowseResult {
        val ftp = FTPClient()
        return try {
            val normalized = if (url.startsWith("ftp://", ignoreCase = true)) url else "ftp://$url"
            val parsed = java.net.URI(normalized)
            val host = parsed.host ?: return NetworkBrowseResult.Failure("FTP URL missing host")
            val port = parsed.port.takeIf { it > 0 } ?: 21
            val remoteDir = parsed.path?.ifBlank { "/" } ?: "/"

            ftp.connectTimeout = 10_000
            ftp.connect(host, port)
            val user = username.ifBlank { "anonymous" }
            val pass = if (username.isBlank()) "anonymous@" else password
            if (!ftp.login(user, pass)) {
                return NetworkBrowseResult.Failure("FTP login failed (${ftp.replyString.trim()})", isAuthError = true)
            }
            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            val files = ftp.listFiles(remoteDir)
            val basePrefix = remoteDir.trimEnd('/')
            val entries = files.mapNotNull { f ->
                val name = f.name
                if (name == "." || name == "..") return@mapNotNull null
                if (!f.isDirectory && !isMediaOrDirectory(name)) return@mapNotNull null
                val nextPath = "$basePrefix/$name"
                val childUrl = "ftp://$host${if (port != 21) ":$port" else ""}$nextPath"
                NetworkRemoteEntry(
                    name = name,
                    url = childUrl,
                    isDirectory = f.isDirectory,
                    sizeBytes = f.size.coerceAtLeast(0L)
                )
            }.sortedWith(compareByDescending<NetworkRemoteEntry> { it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) })
            NetworkBrowseResult.Success(entries, normalized)
        } catch (e: Exception) {
            NetworkBrowseResult.Failure(e.localizedMessage ?: "FTP connection failed")
        } finally {
            runCatching { if (ftp.isConnected) ftp.disconnect() }
        }
    }

    private fun browseSftp(url: String, username: String, password: String): NetworkBrowseResult {
        var jSession: com.jcraft.jsch.Session? = null
        var channel: ChannelSftp? = null
        return try {
            val normalized = if (url.startsWith("sftp://", ignoreCase = true)) url else "sftp://$url"
            val parsed = java.net.URI(normalized)
            val host = parsed.host ?: return NetworkBrowseResult.Failure("SFTP URL missing host")
            val port = parsed.port.takeIf { it > 0 } ?: 22
            val remoteDir = parsed.path?.ifBlank { "/" } ?: "/"

            val jsch = JSch()
            jSession = jsch.getSession(username.ifBlank { "root" }, host, port)
            jSession.setPassword(password)
            jSession.setConfig("StrictHostKeyChecking", "no")
            jSession.timeout = 12_000
            jSession.connect()

            channel = jSession.openChannel("sftp") as ChannelSftp
            channel.connect(12_000)

            @Suppress("UNCHECKED_CAST")
            val vector = channel.ls(remoteDir) as java.util.Vector<ChannelSftp.LsEntry>
            val basePrefix = remoteDir.trimEnd('/')
            val entries = vector.mapNotNull { entry ->
                val name = entry.filename
                if (name == "." || name == "..") return@mapNotNull null
                val isDir = entry.attrs.isDir
                if (!isDir && !isMediaOrDirectory(name)) return@mapNotNull null
                val nextPath = "$basePrefix/$name"
                val childUrl = "sftp://$host${if (port != 22) ":$port" else ""}$nextPath"
                NetworkRemoteEntry(
                    name = name,
                    url = childUrl,
                    isDirectory = isDir,
                    sizeBytes = entry.attrs.size.coerceAtLeast(0L)
                )
            }.sortedWith(compareByDescending<NetworkRemoteEntry> { it.isDirectory }.thenBy { it.name.lowercase(Locale.ROOT) })
            NetworkBrowseResult.Success(entries, normalized)
        } catch (e: Exception) {
            val msg = e.localizedMessage ?: "SFTP connection failed"
            val authErr = msg.contains("Auth fail", ignoreCase = true) || msg.contains("Auth cancel", ignoreCase = true)
            NetworkBrowseResult.Failure(msg, isAuthError = authErr)
        } finally {
            runCatching { channel?.disconnect() }
            runCatching { jSession?.disconnect() }
        }
    }

    private fun tryPropfind(
        client: OkHttpClient,
        url: String,
        authHeader: String?
    ): NetworkBrowseResult? {
        return try {
            val normalizedUrl = if (url.endsWith("/")) url else "$url/"
            val propfindXml = """<?xml version="1.0" encoding="utf-8"?><propfind xmlns="DAV:"><prop><displayname/><getcontentlength/><getcontenttype/><resourcetype/></prop></propfind>"""
            val reqBuilder = Request.Builder()
                .url(normalizedUrl)
                .method("PROPFIND", propfindXml.toRequestBody("application/xml; charset=utf-8".toMediaType()))
                .header("Depth", "1")
            if (authHeader != null) reqBuilder.header("Authorization", authHeader)

            client.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) {
                    return NetworkBrowseResult.Failure("Authentication required (HTTP ${resp.code}).", isAuthError = true)
                }
                if (resp.code !in 200..299) {
                    return null
                }
                val body = resp.body.string()
                val entries = parseWebDavXml(body, normalizedUrl)
                NetworkBrowseResult.Success(entries, normalizedUrl)
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun fetchHtmlDirectory(
        client: OkHttpClient,
        url: String,
        authHeader: String?
    ): NetworkBrowseResult {
        return try {
            val reqBuilder = Request.Builder().url(url).get()
            if (authHeader != null) reqBuilder.header("Authorization", authHeader)

            client.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) {
                    return NetworkBrowseResult.Failure("Authentication required (HTTP ${resp.code}).", isAuthError = true)
                }
                if (!resp.isSuccessful) {
                    return NetworkBrowseResult.Failure("Server returned HTTP ${resp.code} ${resp.message}.")
                }
                val contentType = resp.header("Content-Type").orEmpty()
                val body = resp.body.string()
                val baseUri = Uri.parse(if (url.endsWith("/")) url else "$url/")
                val entries = parseHtmlLinks(body, baseUri, contentType)
                NetworkBrowseResult.Success(entries, url)
            }
        } catch (e: Exception) {
            NetworkBrowseResult.Failure(e.localizedMessage ?: "Connection failed.")
        }
    }

    private fun fetchM3u(
        client: OkHttpClient,
        url: String,
        authHeader: String?
    ): NetworkBrowseResult {
        return try {
            val reqBuilder = Request.Builder().url(url).get()
            if (authHeader != null) reqBuilder.header("Authorization", authHeader)
            client.newCall(reqBuilder.build()).execute().use { resp ->
                if (resp.code == 401 || resp.code == 403) {
                    return NetworkBrowseResult.Failure("Authentication required (HTTP ${resp.code}).", isAuthError = true)
                }
                if (!resp.isSuccessful) {
                    return NetworkBrowseResult.Failure("Playlist fetch failed (HTTP ${resp.code}).")
                }
                val lines = resp.body.string().lines()
                val entries = parseM3uLines(url, lines)
                NetworkBrowseResult.Success(entries, url)
            }
        } catch (e: Exception) {
            NetworkBrowseResult.Failure(e.localizedMessage ?: "Failed to read M3U playlist.")
        }
    }

    internal fun parseM3uLines(baseUrl: String, lines: List<String>): List<NetworkRemoteEntry> {
        val entries = mutableListOf<NetworkRemoteEntry>()
        var pendingTitle: String? = null
        val baseJavaUri = runCatching { java.net.URI(baseUrl) }.getOrNull()
        for (rawLine in lines) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("#EXTINF", ignoreCase = true)) {
                pendingTitle = line.substringAfter(',', "").trim().ifEmpty { null }
            } else if (!line.startsWith("#")) {
                val resolved = if (baseJavaUri != null) {
                    runCatching { baseJavaUri.resolve(line).toString() }.getOrDefault(line)
                } else line
                val label = pendingTitle ?: resolved.substringAfterLast('/').substringBefore('?').ifEmpty { resolved }
                entries.add(NetworkRemoteEntry(name = label, url = resolved, isDirectory = false))
                pendingTitle = null
            }
        }
        return entries
    }

    private fun parseWebDavXml(xml: String, requestUrl: String): List<NetworkRemoteEntry> {
        val results = mutableListOf<NetworkRemoteEntry>()
        val factory = XmlPullParserFactory.newInstance().apply { isNamespaceAware = true }
        val parser = factory.newPullParser().apply { setInput(StringReader(xml)) }
        val baseUri = Uri.parse(requestUrl)
        val selfPath = baseUri.path?.trimEnd('/').orEmpty()

        var inResponse = false
        var href = ""
        var displayName = ""
        var isDir = false
        var size = 0L
        var contentType = ""
        var tagName = ""

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    tagName = parser.name.orEmpty().lowercase()
                    when (tagName) {
                        "response" -> {
                            inResponse = true
                            href = ""
                            displayName = ""
                            isDir = false
                            size = 0L
                            contentType = ""
                        }
                        "collection" -> if (inResponse) isDir = true
                    }
                }
                XmlPullParser.TEXT -> {
                    if (inResponse) {
                        val text = parser.text.orEmpty().trim()
                        if (text.isNotEmpty()) {
                            when (tagName) {
                                "href" -> href = text
                                "displayname" -> displayName = text
                                "getcontentlength" -> size = text.toLongOrNull() ?: 0L
                                "getcontenttype" -> contentType = text
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (parser.name.orEmpty().equals("response", ignoreCase = true) && inResponse) {
                        inResponse = false
                        val resolved = resolveUrl(baseUri, href)
                        val entryPath = Uri.parse(resolved).path?.trimEnd('/').orEmpty()
                        if (resolved.isNotEmpty() && entryPath != selfPath) {
                            val name = displayName.ifBlank {
                                decodedNameFromPath(entryPath).ifBlank { resolved }
                            }
                            if (isDir || isMediaOrDirectory(name)) {
                                results.add(
                                    NetworkRemoteEntry(
                                        name = name,
                                        url = resolved,
                                        isDirectory = isDir,
                                        sizeBytes = size,
                                        contentType = contentType
                                    )
                                )
                            }
                        }
                    }
                    tagName = ""
                }
            }
            eventType = parser.next()
        }
        return results.sortedWith(compareByDescending<NetworkRemoteEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    private fun parseHtmlLinks(html: String, baseUri: Uri, contentType: String): List<NetworkRemoteEntry> {
        if (!contentType.contains("html", ignoreCase = true) && isMediaOrDirectory(baseUri.toString())) {
            return listOf(
                NetworkRemoteEntry(
                    name = decodedNameFromPath(baseUri.path.orEmpty()).ifBlank { "Stream" },
                    url = baseUri.toString(),
                    isDirectory = false,
                    contentType = contentType
                )
            )
        }
        val results = mutableListOf<NetworkRemoteEntry>()
        val seen = HashSet<String>()
        val matcher = HREF_PATTERN.matcher(html)
        val selfPath = baseUri.path?.trimEnd('/').orEmpty()
        while (matcher.find()) {
            val rawHref = matcher.group(1)?.trim().orEmpty()
            if (rawHref.isEmpty() || rawHref.startsWith("?") || rawHref.startsWith("#") ||
                rawHref == "../" || rawHref == "./" || rawHref.startsWith("javascript:")
            ) continue
            val resolved = resolveUrl(baseUri, rawHref)
            val resolvedPath = Uri.parse(resolved).path?.trimEnd('/').orEmpty()
            if (resolvedPath == selfPath || !seen.add(resolved)) continue
            val isDir = rawHref.endsWith("/")
            val name = decodedNameFromPath(resolvedPath)
            if (name.isEmpty()) continue
            if (isDir || isMediaOrDirectory(name)) {
                results.add(NetworkRemoteEntry(name = name, url = resolved, isDirectory = isDir))
            }
        }
        return results.sortedWith(compareByDescending<NetworkRemoteEntry> { it.isDirectory }.thenBy { it.name.lowercase() })
    }

    private fun resolveUrl(base: Uri, relative: String): String {
        return try {
            java.net.URI(base.toString()).resolve(relative).toString()
        } catch (_: Exception) {
            relative
        }
    }

    private fun decodedNameFromPath(path: String): String {
        val last = path.trimEnd('/').substringAfterLast('/')
        return try {
            URLDecoder.decode(last, "UTF-8")
        } catch (_: Exception) {
            last
        }
    }

    internal fun isMediaOrDirectory(name: String): Boolean {
        val lower = name.substringBefore('?').lowercase()
        return lower.endsWith(".mp4") || lower.endsWith(".mkv") || lower.endsWith(".webm") ||
            lower.endsWith(".mov") || lower.endsWith(".avi") || lower.endsWith(".m4v") ||
            lower.endsWith(".ts") || lower.endsWith(".m3u8") || lower.endsWith(".mpd") ||
            lower.endsWith(".mp3") || lower.endsWith(".flac") || lower.endsWith(".aac") ||
            lower.endsWith(".ogg") || lower.endsWith(".opus") || lower.endsWith(".wav") ||
            lower.endsWith(".m4b") || lower.endsWith(".m3u")
    }

    fun buildPlaybackHeaders(
        username: String? = null,
        password: String? = null,
        userAgent: String? = null,
        extraHeaders: Map<String, String> = emptyMap()
    ): Map<String, String> {
        val headers = linkedMapOf<String, String>()
        extraHeaders.forEach { (k, v) ->
            val cleanKey = k.trim()
            if (cleanKey.isNotEmpty() && v.isNotBlank()) headers[cleanKey] = v.trim()
        }
        val cleanUser = username?.trim().orEmpty()
        val cleanPass = password.orEmpty()
        if (cleanUser.isNotEmpty() && cleanPass.isNotEmpty() && headers.keys.none { it.equals("Authorization", ignoreCase = true) }) {
            headers["Authorization"] = Credentials.basic(cleanUser, cleanPass)
        }
        val cleanUa = userAgent?.trim().orEmpty()
        if (cleanUa.isNotEmpty()) {
            headers["User-Agent"] = cleanUa
        }
        return headers
    }

    suspend fun fetchM3uWithHeaders(
        url: String,
        userAgent: String = "MPVXPlayer/1.0",
        username: String? = null,
        password: String? = null
    ): NetworkBrowseResult = withContext(Dispatchers.IO) {
        val cleanUrl = url.trim()
        if (cleanUrl.isEmpty()) return@withContext NetworkBrowseResult.Failure("M3U URL is empty.")
        try {
            val client = OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .build()
            val hdrs = buildPlaybackHeaders(username, password, userAgent)
            val reqBuilder = Request.Builder().url(cleanUrl).get()
            hdrs.forEach { (k, v) -> reqBuilder.header(k, v) }
            client.newCall(reqBuilder.build()).execute().use { resp ->
                if (!resp.isSuccessful) {
                    return@withContext NetworkBrowseResult.Failure("Playlist fetch failed (HTTP ${resp.code}).", isAuthError = resp.code == 401 || resp.code == 403)
                }
                val entries = parseM3uLines(cleanUrl, resp.body.string().lines())
                entries.forEach { entry -> NetworkPlaybackHeaders.registerForUrl(entry.url, hdrs) }
                NetworkBrowseResult.Success(entries, cleanUrl)
            }
        } catch (e: Exception) {
            NetworkBrowseResult.Failure(e.localizedMessage ?: "Failed to fetch M3U playlist.")
        }
    }
}

object NetworkPlaybackHeaders {
    private val prefixHeaders = java.util.concurrent.ConcurrentHashMap<String, Map<String, String>>()

    fun registerForUrl(urlOrBase: String, headers: Map<String, String>) {
        if (urlOrBase.isBlank() || headers.isEmpty()) return
        val origin = extractOriginPrefix(urlOrBase)
        if (origin.isNotEmpty()) {
            prefixHeaders[origin] = headers
        }
        prefixHeaders[urlOrBase.trim()] = headers
    }

    fun getHeadersForUrl(url: String?): Map<String, String> {
        if (url.isNullOrBlank()) return emptyMap()
        val clean = url.trim()
        prefixHeaders[clean]?.let { return it }
        val origin = extractOriginPrefix(clean)
        return if (origin.isNotEmpty()) prefixHeaders[origin].orEmpty() else emptyMap()
    }

    fun clearSessionHeaders() {
        prefixHeaders.clear()
    }

    internal fun extractOriginPrefix(rawUrl: String): String {
        return try {
            val parsed = java.net.URI(rawUrl.trim())
            val scheme = parsed.scheme ?: return ""
            val host = parsed.host ?: return ""
            val port = if (parsed.port > 0) ":${parsed.port}" else ""
            "$scheme://$host$port"
        } catch (_: Exception) {
            ""
        }
    }
}
