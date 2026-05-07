package com.openchat.app.data.repository

import com.openchat.app.data.db.dao.MessageDao
import com.openchat.app.data.db.dao.SessionDao
import com.openchat.app.data.model.Message
import com.openchat.app.data.model.Session
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChatRepository @Inject constructor(
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao
) {
    fun getAllSessions(): Flow<List<Session>> = sessionDao.getAll()
    
    fun getPinnedSessions(): Flow<List<Session>> = sessionDao.getPinned()
    
    fun searchSessions(query: String): Flow<List<Session>> = sessionDao.search(query)
    
    suspend fun getSessionById(id: String): Session? = sessionDao.getById(id)
    
    suspend fun insertSession(session: Session) {
        sessionDao.insert(session)
    }
    
    suspend fun updateSession(session: Session) {
        sessionDao.update(session)
    }
    
    suspend fun deleteSession(id: String) {
        sessionDao.delete(id)
    }

    suspend fun deleteAllSessions() {
        sessionDao.deleteAllSessions()
    }

    fun getMessagesBySessionId(sessionId: String): Flow<List<Message>> = messageDao.getBySessionId(sessionId)
    
    suspend fun getLastNMessages(sessionId: String, n: Int): List<Message> = messageDao.getLastN(sessionId, n)
    
    suspend fun insertMessage(message: Message) {
        messageDao.insert(message)
    }
    
    suspend fun updateMessage(message: Message) {
        messageDao.update(message)
    }
    
    suspend fun deleteMessage(id: String) {
        messageDao.delete(id)
    }
    
    suspend fun deleteMessagesBySessionId(sessionId: String) {
        messageDao.deleteBySessionId(sessionId)
    }
}
