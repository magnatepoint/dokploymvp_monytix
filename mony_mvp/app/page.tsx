'use client'

import { useEffect, useState } from 'react'
import SplashScreen from '@/components/SplashScreen'
import AuthScreen from '@/components/AuthScreen'
import MolyConsole from '@/components/MolyConsole'
import SpendSense from '@/components/SpendSense'
import GoalTracker from '@/components/goaltracker/GoalTracker'
import BudgetPilot from '@/components/budgetpilot/BudgetPilot'
import MoneyMoments from '@/components/moneymoments/MoneyMoments'
import Settings from '@/components/settings/Settings'
import Navigation from '@/components/navigation/Navigation'
import type { Session } from '@/lib/auth/types'
import { getFirebaseAuth } from '@/lib/firebase/client'
import { sessionFromFirebaseUser } from '@/lib/firebase/client'
import { fetchWithAuth } from '@/lib/api/client'

export type Screen = 'molyconsole' | 'spendsense' | 'goaltracker' | 'budgetpilot' | 'moneymoments' | 'settings'

export default function Home() {
  const [showSplash, setShowSplash] = useState(true)
  const [session, setSession] = useState<Session | null>(null)
  const [loading, setLoading] = useState(true)
  const [currentScreen, setCurrentScreen] = useState<Screen>('molyconsole')

  useEffect(() => {
    const auth = getFirebaseAuth()
    if (!auth) {
      setLoading(false)
      return
    }
    const unsubscribe = auth.onAuthStateChanged(async (user) => {
      if (user) {
        const sess = sessionFromFirebaseUser(user)
        setSession(sess)
        try {
          await fetchWithAuth(sess, '/auth/session')
          console.log('Session validated with backend')
        } catch {
          console.warn('Session validation failed')
          await auth.signOut()
          setSession(null)
        }
      } else {
        setSession(null)
      }
      setLoading(false)
    })
    return () => unsubscribe()
  }, [])

  const handleSplashComplete = () => {
    setShowSplash(false)
  }

  const handleSignOut = async () => {
    const auth = getFirebaseAuth()
    if (auth) await auth.signOut()
    setSession(null)
  }

  const handleAuthSuccess = (sess: Session) => {
    setSession(sess)
  }

  if (showSplash) {
    return <SplashScreen onComplete={handleSplashComplete} />
  }

  if (loading) {
    return (
      <div className="flex min-h-screen items-center justify-center bg-black">
        <div className="text-white">Loading...</div>
      </div>
    )
  }

  if (!session) {
    return <AuthScreen onSuccess={handleAuthSuccess} />
  }

  return (
    <div className="relative min-h-screen">
      <Navigation
        currentScreen={currentScreen}
        session={session}
        onNavigate={(screen) => setCurrentScreen(screen)}
        onSignOut={handleSignOut}
      />

      <div className="min-h-screen bg-[#2E2E2E]">
        <div className="md:ml-64">
          <div className="pb-16 md:pb-0">
            {currentScreen === 'molyconsole' && (
              <MolyConsole session={session} onSignOut={handleSignOut} />
            )}
            {currentScreen === 'spendsense' && <SpendSense session={session} />}
            {currentScreen === 'goaltracker' && <GoalTracker session={session} />}
            {currentScreen === 'budgetpilot' && <BudgetPilot session={session} />}
            {currentScreen === 'moneymoments' && <MoneyMoments session={session} />}
            {currentScreen === 'settings' && (
              <Settings session={session} onSignOut={handleSignOut} />
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
