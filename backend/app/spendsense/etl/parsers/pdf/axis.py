"""Axis Bank PDF statement line-based parser.

Supports credit card statements with format:
  Date Transaction Details Amount (INR) Debit/Credit
  e.g. 24 Jan '26 EMI Interest - 34/49, Ref# 42250246 ₹ 2,528.05 Debit
  e.g. BBPS Payment Received - 03 Jan '26 ₹ 19,089.74 Credit
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
    """Parse Axis Bank PDF statements (credit card, savings)."""
    if not lines:
        return None

    haystack = " ".join(lines[:80]).lower()
    if "axis" not in haystack and "axis bank" not in haystack:
        return None
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
