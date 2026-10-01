package com.example.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.OcaGreenPrimary
import com.example.ui.theme.OcaNavySecondary

// Authentic OcaVenteDz Logo Colors
val OcaLogoNavy = Color(0xFF002D62)
val OcaLogoGreen = Color(0xFF00C853)
val OcaLogoLightGreen = Color(0xFF69F0AE)
val OcaLogoCartBlue = Color(0xFF0A4D90)

@Composable
fun OcaVenteOfficialLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 100.dp,
    showTagline: Boolean = true,
    textColorNavy: Color = OcaLogoNavy,
    taglineColor: Color = OcaLogoNavy
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Shopping Cart Illustration Box
        Box(
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(iconSize * 0.22f))
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize().padding(iconSize * 0.12f)) {
                val w = size.width
                val h = size.height

                // 1. Green Speed Lines on the Left
                val lineThickness = h * 0.05f
                val green = OcaLogoGreen

                // Top speed line
                drawLine(
                    color = green,
                    start = Offset(w * 0.02f, h * 0.40f),
                    end = Offset(w * 0.22f, h * 0.40f),
                    strokeWidth = lineThickness,
                    cap = StrokeCap.Round
                )
                // Middle speed line (longest)
                drawLine(
                    color = green,
                    start = Offset(0f, h * 0.50f),
                    end = Offset(w * 0.26f, h * 0.50f),
                    strokeWidth = lineThickness,
                    cap = StrokeCap.Round
                )
                // Lower-middle speed line
                drawLine(
                    color = green,
                    start = Offset(w * 0.08f, h * 0.60f),
                    end = Offset(w * 0.28f, h * 0.60f),
                    strokeWidth = lineThickness,
                    cap = StrokeCap.Round
                )
                // Bottom speed line
                drawLine(
                    color = green,
                    start = Offset(w * 0.15f, h * 0.70f),
                    end = Offset(w * 0.26f, h * 0.70f),
                    strokeWidth = lineThickness,
                    cap = StrokeCap.Round
                )

                // 2. Items inside cart (Silhouette shapes)
                // T-Shirt (Green)
                drawRoundRect(
                    color = Color(0xFF00E676),
                    topLeft = Offset(w * 0.35f, h * 0.08f),
                    size = Size(w * 0.34f, h * 0.34f),
                    cornerRadius = CornerRadius(w * 0.08f, h * 0.08f)
                )

                // Smartphone (Dark slate)
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(w * 0.22f, h * 0.14f),
                    size = Size(w * 0.22f, h * 0.38f),
                    cornerRadius = CornerRadius(w * 0.04f, h * 0.04f)
                )

                // Package box (Warm amber)
                drawRoundRect(
                    color = Color(0xFFF59E0B),
                    topLeft = Offset(w * 0.60f, h * 0.18f),
                    size = Size(w * 0.26f, h * 0.24f),
                    cornerRadius = CornerRadius(w * 0.03f, h * 0.03f)
                )

                // Game Controller / Sneaker (Navy blue & white accent)
                drawRoundRect(
                    color = OcaLogoCartBlue,
                    topLeft = Offset(w * 0.38f, h * 0.22f),
                    size = Size(w * 0.30f, h * 0.26f),
                    cornerRadius = CornerRadius(w * 0.06f, h * 0.06f)
                )

                // 3. Cart Basket (Two-tone: Green top, Navy base)
                val cartBasketPath = Path().apply {
                    moveTo(w * 0.32f, h * 0.44f)
                    lineTo(w * 0.76f, h * 0.44f)
                    lineTo(w * 0.70f, h * 0.68f)
                    lineTo(w * 0.38f, h * 0.68f)
                    close()
                }
                drawPath(cartBasketPath, color = OcaLogoGreen)

                val cartLowerPath = Path().apply {
                    moveTo(w * 0.35f, h * 0.54f)
                    lineTo(w * 0.73f, h * 0.54f)
                    lineTo(w * 0.70f, h * 0.68f)
                    lineTo(w * 0.38f, h * 0.68f)
                    close()
                }
                drawPath(cartLowerPath, color = OcaLogoNavy)

                // 4. Cart Frame & Handle (Deep Navy Blue)
                val cartFrame = Path().apply {
                    moveTo(w * 0.22f, h * 0.32f)
                    lineTo(w * 0.30f, h * 0.32f)
                    lineTo(w * 0.40f, h * 0.72f)
                    lineTo(w * 0.72f, h * 0.72f)
                }
                drawPath(
                    cartFrame,
                    color = OcaLogoNavy,
                    style = Stroke(width = w * 0.07f, cap = StrokeCap.Round)
                )

                // 5. Wheels
                drawCircle(
                    color = OcaLogoNavy,
                    radius = w * 0.09f,
                    center = Offset(w * 0.46f, h * 0.86f)
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.04f,
                    center = Offset(w * 0.46f, h * 0.86f)
                )

                drawCircle(
                    color = OcaLogoNavy,
                    radius = w * 0.09f,
                    center = Offset(w * 0.68f, h * 0.86f)
                )
                drawCircle(
                    color = Color.White,
                    radius = w * 0.04f,
                    center = Offset(w * 0.68f, h * 0.86f)
                )

                // 6. Green Price Tag on the Right
                val tagPath = Path().apply {
                    moveTo(w * 0.82f, h * 0.38f)
                    lineTo(w * 0.98f, h * 0.48f)
                    lineTo(w * 0.90f, h * 0.82f)
                    lineTo(w * 0.74f, h * 0.72f)
                    close()
                }
                drawPath(tagPath, color = OcaLogoGreen)

                // Tag eyelet
                drawCircle(
                    color = Color.White,
                    radius = w * 0.025f,
                    center = Offset(w * 0.84f, h * 0.44f)
                )
            }
        }

        // Brand Name Text: "OcaVenteDz"
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Text(
                text = "OcaVente",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (iconSize.value * 0.28f).sp,
                    letterSpacing = (-0.5).sp
                ),
                color = textColorNavy
            )
            Text(
                text = "Dz",
                style = MaterialTheme.typography.headlineLarge.copy(
                    fontWeight = FontWeight.Black,
                    fontSize = (iconSize.value * 0.28f).sp,
                    letterSpacing = (-0.5).sp
                ),
                color = OcaLogoGreen
            )
        }

        // Tagline: "- بيع وشراء المستعمل بكل ثقة -"
        if (showTagline) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(OcaLogoGreen)
                )
                Text(
                    text = "بيع وشراء المستعمل بكل ثقة",
                    style = MaterialTheme.typography.bodyMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = (iconSize.value * 0.14f).sp
                    ),
                    color = taglineColor
                )
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(OcaLogoGreen)
                )
            }
        }
    }
}

@Composable
fun OcaVenteCompactBadge(
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize().padding(size * 0.12f)) {
            val w = this.size.width
            val h = this.size.height

            // Green speed line
            drawLine(
                color = OcaLogoGreen,
                start = Offset(0f, h * 0.45f),
                end = Offset(w * 0.25f, h * 0.45f),
                strokeWidth = h * 0.08f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = OcaLogoGreen,
                start = Offset(w * 0.05f, h * 0.60f),
                end = Offset(w * 0.28f, h * 0.60f),
                strokeWidth = h * 0.08f,
                cap = StrokeCap.Round
            )

            // Cart Basket
            val basket = Path().apply {
                moveTo(w * 0.34f, h * 0.38f)
                lineTo(w * 0.78f, h * 0.38f)
                lineTo(w * 0.70f, h * 0.68f)
                lineTo(w * 0.40f, h * 0.68f)
                close()
            }
            drawPath(basket, color = OcaLogoGreen)

            // Cart lower half
            val lower = Path().apply {
                moveTo(w * 0.38f, h * 0.52f)
                lineTo(w * 0.74f, h * 0.52f)
                lineTo(w * 0.70f, h * 0.68f)
                lineTo(w * 0.40f, h * 0.68f)
                close()
            }
            drawPath(lower, color = OcaLogoNavy)

            // Frame
            val frame = Path().apply {
                moveTo(w * 0.22f, h * 0.28f)
                lineTo(w * 0.32f, h * 0.28f)
                lineTo(w * 0.42f, h * 0.74f)
                lineTo(w * 0.74f, h * 0.74f)
            }
            drawPath(frame, color = OcaLogoNavy, style = Stroke(width = w * 0.09f, cap = StrokeCap.Round))

            // Wheels
            drawCircle(color = OcaLogoNavy, radius = w * 0.10f, center = Offset(w * 0.48f, h * 0.86f))
            drawCircle(color = Color.White, radius = w * 0.04f, center = Offset(w * 0.48f, h * 0.86f))
            drawCircle(color = OcaLogoNavy, radius = w * 0.10f, center = Offset(w * 0.70f, h * 0.86f))
            drawCircle(color = Color.White, radius = w * 0.04f, center = Offset(w * 0.70f, h * 0.86f))

            // Tag
            val tag = Path().apply {
                moveTo(w * 0.82f, h * 0.36f)
                lineTo(w * 0.98f, h * 0.46f)
                lineTo(w * 0.92f, h * 0.78f)
                lineTo(w * 0.76f, h * 0.68f)
                close()
            }
            drawPath(tag, color = OcaLogoGreen)
        }
    }
}
