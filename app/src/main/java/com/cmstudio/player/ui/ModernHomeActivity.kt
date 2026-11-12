package com.cmstudio.player.ui

import android.Manifest
import android.app.Activity
import android.app.RecoverableSecurityException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import com.cmstudio.player.ui.model.MediaVideoItem
import com.cmstudio.player.ui.player.ModernPlayerActivity
import com.cmstudio.player.ui.theme.MpvxTheme

class ModernHomeActivity : ComponentActivity() {

    private val viewModel: MediaViewModel by viewModels()
    private var requestedNavTab by mutableStateOf<String?>(null)

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.entries.any { it.value }
        if (granted) {
            viewModel.loadMedia()
        }
    }

    private val deleteMediaLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            Toast.makeText(this, "Video deleted", Toast.LENGTH_SHORT).show()
            viewModel.loadMedia()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        AppSettings.init(this)
        com.cmstudio.player.ui.music.AudioPlayerManager.init(this)
        checkAndRequestPermissions()
        handleIncomingIntent(intent)

        setContent {
            val darkMode by AppSettings.darkMode.collectAsState()
            val systemDark = isSystemInDarkTheme()
            val effectiveDark = when (darkMode.uppercase()) {
                "DARK" -> true
                "LIGHT" -> false
                else -> systemDark
            }
            val themePreset by AppSettings.themePreset.collectAsState()
            val isAmoled by AppSettings.isAmoled.collectAsState()
            val customPrimary by AppSettings.customPrimaryColor.collectAsState()
            val customAccent by AppSettings.customAccentColor.collectAsState()
            val customBg by AppSettings.customBackgroundColor.collectAsState()
            val customSurface by AppSettings.customSurfaceColor.collectAsState()

            MpvxTheme(
                darkTheme = effectiveDark,
                themePreset = themePreset,
                isAmoled = isAmoled,
                customPrimaryArgb = customPrimary,
                customAccentArgb = customAccent,
                customBackgroundArgb = customBg,
                customSurfaceArgb = customSurface
            ) {
                val continueWatching by viewModel.continueWatching.collectAsState()
                val recentlyAdded by viewModel.recentlyAdded.collectAsState()
                val allVideos by viewModel.allVideos.collectAsState()
                val allScannedVideos by viewModel.allScannedVideos.collectAsState()
                val folders by viewModel.folders.collectAsState()
                val historyEntries by viewModel.historyEntries.collectAsState()
                val watchStreakGoal by viewModel.watchStreakGoal.collectAsState()

                MainScreen(
                    continueWatchingVideos = continueWatching,
                    recentlyAddedVideos = recentlyAdded,
                    allVideos = allVideos,
                    allScannedVideos = allScannedVideos,
                    folders = folders,
                    historyEntries = historyEntries,
                    watchStreakGoal = watchStreakGoal,
                    requestedDestinationId = requestedNavTab,
                    onConsumeRequestedDestination = { requestedNavTab = null },
                    onVideoClick = { video ->
                        playVideo(video.uri, video.title, queue = allVideos)
                    },
                    onVideoQueuePlay = { video, queue ->
                        playVideo(video.uri, video.title, queue = queue)
                    },
                    onPlayUriWithTitle = { uri, title, startPos ->
                        playVideo(uri, title, startPositionMs = startPos)
                    },
                    onPlayNetworkStreamClick = { streamUrl ->
                        try {
                            playVideo(Uri.parse(streamUrl), "Network Stream")
                        } catch (_: Exception) {
                            Toast.makeText(this, "Invalid Stream URL", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onRefreshLibrary = {
                        viewModel.loadMedia()
                    },
                    onRemoveHistoryEntry = { uri ->
                        viewModel.removeHistoryEntry(uri)
                    },
                    onClearHistory = {
                        viewModel.clearPlaybackHistory()
                    },
                    onDeleteHistoryFiles = { entries ->
                        deleteUris(entries.filter { !it.isNetwork }.map { it.uri }, "file(s)")
                    },
                    onDeleteVideoClick = { video ->
                        deleteVideoItems(listOf(video))
                    },
                    onDeleteVideosBatch = { videos ->
                        deleteVideoItems(videos)
                    },
                    onToggleVideoWatched = { video ->
                        viewModel.toggleVideoWatched(video)
                    },
                    onToggleFolderWatched = { folderId ->
                        viewModel.toggleFolderWatched(folderId)
                    },
                    onOpenSystemPickerFile = { uri ->
                        playVideo(uri, uri.lastPathSegment, queue = emptyList())
                    }
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.loadMedia()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIncomingIntent(intent)
    }

    private fun handleIncomingIntent(intent: Intent?) {
        val openTab = intent?.getStringExtra("open_tab")
        if (!openTab.isNullOrBlank()) {
            requestedNavTab = openTab
        } else if (intent?.getBooleanExtra("open_music", false) == true) {
            requestedNavTab = "MUSIC"
        }

        val incomingUri = intent?.data ?: intent?.getStringExtra("video_uri")?.let { Uri.parse(it) }
        if (incomingUri != null && (intent?.action == Intent.ACTION_VIEW || intent?.hasExtra("video_uri") == true)) {
            playVideo(incomingUri, intent.getStringExtra("video_title"))
        }
    }

    private fun playVideo(
        uri: Uri,
        title: String? = null,
        queue: List<MediaVideoItem> = emptyList(),
        startPositionMs: Long = -1L
    ) {
        val intent = Intent(this, ModernPlayerActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            setDataAndType(uri, "video/*")
            putExtra("video_uri", uri.toString())
            if (!title.isNullOrBlank()) putExtra("video_title", title)
            if (startPositionMs >= 0L) putExtra("start_position_ms", startPositionMs)
            if (queue.isNotEmpty()) {
                val uris = ArrayList<String>(queue.map { it.uri.toString() })
                val titles = ArrayList<String>(queue.map { it.title })
                val idx = queue.indexOfFirst { it.uri == uri }.coerceAtLeast(0)
                putStringArrayListExtra("queue_uris", uris)
                putStringArrayListExtra("queue_titles", titles)
                putExtra("queue_index", idx)
            }
            putExtra("from_home", true)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        startActivity(intent)
    }

    private fun deleteVideoItems(videos: List<MediaVideoItem>) {
        deleteUris(videos.map { it.uri }, "video(s)")
    }

    private fun deleteUris(uris: List<Uri>, noun: String) {
        if (uris.isEmpty()) return
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val contentUris = uris.filter { it.scheme == "content" }
                if (contentUris.isNotEmpty()) {
                    val pendingIntent = MediaStore.createDeleteRequest(contentResolver, contentUris)
                    deleteMediaLauncher.launch(IntentSenderRequest.Builder(pendingIntent.intentSender).build())
                }
                return
            }
            var deletedCount = 0
            uris.forEach { uri ->
                if (uri.scheme == "content") {
                    deletedCount += contentResolver.delete(uri, null, null)
                } else if (uri.scheme == "file" && !uri.path.isNullOrBlank()) {
                    if (java.io.File(uri.path!!).delete()) deletedCount++
                }
            }
            if (deletedCount > 0) {
                Toast.makeText(this, "Deleted $deletedCount $noun", Toast.LENGTH_SHORT).show()
                viewModel.loadMedia()
            } else {
                Toast.makeText(this, "Could not delete selected files", Toast.LENGTH_SHORT).show()
            }
        } catch (securityException: SecurityException) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q && securityException is RecoverableSecurityException) {
                val intentSender = securityException.userAction.actionIntent.intentSender
                deleteMediaLauncher.launch(IntentSenderRequest.Builder(intentSender).build())
            } else {
                Toast.makeText(this, "Permission required to delete files", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Delete failed: ${e.localizedMessage}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun checkAndRequestPermissions() {
        val permissions = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_VIDEO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_VIDEO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissions.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        }
    }
}
