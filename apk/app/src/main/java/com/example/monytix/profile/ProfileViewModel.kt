package com.example.monytix.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.data.BackendApi
import com.example.monytix.data.Supabase
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class ProfileUiState(
    val userEmail: String? = null,
    val userId: String? = null,
    val isExporting: Boolean = false,
    val exportSuccess: Boolean = false,
    val isDeletingData: Boolean = false,
    val deleteDataSuccess: Boolean = false,
    val error: String? = null
)

class ProfileViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    init {
        loadUser()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val session = Supabase.client.auth.currentSessionOrNull()
            _uiState.update {
                it.copy(
                    userEmail = session?.user?.email,
                    userId = session?.user?.id
                )
            }
        }
    }

    fun deleteAllData() {
        viewModelScope.launch {
            val token = Supabase.client.auth.currentSessionOrNull()?.accessToken ?: run {
                _uiState.update { it.copy(error = "Not signed in") }
                return@launch
            }
            _uiState.update { it.copy(isDeletingData = true, error = null) }
            val result = withContext(Dispatchers.IO) { BackendApi.deleteAllData(token) }
            result.fold(
                onSuccess = {
                    _uiState.update {
                        it.copy(
                            isDeletingData = false,
                            deleteDataSuccess = true,
                            error = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isDeletingData = false,
                            error = e.message ?: "Failed to delete data"
                        )
                    }
                }
            )
        }
    }

    fun clearDeleteSuccess() {
        _uiState.update { it.copy(deleteDataSuccess = false) }
    }

    fun logout() {
        viewModelScope.launch {
            Supabase.client.auth.signOut()
        }
    }

    fun exportData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, error = null) }
            val token = Supabase.client.auth.currentSessionOrNull()?.accessToken ?: run {
                _uiState.update { it.copy(isExporting = false, error = "Not signed in") }
                return@launch
            }
            // TODO: Call backend export endpoint when available
            withContext(Dispatchers.IO) {
                kotlinx.coroutines.delay(500) // Simulate
            }
            _uiState.update {
                it.copy(
                    isExporting = false,
                    exportSuccess = true,
                    error = null
                )
            }
        }
    }

    fun deactivateAccount() {
        viewModelScope.launch {
            // TODO: Call backend deactivation when available
            _uiState.update { it.copy(error = "Contact support to deactivate") }
        }
    }

    fun reKyc() {
        viewModelScope.launch {
            // TODO: Navigate to Re-KYC flow when available
            _uiState.update { it.copy(error = "Re-KYC coming soon") }
        }
    }

    fun withdrawConsent() {
        viewModelScope.launch {
            // TODO: Call backend consent withdrawal when available
            _uiState.update { it.copy(error = "Contact support to withdraw consent") }
        }
    }

    fun deleteAccount() {
        viewModelScope.launch {
            // TODO: Call backend delete account when available
            Supabase.client.auth.signOut()
        }
    }
}
