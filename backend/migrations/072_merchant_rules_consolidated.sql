-- ============================================================================
-- Migration 072: Consolidated Merchant Rules (replaces 049, 051, 062, 063, 064)
--
-- Combines all merchant rules and dim_merchant additions from:
-- - 049: Additional merchants (Quadrillion, Orange Auto, Congressional, etc.)
-- - 051: General pan shop rule
-- - 062: Wiggy→Swiggy alias and rule
-- - 063: Miscategorization fix (buffet, cafe, grocery, jewellers, etc.)
-- - 064: HP fuel, Hy Stories, TCS salary, OTT Play
-- ============================================================================

BEGIN;

SET search_path TO spendsense, public, extensions;

-- ============================================================================
-- 1) dim_merchant - from 049 and 064
-- ============================================================================

INSERT INTO spendsense.dim_merchant (
    merchant_code, merchant_name, normalized_name, brand_keywords,
    category_code, subcategory_code, merchant_type, website
)
VALUES
    -- 049: Quadrillion Finance, Orange Auto, Congressional, etc.
    ('quadrillion_finance', 'Quadrillion Finance', 'quadrillion finance',
     ARRAY['quadrillion','quadrillion finance','quadrillionfin'],
     'loans_payments', 'loan_personal', 'finance', NULL),
    ('orange_auto', 'Orange Auto Private', 'orange auto private',
     ARRAY['orange auto','orange auto private','orangeauto'],
     'motor_maintenance', 'motor_services', 'automotive', NULL),
    ('congressional', 'Congressional The Be', 'congressional the be',
     ARRAY['congressional','congressional the be','congressionalthebe'],
     'shopping', 'shop_clothing', 'retail', NULL),
    ('united_blowpast', 'United Blowpast Cont', 'united blowpast cont',
     ARRAY['united blowpast','united blowpast cont','unitedblowpast'],
     'business_expenses', 'biz_other', 'business', NULL),
    ('comfy', 'Comfy', 'comfy', ARRAY['comfy'], 'pets', 'pet_grooming', 'retail', NULL),
    ('icclgroww', 'Icclgroww', 'icclgroww', ARRAY['icclgroww','iccl groww','groww'],
     'investments_commitments', 'inv_sip', 'finance', NULL),
    ('idfc_first', 'IDFC First Bank', 'idfc first bank',
     ARRAY['idfc first','idfc first bank','idfcfirst','idfc'],
     'loans_payments', 'loan_cc_bill', 'banks', 'https://www.idfcfirstbank.com'),
    ('shivi_sree', 'Shivi Sree Milk Poin', 'shivi sree milk poin',
     ARRAY['shivi sree','shivi sree milk poin','shivisree'],
     'food_dining', 'fd_pan_shop', 'retail', NULL),
    -- 064: HP, Hy Stories, OTT Play
    ('hp_petro', 'HP', 'hp petro', ARRAY['hp petro', '5 hp petro', 'hpcl', 'hp petrol'], 'transport', 'tr_fuel', 'offline', NULL),
    ('hy_stories', 'Hy Stories', 'hy stories', ARRAY['hy stories', 'hyd storie'], 'food_dining', 'fd_cafes', 'offline', NULL),
    ('ott_play', 'OTT Play', 'ott play', ARRAY['ott play'], 'entertainment', 'ent_movies_ott', 'online', NULL)
ON CONFLICT (merchant_code) DO UPDATE
SET merchant_name = EXCLUDED.merchant_name,
    normalized_name = EXCLUDED.normalized_name,
    brand_keywords = EXCLUDED.brand_keywords,
    category_code = EXCLUDED.category_code,
    subcategory_code = EXCLUDED.subcategory_code,
    merchant_type = EXCLUDED.merchant_type,
    website = EXCLUDED.website,
    updated_at = NOW();

-- ============================================================================
-- 2) Wiggy→Swiggy: add to brand_keywords (062)
-- ============================================================================
UPDATE spendsense.dim_merchant
SET brand_keywords = array_append(brand_keywords, 'wiggy'),
    updated_at = NOW()
WHERE merchant_code = 'swiggy'
  AND NOT ('wiggy' = ANY(brand_keywords));

-- ============================================================================
-- 3) merchant_alias - from 049, 062, 064
-- ============================================================================
INSERT INTO spendsense.merchant_alias (merchant_id, alias, normalized_alias)
SELECT dm.merchant_id, kw, lower(regexp_replace(kw, '[^a-z0-9]', '', 'g'))
FROM spendsense.dim_merchant dm, unnest(dm.brand_keywords) AS kw
WHERE dm.merchant_code IN ('quadrillion_finance','orange_auto','congressional','united_blowpast','comfy','icclgroww','idfc_first','shivi_sree')
ON CONFLICT (merchant_id, normalized_alias) DO NOTHING;

INSERT INTO spendsense.merchant_alias (merchant_id, alias, normalized_alias)
SELECT dm.merchant_id, 'wiggy', 'wiggy'
FROM spendsense.dim_merchant dm WHERE dm.merchant_code = 'swiggy'
ON CONFLICT (merchant_id, normalized_alias) DO NOTHING;

INSERT INTO spendsense.merchant_alias (merchant_id, alias, normalized_alias)
SELECT dm.merchant_id, '5 hp petro', '5 hp petro'
FROM spendsense.dim_merchant dm WHERE dm.merchant_code = 'hp_petro'
ON CONFLICT (merchant_id, normalized_alias) DO NOTHING;

INSERT INTO spendsense.merchant_alias (merchant_id, alias, normalized_alias)
SELECT dm.merchant_id, 'hyd storie', 'hyd storie'
FROM spendsense.dim_merchant dm WHERE dm.merchant_code = 'hy_stories'
ON CONFLICT (merchant_id, normalized_alias) DO NOTHING;

-- ============================================================================
-- 4) merchant_rules - 049: Additional merchants
-- ============================================================================
INSERT INTO spendsense.merchant_rules (
    rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code,
    active, source, tenant_id, created_by, created_at
)
VALUES
    (gen_random_uuid(), 15, 'merchant', '\bquadrillion\b', 'loans_payments', 'loan_personal', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'description', '\bquadrillion\b', 'loans_payments', 'loan_personal', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'merchant', '\borange\s*auto\b', 'motor_maintenance', 'motor_services', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'description', '\borange\s*auto\b', 'motor_maintenance', 'motor_services', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'merchant', '\bcongressional\b', 'shopping', 'shop_clothing', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'description', '\bcongressional\b', 'shopping', 'shop_clothing', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'merchant', '\bunited\s*blowpast\b', 'business_expenses', 'biz_other', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'description', '\bunited\s*blowpast\b', 'business_expenses', 'biz_other', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'merchant', '\bcomfy\b', 'pets', 'pet_grooming', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'description', '\bcomfy\b', 'pets', 'pet_grooming', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'merchant', '\b(icclgroww|groww)\b', 'investments_commitments', 'inv_sip', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'description', '\b(icclgroww|groww)\b', 'investments_commitments', 'inv_sip', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 12, 'merchant', '\bidfc\s*first\b', 'loans_payments', 'loan_cc_bill', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 12, 'description', '\bidfc\s*first\b', 'loans_payments', 'loan_cc_bill', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 12, 'description', '\bidfcfirst\b', 'loans_payments', 'loan_cc_bill', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'merchant', '\bshivi\s*sree\b', 'food_dining', 'fd_pan_shop', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'description', '\bshivi\s*sree\b', 'food_dining', 'fd_pan_shop', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 10, 'description', '\bbilldkhdfccard\b', 'loans_payments', 'loan_cc_bill', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 10, 'description', '\bbhdfu4f0h84ogq\b', 'loans_payments', 'loan_cc_bill', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 10, 'description', '\bbill.*hdfc.*card\b', 'loans_payments', 'loan_cc_bill', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 8, 'merchant', '\bg\s*vijay\s*kumar\s*goud\b', 'transfers_out', 'tr_out_wallet', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 8, 'merchant', '\bg\s*ravi\b', 'transfers_out', 'tr_out_wallet', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 8, 'merchant', '\bgollagudem\s*ravi\b', 'transfers_out', 'tr_out_wallet', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 8, 'merchant', '\bm\s*s\s*s\s*ravi\s*babu\b', 'transfers_out', 'tr_out_wallet', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 8, 'description', '\bg\s*vijay\s*kumar\s*goud\b', 'transfers_out', 'tr_out_wallet', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 8, 'description', '\bg\s*ravi\b', 'transfers_out', 'tr_out_wallet', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 8, 'description', '\bgollagudem\s*ravi\b', 'transfers_out', 'tr_out_wallet', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 8, 'description', '\bm\s*s\s*s\s*ravi\s*babu\b', 'transfers_out', 'tr_out_wallet', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 5) merchant_rules - 051: General pan shop rule
-- ============================================================================
INSERT INTO spendsense.merchant_rules (
    rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code,
    active, source, tenant_id, created_by, pattern_hash, notes
)
VALUES
    (gen_random_uuid(), 20, 'merchant', '(?i)\bpan', 'food_dining', 'fd_pan_shop', true, 'seed', NULL, NULL, encode(digest('(?i)\bpan', 'sha1'), 'hex'), 'General rule: match "pan" in merchant name'),
    (gen_random_uuid(), 20, 'description', '(?i)\bpan', 'food_dining', 'fd_pan_shop', true, 'seed', NULL, NULL, encode(digest('(?i)\bpan', 'sha1'), 'hex'), 'General rule: match "pan" in description')
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 6) merchant_rules - 062: Wiggy
-- ============================================================================
INSERT INTO spendsense.merchant_rules (
    rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code,
    active, source, tenant_id, created_by, created_at
)
VALUES
    (gen_random_uuid(), 19, 'merchant', '(?i)\bWIGGY\b', 'food_dining', 'fd_online', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 19, 'description', '(?i)\bWIGGY\b', 'food_dining', 'fd_online', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 7) merchant_rules - 063: Miscategorization fix
-- ============================================================================
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 22, 'merchant', '(?i)\b(FLECHAZO\s*)?BUFFET\b', 'food_dining', 'fd_fine', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\b(FLECHAZO\s*)?BUFFET\b', 'food_dining', 'fd_fine', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'merchant', '(?i)\bCAFE\s+SOUTH|CAFE\s*SOUTHERN\b', 'food_dining', 'fd_cafes', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'description', '(?i)\bCAFE\s+SOUTH|CAFE\s*SOUTHERN\b', 'food_dining', 'fd_cafes', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'merchant', '(?i)\b(AVA\s*CLOUD\s*KITCH|CLOUD\s*KITCH|CLOUD\s*KITCHEN)\b', 'food_dining', 'fd_online', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\b(AVA\s*CLOUD\s*KITCH|CLOUD\s*KITCH|CLOUD\s*KITCHEN)\b', 'food_dining', 'fd_online', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'merchant', '(?i)\bKARACHI\s*BAKERY|BAKERY\b', 'food_dining', 'fd_cafes', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'description', '(?i)\bKARACHI\s*BAKERY|BAKERY\b', 'food_dining', 'fd_cafes', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'merchant', '(?i)\b(DHENUSYA\s*ORGANI|ORGANI|ORGANIC|GRAMEE\s*NATUR|NATUR)\b', 'groceries', 'groc_fv', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\b(DHENUSYA\s*ORGANI|ORGANI|ORGANIC|GRAMEE\s*NATUR|NATUR)\b', 'groceries', 'groc_fv', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'merchant', '(?i)\bMAX\s*HYPERMARKET\b', 'groceries', 'groc_hyper', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'merchant', '(?i)\bMS\s*GRAMEE\s*NATUR|GRAMEE\s*NATUR\b', 'groceries', 'groc_fv', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'merchant', '(?i)\bSKAND\s*ENT|SKANDA\s*ENT\b', 'groceries', 'groc_hyper', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'merchant', '(?i)\bFRUIT\s*BU|USHMA\s*FRUIT\b', 'groceries', 'groc_fv', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\bFRUIT\s*BU|USHMA\s*FRUIT\b', 'groceries', 'groc_fv', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'merchant', '(?i)\bPREM\s*JEWELLERS|JEWELLERS\b', 'shopping', 'shop_clothing', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\bPREM\s*JEWELLERS|JEWELLERS\b', 'shopping', 'shop_clothing', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'merchant', '(?i)\b(ECOMMERCE|SHOPCOM\s*ECO|FZB\s*SHOPCOM|ZENEX\s*ECO)\b', 'shopping', 'shop_marketplaces', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\b(ECOMMERCE|SHOPCOM\s*ECO|FZB\s*SHOPCOM|ZENEX\s*ECO)\b', 'shopping', 'shop_marketplaces', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'merchant', '(?i)\bPOOJA\s*SPORTS|SPORTS\b', 'shopping', 'shop_sports_outdoor', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'description', '(?i)\bPOOJA\s*SPORTS|SPORTS\b', 'shopping', 'shop_sports_outdoor', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'merchant', '(?i)\bRI\s*RUPA\s*LADIES|RUPA\s*LADIES\b', 'shopping', 'shop_clothing', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'description', '(?i)\bRI\s*RUPA\s*LADIES|RUPA\s*LADIES\b', 'shopping', 'shop_clothing', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'merchant', '(?i)\bGIRI\s*NEWS|NEWS\s*PAPER|NEWSPAPER\b', 'utilities', 'util_other', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 25, 'description', '(?i)\bGIRI\s*NEWS|NEWS\s*PAPER|NEWSPAPER\b', 'utilities', 'util_other', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 20, 'merchant', '(?i)\bRELIANCE\s*JIO|JIO\s*IN\b', 'utilities', 'util_mobile', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 20, 'description', '(?i)\bRELIANCE\s*JIO|JIO\s*IN\b', 'utilities', 'util_mobile', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

-- ============================================================================
-- 8) merchant_rules - 064: HP, Hy Stories, TCS, OTT Play
-- ============================================================================
INSERT INTO spendsense.merchant_rules (rule_id, priority, applies_to, pattern_regex, category_code, subcategory_code, active, source, tenant_id, created_by, created_at)
VALUES
    (gen_random_uuid(), 22, 'merchant', '(?i)\b(5\s*HP\s*PETRO|HP\s*PETRO|HP\s*PETROL|HPCL)\b', 'transport', 'tr_fuel', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\b(5\s*HP\s*PETRO|HP\s*PETRO|HP\s*PETROL|HPCL)\b', 'transport', 'tr_fuel', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'merchant', '(?i)\b(HYD\s*STORIE|HY\s*STORIES)\b', 'food_dining', 'fd_cafes', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\b(HYD\s*STORIE|HY\s*STORIES)\b', 'food_dining', 'fd_cafes', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'merchant', '(?i)\bTATA\s*CONSULTANCY\s*SERVICES\b', 'income', 'inc_salary', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 15, 'description', '(?i)\bTATA\s*CONSULTANCY\s*SERVICES|TCS\b', 'income', 'inc_salary', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'merchant', '(?i)\bOTT\s*PLAY\b', 'entertainment', 'ent_movies_ott', true, 'seed', NULL, NULL, now()),
    (gen_random_uuid(), 22, 'description', '(?i)\bOTT\s*PLAY\b', 'entertainment', 'ent_movies_ott', true, 'seed', NULL, NULL, now())
ON CONFLICT DO NOTHING;

COMMIT;
