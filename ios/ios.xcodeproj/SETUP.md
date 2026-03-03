# Monytix iOS - Setup Guide

## Prerequisites

- Xcode 15.0 or later
- iOS 17.0+ deployment target
- Swift 5.9+
- CocoaPods or Swift Package Manager

## Installation

1. Clone the repository
2. Open `ios.xcodeproj` in Xcode
3. Select your development team in project settings
4. Build and run (Cmd+R)

## Configuration

### API Configuration
The app connects to the Monytix backend API. Configuration is handled in `BackendApi.swift`.

### Firebase/Auth Setup (required for auth)
1. In [Firebase Console](https://console.firebase.google.com/), open project **monytix-79dac** (or your project).
2. Add an iOS app if needed: Project Settings → Your apps → Add app → iOS. Use your app’s bundle ID (e.g. same as in Xcode).
3. Download **GoogleService-Info.plist** and add it to the Xcode project:
   - In Xcode: right‑click the **ios** group (the app target folder) → Add Files to "ios"… → select `GoogleService-Info.plist`.
   - Leave "Copy items if needed" checked and ensure the **ios** target is checked.
4. Without this file the app will still launch, but Firebase Auth (and any Firebase features) will not work.

### Google Sign-In (optional)
5. For "Continue with Google" to work: in Firebase Console enable the **Google** sign-in method (Authentication → Sign-in method). Then open your **GoogleService-Info.plist** and copy the value of **REVERSED_CLIENT_ID** (e.g. `com.googleusercontent.apps.636445644991-xxxx.apps.googleusercontent.com`). In the Xcode project, open **Info.plist** (or Target → Info → URL Types) and set the URL scheme to that value (replace `com.googleusercontent.apps.REPLACE_WITH_YOUR_REVERSED_CLIENT_ID`).

## Architecture

- **SwiftUI** for all UI components
- **Swift Concurrency** (async/await) for networking
- **Observation Framework** (@Observable) for state management
- **Design System**: MonytixColors, MonytixTypography, MonytixLayout

## Design System

### Colors
- Primary Accent: Cyan/Teal (`MonytixColors.accentPrimary`)
- Secondary Accent: Purple (`MonytixColors.accentSecondary`)
- Background: Dark gradient theme

### Typography
All text styles defined in `MonytixTypography.swift`

### Layout
Consistent spacing and sizing in `MonytixLayout.swift`

## Key Components

- **PreAuthView**: Splash, onboarding, terms, device verification
- **AuthView**: Phone OTP, email/password, Google Sign-In, Sign in with Apple
- **HomeView**: Dashboard with financial insights
- **BudgetPilotView**: Budget tracking and management
- **GoalTrackerView**: Financial goal tracking
- **ProfileView**: User settings and preferences

## Building for Release

1. Update version in `Info.plist`
2. Configure signing & capabilities
3. Archive: Product → Archive
4. Upload to App Store Connect

## Troubleshooting

### Build Errors
- Clean Build Folder: Cmd+Shift+K
- Delete Derived Data: `rm -rf ~/Library/Developer/Xcode/DerivedData`
- Restart Xcode

### Common Issues
- Missing API keys: Check environment configuration
- Signing errors: Verify development team selection
- Missing dependencies: Ensure all packages are resolved

### Runtime / console logs
- **Firebase Analytics**: "Network fetch failed", "TLS error", "Socket is not connected" to `app-analytics-services.com` are common in Simulator or behind VPN/proxy. Analytics will retry; the app still works. Test on a real device on normal Wi‑Fi if you need analytics to connect immediately.
- **"App Delegate does not conform to UIApplicationDelegate"**: Expected with SwiftUI’s `@main` App; Firebase logs this when it can’t swizzle a traditional app delegate. Safe to ignore.
- **Sign in with Apple**: `AKAuthenticationError -7026` or `AuthorizationError 1000` with "process may not map database" (–54) often happens in **Simulator**. Enable "Sign in with Apple" for your App ID in [Apple Developer](https://developer.apple.com/account) → Identifiers → your App ID → Capabilities, add the capability in Xcode (Signing & Capabilities), then test on a **real device**; Simulator support is limited.

## Support

For issues or questions, contact the development team.
