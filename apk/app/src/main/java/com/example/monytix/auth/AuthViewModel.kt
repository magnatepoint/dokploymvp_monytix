package com.example.monytix.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.data.Supabase
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.OtpType
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.OTP
import io.github.jan.supabase.auth.providers.Google
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val resendSecondsLeft: Int = 0
)

class AuthViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()

    private val supabase: Auth
        get() = Supabase.client.auth

    fun sendPhoneOtp(phone: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                val normalized = if (phone.startsWith("+")) phone else "+91$phone"
                supabase.signInWith(OTP) {
                    this.phone = normalized
                }
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        authStep = AuthStep.OTP,
                        phoneForOtp = normalized,
                        otp = "",
                        resendSecondsLeft = 60
                    )
                }
                startResendTimer()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        error = e.message ?: "Failed to send OTP. Ensure Phone auth is configured in Supabase."
                    )
                }
            }
        }
    }

    fun setOtp(otp: String) {
        _uiState.update { it.copy(otp = otp) }
    }

    fun verifyOtp() {
        viewModelScope.launch {
            val currentOtp = _uiState.value.otp
            if (currentOtp.length != 6) return@launch
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                supabase.verifyPhoneOtp(
                    type = OtpType.Phone.SMS,
                    phone = _uiState.value.phoneForOtp,
                    token = currentOtp
                )
                _uiState.update { it.copy(isLoading = false, error = null) }
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

    fun resendOtp() {
        sendPhoneOtp(_uiState.value.phoneForOtp.replace("+91", ""))
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
                supabase.signInWith(Email) {
                    this.email = email
                    this.password = password
                }
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
                supabase.signUpWith(Email) {
                    this.email = email
                    this.password = password
                }
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

    fun signInWithGoogle() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, error = null) }
            try {
                supabase.signInWith(Google)
                _uiState.update { it.copy(isLoading = false, error = null) }
            } catch (e: Exception) {
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
