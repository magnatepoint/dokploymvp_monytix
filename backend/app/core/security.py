from datetime import datetime, timezone

from app.core.config import get_settings
from app.auth.models import AuthenticatedUser


class AuthTokenError(RuntimeError):
    """Raised when a JWT fails verification."""


def decode_firebase_token(token: str) -> AuthenticatedUser:
    """Validate a Firebase ID token and return the parsed claims."""

    settings = get_settings()
    if not settings.firebase_project_id:
        raise AuthTokenError("Firebase is not configured")

    try:
        import firebase_admin
        from firebase_admin import auth as firebase_auth

        # Lazy init Firebase Admin (uses GOOGLE_APPLICATION_CREDENTIALS)
        if not firebase_admin._apps:
            firebase_admin.initialize_app()

        decoded = firebase_auth.verify_id_token(token, check_revoked=True)
    except Exception as exc:
        raise AuthTokenError(f"Invalid Firebase token: {exc!s}") from exc

    # Firebase token: sub=uid, email, email_verified, etc.
    user_id = decoded.get("sub") or decoded.get("user_id")
    if not user_id:
        raise AuthTokenError("Firebase token missing subject")

    email = decoded.get("email")
    exp = decoded.get("exp", 0)
    _ensure_not_expired(exp)

    return AuthenticatedUser(
        user_id=user_id,
        email=email,
        role=None,
        exp=exp,
        aud=decoded.get("aud"),
        iss=decoded.get("iss"),
    )


def decode_auth_token(token: str) -> AuthenticatedUser:
    """Validate Firebase ID token only. No Supabase fallback."""
    settings = get_settings()
    if not settings.firebase_project_id:
        raise AuthTokenError("Firebase is not configured (set FIREBASE_PROJECT_ID and GOOGLE_CREDENTIALS_JSON)")
    return decode_firebase_token(token)


def _ensure_not_expired(exp_timestamp: int) -> None:
    expires_at = datetime.fromtimestamp(exp_timestamp, tz=timezone.utc)
    if expires_at <= datetime.now(tz=timezone.utc):
        raise AuthTokenError("Token has expired")


# Backward compatibility
SupabaseJWTError = AuthTokenError
