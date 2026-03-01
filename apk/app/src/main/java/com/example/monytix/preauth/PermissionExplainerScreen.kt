package com.example.monytix.preauth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.People
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.monytix.R

private val teal = Color(0xFF14B8A6)
private val lightTeal = Color(0xFF99F6E4)
private val surfaceDark = Color(0xFF0D0D0F)
private val surfaceElevated = Color(0xFF161618)

@Composable
fun PermissionExplainerScreen(
    onContinue: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceDark)
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = null,
                tint = teal,
                modifier = Modifier
                    .size(48.dp)
                    .background(teal.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(12.dp)
            )
        }
        Text(
            text = stringResource(R.string.permission_security_experience),
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.permission_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f)
        )
        Spacer(modifier = Modifier.height(24.dp))
        PermissionCard(
            icon = Icons.Default.ChatBubbleOutline,
            title = stringResource(R.string.permission_transaction_title),
            description = stringResource(R.string.permission_transaction_desc)
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionCard(
            icon = Icons.Default.People,
            title = stringResource(R.string.permission_split_title),
            description = stringResource(R.string.permission_split_desc)
        )
        Spacer(modifier = Modifier.height(12.dp))
        PermissionCard(
            icon = Icons.Default.LocationOn,
            title = stringResource(R.string.permission_enhanced_title),
            description = stringResource(R.string.permission_enhanced_desc)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = teal,
                contentColor = Color.White
            )
        ) {
            Text(stringResource(R.string.permission_allow), fontWeight = FontWeight.SemiBold)
        }
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(R.string.permission_later),
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
private fun PermissionCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(surfaceElevated, RoundedCornerShape(16.dp))
            .padding(16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .background(teal.copy(alpha = 0.2f), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = teal,
                modifier = Modifier.size(24.dp)
            )
        }
        Spacer(modifier = Modifier.size(12.dp))
        Column {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.8f)
            )
        }
    }
}
