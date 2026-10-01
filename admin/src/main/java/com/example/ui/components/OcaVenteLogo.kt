package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

val OcaVenteBlueDark = Color(0xFF052D72)
val OcaVenteBluePrimary = Color(0xFF0D4FA6)
val OcaVenteOrange = Color(0xFFFF9800)
val OcaVenteGreen = Color(0xFF22C55E)

/**
 * Compact OcaVenteDz logo for App Bars and headers
 */
@Composable
fun OcaVenteDzCompactLogo(
    modifier: Modifier = Modifier,
    iconSize: Dp = 38.dp,
    titleSize: TextUnit = 20.sp,
    showSubtitle: Boolean = false,
    textColor: Color = Color.White
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Image(
            painter = painterResource(id = R.drawable.ic_ocavente_logo),
            contentDescription = "OcaVenteDz Logo",
            modifier = Modifier
                .size(iconSize)
                .clip(RoundedCornerShape(8.dp))
        )
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "OcaVente",
                    fontSize = titleSize,
                    fontWeight = FontWeight.Black,
                    color = textColor
                )
                Text(
                    text = "Dz",
                    fontSize = titleSize,
                    fontWeight = FontWeight.Black,
                    color = OcaVenteOrange
                )
            }
            if (showSubtitle) {
                Text(
                    text = "بيع و شراء الأشياء المستعملة",
                    fontSize = (titleSize.value * 0.45f).sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.85f)
                )
            }
        }
    }
}

/**
 * Full branded card displaying the complete official OcaVenteDz logo exactly matching the user's design.
 */
@Composable
fun OcaVenteDzFullBrandCard(
    modifier: Modifier = Modifier,
    logoSize: Dp = 90.dp
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .background(
                Brush.verticalGradient(
                    listOf(OcaVenteBluePrimary, OcaVenteBlueDark)
                )
            )
            .padding(16.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(id = R.drawable.ic_ocavente_logo),
                contentDescription = "OcaVenteDz Logo",
                modifier = Modifier
                    .size(logoSize)
                    .clip(RoundedCornerShape(18.dp))
            )
            Spacer(modifier = Modifier.height(10.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "OcaVente",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
                Text(
                    text = "Dz",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Black,
                    color = OcaVenteOrange
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(OcaVenteOrange, RoundedCornerShape(1.dp))
                )
                Text(
                    text = "بيع و شراء الأشياء المستعملة",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Box(
                    modifier = Modifier
                        .width(20.dp)
                        .height(2.dp)
                        .background(OcaVenteOrange, RoundedCornerShape(1.dp))
                )
            }
        }
    }
}
