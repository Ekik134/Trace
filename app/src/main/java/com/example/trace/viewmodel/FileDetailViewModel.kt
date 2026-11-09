package com.example.trace.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.local.model.FileWithTags
import com.example.trace.data.repository.FileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class FileDetailViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val repository = FileRepository(application, db.fileDao(), db.tagDao())

    private val _fileWithTags = MutableStateFlow<FileWithTags?>(null)
    val fileWithTags: StateFlow<FileWithTags?> = _fileWithTags.asStateFlow()

    fun loadFile(fileId: Long) {
        viewModelScope.launch {
            _fileWithTags.value = repository.getFileWithTags(fileId)
        }
    }
}