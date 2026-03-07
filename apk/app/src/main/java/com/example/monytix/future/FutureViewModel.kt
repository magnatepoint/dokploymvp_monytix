package com.example.monytix.future

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.monytix.auth.FirebaseAuthManager
import com.example.monytix.data.BackendApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * State for the Financial Future (forecast) screen.
 * Loads from GET /v1/forecast; falls back to mock on failure.
 */
data class FutureUiState(
    val confidenceLabel: String = "Based on last 90 days",
    val projectionPoints: List<Pair<Float, Float>> = emptyList(),
    val riskStripLabel: String? = null,
    val riskStripSeverity: String = "neutral",
    val savingsOpportunity: String? = null,
    val recommendations: List<FutureRecommendation> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val hasData: Boolean = false
)

data class FutureRecommendation(
    val id: String,
    val title: String,
    val body: String,
    val ctaLabel: String? = null
)

class FutureViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FutureUiState())
    val uiState: StateFlow<FutureUiState> = _uiState.asStateFlow()

    init {
        loadForecast()
    }

    private fun loadForecast() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            val token = FirebaseAuthManager.getIdToken()
            if (token == null) {
                loadMockData()
                return@launch
            }
            when (val result = BackendApi.getForecast(token)) {
                is kotlin.Result.Success -> {
                    val r = result.value
                    val points = r.projection_points.mapNotNull { list ->
                        if (list.size >= 2) Pair(list[0].toFloat(), list[1].toFloat()) else null
                    }
                    _uiState.update {
                        it.copy(
                            confidenceLabel = r.confidence_label.ifEmpty { "Based on this month's cash flow" },
                            projectionPoints = points,
                            riskStripLabel = r.risk_strip_label,
                            riskStripSeverity = r.risk_strip_severity ?: "neutral",
                            savingsOpportunity = r.savings_opportunity,
                            recommendations = r.recommendations.mapIndexed { i, rec ->
                                FutureRecommendation(
                                    id = i.toString(),
                                    title = rec.title,
                                    body = rec.body,
                                    ctaLabel = null
                                )
                            },
                            hasData = points.isNotEmpty(),
                            isLoading = false
                        )
                    }
                }
                is kotlin.Result.Failure -> {
                    loadMockData()
                }
            }
        }
    }

    private fun loadMockData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val points = (0..13).map { i ->
                val x = i / 13f
                val y = 0.4f + 0.5f * (1f - x) + (i % 3) * 0.05f
                Pair(x, y.coerceIn(0.2f, 1f))
            }
            _uiState.update {
                it.copy(
                    projectionPoints = points,
                    riskStripLabel = "Low cash risk: Days 8–10",
                    riskStripSeverity = "warning",
                    savingsOpportunity = "You could save ₹3,200 by trimming dining 10%.",
                    recommendations = listOf(
                        FutureRecommendation(
                            "1",
                            "Delay non-essential spend until payday",
                            "Your projected balance dips mid-month. Consider moving discretionary spend to after payday."
                        ),
                        FutureRecommendation(
                            "2",
                            "Top up Emergency goal by ₹2,000",
                            "You're ahead of pace this month. Putting ₹2,000 into your Emergency goal keeps you on track."
                        )
                    ),
                    hasData = true,
                    isLoading = false
                )
            }
        }
    }

    fun refresh() {
        loadForecast()
    }
}
