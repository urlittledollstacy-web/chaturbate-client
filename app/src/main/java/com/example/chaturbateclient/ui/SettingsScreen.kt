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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaturbateclient.player.VideoQuality

@Composable
fun SettingsScreen(
    autoPlay: Boolean,
    onAutoPlayChange: (Boolean) -> Unit,
    dataSaver: Boolean,
    onDataSaverChange: (Boolean) -> Unit,
    preferredQuality: String,
    onPreferredQualityChange: (String) -> Unit,
    theme: String,
    onThemeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalAppColors.current
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold, color = colors.textPrimary)
        Spacer(Modifier.height(6.dp))
        Text("Playback", color = colors.textSecondary)
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

        Spacer(Modifier.height(20.dp))
        Text("Appearance", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold, color = colors.textPrimary)
        Spacer(Modifier.height(8.dp))

        var showThemeMenu by remember { mutableStateOf(false) }
        ListItem(
            headlineContent = { Text("Theme") },
            supportingContent = { Text("OLED Black is pure black for OLED displays.") },
            trailingContent = {
                Box {
                    TextButton(onClick = { showThemeMenu = true }) { Text(theme) }
                    DropdownMenu(
                        expanded = showThemeMenu,
                        onDismissRequest = { showThemeMenu = false }
                    ) {
                        AppTheme.entries.forEach { item ->
                            DropdownMenuItem(
                                text = { Text(item.label) },
                                onClick = {
                                    onThemeChange(item.label)
                                    showThemeMenu = false
                                }
                            )
                        }
                    }
                }
            }
        )
        HorizontalDivider()
        Spacer(Modifier.height(24.dp))
    }
}

