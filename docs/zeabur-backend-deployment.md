# DingPay Backend Zeabur Deployment Runbook

## Goal

Deploy the DingPay FastAPI license backend to Zeabur China
(`https://zeabur.cn/`) and expose it over HTTPS.

## Current backend shape

- Backend directory: `server/`
- ASGI app: `app.main:app` when Zeabur root is `server/`
- Health check: `GET /health`
- Admin page: `GET /admin`
- API docs: `GET /docs`
- Start command: `uvicorn app.main:app --host 0.0.0.0 --port $PORT`
- Deterministic container deployment: `server/Dockerfile`

## Zeabur China manual setup steps

1. Push the repository to GitHub.
2. Open `https://zeabur.cn/` and confirm you are logged in to the intended
   China-region account.
3. Create a Zeabur project.
4. Add a PostgreSQL service in the same project for persistent
   license/admin data.
5. Add a service from the GitHub repository.
6. If Zeabur shows `Dockerfile is required for arbitrary Git sources`, keep
   the service root at the repository root and use the root `Dockerfile`.
   China-region arbitrary Git source deployment may not auto-detect
   `server/Dockerfile`.
7. If the GitHub integration supports root directory settings in your project,
   either repository root `Dockerfile` or `server/Dockerfile` works. For the
   simplest manual deployment, use the repository root `Dockerfile`.
8. Bind PostgreSQL to the backend service so Zeabur injects `DATABASE_URL`.
9. Add these backend environment variables:
   - `DINGPAY_SECRET_KEY`: long random production secret.
   - `DINGPAY_ADMIN_USERNAME`: initial admin username.
   - `DINGPAY_ADMIN_PASSWORD`: strong initial admin password.
   - `DINGPAY_OFFLINE_GRACE_DAYS`: optional, default `1`.
10. Deploy the service.
11. Verify:
    - `https://<domain>/health` returns `{"ok":true}`.
    - `https://<domain>/admin` opens the admin console.
    - Admin login succeeds with the configured username/password.
12. Configure the Android app backend URL to the Zeabur HTTPS domain.

## Global Zeabur setup reference

These steps are equivalent for `https://zeabur.com/`, but the browser session,
account, and generated tokens are separate from `https://zeabur.cn/`.

1. Push the repository to GitHub/GitLab.
2. Create a Zeabur project.
3. Add a PostgreSQL service in the same project for persistent license/admin data.
4. Add a service from this repository.
5. Set the service root directory to `server`.
6. Prefer the included `server/Dockerfile` for deterministic deployment, or confirm Zeabur detects Python dependencies from `server/requirements.txt` when using Nixpacks.
7. Bind PostgreSQL to the backend service so Zeabur injects `DATABASE_URL`.
8. Add these backend environment variables:
   - `DINGPAY_SECRET_KEY`: long random production secret.
   - `DINGPAY_ADMIN_USERNAME`: initial admin username.
   - `DINGPAY_ADMIN_PASSWORD`: strong initial admin password.
   - `DINGPAY_OFFLINE_GRACE_DAYS`: optional, default `1`.
9. Deploy the service.
10. Verify:
    - `https://<domain>/health` returns `{"ok":true}`.
    - `https://<domain>/admin` opens the admin console.
    - Admin login succeeds with the configured username/password.
11. Configure the Android app backend URL to the Zeabur HTTPS domain.

## Global account CLI re-authentication

Use this when the local machine was previously logged in to another Zeabur
Global account.

```powershell
npx zeabur@latest auth status --json
npx zeabur@latest auth logout
npx zeabur@latest auth login --token <new-account-token>
npx zeabur@latest auth status --json
```

Browser login can be used with `npx zeabur@latest auth login`, but the current
official CLI targets the Global endpoint (`api.zeabur.com`). It does not verify
a `zeabur.cn` browser session. For China-region deployments, verify the account
inside the `https://zeabur.cn/` dashboard and deploy from the web console.

## Notes and risks

- Do not rely on default secrets (`change-me-in-production`, `admin123`) in production.
- SQLite fallback is only for local testing or disposable deployments. Use Zeabur PostgreSQL for real customer data.
- Database tables are currently created by SQLAlchemy `metadata.create_all()` on startup. This is acceptable for the current MVP, but migrations should be added before complex schema changes.
- The backend only stores license and device authorization metadata; it must not receive payment notification content.

## Direct AI development prompt for future deployment work

```text
You are working in D:\KAI\DingPay. The DingPay backend must deploy to Zeabur from the server/ directory. Preserve the privacy boundary: do not add endpoints that upload payment notification title/body/amount/payer/order content. Keep the ASGI entrypoint app.main:app, expose GET /health, and use Zeabur's $PORT. Prefer Zeabur PostgreSQL via DATABASE_URL, while keeping DINGPAY_DATABASE_URL as an override for local/custom deployments. After changes, run server tests and verify uvicorn can import app.main:app from the server directory.
```
