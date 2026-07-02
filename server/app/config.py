from functools import lru_cache
from os import getenv


class Settings:
    def __init__(self) -> None:
        self.database_url = getenv("DINGPAY_DATABASE_URL", "sqlite:///./dingpay_license.db")
        self.secret_key = getenv("DINGPAY_SECRET_KEY", "change-me-in-production")
        self.admin_username = getenv("DINGPAY_ADMIN_USERNAME", "admin")
        self.admin_password = getenv("DINGPAY_ADMIN_PASSWORD", "admin123")
        self.offline_grace_days = int(getenv("DINGPAY_OFFLINE_GRACE_DAYS", "1"))


@lru_cache
def get_settings() -> Settings:
    return Settings()
