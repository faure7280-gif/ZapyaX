package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.DualChannelStats
import com.example.ui.theme.AcrylicCardBg
import com.example.ui.theme.AcrylicHighlight
import androidx.compose.foundation.Canvas
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.example.ui.theme.BorderSubtle
import com.example.ui.theme.ChannelOneColor
import com.example.ui.theme.ChannelTwoColor
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VoidDark
import com.example.ui.theme.ZapyaGradient
import com.example.ui.theme.ZapyaOrange
import com.example.ui.theme.ZapyaPurple
import com.example.ui.theme.ZapyaRadialGlow

@Composable
fun ZapyaZLogo(
    modifier: Modifier = Modifier,
    size: Dp = 42.dp
) {
    Box(
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(size * 0.28f))
            .background(
                Brush.linearGradient(
                    colors = listOf(
                        VoidDark,
                        Color(0xFF1E1035)
                    )
                )
            )
            .border(
                1.5.dp,
                Brush.linearGradient(
                    colors = listOf(
                        ZapyaPurple,
                        ZapyaOrange
                    )
                ),
                RoundedCornerShape(size * 0.28f)
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidth = this.size.minDimension * 0.18f
            val w = this.size.width
            val h = this.size.height

            // Glow blur simulation
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0x77A855F7),
                        Color(0x44FF6B00),
                        Color.Transparent
                    ),
                    radius = w * 0.65f
                ),
                radius = w * 0.5f
            )

            // Dynamic Zapya-style Z path with fast angles
            val path = androidx.compose.ui.graphics.Path().apply {
                val padX = w * 0.22f
                val padY = h * 0.24f
                // Top horizontal stroke
                moveTo(padX, padY)
                lineTo(w - padX, padY)
                // Diagonal stroke
                lineTo(padX, h - padY)
                // Bottom horizontal stroke
                lineTo(w - padX, h - padY)
            }

            drawPath(
                path = path,
                brush = Brush.linearGradient(
                    colors = listOf(ZapyaPurple, ZapyaOrange),
                    start = Offset(0f, 0f),
                    end = Offset(w, h)
                ),
                style = Stroke(
                    width = strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

@Composable
fun AcrylicCard(
    modifier: Modifier = Modifier,
    shape: Shape = RoundedCornerShape(20.dp),
    borderColor: Color = BorderSubtle,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(
                            borderColor,
                            Color(0x33FF6B00),
                            AcrylicHighlight
                        )
                    )
                ),
                shape
            ),
        shape = shape,
        color = AcrylicCardBg,
        shadowElevation = 8.dp
    ) {
        Box(
            modifier = Modifier.background(ZapyaRadialGlow)
        ) {
            content()
        }
    }
}

@Composable
fun E2eeSecurityPill(
    fingerprint: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .clip(RoundedCornerShape(30.dp))
            .clickable { onClick() }
            .border(
                BorderStroke(1.dp, Color(0x3310B981)),
                RoundedCornerShape(30.dp)
            )
            .testTag("e2ee_security_pill"),
        color = Color(0xD9062319),
        shape = RoundedCornerShape(30.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Cifrado E2EE",
                tint = EmeraldShield,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = "AES-256 E2EE",
                color = EmeraldShield,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .background(Color(0x6610B981), CircleShape)
            )
            Text(
                text = if (fingerprint.isNotBlank()) fingerprint else "VERIFICADO",
                color = Color(0xFF6EE7B7),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun DualChannelActiveBanner(
    stats: DualChannelStats,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val alphaAnim by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "alpha"
    )

    AcrylicCard(
        modifier = modifier.fillMaxWidth(),
        borderColor = ZapyaPurple
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(10.dp)
                            .background(
                                ZapyaOrange.copy(alpha = alphaAnim),
                                CircleShape
                            )
                    )
                    Text(
                        text = "MODO DUAL STREAM ACTIVO",
                        color = ZapyaOrange,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Speed,
                        contentDescription = "Velocidad combinada",
                        tint = ZapyaOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "%.1f MB/s".format(stats.aggregateSpeedMBps),
                        color = TextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Text(
                text = "Envío simultáneo de 2 archivos en paralelo con balanceo de carga",
                color = TextSecondary,
                fontSize = 12.sp
            )

            // Stream 1 Gauge (Electric Purple)
            StreamWorkerRow(
                channelLabel = "CANAL 1 (MORADO)",
                channelColor = ChannelOneColor,
                isActive = stats.channel1Active,
                fileName = stats.channel1FileName,
                progress = stats.channel1Progress,
                speedMBps = stats.channel1SpeedMBps
            )

            // Stream 2 Gauge (Sunset Orange)
            StreamWorkerRow(
                channelLabel = "CANAL 2 (NARANJA)",
                channelColor = ChannelTwoColor,
                isActive = stats.channel2Active,
                fileName = stats.channel2FileName,
                progress = stats.channel2Progress,
                speedMBps = stats.channel2SpeedMBps
            )
        }
    }
}

@Composable
private fun StreamWorkerRow(
    channelLabel: String,
    channelColor: Color,
    isActive: Boolean,
    fileName: String,
    progress: Float,
    speedMBps: Float
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .border(
                BorderStroke(1.dp, if (isActive) channelColor.copy(alpha = 0.6f) else Color(0x1AFFFFFF)),
                RoundedCornerShape(12.dp)
            ),
        color = if (isActive) channelColor.copy(alpha = 0.12f) else Color(0x1A0E0A1F)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = channelLabel,
                        tint = if (isActive) channelColor else TextTertiary,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = channelLabel,
                        color = if (isActive) channelColor else TextTertiary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                if (isActive) {
                    Text(
                        text = "%.1f MB/s".format(speedMBps),
                        color = channelColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Text(
                        text = "En espera de archivo",
                        color = TextTertiary,
                        fontSize = 11.sp
                    )
                }
            }

            if (isActive) {
                Text(
                    text = fileName.ifBlank { "Transmitiendo flujo cifrado..." },
                    color = TextPrimary,
                    fontSize = 13.sp,
                    maxLines = 1,
                    fontWeight = FontWeight.Medium
                )
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp)),
                    color = channelColor,
                    trackColor = Color(0x33FFFFFF),
                    strokeCap = StrokeCap.Round
                )
            }
        }
    }
}
