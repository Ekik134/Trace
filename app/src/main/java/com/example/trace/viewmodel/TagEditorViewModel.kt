package com.example.trace.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.model.TagModel
import com.example.trace.data.repository.TagRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class TagEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = TagRepository(db.tagDao(), db.tagRelationDao())

    private val _currentTag = MutableStateFlow<TagModel?>(null)
    val currentTag: StateFlow<TagModel?> = _currentTag.asStateFlow()

    private val _parents = MutableStateFlow<List<TagModel>>(emptyList())
    val parents: StateFlow<List<TagModel>> = _parents.asStateFlow()

    private val _children = MutableStateFlow<List<TagModel>>(emptyList())
    val children: StateFlow<List<TagModel>> = _children.asStateFlow()

    private val _saveSuccess = MutableSharedFlow<Unit>()
    val saveSuccess: SharedFlow<Unit> = _saveSuccess.asSharedFlow()

    private val _errorEvent = MutableSharedFlow<String>()
    val errorEvent: SharedFlow<String> = _errorEvent.asSharedFlow()

    private val _deleteSuccess = MutableSharedFlow<Unit>()
    val deleteSuccess: SharedFlow<Unit> = _deleteSuccess.asSharedFlow()


    fun loadTag(tagId: Long) {
        viewModelScope.launch {
            _currentTag.value = repository.getTagById(tagId)
            // Принудительно перезагружаем списки
            refreshRelations(tagId)
        }
    }

    // Вспомогательный метод для обновления связей
    private suspend fun refreshRelations(tagId: Long) {
        _parents.value = repository.getParentsOf(tagId)
        _children.value = repository.getChildrenOf(tagId)
    }

    fun saveTag(tagId: Long?, name: String) {
        viewModelScope.launch {
            if (tagId == null) {
                repository.createTag(name)
            } else {
                repository.updateTag(TagModel(id = tagId, name = name, iconUri = null))
            }
            _saveSuccess.emit(Unit)
        }
    }

    fun addParentRelation(parentId: Long, childId: Long) {
        viewModelScope.launch {
            val result = repository.addRelation(parentId = parentId, childId = childId)
            result.onSuccess {
                refreshRelations(childId) // childId - это текущий тег
            }.onFailure { error ->
                _errorEvent.emit(error.message ?: "Ошибка связи")
            }
        }
    }

    fun addChildRelation(parentId: Long, childId: Long) {
        viewModelScope.launch {
            val result = repository.addRelation(parentId = parentId, childId = childId)
            result.onSuccess {
                refreshRelations(parentId) // parentId - это текущий тег
            }.onFailure { error ->
                _errorEvent.emit(error.message ?: "Ошибка связи")
            }
        }
    }

    fun deleteTag(tagId: Long) {
        viewModelScope.launch {
            val result = repository.deleteTag(tagId)
            result.onSuccess {
                _deleteSuccess.emit(Unit) // Сообщаем фрагменту об успехе
            }.onFailure { error ->
                _errorEvent.emit(error.message ?: "Ошибка удаления")
            }
        }
    }

    fun removeRelation(parentId: Long, childId: Long, currentTagId: Long) {
        viewModelScope.launch {
            val result = repository.removeRelation(parentId, childId)
            result.onSuccess {
                // Перезагружаем оба списка для текущего тега
                refreshRelations(currentTagId)
            }
        }
    }
}