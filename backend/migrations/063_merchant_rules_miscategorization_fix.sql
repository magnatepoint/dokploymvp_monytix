-- ============================================================================
-- Migration 063: Fix miscategorized merchants (Transfers Out → correct categories)
--
-- User feedback: Many merchants incorrectly categorized as Transfers Out.
-- Adds merchant rules for: buffet, jewellers, organic, newspaper, jio,
-- skand ent, ecommerce, cafe, cloud kitchen, grocery, hypermarket, bakery,
-- fruit shops, sports, ladies clothing.
-- ============================================================================

BEGIN;

SET search_path TO spendsense, public, extensions;

-- ============================================================================
-- 1) Restaurant / Food - BUFFET, CAFE, CLOUD KITCHEN, BAKERY
-- ============================================================================

-- Buffet (e.g. flechazo buffet) → restaurant
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 22, 'merchant', '(?i)\b(FLECHAZO\s*)?BUFFET\b', 'food_dining', 'fd_fine', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\b(FLECHAZO\s*)?BUFFET\b', 'food_dining', 'fd_fine', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- Cafe (e.g. cafe southern f) → cafés & bakeries
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 25, 'merchant', '(?i)\bCAFE\s+SOUTH|CAFE\s*SOUTHERN\b', 'food_dining', 'fd_cafes', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'description', '(?i)\bCAFE\s+SOUTH|CAFE\s*SOUTHERN\b', 'food_dining', 'fd_cafes', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- Cloud kitchen (e.g. ava cloud kitch) → online food delivery
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 22, 'merchant', '(?i)\b(AVA\s*CLOUD\s*KITCH|CLOUD\s*KITCH|CLOUD\s*KITCHEN)\b', 'food_dining', 'fd_online', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\b(AVA\s*CLOUD\s*KITCH|CLOUD\s*KITCH|CLOUD\s*KITCHEN)\b', 'food_dining', 'fd_online', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- Bakery (e.g. karachi bakery)
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 25, 'merchant', '(?i)\bKARACHI\s*BAKERY|BAKERY\b', 'food_dining', 'fd_cafes', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'description', '(?i)\bKARACHI\s*BAKERY|BAKERY\b', 'food_dining', 'fd_cafes', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 2) Groceries - ORGANIC, NATURAL, HYPERMARKET, FRUIT
-- ============================================================================

-- Organic (e.g. dhenusya organi, ms gramee natur) → groceries
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 22, 'merchant', '(?i)\b(DHENUSYA\s*ORGANI|ORGANI|ORGANIC|GRAMEE\s*NATUR|NATUR)\b', 'groceries', 'groc_fv', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\b(DHENUSYA\s*ORGANI|ORGANI|ORGANIC|GRAMEE\s*NATUR|NATUR)\b', 'groceries', 'groc_fv', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- Max Hypermarket, Gramee Natur, Skand Ent → groceries
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 22, 'merchant', '(?i)\bMAX\s*HYPERMARKET\b', 'groceries', 'groc_hyper', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'merchant', '(?i)\bMS\s*GRAMEE\s*NATUR|GRAMEE\s*NATUR\b', 'groceries', 'groc_fv', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'merchant', '(?i)\bSKAND\s*ENT|SKANDA\s*ENT\b', 'groceries', 'groc_hyper', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- Fruit (e.g. ushma fruit bu)
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 22, 'merchant', '(?i)\bFRUIT\s*BU|USHMA\s*FRUIT\b', 'groceries', 'groc_fv', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\bFRUIT\s*BU|USHMA\s*FRUIT\b', 'groceries', 'groc_fv', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 3) Shopping - JEWELLERS, ECOMMERCE, SPORTS, LADIES
-- ============================================================================

-- Jewellers / Gold (e.g. prem jewellers)
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 22, 'merchant', '(?i)\bPREM\s*JEWELLERS|JEWELLERS\b', 'shopping', 'shop_clothing', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\bPREM\s*JEWELLERS|JEWELLERS\b', 'shopping', 'shop_clothing', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- Ecommerce (e.g. zenex ecommerce, fzb shopcom eco)
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 22, 'merchant', '(?i)\b(ECOMMERCE|SHOPCOM\s*ECO|FZB\s*SHOPCOM|ZENEX\s*ECO)\b', 'shopping', 'shop_marketplaces', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\b(ECOMMERCE|SHOPCOM\s*ECO|FZB\s*SHOPCOM|ZENEX\s*ECO)\b', 'shopping', 'shop_marketplaces', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- Sports (e.g. pooja sports)
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 25, 'merchant', '(?i)\bPOOJA\s*SPORTS|SPORTS\b', 'shopping', 'shop_sports_outdoor', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'description', '(?i)\bPOOJA\s*SPORTS|SPORTS\b', 'shopping', 'shop_sports_outdoor', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- Ladies / Rupa (e.g. ri rupa ladies)
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 25, 'merchant', '(?i)\bRI\s*RUPA\s*LADIES|RUPA\s*LADIES\b', 'shopping', 'shop_clothing', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'description', '(?i)\bRI\s*RUPA\s*LADIES|RUPA\s*LADIES\b', 'shopping', 'shop_clothing', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 4) Utilities - NEWSPAPER, JIO RECHARGE
-- ============================================================================

-- Newspaper (e.g. giri news papers)
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 25, 'merchant', '(?i)\bGIRI\s*NEWS|NEWS\s*PAPER|NEWSPAPER\b', 'utilities', 'util_other', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'description', '(?i)\bGIRI\s*NEWS|NEWS\s*PAPER|NEWSPAPER\b', 'utilities', 'util_other', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- Jio / Reliance Jio (recharge - reinforce existing rule with broader pattern)
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 20, 'merchant', '(?i)\bRELIANCE\s*JIO|JIO\s*IN\b', 'utilities', 'util_mobile', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 20, 'description', '(?i)\bRELIANCE\s*JIO|JIO\s*IN\b', 'utilities', 'util_mobile', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

COMMIT;
