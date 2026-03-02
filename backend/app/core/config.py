import logging
from functools import lru_cache
from pathlib import Path
from typing import Annotated

from dotenv import load_dotenv

logger = logging.getLogger(__name__)
from pydantic import AnyHttpUrl, BeforeValidator, Field, PostgresDsn, RedisDsn, ValidationError, model_validator
from pydantic_settings import BaseSettings, SettingsConfigDict


def _ensure_url_scheme(v: str) -> str:
    """Prepend https:// if value looks like a hostname without scheme."""
    if isinstance(v, str) and v.strip() and not v.strip().startswith(("http://", "https://")):
        return f"https://{v.strip()}"
    return v

APP_DIR = Path(__file__).resolve().parents[1]
BACKEND_DIR = APP_DIR.parent
ROOT_DIR = BACKEND_DIR.parent

for env_path in (BACKEND_DIR / ".env", ROOT_DIR / ".env"):
    if env_path.exists():
        load_dotenv(env_path, override=False)


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    environment: str = Field(default="development", alias="ENVIRONMENT")
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")
    workers: int = Field(default=2, alias="WORKERS")
    supabase_url: AnyHttpUrl | None = Field(default=None, alias="SUPABASE_URL")
    supabase_anon_key: str | None = Field(default=None, alias="SUPABASE_ANON_KEY")
    supabase_service_role_key: str | None = Field(default=None, alias="SUPABASE_SERVICE_ROLE_KEY")
    supabase_jwt_secret: str | None = Field(default=None, alias="SUPABASE_JWT_SECRET")
    firebase_project_id: str | None = Field(default=None, alias="FIREBASE_PROJECT_ID")
    frontend_origin: Annotated[AnyHttpUrl, BeforeValidator(_ensure_url_scheme)] = Field(
        default="http://localhost:5173", alias="FRONTEND_ORIGIN"
    )
    postgres_dsn: PostgresDsn = Field(alias="POSTGRES_URL")
    redis_url: RedisDsn = Field(default="redis://localhost:6379/0", alias="REDIS_URL")
    celery_broker_url: RedisDsn | None = Field(default=None, alias="CELERY_BROKER_URL")
    gmail_client_id: str | None = Field(default=None, alias="GMAIL_CLIENT_ID")
    gmail_client_secret: str | None = Field(default=None, alias="GMAIL_CLIENT_SECRET")
    gmail_redirect_uri: AnyHttpUrl | None = Field(default=None, alias="GMAIL_REDIRECT_URI")
    gmail_token_uri: AnyHttpUrl = Field(
        default="https://oauth2.googleapis.com/token", alias="GMAIL_TOKEN_URI"
    )
    # Pub/Sub for Gmail real-time notifications
    gcp_project_id: str | None = Field(default=None, alias="GCP_PROJECT_ID")
    gmail_pubsub_topic: str = Field(default="gmail-events", alias="GMAIL_PUBSUB_TOPIC")
    google_application_credentials: str | None = Field(
        default=None, alias="GOOGLE_APPLICATION_CREDENTIALS"
    )
    google_credentials_json: str | None = Field(
        default=None, alias="GOOGLE_CREDENTIALS_JSON"
    )
    
    # Base directory for models and data
    base_dir: Path = Field(default=BACKEND_DIR, alias="BASE_DIR")

    @model_validator(mode="after")
    def ensure_auth_configured(self) -> "Settings":
        if not self.auth_is_configured:
            raise ValueError(
                "Firebase auth is required. Set FIREBASE_PROJECT_ID and GOOGLE_CREDENTIALS_JSON."
            )
        return self

    @property
    def auth_is_configured(self) -> bool:
        """True if Firebase auth is configured (API uses Firebase ID tokens only)."""
        return bool(self.firebase_project_id)

    @property
    def gmail_is_configured(self) -> bool:
        """True if Gmail OAuth credentials are set (enables Gmail integration)."""
        return bool(
            self.gmail_client_id and self.gmail_client_secret and self.gmail_redirect_uri
        )

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )


@lru_cache
def get_settings() -> Settings:
    """Return cached settings instance."""
    try:
        settings = Settings()
        
        # Handle GOOGLE_CREDENTIALS_JSON -> GOOGLE_APPLICATION_CREDENTIALS file (for Firebase)
        if settings.google_credentials_json:
            import json
            import os
            import tempfile

            try:
                # Minify: strip whitespace so multiline paste still parses
                raw = settings.google_credentials_json.strip()
                creds_dict = json.loads(raw)
                project = creds_dict.get("project_id", "?")
                with tempfile.NamedTemporaryFile(mode="w", suffix=".json", delete=False) as tmp:
                    json.dump(creds_dict, tmp)
                    tmp_path = tmp.name
                settings.google_application_credentials = tmp_path
                os.environ["GOOGLE_APPLICATION_CREDENTIALS"] = tmp_path
                logger.info(
                    "Firebase credentials loaded from GOOGLE_CREDENTIALS_JSON (project=%s)",
                    project,
                )
            except Exception as e:
                logger.warning(
                    "Failed to process GOOGLE_CREDENTIALS_JSON: %s. "
                    "Firebase auth may use GOOGLE_APPLICATION_CREDENTIALS path instead.",
                    e,
                )

        return settings
    except ValidationError as exc:  # pragma: no cover - startup validation helper
        missing = ", ".join(err["loc"][0] for err in exc.errors())
        raise RuntimeError(
            "Missing required environment variables. "
            f"Please set: {missing}. Check backend/.env."
        ) from exc

