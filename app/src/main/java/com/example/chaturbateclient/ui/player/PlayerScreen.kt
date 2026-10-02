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
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Fullscreen
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun PlayerScreen(
    username: String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "Player source pending",
                color = Color(0xFF8E8E8E),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Surface(
            color = Color.Black,
            tonalElevation = 0.dp
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = username,
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Live room",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFF8E8E8E)
                        )
                    }

                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.FavoriteBorder, "Favorite")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Settings, "Playback settings")
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Fullscreen, "Fullscreen")
                    }
                }
            }
        }
    }
}
