-- ============================================================================
-- Migration 064: Add merchant rules for HP fuel, Hy Stories, TCS salary, OTT Play
--
-- User feedback: These merchants incorrectly shown as Transfers Out.
-- Also addresses merchant name truncation (e.g. "5 hp petro", "hyd storie").
-- ============================================================================

BEGIN;

SET search_path TO spendsense, public, extensions;

-- ============================================================================
-- 1) HP Petrol / Fuel (e.g. "5 hp petro")
-- ============================================================================

INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 22, 'merchant', '(?i)\b(5\s*HP\s*PETRO|HP\s*PETRO|HP\s*PETROL|HPCL)\b', 'transport', 'tr_fuel', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\b(5\s*HP\s*PETRO|HP\s*PETRO|HP\s*PETROL|HPCL)\b', 'transport', 'tr_fuel', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 2) Hy Stories - cafe/food (e.g. "hyd storie")
-- ============================================================================

INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 22, 'merchant', '(?i)\b(HYD\s*STORIE|HY\s*STORIES)\b', 'food_dining', 'fd_cafes', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\b(HYD\s*STORIE|HY\s*STORIES)\b', 'food_dining', 'fd_cafes', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 3) Tata Consultancy Services - salary (e.g. "tata consultancy services")
-- ============================================================================

INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 15, 'merchant', '(?i)\bTATA\s*CONSULTANCY\s*SERVICES\b', 'income', 'inc_salary', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'description', '(?i)\bTATA\s*CONSULTANCY\s*SERVICES|TCS\b', 'income', 'inc_salary', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 4) OTT Play - OTT subscription (e.g. "ott play")
-- ============================================================================

INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 22, 'merchant', '(?i)\bOTT\s*PLAY\b', 'entertainment', 'ent_movies_ott', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\bOTT\s*PLAY\b', 'entertainment', 'ent_movies_ott', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 5) Add dim_merchant + merchant_alias for display name normalization
--    (5 hp petro → HP, hyd storie → Hy Stories)
-- ============================================================================

-- HP / HPCL (fuel) - for display "HP"
INSERT INTO spendsense.dim_merchant (
    merchant_code, merchant_name, normalized_name, brand_keywords,
    category_code, subcategory_code, merchant_type, website
)
VALUES
    ('hp_petro', 'HP', 'hp petro', ARRAY['hp petro', '5 hp petro', 'hpcl', 'hp petrol'], 'transport', 'tr_fuel', 'offline', NULL)
ON CONFLICT (merchant_code) DO UPDATE
SET brand_keywords = ARRAY['hp petro', '5 hp petro', 'hpcl', 'hp petrol'],
    updated_at = NOW();

-- Hy Stories (cafe)
INSERT INTO spendsense.dim_merchant (
    merchant_code, merchant_name, normalized_name, brand_keywords,
    category_code, subcategory_code, merchant_type, website
)
VALUES
    ('hy_stories', 'Hy Stories', 'hy stories', ARRAY['hy stories', 'hyd storie'], 'food_dining', 'fd_cafes', 'offline', NULL)
ON CONFLICT (merchant_code) DO UPDATE
SET brand_keywords = ARRAY['hy stories', 'hyd storie'],
    updated_at = NOW();

-- OTT Play
INSERT INTO spendsense.dim_merchant (
    merchant_code, merchant_name, normalized_name, brand_keywords,
    category_code, subcategory_code, merchant_type, website
)
VALUES
    ('ott_play', 'OTT Play', 'ott play', ARRAY['ott play'], 'entertainment', 'ent_movies_ott', 'online', NULL)
ON CONFLICT (merchant_code) DO UPDATE
SET updated_at = NOW();

-- Add merchant_alias for display normalization
INSERT INTO spendsense.merchant_alias (merchant_id, alias, normalized_alias)
SELECT dm.merchant_id, '5 hp petro', '5 hp petro'
FROM spendsense.dim_merchant dm WHERE dm.merchant_code = 'hp_petro'
ON CONFLICT (merchant_id, normalized_alias) DO NOTHING;

INSERT INTO spendsense.merchant_alias (merchant_id, alias, normalized_alias)
SELECT dm.merchant_id, 'hyd storie', 'hyd storie'
FROM spendsense.dim_merchant dm WHERE dm.merchant_code = 'hy_stories'
ON CONFLICT (merchant_id, normalized_alias) DO NOTHING;

COMMIT;
