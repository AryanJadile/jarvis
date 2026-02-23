package com.jarvis

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class MainActivity : AppCompatActivity() {

    private lateinit var tvBasePermissions: TextView
    private lateinit var tvAccessibility: TextView
    private lateinit var btnRequestPermissions: Button
    private lateinit var btnOpenAccessibility: Button

    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            permissions.entries.forEach {
                val permissionName = it.key
                val isGranted = it.value
                Log.e("JarvisPermissions", "$permissionName granted: $isGranted")
            }
            updateStatus()
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                val serviceIntent = Intent(this, JarvisService::class.java)
                ContextCompat.startForegroundService(this, serviceIntent)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        tvBasePermissions = findViewById(R.id.tvBasePermissions)
        tvAccessibility = findViewById(R.id.tvAccessibility)
        btnRequestPermissions = findViewById(R.id.btnRequestPermissions)
        btnOpenAccessibility = findViewById(R.id.btnOpenAccessibility)

        btnRequestPermissions.setOnClickListener {
            requestPermissions()
        }

        btnOpenAccessibility.setOnClickListener {
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            startActivity(intent)
        }

        // We launch the Jarvis service in the foreground only if permission is granted,
        // otherwise we wait until it's granted from the request permission launcher.
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            Log.e("JarvisMainActivity", "RECORD_AUDIO granted, attempting to start JarvisService")
            val serviceIntent = Intent(this, JarvisService::class.java)
            try {
                ContextCompat.startForegroundService(this, serviceIntent)
                Log.e("JarvisMainActivity", "Successfully called startForegroundService for JarvisService")
            } catch (e: Exception) {
                Log.e("JarvisMainActivity", "Failed to start JarvisService: ${e.message}")
            }
        } else {
             Log.e("JarvisMainActivity", "RECORD_AUDIO not granted, deferring JarvisService start")
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
    }

    private fun updateStatus() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }

        val allGranted = permissionsToRequest.all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }

        if (allGranted) {
            tvBasePermissions.text = "All Granted"
            tvBasePermissions.setTextColor(Color.GREEN)
        } else {
            tvBasePermissions.text = "Missing Permissions"
            tvBasePermissions.setTextColor(Color.RED)
        }

        if (isAccessibilityServiceEnabled()) {
            tvAccessibility.text = "Enabled"
            tvAccessibility.setTextColor(Color.GREEN)
        } else {
            tvAccessibility.text = "Disabled"
            tvAccessibility.setTextColor(Color.RED)
        }
    }

    private fun isAccessibilityServiceEnabled(): Boolean {
        val expectedComponentName = "${packageName}/${JarvisAccessibilityService::class.java.name}"
        val enabledServices = Settings.Secure.getString(
            contentResolver,
            Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
        ) ?: return false

        val colonSplitter = TextUtils.SimpleStringSplitter(':')
        colonSplitter.setString(enabledServices)
        while (colonSplitter.hasNext()) {
            val componentName = colonSplitter.next()
            if (componentName.equals(expectedComponentName, ignoreCase = true)) {
                return true
            }
        }
        return false
    }

    private fun requestPermissions() {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.CALL_PHONE,
            Manifest.permission.READ_CONTACTS
        )
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        
        requestPermissionLauncher.launch(permissionsToRequest.toTypedArray())
    }
}
