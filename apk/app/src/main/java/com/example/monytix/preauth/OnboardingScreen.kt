package com.example.monytix.preauth

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.monytix.R
import com.example.monytix.ui.theme.BackgroundGradientBottom
import com.example.monytix.ui.theme.BackgroundGradientTop
import kotlinx.coroutines.launch

private val teal = Color(0xFF14B8A6)
private val purple = Color(0xFFA78BFA)
private val glassBg = Color.White.copy(alpha = 0.08f)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    onLogin: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(BackgroundGradientTop, BackgroundGradientBottom)
                )
            )
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            when (page) {
                0 -> Slide1Content()
                1 -> Slide2Content()
                2 -> Slide3Content()
            }
        }
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { index ->
                    Box(
                        modifier = Modifier
                            .padding(4.dp)
                            .then(
                                if (index == pagerState.currentPage) {
                                    Modifier
                                        .size(24.dp, 4.dp)
                                        .background(teal, RoundedCornerShape(2.dp))
                                } else {
                                    Modifier
                                        .size(8.dp)
                                        .background(Color.White.copy(alpha = 0.3f), CircleShape)
                                }
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
            if (pagerState.currentPage < 2) {
                Button(
                    onClick = {
                        scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = teal,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.onboarding_continue_mobile), fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(Icons.Default.ArrowForward, contentDescription = null, modifier = Modifier.size(20.dp))
                }
            } else {
                OutlinedButton(
                    onClick = onComplete,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                    border = androidx.compose.foundation.BorderStroke(2.dp, teal)
                ) {
                    Text(stringResource(R.string.onboarding_get_started), fontWeight = FontWeight.SemiBold)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (pagerState.currentPage < 2) "Existing user? " else "Already have an account? ",
                    color = Color.White.copy(alpha = 0.8f),
                    style = MaterialTheme.typography.bodyMedium
                )
                androidx.compose.material3.TextButton(onClick = onLogin) {
                    Text("Login", color = teal, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun Slide1Content() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "₹",
            style = MaterialTheme.typography.displayLarge,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            GlassChip(icon = Icons.Default.ShowChart, label = "+12.4%")
            GlassChip(icon = Icons.Default.AutoAwesome, label = "AI CREDIT SCORE\n₹ 782")
        }
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "Smart Wealth for ",
            style = MaterialTheme.typography.headlineLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Bharat",
            style = MaterialTheme.typography.headlineLarge,
            color = teal,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_slide1_subtitle),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun Slide2Content() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "₹",
            style = MaterialTheme.typography.displayLarge,
            color = Color.White.copy(alpha = 0.9f)
        )
        Spacer(modifier = Modifier.height(48.dp))
        Text(
            text = "AI-powered insights &\n",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "future ",
            style = MaterialTheme.typography.headlineMedium,
            color = teal,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "predictions",
            style = MaterialTheme.typography.headlineMedium,
            color = purple,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun Slide3Content() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(24.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            Row(
                modifier = Modifier
                    .background(glassBg, RoundedCornerShape(20.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(Color(0xFF34D399), CircleShape)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = stringResource(R.string.onboarding_encrypted),
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White
                )
            }
        }
        Spacer(modifier = Modifier.height(32.dp))
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            tint = teal,
            modifier = Modifier.size(80.dp)
        )
        Text(
            text = "256-BIT AES",
            style = MaterialTheme.typography.labelSmall,
            color = Color.White.copy(alpha = 0.6f)
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Bank-grade ",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Encryption & ",
            style = MaterialTheme.typography.headlineMedium,
            color = teal,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Privacy-first design",
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.onboarding_slide3_body),
            style = MaterialTheme.typography.bodyLarge,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        androidx.compose.foundation.layout.Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(4.dp)
                    .background(teal, RoundedCornerShape(2.dp))
            )
        }
    }
}

@Composable
private fun GlassChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String
) {
    Column(
        modifier = Modifier
            .background(glassBg, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = teal,
            modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = Color.White
        )
    }
}
