package com.example.trace.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "files")
data class FileEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String?,
    val localUri: String?,
    val createdAt: String?,
    val mimeType: String?,
    val size: Long?,
    val previewPath: String?
)