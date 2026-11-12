package com.cmstudio.player.ui.network

import android.net.Uri
import com.hierynomus.msdtyp.AccessMask
import com.hierynomus.mssmb2.SMB2CreateDisposition
import com.hierynomus.mssmb2.SMB2ShareAccess
import com.hierynomus.smbj.SMBClient
import com.hierynomus.smbj.auth.AuthenticationContext
import com.hierynomus.smbj.share.DiskShare
import com.jcraft.jsch.ChannelSftp
import com.jcraft.jsch.JSch
import fi.iki.elonen.NanoHTTPD
import org.apache.commons.net.ftp.FTP
import org.apache.commons.net.ftp.FTPClient
import java.io.FilterInputStream
import java.io.InputStream
import java.util.EnumSet
import java.util.Locale
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

data class RemoteStreamTarget(
    val protocol: String, // SMB, FTP, SFTP
    val host: String,
    val port: Int,
    val username: String,
    val password: String,
    val shareName: String = "",
    val remotePath: String,
    val mimeType: String = "video/mp4"
)

/**
 * Loopback-only (`127.0.0.1`) HTTP/1.1 range-supporting streaming proxy allowing both
 * `ExoPlayer` and `libmpv` to play & seek `SMB`, `FTP`, and `SFTP` files cleanly without
 * exposing any open network socket externally.
 */
object NetworkStreamProxy {

    private val sessionToken = UUID.randomUUID().toString().replace("-", "")
    private val targets = ConcurrentHashMap<String, RemoteStreamTarget>()
    private val lock = Any()
    private var server: LocalBridgeServer? = null
    private var boundPort: Int = 0

    fun registerTarget(target: RemoteStreamTarget): String {
        ensureStarted()
        val id = UUID.randomUUID().toString().replace("-", "")
        targets[id] = target
        val safeName = Uri.encode(target.remotePath.substringAfterLast('/').ifBlank { "stream.mp4" })
        return "http://127.0.0.1:$boundPort/stream/$id/$safeName?tok=$sessionToken"
    }

    fun clearTargets() {
        targets.clear()
    }

    private fun ensureStarted() {
        synchronized(lock) {
            if (server?.isAlive == true && boundPort > 0) return
            val s = LocalBridgeServer()
            s.start(NanoHTTPD.SOCKET_READ_TIMEOUT, false)
            boundPort = s.listeningPort
            server = s
        }
    }

    private class LocalBridgeServer : NanoHTTPD("127.0.0.1", 0) {
        override fun serve(session: IHTTPSession): Response {
            if (session.parms["tok"] != sessionToken) {
                return newFixedLengthResponse(Response.Status.FORBIDDEN, MIME_PLAINTEXT, "Forbidden")
            }
            val segments = session.uri.trimStart('/').split('/')
            if (segments.size < 2 || segments[0] != "stream") {
                return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Not found")
            }
            val target = targets[segments[1]]
                ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "Stream expired")

            return try {
                serveTarget(session, target)
            } catch (e: Exception) {
                newFixedLengthResponse(
                    Response.Status.INTERNAL_ERROR,
                    MIME_PLAINTEXT,
                    e.localizedMessage ?: "Stream bridge error"
                )
            }
        }

        private fun serveTarget(session: IHTTPSession, target: RemoteStreamTarget): Response {
            val rangeHeader = session.headers["range"]
            return when (target.protocol.uppercase(Locale.ROOT)) {
                "SMB" -> serveSmb(target, rangeHeader)
                "FTP" -> serveFtp(target, rangeHeader)
                "SFTP" -> serveSftp(target, rangeHeader)
                else -> newFixedLengthResponse(Response.Status.BAD_REQUEST, MIME_PLAINTEXT, "Unsupported protocol")
            }
        }

        private fun parseRange(rangeHeader: String?, totalLength: Long): Pair<Long, Long> {
            if (rangeHeader.isNullOrBlank() || !rangeHeader.startsWith("bytes=")) {
                return 0L to (totalLength - 1L).coerceAtLeast(0L)
            }
            val spec = rangeHeader.removePrefix("bytes=").substringBefore(',').trim()
            val dash = spec.indexOf('-')
            if (dash < 0) return 0L to (totalLength - 1L).coerceAtLeast(0L)
            val start = spec.substring(0, dash).toLongOrNull() ?: 0L
            val end = spec.substring(dash + 1).toLongOrNull() ?: (totalLength - 1L)
            val safeStart = start.coerceIn(0L, (totalLength - 1L).coerceAtLeast(0L))
            val safeEnd = end.coerceIn(safeStart, (totalLength - 1L).coerceAtLeast(safeStart))
            return safeStart to safeEnd
        }

        private fun buildRangeResponse(
            stream: InputStream,
            mimeType: String,
            totalLength: Long,
            start: Long,
            end: Long,
            isPartial: Boolean
        ): Response {
            val contentLength = (end - start + 1L).coerceAtLeast(0L)
            val resp = if (isPartial && totalLength > 0L) {
                newFixedLengthResponse(Response.Status.PARTIAL_CONTENT, mimeType, stream, contentLength).apply {
                    addHeader("Content-Range", "bytes $start-$end/$totalLength")
                }
            } else if (totalLength > 0L) {
                newFixedLengthResponse(Response.Status.OK, mimeType, stream, totalLength)
            } else {
                newChunkedResponse(Response.Status.OK, mimeType, stream)
            }
            resp.addHeader("Accept-Ranges", "bytes")
            return resp
        }

        private fun serveSmb(target: RemoteStreamTarget, rangeHeader: String?): Response {
            val client = SMBClient()
            val conn = client.connect(target.host, target.port)
            val auth = if (target.username.isBlank()) AuthenticationContext.guest()
            else AuthenticationContext(target.username, target.password.toCharArray(), "")
            val smbSession = conn.authenticate(auth)
            val share = smbSession.connectShare(target.shareName) as DiskShare
            val smbPath = target.remotePath.trimStart('/')
            val file = share.openFile(
                smbPath,
                EnumSet.of(AccessMask.GENERIC_READ),
                null,
                SMB2ShareAccess.ALL,
                SMB2CreateDisposition.FILE_OPEN,
                null
            )
            val totalLen = file.fileInformation.standardInformation.endOfFile
            val (start, end) = parseRange(rangeHeader, totalLen)
            val rawIn = file.inputStream
            if (start > 0L) rawIn.skip(start)
            val wrapped = object : FilterInputStream(rawIn) {
                override fun close() {
                    runCatching { super.close() }
                    runCatching { file.close() }
                    runCatching { share.close() }
                    runCatching { smbSession.close() }
                    runCatching { conn.close() }
                    runCatching { client.close() }
                }
            }
            return buildRangeResponse(wrapped, target.mimeType, totalLen, start, end, !rangeHeader.isNullOrBlank())
        }

        private fun serveFtp(target: RemoteStreamTarget, rangeHeader: String?): Response {
            val ftp = FTPClient()
            ftp.connectTimeout = 10_000
            ftp.connect(target.host, target.port)
            val user = target.username.ifBlank { "anonymous" }
            val pass = if (target.username.isBlank()) "anonymous@" else target.password
            if (!ftp.login(user, pass)) {
                ftp.disconnect()
                return newFixedLengthResponse(Response.Status.UNAUTHORIZED, MIME_PLAINTEXT, "FTP authentication failed")
            }
            ftp.enterLocalPassiveMode()
            ftp.setFileType(FTP.BINARY_FILE_TYPE)
            val totalLen = runCatching { ftp.mlistFile(target.remotePath)?.size ?: -1L }.getOrDefault(-1L)
            val (start, end) = if (totalLen > 0L) parseRange(rangeHeader, totalLen) else 0L to -1L
            if (start > 0L) ftp.restartOffset = start
            val rawIn = ftp.retrieveFileStream(target.remotePath)
                ?: return newFixedLengthResponse(Response.Status.NOT_FOUND, MIME_PLAINTEXT, "FTP file open failed")
            val wrapped = object : FilterInputStream(rawIn) {
                override fun close() {
                    runCatching { super.close() }
                    runCatching { ftp.completePendingCommand() }
                    runCatching { ftp.logout() }
                    runCatching { ftp.disconnect() }
                }
            }
            return buildRangeResponse(wrapped, target.mimeType, totalLen, start, end, !rangeHeader.isNullOrBlank() && totalLen > 0L)
        }

        private fun serveSftp(target: RemoteStreamTarget, rangeHeader: String?): Response {
            val jsch = JSch()
            val jSession = jsch.getSession(target.username.ifBlank { "root" }, target.host, target.port)
            jSession.setPassword(target.password)
            jSession.setConfig("StrictHostKeyChecking", "no")
            jSession.timeout = 12_000
            jSession.connect()
            val channel = jSession.openChannel("sftp") as ChannelSftp
            channel.connect(12_000)
            val totalLen = runCatching { channel.lstat(target.remotePath).size }.getOrDefault(-1L)
            val (start, end) = if (totalLen > 0L) parseRange(rangeHeader, totalLen) else 0L to -1L
            val rawIn = channel.get(target.remotePath, null, start)
            val wrapped = object : FilterInputStream(rawIn) {
                override fun close() {
                    runCatching { super.close() }
                    runCatching { channel.disconnect() }
                    runCatching { jSession.disconnect() }
                }
            }
            return buildRangeResponse(wrapped, target.mimeType, totalLen, start, end, !rangeHeader.isNullOrBlank() && totalLen > 0L)
        }
    }
}
