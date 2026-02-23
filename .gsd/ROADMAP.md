# ROADMAP.md

> **Current Phase**: Not started
> **Milestone**: v1.0

## Must-Haves (from SPEC)
- [x] Offline wake-word detection (e.g., "Hey Jarvis")
- [x] Voice-to-text recognition via Android SpeechRecognizer
- [x] Gemini API integration for intent parsing
- [x] Execution of 4 core intents: WhatsApp, Alarms, Calls, Notes
- [x] Text-to-Speech (TTS) audio feedback

## Phases

### Phase 1: Foundation & Voice Service
**Status**: ✅ Complete
**Objective**: Build the core Android foreground service that runs in the background continuously listening for the wake word using an offline engine (like Porcupine). Once triggered, it activates the system SpeechRecognizer to capture the user's command as text.
**Requirements**: REQ-01, REQ-02, REQ-04

### Phase 2: LLM Brain Integration
**Status**: ✅ Complete
**Objective**: Connect the captured text to the Gemini API. Implement prompt engineering to ensure Gemini returns a structured JSON object detailing the exact action, target, and payload needed to execute the command. Integrate TTS to speak back the parsed intent.
**Requirements**: REQ-03, REQ-09

### Phase 3: Android Intents & Execution
**Status**: ✅ Complete
**Objective**: Build the execution engine that takes the structured JSON from Gemini and fires the necessary Android Intents. Implement creating alarms, making phone calls (fetching contact numbers), and saving notes locally.
**Requirements**: REQ-05, REQ-06, REQ-08

### Phase 4: WhatsApp Integration & Polish
**Status**: ✅ Complete
**Objective**: Implement the most complex intent: sending a WhatsApp message. Depending on security constraints, this may involve deep linking or Accessibility Services. Finalize the UI for granting necessary system permissions and keeping the background service alive.
**Requirements**: REQ-07, REQ-02
