---
phase: 3
plan: 3
wave: 3
depends_on: [1]
files_modified: 
  - "app/src/main/java/com/jarvis/JarvisService.kt"
autonomous: true
user_setup: []

must_haves:
  truths:
    - "Jarvis can save a text note locally"
    - "A file is created and appended to on the Android internal storage"
  artifacts:
    - "File I/O logic in JarvisService.kt"
---

# Plan 3.3: Local Notes

<objective>
Implement the `TAKE_NOTE` action inside the intent engine. Ensure Jarvis writes the transcribed note content securely to a local file on the device, allowing the user to read or retrieve it later.
</objective>

<context>
Load for context:
- .gsd/SPEC.md
- app/src/main/java/com/jarvis/JarvisService.kt
</context>

<tasks>

<task type="auto">
  <name>Implement Local Storage Appender</name>
  <files>app/src/main/java/com/jarvis/JarvisService.kt</files>
  <action>
    Inside the `TAKE_NOTE` block of `executeIntent()`, extract `noteContent` from the `parameters` JSONObject.
    Use standard Kotlin file I/O on `applicationContext.filesDir` to append to a file named `jarvis_notes.txt`.
    Format the entry with a timestamp, e.g., `"[DATE TIME] $noteContent\n"`. Note: `java.util.Date` or `android.text.format.DateFormat` is fine.
    Ensure this is wrapped in a `try/catch` and executes on the IO dispatcher (it should already be since `executeIntent` is called from the Gemini coroutine).
  </action>
  <verify>Review `JarvisService.kt` `TAKE_NOTE` block to ensure `openFileOutput` or `File(filesDir, ...).appendText` is used.</verify>
  <done>Jarvis can permanently save a note to internal storage.</done>
</task>

</tasks>

<verification>
After all tasks, verify:
- [ ] Build and run the app.
- [ ] Say "Jarvis".
- [ ] Say "Take a note that the gate code is 1 2 3 4".
- [ ] Verify you hear "I've noted that the gate code is 1234" via TTS.
- [ ] Use Android Studio Device File Explorer or ADB shell to pull `/data/data/com.jarvis/files/jarvis_notes.txt`.
- [ ] Verify the file contains the note with a timestamp.
</verification>

<success_criteria>
- [ ] All tasks verified
- [ ] Must-haves confirmed
</success_criteria>
