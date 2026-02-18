"""
Priority-first financial classification for transactions.

Computes financial_class, obligation_flag, instrument_type, counterparty_type,
and priority_rank from channel, description, and enrichment context.
"""

from __future__ import annotations

import re
from dataclasses import dataclass
from typing import Any

# Priority rank per doc: INCOME=1 .. OTHER=9
_FINANCIAL_CLASS_RANK: dict[str, int] = {
    "INCOME": 1,
    "LOAN_EMI": 2,
    "CREDIT_CARD_PAYMENT": 3,
    "INVESTMENT": 4,
    "INSURANCE": 5,
    "TRANSFER_IN": 6,
    "TRANSFER_OUT": 7,
    "MERCHANT_PAYMENT": 8,
    "OTHER": 9,
}


@dataclass
class FinancialClassificationResult:
    financial_class: str
    obligation_flag: bool
    instrument_type: str
    counterparty_type: str
    priority_rank: int


def classify_financial(
    *,
    channel_type: str | None = None,
    cr_dr: str | None = None,
    direction: str | None = None,
    raw_description: str | None = None,
    counterparty_name: str | None = None,
    counterparty_vpa: str | None = None,
    category_id: str | None = None,
    cat_l1: str | None = None,
    transfer_type: str | None = None,
    is_card_payment: bool = False,
    is_loan_payment: bool = False,
    is_investment: bool = False,
    merchant_flag: bool = False,
    amount: float = 0.0,
) -> FinancialClassificationResult:
    """
    Compute financial classification from parsed + enrichment context.

    Call after category/merchant are determined. CRED-specific logic runs
    first for descriptions containing CRED; then ACH/NACH, UPI, NEFT/IMPS;
    finally default from cat_l1/category_id.
    """
    raw = (raw_description or "").strip().upper()
    desc_lower = (raw_description or "").strip().lower()
    channel = (channel_type or "").upper()
    dr = (cr_dr or "").upper() in ("D", "DEBIT")
    # direction: IN/OUT (parsed) or debit/credit
    out = dr or (direction or "").upper() in ("OUT", "DEBIT")
    cat_l1_norm = (cat_l1 or "").strip().lower()
    category_norm = (category_id or "").strip().lower()

    # --- Credits ---
    if not out:
        return _classify_credit(
            channel=channel,
            raw=raw,
            desc_lower=desc_lower,
            category_norm=category_norm,
            cat_l1_norm=cat_l1_norm,
            transfer_type=transfer_type or "",
        )

    # --- Debits: CRED (must run before generic CRED merchant rule) ---
    if "CRED" in raw:
        cred_result = _classify_cred_debit(raw=raw, desc_lower=desc_lower, amount=amount)
        if cred_result:
            return cred_result

    # --- Debits: ACH / NACH ---
    if channel in ("ACH", "NACH"):
        ach_result = _classify_ach_nach_debit(
            raw=raw,
            desc_lower=desc_lower,
            category_norm=category_norm,
            cat_l1_norm=cat_l1_norm,
            is_loan_payment=is_loan_payment,
            is_card_payment=is_card_payment,
            is_investment=is_investment,
        )
        if ach_result:
            return ach_result

    # --- Debits: UPI ---
    if channel == "UPI":
        return _classify_upi_debit(merchant_flag=merchant_flag)

    # --- Debits: NEFT / IMPS ---
    if channel in ("NEFT", "IMPS"):
        neft_result = _classify_neft_imps_debit(
            raw=raw,
            desc_lower=desc_lower,
            category_norm=category_norm,
            cat_l1_norm=cat_l1_norm,
            is_investment=is_investment,
        )
        if neft_result:
            return neft_result

    # --- Default from cat_l1 / category_id ---
    return _classify_default_debit(
        category_norm=category_norm,
        cat_l1_norm=cat_l1_norm,
        transfer_type=transfer_type or "",
        merchant_flag=merchant_flag,
        is_loan_payment=is_loan_payment,
        is_card_payment=is_card_payment,
        is_investment=is_investment,
    )


def _classify_credit(
    channel: str,
    raw: str,
    desc_lower: str,
    category_norm: str,
    cat_l1_norm: str,
    transfer_type: str,
) -> FinancialClassificationResult:
    # Salary / recurring income or NEFT credit + pattern
    salary_keywords = [
        "salary", "sal ", "payroll", "wages", "neft credit", "credit neft",
        "credit-salary", "salary credit", "creditsalary", "income",
    ]
    if any(k in desc_lower for k in salary_keywords):
        return _result("INCOME", False, "SALARY", "BANK", 1)
    if channel == "NEFT" and ("CREDIT" in raw or "BY TRANSFER" in raw):
        return _result("INCOME", False, "SALARY", "BANK", 1)
    # Own-account / self transfer
    if transfer_type == "SELF" or "self" in desc_lower or "sweep" in desc_lower:
        return _result("TRANSFER_IN", False, "P2P_TRANSFER", "BANK", 6)
    if cat_l1_norm == "income" or category_norm in ("banks", "income"):
        return _result("INCOME", False, "SALARY", "BANK", 1)
    if cat_l1_norm == "transfer" or category_norm in ("transfers_in",):
        return _result("TRANSFER_IN", False, "P2P_TRANSFER", "INDIVIDUAL", 6)
    return _result("OTHER", False, "OTHER", "UNKNOWN", 9)


def _classify_cred_debit(
    raw: str, desc_lower: str, amount: float
) -> FinancialClassificationResult | None:
    # Priority: CRED.INSURANCE → CRED.CLUB/CRED CASH → CREDPAY. → generic CRED CC
    if "CRED.INSURANCE" in raw or "CRED INSURANCE" in raw:
        return _result("INSURANCE", True, "INSURANCE_PREMIUM", "INSURER", 5)
    if "CRED.CLUB" in raw or "CRED CASH" in raw or "CREDCLUB" in raw or "CREDCASH" in raw:
        return _result("LOAN_EMI", True, "EMI", "NBFC", 2)
    if "CREDPAY." in raw or "CREDPAY-" in raw:
        return _result("MERCHANT_PAYMENT", False, "MERCHANT_UPI", "MERCHANT", 8)
    # Generic CRED: treat as credit card bill (obligation); instrument CREDIT_CARD_BILL
    if "CRED" in raw and (amount >= 1000 or "CARD" in raw or "PAYMENT" in raw or "CC" in raw):
        return _result("CREDIT_CARD_PAYMENT", True, "CREDIT_CARD_BILL", "BANK", 3)
    if "CRED" in raw:
        return _result("CREDIT_CARD_PAYMENT", True, "CREDIT_CARD_BILL", "BANK", 3)
    return None


def _classify_ach_nach_debit(
    raw: str,
    desc_lower: str,
    category_norm: str,
    cat_l1_norm: str,
    is_loan_payment: bool,
    is_card_payment: bool,
    is_investment: bool,
) -> FinancialClassificationResult | None:
    # EMI / loan entity (e.g. RACPC, FINANCE, LOAN)
    emi_keywords = ["emi", "loan", "racpc", "finance", "nbfc", "hdfc loan", "icici loan", "sbi loan"]
    if any(k in desc_lower for k in emi_keywords) or is_loan_payment:
        return _result("LOAN_EMI", True, "EMI", "NBFC", 2)
    # CC / bill
    if "card" in desc_lower or "bill" in desc_lower or "credit card" in desc_lower or is_card_payment:
        return _result("CREDIT_CARD_PAYMENT", True, "CREDIT_CARD_BILL", "BANK", 3)
    # MF / CAMS / NSE / broker
    mf_keywords = ["hdfcmf", "cams", "nsemfs", "nse mf", "birla sunlife", "icici prudential", "mutual", "sip"]
    if any(k in desc_lower for k in mf_keywords) or is_investment:
        return _result("INVESTMENT", True, "SIP", "AMC", 4)
    # Insurance
    ins_keywords = ["insurance", "birla sun life", "hdfc life", "icici lombard", "premium"]
    if any(k in desc_lower for k in ins_keywords):
        return _result("INSURANCE", True, "INSURANCE_PREMIUM", "INSURER", 5)
    # Already tagged by enrichment rule
    if cat_l1_norm == "loan":
        return _result("LOAN_EMI", True, "EMI", "NBFC", 2)
    if cat_l1_norm == "investment":
        return _result("INVESTMENT", True, "SIP", "AMC", 4)
    if category_norm in ("credit_cards", "loans_emi", "loans_payments"):
        return _result("CREDIT_CARD_PAYMENT", True, "CREDIT_CARD_BILL", "BANK", 3)
    return None


def _classify_upi_debit(merchant_flag: bool) -> FinancialClassificationResult:
    if merchant_flag:
        return _result("MERCHANT_PAYMENT", False, "MERCHANT_UPI", "MERCHANT", 8)
    return _result("TRANSFER_OUT", False, "P2P_TRANSFER", "INDIVIDUAL", 7)


def _classify_neft_imps_debit(
    raw: str,
    desc_lower: str,
    category_norm: str,
    cat_l1_norm: str,
    is_investment: bool,
) -> FinancialClassificationResult | None:
    broker_keywords = ["broker", "zerodha", "upstox", "angel", "amc", "mf", "cams", "nse "]
    if any(k in desc_lower for k in broker_keywords) or is_investment:
        return _result("INVESTMENT", True, "BROKERAGE_SETTLEMENT", "BROKER", 4)
    if "card" in desc_lower or "bill" in desc_lower or "credit" in desc_lower:
        return _result("CREDIT_CARD_PAYMENT", True, "CREDIT_CARD_BILL", "BANK", 3)
    if cat_l1_norm == "investment":
        return _result("INVESTMENT", True, "SIP", "AMC", 4)
    return _result("TRANSFER_OUT", False, "P2P_TRANSFER", "INDIVIDUAL", 7)


def _classify_default_debit(
    category_norm: str,
    cat_l1_norm: str,
    transfer_type: str,
    merchant_flag: bool,
    is_loan_payment: bool,
    is_card_payment: bool,
    is_investment: bool,
) -> FinancialClassificationResult:
    if cat_l1_norm == "income":
        return _result("INCOME", False, "OTHER", "UNKNOWN", 1)
    if cat_l1_norm == "loan" or is_loan_payment or category_norm in ("loans_emi", "loans_payments"):
        return _result("LOAN_EMI", True, "EMI", "NBFC", 2)
    if is_card_payment or category_norm == "credit_cards":
        return _result("CREDIT_CARD_PAYMENT", True, "CREDIT_CARD_BILL", "BANK", 3)
    if cat_l1_norm == "investment" or is_investment:
        return _result("INVESTMENT", True, "SIP", "AMC", 4)
    if "insurance" in category_norm or "insurance" in cat_l1_norm:
        return _result("INSURANCE", True, "INSURANCE_PREMIUM", "INSURER", 5)
    if transfer_type in ("P2P", "SELF") or category_norm in ("transfers_out", "transfers_in"):
        if "transfers_in" in category_norm:
            return _result("TRANSFER_IN", False, "P2P_TRANSFER", "INDIVIDUAL", 6)
        return _result("TRANSFER_OUT", False, "P2P_TRANSFER", "INDIVIDUAL", 7)
    if merchant_flag:
        return _result("MERCHANT_PAYMENT", False, "MERCHANT_UPI", "MERCHANT", 8)
    # Expense categories → merchant payment
    if cat_l1_norm == "expense" and category_norm not in ("transfers_out",):
        return _result("MERCHANT_PAYMENT", False, "MERCHANT_UPI", "MERCHANT", 8)
    return _result("OTHER", False, "OTHER", "UNKNOWN", 9)


def _result(
    financial_class: str,
    obligation_flag: bool,
    instrument_type: str,
    counterparty_type: str,
    priority_rank: int,
) -> FinancialClassificationResult:
    return FinancialClassificationResult(
        financial_class=financial_class,
        obligation_flag=obligation_flag,
        instrument_type=instrument_type,
        counterparty_type=counterparty_type,
        priority_rank=priority_rank,
    )


def to_dict(result: FinancialClassificationResult) -> dict[str, Any]:
    """Convert result to dict for DB insert."""
    return {
        "financial_class": result.financial_class,
        "obligation_flag": result.obligation_flag,
        "instrument_type": result.instrument_type,
        "counterparty_type": result.counterparty_type,
        "priority_rank": result.priority_rank,
    }
