"""Hook to refresh budget state after transaction create/import."""

import logging
from datetime import date
from typing import Any
from uuid import UUID

import asyncpg

logger = logging.getLogger(__name__)

# Map txn_type to budget bucket (needs/wants/assets)
# debt, protection, tax -> needs; charity, fees -> wants; assets stays
_TXN_TO_BUCKET = {
    "needs": "needs",
    "debt": "needs",
    "protection": "needs",
    "tax": "needs",
    "wants": "wants",
    "charity": "wants",
    "fees": "wants",
    "assets": "assets",
    "business": "wants",  # include in wants for personal budget
}
# transfer debit: exclude from spend (money moving); transfer credit: income
# income: credit only


async def refresh_budget_aggregate(
    conn: asyncpg.Connection,
    user_id: str,
    month: date | None = None,
) -> dict[str, Any] | None:
    """
    Recompute and upsert budget_user_month_aggregate for user+month.
    Maps txn_type to needs/wants/assets (debt, protection, tax -> needs; etc).
    Returns the updated aggregate or None if no commit exists.
    """
    user_uuid = UUID(user_id)
    if month is None:
        month = date.today().replace(day=1)
    else:
        month = month.replace(day=1)

    # Build CASE for bucket mapping (needs: needs, debt, protection, tax; wants: wants, charity, fees, business)
    needs_cases = " + ".join(
        f"CASE WHEN txn_type = '{t}' AND direction = 'debit' THEN amount ELSE 0 END"
        for t in ["needs", "debt", "protection", "tax"]
    )
    wants_cases = " + ".join(
        f"CASE WHEN txn_type = '{t}' AND direction = 'debit' THEN amount ELSE 0 END"
        for t in ["wants", "charity", "fees", "business"]
    )
    assets_case = "CASE WHEN txn_type = 'assets' AND direction = 'debit' THEN amount ELSE 0 END"

    await conn.execute(
        f"""
        WITH m AS (SELECT date_trunc('month', $2::date)::date AS month),
        actuals AS (
            SELECT user_id, date_trunc('month', txn_date)::date AS month,
                   SUM(CASE WHEN txn_type = 'income' AND direction = 'credit' THEN amount ELSE 0 END) AS income_amt,
                   SUM(({needs_cases})) AS needs_amt,
                   SUM(({wants_cases})) AS wants_amt,
                   SUM(({assets_case})) AS assets_amt
            FROM spendsense.vw_txn_effective
            WHERE user_id = $1 AND date_trunc('month', txn_date) = (SELECT month FROM m)
            GROUP BY user_id, date_trunc('month', txn_date)
        ),
        plan AS (
            SELECT c.user_id, c.month, c.alloc_needs_pct, c.alloc_wants_pct, c.alloc_assets_pct
            FROM budgetpilot.user_budget_commit c
            WHERE c.user_id = $1 AND c.month = (SELECT month FROM m)
        ),
        joined AS (
            SELECT p.user_id, p.month,
                   COALESCE(a.income_amt, 0) AS income_amt,
                   COALESCE(a.needs_amt, 0) AS needs_amt,
                   COALESCE(a.wants_amt, 0) AS wants_amt,
                   COALESCE(a.assets_amt, 0) AS assets_amt,
                   p.alloc_needs_pct, p.alloc_wants_pct, p.alloc_assets_pct
            FROM plan p
            LEFT JOIN actuals a ON a.user_id = p.user_id AND a.month = p.month
        ),
        planned AS (
            SELECT j.*,
                   ROUND(COALESCE(j.income_amt, 0) * COALESCE(j.alloc_needs_pct, 0), 2) AS planned_needs_amt,
                   ROUND(COALESCE(j.income_amt, 0) * COALESCE(j.alloc_wants_pct, 0), 2) AS planned_wants_amt,
                   ROUND(COALESCE(j.income_amt, 0) * COALESCE(j.alloc_assets_pct, 0), 2) AS planned_assets_amt
            FROM joined j
        )
        INSERT INTO budgetpilot.budget_user_month_aggregate (
            user_id, month, income_amt,
            needs_amt, planned_needs_amt, variance_needs_amt,
            wants_amt, planned_wants_amt, variance_wants_amt,
            assets_amt, planned_assets_amt, variance_assets_amt,
            computed_at
        )
        SELECT p.user_id, p.month, p.income_amt,
               p.needs_amt, p.planned_needs_amt, ROUND(p.needs_amt - p.planned_needs_amt, 2),
               p.wants_amt, p.planned_wants_amt, ROUND(p.wants_amt - p.planned_wants_amt, 2),
               p.assets_amt, p.planned_assets_amt, ROUND(p.assets_amt - p.planned_assets_amt, 2),
               NOW()
        FROM planned p
        ON CONFLICT (user_id, month) DO UPDATE
        SET income_amt = EXCLUDED.income_amt,
            needs_amt = EXCLUDED.needs_amt, planned_needs_amt = EXCLUDED.planned_needs_amt,
            variance_needs_amt = EXCLUDED.variance_needs_amt,
            wants_amt = EXCLUDED.wants_amt, planned_wants_amt = EXCLUDED.planned_wants_amt,
            variance_wants_amt = EXCLUDED.variance_wants_amt,
            assets_amt = EXCLUDED.assets_amt, planned_assets_amt = EXCLUDED.planned_assets_amt,
            variance_assets_amt = EXCLUDED.variance_assets_amt,
            computed_at = NOW()
        """,
        user_uuid,
        month,
    )

    # Only return aggregate if user has a commit for this month
    row = await conn.fetchrow(
        """
        SELECT b.user_id, b.month, b.income_amt,
               b.needs_amt, b.planned_needs_amt, b.variance_needs_amt,
               b.wants_amt, b.planned_wants_amt, b.variance_wants_amt,
               b.assets_amt, b.planned_assets_amt, b.variance_assets_amt,
               b.computed_at
        FROM budgetpilot.budget_user_month_aggregate b
        JOIN budgetpilot.user_budget_commit c ON c.user_id = b.user_id AND c.month = b.month
        WHERE b.user_id = $1 AND b.month = $2
        """,
        user_uuid,
        month,
    )
    return dict(row) if row else None


async def process_transaction_for_budget_by_id(
    conn: asyncpg.Connection,
    user_id: str,
    txn_id: str,
) -> dict[str, Any] | None:
    """
    Refresh budget aggregate after a transaction is created/updated.
    Returns budget state for TransactionCreateResponse (actual_split, deviation, suggestion).
    """
    try:
        user_uuid = UUID(user_id)
    except (ValueError, TypeError):
        logger.warning(f"Invalid user_id for budget processing: {user_id}")
        return None

    row = await conn.fetchrow(
        """
        SELECT txn_id, user_id, txn_date, amount, direction, txn_type
        FROM spendsense.vw_txn_effective
        WHERE txn_id = $1::uuid AND user_id = $2::uuid
        """,
        txn_id,
        user_id,
    )
    if not row:
        logger.debug(f"Transaction {txn_id} not found for budget processing")
        return None

    txn_date_val = row["txn_date"]
    if hasattr(txn_date_val, "date"):
        txn_date_val = txn_date_val.date()
    elif isinstance(txn_date_val, str):
        txn_date_val = date.fromisoformat(txn_date_val)

    month = txn_date_val.replace(day=1)
    aggregate = await refresh_budget_aggregate(conn, user_id, month)
    if not aggregate:
        return None

    income = float(aggregate.get("income_amt", 0) or 0)
    needs_amt = float(aggregate.get("needs_amt", 0) or 0)
    wants_amt = float(aggregate.get("wants_amt", 0) or 0)
    assets_amt = float(aggregate.get("assets_amt", 0) or 0)
    planned_needs = float(aggregate.get("planned_needs_amt", 0) or 0)
    planned_wants = float(aggregate.get("planned_wants_amt", 0) or 0)
    planned_assets = float(aggregate.get("planned_assets_amt", 0) or 0)

    total_spend = needs_amt + wants_amt + assets_amt
    actual_needs_pct = needs_amt / income if income > 0 else 0
    actual_wants_pct = wants_amt / income if income > 0 else 0
    actual_savings_pct = assets_amt / income if income > 0 else 0

    planned_needs_pct = planned_needs / income if income > 0 else 0
    planned_wants_pct = planned_wants / income if income > 0 else 0
    planned_savings_pct = planned_assets / income if income > 0 else 0

    deviation_needs_pct = (actual_needs_pct - planned_needs_pct) * 100
    deviation_wants_pct = (actual_wants_pct - planned_wants_pct) * 100
    deviation_savings_pct = (actual_savings_pct - planned_savings_pct) * 100

    # Suggested adjustment (guardrails: max 5% shift, only if deviation > 5%)
    autopilot_suggestion = _compute_suggestion(
        deviation_needs_pct, deviation_wants_pct, deviation_savings_pct,
        planned_needs_pct, planned_wants_pct, planned_savings_pct,
    )

    alerts = []
    if deviation_savings_pct < -5:
        alerts.append("Savings below target")
    if deviation_wants_pct > 5:
        alerts.append("Wants overspending")
    if deviation_needs_pct > 5:
        alerts.append("Needs overspending")

    return {
        "budget_state_updated": True,
        "actual_split": {
            "needs": round(actual_needs_pct * 100, 1),
            "wants": round(actual_wants_pct * 100, 1),
            "savings": round(actual_savings_pct * 100, 1),
        },
        "deviation": {
            "needs": round(deviation_needs_pct, 1),
            "wants": round(deviation_wants_pct, 1),
            "savings": round(deviation_savings_pct, 1),
        },
        "autopilot_suggestion": autopilot_suggestion,
        "alerts": alerts,
    }


def _compute_suggestion(
    dev_needs: float,
    dev_wants: float,
    dev_savings: float,
    planned_needs_pct: float,
    planned_wants_pct: float,
    planned_savings_pct: float,
) -> dict[str, Any] | None:
    """
    Suggest adjustment within guardrails.
    Max 5% shift, only if deviation > 5%.
    Never reduce needs below 45% (safe minimum).
    """
    # Only suggest if savings shortfall > 5% or wants overspend > 5%
    if dev_savings >= -5 and dev_wants <= 5:
        return None

    shift_pct = 0.0
    shift_from = None
    shift_to = "savings"

    if dev_savings < -5 and dev_wants > 5:
        # Shift from wants to savings (most common)
        shift_from = "wants"
        shift_pct = min(5, abs(dev_savings) / 2, dev_wants)
    elif dev_savings < -5 and dev_needs > 5:
        # Needs overspend - suggest tightening wants to compensate
        shift_from = "wants"
        shift_pct = min(5, abs(dev_savings) / 2)
    elif dev_savings < -5:
        shift_from = "wants" if planned_wants_pct > 0.15 else "needs"
        shift_pct = min(5, abs(dev_savings) / 2)

    if shift_pct < 2:
        return None

    # Guardrail: never suggest reducing needs below 45%
    if shift_from == "needs" and (planned_needs_pct - shift_pct / 100) < 0.45:
        return None

    return {
        "shift_from": shift_from,
        "shift_to": shift_to,
        "pct": round(shift_pct, 1),
        "message": f"Move {shift_pct:.0f}% from {shift_from} to savings",
    }
