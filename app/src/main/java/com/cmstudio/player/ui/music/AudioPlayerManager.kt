package com.cmstudio.player.ui.music

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.audiofx.Equalizer
import android.media.audiofx.LoudnessEnhancer
import android.util.Log
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.cmstudio.player.PlaybackHistoryBridge
import com.cmstudio.player.service.MediaPlaybackService
import com.cmstudio.player.ui.AppSettings
import com.cmstudio.player.ui.player.EqualizerPresetsCatalog
import kotlin.math.roundToInt
import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Shared singleton audio controller backed by Media3 ExoPlayer and [MediaPlaybackService].
 * Re-creates ExoPlayer automatically after [release] so re-opening music tracks works cleanly.
 */
object AudioPlayerManager {
    private const val TAG = "AudioPlayerManager"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var appContext: Context? = null
    private var exoPlayer: ExoPlayer? = null
    private var mediaControllerFuture: ListenableFuture<MediaController>? = null
    private var positionPollJob: Job? = null
    private var sleepTimerJob: Job? = null
    private var standbyReleaseJob: Job? = null
    private var equalizer: Equalizer? = null
    private var loudnessNormEnhancer: LoudnessEnhancer? = null
    private var activeSessionId: Int = C.AUDIO_SESSION_ID_UNSET

    private val _sleepTimerRemainingMs = MutableStateFlow(0L)
    val sleepTimerRemainingMs: StateFlow<Long> = _sleepTimerRemainingMs.asStateFlow()

    private val _currentTrack = MutableStateFlow<RealAudioTrack?>(null)
    val currentTrack: StateFlow<RealAudioTrack?> = _currentTrack.asStateFlow()

    private val _playlist = MutableStateFlow<List<RealAudioTrack>>(emptyList())
    val playlist: StateFlow<List<RealAudioTrack>> = _playlist.asStateFlow()

    private val _currentIndex = MutableStateFlow(0)
    val currentIndex: StateFlow<Int> = _currentIndex.asStateFlow()

    private val _isPlaying = MutableStateFlow(false)
    val isPlaying: StateFlow<Boolean> = _isPlaying.asStateFlow()

    private val _currentPositionMs = MutableStateFlow(0L)
    val currentPositionMs: StateFlow<Long> = _currentPositionMs.asStateFlow()

    private val _durationMs = MutableStateFlow(0L)
    val durationMs: StateFlow<Long> = _durationMs.asStateFlow()

    private val _isShuffle = MutableStateFlow(false)
    val isShuffle: StateFlow<Boolean> = _isShuffle.asStateFlow()

    private val _isRepeat = MutableStateFlow(false)
    val isRepeat: StateFlow<Boolean> = _isRepeat.asStateFlow()

    private val _lastError = MutableStateFlow<String?>(null)
    val lastError: StateFlow<String?> = _lastError.asStateFlow()

    private val playerListener = object : Player.Listener {
        override fun onIsPlayingChanged(playing: Boolean) {
            _isPlaying.value = playing
            AppSettings.setMiniPlayerPlaying(playing)
            if (playing) {
                standbyReleaseJob?.cancel()
                standbyReleaseJob = null
                startPositionPolling()
            } else {
                scheduleStandbyIfEnabled()
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            val player = exoPlayer ?: return
            val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
                ?: _currentTrack.value?.durationMs
                ?: 0L
            _durationMs.value = dur
            if (playbackState == Player.STATE_ENDED) {
                _isPlaying.value = false
                AppSettings.setMiniPlayerPlaying(false)
            }
        }

        override fun onMediaItemTransition(mediaItem: MediaItem?, reason: Int) {
            val player = exoPlayer ?: return
            val idx = player.currentMediaItemIndex
            val list = _playlist.value
            if (idx in list.indices) {
                _currentIndex.value = idx
                val track = list[idx]
                _currentTrack.value = track
                val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L } ?: track.durationMs
                _durationMs.value = dur
                _currentPositionMs.value = 0L
                syncMiniPlayerUi(track, player.isPlaying)
            }
        }

        override fun onAudioSessionIdChanged(audioSessionId: Int) {
            activeSessionId = audioSessionId
            applyAudioEffectsNow()
        }

        override fun onPlayerError(error: PlaybackException) {
            Log.e(TAG, "Audio playback error: ${error.errorCodeName}", error)
            _lastError.value = error.localizedMessage ?: error.errorCodeName
            _isPlaying.value = false
            AppSettings.setMiniPlayerPlaying(false)
        }
    }

    fun init(context: Context) {
        if (appContext == null) {
            appContext = context.applicationContext
        }
    }

    fun getOrCreatePlayer(context: Context): ExoPlayer {
        val ctx = (appContext ?: context.applicationContext).also { appContext = it }
        exoPlayer?.let { return it }

        val audioAttrs = AudioAttributes.Builder()
            .setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MUSIC)
            .build()

        val created = ExoPlayer.Builder(ctx)
            .setAudioAttributes(audioAttrs, true)
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_LOCAL)
            .build()
        created.shuffleModeEnabled = _isShuffle.value
        created.repeatMode = if (_isRepeat.value) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
        created.addListener(playerListener)
        exoPlayer = created
        return created
    }

    private fun ensureServiceStarted() {
        val ctx = appContext ?: return
        // Bind via Media3 MediaController rather than calling startForegroundService() manually
        // with an empty Intent, avoiding ForegroundServiceDidNotStartInTimeException while
        // allowing MediaSessionService to manage its own foreground notification lifecycle.
        if (mediaControllerFuture == null) {
            try {
                val token = SessionToken(ctx, ComponentName(ctx, MediaPlaybackService::class.java))
                mediaControllerFuture = MediaController.Builder(ctx, token).buildAsync()
            } catch (t: Throwable) {
                Log.w(TAG, "MediaController bind failed: ${t.message}")
            }
        }
        try {
            ctx.startService(Intent(ctx, MediaPlaybackService::class.java))
        } catch (t: Throwable) {
            Log.w(TAG, "Audio service start deferred: ${t.message}")
        }
    }

    private fun startPositionPolling() {
        if (positionPollJob?.isActive == true) return
        positionPollJob = scope.launch {
            while (isActive) {
                val player = exoPlayer
                if (player != null) {
                    val pos = player.currentPosition.coerceAtLeast(0L)
                    val dur = player.duration.takeIf { it != C.TIME_UNSET && it > 0L }
                        ?: _durationMs.value
                    _currentPositionMs.value = pos
                    _durationMs.value = dur
                    val progress = if (dur > 0L) (pos.toFloat() / dur.toFloat()).coerceIn(0f, 1f) else 0f
                    AppSettings.setMiniPlayerProgress(progress)
                } else {
                    break
                }
                delay(400L)
            }
        }
    }

    private fun toMediaItem(track: RealAudioTrack): MediaItem {
        val targetUri = if (track.isRadio && track.streamUrl.isNotBlank()) {
            android.net.Uri.parse(track.streamUrl)
        } else {
            track.uri
        }
        val metadataBuilder = MediaMetadata.Builder()
            .setTitle(track.title)
            .setArtist(track.artist)
            .setAlbumTitle(track.album)
        if (track.artworkUri != null) {
            metadataBuilder.setArtworkUri(track.artworkUri)
        }
        return MediaItem.Builder()
            .setMediaId(track.id)
            .setUri(targetUri)
            .setMediaMetadata(metadataBuilder.build())
            .build()
    }

    private fun syncMiniPlayerUi(track: RealAudioTrack, playing: Boolean) {
        val subtitle = buildString {
            append(track.artist.ifBlank { "Unknown Artist" })
            if (track.album.isNotBlank()) {
                append(" • ")
                append(track.album)
            }
        }
        AppSettings.updateMiniPlayer(track.title, subtitle, playing)
        appContext?.let { ctx ->
            val targetUri = if (track.isRadio && track.streamUrl.isNotBlank()) android.net.Uri.parse(track.streamUrl) else track.uri
            PlaybackHistoryBridge.recordPlayback(
                context = ctx,
                uri = targetUri,
                title = track.title,
                mimeType = "audio/*",
                positionMs = exoPlayer?.currentPosition?.coerceAtLeast(0L) ?: 0L,
                durationMs = track.durationMs.coerceAtLeast(0L),
                rememberPosition = false
            )
        }
    }

    fun applyAudioEffectsNow() {
        val sid = activeSessionId.takeIf { it != C.AUDIO_SESSION_ID_UNSET && it != 0 }
            ?: exoPlayer?.audioSessionId?.takeIf { it != C.AUDIO_SESSION_ID_UNSET && it != 0 }
            ?: return
        val bandsDb = EqualizerPresetsCatalog.bandsForPreset(
            AppSettings.eqPreset.value,
            AppSettings.eqCustomBandsDb.value
        )
        try {
            val eq = equalizer ?: Equalizer(0, sid).also { equalizer = it }
            eq.enabled = true
            val bands = eq.numberOfBands.toInt()
            val minLv = eq.bandLevelRange[0]
            val maxLv = eq.bandLevelRange[1]
            for (b in 0 until bands) {
                val targetDb = bandsDb.getOrElse((b * 5) / bands.coerceAtLeast(1)) { 0f }
                val ratio = (targetDb / 12f).coerceIn(-1f, 1f)
                val level: Short = if (ratio >= 0f) (ratio * maxLv).roundToInt().toShort()
                else (-ratio * minLv).roundToInt().toShort()
                eq.setBandLevel(b.toShort(), level.coerceIn(minLv, maxLv))
            }
        } catch (_: Throwable) {
        }
        try {
            val normEnabled = AppSettings.volumeNormalization.value
            if (normEnabled) {
                val enh = loudnessNormEnhancer ?: LoudnessEnhancer(sid).also { loudnessNormEnhancer = it }
                enh.setTargetGain(350) // +3.5 dB gentle leveling for quiet tracks
                enh.enabled = true
            } else {
                loudnessNormEnhancer?.enabled = false
            }
        } catch (_: Throwable) {
        }
    }

    private fun scheduleStandbyIfEnabled() {
        standbyReleaseJob?.cancel()
        val delaySec = AppSettings.audioStandbyDelaySec.value
        if (delaySec <= 0) return
        standbyReleaseJob = scope.launch {
            delay(delaySec * 1000L)
            if (!_isPlaying.value && exoPlayer != null) {
                release()
            }
        }
    }

    fun setPlaybackSpeed(speed: Float) {
        val clamped = speed.coerceIn(0.5f, 3.0f)
        val pitch = if (AppSettings.audioPitchCorrection.value) 1.0f else clamped
        exoPlayer?.playbackParameters = PlaybackParameters(clamped, pitch)
    }

    fun setSleepTimerMinutes(minutes: Int) {
        sleepTimerJob?.cancel()
        val totalMs = (minutes * 60_000L).coerceAtLeast(60_000L)
        _sleepTimerRemainingMs.value = totalMs
        exoPlayer?.volume = 1.0f
        sleepTimerJob = scope.launch {
            while (_sleepTimerRemainingMs.value > 0L && isActive) {
                delay(1000L)
                val next = (_sleepTimerRemainingMs.value - 1000L).coerceAtLeast(0L)
                _sleepTimerRemainingMs.value = next
                if (next in 1L..5000L) {
                    exoPlayer?.volume = (next.toFloat() / 5000f).coerceIn(0.05f, 1f)
                }
                if (next == 0L) {
                    exoPlayer?.pause()
                    exoPlayer?.volume = 1.0f
                }
            }
        }
    }

    fun cancelSleepTimer() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        _sleepTimerRemainingMs.value = 0L
        exoPlayer?.volume = 1.0f
    }

    fun playQueue(queue: List<RealAudioTrack>, startIndex: Int = 0, startPositionMs: Long = 0L) {
        if (queue.isEmpty()) return
        val ctx = appContext ?: return
        val safeIndex = startIndex.coerceIn(0, queue.lastIndex)
        _lastError.value = null
        _playlist.value = queue
        _currentIndex.value = safeIndex
        val activeTrack = queue[safeIndex]
        _currentTrack.value = activeTrack
        _durationMs.value = activeTrack.durationMs
        _currentPositionMs.value = startPositionMs.coerceAtLeast(0L)

        val player = getOrCreatePlayer(ctx)
        val mediaItems = queue.map(::toMediaItem)
        player.setMediaItems(mediaItems, safeIndex, startPositionMs.coerceAtLeast(0L))
        player.prepare()
        player.playWhenReady = true
        player.play()

        _isPlaying.value = true
        syncMiniPlayerUi(activeTrack, true)
        startPositionPolling()
        ensureServiceStarted()
    }

    fun playTrack(track: RealAudioTrack) {
        val currentQueue = _playlist.value
        val existingIdx = currentQueue.indexOfFirst { it.id == track.id }
        if (existingIdx >= 0 && exoPlayer != null && exoPlayer?.mediaItemCount == currentQueue.size) {
            _currentIndex.value = existingIdx
            _currentTrack.value = track
            _durationMs.value = track.durationMs
            _currentPositionMs.value = 0L
            exoPlayer?.seekToDefaultPosition(existingIdx)
            exoPlayer?.playWhenReady = true
            exoPlayer?.play()
            syncMiniPlayerUi(track, true)
            ensureServiceStarted()
        } else {
            playQueue(listOf(track), 0)
        }
    }

    fun togglePlayPause() {
        val player = exoPlayer
        if (player == null) {
            val track = _currentTrack.value ?: return
            playQueue(_playlist.value.ifEmpty { listOf(track) }, _currentIndex.value)
            return
        }
        if (player.isPlaying) {
            player.pause()
            _isPlaying.value = false
            AppSettings.setMiniPlayerPlaying(false)
        } else {
            if (player.playbackState == Player.STATE_ENDED) {
                player.seekTo(0L)
            }
            if (player.playbackState == Player.STATE_IDLE) {
                player.prepare()
            }
            player.play()
            _isPlaying.value = true
            AppSettings.setMiniPlayerPlaying(true)
            startPositionPolling()
            ensureServiceStarted()
        }
    }

    fun seekTo(positionMs: Long) {
        val clamped = positionMs.coerceAtLeast(0L)
        exoPlayer?.seekTo(clamped)
        _currentPositionMs.value = clamped
        val dur = _durationMs.value
        if (dur > 0L) {
            AppSettings.setMiniPlayerProgress((clamped.toFloat() / dur.toFloat()).coerceIn(0f, 1f))
        }
    }

    fun playNext() {
        val player = exoPlayer ?: return
        if (player.hasNextMediaItem()) {
            player.seekToNextMediaItem()
            player.play()
        } else if (_playlist.value.isNotEmpty() && _isRepeat.value) {
            player.seekToDefaultPosition(0)
            player.play()
        }
    }

    fun playPrevious() {
        val player = exoPlayer ?: return
        if (player.currentPosition > 3500L) {
            player.seekTo(0L)
        } else if (player.hasPreviousMediaItem()) {
            player.seekToPreviousMediaItem()
            player.play()
        } else {
            player.seekTo(0L)
        }
    }

    fun toggleShuffle() {
        val next = !_isShuffle.value
        _isShuffle.value = next
        exoPlayer?.shuffleModeEnabled = next
    }

    fun toggleRepeat() {
        val next = !_isRepeat.value
        _isRepeat.value = next
        exoPlayer?.repeatMode = if (next) Player.REPEAT_MODE_ALL else Player.REPEAT_MODE_OFF
    }

    fun removeFromQueue(index: Int) {
        val list = _playlist.value.toMutableList()
        if (index !in list.indices) return
        list.removeAt(index)
        _playlist.value = list
        exoPlayer?.removeMediaItem(index)
        if (list.isEmpty()) {
            release()
            return
        }
        val newIdx = exoPlayer?.currentMediaItemIndex?.coerceIn(0, list.lastIndex) ?: 0
        _currentIndex.value = newIdx
        _currentTrack.value = list[newIdx]
        syncMiniPlayerUi(list[newIdx], exoPlayer?.isPlaying == true)
    }

    fun release() {
        sleepTimerJob?.cancel()
        sleepTimerJob = null
        standbyReleaseJob?.cancel()
        standbyReleaseJob = null
        _sleepTimerRemainingMs.value = 0L
        try { equalizer?.release() } catch (_: Throwable) {}
        equalizer = null
        try { loudnessNormEnhancer?.release() } catch (_: Throwable) {}
        loudnessNormEnhancer = null
        activeSessionId = C.AUDIO_SESSION_ID_UNSET
        positionPollJob?.cancel()
        positionPollJob = null
        mediaControllerFuture?.let { future ->
            try {
                MediaController.releaseFuture(future)
            } catch (_: Throwable) {
            }
        }
        mediaControllerFuture = null
        MediaPlaybackService.stopAndRelease()
        exoPlayer?.let { player ->
            player.removeListener(playerListener)
            player.stop()
            player.release()
        }
        exoPlayer = null
        _isPlaying.value = false
        _currentPositionMs.value = 0L
        _durationMs.value = 0L
        _currentTrack.value = null
        _playlist.value = emptyList()
        AppSettings.dismissMiniPlayer()
        appContext?.let { ctx ->
            try {
                ctx.stopService(Intent(ctx, MediaPlaybackService::class.java))
            } catch (_: Throwable) {
            }
        }
    }
}
