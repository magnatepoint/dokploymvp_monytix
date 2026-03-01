# Firebase Auth Migration

Auth has been migrated from Supabase to Firebase across backend, APK, and iOS.

## Backend Setup

1. **Add environment variables** to `backend/.env`:
   ```
   FIREBASE_PROJECT_ID=monytix-79dac
   GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json
   ```
   Or use `GOOGLE_CREDENTIALS_JSON` with the service account JSON as a string (for containers).

2. **Firebase service account**: Download from [Firebase Console](https://console.firebase.google.com) → Project Settings → Service Accounts → Generate new private key.

3. **Optional**: Keep Supabase vars for backward compatibility during transition. Backend tries Firebase first, then Supabase for token verification.

## APK Setup

1. **google-services.json** is already in `apk/app/`. Ensure it matches your Firebase project.

2. **Web Client ID** for Google Sign-In is in `apk/app/src/main/res/values/strings.xml` as `default_web_client_id`. Update if your Firebase project uses a different OAuth client.

3. **Phone Auth**: Enable Phone authentication in Firebase Console → Authentication → Sign-in method.

4. **Build**: `./gradlew assembleDebug`

## iOS Setup

1. **Add GoogleService-Info.plist**: Download from Firebase Console → Project Settings → Your iOS app → Download `GoogleService-Info.plist`. Add it to the ios target in Xcode.

2. **Firebase package**: The project uses Firebase iOS SDK via SPM. Resolve packages in Xcode (File → Packages → Resolve Package Versions).

3. **Google Sign-In**: The current AuthViewModel shows a placeholder for Google. To enable: add `GoogleSignIn` package and implement the OAuth flow, then create Firebase credential with `GoogleAuthProvider.credential(withIDToken:accessToken:)`.

4. **Phone Auth**: May require `AuthUIDelegate` for reCAPTCHA. Pass a delegate that presents `SFSafariViewController` if `nil` causes issues.

## User ID Mapping

- **Firebase**: `user_id` = Firebase UID (`sub` claim in ID token)
- **Supabase (legacy)**: `user_id` = Supabase UUID
- Existing Supabase users will need to sign up again with Firebase; data is keyed by `user_id`.

## Backend Auth Flow

1. Client sends `Authorization: Bearer <firebase_id_token>` on API requests
2. Backend verifies token via Firebase Admin SDK
3. Extracts `sub` as `user_id` for all operations
