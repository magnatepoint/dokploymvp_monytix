from datetime import datetime, timezone

import jwt
from jwt import InvalidTokenError

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


def decode_supabase_jwt(token: str) -> AuthenticatedUser:
    """Validate a Supabase JWT and return the parsed claims (legacy)."""

    settings = get_settings()
    if not settings.supabase_jwt_secret:
        raise AuthTokenError("Supabase is not configured")

    try:
        payload = jwt.decode(
            token,
            settings.supabase_jwt_secret,
            algorithms=["HS256"],
            audience="authenticated",
            leeway=600,  # 10 minutes leeway for clock skew
        )
    except InvalidTokenError as exc:
        raise AuthTokenError("Invalid Supabase JWT") from exc

    claims = AuthenticatedUser(**payload)
    _ensure_not_expired(claims.exp)
    return claims


def decode_auth_token(token: str) -> AuthenticatedUser:
    """Validate token: try Firebase first, then Supabase (legacy)."""
    settings = get_settings()
    errors: list[str] = []

    # Try Firebase first if configured
    if settings.firebase_project_id:
        try:
            return decode_firebase_token(token)
        except AuthTokenError as e:
            errors.append(f"Firebase: {e}")
        except Exception as e:
            errors.append(f"Firebase: {e!s}")

    # Fall back to Supabase if configured
    if settings.supabase_jwt_secret:
        try:
            return decode_supabase_jwt(token)
        except AuthTokenError as e:
            errors.append(f"Supabase: {e}")
        except Exception as e:
            errors.append(f"Supabase: {e!s}")

    # Surface the first provider's error so client/logs show why (e.g. "Firebase: Invalid Firebase token")
    if errors:
        raise AuthTokenError(errors[0] if len(errors) == 1 else f"{errors[0]} (then {errors[1]})")
    raise AuthTokenError("No auth provider configured (set FIREBASE_PROJECT_ID or SUPABASE_JWT_SECRET)")


def _ensure_not_expired(exp_timestamp: int) -> None:
    expires_at = datetime.fromtimestamp(exp_timestamp, tz=timezone.utc)
    if expires_at <= datetime.now(tz=timezone.utc):
        raise AuthTokenError("Token has expired")


# Backward compatibility
SupabaseJWTError = AuthTokenError
