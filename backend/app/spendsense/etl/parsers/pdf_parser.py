"""PDF bank statement parser - orchestrates table extraction and bank-specific fallback parsers."""

from __future__ import annotations

import io
import logging
from typing import Any

import pdfplumber  # type: ignore[import-untyped]
from pdfplumber.utils.exceptions import PdfminerException  # type: ignore[import-untyped]

try:
    from pdfminer.pdfdocument import PDFPasswordIncorrect as PDFPasswordError  # type: ignore[import-untyped]
except ImportError:
    PDFPasswordError = Exception  # type: ignore[assignment]

try:
    import fitz  # type: ignore[import-untyped]
except ImportError:
    fitz = None  # type: ignore[assignment]

import pandas as pd  # type: ignore[import-untyped]

from .common import SpendSenseParseError, dataframe_to_records, infer_bank_code, structure_dataframe
from .pdf import BANK_PARSERS

logger = logging.getLogger(__name__)

TABLE_SETTING_PRESETS: list[dict[str, Any] | None] = [
    None,  # default behaviour
    {
        "vertical_strategy": "lines",
        "horizontal_strategy": "lines",
        "intersection_tolerance": 5,
        "snap_tolerance": 3,
        "join_tolerance": 3,
        "edge_min_length": 3,
        "min_words_vertical": 1,
        "min_words_horizontal": 1,
    },
    {
        "vertical_strategy": "lines",
        "horizontal_strategy": "text",
        "intersection_tolerance": 5,
        "snap_tolerance": 3,
        "join_tolerance": 3,
        "min_words_vertical": 1,
        "min_words_horizontal": 1,
        "text_tolerance": 3,
    },
    {
        "vertical_strategy": "text",
        "horizontal_strategy": "text",
        "intersection_tolerance": 5,
        "snap_tolerance": 2,
        "text_tolerance": 3,
    },
]


def _extract_pdf_tables(buffer: io.BytesIO, password: str | None = None) -> pd.DataFrame:
    """Extract tables from PDF. Tries all presets per page and keeps the result with max rows.

    Plan fix: Previously the first preset that returned tables won; that preset often
    produces merged cells (one date spanning many rows). Now we try all presets and
    choose the one yielding the most data rows per page.
    """
    all_rows: list[list[str]] = []
    total_pages = 0
    pages_with_tables = 0

    try:
        with pdfplumber.open(buffer, password=password) as pdf:
            total_pages = len(pdf.pages)
            for page_num, page in enumerate(pdf.pages, 1):
                best_rows: list[list[str]] = []
                best_preset_idx: int | None = None

                for preset_idx, settings in enumerate(TABLE_SETTING_PRESETS):
                    if settings is None:
                        tables = page.extract_tables() or []
                    else:
                        tables = page.extract_tables(table_settings=settings) or []

                    page_rows: list[list[str]] = []
                    for table in tables:
                        if not table:
                            continue
                        for raw_row in table:
                            if not raw_row:
                                continue
                            cleaned_row: list[str] = []
                            for cell in raw_row:
                                if cell is None:
                                    cleaned_row.append("")
                                else:
                                    cleaned_row.append(str(cell).strip())
                            if any(value for value in cleaned_row):
                                page_rows.append(cleaned_row)

                    if len(page_rows) > len(best_rows):
                        best_rows = page_rows
                        best_preset_idx = preset_idx

                if best_rows:
                    pages_with_tables += 1
                    logger.debug(
                        "PDF page %d: preset %s yielded %d rows",
                        page_num,
                        "default" if best_preset_idx == 0 else best_preset_idx,
                        len(best_rows),
                    )
                    all_rows.extend(best_rows)
    except PDFPasswordError:
        if password:
            raise SpendSenseParseError("Incorrect PDF password. Please re-enter and try again.")
        raise SpendSenseParseError("PDF is password protected. Enter the password before uploading.")
    except PdfminerException as exc:
        if exc.args and isinstance(exc.args[0], PDFPasswordError):
            if password:
                raise SpendSenseParseError("Incorrect PDF password. Please re-enter and try again.")
            raise SpendSenseParseError("PDF is password protected. Enter the password before uploading.")
        raise SpendSenseParseError(f"Unable to read PDF file: {exc}")

    if not all_rows:
        error_msg = (
            f"No tabular data found in PDF file. "
            f"Processed {total_pages} page(s), found tables on {pages_with_tables} page(s). "
            f"The PDF may be scanned (image-based) or use a non-standard format. "
            f"Please ensure the PDF contains extractable text and tables."
        )
        raise SpendSenseParseError(error_msg)

    logger.info("PDF table extraction: %d total rows from %d pages", len(all_rows), pages_with_tables)
    max_cols = max(len(row) for row in all_rows)
    normalized_rows = [row + [""] * (max_cols - len(row)) for row in all_rows]
    return pd.DataFrame(normalized_rows)


def _extract_lines_with_pdfplumber(buffer: io.BytesIO, password: str | None = None) -> list[str] | None:
    """Extract text lines using pdfplumber (already handles password-protected PDFs)."""
    buffer.seek(0)
    try:
        with pdfplumber.open(buffer, password=password) as pdf:
            lines: list[str] = []
            total_text_length = 0
            for page in pdf.pages:
                text = page.extract_text() or ""
                total_text_length += len(text)
                for line in text.splitlines():
                    stripped = line.strip()
                    if stripped:
                        lines.append(stripped)
            if total_text_length < 100 and len(lines) < 10:
                return None
            return lines if lines else None
    except Exception:
        return None


def _extract_lines_with_pymupdf(buffer: io.BytesIO, password: str | None = None) -> list[str] | None:
    if fitz is None:
        return None
    buffer.seek(0)
    try:
        doc = fitz.open(stream=buffer.read(), filetype="pdf")
    except Exception:
        return None

    # Always try authenticate when password provided; some encrypted PDFs don't set needs_pass until content access
    if password and password.strip():
        auth_ok = doc.authenticate(password.strip())
        if not auth_ok:
            doc.close()
            raise SpendSenseParseError("Incorrect PDF password. Please re-enter and try again.")
    else:
        needs_pass = bool(getattr(doc, "needs_pass", False))
        if needs_pass:
            doc.close()
            raise SpendSenseParseError("PDF is password protected. Enter the password before uploading.")

    lines: list[str] = []
    total_text_length = 0
    try:
        for page in doc:
            text = page.get_text("text") or ""
            total_text_length += len(text)
            for line in text.splitlines():
                stripped = line.strip()
                if stripped:
                    lines.append(stripped)
    except ValueError as e:
        doc.close()
        if "encrypted" in str(e).lower() or "closed" in str(e).lower():
            raise SpendSenseParseError(
                "PDF is password protected. Enter the password before uploading."
            ) from e
        raise
    finally:
        doc.close()

    if total_text_length < 100 and len(lines) < 10:
        return None

    return lines if lines else None


def parse_pdf_file(data: bytes, filename: str, password: str | None = None) -> list[dict[str, Any]]:
    """Parse PDF bank statement into normalized transaction records."""
    buffer = io.BytesIO(data)
    bank_code = infer_bank_code(filename)
    lines: list[str] | None = None

    # If filename doesn't hint the bank, extract text and infer from content (headers often have bank name)
    if not bank_code:
        lines = _extract_lines_with_pdfplumber(buffer, password=password)
        if not lines:
            lines = _extract_lines_with_pymupdf(buffer, password=password)
        if lines:
            sample = " ".join(lines[:80])  # Headers/footers usually in first pages
            bank_code = infer_bank_code(filename, sample_text=sample)
            buffer.seek(0)

    # SBI, Canara, Axis, Kotak: table extraction produces poor results. Prefer text-based parsers.
    if bank_code in ("sbi_bank", "canara_bank", "axis_bank", "kotak_bank"):
        logger.info("Using line-based parser for %s (bank=%s)", filename, bank_code)
        if not lines:
            lines = _extract_lines_with_pdfplumber(buffer, password=password)
        if not lines:
            lines = _extract_lines_with_pymupdf(buffer, password=password)
        if lines:
            parser_map = {"sbi_bank": "SBI", "canara_bank": "Canara", "axis_bank": "Axis", "kotak_bank": "Kotak"}
            target = parser_map.get(bank_code)
            if target:
                for bank_name, parser in BANK_PARSERS:
                    if bank_name == target:
                        try:
                            df = parser(lines)
                            if df is not None and not df.empty:
                                logger.info("Parsed %s using %s line-based parser (%d transactions)", filename, target, len(df))
                                return dataframe_to_records(df, bank_code=bank_code)
                            if df is not None and df.empty:
                                logger.warning("%s parser returned empty for %s, falling back to table extraction", target, filename)
                        except Exception as e:
                            logger.warning("%s parser failed for %s: %s", target, filename, e)
                        break
        buffer.seek(0)

    try:
        df_raw = _extract_pdf_tables(buffer, password)
        logger.debug("parse_pdf_file: extracted %d raw rows from tables", len(df_raw))
        df = structure_dataframe(df_raw, is_pdf=True)
        return dataframe_to_records(df, bank_code=bank_code)
    except SpendSenseParseError as primary_error:
        logger.info("Table extraction failed for %s, attempting text extraction fallback: %s", filename, primary_error)
        # Try pdfplumber first (handles password-protected PDFs); then PyMuPDF
        lines = _extract_lines_with_pdfplumber(buffer, password=password)
        if not lines:
            lines = _extract_lines_with_pymupdf(buffer, password=password)
        if lines:
            logger.info("Extracted %d lines from PDF, trying bank-specific parsers", len(lines))
            # When filename doesn't hint the bank, infer_bank_code is None. Use parser's bank.
            _PARSER_TO_CODE = {"Axis": "axis_bank", "SBI": "sbi_bank", "Canara": "canara_bank", "HDFC": "hdfc_bank", "ICICI": "icici_bank", "Kotak": "kotak_bank", "Federal": "federal_bank"}
            for bank_name, parser in BANK_PARSERS:
                try:
                    df = parser(lines)
                    if df is not None and not df.empty:
                        logger.info("Successfully parsed %s using %s parser", filename, bank_name)
                        effective_bank_code = bank_code or _PARSER_TO_CODE.get(bank_name)
                        return dataframe_to_records(df, bank_code=effective_bank_code)
                except Exception as e:
                    logger.debug("%s parser failed: %s", bank_name, e)
                    continue

            logger.warning(
                "All parsing methods failed for %s. Extracted %d lines but no bank parser matched. Original: %s",
                filename,
                len(lines),
                primary_error,
            )
        else:
            logger.warning(
                "Text extraction fallback failed for %s. PDF may be corrupted or image-based. Original: %s",
                filename,
                primary_error,
            )
        raise primary_error
