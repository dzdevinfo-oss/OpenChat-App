package com.openchat.app.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "app_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val scope = CoroutineScope(Dispatchers.IO)

    private object PreferencesKeys {
        val GLOBAL_SYSTEM_PROMPT = stringPreferencesKey("global_system_prompt")
        val IS_MEMORY_ENABLED = booleanPreferencesKey("is_memory_enabled")
        val THEME = stringPreferencesKey("theme")
    }

    private val _globalSystemPrompt = MutableStateFlow("")
    val globalSystemPrompt: StateFlow<String> = _globalSystemPrompt.asStateFlow()

    private val _isMemoryEnabled = MutableStateFlow(true)
    val isMemoryEnabled: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()
    
    private val _theme = MutableStateFlow("system")
    val theme: StateFlow<String> = _theme.asStateFlow()

    init {
        scope.launch {
            val prefs = context.dataStore.data.first()
            _globalSystemPrompt.value = prefs[PreferencesKeys.GLOBAL_SYSTEM_PROMPT] ?: ""
            _isMemoryEnabled.value = prefs[PreferencesKeys.IS_MEMORY_ENABLED] ?: true
            _theme.value = prefs[PreferencesKeys.THEME] ?: "system"

            launch {
                context.dataStore.data.map { it[PreferencesKeys.GLOBAL_SYSTEM_PROMPT] ?: "" }.collect {
                    _globalSystemPrompt.value = it
                }
            }
            launch {
                context.dataStore.data.map { it[PreferencesKeys.IS_MEMORY_ENABLED] ?: true }.collect {
                    _isMemoryEnabled.value = it
                }
            }
            launch {
                context.dataStore.data.map { it[PreferencesKeys.THEME] ?: "system" }.collect {
                    _theme.value = it
                }
            }
        }
    }

    fun setGlobalSystemPrompt(prompt: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.GLOBAL_SYSTEM_PROMPT] = prompt
            }
        }
    }

    fun setMemoryEnabled(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_MEMORY_ENABLED] = enabled
            }
        }
    }
    
    fun setTheme(theme: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.THEME] = theme
            }
        }
    }
}
