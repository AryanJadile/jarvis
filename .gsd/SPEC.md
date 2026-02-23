# SPEC.md — Project Specification

> **Status**: `FINALIZED`

## Vision
A lightweight, voice-activated native Android application named "Jarvis" that acts as a personal AI assistant. It listens in the background for a wake word and uses the Gemini LLM to interpret natural language commands, executing system-level actions and providing voice feedback.

## Goals
1. Provide a hands-free, voice-first interface activated by a custom wake word (e.g., "Hey Jarvis").
2. Execute core mobile tasks: sending WhatsApp messages, setting alarms, making phone calls, and taking notes.
3. Understand natural language variations of commands using the Gemini LLM.
4. Provide conversational audio feedback using Text-to-Speech (TTS).

## Non-Goals (Out of Scope)
- iOS support (Android only).
- Complex multi-step internet research or web scraping in v1.
- Smart home device control in v1.
- A complex visual UI (the focus is on the background voice service and necessary permissions UI).

## Users
The primary user is the device owner who wants hands-free control over their phone for basic, frequent tasks, similar to the experience seen in the Iron Man movies.

## Constraints
- **Technical constraints**: Native Android development (Kotlin). Requires handling complex Android background service limitations and battery optimization exclusions to keep the wake-word listener active.
- **API constraints**: Relies on the Gemini API for natural language understanding. WhatsApp integration will likely require Android Accessibility Services or specific Intent handling, as WhatsApp does not have a public API for sending messages silently without user interaction.
- **Hardware constraints**: Continuous microphone access will impact battery life.

## Success Criteria
- [ ] App successfully runs in the background and detects the wake word reliably.
- [ ] User can say "Hey Jarvis, tell [Contact Name] I am running late on WhatsApp" and the message is sent.
- [ ] User can say "Hey Jarvis, set an alarm for 7 AM" and the system alarm is set.
- [ ] User can say "Hey Jarvis, call [Contact Name]" and the phone dialer initiates the call.
- [ ] User can say "Hey Jarvis, take a note that [Note Content]" and the note is saved locally.
- [ ] Jarvis acknowledges commands and reports success/failure using TTS.
