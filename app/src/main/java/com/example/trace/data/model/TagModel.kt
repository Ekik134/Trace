package com.example.trace.data.model

data class TagModel(
    val id: Long,
    val name: String,
    val iconUri: String?,
    val fileCount: Int = 0
)