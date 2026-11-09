package com.example.trace.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trace.data.local.AppDatabase
import com.example.trace.data.preferences.UserPreferences
import com.example.trace.data.repository.FileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

class ProfileViewModel(application: Application) : AndroidViewModel(application) {
    private val db = AppDatabase.getDatabase(application)
    private val repository = FileRepository(application, db.fileDao(), db.tagDao())
    private val userPreferences = UserPreferences(application)

    val userName: StateFlow<String> = userPreferences.userNameFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Загрузка...")

    val fileCount: StateFlow<Int> = repository.getFilesCount()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)
}