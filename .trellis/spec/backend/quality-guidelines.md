# Quality Guidelines

> Code quality standards for backend development.

---

## Overview

<!--
Document your project's quality standards here.

Questions to answer:
- What patterns are forbidden?
- What linting rules do you enforce?
- What are your testing requirements?
- What code review standards apply?
-->

(To be filled by the team)

---

## Forbidden Patterns

<!-- Patterns that should never be used and why -->

(To be filled by the team)

---

## Required Patterns

<!-- Patterns that must always be used -->

### Android Notification-Triggered TTS

**Scope / Trigger**: Any code path that starts `TextToSpeech` from
`NotificationListenerService`, a background coroutine, or another service
callback.

**Contract**:
- Route `TextToSpeech` initialization and `speak(...)` calls through the main
  looper.
- Request transient audio focus before speaking and release it from the
  utterance completion/error callback.
- Return a structured result from the TTS boundary, at minimum:
  `accepted: Boolean`, `queued: Boolean`, and a human-readable diagnostic
  message.
- Persist notification diagnostics after the TTS request result is known. Do
  not mark a payment as "broadcast" before the TTS boundary accepts the
  request.

**Validation & Error Matrix**:
- TTS engine init failed -> diagnostic stage `tts_failed`.
- TTS still initializing -> diagnostic stage `tts_queued`.
- Missing selected language -> try a configured fallback language, then report
  `tts_failed` if no usable voice exists.
- `TextToSpeech.speak(...)` returns non-success -> diagnostic stage
  `tts_failed`.

**Good/Base/Bad Cases**:
- Good: Matching notification saves a record, TTS accepts the request, and the
  diagnostic says broadcast or queued.
- Base: TTS is still initializing, so the request is queued and flushed after
  init.
- Bad: UI shows "broadcast" even though `speak(...)` was never called or
  returned an error.

---

## Testing Requirements

<!-- What level of testing is expected -->

- Run `:app:compileDebugKotlin` after Android service/TTS changes.
- Run `:app:testDebugUnitTest` when parser, matching, diagnostic, or pure Kotlin
  behavior changes.
- Run `:app:lintDebug` before reporting Android UI/service work as complete.

---

## Code Review Checklist

<!-- What reviewers should check -->

- For notification-to-TTS changes, verify the flow is:
  notification extraction -> rule match -> amount parse -> dedupe/save -> TTS
  request -> diagnostic persistence.
- Confirm diagnostics distinguish rule mismatch, no amount, duplicate, queued
  TTS, accepted broadcast, and TTS failure.
