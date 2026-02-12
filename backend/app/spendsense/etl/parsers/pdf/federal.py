"""Federal Bank PDF statement line-based parser."""

from __future__ import annotations

import re
from datetime import date
from typing import Any

import pandas as pd  # type: ignore[import-untyped]

# Federal uses "DD MMM" format; year inferred from statement period (Apr–Mar)
MONTH_TO_NUM = {
    "jan": "01", "feb": "02", "mar": "03", "apr": "04", "may": "05", "jun": "06",
    "jul": "07", "aug": "08", "sep": "09", "oct": "10", "nov": "11", "dec": "12",
}


def parse_federal_pdf(lines: list[str]) -> pd.DataFrame | None:
    """Parse Federal Bank PDF statements. Format: Date (DD MMM), Description, Amount Balance."""
    if not lines:
        return None

    haystack = " ".join(lines[:100]).lower()
    if "federal" not in haystack and "fdrl" not in haystack:
        return None

    # Infer year from statement period (e.g. "1 April 2025 to 31 March 2026")
    start_year = 2025
    end_year = 2026
    for line in lines[:60]:
        m = re.search(r"(?:1\s+)?(?:April|Apr)\s+(\d{4})", line, re.I)
        if m:
            start_year = int(m.group(1))
            break
    for line in lines[:60]:
        m = re.search(r"(?:31\s+)?(?:March|Mar)\s+(\d{4})", line, re.I)
        if m:
            end_year = int(m.group(1))
            break

    date_regex = re.compile(r"^(\d{2})\s+(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\s*$", re.I)
    # Amount and balance: same line "5,000.00 17,860.76" OR separate lines
    amount_balance_regex = re.compile(r"^([\d,]+\.?\d*)\s+([\d,]+\.?\d*)\s*$")
    single_amount_regex = re.compile(r"^([\d,]+\.?\d*)\s*$")

    # Direction from description: credit vs debit
    def _is_credit(desc: str) -> bool:
        lower = desc.lower()
        return "upi in" in lower or "upi in/" in lower or "upiin" in lower

    parsed_rows: list[dict[str, Any]] = []
    i = 0

    while i < len(lines):
        line = lines[i].strip()
        date_match = date_regex.match(line)
        if not date_match:
            i += 1
            continue

        day = date_match.group(1)
        mon = date_match.group(2)
        mon_num = MONTH_TO_NUM.get(mon.lower(), "01")
        year = start_year if mon.lower() in ("apr", "may", "jun", "jul", "aug", "sep", "oct", "nov", "dec") else end_year
        txn_date = date(year, int(mon_num), int(day))
        i += 1

        description_parts: list[str] = []
        withdrawal_amt: float | None = None
        deposit_amt: float | None = None

        while i < len(lines):
            candidate = lines[i].strip()

            if date_regex.match(candidate):
                break

            # Skip pure numeric ref lines (0, 0000, 5411) - must be before amount matching
            if re.match(r"^\d{1,6}$", candidate):
                i += 1
                continue

            # Same line: "5,000.00 17,860.76"
            ab_match = amount_balance_regex.match(candidate)
            if ab_match:
                amount_val = ab_match.group(1).replace(",", "")
                try:
                    amount_float = float(amount_val)
                    desc = " ".join(part.strip() for part in description_parts if part.strip())
                    if _is_credit(desc):
                        deposit_amt = amount_float
                    else:
                        withdrawal_amt = amount_float
                    i += 1
                    break
                except ValueError:
                    pass

            # Separate lines: "5,000.00" then "17,860.76" (amount then balance)
            amt_match = single_amount_regex.match(candidate)
            if amt_match and description_parts and i + 1 < len(lines):
                next_line = lines[i + 1].strip()
                if single_amount_regex.match(next_line):
                    amount_val = amt_match.group(1).replace(",", "")
                    try:
                        amount_float = float(amount_val)
                        desc = " ".join(part.strip() for part in description_parts if part.strip())
                        if _is_credit(desc):
                            deposit_amt = amount_float
                        else:
                            withdrawal_amt = amount_float
                        i += 2
                        break
                    except ValueError:
                        pass

            # Skip pure numeric ref lines like "0", "0000", "5411" between description and amount
            if re.match(r"^\d{1,6}$", candidate):
                i += 1
                continue

            if candidate:
                description_parts.append(candidate)

            i += 1

        if withdrawal_amt is not None or deposit_amt is not None:
            description = " ".join(part.strip() for part in description_parts if part.strip())
            parsed_rows.append({
                "txn_date": txn_date,
                "description": description,
                "withdrawal_amt": withdrawal_amt,
                "deposit_amt": deposit_amt,
            })

    if not parsed_rows:
        return None

    return pd.DataFrame(parsed_rows)
