package com.example.trace.data.local.dao

import androidx.room.*
import com.example.trace.data.local.entity.TagRelationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagRelationDao {

    // Получить все связи (для построения древа)
    @Query("SELECT * FROM tag_hierarchy")
    fun getAllRelations(): Flow<List<TagRelationEntity>>

    // Получить только дочерние теги для конкретного родителя
    @Query("SELECT * FROM tag_hierarchy WHERE ancestorId = :parentId")
    suspend fun getChildrenOf(parentId: Long): List<TagRelationEntity>

    // Получить только родительские теги для конкретного ребенка
    @Query("SELECT * FROM tag_hierarchy WHERE descendantId = :childId")
    suspend fun getParentsOf(childId: Long): List<TagRelationEntity>

    // Добавить связь (родитель -> ребенок)
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRelation(relation: TagRelationEntity)

    // Удалить конкретную связь
    @Delete
    suspend fun deleteRelation(relation: TagRelationEntity)

    // Очистить все связи для тега (если тег удаляется)
    @Query("DELETE FROM tag_hierarchy WHERE ancestorId = :tagId OR descendantId = :tagId")
    suspend fun clearRelationsForTag(tagId: Long)

    @Query("""
    WITH RECURSIVE descendants(id) AS (
        SELECT descendantId FROM tag_hierarchy WHERE ancestorId = :childId
        UNION
        SELECT th.descendantId FROM tag_hierarchy th
        INNER JOIN descendants d ON th.ancestorId = d.id
    )
    SELECT EXISTS(SELECT 1 FROM descendants WHERE id = :parentId)
""")
    suspend fun isDescendant(childId: Long, parentId: Long): Boolean
}