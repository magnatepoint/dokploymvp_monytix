"""Kotak Bank PDF statement line-based parser."""

from __future__ import annotations

import re
from typing import Any

import pandas as pd  # type: ignore[import-untyped]


def parse_kotak_pdf(lines: list[str]) -> pd.DataFrame | None:
    """Parse Kotak bank PDF statements."""
    if not lines:
        return None

    if not any("kotak" in line.lower() for line in lines):
        return None

    date_regex = re.compile(r"^(\d{2}-\d{2}-\d{4})$")
    amount_regex = re.compile(r"([\d,.]+)\((Cr|Dr)\)", re.IGNORECASE)
    parsed_rows: list[dict[str, Any]] = []
    i = 0

    while i < len(lines):
        line = lines[i]
        date_match = date_regex.match(line)
        if not date_match:
            i += 1
            continue

        txn_date = date_match.group(1)
        i += 1
        narration_parts: list[str] = []

        amount_line = None
        while i < len(lines):
            candidate = lines[i]
            if amount_regex.search(candidate):
                amount_line = candidate
                break
            if date_regex.match(candidate):
                break
            narration_parts.append(candidate)
            i += 1

        if amount_line is None or i >= len(lines):
            continue

        amount_match = amount_regex.search(amount_line)
        balance_line_index = i + 1
        if not amount_match or balance_line_index >= len(lines):
            i += 1
            continue

        balance_line = lines[balance_line_index]
        balance_match = amount_regex.search(balance_line)
        if not balance_match:
            i += 1
            continue

        description = " ".join(part.strip() for part in narration_parts if part.strip())
        amount_val = amount_match.group(1).replace(",", "")
        balance_val = balance_match.group(1).replace(",", "")
        try:
            amount_float = float(amount_val)
            balance_float = float(balance_val)
        except ValueError:
            i += 1
            continue

        amount_dir = amount_match.group(2).lower()
        balance_dir = balance_match.group(2).lower()

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
        i = balance_line_index + 1

    if not parsed_rows:
        return None

    return pd.DataFrame(parsed_rows)
