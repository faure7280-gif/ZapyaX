package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.TransferManager
import com.example.ui.screens.ActiveTransferScreen
import com.example.ui.screens.HistoryScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.QrScannerScreen
import com.example.ui.screens.ReceiveScreen
import com.example.ui.screens.SendScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.TextTertiary
import com.example.ui.theme.VoidDark
import com.example.ui.theme.ObsidianDark

enum class AppNavScreen {
    HOME,
    SEND,
    RECEIVE,
    SCANNER,
    TRANSFERS,
    HISTORY
}

class MainActivity : ComponentActivity() {

    private lateinit var transferManager: TransferManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        transferManager = TransferManager(this)

        setContent {
            MyApplicationTheme {
                MainAppContent(transferManager = transferManager)
            }
        }
    }
}

@Composable
fun MainAppContent(transferManager: TransferManager) {
    var currentScreen by remember { mutableStateOf(AppNavScreen.HOME) }
    val isTransferActive by transferManager.isTransferActive.collectAsState()

    // Handle back button on sub-screens
    BackHandler(enabled = currentScreen != AppNavScreen.HOME) {
        currentScreen = when (currentScreen) {
            AppNavScreen.SCANNER -> AppNavScreen.SEND
            else -> AppNavScreen.HOME
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = VoidDark,
        contentWindowInsets = WindowInsets.systemBars,
        bottomBar = {
            AcrylicBottomNavBar(
                currentScreen = currentScreen,
                isTransferActive = isTransferActive,
                onScreenSelected = { screen ->
                    currentScreen = screen
                }
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            VoidDark,
                            ObsidianDark,
                            VoidDark
                        )
                    )
                )
        ) {
            when (currentScreen) {
                AppNavScreen.HOME -> {
                    HomeScreen(
                        transferManager = transferManager,
                        onNavigateToSend = { currentScreen = AppNavScreen.SEND },
                        onNavigateToReceive = { currentScreen = AppNavScreen.RECEIVE },
                        onNavigateToTransfers = { currentScreen = AppNavScreen.TRANSFERS }
                    )
                }
                AppNavScreen.SEND -> {
                    SendScreen(
                        transferManager = transferManager,
                        onBack = { currentScreen = AppNavScreen.HOME },
                        onOpenQrScanner = { currentScreen = AppNavScreen.SCANNER },
                        onNavigateToTransfers = { currentScreen = AppNavScreen.TRANSFERS }
                    )
                }
                AppNavScreen.RECEIVE -> {
                    ReceiveScreen(
                        transferManager = transferManager,
                        onBack = { currentScreen = AppNavScreen.HOME },
                        onNavigateToTransfers = { currentScreen = AppNavScreen.TRANSFERS }
                    )
                }
                AppNavScreen.SCANNER -> {
                    QrScannerScreen(
                        transferManager = transferManager,
                        onBack = { currentScreen = AppNavScreen.SEND },
                        onNavigateToTransfers = { currentScreen = AppNavScreen.TRANSFERS }
                    )
                }
                AppNavScreen.TRANSFERS -> {
                    ActiveTransferScreen(
                        transferManager = transferManager,
                        onBack = { currentScreen = AppNavScreen.HOME }
                    )
                }
                AppNavScreen.HISTORY -> {
                    HistoryScreen(
                        transferManager = transferManager
                    )
                }
            }
        }
    }
}

@Composable
fun AcrylicBottomNavBar(
    currentScreen: AppNavScreen,
    isTransferActive: Boolean,
    onScreenSelected: (AppNavScreen) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp))
            .border(
                BorderStroke(
                    1.dp,
                    Brush.linearGradient(
                        listOf(com.example.ui.theme.ZapyaPurple, com.example.ui.theme.ZapyaOrange)
                    )
                ),
                RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp)
            ),
        color = Color(0xF2140D28),
        shadowElevation = 16.dp
    ) {
        NavigationBar(
            containerColor = Color.Transparent,
            tonalElevation = 0.dp,
            modifier = Modifier.height(72.dp)
        ) {
            // Home
            NavigationBarItem(
                selected = currentScreen == AppNavScreen.HOME,
                onClick = { onScreenSelected(AppNavScreen.HOME) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Inicio",
                        modifier = Modifier.size(24.dp)
                    )
                },
                label = { Text("Inicio", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = navigationColors(),
                modifier = Modifier.testTag("nav_home")
            )

            // Send
            NavigationBarItem(
                selected = currentScreen == AppNavScreen.SEND,
                onClick = { onScreenSelected(AppNavScreen.SEND) },
                icon = {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Send,
                        contentDescription = "Enviar",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = { Text("Enviar", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = navigationColors(),
                modifier = Modifier.testTag("nav_send")
            )

            // Receive
            NavigationBarItem(
                selected = currentScreen == AppNavScreen.RECEIVE,
                onClick = { onScreenSelected(AppNavScreen.RECEIVE) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Download,
                        contentDescription = "Recibir",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = { Text("Recibir", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = navigationColors(),
                modifier = Modifier.testTag("nav_receive")
            )

            // Active transfers / monitor
            NavigationBarItem(
                selected = currentScreen == AppNavScreen.TRANSFERS,
                onClick = { onScreenSelected(AppNavScreen.TRANSFERS) },
                icon = {
                    BadgedBox(
                        badge = {
                            if (isTransferActive) {
                                Badge(
                                    containerColor = com.example.ui.theme.ZapyaOrange,
                                    contentColor = Color.Black
                                ) {
                                    Text("2x", fontSize = 9.sp, fontWeight = FontWeight.Black)
                                }
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Sync,
                            contentDescription = "Monitor",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                },
                label = { Text("Monitor", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = navigationColors(),
                modifier = Modifier.testTag("nav_transfers")
            )

            // History
            NavigationBarItem(
                selected = currentScreen == AppNavScreen.HISTORY,
                onClick = { onScreenSelected(AppNavScreen.HISTORY) },
                icon = {
                    Icon(
                        imageVector = Icons.Default.History,
                        contentDescription = "Historial",
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = { Text("Historial", fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = navigationColors(),
                modifier = Modifier.testTag("nav_history")
            )
        }
    }
}

@Composable
private fun navigationColors() = NavigationBarItemDefaults.colors(
    selectedIconColor = Color.Black,
    selectedTextColor = com.example.ui.theme.ZapyaOrange,
    indicatorColor = com.example.ui.theme.ZapyaOrange,
    unselectedIconColor = com.example.ui.theme.TextTertiary,
    unselectedTextColor = com.example.ui.theme.TextTertiary
)
