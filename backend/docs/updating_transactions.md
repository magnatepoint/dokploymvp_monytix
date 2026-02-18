Monytix Transaction Classification – Structural Upgrade Document 

 

Purpose 

 

This document explains the required structural upgrades to Monytix transaction classification logic. 

 

Current categorization is too surface-level (only channel-based). We need a priority-first financial classification engine. 

Shape 

Key Business Goal 

 

The most important outcomes are: 

Correctly identify Income 

Correctly identify Loan EMI 

Correctly identify Credit Card Payments 

Correctly identify Investments (MF/Stocks/SIP) 

Correctly identify Insurance Premiums 

 

Once these are correctly detected, remaining transactions naturally fall into: 

Transfers 

Merchant spends 

Needs/Wants 

Shape 

Problem in Current Model 

 

Current schema fields: 

bank_code 

cr_dr 

channel_type 

direction 

category_id 

subcategory_id 

cat_l1 

raw_description 

 

Issues 

channel_type is too generic 

No priority-based financial layer 

No instrument-level classification 

No counterparty typing 

Shape 

Required Structural Additions 

 

1. financial_class (Level-1 Priority Category) 

 

Add a new column: 

financial_class ENUM( 

  INCOME, 

  LOAN_EMI, 

  CREDIT_CARD_PAYMENT, 

  INVESTMENT, 

  INSURANCE, 

  TRANSFER_IN, 

  TRANSFER_OUT, 

  MERCHANT_PAYMENT, 

  OTHER 

) 

This becomes the primary classification output. 

Shape 

2. obligation_flag 

 

Boolean marker: 

1 = Mandatory committed payments (EMI, SIP, Insurance, CC Bill) 

0 = Discretionary spends 

Shape 

3. instrument_type 

 

More granular transaction intent: 

 

Examples: 

SALARY 

EMI 

SIP 

CREDIT_CARD_BILL 

BROKERAGE_SETTLEMENT 

INSURANCE_PREMIUM 

DONATION 

MERCHANT_UPI 

P2P_TRANSFER 

Shape 

4. counterparty_type 

 

Classifies who the transaction is with: 

BANK 

NBFC 

AMC 

INSURER 

BROKER 

GOVT 

MERCHANT 

INDIVIDUAL 

NGO 

UNKNOWN 

Shape 

5. priority_rank 

 

Numeric ordering for classification priority: 

financial_class 

rank 

INCOME 

1 

LOAN_EMI 

2 

CREDIT_CARD_PAYMENT 

3 

INVESTMENT 

4 

INSURANCE 

5 

TRANSFER_IN 

6 

TRANSFER_OUT 

7 

MERCHANT_PAYMENT 

8 

OTHER 

9 

Shape 

SQL Schema Upgrade 

ALTER TABLE transactions 

ADD COLUMN financial_class VARCHAR(30), 

ADD COLUMN obligation_flag BOOLEAN DEFAULT 0, 

ADD COLUMN instrument_type VARCHAR(40), 

ADD COLUMN counterparty_type VARCHAR(30), 

ADD COLUMN priority_rank INT; 

Shape 

Channel-Specific Intelligence Rules 

 

ACH / NACH 

 

Most ACH/NACH debits are: 

EMI 

SIP 

Insurance 

Credit card bills 

 

Rule: 

IF channel_type IN ('ACH','NACH') AND cr_dr='DR' 

  THEN run priority keyword detection: 

    EMI → Investment → Insurance → CC 

Examples: 

RACPC SECUNDERABAD → LOAN_EMI 

HDFCMF / CAMS / NSEMFS → INVESTMENT (SIP) 

BIRLA SUNLIFE → INSURANCE or MF 

Shape 

NEFT Credit 

 

NEFT credit transactions are mostly: 

Salary 

Corporate payouts 

 

Rule: 

IF channel_type='NEFT' AND cr_dr='CR' 

  AND (salary keywords OR recurring monthly pattern) 

    → financial_class = INCOME 

Shape 

UPI Debits 

 

UPI debits split into: 

Merchant payments 

Person-to-person transfers 

 

Rule: 

IF merchant dictionary match → MERCHANT_PAYMENT 

ELSE → TRANSFER_OUT 

Shape 

Master Classification Flow 

 

Credit Transactions 

Salary keywords / recurring income → INCOME 

Own account transfer → TRANSFER_IN 

Else → OTHER 

 

Debit Transactions 

ACH/NACH priority detection: 

EMI → CC → Investment → Insurance 

UPI: 

Merchant → MERCHANT_PAYMENT 

Else → TRANSFER_OUT 

NEFT/IMPS: 

To broker/AMC → INVESTMENT 

To card number → CREDIT_CARD_PAYMENT 

Else → TRANSFER_OUT 

Shape 

Expected Developer Outcome 

 

After implementing this structure: 

Income detection becomes reliable 

EMI burden calculation possible 

SIP discipline and investment tracking accurate 

Insurance premiums separated from lifestyle spends 

Needs/Wants classification becomes clean residual layer 

Shape 

Next Enhancements 

 

Future modules can build: 

Obligation Stress Score 

Income Stability Score 

Investment Discipline Index 

Behavioral spending insights 

Shape 

Shape 

UPI Merchant vs Person Identification Logic 

 

This is a critical layer because UPI transactions form a large % of debit volume. Correct identification directly impacts: 

Merchant spend tracking 

Transfer out detection 

Needs vs Wants classification 

Behavioral analytics 

 

UPI transactions must be split into: 

MERCHANT_PAYMENT 

TRANSFER_OUT (Person-to-Person) 

Shape 

Structural Enhancements Required 

 

Add supporting fields if not present: 

upi_handle 

merchant_flag (boolean) 

merchant_category_code (optional future) 

entity_name (normalized counterparty name) 

Shape 

Multi-Layer Detection Logic 

 

UPI classification must follow 3 layers: 

Structural Pattern Detection 

Semantic Keyword Detection 

Behavioral Pattern Detection 

 

Only after combining these layers should merchant_flag be finalized. 

Shape 

Layer 1 – Structural Pattern Detection 

 

A. UPI Handle Analysis 

 

If UPI handle contains: 

@okaxis 

@ybl 

@ibl 

@paytm 

@apl 

@axl 

@icici 

 

Then parse prefix name. 

 

Rules: 

If handle contains business-like string (ENTERPRISE, STORE, MART, TRADERS, SERVICES, PVT, LTD, MEDICAL, PHARMA, ELECTRONICS) → Likely MERCHANT 

If handle resembles mobile number (10-digit numeric prefix) → Likely INDIVIDUAL 

 

Example: 

 

rajkirana@okaxis → MERCHANT 

9876543210@ibl → INDIVIDUAL 

Shape 

B. UPI Description Markers 

 

If raw_description contains: 

QR 

POS 

PAYTM QR 

BHARATPE 

PHONEPE MERCHANT 

GOOGLE PAY BUSINESS 

AMAZON 

SWIGGY 

ZOMATO 

 

→ MERCHANT_PAYMENT 

 

If description contains: 

FROM 

TO 

FRIEND 

SELF 

TRANSFER 

 

→ Likely TRANSFER_OUT 

Shape 

Layer 2 – Semantic Keyword Dictionary 

 

Maintain a Merchant Dictionary Table: 

 

Table: merchant_dictionary 

 

Fields: 

normalized_name 

merchant_type 

confidence_score 

 

Example Entries: 

DMART 

AMAZON 

FLIPKART 

SWIGGY 

ZOMATO 

UBER 

OLA 

MEDPLUS 

APOLLO PHARMACY 

 

If fuzzy match > threshold → MERCHANT_PAYMENT 

Shape 

Layer 3 – Behavioral Pattern Detection 

 

This improves accuracy significantly. 

 

A. Frequency Pattern 

 

If counterparty appears: 

3 times per month 

Similar transaction sizes 

Small rounded amounts 

 

Likely MERCHANT. 

 

B. Contact Name Matching 

 

If counterparty name matches saved contact list or appears like personal name (First Last pattern, no business keywords) → Likely INDIVIDUAL. 

 

C. Amount Behavior 

Highly variable amounts → MERCHANT 

Round fixed transfers (5000, 10000 monthly) → Possible PERSONAL TRANSFER 

Shape 

Final Decision Logic 

 

Pseudo-flow: 

IF channel_type='UPI' AND cr_dr='DR': 

 

  score = 0 

 

  IF structural merchant indicators → score += 2 

  IF semantic dictionary match → score += 3 

  IF behavioral merchant pattern → score += 2 

 

  IF structural person indicators → score -= 2 

  IF personal name match → score -= 2 

 

  IF score >= 3: 

      financial_class = MERCHANT_PAYMENT 

      merchant_flag = 1 

      counterparty_type = MERCHANT 

  ELSE: 

      financial_class = TRANSFER_OUT 

      merchant_flag = 0 

      counterparty_type = INDIVIDUAL 

Shape 

Why Multi-Layer Approach is Mandatory 

 

Only structural detection → high false positives. 

Only keyword detection → misses new merchants. 

Only behavioral → slow learning. 

 

Combined scoring approach ensures: 

High precision 

Self-improving system 

Lower misclassification of personal transfers 

Shape 

Developer Implementation Suggestion 

 

Create a UPI classification module: 

 

Module Order: 

Normalize entity_name 

Extract upi_handle 

Run structural rules 

Run merchant dictionary lookup 

Run behavioral scoring 

Assign merchant_flag and financial_class 

 

This module should run before lifestyle categorization. 

Shape 

Shape 

CRED Transaction Classification Logic 

 

CRED transactions require special handling because CRED acts as an intermediary platform for: 

Credit card bill payments 

Personal loans (CRED Cash / CRED Club) 

Insurance purchases 

Merchant expense payments (via CRED Pay) 

 

Therefore, classification must not rely only on the word “CRED”. 

Shape 

Step 1: Identify CRED Root Marker 

 

If raw_description contains: 

CRED 

CREDPAY 

CRED.CLUB 

CRED CASH 

CRED INSURANCE 

 

Then route transaction to CRED classification module. 

Shape 

CRED Scenario Logic 

 

1️⃣ CRED – Credit Card Bill Payment 

 

Typical Descriptions: 

CRED 

CRED PAYMENT 

CRED CC 

CRED.CLUB (sometimes used for CC routing) 

 

Detection Logic: 

IF contains 'CRED' 

AND (contains card reference OR large rounded amount OR monthly recurring pattern) 

AND cr_dr='DR' 

THEN 

  financial_class = CREDIT_CARD_PAYMENT 

  instrument_type = CREDIT_CARD_BILL 

  obligation_flag = 1 

  counterparty_type = FINTECH 

Supporting Signals: 

Amount matches known credit card outstanding 

Monthly recurring pattern 

Amount > typical merchant spend 

Shape 

2️⃣ CRED.CLUB – Loan Repayment 

 

CRED Club / CRED Cash typically indicates personal loan repayment. 

 

Detection Logic: 

IF raw_description contains 'CRED.CLUB' OR 'CRED CASH' 

THEN 

  financial_class = LOAN_EMI 

  instrument_type = EMI 

  obligation_flag = 1 

  counterparty_type = NBFC 

Supporting Signals: 

Fixed EMI amount monthly 

Tenure-like recurring pattern 

Shape 

3️⃣ CRED.INSURANCE – Insurance Premium 

 

Detection Logic: 

IF raw_description contains 'CRED.INSURANCE' 

THEN 

  financial_class = INSURANCE 

  instrument_type = INSURANCE_PREMIUM 

  obligation_flag = 1 

  counterparty_type = INSURER 

Shape 

4️⃣ CREDPAY.SWIGGY / CREDPAY. 

 

Examples: 

CREDPAY.SWIGGY 

CREDPAY.ZOMATO 

CREDPAY.UBER 

CREDPAY.AMAZON 

 

These are merchant expense payments routed via CRED Pay. 

 

Detection Logic: 

IF raw_description contains 'CREDPAY.' 

THEN 

  Extract merchant_name after 'CREDPAY.' 

 

  financial_class = MERCHANT_PAYMENT 

  instrument_type = MERCHANT_UPI 

  obligation_flag = 0 

  counterparty_type = MERCHANT 

Important: 

The presence of CRED does NOT mean credit card bill. 

If ‘CREDPAY.’ exists → Always treat as expense unless proven otherwise. 

Shape 

Priority Resolution for CRED 

 

If multiple indicators exist, follow this order: 

CRED.INSURANCE → INSURANCE 

CRED.CLUB / CRED CASH → LOAN_EMI 

CREDPAY. → MERCHANT_PAYMENT 

Generic CRED + recurring large monthly debit → CREDIT_CARD_PAYMENT 

Shape 

Developer Implementation Flow 

IF raw_description LIKE '%CRED%': 

   IF contains 'CRED.INSURANCE' → INSURANCE 

   ELSE IF contains 'CRED.CLUB' OR 'CRED CASH' → LOAN_EMI 

   ELSE IF contains 'CREDPAY.' → MERCHANT_PAYMENT 

   ELSE → evaluate for CREDIT_CARD_PAYMENT (recurring + large amount rule) 

This CRED module must execute before generic merchant detection to avoid misclassification. 

Shape 

End of Document 

 