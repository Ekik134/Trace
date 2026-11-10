package com.example.trace.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.model.FileModel
import com.example.trace.data.repository.FileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = FileRepository(application, db.fileDao(), db.tagDao())

    // Поток, который отдает только 3 последних файла
    val recentFiles: StateFlow<List<FileModel>> = repository.getAllFiles()
        .map { files ->
            // Берем первые 3 файла (они уже отсортированы по дате в DAO)
            files.take(3)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )
}