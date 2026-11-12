package com.cmstudio.player.ui.music

import android.content.ContentUris
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import java.io.File
import java.util.Locale

data class RealAudioTrack(
    val id: String,
    val uri: Uri,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val durationText: String,
    val sizeBytes: Long,
    val sizeText: String,
    val artworkUri: Uri?,
    val path: String,
    val folderName: String,
    val isRadio: Boolean = false,
    val streamUrl: String = ""
)

data class RealAudioAlbum(
    val id: String,
    val title: String,
    val artist: String,
    val trackCount: Int,
    val artworkUri: Uri?
)

data class RealAudioArtist(
    val name: String,
    val trackCount: Int,
    val albumCount: Int
)

data class RealAudioFolder(
    val name: String,
    val path: String,
    val trackCount: Int
)

object AudioLibrary {

    fun scanAudio(context: Context): List<RealAudioTrack> {
        val tracks = mutableListOf<RealAudioTrack>()
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.SIZE,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID
        )

        val minDurationMs = com.cmstudio.player.ui.AppSettings.minAudioDurationSec.value.coerceIn(0, 300) * 1000L
        val selection = if (minDurationMs > 0L) {
            "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} >= $minDurationMs"
        } else {
            "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        }
        val sortOrder = "${MediaStore.Audio.Media.TITLE} COLLATE NOCASE ASC"

        try {
            val cursor: Cursor? = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val sizeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Track"
                    val artist = c.getString(artistCol).takeIf { !it.isNullOrBlank() && it != "<unknown>" } ?: "Unknown Artist"
                    val album = c.getString(albumCol).takeIf { !it.isNullOrBlank() && it != "<unknown>" } ?: "Unknown Album"
                    val durationMs = c.getLong(durationCol)
                    val size = c.getLong(sizeCol)
                    val path = c.getString(dataCol) ?: ""
                    val albumId = c.getLong(albumIdCol)

                    val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artworkUri = try {
                        ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"), albumId)
                    } catch (e: Exception) {
                        null
                    }

                    val folderName = if (path.isNotBlank()) {
                        File(path).parentFile?.name ?: "Music"
                    } else "Music"

                    tracks.add(
                        RealAudioTrack(
                            id = id.toString(),
                            uri = uri,
                            title = title,
                            artist = artist,
                            album = album,
                            durationMs = durationMs,
                            durationText = formatDuration(durationMs),
                            sizeBytes = size,
                            sizeText = formatFileSize(size),
                            artworkUri = artworkUri,
                            path = path,
                            folderName = folderName
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return tracks
    }

    fun groupAlbums(tracks: List<RealAudioTrack>): List<RealAudioAlbum> {
        return tracks.groupBy { it.album }
            .map { (albumTitle, albumTracks) ->
                val first = albumTracks.first()
                RealAudioAlbum(
                    id = albumTitle,
                    title = albumTitle,
                    artist = first.artist,
                    trackCount = albumTracks.size,
                    artworkUri = first.artworkUri
                )
            }.sortedBy { it.title.lowercase(Locale.ROOT) }
    }

    fun groupArtists(tracks: List<RealAudioTrack>): List<RealAudioArtist> {
        return tracks.groupBy { it.artist }
            .map { (artistName, artistTracks) ->
                val albumCount = artistTracks.map { it.album }.distinct().size
                RealAudioArtist(
                    name = artistName,
                    trackCount = artistTracks.size,
                    albumCount = albumCount
                )
            }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    fun groupFolders(tracks: List<RealAudioTrack>): List<RealAudioFolder> {
        return tracks.groupBy { it.folderName }
            .map { (folderName, folderTracks) ->
                val path = folderTracks.firstOrNull()?.path?.let { File(it).parent ?: "" } ?: ""
                RealAudioFolder(
                    name = folderName,
                    path = path,
                    trackCount = folderTracks.size
                )
            }.sortedBy { it.name.lowercase(Locale.ROOT) }
    }

    private fun formatDuration(ms: Long): String {
        val totalSec = ms / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return String.format(Locale.US, "%02d:%02d", min, sec)
    }

    private fun formatFileSize(bytes: Long): String {
        if (bytes <= 0) return "0 MB"
        val mb = bytes.toDouble() / (1024 * 1024)
        return String.format(Locale.US, "%.1f MB", mb)
    }
}
