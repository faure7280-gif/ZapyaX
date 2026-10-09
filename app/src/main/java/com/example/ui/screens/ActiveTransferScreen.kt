package com.example.ui.screens

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransferManager
import com.example.model.TransferDirection
import com.example.model.TransferFile
import com.example.model.TransferStatus
import com.example.ui.components.AcrylicCard
import com.example.ui.components.DualChannelActiveBanner
import com.example.ui.components.E2eeSecurityPill
import com.example.ui.components.SecurityAuditDialog
import com.example.ui.theme.ChannelOneColor
import com.example.ui.theme.ChannelTwoColor
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.NeonCoral
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ActiveTransferScreen(
    transferManager: TransferManager,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val activeTransfers by transferManager.activeTransfers.collectAsState()
    val dualStats by transferManager.dualChannelStats.collectAsState()
    val isTransferActive by transferManager.isTransferActive.collectAsState()
    val overallProgress by transferManager.overallProgress.collectAsState()
    val overallSpeed by transferManager.overallSpeedMBps.collectAsState()
    val direction by transferManager.transferDirection.collectAsState()
    val fingerprint by transferManager.e2eeFingerprint.collectAsState()
    val peerInfo by transferManager.activePeer.collectAsState()

    var showSecurityAudit by remember { mutableStateOf(false) }

    if (showSecurityAudit) {
        SecurityAuditDialog(
            fingerprint = fingerprint,
            onDismiss = { showSecurityAudit = false }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Screen Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.testTag("transfer_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = if (direction == TransferDirection.SENDING) "Transmitiendo Archivos" else "Recibiendo Archivos",
                        color = TextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Peer: ${peerInfo?.deviceName ?: "Dispositivo Conectado"}",
                        color = com.example.ui.theme.ZapyaOrange,
                        fontSize = 12.sp
                    )
                }
            }

            E2eeSecurityPill(
                fingerprint = fingerprint.take(11),
                onClick = { showSecurityAudit = true }
            )
        }

        // Live Dual-Stream Monitor (Visualizing Worker 1 & Worker 2 in parallel)
        DualChannelActiveBanner(stats = dualStats)

        // Session Overview Cockpit
        AcrylicCard(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
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
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = "Segundo plano",
                            tint = EmeraldShield,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Segundo plano activo (WakeLock)",
                            color = EmeraldShield,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "${(overallProgress * 100).toInt()}%",
                        color = TextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black
                    )
                }

                LinearProgressIndicator(
                    progress = { overallProgress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = com.example.ui.theme.ZapyaOrange,
                    trackColor = Color(0x33FFFFFF),
                    strokeCap = StrokeCap.Round
                )
            }
        }

        // Files List Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "COLA DE TRANSMISIÓN (${activeTransfers.size})",
                color = TextTertiary,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )

            if (isTransferActive) {
                Text(
                    text = "Cancelar",
                    color = NeonCoral,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable { transferManager.cancelActiveTransfer() }
                        .testTag("cancel_transfer_button")
                )
            }
        }

        // Files List
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(activeTransfers) { file ->
                ActiveFileTransferCard(file = file)
            }
        }

        // Return Home or Finish button
        if (!isTransferActive && activeTransfers.isNotEmpty()) {
            Button(
                onClick = onBack,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("finish_transfers_button"),
                colors = ButtonDefaults.buttonColors(containerColor = com.example.ui.theme.ZapyaOrange),
                shape = RoundedCornerShape(12.dp)
            ) {
                Text(
                    text = "Transferencia finalizada - Volver al inicio",
                    color = Color.Black,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun ActiveFileTransferCard(file: TransferFile) {
    val isStreaming = file.status == TransferStatus.STREAMING
    val isCompleted = file.status == TransferStatus.COMPLETED

    val channelColor = when (file.channelId) {
        1 -> ChannelOneColor
        2 -> ChannelTwoColor
        else -> TextTertiary
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(
                BorderStroke(
                    1.dp,
                    if (isStreaming) channelColor else if (isCompleted) Color(0x3310B981) else Color(0x1AFFFFFF)
                ),
                RoundedCornerShape(14.dp)
            ),
        color = if (isStreaming) Color(0x2E0B1A2F) else SurfaceDark
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Status / Channel tag
                    if (file.channelId != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = channelColor.copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "CH ${file.channelId}",
                                color = channelColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    Text(
                        text = file.name,
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1
                    )
                }

                // Status or speed
                when {
                    isCompleted -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CheckCircle,
                                contentDescription = "Completado",
                                tint = EmeraldShield,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = "Listo",
                                color = EmeraldShield,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    isStreaming -> {
                        Text(
                            text = "%.1f MB/s".format(file.speedMBps),
                            color = channelColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    else -> {
                        Text(
                            text = file.status.label,
                            color = TextTertiary,
                            fontSize = 11.sp
                        )
                    }
                }
            }

            // Progress bar
            LinearProgressIndicator(
                progress = { file.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = if (isCompleted) EmeraldShield else channelColor,
                trackColor = Color(0x26FFFFFF),
                strokeCap = StrokeCap.Round
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = file.formattedSize,
                    color = TextSecondary,
                    fontSize = 11.sp
                )
                Text(
                    text = "${(file.progress * 100).toInt()}%",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }
    }
}
