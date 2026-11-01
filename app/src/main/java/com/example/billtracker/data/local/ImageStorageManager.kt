package com.example.billtracker.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

object ImageStorageManager {

    private const val PHOTO_DIR = "meter_photos"
    private const val MAX_IMAGE_DIMENSION = 1280

    private fun getPhotoDirectory(context: Context): File {
        val dir = File(context.filesDir, PHOTO_DIR)
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    /**
     * Copies and compresses a Uri image into app private storage.
     * Returns the stored File absolutePath.
     */
    suspend fun saveImageFromUri(context: Context, uri: Uri): String? = withContext(Dispatchers.IO) {
        try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return@withContext null
            val originalBitmap = BitmapFactory.decodeStream(inputStream)
            inputStream.close()
            if (originalBitmap == null) return@withContext null

            saveResizedBitmap(context, originalBitmap)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Saves a camera preview Bitmap into app private storage after resizing.
     */
    suspend fun saveBitmap(context: Context, bitmap: Bitmap): String? = withContext(Dispatchers.IO) {
        try {
            saveResizedBitmap(context, bitmap)
        } catch (_: Exception) {
            null
        }
    }

    private fun saveResizedBitmap(context: Context, bitmap: Bitmap): String {
        val resized = resizeBitmap(bitmap, MAX_IMAGE_DIMENSION)
        val file = File(getPhotoDirectory(context), "photo_${UUID.randomUUID()}.jpg")
        val outputStream = FileOutputStream(file)
        resized.compress(Bitmap.CompressFormat.JPEG, 85, outputStream)
        outputStream.flush()
        outputStream.close()
        return file.absolutePath
    }

    private fun resizeBitmap(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap

        val ratio = width.toFloat() / height.toFloat()
        val newWidth: Int
        val newHeight: Int
        if (width > height) {
            newWidth = maxDimension
            newHeight = (maxDimension / ratio).toInt()
        } else {
            newHeight = maxDimension
            newWidth = (maxDimension * ratio).toInt()
        }
        return Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
    }

    /**
     * Delete an unused photo file safely.
     */
    suspend fun deletePhotoFile(path: String?): Boolean = withContext(Dispatchers.IO) {
        if (path.isNullOrBlank()) return@withContext false
        try {
            val file = File(path)
            if (file.exists()) file.delete() else false
        } catch (_: Exception) {
            false
        }
    }
}
