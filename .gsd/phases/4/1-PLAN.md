---
phase: 4
plan: 1
wave: 1
depends_on: [3]
files_modified: 
  - "app/src/main/AndroidManifest.xml"
  - "app/src/main/res/xml/accessibility_service_config.xml"
  - "app/src/main/java/com/jarvis/JarvisAccessibilityService.kt"
  - "app/src/main/java/com/jarvis/JarvisService.kt"
autonomous: true
user_setup:
  - "Must manually enable Jarvis Accessibility Service in Android Settings > Accessibility"

must_haves:
  truths:
    - "Jarvis can auto-click the WhatsApp Send button"
    - "JarvisService safely launches WhatsApp with a pre-filled Intent"
  artifacts:
    - "JarvisAccessibilityService.kt"
    - "accessibility_service_config.xml"
---

# Plan 4.1: WhatsApp Integration (Accessibility Service)

<objective>
Implement the `SEND_WHATSAPP` logic. Since WhatsApp has no background text-sending API, we will use an `AccessibilityService` to detect when WhatsApp opens (triggered by Jarvis) and virtually click the "Send" button on behalf of the user, achieving a fully hands-free experience.
</objective>

<context>
Load for context:
- .gsd/SPEC.md
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/jarvis/JarvisService.kt
</context>

<tasks>

<task type="auto">
  <name>Create Accessibility Service Config</name>
  <files>app/src/main/res/xml/accessibility_service_config.xml</files>
  <action>
    Create the `res/xml` directory if it doesn't exist.
    Create `accessibility_service_config.xml`.
    Configure it to intercept window state changes (`typeWindowStateChanged` and `typeWindowContentChanged`) specifically for the `com.whatsapp` package.
    Set `accessibilityFlags="flagDefault"` and `canRetrieveWindowContent="true"`.
  </action>
  <verify>cat app/src/main/res/xml/accessibility_service_config.xml</verify>
  <done>The config XML restricts the service to only reading WhatsApp, minimizing privacy overhead.</done>
</task>

<task type="auto">
  <name>Implement JarvisAccessibilityService</name>
  <files>app/src/main/java/com/jarvis/JarvisAccessibilityService.kt</files>
  <action>
    Create a new class `JarvisAccessibilityService` extending `AccessibilityService`.
    Override `onAccessibilityEvent(event)`. If the event comes from `com.whatsapp`, get the `rootInActiveWindow`.
    Use `findAccessibilityNodeInfosByViewId("com.whatsapp:id/send")`. If it exists and is clickable, call `performAction(AccessibilityNodeInfo.ACTION_CLICK)`.
    (Bonus: only do this if a global boolean or shared preference `isJarvisSending` is true, resetting it afterward, to prevent auto-sending all user messages).
  </action>
  <verify>Review `JarvisAccessibilityService.kt` `onAccessibilityEvent` function.</verify>
  <done>The bot can press Send inside WhatsApp.</done>
</task>

<task type="auto">
  <name>Register Service in Manifest</name>
  <files>app/src/main/AndroidManifest.xml</files>
  <action>
    Add the `<service>` tag for `JarvisAccessibilityService`.
    Require permission `android.permission.BIND_ACCESSIBILITY_SERVICE`.
    Add an intent filter for `android.accessibilityservice.AccessibilityService`.
    Add the `meta-data` tag linking to `@xml/accessibility_service_config`.
  </action>
  <verify>Review `AndroidManifest.xml` beneath the `JarvisService` declaration.</verify>
  <done>Android OS is aware of the new Accessibility Service.</done>
</task>

<task type="auto">
  <name>Trigger WhatsApp Intent in JarvisService</name>
  <files>app/src/main/java/com/jarvis/JarvisService.kt</files>
  <action>
    In the `executeIntent` function under `SEND_WHATSAPP`, extract `targetName` and `messageText`.
    Use the `getPhoneNumber(name)` helper to resolve the contact.
    If found, strip non-numeric characters from the number (WhatsApp requires format like `15551234567`).
    Create an `Intent.ACTION_VIEW` deep linking to `Uri.parse("https://api.whatsapp.com/send?phone=$cleanNumber&text=${Uri.encode(messageText)}")`.
    Set `Intent.FLAG_ACTIVITY_NEW_TASK` and `setPackage("com.whatsapp")` to ensure it routes directly to the app.
    Broadcast or set the `isJarvisSending` boolean (static or preferences) to `true`.
    Execute `startActivity(whatsappIntent)`.
  </action>
  <verify>Review the `SEND_WHATSAPP` block in `JarvisService.kt` to ensure URL encoding and package name are correct.</verify>
  <done>Jarvis can wake up WhatsApp with a drafted message.</done>
</task>

</tasks>

<verification>
After all tasks, verify:
- [ ] Build the app and deploy to a real device (emulators often lack WhatsApp).
- [ ] Go to Android Settings > Accessibility > Installed Services > Jarvis and turn it ON.
- [ ] Ensure a dummy contact has a formatted phone number starting with the country code.
- [ ] Say "Jarvis".
- [ ] Say "Send WhatsApp to [Dummy Contact Name] saying hello".
- [ ] Verify you hear TTS confirm.
- [ ] Verify WhatsApp foregrounds, pastes "hello", and is instantly clicked/sent by the Accessibility Service.
</verification>

<success_criteria>
- [ ] All tasks verified
- [ ] Must-haves confirmed
</success_criteria>
