package com.example.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.OcaVenteOfficialLogo
import com.example.ui.theme.OcaGreenDark
import com.example.ui.theme.OcaGreenPrimary
import com.example.ui.theme.OcaNavySecondary
import kotlinx.coroutines.delay

@Composable
fun SplashScreen(
    onSplashFinished: () -> Unit
) {
    val scale = rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    LaunchedEffect(Unit) {
        delay(5000)
        onSplashFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        OcaNavySecondary,
                        Color(0xFF0F2236),
                        Color(0xFF081525)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // Official Logo Container
            OcaVenteOfficialLogo(
                modifier = Modifier.scale(scale.value),
                iconSize = 120.dp,
                showTagline = true,
                textColorNavy = Color.White,
                taglineColor = Color(0xFF69F0AE)
            )

            Spacer(modifier = Modifier.height(36.dp))

            // Developer Credit
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color.White.copy(alpha = 0.1f)
            ) {
                Text(
                    text = "برمجة راهم محمد لمين",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Subtle Loading Indicator
            CircularProgressIndicator(
                color = Color(0xFF00C853),
                modifier = Modifier.size(32.dp),
                strokeWidth = 3.dp
            )
        }

        // Bottom Copyright
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "جميع الحقوق محفوظة @2026",
                color = Color.White.copy(alpha = 0.75f),
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Medium),
                textAlign = TextAlign.Center
            )
            Text(
                text = "OcaVenteDz • الجزائر 🇩🇿",
                color = Color(0xFF69F0AE).copy(alpha = 0.85f),
                style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                textAlign = TextAlign.Center
            )
        }
    }
}
