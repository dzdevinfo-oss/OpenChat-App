package com.openchat.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "api_providers")
data class ApiProvider(
    @PrimaryKey val id: String,
    val name: String,
    val baseUrl: String,
    val encryptedApiKey: String,
    val isActive: Boolean,
    val createdAt: Long
)
