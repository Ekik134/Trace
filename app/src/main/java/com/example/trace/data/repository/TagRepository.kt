package com.example.trace.data.repository

import com.example.trace.data.local.dao.TagDao
import com.example.trace.data.local.dao.TagRelationDao
import com.example.trace.data.local.dao.TagWithCount
import com.example.trace.data.local.entity.TagEntity
import com.example.trace.data.local.entity.TagRelationEntity
import com.example.trace.data.model.TagModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class TagRepository(
    private val tagDao: TagDao,
    private val tagRelationDao: TagRelationDao
) {

    // --- ПОЛУЧЕНИЕ ДАННЫХ ---

    /**
     * Получить все теги в виде Flow для реактивного обновления UI
     */
    fun getAllTags(): Flow<List<TagModel>> {
        return tagDao.getAllTags().map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Получить теги с количеством привязанных к ним файлов (для экрана Тегов / Древа)
     */
    fun getTagsWithFileCount(): Flow<List<TagModel>> {
        return tagDao.getTagsWithFileCount().map { list ->
            list.map {
                it.tag.toModel(fileCount = it.fileCount)
            }
        }
    }

    /**
     * Поиск тегов по названию
     */
    fun searchTags(query: String): Flow<List<TagModel>> {
        return tagDao.searchTags(query).map { entities ->
            entities.map { it.toModel() }
        }
    }

    /**
     * Получить конкретный тег по ID
     */
    suspend fun getTagById(tagId: Long): TagModel? {
        return tagDao.getTagById(tagId)?.toModel()
    }

    /**
     * Получить все связи (иерархию) для построения древа
     */
    fun getAllRelations(): Flow<List<TagRelationEntity>> {
        return tagRelationDao.getAllRelations()
    }

    /**
     * Получить родителей (теги, в которые входит текущий)
     */
    suspend fun getParentsOf(tagId: Long): List<TagModel> {
        val parentRelations = tagRelationDao.getParentsOf(tagId)
        return parentRelations.mapNotNull { relation ->
            tagDao.getTagById(relation.ancestorId)?.toModel()
        }
    }

    /**
     * Получить детей (теги, которые включает текущий)
     */
    suspend fun getChildrenOf(tagId: Long): List<TagModel> {
        val childRelations = tagRelationDao.getChildrenOf(tagId)
        return childRelations.mapNotNull { relation ->
            tagDao.getTagById(relation.descendantId)?.toModel()
        }
    }

    // --- СОЗДАНИЕ И РЕДАКТИРОВАНИЕ ---

    /**
     * Создать новый тег
     */
    suspend fun createTag(name: String, iconUri: String? = null): Result<Long> {
        return try {
            val tag = TagEntity(name = name, iconUri = iconUri)
            val id = tagDao.insertTag(tag)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка при создании тега: ${e.message}"))
        }
    }

    /**
     * Обновить существующий тег
     */
    suspend fun updateTag(tag: TagModel): Result<Unit> {
        return try {
            tagDao.updateTag(
                TagEntity(id = tag.id, name = tag.name, iconUri = tag.iconUri)
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка при обновлении тега: ${e.message}"))
        }
    }

    /**
     * Удалить тег (вместе со всеми его связями)
     */
    suspend fun deleteTag(tagId: Long): Result<Unit> {
        return try {
            val tag = tagDao.getTagById(tagId)
            if (tag != null) {
                tagRelationDao.clearRelationsForTag(tagId) // Сначала чистим связи
                tagDao.deleteTag(tag) // Удаляем сам тег
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка при удалении тега: ${e.message}"))
        }
    }

    // --- УПРАВЛЕНИЕ СВЯЗЯМИ (ИЕРАРХИЯ) ---

    /**
     * Добавить связь между тегами с полной валидацией
     * @param parentId ID родительского тега ("Включает в себя")
     * @param childId ID дочернего тега ("Входит в")
     */
    suspend fun addRelation(parentId: Long, childId: Long): Result<Unit> {
        // 1. Защита: Нельзя связать тег с самим собой
        if (parentId == childId) {
            return Result.failure(Exception("Тег не может быть родителем самому себе"))
        }

        // 2. Защита: Проверка на создание цикла (например, А -> Б -> А)
        // Если childId уже является предком parentId, то добавление связи создаст петлю.
        if (tagRelationDao.isDescendant(childId = parentId, parentId = childId)) {
            return Result.failure(Exception("Невозможно добавить связь: это создаст цикл в древе"))
        }

        // 3. Вставка (OnConflictStrategy.IGNORE защитит от дубликатов)
        return try {
            tagRelationDao.insertRelation(TagRelationEntity(parentId, childId))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка при добавлении связи: ${e.message}"))
        }
    }

    /**
     * Удалить связь между тегами
     */
    suspend fun removeRelation(parentId: Long, childId: Long): Result<Unit> {
        return try {
            val relation = TagRelationEntity(parentId, childId)
            tagRelationDao.deleteRelation(relation)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(Exception("Ошибка при удалении связи: ${e.message}"))
        }
    }

    // --- МАППИНГ (Entity -> Model) ---

    private fun TagEntity.toModel(fileCount: Int = 0): TagModel {
        return TagModel(
            id = this.id,
            name = this.name,
            iconUri = this.iconUri,
            fileCount = fileCount
        )
    }
}