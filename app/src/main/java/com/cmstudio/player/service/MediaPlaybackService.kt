package com.cmstudio.player.service

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.annotation.OptIn
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.cmstudio.player.ui.ModernHomeActivity
import com.cmstudio.player.ui.music.AudioPlayerManager
import com.cmstudio.player.ui.player.ModernPlayerActivity

/**
 * Background/foreground Media3 MediaSessionService backing BOTH music/audio playback
 * and background video playback with lockscreen controls, media notification, and focus handling.
 */
@OptIn(UnstableApi::class)
class MediaPlaybackService : MediaSessionService() {

    private var mediaSession: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        activeInstance = this
        ensureMediaSession(isVideoAttach = attachedVideoPlayer != null)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_ATTACH_VIDEO -> ensureMediaSession(isVideoAttach = true)
            ACTION_DETACH_VIDEO -> {
                if (mediaSession?.player === attachedVideoPlayer || attachedVideoPlayer == null) {
                    releaseSessionOnly()
                    stopForeground(STOP_FOREGROUND_REMOVE)
                    stopSelf()
                }
            }
            else -> ensureMediaSession(isVideoAttach = false)
        }
        return super.onStartCommand(intent, flags, startId)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? {
        if (mediaSession == null && (attachedVideoPlayer != null || AudioPlayerManager.currentTrack.value != null)) {
            ensureMediaSession(isVideoAttach = attachedVideoPlayer != null)
        }
        return mediaSession
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        val player = mediaSession?.player
        if (player == null || !player.playWhenReady || player.mediaItemCount == 0) {
            stopSelf()
        }
    }

    override fun onDestroy() {
        releaseSessionOnly()
        if (activeInstance === this) {
            activeInstance = null
        }
        super.onDestroy()
    }

    private fun ensureMediaSession(isVideoAttach: Boolean) {
        val videoPlayer = if (isVideoAttach) attachedVideoPlayer else null
        if (videoPlayer != null) {
            if (mediaSession?.player === videoPlayer) return
            releaseSessionOnly()
            val launchVideo = Intent(this, ModernPlayerActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT
            }
            val pendingIntent = PendingIntent.getActivity(
                this,
                1002,
                launchVideo,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            mediaSession = MediaSession.Builder(this, videoPlayer)
                .setId("mpvx_video_session")
                .setSessionActivity(pendingIntent)
                .build()
            return
        }

        val musicAlreadyActive = mediaSession != null && mediaSession?.id == "mpvx_music_session"
        if (musicAlreadyActive) return
        releaseSessionOnly()
        AudioPlayerManager.init(this)
        val player = AudioPlayerManager.getOrCreatePlayer(this)
        val launchIntent = Intent(this, ModernHomeActivity::class.java).apply {
            putExtra("open_tab", "MUSIC")
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this,
            1001,
            launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        mediaSession = MediaSession.Builder(this, player)
            .setId("mpvx_music_session")
            .setSessionActivity(pendingIntent)
            .build()
    }

    private fun releaseSessionOnly() {
        mediaSession?.let { session ->
            try {
                removeSession(session)
                session.release()
            } catch (t: Throwable) {
                Log.w(TAG, "Error releasing MediaSession: ${t.message}")
            }
            mediaSession = null
        }
    }

    companion object {
        private const val TAG = "MediaPlaybackService"
        private const val ACTION_ATTACH_VIDEO = "com.cmstudio.player.ACTION_ATTACH_VIDEO"
        private const val ACTION_DETACH_VIDEO = "com.cmstudio.player.ACTION_DETACH_VIDEO"

        @Volatile
        private var activeInstance: MediaPlaybackService? = null

        @Volatile
        var attachedVideoPlayer: Player? = null
            private set

        fun attachVideoPlayer(context: Context, player: Player) {
            attachedVideoPlayer = player
            val intent = Intent(context.applicationContext, MediaPlaybackService::class.java).apply {
                action = ACTION_ATTACH_VIDEO
            }
            try {
                ContextCompat.startForegroundService(context.applicationContext, intent)
            } catch (t: Throwable) {
                Log.w(TAG, "Unable to start foreground video session: ${t.message}")
            }
        }

        fun detachVideoPlayer(context: Context, player: Player?) {
            if (player != null && attachedVideoPlayer !== player) return
            attachedVideoPlayer = null
            activeInstance?.let { service ->
                if (service.mediaSession?.id == "mpvx_video_session") {
                    service.releaseSessionOnly()
                    try {
                        service.stopForeground(STOP_FOREGROUND_REMOVE)
                    } catch (_: Throwable) {
                    }
                    service.stopSelf()
                }
            }
            val intent = Intent(context.applicationContext, MediaPlaybackService::class.java).apply {
                action = ACTION_DETACH_VIDEO
            }
            runCatching { context.applicationContext.stopService(intent) }
        }

        fun stopAndRelease() {
            attachedVideoPlayer = null
            activeInstance?.let { service ->
                service.releaseSessionOnly()
                try {
                    service.stopForeground(STOP_FOREGROUND_REMOVE)
                } catch (_: Throwable) {
                }
                service.stopSelf()
            }
        }
    }
}
