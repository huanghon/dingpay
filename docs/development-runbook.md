# DingPay Development Runbook

## Android

Build debug APK:

```powershell
cd D:\KAI\DingPay
.\gradlew.bat :app:assembleDebug
```

APK output:

```text
D:\KAI\DingPay\app\build\outputs\apk\debug\app-debug.apk
```

Local emulator backend URL:

```text
http://10.0.2.2:8000
```

Physical Android devices should use the PC/server LAN IP or a deployed HTTPS
domain instead.

## License Server

Run the local FastAPI backend:

```powershell
cd D:\KAI\DingPay
$env:DINGPAY_SECRET_KEY="change-me"
$env:DINGPAY_ADMIN_USERNAME="admin"
$env:DINGPAY_ADMIN_PASSWORD="admin123"
python -m uvicorn server.app.main:app --reload --host 0.0.0.0 --port 8000
```

Health check:

```powershell
Invoke-RestMethod http://127.0.0.1:8000/health
```

## Verification

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
python -m compileall server
python -m pytest server
```

## Current MVP Limits

- Notification parsing depends on real notification text from bank/Yappy/email
  apps.
- Background behavior still depends on Android notification listener access and
  vendor battery rules.
- HTTP is enabled for local debugging. Production should deploy HTTPS and turn
  cleartext off.
- The backend does not store payment notification title, body, payer, amount,
  or payment records.
