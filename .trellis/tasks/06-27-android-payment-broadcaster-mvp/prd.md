# Android Payment Broadcaster MVP

## Goal

Build the first usable version of an Android payment notification broadcaster:
the Android app listens to local system notifications from configured payment
apps, extracts payment amounts locally, speaks the result through Android TTS,
stores local payment records, and gates the feature behind a license-card
activation system.

## Requirements

- Replace the template Android screen with a Compose app for payment broadcast.
- Keep notification contents local to the device. The server receives only
  license, device, authorization, and app-version data.
- Support three preset notification sources:
  - Bank payments: default package `com.bgeneral`.
  - Yappy payments: default package `com.yappy`.
  - Email payments: default package `com.android.email`.
- Let users enable or disable each source.
- Extract text from Android notification extras and match enabled rules by
  package name and keywords.
- Parse amounts in common forms such as `$1.25`, `+$1.25`, `USD 1.25`, and
  `B/. 1.25`.
- Deduplicate notifications before saving or speaking.
- Store payment records locally and show recent totals.
- Use Android `TextToSpeech` for Chinese and Spanish broadcast templates.
- Provide system guidance for notification listener permission, battery
  optimization, and TTS settings.
- Provide a license activation screen with device ID, license key input,
  authorization status, expiry, and remaining days.
- Add a simple backend license system with admin login, card generation, card
  listing, disable, unbind, activation, check, and heartbeat APIs.

## Constraints

- Continue using the existing Android project in `D:\KAI\DingPay`.
- Keep package name `com.hege.dingpay` unless explicitly changed later.
- Use Kotlin and Jetpack Compose for Android.
- Android support starts at `minSdk 24`.
- This is not a bank or Yappy official API integration. Correct behavior
  depends on third-party apps posting notifications and on Android system
  permissions/background limits.
- Do not upload payment notification text or amounts to the backend.

## Acceptance Criteria

- [ ] Opening the app shows the payment broadcaster dashboard instead of the
      template greeting.
- [ ] Users can open notification listener settings and the app can display
      whether listener permission is enabled.
- [ ] Enabled rules receive matching notifications through
      `NotificationListenerService`.
- [ ] Matching notifications create one local payment record and trigger TTS.
- [ ] Duplicate notifications do not create duplicate records or repeated TTS.
- [ ] Disabling a source prevents that source from being spoken.
- [ ] Payment records screen shows count, total amount, list rows, and clear.
- [ ] Settings screen shows language, authorization, listener, battery, TTS,
      and about sections.
- [ ] License activation calls backend activation/check endpoints and blocks
      broadcast when unlicensed or expired.
- [ ] Backend can create cards, list cards, disable cards, unbind a device, and
      validate app activation/check requests.
- [ ] Build or verification commands are run and results recorded.
