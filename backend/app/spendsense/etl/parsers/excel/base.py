"""Shared base logic for Excel/CSV parsing."""

from __future__ import annotations

import io
from pathlib import Path

import pandas as pd  # type: ignore[import-untyped]

from ..common import SpendSenseParseError

try:
    from xlrd.biffh import XLRDError  # type: ignore[import-untyped]
except Exception:
    class XLRDError(Exception):  # type: ignore
        pass


def read_spreadsheet(data: bytes, filename: str) -> pd.DataFrame:
    """Read CSV/XLS/XLSX file into a raw DataFrame with no header assumed."""
    ext = Path(filename).suffix.lower()
    buffer = io.BytesIO(data)

    if ext == ".csv":
        buffer.seek(0)
        return pd.read_csv(buffer, header=None, keep_default_na=False)
    if ext in {".xls", ".xlsx"}:
        buffer.seek(0)
        try:
            return pd.read_excel(buffer, header=None, keep_default_na=False)
        except (ValueError, UnicodeDecodeError, XLRDError):
            df = _read_text_like_spreadsheet(data)
            if df is None:
                raise SpendSenseParseError(
                    f"Unable to read spreadsheet contents from {filename}. "
                    "Please upload a valid Excel/CSV export."
                )
            return df
    raise SpendSenseParseError(f"Unsupported Excel extension: {ext}")


def _read_text_like_spreadsheet(data: bytes) -> pd.DataFrame | None:
    """Fallback for XLS files that are actually tab/space-delimited text."""
    text = _decode_bytes(data)
    if not text.strip():
        return None
    rows: list[list[str]] = []
    has_tab = "\t" in text
    for raw_line in text.splitlines():
        if not raw_line.strip():
            continue
        if has_tab:
            cells = [cell.strip() for cell in raw_line.split("\t")]
        else:
            cells = [segment.strip() for segment in raw_line.split("  ") if segment.strip()]
            if not cells:
                cells = [raw_line.strip()]
        rows.append(cells)

    if not rows:
        return None

    max_cols = max(len(row) for row in rows)
    normalized = [row + [""] * (max_cols - len(row)) for row in rows]
    return pd.DataFrame(normalized)


def _decode_bytes(data: bytes) -> str:
    for encoding in ("utf-8", "latin-1"):
        try:
            return data.decode(encoding)
        except UnicodeDecodeError:
            continue
    return data.decode("utf-8", errors="ignore")
