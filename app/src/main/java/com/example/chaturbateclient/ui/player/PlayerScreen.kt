package com.example.chaturbateclient.ui.player

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.chaturbateclient.player.VideoQuality

@Composable
fun PlayerScreen(
    username: String,
    onBack: () -> Unit,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showQualityMenu by remember { mutableStateOf(false) }
    var quality by remember { mutableStateOf(VideoQuality.Auto) }

    Column(
        modifier = modifier.fillMaxSize().background(Color.Black)
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Playback source pending",
                color = Color(0xFF8E8E8E),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Surface(color = Color.Black, tonalElevation = 0.dp) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = "Back")
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(username, style = MaterialTheme.typography.titleLarge)
                        Text("Live room", style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E8E8E))
                    }

                    IconButton(onClick = onFavorite) {
                        Icon(
                            if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = "Favorite"
                        )
                    }
                    Box {
                        IconButton(onClick = { showQualityMenu = true }) {
                            Icon(Icons.Outlined.Settings, contentDescription = "Quality")
                        }
                        DropdownMenu(
                            expanded = showQualityMenu,
                            onDismissRequest = { showQualityMenu = false }
                        ) {
                            VideoQuality.entries.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.label) },
                                    onClick = {
                                        quality = item
                                        showQualityMenu = false
                                    },
                                    trailingIcon = if (quality == item) {
                                        { Text("✓") }
                                    } else null
                                )
                            }
                        }
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Fullscreen, contentDescription = "Fullscreen")
                    }
                }
            }
        }
    }
}
