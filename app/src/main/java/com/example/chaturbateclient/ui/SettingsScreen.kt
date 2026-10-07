package com.example.chaturbateclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaturbateclient.player.VideoQuality
import com.example.chaturbateclient.player.VideoResizeMode

@Composable
fun SettingsScreen(
    autoPlay: Boolean,
    onAutoPlayChange: (Boolean) -> Unit,
    dataSaver: Boolean,
    onDataSaverChange: (Boolean) -> Unit,
    preferredQuality: String,
    onPreferredQualityChange: (String) -> Unit,
    resizeMode: String,
    onResizeModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Playback", color = Color(0xFF9A9A9A))
        Spacer(Modifier.height(18.dp))

        ListItem(
            headlineContent = { Text("Autoplay") },
            supportingContent = { Text("Start a room automatically when playback is ready.") },
            trailingContent = { Switch(checked = autoPlay, onCheckedChange = onAutoPlayChange) }
        )
        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Data saver") },
            supportingContent = { Text("Prefer lower playback quality when enabled.") },
            trailingContent = { Switch(checked = dataSaver, onCheckedChange = onDataSaverChange) }
        )
        HorizontalDivider()

        var showQualityMenu by remember { mutableStateOf(false) }
        ListItem(
            headlineContent = { Text("Preferred quality") },
            supportingContent = { Text("Initial track constraint for playback.") },
            trailingContent = {
                Box {
                    TextButton(onClick = { showQualityMenu = true }) { Text(preferredQuality) }
                    DropdownMenu(
                        expanded = showQualityMenu,
                        onDismissRequest = { showQualityMenu = false }
                    ) {
                        VideoQuality.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.label) },
                                onClick = {
                                    onPreferredQualityChange(item.label)
                                    showQualityMenu = false
                                }
                            )
                        }
                    }
                }
            }
        )
        HorizontalDivider()

        var showResizeMenu by remember { mutableStateOf(false) }
        ListItem(
            headlineContent = { Text("Video resize") },
            supportingContent = { Text("Original keeps the aspect ratio, Zoom fills and crops, Stretch fills and distorts.") },
            trailingContent = {
                Box {
                    TextButton(onClick = { showResizeMenu = true }) { Text(resizeMode) }
                    DropdownMenu(
                        expanded = showResizeMenu,
                        onDismissRequest = { showResizeMenu = false }
                    ) {
                        VideoResizeMode.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.label) },
                                onClick = {
                                    onResizeModeChange(item.label)
                                    showResizeMenu = false
                                }
                            )
                        }
                    }
                }
            }
        )
        HorizontalDivider()

        Spacer(Modifier.height(20.dp))
        Text("OLED theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Pure black is always enabled for OLED displays.", color = Color(0xFF9A9A9A))
        Spacer(Modifier.height(24.dp))
    }
}

