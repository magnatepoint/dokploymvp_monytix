package com.example.monytix.auth

import android.app.Application
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.R
import com.example.monytix.analytics.AnalyticsHelper
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

enum class AuthStep {
    LOGIN,
    OTP
}

data class AuthUiState(
    val authStep: AuthStep = AuthStep.LOGIN,
    val isLoading: Boolean = false,
    val error: String? = null,
    val signUpSuccess: Boolean = false,
    val phoneForOtp: String = "",
    val otp: String = "",
    val resendSecondsLeft: Int = 0,
    val verificationId: String? = null
)

class AuthViewModel(application: Application) : AndroidViewModel(application) {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val auth: FirebaseAuth = FirebaseAuth.getInstance()
    private val appContext = application.applicationContext

    fun sendPhoneOtp(phone: String, activity: ComponentActivity) {
        AnalyticsHelper.logEvent("send_otp")
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val normalized = if (phone.startsWith("+")) phone else "+91$phone"
                val callbacks = object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        viewModelScope.launch {
                            signInWithPhoneCredential(credential)
                        }
                    }
                    override fun onVerificationFailed(e: FirebaseException) {
                        viewModelScope.launch {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    error = e.message ?: "Failed to send OTP"
                                )
                            }
                        }
                    }
                    override fun onCodeSent(
                        verificationId: String,
                        token: PhoneAuthProvider.ForceResendingToken
                    ) {
                        viewModelScope.launch {
                            _uiState.update {
                                it.copy(
                                    isLoading = false,
                                    authStep = AuthStep.OTP,
                                    phoneForOtp = normalized,
                                    otp = "",
                                    resendSecondsLeft = 60,
                                    verificationId = verificationId
                                )
                            }
                            startResendTimer()
                        }
                    }
                }
                val options = PhoneAuthOptions.newBuilder(auth)
                    .setPhoneNumber(normalized)
                    .setTimeout(60L, java.util.concurrent.TimeUnit.SECONDS)
                    .setActivity(activity)
                    .setCallbacks(callbacks)
                    .build()
                PhoneAuthProvider.verifyPhoneNumber(options)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to send OTP. Enable Phone auth in Firebase Console."
                    )
                }
            }
        }
    }

    private suspend fun signInWithPhoneCredential(credential: PhoneAuthCredential) {
        try {
            auth.signInWithCredential(credential).await()
            _uiState.update { it.copy(isLoading = false, error = null) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = e.message ?: "Verification failed"
                )
            }
        }
    }

    fun setOtp(otp: String) {
        _uiState.update { it.copy(otp = otp) }
    }

    fun verifyOtp() {
        AnalyticsHelper.logEvent("verify_otp")
        viewModelScope.launch {
            val currentOtp = _uiState.value.otp
            val verificationId = _uiState.value.verificationId
            if (currentOtp.length != 6 || verificationId == null) return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val credential = PhoneAuthProvider.getCredential(verificationId, currentOtp)
                signInWithPhoneCredential(credential)
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Invalid OTP"
                    )
                }
            }
        }
    }

    fun resendOtp(activity: ComponentActivity) {
        AnalyticsHelper.logEvent("resend_otp")
        sendPhoneOtp(_uiState.value.phoneForOtp.replace("+91", ""), activity)
    }

    private fun startResendTimer() {
        viewModelScope.launch {
            var seconds = 60
            while (seconds > 0) {
                delay(1000)
                seconds--
                _uiState.update { it.copy(resendSecondsLeft = seconds) }
            }
        }
    }

    fun signIn(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                auth.signInWithEmailAndPassword(email, password).await()
                AnalyticsHelper.logEvent("sign_in_email", mapOf("method" to "email"))
                _uiState.update { it.copy(isLoading = false, error = null) }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Sign in failed"
                    )
                }
            }
        }
    }

    fun signUp(email: String, password: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null, signUpSuccess = false) }
            try {
                auth.createUserWithEmailAndPassword(email, password).await()
                AnalyticsHelper.logEvent("sign_up_email", mapOf("method" to "email"))
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = null,
                        signUpSuccess = true
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Sign up failed",
                        signUpSuccess = false
                    )
                }
            }
        }
    }

    fun signInWithGoogle(activity: ComponentActivity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val webClientId = appContext.getString(R.string.default_web_client_id)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(true)
                    .setServerClientId(webClientId)
                    .build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val credentialManager = CredentialManager.create(activity)
                val result = credentialManager.getCredential(activity, request)
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCredential = GoogleAuthProvider.getCredential(
                        googleCredential.idToken,
                        null
                    )
                    auth.signInWithCredential(firebaseCredential).await()
                    AnalyticsHelper.logEvent("sign_in_google", mapOf("method" to "google"))
                    _uiState.update { it.copy(isLoading = false, error = null) }
                } else {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            error = "Invalid credential type"
                        )
                    }
                }
            } catch (e: GetCredentialException) {
                Log.e("AuthViewModel", "Credential Manager error", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Google sign in failed"
                    )
                }
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign-in failed", e)
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Google sign in failed"
                    )
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(error = null, signUpSuccess = false) }
    }
}
