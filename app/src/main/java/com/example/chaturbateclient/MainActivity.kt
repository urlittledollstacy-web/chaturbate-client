package com.example.chaturbateclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import com.example.chaturbateclient.data.ApiRoom
import com.example.chaturbateclient.data.AppPreferences
import com.example.chaturbateclient.data.ChaturbateApi
import com.example.chaturbateclient.data.JsonRoomCacheStore
import com.example.chaturbateclient.data.filterRooms
import com.example.chaturbateclient.data.normalizedImageUrl
import com.example.chaturbateclient.ui.SettingsScreen
import com.example.chaturbateclient.ui.player.PlayerScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

private val OledBlack = Color.Black
private val OledCard = Color(0xFF0A0A0A)
private val OledSurface = Color(0xFF080808)
private val TextPrimary = Color(0xFFF5F5F5)
private val TextSecondary = Color(0xFF9A9A9A)
private val Accent = Color(0xFFD8B4FE)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ClientTheme { ClientApp() } }
    }
}

@Composable
private fun ClientTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = androidx.compose.material3.darkColorScheme(
            background = OledBlack,
            surface = OledSurface,
            onBackground = TextPrimary,
            onSurface = TextPrimary
        ),
        content = content
    )
}

@Composable
private fun ClientApp() {
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedRoom by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val preferences = remember(context) { AppPreferences(context) }
    val cacheStore = remember(context) { JsonRoomCacheStore(context) }

    var favorites by remember { mutableStateOf(preferences.loadFavorites()) }
    var autoPlay by remember { mutableStateOf(preferences.autoPlay) }
    var dataSaver by remember { mutableStateOf(preferences.dataSaver) }
    var preferredQuality by remember { mutableStateOf(preferences.preferredQuality) }
    var rooms by remember { mutableStateOf<List<ApiRoom>>(emptyList()) }
    var loadingRooms by remember { mutableStateOf(false) }
    var roomError by remember { mutableStateOf<String?>(null) }
    var refreshNonce by remember { mutableStateOf(0) }

    LaunchedEffect(selectedTab, refreshNonce) {
        if (selectedTab != 0) return@LaunchedEffect

        // Cache reads/writes happen off the main thread.
        val cached = withContext(Dispatchers.IO) { cacheStore.load() }
        if (cached.isNotEmpty()) {
            rooms = cached
            loadingRooms = false
        } else {
            loadingRooms = true
        }
        roomError = null

        // Best-effort refresh; never let a smaller/empty result replace a larger good cache.
        runCatching {
            withContext(Dispatchers.IO) { ChaturbateApi.fetchAllOnlineRooms() }
        }.onSuccess { fresh ->
            if (fresh.isNotEmpty()) {
                withContext(Dispatchers.IO) { cacheStore.save(fresh) }
                rooms = fresh
            } else if (rooms.isEmpty()) {
                roomError = "No live rooms returned."
            }
            loadingRooms = false
        }.onFailure {
            if (it is CancellationException) throw it
            if (rooms.isEmpty()) roomError = it.message ?: "Unable to load live rooms."
            loadingRooms = false
        }
    }

    BackHandler(enabled = selectedRoom != null) { selectedRoom = null }

    if (selectedRoom != null) {
        PlayerScreen(
            username = selectedRoom.orEmpty(),
            onBack = { selectedRoom = null },
            isFavorite = favorites.contains(selectedRoom),
            onFavorite = {
                val name = selectedRoom.orEmpty()
                favorites = if (favorites.contains(name)) favorites - name else favorites + name
                preferences.saveFavorites(favorites)
            },
            autoPlay = autoPlay,
            dataSaver = dataSaver,
            preferredQuality = preferredQuality,
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Scaffold(
        containerColor = OledBlack,
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding(), containerColor = OledBlack) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }, label = { Text("Home") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Favorites") }, label = { Text("Favorites") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }, label = { Text("Settings") })
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(
                rooms = rooms,
                query = query,
                loading = loadingRooms,
                error = roomError,
                onQueryChange = { query = it },
                modifier = Modifier.padding(padding),
                onRoomClick = { selectedRoom = it.username },
                onRetry = { refreshNonce++ }
            )
            1 -> FavoritesScreen(
                favorites = favorites,
                onRoomClick = { selectedRoom = it },
                modifier = Modifier.padding(padding)
            )
            2 -> SettingsScreen(
                autoPlay = autoPlay,
                onAutoPlayChange = { autoPlay = it; preferences.autoPlay = it },
                dataSaver = dataSaver,
                onDataSaverChange = { dataSaver = it; preferences.dataSaver = it },
                preferredQuality = preferredQuality,
                onPreferredQualityChange = { preferredQuality = it; preferences.preferredQuality = it },
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@Composable
private fun HomeScreen(
    rooms: List<ApiRoom>,
    query: String,
    loading: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRoomClick: (ApiRoom) -> Unit,
    onRetry: () -> Unit
) {
    val normalizedQuery = query.trim()
    val filtered = remember(rooms, normalizedQuery) { filterRooms(rooms, normalizedQuery) }

    Column(modifier = modifier.fillMaxSize().background(OledBlack).padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(16.dp))
        Text("Discover", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(4.dp))
        Text("Search scope: loaded live rooms only.", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        Spacer(Modifier.height(12.dp))
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search loaded live rooms") },
            leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
            colors = TextFieldDefaults.colors(
                focusedContainerColor = OledSurface,
                unfocusedContainerColor = OledSurface,
                focusedTextColor = TextPrimary,
                unfocusedTextColor = TextPrimary,
                focusedPlaceholderColor = TextSecondary,
                unfocusedPlaceholderColor = TextSecondary,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent
            )
        )
        Spacer(Modifier.height(16.dp))
        Text("Live rooms", color = TextSecondary)
        Spacer(Modifier.height(8.dp))

        when {
            loading && rooms.isEmpty() -> Text(
                "Loading live rooms…",
                color = TextSecondary,
                modifier = Modifier.padding(12.dp)
            )
            error != null && rooms.isEmpty() -> Column(modifier = Modifier.padding(12.dp)) {
                Text(error, color = TextSecondary)
                Spacer(Modifier.height(8.dp))
                Text("Tap to retry", color = Accent, modifier = Modifier.clickable(onClick = onRetry))
            }
            filtered.isEmpty() -> Text(
                if (query.isBlank()) "No live rooms are currently loaded."
                else "No loaded room matches. This does not mean the profile is offline or nonexistent.",
                color = TextSecondary,
                modifier = Modifier.padding(12.dp)
            )
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(filtered, key = { it.username.lowercase() }) { room ->
                    RoomCard(room, onClick = { onRoomClick(room) })
                }
            }
        }
    }
}

@Composable
private fun RoomThumbFallback(username: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            username.firstOrNull()?.uppercase() ?: "?",
            fontWeight = FontWeight.SemiBold,
            color = TextSecondary
        )
    }
}

@Composable
private fun RoomCard(room: ApiRoom, onClick: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = OledCard)) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(
                modifier = Modifier.size(58.dp),
                color = Color(0xFF151515),
                shape = RoundedCornerShape(16.dp)
            ) {
                val imageUrl = normalizedImageUrl(room.imageUrl)
                if (imageUrl != null) {
                    SubcomposeAsyncImage(
                        model = imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        loading = null,
                        error = { RoomThumbFallback(room.username) }
                    )
                } else {
                    RoomThumbFallback(room.username)
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(room.username, fontWeight = FontWeight.SemiBold)
                val subtitle = room.subject.ifBlank { room.category }
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = TextSecondary, maxLines = 1)
                }
            }
            Text(room.viewers.toString() + " viewers", color = TextSecondary)
        }
    }
}

@Composable
private fun FavoritesScreen(favorites: Set<String>, onRoomClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().background(OledBlack).padding(18.dp)) {
        Text("Favorites", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            if (favorites.isEmpty()) "Rooms you favorite will appear here." else favorites.size.toString() + " saved rooms",
            color = TextSecondary
        )
        Spacer(Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(favorites.toList(), key = { it.lowercase() }) { name ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onRoomClick(name) },
                    colors = CardDefaults.cardColors(containerColor = OledCard),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Star, contentDescription = null, tint = Accent)
                        Spacer(Modifier.size(12.dp))
                        Text(name, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
