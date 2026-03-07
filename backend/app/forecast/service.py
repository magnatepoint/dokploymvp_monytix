"""Forecast (cash projection) service."""

import logging
from datetime import date, timedelta
from uuid import UUID

from asyncpg import Pool

from app.core.user_id import to_user_uuid

from .models import ForecastRecommendation, ForecastResponse

logger = logging.getLogger(__name__)


class ForecastService:
    """Build 14-day cash projection and recommendations from transaction data."""

    def __init__(self, pool: Pool) -> None:
        self._pool = pool

    async def get_forecast(self, user_id: str) -> ForecastResponse:
        """Return projection points (0–13 days), confidence, risk strip, savings, recommendations."""
        user_uuid = to_user_uuid(user_id)
        today = date.today()
        start_of_month = today.replace(day=1)
        # Current month income and expenses
        row = await self._pool.fetchrow(
            """
            SELECT
                COALESCE(SUM(CASE WHEN tf.direction = 'credit' THEN tf.amount ELSE 0 END), 0) AS income,
                COALESCE(SUM(CASE WHEN tf.direction = 'debit' THEN tf.amount ELSE 0 END), 0) AS expenses
            FROM spendsense.txn_fact tf
            WHERE tf.user_id = $1
              AND tf.txn_date >= $2
              AND tf.txn_date <= $3
            """,
            user_uuid,
            start_of_month,
            today,
        )
        income = float(row["income"] or 0)
        expenses = float(row["expenses"] or 0)
        surplus = income - expenses
        # Normalized projection: 14 points, value 0–1 (1 = healthy, 0 = low)
        # Simple model: linear trend from surplus. If surplus >= 0, stay high; else trend down.
        if income <= 0 and expenses <= 0:
            # No data: flat line at 0.5, low confidence
            points = [[float(i), 0.5] for i in range(14)]
            confidence_label = "Low confidence – add more data"
            risk_strip_label = "Upload statements to see your projection"
            risk_strip_severity = "neutral"
            savings_opportunity = None
            recommendations = [
                ForecastRecommendation(
                    title="Add your data",
                    body="Connect an account or upload a statement to get a 14-day cash flow projection.",
                ),
            ]
        else:
            # Scale so that surplus = 0 -> trend to ~0.3 by day 13; surplus > 0 -> stay ~0.7–1
            if income > 0:
                surplus_ratio = surplus / income
            else:
                surplus_ratio = -1.0
            points = []
            for i in range(14):
                # 0 = today, 13 = 13 days ahead. Trend down if negative surplus.
                trend = 1.0 - (i / 14) * (1.0 - surplus_ratio) * 0.7
                trend = max(0.1, min(1.0, trend))
                points.append([float(i), round(trend, 4)])
            if surplus_ratio >= 0:
                confidence_label = "Based on this month's cash flow"
                risk_strip_label = "On track"
                risk_strip_severity = "neutral"
                if surplus > 5000:
                    savings_opportunity = f"You could save ₹{int(surplus):,} this month"
                else:
                    savings_opportunity = None
                recommendations = [
                    ForecastRecommendation(
                        title="Keep monitoring",
                        body="Your cash flow looks positive. Check the Goals tab to put surplus toward targets.",
                    ),
                ]
            else:
                confidence_label = "Based on this month's cash flow"
                risk_strip_label = "Dip ahead – consider delaying non-essential spend until after payday"
                risk_strip_severity = "warning"
                savings_opportunity = None
                recommendations = [
                    ForecastRecommendation(
                        title="Trim discretionary spend",
                        body="Your spending is ahead of income this month. Focus on needs and defer wants where possible.",
                    ),
                ]
        return ForecastResponse(
            projection_points=points,
            confidence_label=confidence_label,
            risk_strip_label=risk_strip_label,
            risk_strip_severity=risk_strip_severity,
            savings_opportunity=savings_opportunity,
            recommendations=recommendations,
        )
