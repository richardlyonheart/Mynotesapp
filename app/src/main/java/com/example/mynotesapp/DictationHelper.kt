package com.example.mynotesapp

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.compose.runtime.mutableStateOf
import com.example.mynotesapp.data.Note
import com.example.mynotesapp.viewmodel.NoteViewModel

class DictationHelper(
    private val activity: Activity,
    private val viewModel: NoteViewModel
) {
    val recognizedText = mutableStateOf("")
    private val speechRecognizer: SpeechRecognizer =
        SpeechRecognizer.createSpeechRecognizer(activity)

    var isListening = false
        private set

    fun toggleListening() {
        if (isListening) stopListening() else startListening()
    }

    private fun startListening() {
        isListening = true
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
            )
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US")
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    val text = matches[0]
                    recognizedText.value = text

                    // Auto-save each dictated phrase
                    viewModel.insert(Note(title = "Dictated Note", content = text))

                    recognizedText.value = ""
                    // Restart listening for continuous dictation
                    if (isListening) startListening()
                }
            }

            override fun onError(error: Int) {
                if (isListening) startListening()
            }

            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onRmsChanged(rmsdB: Float) {}
        })

        speechRecognizer.startListening(intent)
    }

    fun stopListening() {
        isListening = false
        speechRecognizer.stopListening()
    }
}