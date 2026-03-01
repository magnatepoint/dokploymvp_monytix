package com.example.monytix

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.provider.OpenableColumns
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.navigationsuite.NavigationSuiteScaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
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
import com.example.monytix.auth.FirebaseAuthManager
import com.example.monytix.budgetpilot.BudgetPilotScreen
import com.example.monytix.goaltracker.GoalTrackerScreen
import com.example.monytix.moneymoments.MoneyMomentsScreen
import com.example.monytix.home.HomeScreen
import com.example.monytix.profile.ProfileScreen
import com.example.monytix.spendsense.PendingManualAddHolder
import com.example.monytix.spendsense.PendingUploadHolder
import com.example.monytix.ui.theme.MonytixTheme

class MainActivity : ComponentActivity() {
    private val filePickerLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d("MonytixUpload", "Activity file picker callback: uri=$uri")
        uri?.let { u ->
            lifecycleScope.launch {
                try {
                    Log.d("MonytixUpload", "Reading file from uri=$u")
                    val result = withContext(Dispatchers.IO) {
                        var name = u.lastPathSegment ?: "statement.pdf"
                        contentResolver.query(u, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                            if (cursor.moveToFirst()) {
                                val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                                if (idx >= 0) cursor.getString(idx)?.takeIf { n -> n.isNotBlank() }?.let { name = it }
                            }
                        }
                        val b = contentResolver.openInputStream(u)?.use { stream -> stream.readBytes() }
                        if (b != null && b.isNotEmpty()) Pair(b, name) else null
                    }
                    if (result != null) {
                        Log.d("MonytixUpload", "File read ok: filename=${result.second}, bytes=${result.first.size}")
                        PendingUploadHolder.state.value = result
                    } else {
                        Log.w("MonytixUpload", "File read returned null or empty")
                    }
                } catch (e: Exception) {
                    Log.e("MonytixUpload", "File read failed", e)
                }
            }
        } ?: Log.w("MonytixUpload", "File picker returned null uri")
    }

    fun launchFilePicker() {
        Log.d("MonytixUpload", "Activity.launchFilePicker()")
        filePickerLauncher.launch("*/*")
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        var keepSplash = true
        splashScreen.setKeepOnScreenCondition { keepSplash }
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MonytixTheme {
                MonytixApp(onSessionReady = { keepSplash = false })
            }
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
    }
}

@PreviewScreenSizes
@Composable
fun MonytixApp(
    preAuthViewModel: PreAuthViewModel = viewModel(
        factory = PreAuthViewModelFactory(LocalContext.current.applicationContext as android.app.Application)
    ),
    authViewModel: AuthViewModel = viewModel(),
    onSessionReady: () -> Unit = {}
) {
    val isSignedIn by FirebaseAuthManager.isSignedIn.collectAsState(initial = FirebaseAuthManager.currentUser != null)

    LaunchedEffect(isSignedIn) {
        delay(150) // Allow first frame to compose before dismissing splash
        onSessionReady()
    }

    if (isSignedIn) {
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
    val pendingUpload by PendingUploadHolder.state
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    LaunchedEffect(pendingUpload) {
        if (pendingUpload != null) {
            Log.d("MonytixUpload", "Pending upload set, switching to DATA tab")
            currentDestination = AppDestinations.DATA
        }
    }

    NavigationSuiteScaffold(
        navigationSuiteItems = {
            AppDestinations.entries.forEach {
                val fullName = when (it) {
                    AppDestinations.HOME -> "MolyConsole"
                    AppDestinations.DATA -> "SpendSense"
                    AppDestinations.GOALS -> "GoalTracker"
                    AppDestinations.BUDGET -> "BudgetPilot"
                    AppDestinations.FAVORITES -> "MoneyMoments"
                    AppDestinations.PROFILE -> "Profile"
                }
                item(
                    icon = {
                        Icon(
                            it.icon,
                            contentDescription = fullName
                        )
                    },
                    label = { Text(it.label) },
                    selected = it == currentDestination,
                    onClick = { currentDestination = it }
                )
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }
        ) { innerPadding ->
            AnimatedContent(
                targetState = currentDestination,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                },
                label = "screen_transition"
            ) { destination ->
                when (destination) {
                    AppDestinations.HOME -> HomeScreen(
                        modifier = Modifier.padding(innerPadding),
                        isSelected = currentDestination == AppDestinations.HOME,
                        onNavigateTo = { currentDestination = it },
                        onShowSnackbar = { scope.launch { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short) } },
                        onLaunchFilePicker = { (context as? MainActivity)?.launchFilePicker() },
                        onAddTransaction = {
                            currentDestination = AppDestinations.DATA
                            PendingManualAddHolder.state.value = true
                        }
                    )
                    AppDestinations.DATA -> SpendSenseScreen(
                        modifier = Modifier.padding(innerPadding),
                        onLaunchFilePicker = { (context as? MainActivity)?.launchFilePicker() }
                    )
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
    HOME("Home", Icons.Default.Home),
    DATA("Spend", Icons.Default.AttachMoney),
    GOALS("Goals", Icons.Default.Flag),
    BUDGET("Budget", Icons.Default.PieChart),
    FAVORITES("Moments", Icons.Default.Favorite),
    PROFILE("Profile", Icons.Default.AccountBox),
}

@Preview(showBackground = true)
@Composable
fun HomePreview() {
    MonytixTheme {
        HomeScreen()
    }
}