package com.example.monytix

import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.provider.OpenableColumns
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.ComponentActivity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBox
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.example.monytix.R
import com.example.monytix.auth.FirebaseAuthManager
import com.example.monytix.budgetpilot.BudgetPilotScreen
import com.example.monytix.goaltracker.GoalTrackerScreen
import com.example.monytix.moneymoments.MoneyMomentsScreen
import com.example.monytix.home.HomeScreen
import com.example.monytix.profile.ProfileScreen
import com.example.monytix.spendsense.PendingManualAddHolder
import com.example.monytix.spendsense.PendingUploadHolder
import com.example.monytix.spendsense.UploadProcessingState
import com.example.monytix.spendsense.UploadProcessingPhase
import com.example.monytix.ui.theme.MonytixTheme

class MainActivity : FragmentActivity() {
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

private const val TOUR_STEP_UPLOAD = 0
private const val TOUR_STEP_HOME = 1
private const val TOUR_STEP_SPEND = 2
private const val TOUR_STEP_GOALS = 3
private const val TOUR_STEP_BUDGET = 4
private const val TOUR_STEP_MOMENTS = 5
private const val TOUR_STEP_PROFILE = 6
private const val TOUR_STEP_DONE = 7
private const val TOUR_STEP_COUNT = 8

private fun tourStepToDestination(step: Int): AppDestinations = when (step) {
    TOUR_STEP_UPLOAD, TOUR_STEP_SPEND -> AppDestinations.DATA
    TOUR_STEP_HOME -> AppDestinations.HOME
    TOUR_STEP_GOALS -> AppDestinations.GOALS
    TOUR_STEP_BUDGET -> AppDestinations.BUDGET
    TOUR_STEP_MOMENTS -> AppDestinations.FAVORITES
    TOUR_STEP_PROFILE -> AppDestinations.PROFILE
    else -> AppDestinations.HOME
}

@Composable
private fun ExitConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exit_confirm_title)) },
        confirmButton = {
            Button(onClick = onConfirm) {
                Text(stringResource(R.string.exit_confirm_exit))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.exit_confirm_cancel))
            }
        }
    )
}

@Composable
internal fun MainContent(
    tourActive: Boolean = false,
    onTourComplete: () -> Unit = {}
) {
    var currentDestination by rememberSaveable { mutableStateOf(AppDestinations.HOME) }
    var currentTourStep by remember { mutableStateOf(0) }
    var showExitConfirm by remember { mutableStateOf(false) }
    val pendingUpload by PendingUploadHolder.state
    val uploadPhase = UploadProcessingState.phase.value
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    val effectiveDestination = if (tourActive) tourStepToDestination(currentTourStep) else currentDestination

    BackHandler(enabled = true) {
        if (showExitConfirm) {
            showExitConfirm = false
            return@BackHandler
        }
        if (tourActive) {
            onTourComplete()
            return@BackHandler
        }
        if (effectiveDestination != AppDestinations.HOME) {
            currentDestination = AppDestinations.HOME
            return@BackHandler
        }
        showExitConfirm = true
    }
    if (showExitConfirm) {
        ExitConfirmDialog(
            onConfirm = { activity?.finish() },
            onDismiss = { showExitConfirm = false }
        )
    }

    LaunchedEffect(pendingUpload) {
        if (pendingUpload != null && !tourActive) {
            Log.d("MonytixUpload", "Pending upload set, switching to DATA tab")
            currentDestination = AppDestinations.DATA
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
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
                        selected = it == effectiveDestination,
                        onClick = { if (!tourActive) currentDestination = it }
                    )
                }
            }
        ) {
            Scaffold(
                modifier = Modifier.fillMaxSize(),
                snackbarHost = { SnackbarHost(snackbarHostState) { Snackbar(it) } }
            ) { innerPadding ->
                AnimatedContent(
                    targetState = effectiveDestination,
                    transitionSpec = {
                        fadeIn() togetherWith fadeOut()
                    },
                    label = "screen_transition"
                ) { destination ->
                    when (destination) {
                        AppDestinations.HOME -> HomeScreen(
                            modifier = Modifier.padding(innerPadding),
                            isSelected = effectiveDestination == AppDestinations.HOME,
                            onNavigateTo = { if (!tourActive) currentDestination = it },
                            onShowSnackbar = { scope.launch { snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short) } },
                            onLaunchFilePicker = { (context as? MainActivity)?.launchFilePicker() },
                            onAddTransaction = {
                                if (!tourActive) {
                                    currentDestination = AppDestinations.DATA
                                    PendingManualAddHolder.state.value = true
                                }
                            }
                        )
                        AppDestinations.DATA -> SpendSenseScreen(
                            modifier = Modifier.padding(innerPadding),
                            onLaunchFilePicker = { (context as? MainActivity)?.launchFilePicker() }
                        )
                        AppDestinations.GOALS -> GoalTrackerScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateTo = { if (!tourActive) currentDestination = it }
                        )
                        AppDestinations.BUDGET -> BudgetPilotScreen(
                            modifier = Modifier.padding(innerPadding),
                            onNavigateTo = { if (!tourActive) currentDestination = it }
                        )
                        AppDestinations.FAVORITES -> MoneyMomentsScreen(modifier = Modifier.padding(innerPadding))
                        AppDestinations.PROFILE -> ProfileScreen(modifier = Modifier.padding(innerPadding))
                    }
                }
            }
        }

        if (tourActive) {
            GuidedTourOverlay(
                step = currentTourStep,
                uploadPhase = uploadPhase,
                onNext = {
                    if (currentTourStep < TOUR_STEP_DONE) {
                        currentTourStep++
                    } else {
                        onTourComplete()
                    }
                },
                onSkipTour = onTourComplete
            )
        }
    }
}

@Composable
private fun GuidedTourOverlay(
    step: Int,
    uploadPhase: UploadProcessingPhase,
    onNext: () -> Unit,
    onSkipTour: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.3f))
            .padding(24.dp),
        contentAlignment = Alignment.BottomCenter
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = when (step) {
                        TOUR_STEP_UPLOAD -> stringResource(R.string.guided_tour_step_upload_title)
                        TOUR_STEP_HOME -> stringResource(R.string.guided_tour_step_home_title)
                        TOUR_STEP_SPEND -> stringResource(R.string.guided_tour_step_spend_title)
                        TOUR_STEP_GOALS -> stringResource(R.string.guided_tour_step_goals_title)
                        TOUR_STEP_BUDGET -> stringResource(R.string.guided_tour_step_budget_title)
                        TOUR_STEP_MOMENTS -> stringResource(R.string.guided_tour_step_moments_title)
                        TOUR_STEP_PROFILE -> stringResource(R.string.guided_tour_step_profile_title)
                        TOUR_STEP_DONE -> stringResource(R.string.guided_tour_step_done_title)
                        else -> ""
                    },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = when (step) {
                        TOUR_STEP_UPLOAD -> stringResource(R.string.guided_tour_step_upload_body)
                        TOUR_STEP_HOME -> stringResource(R.string.guided_tour_step_home_body)
                        TOUR_STEP_SPEND -> stringResource(R.string.guided_tour_step_spend_body)
                        TOUR_STEP_GOALS -> stringResource(R.string.guided_tour_step_goals_body)
                        TOUR_STEP_BUDGET -> stringResource(R.string.guided_tour_step_budget_body)
                        TOUR_STEP_MOMENTS -> stringResource(R.string.guided_tour_step_moments_body)
                        TOUR_STEP_PROFILE -> stringResource(R.string.guided_tour_step_profile_body)
                        TOUR_STEP_DONE -> stringResource(R.string.guided_tour_step_done_body)
                        else -> ""
                    },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End)
                ) {
                    TextButton(onClick = onSkipTour) {
                        Text(stringResource(R.string.quick_tour_skip), color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    val isUploadStep = step == TOUR_STEP_UPLOAD
                    val canAdvanceFromUpload = uploadPhase == UploadProcessingPhase.Complete
                    if (step == TOUR_STEP_UPLOAD && uploadPhase != UploadProcessingPhase.Complete) {
                        TextButton(onClick = onNext) {
                            Text(stringResource(R.string.guided_tour_step_upload_skip), color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    Button(
                        onClick = onNext,
                        enabled = !isUploadStep || canAdvanceFromUpload
                    ) {
                        Text(
                            if (step == TOUR_STEP_DONE) stringResource(R.string.guided_tour_done)
                            else stringResource(R.string.quick_tour_next)
                        )
                    }
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