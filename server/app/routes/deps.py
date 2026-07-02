from fastapi import Depends, Header, HTTPException
from sqlalchemy.orm import Session

from ..database import get_db
from ..security import verify_admin_token


def require_admin(
    authorization: str = Header(default=""),
    db: Session = Depends(get_db),
) -> str:
    if not authorization.startswith("Bearer "):
        raise HTTPException(status_code=401, detail="缺少管理员 token")
    username = verify_admin_token(authorization.removeprefix("Bearer ").strip())
    if not username:
        raise HTTPException(status_code=401, detail="管理员 token 无效")
    return username
