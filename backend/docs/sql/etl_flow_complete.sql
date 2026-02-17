-- ============================================================================
-- ETL Flow: upload_batch → txn_staging → txn_fact → txn_parsed → txn_enriched
--           txn_override (user edits) overrides txn_enriched
--           vw_txn_effective = final display view
-- ============================================================================

SET search_path TO spendsense, public;

-- ============================================================================
-- 1) FULL FLOW QUERY: Trace a transaction from upload to effective view
--    Use for debugging or to inspect the pipeline for a given upload
-- ============================================================================

-- Replace :upload_id with your batch UUID, or use a subquery
/*
SELECT
    ub.upload_id,
    ub.status AS batch_status,
    ub.total_records,
    ts.staging_id,
    ts.txn_date AS staging_date,
    ts.description_raw,
    ts.amount AS staging_amount,
    f.txn_id AS fact_id,
    f.merchant_name_norm,
    tp.parsed_id,
    tp.channel_type,
    tp.counterparty_name,
    te.category_id AS enriched_category,
    te.subcategory_id AS enriched_subcategory,
    te.confidence,
    lo.category_code AS override_category,
    v.category_code AS effective_category,
    v.txn_type AS effective_txn_type,
    v.confidence AS effective_confidence
FROM spendsense.upload_batch ub
LEFT JOIN spendsense.txn_staging ts ON ts.upload_id = ub.upload_id
LEFT JOIN spendsense.txn_fact f ON f.upload_id = ub.upload_id
    AND f.user_id = ts.user_id
    AND f.txn_date = ts.txn_date
    AND f.amount = ts.amount
    AND f.direction = ts.direction
    AND COALESCE(f.txn_external_id, '') = COALESCE(ts.raw_txn_id, '')
LEFT JOIN spendsense.txn_parsed tp ON tp.fact_txn_id = f.txn_id
LEFT JOIN spendsense.txn_enriched te ON te.parsed_id = tp.parsed_id
LEFT JOIN LATERAL (
    SELECT DISTINCT ON (o.txn_id) o.txn_id, o.category_code, o.subcategory_code, o.txn_type
    FROM spendsense.txn_override o
    WHERE o.txn_id = f.txn_id
    ORDER BY o.txn_id, o.created_at DESC
) lo ON lo.txn_id = f.txn_id
LEFT JOIN spendsense.vw_txn_effective v ON v.txn_id = f.txn_id
WHERE ub.upload_id = 'YOUR-UPLOAD-UUID-HERE'::uuid
ORDER BY ts.txn_date DESC, ts.amount DESC;
*/

-- ============================================================================
-- 2) SIMPLIFIED FLOW: fact → parsed → enriched → override → effective
--    (One row per txn_id - use when staging is already transformed)
-- ============================================================================

SELECT
    f.txn_id,
    f.user_id,
    f.upload_id,
    f.txn_date,
    f.amount,
    f.direction,
    f.description,
    f.merchant_name_norm,
    tp.parsed_id,
    tp.channel_type,
    tp.counterparty_name,
    te.category_id   AS enriched_category,
    te.subcategory_id AS enriched_subcategory,
    te.cat_l1        AS enriched_txn_type,
    te.confidence    AS enriched_confidence,
    o.category_code  AS override_category,
    o.subcategory_code AS override_subcategory,
    v.category_code  AS effective_category,
    v.subcategory_code AS effective_subcategory,
    v.txn_type       AS effective_txn_type,
    v.merchant_name  AS effective_merchant_name,
    v.confidence     AS effective_confidence
FROM spendsense.txn_fact f
LEFT JOIN spendsense.txn_parsed tp ON tp.fact_txn_id = f.txn_id
LEFT JOIN spendsense.txn_enriched te ON te.parsed_id = tp.parsed_id
LEFT JOIN LATERAL (
    SELECT DISTINCT ON (o.txn_id) o.txn_id, o.category_code, o.subcategory_code, o.txn_type
    FROM spendsense.txn_override o
    WHERE o.txn_id = f.txn_id
    ORDER BY o.txn_id, o.created_at DESC
) o ON o.txn_id = f.txn_id
LEFT JOIN spendsense.vw_txn_effective v ON v.txn_id = f.txn_id
-- Add: WHERE f.user_id = 'uuid' OR WHERE f.upload_id = 'uuid' to filter
ORDER BY f.txn_date DESC
LIMIT 50;

-- ============================================================================
-- 3) vw_txn_effective VIEW DEFINITION (from migration 067)
--    Final display: fact + parsed + enriched, with user override winning
-- ============================================================================

/*
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
  (CASE WHEN lo.txn_id IS NOT NULL THEN 1.0 ELSE COALESCE(te.confidence, 0.5) END)::numeric(3,2) AS confidence
FROM spendsense.txn_fact f
LEFT JOIN spendsense.txn_parsed tp ON tp.fact_txn_id = f.txn_id
LEFT JOIN spendsense.txn_enriched te ON te.parsed_id = tp.parsed_id
LEFT JOIN last_override lo ON lo.txn_id = f.txn_id;
*/

-- ============================================================================
-- 4) PIPELINE COUNTS: Check row counts at each stage (for a given upload)
-- ============================================================================

-- Uncomment and set upload_id to debug a specific batch
/*
WITH batch AS (
  SELECT upload_id, user_id, status, total_records
  FROM spendsense.upload_batch
  WHERE upload_id = 'YOUR-UPLOAD-UUID-HERE'::uuid
)
SELECT 'upload_batch' AS stage, 1 AS cnt FROM batch
UNION ALL
SELECT 'txn_staging', COUNT(*)::int FROM spendsense.txn_staging ts JOIN batch b ON ts.upload_id = b.upload_id
UNION ALL
SELECT 'txn_fact', COUNT(*)::int FROM spendsense.txn_fact f JOIN batch b ON f.upload_id = b.upload_id
UNION ALL
SELECT 'txn_parsed', COUNT(*)::int
FROM spendsense.txn_fact f
JOIN batch b ON f.upload_id = b.upload_id
JOIN spendsense.txn_parsed tp ON tp.fact_txn_id = f.txn_id
UNION ALL
SELECT 'txn_enriched', COUNT(*)::int
FROM spendsense.txn_fact f
JOIN batch b ON f.upload_id = b.upload_id
JOIN spendsense.txn_parsed tp ON tp.fact_txn_id = f.txn_id
JOIN spendsense.txn_enriched te ON te.parsed_id = tp.parsed_id
UNION ALL
SELECT 'vw_txn_effective', COUNT(*)::int
FROM spendsense.txn_fact f
JOIN batch b ON f.upload_id = b.upload_id
JOIN spendsense.vw_txn_effective v ON v.txn_id = f.txn_id;
*/

-- ============================================================================
-- 5) TABLE RELATIONSHIPS (reference)
-- ============================================================================
/*
upload_batch (upload_id PK)
    │
    ├── txn_staging (upload_id FK, user_id, raw_txn_id, txn_date, description_raw, amount, direction, merchant_raw, bank_code, channel)
    │       └── [ETL transforms to txn_fact]
    │
    └── txn_fact (upload_id FK, user_id, txn_id PK, txn_external_id, txn_date, description, amount, direction, merchant_id, merchant_name_norm, bank_code, channel)
            │
            ├── txn_parsed (fact_txn_id FK → txn_fact.txn_id, parsed_id PK, channel_type, counterparty_name, direction, raw_description, ...)
            │       │
            │       └── txn_enriched (parsed_id FK → txn_parsed.parsed_id, category_id, subcategory_id, cat_l1, confidence, ...)
            │
            └── txn_override (txn_id FK → txn_fact.txn_id, category_code, subcategory_code, txn_type)  -- user edits, latest wins
                    │
                    └── vw_txn_effective = fact + parsed + enriched, with override overriding enriched
*/
