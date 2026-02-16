package com.example.monytix.budgetpilot

import com.example.monytix.data.BudgetStateUpdate

/**
 * Cache for budget state updates from transaction create.
 * BudgetPilot reads this to refresh and show deviation/suggestion.
 */
object BudgetUpdateCache {
    @Volatile
    var lastBudgetState: BudgetStateUpdate? = null
        private set

    fun setFromTransactionCreate(state: BudgetStateUpdate?) {
        lastBudgetState = state
    }

    fun consume(): BudgetStateUpdate? {
        val copy = lastBudgetState
        lastBudgetState = null
        return copy
    }

    fun peek(): BudgetStateUpdate? = lastBudgetState
}
