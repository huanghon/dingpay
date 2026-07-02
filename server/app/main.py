from contextlib import asynccontextmanager
from pathlib import Path

from fastapi import FastAPI
from fastapi.responses import FileResponse
from sqlalchemy import select

from .config import get_settings
from .database import SessionLocal, init_db
from .models import AdminUser
from .routes import admin, license
from .security import hash_password


@asynccontextmanager
async def lifespan(app: FastAPI):
    del app
    init_db()
    seed_admin()
    yield


app = FastAPI(title="DingPay License Server", version="0.1.0", lifespan=lifespan)
app.include_router(admin.router)
app.include_router(license.router)


@app.get("/admin", include_in_schema=False)
def admin_page() -> FileResponse:
    return FileResponse(Path(__file__).parent / "static" / "admin.html")


@app.get("/health")
def health() -> dict:
    return {"ok": True}


def seed_admin() -> None:
    settings = get_settings()
    db = SessionLocal()
    try:
        existing = db.execute(select(AdminUser).where(AdminUser.username == settings.admin_username)).scalar_one_or_none()
        if existing:
            return
        db.add(
            AdminUser(
                username=settings.admin_username,
                password_hash=hash_password(settings.admin_password),
            )
        )
        db.commit()
    finally:
        db.close()
