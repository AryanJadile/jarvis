# Phase 4 Research: WhatsApp Integration & UI Polish

## Objective
Take the `SEND_WHATSAPP` parsed JSON intent from Gemini and send a WhatsApp message to the specified contact natively on Android.
Followed by polishing the UI to ensure the background permissions are fully transparent and cleanly requested.

## 1. WhatsApp Intent vs Accessibility Service

### Method A: The Standard WhatsApp URL Scheme
- **Action:** `Intent.ACTION_VIEW`
- **Data:** `Uri.parse("https://api.whatsapp.com/send?phone=$number&text=$message")`
- **Pros:** simple, requires no extra permissions, officially supported.
- **Cons:** It *opens* the WhatsApp UI and pre-fills the text field. The user still has to physically tap "Send". This violates the "hands-free" goal of Jarvis.

### Method B: Accessibility Service UI Automation (Selected Method)
- **Action:** Build an Android `AccessibilityService`.
- **How it works:** 
  1. Trigger Method A to open the chat natively.
  2. The `AccessibilityService` detects that the `com.whatsapp` package has opened on the screen.
  3. The service scans the view hierarchy for the "Send" button (usually an `ImageButton` with a specific content description) and performs an `AccessibilityNodeInfo.ACTION_CLICK`.
- **Pros:** 100% hands-free.
- **Cons:** Requires the user to dig into Android Settings > Accessibility to grant Jarvis special screen-reading privileges. 

**Decision:** We must use **Method B**. An AI assistant that forces you to touch the screen to send a message is useless.

## 2. AccessibilityService Architecture
We need to:
1. Create `JarvisAccessibilityService.kt` extending `AccessibilityService`.
2. Register it in the `AndroidManifest.xml` with the `BIND_ACCESSIBILITY_SERVICE` permission and an XML configuration file (`@xml/accessibility_service_config`).
3. Inside `onAccessibilityEvent(event)`, check if `event.packageName == "com.whatsapp"`.
4. Delay briefly to allow the UI to render, then search for `AccessibilityNodeInfo.findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")` or by content description ("Send").
5. Perform `ACTION_CLICK`.
6. Use a global flag/Intent extra so we *only* auto-click when Jarvis initiated the message (we don't want to auto-send every message the user types manually).

## 3. UI Polish
Because Jarvis relies on scary-looking permissions (Foreground Microphone, Contacts, Phone, and now Accessibility), `MainActivity` should act as a dashboard showing the status of these permissions with buttons to jump to settings if they are missing.
