package com.example.chaturbateclient.ui.player

import android.app.Activity
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.AspectRatio
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
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.example.chaturbateclient.data.ChaturbateApi
import com.example.chaturbateclient.player.VideoQuality
import com.example.chaturbateclient.player.VideoResizeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@androidx.annotation.OptIn(UnstableApi::class)
private fun resizeModeFor(mode: VideoResizeMode): Int = when (mode) {
    VideoResizeMode.Original -> AspectRatioFrameLayout.RESIZE_MODE_FIT
    VideoResizeMode.Zoom -> AspectRatioFrameLayout.RESIZE_MODE_ZOOM
    VideoResizeMode.Stretch -> AspectRatioFrameLayout.RESIZE_MODE_FILL
}

// Matches the app's accent so the resize control reads as a distinct, tappable control.
private val PlayerAccent = Color(0xFFD8B4FE)

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    username: String,
    onBack: () -> Unit,
    isFavorite: Boolean,
    onFavorite: () -> Unit,
    autoPlay: Boolean,
    dataSaver: Boolean,
    preferredQuality: String,
    videoResizeMode: String,
    onVideoResizeModeChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val lifecycleOwner = LocalLifecycleOwner.current
    var showQualityMenu by remember { mutableStateOf(false) }
    var showResizeMenu by remember { mutableStateOf(false) }
    var fullscreen by rememberSaveable { mutableStateOf(false) }
    var quality by remember {
        mutableStateOf(VideoQuality.entries.firstOrNull { it.label == preferredQuality } ?: VideoQuality.Auto)
    }
    var source by remember { mutableStateOf<String?>(null) }
    var status by remember { mutableStateOf("Resolving stream…") }
    var playbackError by remember { mutableStateOf<String?>(null) }

    val trackSelector = remember(context) { DefaultTrackSelector(context) }
    val resize = VideoResizeMode.fromLabel(videoResizeMode)
    val player = remember(context) {
        ExoPlayer.Builder(context).setTrackSelector(trackSelector).build()
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
                result.roomStatus != "public" -> status = "Room is " + result.roomStatus
                result.hlsUrl.isNullOrBlank() -> status = "No HLS source returned"
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

    // Quality changes only adjust track constraints; they no longer restart playback.
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
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_STOP -> player.pause()
                Lifecycle.Event.ON_START -> if (autoPlay && source != null) player.play()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    DisposableEffect(fullscreen, activity) {
        val window = activity?.window ?: return@DisposableEffect onDispose { }
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        if (fullscreen) {
            controller.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            controller.hide(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
            controller.show(WindowInsetsCompat.Type.systemBars())
            // Exiting fullscreen returns to portrait; the inline player stays portrait.
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
        }
        onDispose {
            controller.show(WindowInsetsCompat.Type.systemBars())
            activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler(enabled = fullscreen) { fullscreen = false }

    if (fullscreen) {
        AndroidView(
            factory = {
                PlayerView(it).apply {
                    useController = true
                    controllerAutoShow = true
                    controllerHideOnTouch = true
                    this.player = player
                    resizeMode = resizeModeFor(resize)
                    setShutterBackgroundColor(android.graphics.Color.BLACK)
                    // Show the library's own fullscreen toggle as an exit control,
                    // so there is a visible way back to portrait.
                    setFullscreenButtonState(true)
                    setFullscreenButtonClickListener { isFullscreen ->
                        if (!isFullscreen) fullscreen = false
                    }
                }
            },
            update = {
                it.resizeMode = resizeModeFor(resize)
                it.setFullscreenButtonState(true)
            },
            modifier = modifier.fillMaxSize().background(Color.Black)
        )
        return
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
                        controllerAutoShow = true
                        controllerHideOnTouch = true
                        this.player = player
                        resizeMode = resizeModeFor(resize)
                        setShutterBackgroundColor(android.graphics.Color.BLACK)
                    }
                },
                update = { it.resizeMode = resizeModeFor(resize) },
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

        // The player chrome stays dark in every app theme (the video surface is always black),
        // so pin the content color light. Without this, light themes leave the default dark
        // icons and title on the black bar, making them invisible though still tappable.
        Surface(color = Color.Black, contentColor = Color(0xFFF5F5F5)) {
            Column(Modifier.padding(horizontal = 14.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back") }
                    Column(Modifier.weight(1f)) {
                        Text(username, style = MaterialTheme.typography.titleLarge)
                        Text(status, style = MaterialTheme.typography.bodySmall, color = Color(0xFF8E8E8E))
                    }
                    IconButton(onClick = onFavorite) {
                        Icon(if (isFavorite) Icons.Outlined.Favorite else Icons.Outlined.FavoriteBorder, contentDescription = "Favorite")
                    }
                    Box {
                        IconButton(onClick = { showQualityMenu = true }) { Icon(Icons.Outlined.Settings, contentDescription = "Quality") }
                        DropdownMenu(expanded = showQualityMenu, onDismissRequest = { showQualityMenu = false }) {
                            VideoQuality.entries.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.label + if (dataSaver && item.maxHeight != null && item.maxHeight > 480) " (capped)" else "") },
                                    onClick = { quality = item; showQualityMenu = false }
                                )
                            }
                        }
                    }
                    Box {
                        // Accent-tinted so it reads as a distinct control, not another row in the gear menu.
                        IconButton(
                            onClick = { showResizeMenu = true },
                            modifier = Modifier.background(PlayerAccent.copy(alpha = 0.18f), CircleShape)
                        ) {
                            Icon(
                                Icons.Outlined.AspectRatio,
                                contentDescription = "Video resize",
                                tint = PlayerAccent
                            )
                        }
                        DropdownMenu(
                            expanded = showResizeMenu,
                            onDismissRequest = { showResizeMenu = false }
                        ) {
                            VideoResizeMode.entries.forEach { item ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            item.label + if (item.label == resize.label) "  ✓" else "",
                                            color = if (item.label == resize.label) PlayerAccent else Color.Unspecified
                                        )
                                    },
                                    onClick = {
                                        onVideoResizeModeChange(item.label)
                                        showResizeMenu = false
                                    }
                                )
                            }
                        }
                    }
                    IconButton(onClick = { fullscreen = true }) {
                        Icon(Icons.Outlined.Fullscreen, contentDescription = "Fullscreen")
                    }
                }
                playbackError?.let { Text(it, color = Color(0xFFFF8A80), style = MaterialTheme.typography.bodySmall) }
            }
        }
    }
}
