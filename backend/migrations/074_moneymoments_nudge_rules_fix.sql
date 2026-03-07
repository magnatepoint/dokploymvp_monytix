-- Money Moments Nudge Rules Fix (plan: soften single rule, add broader rule set)
-- 1) Soften DINING_3PLUS_WEEK and reduce cooldown
-- 2) Add DINING_HIGH_WANTS (stronger) + template
-- 3) Add DINING_2PLUS_WEEK, SHOPPING_3PLUS_WEEK, WANTS_SHARE_HIGH_30D, GOAL_UNDERFUNDED + templates

BEGIN;

-- 1. Update existing rule: softer conditions, shorter cooldown
UPDATE moneymoments.mm_nudge_rule_master
SET
  trigger_conditions_json = '{"dining_txn_7d_min": 3}'::jsonb,
  cooldown_days = 5,
  description = 'Dining frequency nudge (3+ times this week)',
  updated_at = NOW()
WHERE rule_id = 'DINING_3PLUS_WEEK';

-- 2. Insert DINING_HIGH_WANTS (stronger: dining + wants_share), cooldown 7
INSERT INTO moneymoments.mm_nudge_rule_master (
  rule_id, name, description, target_domain, segment_criteria_json,
  trigger_conditions_json, score_formula_json, cooldown_days, daily_cap, priority, active
)
VALUES (
  'DINING_HIGH_WANTS',
  'Dining + high wants share',
  'Dining plus elevated wants share',
  'dining',
  '{}'::jsonb,
  '{"dining_txn_7d_min": 3, "wants_share_30d_min": 0.30}'::jsonb,
  '{}'::jsonb,
  7,
  1,
  10,
  TRUE
)
ON CONFLICT (rule_id) DO UPDATE SET
  trigger_conditions_json = EXCLUDED.trigger_conditions_json,
  cooldown_days = EXCLUDED.cooldown_days,
  priority = EXCLUDED.priority,
  updated_at = NOW();

-- Template for DINING_HIGH_WANTS (reuse same copy as DINING_3PLUS_WEEK)
INSERT INTO moneymoments.mm_nudge_template_master (
  template_code, rule_id, channel, locale, title_template, body_template,
  cta_text, cta_deeplink, humor_style, active
)
VALUES (
  'HUMOR_DINING_HIGH_WANTS',
  'DINING_HIGH_WANTS',
  'in_app',
  'en-IN',
  'Skip just one dinner out = ₹{{save}} closer to {{goal}}',
  'Looks tasty… but your {{goal}} is hungrier 😋 Trim one dine-out this week and park ₹{{save}} to your {{goal}}.',
  'Adjust Budget',
  'monytix://budget/adjust',
  'witty',
  TRUE
)
ON CONFLICT (template_code) DO NOTHING;

-- 3. Additional rules (with current signal only)
INSERT INTO moneymoments.mm_nudge_rule_master (
  rule_id, name, description, target_domain, segment_criteria_json,
  trigger_conditions_json, score_formula_json, cooldown_days, daily_cap, priority, active
)
VALUES
  ('DINING_2PLUS_WEEK', 'Dining 2+ times this week', 'Light dining awareness nudge', 'dining', '{}'::jsonb, '{"dining_txn_7d_min": 2}'::jsonb, '{}'::jsonb, 3, 1, 6, TRUE),
  ('SHOPPING_3PLUS_WEEK', 'Shopping 3+ times this week', 'Shopping habit awareness', 'shopping', '{}'::jsonb, '{"shopping_txn_7d_min": 3}'::jsonb, '{}'::jsonb, 4, 1, 7, TRUE),
  ('WANTS_SHARE_HIGH_30D', 'High wants share this month', 'Wants share is elevated', 'general', '{}'::jsonb, '{"wants_share_30d_min": 0.35}'::jsonb, '{}'::jsonb, 5, 1, 8, TRUE),
  ('GOAL_UNDERFUNDED', 'Top goal underfunded', 'Goal progress needs attention', 'general', '{}'::jsonb, '{"rank1_goal_underfund_amt_min": 1000}'::jsonb, '{}'::jsonb, 5, 1, 9, TRUE)
ON CONFLICT (rule_id) DO UPDATE SET
  trigger_conditions_json = EXCLUDED.trigger_conditions_json,
  cooldown_days = EXCLUDED.cooldown_days,
  priority = EXCLUDED.priority,
  updated_at = NOW();

-- Templates for each new rule (generic in_app)
INSERT INTO moneymoments.mm_nudge_template_master (
  template_code, rule_id, channel, locale, title_template, body_template,
  cta_text, cta_deeplink, humor_style, active
)
VALUES
  ('HUMOR_DINING_2PLUS', 'DINING_2PLUS_WEEK', 'in_app', 'en-IN',
   'A little less dining out = ₹{{save}} for {{goal}}',
   'You have been dining out a bit this week. Skip one and put ₹{{save}} toward {{goal}}.',
   'Adjust Budget', 'monytix://budget/adjust', 'friendly', TRUE),
  ('HUMOR_SHOPPING_3PLUS', 'SHOPPING_3PLUS_WEEK', 'in_app', 'en-IN',
   'Small cut in shopping = ₹{{save}} for {{goal}}',
   'A few shopping trips this week. Trim one and move ₹{{save}} to {{goal}}.',
   'View Budget', 'monytix://budget', 'friendly', TRUE),
  ('HUMOR_WANTS_HIGH', 'WANTS_SHARE_HIGH_30D', 'in_app', 'en-IN',
   'Wants share is high – redirect ₹{{save}} to {{goal}}?',
   'Your wants share is up this month. Consider shifting ₹{{save}} to {{goal}}.',
   'Adjust Budget', 'monytix://budget/adjust', 'friendly', TRUE),
  ('HUMOR_GOAL_UNDERFUNDED', 'GOAL_UNDERFUNDED', 'in_app', 'en-IN',
   '{{goal}} could use a little more',
   'Your top goal is behind target. Add a small amount and get back on track.',
   'View Goals', 'monytix://goals', 'friendly', TRUE)
ON CONFLICT (template_code) DO NOTHING;

COMMIT;
