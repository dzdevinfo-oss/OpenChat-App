package com.openchat.app.data.model

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import androidx.room.Index

@Entity(
    tableName = "workspace_files",
    foreignKeys = [
        ForeignKey(
            entity = Session::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("sessionId")]
)
data class WorkspaceFile(
    @PrimaryKey val id: String,
    val workspaceId: String,
    val sessionId: String,
    val fileName: String,
    val filePath: String,
    val fileType: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long,
    val isDeleted: Boolean,
    val previousContent: String?,
    val isFolder: Boolean = false,
    val parentId: String? = null
)
