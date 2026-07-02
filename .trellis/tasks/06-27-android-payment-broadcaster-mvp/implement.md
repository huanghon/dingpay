# Implementation Plan

## Phase 1: Android Foundation

- [x] Add Gradle dependencies for Compose navigation, Room, DataStore,
      serialization/networking as needed.
- [x] Add `DingPayApplication` and an `AppContainer`.
- [x] Add settings models, default payment rules, and local repositories.
- [x] Replace the template `MainActivity` with app navigation and dashboard.

## Phase 2: Local Payment Features

- [x] Add Room entities/DAO/database for payment records.
- [x] Implement notification text extraction, matching, amount parsing, and
      deduplication.
- [x] Add `PaymentNotificationListenerService` and manifest declarations.
- [x] Add `TtsManager` with Chinese and Spanish templates.
- [x] Build dashboard, records, listener config, settings, license, and about UI.

## Phase 3: License Backend

- [x] Create `server/` FastAPI project.
- [x] Add SQLite database models and startup table creation for MVP.
- [x] Add admin login/card management endpoints.
- [x] Add Android activation/check/heartbeat endpoints.
- [x] Add README with local run commands and environment variables.

## Phase 4: Integration And Verification

- [x] Run Android Gradle build or explain environment blockers.
- [x] Run backend tests or import/startup checks.
- [x] Manually review privacy boundary: backend receives no notification
      contents or payment amounts.
- [x] Update docs with run instructions and known limitations.

## Validation Commands

- `.\gradlew.bat :app:assembleDebug`
- `python -m compileall server`
- `python -m pytest server` if tests are added

## Rollback Points

- Android dependency changes are isolated in `gradle/libs.versions.toml` and
  `app/build.gradle.kts`.
- Backend code is isolated under `server/`.
- If Room or backend dependencies block builds, keep UI/parser/service work
  independent and document the blocker.
