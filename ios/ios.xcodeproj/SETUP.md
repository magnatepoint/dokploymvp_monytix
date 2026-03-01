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

### Firebase/Auth Setup
If using Firebase, add your `GoogleService-Info.plist` to the project root.

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

## Support

For issues or questions, contact the development team.
