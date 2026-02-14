"""HDFC Bank PDF statement line-based parser.

Supports:
1. Single-line format: DD/MM/YY Narration Ref DD/MM/YY Withdrawal ClosingBalance
   e.g. 01/04/25 BHDFU4F0H84OGQ/BILLDKHDFCCARD 0000259180488742 01/04/25 140,000.00 102,853.67
2. Multi-line format: date on own line, then narration/ref/amounts on following lines
"""

from __future__ import annotations

import re
from datetime import datetime
from typing import Any

import pandas as pd  # type: ignore[import-untyped]

# Single-line: Date Narration Ref ValueDt Amt1 Amt2 (Amt1=withdrawal or deposit, Amt2=closing)
_HDFC_SINGLE_LINE = re.compile(
    r"^(\d{2}/\d{2}/\d{2})\s+(.+)\s+(\d{2}/\d{2}/\d{2})\s+([\d,]+\.\d{2})\s+([\d,]+\.\d{2})\s*$"
)


def _parse_hdfc_date(s: str) -> datetime | None:
    """Parse DD/MM/YY format."""
    m = re.match(r"^(\d{2})/(\d{2})/(\d{2})\s*$", s.strip())
    if not m:
        return None
    try:
        yy = int(m.group(3))
        year = 2000 + yy if yy < 50 else 1900 + yy
        return datetime(year, int(m.group(2)), int(m.group(1)))
    except ValueError:
        return None


def _parse_hdfc_single_line_format(lines: list[str]) -> pd.DataFrame | None:
    """Parse HDFC statements where each transaction is on one line."""
    parsed_rows: list[dict[str, Any]] = []
    prev_balance: float | None = None

    skip = {"date narration chq./ref.no.", "statementof account", "hdfc bank limited"}

    for line in lines:
        line_stripped = line.strip()
        if not line_stripped or len(line_stripped) < 30:
            continue
        if any(s in line_stripped.lower().replace(" ", "") for s in skip):
            continue

        m = _HDFC_SINGLE_LINE.match(line_stripped)
        if not m:
            continue

        date_str, narration_ref, value_dt, amt1_str, amt2_str = m.groups()
        dt = _parse_hdfc_date(date_str)
        if not dt:
            continue
        try:
            amt1 = float(amt1_str.replace(",", ""))
            amt2 = float(amt2_str.replace(",", ""))
        except ValueError:
            continue

        # amt1 = withdrawal or deposit, amt2 = closing balance
        closing = amt2
        if prev_balance is not None:
            is_credit = closing > prev_balance
        else:
            is_credit = False

        parsed_rows.append({
            "txn_date": dt.date(),
            "description": narration_ref.strip(),
            "withdrawal_amt": None if is_credit else amt1,
            "deposit_amt": amt1 if is_credit else None,
        })
        prev_balance = closing

    return pd.DataFrame(parsed_rows) if parsed_rows else None


def parse_hdfc_pdf(lines: list[str]) -> pd.DataFrame | None:
    """Parse HDFC bank PDF statements (single-line or vertical format)."""
    if not lines:
        return None

    haystack = " ".join(lines[:80]).lower()
    if "hdfc" not in haystack:
        return None

    # Try single-line format first (Date Narration Ref ValueDt Amt Amt)
    df = _parse_hdfc_single_line_format(lines)
    if df is not None and not df.empty:
        return df

    # Fall back to vertical format
    date_regex = re.compile(r"^(\d{2}/\d{2}/\d{2})$")
    amount_regex = re.compile(r"([\d,]+\.?\d*)")
    parsed_rows: list[dict[str, Any]] = []
    i = 0

    while i < len(lines):
        line = lines[i].strip()
        date_match = date_regex.match(line)
        if not date_match:
            i += 1
            continue

        txn_date = date_match.group(1)
        i += 1

        narration_parts: list[str] = []
        chq_ref_no: str | None = None
        value_date: str | None = None
        withdrawal_amt: float | None = None
        deposit_amt: float | None = None
        closing_balance: float | None = None

        while i < len(lines):
            candidate = lines[i].strip()

            if date_regex.match(candidate):
                break

            if not chq_ref_no and (candidate.startswith("UPI-") or candidate.startswith("0000") or re.match(r"^\d+", candidate)):
                chq_ref_no = candidate
                i += 1
                continue

            value_date_match = date_regex.match(candidate)
            if value_date_match and not value_date:
                value_date = value_date_match.group(1)
                i += 1
                continue

            amount_match = amount_regex.search(candidate)
            if amount_match and len(candidate.split()) <= 2:
                amount_val = amount_match.group(1).replace(",", "")
                try:
                    amount_float = float(amount_val)
                    if i + 1 < len(lines):
                        next_line = lines[i + 1].strip()
                        next_amount_match = amount_regex.search(next_line)
                        if next_amount_match and len(next_line.split()) <= 2:
                            balance_val = next_amount_match.group(1).replace(",", "")
                            try:
                                balance_float = float(balance_val)
                                closing_balance = balance_float

                                if parsed_rows:
                                    prev_balance = parsed_rows[-1].get("balance", 0) or 0
                                    if balance_float > prev_balance:
                                        deposit_amt = amount_float
                                    else:
                                        withdrawal_amt = amount_float
                                else:
                                    if amount_float > 0:
                                        deposit_amt = amount_float
                                    else:
                                        withdrawal_amt = abs(amount_float)

                                i += 2
                                break
                            except ValueError:
                                pass
                except ValueError:
                    pass

            if candidate and not date_regex.match(candidate):
                narration_parts.append(candidate)

            i += 1

        if txn_date and (withdrawal_amt is not None or deposit_amt is not None):
            description = " ".join(part.strip() for part in narration_parts if part.strip())

            row: dict[str, Any] = {
                "txn_date": value_date if value_date else txn_date,
                "description": description,
                "withdrawal_amt": withdrawal_amt,
                "deposit_amt": deposit_amt,
                "balance": closing_balance,
            }
            if chq_ref_no:
                row["raw_txn_id"] = chq_ref_no
            parsed_rows.append(row)

    if not parsed_rows:
        return None

    return pd.DataFrame(parsed_rows)
