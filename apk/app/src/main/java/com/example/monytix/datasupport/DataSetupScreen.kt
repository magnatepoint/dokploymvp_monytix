package com.example.monytix.datasupport

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.monytix.R

@Composable
fun DataSetupScreen(
    viewModel: DataSetupViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()

    when (val step = uiState.step) {
        DataSetupStep.UploadOrManual -> {
            UploadOrManualChoice(
                onUpload = viewModel::showUploadStatement,
                onManualIncome = { viewModel.showManualAdd(ManualAddType.INCOME) },
                onManualExpense = { viewModel.showManualAdd(ManualAddType.EXPENSE) },
                onManualLoan = { viewModel.showManualAdd(ManualAddType.LOAN) },
                onManualInvestment = { viewModel.showManualAdd(ManualAddType.INVESTMENT) }
            )
        }
        DataSetupStep.UploadStatement -> {
            UploadStatementScreen(
                viewModel = viewModel,
                onBack = viewModel::backToUploadOrManual
            )
        }
        DataSetupStep.ParsingProgress -> {
            ParsingProgressScreen()
        }
        DataSetupStep.DataImportedSuccess -> {
            DataImportedSuccessScreen(
                onDone = viewModel::onDataImportedDone
            )
        }
        DataSetupStep.Analyzing -> {
            AnalyzingScreen(
                progress = uiState.analyzingProgress
            )
        }
        DataSetupStep.FirstInsightReveal -> {
            uiState.kpis?.let { kpis ->
                FirstInsightRevealScreen(
                    kpis = kpis,
                    onDone = viewModel::onFirstInsightDone
                )
            } ?: run {
                AnalyzingScreen(progress = 0.9f)
            }
        }
        DataSetupStep.ManualAdd -> {
            ManualAddScreen(
                viewModel = viewModel,
                type = uiState.manualAddType ?: ManualAddType.EXPENSE,
                onBack = viewModel::backToUploadOrManual
            )
        }
    }
}

@Composable
private fun UploadOrManualChoice(
    onUpload: () -> Unit,
    onManualIncome: () -> Unit,
    onManualExpense: () -> Unit,
    onManualLoan: () -> Unit,
    onManualInvestment: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(24.dp)
    ) {
        Text(
            text = stringResource(R.string.data_setup_title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.data_setup_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(32.dp))

        DataOption(
            icon = Icons.Default.Upload,
            title = stringResource(R.string.upload_option_title),
            subtitle = stringResource(R.string.upload_option_subtitle),
            onClick = onUpload
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = stringResource(R.string.manual_option_title),
            style = MaterialTheme.typography.titleMedium,
            color = Color.White.copy(alpha = 0.9f),
            modifier = Modifier.padding(top = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ManualSubOption(
                text = stringResource(R.string.manual_income),
                modifier = Modifier.weight(1f),
                onClick = onManualIncome
            )
            ManualSubOption(
                text = stringResource(R.string.manual_expense),
                modifier = Modifier.weight(1f),
                onClick = onManualExpense
            )
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ManualSubOption(
                text = stringResource(R.string.manual_loan),
                modifier = Modifier.weight(1f),
                onClick = onManualLoan
            )
            ManualSubOption(
                text = stringResource(R.string.manual_investment),
                modifier = Modifier.weight(1f),
                onClick = onManualInvestment
            )
        }
    }
}

@Composable
private fun DataOption(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(20.dp)
            .background(Color.White.copy(alpha = 0.08f), RoundedCornerShape(12.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.padding(end = 16.dp)
            )
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun ManualSubOption(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Button(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(8.dp),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
        contentPadding = ButtonDefaults.ContentPadding
    ) {
        Text(text, style = MaterialTheme.typography.bodyMedium)
    }
}
