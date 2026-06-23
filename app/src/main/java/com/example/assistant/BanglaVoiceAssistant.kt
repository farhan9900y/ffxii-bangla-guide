package com.example.assistant

import android.content.Context
import android.speech.tts.TextToSpeech
import android.util.Log
import java.util.Locale

class BanglaVoiceAssistant(
    private val context: Context,
    private val onInitCompleted: (Boolean) -> Unit = {}
) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    var isReady = false
        private set

    init {
        try {
            tts = TextToSpeech(context, this)
        } catch (e: Exception) {
            Log.e("BanglaVoiceAssistant", "Error creating TTS: ${e.message}")
            onInitCompleted(false)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val locale = Locale("bn", "BD")
            val result = tts?.setLanguage(locale)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e("BanglaVoiceAssistant", "Bangla (bn_BD) is not supported, trying bn.")
                val fallbackResult = tts?.setLanguage(Locale("bn"))
                if (fallbackResult == TextToSpeech.LANG_MISSING_DATA || fallbackResult == TextToSpeech.LANG_NOT_SUPPORTED) {
                    Log.e("BanglaVoiceAssistant", "Any Bengali language is unsupported on this engine.")
                    isReady = false
                    onInitCompleted(false)
                } else {
                    configureVoiceParams()
                    onInitCompleted(true)
                }
            } else {
                configureVoiceParams()
                onInitCompleted(true)
            }
        } else {
            Log.e("BanglaVoiceAssistant", "TTS Initialization failed.")
            isReady = false
            onInitCompleted(false)
        }
    }

    private fun configureVoiceParams() {
        tts?.setPitch(0.95f)      // Slightly lower pitch for a friendly male gamer tone
        tts?.setSpeechRate(0.88f)   // Natural, slightly slower to make Bangla letters pronounce beautifully
        isReady = true
    }

    fun speak(text: String) {
        if (tts != null) {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "FFXII_Jini_Voice_Utterance")
        } else {
            Log.w("BanglaVoiceAssistant", "TTS is null, can't speak.")
        }
    }

    fun stop() {
        tts?.stop()
    }

    fun shutdown() {
        try {
            tts?.stop()
            tts?.shutdown()
            tts = null
        } catch (e: Exception) {
            Log.e("BanglaVoiceAssistant", "Error shutting down TTS: ${e.message}")
        }
    }
}
