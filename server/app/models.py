from datetime import UTC, datetime

from sqlalchemy import DateTime, ForeignKey, Integer, String, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship

from .database import Base


def now_utc() -> datetime:
    return datetime.now(UTC).replace(tzinfo=None)


class AdminUser(Base):
    __tablename__ = "admin_users"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    username: Mapped[str] = mapped_column(String(80), unique=True, index=True)
    password_hash: Mapped[str] = mapped_column(String(255))
    created_at: Mapped[datetime] = mapped_column(DateTime, default=now_utc)


class LicenseCard(Base):
    __tablename__ = "license_cards"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    key_hash: Mapped[str] = mapped_column(String(64), unique=True, index=True)
    key_preview: Mapped[str] = mapped_column(String(16), index=True)
    duration_days: Mapped[int] = mapped_column(Integer)
    status: Mapped[str] = mapped_column(String(20), default="unused", index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime, default=now_utc)
    activated_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    expires_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)
    disabled_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)

    activations: Mapped[list["DeviceActivation"]] = relationship(back_populates="license_card")


class DeviceActivation(Base):
    __tablename__ = "device_activations"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    license_id: Mapped[int] = mapped_column(ForeignKey("license_cards.id"), index=True)
    device_id: Mapped[str] = mapped_column(String(255), index=True)
    app_version: Mapped[str] = mapped_column(String(40), default="")
    first_seen_at: Mapped[datetime] = mapped_column(DateTime, default=now_utc)
    last_seen_at: Mapped[datetime] = mapped_column(DateTime, default=now_utc)
    revoked_at: Mapped[datetime | None] = mapped_column(DateTime, nullable=True)

    license_card: Mapped[LicenseCard] = relationship(back_populates="activations")


class AuditLog(Base):
    __tablename__ = "audit_logs"

    id: Mapped[int] = mapped_column(Integer, primary_key=True, index=True)
    actor: Mapped[str] = mapped_column(String(100))
    action: Mapped[str] = mapped_column(String(100), index=True)
    target_id: Mapped[str] = mapped_column(String(100), default="")
    created_at: Mapped[datetime] = mapped_column(DateTime, default=now_utc)
    metadata_json: Mapped[str] = mapped_column(Text, default="{}")
