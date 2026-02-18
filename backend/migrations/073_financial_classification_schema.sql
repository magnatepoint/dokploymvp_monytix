-- ============================================================================
-- Migration 073: Financial classification schema (priority-first classification)
-- Adds financial_class, obligation_flag, instrument_type, counterparty_type, priority_rank
-- to txn_enriched and exposes them in vw_txn_effective.
-- ============================================================================

BEGIN;

SET search_path TO spendsense, public;

-- Add columns to txn_enriched
ALTER TABLE spendsense.txn_enriched
    ADD COLUMN IF NOT EXISTS financial_class VARCHAR(30),
    ADD COLUMN IF NOT EXISTS obligation_flag BOOLEAN DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS instrument_type VARCHAR(40),
    ADD COLUMN IF NOT EXISTS counterparty_type VARCHAR(30),
    ADD COLUMN IF NOT EXISTS priority_rank INT;

CREATE INDEX IF NOT EXISTS ix_txn_enriched_financial_class
    ON spendsense.txn_enriched(financial_class)
    WHERE financial_class IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_txn_enriched_obligation_flag
    ON spendsense.txn_enriched(obligation_flag)
    WHERE obligation_flag = TRUE;

-- Recreate vw_txn_effective with new columns from txn_enriched
DROP VIEW IF EXISTS spendsense.vw_txn_effective CASCADE;

CREATE OR REPLACE VIEW spendsense.vw_txn_effective AS
WITH last_override AS (
  SELECT DISTINCT ON (o.txn_id)
    o.txn_id, o.category_code, o.subcategory_code, o.txn_type, o.created_at
  FROM spendsense.txn_override o
  ORDER BY o.txn_id, o.created_at DESC
)
SELECT
  f.txn_id,
  f.user_id,
  f.txn_date,
  f.amount,
  f.direction,
  f.currency,
  f.description,
  COALESCE(lo.category_code, te.category_id) AS category_code,
  COALESCE(lo.subcategory_code, te.subcategory_id) AS subcategory_code,
  CASE
    WHEN lo.txn_type IS NOT NULL THEN lo.txn_type
    WHEN COALESCE(lo.category_code, te.category_id) IS NOT NULL THEN
      COALESCE(
        (SELECT dc.txn_type FROM spendsense.dim_category dc WHERE dc.category_code = COALESCE(lo.category_code, te.category_id)),
        CASE LOWER(COALESCE(te.cat_l1, ''))
          WHEN 'income' THEN 'income'
          WHEN 'loan' THEN 'debt'
          WHEN 'investment' THEN 'assets'
          WHEN 'transfer' THEN 'transfer'
          WHEN 'fee' THEN 'fees'
          WHEN 'cash' THEN 'transfer'
          ELSE 'wants'
        END
      )
    WHEN te.cat_l1 IS NOT NULL THEN
      CASE LOWER(te.cat_l1)
        WHEN 'income' THEN 'income'
        WHEN 'expense' THEN 'wants'
        WHEN 'loan' THEN 'debt'
        WHEN 'investment' THEN 'assets'
        WHEN 'transfer' THEN 'transfer'
        WHEN 'fee' THEN 'fees'
        WHEN 'cash' THEN 'transfer'
        ELSE 'wants'
      END
    WHEN f.direction = 'credit' THEN 'income'
    ELSE (
      SELECT COALESCE(dc.txn_type, 'wants')
      FROM spendsense.dim_category dc
      WHERE dc.category_code = COALESCE(lo.category_code, te.category_id)
    )
  END AS txn_type,
  f.merchant_id,
  f.merchant_name_norm,
  tp.counterparty_name,
  COALESCE(te.merchant_name, tp.counterparty_name, f.merchant_name_norm) AS merchant_name,
  CASE
    WHEN COALESCE(te.transfer_type, '') IN ('P2P', 'SELF') THEN 'N'
    WHEN te.merchant_id IS NOT NULL OR te.merchant_name IS NOT NULL THEN 'Y'
    WHEN f.merchant_id IS NOT NULL THEN 'Y'
    WHEN COALESCE(TRIM(f.merchant_name_norm), '') <> '' THEN 'Y'
    ELSE 'N'
  END AS merchant_flag,
  COALESCE(tp.channel_type, te.channel_type, f.channel) AS channel_type,
  COALESCE(te.raw_description, tp.raw_description, f.description) AS raw_description,
  f.bank_code,
  f.channel,
  COALESCE(tp.created_at::time, f.created_at::time) AS txn_time,
  (CASE WHEN lo.txn_id IS NOT NULL THEN 1.0 ELSE COALESCE(te.confidence, 0.5) END)::numeric(3,2) AS confidence,
  te.financial_class,
  COALESCE(te.obligation_flag, FALSE) AS obligation_flag,
  te.instrument_type,
  te.counterparty_type,
  te.priority_rank
FROM spendsense.txn_fact f
LEFT JOIN spendsense.txn_parsed tp ON tp.fact_txn_id = f.txn_id
LEFT JOIN spendsense.txn_enriched te ON te.parsed_id = tp.parsed_id
LEFT JOIN last_override lo ON lo.txn_id = f.txn_id;

COMMENT ON VIEW spendsense.vw_txn_effective IS
'Effective transaction view with enriched metadata, confidence, and financial classification.';

COMMIT;
