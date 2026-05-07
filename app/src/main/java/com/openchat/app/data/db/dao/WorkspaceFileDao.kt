package com.openchat.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.openchat.app.data.model.WorkspaceFile
import kotlinx.coroutines.flow.Flow

@Dao
interface WorkspaceFileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(file: WorkspaceFile)

    @Update
    suspend fun update(file: WorkspaceFile)

    @Query("UPDATE workspace_files SET isDeleted = 1, updatedAt = :updatedAt WHERE id = :id")
    suspend fun delete(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM workspace_files WHERE sessionId = :sessionId AND isDeleted = 0 ORDER BY updatedAt DESC")
    fun getBySessionId(sessionId: String): Flow<List<WorkspaceFile>>

    @Query("SELECT * FROM workspace_files WHERE id = :id")
    suspend fun getById(id: String): WorkspaceFile?

    @Query("UPDATE workspace_files SET content = previousContent, previousContent = content, updatedAt = :updatedAt WHERE id = :id AND previousContent IS NOT NULL")
    suspend fun undoLastEdit(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM workspace_files WHERE id = :id")
    suspend fun permanentDelete(id: String)

    @Query("UPDATE workspace_files SET isDeleted = 0, updatedAt = :updatedAt WHERE id = :id")
    suspend fun recoverDeleted(id: String, updatedAt: Long = System.currentTimeMillis())

    @Query("SELECT * FROM workspace_files WHERE sessionId = :sessionId AND isDeleted = 1 ORDER BY updatedAt DESC")
    fun getDeletedBySession(sessionId: String): Flow<List<WorkspaceFile>>

    @Query("UPDATE workspace_files SET fileName = :newName, filePath = :newPath, updatedAt = :updatedAt WHERE id = :id")
    suspend fun renameFile(id: String, newName: String, newPath: String, updatedAt: Long = System.currentTimeMillis())
}
