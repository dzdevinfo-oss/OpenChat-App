package com.openchat.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.openchat.app.data.model.AiModel
import kotlinx.coroutines.flow.Flow

@Dao
interface AiModelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(model: AiModel)

    @Update
    suspend fun update(model: AiModel)

    @Query("DELETE FROM ai_models WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM ai_models")
    fun getAll(): Flow<List<AiModel>>

    @Query("SELECT * FROM ai_models WHERE providerId = :providerId")
    fun getByProvider(providerId: String): Flow<List<AiModel>>

    @Query("SELECT * FROM ai_models WHERE isBuiltIn = 1")
    fun getBuiltIn(): Flow<List<AiModel>>

    @Query("SELECT * FROM ai_models WHERE modelId = :modelId LIMIT 1")
    suspend fun getByModelId(modelId: String): AiModel?
}
