import secrets
import string
from datetime import timedelta

from fastapi import HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..config import get_settings
from ..models import AuditLog, DeviceActivation, LicenseCard, now_utc
from ..security import hash_license_key, sign_payload, verify_signed_payload
from ..schemas import LicenseResponse


class LicenseService:
    def __init__(self, db: Session) -> None:
        self.db = db

    def generate_cards(self, count: int, duration_days: int, actor: str) -> list[str]:
        keys: list[str] = []
        for _ in range(count):
            license_key = self._new_license_key()
            keys.append(license_key)
            self.db.add(
                LicenseCard(
                    key_hash=hash_license_key(license_key),
                    key_preview=license_key[-8:],
                    duration_days=duration_days,
                    status="unused",
                )
            )
        self._audit(actor, "generate_cards", "", {"count": count, "durationDays": duration_days})
        self.db.commit()
        return keys

    def activate(self, license_key: str, device_id: str, app_version: str) -> LicenseResponse:
        card = self._get_card_by_key(license_key)
        self._ensure_card_usable(card)
        activation = self._active_activation(card)
        now = now_utc()
        if activation and activation.device_id != device_id:
            raise HTTPException(status_code=409, detail="卡密已绑定其他设备")
        if not activation:
            activation = DeviceActivation(
                license_id=card.id,
                device_id=device_id,
                app_version=app_version,
                first_seen_at=now,
                last_seen_at=now,
            )
            card.activated_at = now
            card.expires_at = None if card.duration_days == 0 else now + timedelta(days=card.duration_days)
            card.status = "active"
            self.db.add(activation)
        else:
            activation.last_seen_at = now
            activation.app_version = app_version
        self._refresh_expired(card)
        self._audit("device", "activate", str(card.id), {"deviceId": device_id, "appVersion": app_version})
        self.db.commit()
        return self._response(card, license_key, device_id)

    def check(self, license_key: str, device_id: str, signed_token: str, app_version: str) -> LicenseResponse:
        token_payload = verify_signed_payload(signed_token)
        if not token_payload or token_payload.get("deviceId") != device_id:
            raise HTTPException(status_code=401, detail="授权 token 无效")
        card = self._get_card_by_key(license_key)
        activation = self._active_activation(card)
        if not activation or activation.device_id != device_id:
            raise HTTPException(status_code=403, detail="设备未绑定")
        activation.last_seen_at = now_utc()
        activation.app_version = app_version
        self._refresh_expired(card)
        self.db.commit()
        return self._response(card, license_key, device_id)

    def heartbeat(self, device_id: str, signed_token: str, app_version: str) -> LicenseResponse:
        token_payload = verify_signed_payload(signed_token)
        if not token_payload or token_payload.get("deviceId") != device_id:
            raise HTTPException(status_code=401, detail="授权 token 无效")
        card_id = int(token_payload.get("licenseId", 0))
        card = self.db.get(LicenseCard, card_id)
        if not card:
            raise HTTPException(status_code=404, detail="卡密不存在")
        activation = self._active_activation(card)
        if not activation or activation.device_id != device_id:
            raise HTTPException(status_code=403, detail="设备未绑定")
        activation.last_seen_at = now_utc()
        activation.app_version = app_version
        self._refresh_expired(card)
        self.db.commit()
        return self._response(card, "", device_id)

    def disable(self, card_id: int, actor: str) -> None:
        card = self.db.get(LicenseCard, card_id)
        if not card:
            raise HTTPException(status_code=404, detail="卡密不存在")
        card.status = "disabled"
        card.disabled_at = now_utc()
        self._audit(actor, "disable_card", str(card.id), {})
        self.db.commit()

    def unbind(self, card_id: int, actor: str) -> None:
        card = self.db.get(LicenseCard, card_id)
        if not card:
            raise HTTPException(status_code=404, detail="卡密不存在")
        for activation in card.activations:
            if activation.revoked_at is None:
                activation.revoked_at = now_utc()
        if card.status != "disabled":
            card.status = "unused"
            card.activated_at = None
            card.expires_at = None
        self._audit(actor, "unbind_card", str(card.id), {})
        self.db.commit()

    def delete(self, card_id: int, actor: str) -> None:
        card = self.db.get(LicenseCard, card_id)
        if not card:
            raise HTTPException(status_code=404, detail="license card not found")
        self._audit(actor, "delete_card", str(card.id), {"keyPreview": card.key_preview})
        for activation in list(card.activations):
            self.db.delete(activation)
        self.db.delete(card)
        self.db.commit()

    def _get_card_by_key(self, license_key: str) -> LicenseCard:
        card = self.db.execute(
            select(LicenseCard).where(LicenseCard.key_hash == hash_license_key(license_key))
        ).scalar_one_or_none()
        if not card:
            raise HTTPException(status_code=404, detail="卡密不存在")
        return card

    def _ensure_card_usable(self, card: LicenseCard) -> None:
        self._refresh_expired(card)
        if card.status == "disabled":
            raise HTTPException(status_code=403, detail="卡密已禁用")
        if card.status == "expired":
            raise HTTPException(status_code=403, detail="卡密已过期")

    def _refresh_expired(self, card: LicenseCard) -> None:
        if card.expires_at and card.expires_at <= now_utc() and card.status != "disabled":
            card.status = "expired"

    def _active_activation(self, card: LicenseCard) -> DeviceActivation | None:
        return next((item for item in card.activations if item.revoked_at is None), None)

    def _response(self, card: LicenseCard, license_key: str, device_id: str) -> LicenseResponse:
        self._refresh_expired(card)
        expires_at_millis = int(card.expires_at.timestamp() * 1000) if card.expires_at else 0
        status = "active" if card.status == "active" else card.status
        token = ""
        if card.id and device_id:
            token = sign_payload(
                {
                    "licenseId": card.id,
                    "deviceId": device_id,
                    "status": status,
                    "expiresAtMillis": expires_at_millis,
                }
            )
        return LicenseResponse(
            licenseKey=license_key,
            status=status,
            expiresAtMillis=expires_at_millis,
            signedToken=token,
            offlineGraceDays=get_settings().offline_grace_days,
        )

    def _audit(self, actor: str, action: str, target_id: str, metadata: dict) -> None:
        self.db.add(AuditLog(actor=actor, action=action, target_id=target_id, metadata_json=str(metadata)))

    @staticmethod
    def _new_license_key() -> str:
        alphabet = string.ascii_uppercase + string.digits
        parts = ["".join(secrets.choice(alphabet) for _ in range(4)) for _ in range(4)]
        return "DP-" + "-".join(parts)
