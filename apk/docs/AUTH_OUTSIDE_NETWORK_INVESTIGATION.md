# Auth "Outside Network" / Blank Login Screen – Investigation

## What happens

When logging in (e.g. "Continue with Google") **outside your usual network** (e.g. mobile data or another Wi‑Fi), the in-app browser tab can show a **blank screen** at the Supabase URL (`https://<project>.supabase.co`). Supabase dashboard and redirect URLs are correctly set.

## Auth flow in the app

1. User taps "Continue with Google" → `AuthViewModel.signInWithGoogle()` → `supabase.signInWith(Google)`.
2. **Supabase Auth (auth-kt)** builds the OAuth URL and opens it in the **external browser** (default; Custom Tabs was disabled to improve redirect handling).
3. User signs in on Google; Supabase then redirects to **`monytix://auth`** with the session (fragment or query).
4. **Android** should deliver that URL to the app via the deep link; **MainActivity** calls `Supabase.client.handleDeeplinks(intent)` in `onCreate` and `onNewIntent`, so the SDK parses the session and the app logs the user in.

So the blank screen is the **Custom Tab** content (Supabase/Google auth page) not loading or not finishing correctly when "outside the network".

## Possible causes (in order of likelihood)

### 1. Custom Tabs + redirect not handed back to the app

- **Known Android/Custom Tabs behavior**: When the OAuth provider redirects to a custom scheme (`monytix://auth`), that redirect is not always delivered to the app when the flow runs inside **Chrome Custom Tabs** (no user gesture, or browser handling of app links).
- **Effect**: The tab can stay on a blank or “redirect” page instead of closing and returning to the app, so it looks like "blank screen" or "stuck".
- **Fix to try**: Use the **external browser** for OAuth instead of Custom Tabs. The app is currently forced to Custom Tabs in `SupabaseClient.kt`. If the auth-kt default is external browser, removing that override (or switching to `ExternalAuthAction.Browser()` if the API exposes it) can improve redirect handling on some networks/devices.

### 2. Network/DNS/TLS on the path to Supabase

- On some networks (e.g. corporate, VPN, or restrictive mobile data), requests from **Custom Tabs** to Supabase might:
  - Be blocked or altered (proxy/firewall).
  - Hit DNS or TLS issues (e.g. intercepting certs, blocking `supabase.co`).
- **Effect**: The first Supabase page (or a redirect) never loads → blank screen.
- **Check**: On the **same** network where it fails, open `https://<your-project-ref>.supabase.co` in the **normal** browser (Chrome/Safari). If that also fails or is blank, the issue is network/DNS/TLS to Supabase, not the app.

### 3. Supabase project paused or wrong URL

- Free-tier Supabase projects can **pause**; the project URL then returns a generic or blank page.
- **Check**: In [Supabase Dashboard](https://supabase.com/dashboard), confirm the project is **active** and the URL matches `SUPABASE_URL` in `local.properties` (and thus `BuildConfig.SUPABASE_URL` in the app). The app opens whatever URL is in `BuildConfig.SUPABASE_URL` at build time.

### 4. Redirect URL mismatch

- Supabase must redirect to **exactly** the scheme/host the app uses: **`monytix://auth`** (no path).
- **Check**: In Supabase → **Authentication** → **URL Configuration** → **Redirect URLs**, you should have `monytix://auth`.
- **App side**: `SupabaseClient.kt` uses `scheme = "monytix"`, `host = "auth"`. The manifest has `<data android:scheme="monytix" android:host="auth" />`. No path is set, so `monytix://auth` and `monytix://auth#...` are both valid.

### 5. Deep link not opening the app (intent filter)

- If the redirect were to a wrong scheme/host, or if the app weren’t the default handler, the system might not open the app and the user could stay on a blank or error page in the browser.
- **Check**: Confirm the app is built with the same `applicationId` and that you’re testing the same build where `monytix://auth` is configured. No need to change intent filters if Supabase is configured to `monytix://auth` and the app declares that scheme/host.

### 6. Backend URL only reachable on one network

- After login, the app calls your **backend** (`BACKEND_URL`, e.g. `https://api.monytix.ai`). If that URL is only reachable on your office/home network, then **outside the network** the app might:
  - Still complete Supabase login (Custom Tab might close and app get the session).
  - Then fail on the first API call (e.g. `/auth/session` or `/health`), which can look like "login didn’t work".
- So: if the **browser** tab is blank, the main suspect is 1–3 above. If the tab **does** close and the app opens but then shows an error or empty state, also verify `BACKEND_URL` is reachable from that network (or use a public URL and set it in `local.properties` and rebuild).

## Changes made in the repo

1. **SUPABASE_SETUP.md**  
   - Section **"Login not working outside my network / blank auth screen"** added with checks and steps (Supabase URL, VPN, project paused, redirect URL, backend URL).

2. **AuthScreen.kt**  
   - Hint under the Google button: *"If the login page doesn't load, check your network or try without VPN."*

3. **SupabaseClient.kt (optional)**  
   - You can try **using the default external auth action** (external browser instead of Custom Tabs) by removing or commenting out:
     - `defaultExternalAuthAction = ExternalAuthAction.CustomTabs()`
   - If the auth-kt default is external browser, OAuth will open in the system browser; when Supabase redirects to `monytix://auth`, Android should open the app. If the API requires an explicit value, use the one that means “open in external browser” (e.g. `ExternalAuthAction.Browser()` if present in your auth-kt version).

## What to do next

1. **Reproduce on the bad network**  
   - Open `https://<your-project-ref>.supabase.co` in the **normal** browser (not in the app).  
   - If it’s blank or errors there too → focus on network/VPN/Supabase project (2–3).  
   - If it loads there but is blank only in the app → focus on Custom Tabs and redirect handling (1, 4, 5).

2. **Try external browser**  
   - In `SupabaseClient.kt`, remove or change `defaultExternalAuthAction` so the app uses the external browser for OAuth (if your auth-kt version allows).  
   - Rebuild, test again on the same “outside network” scenario.

3. **Verify Supabase**  
   - Project not paused; redirect URL is exactly `monytix://auth`; no typos in `SUPABASE_URL` in `local.properties` and in the dashboard.

4. **Backend reachability**  
   - If login seems to complete but the app then fails, check that `BACKEND_URL` is reachable from the current network (or switch to a public backend URL and rebuild).

### 7. Mobile data / carrier DNS issues

- **Symptom**: Works on WiFi but fails on mobile data (4G/5G), VPN off.
- **Cause**: Some mobile carriers block or misresolve domains (e.g. `supabase.co`, `api.monytix.ai`), or use DNS that blocks Cloudflare.
- **Fix on Android**: Settings → Network & internet → Private DNS → set to `dns.google` or `one.one.one.one` (uses DNS over TLS, bypasses carrier DNS).
- **Diagnostic**: On mobile data, open `https://api.monytix.ai/health` in Chrome. If it fails there too, it's a network/carrier issue.
- **Debug logs**: Check `adb logcat -s BackendApi` for exact error. "Unable to resolve host" → DNS/ISP block; "Connection timed out" → firewall/carrier block.

### 8. Production: Custom domain for Supabase

- **Long-term fix**: If carriers block `supabase.co`, set up a **Custom Domain** (e.g. `auth.monytix.ai`) in Supabase Dashboard → Authentication → URL Configuration. Point your app to the custom domain instead of `*.supabase.co`. Custom domains are less likely to be blocked.
