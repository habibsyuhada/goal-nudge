package com.goalnudge.app.ui.navigation

object Routes {
    const val ONBOARDING = "onboarding"
    const val GOALS = "goals"
    const val GOAL_EDIT = "goal_edit"
    const val GOAL_EDIT_ARG = "goalId"
    const val GOAL_EDIT_WITH_ARG = "$GOAL_EDIT/{$GOAL_EDIT_ARG}"
    const val SETTINGS = "settings"
    const val METRICS = "metrics"

    fun goalEdit(goalId: Long) = "$GOAL_EDIT/$goalId"
}
