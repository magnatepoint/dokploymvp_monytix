package com.example.monytix.datasupport

import android.net.Uri
import android.util.Log
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.monytix.R

@Composable
fun UploadStatementScreen(
    viewModel: DataSetupViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val uiState by viewModel.uiState.collectAsState()
    val contentResolver = context.contentResolver
    var pendingFile by remember { mutableStateOf<Pair<ByteArray, String>?>(null) }
    var password by remember { mutableStateOf("") }
    val needsPassword = uiState.error?.lowercase()?.contains("password") == true
    val effectivePendingFile = pendingFile ?: uiState.pendingRetryFile

    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        Log.d("MonytixUpload", "UploadStatementScreen file picker: uri=$uri")
        uri?.let {
            try {
                var filename = it.lastPathSegment ?: "statement.pdf"
                contentResolver.query(it, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                        if (idx >= 0) cursor.getString(idx)?.takeIf { n -> n.isNotBlank() }?.let { filename = it }
                    }
                }
                val bytes = contentResolver.openInputStream(it)?.use { stream -> stream.readBytes() }
                if (bytes != null && bytes.isNotEmpty()) {
                    Log.d("MonytixUpload", "UploadStatementScreen: file read ok filename=$filename bytes=${bytes.size}")
                    pendingFile = Pair(bytes, filename)
                    viewModel.clearError()
                    viewModel.clearPendingRetry()
                } else {
                    Log.w("MonytixUpload", "UploadStatementScreen: file read null or empty")
                }
            } catch (e: Exception) {
                Log.e("MonytixUpload", "UploadStatementScreen: file read failed", e)
                viewModel.clearError()
            }
        }
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
            text = stringResource(R.string.upload_title),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.upload_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.7f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = stringResource(R.string.upload_formats),
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(24.dp))

        if (uiState.error != null) {
            Text(
                text = uiState.error!!,
                color = Color(0xFFEF5350),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
        }

        if (effectivePendingFile == null) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(2.dp, Color.White.copy(alpha = 0.4f), RoundedCornerShape(12.dp))
                    .padding(32.dp)
                    .clickable(enabled = !uiState.isLoading) {
                        filePickerLauncher.launch("*/*")
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(48.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Uploading...",
                        color = Color.White.copy(alpha = 0.8f),
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    androidx.compose.material3.Icon(
                        imageVector = Icons.Filled.Upload,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = stringResource(R.string.upload_tap),
                        color = Color.White,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            val (bytes, filename) = effectivePendingFile
            Text(
                text = filename,
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            Text(
                text = if (needsPassword) "PDF Password (required)" else "PDF Password (optional)",
                color = if (needsPassword) Color(0xFFFFB74D) else Color.White.copy(alpha = 0.7f),
                style = MaterialTheme.typography.labelMedium,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                placeholder = {
                    Text(
                        if (needsPassword) "Enter password to unlock PDF" else "Enter password if file is encrypted",
                        color = Color.White.copy(alpha = 0.5f)
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color.White.copy(alpha = 0.6f),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                    cursorColor = Color.White,
                    focusedLabelColor = Color.White.copy(alpha = 0.7f),
                    unfocusedLabelColor = Color.White.copy(alpha = 0.5f)
                )
            )
            if (needsPassword) {
                Text(
                    text = "This file is password protected. Enter the password and tap Upload.",
                    color = Color(0xFFFFB74D),
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Button(
                    onClick = { pendingFile = null; password = ""; viewModel.clearError(); viewModel.clearPendingRetry() },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.White.copy(alpha = 0.2f), contentColor = Color.White)
                ) {
                    Text("Change File")
                }
                Button(
                    onClick = {
                        if (needsPassword && password.isBlank()) return@Button
                        viewModel.uploadFile(bytes, filename, password.takeIf { it.isNotBlank() })
                    },
                    enabled = !uiState.isLoading && (!needsPassword || password.isNotBlank()),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD4AF37), contentColor = Color.Black)
                ) {
                    Text(if (uiState.isLoading) "Uploading..." else "Upload")
                }
            }
        }
    }
}
