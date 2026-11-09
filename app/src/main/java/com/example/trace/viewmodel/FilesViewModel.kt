package com.example.trace.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.model.FileModel
import com.example.trace.data.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn

class FilesViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = FileRepository(application, db.fileDao(), db.tagDao())

    // Поток для поискового запроса
    private val searchQuery = MutableStateFlow("")

    // Основной поток файлов (реагирует на поиск)
    val files: StateFlow<List<FileModel>> = searchQuery
        .flatMapLatest { query ->
            if (query.isEmpty()) {
                repository.getAllFiles()
            } else {
                repository.searchFiles(query)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Метод для обновления поискового запроса
    fun onSearchQueryChanged(query: String) {
        searchQuery.value = query
    }
}