from fastapi import APIRouter, Depends, HTTPException
from sqlalchemy import select
from sqlalchemy.orm import Session

from ..config import get_settings
from ..database import get_db
from ..models import AdminUser, LicenseCard
from ..schemas import (
    AdminLoginRequest,
    AdminLoginResponse,
    GenerateCardsRequest,
    GeneratedCardsResponse,
    LicenseCardResponse,
)
from ..security import create_admin_token, verify_password
from ..services.license_service import LicenseService
from .deps import require_admin

router = APIRouter(prefix="/api/admin", tags=["admin"])


@router.post("/login", response_model=AdminLoginResponse)
def login(request: AdminLoginRequest, db: Session = Depends(get_db)) -> AdminLoginResponse:
    user = db.execute(select(AdminUser).where(AdminUser.username == request.username)).scalar_one_or_none()
    if not user or not verify_password(request.password, user.password_hash):
        raise HTTPException(status_code=401, detail="用户名或密码错误")
    return AdminLoginResponse(token=create_admin_token(user.username))


@router.post("/cards/generate", response_model=GeneratedCardsResponse)
def generate_cards(
    request: GenerateCardsRequest,
    actor: str = Depends(require_admin),
    db: Session = Depends(get_db),
) -> GeneratedCardsResponse:
    keys = LicenseService(db).generate_cards(request.count, request.durationDays, actor)
    return GeneratedCardsResponse(licenseKeys=keys)


@router.get("/cards", response_model=list[LicenseCardResponse])
def list_cards(
    actor: str = Depends(require_admin),
    db: Session = Depends(get_db),
) -> list[LicenseCardResponse]:
    del actor
    cards = db.execute(select(LicenseCard).order_by(LicenseCard.created_at.desc())).scalars().all()
    return [_card_response(card) for card in cards]


@router.post("/cards/{card_id}/disable")
def disable_card(
    card_id: int,
    actor: str = Depends(require_admin),
    db: Session = Depends(get_db),
) -> dict:
    LicenseService(db).disable(card_id, actor)
    return {"ok": True}


@router.post("/cards/{card_id}/unbind")
def unbind_card(
    card_id: int,
    actor: str = Depends(require_admin),
    db: Session = Depends(get_db),
) -> dict:
    LicenseService(db).unbind(card_id, actor)
    return {"ok": True}


@router.delete("/cards/{card_id}")
def delete_card(
    card_id: int,
    actor: str = Depends(require_admin),
    db: Session = Depends(get_db),
) -> dict:
    LicenseService(db).delete(card_id, actor)
    return {"ok": True}


def _card_response(card: LicenseCard) -> LicenseCardResponse:
    activation = next((item for item in card.activations if item.revoked_at is None), None)
    return LicenseCardResponse(
        id=card.id,
        keyPreview=card.key_preview,
        durationDays=card.duration_days,
        status=card.status,
        createdAt=card.created_at.isoformat(),
        activatedAt=card.activated_at.isoformat() if card.activated_at else None,
        expiresAt=card.expires_at.isoformat() if card.expires_at else None,
        disabledAt=card.disabled_at.isoformat() if card.disabled_at else None,
        deviceId=activation.device_id if activation else None,
        lastSeenAt=activation.last_seen_at.isoformat() if activation else None,
    )
