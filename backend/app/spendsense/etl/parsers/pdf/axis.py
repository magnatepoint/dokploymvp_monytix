"""Axis Bank PDF statement line-based parser.

Supports:
1. Credit card statements: Date Transaction Details Amount (INR) Debit/Credit
   e.g. 24 Jan '26 EMI Interest - 34/49, Ref# 42250246 ₹ 2,528.05 Debit
   e.g. BBPS Payment Received - 03 Jan '26 ₹ 19,089.74 Credit

2. Savings account statements: Tran Date | Particulars | Debit | Credit | Balance
   e.g. Statement of Axis Account No :911010025601581 for the period...
   01-08-2025 ACH-DR-Indian Clearing Corp- / 0000WB4CBFHBU6DYPC252 3000.00 113395.71
   02-08-2025 UPI/P2M/.../CRED Club 20000.00 58198.71  (debit)
   03-08-2025 CreditCard Payment XX 9594 25000.00 33198.71  (credit)
"""

from __future__ import annotations

import re
from datetime import datetime
from typing import Any

import pandas as pd  # type: ignore[import-untyped]

MONTH_TO_NUM = {
    "jan": "01", "feb": "02", "mar": "03", "apr": "04", "may": "05", "jun": "06",
    "jul": "07", "aug": "08", "sep": "09", "oct": "10", "nov": "11", "dec": "12",
}


def _parse_axis_date(date_str: str) -> datetime | None:
    """Parse Axis date format: DD MMM 'YY (e.g. 24 Jan '26)."""
    m = re.match(r"^(\d{1,2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+'(\d{2})\s*$", date_str, re.I)
    if not m:
        return None
    day, month_name, yy = m.group(1), m.group(2).lower()[:3], m.group(3)
    month = MONTH_TO_NUM.get(month_name)
    if not month:
        return None
    year = 2000 + int(yy)
    try:
        return datetime(year, int(month), int(day))
    except ValueError:
        return None


def parse_axis_pdf(lines: list[str]) -> pd.DataFrame | None:
    """Parse Axis Bank PDF statements (credit card or savings)."""
    if not lines:
        return None

    haystack = " ".join(lines[:80]).lower()
    if "axis" not in haystack and "axis bank" not in haystack:
        return None

    # Try savings format first (Statement of Axis Account, Tran Date, Debit Credit Balance)
    if "statement of axis account" in haystack or "tran date" in haystack:
        df = parse_axis_savings_pdf(lines)
        if df is not None and not df.empty:
            return df

    if "transaction" not in haystack and "debit" not in haystack and "credit" not in haystack:
        return None

    # Pattern 1: Date at start - "24 Jan '26 EMI Interest ... ₹ 2,528.05 Debit"
    pattern_date_first = re.compile(
        r"^(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+'\d{2})\s+(.+?)\s+₹\s+([\d,]+\.\d{2})\s+(Debit|Credit)\s*$",
        re.I,
    )
    # Pattern 2: Date in middle - "BBPS Payment Received - 03 Jan '26 ₹ 19,089.74 Credit"
    pattern_date_mid = re.compile(
        r"^(.+?)\s+(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+'\d{2})\s+₹\s+([\d,]+\.\d{2})\s+(Debit|Credit)\s*$",
        re.I,
    )
    # Pattern 3: Date-only (description on previous line) - "03 Jan '26 ₹ 19,089.74 Credit"
    pattern_date_only = re.compile(
        r"^(\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s+'\d{2})\s+₹\s+([\d,]+\.\d{2})\s+(Debit|Credit)\s*$",
        re.I,
    )

    def _skip_line(s: str) -> bool:
        return bool(
            re.match(r"^Date\s+Transaction\s+Details\s+Amount", s, re.I)
            or re.match(r"^\*\*End of Transaction Summary\*\*", s)
            or re.match(r"^\*\*End of Active Loans Summary\*\*", s)
            or re.match(r"^Page\s+\d+\s+of\s+\d+", s, re.I)
            or re.match(r"^View Active Loans", s, re.I)
            or re.match(r"^Payment Summary", s, re.I)
            or re.match(r"^Transaction Summary", s, re.I)
            or re.match(r"^Active Loans Summary", s, re.I)
            or re.match(r"^S\.No\s+Loan Type", s, re.I)
            or re.match(r"^Total\s+Remaining", s, re.I)
        )

    parsed_rows: list[dict[str, Any]] = []
    in_transaction_section = False
    prev_desc_continuation: str | None = None  # For multi-line: "BBPS Payment Received -" then "03 Jan '26 ₹ ... Credit"

    for i, line in enumerate(lines):
        line = line.strip()
        if not line:
            continue

        if "Transaction Summary" in line or "Date Transaction Details" in line:
            in_transaction_section = True
        if "**End of Transaction Summary**" in line:
            break
        if _skip_line(line):
            continue
        if not in_transaction_section and "₹" not in line:
            continue

        m_date_only = pattern_date_only.match(line)

        if m_date_only and prev_desc_continuation:
            date_str, amount_str, direction = m_date_only.group(1), m_date_only.group(2), m_date_only.group(3)
            desc = prev_desc_continuation
            prev_desc_continuation = None
        else:
            prev_desc_continuation = None
            # Try date-first pattern
            m = pattern_date_first.match(line)
            if m:
                date_str, desc, amount_str, direction = m.group(1), m.group(2), m.group(3), m.group(4)
            else:
                m = pattern_date_mid.match(line)
                if m:
                    desc_part, date_str, amount_str, direction = m.group(1), m.group(2), m.group(3), m.group(4)
                    desc = f"{desc_part} {date_str}".strip()
                else:
                    # Line might be description continuation (e.g. "BBPS Payment Received -")
                    if in_transaction_section and "₹" not in line and not re.match(r"^\d{1,2}\s+(?:Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)", line, re.I):
                        prev_desc_continuation = line
                    continue

        dt = _parse_axis_date(date_str.strip())
        if not dt:
            continue

        try:
            amount = float(amount_str.replace(",", ""))
        except ValueError:
            continue

        is_credit = direction.strip().lower() == "credit"
        parsed_rows.append({
            "txn_date": dt.date(),
            "description": desc.strip(),
            "withdrawal_amt": None if is_credit else amount,
            "deposit_amt": amount if is_credit else None,
        })

    if not parsed_rows:
        return None

    return pd.DataFrame(parsed_rows)


# Axis savings: PDF table extraction can put date on same line as amounts.
# Format: "01-08-2025 0000WB4CBFHBU6DYPC252 3000.00 113395.71 1817"
# Description often on previous line: "ACH-DR-Indian Clearing Corp-"
_AXIS_SAVINGS_TXN_LINE = re.compile(
    r"^(\d{2}-\d{2}-\d{4})\s+(.+?)\s+([\d,]+\.\d{2})\s+([\d,]+\.\d{2})(?:\s+(\d+))?\s*$"
)
_AXIS_OPENING_BALANCE = re.compile(r"^OPENING\s+BALANCE\s+([\d,]+\.\d{2})\s*$", re.I)


def _parse_axis_savings_date(s: str) -> datetime | None:
    """Parse DD-MM-YYYY format."""
    m = re.match(r"^(\d{2})-(\d{2})-(\d{4})\s*$", s.strip())
    if not m:
        return None
    try:
        return datetime(int(m.group(3)), int(m.group(2)), int(m.group(1)))
    except ValueError:
        return None


def parse_axis_savings_pdf(lines: list[str]) -> pd.DataFrame | None:
    """Parse Axis Bank savings account statements (Tran Date | Particulars | Debit | Credit | Balance)."""
    haystack = " ".join(lines[:100]).lower()
    if "statement of axis account" not in haystack and "axis" not in haystack:
        return None
    if "tran date" not in haystack and "debit" not in haystack and "credit" not in haystack:
        return None

    skip_patterns = [
        r"^CLOSING BALANCE",
        r"^Tran Date",
        r"^Statement of Axis",
        r"^-- \d+ of \d+ --",
        r"^Page \d+",
        r"^Init\.?\s*$",
        r"^Br\s*$",
        r"TRANSACTION TOTAL",
        r"CLOSING BALANCE",
    ]

    def _skip(s: str) -> bool:
        return any(re.search(p, s, re.I) for p in skip_patterns)

    parsed_rows: list[dict[str, Any]] = []
    prev_balance: float | None = None
    prev_desc: str | None = None

    for line in lines:
        line_stripped = line.strip()
        if not line_stripped:
            continue

        # Capture OPENING BALANCE
        m_open = _AXIS_OPENING_BALANCE.match(line_stripped)
        if m_open:
            try:
                prev_balance = float(m_open.group(1).replace(",", ""))
            except ValueError:
                pass
            prev_desc = None
            continue

        if _skip(line_stripped):
            prev_desc = None
            continue

        # Transaction line: DD-MM-YYYY ref_or_desc amount1 amount2 [branch]
        m = _AXIS_SAVINGS_TXN_LINE.match(line_stripped)
        if m:
            date_str, mid_part, num1_str, num2_str = m.group(1), m.group(2), m.group(3), m.group(4)
            dt = _parse_axis_savings_date(date_str)
            if not dt:
                prev_desc = line_stripped
                continue
            try:
                num1 = float(num1_str.replace(",", ""))
                num2 = float(num2_str.replace(",", ""))
            except ValueError:
                prev_desc = line_stripped
                continue

            # num1 = txn amount, num2 = new balance. Direction from balance change.
            new_balance = num2
            amount = num1
            if prev_balance is not None:
                is_credit = new_balance > prev_balance
            else:
                is_credit = False

            # Description: prev line + mid_part (ref/continuation)
            if prev_desc:
                desc = f"{prev_desc} {mid_part}".strip()
            else:
                desc = mid_part.strip()
            if not desc:
                desc = "Unknown"

            parsed_rows.append({
                "txn_date": dt.date(),
                "description": desc,
                "withdrawal_amt": None if is_credit else amount,
                "deposit_amt": amount if is_credit else None,
            })
            prev_balance = new_balance
            prev_desc = None
        else:
            # Not a txn line - accumulate as description for next line
            prev_desc = f"{prev_desc} {line_stripped}".strip() if prev_desc else line_stripped

    if not parsed_rows:
        return None
    return pd.DataFrame(parsed_rows)
