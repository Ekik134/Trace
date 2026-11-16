package com.example.trace.data.repository

import android.content.Context
import android.net.Uri
import com.example.trace.data.local.FileStorageManager
import com.example.trace.data.local.dao.FileDao
import com.example.trace.data.local.dao.TagDao
import com.example.trace.data.local.entity.FileEntity
import com.example.trace.data.local.entity.FileTagCrossRef
import com.example.trace.data.local.model.FileWithTags
import com.example.trace.data.model.FileModel
import com.example.trace.data.model.TagModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class FileRepository(
    private val context: Context,
    private val fileDao: FileDao,
    private val tagDao: TagDao
) {

    // --- ПОЛУЧЕНИЕ ДАННЫХ ---
// Возвращает все файлы (с тегами)
    fun getAllFiles(): Flow<List<FileModel>> {
        return fileDao.getAllFilesWithTags().map { entities ->
            entities.map { fileWithTags ->
                fileWithTags.file.toModel().copy(
                    tags = fileWithTags.tags.map { TagModel(it.id, it.name, it.iconUri) }
                )
            }
        }
    }

    fun searchFilesWithTags(query: String): Flow<List<FileModel>> {
        return fileDao.searchFilesWithTags(query).map { entities ->
            entities.map { fileWithTags ->
                fileWithTags.file.toModel().copy(
                    tags = fileWithTags.tags.map { TagModel(it.id, it.name, it.iconUri) }
                )
            }
        }
    }

    suspend fun getFileWithTags(fileId: Long): FileWithTags? {
        return withContext(Dispatchers.IO) {
            fileDao.getFileWithTags(fileId)
        }
    }

    fun searchFiles(query: String): Flow<List<FileModel>> {
        return fileDao.searchFiles(query).map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun getFilesByTag(tagId: Long): Flow<List<FileModel>> {
        return fileDao.getFilesByTag(tagId).map { entities ->
            entities.map { it.toModel() }
        }
    }

    fun getFilesCount(): Flow<Int> {
        return fileDao.getFilesCount()
    }

    // --- СОХРАНЕНИЕ И РЕДАКТИРОВАНИЕ ---
    suspend fun addFile(
        uri: Uri,
        name: String,
        description: String?,
        mimeType: String?,
        size: Long?,
        tagIds: List<Long>
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            // 1. Используем FileStorageManager для копирования и создания миниатюры
            val storageManager = FileStorageManager(context)
            val (localPath, previewPath) = storageManager.saveFileToInternalStorage(uri, mimeType)

            if (localPath == null) {
                return@withContext Result.failure(Exception("Не удалось сохранить файл на устройстве"))
            }

            // 2. Создаем сущность
            val fileEntity = FileEntity(
                name = name,
                description = description,
                localUri = localPath,
                createdAt = getCurrentDate(),
                mimeType = mimeType,
                size = size,
                previewPath = previewPath // <--- Теперь путь к миниатюре сохраняется!
            )

            // 3. Вставляем в БД
            val newFileId = fileDao.insertFile(fileEntity)

            // 4. Привязываем теги
            tagIds.forEach { tagId ->
                fileDao.insertFileTagCrossRef(FileTagCrossRef(newFileId, tagId))
            }

            Result.success(newFileId)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка при добавлении файла: ${e.message}"))
        }
    }

    suspend fun updateFile(
        fileId: Long,
        newName: String,
        newDescription: String?,
        newTagIds: List<Long>
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            // 1. Получаем текущий файл
            val existingFile = fileDao.getFileById(fileId)
                ?: return@withContext Result.failure(Exception("Файл не найден"))

            // 2. Обновляем поля
            val updatedFile = existingFile.copy(
                name = newName,
                description = newDescription
            )
            fileDao.updateFile(updatedFile)

            // 3. Обновляем теги (удаляем старые, добавляем новые)
            fileDao.clearTagsForFile(fileId)
            newTagIds.forEach { tagId ->
                fileDao.insertFileTagCrossRef(FileTagCrossRef(fileId, tagId))
            }

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка при обновлении файла: ${e.message}"))
        }
    }

    suspend fun deleteFile(fileId: Long): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val file = fileDao.getFileById(fileId)
            if (file != null) {
                // 1. Удаляем физический файл
                file.localUri?.let { path ->
                    val fileToDelete = File(path)
                    if (fileToDelete.exists()) {
                        fileToDelete.delete()
                    }
                }

                // 2. Удаляем из БД (каскадно удалятся связи file_tags)
                fileDao.deleteFile(file)
                file.previewPath?.let { previewPath ->
                    val previewFile = File(previewPath)
                    if (previewFile.exists()) {
                        previewFile.delete()
                    }
                }
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка при удалении файла: ${e.message}"))
        }
    }

    // --- ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ---

    private fun copyFileToInternalStorage(uri: Uri): String? {
        return try {
            val inputStream = context.contentResolver.openInputStream(uri) ?: return null
            // Создаем уникальное имя файла
            val fileName = "file_${System.currentTimeMillis()}.tmp"
            val file = File(context.filesDir, fileName)

            val outputStream = FileOutputStream(file)
            inputStream.copyTo(outputStream)

            inputStream.close()
            outputStream.close()

            file.absolutePath // Возвращаем абсолютный путь
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getCurrentDate(): String {
        val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
        return sdf.format(Date())
    }

    private fun FileEntity.toModel(): FileModel {
        return FileModel(
            id = this.id,
            name = this.name,
            description = this.description,
            localUri = this.localUri,
            createdAt = this.createdAt,
            mimeType = this.mimeType,
            size = this.size,
            previewPath = this.previewPath
        )
    }
}