"""ICICI Bank PDF statement line-based parser (fallback when table extraction fails)."""

from __future__ import annotations

import re
from typing import Any

import pandas as pd  # type: ignore[import-untyped]


def parse_icici_pdf(lines: list[str]) -> pd.DataFrame | None:
    """Parse ICICI bank PDF statements as fallback when generic extraction fails."""
    if not lines:
        return None

    # Detect ICICI bank
    if not any("icici" in line.lower() for line in lines[:50]):
        return None

    # Support multiple date formats: DD/MM/YYYY, DD-MM-YYYY, D/M/YYYY
    date_regexes = [
        re.compile(r"^(\d{2}/\d{2}/\d{4})$"),
        re.compile(r"^(\d{2}-\d{2}-\d{4})$"),
        re.compile(r"^(\d{1,2}/\d{1,2}/\d{4})$"),
        re.compile(r"^(\d{1,2}-\d{1,2}-\d{4})$"),
    ]

    def _match_date(line: str) -> str | None:
        for regex in date_regexes:
            m = regex.match(line.strip())
            if m:
                return m.group(1)
        return None

    # Amount format: numbers with commas (e.g., 21.00, 3,035.00)
    amount_regex = re.compile(r"([\d,]+\.?\d*)")
    parsed_rows: list[dict[str, Any]] = []
    i = 0

    # Look for transaction table start - flexible header detection
    header_found = False
    for j, line in enumerate(lines[:100]):
        lower = line.lower()
        if "value date" in lower or "transaction date" in lower or "valuedate" in lower or "transactiondate" in lower:
            header_found = True
            i = j + 1
            break

    if not header_found:
        i = 0

    while i < len(lines):
        line = lines[i].strip()
        txn_date = _match_date(line)
        if not txn_date:
            i += 1
            continue

        # Found a date - could be Value Date or Transaction Date
        first_date = txn_date
        i += 1

        # Look for second date (Transaction Date if first was Value Date)
        second_date: str | None = None
        if i < len(lines):
            sd = _match_date(lines[i])
            if sd:
                second_date = sd
                i += 1

        # Use Transaction Date if available, otherwise Value Date
        txn_date = second_date if second_date else first_date

        # Collect description/remarks (may span multiple lines)
        description_parts: list[str] = []
        withdrawal_amt: float | None = None
        deposit_amt: float | None = None
        balance: float | None = None

        # Look for amounts and description - support 2-line (amount+balance) and 3-line patterns
        while i < len(lines):
            candidate = lines[i].strip()

            # Check if we hit the next transaction (new date)
            if _match_date(candidate):
                break

            # Check for amount (withdrawal or deposit)
            amount_match = amount_regex.search(candidate)
            if amount_match:
                amount_val = amount_match.group(1).replace(",", "")
                try:
                    amount_float = float(amount_val)

                    # Try 3-line pattern: withdrawal/deposit, next amount, balance
                    if i + 1 < len(lines):
                        next_candidate = lines[i + 1].strip()
                        next_amount_match = amount_regex.search(next_candidate)
                        if next_amount_match:
                            next_amount_val = next_amount_match.group(1).replace(",", "")
                            try:
                                next_amount_float = float(next_amount_val)

                                # 3-line: amount, amount, balance
                                if i + 2 < len(lines):
                                    balance_candidate = lines[i + 2].strip()
                                    balance_match = amount_regex.search(balance_candidate)
                                    if balance_match:
                                        balance_val = balance_match.group(1).replace(",", "")
                                        try:
                                            balance = float(balance_val.replace(",", ""))
                                            if not withdrawal_amt and not deposit_amt:
                                                if parsed_rows:
                                                    prev_balance = parsed_rows[-1].get("balance", 0) or 0
                                                    if balance > prev_balance:
                                                        deposit_amt = amount_float
                                                    else:
                                                        withdrawal_amt = amount_float
                                                else:
                                                    withdrawal_amt = amount_float
                                                i += 3
                                                break
                                        except ValueError:
                                            pass

                                # 2-line: amount + balance (single withdrawal or deposit column)
                                if not withdrawal_amt and not deposit_amt:
                                    balance = next_amount_float
                                    if parsed_rows:
                                        prev_balance = parsed_rows[-1].get("balance", 0) or 0
                                        if balance > prev_balance:
                                            deposit_amt = amount_float
                                        else:
                                            withdrawal_amt = amount_float
                                    else:
                                        withdrawal_amt = amount_float
                                    i += 2
                                    break
                            except ValueError:
                                pass

                    # 1-line fallback: amount only (no balance) - still record
                    if not withdrawal_amt and not deposit_amt:
                        withdrawal_amt = amount_float
                        i += 1
                        break
                except ValueError:
                    description_parts.append(candidate)
            else:
                description_parts.append(candidate)

            i += 1

        # Build transaction row
        if txn_date:
            description = " ".join(part.strip() for part in description_parts if part.strip())

            if withdrawal_amt is not None or deposit_amt is not None or description:
                row: dict[str, Any] = {
                    "txn_date": txn_date,
                    "description": description,
                    "withdrawal_amt": withdrawal_amt,
                    "deposit_amt": deposit_amt,
                    "balance": balance,
                }
                parsed_rows.append(row)

    if not parsed_rows:
        return None

    return pd.DataFrame(parsed_rows)
