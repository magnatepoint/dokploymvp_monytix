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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.monytix.R
import com.example.monytix.ui.MonytixSpinner
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Composable
fun ManualAddScreen(
    viewModel: DataSetupViewModel,
    type: ManualAddType,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState = viewModel.uiState
    var merchant by remember { mutableStateOf("") }
    var amount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)

    val (title, direction, categoryHint) = when (type) {
        ManualAddType.INCOME -> Triple(R.string.manual_income, "credit", "inc_other")
        ManualAddType.EXPENSE -> Triple(R.string.manual_expense, "debit", "shop_general")
        ManualAddType.LOAN -> Triple(R.string.manual_loan, "debit", "loans_payments")
        ManualAddType.INVESTMENT -> Triple(R.string.manual_investment, "debit", "investments_commitments")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = Color.White)
        ) {
            Text("← Back")
        }
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.value.error != null) {
            Text(
                text = uiState.value.error!!,
                color = Color(0xFFEF5350),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        OutlinedTextField(
            value = merchant,
            onValueChange = { merchant = it },
            label = { Text("Merchant / Description", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.value.isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { c -> c.isDigit() || c == '.' } },
            label = { Text("Amount (₹)", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.value.isLoading,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = description,
            onValueChange = { description = it },
            label = { Text("Notes (optional)", color = Color.White.copy(alpha = 0.7f)) },
            modifier = Modifier.fillMaxWidth(),
            enabled = !uiState.value.isLoading,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                cursorColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                val amt = amount.toDoubleOrNull()
                if (amt != null && amt > 0 && merchant.isNotBlank()) {
                    viewModel.createManualTransaction(
                        txnDate = today,
                        merchantName = merchant,
                        description = description.ifBlank { null },
                        amount = amt,
                        direction = direction,
                        categoryCode = categoryHint
                    )
                }
            },
            modifier = Modifier.fillMaxWidth().height(52.dp),
            enabled = !uiState.value.isLoading && merchant.isNotBlank() && amount.toDoubleOrNull() != null,
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color.White,
                contentColor = Color.Black
            )
        ) {
            if (uiState.value.isLoading) {
                MonytixSpinner(size = 20.dp, stroke = 2.dp)
            } else {
                Text(stringResource(R.string.manual_add), fontWeight = FontWeight.Medium)
            }
        }
    }
}
