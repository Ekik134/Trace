package com.example.trace.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

@Entity(
    tableName = "tag_hierarchy",
    primaryKeys = ["ancestorId", "descendantId"], // Составной ключ
    foreignKeys = [
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["ancestorId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = TagEntity::class,
            parentColumns = ["id"],
            childColumns = ["descendantId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("descendantId")]
)
data class TagRelationEntity(
    val ancestorId: Long,   // ID родительского тега (предок)
    val descendantId: Long  // ID дочернего тега (потомок)
)