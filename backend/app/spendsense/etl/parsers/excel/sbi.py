"""SBI Bank Excel/CSV statement parser."""

from __future__ import annotations

import pandas as pd  # type: ignore[import-untyped]

from ..common import dataframe_to_records, infer_bank_code, structure_dataframe


def parse_sbi_excel(df_raw: pd.DataFrame, filename: str) -> list[dict]:
    """Parse SBI bank Excel/CSV statement. Uses generic structure detection."""
    sample_text = " ".join(df_raw.head(5).astype(str).values.ravel().tolist())
    bank_code = infer_bank_code(filename, sample_text) or "sbi_bank"
    df = structure_dataframe(df_raw, is_pdf=False)
    return dataframe_to_records(df, bank_code=bank_code)
