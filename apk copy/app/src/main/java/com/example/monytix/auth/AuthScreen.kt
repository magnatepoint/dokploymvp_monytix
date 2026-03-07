package com.example.monytix.auth

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.example.monytix.R
import com.example.monytix.analytics.AnalyticsHelper
import com.example.monytix.ui.MonytixSpinner
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
import com.example.monytix.ui.theme.Background
import com.example.monytix.ui.theme.BorderSubtle
import com.example.monytix.ui.theme.CyanGlow
import com.example.monytix.ui.theme.CyanPrimary
import com.example.monytix.ui.theme.CyanSecondary
import com.example.monytix.ui.theme.SurfaceSecondary
import com.example.monytix.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? androidx.activity.ComponentActivity
    val uiState by viewModel.uiState.collectAsState()

    LaunchedEffect(uiState.authStep) {
        val screenName = if (uiState.authStep == AuthStep.OTP) "otp" else "auth"
        AnalyticsHelper.logScreenView(screenName)
    }

    when (uiState.authStep) {
        AuthStep.LOGIN -> LoginContent(viewModel = viewModel, uiState = uiState, activity = activity)
        AuthStep.OTP -> OtpScreen(
            phone = uiState.phoneForOtp,
            otp = uiState.otp,
            onOtpChange = { viewModel.setOtp(it) },
            resendSecondsLeft = uiState.resendSecondsLeft,
            onVerify = { viewModel.verifyOtp() },
            onResend = { activity?.let { viewModel.resendOtp(it) } }
        )
    }
}

private val AuthInputShape = RoundedCornerShape(20.dp)
private const val AuthLogoScaleStart = 0.96f

@Composable
private fun LoginContent(
    viewModel: AuthViewModel,
    uiState: AuthUiState,
    activity: androidx.activity.ComponentActivity?,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var isLogin by remember { mutableStateOf(true) }
    var logoVisible by remember { mutableStateOf(false) }
    var formVisible by remember { mutableStateOf(false) }
    var ctaVisible by remember { mutableStateOf(false) }

    LaunchedEffect(isLogin) { viewModel.clearError() }
    LaunchedEffect(Unit) {
        logoVisible = true
        kotlinx.coroutines.delay(80)
        formVisible = true
        kotlinx.coroutines.delay(120)
        ctaVisible = true
    }

    val logoScale by animateFloatAsState(
        targetValue = if (logoVisible) 1f else AuthLogoScaleStart,
        animationSpec = tween(durationMillis = 400),
        label = "logoScale"
    )
    val logoAlpha by animateFloatAsState(
        targetValue = if (logoVisible) 1f else 0f,
        animationSpec = tween(durationMillis = 350),
        label = "logoAlpha"
    )

    val colorScheme = MaterialTheme.colorScheme
    val authBrush = Brush.radialGradient(
        colors = listOf(CyanGlow, Background),
        center = Offset(0.5f, 0.15f),
        radius = 1200f
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(authBrush)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Logo with subtle cyan glow behind
            Box(
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = logoScale
                        scaleY = logoScale
                        alpha = logoAlpha
                    },
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(500.dp)
                        .background(
                            brush = Brush.radialGradient(
                                colors = listOf(CyanGlow, Color.Transparent)
                            ),
                            shape = CircleShape
                        )
                )
                Image(
                    painter = painterResource(R.drawable.logo),
                    contentDescription = "MONYTIX Logo",
                    modifier = Modifier.size(400.dp)
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = stringResource(R.string.auth_tagline),
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "🔒 " + stringResource(R.string.auth_security_cue),
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary.copy(alpha = 0.8f)
            )
            Spacer(modifier = Modifier.height(40.dp))

            AnimatedVisibility(
                visible = formVisible,
                enter = fadeIn(tween(300)) + slideInVertically(initialOffsetY = { it / 4 }, animationSpec = tween(400))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.error != null) {
                        Text(
                            text = uiState.error!!,
                            color = colorScheme.error,
                            style = MaterialTheme.typography.bodyMedium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email", color = TextSecondary) },
                        placeholder = { Text("your@email.com", color = TextSecondary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Email,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.8f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AuthInputShape)
                            .background(SurfaceSecondary)
                            .border(1.dp, BorderSubtle, AuthInputShape),
                        enabled = !uiState.isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = CyanPrimary,
                            focusedLeadingIconColor = CyanPrimary,
                            unfocusedLeadingIconColor = TextSecondary.copy(alpha = 0.8f)
                        ),
                        shape = AuthInputShape
                    )
                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password", color = TextSecondary) },
                        placeholder = { Text("••••••••", color = TextSecondary.copy(alpha = 0.5f)) },
                        singleLine = true,
                        visualTransformation = PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        leadingIcon = {
                            Icon(
                                Icons.Default.Lock,
                                contentDescription = null,
                                tint = TextSecondary.copy(alpha = 0.8f)
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(AuthInputShape)
                            .background(SurfaceSecondary)
                            .border(1.dp, BorderSubtle, AuthInputShape),
                        enabled = !uiState.isLoading,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = colorScheme.onSurface,
                            unfocusedTextColor = colorScheme.onSurface,
                            focusedBorderColor = Color.Transparent,
                            unfocusedBorderColor = Color.Transparent,
                            cursorColor = CyanPrimary,
                            focusedLeadingIconColor = CyanPrimary,
                            unfocusedLeadingIconColor = TextSecondary.copy(alpha = 0.8f)
                        ),
                        shape = AuthInputShape
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            AnimatedVisibility(
                visible = ctaVisible,
                enter = fadeIn(tween(400))
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    val ctaEnabled = !uiState.isLoading && email.isNotBlank() && password.length >= 6
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(54.dp)
                            .shadow(12.dp, RoundedCornerShape(20.dp), ambientColor = CyanPrimary, spotColor = CyanPrimary)
                            .clip(RoundedCornerShape(20.dp))
                            .background(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(CyanPrimary, CyanSecondary)
                                )
                            )
                            .alpha(if (ctaEnabled) 1f else 0.5f)
                            .clickable(enabled = ctaEnabled) {
                                if (isLogin) viewModel.signIn(email, password)
                                else viewModel.signUp(email, password)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isLoading) {
                            MonytixSpinner(size = 20.dp, stroke = 2.dp)
                        } else {
                            Text(
                                text = if (isLogin) stringResource(R.string.auth_access_intelligence) else stringResource(R.string.auth_sign_up),
                                color = Color(0xFF0B1220),
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp
                            )
                        }
                    }
                    TextButton(onClick = { isLogin = !isLogin }) {
                        Text(
                            text = if (isLogin) stringResource(R.string.auth_new_to_monytix) else stringResource(R.string.auth_already_have_account),
                            color = TextSecondary
                        )
                    }

                    val fragActivity = activity as? FragmentActivity
                    val showBiometric = fragActivity != null &&
                        BiometricHelper.canAuthenticate(fragActivity) &&
                        SecureTokenStorage.hasStoredCredentials(context)
                    if (showBiometric && isLogin) {
                        OutlinedButton(
                            onClick = {
                                fragActivity?.let { fa ->
                                    BiometricHelper.showBiometricPrompt(
                                        activity = fa,
                                        onSuccess = { viewModel.signInWithStoredToken() },
                                        onError = { _, msg ->
                                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                                        }
                                    )
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(48.dp),
                            enabled = !uiState.isLoading,
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary)
                        ) {
                            Icon(
                                Icons.Default.Fingerprint,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.size(8.dp))
                            Text(
                                stringResource(R.string.auth_login_with_biometrics),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    OutlinedButton(
                        onClick = {
                            (context as? androidx.activity.ComponentActivity)?.let {
                                viewModel.signInWithGoogle(it)
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        enabled = !uiState.isLoading,
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = TextSecondary.copy(alpha = 0.9f))
                    ) {
                        Text(
                            stringResource(R.string.auth_google),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
