---
phase: 3
plan: 1
wave: 1
depends_on: []
files_modified: 
  - "app/src/main/AndroidManifest.xml"
  - "app/src/main/java/com/jarvis/JarvisService.kt"
autonomous: true
user_setup: []

must_haves:
  truths:
    - "A JSON intent parser exists to map Gemini output to native Android actions"
    - "The SET_ALARM intent automatically queues an alarm via AlarmClock"
  artifacts:
    - "executeIntent() function in JarvisService.kt"
---

# Plan 3.1: Intent Execution Engine & Alarms

<objective>
Build the core parsing switch block inside `JarvisService.kt` to route the structured JSON payload coming from Gemini into actionable Kotlin code. Implement the silent `SET_ALARM` intent as the first working action.
</objective>

<context>
Load for context:
- .gsd/SPEC.md
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/jarvis/JarvisService.kt
</context>

<tasks>

<task type="auto">
  <name>Add SET_ALARM Permission</name>
  <files>app/src/main/AndroidManifest.xml</files>
  <action>
    Add `<uses-permission android:name="com.android.alarm.permission.SET_ALARM" />` outside the `application` block in `AndroidManifest.xml`.
  </action>
  <verify>cat app/src/main/AndroidManifest.xml | Select-String "SET_ALARM"</verify>
  <done>Permission is registered in the manifest.</done>
</task>

<task type="auto">
  <name>Create Intent Engine</name>
  <files>app/src/main/java/com/jarvis/JarvisService.kt</files>
  <action>
    In `JarvisService`, create a new function `private fun executeIntent(json: JSONObject)`.
    Inside this function, extract the `"intent"` string.
    Create a Kotlin `when(intentName)` block to handle `SET_ALARM`, `MAKE_CALL`, `TAKE_NOTE`, `SEND_WHATSAPP`, and `UNKNOWN`.
    Hook up this `executeIntent(json)` call right after `tts?.speak()` inside the Gemini coroutine in the `onResults` callback.
  </action>
  <verify>Review `JarvisService.kt` for the `executeIntent` function and the internal `when` switch.</verify>
  <done>The parsing switch is wired up to the LLM processor.</done>
</task>

<task type="auto">
  <name>Implement SET_ALARM Action</name>
  <files>app/src/main/java/com/jarvis/JarvisService.kt</files>
  <action>
    Inside the `SET_ALARM` block of `executeIntent()`, extract `timeHour` and `timeMinute` from the `parameters` JSONObject.
    Create a native Android Intent targeting `android.provider.AlarmClock.ACTION_SET_ALARM`.
    Use `putExtra` for `AlarmClock.EXTRA_HOUR`, `AlarmClock.EXTRA_MINUTES`, and **crucially**, set `AlarmClock.EXTRA_SKIP_UI` to `true` to ensure Jarvis doesn't pop the clock app onto the screen.
    Add `Intent.FLAG_ACTIVITY_NEW_TASK` to the Intent since it is being fired from a background Service.
    Execute `startActivity(alarmIntent)`.
  </action>
  <verify>Review `JarvisService.kt` `SET_ALARM` block to ensure all intent putExtras and Flags match the spec.</verify>
  <done>Jarvis can set alarms natively based on voice commands.</done>
</task>

</tasks>

<verification>
After all tasks, verify:
- [ ] Build and run the app.
- [ ] Say "Jarvis".
- [ ] Say "Set an alarm for 6 thirty AM".
- [ ] Verify ADB logcat prints the correct parsed Gemini JSON.
- [ ] Verify you hear "I have set your alarm for 6:30 AM" via TTS.
- [ ] Verify an alarm is actually queued in the system Clock app for 06:30.
</verification>

<success_criteria>
- [ ] All tasks verified
- [ ] Must-haves confirmed
</success_criteria>
