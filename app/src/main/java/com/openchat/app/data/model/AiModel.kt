package com.openchat.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "ai_models",
    foreignKeys = [
        ForeignKey(
            entity = ApiProvider::class,
            parentColumns = ["id"],
            childColumns = ["providerId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("providerId")]
)
data class AiModel(
    @PrimaryKey val id: String,
    val modelId: String,
    val displayName: String,
    val providerId: String,
    val isBuiltIn: Boolean,
    val censorMode: String,
    val contextWindow: Int?,
    val supportsVision: Boolean,
    val supportsStreaming: Boolean
)
