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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen(
    autoPlay: Boolean,
    onAutoPlayChange: (Boolean) -> Unit,
    dataSaver: Boolean,
    onDataSaverChange: (Boolean) -> Unit,
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

        Spacer(Modifier.height(20.dp))
        Text("OLED theme", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(4.dp))
        Text("Pure black is always enabled for OLED displays.", color = Color(0xFF9A9A9A))
        Spacer(Modifier.height(24.dp))
    }
}
