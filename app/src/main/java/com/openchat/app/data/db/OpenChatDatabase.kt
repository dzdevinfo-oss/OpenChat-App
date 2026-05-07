package com.openchat.app.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.openchat.app.data.db.dao.*
import com.openchat.app.data.model.*

@Database(
    entities = [
        Session::class,
        Message::class,
        ApiProvider::class,
        AiModel::class,
        WorkspaceFile::class,
        Memory::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(StringListConverter::class)
abstract class OpenChatDatabase : RoomDatabase() {
    abstract fun sessionDao(): SessionDao
    abstract fun messageDao(): MessageDao
    abstract fun apiProviderDao(): ApiProviderDao
    abstract fun aiModelDao(): AiModelDao
    abstract fun workspaceFileDao(): WorkspaceFileDao
    abstract fun memoryDao(): MemoryDao
}
