package com.example.monytix

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewScreenSizes
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.monytix.auth.AuthScreen
import com.example.monytix.auth.AuthViewModel
import com.example.monytix.auth.PostAuthGate
import com.example.monytix.spendsense.SpendSenseScreen
import com.example.monytix.preauth.PreAuthScreen
import com.example.monytix.preauth.PreAuthViewModel
import com.example.monytix.preauth.PreAuthViewModelFactory
import com.example.monytix.data.Supabase
import com.example.monytix.budgetpilot.BudgetPilotScreen
import com.example.monytix.goaltracker.GoalTrackerScreen
import com.example.monytix.moneymoments.MoneyMomentsScreen
import com.example.monytix.home.HomeScreen
import com.example.monytix.profile.ProfileScreen
import com.example.monytix.ui.theme.MonytixTheme
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.handleDeeplinks
import io.github.jan.supabase.auth.status.SessionStatus

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        Supabase.client.handleDeeplinks(intent)
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonytixTheme {
                MonytixApp()
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        Supabase.client.handleDeeplinks(intent)
    }
}

@PreviewScreenSizes
@Composable
fun MonytixApp(
    preAuthViewModel: PreAuthViewModel = viewModel(
        factory = PreAuthViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    ),
    authViewModel: AuthViewModel = viewModel()
) {
    val sessionStatus by Supabase.client.auth.sessionStatus.collectAsState(initial = SessionStatus.Initializing)

    if (sessionStatus is SessionStatus.Authenticated) {
        PostAuthGate()
    } else {
        PreAuthScreen(
            preAuthViewModel = preAuthViewModel,
            authViewModel = authViewModel
        )
    }
}

@Composable
internal fun MainContent() {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = it.label
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_transition"
            ) { destination ->
                when (destination) {
                    AppDestinations.HOME -> HomeScreen(modifier = Modifier.padding(innerPadding))
                    AppDestinations.DATA -> SpendSenseScreen(modifier = Modifier.padding(innerPadding))
                    AppDestinations.GOALS -> GoalTrackerScreen(
                        modifier = Modifier.padding(innerPadding),
                        onNavigateTo = { currentDestination = it }
                    )
                    AppDestinations.BUDGET -> BudgetPilotScreen(
                        modifier = Modifier.padding(innerPadding),
                        onNavigateTo = { currentDestination = it }
                    )
                    AppDestinations.FAVORITES -> MoneyMomentsScreen(modifier = Modifier.padding(innerPadding))
                    AppDestinations.PROFILE -> ProfileScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

enum class AppDestinations(
    val label: String,
    val icon: ImageVector,
) {
    HOME("MolyConsole", Icons.Default.Home),
    DATA("SpendSense", Icons.Default.AttachMoney),
    GOALS("GoalTracker", Icons.Default.Flag),
    BUDGET("BudgetPilot", Icons.Default.PieChart),
    FAVORITES("MoneyMoments", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    MonytixTheme {
        HomeScreen()
    }
}