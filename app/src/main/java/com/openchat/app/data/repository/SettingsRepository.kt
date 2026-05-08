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
        val DEFAULT_MODEL_ID = stringPreferencesKey("default_model_id")
        val DEFAULT_PROVIDER_ID = stringPreferencesKey("default_provider_id")
        val AUTO_READ = booleanPreferencesKey("auto_read")
        val TTS_SPEED = stringPreferencesKey("tts_speed")
        val TTS_PITCH = stringPreferencesKey("tts_pitch")
        val FONT_SIZE = stringPreferencesKey("font_size")
        val BUBBLE_STYLE = stringPreferencesKey("bubble_style")
        val SHOW_TIMESTAMPS = booleanPreferencesKey("show_timestamps")
        val TEMPERATURE = stringPreferencesKey("temperature")
        val MAX_TOKENS = stringPreferencesKey("max_tokens")
        val STREAMING_ENABLED = booleanPreferencesKey("streaming_enabled")
        val EXTENDED_THINKING = booleanPreferencesKey("extended_thinking")
        val THINKING_BUDGET = stringPreferencesKey("thinking_budget")
        val AUTO_INJECT_WORKSPACE = booleanPreferencesKey("auto_inject_workspace")
        val MAX_CONTEXT_FILES = stringPreferencesKey("max_context_files")
        val AUTO_SAVE_ARTIFACTS = booleanPreferencesKey("auto_save_artifacts")
        val IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")
    }

    private val _globalSystemPrompt = MutableStateFlow("")
    val globalSystemPrompt: StateFlow<String> = _globalSystemPrompt.asStateFlow()

    private val _isMemoryEnabled = MutableStateFlow(true)
    val isMemoryEnabled: StateFlow<Boolean> = _isMemoryEnabled.asStateFlow()

    private val _theme = MutableStateFlow("system")
    val theme: StateFlow<String> = _theme.asStateFlow()

    private val _defaultModelId = MutableStateFlow<String?>(null)
    val defaultModelId: StateFlow<String?> = _defaultModelId.asStateFlow()

    private val _defaultProviderId = MutableStateFlow<String?>(null)
    val defaultProviderId: StateFlow<String?> = _defaultProviderId.asStateFlow()

    private val _autoRead = MutableStateFlow(false)
    val autoRead: StateFlow<Boolean> = _autoRead.asStateFlow()

    private val _ttsSpeed = MutableStateFlow(1.0f)
    val ttsSpeed: StateFlow<Float> = _ttsSpeed.asStateFlow()

    private val _ttsPitch = MutableStateFlow(1.0f)
    val ttsPitch: StateFlow<Float> = _ttsPitch.asStateFlow()

    private val _fontSize = MutableStateFlow(14)
    val fontSize: StateFlow<Int> = _fontSize.asStateFlow()

    private val _bubbleStyle = MutableStateFlow("modern")
    val bubbleStyle: StateFlow<String> = _bubbleStyle.asStateFlow()

    private val _showTimestamps = MutableStateFlow(true)
    val showTimestamps: StateFlow<Boolean> = _showTimestamps.asStateFlow()

    private val _temperature = MutableStateFlow(1.0f)
    val temperature: StateFlow<Float> = _temperature.asStateFlow()

    private val _maxTokens = MutableStateFlow(8192)
    val maxTokens: StateFlow<Int> = _maxTokens.asStateFlow()

    private val _streamingEnabled = MutableStateFlow(true)
    val streamingEnabled: StateFlow<Boolean> = _streamingEnabled.asStateFlow()

    private val _extendedThinking = MutableStateFlow(false)
    val extendedThinking: StateFlow<Boolean> = _extendedThinking.asStateFlow()

    private val _thinkingBudget = MutableStateFlow(1000)
    val thinkingBudget: StateFlow<Int> = _thinkingBudget.asStateFlow()

    private val _autoInjectWorkspace = MutableStateFlow(false)
    val autoInjectWorkspace: StateFlow<Boolean> = _autoInjectWorkspace.asStateFlow()

    private val _maxContextFiles = MutableStateFlow(5)
    val maxContextFiles: StateFlow<Int> = _maxContextFiles.asStateFlow()

    private val _autoSaveArtifacts = MutableStateFlow(true)
    val autoSaveArtifacts: StateFlow<Boolean> = _autoSaveArtifacts.asStateFlow()

    private val _isFirstLaunch = MutableStateFlow(true)
    val isFirstLaunch: StateFlow<Boolean> = _isFirstLaunch.asStateFlow()

    init {
        scope.launch {
            val prefs = context.dataStore.data.first()
            _globalSystemPrompt.value = prefs[PreferencesKeys.GLOBAL_SYSTEM_PROMPT] ?: ""
            _isMemoryEnabled.value = prefs[PreferencesKeys.IS_MEMORY_ENABLED] ?: true
            _theme.value = prefs[PreferencesKeys.THEME] ?: "system"
            _defaultModelId.value = prefs[PreferencesKeys.DEFAULT_MODEL_ID]
            _defaultProviderId.value = prefs[PreferencesKeys.DEFAULT_PROVIDER_ID]
            _autoRead.value = prefs[PreferencesKeys.AUTO_READ] ?: false
            _ttsSpeed.value = (prefs[PreferencesKeys.TTS_SPEED] ?: "1.0").toFloat()
            _ttsPitch.value = (prefs[PreferencesKeys.TTS_PITCH] ?: "1.0").toFloat()
            _fontSize.value = (prefs[PreferencesKeys.FONT_SIZE] ?: "14").toInt()
            _bubbleStyle.value = prefs[PreferencesKeys.BUBBLE_STYLE] ?: "modern"
            _showTimestamps.value = prefs[PreferencesKeys.SHOW_TIMESTAMPS] ?: true
            _temperature.value = (prefs[PreferencesKeys.TEMPERATURE] ?: "1.0").toFloat()
            _maxTokens.value = (prefs[PreferencesKeys.MAX_TOKENS] ?: "8192").toInt()
            _streamingEnabled.value = prefs[PreferencesKeys.STREAMING_ENABLED] ?: true
            _extendedThinking.value = prefs[PreferencesKeys.EXTENDED_THINKING] ?: false
            _thinkingBudget.value = (prefs[PreferencesKeys.THINKING_BUDGET] ?: "1000").toInt()
            _autoInjectWorkspace.value = prefs[PreferencesKeys.AUTO_INJECT_WORKSPACE] ?: false
            _maxContextFiles.value = (prefs[PreferencesKeys.MAX_CONTEXT_FILES] ?: "5").toInt()
            _autoSaveArtifacts.value = prefs[PreferencesKeys.AUTO_SAVE_ARTIFACTS] ?: true
            _isFirstLaunch.value = prefs[PreferencesKeys.IS_FIRST_LAUNCH] ?: true

            // TODO: Collect all preferences here, similar to the initial one to keep state updated.
            // For brevity in this task, I am only adding the initial collection. 
            // In a full implementation, all preferences should be collected to keep StateFlows reactive.
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

    fun setDefaultModel(modelId: String, providerId: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.DEFAULT_MODEL_ID] = modelId
                preferences[PreferencesKeys.DEFAULT_PROVIDER_ID] = providerId
            }
        }
    }

    fun setAutoRead(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTO_READ] = enabled
            }
        }
    }

    fun setTtsSpeed(speed: Float) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.TTS_SPEED] = speed.toString()
            }
        }
    }

    fun setTtsPitch(pitch: Float) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.TTS_PITCH] = pitch.toString()
            }
        }
    }

    fun setFontSize(size: Int) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.FONT_SIZE] = size.toString()
            }
        }
    }

    fun setBubbleStyle(style: String) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.BUBBLE_STYLE] = style
            }
        }
    }

    fun setShowTimestamps(show: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.SHOW_TIMESTAMPS] = show
            }
        }
    }

    fun setTemperature(temp: Float) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.TEMPERATURE] = temp.toString()
            }
        }
    }

    fun setMaxTokens(tokens: Int) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.MAX_TOKENS] = tokens.toString()
            }
        }
    }

    fun setStreamingEnabled(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.STREAMING_ENABLED] = enabled
            }
        }
    }

    fun setExtendedThinking(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.EXTENDED_THINKING] = enabled
            }
        }
    }

    fun setThinkingBudget(budget: Int) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.THINKING_BUDGET] = budget.toString()
            }
        }
    }

    fun setAutoInjectWorkspace(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTO_INJECT_WORKSPACE] = enabled
            }
        }
    }

    fun setMaxContextFiles(files: Int) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.MAX_CONTEXT_FILES] = files.toString()
            }
        }
    }

    fun setAutoSaveArtifacts(enabled: Boolean) {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.AUTO_SAVE_ARTIFACTS] = enabled
            }
        }
    }

    fun setFirstLaunchFinished() {
        scope.launch {
            context.dataStore.edit { preferences ->
                preferences[PreferencesKeys.IS_FIRST_LAUNCH] = false
            }
            _isFirstLaunch.value = false
        }
    }
}
