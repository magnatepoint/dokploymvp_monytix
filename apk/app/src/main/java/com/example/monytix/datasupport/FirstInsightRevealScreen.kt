package com.example.monytix.datasupport

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.monytix.data.KpiResponse
import com.example.monytix.R

@Composable
fun FirstInsightRevealScreen(
    kpis: KpiResponse,
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val burnRate = kpis.total_debits_amount
    val income = kpis.income_amount
    val savingsRate = if (income > 0) ((income - burnRate) / income * 100).coerceIn(0.0, 100.0) else 0.0
    val top3 = kpis.top_categories.take(3)
    val emiAmount = kpis.top_categories
        .filter { it.category_code.contains("loan", ignoreCase = true) || it.category_code == "loans_payments" }
        .sumOf { it.spend_amount }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.insight_reveal_title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.insight_reveal_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        InsightCard(
            title = stringResource(R.string.insight_burn_rate),
            value = formatCurrency(burnRate),
            subtitle = stringResource(R.string.insight_burn_rate_desc)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (top3.isNotEmpty()) {
            InsightCard(
                title = stringResource(R.string.insight_top_categories),
                value = top3.joinToString("\n") { "• ${it.category_name}: ${formatCurrency(it.spend_amount)}" },
                subtitle = ""
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        InsightCard(
            title = stringResource(R.string.insight_savings_rate),
            value = "${savingsRate.toInt()}%",
            subtitle = stringResource(R.string.insight_savings_rate_desc)
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (emiAmount > 0) {
            InsightCard(
                title = stringResource(R.string.insight_emi_risk),
                value = formatCurrency(emiAmount),
                subtitle = stringResource(R.string.insight_emi_risk_desc),
                isWarning = true
            )
            Spacer(modifier = Modifier.height(16.dp))
        } else {
            InsightCard(
                title = stringResource(R.string.insight_emi_risk),
                value = stringResource(R.string.insight_no_emi),
                subtitle = stringResource(R.string.insight_emi_safe_desc),
                isWarning = false
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onDone,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            Text(stringResource(R.string.insight_done), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun InsightCard(
    title: String,
    value: String,
    subtitle: String,
    isWarning: Boolean = false,
    modifier: Modifier = Modifier
) {
    val bgColor = if (isWarning) Color(0x33FF9800) else Color.White.copy(alpha = 0.08f)
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(bgColor, RoundedCornerShape(12.dp))
            .padding(20.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleSmall,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            color = if (isWarning) Color(0xFFFF9800) else Color.White,
            fontWeight = FontWeight.SemiBold
        )
        if (subtitle.isNotEmpty()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        }
    }
}

private fun formatCurrency(amount: Double): String {
    val abs = kotlin.math.abs(amount)
    val formatted = java.text.NumberFormat.getIntegerInstance(java.util.Locale.US).format(abs.toLong())
    return if (amount < 0) "-₹$formatted" else "₹$formatted"
}
