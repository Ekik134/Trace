package com.example.trace.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.model.TagModel
import com.example.trace.data.repository.TagRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class TagsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    // Передаем TagDao и TagRelationDao (даже если связи пока не используются)
    private val repository = TagRepository(db.tagDao(), db.tagRelationDao())

    private val searchQuery = MutableStateFlow("")

    // Поток тегов (с поиском)
    val tags: StateFlow<List<TagModel>> = searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.getAllTags() // Или getTagsWithFileCount() если хотите видеть кол-во
            } else {
                repository.searchTags(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }
}