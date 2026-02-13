-- ============================================================================
-- Migration 067: Add confidence to vw_txn_effective
--
-- Exposes categorization confidence for "Review" badge in UI.
-- User overrides = 1.0, else use enriched confidence (or 0.5 if unenriched).
-- ============================================================================

BEGIN;

SET search_path TO spendsense, public;

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
  -- Confidence: 1.0 if user override, else enriched confidence (0.5 if unenriched)
  (CASE WHEN lo.txn_id IS NOT NULL THEN 1.0 ELSE COALESCE(te.confidence, 0.5) END)::numeric(3,2) AS confidence
FROM spendsense.txn_fact f
LEFT JOIN spendsense.txn_parsed tp ON tp.fact_txn_id = f.txn_id
LEFT JOIN spendsense.txn_enriched te ON te.parsed_id = tp.parsed_id
LEFT JOIN last_override lo ON lo.txn_id = f.txn_id;

COMMENT ON VIEW spendsense.vw_txn_effective IS
'Effective transaction view with enriched metadata and confidence for Review badge.';

COMMIT;
