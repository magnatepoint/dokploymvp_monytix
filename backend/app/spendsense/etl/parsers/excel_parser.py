"""Excel/CSV bank statement parser - orchestrates bank-specific parsers."""

from __future__ import annotations

from pathlib import Path

from .common import SpendSenseParseError, dataframe_to_records, infer_bank_code, structure_dataframe
from .excel import BANK_EXCEL_PARSERS, read_spreadsheet


def parse_excel_file(data: bytes, filename: str) -> list[dict]:
    """Parse CSV/XLS/XLSX statements into normalized records."""
    df_raw = read_spreadsheet(data, filename)
    sample_text = " ".join(df_raw.head(5).astype(str).values.ravel().tolist())
    bank_code = infer_bank_code(filename, sample_text)

    # Try bank-specific parser if we have one
    if bank_code:
        bank_lower = bank_code.lower()
        for _name, keyword, parser in BANK_EXCEL_PARSERS:
            if keyword in bank_lower or keyword in filename.lower():
                try:
                    return parser(df_raw, filename)
                except SpendSenseParseError:
                    raise
                except Exception:
                    pass  # Fall through to generic

    # Generic fallback
    bank_code = infer_bank_code(filename, sample_text)
    df = structure_dataframe(df_raw, is_pdf=False)
    return dataframe_to_records(df, bank_code=bank_code)
