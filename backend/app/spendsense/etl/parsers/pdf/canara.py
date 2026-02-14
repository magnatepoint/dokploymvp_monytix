"""Canara Bank PDF statement line-based parser."""

from __future__ import annotations

import re
from datetime import datetime
from typing import Any

import pandas as pd  # type: ignore[import-untyped]


def parse_canara_pdf(lines: list[str]) -> pd.DataFrame | None:
    """Parse Canara Bank PDF statements. Format: Date, Particulars, Deposits/Withdrawals, Balance."""
    if not lines:
        return None

    haystack = " ".join(lines[:80]).lower()
    if "canara" not in haystack and "cnrb" not in haystack:
        return None
    if "statement for" not in haystack and "particulars" not in haystack:
        return None

    # Line with date and amount+balance at end: "DD-MM-YYYY [desc] AMOUNT BALANCE"
    # Try with optional desc first (for "DD-MM-YYYY AMOUNT BALANCE"), then with desc
    amount_line_with_desc = re.compile(
        r"^(\d{2}-\d{2}-\d{4})\s+(.+?)\s+([\d,]+\.?\d*)\s+([\d,]+\.?\d*)\s*$"
    )
    amount_line_no_desc = re.compile(
        r"^(\d{2}-\d{2}-\d{4})\s+([\d,]+\.?\d*)\s+([\d,]+\.?\d*)\s*$"
    )
    date_regex = re.compile(r"^(\d{2}-\d{2}-\d{4})\b")

    def _skip_line(s: str) -> bool:
        return bool(
            re.match(r"^--\s+\d+\s+of\s+\d+\s+--\s*$", s)
            or re.match(r"^Date\s+Particulars\s+Deposits", s, re.I)
            or re.match(r"^Opening\s+Balance", s, re.I)
            or re.match(r"^page\s+\d+\s*$", s, re.I)
            or re.match(r"^Chq:\s*$", s)
            or re.match(r"^Chq:\s*\d+$", s)
        )

    def _is_section_start(s: str) -> bool:
        return bool(
            re.match(r"^Date\s+Particulars\s+Deposits", s, re.I)
            or re.match(r"^Opening\s+Balance", s, re.I)
        )

    def _is_credit(desc: str) -> bool:
        upper = desc.upper()
        if "MOB-IMPS-CR" in upper or "IMPS-CR" in upper:
            return True
        if "UPI/CR" in upper or "UPI CR" in upper:
            return True
        if "SBINT" in upper:
            return True
        if "INET-IMPS-CR" in upper or "INET-IMPS CR" in upper:
            return True
        if "NEFT CR" in upper or "NEFT-CR" in upper or "RTGS CR" in upper or "RTGS-CR" in upper:
            return True
        if "CR/" in upper and "DR/" not in upper[: max(0, upper.find("CR/") - 3)]:
            return True
        return False

    parsed_rows: list[dict[str, Any]] = []
    description_parts: list[str] = []
    in_section = False
    i = 0

    while i < len(lines):
        line = lines[i].strip()

        if _is_section_start(line):
            in_section = True
        if _skip_line(line):
            i += 1
            continue
        if not in_section:
            i += 1
            continue

        # Amount line: "DD-MM-YYYY ... AMOUNT BALANCE" or "DD-MM-YYYY AMOUNT BALANCE"
        amt_match = amount_line_with_desc.match(line)
        if not amt_match:
            amt_match = amount_line_no_desc.match(line)
        if amt_match:
            date_str = amt_match.group(1)
            if len(amt_match.groups()) >= 4:
                desc_part = amt_match.group(2).strip()
                amount_val = amt_match.group(3).replace(",", "")
            else:
                desc_part = ""
                amount_val = amt_match.group(2).replace(",", "")
            try:
                amount_float = float(amount_val)
                dt = datetime.strptime(date_str, "%d-%m-%Y")
                txn_date = dt.date()

                # Add desc_part (middle of amount line) if it's narrative, not amount/balance
                if desc_part and not re.match(r"^[\d,\.\s]+$", desc_part):
                    if not re.search(r"[\d,]+\.?\d*\s+[\d,]+\.?\d*\s*$", desc_part):
                        description_parts.append(desc_part)

                # Prune fragments that look like amount lines from concatenation
                parts_clean = []
                for p in description_parts:
                    p = p.strip()
                    if not p:
                        continue
                    if re.match(r"^\d{2}-\d{2}-\d{4}\s+[\d,]+\.?\d*\s+[\d,]+\.?\d*$", p):
                        continue
                    parts_clean.append(p)
                description = " ".join(parts_clean)

                if _is_credit(description):
                    parsed_rows.append({
                        "txn_date": txn_date,
                        "description": description,
                        "withdrawal_amt": None,
                        "deposit_amt": amount_float,
                    })
                else:
                    parsed_rows.append({
                        "txn_date": txn_date,
                        "description": description,
                        "withdrawal_amt": amount_float,
                        "deposit_amt": None,
                    })
            except ValueError:
                pass

            description_parts = []
            i += 1
            continue

        # Date-only line: "DD-MM-YYYY" - start of new block (amount on next line)
        if date_regex.match(line) and not amount_line_with_desc.match(line) and not amount_line_no_desc.match(line):
            rest = line[date_regex.match(line).end() :].strip()
            if rest:
                description_parts.append(rest)
            i += 1
            continue

        # Description continuation
        if line and not re.match(r"^Chq:\s*\d*$", line):
            description_parts.append(line)
        i += 1

    if not parsed_rows:
        return None

    return pd.DataFrame(parsed_rows)
