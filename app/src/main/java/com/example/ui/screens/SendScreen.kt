package com.example.ui.screens

import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.VideoLibrary
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransferManager
import com.example.model.FileCategory
import com.example.model.TransferFile
import com.example.ui.components.AcrylicCard
import com.example.ui.theme.EmeraldShield
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.ZapyaGradient
import com.example.ui.theme.ZapyaOrange
import com.example.ui.theme.ZapyaOrangeGlow
import com.example.ui.theme.ZapyaPurple
import java.util.UUID

@Composable
fun SendScreen(
    transferManager: TransferManager,
    onBack: () -> Unit,
    onOpenQrScanner: () -> Unit,
    onNavigateToTransfers: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val selectedFiles by transferManager.selectedFiles.collectAsState()
    val deviceMediaFiles by transferManager.deviceMediaFiles.collectAsState()
    val discoveredPeers by transferManager.peerDiscoveryManager.discoveredPeers.collectAsState()

    var selectedCategory by remember { mutableStateOf(FileCategory.ALL) }

    // Start listening for nearby ZapyaX devices on the local network
    DisposableEffect(Unit) {
        transferManager.peerDiscoveryManager.startListening()
        onDispose {
            transferManager.peerDiscoveryManager.stopListening()
        }
    }

    // System file picker for selecting any real document, video, apk, or archive
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris: List<Uri> ->
        for (uri in uris) {
            var fileName = "archivo_${UUID.randomUUID().toString().take(6)}"
            var fileSize = 1024L
            val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"

            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) fileName = cursor.getString(nameIndex) ?: fileName
                    if (sizeIndex != -1) fileSize = cursor.getLong(sizeIndex).coerceAtLeast(1024L)
                }
            }

            val category = when {
                mime.startsWith("image/") -> FileCategory.PHOTOS
                mime.startsWith("video/") -> FileCategory.VIDEOS
                mime.startsWith("audio/") -> FileCategory.AUDIO
                mime.contains("zip") || mime.contains("rar") || mime.contains("tar") -> FileCategory.ARCHIVES
                else -> FileCategory.DOCUMENTS
            }

            transferManager.addSelectedFile(
                TransferFile(
                    id = UUID.randomUUID().toString(),
                    name = fileName,
                    sizeBytes = fileSize,
                    mimeType = mime,
                    uriString = uri.toString(),
                    category = category,
                    isSimulated = false
                )
            )
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
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
                    modifier = Modifier.testTag("send_back_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Atrás",
                        tint = TextPrimary
                    )
                }
                Column {
                    Text(
                        text = "Enviar Archivos",
                        color = TextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Selección real para envío dual 2x2",
                        color = ZapyaOrange,
                        fontSize = 11.sp
                    )
                }
            }

            Button(
                onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0x33A855F7)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, ZapyaPurple),
                modifier = Modifier.testTag("pick_system_files_button")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Explorar",
                        tint = ZapyaOrangeGlow,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Explorador",
                        color = ZapyaOrangeGlow,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Categories filter tabs
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(FileCategory.values()) { cat ->
                FilterChip(
                    selected = selectedCategory == cat,
                    onClick = {
                        selectedCategory = cat
                        transferManager.loadDeviceMedia(cat)
                    },
                    label = {
                        Text(
                            text = cat.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (selectedCategory == cat) FontWeight.Bold else FontWeight.Normal
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = ZapyaOrange,
                        selectedLabelColor = Color.Black,
                        containerColor = SurfaceDark,
                        labelColor = TextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = selectedCategory == cat,
                        borderColor = if (selectedCategory == cat) ZapyaOrange else Color(0x26FFFFFF)
                    )
                )
            }
        }

        // Nearby Discovered Peers Bar (if any discovered on Wi-Fi/Hotspot)
        if (discoveredPeers.isNotEmpty()) {
            AcrylicCard(
                modifier = Modifier.fillMaxWidth(),
                borderColor = EmeraldShield
            ) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Wifi,
                            contentDescription = null,
                            tint = EmeraldShield,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "RECEPTORES DETECTADOS EN RED LOCAL",
                            color = EmeraldShield,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    discoveredPeers.forEach { peer ->
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    if (selectedFiles.isNotEmpty()) {
                                        transferManager.startRealTransferToPeer(peer)
                                        onNavigateToTransfers()
                                    }
                                },
                            color = Color(0x3310B981)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = peer.deviceName,
                                    color = TextPrimary,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Conectar (${peer.ipAddress})",
                                    color = EmeraldShield,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }
            }
        }

        // Files List / Selection
        LazyColumn(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Selected files banner
            if (selectedFiles.isNotEmpty()) {
                item {
                    AcrylicCard(
                        modifier = Modifier.fillMaxWidth(),
                        borderColor = ZapyaOrange
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
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = ZapyaOrange,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "${selectedFiles.size} archivos seleccionados",
                                        color = TextPrimary,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Limpiar",
                                    color = Color(0xFFFF4081),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.clickable { transferManager.clearSelectedFiles() }
                                )
                            }
                            Text(
                                text = "Transmisión concurrente de 2 en 2 con cifrado AES-256-GCM directo.",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            item {
                Text(
                    text = "ARCHIVOS EN EL DISPOSITIVO",
                    color = TextTertiary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            if (deviceMediaFiles.isEmpty() && selectedFiles.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { filePickerLauncher.launch(arrayOf("*/*")) },
                        color = Color(0x1AFFFFFF),
                        border = BorderStroke(1.dp, Color(0x26FFFFFF))
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = ZapyaOrangeGlow,
                                modifier = Modifier.size(36.dp)
                            )
                            Text(
                                text = "Toca para abrir el explorador y seleccionar archivos",
                                color = TextPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Puedes elegir fotos, videos, documentos o cualquier formato",
                                color = TextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }

            // Render device files
            items(deviceMediaFiles) { file ->
                val isSelected = selectedFiles.any { it.id == file.id }
                RealFileItemRow(
                    file = file,
                    isSelected = isSelected,
                    onToggle = {
                        if (isSelected) {
                            transferManager.removeSelectedFile(file.id)
                        } else {
                            transferManager.addSelectedFile(file)
                        }
                    }
                )
            }
        }

        // Bottom Action Bar: QR Scanner & Send
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0x33A855F7),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = null,
                        tint = ZapyaOrange,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = "Modo Dual 2x2 activo: 2 archivos transmitiéndose simultáneamente",
                        color = ZapyaOrangeGlow,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onOpenQrScanner,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("scan_qr_to_send_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (selectedFiles.isNotEmpty()) ZapyaOrange else Color(0xFF261545)
                    ),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, ZapyaPurple)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.QrCodeScanner,
                            contentDescription = "Escanear QR del receptor",
                            tint = if (selectedFiles.isNotEmpty()) Color.Black else ZapyaOrange,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = if (selectedFiles.isNotEmpty()) "Escanear QR y Enviar (${selectedFiles.size})" else "Escanear QR de Receptor",
                            color = if (selectedFiles.isNotEmpty()) Color.Black else TextPrimary,
                            fontWeight = FontWeight.Black,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RealFileItemRow(
    file: TransferFile,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val categoryIcon: ImageVector = when (file.category) {
        FileCategory.PHOTOS -> Icons.Default.Image
        FileCategory.VIDEOS -> Icons.Default.VideoLibrary
        FileCategory.AUDIO -> Icons.Default.AudioFile
        FileCategory.DOCUMENTS -> Icons.Default.Description
        FileCategory.ARCHIVES -> Icons.Default.FolderZip
        FileCategory.ALL -> Icons.Default.Description
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onToggle() }
            .border(
                BorderStroke(
                    1.dp,
                    if (isSelected) ZapyaOrange else Color(0x1AFFFFFF)
                ),
                RoundedCornerShape(14.dp)
            )
            .testTag("file_item_${file.id}"),
        color = if (isSelected) Color(0x33451A00) else SurfaceDark
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(
                        if (isSelected) ZapyaOrange else Color(0x26FFFFFF),
                        RoundedCornerShape(10.dp)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon,
                    contentDescription = null,
                    tint = if (isSelected) Color.Black else TextPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = file.name,
                    color = TextPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = file.formattedSize,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Text(
                        text = "•",
                        color = TextTertiary,
                        fontSize = 10.sp
                    )
                    Text(
                        text = "Cifrado AES-256",
                        color = EmeraldShield,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(
                        if (isSelected) ZapyaOrange else Color.Transparent,
                        CircleShape
                    )
                    .border(
                        BorderStroke(
                            1.5.dp,
                            if (isSelected) ZapyaOrange else Color(0x66FFFFFF)
                        ),
                        CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Seleccionado",
                        tint = Color.Black,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}
