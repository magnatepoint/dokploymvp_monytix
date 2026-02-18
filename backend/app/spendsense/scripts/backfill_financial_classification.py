#!/usr/bin/env python3
"""
Backfill financial_class, obligation_flag, instrument_type, counterparty_type, priority_rank
for existing txn_enriched rows using txn_parsed + classification.

Run after migration 073. Safe to run multiple times (idempotent).

Usage:
    python -m app.spendsense.scripts.backfill_financial_classification [--user-id USER_ID] [--batch-size 500] [--dry-run]
    python -m app.spendsense.scripts.backfill_financial_classification --all   # update all rows, not just NULL
"""
import argparse
import asyncio
import logging
import sys
from pathlib import Path

backend_dir = Path(__file__).resolve().parent.parent.parent.parent
sys.path.insert(0, str(backend_dir))

import asyncpg

from app.core.config import get_settings
from app.spendsense.services.financial_classification import classify_financial

logging.basicConfig(
    level=logging.INFO,
    format="%(asctime)s - %(name)s - %(levelname)s - %(message)s",
)
logger = logging.getLogger(__name__)


async def backfill_financial_classification(
    conn: asyncpg.Connection,
    user_id: str | None = None,
    batch_size: int = 500,
    dry_run: bool = False,
    all_rows: bool = False,
) -> int:
    """
    Update txn_enriched with financial classification from txn_parsed + classify_financial().

    By default only rows where financial_class IS NULL are updated. Use --all to update every row.

    Returns number of rows updated.
    """
    if all_rows:
        where_clause = ""
        params: tuple = ()
    else:
        where_clause = " AND te.financial_class IS NULL"
        params = ()

    if user_id:
        where_clause += " AND tf.user_id = $1"
        params = (user_id,)

    # Require migration 073 (financial_class column) to be applied
    has_column = await conn.fetchval("""
        SELECT EXISTS (
            SELECT 1 FROM information_schema.columns
            WHERE table_schema = 'spendsense' AND table_name = 'txn_enriched'
            AND column_name = 'financial_class'
        )
    """)
    if not has_column:
        logger.error(
            "Column spendsense.txn_enriched.financial_class not found. "
            "Run migration 073_financial_classification_schema.sql first."
        )
        return 0

    count_sql = f"""
        SELECT COUNT(*)
        FROM spendsense.txn_enriched te
        JOIN spendsense.txn_parsed tp ON tp.parsed_id = te.parsed_id
        JOIN spendsense.txn_fact tf ON tf.txn_id = tp.fact_txn_id
        WHERE 1=1 {where_clause}
    """
    total = await conn.fetchval(count_sql, *params)
    if total == 0:
        logger.info("No rows to backfill")
        return 0

    logger.info("Backfilling financial classification for %s rows (batch_size=%s, dry_run=%s)", total, batch_size, dry_run)

    select_sql = f"""
        SELECT te.parsed_id, tp.raw_description, tp.counterparty_name, tp.counterparty_vpa,
            tp.channel_type, tp.cr_dr, tp.direction,
            te.category_id, te.cat_l1, te.transfer_type, te.is_card_payment,
            te.is_loan_payment, te.is_investment, te.amount
        FROM spendsense.txn_enriched te
        JOIN spendsense.txn_parsed tp ON tp.parsed_id = te.parsed_id
        JOIN spendsense.txn_fact tf ON tf.txn_id = tp.fact_txn_id
        WHERE 1=1 {where_clause}
        ORDER BY te.parsed_id
    """
    rows = await conn.fetch(select_sql, *params)
    updated = 0
    for r in rows:
        cat = (r["category_id"] or "").strip().lower()
        merchant_flag = cat not in ("transfers_out", "transfers_in")
        fc = classify_financial(
            channel_type=r["channel_type"],
            cr_dr=r["cr_dr"],
            direction=r["direction"],
            raw_description=r["raw_description"],
            counterparty_name=r["counterparty_name"],
            counterparty_vpa=r["counterparty_vpa"],
            category_id=r["category_id"],
            cat_l1=r["cat_l1"],
            transfer_type=r["transfer_type"],
            is_card_payment=bool(r["is_card_payment"]),
            is_loan_payment=bool(r["is_loan_payment"]),
            is_investment=bool(r["is_investment"]),
            merchant_flag=merchant_flag,
            amount=float(r["amount"] or 0),
        )
        if not dry_run:
            await conn.execute(
                """
                UPDATE spendsense.txn_enriched
                SET financial_class = $1, obligation_flag = $2, instrument_type = $3,
                    counterparty_type = $4, priority_rank = $5
                WHERE parsed_id = $6
                """,
                fc.financial_class,
                fc.obligation_flag,
                fc.instrument_type,
                fc.counterparty_type,
                fc.priority_rank,
                r["parsed_id"],
            )
        updated += 1
        if updated % batch_size == 0:
            logger.info("Backfilled %s / %s", updated, total)

    logger.info("Backfill complete: %s rows %s", updated, "would be updated (dry-run)" if dry_run else "updated")
    return updated


async def main() -> None:
    parser = argparse.ArgumentParser(description="Backfill financial classification on txn_enriched")
    parser.add_argument("--user-id", type=str, help="Limit to this user_id")
    parser.add_argument("--batch-size", type=int, default=500, help="Log progress every N rows")
    parser.add_argument("--dry-run", action="store_true", help="Do not write; only count and log")
    parser.add_argument("--all", action="store_true", help="Update all enriched rows (default: only where financial_class IS NULL)")
    args = parser.parse_args()

    settings = get_settings()
    conn = await asyncpg.connect(str(settings.postgres_dsn), statement_cache_size=0, command_timeout=600)
    try:
        await backfill_financial_classification(
            conn,
            user_id=args.user_id,
            batch_size=args.batch_size,
            dry_run=args.dry_run,
            all_rows=args.all,
        )
    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(main())
