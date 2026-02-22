---
phase: 1
plan: 1
wave: 1
depends_on: []
files_modified: 
  - "app/build.gradle.kts"
  - "app/src/main/AndroidManifest.xml"
  - "app/src/main/java/com/jarvis/MainActivity.kt"
autonomous: false
user_setup: 
  - service: android-studio
    why: "Generate base project structure"
    task: "Open Android Studio -> New Project -> Empty Views Activity -> Name: Jarvis, Package: com.jarvis, Language: Kotlin, Minimum SDK: API 26"

must_haves:
  truths:
    - "A valid, compilable Android project exists"
    - "The app can request and be granted RECORD_AUDIO permission"
  artifacts:
    - "app/src/main/AndroidManifest.xml has necessary permissions"
---

# Plan 1.1: Base Android Application Structure

<objective>
Initialize the base Android native project, setup essential dependencies, and implement the initial permission request flow.

Purpose: We need a valid Android project shell before we can implement the complex background services.
Output: A compiling Android app that asks the user for microphone and notification permissions immediately upon launch.
</objective>

<context>
Load for context:
- .gsd/SPEC.md
- .gsd/phases/1/RESEARCH.md
</context>

<tasks>

<task type="checkpoint:human-action">
  <name>Generate Base Android Project</name>
  <files>app/build.gradle.kts, settings.gradle.kts</files>
  <action>
    The AI cannot reliably generate a complex multi-file Android Gradle project structure from scratch via CLI. 
    The user must open Android Studio and generate the base Empty Views Activity project.
    Name: Jarvis. Package: com.jarvis. Language: Kotlin.
  </action>
  <verify>Check that `./gradlew build` completes successfully or `app/build.gradle.kts` exists.</verify>
  <done>Base Android project files exist in the directory.</done>
</task>

<task type="auto">
  <name>Configure Manifest Permissions</name>
  <files>app/src/main/AndroidManifest.xml</files>
  <action>
    Add the required permissions for the entire project to the manifest:
    - `android.permission.RECORD_AUDIO`
    - `android.permission.INTERNET` (needed for Picovoice validation)
    - `android.permission.FOREGROUND_SERVICE`
    - `android.permission.FOREGROUND_SERVICE_MICROPHONE` (required for Android 14+)
    - `android.permission.POST_NOTIFICATIONS` (required for Android 13+)
  </action>
  <verify>cat app/src/main/AndroidManifest.xml | Select-String "RECORD_AUDIO"</verify>
  <done>Manifest contains all 5 required permissions.</done>
</task>

<task type="auto">
  <name>Implement Permission Request UI</name>
  <files>app/src/main/java/com/jarvis/MainActivity.kt</files>
  <action>
    Modify MainActivity to check for and request `Manifest.permission.RECORD_AUDIO` and `Manifest.permission.POST_NOTIFICATIONS` onCreate.
    Use ActivityResultContracts.RequestMultiplePermissions().
    AVOID: Complex UI layouts. Just use standard Android Toast or Log to indicate if permissions were granted or denied. Keep it minimal.
  </action>
  <verify>Review MainActivity.kt to ensure the permission launcher is implemented and called onCreate.</verify>
  <done>App requests Mic and Notification permissions on launch.</done>
</task>

</tasks>

<verification>
After all tasks, verify:
- [ ] `./gradlew app:assembleDebug` builds successfully.
- [ ] User confirms that running the app triggers the Android permission dialogs.
</verification>

<success_criteria>
- [ ] All tasks verified
- [ ] Must-haves confirmed
</success_criteria>
