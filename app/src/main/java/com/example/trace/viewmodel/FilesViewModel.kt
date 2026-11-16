package com.example.trace.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.model.FileModel
import com.example.trace.data.repository.FileRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn

@OptIn(ExperimentalCoroutinesApi::class)
class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = FileRepository(application, db.fileDao(), db.tagDao())

    private val searchQuery = MutableStateFlow("")
    private val selectedTagIds = MutableStateFlow<Set<Long>>(emptySet())

    val files: StateFlow<List<FileModel>> = combine(searchQuery, selectedTagIds) { query, tags ->
        Pair(query, tags)
    }.flatMapLatest { (query, tags) ->
        // 1. Получаем базовый список (все или по поиску)
        val baseFlow = if (query.isEmpty()) {
            repository.getAllFiles()
        } else {
            repository.searchFilesWithTags(query)
        }

        // 2. Фильтруем по тегам в памяти
        baseFlow.flatMapLatest { files ->
            if (tags.isEmpty()) {
                flowOf(files)
            } else {
                flowOf(
                    files.filter { file ->
                        file.tags.any { tag -> tags.contains(tag.id) }
                    }
                )
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }

    fun onTagsSelected(tagIds: Set<Long>) {
        selectedTagIds.value = tagIds
    }

    fun getSelectedTagIds(): Set<Long> = selectedTagIds.value
}