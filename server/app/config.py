from functools import lru_cache
from os import getenv


def _database_url() -> str:
    # Zeabur managed databases usually expose DATABASE_URL. Keep the
    # project-specific variable as the first override for local/dev parity.
    value = getenv("DINGPAY_DATABASE_URL") or getenv("DATABASE_URL") or "sqlite:///./dingpay_license.db"
    # SQLAlchemy expects the normalized postgresql:// scheme.
    if value.startswith("postgres://"):
        value = "postgresql://" + value.removeprefix("postgres://")
    return value


class Settings:
    def __init__(self) -> None:
        self.database_url = _database_url()
        self.secret_key = getenv("DINGPAY_SECRET_KEY", "change-me-in-production")
        self.admin_username = getenv("DINGPAY_ADMIN_USERNAME", "admin")
        self.admin_password = getenv("DINGPAY_ADMIN_PASSWORD", "admin123")
        self.offline_grace_days = int(getenv("DINGPAY_OFFLINE_GRACE_DAYS", "1"))


@lru_cache
def get_settings() -> Settings:
    return Settings()
