package com.example.monytix.system

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.data.BackendApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

sealed class AppBlockUiState {
    data object Ready : AppBlockUiState()
    data object Maintenance : AppBlockUiState()
    data object InternetError : AppBlockUiState()
    data object ServerDown : AppBlockUiState()
}

class AppBlockViewModel(
    private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow<AppBlockUiState>(AppBlockUiState.Ready)
    val uiState: StateFlow<AppBlockUiState> = _uiState.asStateFlow()

    init {
        checkBlockState()
    }

    fun retry() {
        checkBlockState()
    }

    /** Bypass block and proceed to app (for development when backend is unreachable). */
    fun continueAnyway() {
        _uiState.update { AppBlockUiState.Ready }
    }

    private fun checkBlockState() {
        viewModelScope.launch {
            if (!isNetworkAvailable()) {
                _uiState.update { AppBlockUiState.InternetError }
                return@launch
            }
            val configResult = withContext(Dispatchers.IO) { BackendApi.getConfig() }
            val config = configResult.getOrNull()
            if (config?.maintenance_mode == true) {
                _uiState.update { AppBlockUiState.Maintenance }
                return@launch
            }
            val healthResult = withContext(Dispatchers.IO) { BackendApi.healthCheck() }
            if (healthResult.isFailure) {
                _uiState.update { AppBlockUiState.ServerDown }
                return@launch
            }
            _uiState.update { AppBlockUiState.Ready }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        } catch (e: SecurityException) {
            // ACCESS_NETWORK_STATE not granted; assume available, proceed to health check
            true
        }
    }
}
