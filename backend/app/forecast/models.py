"""Pydantic models for Forecast API."""

from pydantic import BaseModel, Field


class ForecastRecommendation(BaseModel):
    """Single recommendation for the user."""

    title: str
    body: str


class ForecastResponse(BaseModel):
    """Cash flow projection and related metadata for the Financial Future tab."""

    projection_points: list[list[float]] = Field(
        ...,
        description="List of [day_index, normalized_value 0-1] for chart. Typically 14 points.",
    )
    confidence_label: str = Field(
        ...,
        description="e.g. Based on this month cash flow or Low confidence.",
    )
    risk_strip_label: str | None = Field(
        None,
        description="Short risk message.",
    )
    risk_strip_severity: str = Field(
        "neutral",
        description="One of: neutral, warning, danger.",
    )
    savings_opportunity: str | None = Field(
        None,
        description="e.g. You could save amount this month.",
    )
    recommendations: list[ForecastRecommendation] = Field(
        default_factory=list,
        description="0-3 recommendation items.",
    )
