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
    val confidenceLabel: String = "Based on this month's cash flow and spending trend",
    val projectionPoints: List<Pair<Float, Float>> = emptyList(),
    val heroMessage: String = "",  // e.g. "Your balance may drop below ₹10,000 in 18 days" or "On track — no shortfall in the next 30 days"
    val heroIsRisk: Boolean = false,
    val lowPointDayIndex: Int? = null,  // index in projectionPoints for chart "Low" marker
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
            val result = BackendApi.getForecast(token)
            result.fold(
                onSuccess = { r ->
                    val points = r.projection_points.mapNotNull { list ->
                        if (list.size >= 2) Pair(list[0].toFloat(), list[1].toFloat()) else null
                    }
                    val (heroMsg, heroRisk, lowIdx) = deriveHeroAndLowPoint(
                        points,
                        r.risk_strip_severity,
                        r.risk_strip_label
                    )
                    _uiState.update {
                        it.copy(
                            confidenceLabel = r.confidence_label.ifEmpty { "Based on this month's cash flow and spending trend" },
                            projectionPoints = points,
                            heroMessage = heroMsg,
                            heroIsRisk = heroRisk,
                            lowPointDayIndex = lowIdx,
                            riskStripLabel = r.risk_strip_label,
                            riskStripSeverity = r.risk_strip_severity,
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
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                },
                onFailure = { e ->
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            hasData = false,
                            errorMessage = e.message ?: "Unable to load forecast"
                        )
                    }
                }
            )
        }
    }

    private fun deriveHeroAndLowPoint(
        points: List<Pair<Float, Float>>,
        severity: String,
        riskLabel: String?
    ): Triple<String, Boolean, Int?> {
        if (points.size < 2) return Triple(
            "On track — no shortfall in the next 30 days",
            false,
            null
        )
        var minY = points[0].second
        var minIdx = 0
        points.forEachIndexed { i, (_, y) ->
            if (y < minY) {
                minY = y
                minIdx = i
            }
        }
        val totalDays = 30
        val lowPointDay = ((minIdx.toFloat() / (points.size - 1).coerceAtLeast(1)) * totalDays).toInt().coerceIn(1, totalDays)
        val isRisk = severity == "warning" || severity == "danger"
        val heroMsg = when {
            isRisk && riskLabel != null -> riskLabel
            isRisk -> "Your balance may hit a low point in $lowPointDay days."
            else -> "On track — no shortfall in the next 30 days"
        }
        return Triple(heroMsg, isRisk, minIdx)
    }

    private fun loadMockData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val points = (0..29).map { i ->
                val x = i / 29f
                val y = 0.5f + 0.4f * (1f - x) - 0.3f * (1f - kotlin.math.abs(x - 0.5f) * 2f)
                Pair(x, y.coerceIn(0.15f, 1f))
            }
            val (heroMsg, heroRisk, lowIdx) = deriveHeroAndLowPoint(points, "warning", "Your balance may hit a low point in 18 days.")
            _uiState.update {
                it.copy(
                    projectionPoints = points,
                    heroMessage = heroMsg,
                    heroIsRisk = heroRisk,
                    lowPointDayIndex = lowIdx,
                    confidenceLabel = "Based on this month's cash flow and spending trend",
                    riskStripLabel = "Dip ahead — consider delaying non-essential spend until after payday.",
                    riskStripSeverity = "warning",
                    savingsOpportunity = "You could save ₹3,200 by trimming dining 10%.",
                    recommendations = listOf(
                        FutureRecommendation(
                            "1",
                            "Trim discretionary spend",
                            "Your spending is ahead of income this month. Focus on needs and defer wants where possible."
                        ),
                        FutureRecommendation(
                            "2",
                            "Top up Emergency goal by ₹2,000",
                            "You're ahead of pace this month. Putting ₹2,000 into your Emergency goal keeps you on track."
                        )
                    ),
                    hasData = true,
                    isLoading = false,
                    errorMessage = null
                )
            }
        }
    }

    fun refresh() {
        loadForecast()
    }
}
