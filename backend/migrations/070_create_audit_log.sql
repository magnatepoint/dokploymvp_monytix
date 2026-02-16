-- ============================================================================
-- Migration 070: Create audit_log for fintech compliance (sensitive actions)
-- ============================================================================

BEGIN;

CREATE TABLE IF NOT EXISTS spendsense.audit_log (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  occurred_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
  action TEXT NOT NULL,
  user_id UUID NOT NULL,
  details JSONB DEFAULT '{}',
  ip_address TEXT,
  created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_audit_log_user_id ON spendsense.audit_log(user_id);
CREATE INDEX IF NOT EXISTS idx_audit_log_occurred_at ON spendsense.audit_log(occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_log_action ON spendsense.audit_log(action);

COMMENT ON TABLE spendsense.audit_log IS 'Audit trail for sensitive actions (data export, account delete, consent withdrawal, etc.)';

COMMIT;
