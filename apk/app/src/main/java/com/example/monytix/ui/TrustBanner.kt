package com.example.monytix.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.monytix.R

/**
 * Trust signal block for fintech flows: login, upload, onboarding.
 * Premium, calm copy to reinforce encryption and user control.
 */
@Composable
fun TrustBanner(
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    learnMoreLabel: String? = null,
    onLearnMore: (() -> Unit)? = null
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }
        Text(
            text = headline,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.9f),
            modifier = Modifier.padding(top = 4.dp)
        )
        if (learnMoreLabel != null && onLearnMore != null) {
            TextButton(
                onClick = onLearnMore,
                modifier = Modifier.padding(top = 4.dp)
            ) {
                Text(learnMoreLabel)
            }
        }
    }
}

@Composable
fun TrustBannerEncryption(modifier: Modifier = Modifier) {
    TrustBanner(
        headline = stringResource(R.string.trust_encryption_title),
        body = stringResource(R.string.trust_encryption_body),
        modifier = modifier
    )
}

@Composable
fun TrustBannerUpload(modifier: Modifier = Modifier) {
    TrustBanner(
        headline = stringResource(R.string.trust_upload_title),
        body = stringResource(R.string.trust_upload_body),
        modifier = modifier
    )
}

@Composable
fun TrustBannerReadOnly(modifier: Modifier = Modifier) {
    TrustBanner(
        headline = stringResource(R.string.trust_read_only_title),
        body = stringResource(R.string.trust_read_only_body),
        modifier = modifier
    )
}
