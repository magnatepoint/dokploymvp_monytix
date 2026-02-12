'use client'

import { useEffect, useState } from 'react'
import type { Session } from '@supabase/supabase-js'
import Image from 'next/image'
import { fetchAccounts } from '@/lib/api/console'
import type { Account } from '@/types/console'
import { glassCardPrimary } from '@/lib/theme/glass'

const BANK_LOGO_MAP: Record<string, string> = {
  icici_bank: '/banks/icici.png',
  hdfc_bank: '/banks/hdfc.png',
  sbi_bank: '/banks/sbi.png',
  kotak_bank: '/banks/kotak.png',
  federal_bank: '/banks/federal.png',
  axis_bank: '/banks/axis.png',
  canara_bank: '/banks/canara.png',
}

function getBankLogoUrl(bankCode: string | undefined): string | null {
  if (!bankCode) return null
  const code = bankCode.toLowerCase().trim()
  return BANK_LOGO_MAP[code] ?? null
}

function BankLogo({ bankCode, fallback }: { bankCode: string | undefined; fallback: React.ReactNode }) {
  const url = getBankLogoUrl(bankCode)
  if (!url) return <>{fallback}</>
  return (
    <Image src={url} alt="" width={48} height={48} className="rounded-lg object-contain" unoptimized />
  )
}

interface AccountsTabProps {
  session: Session
}

export default function AccountsTab({ session }: AccountsTabProps) {
  const [accounts, setAccounts] = useState<Account[]>([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState<string | null>(null)

  const loadAccounts = async () => {
    setLoading(true)
    setError(null)
    try {
      const list = await fetchAccounts(session)
      setAccounts(list)
    } catch (err) {
      setError(err instanceof Error ? err.message : 'Failed to load accounts')
      console.error('Error loading accounts:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    loadAccounts()
  }, [session])

  const formatCurrency = (amount: number) => {
    return new Intl.NumberFormat('en-IN', {
      style: 'currency',
      currency: 'INR',
      maximumFractionDigits: 0,
    }).format(amount)
  }

  const getAccountIcon = (type: string) => {
    const icons: Record<string, string> = {
      CHECKING: '💳',
      SAVINGS: '🏦',
      INVESTMENT: '📈',
      CREDIT: '💳',
    }
    return icons[type] || '💳'
  }

  const getAccountColor = (type: string) => {
    const colors: Record<string, string> = {
      CHECKING: 'text-blue-400',
      SAVINGS: 'text-green-400',
      INVESTMENT: 'text-purple-400',
      CREDIT: 'text-red-400',
    }
    return colors[type] || 'text-gray-400'
  }

  if (loading && accounts.length === 0 && !error) {
    return (
      <div className="max-w-7xl mx-auto space-y-4">
        <h2 className="text-xl font-bold text-white mb-4">Accounts</h2>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className={`${glassCardPrimary} p-4 animate-pulse`}>
              <div className="h-5 bg-white/10 rounded w-1/3 mb-3" />
              <div className="h-8 bg-white/10 rounded w-1/2" />
            </div>
          ))}
        </div>
      </div>
    )
  }

  if (error) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-4">
        <div className="text-red-400 text-center">
          <p className="text-lg font-bold mb-2">Unable to Load Accounts</p>
          <p className="text-sm">{error}</p>
        </div>
        <button
          onClick={loadAccounts}
          className="px-6 py-2 bg-[#D4AF37] text-black rounded-lg font-medium hover:bg-[#D4AF37]/90 transition-colors"
        >
          Retry
        </button>
      </div>
    )
  }

  if (accounts.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center py-20 gap-4">
        <span className="text-5xl">💳</span>
        <p className="text-lg font-semibold text-white">No Accounts</p>
        <p className="text-sm text-gray-400">Upload bank statements to see your accounts</p>
      </div>
    )
  }

  return (
    <div className="max-w-7xl mx-auto space-y-4">
      <h2 className="text-xl font-bold text-white mb-4">Your Accounts</h2>
      {accounts.map((account) => (
        <AccountCard key={account.id} account={account} />
      ))}
    </div>
  )

  function AccountCard({ account }: { account: Account }) {
    const iconFallback = (
      <div className={`text-4xl ${getAccountColor(account.account_type)}`}>
        {getAccountIcon(account.account_type)}
      </div>
    )
    return (
      <div className={`${glassCardPrimary} p-6`}>
        <div className="flex items-center gap-4">
          <div className="flex-shrink-0 w-12 h-12 flex items-center justify-center">
            <BankLogo bankCode={account.bank_code} fallback={iconFallback} />
          </div>
          <div className="flex-1">
            <h3 className="text-lg font-bold text-white mb-1">{account.bank_name}</h3>
            <div className="flex items-center gap-2 text-sm text-gray-400">
              <span>{account.account_type}</span>
              {account.account_number && <span>• {account.account_number}</span>}
            </div>
          </div>
          <div className="text-right">
            <p className="text-lg font-bold text-white">{formatCurrency(account.balance)}</p>
          </div>
        </div>
      </div>
    )
  }
}
