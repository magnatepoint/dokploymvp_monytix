-- ============================================================================
-- Migration 062: Add Wiggy→Swiggy merchant alias and rule
--
-- Bank statements often show "wiggy" or "wiggy limited" (truncated/OCR for Swiggy).
-- This migration:
-- 1. Adds merchant_alias: wiggy → Swiggy (for display name normalization)
-- 2. Adds merchant_rule: WIGGY pattern → food_dining/fd_online
-- 3. Adds wiggy to Swiggy's brand_keywords in dim_merchant
-- ============================================================================

BEGIN;

SET search_path TO spendsense, public, extensions;

-- ============================================================================
-- 1) Add 'wiggy' to Swiggy's brand_keywords in dim_merchant
-- ============================================================================
UPDATE spendsense.dim_merchant
SET brand_keywords = array_append(brand_keywords, 'wiggy'),
    updated_at = NOW()
WHERE merchant_code = 'swiggy'
  AND NOT ('wiggy' = ANY(brand_keywords));

-- ============================================================================
-- 2) Add merchant_alias: wiggy → Swiggy (for proper display name)
-- ============================================================================
INSERT INTO spendsense.merchant_alias (merchant_id, alias, normalized_alias)
SELECT dm.merchant_id, 'wiggy', 'wiggy'
FROM spendsense.dim_merchant dm
WHERE dm.merchant_code = 'swiggy'
ON CONFLICT (merchant_id, normalized_alias) DO NOTHING;

-- ============================================================================
-- 3) Add merchant_rule for WIGGY pattern (catches "wiggy", "wiggy limited", etc.)
-- ============================================================================
INSERT INTO spendsense.merchant_rules (
    rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code,
    active, source, tenant_id, created_by, created_at
)
VALUES
    (gen_random_uuid(), 19, 'merchant', '(?i)\bWIGGY\b', 'food_dining', 'fd_online', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 19, 'description', '(?i)\bWIGGY\b', 'food_dining', 'fd_online', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

COMMIT;
