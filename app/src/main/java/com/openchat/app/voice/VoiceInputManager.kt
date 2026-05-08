package com.openchat.app.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceInputManager @Inject constructor(
    @ApplicationContext private val context: Context
) : RecognitionListener {

    private var speechRecognizer: SpeechRecognizer? = null
    private val _isListening = MutableStateFlow(false)
    val isListening = _isListening.asStateFlow()

    private val _partialText = MutableStateFlow("")
    val partialText = _partialText.asStateFlow()

    private val _finalText = MutableStateFlow("")
    val finalText = _finalText.asStateFlow()

    private var onResultListener: ((String) -> Unit)? = null

    init {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(this)
        }
    }

    fun startListening(onResult: (String) -> Unit) {
        if (speechRecognizer == null) return
        
        onResultListener = onResult
        _partialText.value = ""
        _finalText.value = ""
        
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
        }
        
        speechRecognizer?.startListening(intent)
        _isListening.value = true
    }

    fun stopListening() {
        speechRecognizer?.stopListening()
        _isListening.value = false
    }

    override fun onReadyForSpeech(params: Bundle?) {}
    override fun onBeginningOfSpeech() {
        _isListening.value = true
    }
    override fun onRmsChanged(rmsdB: Float) {}
    override fun onBufferReceived(buffer: ByteArray?) {}
    override fun onEndOfSpeech() {
        _isListening.value = false
    }

    override fun onError(error: Int) {
        _isListening.value = false
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val text = matches[0]
            _finalText.value = text
            onResultListener?.invoke(text)
        }
        _isListening.value = false
    }

    override fun onPartialResults(partialResults: Bundle?) {
        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            _partialText.value = matches[0]
        }
    }

    override fun onEvent(eventType: Int, params: Bundle?) {}

    fun destroy() {
        speechRecognizer?.destroy()
        speechRecognizer = null
    }
}
