import base64
import hashlib
import hmac
import json
import secrets
from datetime import UTC, datetime, timedelta

from .config import get_settings


def hash_license_key(license_key: str) -> str:
    return hashlib.sha256(license_key.strip().encode("utf-8")).hexdigest()


def hash_password(password: str, salt: str | None = None) -> str:
    salt = salt or secrets.token_hex(16)
    digest = hashlib.pbkdf2_hmac("sha256", password.encode("utf-8"), salt.encode("utf-8"), 120_000)
    return f"pbkdf2_sha256${salt}${digest.hex()}"


def verify_password(password: str, password_hash: str) -> bool:
    try:
        algorithm, salt, digest = password_hash.split("$", 2)
    except ValueError:
        return False
    if algorithm != "pbkdf2_sha256":
        return False
    return hmac.compare_digest(hash_password(password, salt).split("$", 2)[2], digest)


def sign_payload(payload: dict) -> str:
    settings = get_settings()
    body = json.dumps(payload, separators=(",", ":"), sort_keys=True).encode("utf-8")
    signature = hmac.new(settings.secret_key.encode("utf-8"), body, hashlib.sha256).digest()
    return ".".join(
        [
            base64.urlsafe_b64encode(body).decode("ascii").rstrip("="),
            base64.urlsafe_b64encode(signature).decode("ascii").rstrip("="),
        ]
    )


def verify_signed_payload(token: str) -> dict | None:
    try:
        body_b64, sig_b64 = token.split(".", 1)
        body = base64.urlsafe_b64decode(_pad(body_b64))
        expected = hmac.new(get_settings().secret_key.encode("utf-8"), body, hashlib.sha256).digest()
        actual = base64.urlsafe_b64decode(_pad(sig_b64))
    except Exception:
        return None
    if not hmac.compare_digest(expected, actual):
        return None
    return json.loads(body.decode("utf-8"))


def create_admin_token(username: str) -> str:
    expires_at = datetime.now(UTC) + timedelta(hours=12)
    return sign_payload({"sub": username, "kind": "admin", "exp": int(expires_at.timestamp())})


def verify_admin_token(token: str) -> str | None:
    payload = verify_signed_payload(token)
    if not payload or payload.get("kind") != "admin":
        return None
    if int(payload.get("exp", 0)) < int(datetime.now(UTC).timestamp()):
        return None
    return str(payload.get("sub", ""))


def _pad(value: str) -> bytes:
    return (value + "=" * (-len(value) % 4)).encode("ascii")
