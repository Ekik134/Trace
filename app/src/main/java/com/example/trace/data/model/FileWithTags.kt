package com.example.trace.data.local.model

import androidx.room.Embedded
import androidx.room.Junction
import androidx.room.Relation
import com.example.trace.data.local.entity.FileEntity
import com.example.trace.data.local.entity.FileTagCrossRef
import com.example.trace.data.local.entity.TagEntity

data class FileWithTags(
    @Embedded val file: FileEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = FileTagCrossRef::class,
            parentColumn = "fileId",
            entityColumn = "tagId"
        )
    )
    val tags: List<TagEntity>
)