package com.example.trace.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.trace.data.preferences.UserPreferences
import com.example.trace.data.repository.FileRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ProfileViewModel(
    application: Application,
    private val fileRepository: FileRepository // Внедряем через DI (Hilt/Koin) или создаем вручную
) : AndroidViewModel(application) {

    private val userPreferences = UserPreferences(application.applicationContext)

    // 1. Имя пользователя из DataStore
    val userName: StateFlow<String> = userPreferences.userNameFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = "Загрузка..."
        )

    // 2. Количество файлов из Room (SQLite)
    val fileCount: StateFlow<Int> = fileRepository.getFilesCount()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )
}