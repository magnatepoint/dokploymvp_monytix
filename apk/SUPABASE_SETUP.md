# Supabase & Backend Setup for Monytix App

## 1. Create a Supabase Project

1. Go to [supabase.com](https://supabase.com) and create a new project
2. In your project dashboard, go to **Settings** → **API** to get your credentials

## 2. Configure Credentials

Add your Supabase credentials to `local.properties` in the apk directory:

```properties
SUPABASE_URL=https://vwagtikpxbhjrffolrqn.supabase.co
# Use publishable key (sb_publishable_xxx) for mobile apps, or anon key
SUPABASE_ANON_KEY=your-publishable-or-anon-key
# Or: SUPABASE_PUBLISHABLE_KEY=your-publishable-key
```

Get your key from **Supabase Dashboard** → **Settings** → **API**:
- **Publishable key** (recommended for mobile): `sb_publishable_xxx` format
- **Anon key** (legacy): also works for client-side

**Important:** Do not commit `local.properties` to version control (it should be in `.gitignore`).

## 3. Configure Auth Redirect URL

For email confirmation links and OAuth to work:

1. Go to **Authentication** → **URL Configuration** in your Supabase dashboard
2. Add this to **Redirect URLs**: `monytix://auth`

## 4. Enable Email Auth

1. Go to **Authentication** → **Providers** in your Supabase dashboard
2. Ensure **Email** provider is enabled
3. Configure email templates if desired (Settings → Auth → Email Templates)

## 4b. Enable Google Sign-In (Optional)

1. In [Google Cloud Console](https://console.cloud.google.com/apis/credentials), create OAuth 2.0 credentials:
   - **Application type**: Web application (for Supabase callback)
   - **Authorized JavaScript origins**: Add `https://vwagtikpxbhjrffolrqn.supabase.co` and your Site URL
   - **Authorized redirect URIs**: Add `https://vwagtikpxbhjrffolrqn.supabase.co/auth/v1/callback`
2. Copy the **Client ID** and **Client Secret**
3. In Supabase: **Authentication** → **Providers** → **Google** → Enable and paste Client ID + Secret
4. Ensure redirect URL `monytix://auth` is in **Authentication** → **URL Configuration** → **Redirect URLs**

## 5. Backend Connection

Add your backend URL to `local.properties`:

```properties
# Default (no BACKEND_URL): uses http://34.14.136.76:8001
# Local dev - emulator: BACKEND_URL=http://10.0.2.2:8000
# Local dev - physical device: BACKEND_URL=http://YOUR_IP:8000
BACKEND_URL=http://34.14.136.76:8001
```

Start the backend from the project root:
```bash
cd backend && ./start.sh
```

The app will call `/health` and `/auth/session` (with Supabase JWT) to verify the connection.

## 6. Todos Table (Optional)

To enable the TodoList on the Home screen, run the SQL in `apk/supabase_todos.sql` in your Supabase SQL Editor.

## 7. Build and Run

```bash
./gradlew assembleDebug
```

The app will show the login/registration screen when not authenticated, and the main app (with TodoList + backend status) when signed in.
