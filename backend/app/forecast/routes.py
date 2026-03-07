"""Forecast API routes."""

from fastapi import APIRouter, Depends

from app.auth.dependencies import get_current_user
from app.auth.models import AuthenticatedUser
from app.dependencies.database import get_db_pool
from asyncpg import Pool

from .service import ForecastService
from .models import ForecastResponse

router = APIRouter(prefix="/v1/forecast", tags=["forecast"])


def get_service(pool: Pool = Depends(get_db_pool)) -> ForecastService:
    return ForecastService(pool)


@router.get("", response_model=ForecastResponse, summary="Get 14-day cash flow projection")
async def get_forecast(
    user: AuthenticatedUser = Depends(get_current_user),
    service: ForecastService = Depends(get_service),
) -> ForecastResponse:
    """Return projection points, confidence label, risk strip, savings opportunity, and recommendations for the Financial Future tab."""
    return await service.get_forecast(user.user_id)
