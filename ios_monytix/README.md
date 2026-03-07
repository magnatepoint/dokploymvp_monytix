# ios_monytix

Monytix iOS app — design system, auth (login/registration), and main UI. Same auth + backend logic as the Android APK.

## Project structure

```
ios_monytix/
├── App/                 # App entry, Firebase, Google Sign-In
│   ├── ios_monytixApp.swift
│   └── AppDelegate.swift
├── Auth/                # Login / registration
│   ├── AuthManager.swift
│   └── AuthScreen.swift
├── Views/               # Main screens (post-login)
│   └── ContentView.swift
├── Theme/               # Design tokens (colors, motion, chart)
│   ├── MonytixTheme.swift
│   ├── MonytixChartTokens.swift
│   └── MonytixMotion.swift
├── Components/          # Reusable UI (buttons, cards, loader)
│   ├── MonytixComponents.swift
│   └── MonytixRingLoader.swift
├── Charts/              # Chart views
│   ├── WeekdayHeatmap.swift
│   └── RankedBars.swift
├── Utilities/           # Helpers, backend API, config
│   ├── MonytixUtilities.swift
│   ├── BackendConfig.swift   # BACKEND_URL (same as APK)
│   └── BackendApi.swift      # GET /auth/session, health
├── Assets.xcassets/     # Images, colors, app icon
└── Docs/                # Project docs
    ├── README.md
    ├── QUICK_START.md
    └── …
```

## Auth (same as APK)

- **Firebase Auth** for email/password and Google Sign-In.
- **Backend** `GET /auth/session` validates the Firebase ID token (Bearer); use `AuthManager.shared.getIdToken()` for all API calls.
- Backend base URL: set `BACKEND_URL` in Info.plist or it defaults to `https://backend.monytix.ai`.

**Setup:** Add `GoogleService-Info.plist` from the Firebase Console (same project as Android). For Google Sign-In, add the URL scheme from `REVERSED_CLIENT_ID` in that plist to the app’s Info (or use a custom Info.plist that merges it).

## Build

Open `ios_monytix.xcodeproj` in Xcode, resolve packages, add `GoogleService-Info.plist`, then run (⌘R).

## Local backend and “Service unavailable (530)”

If you see **Service unavailable (530)** in the app, the API host (e.g. `https://backend.monytix.ai`) or its tunnel is unreachable. Your **local** backend logs (e.g. `GET /health 200` from 127.0.0.1) are from requests hitting the server directly (e.g. another client or a different base URL).

To use your **local** backend from the iOS app:

1. In Xcode: **Edit Scheme → Run → Arguments → Environment Variables** (or add to Info.plist):
   - `BACKEND_URL` = `http://127.0.0.1:8000` (or your backend port; no trailing slash).
2. Run the backend on that host/port, then run the app. Health and all API calls will go to localhost.

The **nw_connection_copy_connected_*** console messages are from iOS’s network stack when something queries connection metadata before the connection is established. They are harmless and often appear when many requests start at once; the app defers the health check slightly to reduce that.

**Other harmless simulator messages:** `BSSettings initWithXPCDictionary`, `profileActivation`, `ShellSceneKit.ProfileActivation` and “Ignoring attempt to add focus items in already-visited container” are from the system/simulator (SwiftUI focus and settings). You can ignore them; they do not indicate a bug in the app.

## Performance (slow launch / lag)

- **Test in Release:** In Xcode choose **Product → Scheme → Edit Scheme → Run → Build Configuration = Release**. Debug builds are much slower; Release often fixes perceived lag.
- **Launch:** Firebase is configured in app `init()`; Google Sign-In is configured on first use (when the user taps “Continue with Google”) so launch stays light.
- **First screen:** Sample data is static; no heavy work or repeated `print()` in hot paths. If you add API/dashboard loading, do it off the main thread and show a skeleton first.

## Docs

See the `Docs/` folder for setup, theme reference, and quick start.
