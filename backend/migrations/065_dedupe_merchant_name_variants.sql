-- ============================================================================
-- Migration 065: Remove duplicate transactions with merchant name variants
--
-- Same transaction can appear as "5 hp petro" and "hp petro" due to PDF/OCR variance.
-- This migration deletes duplicate txn_fact rows, keeping the one with the shorter
-- (cleaner) merchant name. Cascade will remove related txn_parsed and txn_enriched.
-- ============================================================================

BEGIN;

SET search_path TO spendsense, public, extensions;

-- Delete duplicate txn_fact rows:
-- Same: user, date, amount, direction, canonical merchant (strip leading digits)
-- Keep: row with shortest merchant_name_norm (cleaner: "hp petro" over "5 hp petro")
WITH dups AS (
  SELECT
    txn_id,
    ROW_NUMBER() OVER (
      PARTITION BY
        user_id,
        txn_date,
        amount,
        direction,
        TRIM(REGEXP_REPLACE(COALESCE(merchant_name_norm, ''), '^\d+\s+', ''))
      ORDER BY LENGTH(COALESCE(merchant_name_norm, '')), merchant_name_norm, txn_id
    ) AS rn
  FROM spendsense.txn_fact
  WHERE TRIM(REGEXP_REPLACE(COALESCE(merchant_name_norm, ''), '^\d+\s+', '')) != ''
)
DELETE FROM spendsense.txn_fact
WHERE txn_id IN (SELECT txn_id FROM dups WHERE rn > 1);

COMMIT;
