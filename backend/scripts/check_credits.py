#!/usr/bin/env python3
"""Check if user has credit transactions in the database.
Run: POSTGRES_URL=... python scripts/check_credits.py [user_id]
"""

import asyncio
import os
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

import asyncpg


async def main():
    postgres_url = os.getenv("POSTGRES_URL")
    if not postgres_url:
        print("Set POSTGRES_URL environment variable")
        sys.exit(1)

    user_id = sys.argv[1] if len(sys.argv) > 1 else None
    if not user_id:
        # Get first user with transactions
        conn = await asyncpg.connect(postgres_url, statement_cache_size=0)
        try:
            row = await conn.fetchrow(
                """
                SELECT user_id FROM spendsense.txn_fact
                GROUP BY user_id
                ORDER BY COUNT(*) DESC
                LIMIT 1
                """
            )
            if not row:
                print("No transactions in database")
                return
            user_id = str(row["user_id"])
            print(f"Using user_id: {user_id}")
        finally:
            await conn.close()

    conn = await asyncpg.connect(postgres_url, statement_cache_size=0)
    try:
        row = await conn.fetchrow(
            """
            SELECT
                COUNT(*) as total,
                COUNT(CASE WHEN direction = 'credit' THEN 1 END) as credits,
                COUNT(CASE WHEN direction = 'debit' THEN 1 END) as debits,
                COALESCE(SUM(CASE WHEN direction = 'credit' THEN amount ELSE 0 END), 0) as credit_sum,
                COALESCE(SUM(CASE WHEN direction = 'debit' THEN amount ELSE 0 END), 0) as debit_sum
            FROM spendsense.txn_fact
            WHERE user_id = $1
            """,
            user_id,
        )
        print(f"\nUser {user_id}:")
        print(f"  Total: {row['total']}")
        print(f"  Credits: {row['credits']} (sum: ₹{row['credit_sum']:,.2f})")
        print(f"  Debits:  {row['debits']} (sum: ₹{row['debit_sum']:,.2f})")

        if row["credits"] == 0:
            print("\n  No credit transactions in DB.")
            print("  Possible causes:")
            print("  1. Data from MongoDB/Gmail - may not include credits")
            print("  2. AXISMB PDF not uploaded - upload via SpendSense → Upload Statement")
            print("  3. Old upload before Axis parser was added")
        else:
            samples = await conn.fetch(
                """
                SELECT txn_date, description, amount, direction
                FROM spendsense.txn_fact
                WHERE user_id = $1 AND direction = 'credit'
                ORDER BY txn_date DESC
                LIMIT 5
                """,
                user_id,
            )
            print("\n  Sample credits:")
            for r in samples:
                print(f"    {r['txn_date']} | {str(r['description'])[:50]:50} | ₹{r['amount']:,.2f}")
    finally:
        await conn.close()


if __name__ == "__main__":
    asyncio.run(main())
