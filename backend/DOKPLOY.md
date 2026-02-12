# Deploy Backend to Dokploy

This guide walks you through deploying the MVP backend to [Dokploy](https://dokploy.com).

## Prerequisites

- Dokploy installed on your server (`curl -sSL https://dokploy.com/install.sh | sh`)
- PostgreSQL database (e.g. [Supabase](https://supabase.com))
- A domain (optional; for HTTPS via Dokploy)

## Quick Start

### 1. Create Docker Compose Project in Dokploy

1. Log in to your Dokploy dashboard
2. Create a new project (e.g. "MVP")
3. Add a **Compose** service
4. Choose **Docker Compose** (not Stack)
5. Configure source:
   - **Provider**: GitHub / GitLab / Git
   - **Repository**: your repo URL
   - **Branch**: `main` (or your default branch)
   - **Compose Path**: `backend/docker-compose.dokploy.yml`
   - **Build Context**: Leave default (Dokploy uses the repo root; compose path indicates backend folder)

### 2. Set Environment Variables

In the **Environment** tab, add these variables (Dokploy creates a `.env` file):

| Variable | Required | Description |
|----------|----------|-------------|
| `POSTGRES_URL` | Yes | PostgreSQL connection string (e.g. Supabase: `postgresql://postgres.xxx:password@aws-0-region.pooler.supabase.com:6543/postgres`) |
| `SUPABASE_URL` | Yes | Supabase project URL |
| `SUPABASE_ANON_KEY` | Yes | Supabase anonymous key |
| `SUPABASE_SERVICE_ROLE_KEY` | Yes | Supabase service role key |
| `SUPABASE_JWT_SECRET` | Yes | Supabase JWT secret (for signing tokens) |
| `FRONTEND_ORIGIN` | Yes | Frontend URL for CORS. Use full URL: `https://mvp1.monytix.ai` or `https://app.yourdomain.com` (hostname-only values like `mvp1.monytix.ai` are auto-prefixed with `https://`) |
| `ENVIRONMENT` | No | Default: `production` |
| `APP_PORT` | No | Host port (default: `8001`) |

**Optional (Gmail integration):** `GMAIL_CLIENT_ID`, `GMAIL_CLIENT_SECRET`, `GMAIL_REDIRECT_URI`. If omitted, Gmail routes return 503. See `.env.production.example`.

### 3. Configure Domain (Optional)

1. Go to the **Domains** tab
2. Click **Add Domain**
3. Select the **backend** service
4. Enter your API domain (e.g. `api.yourdomain.com`)
5. Enable HTTPS (Dokploy uses Let's Encrypt)
6. Point your DNS A record to the Dokploy server IP

### 4. Deploy

1. Click **Deploy**
2. Wait for the build and startup
3. Check **Logs** for any errors
4. Run migrations once (see below)

## Run Migrations

After the first deploy, run migrations against your database:

```bash
# From your machine (with POSTGRES_URL in env)
cd backend
export POSTGRES_URL="postgresql://..."
python deploy/scripts/run_migrations.py
```

Or use a one-off Dokploy container if you have console access.

## Architecture

The compose file runs:

- **backend**: FastAPI (uvicorn) on port 8001
- **celery-worker**: Background task processing (statement ingestion, enrichment)
- **celery-beat**: Scheduled tasks
- **redis**: Message broker and cache

All services use **named volumes** (`backend-logs`, `backend-data`, `redis-data`) so data persists across deployments.

## Troubleshooting

### Build fails

- Ensure `backend/Dockerfile` and `backend/requirements.txt` exist
- Check build logs for Python dependency errors

### Backend can't connect to PostgreSQL

- Verify `POSTGRES_URL` is correct and the database allows connections from your Dokploy server IP
- For Supabase: use the **Connection pooling** URL (port 6543) for server-side apps

### Celery tasks not running

- Ensure Redis is healthy (check Logs for `mvp-redis`)
- Verify `CELERY_BROKER_URL` and `REDIS_URL` resolve to `redis://redis:6379/0` (redis = service name)

### Domain not resolving

- Wait 1–2 minutes after deploy for Traefik to pick up the route
- Confirm DNS A record points to the Dokploy server
- Use **Preview Compose** in Dokploy to see the generated Traefik labels
