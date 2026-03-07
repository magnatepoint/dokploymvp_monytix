# Why the new UI (MolyConsole / Future / Assistant) might not show

The **new UI** (command-center Home, Financial Future tab, Ask MONYTIX assistant) lives inside `MainContent`, which is only shown after these steps:

1. **Signed in** – Firebase auth; otherwise you see PreAuthScreen.
2. **MPIN set** – After first sign-in you must set an MPIN; until then you see SetMpinScreen.
3. **Biometric decided** – You must tap “Enable” or “Skip” on the biometrics screen once.
4. **App block clear** – `AppBlockState` must be `Ready`. Otherwise you see:
   - **Internet error** – device has no (or no validated) internet
   - **Maintenance** – backend config says `maintenance_mode: true`
   - **Server down** – backend health check failed (unreachable or error)

## What to do

- **Stuck on “Service temporarily unavailable” (server down)**  
  - Tap **“Continue anyway”** to bypass and open the app; the new UI will load. API calls will fail until the backend is reachable.  
  - **Debug builds:** If the backend is not running, the app now automatically goes to `Ready` in debug, so you can reach the new UI without a running backend (see `AppBlockViewModel`).

- **Stuck on Set MPIN or Biometrics**  
  Complete those screens once (set MPIN, then enable or skip biometrics). After that you’ll get to the main app.

- **Backend not reachable**  
  - Emulator: use `10.0.2.2` for host machine (e.g. in `local.properties`: `BACKEND_URL=http://10.0.2.2:8000`).  
  - Device: use your machine’s LAN IP or a deployed backend URL in `local.properties`.

Once all gates pass (or you tap “Continue anyway” / use a debug build), you should see the bottom nav with **Home (MolyConsole)**, **Future**, **Spend**, **Goals**, **Budget**, **Moments**, **Profile**. The new UI is Home (command center), Future tab, and the Ask MONYTIX FAB/sheet.
