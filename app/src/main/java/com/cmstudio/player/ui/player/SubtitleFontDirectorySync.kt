package com.cmstudio.player.ui.player

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import com.cmstudio.player.engine.MpvEngineController
import com.cmstudio.player.ui.AppSettings
import java.io.File
import java.io.FileOutputStream

/**
 * Copies user-selected `.ttf`/`.otf` fonts from a SAF directory URI into `filesDir/fonts/`
 * so both `libmpv` (`sub-fonts-dir`) and ExoPlayer (`SubtitleView`) load them without needing
 * broad filesystem storage permissions.
 */
object SubtitleFontDirectorySync {

    fun syncFromTreeUri(context: Context, treeUri: Uri): Int {
        runCatching {
            val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION
            context.contentResolver.takePersistableUriPermission(treeUri, flags)
        }
        val targetDir = File(context.filesDir, "fonts").apply { mkdirs() }
        val docId = runCatching { DocumentsContract.getTreeDocumentId(treeUri) }.getOrNull()
        val label = docId?.substringAfterLast(':')?.substringAfterLast('/')?.ifBlank { "Custom Fonts" } ?: "Custom Fonts"
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, docId)
        val projection = arrayOf(
            DocumentsContract.Document.COLUMN_DOCUMENT_ID,
            DocumentsContract.Document.COLUMN_DISPLAY_NAME,
            DocumentsContract.Document.COLUMN_MIME_TYPE
        )
        var copiedCount = 0
        var firstCopiedFile: File? = null
        var firstFontName: String? = null

        runCatching {
            context.contentResolver.query(childrenUri, projection, null, null, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
                val nameCol = cursor.getColumnIndexOrThrow(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
                while (cursor.moveToNext() && copiedCount < 24) {
                    val childId = cursor.getString(idCol) ?: continue
                    val name = cursor.getString(nameCol) ?: continue
                    val lower = name.lowercase()
                    if (!lower.endsWith(".ttf") && !lower.endsWith(".otf") && !lower.endsWith(".ttc")) continue
                    val docUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, childId)
                    val safeName = name.replace(Regex("""[^A-Za-z0-9._-]"""), "_")
                    val dest = File(targetDir, safeName)
                    runCatching {
                        context.contentResolver.openInputStream(docUri)?.use { input ->
                            FileOutputStream(dest).use { output -> input.copyTo(output) }
                        }
                        if (dest.isFile && dest.length() > 0L) {
                            copiedCount++
                            if (firstCopiedFile == null) {
                                firstCopiedFile = dest
                                firstFontName = name
                            }
                        }
                    }
                }
            }
        }

        AppSettings.setSubFontsDir(treeUri.toString(), "$label ($copiedCount fonts)")
        val firstFont = firstCopiedFile
        val firstFontTitle = firstFontName
        if (firstFont != null && !firstFontTitle.isNullOrBlank()) {
            AppSettings.setSubtitleCustomFont(firstFont.absolutePath, firstFontTitle)
        }
        MpvEngineController.setSubtitleFontsDir(targetDir.absolutePath)
        return copiedCount
    }

    fun clearFontsDirectory(context: Context) {
        AppSettings.setSubFontsDir("", "App Default")
        val targetDir = File(context.filesDir, "fonts")
        runCatching {
            targetDir.listFiles()?.forEach { file ->
                if (!file.name.equals("custom_subtitle_font.ttf", ignoreCase = true)) {
                    file.delete()
                }
            }
        }
        MpvEngineController.setSubtitleFontsDir(targetDir.apply { mkdirs() }.absolutePath)
    }
}
