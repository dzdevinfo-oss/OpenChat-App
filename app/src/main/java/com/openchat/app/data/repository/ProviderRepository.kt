package com.openchat.app.data.repository

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.openchat.app.data.db.dao.AiModelDao
import com.openchat.app.data.db.dao.ApiProviderDao
import com.openchat.app.data.model.AiModel
import com.openchat.app.data.model.ApiProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProviderRepository @Inject constructor(
    private val apiProviderDao: ApiProviderDao,
    private val aiModelDao: AiModelDao,
    @ApplicationContext private val context: Context
) {

    private val masterKey = MasterKey.Builder(context)
        .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
        .build()

    private val sharedPrefs = EncryptedSharedPreferences.create(
        context,
        "openchat_api_keys",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun getAllProviders(): Flow<List<ApiProvider>> = apiProviderDao.getAll()

    fun getActiveProviders(): Flow<List<ApiProvider>> = apiProviderDao.getActive()

    suspend fun getProviderById(id: String): ApiProvider? = apiProviderDao.getById(id)

    suspend fun insertProvider(provider: ApiProvider, rawApiKey: String) {
        sharedPrefs.edit().putString(provider.id, rawApiKey).apply()
        val providerToSave = provider.copy(encryptedApiKey = "stored_in_prefs")
        apiProviderDao.insert(providerToSave)
    }

    suspend fun updateProvider(provider: ApiProvider, rawApiKey: String?) {
        if (rawApiKey != null) {
            sharedPrefs.edit().putString(provider.id, rawApiKey).apply()
        }
        apiProviderDao.update(provider.copy(encryptedApiKey = "stored_in_prefs"))
    }

    suspend fun deleteProvider(id: String) {
        sharedPrefs.edit().remove(id).apply()
        apiProviderDao.delete(id)
    }

    fun getApiKey(providerId: String): String? {
        return sharedPrefs.getString(providerId, null)
    }

    fun getAllModels(): Flow<List<AiModel>> = aiModelDao.getAll()

    fun getModelsByProvider(providerId: String): Flow<List<AiModel>> = aiModelDao.getByProvider(providerId)

    fun getBuiltInModels(): Flow<List<AiModel>> = aiModelDao.getBuiltIn()

    suspend fun getModelByModelId(modelId: String): AiModel? = aiModelDao.getByModelId(modelId)

    suspend fun insertModel(model: AiModel) {
        aiModelDao.insert(model)
    }

    suspend fun updateModel(model: AiModel) {
        aiModelDao.update(model)
    }

    suspend fun deleteModel(id: String) {
        aiModelDao.delete(id)
    }
}
