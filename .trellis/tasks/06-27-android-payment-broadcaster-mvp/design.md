# Design

## Android Architecture

The Android app will stay in one `:app` module and use a small feature-based
structure under `com.hege.dingpay`:

- `data`: local entities, repositories, settings, defaults, and parsers.
- `service`: Android services and platform managers such as notification
  listener and TTS.
- `ui`: Compose screens and reusable UI components.
- `license`: device ID, license API client, local license cache, and models.

For this first pass, dependency injection will be manual through an
`AppContainer` owned by a custom `Application` class. This avoids introducing a
DI framework before the domain stabilizes.

## Local Data Flow

```
Android notification
  -> PaymentNotificationListenerService
  -> NotificationTextExtractor
  -> PaymentRuleMatcher
  -> PaymentAmountParser
  -> PaymentRepository
  -> Room database / UI state
  -> TtsManager
```

Validation responsibilities:

- Service boundary: ignore notifications without text or without an enabled
  matching rule.
- Parser boundary: normalize amount and currency; fail closed if amount cannot
  be parsed.
- Repository boundary: deduplicate by notification key and raw hash.
- UI boundary: display stable domain models, not raw notification payloads.

## License Flow

```
User enters license key
  -> Android license repository
  -> Backend /api/license/activate
  -> Signed authorization payload saved in local settings
  -> Notification listener checks repository before broadcasting
```

The app should tolerate short offline windows using a cached expiry and grace
period, but the backend remains the authority for disabled/expired licenses.

## Backend Architecture

Add a `server/` directory for a small FastAPI license service:

- `app/main.py`: FastAPI app and router registration.
- `app/config.py`: environment-based configuration.
- `app/database.py`: SQLAlchemy engine/session setup.
- `app/models.py`: admin users, license cards, device activations, audit logs.
- `app/schemas.py`: request/response schemas.
- `app/security.py`: hashing, token signing, and password checks.
- `app/services/license_service.py`: license business rules.
- `app/routes/admin.py`: card management endpoints.
- `app/routes/license.py`: Android activation/check endpoints.

SQLite will be the default database for local delivery. PostgreSQL can be used
later by changing `DATABASE_URL`.

## Privacy Boundary

The backend must never receive notification title, text, amount, or payer data.
Android API payloads to the backend are limited to license key, device ID, app
version, signed token, and time metadata.

## Error Handling

Android UI should show user-facing status messages for permissions, TTS, and
license failures. Backend API errors use a consistent JSON shape through
FastAPI `HTTPException` details.

## Compatibility Notes

`NotificationListenerService` requires user-granted notification listener
access and a manifest service with `android.permission.BIND_NOTIFICATION_LISTENER_SERVICE`.
Android background restrictions can still stop or limit behavior; the app will
provide settings guidance instead of promising permanent background execution.
