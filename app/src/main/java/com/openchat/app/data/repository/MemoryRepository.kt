package com.openchat.app.data.repository

import com.openchat.app.data.db.dao.MemoryDao
import com.openchat.app.data.model.Memory
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MemoryRepository @Inject constructor(
    private val memoryDao: MemoryDao
) {
    fun getAllMemories(): Flow<List<Memory>> = memoryDao.getAll()
    
    fun getBySession(sessionId: String): Flow<List<Memory>> = memoryDao.getBySession(sessionId)
    
    fun getGlobal(): Flow<List<Memory>> = memoryDao.getGlobal()
    
    suspend fun insertMemory(memory: Memory) {
        memoryDao.insert(memory)
    }
    
    suspend fun updateMemory(memory: Memory) {
        memoryDao.update(memory)
    }
    
    suspend fun deleteMemory(id: String) {
        memoryDao.delete(id)
    }
}
