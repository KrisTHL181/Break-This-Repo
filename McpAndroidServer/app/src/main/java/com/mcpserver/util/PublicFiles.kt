package com.mcpserver.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import java.io.File

/**
 * Writes files to places the user can actually reach.
 *
 * On API 29+ scoped storage blocks direct writes into /sdcard/Download, so the
 * primary route is a MediaStore insert. A direct file write is still attempted
 * first because it also works when the app holds "all files" access.
 */
object PublicFiles {

    private const val TAG = "PublicFiles"

    /** Public Download folder as a plain filesystem path. */
    @Suppress("DEPRECATION")
    val downloadDir: File
        get() = File(Environment.getExternalStorageDirectory(), Environment.DIRECTORY_DOWNLOADS)

    /**
     * Save [bytes] as [fileName] inside the public Download folder.
     * @return the absolute path, or null when both routes failed.
     */
    fun saveToDownloads(context: Context, fileName: String, mimeType: String, bytes: ByteArray): String? {
        runCatching {
            val dir = downloadDir
            if (dir.isDirectory || dir.mkdirs()) {
                val target = File(dir, fileName)
                target.writeBytes(bytes)
                Log.i(TAG, "direct write ok: ${target.absolutePath}")
                return target.absolutePath
            }
        }.onFailure { Log.w(TAG, "direct download write failed", it) }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            runCatching {
                val resolver = context.contentResolver
                val collection = MediaStore.Downloads.EXTERNAL_CONTENT_URI
                runCatching {
                    resolver.query(
                        collection,
                        arrayOf(MediaStore.MediaColumns._ID),
                        "${MediaStore.MediaColumns.DISPLAY_NAME}=?",
                        arrayOf(fileName),
                        null
                    )?.use { cursor ->
                        while (cursor.moveToNext()) {
                            val id = cursor.getLong(0)
                            resolver.delete(Uri.withAppendedPath(collection, id.toString()), null, null)
                        }
                    }
                }
                val values = ContentValues().apply {
                    put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                    put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
                    put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                }
                val uri = resolver.insert(collection, values) ?: return@runCatching
                resolver.openOutputStream(uri)?.use { it.write(bytes) }
                Log.i(TAG, "MediaStore write ok: $uri")
                return File(downloadDir, fileName).absolutePath
            }.onFailure { Log.w(TAG, "MediaStore write failed", it) }
        }
        return null
    }

    /** Save [bytes] inside the app-private external dir (always writable). */
    fun saveToAppExternal(context: Context, fileName: String, bytes: ByteArray): String? =
        runCatching {
            val dir = context.getExternalFilesDir(null) ?: return@runCatching null
            val target = File(dir, fileName)
            target.writeBytes(bytes)
            target.absolutePath
        }.getOrNull()
}
