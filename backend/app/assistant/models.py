"""Pydantic models for Assistant API."""

from pydantic import BaseModel, Field


class AskRequest(BaseModel):
    """Request body for Ask MONYTIX."""

    prompt: str = Field(..., min_length=1, description="User question or selected prompt text.")


class AskResponse(BaseModel):
    """Response with assistant answer (plain text; can extend to structured later)."""

    answer: str = Field(..., description="Assistant reply.")
