'use client'

import { useEffect, useState } from 'react'
import { createClient } from '@/lib/supabase/client'
import SplashScreen from '@/components/SplashScreen'
import AuthScreen from '@/components/AuthScreen'
import MolyConsole from '@/components/MolyConsole'
import SpendSense from '@/components/SpendSense'
import GoalTracker from '@/components/goaltracker/GoalTracker'
import BudgetPilot from '@/components/budgetpilot/BudgetPilot'
import MoneyMoments from '@/components/moneymoments/MoneyMoments'
import Settings from '@/components/settings/Settings'
import Navigation from '@/components/navigation/Navigation'
import type { Session } from '@supabase/supabase-js'

export type Screen = 'molyconsole' | 'spendsense' | 'goaltracker' | 'budgetpilot' | 'moneymoments' | 'settings'

export default function Home() {
  const [showSplash, setShowSplash] = useState(true)
  const [session, setSession] = useState<Session | null>(null)
  const [loading, setLoading] = useState(true)
  const [currentScreen, setCurrentScreen] = useState<Screen>('molyconsole')
  const supabase = createClient()

  useEffect(() => {
    // Check for existing session
    supabase.auth.getSession().then(({ data: { session } }) => {
      setSession(session)
      setLoading(false)
    })

    // Listen for auth changes
    const {
      data: { subscription },
    } = supabase.auth.onAuthStateChange((_event, session) => {
      setSession(session)
      if (session) {
        // Validate session with backend
        validateSessionWithBackend(session)
      }
    })

    return () => subscription.unsubscribe()
  }, [])

  const validateSessionWithBackend = async (session: Session) => {
    const rawApiUrl = process.env.NEXT_PUBLIC_API_URL || 'https://api.monytix.ai'
    let API_BASE_URL = rawApiUrl.split('/').slice(0, 3).join('/')
    const urlObj = new URL(API_BASE_URL)
    const isWrongHostname = urlObj.hostname === 'mvp.monytix.ai' || urlObj.hostname.includes('mvp.monytix.ai')
    if (isWrongHostname) API_BASE_URL = 'https://api.monytix.ai'
    const BACKUP_URL = 'https://backend.monytix.ai'

    const tryValidate = async (base: string) => {
      const res = await fetch(`${base}/auth/session`, {
        method: 'GET',
        headers: { Authorization: `Bearer ${session.access_token}` },
      })
      return res.ok
    }

    try {
      let ok = await tryValidate(API_BASE_URL)
      if (!ok && API_BASE_URL !== BACKUP_URL) {
        console.warn('[Debug] Primary backend failed, trying backup:', BACKUP_URL)
        ok = await tryValidate(BACKUP_URL)
      }
      if (ok) {
        console.log('Session validated with backend')
      } else {
        console.warn('Session validation failed')
        await supabase.auth.signOut()
        setSession(null)
      }
    } catch (error) {
      if (API_BASE_URL !== BACKUP_URL) {
        try {
          const ok = await tryValidate(BACKUP_URL)
          if (ok) { console.log('Session validated with backup backend'); return }
        } catch (_) {}
      }
      console.error('Failed to validate session:', error)
    }
  }

  const handleSplashComplete = () => {
    setShowSplash(false)
  }

  const handleSignOut = async () => {
    await supabase.auth.signOut()
    setSession(null)
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
    return <AuthScreen />
  }

  // User is authenticated - show current screen with navigation
  return (
    <div className="relative min-h-screen">
      {/* Navigation Component */}
      <Navigation
        currentScreen={currentScreen}
        session={session}
        onNavigate={(screen) => setCurrentScreen(screen)}
        onSignOut={handleSignOut}
      />

      {/* Main Content Area */}
      <div className="min-h-screen bg-[#2E2E2E]">
        {/* Desktop: Add left margin for sidebar */}
        <div className="md:ml-64">
          {/* Mobile: Add bottom padding for bottom nav */}
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
