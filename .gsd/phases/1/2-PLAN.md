---
phase: 1
plan: 2
wave: 2
depends_on: [1]
files_modified: 
  - "app/build.gradle.kts"
  - "app/src/main/java/com/jarvis/JarvisService.kt"
  - "app/src/main/AndroidManifest.xml"
  - "app/src/main/java/com/jarvis/MainActivity.kt"
autonomous: true
user_setup:
  - service: picovoice
    why: "Requires access key for Porcupine"
    task: "Go to console.picovoice.ai, create a free account, copy the Access Key and put it in local.properties as PICOVOICE_API_KEY"

must_haves:
  truths:
    - "A Foreground Service runs continuously indicating Jarvis is listening"
    - "The Porcupine engine detects the 'Jarvis' wake word offline"
  artifacts:
    - "app/src/main/java/com/jarvis/JarvisService.kt exists"
---

# Plan 1.2: Porcupine Wakeword Listener

<objective>
Implement the core background listener using Picovoice Porcupine inside an Android Foreground Service. 

Purpose: Jarvis must be able to wake up to a specific word without draining the battery or relying on constant cloud streaming.
Output: A Foreground Service that logs to logcat when "Jarvis" is heard.
</objective>

<context>
Load for context:
- .gsd/SPEC.md
- app/build.gradle.kts
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/jarvis/MainActivity.kt
</context>

<tasks>

<task type="auto">
  <name>Add Porcupine Dependency</name>
  <files>app/build.gradle.kts</files>
  <action>
    Add the `ai.picovoice:porcupine-android:3.0.1` dependency to the app-level `build.gradle.kts`.
    Sync the project.
  </action>
  <verify>cat app/build.gradle.kts | Select-String "porcupine-android"</verify>
  <done>Dependency is present in gradle file.</done>
</task>

<task type="auto">
  <name>Create Foreground Service</name>
  <files>app/src/main/java/com/jarvis/JarvisService.kt, app/src/main/AndroidManifest.xml, app/src/main/java/com/jarvis/MainActivity.kt</files>
  <action>
    Create `JarvisService` extending `Service`.
    1. Implement `onStartCommand` to promote it to a Foreground Service using `ServiceCompat.startForeground`.
    2. Create a persistent Notification with a "Listening" system icon. The channel ID must be "jarvis_channel".
    3. Declare the service in `AndroidManifest.xml` with `android:foregroundServiceType="microphone"`.
    4. Modify `MainActivity.kt` to start this service using `ContextCompat.startForegroundService` after permissions are granted.
  </action>
  <verify>cat app/src/main/AndroidManifest.xml | Select-String ".JarvisService"</verify>
  <done>JarvisService is declared and starts when MainActivity is launched.</done>
</task>

<task type="auto">
  <name>Implement Porcupine Manager</name>
  <files>app/src/main/java/com/jarvis/JarvisService.kt</files>
  <action>
    Inside `JarvisService`, instantiate `PorcupineManager`.
    1. Read the API key from `BuildConfig.PICOVOICE_API_KEY` (ensure `buildConfig = true` is in gradle and key is injected from `local.properties`).
    2. Use the built-in "Jarvis" keyword: `Porcupine.BuiltInKeyword.JARVIS`.
    3. Start the manager in `onCreate` or `onStartCommand`. Stop it in `onDestroy`.
    4. Provide a callback that logs "WAKE WORD DETECTED: JARVIS" heavily to Logcat.
    AVOID: DO NOT handle audio permissions in the Service. Assume MainActivity handled them.
  </action>
  <verify>cat app/src/main/java/com/jarvis/JarvisService.kt | Select-String "PorcupineManager"</verify>
  <done>PorcupineManager is initialized and listening within the service lifecycle.</done>
</task>

</tasks>

<verification>
After all tasks, verify:
- [ ] Ensure `local.properties` contains `PICOVOICE_API_KEY=YOUR_KEY`
- [ ] Build and run the app.
- [ ] Look at the device notifications: it should say Jarvis is running.
- [ ] Speak "Jarvis" near the emulator/phone.
- [ ] ADB Logcat must show "WAKE WORD DETECTED: JARVIS".
</verification>

<success_criteria>
- [ ] All tasks verified
- [ ] Must-haves confirmed
</success_criteria>
