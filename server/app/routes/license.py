from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session

from ..database import get_db
from ..schemas import LicenseActivateRequest, LicenseCheckRequest, LicenseHeartbeatRequest, LicenseResponse
from ..services.license_service import LicenseService

router = APIRouter(prefix="/api/license", tags=["license"])


@router.post("/activate", response_model=LicenseResponse)
def activate(request: LicenseActivateRequest, db: Session = Depends(get_db)) -> LicenseResponse:
    return LicenseService(db).activate(request.licenseKey, request.deviceId, request.appVersion)


@router.post("/check", response_model=LicenseResponse)
def check(request: LicenseCheckRequest, db: Session = Depends(get_db)) -> LicenseResponse:
    return LicenseService(db).check(
        request.licenseKey,
        request.deviceId,
        request.signedToken,
        request.appVersion,
    )


@router.post("/heartbeat", response_model=LicenseResponse)
def heartbeat(request: LicenseHeartbeatRequest, db: Session = Depends(get_db)) -> LicenseResponse:
    return LicenseService(db).heartbeat(request.deviceId, request.signedToken, request.appVersion)
