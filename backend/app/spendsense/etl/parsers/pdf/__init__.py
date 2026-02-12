"""PDF bank statement parsers - one module per bank for easy maintenance."""

from .icici import parse_icici_pdf
from .hdfc import parse_hdfc_pdf
from .kotak import parse_kotak_pdf
from .federal import parse_federal_pdf
from .axis import parse_axis_pdf
from .sbi import parse_sbi_pdf
from .canara import parse_canara_pdf

BANK_PARSERS = [
    ("Kotak", parse_kotak_pdf),
    ("HDFC", parse_hdfc_pdf),
    ("ICICI", parse_icici_pdf),
    ("Federal", parse_federal_pdf),
    ("Axis", parse_axis_pdf),
    ("SBI", parse_sbi_pdf),
    ("Canara", parse_canara_pdf),
]

__all__ = ["parse_icici_pdf", "parse_hdfc_pdf", "parse_kotak_pdf", "parse_federal_pdf", "parse_axis_pdf", "parse_sbi_pdf", "parse_canara_pdf", "BANK_PARSERS"]
