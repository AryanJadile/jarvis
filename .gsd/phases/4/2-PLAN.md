---
phase: 4
plan: 2
wave: 2
depends_on: [1]
files_modified: 
  - "app/src/main/res/layout/activity_main.xml"
  - "app/src/main/java/com/jarvis/MainActivity.kt"
autonomous: true
user_setup: []

must_haves:
  truths:
    - "MainActivity provides a dashboard of all required permissions"
    - "Buttons exist to take the user to Android Settings if Foreground Service or Accessibility are missing"
  artifacts:
    - "MainActivity views"
---

# Plan 4.2: UI Polish (Permission Checks)

<objective>
Because Jarvis relies deeply on system permissions that might be revoked or difficult to find (Accessibility), the `MainActivity` must be transformed into a simple, reliable dashboard. It should visibly display red/green status indicators for Microphone, Contacts, Phone, and Accessibility, with actionable buttons to fix them natively.
</objective>

<context>
Load for context:
- .gsd/SPEC.md
- app/src/main/java/com/jarvis/MainActivity.kt
</context>

<tasks>

<task type="auto">
  <name>Build Permissions UI Layout</name>
  <files>app/src/main/res/layout/activity_main.xml</files>
  <action>
    Convert `activity_main.xml` to a `LinearLayout` or `ConstraintLayout` with a scroll view.
    Add text views indicating the status of:
    1. Base Permissions (Mic, Notifications, Phone, Contacts)
    2. Jarvis Accessibility Service
    Add two Buttons:
    1. "Request Runtime Permissions"
    2. "Open Accessibility Settings"
  </action>
  <verify>Review the XML layout source to ensure views exist.</verify>
  <done>The visual structure for the dashboard is complete.</done>
</task>

<task type="auto">
  <name>Implement Status Logic in MainActivity</name>
  <files>app/src/main/java/com/jarvis/MainActivity.kt</files>
  <action>
    In `onResume()`, execute a permission check across `Manifest.permission.RECORD_AUDIO`, `CALL_PHONE`, `READ_CONTACTS`. Update the UI text/colors to reflect granted status.
    Implement a helper `isAccessibilityServiceEnabled()` that checks `Settings.Secure.getString(contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)` to look for `com.jarvis/.JarvisAccessibilityService`. Update the Accessibility UI text/color.
  </action>
  <verify>Review `MainActivity.kt` `onResume` and helper functions.</verify>
  <done>The UI dynamically reflects the true state of the required system privileges.</done>
</task>

<task type="auto">
  <name>Wire Up Settings Intents</name>
  <files>app/src/main/java/com/jarvis/MainActivity.kt</files>
  <action>
    Wire the "Request Runtime Permissions" button to fire the existing `requestPermissionLauncher`.
    Wire the "Open Accessibility Settings" button to fire `Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)`.
  </action>
  <verify>Review `MainActivity.kt` button click listeners.</verify>
  <done>The user can navigate directly to the correct Android settings menu.</done>
</task>

</tasks>

<verification>
After all tasks, verify:
- [ ] Build and launch the app visually.
- [ ] Verify the UI shows the current state of permissions.
- [ ] Click "Open Accessibility Settings", grant Jarvis the privilege, and press back.
- [ ] Verify the UI dynamically updates to show that Accessibility is active.
</verification>

<success_criteria>
- [ ] All tasks verified
- [ ] Must-haves confirmed
</success_criteria>
