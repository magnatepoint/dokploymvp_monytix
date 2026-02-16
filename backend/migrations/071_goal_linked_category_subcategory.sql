-- ============================================================================
-- Migration 071: Add linked_category/subcategory to goals for deterministic matching
-- Enables: Credit Card Paydown = transfers_out + credit_card_payment + debit
-- ============================================================================
BEGIN;

ALTER TABLE goal.user_goals_master
  ADD COLUMN IF NOT EXISTS linked_category_code VARCHAR(64),
  ADD COLUMN IF NOT EXISTS linked_subcategory_code VARCHAR(64),
  ADD COLUMN IF NOT EXISTS linked_direction VARCHAR(8) CHECK (linked_direction IN ('debit', 'credit') OR linked_direction IS NULL),
  ADD COLUMN IF NOT EXISTS linked_min_amount NUMERIC(14,2),
  ADD COLUMN IF NOT EXISTS linked_match_confidence NUMERIC(3,2);

COMMENT ON COLUMN goal.user_goals_master.linked_category_code IS 'Match txn when category_code equals this (e.g. transfers_out, credit_cards)';
COMMENT ON COLUMN goal.user_goals_master.linked_subcategory_code IS 'Match txn when subcategory_code equals this (e.g. credit_card_payment, cc_bill_payment)';
COMMENT ON COLUMN goal.user_goals_master.linked_direction IS 'Optional: only match debit or credit';
COMMENT ON COLUMN goal.user_goals_master.linked_min_amount IS 'Optional: only match if |amount| >= this';

COMMIT;
