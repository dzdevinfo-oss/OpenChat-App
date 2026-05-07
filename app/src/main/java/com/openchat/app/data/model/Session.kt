package com.openchat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class Session(
    @PrimaryKey val id: String,
    val title: String,
    val createdAt: Long,
    val updatedAt: Long,
    val modelId: String,
    val providerId: String,
    val systemPrompt: String?,
    val isPinned: Boolean,
    val workspaceId: String?
)
