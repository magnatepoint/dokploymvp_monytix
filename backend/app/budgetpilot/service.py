"""BudgetPilot service layer."""

import logging
from datetime import date
from typing import Any
from uuid import UUID

import asyncpg

from .budget_repository import BudgetRepository
from .recommendation_engine import RecommendationEngine

logger = logging.getLogger(__name__)


class BudgetService:
    """Service for BudgetPilot operations."""

    def __init__(self, pool: asyncpg.Pool):
        self.pool = pool

    async def get_recommendations(
        self, user_id: UUID, month: date | None = None
    ) -> list[dict[str, Any]]:
        """Get budget recommendations for a user."""
        async with self.pool.acquire() as conn:
            repo = BudgetRepository(conn)
            engine = RecommendationEngine(repo)
            return await engine.generate_recommendations(user_id, month)

    async def commit_budget(
        self,
        user_id: UUID,
        month: date,
        plan_code: str,
        goal_allocations: dict[str, float] | None = None,
        notes: str | None = None,
    ) -> dict[str, Any]:
        """
        Commit a budget plan for a user.
        
        Args:
            user_id: User ID
            month: Target month
            plan_code: Selected plan code
            goal_allocations: Optional dict of {goal_id: amount} to override auto-allocation
            notes: Optional notes
        
        Returns:
            Committed budget with goal allocations
        """
        async with self.pool.acquire() as conn:
            repo = BudgetRepository(conn)

            # 1. Get the plan template to get percentages
            templates = await repo.get_plan_templates(active_only=True)
            template = next((t for t in templates if t["plan_code"] == plan_code), None)
            if not template:
                raise ValueError(f"Plan template '{plan_code}' not found")

            # 2. Get user's monthly income estimate
            spending = await repo.get_user_spending_pattern(user_id, months_back=3)
            if not spending or spending["avg_income"] <= 0:
                # Fallback: use current month income
                async with conn.transaction():
                    income_row = await repo.conn.fetchrow(
                        """
                        SELECT SUM(amount) AS income_amt
                        FROM spendsense.vw_txn_effective
                        WHERE user_id = $1
                          AND txn_type = 'income'
                          AND direction = 'credit'
                          AND date_trunc('month', txn_date) = date_trunc('month', CURRENT_DATE)
                        """,
                        user_id,
                    )
                    monthly_income = float(income_row["income_amt"] or 0) if income_row else 0
            else:
                monthly_income = spending["avg_income"]

            # 3. Calculate allocations
            alloc_needs_pct = float(template["base_needs_pct"])
            alloc_wants_pct = float(template["base_wants_pct"])
            alloc_assets_pct = float(template["base_assets_pct"])

            # 4. Commit the budget
            await repo.commit_budget(
                user_id,
                month,
                plan_code,
                alloc_needs_pct,
                alloc_wants_pct,
                alloc_assets_pct,
                notes,
            )

            # 5. Compute and store goal allocations
            goal_alloc_list = await self._compute_goal_allocations(
                repo, user_id, month, monthly_income, alloc_assets_pct, goal_allocations
            )
            await repo.set_goal_allocations(user_id, month, goal_alloc_list)

            # 6. Return committed budget
            commit = await repo.get_user_commit(user_id, month)
            allocations = await repo.get_goal_allocations(user_id, month)

            return {
                **commit,
                "goal_allocations": allocations,
            }

    async def _compute_goal_allocations(
        self,
        repo: BudgetRepository,
        user_id: UUID,
        month: date,
        monthly_income: float,
        savings_pct: float,
        manual_allocations: dict[str, float] | None = None,
    ) -> list[dict[str, Any]]:
        """Compute goal-level allocations from savings budget."""
        if manual_allocations:
            # Use manual allocations if provided
            total_savings = monthly_income * savings_pct
            total_manual = sum(manual_allocations.values())
            if abs(total_manual - total_savings) > 0.01:
                logger.warning(
                    f"Manual allocations sum ({total_manual}) doesn't match savings budget ({total_savings})"
                )

            return [
                {
                    "goal_id": goal_id,
                    "weight_pct": amount / total_savings if total_savings > 0 else 0,
                    "planned_amount": amount,
                }
                for goal_id, amount in manual_allocations.items()
            ]

        # Auto-compute based on goals and attributes
        goals = await repo.get_user_goals(user_id)
        if not goals:
            return []

        goal_attrs = await repo.get_goal_attributes(user_id)
        total_savings = monthly_income * savings_pct

        # Compute weights
        weights = []
        for goal in goals:
            goal_id = str(goal["goal_id"])
            priority_rank = goal.get("priority_rank") or 5
            attrs = goal_attrs.get(goal_id, {})

            # Weight formula: priority (inverse) + essentiality + urgency
            priority_weight = max(1, 6 - min(5, priority_rank))
            essentiality = attrs.get("essentiality_score", 50) / 25.0
            urgency = attrs.get("urgency_score", 50) / 25.0

            raw_weight = priority_weight + essentiality + urgency
            weights.append({
                "goal_id": goal_id,
                "raw_weight": raw_weight,
            })

        # Normalize weights
        total_weight = sum(w["raw_weight"] for w in weights)
        if total_weight == 0:
            return []

        allocations = []
        for w in weights:
            weight_pct = w["raw_weight"] / total_weight
            planned_amount = total_savings * weight_pct
            allocations.append({
                "goal_id": w["goal_id"],
                "weight_pct": round(weight_pct, 4),
                "planned_amount": round(planned_amount, 2),
            })

        return allocations

    async def get_committed_budget(
        self, user_id: UUID, month: date | None = None
    ) -> dict[str, Any] | None:
        """Get user's committed budget for a month."""
        async with self.pool.acquire() as conn:
            repo = BudgetRepository(conn)
            commit = await repo.get_user_commit(user_id, month)
            if not commit:
                return None

            allocations = await repo.get_goal_allocations(user_id, month)
            return {
                **commit,
                "goal_allocations": allocations,
            }

    async def get_month_aggregate(
        self, user_id: UUID, month: date | None = None
    ) -> dict[str, Any] | None:
        """Get monthly aggregate (actuals vs planned) for a user."""
        async with self.pool.acquire() as conn:
            repo = BudgetRepository(conn)
            return await repo.get_month_aggregate(user_id, month)

    async def get_budget_state(
        self, user_id: UUID, month: date | None = None
    ) -> dict[str, Any]:
        """
        Get unified budget state: committed plan, actuals, deviation, plans with scores.
        Refreshes aggregate before returning so data is always current.
        """
        if month is None:
            month = date.today().replace(day=1)
        else:
            month = month.replace(day=1)

        async with self.pool.acquire() as conn:
            # Refresh aggregate first (ensures we have latest from transactions)
            from .transaction_hook import refresh_budget_aggregate
            await refresh_budget_aggregate(conn, str(user_id), month)

            repo = BudgetRepository(conn)
            commit = await repo.get_user_commit(user_id, month)
            aggregate = await repo.get_month_aggregate(user_id, month)
            try:
                recommendations = await self.get_recommendations(user_id, month)
            except Exception as e:
                logger.warning(f"get_recommendations failed, using plan templates: {e}")
                recommendations = []
            if not recommendations:
                templates = await repo.get_plan_templates(active_only=True)
                recommendations = [
                    {
                        "plan_code": t["plan_code"],
                        "name": t["name"],
                        "description": t.get("description"),
                        "score": 0.5,
                        "recommendation_reason": "Default plan. Add transactions for personalized recommendations.",
                    }
                    for t in templates[:5]
                ]

        if not commit:
            return {
                "month": month.strftime("%Y-%m"),
                "committed_plan": None,
                "actual": None,
                "deviation": None,
                "plans": [
                    {
                        "plan_id": r["plan_code"],
                        "name": r.get("name", r["plan_code"]),
                        "score": float(r.get("score", 0)),
                        "reason": r.get("recommendation_reason", ""),
                    }
                    for r in recommendations[:5]
                ],
                "last_updated_at": None,
            }

        target = {
            "needs": round(float(commit["alloc_needs_pct"]) * 100, 1),
            "wants": round(float(commit["alloc_wants_pct"]) * 100, 1),
            "savings": round(float(commit["alloc_assets_pct"]) * 100, 1),
        }

        if not aggregate or aggregate.get("income_amt", 0) <= 0:
            return {
                "month": month.strftime("%Y-%m"),
                "committed_plan": {
                    "plan_id": commit["plan_code"],
                    "target": target,
                },
                "income_amt": 0,
                "actual": {
                    "needs_pct": 0,
                    "wants_pct": 0,
                    "savings_pct": 0,
                    "needs_amt": 0,
                    "wants_amt": 0,
                    "savings_amt": 0,
                },
                "deviation": {"needs": 0, "wants": 0, "savings": 0},
                "plans": [
                    {
                        "plan_id": r["plan_code"],
                        "name": r.get("name", r["plan_code"]),
                        "score": float(r.get("score", 0)),
                        "reason": (r.get("recommendation_reason") or "")[:80],
                    }
                    for r in recommendations[:5]
                ],
                "last_updated_at": (
                    aggregate["computed_at"].isoformat()
                    if aggregate and aggregate.get("computed_at")
                    else None
                ),
            }

        income = float(aggregate["income_amt"])
        needs_amt = float(aggregate["needs_amt"])
        wants_amt = float(aggregate["wants_amt"])
        savings_amt = float(aggregate["assets_amt"])
        actual_needs_pct = round((needs_amt / income) * 100, 1) if income > 0 else 0
        actual_wants_pct = round((wants_amt / income) * 100, 1) if income > 0 else 0
        actual_savings_pct = round((savings_amt / income) * 100, 1) if income > 0 else 0

        deviation = {
            "needs": round(actual_needs_pct - target["needs"], 1),
            "wants": round(actual_wants_pct - target["wants"], 1),
            "savings": round(actual_savings_pct - target["savings"], 1),
        }

        return {
            "month": month.strftime("%Y-%m"),
            "committed_plan": {
                "plan_id": commit["plan_code"],
                "target": target,
            },
            "income_amt": round(income, 2),
            "actual": {
                "needs_pct": actual_needs_pct,
                "wants_pct": actual_wants_pct,
                "savings_pct": actual_savings_pct,
                "needs_amt": round(needs_amt, 2),
                "wants_amt": round(wants_amt, 2),
                "savings_amt": round(savings_amt, 2),
            },
            "deviation": deviation,
            "plans": [
                {
                    "plan_id": r["plan_code"],
                    "name": r.get("name", r["plan_code"]),
                    "score": float(r.get("score", 0)),
                    "reason": (r.get("recommendation_reason") or "")[:80],
                }
                for r in recommendations[:5]
            ],
            "last_updated_at": (
                aggregate["computed_at"].isoformat()
                if aggregate.get("computed_at")
                else None
            ),
        }

    async def apply_adjustment(
        self,
        user_id: UUID,
        month: date,
        shift_from: str,
        shift_to: str,
        pct: float,
    ) -> dict[str, Any]:
        """
        Apply Autopilot suggestion: shift pct from shift_from to shift_to.
        Guardrails: max 5% per adjustment, never reduce needs below 45%.
        """
        async with self.pool.acquire() as conn:
            repo = BudgetRepository(conn)
            commit = await repo.get_user_commit(user_id, month)
            if not commit:
                return {"status": "no_commitment", "budget": None}

            alloc_needs = float(commit["alloc_needs_pct"])
            alloc_wants = float(commit["alloc_wants_pct"])
            alloc_assets = float(commit["alloc_assets_pct"])

            # Guardrails
            pct = min(5.0, max(0.5, pct)) / 100.0  # clamp to 0.5-5%
            shift_from = shift_from.lower()
            shift_to = shift_to.lower() if shift_to else "savings"

            if shift_from == "needs":
                new_needs = alloc_needs - pct
                if new_needs < 0.45:
                    return {
                        "status": "rejected",
                        "reason": "Cannot reduce needs below 45%",
                        "budget": await self.get_committed_budget(user_id, month),
                    }
                alloc_needs = new_needs
            elif shift_from == "wants":
                alloc_wants = max(0.05, alloc_wants - pct)
            else:
                return {"status": "rejected", "reason": f"Invalid shift_from: {shift_from}"}

            if shift_to in ("savings", "assets"):
                alloc_assets += pct

            # Normalize to 1.0
            total = alloc_needs + alloc_wants + alloc_assets
            alloc_needs /= total
            alloc_wants /= total
            alloc_assets /= total

            await repo.commit_budget(
                user_id,
                month,
                str(commit["plan_code"]),
                alloc_needs,
                alloc_wants,
                alloc_assets,
                notes=f"Autopilot: +{pct*100:.0f}% {shift_to} from {shift_from}",
            )

            # Refresh goal allocations
            spending = await repo.get_user_spending_pattern(user_id, months_back=3)
            monthly_income = spending["avg_income"] if spending else 0
            if monthly_income <= 0:
                income_row = await conn.fetchrow(
                    """
                    SELECT SUM(amount) AS income_amt
                    FROM spendsense.vw_txn_effective
                    WHERE user_id = $1 AND txn_type = 'income' AND direction = 'credit'
                      AND date_trunc('month', txn_date) = $2
                    """,
                    user_id,
                    month,
                )
                monthly_income = float(income_row["income_amt"] or 0) if income_row else 0

            goal_alloc_list = await self._compute_goal_allocations(
                repo, user_id, month, monthly_income, alloc_assets, None
            )
            await repo.set_goal_allocations(user_id, month, goal_alloc_list)

            # Refresh aggregate
            from .transaction_hook import refresh_budget_aggregate
            await refresh_budget_aggregate(conn, str(user_id), month)

            budget = await self.get_committed_budget(user_id, month)
            return {"status": "applied", "budget": budget}

