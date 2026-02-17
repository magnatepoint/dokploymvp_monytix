-- ============================================================================
-- dim_category and dim_subcategory - Schema & Seed Data
-- Current state after migrations 001, 015, 040, 047
-- ============================================================================

SET search_path TO spendsense, public;

-- ============================================================================
-- 1) dim_category - Table Definition
-- ============================================================================

CREATE TABLE IF NOT EXISTS spendsense.dim_category (
    category_code   VARCHAR(32) PRIMARY KEY,
    category_name   VARCHAR(64) NOT NULL,
    txn_type        VARCHAR(12) NOT NULL CHECK (txn_type IN (
        'income', 'needs', 'wants', 'assets', 'debt', 'protection',
        'transfer', 'fees', 'tax', 'charity', 'business'
    )),
    display_order   SMALLINT NOT NULL DEFAULT 100,
    active          BOOLEAN NOT NULL DEFAULT TRUE,
    user_id         UUID,                                    -- NULL = system category
    is_custom       BOOLEAN NOT NULL DEFAULT FALSE
);

-- Unique: system categories (user_id IS NULL) have unique category_code
CREATE UNIQUE INDEX IF NOT EXISTS idx_dim_category_code_system
    ON spendsense.dim_category(category_code) WHERE user_id IS NULL;

-- Unique: custom categories (user_id IS NOT NULL) unique per (category_code, user_id)
CREATE UNIQUE INDEX IF NOT EXISTS idx_dim_category_code_user
    ON spendsense.dim_category(category_code, user_id) WHERE user_id IS NOT NULL;


-- ============================================================================
-- 2) dim_subcategory - Table Definition
-- ============================================================================

CREATE TABLE IF NOT EXISTS spendsense.dim_subcategory (
    subcategory_code  VARCHAR(48) PRIMARY KEY,
    category_code     VARCHAR(32) NOT NULL REFERENCES spendsense.dim_category(category_code) ON UPDATE CASCADE,
    subcategory_name  VARCHAR(80) NOT NULL,
    display_order     SMALLINT NOT NULL DEFAULT 100,
    active            BOOLEAN NOT NULL DEFAULT TRUE,
    user_id           UUID,                                    -- NULL = system subcategory
    is_custom         BOOLEAN NOT NULL DEFAULT FALSE
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_dim_subcategory_code_system
    ON spendsense.dim_subcategory(subcategory_code) WHERE user_id IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS idx_dim_subcategory_code_user
    ON spendsense.dim_subcategory(subcategory_code, user_id) WHERE user_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS ix_dim_subcategory_category
    ON spendsense.dim_subcategory(category_code);


-- ============================================================================
-- 3) dim_category - Seed Data (system categories, user_id IS NULL)
-- ============================================================================

INSERT INTO spendsense.dim_category (category_code, category_name, txn_type, display_order, active, user_id, is_custom)
VALUES
    ('income', 'Income', 'income', 5, TRUE, NULL, FALSE),
    ('transfers_in', 'Transfers In', 'transfer', 7, TRUE, NULL, FALSE),
    ('transfers_out', 'Transfers Out', 'transfer', 8, TRUE, NULL, FALSE),
    ('loans_payments', 'Loan Payments (EMIs)', 'debt', 10, TRUE, NULL, FALSE),
    ('investments_commitments', 'Regular Investments / Commitments', 'assets', 12, TRUE, NULL, FALSE),
    ('insurance_premiums', 'Insurance Premiums', 'protection', 14, TRUE, NULL, FALSE),
    ('housing_fixed', 'Housing (Rent & Society)', 'needs', 16, TRUE, NULL, FALSE),
    ('utilities', 'Utilities', 'needs', 18, TRUE, NULL, FALSE),
    ('entertainment', 'Entertainment & OTT', 'wants', 30, TRUE, NULL, FALSE),
    ('food_dining', 'Food & Dining / Nightlife', 'wants', 32, TRUE, NULL, FALSE),
    ('groceries', 'Groceries', 'needs', 34, TRUE, NULL, FALSE),
    ('medical', 'Medical & Healthcare', 'needs', 36, TRUE, NULL, FALSE),
    ('fitness', 'Fitness & Sports', 'wants', 38, TRUE, NULL, FALSE),
    ('transport', 'Transport & Travel', 'needs', 40, TRUE, NULL, FALSE),
    ('shopping', 'Shopping & Retail', 'wants', 42, TRUE, NULL, FALSE),
    ('education', 'Education', 'needs', 44, TRUE, NULL, FALSE),
    ('child_care', 'Child Care', 'needs', 46, TRUE, NULL, FALSE),
    ('motor_maintenance', 'Motor Maintenance', 'needs', 48, TRUE, NULL, FALSE),
    ('pets', 'Pets', 'wants', 50, TRUE, NULL, FALSE),
    ('banks', 'Bank Interest & Fees', 'fees', 60, TRUE, NULL, FALSE),
    ('govt_tax', 'Government Taxes', 'tax', 62, TRUE, NULL, FALSE),
    ('charity_donations', 'Charity & Donations', 'charity', 70, TRUE, NULL, FALSE),
    ('festivals_rituals', 'Festivals, Rituals & Celebrations', 'wants', 72, TRUE, NULL, FALSE),
    ('family_support', 'Family Support & Obligations', 'needs', 74, TRUE, NULL, FALSE),
    ('business_expenses', 'Business & Freelance Expenses', 'business', 80, TRUE, NULL, FALSE),
    ('govt_benefits', 'Government Benefits & Subsidies', 'income', 82, TRUE, NULL, FALSE)
ON CONFLICT (category_code) DO UPDATE SET
    category_name = EXCLUDED.category_name,
    txn_type = EXCLUDED.txn_type,
    display_order = EXCLUDED.display_order,
    active = EXCLUDED.active;


-- ============================================================================
-- 4) dim_subcategory - Seed Data (system subcategories, user_id IS NULL)
-- ============================================================================

-- INCOME
INSERT INTO spendsense.dim_subcategory (subcategory_code, category_code, subcategory_name, display_order, active, user_id, is_custom)
VALUES
    ('inc_salary', 'income', 'Salary / Payroll', 10, TRUE, NULL, FALSE),
    ('inc_side', 'income', 'Side Income / Freelance', 11, TRUE, NULL, FALSE),
    ('inc_business', 'income', 'Business Income', 12, TRUE, NULL, FALSE),
    ('inc_interest', 'income', 'Interest Income', 13, TRUE, NULL, FALSE),
    ('inc_dividend', 'income', 'Dividends', 14, TRUE, NULL, FALSE),
    ('inc_tax_refund', 'income', 'Tax Refund', 15, TRUE, NULL, FALSE),
    ('inc_other', 'income', 'Other Income', 19, TRUE, NULL, FALSE)
ON CONFLICT (subcategory_code) DO UPDATE SET category_code = EXCLUDED.category_code, subcategory_name = EXCLUDED.subcategory_name, display_order = EXCLUDED.display_order, active = EXCLUDED.active;

-- TRANSFERS
INSERT INTO spendsense.dim_subcategory (subcategory_code, category_code, subcategory_name, display_order, active, user_id, is_custom)
VALUES
    ('tr_in_deposit', 'transfers_in', 'Cash/Cheque/ATM Deposit', 10, TRUE, NULL, FALSE),
    ('tr_in_savings', 'transfers_in', 'Savings Sweep / Sweep In', 12, TRUE, NULL, FALSE),
    ('tr_in_invest_ret', 'transfers_in', 'From Investments / PF / Retirement', 14, TRUE, NULL, FALSE),
    ('tr_in_internal', 'transfers_in', 'Account Transfer In', 16, TRUE, NULL, FALSE),
    ('tr_in_other', 'transfers_in', 'Other Transfer In', 18, TRUE, NULL, FALSE),
    ('tr_out_savings', 'transfers_out', 'Transfer to Savings/Joint', 10, TRUE, NULL, FALSE),
    ('tr_out_atm', 'transfers_out', 'Cash / ATM Withdrawal', 12, TRUE, NULL, FALSE),
    ('tr_out_wallet', 'transfers_out', 'Bank → Wallet / UPI', 14, TRUE, NULL, FALSE),
    ('tr_out_sweep', 'transfers_out', 'Sweep Out (FD / Linked)', 16, TRUE, NULL, FALSE),
    ('tr_out_other', 'transfers_out', 'Other Transfer Out', 18, TRUE, NULL, FALSE)
ON CONFLICT (subcategory_code) DO UPDATE SET category_code = EXCLUDED.category_code, subcategory_name = EXCLUDED.subcategory_name, display_order = EXCLUDED.display_order, active = EXCLUDED.active;

-- LOANS / INVESTMENTS / INSURANCE
INSERT INTO spendsense.dim_subcategory (subcategory_code, category_code, subcategory_name, display_order, active, user_id, is_custom)
VALUES
    ('loan_home', 'loans_payments', 'Home Loan EMI', 10, TRUE, NULL, FALSE),
    ('loan_car', 'loans_payments', 'Car Loan EMI', 11, TRUE, NULL, FALSE),
    ('loan_bike', 'loans_payments', 'Two-Wheeler EMI', 12, TRUE, NULL, FALSE),
    ('loan_personal', 'loans_payments', 'Personal Loan EMI', 13, TRUE, NULL, FALSE),
    ('loan_student', 'loans_payments', 'Education Loan EMI', 14, TRUE, NULL, FALSE),
    ('loan_cc_bill', 'loans_payments', 'Credit Card Bill Payment', 15, TRUE, NULL, FALSE),
    ('loan_other', 'loans_payments', 'Other Loan/Debt Payment', 19, TRUE, NULL, FALSE),
    ('inv_sip', 'investments_commitments', 'Mutual Fund SIP', 10, TRUE, NULL, FALSE),
    ('inv_nps', 'investments_commitments', 'NPS', 11, TRUE, NULL, FALSE),
    ('inv_fd_rd', 'investments_commitments', 'FD / RD', 12, TRUE, NULL, FALSE),
    ('inv_ppf', 'investments_commitments', 'PPF', 13, TRUE, NULL, FALSE),
    ('inv_stocks', 'investments_commitments', 'Stocks / ETFs', 14, TRUE, NULL, FALSE),
    ('inv_gold', 'investments_commitments', 'Gold / SGB', 15, TRUE, NULL, FALSE),
    ('ins_life', 'insurance_premiums', 'Life Insurance', 10, TRUE, NULL, FALSE),
    ('ins_health', 'insurance_premiums', 'Health Insurance', 11, TRUE, NULL, FALSE),
    ('ins_motor', 'insurance_premiums', 'Motor Insurance', 12, TRUE, NULL, FALSE),
    ('ins_home_other', 'insurance_premiums', 'Home / Other Insurance', 13, TRUE, NULL, FALSE)
ON CONFLICT (subcategory_code) DO UPDATE SET category_code = EXCLUDED.category_code, subcategory_name = EXCLUDED.subcategory_name, display_order = EXCLUDED.display_order, active = EXCLUDED.active;

-- HOUSING / UTILITIES
INSERT INTO spendsense.dim_subcategory (subcategory_code, category_code, subcategory_name, display_order, active, user_id, is_custom)
VALUES
    ('house_rent', 'housing_fixed', 'Rent', 10, TRUE, NULL, FALSE),
    ('house_society', 'housing_fixed', 'Society / Maintenance', 11, TRUE, NULL, FALSE),
    ('house_maid', 'housing_fixed', 'Maid / Security / House Help', 12, TRUE, NULL, FALSE),
    ('util_electricity', 'utilities', 'Electricity', 10, TRUE, NULL, FALSE),
    ('util_water', 'utilities', 'Water', 11, TRUE, NULL, FALSE),
    ('util_gas_lpg', 'utilities', 'Gas / LPG / PNG', 12, TRUE, NULL, FALSE),
    ('util_broadband', 'utilities', 'Internet / Broadband', 13, TRUE, NULL, FALSE),
    ('util_mobile', 'utilities', 'Mobile / Telephone', 14, TRUE, NULL, FALSE),
    ('util_dth_cable', 'utilities', 'DTH / Cable TV', 15, TRUE, NULL, FALSE),
    ('util_sewage_waste', 'utilities', 'Sewage / Waste Mgmt', 16, TRUE, NULL, FALSE),
    ('util_other', 'utilities', 'Other Utilities', 19, TRUE, NULL, FALSE)
ON CONFLICT (subcategory_code) DO UPDATE SET category_code = EXCLUDED.category_code, subcategory_name = EXCLUDED.subcategory_name, display_order = EXCLUDED.display_order, active = EXCLUDED.active;

-- ENTERTAINMENT / FOOD / GROCERIES
INSERT INTO spendsense.dim_subcategory (subcategory_code, category_code, subcategory_name, display_order, active, user_id, is_custom)
VALUES
    ('ent_movies_ott', 'entertainment', 'Movies & TV / OTT', 10, TRUE, NULL, FALSE),
    ('ent_music', 'entertainment', 'Music & Audio', 11, TRUE, NULL, FALSE),
    ('ent_gaming', 'entertainment', 'Video Games & eSports', 12, TRUE, NULL, FALSE),
    ('ent_sports_events', 'entertainment', 'Sporting Events & Tickets', 13, TRUE, NULL, FALSE),
    ('ent_amusement', 'entertainment', 'Amusement Parks & Events', 14, TRUE, NULL, FALSE),
    ('ent_museums_arts', 'entertainment', 'Museums & Art Exhibitions', 15, TRUE, NULL, FALSE),
    ('ent_casinos', 'entertainment', 'Casinos & Gambling', 16, TRUE, NULL, FALSE),
    ('ent_adventure', 'entertainment', 'Adventure & Recreation', 17, TRUE, NULL, FALSE),
    ('ent_nightlife', 'entertainment', 'Nightlife & Parties', 18, TRUE, NULL, FALSE),
    ('ent_cultural', 'entertainment', 'Cultural & Festive Events', 19, TRUE, NULL, FALSE),
    ('ent_other', 'entertainment', 'Other Entertainment', 20, TRUE, NULL, FALSE),
    ('fd_quick_service', 'food_dining', 'Quick Service / Fast Food', 10, TRUE, NULL, FALSE),
    ('fd_fine', 'food_dining', 'Fine & Casual Dining', 11, TRUE, NULL, FALSE),
    ('fd_cafes', 'food_dining', 'Cafés & Bakeries', 12, TRUE, NULL, FALSE),
    ('fd_pubs_bars', 'food_dining', 'Pubs & Bars', 13, TRUE, NULL, FALSE),
    ('fd_street_food', 'food_dining', 'Street Food & Local Eateries', 14, TRUE, NULL, FALSE),
    ('fd_pan_shop', 'food_dining', 'Pan / Cigarette Shop', 15, TRUE, NULL, FALSE),
    ('fd_online', 'food_dining', 'Online Food Delivery (Swiggy/Zomato)', 16, TRUE, NULL, FALSE),
    ('fd_desserts', 'food_dining', 'Desserts & Sweet Shops', 17, TRUE, NULL, FALSE),
    ('groc_hyper', 'groceries', 'Hypermarkets / Department Stores', 10, TRUE, NULL, FALSE),
    ('groc_online', 'groceries', 'Online Groceries / Q-commerce', 11, TRUE, NULL, FALSE),
    ('groc_fv', 'groceries', 'Vegetable & Fruit Stores', 12, TRUE, NULL, FALSE),
    ('groc_meat', 'groceries', 'Meat / Poultry / Seafood', 13, TRUE, NULL, FALSE)
ON CONFLICT (subcategory_code) DO UPDATE SET category_code = EXCLUDED.category_code, subcategory_name = EXCLUDED.subcategory_name, display_order = EXCLUDED.display_order, active = EXCLUDED.active;

-- MEDICAL / FITNESS / TRANSPORT
INSERT INTO spendsense.dim_subcategory (subcategory_code, category_code, subcategory_name, display_order, active, user_id, is_custom)
VALUES
    ('med_dental', 'medical', 'Dental Care', 10, TRUE, NULL, FALSE),
    ('med_eye', 'medical', 'Eye Care / Optometry', 11, TRUE, NULL, FALSE),
    ('med_general', 'medical', 'General / Hospitals / Nursing', 12, TRUE, NULL, FALSE),
    ('med_pharma', 'medical', 'Pharmacies & Supplements', 13, TRUE, NULL, FALSE),
    ('med_apps', 'medical', 'Medical Apps / Services', 14, TRUE, NULL, FALSE),
    ('med_other', 'medical', 'Other Medical', 19, TRUE, NULL, FALSE),
    ('fit_gyms', 'fitness', 'Gyms & Fitness Centers', 10, TRUE, NULL, FALSE),
    ('fit_sports', 'fitness', 'Sports & Coaching / Gear', 11, TRUE, NULL, FALSE),
    ('tr_apps', 'transport', 'Transport Apps (Uber/Ola etc.)', 10, TRUE, NULL, FALSE),
    ('tr_public', 'transport', 'Public Transit (Rail/Metro/Bus)', 11, TRUE, NULL, FALSE),
    ('tr_taxis', 'transport', 'Taxis / Auto / Ride-share', 12, TRUE, NULL, FALSE),
    ('tr_tolls', 'transport', 'Tolls / FASTag', 13, TRUE, NULL, FALSE),
    ('tr_travel', 'transport', 'Flights / Bus / Train / Cruise', 14, TRUE, NULL, FALSE),
    ('tr_lodging', 'transport', 'Hotels / Stays / Airbnb', 15, TRUE, NULL, FALSE),
    ('tr_fuel', 'transport', 'Fuel / Petrol / Diesel', 16, TRUE, NULL, FALSE),
    ('tr_other', 'transport', 'Other Transport', 19, TRUE, NULL, FALSE)
ON CONFLICT (subcategory_code) DO UPDATE SET category_code = EXCLUDED.category_code, subcategory_name = EXCLUDED.subcategory_name, display_order = EXCLUDED.display_order, active = EXCLUDED.active;

-- SHOPPING / EDUCATION / CHILD CARE / MOTOR / PETS / BANKS / TAX
INSERT INTO spendsense.dim_subcategory (subcategory_code, category_code, subcategory_name, display_order, active, user_id, is_custom)
VALUES
    ('shop_clothing', 'shopping', 'Clothing & Accessories', 10, TRUE, NULL, FALSE),
    ('shop_electronics', 'shopping', 'Electronics & Gadgets', 11, TRUE, NULL, FALSE),
    ('shop_marketplaces', 'shopping', 'Online Marketplaces', 12, TRUE, NULL, FALSE),
    ('shop_beauty', 'shopping', 'Beauty & Personal Care', 13, TRUE, NULL, FALSE),
    ('shop_stationery', 'shopping', 'Stationery & Office Supplies', 14, TRUE, NULL, FALSE),
    ('shop_home_kitchen', 'shopping', 'Home & Kitchen / Furnishings', 15, TRUE, NULL, FALSE),
    ('shop_gifts', 'shopping', 'Gifts & Novelties', 16, TRUE, NULL, FALSE),
    ('shop_books_media', 'shopping', 'Books & Media', 17, TRUE, NULL, FALSE),
    ('shop_hobbies', 'shopping', 'Hobbies & Crafts', 18, TRUE, NULL, FALSE),
    ('shop_pet_supplies', 'shopping', 'Pet Supplies', 19, TRUE, NULL, FALSE),
    ('shop_sports_outdoor', 'shopping', 'Sports & Outdoor', 20, TRUE, NULL, FALSE),
    ('shop_auto_supplies', 'shopping', 'Automotive Supplies', 21, TRUE, NULL, FALSE),
    ('shop_children_toys', 'shopping', 'Children & Toys', 22, TRUE, NULL, FALSE),
    ('shop_general', 'shopping', 'General Merchandise', 29, TRUE, NULL, FALSE),
    ('edu_school_fees', 'education', 'School / College Fees', 10, TRUE, NULL, FALSE),
    ('edu_tuition', 'education', 'Tuition / Coaching', 11, TRUE, NULL, FALSE),
    ('edu_online', 'education', 'Online Courses / Certifications', 12, TRUE, NULL, FALSE),
    ('child_education', 'child_care', 'Child Education Expenses', 10, TRUE, NULL, FALSE),
    ('child_daycare', 'child_care', 'Daycare / Babysitting', 11, TRUE, NULL, FALSE),
    ('motor_services', 'motor_maintenance', 'General Services / Repairs', 10, TRUE, NULL, FALSE),
    ('motor_insurance', 'motor_maintenance', 'Motor Insurance', 11, TRUE, NULL, FALSE),
    ('pet_grooming', 'pets', 'Grooming / Boarding / Bathing', 10, TRUE, NULL, FALSE),
    ('pet_food', 'pets', 'Pet Food', 11, TRUE, NULL, FALSE),
    ('pet_vaccine', 'pets', 'Vaccination / Vet', 12, TRUE, NULL, FALSE),
    ('pet_insurance', 'pets', 'Pet Insurance', 13, TRUE, NULL, FALSE),
    ('bank_interest', 'banks', 'Interest Credit (Bank)', 10, TRUE, NULL, FALSE),
    ('bank_charges', 'banks', 'Bank Charges / Fees', 11, TRUE, NULL, FALSE),
    ('bank_sweep', 'banks', 'Savings Sweep / Auto', 12, TRUE, NULL, FALSE),
    ('tax_income', 'govt_tax', 'Income Tax / TDS', 10, TRUE, NULL, FALSE),
    ('tax_gst', 'govt_tax', 'GST / Challan', 11, TRUE, NULL, FALSE),
    ('tax_other', 'govt_tax', 'Other Government Taxes', 19, TRUE, NULL, FALSE)
ON CONFLICT (subcategory_code) DO UPDATE SET category_code = EXCLUDED.category_code, subcategory_name = EXCLUDED.subcategory_name, display_order = EXCLUDED.display_order, active = EXCLUDED.active;

-- CHARITY / FESTIVALS / FAMILY / BUSINESS / GOVT BENEFITS (from migration 047)
INSERT INTO spendsense.dim_subcategory (subcategory_code, category_code, subcategory_name, display_order, active, user_id, is_custom)
VALUES
    ('char_temple', 'charity_donations', 'Temple / Puja / Hundi', 10, TRUE, NULL, FALSE),
    ('char_mosque', 'charity_donations', 'Mosque / Zakat / Sadaqah', 11, TRUE, NULL, FALSE),
    ('char_church', 'charity_donations', 'Church Offerings', 12, TRUE, NULL, FALSE),
    ('char_gurudwara', 'charity_donations', 'Gurudwara / Langar / Seva', 13, TRUE, NULL, FALSE),
    ('char_ngo', 'charity_donations', 'NGOs & Social Causes', 14, TRUE, NULL, FALSE),
    ('char_crowdfund', 'charity_donations', 'Crowdfunding / Medical Help', 15, TRUE, NULL, FALSE),
    ('char_other', 'charity_donations', 'Other Donations & Offerings', 19, TRUE, NULL, FALSE),
    ('fest_pooja_items', 'festivals_rituals', 'Puja Items & Ritual Needs', 10, TRUE, NULL, FALSE),
    ('fest_decor', 'festivals_rituals', 'Decorations & Lights', 11, TRUE, NULL, FALSE),
    ('fest_sweets', 'festivals_rituals', 'Sweets & Snacks', 12, TRUE, NULL, FALSE),
    ('fest_clothing', 'festivals_rituals', 'Festival Clothing & Wear', 13, TRUE, NULL, FALSE),
    ('fest_gifts', 'festivals_rituals', 'Festival Gifts', 14, TRUE, NULL, FALSE),
    ('fest_pandal', 'festivals_rituals', 'Pandal / Mandap & Events', 15, TRUE, NULL, FALSE),
    ('fest_other', 'festivals_rituals', 'Other Festival Expenses', 19, TRUE, NULL, FALSE),
    ('fam_parents', 'family_support', 'Monthly Support to Parents/In-laws', 10, TRUE, NULL, FALSE),
    ('fam_relatives', 'family_support', 'Support to Siblings/Relatives', 11, TRUE, NULL, FALSE),
    ('fam_medical_help', 'family_support', 'Helping Family with Medical Bills', 12, TRUE, NULL, FALSE),
    ('fam_education_help', 'family_support', 'Paying Relatives'' Education Fees', 13, TRUE, NULL, FALSE),
    ('fam_other', 'family_support', 'Other Family Support', 19, TRUE, NULL, FALSE),
    ('biz_raw_materials', 'business_expenses', 'Materials / Inventory', 10, TRUE, NULL, FALSE),
    ('biz_tools_software', 'business_expenses', 'Tools, SaaS, Domains, Hosting', 11, TRUE, NULL, FALSE),
    ('biz_marketing', 'business_expenses', 'Ads, Marketing, Campaigns', 12, TRUE, NULL, FALSE),
    ('biz_travel', 'business_expenses', 'Client Travel & Stays', 13, TRUE, NULL, FALSE),
    ('biz_salary', 'business_expenses', 'Salaries/Stipends to Staff', 14, TRUE, NULL, FALSE),
    ('biz_rent', 'business_expenses', 'Office / Shop Rent', 15, TRUE, NULL, FALSE),
    ('biz_other', 'business_expenses', 'Other Business Expenses', 19, TRUE, NULL, FALSE),
    ('govt_subsidy', 'govt_benefits', 'LPG, Fertilizer & Other Subsidies', 10, TRUE, NULL, FALSE),
    ('govt_scheme', 'govt_benefits', 'PM Kisan, Pensions, Welfare Schemes', 11, TRUE, NULL, FALSE),
    ('govt_other_benefits', 'govt_benefits', 'Other Government Benefits', 19, TRUE, NULL, FALSE)
ON CONFLICT (subcategory_code) DO UPDATE SET category_code = EXCLUDED.category_code, subcategory_name = EXCLUDED.subcategory_name, display_order = EXCLUDED.display_order, active = EXCLUDED.active;


-- ============================================================================
-- 5) Query: List categories with subcategory count
-- ============================================================================

/*
SELECT
    c.category_code,
    c.category_name,
    c.txn_type,
    c.display_order,
    COUNT(s.subcategory_code) AS subcategory_count
FROM spendsense.dim_category c
LEFT JOIN spendsense.dim_subcategory s ON s.category_code = c.category_code AND s.active = TRUE
WHERE c.active = TRUE AND c.user_id IS NULL
GROUP BY c.category_code, c.category_name, c.txn_type, c.display_order
ORDER BY c.display_order;
*/
