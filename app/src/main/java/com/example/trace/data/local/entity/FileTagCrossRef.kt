package com.example.trace.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey

@Entity(
    tableName = "file_tags",
    primaryKeys = ["fileId", "tagId"], // Составной первичный ключ
    foreignKeys = [
        ForeignKey(
            entity = FileEntity::class,
            parentColumns = ["id"],
            childColumns = ["fileId"],
            onDelete = ForeignKey.CASCADE // При удалении файла удаляются связи
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["tagId"],
            onDelete = ForeignKey.CASCADE // При удалении тега удаляются связи
        )
    ],
    indices = [androidx.room.Index("tagId")] // Индекс для быстрого поиска по тегам
)
data class FileTagCrossRef(
    val fileId: Long,
    val tagId: Long
)