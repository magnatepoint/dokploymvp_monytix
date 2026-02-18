"""
Unit tests for financial_classification.classify.

Covers: credits (INCOME, TRANSFER_IN, OTHER), CRED debits, ACH/NACH, UPI, NEFT/IMPS, default.
"""
import sys
from pathlib import Path

backend_dir = Path(__file__).resolve().parent.parent.parent
sys.path.insert(0, str(backend_dir))

import pytest

from app.spendsense.services.financial_classification import (
    classify_financial,
    FinancialClassificationResult,
    to_dict,
)

# Priority ranks used in assertions (match module's _FINANCIAL_CLASS_RANK)
INCOME_RANK = 1
OTHER_RANK = 9


def test_credit_salary_income():
    """Credit with salary keyword -> INCOME."""
    r = classify_financial(
        channel_type="NEFT",
        cr_dr="C",
        direction="IN",
        raw_description="SALARY CREDIT JAN 2025",
        category_id=None,
        cat_l1=None,
    )
    assert r.financial_class == "INCOME"
    assert r.obligation_flag is False
    assert r.priority_rank == INCOME_RANK


def test_credit_transfer_in():
    """Credit with transfers_in category -> TRANSFER_IN."""
    r = classify_financial(
        channel_type="UPI",
        cr_dr="C",
        direction="IN",
        raw_description="UPI credit",
        category_id="transfers_in",
        cat_l1="transfer",
    )
    assert r.financial_class == "TRANSFER_IN"
    assert r.obligation_flag is False


def test_credit_other():
    """Credit with no strong signal -> OTHER."""
    r = classify_financial(
        channel_type="UPI",
        cr_dr="C",
        direction="IN",
        raw_description="Random refund",
        category_id=None,
        cat_l1=None,
    )
    assert r.financial_class == "OTHER"
    assert r.priority_rank == OTHER_RANK


def test_cred_insurance():
    """CRED.INSURANCE debit -> INSURANCE."""
    r = classify_financial(
        channel_type="UPI",
        cr_dr="D",
        direction="OUT",
        raw_description="CRED.INSURANCE PREMIUM PAYMENT",
        category_id=None,
        cat_l1=None,
        amount=500,
    )
    assert r.financial_class == "INSURANCE"
    assert r.obligation_flag is True
    assert r.instrument_type == "INSURANCE_PREMIUM"


def test_cred_club_loan_emi():
    """CRED.CLUB / CRED CASH -> LOAN_EMI."""
    r = classify_financial(
        channel_type="UPI",
        cr_dr="D",
        direction="OUT",
        raw_description="CRED CASH EMI DEBIT",
        category_id=None,
        cat_l1=None,
        amount=2000,
    )
    assert r.financial_class == "LOAN_EMI"
    assert r.obligation_flag is True


def test_credpay_merchant():
    """CREDPAY.* -> MERCHANT_PAYMENT."""
    r = classify_financial(
        channel_type="UPI",
        cr_dr="D",
        direction="OUT",
        raw_description="CREDPAY.SWIGGY PAYMENT",
        category_id="credit_cards",
        cat_l1="wants",
        amount=300,
    )
    assert r.financial_class == "MERCHANT_PAYMENT"
    assert r.obligation_flag is False
    assert r.counterparty_type == "MERCHANT"


def test_cred_generic_credit_card():
    """Generic CRED with amount >= 1000 -> CREDIT_CARD_PAYMENT."""
    r = classify_financial(
        channel_type="UPI",
        cr_dr="D",
        direction="OUT",
        raw_description="CRED BILL PAYMENT",
        category_id=None,
        cat_l1=None,
        amount=5000,
    )
    assert r.financial_class == "CREDIT_CARD_PAYMENT"
    assert r.obligation_flag is True


def test_ach_nach_emi():
    """ACH/NACH with EMI/loan keywords -> LOAN_EMI."""
    r = classify_financial(
        channel_type="ACH",
        cr_dr="D",
        direction="OUT",
        raw_description="HDFC BANK EMI DEBIT",
        counterparty_name="HDFC",
        category_id=None,
        cat_l1=None,
        is_loan_payment=False,
        is_investment=False,
    )
    assert r.financial_class == "LOAN_EMI"
    assert r.obligation_flag is True


def test_ach_nach_investment():
    """ACH/NACH with MF/SIP keywords -> INVESTMENT."""
    r = classify_financial(
        channel_type="NACH",
        cr_dr="D",
        direction="OUT",
        raw_description="HDFCMF SIP DEBIT",
        counterparty_name="CAMS",
        category_id=None,
        cat_l1=None,
        is_loan_payment=False,
        is_investment=False,
    )
    assert r.financial_class == "INVESTMENT"
    assert r.obligation_flag is True


def test_upi_merchant():
    """UPI debit with merchant_flag -> MERCHANT_PAYMENT."""
    r = classify_financial(
        channel_type="UPI",
        cr_dr="D",
        direction="OUT",
        raw_description="AMAZON UPI",
        category_id="shopping",
        cat_l1="wants",
        merchant_flag=True,
    )
    assert r.financial_class == "MERCHANT_PAYMENT"
    assert r.instrument_type == "MERCHANT_UPI"


def test_upi_p2p():
    """UPI debit without merchant_flag -> TRANSFER_OUT (P2P)."""
    r = classify_financial(
        channel_type="UPI",
        cr_dr="D",
        direction="OUT",
        raw_description="UPI transfer",
        category_id="transfers_out",
        cat_l1="transfer",
        merchant_flag=False,
    )
    assert r.financial_class == "TRANSFER_OUT"
    assert r.instrument_type == "P2P_TRANSFER"
    assert r.counterparty_type == "INDIVIDUAL"


def test_default_credit_card():
    """Default path with is_card_payment -> CREDIT_CARD_PAYMENT."""
    r = classify_financial(
        channel_type="POS",
        cr_dr="D",
        direction="OUT",
        raw_description="CARD PAYMENT",
        category_id="credit_cards",
        cat_l1="obligation",
        is_card_payment=True,
        merchant_flag=False,
    )
    assert r.financial_class == "CREDIT_CARD_PAYMENT"
    assert r.obligation_flag is True


def test_to_dict():
    """to_dict returns dict with all five fields for DB insert."""
    res = FinancialClassificationResult(
        financial_class="INCOME",
        obligation_flag=False,
        instrument_type="SALARY",
        counterparty_type="BANK",
        priority_rank=1,
    )
    row = to_dict(res)
    assert row["financial_class"] == "INCOME"
    assert row["obligation_flag"] is False
    assert row["instrument_type"] == "SALARY"
    assert row["counterparty_type"] == "BANK"
    assert row["priority_rank"] == 1
