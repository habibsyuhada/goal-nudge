package com.goalnudge.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.goalnudge.app.ui.goal.GoalEditScreen
import com.goalnudge.app.ui.goal.GoalListScreen
import com.goalnudge.app.ui.metrics.MetricsScreen
import com.goalnudge.app.ui.navigation.Routes
import com.goalnudge.app.ui.onboarding.OnboardingScreen
import com.goalnudge.app.ui.settings.SettingsScreen
import com.goalnudge.app.ui.theme.GoalNudgeTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val mainViewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            GoalNudgeTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    val onboardingCompleted by mainViewModel.onboardingCompleted.collectAsState()
                    when (onboardingCompleted) {
                        null -> Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator()
                        }
                        else -> GoalNudgeNavHost(startAtOnboarding = onboardingCompleted != true)
                    }
                }
            }
        }
    }
}

@androidx.compose.runtime.Composable
private fun GoalNudgeNavHost(startAtOnboarding: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (startAtOnboarding) Routes.ONBOARDING else Routes.GOALS

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(onFinished = {
                navController.navigate(Routes.GOALS) {
                    popUpTo(Routes.ONBOARDING) { inclusive = true }
                }
            })
        }
        composable(Routes.GOALS) {
            GoalListScreen(
                onAddGoal = { navController.navigate(Routes.goalEdit(0L)) },
                onEditGoal = { id -> navController.navigate(Routes.goalEdit(id)) },
                onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                onOpenMetrics = { navController.navigate(Routes.METRICS) }
            )
        }
        composable(
            route = Routes.GOAL_EDIT_WITH_ARG,
            arguments = listOf(navArgument(Routes.GOAL_EDIT_ARG) { type = NavType.LongType })
        ) { backStackEntry ->
            val goalId = backStackEntry.arguments?.getLong(Routes.GOAL_EDIT_ARG) ?: 0L
            GoalEditScreen(
                goalId = goalId,
                onDone = { navController.popBackStack() },
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.SETTINGS) {
            SettingsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.METRICS) {
            MetricsScreen(onBack = { navController.popBackStack() })
        }
    }
}
