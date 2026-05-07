package com.openchat.app.data.db.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.openchat.app.data.model.ApiProvider
import kotlinx.coroutines.flow.Flow

@Dao
interface ApiProviderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(provider: ApiProvider)

    @Update
    suspend fun update(provider: ApiProvider)

    @Query("DELETE FROM api_providers WHERE id = :id")
    suspend fun delete(id: String)

    @Query("SELECT * FROM api_providers")
    fun getAll(): Flow<List<ApiProvider>>

    @Query("SELECT * FROM api_providers WHERE id = :id")
    suspend fun getById(id: String): ApiProvider?

    @Query("SELECT * FROM api_providers WHERE isActive = 1")
    fun getActive(): Flow<List<ApiProvider>>
}
