package com.openchat.app.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceManager @Inject constructor(
    @ApplicationContext private val context: Context
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isInitialized = false
    private var pendingText: String? = null

    init {
        tts = TextToSpeech(context, this)
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            tts?.language = Locale.getDefault()
            isInitialized = true
            pendingText?.let { 
                speak(it)
                pendingText = null
            }
        }
    }

    fun speak(text: String, speed: Float = 1.0f, pitch: Float = 1.0f) {
        if (!isInitialized) {
            pendingText = text
            return
        }
        
        if (text.isNotBlank()) {
            tts?.setSpeechRate(speed)
            tts?.setPitch(pitch)
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "TTS_ID")
        }
    }

    fun stop() {
        if (isInitialized) {
            tts?.stop()
        }
    }
    
    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
    }
}
