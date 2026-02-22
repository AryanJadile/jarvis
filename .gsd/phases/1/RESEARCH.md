# Phase 1 Research: Offline Wakeword & Speech Recognition

## Objective
Determine the best offline wake-word detection engine and speech-to-text integration for a native Android application (Kotlin) that needs to run continuously in the background.

## Wakeword Engine Options

### 1. Picovoice Porcupine
- **Pros:** Highly accurate, very low power consumption, specifically designed for always-on wake word detection. Native Android SDK (Kotlin/Java). Free tier covers personal use (up to 3 users and limited monthly active users, which is fine for this project).
- **Cons:** Requires an access key from the Picovoice console. Requires an internet connection *once* every 30 days to validate the key, but detection is 100% offline.
- **Decision:** **Selected.** It is the industry standard for this exact use case on mobile.

### 2. CMU Sphinx (PocketSphinx)
- **Pros:** Completely free and open-source. 100% offline.
- **Cons:** Extremely outdated. Hard to integrate with modern Android (Gradle, newer NDKs). Poor accuracy compared to modern deep-learning models like Porcupine. Very high false positive rate.
- **Decision:** Rejected.

### 3. Google Assistant / Android Built-in
- **Pros:** Already running on the phone.
- **Cons:** We cannot intercept the "Hey Google" wake word to launch our own background service seamlessly without user interaction or complex Accessibility Service hacks.
- **Decision:** Rejected. We need a custom wake word ("Hey Jarvis").

## Speech-to-Text (STT) Options

Once the wake word is detected, we need to convert the subsequent spoken command to text.

### 1. Android `SpeechRecognizer` (Built-in)
- **Pros:** Free, built right into the Android OS. Supports many languages. Handles the audio recording buffer automatically.
- **Cons:** Subject to Android's aggressive background microphone restrictions. Usually requires a visual UI prompt (the Google microphone dialog) unless implemented very carefully as a continuous recognition service (which is prone to failure). Accuracy depends on the device's Google App version.
- **Decision:** **Selected for v1.** It is the most native way to do it.

### 2. Picovoice Cheetah / Leopard
- **Pros:** 100% offline, incredibly fast and accurate.
- **Cons:** Picovoice free tier limits might be hit if STT is used heavily along with Porcupine.
- **Decision:** Fallback option if Android's built-in `SpeechRecognizer` struggles in the background.

## Android Background Execution Restrictions

Android 10+ has significant restrictions on background microphone access. A background service *cannot* access the microphone unless it is a **Foreground Service** with the `cameraAndMicrophone` type, and it must show a persistent notification.

### Implementation Strategy
1.  **Foreground Service:** We must create a Foreground Service that holds a partial wake lock and shows a persistent "Jarvis is listening" notification.
2.  **Porcupine Integration:** Run Porcupine inside this Foreground Service.
3.  **Handoff to STT:** When Porcupine fires the wake word callback, immediately pause Porcupine and trigger the Android `SpeechRecognizer` to capture the intent.
4.  **Audio Focus:** We'll need to manage Android AudioManager focus to ensure the recording doesn't get hijacked by other apps (like music players).

## Required UI/Setup Elements
Since this is a native Android project from scratch, Phase 1 must also include:
- Generating the base Android project structure (`build.gradle.kts`, `AndroidManifest.xml`, `MainActivity.kt`).
- Requesting `RECORD_AUDIO` permission from the user on first launch.
- A simple settings UI to input the Picovoice Access Key.
