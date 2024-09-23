package com.uoa.core.database.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "causes",
    indices = [
        Index(value = ["unsafeBehaviourId"])
    ],
    foreignKeys = [
        ForeignKey(entity = UnsafeBehaviourEntity::class, parentColumns = ["id"], childColumns = ["unsafeBehaviourId"], onDelete = ForeignKey.CASCADE),
//        ForeignKey(entity = UnsafeBehaviourEntity::class, parentColumns = ["tripId"], childColumns = ["tripId"], onDelete = ForeignKey.CASCADE)
    ]
)
data class CauseEntity(
    @PrimaryKey(autoGenerate = false)
    val id: UUID,
    val unsafeBehaviourId: UUID,
    val name: String,
    val influence: Boolean?,
    val createdAt: String,
    val updatedAt: String?
)
