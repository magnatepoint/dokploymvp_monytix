from fastapi import APIRouter, Depends, Request

from app.core.audit import AuditAction, persist_audit
from app.dependencies.database import get_db_pool

from .dependencies import get_current_user
from .models import AuthenticatedUser, LoginRequest, LoginResponse, SessionResponse
from .service import SupabaseAuthService

router = APIRouter(prefix="/auth", tags=["auth"])


@router.post("/login", response_model=LoginResponse, summary="Supabase password login (legacy)")
async def login(payload: LoginRequest) -> LoginResponse:
    """Authenticate via Supabase Auth and return the session tokens.
    Legacy endpoint for Supabase users. Firebase clients use ID tokens directly."""
    from app.core.config import get_settings
    settings = get_settings()
    if not settings.supabase_url or not settings.supabase_service_role_key:
        from fastapi import HTTPException
        raise HTTPException(status_code=501, detail="Supabase login is disabled. Use Firebase Auth.")
    service = SupabaseAuthService()
    return await service.sign_in_with_password(payload)


@router.get("/session", response_model=SessionResponse, summary="Validate Supabase session")
async def session(user: AuthenticatedUser = Depends(get_current_user)) -> SessionResponse:
    """Validate the provided Supabase JWT and return the associated user."""

    return SessionResponse(
        user_id=user.user_id,
        email=user.email,
        role=user.role,
    )


@router.post("/export-data", summary="Request data export (GDPR)")
async def export_data(
    request: Request,
    user: AuthenticatedUser = Depends(get_current_user),
    pool=Depends(get_db_pool),
):
    """Request export of user data. Persists to audit log."""
    client_host = request.client.host if request.client else None
    await persist_audit(
        pool,
        AuditAction.DATA_EXPORT,
        user.user_id,
        details={"email": user.email},
        ip_address=client_host,
    )
    return {"status": "requested", "message": "Data export will be processed. You will receive a download link via email."}

