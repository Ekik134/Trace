package com.example.trace.data.local

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import java.io.File
import java.io.FileOutputStream

class FileStorageManager(private val context: Context) {

    // Сохраняет файл и возвращает путь к нему и путь к превью (если есть)
    fun saveFileToInternalStorage(uri: Uri, mimeType: String?): Pair<String?, String?> {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return Pair(null, null)

            // Генерируем уникальное имя файла
            val fileName = "file_${System.currentTimeMillis()}.${getExtension(mimeType)}"
            val file = File(context.filesDir, fileName)

            // Копируем файл
            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)
            inputStream.close()
            outputStream.close()

            // Создаем миниатюру (только для картинок)
            var previewPath: String? = null
            if (mimeType?.startsWith("image/") == true) {
                previewPath = createThumbnail(file, fileName)
            }

            Pair(file.absolutePath, previewPath)
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(null, null)
        }
    }

    private fun createThumbnail(originalFile: File, fileName: String): String? {
        return try {
            // Декодируем картинку с уменьшением размера (для экономии памяти)
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(originalFile.absolutePath, options)

            options.inSampleSize = calculateInSampleSize(options, 200, 200)
            options.inJustDecodeBounds = false

            val bitmap = BitmapFactory.decodeFile(originalFile.absolutePath, options) ?: return null

            // Сохраняем превью
            val previewFile = File(context.filesDir, "thumb_$fileName.jpg")
            val outputStream = FileOutputStream(previewFile)
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            outputStream.close()

            previewFile.absolutePath
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1
        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2
            while (halfHeight / inSampleSize >= reqHeight && halfWidth / inSampleSize >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

    private fun getExtension(mimeType: String?): String {
        return when (mimeType) {
            "image/png" -> "png"
            "image/jpeg" -> "jpg"
            "image/gif" -> "gif"
            "application/pdf" -> "pdf"
            else -> "tmp"
        }
    }
}