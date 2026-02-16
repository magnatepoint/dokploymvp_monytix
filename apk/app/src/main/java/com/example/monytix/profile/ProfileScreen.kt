package com.example.monytix.profile

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PersonOff
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.monytix.BuildConfig
import com.example.monytix.R
import com.example.monytix.ui.theme.Error
import com.example.monytix.ui.theme.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var showLogoutConfirm by remember { mutableStateOf(false) }
    var showDeleteAccount by remember { mutableStateOf(false) }
    var showDeleteDataConfirm by remember { mutableStateOf(false) }
    val colorScheme = MaterialTheme.colorScheme

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.profile_title), color = colorScheme.onBackground) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colorScheme.background,
                    titleContentColor = colorScheme.onBackground
                )
            )
        },
        containerColor = colorScheme.background
    ) { innerPadding ->
        LazyColumn(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item {
                Text(
                    text = "Manage your account and preferences",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 8.dp)
                )
            }
            item {
                ProfileSectionCard(title = "Account", icon = "👤") {
                    ProfileInfoRow(label = "Email", value = uiState.userEmail ?: stringResource(R.string.profile_guest))
                    ProfileInfoRow(label = "User ID", value = (uiState.userId ?: "").take(8).let { if (it.isNotEmpty()) "$it..." else "—" })
                }
            }
            item {
                ProfileSectionCard(title = "Preferences", icon = "⚙️") {
                    ProfilePrefRow(
                        icon = Icons.Default.Notifications,
                        title = "Notifications",
                        subtitle = "Manage notification preferences",
                        onClick = { /* Coming soon */ }
                    )
                    ProfilePrefRow(
                        icon = Icons.Default.Settings,
                        title = "Currency",
                        subtitle = "INR (Indian Rupee)",
                        onClick = { /* Coming soon */ }
                    )
                    ProfilePrefRow(
                        icon = Icons.Default.Settings,
                        title = "Theme",
                        subtitle = "Dark",
                        onClick = { /* Coming soon */ }
                    )
                }
            }
            item {
                ProfileSectionCard(title = "Data Management", icon = "💾") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = { showDeleteDataConfirm = true })
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "🗑️", style = MaterialTheme.typography.titleLarge, modifier = Modifier.width(32.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Delete All Data",
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold,
                                color = Error
                            )
                            Text(
                                text = "Permanently delete all your data",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                        if (uiState.isDeletingData) {
                            CircularProgressIndicator(modifier = Modifier.width(24.dp).height(24.dp), color = MaterialTheme.colorScheme.onSurface, strokeWidth = 2.dp)
                        }
                    }
                }
            }
            item {
                ProfileSectionCard(title = "About", icon = "ℹ️") {
                    ProfileInfoRow(label = "App Version", value = BuildConfig.VERSION_NAME ?: "1.0.0")
                    ProfileInfoRow(label = "Build", value = BuildConfig.VERSION_CODE.toString())
                }
            }
            item {
                ProfileSectionTitle(stringResource(R.string.profile_account))
            }
            item {
                ProfileMenuItem(
                    icon = Icons.AutoMirrored.Filled.Logout,
                    title = stringResource(R.string.profile_logout),
                    onClick = { showLogoutConfirm = true },
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                ProfileMenuItem(
                    icon = Icons.Default.Download,
                    title = stringResource(R.string.profile_data_export),
                    onClick = { viewModel.exportData() },
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                ProfileMenuItem(
                    icon = Icons.Default.PersonOff,
                    title = stringResource(R.string.profile_deactivate),
                    onClick = { viewModel.deactivateAccount() },
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                ProfileMenuItem(
                    icon = Icons.Default.VerifiedUser,
                    title = stringResource(R.string.profile_re_kyc),
                    onClick = { viewModel.reKyc() },
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                ProfileMenuItem(
                    icon = Icons.Default.Warning,
                    title = stringResource(R.string.profile_consent_withdrawal),
                    onClick = { viewModel.withdrawConsent() },
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            item {
                ProfileSectionTitle(stringResource(R.string.profile_danger_zone))
            }
            item {
                ProfileMenuItem(
                    icon = Icons.Default.Delete,
                    title = stringResource(R.string.profile_delete_account),
                    onClick = { showDeleteAccount = true },
                    isDestructive = true,
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            if (uiState.error != null) {
                item {
                    Text(
                        text = uiState.error!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = Error,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            }
        }
    }

    if (showLogoutConfirm) {
        LogoutConfirmDialog(
            onConfirm = {
                viewModel.logout()
                showLogoutConfirm = false
            },
            onDismiss = { showLogoutConfirm = false }
        )
    }

    if (showDeleteAccount) {
        DeleteAccountDialog(
            onConfirm = {
                viewModel.deleteAccount()
                showDeleteAccount = false
            },
            onDismiss = { showDeleteAccount = false }
        )
    }

    if (showDeleteDataConfirm) {
        DeleteDataConfirmDialog(
            onConfirm = {
                viewModel.deleteAllData()
                showDeleteDataConfirm = false
            },
            onDismiss = { showDeleteDataConfirm = false }
        )
    }

    if (uiState.deleteDataSuccess) {
        DeleteDataSuccessDialog(
            onDismiss = { viewModel.clearDeleteSuccess() }
        )
    }
}

@Composable
private fun ProfileSectionCard(
    title: String,
    icon: String,
    content: @Composable () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = GlassCard),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = icon, style = MaterialTheme.typography.titleMedium)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = androidx.compose.ui.text.font.FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ProfileInfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Text(text = value, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
private fun ProfilePrefRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f))
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
        }
    }
}

@Composable
private fun DeleteDataConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete All Data", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Text(
                "This will permanently delete all your transaction data, goals, budgets, and moments. This action cannot be undone. Are you sure you want to continue?",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text("Delete", color = Error)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel", color = MaterialTheme.colorScheme.onSurface)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    )
}

@Composable
private fun DeleteDataSuccessDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Data Deleted", color = MaterialTheme.colorScheme.onSurface) },
        text = {
            Text(
                "All your data has been successfully deleted.",
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
            )
        },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("OK", color = MaterialTheme.colorScheme.primary)
            }
        },
        containerColor = MaterialTheme.colorScheme.background
    )
}

@Composable
private fun ProfileSectionTitle(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
        modifier = Modifier.padding(top = 16.dp, bottom = 4.dp)
    )
}

@Composable
private fun ProfileMenuItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    onClick: () -> Unit,
    isDestructive: Boolean = false,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val contentColor = if (isDestructive) Error else tint
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = contentColor)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            color = contentColor
        )
    }
}

@Composable
private fun LogoutConfirmDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.logout_confirm_title)) },
        text = { Text(stringResource(R.string.logout_confirm_desc)) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.logout_confirm_yes), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.logout_confirm_cancel))
            }
        }
    )
}

@Composable
private fun DeleteAccountDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.delete_account_title)) },
        text = { Text(stringResource(R.string.delete_account_desc)) },
        confirmButton = {
            androidx.compose.material3.TextButton(onClick = onConfirm) {
                Text(stringResource(R.string.delete_account_confirm), color = MaterialTheme.colorScheme.error)
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.delete_account_cancel))
            }
        }
    )
}
