"""Map Firebase UID (string) to a deterministic UUID for DB compatibility.

Goal, budgetpilot, and moneymoments tables use user_id UUID. Firebase UIDs are
opaque strings (e.g. r1Q3QF41FzeadgdzI7BybzuPWMZ2). We derive a stable UUID
so the same Firebase user always maps to the same UUID without schema changes.
"""

from uuid import UUID, uuid5

# Namespace for Firebase UID -> UUID mapping (arbitrary but fixed)
_FIREBASE_UID_NAMESPACE = UUID("6ba7b810-9dad-11d1-80b4-00c04fd430c8")


def firebase_uid_to_uuid(uid: str) -> UUID:
    """Return a deterministic UUID for a Firebase UID. Same UID always yields same UUID."""
    return uuid5(_FIREBASE_UID_NAMESPACE, f"firebase:{uid}")


def to_user_uuid(user_id: str | UUID) -> UUID:
    """Convert user_id to UUID: pass-through if already UUID; else parse or treat as Firebase UID."""
    if isinstance(user_id, UUID):
        return user_id
    try:
        return UUID(user_id)
    except (ValueError, TypeError):
        return firebase_uid_to_uuid(user_id)
