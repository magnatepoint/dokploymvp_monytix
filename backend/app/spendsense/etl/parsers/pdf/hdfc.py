"""HDFC Bank PDF statement line-based parser."""

from __future__ import annotations

import re
from typing import Any

import pandas as pd  # type: ignore[import-untyped]


def parse_hdfc_pdf(lines: list[str]) -> pd.DataFrame | None:
    """Parse HDFC bank PDF statements with vertical transaction format."""
    if not lines:
        return None

    if not any("hdfc" in line.lower() for line in lines[:50]):
        return None

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
