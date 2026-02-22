---
phase: 2
plan: 1
wave: 1
depends_on: []
files_modified: 
  - "app/build.gradle.kts"
  - "app/src/main/java/com/jarvis/JarvisService.kt"
autonomous: true
user_setup:
  - service: googleai
    why: "Requires API key for Gemini access"
    task: "Go to aistudio.google.com, create an API key, and add it to local.properties as GEMINI_API_KEY"

must_haves:
  truths:
    - "A valid Gemini API Key is injected into the build"
    - "The transcribed voice command is sent to Gemini"
    - "Gemini returns a parsed JSON response containing the user's intent"
  artifacts:
    - "A method in JarvisService that calls the Gemini API"
---

# Plan 2.1: Gemini AI SDK & JSON Prompting

<objective>
Integrate the official Google AI Client SDK for Android into `JarvisService` and implement the prompt engineering required to structure the user's spoken command into an executable JSON intent.

Purpose: To give Jarvis a "brain" capable of natural language understanding.
Output: ADB Logcat prints a structured JSON object detailing the exact action Jarvis should take when a user speaks a command.
</objective>

<context>
Load for context:
- .gsd/SPEC.md
- app/build.gradle.kts
- app/src/main/java/com/jarvis/JarvisService.kt
</context>

<tasks>

<task type="auto">
  <name>Add Gemini Dependency & Key Setup</name>
  <files>app/build.gradle.kts</files>
  <action>
    Add the Google AI SDK dependency: `implementation("com.google.ai.client.generativeai:generativeai:0.2.2")` (or latest stable, 0.2.x is good for API 26+).
    Update the `Properties()` parsing block at the top of the file to also read `GEMINI_API_KEY` from `local.properties`.
    Add a new `buildConfigField` for `GEMINI_API_KEY` in the `defaultConfig` block.
    Sync Gradle.
  </action>
  <verify>cat app/build.gradle.kts | Select-String "generativeai"</verify>
  <done>Gemini SDK dependency is in the build file alongside the new BuildConfig injected key.</done>
</task>

<task type="auto">
  <name>Implement Gemini Call in JarvisService</name>
  <files>app/src/main/java/com/jarvis/JarvisService.kt</files>
  <action>
    Inside `JarvisService.kt`, update `onResults()` where the transcribed command is obtained.
    1. Initialize the `GenerativeModel`. Use `"gemini-1.5-flash"` for speed.
    2. Pass the `GEMINI_API_KEY` from `BuildConfig`.
    3. Use `generationConfig` to explicitly set `responseMimeType = "application/json"`.
    4. Provide the system instruction as a `Content` block defining the JSON schema for 4 intents (SEND_WHATSAPP, SET_ALARM, MAKE_CALL, TAKE_NOTE) plus a `spokenResponse` field.
    5. In an asynchronous Coroutine scope (`GlobalScope.launch(Dispatchers.IO)` or a dedicated job), call `generativeModel.generateContent(command)`.
    6. Log the parsed JSON text heavily to Logcat.
  </action>
  <verify>Review `JarvisService.kt` to ensure `GenerativeModel` is instantiated and called within a Coroutine.</verify>
  <done>Transcribed text is passed to Gemini, which returns a structured JSON string to the debug log.</done>
</task>

</tasks>

<verification>
After all tasks, verify:
- [ ] Ensure `local.properties` contains `GEMINI_API_KEY=YOUR_KEY`
- [ ] Build and run the app.
- [ ] Say "Jarvis".
- [ ] Say "Remind me to wake up at 7am".
- [ ] Check ADB Logcat for the transcribed intent and ensure Gemini responded with `{ "intent": "SET_ALARM", "parameters": {"timeHour": 7, "timeMinute": 0}, "spokenResponse": "I've set your alarm for 7 AM." }`.
</verification>

<success_criteria>
- [ ] All tasks verified
- [ ] Must-haves confirmed
</success_criteria>
