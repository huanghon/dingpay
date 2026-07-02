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

## Privacy Boundary

The Android app must not upload notification title, body, amount, payer, or
payment record data. This backend only accepts license key, device ID, app
version, signed token, and authorization metadata.
