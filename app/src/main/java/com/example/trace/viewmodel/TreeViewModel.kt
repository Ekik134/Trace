package com.example.trace.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.local.entity.TagEntity
import com.example.trace.data.local.entity.TagRelationEntity
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

// Модель для отображения в RecyclerView
data class TreeNode(
    val id: Long,
    val name: String,
    val fileCount: Int,
    val level: Int,
    val iconUri: String? = null
)

class TreeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)

    val treeNodes: StateFlow<List<TreeNode>> = combine(
        db.tagDao().getAllTags(),
        db.tagDao().getTagsWithFileCount(),
        db.tagRelationDao().getAllRelations()
    ) { tags, tagsWithCount, relations ->
        buildTree(tags, tagsWithCount.associate { it.tag.id to it.fileCount }, relations)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Рекурсивная сборка древа
    private fun buildTree(
        tags: List<TagEntity>,
        fileCounts: Map<Long, Int>,
        relations: List<TagRelationEntity>
    ): List<TreeNode> {
        val result = mutableListOf<TreeNode>()

        // Находим корневые теги (те, у которых нет родителей)
        val childIds = relations.map { it.descendantId }.toSet()
        val rootTags = tags.filter { it.id !in childIds }

        // Рекурсивная функция добавления детей
        fun addChildren(parentId: Long, level: Int) {
            val children = relations.filter { it.ancestorId == parentId }
            children.forEach { relation ->
                val childTag = tags.find { it.id == relation.descendantId } ?: return@forEach
                result.add(
                    TreeNode(
                        id = childTag.id,
                        name = childTag.name,
                        fileCount = fileCounts[childTag.id] ?: 0,
                        level = level,
                        iconUri = childTag.iconUri
                    )
                )
                addChildren(childTag.id, level + 1)
            }
        }

        // Проходим по корням и собираем всё в список
        rootTags.forEach { root ->
            result.add(
                TreeNode(
                    id = root.id,
                    name = root.name,
                    fileCount = fileCounts[root.id] ?: 0,
                    level = 0,
                    iconUri = root.iconUri
                )
            )
            addChildren(root.id, 1)
        }

        return result
    }
}