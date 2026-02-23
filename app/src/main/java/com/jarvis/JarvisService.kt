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
import android.speech.tts.TextToSpeech
import android.provider.AlarmClock
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
import com.google.ai.client.generativeai.type.RequestOptions

import org.json.JSONObject
import java.util.Locale

class JarvisService : Service(), RecognitionListener, TextToSpeech.OnInitListener {

    private val CHANNEL_ID = "jarvis_channel"
    private val NOTIFICATION_ID = 1
    private val TAG = "JarvisService"
    
    private var porcupineManager: PorcupineManager? = null
    private var speechRecognizer: SpeechRecognizer? = null
    private var mainHandler: Handler = Handler(Looper.getMainLooper())
    private var toneGenerator: ToneGenerator? = null
    private var generativeModel: GenerativeModel? = null
    private var tts: TextToSpeech? = null
    private var systemInstructionText: String = ""

    private fun createNotification(contentText: String = "Jarvis is running in the background"): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Jarvis")
            .setContentText(contentText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun updateNotification(text: String) {
        val notification = createNotification(text)
        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    override fun onCreate() {
        super.onCreate()
        Log.e(TAG, "JarvisService onCreate started")
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification("Starting up..."))
        
        toneGenerator = ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100)
        tts = TextToSpeech(this, this)
        
        mainHandler.post {
            initSpeechRecognizer()
        }
        updateNotification("Initializing up Porcupine...")
        initPorcupine()
        updateNotification("Initializing Gemini...")
        initGenerativeModel()
        updateNotification("Ready: Listening for wake word")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale.US)
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                Log.e(TAG, "TTS Language is not supported.")
            } else {
                Log.e(TAG, "TextToSpeech initialized successfully.")
            }
        } else {
            Log.e(TAG, "TextToSpeech initialization failed.")
        }
    }

    private fun initGenerativeModel() {
        val geminiApiKey = BuildConfig.GEMINI_API_KEY
        if (geminiApiKey.isEmpty() || geminiApiKey == "YOUR_KEY") {
            Log.e(TAG, "Gemini API Key is missing!")
            return
        }

        systemInstructionText = "You are the brain of an Android mobile assistant. Your job is to parse the user's spoken command and determine which of the 4 supported intents they want to execute:\n" +
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
                 "}"
        generativeModel = GenerativeModel(
            modelName = "gemini-2.5-flash",
            apiKey = geminiApiKey
        )
        Log.e(TAG, "GenerativeModel initialized for JSON output.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.e(TAG, "JarvisService Started via onStartCommand")
        return START_STICKY
    }

    private fun initSpeechRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(this)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)
            speechRecognizer?.setRecognitionListener(this)
            Log.e(TAG, "SpeechRecognizer initialized")
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
                    Log.e(TAG, "===============================================")
                    Log.e(TAG, "WAKE WORD DETECTED: JARVIS")
                    Log.e(TAG, "===============================================")
                    handleWakeWordDetected()
                }
            }

            porcupineManager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                .build(applicationContext, keywordCallback)

            porcupineManager?.start()
            Log.e(TAG, "Porcupine initialized and listening for 'Jarvis'")

        } catch (e: PorcupineException) {
            Log.e(TAG, "Failed to initialize Porcupine: ${e.message}")
            updateNotification("Error: Porcupine init failed")
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
                updateNotification("Wake Word! Starting Speech Recognizer...")
                val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                    putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
                    putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                }
                Log.e(TAG, "Starting SpeechRecognizer for command input...")
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
                Log.e(TAG, "Porcupine restarted, listening for wake word again.")
            } catch (e: PorcupineException) {
                Log.e(TAG, "Failed to restart Porcupine: ${e.message}")
            }
        }
    }

    // --- SpeechRecognizer Callbacks ---

    override fun onReadyForSpeech(params: Bundle?) {
        Log.e(TAG, "SpeechRecognizer: Ready for speech")
        updateNotification("Listening for your command...")
    }

    override fun onBeginningOfSpeech() {
        Log.e(TAG, "SpeechRecognizer: Beginning of speech")
        updateNotification("Hearing speech...")
    }

    override fun onRmsChanged(rmsdB: Float) {}

    override fun onBufferReceived(buffer: ByteArray?) {}

    override fun onEndOfSpeech() {
        Log.e(TAG, "SpeechRecognizer: End of speech")
        updateNotification("Processing command...")
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
        updateNotification("SR Error: $errorMessage")
        restartPorcupine()
    }

    override fun onResults(results: Bundle?) {
        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
        if (!matches.isNullOrEmpty()) {
            val command = matches[0]
            Log.e(TAG, ">>> TRANSCRIBED COMMAND: \"$command\" <<<")
            updateNotification("Parsed: $command")
            
            if (generativeModel != null) {
                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        Log.e(TAG, "Sending command to Gemini...")
                        val fullPrompt = "$systemInstructionText\n\nUser command: $command"
                        val response = generativeModel?.generateContent(fullPrompt)
                        val responseText = response?.text ?: ""
                        Log.e(TAG, "=== GEMINI JSON RESPONSE ===")
                        Log.e(TAG, responseText)
                        Log.e(TAG, "============================")
                        
                        if (responseText.isNotEmpty()) {
                            // Extract spoken response and talk
                            val json = JSONObject(responseText)
                            val spokenResponse = json.optString("spokenResponse")
                            if (spokenResponse.isNotEmpty()) {
                                updateNotification("Speaking: $spokenResponse")
                                tts?.speak(spokenResponse, TextToSpeech.QUEUE_FLUSH, null, "JarvisUtteranceId")
                            }
                            executeIntent(json)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Gemini generation failed: ${e.message}")
                        updateNotification("Gemini error: ${e.message}")
                    } finally {
                        restartPorcupine()
                    }
                }
            } else {
                restartPorcupine()
            }
        } else {
            Log.e(TAG, "SpeechRecognizer finished but returned no text.")
            updateNotification("No text transcribed")
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
        tts?.stop()
        tts?.shutdown()
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            Log.e(TAG, "Porcupine stopped and deleted")
        } catch (e: PorcupineException) {
            Log.e(TAG, "Error stopping Porcupine: ${e.message}")
        }
        mainHandler.post {
            speechRecognizer?.destroy()
            Log.e(TAG, "SpeechRecognizer destroyed")
        }
        Log.e(TAG, "JarvisService Destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    private fun executeIntent(json: JSONObject) {
        val intentName = json.optString("intent", "UNKNOWN")
        val parameters = json.optJSONObject("parameters") ?: JSONObject()

        when (intentName) {
            "SET_ALARM" -> {
                val timeHour = parameters.optInt("timeHour", -1)
                val timeMinute = parameters.optInt("timeMinute", -1)

                if (timeHour != -1 && timeMinute != -1) {
                    val alarmIntent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                        putExtra(AlarmClock.EXTRA_HOUR, timeHour)
                        putExtra(AlarmClock.EXTRA_MINUTES, timeMinute)
                        putExtra(AlarmClock.EXTRA_SKIP_UI, true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    try {
                        startActivity(alarmIntent)
                        Log.e(TAG, "Alarm set for $timeHour:$timeMinute")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to set alarm: ${e.message}")
                    }
                } else {
                    Log.e(TAG, "Invalid alarm parameters from Gemini: hour=$timeHour, min=$timeMinute")
                }
            }
            "MAKE_CALL" -> {
                val targetName = parameters.optString("targetName", "")
                if (targetName.isNotEmpty()) {
                    val number = getPhoneNumber(targetName)
                    if (number != null) {
                        val callIntent = Intent(Intent.ACTION_CALL).apply {
                            data = android.net.Uri.parse("tel:$number")
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK
                        }
                        try {
                            startActivity(callIntent)
                            Log.e(TAG, "Calling $targetName ($number)")
                        } catch (e: Exception) {
                            Log.e(TAG, "Failed to make call: ${e.message}")
                        }
                    } else {
                        Log.e(TAG, "Could not find contact for $targetName")
                        tts?.speak("I couldn't find a contact named $targetName.", TextToSpeech.QUEUE_FLUSH, null, "JarvisUtteranceId")
                    }
                } else {
                    Log.e(TAG, "Invalid call parameters from Gemini: targetName=$targetName")
                }
            }
            "TAKE_NOTE" -> {
                val noteContent = parameters.optString("noteContent", "")
                if (noteContent.isNotEmpty()) {
                    try {
                        val timestamp = java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault()).format(java.util.Date())
                        val file = java.io.File(applicationContext.filesDir, "jarvis_notes.txt")
                        file.appendText("[$timestamp] $noteContent\n")
                        Log.e(TAG, "Note saved to ${file.absolutePath}")
                    } catch (e: Exception) {
                        Log.e(TAG, "Failed to save note: ${e.message}")
                    }
                } else {
                    Log.e(TAG, "Invalid note parameters from Gemini: noteContent is empty")
                }
            }
            "SEND_WHATSAPP" -> {
                val targetName = parameters.optString("targetName", "")
                val messageText = parameters.optString("messageText", "")
                if (targetName.isNotEmpty() && messageText.isNotEmpty()) {
                    val number = getPhoneNumber(targetName)
                    if (number != null) {
                        try {
                            // WhatsApp requires pure numbers with country code (e.g. 15551234567)
                            val cleanNumber = number.replace(Regex("[^0-9]"), "")
                            val whatsappIntent = Intent(Intent.ACTION_VIEW).apply {
                                data = android.net.Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${android.net.Uri.encode(messageText)}")
                                flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                setPackage("com.whatsapp")
                            }
                            // Notify our AccessibilityService to auto-send this specific message
                            JarvisAccessibilityService.isJarvisSending = true
                            startActivity(whatsappIntent)
                            Log.e(TAG, "Launched WhatsApp for $targetName ($cleanNumber)")
                        } catch (e: Exception) {
                            Log.e(TAG, "WhatsApp not installed or failed to launch: ${e.message}")
                            JarvisAccessibilityService.isJarvisSending = false
                            tts?.speak("I couldn't open WhatsApp. Is it installed?", TextToSpeech.QUEUE_FLUSH, null, "JarvisUtteranceId")
                        }
                    } else {
                        Log.e(TAG, "Could not find contact for $targetName")
                        tts?.speak("I couldn't find a contact named $targetName.", TextToSpeech.QUEUE_FLUSH, null, "JarvisUtteranceId")
                    }
                } else {
                    Log.e(TAG, "Invalid WhatsApp parameters from Gemini: target=$targetName, msg=$messageText")
                }
            }
            "UNKNOWN" -> Log.e(TAG, "Gemini could not determine intent.")
        }
    }

    private fun getPhoneNumber(name: String): String? {
        val resolver = applicationContext.contentResolver
        val uri = android.provider.ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${android.provider.ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        val selectionArgs = arrayOf("%$name%")
        
        var number: String? = null
        resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                val numberIndex = cursor.getColumnIndex(android.provider.ContactsContract.CommonDataKinds.Phone.NUMBER)
                if (numberIndex >= 0) {
                    number = cursor.getString(numberIndex)
                }
            }
        }
        return number
    }
}
