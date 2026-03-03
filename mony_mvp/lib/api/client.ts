import type { Session } from '@/lib/auth/types'

// Validate API URL - should not contain paths like /auth/callback
const rawApiUrl = process.env.NEXT_PUBLIC_API_URL || 'https://backend.monytix.ai'
let API_BASE_URL = rawApiUrl.split('/').slice(0, 3).join('/') // Remove any paths, keep only protocol://host
// Ensure no trailing slash
API_BASE_URL = API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL

// Check if API URL is incorrectly configured
const urlObj = new URL(API_BASE_URL)
const isWrongHostname = urlObj.hostname === 'mvp.monytix.ai' || urlObj.hostname.includes('mvp.monytix.ai')
const hasPath = rawApiUrl !== API_BASE_URL

// If hostname is wrong, use the correct default API URL
if (isWrongHostname) {
  console.error('[API] ⚠️ CRITICAL: NEXT_PUBLIC_API_URL points to frontend domain instead of API domain!')
  console.error('[API] Current value:', rawApiUrl)
  console.error('[API] Using fallback: https://backend.monytix.ai')
  console.error('[API] Fix in Cloudflare Pages → Settings → Environment Variables')
  console.error('[API] Set NEXT_PUBLIC_API_URL to: https://backend.monytix.ai')
  API_BASE_URL = 'https://backend.monytix.ai'
} else if (hasPath) {
  console.warn('[API] ⚠️ NEXT_PUBLIC_API_URL contains a path (auto-stripped)')
  console.warn('[API] Current value:', rawApiUrl)
  console.warn('[API] Using:', API_BASE_URL)
  console.warn('[API] Fix in Cloudflare Pages → Settings → Environment Variables')
}

const REQUEST_TIMEOUT = 45000 // 45 seconds (Overview KPIs/insights can be slow with large datasets)

export const BACKUP_API_BASE_URL = 'https://api.monytix.ai'

function isRetryableWithBackup(error: any): boolean {
  if (error?.status && error.status >= 500) return true
  if (error?.isNetworkError || error?.isTimeout) return true
  if (error instanceof TypeError && error.message === 'Failed to fetch') return true
  if (error?.name === 'AbortError') return true
  return false
}

async function doFetch<T>(
  baseUrl: string,
  endpoint: string,
  session: Session,
  options?: RequestInit
): Promise<T> {
  const token = await session.getValidToken()
  const endpointPath = endpoint.startsWith('/') ? endpoint : `/${endpoint}`
  const fullUrl = `${baseUrl.replace(/\/$/, '')}${endpointPath}`
  const controller = new AbortController()
  const timeoutId = setTimeout(() => controller.abort(), REQUEST_TIMEOUT)

  const response = await fetch(fullUrl, {
    ...options,
    signal: controller.signal,
    headers: {
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
      ...options?.headers,
    },
  })

  clearTimeout(timeoutId)

  if (!response.ok) {
    let errorMessage = `Failed to ${options?.method ?? 'GET'} ${endpoint}: ${response.statusText}`
    if (response.status === 401 || response.status === 403) {
      errorMessage = 'Your session has expired. Please refresh the page and try again.'
    } else if (response.status === 404) {
      errorMessage = 'The requested resource was not found. It may have been deleted.'
    } else if (response.status >= 500) {
      errorMessage = 'Server error. Please try again later or contact support if the problem persists.'
    }
    try {
      const body = await response.json()
      if (response.status === 422 && body.detail) {
        if (Array.isArray(body.detail)) {
          errorMessage = `Validation error: ${body.detail.map((e: any) => e.msg || 'validation error').join(', ')}`
        } else if (typeof body.detail === 'string') {
          errorMessage = body.detail
        }
      } else if (body.detail && response.status < 500) {
        errorMessage = typeof body.detail === 'string' ? body.detail : JSON.stringify(body.detail)
      }
    } catch (_) {}
    const err = new Error(errorMessage) as any
    err.status = response.status
    err.isNetworkError = false
    throw err
  }

  return response.json()
}

export async function fetchWithAuth<T>(
  session: Session,
  endpoint: string,
  options?: RequestInit
): Promise<T> {
  // Validate session before making request
  if (!session?.getValidToken) {
    throw new Error('Authentication required. Please log in again.')
  }

  const baseUrl = API_BASE_URL.endsWith('/') ? API_BASE_URL.slice(0, -1) : API_BASE_URL
  const fullUrl = `${baseUrl}${endpoint.startsWith('/') ? endpoint : `/${endpoint}`}`

  console.log(`[API] ${options?.method || 'GET'} ${fullUrl}`)
  if (!process.env.NEXT_PUBLIC_API_URL) {
    console.warn('[API] ⚠️ NEXT_PUBLIC_API_URL not set, using default:', API_BASE_URL)
  }

  const attempt = async (url: string): Promise<T> => {
    try {
      return await doFetch<T>(url, endpoint, session, options)
    } catch (e: any) {
      if (e.name === 'AbortError' || e.message?.includes('timeout')) {
        const timeoutError = new Error('Request timed out. The server is taking too long to respond. Please try again.') as any
        timeoutError.isNetworkError = true
        timeoutError.isTimeout = true
        throw timeoutError
      }
      if (e instanceof TypeError && e.message === 'Failed to fetch') {
        const err = e as any
        err.isNetworkError = true
      }
      throw e
    }
  }

  try {
    return await attempt(baseUrl)
  } catch (primaryError: any) {
    if (!isRetryableWithBackup(primaryError) || baseUrl === BACKUP_API_BASE_URL) {
      if (primaryError instanceof TypeError && primaryError.message === 'Failed to fetch') {
        const isCorsError = (typeof window !== 'undefined' && !navigator.onLine)
        console.error('[API Error] Failed to fetch', { endpoint, url: fullUrl, apiBaseUrl: baseUrl })
        const networkError = new Error(
          `Network error: Unable to reach ${baseUrl}. Check if the API is running and accessible.`
        ) as any
        networkError.isNetworkError = true
        throw networkError
      }
      throw primaryError
    }
    console.warn('[API] Primary backend failed, trying backup:', BACKUP_API_BASE_URL, primaryError?.message)
    try {
      return await attempt(BACKUP_API_BASE_URL)
    } catch (backupError) {
      throw primaryError
    }
  }
}

export async function validateSession(session: Session): Promise<{ user_id: string; email?: string | null }> {
  return fetchWithAuth(session, '/auth/session')
}
