package com.jarvis

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import ai.picovoice.porcupine.PorcupineException

class JarvisService : Service() {

    private val CHANNEL_ID = "jarvis_channel"
    private val NOTIFICATION_ID = 1
    private val TAG = "JarvisService"
    
    private var porcupineManager: PorcupineManager? = null

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        initPorcupine()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "JarvisService Started via onStartCommand")
        return START_STICKY
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
                    // In Plan 3, we will hand this off to SpeechRecognizer
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
        try {
            porcupineManager?.stop()
            porcupineManager?.delete()
            Log.d(TAG, "Porcupine stopped and deleted")
        } catch (e: PorcupineException) {
            Log.e(TAG, "Error stopping Porcupine: ${e.message}")
        }
        Log.d(TAG, "JarvisService Destroyed")
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}
