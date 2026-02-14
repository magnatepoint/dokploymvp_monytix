-- ============================================================================
-- Migration 069: Create ml_merchant_alias for user feedback (merchant corrections)
--
-- Migration 041 tried to CREATE TABLE merchant_alias with user_id, merchant_hash,
-- but merchant_alias already existed from 013 (merchant_id, alias, normalized_alias).
-- CREATE TABLE IF NOT EXISTS did nothing. This creates the intended table as
-- ml_merchant_alias for ML merchant feedback / user corrections.
-- ============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS spendsense.ml_merchant_alias (
  alias_id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id UUID,
  merchant_hash TEXT NOT NULL,
  alias_pattern TEXT NOT NULL,
  normalized_name TEXT NOT NULL,
  channel_override TEXT,
  usage_count INTEGER NOT NULL DEFAULT 0,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  UNIQUE (user_id, merchant_hash)
);

CREATE INDEX IF NOT EXISTS idx_ml_merchant_alias_user_hash
  ON spendsense.ml_merchant_alias(user_id, merchant_hash);

CREATE INDEX IF NOT EXISTS idx_ml_merchant_alias_global
  ON spendsense.ml_merchant_alias(merchant_hash)
  WHERE user_id IS NULL;

COMMENT ON TABLE spendsense.ml_merchant_alias IS 'User and global merchant name/channel overrides from ML feedback';

COMMIT;
