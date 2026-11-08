package com.example.trace.data.model

data class FileModel(
    val id: Long,
    val name: String,
    val description: String?,
    val localUri: String?,
    val createdAt: String?,
    val mimeType: String?,
    val size: Long?,
    val previewPath: String?,
    val tags: List<TagModel> = emptyList() // Список тегов для деталей файла
)