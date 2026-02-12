"""Excel/CSV bank statement parsers - one module per bank for easy maintenance."""

from .base import read_spreadsheet
from .icici import parse_icici_excel
from .hdfc import parse_hdfc_excel
from .sbi import parse_sbi_excel
from .federal import parse_federal_excel

BANK_EXCEL_PARSERS = [
    ("ICICI", "icici", parse_icici_excel),
    ("HDFC", "hdfc", parse_hdfc_excel),
    ("SBI", "sbi", parse_sbi_excel),
    ("Federal", "federal", parse_federal_excel),
]

__all__ = [
    "read_spreadsheet",
    "parse_icici_excel",
    "parse_hdfc_excel",
    "parse_sbi_excel",
    "parse_federal_excel",
    "BANK_EXCEL_PARSERS",
]
