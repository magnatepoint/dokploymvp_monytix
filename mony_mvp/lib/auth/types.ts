/**
 * Session type used for API calls. Backend expects Firebase ID token (Bearer)
 * and uses the token's UID (Firebase user_id) for spendsense, goaltracker, budgetpilot, moneymoments.
 */
export interface Session {
  /** Returns a valid Firebase ID token (refreshes if needed). */
  getValidToken(): Promise<string>
  user: {
    id: string
    email?: string | null
  }
}
