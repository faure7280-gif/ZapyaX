package com.example.data

import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.webkit.MimeTypeMap
import androidx.core.content.FileProvider
import com.example.model.FileCategory
import com.example.model.TransferFile
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream

object DeviceStorageHelper {

    fun queryDeviceMedia(context: Context, category: FileCategory): List<TransferFile> {
        val result = mutableListOf<TransferFile>()
        val resolver = context.contentResolver

        try {
            when (category) {
                FileCategory.PHOTOS -> {
                    queryCollection(
                        resolver,
                        MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Images.Media._ID,
                        MediaStore.Images.Media.DISPLAY_NAME,
                        MediaStore.Images.Media.SIZE,
                        MediaStore.Images.Media.MIME_TYPE,
                        FileCategory.PHOTOS,
                        result
                    )
                }
                FileCategory.VIDEOS -> {
                    queryCollection(
                        resolver,
                        MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Video.Media._ID,
                        MediaStore.Video.Media.DISPLAY_NAME,
                        MediaStore.Video.Media.SIZE,
                        MediaStore.Video.Media.MIME_TYPE,
                        FileCategory.VIDEOS,
                        result
                    )
                }
                FileCategory.AUDIO -> {
                    queryCollection(
                        resolver,
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        MediaStore.Audio.Media._ID,
                        MediaStore.Audio.Media.DISPLAY_NAME,
                        MediaStore.Audio.Media.SIZE,
                        MediaStore.Audio.Media.MIME_TYPE,
                        FileCategory.AUDIO,
                        result
                    )
                }
                FileCategory.DOCUMENTS, FileCategory.ARCHIVES -> {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        queryCollection(
                            resolver,
                            MediaStore.Downloads.EXTERNAL_CONTENT_URI,
                            MediaStore.Downloads._ID,
                            MediaStore.Downloads.DISPLAY_NAME,
                            MediaStore.Downloads.SIZE,
                            MediaStore.Downloads.MIME_TYPE,
                            category,
                            result
                        )
                    }
                }
                FileCategory.ALL -> {
                    result.addAll(queryDeviceMedia(context, FileCategory.PHOTOS))
                    result.addAll(queryDeviceMedia(context, FileCategory.VIDEOS))
                    result.addAll(queryDeviceMedia(context, FileCategory.AUDIO))
                }
            }
        } catch (e: Exception) {
            // Permission not granted or empty collection
        }

        return result.take(50)
    }

    private fun queryCollection(
        resolver: ContentResolver,
        collectionUri: Uri,
        idCol: String,
        nameCol: String,
        sizeCol: String,
        mimeCol: String,
        category: FileCategory,
        targetList: MutableList<TransferFile>
    ) {
        val projection = arrayOf(idCol, nameCol, sizeCol, mimeCol)
        val sortOrder = "$idCol DESC"

        resolver.query(collectionUri, projection, null, null, sortOrder)?.use { cursor ->
            val idIndex = cursor.getColumnIndex(idCol)
            val nameIndex = cursor.getColumnIndex(nameCol)
            val sizeIndex = cursor.getColumnIndex(sizeCol)
            val mimeIndex = cursor.getColumnIndex(mimeCol)

            while (cursor.moveToNext() && targetList.size < 40) {
                val id = if (idIndex != -1) cursor.getLong(idIndex) else 0L
                val name = if (nameIndex != -1) cursor.getString(nameIndex) ?: "archivo" else "archivo"
                val size = if (sizeIndex != -1) cursor.getLong(sizeIndex) else 0L
                val mime = if (mimeIndex != -1) cursor.getString(mimeIndex) ?: "application/octet-stream" else "application/octet-stream"

                val contentUri = Uri.withAppendedPath(collectionUri, id.toString())

                if (size > 0) {
                    targetList.add(
                        TransferFile(
                            id = "media_$id",
                            name = name,
                            sizeBytes = size,
                            mimeType = mime,
                            uriString = contentUri.toString(),
                            category = category,
                            isSimulated = false
                        )
                    )
                }
            }
        }
    }

    fun openFile(context: Context, filePath: String, mimeType: String) {
        try {
            val file = File(filePath)
            if (!file.exists()) return

            val uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            } else {
                Uri.fromFile(file)
            }

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun getReceivedDirectory(context: Context): File {
        val downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        val zapyaDir = File(downloadsDir, "ZapyaX")
        if (!zapyaDir.exists()) {
            zapyaDir.mkdirs()
        }
        return if (zapyaDir.exists() && zapyaDir.canWrite()) {
            zapyaDir
        } else {
            val externalDir = context.getExternalFilesDir("ZapyaX_Received") ?: context.filesDir
            if (!externalDir.exists()) externalDir.mkdirs()
            externalDir
        }
    }

    fun notifyMediaScanner(context: Context, file: File, mimeType: String) {
        MediaScannerConnection.scanFile(
            context,
            arrayOf(file.absolutePath),
            arrayOf(mimeType),
            null
        )
    }
}
