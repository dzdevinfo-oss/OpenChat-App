package com.openchat.app.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.openchat.app.data.db.OpenChatDatabase
import com.openchat.app.data.db.dao.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): OpenChatDatabase {
        return Room.databaseBuilder(
            context,
            OpenChatDatabase::class.java,
            "openchat.db"
        )
        .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
        .build()
    }

    @Provides
    fun provideSessionDao(db: OpenChatDatabase): SessionDao = db.sessionDao()

    @Provides
    fun provideMessageDao(db: OpenChatDatabase): MessageDao = db.messageDao()

    @Provides
    fun provideApiProviderDao(db: OpenChatDatabase): ApiProviderDao = db.apiProviderDao()

    @Provides
    fun provideAiModelDao(db: OpenChatDatabase): AiModelDao = db.aiModelDao()

    @Provides
    fun provideWorkspaceFileDao(db: OpenChatDatabase): WorkspaceFileDao = db.workspaceFileDao()

    @Provides
    fun provideMemoryDao(db: OpenChatDatabase): MemoryDao = db.memoryDao()
}
