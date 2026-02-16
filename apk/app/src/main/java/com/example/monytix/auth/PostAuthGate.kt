package com.example.monytix.auth

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.example.monytix.MainContent
import com.example.monytix.system.AppBlockState

@Composable
fun PostAuthGate(
    modifier: androidx.compose.ui.Modifier = androidx.compose.ui.Modifier
) {
    val context = LocalContext.current
    var pendingMpin by remember { mutableStateOf<String?>(null) }
    var biometricDecided by remember { mutableStateOf(BiometricPreferences.hasDecided(context)) }

    when {
        !MpinManager.isMpinSet(context) -> {
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
        else -> AppBlockState { MainContent() }
    }
}
