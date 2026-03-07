-- Money Moments Nudge Pipeline Diagnostics (run these to validate funnel)
-- Schema: mm_signal_daily uses as_of_date; mm_nudge_delivery_log uses sent_at; rules use trigger_conditions_json
--
-- Funnel validation order:
-- 1. Users with mm_signal_daily today (query below)
-- 2. Users matching at least one rule -> use mm_nudge_candidate (candidates by status)
-- 3. Users blocked by suppression -> mm_user_suppression (muted_until, daily_cap)
-- 4. Users blocked by cooldown -> engine skips when same rule_id delivered within cooldown_days
-- 5. Template resolution -> each rule must have in_app active template
-- 6. Rows in mm_nudge_delivery_log = final delivery count
--
-- App verification (if nudges don't show on mobile):
-- - GET /v1/moneymoments/nudges returns rows from mm_nudge_delivery_log for authenticated user.
-- - Ensure firebase_uid_to_uuid(user_id) matches user_id in delivery_log (real auth, not mock).
-- - Run POST /signals/compute, POST /nudges/evaluate, POST /nudges/process so candidates become deliveries.

-- 1) How many active rules?
SELECT count(*) AS rules
FROM moneymoments.mm_nudge_rule_master
WHERE active = TRUE;

-- 2) Rules and thresholds
SELECT rule_id, name, priority, trigger_conditions_json, cooldown_days
FROM moneymoments.mm_nudge_rule_master
WHERE active = TRUE
ORDER BY priority DESC;

-- 3) Are daily signals populated?
SELECT as_of_date AS signal_date, count(*) AS users_with_signals
FROM moneymoments.mm_signal_daily
GROUP BY 1
ORDER BY 1 DESC
LIMIT 14;

-- 4) Delivery volume by day
SELECT date(sent_at) AS dt, count(*) AS nudges
FROM moneymoments.mm_nudge_delivery_log
GROUP BY 1
ORDER BY 1 DESC
LIMIT 30;

-- 5) Delivery volume by rule
SELECT rule_id, count(*) AS deliveries
FROM moneymoments.mm_nudge_delivery_log
GROUP BY rule_id
ORDER BY 2 DESC;

-- 6) Users who satisfy the softer dining rule today (dining 3+ only)
SELECT count(*) AS eligible_users
FROM moneymoments.mm_signal_daily
WHERE as_of_date = current_date
  AND dining_txn_7d >= 3;

-- 7) Current strict rule (dining 3+ AND wants_share AND underfund >= 1000)
SELECT count(*) AS eligible_users_strict
FROM moneymoments.mm_signal_daily
WHERE as_of_date = current_date
  AND dining_txn_7d >= 3
  AND COALESCE(wants_share_30d, 0) >= 0.30
  AND COALESCE(rank1_goal_underfund_amt, 0) >= 1000;

-- Funnel: candidates by status (run after evaluate)
SELECT status, count(*) AS cnt
FROM moneymoments.mm_nudge_candidate
WHERE as_of_date = current_date
GROUP BY status;

-- Users with signal today
SELECT count(*) AS users_with_signal_today
FROM moneymoments.mm_signal_daily
WHERE as_of_date = current_date;
