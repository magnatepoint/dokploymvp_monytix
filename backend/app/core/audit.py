"""Audit logging for sensitive actions. Required for fintech compliance."""

import json
import logging
from datetime import datetime, timezone
from enum import Enum
from typing import Any

from asyncpg import Pool

logger = logging.getLogger("audit")


class AuditAction(str, Enum):
    """Sensitive actions that must be logged."""

    DATA_EXPORT = "data_export"
    ACCOUNT_DELETE = "account_delete"
    ACCOUNT_DEACTIVATE = "account_deactivate"
    CONSENT_WITHDRAWAL = "consent_withdrawal"
    LOGIN_NEW_DEVICE = "login_new_device"
    SESSION_REVOKE = "session_revoke"
    ADMIN_ACTION = "admin_action"


def _audit_payload(
    action: AuditAction,
    user_id: str,
    details: dict[str, Any] | None = None,
    ip_address: str | None = None,
) -> dict[str, Any]:
    return {
        "timestamp": datetime.now(timezone.utc).isoformat(),
        "action": action.value,
        "user_id": user_id,
        "details": details or {},
        "ip_address": ip_address,
    }


def log_audit(
    action: AuditAction,
    user_id: str,
    details: dict[str, Any] | None = None,
    ip_address: str | None = None,
) -> None:
    """Log a sensitive action to the application logger (when DB is not available)."""
    payload = _audit_payload(action, user_id, details, ip_address)
    logger.info("AUDIT: %s", payload)


async def persist_audit(
    pool: Pool,
    action: AuditAction,
    user_id: str,
    details: dict[str, Any] | None = None,
    ip_address: str | None = None,
) -> None:
    """Persist audit event to spendsense.audit_log and log."""
    payload = _audit_payload(action, user_id, details, ip_address)
    try:
        async with pool.acquire() as conn:
            await conn.execute(
                """
                INSERT INTO spendsense.audit_log (action, user_id, details, ip_address)
                VALUES ($1, $2, $3::jsonb, $4)
                """,
                action.value,
                user_id,
                json.dumps(details or {}),
                ip_address,
            )
    except Exception as exc:
        logger.error("Failed to persist audit: %s", exc, extra={"payload": payload})
        raise
    logger.info("AUDIT: %s", payload)
