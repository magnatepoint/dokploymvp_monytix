package com.example.monytix.auth

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.example.monytix.MainContent
import com.example.monytix.R
import com.example.monytix.analytics.AnalyticsHelper
import com.example.monytix.quicktour.QuickTourPreferences
import com.example.monytix.system.AppBlockState

@Composable
fun PostAuthGate(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val context = LocalContext.current
    var pendingMpin by remember { mutableStateOf<String?>(null) }
    var biometricDecided by remember { mutableStateOf(BiometricPreferences.hasDecided(context)) }

    val mpinSet = MpinManager.isMpinSet(context)
    LaunchedEffect(mpinSet, biometricDecided) {
        when {
            !mpinSet -> AnalyticsHelper.logScreenView("set_mpin")
            !biometricDecided -> AnalyticsHelper.logScreenView("enable_biometrics")
        }
    }

    when {
        !MpinManager.isMpinSet(context) -> {
            var showExitConfirm by remember { mutableStateOf(false) }
            val activity = context as? ComponentActivity
            BackHandler(enabled = true) {
                if (showExitConfirm) showExitConfirm = false else showExitConfirm = true
            }
            if (showExitConfirm) {
                AlertDialog(
                    onDismissRequest = { showExitConfirm = false },
                    title = { Text(stringResource(R.string.exit_confirm_title)) },
                    confirmButton = {
                        Button(onClick = { activity?.finish() }) {
                            Text(stringResource(R.string.exit_confirm_exit))
                        }
                    },
                    dismissButton = {
                        TextButton(onClick = { showExitConfirm = false }) {
                            Text(stringResource(R.string.exit_confirm_cancel))
                        }
                    }
                )
            }
            if (pendingMpin == null) {
                SetMpinScreen(
                    isConfirmStep = false,
                    onMpinSet = { pin -> pendingMpin = pin }
                )
            } else {
                SetMpinScreen(
                    isConfirmStep = true,
                    onMpinSet = { pin ->
                        if (pin == pendingMpin) {
                            AnalyticsHelper.logEvent("mpin_set")
                            MpinManager.setMpin(context, pin)
                            pendingMpin = null
                        } else {
                            pendingMpin = null
                        }
                    }
                )
            }
        }
        !biometricDecided -> {
            BackHandler(enabled = true) {
                BiometricPreferences.setDecided(context)
                biometricDecided = true
            }
            EnableBiometricsScreen(
                onEnable = {
                    BiometricPreferences.setDecided(context)
                    biometricDecided = true
                },
                onSkip = {
                    BiometricPreferences.setDecided(context)
                    biometricDecided = true
                }
            )
        }
        else -> {
            var quickTourCompleted by remember { mutableStateOf(QuickTourPreferences.hasCompleted(context)) }
            AppBlockState {
                MainContent(
                    tourActive = !quickTourCompleted,
                    onTourComplete = {
                        QuickTourPreferences.setCompleted(context)
                        quickTourCompleted = true
                    }
                )
            }
        }
    }
}
