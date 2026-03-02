from datetime import datetime
from typing import Any

from pydantic import BaseModel, ConfigDict, EmailStr, Field, SecretStr


class LoginRequest(BaseModel):
    """Credentials for Supabase password login."""

    email: EmailStr
    password: SecretStr


class LoginResponse(BaseModel):
    """Subset of Supabase login payload returned to the frontend."""

    access_token: str = Field(..., alias="access_token")
    refresh_token: str = Field(..., alias="refresh_token")
    token_type: str = Field(..., alias="token_type")
    expires_in: int = Field(..., alias="expires_in")
    expires_at: datetime | None = Field(default=None, alias="expires_at")
    user: dict[str, Any]


class AuthenticatedUser(BaseModel):
    """User claims extracted from a Firebase ID token (decoded payload may have 'user_id' or 'sub')."""

    model_config = ConfigDict(populate_by_name=True)

    user_id: str = Field(..., alias="sub")
    email: EmailStr | None = None
    role: str | None = None
    exp: int
    aud: str | None = None
    iss: str | None = None


class SessionResponse(BaseModel):
    """Payload returned after validating Firebase ID token. Keys must match clients (e.g. Android)."""

    user_id: str
    email: EmailStr | None = None
    role: str | None = None

