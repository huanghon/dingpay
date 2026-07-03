# DingPay License Server

FastAPI backend for DingPay license-card activation.

## Local Run

```powershell
cd D:\KAI\DingPay
$env:DINGPAY_SECRET_KEY="change-me"
$env:DINGPAY_ADMIN_USERNAME="admin"
$env:DINGPAY_ADMIN_PASSWORD="admin123"
python -m uvicorn server.app.main:app --reload --host 0.0.0.0 --port 8000
```

Android emulator default server URL: `http://10.0.2.2:8000`.

Admin web console: `http://127.0.0.1:8000/admin`.

API docs remain available at `http://127.0.0.1:8000/docs`.

## Zeabur Deployment

This backend is ready to run as a Zeabur service from the `server/` directory. A `Dockerfile` is included for deterministic Zeabur builds; the `Procfile` remains available for Python/Nixpacks-style deployments.

### Service settings

1. Push this repository to GitHub/GitLab.
2. In Zeabur, create a project and add a service from the repository.
3. Set the service root / root directory to `server`.
4. Prefer the included `Dockerfile` for deterministic deployment. If using Python/Nixpacks instead, the included `Procfile` starts the service with:

```bash
uvicorn app.main:app --host 0.0.0.0 --port $PORT
```

### Required environment variables

Set these in the Zeabur service environment:

| Variable | Required | Example | Notes |
| --- | --- | --- | --- |
| `DINGPAY_SECRET_KEY` | Yes | long-random-secret | Used to sign admin/license tokens. Change before production. |
| `DINGPAY_ADMIN_USERNAME` | Yes | admin | Initial admin account username. |
| `DINGPAY_ADMIN_PASSWORD` | Yes | strong-password | Initial admin account password. |
| `DATABASE_URL` | Recommended | Zeabur PostgreSQL binding | Use Zeabur PostgreSQL for persistent production data. |
| `DINGPAY_DATABASE_URL` | Optional | postgresql://... | Overrides `DATABASE_URL` when set. Useful for custom databases. |
| `DINGPAY_OFFLINE_GRACE_DAYS` | Optional | 1 | Offline license grace period. |

For production, bind a Zeabur PostgreSQL service so `DATABASE_URL` is injected. If no database URL is configured, the app falls back to local SQLite (`sqlite:///./dingpay_license.db`), which is suitable for local testing but not reliable for Zeabur production persistence unless a volume is explicitly configured.

### Smoke checks after deployment

Replace `<backend-domain>` with the Zeabur domain:

```bash
curl https://<backend-domain>/health
curl https://<backend-domain>/admin
```

Expected health response:

```json
{"ok":true}
```

Point the Android app server URL to `https://<backend-domain>` after the health check passes.

## Privacy Boundary

The Android app must not upload notification title, body, amount, payer, or
payment record data. This backend only accepts license key, device ID, app
version, signed token, and authorization metadata.