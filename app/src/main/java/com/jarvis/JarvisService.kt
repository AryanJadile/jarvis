package com.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.media.ToneGenerator
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.app.NotificationCompat
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import ai.picovoice.porcupine.PorcupineException

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.ai.client.generativeai.type.generationConfig

class JarvisService : Service(), RecognitionListener {

    private val CHANNEL_ID = "jarvis_channel"
    private val NOTIFICATION_ID = 1
    private val TAG = "JarvisService"
    
    private var porcupineManager: PorcupineManager? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var mainHandler: Handler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null
    private var generativeModel: GenerativeModel? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        
        mainHandler.post {
            initSpeechRecognizer()
        }
        initPorcupine()
        initGenerativeModel()
    }

    private fun initGenerativeModel() {
        val geminiApiKey = BuildConfig.GEMINI_API_KEY
        if (geminiApiKey.isEmpty() || geminiApiKey == "YOUR_KEY") {
            Log.e(TAG, "Gemini API Key is missing!")
            return
        }

        val systemInstruction = content {
            text("You are the brain of an Android mobile assistant. Your job is to parse the user's spoken command and determine which of the 4 supported intents they want to execute:\n" +
                 "1. SEND_WHATSAPP (Requires 'targetName' and 'messageText')\n" +
                 "2. SET_ALARM (Requires 'timeHour' and 'timeMinute' in 24h format)\n" +
                 "3. MAKE_CALL (Requires 'targetName')\n" +
                 "4. TAKE_NOTE (Requires 'noteContent')\n" +
                 "5. UNKNOWN (If the command doesn't match the above)\n\n" +
                 "You must ALWAYS respond in valid JSON matching this schema:\n" +
                 "{\n" +
                 "  \"intent\": \"<INTENT_NAME>\",\n" +
                 "  \"parameters\": { ... },\n" +
                 "  \"spokenResponse\": \"<A short, natural sentence to speak back to the user acknowledging the action>\"\n" +
                 "}")
        }

        generativeModel = GenerativeModel(
            modelName = "gemini-1.5-flash",
            apiKey = geminiApiKey,
            systemInstruction = systemInstruction,
            generationConfig = generationConfig {
                responseMimeType = "application/json"
            }
        )
        Log.d(TAG, "GenerativeModel initialized for JSON output.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "JarvisService Started via onStartCommand")
        return START_STICKY
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(this)
            Log.d(TAG, "SpeechRecognizer initialized")
        } else {
            Log.e(TAG, "Speech Recognition is not available on this device.")
        }
    }

    private fun initPorcupine() {
        val accessKey = BuildConfig.PICOVOICE_API_KEY
        
        if (accessKey.isEmpty() || accessKey == "YOUR_KEY") {
            Log.e(TAG, "Picovoice Access Key is missing! Cannot start Porcupine.")
            return
        }

        try {
            val keywordCallback = PorcupineManagerCallback { keywordIndex ->
                if (keywordIndex == 0) {
                    Log.d(TAG, "===============================================")
                    Log.d(TAG, "WAKE WORD DETECTED: JARVIS")
                    Log.d(TAG, "===============================================")
                    handleWakeWordDetected()
                }
            }

            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                .build(applicationContext, keywordCallback)

            porcupineManager?.start()
            Log.d(TAG, "Porcupine initialized and listening for 'Jarvis'")

        } catch (e: PorcupineException) {
            Log.e(TAG, "Failed to initialize Porcupine: ${e.message}")
        }
    }
    
    private fun handleWakeWordDetected() {
        try {
            // 1. Pause Porcupine
            porcupineManager?.stop()
            
            // 2. Play a beep sound to acknowledge
            toneGenerator?.startTone(ToneGenerator.TONE_PROP_BEEP)
            
            // 3. Start Speech Recognizer on main thread
            mainHandler.post {
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                Log.d(TAG, "Starting SpeechRecognizer for command input...")
                speechRecognizer?.startListening(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling wake word: ${e.message}")
            restartPorcupine()
        }
    }
    
    private fun restartPorcupine() {
        mainHandler.post {
            try {
                porcupineManager?.start()
                Log.d(TAG, "Porcupine restarted, listening for wake word again.")
            } catch (e: PorcupineException) {
                Log.e(TAG, "Failed to restart Porcupine: ${e.message}")
            }
        }
    }

    // --- SpeechRecognizer Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        Log.d(TAG, "SpeechRecognizer: Ready for speech")
    }

    override fun onBeginningOfSpeech() {
        Log.d(TAG, "SpeechRecognizer: Beginning of speech")
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.d(TAG, "SpeechRecognizer: End of speech")
    }

    override fun onError(error: Int) {
        val errorMessage = when (error) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client side error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "RecognitionService busy"
            SpeechRecognizer.ERROR_SERVER -> "Error from server"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No speech input"
            else -> "Didn't understand, please try again."
        }
        Log.e(TAG, "SpeechRecognizer Error: $errorMessage ($error)")
        restartPorcupine()
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val command = matches[0]
            Log.d(TAG, ">>> TRANSCRIBED COMMAND: \"$command\" <<<")
            
            if (generativeModel != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.d(TAG, "Sending command to Gemini...")
                        val response = generativeModel?.generateContent(command)
                        Log.d(TAG, "=== GEMINI JSON RESPONSE ===")
                        Log.d(TAG, response?.text ?: "Empty response")
                        Log.d(TAG, "============================")
                    } catch (e: Exception) {
                        Log.e(TAG, "Gemini generation failed: ${e.message}")
                    } finally {
                        restartPorcupine()
                    }
                }
            } else {
                restartPorcupine()
            }
        } else {
            Log.d(TAG, "SpeechRecognizer finished but returned no text.")
            restartPorcupine()
        }
    }

    override fun onPartialResults(partialResults: Bundle?) {}

    override fun onEvent(eventType: Int, params: Bundle?) {}

    // --- Notification & Lifecycle ---

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis is Listening")
            .setContentText("Waiting for wake word...")
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    override fun onDestroy() {
        super.onDestroy()
        toneGenerator?.release()
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            Log.d(TAG, "Porcupine stopped and deleted")
        } catch (e: PorcupineException) {
            Log.e(TAG, "Error stopping Porcupine: ${e.message}")
        }
        mainHandler.post {
            speechRecognizer?.destroy()
            Log.d(TAG, "SpeechRecognizer destroyed")
        }
        Log.d(TAG, "JarvisService Destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
