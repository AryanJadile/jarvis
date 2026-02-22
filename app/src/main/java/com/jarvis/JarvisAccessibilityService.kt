package com.jarvis

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class JarvisAccessibilityService : AccessibilityService() {
    private val TAG = "JarvisAccessibility"

    companion object {
        var isJarvisSending = false
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (!isJarvisSending) return
        
        event ?: return
        if (event.packageName == "com.whatsapp") {
            CoroutineScope(Dispatchers.Main).launch {
                delay(1500) // Wait for WhatsApp chat UI to settle and load drafted text
                val rootNode = rootInActiveWindow
                if (rootNode != null) {
                    // Method 1: finding by widely used view ID for WhatsApp send button
                    var clicked = false
                    val sendNodes = rootNode.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")
                    if (sendNodes.isNotEmpty()) {
                        for (node in sendNodes) {
                            if (node.isClickable) {
                                Log.d(TAG, "Found WhatsApp send button by ID. Clicking...")
                                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                isJarvisSending = false
                                clicked = true
                                break
                            }
                        }
                    }
                    
                    // Method 2: fallback to searching content description if ID changes
                    if (!clicked) {
                        val fallbackNodes = rootNode.findAccessibilityNodeInfosByText("Send")
                        for (node in fallbackNodes) {
                            if (node.isClickable && (node.className?.contains("ImageButton") == true || node.className?.contains("Button") == true)) {
                                Log.d(TAG, "Found WhatsApp send button by Text/Description. Clicking...")
                                node.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                                isJarvisSending = false
                                break
                            }
                        }
                    }
                    rootNode.recycle()
                }
            }
        }
    }

    override fun onInterrupt() {
        Log.e(TAG, "Accessibility Service interrupted")
    }
}
