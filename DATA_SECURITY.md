# Data Security Audit — Monytix MVP

Audit against fintech production requirements for encryption, key management, and internal access.

---

## 🔒 Encryption

| Requirement | Status | Notes |
|-------------|--------|-------|
| **TLS 1.2+ enforced** | ✅ | Backend uses HTTPS in production (Dokploy/nginx). Supabase uses TLS. Ktor client uses platform TLS. |
| **AES-256 encryption at rest** | ✅ | Supabase/Postgres: encryption at rest. APK: MPIN stored in `EncryptedSharedPreferences` (AES256-GCM keys, AES256-SIV for pref keys). |
| **Sensitive DB fields encrypted** | ⚠️ | Supabase provides DB encryption. No application-level field encryption for PII (account numbers, etc.). Consider for high-sensitivity fields. |
| **Secrets not hardcoded in app** | ⚠️ | Supabase URL/key loaded from `local.properties` (gitignored). Fallback `"your-key"` in `build.gradle.kts` when file missing — **release must fail if missing**. |

---

## 🔑 Key Management

| Requirement | Status | Notes |
|-------------|--------|-------|
| **KMS used (AWS/GCP)** | ⚠️ | Supabase manages DB keys. Backend uses `SUPABASE_JWT_SECRET` from env. No explicit KMS integration. |
| **Keys rotated periodically** | ⚠️ | Manual process. Document rotation procedure for `SUPABASE_JWT_SECRET`, `SUPABASE_SERVICE_ROLE_KEY`. |
| **No keys in GitHub** | ✅ | `.env`, `local.properties`, `*.pem`, `gmail-pubsub-key.json` in `.gitignore`. No secrets in committed code. |

---

## 🧑‍💻 Internal Access

| Requirement | Status | Notes |
|-------------|--------|-------|
| **Production DB not accessible publicly** | ✅ | Supabase: connection pooler, IP allowlists. `POSTGRES_URL` from env, not exposed. |
| **Role-based access control** | ✅ | JWT-based auth; `user_id` scopes all queries. Supabase RLS on Postgres. |
| **Audit logs enabled** | ✅ | `spendsense.audit_log` table; `persist_audit()` for data export, account delete, consent withdrawal. |
| **Admin actions logged** | ⚠️ | No admin panel yet. Add audit logging when implementing admin/support actions. |

---

## Implemented Fixes

1. **MPIN hashing**: Replaced weak `hashCode()` with SHA-256 + salt (AES-256-GCM storage unchanged).
2. **Release build**: Use `-PciRelease` when building release in CI; build fails if secrets missing.
3. **Audit logging**: `backend/app/core/audit.py` — `persist_audit(pool, action, user_id, details)` writes to `spendsense.audit_log`. Migration `070_create_audit_log.sql`. `/auth/export-data` endpoint demonstrates usage.

---

## Recommendations

### High priority
- [ ] Use build flavors so release never uses `local.properties` fallbacks; inject via CI secrets.
- [ ] Add audit table and log: data export, account deletion, consent withdrawal, login from new device.
- [ ] Consider KMS (e.g. AWS Secrets Manager) for production secrets.

### Medium priority
- [ ] Field-level encryption for high-sensitivity columns (e.g. account numbers) if required by compliance.
- [ ] Document key rotation runbook for `SUPABASE_JWT_SECRET` and service role key.
- [x] Restrict `network_security_config` cleartext to debug builds only — done (`src/debug/` vs `src/main/`).

### Low priority
- [ ] Re-KYC and consent withdrawal: wire to backend and log in audit.
