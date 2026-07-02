# Database Guidelines

> Database patterns and conventions for this project.

---

## Overview

<!--
Document your project's database conventions here.

Questions to answer:
- What ORM/query library do you use?
- How are migrations managed?
- What are the naming conventions for tables/columns?
- How do you handle transactions?
-->

(To be filled by the team)

---

## Query Patterns

<!-- How should queries be written? Batch operations? -->

(To be filled by the team)

---

## Migrations

<!-- How to create and run migrations -->

(To be filled by the team)

---

## Naming Conventions

<!-- Table names, column names, index names -->

(To be filled by the team)

---

## Common Mistakes

<!-- Database-related mistakes your team has made -->

(To be filled by the team)

---

## Scenario: Permanent License Cards

### 1. Scope / Trigger
- Trigger: Any backend or Android change that creates, lists, activates, checks,
  displays, exports, disables, unbinds, or deletes DingPay license cards.
- This is a cross-layer contract spanning admin API payloads, SQLAlchemy models,
  the admin web console, and Android license state.

### 2. Signatures
- Admin generate request: `POST /api/admin/cards/generate`
  with `durationDays: int`.
- Database field: `license_cards.duration_days: Integer`.
- License response: `expiresAtMillis: int`.

### 3. Contracts
- `durationDays > 0` means a time-limited card.
- `durationDays == 0` means a permanent card.
- Permanent cards keep `license_cards.expires_at = null` after activation.
- Permanent license API responses return `status = "active"` and
  `expiresAtMillis = 0`.
- Android treats `status == "active" && expiresAtMillis <= 0` as permanent and
  authorized.

### 4. Validation & Error Matrix
- `durationDays < 0` -> request validation error.
- `durationDays > 3650` -> request validation error.
- Permanent card disabled -> activation/check must fail like any disabled card.
- Permanent card bound to another device -> activation must fail with the normal
  binding conflict.

### 5. Good/Base/Bad Cases
- Good: Generate with `durationDays = 0`, activate once, list shows no
  expiration, Android shows permanent authorization.
- Base: Generate with `durationDays = 7`, activate once, expiration is now plus
  seven days.
- Bad: A permanent card receives `expires_at = activated_at`, immediately
  refreshes to `expired`, and Android blocks broadcasts.

### 6. Tests Required
- Backend integration test generates `durationDays = 0`.
- Assert activation returns `status == "active"` and `expiresAtMillis == 0`.
- Assert check returns `status == "active"` and `expiresAtMillis == 0`.
- Assert admin card list returns `durationDays == 0` and `expiresAt is null`.
- Android compile or unit coverage must prove permanent active state is accepted
  whenever license state handling changes.

### 7. Wrong vs Correct

#### Wrong
```python
card.expires_at = now + timedelta(days=card.duration_days)
```

For `duration_days = 0`, this expires immediately.

#### Correct
```python
card.expires_at = None if card.duration_days == 0 else now + timedelta(days=card.duration_days)
```

Keep permanent-card semantics at the service boundary and let API/UI layers
format the same `0`/`null` contract consistently.
