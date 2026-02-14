-- ============================================================================
-- Migration 068: Backfill bank_code for transactions with NULL bank_code
--
-- 1) Uses upload_batch.file_name to infer bank (e.g. AXISMB_*.pdf, HDFC*.pdf).
-- 2) For uploads where filename doesn't hint: infer from IFSC in descriptions
--    (UTIB=Axis, HDFC=HDFC, SBIN=SBI, etc.). Fixes accounts/bank filter.
-- ============================================================================

BEGIN;

-- Step 1: Backfill from upload filename
UPDATE spendsense.txn_fact tf
SET bank_code = ub.inferred_bank_code
FROM (
    SELECT
        ub.upload_id,
        CASE
            WHEN LOWER(COALESCE(ub.file_name, '')) ~ 'axis' THEN 'axis_bank'
            WHEN LOWER(COALESCE(ub.file_name, '')) ~ 'hdfc' THEN 'hdfc_bank'
            WHEN LOWER(COALESCE(ub.file_name, '')) ~ 'icici' THEN 'icici_bank'
            WHEN LOWER(COALESCE(ub.file_name, '')) ~ 'sbi' THEN 'sbi_bank'
            WHEN LOWER(COALESCE(ub.file_name, '')) ~ 'kotak' THEN 'kotak_bank'
            WHEN LOWER(COALESCE(ub.file_name, '')) ~ 'canara' THEN 'canara_bank'
            WHEN LOWER(COALESCE(ub.file_name, '')) ~ 'federal' THEN 'federal_bank'
            ELSE NULL
        END AS inferred_bank_code
    FROM spendsense.upload_batch ub
    WHERE ub.file_name IS NOT NULL
      AND TRIM(ub.file_name) != ''
) ub
WHERE tf.upload_id = ub.upload_id
  AND (tf.bank_code IS NULL OR tf.bank_code = '')
  AND ub.inferred_bank_code IS NOT NULL;

-- Step 2: For uploads with NULL bank_code, infer from IFSC in descriptions
-- (UTIB=Axis Bank, HDFC=HDFC, SBIN=SBI, etc.)
UPDATE spendsense.txn_fact tf
SET bank_code = inferred.inferred_bank_code
FROM (
    SELECT
        tf2.upload_id,
        CASE
            WHEN bool_or(tf2.description ~ 'UTIB[0-9]') THEN 'axis_bank'
            WHEN bool_or(tf2.description ~ 'HDFC[0-9]') THEN 'hdfc_bank'
            WHEN bool_or(tf2.description ~ 'SBIN[0-9]') THEN 'sbi_bank'
            WHEN bool_or(tf2.description ~ 'ICIC[0-9]') THEN 'icici_bank'
            WHEN bool_or(tf2.description ~ 'KKBK[0-9]') THEN 'kotak_bank'
            WHEN bool_or(tf2.description ~ 'CNRB[0-9]') THEN 'canara_bank'
            WHEN bool_or(tf2.description ~ 'FDRL[0-9]') THEN 'federal_bank'
            ELSE NULL
        END AS inferred_bank_code
    FROM spendsense.txn_fact tf2
    WHERE (tf2.bank_code IS NULL OR tf2.bank_code = '')
      AND tf2.description IS NOT NULL
    GROUP BY tf2.upload_id
) inferred
WHERE tf.upload_id = inferred.upload_id
  AND (tf.bank_code IS NULL OR tf.bank_code = '')
  AND inferred.inferred_bank_code IS NOT NULL;

COMMIT;
