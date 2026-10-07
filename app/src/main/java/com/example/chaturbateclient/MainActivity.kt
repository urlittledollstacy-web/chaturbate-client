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
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
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
import com.example.chaturbateclient.ui.AppTheme
import com.example.chaturbateclient.ui.ClientTheme
import com.example.chaturbateclient.ui.LocalAppColors
import com.example.chaturbateclient.ui.SettingsScreen
import com.example.chaturbateclient.ui.player.PlayerScreen
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent { ClientApp() }
    }
}

@Composable
private fun ClientApp() {
    val context = LocalContext.current
    val preferences = remember(context) { AppPreferences(context) }
    var themeLabel by rememberSaveable { mutableStateOf(preferences.theme) }
    val theme = AppTheme.fromLabel(themeLabel)

    ClientTheme(theme) {
        ClientContent(
            preferences = preferences,
            themeLabel = themeLabel,
            onThemeChange = { themeLabel = it; preferences.theme = it }
        )
    }
}

@Composable
private fun ClientContent(
    preferences: AppPreferences,
    themeLabel: String,
    onThemeChange: (String) -> Unit
) {
    val colors = LocalAppColors.current
    var selectedTab by rememberSaveable { mutableStateOf(0) }
    var query by rememberSaveable { mutableStateOf("") }
    var selectedRoom by rememberSaveable { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val cacheStore = remember(context) { JsonRoomCacheStore(context) }

    var favorites by remember { mutableStateOf(preferences.loadFavorites()) }
    var autoPlay by remember { mutableStateOf(preferences.autoPlay) }
    var dataSaver by remember { mutableStateOf(preferences.dataSaver) }
    var preferredQuality by remember { mutableStateOf(preferences.preferredQuality) }
    var videoResizeMode by remember { mutableStateOf(preferences.videoResizeMode) }
    var rooms by remember { mutableStateOf<List<ApiRoom>>(emptyList()) }
    var loadingRooms by remember { mutableStateOf(false) }
    var roomError by remember { mutableStateOf<String?>(null) }
    var refreshNonce by remember { mutableStateOf(0) }
    val gridState = rememberLazyGridState()
    val scope = rememberCoroutineScope()

    // Loads once at startup and on explicit refresh (pull-to-refresh or tapping Home while on Home).
    // Switching tabs does not reload.
    LaunchedEffect(refreshNonce) {
        // Cache reads/writes happen off the main thread.
        val cached = withContext(Dispatchers.IO) { cacheStore.load() }
        if (cached.isNotEmpty()) rooms = cached
        // Always flag loading so a refresh (not just the first load) shows its indicator.
        loadingRooms = true
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

    // A refresh should also return the feed to the top.
    LaunchedEffect(refreshNonce) {
        if (refreshNonce > 0) gridState.animateScrollToItem(0)
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
            videoResizeMode = videoResizeMode,
            onVideoResizeModeChange = { videoResizeMode = it; preferences.videoResizeMode = it },
            modifier = Modifier.fillMaxSize()
        )
        return
    }

    Scaffold(
        containerColor = colors.background,
        bottomBar = {
            NavigationBar(modifier = Modifier.navigationBarsPadding(), containerColor = colors.background) {
                NavigationBarItem(selected = selectedTab == 0, onClick = {
                    // Tapping Home while already on Home scrolls to the top; it refreshes only
                    // when the feed is already at the top. Arriving from another tab does nothing extra.
                    if (selectedTab == 0) {
                        if (gridState.firstVisibleItemIndex == 0) refreshNonce++ else scope.launch { gridState.animateScrollToItem(0) }
                    } else {
                        selectedTab = 0
                    }
                }, icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") }, label = { Text("Home") })
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
                onRetry = { refreshNonce++ },
                gridState = gridState
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
                theme = themeLabel,
                onThemeChange = onThemeChange,
                modifier = Modifier.padding(padding)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreen(
    rooms: List<ApiRoom>,
    query: String,
    loading: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRoomClick: (ApiRoom) -> Unit,
    onRetry: () -> Unit,
    gridState: LazyGridState
) {
    val colors = LocalAppColors.current
    val normalizedQuery = query.trim()
    val filtered = remember(rooms, normalizedQuery) { filterRooms(rooms, normalizedQuery) }
    val refreshing = loading && rooms.isNotEmpty()

    Column(modifier = modifier.fillMaxSize().background(colors.background)) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Spacer(Modifier.height(16.dp))
            Text("Discover", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(4.dp))
            Text("Search scope: loaded live rooms only.", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            Spacer(Modifier.height(12.dp))
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                placeholder = { Text("Search loaded live rooms") },
                leadingIcon = { Icon(Icons.Outlined.Search, contentDescription = null) },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = colors.surface,
                    unfocusedContainerColor = colors.surface,
                    focusedTextColor = colors.textPrimary,
                    unfocusedTextColor = colors.textPrimary,
                    focusedPlaceholderColor = colors.textSecondary,
                    unfocusedPlaceholderColor = colors.textSecondary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
            Spacer(Modifier.height(16.dp))
            Text("Live rooms", color = colors.textSecondary)
            Spacer(Modifier.height(8.dp))
        }

        when {
            loading && rooms.isEmpty() -> Text(
                "Loading live rooms…",
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(12.dp)
            )
            error != null && rooms.isEmpty() -> Column(modifier = Modifier.padding(horizontal = 16.dp).padding(12.dp)) {
                Text(error, color = colors.textSecondary)
                Spacer(Modifier.height(8.dp))
                Text("Tap to retry", color = colors.accent, modifier = Modifier.clickable(onClick = onRetry))
            }
            filtered.isEmpty() -> Text(
                if (query.isBlank()) "No live rooms are currently loaded."
                else "No loaded room matches. This does not mean the profile is offline or nonexistent.",
                color = colors.textSecondary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(12.dp)
            )
            else -> PullToRefreshBox(
                isRefreshing = refreshing,
                onRefresh = onRetry,
                modifier = Modifier.fillMaxSize()
            ) {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    state = gridState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    gridItems(filtered, key = { it.username.lowercase() }) { room ->
                        RoomGridCard(room, onClick = { onRoomClick(room) })
                    }
                }
            }
        }
    }
}

@Composable
private fun RoomThumbFallback(username: String) {
    val colors = LocalAppColors.current
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(
            username.firstOrNull()?.uppercase() ?: "?",
            fontWeight = FontWeight.SemiBold,
            color = colors.textSecondary
        )
    }
}

@Composable
private fun RoomGridCard(room: ApiRoom, onClick: () -> Unit) {
    val colors = LocalAppColors.current
    Card(modifier = Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = colors.card)) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(4f / 3f)
                    .background(colors.thumbPlaceholder)
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
            Column(modifier = Modifier.padding(10.dp)) {
                Text(room.username, fontWeight = FontWeight.SemiBold, maxLines = 1)
                val subtitle = room.subject.ifBlank { room.category }
                if (subtitle.isNotBlank()) {
                    Text(subtitle, color = colors.textSecondary, maxLines = 1, style = MaterialTheme.typography.bodySmall)
                }
                Spacer(Modifier.height(4.dp))
                Text(room.viewers.toString() + " viewers", color = colors.textSecondary, style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

@Composable
private fun FavoritesScreen(favorites: Set<String>, onRoomClick: (String) -> Unit, modifier: Modifier = Modifier) {
    val colors = LocalAppColors.current
    Column(modifier = modifier.fillMaxSize().background(colors.background).padding(18.dp)) {
        Text("Favorites", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            if (favorites.isEmpty()) "Rooms you favorite will appear here." else favorites.size.toString() + " saved rooms",
            color = colors.textSecondary
        )
        Spacer(Modifier.height(20.dp))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(favorites.toList(), key = { it.lowercase() }) { name ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onRoomClick(name) },
                    colors = CardDefaults.cardColors(containerColor = colors.card),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Star, contentDescription = null, tint = colors.accent)
                        Spacer(Modifier.size(12.dp))
                        Text(name, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
        }
    }
}
