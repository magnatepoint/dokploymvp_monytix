package com.example.monytix.goaltracker

import com.example.monytix.data.UpdatedGoalItem

/**
 * Cache for recently updated goals from transaction create.
 * GoalTracker reads this to animate changed cards and show feedback.
 */
object GoalUpdateCache {
    @Volatile
    var lastUpdatedGoals: List<UpdatedGoalItem> = emptyList()
        private set

    fun setFromTransactionCreate(updated: List<UpdatedGoalItem>) {
        lastUpdatedGoals = updated
    }

    fun consume(): List<UpdatedGoalItem> {
        val copy = lastUpdatedGoals
        lastUpdatedGoals = emptyList()
        return copy
    }

    fun peek(): List<UpdatedGoalItem> = lastUpdatedGoals
}
