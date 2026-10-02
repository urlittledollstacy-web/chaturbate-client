package com.example.chaturbateclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
    onShowDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().background(Color.Black).verticalScroll(rememberScrollState()).padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Settings", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Playback", color = Color(0xFF9A9A9A))
        Spacer(Modifier.height(18.dp))

        ListItem(
            headlineContent = { Text("Autoplay") },
            supportingContent = { Text("Start playback automatically when a source is ready.") },
            trailingContent = { Switch(checked = autoPlay, onCheckedChange = onAutoPlayChange) }
        )
        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Data saver") },
            supportingContent = { Text("Cap the selected video track to reduce bandwidth.") },
            trailingContent = { Switch(checked = dataSaver, onCheckedChange = onDataSaverChange) }
        )
        HorizontalDivider()

        ListItem(
            headlineContent = { Text("Preferred quality") },
            supportingContent = { Text("Used as the initial track constraint when available.") },
            trailingContent = {
                TextButton(onClick = {
                    val current = VideoQuality.entries.firstOrNull { it.label == preferredQuality } ?: VideoQuality.Auto
                    val next = VideoQuality.entries[(current.ordinal + 1) % VideoQuality.entries.size]
                    onPreferredQualityChange(next.label)
                }) {
                    Text(preferredQuality)
                }
            }
        )
        HorizontalDivider()

        Spacer(Modifier.height(20.dp))
        Text("Diagnostics", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Text("Diagnostics are opt-in and sanitized; credentials, cookies and signed URLs are excluded.", color = Color(0xFF9A9A9A))
        TextButton(onClick = onShowDiagnostics) { Text("View network diagnostics") }

        Spacer(Modifier.height(20.dp))
        Text("OLED theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Pure black is always enabled for OLED displays.", color = Color(0xFF9A9A9A))
        Spacer(Modifier.height(24.dp))
    }
}
