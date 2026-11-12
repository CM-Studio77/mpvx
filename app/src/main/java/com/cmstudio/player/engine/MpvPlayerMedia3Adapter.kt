package com.cmstudio.player.engine

import android.net.Uri
import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.ListenableFuture
import java.util.concurrent.Executor
import java.util.concurrent.TimeUnit

/**
 * Lightweight Media3 [Player] adapter backed by [MpvEngineController] so that
 * `MPV` video sessions get identical MediaSession notification, lockscreen controls,
 * and background playback lifecycle through [com.cmstudio.player.service.MediaPlaybackService].
 */
@OptIn(UnstableApi::class)
class MpvPlayerMedia3Adapter(
    looper: Looper,
    private val titleProvider: () -> String,
    private val uriProvider: () -> Uri?
) : SimpleBasePlayer(looper) {

    fun notifyStateChanged() {
        runCatching { invalidateState() }
    }

    override fun getState(): State {
        val snap = MpvEngineController.snapshot.value
        val uri = uriProvider()
        val title = titleProvider().ifBlank { "MPVX Video" }

        val commands = Player.Commands.Builder()
            .addAll(
                Player.COMMAND_PLAY_PAUSE,
                Player.COMMAND_PREPARE,
                Player.COMMAND_STOP,
                Player.COMMAND_SEEK_IN_CURRENT_MEDIA_ITEM,
                Player.COMMAND_SEEK_BACK,
                Player.COMMAND_SEEK_FORWARD,
                Player.COMMAND_GET_CURRENT_MEDIA_ITEM,
                Player.COMMAND_GET_METADATA,
                Player.COMMAND_GET_TIMELINE,
                Player.COMMAND_SET_SPEED_AND_PITCH
            )
            .build()

        val metadata = MediaMetadata.Builder()
            .setTitle(title)
            .build()

        val item = MediaItem.Builder()
            .setMediaId(uri?.toString() ?: "mpv_current")
            .apply { if (uri != null) setUri(uri) }
            .setMediaMetadata(metadata)
            .build()

        val durationUs = if (snap.durationMs > 0L) snap.durationMs * 1000L else androidx.media3.common.C.TIME_UNSET
        val mediaItemData = MediaItemData.Builder("mpv_item_0")
            .setMediaItem(item)
            .setMediaMetadata(metadata)
            .setIsSeekable(snap.durationMs > 0L)
            .setDurationUs(durationUs)
            .build()

        val playbackState = when {
            !snap.initialized -> Player.STATE_IDLE
            snap.isEnded -> Player.STATE_ENDED
            snap.isBuffering -> Player.STATE_BUFFERING
            snap.isFileLoaded || snap.durationMs > 0L -> Player.STATE_READY
            else -> Player.STATE_BUFFERING
        }

        return State.Builder()
            .setAvailableCommands(commands)
            .setPlaylist(listOf(mediaItemData))
            .setCurrentMediaItemIndex(0)
            .setPlayWhenReady(snap.isPlaying, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
            .setPlaybackState(playbackState)
            .setContentPositionMs { MpvEngineController.snapshot.value.positionMs.coerceAtLeast(0L) }
            .setContentBufferedPositionMs { MpvEngineController.snapshot.value.bufferedMs.coerceAtLeast(0L) }
            .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        MpvEngineController.setPaused(!playWhenReady)
        invalidateState()
        return CompletedVoidFuture
    }

    override fun handlePrepare(): ListenableFuture<*> {
        invalidateState()
        return CompletedVoidFuture
    }

    override fun handleStop(): ListenableFuture<*> {
        MpvEngineController.pause()
        invalidateState()
        return CompletedVoidFuture
    }

    override fun handleRelease(): ListenableFuture<*> {
        return CompletedVoidFuture
    }

    override fun handleSeek(mediaItemIndex: Int, positionMs: Long, seekCommand: Int): ListenableFuture<*> {
        MpvEngineController.seekTo(positionMs.coerceAtLeast(0L))
        invalidateState()
        return CompletedVoidFuture
    }

    override fun handleSetPlaybackParameters(playbackParameters: PlaybackParameters): ListenableFuture<*> {
        MpvEngineController.setSpeed(playbackParameters.speed, playbackParameters.pitch == 1.0f)
        invalidateState()
        return CompletedVoidFuture
    }

    private object CompletedVoidFuture : ListenableFuture<Void?> {
        override fun cancel(mayInterruptIfRunning: Boolean): Boolean = false
        override fun isCancelled(): Boolean = false
        override fun isDone(): Boolean = true
        override fun get(): Void? = null
        override fun get(timeout: Long, unit: TimeUnit): Void? = null
        override fun addListener(listener: Runnable, executor: Executor) {
            executor.execute(listener)
        }
    }
}
