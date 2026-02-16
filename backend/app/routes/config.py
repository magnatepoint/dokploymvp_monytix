"""Public app config endpoint for mobile clients (version check, feature flags)."""

import os

from fastapi import APIRouter
from pydantic import BaseModel, Field

router = APIRouter(tags=["config"])


class ConfigResponse(BaseModel):
    """App config for mobile clients."""

    min_version_code: int = Field(default=1, description="Minimum app version code required")
    app_store_url: str = Field(
        default="https://play.google.com/store/apps/details?id=com.example.monytix",
        description="Play Store URL for force update",
    )
    feature_flags: dict[str, bool] = Field(default_factory=dict, description="Feature flags")
    maintenance_mode: bool = Field(default=False, description="When true, app shows maintenance screen")


@router.get("/config", response_model=ConfigResponse, summary="Get app config")
async def get_config() -> ConfigResponse:
    """Return app config for mobile clients. No auth required."""
    min_version = int(os.environ.get("MIN_VERSION_CODE", "1"))
    app_store_url = os.environ.get(
        "APP_STORE_URL",
        "https://play.google.com/store/apps/details?id=com.example.monytix",
    )
    maintenance = os.environ.get("MAINTENANCE_MODE", "false").lower() in ("1", "true", "yes")
    return ConfigResponse(
        min_version_code=min_version,
        app_store_url=app_store_url,
        feature_flags={},
        maintenance_mode=maintenance,
    )
