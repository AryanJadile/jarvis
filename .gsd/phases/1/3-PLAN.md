---
phase: 1
plan: 3
wave: 3
depends_on: [2]
files_modified: 
  - "app/src/main/java/com/jarvis/JarvisService.kt"
autonomous: true
user_setup: []

must_haves:
  truths:
    - "When 'Jarvis' is heard, the app starts listening for the user's command"
    - "The command is transcribed into text"
  artifacts:
    - "SpeechRecognizer implementation in JarvisService.kt"
---

# Plan 1.3: Voice Command Transcription

<objective>
Integrate Android's built-in `SpeechRecognizer` to capture and transcribe the user's spoken command immediately following the wake word.

Purpose: Once Jarvis wakes up, we need to know what the user wants it to do.
Output: A string of text representing the user's command logged to the console (e.g., "turn on the lights").
</objective>

<context>
Load for context:
- .gsd/SPEC.md
- app/src/main/java/com/jarvis/JarvisService.kt
</context>

<tasks>

<task type="auto">
  <name>Initialize SpeechRecognizer</name>
  <files>app/src/main/java/com/jarvis/JarvisService.kt</files>
  <action>
    In `JarvisService`, initialize `SpeechRecognizer.createSpeechRecognizer(this)`.
    Set a `RecognitionListener`.
    Implement `onResults` to extract the spoken text: `results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.get(0)`.
    Log the transcribed text heavily to Logcat.
    AVOID: DO NOT initialize visual UI elements. This must run in the background service.
  </action>
  <verify>cat app/src/main/java/com/jarvis/JarvisService.kt | Select-String "SpeechRecognizer"</verify>
  <done>SpeechRecognizer is instantiated and has a listener attached.</done>
</task>

<task type="auto">
  <name>Link Porcupine to SpeechRecognizer</name>
  <files>app/src/main/java/com/jarvis/JarvisService.kt</files>
  <action>
    When Porcupine detects "Jarvis", execute the handoff logic:
    1. Stop or pause the `PorcupineManager`.
    2. Optional: Play a short "beep" or acknowledgment sound using `MediaPlayer` or `ToneGenerator` so the user knows it's listening.
    3. Call `speechRecognizer.startListening(intent)`. The intent should use `RecognizerIntent.ACTION_RECOGNIZE_SPEECH` and `EXTRA_LANGUAGE_MODEL` set to `LANGUAGE_MODEL_FREE_FORM`.
  </action>
  <verify>Review `JarvisService.kt` to ensure `startListening` is called inside the Porcupine wake word callback.</verify>
  <done>Detecting the wake word triggers the speech recognizer to start listening.</done>
</task>

<task type="auto">
  <name>Restart Listening Loop</name>
  <files>app/src/main/java/com/jarvis/JarvisService.kt</files>
  <action>
    After the `SpeechRecognizer` finishes (either via `onResults`, `onError`, or timeout), restart `PorcupineManager` so Jarvis is ready for the next wake word.
    Ensure errors (like NO_MATCH) are gracefully handled and still restart Porcupine.
  </action>
  <verify>Review the `RecognitionListener` callbacks (`onResults`, `onError`) to ensure `porcupineManager.start()` is called.</verify>
  <done>Jarvis resumes waiting for the wake word after a command is transcribed or fails.</done>
</task>

</tasks>

<verification>
After all tasks, verify:
- [ ] Build and run the app.
- [ ] Say "Jarvis".
- [ ] Wait for a moment, then say "What time is it?".
- [ ] Verify ADB Logcat shows "Jarvis" detected, followed by the transcription "what time is it".
</verification>

<success_criteria>
- [ ] All tasks verified
- [ ] Must-haves confirmed
</success_criteria>
