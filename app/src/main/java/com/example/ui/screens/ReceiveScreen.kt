package com.example.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.widget.Toast
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransferManager
import com.example.qr.QrCodeHelper
import com.example.ui.components.AcrylicCard
import com.example.ui.components.SecurityAuditDialog
import com.example.ui.theme.CyberViolet
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary

@Composable
fun ReceiveScreen(
    transferManager: TransferManager,
    onBack: () -> Unit,
    onNavigateToTransfers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val connectionInfo by transferManager.myConnectionInfo.collectAsState()
    val isListening by transferManager.isServerListening.collectAsState()
    val isTransferActive by transferManager.isTransferActive.collectAsState()

    var qrBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var showSecurityAudit by remember { mutableStateOf(false) }

    // Start server when screen is active
    DisposableEffect(Unit) {
        transferManager.startReceiverServer()
        onDispose {
            // Keep listening if user stays or can stop
        }
    }

    LaunchedEffect(connectionInfo) {
        connectionInfo?.let { info ->
            val payload = info.toQrPayload()
            qrBitmap = QrCodeHelper.generateQrBitmap(
                content = payload,
                width = 512,
                height = 512,
                foregroundRgb = android.graphics.Color.BLACK,
                backgroundRgb = android.graphics.Color.WHITE
            )
        }
    }

    // Auto-navigate to active transfers when a transfer begins
    LaunchedEffect(isTransferActive) {
        if (isTransferActive) {
            onNavigateToTransfers()
        }
    }

    if (showSecurityAudit) {
        SecurityAuditDialog(
            fingerprint = connectionInfo?.fingerprint ?: "",
            onDismiss = { showSecurityAudit = false }
        )
    }

    // Radar pulse animation
    val infiniteTransition = rememberInfiniteTransition(label = "radar")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Top Header
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
                    modifier = Modifier.testTag("receive_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = "Recibir Archivos",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Escanea este código desde el emisor",
                        color = com.example.ui.theme.ZapyaOrange,
                        fontSize = 11.sp
                    )
                }
            }

            IconButton(
                onClick = { transferManager.refreshMyConnectionInfo() },
                modifier = Modifier.testTag("refresh_keys_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Regenerar clave",
                    tint = com.example.ui.theme.ZapyaOrange
                )
            }
        }

        // QR Code Display Card
        Box(
            modifier = Modifier
                .scale(pulseScale)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .size(260.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .border(
                        BorderStroke(
                            2.5.dp,
                            Brush.linearGradient(
                                listOf(com.example.ui.theme.ZapyaPurple, com.example.ui.theme.ZapyaOrange)
                            )
                        ),
                        RoundedCornerShape(24.dp)
                    ),
                color = Color.White,
                shadowElevation = 16.dp
            ) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    qrBitmap?.let { bmp ->
                        Image(
                            bitmap = bmp.asImageBitmap(),
                            contentDescription = "Código QR de Emparejamiento ZapyaX",
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(14.dp)
                                .testTag("receiver_qr_image")
                        )
                    }
                }
            }
        }

        // Radar status indicator
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(EmeraldShield, CircleShape)
            )
            Text(
                text = "Servidor activo esperando conexión dual",
                color = EmeraldShield,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Connection & E2EE Info Card
        AcrylicCard(
            modifier = Modifier.fillMaxWidth(),
            borderColor = Color(0x3300F0FF)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "DATOS DE CONEXIÓN P2P",
                    color = com.example.ui.theme.ZapyaOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Dispositivo:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = connectionInfo?.deviceName ?: "Android Local",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Dirección IP:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "${connectionInfo?.ipAddress ?: "192.168.1.1"}:${connectionInfo?.port ?: 8989}",
                        color = TextPrimary,
                        fontSize = 13.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Huella Criptográfica:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = connectionInfo?.fingerprint?.take(11) ?: "VALIDADO",
                        color = EmeraldShield,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Cifrado:",
                        color = TextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        text = "AES-256-GCM (Sin Servidor)",
                        color = EmeraldShield,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Action Buttons: Copy link & Test incoming transfer
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = {
                    val clip = ClipData.newPlainText("ZapyaX QR", connectionInfo?.toQrPayload() ?: "")
                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    cm.setPrimaryClip(clip)
                    Toast.makeText(context, "Enlace ZapyaX copiado al portapapeles", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("copy_payload_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF162544)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0x3300F0FF))
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copiar",
                        tint = com.example.ui.theme.ZapyaOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Copiar Enlace",
                        color = TextPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Button(
                onClick = {
                    val downloadsFolder = com.example.data.DeviceStorageHelper.getReceivedDirectory(context)
                    Toast.makeText(context, "Descargas guardadas en: ${downloadsFolder.name}", Toast.LENGTH_LONG).show()
                },
                modifier = Modifier
                    .weight(1f)
                    .height(48.dp)
                    .testTag("received_files_directory_button"),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0C2B22)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, EmeraldShield)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Descargas",
                        tint = EmeraldShield,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Carpeta ZapyaX",
                        color = EmeraldShield,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
