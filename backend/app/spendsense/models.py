from datetime import date, datetime
from typing import Any
from pydantic import BaseModel, Field


class SourceType:
    MANUAL = "manual"
    EMAIL = "email"
    FILE = "file"


class StagingRecord(BaseModel):
    txn_id: str
    user_id: str
    txn_date: date
    description: str
    amount: float
    direction: str


class UploadBatchCreate(BaseModel):
    source_type: str
    account_ref: str | None = None


class UploadBatch(BaseModel):
    upload_id: str
    user_id: str
    source_type: str
    account_ref: str | None = None
    status: str
    created_at: datetime
    error_message: str | None = None  # From error_json when status=failed
    from_date: date | None = None  # Min txn_date in batch (for nudge/moments pipeline)
    to_date: date | None = None  # Max txn_date in batch


class TransactionRecord(BaseModel):
    txn_id: str
    txn_date: date
    txn_time: str | None = None  # Time of day (e.g. "15:30:00") from view
    recorded_at: datetime | None = None  # When the row was inserted (txn_fact.created_at)
    merchant: str | None
    category: str | None
    subcategory: str | None
    bank_code: str | None
    channel: str | None
    amount: float
    direction: str
    confidence: float | None = None


class UpdatedGoalItem(BaseModel):
    """Goal updated by a transaction - for UI toast + animation."""

    goal_id: str
    goal_name: str
    delta: float
    prev_pct: float
    new_pct: float
    reason: str


class BudgetStateUpdate(BaseModel):
    """Budget state updated by a transaction - for Autopilot UI."""

    budget_state_updated: bool = True
    actual_split: dict[str, float] = Field(default_factory=dict)  # needs, wants, savings pct
    deviation: dict[str, float] = Field(default_factory=dict)  # needs, wants, savings pct delta
    autopilot_suggestion: dict[str, Any] | None = None  # shift_from, shift_to, pct, message
    alerts: list[str] = Field(default_factory=list)


class TransactionCreateResponse(BaseModel):
    """Response for POST /transactions - includes transaction + affected goals + budget state."""

    txn_id: str
    txn_date: date
    merchant: str | None
    category: str | None
    subcategory: str | None
    bank_code: str | None
    channel: str | None
    amount: float
    direction: str
    confidence: float | None = None
    updated_goals: list[UpdatedGoalItem] = Field(default_factory=list)
    budget_state: BudgetStateUpdate | None = None


class TransactionListResponse(BaseModel):
    transactions: list[TransactionRecord]
    total: int
    page: int
    page_size: int


class TransactionSummaryResponse(BaseModel):
    debit_total: float = 0.0
    credit_total: float = 0.0
    debit_count: int = 0
    credit_count: int = 0


class TransactionCreate(BaseModel):
    txn_date: date
    merchant_name: str
    description: str | None = None
    amount: float
    direction: str
    category_code: str | None = None
    subcategory_code: str | None = None
    channel: str | None = None
    account_ref: str | None = None


class TransactionUpdate(BaseModel):
    category_code: str | None = None
    subcategory_code: str | None = None
    txn_type: str | None = None
    merchant_name: str | None = None
    channel: str | None = None


class CategoryResponse(BaseModel):
    category_code: str
    category_name: str
    is_custom: bool = False
    txn_type: str | None = None


class SubcategoryResponse(BaseModel):
    subcategory_code: str
    subcategory_name: str
    category_code: str
    is_custom: bool = False


class CategorySpendKPI(BaseModel):
    category_code: str
    category_name: str
    txn_count: int
    spend_amount: float
    income_amount: float
    delta_pct: float | None = None


class WantsGauge(BaseModel):
    ratio: float
    label: str
    threshold_crossed: bool


class BestMonthSnapshot(BaseModel):
    month: date
    net_amount: float
    delta_pct: float | None
    is_current_best: bool


class LootDropSummary(BaseModel):
    batch_id: str
    occurred_at: datetime
    transactions_unlocked: int
    rarity: str = "common"


class AvailableMonthsResponse(BaseModel):
    data: list[str]


class SpendSenseKPI(BaseModel):
    month: date | None
    income_amount: float
    needs_amount: float
    wants_amount: float
    assets_amount: float
    total_debits_amount: float = 0.0  # All debits for the month (for "This Month" spending)
    top_categories: list[CategorySpendKPI]
    wants_gauge: WantsGauge | None = None
    best_month: BestMonthSnapshot | None = None
    recent_loot_drop: LootDropSummary | None = None


class SpendSenseActivity(BaseModel):
    timestamp: datetime
    title: str
    meta: str


# Insights Models
class TimeSeriesPoint(BaseModel):
    date: str  # YYYY-MM-DD or YYYY-MM
    value: float
    label: str | None = None


class CategoryBreakdownItem(BaseModel):
    category_code: str
    category_name: str
    amount: float
    percentage: float
    transaction_count: int
    avg_transaction: float


class SpendingTrend(BaseModel):
    period: str  # YYYY-MM
    income: float
    expenses: float
    net: float
    needs: float
    wants: float
    assets: float


class RecurringTransaction(BaseModel):
    merchant_name: str
    category_code: str
    category_name: str
    subcategory_code: str | None
    subcategory_name: str | None
    frequency: str  # "monthly", "weekly", "daily", etc.
    avg_amount: float
    last_occurrence: date
    next_expected: date | None
    transaction_count: int
    total_amount: float


class SpendingPattern(BaseModel):
    day_of_week: str | None = None
    time_of_day: str | None = None
    amount: float
    transaction_count: int


class InsightsResponse(BaseModel):
    time_series: list[TimeSeriesPoint]
    category_breakdown: list[CategoryBreakdownItem]
    spending_trends: list[SpendingTrend]
    recurring_transactions: list[RecurringTransaction]
    spending_patterns: list[SpendingPattern]
    top_merchants: list[dict[str, Any]]
    anomalies: list[dict[str, Any]] | None = None


class TopInsightItem(BaseModel):
    """Single insight for command-center Home (top 3)."""

    id: str
    title: str
    message: str
    type: str  # risk, optimization, pattern, goal_progress, budget_tip, on_track
    confidence: float | None = None


class TopInsightsResponse(BaseModel):
    """Top N insights for Home command center."""

    insights: list[TopInsightItem]


class AccountItem(BaseModel):
    """Account derived from transaction data (bank_code + account_ref)."""

    id: str
    bank_code: str
    bank_name: str
    account_number: str | None
    balance: float
    account_type: str  # SAVINGS, CHECKING, etc.
    transaction_count: int
    last_txn_date: date | None = None


class AccountsListResponse(BaseModel):
    accounts: list[AccountItem]
