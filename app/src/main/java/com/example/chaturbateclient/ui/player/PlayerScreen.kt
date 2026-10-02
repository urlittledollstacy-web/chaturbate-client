package com.example.chaturbateclient.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import android.view.View
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.outlined.FullscreenExit
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.PlayerView
import com.example.chaturbateclient.data.ChaturbateApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun PlayerScreen(
    username: String,
    onBack: () -> Unit,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    autoPlay: Boolean,
    dataSaver: Boolean,
    preferredQuality: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var showQualityMenu by remember { mutableStateOf(false) }
    var fullscreen by remember { mutableStateOf(false) }
    var quality by remember {
        mutableStateOf(VideoQuality.entries.firstOrNull { it.label == preferredQuality } ?: VideoQuality.Auto)
    }
    var source by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Resolving stream…") }
    var playbackError by remember { mutableStateOf<String?>(null) }

    val trackSelector = remember(context) { DefaultTrackSelector(context) }
    val player = remember(context) {
        ExoPlayer.Builder(context)
            .setTrackSelector(trackSelector)
            .build()
    }

    fun applyQuality() {
        val selected = if (dataSaver) {
            when (quality) {
                VideoQuality.Auto, VideoQuality.P240, VideoQuality.P360, VideoQuality.P480 -> quality
                else -> VideoQuality.P480
            }
        } else quality
        trackSelector.setParameters(
            trackSelector.buildUponParameters().setMaxVideoSize(
                Int.MAX_VALUE,
                selected.maxHeight ?: Int.MAX_VALUE
            )
        )
    }

    LaunchedEffect(username) {
        source = null
        playbackError = null
        status = "Resolving stream…"
        runCatching {
            withContext(Dispatchers.IO) { ChaturbateApi.fetchPlaybackSource(username) }
        }.onSuccess { result ->
            when {
                result.roomStatus.equals("offline", true) -> status = "Room is offline"
                result.hlsUrl.isNullOrBlank() -> status = "No playable stream source returned"
                else -> {
                    source = result.hlsUrl
                    status = "Preparing playback…"
                }
            }
        }.onFailure {
            playbackError = it.message ?: "Unable to resolve stream."
            status = "Playback unavailable"
        }
    }

    LaunchedEffect(source) {
        val url = source ?: return@LaunchedEffect
        applyQuality()
        playbackError = null
        player.setMediaItem(MediaItem.fromUri(url))
        player.prepare()
        player.playWhenReady = autoPlay
    }

    LaunchedEffect(quality, dataSaver) { applyQuality() }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                status = when (playbackState) {
                    Player.STATE_BUFFERING -> "Buffering…"
                    Player.STATE_READY -> if (player.isPlaying) "Playing" else "Ready"
                    Player.STATE_ENDED -> "Stream ended"
                    else -> status
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                playbackError = error.errorCodeName
                status = "Playback error"
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    DisposableEffect(lifecycleOwner, player, autoPlay) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            when (event) {
                androidx.lifecycle.Lifecycle.Event.ON_STOP -> player.pause()
                androidx.lifecycle.Lifecycle.Event.ON_START -> if (autoPlay && source != null) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(fullscreen, activity) {
        val window = activity?.window ?: return@DisposableEffect onDispose { }
        if (fullscreen) {
            WindowCompat.getInsetsController(window, window.decorView).hide(WindowInsetsCompat.Type.systemBars())
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN or
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
        onDispose {
            WindowCompat.getInsetsController(window, window.decorView).show(WindowInsetsCompat.Type.systemBars())
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_VISIBLE
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler(enabled = fullscreen) { fullscreen = false }

    if (fullscreen) {
        AndroidView(
            factory = { PlayerView(it).apply { useController = true; this.player = player } },
            modifier = modifier.fillMaxSize().background(Color.Black)
        )
        return
    }

    Column(modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.fillMaxWidth().weight(1f).background(Color.Black), contentAlignment = Alignment.Center) {
            AndroidView(
                factory = {
                    PlayerView(it).apply {
                        useController = true
                        controllerAutoShow = true
                        controllerHideOnTouch = true
                        this.player = player
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
            if (source == null) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (playbackError == null && status.contains("Resolving")) CircularProgressIndicator()
                    Text(status, color = Color(0xFFBDBDBD), modifier = Modifier.padding(top = 12.dp))
                    playbackError?.let { Text(it, color = Color(0xFFFF8A80), modifier = Modifier.padding(top = 8.dp)) }
                }
            }
        }

        Surface(color = Color.Black) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.Outlined.ArrowBack, "Back") }
                    Column(Modifier.weight(1f)) {
                        Text(username, style = MaterialTheme.typography.titleLarge)
                        Text(status, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E8E8E))
                    }
                    IconButton(onClick = onFavorite) {
                        Icon(if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, "Favorite")
                    }
                    Box {
                        IconButton(onClick = { showQualityMenu = true }) { Icon(Icons.Outlined.Settings, "Quality") }
                        DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }) {
                            VideoQuality.entries.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.label + if (dataSaver && item.maxHeight != null && item.maxHeight > 480) " (capped)" else "") },
                                    onClick = { quality = item; showQualityMenu = false }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { fullscreen = true }) { Icon(Icons.Outlined.Fullscreen, "Fullscreen") }
                }
                playbackError?.let { Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
