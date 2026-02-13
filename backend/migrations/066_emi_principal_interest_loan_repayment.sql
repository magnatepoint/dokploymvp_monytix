-- ============================================================================
-- Migration 066: EMI Principal / EMI Interest → Loan Repayment EMI
--
-- User feedback: EMI Principal and EMI Interest should be categorized as
-- "Loan Repayment EMI" (category + subcategory) instead of Transfers Out.
-- ============================================================================

BEGIN;

SET search_path TO spendsense, public, extensions;

-- ============================================================================
-- 1) Add subcategory: loan_repayment_emi (Loan Repayment EMI)
-- ============================================================================

INSERT INTO spendsense.dim_subcategory (
    subcategory_code, category_code, subcategory_name, display_order, active, user_id, is_custom
)
VALUES
    ('loan_repayment_emi', 'loans_payments', 'Loan Repayment EMI', 16, TRUE, NULL, FALSE)
ON CONFLICT (subcategory_code)
DO UPDATE SET
    category_code = EXCLUDED.category_code,
    subcategory_name = EXCLUDED.subcategory_name,
    display_order = EXCLUDED.display_order,
    active = TRUE,
    is_custom = FALSE,
    user_id = NULL;

-- ============================================================================
-- 2) Add merchant rules for EMI Principal and EMI Interest
--    Matches: "EMI Principal - 34/49", "EMI Interest - 34/49", "emi principal", etc.
-- ============================================================================

INSERT INTO spendsense.merchant_rules (
    rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code,
    active, source, tenant_id, created_by, pattern_hash
)
VALUES
    (gen_random_uuid(), 20, 'merchant', '(?i)\b(EMI\s*PRINCIPAL|EMI\s*INTEREST)\b', 'loans_payments', 'loan_repayment_emi', true, 'seed', NULL, NULL, encode(digest('(?i)\b(EMI\s*PRINCIPAL|EMI\s*INTEREST)\b', 'sha1'), 'hex')),
    (gen_random_uuid(), 20, 'description', '(?i)\b(EMI\s*PRINCIPAL|EMI\s*INTEREST)\b', 'loans_payments', 'loan_repayment_emi', true, 'seed', NULL, NULL, encode(digest('(?i)\b(EMI\s*PRINCIPAL|EMI\s*INTEREST)\b', 'sha1'), 'hex'));

COMMIT;
