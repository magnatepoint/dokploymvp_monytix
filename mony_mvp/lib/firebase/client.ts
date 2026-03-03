import { initializeApp, getApps, type FirebaseApp } from 'firebase/app'
import { getAuth, type User } from 'firebase/auth'
import type { Session } from '@/lib/auth/types'

const firebaseConfig = {
  apiKey: process.env.NEXT_PUBLIC_FIREBASE_API_KEY,
  authDomain: process.env.NEXT_PUBLIC_FIREBASE_AUTH_DOMAIN,
  projectId: process.env.NEXT_PUBLIC_FIREBASE_PROJECT_ID,
  storageBucket: process.env.NEXT_PUBLIC_FIREBASE_STORAGE_BUCKET,
  messagingSenderId: process.env.NEXT_PUBLIC_FIREBASE_MESSAGING_SENDER_ID,
  appId: process.env.NEXT_PUBLIC_FIREBASE_APP_ID,
}

function getApp(): FirebaseApp | null {
  if (typeof window === 'undefined') return null
  const apps = getApps()
  if (apps.length > 0) return apps[0] as FirebaseApp
  if (!firebaseConfig.apiKey || !firebaseConfig.projectId) return null
  return initializeApp(firebaseConfig)
}

export function getFirebaseAuth() {
  if (typeof window === 'undefined') return null
  const app = getApp()
  return app ? getAuth(app) : null
}

/** Build a Session from a Firebase User for API calls (Bearer = Firebase ID token). */
export function sessionFromFirebaseUser(user: User): Session {
  return {
    getValidToken: () => user.getIdToken(true),
    user: {
      id: user.uid,
      email: user.email ?? null,
    },
  }
}
