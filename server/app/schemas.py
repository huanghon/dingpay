from pydantic import BaseModel, Field


class AdminLoginRequest(BaseModel):
    username: str
    password: str


class AdminLoginResponse(BaseModel):
    token: str


class GenerateCardsRequest(BaseModel):
    count: int = Field(ge=1, le=500)
    durationDays: int = Field(ge=0, le=3650)


class GeneratedCardsResponse(BaseModel):
    licenseKeys: list[str]


class LicenseCardResponse(BaseModel):
    id: int
    keyPreview: str
    durationDays: int
    status: str
    createdAt: str
    activatedAt: str | None
    expiresAt: str | None
    disabledAt: str | None
    deviceId: str | None
    lastSeenAt: str | None


class LicenseActivateRequest(BaseModel):
    licenseKey: str
    deviceId: str
    appVersion: str = ""


class LicenseCheckRequest(BaseModel):
    licenseKey: str
    deviceId: str
    signedToken: str
    appVersion: str = ""


class LicenseHeartbeatRequest(BaseModel):
    deviceId: str
    signedToken: str
    appVersion: str = ""


class LicenseResponse(BaseModel):
    licenseKey: str
    status: str
    expiresAtMillis: int
    signedToken: str
    offlineGraceDays: int
