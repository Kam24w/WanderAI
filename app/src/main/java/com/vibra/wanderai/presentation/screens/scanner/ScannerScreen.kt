package com.vibra.wanderai.presentation.screens.scanner

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vibra.wanderai.R
import com.vibra.wanderai.ui.theme.DarkBackground
import com.vibra.wanderai.ui.theme.GoldAccent
import com.vibra.wanderai.ui.theme.NavyDeep
import com.vibra.wanderai.ui.theme.TealBrand

@Composable
fun ScannerScreen() {
    val pulse = rememberInfiniteTransition(label = "scan")
    val scanAlpha by pulse.animateFloat(
        initialValue = 0.3f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Reverse),
        label = "alpha"
    )
    val scanY by pulse.animateFloat(
        initialValue = 0f, targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000, easing = LinearEasing), RepeatMode.Restart),
        label = "scanLine"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF050D1A), DarkBackground, NavyDeep)))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            Spacer(Modifier.height(24.dp))

            // ── Top status bar ───────────────────────────────────────────────
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .background(TealBrand.copy(alpha = scanAlpha), CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.scanner_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActionIcon(icon = Icons.Outlined.FlashOn)
                    ActionIcon(icon = Icons.Outlined.History)
                }
            }

            Spacer(Modifier.height(16.dp))

            Text(
                text = stringResource(R.string.scanner_instruction),
                style = MaterialTheme.typography.bodyMedium,
                color = TealBrand,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(Modifier.weight(0.4f))

            // ── Viewfinder ───────────────────────────────────────────────────
            Box(
                modifier = Modifier.size(260.dp),
                contentAlignment = Alignment.Center
            ) {
                // Dark glass background
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .background(Color.White.copy(alpha = 0.03f))
                )

                // Animated scan line
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(28.dp))
                        .drawWithContent {
                            drawContent()
                            val y = size.height * scanY
                            drawLine(
                                brush = Brush.horizontalGradient(
                                    listOf(
                                        Color.Transparent,
                                        TealBrand.copy(alpha = 0.6f),
                                        TealBrand.copy(alpha = 0.8f),
                                        TealBrand.copy(alpha = 0.6f),
                                        Color.Transparent
                                    )
                                ),
                                start = Offset(0f, y),
                                end = Offset(size.width, y),
                                strokeWidth = 2.dp.toPx(),
                                cap = StrokeCap.Round
                            )
                        }
                )

                // Camera icon
                Icon(
                    Icons.Outlined.CameraAlt,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.18f),
                    modifier = Modifier.size(72.dp)
                )

                // Corner brackets
                CornerBracket(260.dp, TealBrand.copy(alpha = scanAlpha))
            }

            Spacer(Modifier.height(28.dp))

            // ── Hint text ────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.scanner_hint),
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.5f),
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(Modifier.weight(0.6f))

            // ── Coming soon card ─────────────────────────────────────────────
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.05f))
                    .border(1.dp, TealBrand.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(TealBrand.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Outlined.AutoAwesome, null,
                            tint = GoldAccent, modifier = Modifier.size(22.dp))
                    }
                    Spacer(Modifier.width(14.dp))
                    Column {
                        Text(
                            text = "WanderAI Vision",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = stringResource(R.string.scanner_coming_soon),
                            style = MaterialTheme.typography.bodySmall,
                            color = TealBrand,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun ActionIcon(icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, null, tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun CornerBracket(size: Dp, color: Color) {
    val stroke = 2.5.dp
    val cornerSize = 24.dp
    val radius = 8.dp

    Box(
        modifier = Modifier
            .size(size)
            .drawWithContent {
                drawContent()
                val s = this.size
                val c = cornerSize.toPx()
                val r = radius.toPx()
                val sw = stroke.toPx()

                // Top-left
                drawLine(color, Offset(r, 0f), Offset(c, 0f), sw, StrokeCap.Round)
                drawLine(color, Offset(0f, r), Offset(0f, c), sw, StrokeCap.Round)
                // Top-right
                drawLine(color, Offset(s.width - c, 0f), Offset(s.width - r, 0f), sw, StrokeCap.Round)
                drawLine(color, Offset(s.width, r), Offset(s.width, c), sw, StrokeCap.Round)
                // Bottom-left
                drawLine(color, Offset(r, s.height), Offset(c, s.height), sw, StrokeCap.Round)
                drawLine(color, Offset(0f, s.height - c), Offset(0f, s.height - r), sw, StrokeCap.Round)
                // Bottom-right
                drawLine(color, Offset(s.width - c, s.height), Offset(s.width - r, s.height), sw, StrokeCap.Round)
                drawLine(color, Offset(s.width, s.height - c), Offset(s.width, s.height - r), sw, StrokeCap.Round)
            }
    )
}
