package com.example.trace.data.local.dao

import androidx.room.*
import com.example.trace.data.local.entity.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {

    @Query("SELECT * FROM tags ORDER BY name ASC")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tags WHERE id = :tagId")
    suspend fun getTagById(tagId: Long): TagEntity?

    @Query("SELECT * FROM tags WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    fun searchTags(query: String): Flow<List<TagEntity>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertTag(tag: TagEntity): Long

    @Update
    suspend fun updateTag(tag: TagEntity)

    @Delete
    suspend fun deleteTag(tag: TagEntity)

    // Запрос для получения тегов, привязанных к конкретному файлу
    @Query("""
        SELECT * FROM tags 
        INNER JOIN file_tags ON tags.id = file_tags.tagId 
        WHERE file_tags.fileId = :fileId
    """)
    suspend fun getTagsForFile(fileId: Long): List<TagEntity>

    // Запрос для получения количества файлов у каждого тега (для карточек древа)
    @Query("""
        SELECT tags.*, COUNT(file_tags.fileId) as fileCount 
        FROM tags 
        LEFT JOIN file_tags ON tags.id = file_tags.tagId 
        GROUP BY tags.id
    """)
    fun getTagsWithFileCount(): Flow<List<TagWithCount>>
}

// Вспомогательный класс для хранения тега и количества файлов
data class TagWithCount(
    @Embedded val tag: TagEntity,
    val fileCount: Int
)