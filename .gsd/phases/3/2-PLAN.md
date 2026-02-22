---
phase: 3
plan: 2
wave: 2
depends_on: [1]
files_modified: 
  - "app/src/main/AndroidManifest.xml"
  - "app/src/main/java/com/jarvis/MainActivity.kt"
  - "app/src/main/java/com/jarvis/JarvisService.kt"
autonomous: true
user_setup: []

must_haves:
  truths:
    - "Jarvis requests CALL_PHONE and READ_CONTACTS permissions on startup"
    - "The MAKE_CALL intent parses a name, finds the number, and initiates a phone call"
  artifacts:
    - "Contact lookup logic in JarvisService.kt"
---

# Plan 3.2: Phone Calls

<objective>
Implement the `MAKE_CALL` action inside the intent engine. Ensure Jarvis can read the device's contacts, match the spoken name with a phone number, and fire a system phone call intent natively.
</objective>

<context>
Load for context:
- .gsd/SPEC.md
- app/src/main/AndroidManifest.xml
- app/src/main/java/com/jarvis/MainActivity.kt
- app/src/main/java/com/jarvis/JarvisService.kt
</context>

<tasks>

<task type="auto">
  <name>Add Calling & Contact Permissions</name>
  <files>
    - app/src/main/AndroidManifest.xml
    - app/src/main/java/com/jarvis/MainActivity.kt
  </files>
  <action>
    Add `<uses-permission android:name="android.permission.CALL_PHONE" />` and `<uses-permission android:name="android.permission.READ_CONTACTS" />` to the Manifest.
    In `MainActivity.kt`, add `Manifest.permission.CALL_PHONE` and `Manifest.permission.READ_CONTACTS` to the `permissionsToRequest` list so the user is prompted on launch.
  </action>
  <verify>cat app/src/main/java/com/jarvis/MainActivity.kt | Select-String "CALL_PHONE"</verify>
  <done>Permissions are requested in Manifest and runtime activity block.</done>
</task>

<task type="auto">
  <name>Implement Contact Lookup & Call Action</name>
  <files>app/src/main/java/com/jarvis/JarvisService.kt</files>
  <action>
    Inside the `MAKE_CALL` block of `executeIntent()`, extract `targetName` from the `parameters` JSONObject.
    Write a private helper: `fun getPhoneNumber(name: String): String?`. Use a `ContentResolver` to query `ContactsContract.CommonDataKinds.Phone.CONTENT_URI` for a match against the `DISPLAY_NAME` like `%name%`. Take the first match.
    If a number is found, construct `Intent(Intent.ACTION_CALL)` with `Uri.parse("tel:$number")`.
    Set `Intent.FLAG_ACTIVITY_NEW_TASK`.
    Execute `startActivity(callIntent)`.
    Handle cases where the contact isn't found and have TTS optionally apologize.
  </action>
  <verify>Review `JarvisService.kt` `MAKE_CALL` block to ensure `ContactsContract` query is valid.</verify>
  <done>Jarvis can look up a name and dial the phone automatically.</done>
</task>

</tasks>

<verification>
After all tasks, verify:
- [ ] Ensure the app is granted Contacts and Phone permissions.
- [ ] Ensure a dummy contact exists on the test device with a valid phone number.
- [ ] Build and run the app.
- [ ] Say "Jarvis".
- [ ] Say "Call [Dummy Contact Name]".
- [ ] Verify you hear "Calling [Dummy Contact Name]" via TTS.
- [ ] Verify the phone dialer opens successfully and initiates the call.
</verification>

<success_criteria>
- [ ] All tasks verified
- [ ] Must-haves confirmed
</success_criteria>
