package com.example.trace.data.local.dao

import androidx.room.*
import com.example.trace.data.local.entity.FileEntity
import com.example.trace.data.local.entity.FileTagCrossRef
import com.example.trace.data.local.model.FileWithTags
import kotlinx.coroutines.flow.Flow

@Dao
interface FileDao {

    // --- CRUD для Файлов ---

    @Query("SELECT * FROM files ORDER BY createdAt DESC")
    fun getAllFiles(): Flow<List<FileEntity>>

    @Query("SELECT * FROM files WHERE id = :fileId")
    suspend fun getFileById(fileId: Long): FileEntity?

    // Поиск по названию
    @Query("SELECT * FROM files WHERE name LIKE '%' || :query || '%' ORDER BY createdAt DESC")
    fun searchFiles(query: String): Flow<List<FileEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFile(file: FileEntity): Long

    @Update
    suspend fun updateFile(file: FileEntity)

    @Delete
    suspend fun deleteFile(file: FileEntity)

    // --- Работа с тегами файла ---

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertFileTagCrossRef(crossRef: FileTagCrossRef)

    @Query("DELETE FROM file_tags WHERE fileId = :fileId")
    suspend fun clearTagsForFile(fileId: Long)

    // Получение файла вместе с его тегами (используем @Transaction для связки запросов)
    @Transaction
    @Query("SELECT * FROM files WHERE id = :fileId")
    suspend fun getFileWithTags(fileId: Long): FileWithTags?

    // Получение файлов, отфильтрованных по тегу
    @Transaction
    @Query("""
        SELECT * FROM files 
        INNER JOIN file_tags ON files.id = file_tags.fileId 
        WHERE file_tags.tagId = :tagId
        ORDER BY files.createdAt DESC
    """)
    fun getFilesByTag(tagId: Long): Flow<List<FileEntity>>
}