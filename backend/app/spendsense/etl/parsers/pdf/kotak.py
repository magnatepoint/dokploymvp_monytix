"""Kotak Bank PDF statement line-based parser."""

from __future__ import annotations

import re
from typing import Any

import pandas as pd  # type: ignore[import-untyped]

# Format: DD-MM-YYYY <narration> <ref> amount(Dr/Cr) balance(Cr)
# Can span multiple lines when narration wraps (e.g. "Recd:IMPS/.../KKBK\n/X5508/20240\nIMPS-...")
_AMOUNT_BALANCE_RE = re.compile(
    r"([\d,.]+)\((Cr|Dr)\)\s+([\d,.]+)\((Cr|Dr)\)\s*$",
    re.IGNORECASE,
)
_DATE_START_RE = re.compile(r"^(\d{2}-\d{2}-\d{4})\s+")


def parse_kotak_pdf(lines: list[str]) -> pd.DataFrame | None:
    """Parse Kotak bank PDF statements.

    Format: DD-MM-YYYY <narration> <ref> amount(Dr/Cr) balance(Cr)
    Bank identified by 'kotak' or 'kkbk' (IFSC) in content.
    """
    if not lines:
        return None

    haystack = " ".join(line.lower() for line in lines[:50])
    if "kotak" not in haystack and "kkbk" not in haystack:
        return None

    parsed_rows: list[dict[str, Any]] = []
    i = 0

    while i < len(lines):
        line = lines[i]
        date_match = _DATE_START_RE.match(line)
        if not date_match:
            i += 1
            continue

        txn_date = date_match.group(1)
        # Collect lines until we find amount(Dr/Cr) balance(Cr) at end
        block_lines = [line]
        i += 1

        while i < len(lines):
            next_line = lines[i]
            # Stop if next line starts with a date (new transaction)
            if _DATE_START_RE.match(next_line):
                break
            block_lines.append(next_line)
            i += 1
            # Check if this block now ends with amount balance
            block = " ".join(block_lines)
            if _AMOUNT_BALANCE_RE.search(block):
                break

        block = " ".join(block_lines)
        match = _AMOUNT_BALANCE_RE.search(block)
        if not match:
            continue

        amount_val = match.group(1).replace(",", "")
        amount_dir = match.group(2).lower()
        balance_val = match.group(3).replace(",", "")
        balance_dir = match.group(4).lower()
        try:
            amount_float = float(amount_val)
            balance_float = float(balance_val)
        except ValueError:
            continue

        # Narration is everything between date and the first amount
        rest = block[: match.start()].strip()
        # Remove the date from start
        rest = _DATE_START_RE.sub("", rest, count=1).strip()
        description = rest if rest else "Transaction"

        row: dict[str, Any] = {
            "txn_date": txn_date,
            "description": description,
            "withdrawal_amt": None,
            "deposit_amt": None,
            "balance": balance_float if balance_dir == "cr" else -balance_float,
        }
        if amount_dir == "dr":
            row["withdrawal_amt"] = amount_float
        else:
            row["deposit_amt"] = amount_float

        parsed_rows.append(row)

    if not parsed_rows:
        return None

    return pd.DataFrame(parsed_rows)
