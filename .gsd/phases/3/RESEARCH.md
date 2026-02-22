# Phase 3 Research: Android Intents & Execution

## Objective
Take the parsed JSON intent from Gemini and execute it natively on the Android device. This phase covers Alarms, Phone Calls, and Local Notes (reserving WhatsApp for Phase 4).

## 1. Intent Engine Architecture
We will create an `IntentExecutor` or handle it directly within `JarvisService` (since the service possesses the Android `Context` required to launch Activities and Intents). 
Because `JarvisService` runs in the background, all `startActivity` calls must include `Intent.FLAG_ACTIVITY_NEW_TASK`.

### JSON Schema Recap
Gemini returns:
```json
{
  "intent": "SET_ALARM | MAKE_CALL | TAKE_NOTE | UNKNOWN",
  "parameters": { ... },
  "spokenResponse": "..."
}
```

## 2. Setting Alarms (`SET_ALARM`)
- **Action:** `android.provider.AlarmClock.ACTION_SET_ALARM`
- **Parameters:** `EXTRA_HOUR`, `EXTRA_MINUTES`, `EXTRA_SKIP_UI` (to set it silently in the background).
- **Permissions Required:** `<uses-permission android:name="com.android.alarm.permission.SET_ALARM" />`
- **Execution:** `startActivity(intent)`

## 3. Making Phone Calls (`MAKE_CALL`)
- **Action:** `android.intent.action.CALL`
- **Parameters:** `Uri.parse("tel:$number")`
- **Permissions Required:** 
  - `<uses-permission android:name="android.permission.CALL_PHONE" />` (Runtime)
  - `<uses-permission android:name="android.permission.READ_CONTACTS" />` (Runtime - to look up the number by name)
- **Execution:**
  1. Query `ContactsContract.CommonDataKinds.Phone.CONTENT_URI` filtering by `DISPLAY_NAME` using the target name parsed by Gemini.
  2. Extract the first matching phone number.
  3. Fire the `ACTION_CALL` intent.

## 4. Taking Notes (`TAKE_NOTE`)
- **Action:** Local File I/O
- **Parameters:** Append the `noteContent` string to a local file in the app's internal storage (`notes.txt`).
- **Permissions Required:** None. Internal storage (`context.filesDir`) requires no permissions.
- **Execution:** Standard Kotlin `File(filesDir, "notes.txt").appendText("...")`
