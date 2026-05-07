package com.openchat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "memories",
    indices = [Index("sessionId")]
)
data class Memory(
    @PrimaryKey val id: String,
    val content: String,
    val createdAt: Long,
    val sessionId: String?,
    val isActive: Boolean
)
