package com.example.chaturbateclient.ui.player

import android.net.Uri
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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.example.chaturbateclient.data.ChaturbateApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private enum class VideoQuality(val label: String, val maxHeight: Int?) {
    Auto("Auto", null),
    P360("360p", 360),
    P480("480p", 480),
    P720("720p", 720),
    P1080("1080p", 1080)
}

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
    var source by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Resolving stream…") }
    var playbackError by remember { mutableStateOf<String?>(null) }

    val context = LocalContext.current
    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            setParameters(buildUponParameters().setForceHighestSupportedBitrate(false))
        }
    }
    val player = remember {
        ExoPlayer.Builder(context).setTrackSelector(trackSelector).build()
    }

    LaunchedEffect(username) {
        source = null
        playbackError = null
        status = "Resolving stream…"
        runCatching {
            withContext(Dispatchers.IO) { ChaturbateApi.fetchPlaybackSource(username) }
        }.onSuccess { result ->
            when {
                result.roomStatus != "public" -> status = "Room is " + result.roomStatus
                result.hlsUrl.isNullOrBlank() -> status = "No HLS source returned"
                else -> {
                    source = result.hlsUrl
                    status = "Loading…"
                }
            }
        }.onFailure {
            playbackError = it.message ?: "Unable to resolve stream"
            status = "Playback unavailable"
        }
    }

    LaunchedEffect(source, quality) {
        val url = source ?: return@LaunchedEffect
        trackSelector.setParameters(
            trackSelector.buildUponParameters().setMaxVideoSize(
                Int.MAX_VALUE,
                quality.maxHeight ?: Int.MAX_VALUE
            )
        )
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        player.prepare()
        player.playWhenReady = true
    }

    DisposableEffect(Unit) {
        onDispose { player.release() }
    }

    Column(modifier = modifier.fillMaxSize().background(Color.Black)) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(1f).background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        useController = true
                        this.player = player
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            if (source == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (playbackError == null) CircularProgressIndicator()
                    Text(
                        text = playbackError ?: status,
                        color = Color(0xFFBDBDBD),
                        modifier = Modifier.padding(top = 12.dp)
                    )
                }
            }
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
                        Text(status, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E8E8E))
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
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Outlined.Fullscreen, contentDescription = "Fullscreen")
                    }
                }
                playbackError?.let {
                    Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}
