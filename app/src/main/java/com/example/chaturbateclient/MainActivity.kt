package com.example.chaturbateclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.chaturbateclient.data.ApiRoom
import com.example.chaturbateclient.data.AppPreferences
import com.example.chaturbateclient.data.DiscoveryViewModel
import com.example.chaturbateclient.ui.SettingsScreen
import com.example.chaturbateclient.ui.player.PlayerScreen

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
private fun ClientApp(viewModel: DiscoveryViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val searchResults by viewModel.searchResults.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    val preferences = remember(context) { AppPreferences(context) }

    var selectedTab by remember { mutableStateOf(0) }
    var selectedRoom by remember { mutableStateOf<String?>(null) }
    var favorites by remember { mutableStateOf(preferences.loadFavorites()) }
    var autoPlay by remember { mutableStateOf(preferences.autoPlay) }
    var dataSaver by remember { mutableStateOf(preferences.dataSaver) }
    var preferredQuality by remember { mutableStateOf(preferences.preferredQuality) }
    var showDiagnostic by remember { mutableStateOf(false) }

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
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = OledBlack
            ) {
                NavigationBarItem(selected = selectedTab == 0, onClick = { selectedTab = 0 }, icon = { Icon(Icons.Outlined.Home, null) }, label = { Text("Discover") })
                NavigationBarItem(selected = selectedTab == 1, onClick = { selectedTab = 1 }, icon = { Icon(Icons.Outlined.FavoriteBorder, null) }, label = { Text("Favorites") })
                NavigationBarItem(selected = selectedTab == 2, onClick = { selectedTab = 2 }, icon = { Icon(Icons.Outlined.Settings, null) }, label = { Text("Settings") })
            }
        }
    ) { padding ->
        when (selectedTab) {
            0 -> HomeScreen(
                rooms = searchResults,
                query = query,
                loading = state.loading,
                refreshing = state.refreshing,
                partial = state.isPartial,
                error = state.error,
                onQueryChange = viewModel::setQuery,
                onRoomClick = { selectedRoom = it.username },
                onRetry = viewModel::refresh,
                onRefresh = viewModel::refresh,
                onDiagnostics = { showDiagnostic = true },
                modifier = Modifier.padding(padding)
            )
            1 -> FavoritesScreen(favorites, onRoomClick = { selectedRoom = it }, modifier = Modifier.padding(padding))
            else -> SettingsScreen(
                autoPlay = autoPlay,
                onAutoPlayChange = { autoPlay = it; preferences.autoPlay = it },
                dataSaver = dataSaver,
                onDataSaverChange = { dataSaver = it; preferences.dataSaver = it },
                preferredQuality = preferredQuality,
                onPreferredQualityChange = { preferredQuality = it; preferences.preferredQuality = it },
                onShowDiagnostics = { showDiagnostic = true },
                modifier = Modifier.padding(padding)
            )
        }
    }

    if (showDiagnostic) {
        AlertDialog(
            onDismissRequest = { showDiagnostic = false },
            title = { Text("Network diagnostics") },
            text = {
                Text(
                    state.lastDiagnostic?.safeSummary()
                        ?: "No network diagnostic is currently available. Diagnostics are captured for errors, redirects, and unexpected responses."
                )
            },
            confirmButton = {
                androidx.compose.material3.TextButton(onClick = { showDiagnostic = false }) { Text("Close") }
            }
        )
    }
}

@Composable
private fun HomeScreen(
    rooms: List<ApiRoom>,
    query: String,
    loading: Boolean,
    refreshing: Boolean,
    partial: Boolean,
    error: String?,
    onQueryChange: (String) -> Unit,
    onRoomClick: (ApiRoom) -> Unit,
    onRetry: () -> Unit,
    onRefresh: () -> Unit,
    onDiagnostics: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.fillMaxSize().background(OledBlack).padding(horizontal = 16.dp)) {
        Spacer(Modifier.size(16.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Discover", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
            IconButton(onClick = onRefresh) { Icon(Icons.Outlined.Refresh, "Refresh") }
        }
        Text(
            if (partial) "Live catalogue • coverage is partial" else "Live catalogue",
            color = if (partial) Color(0xFFFFCC80) else TextSecondary
        )
        Spacer(Modifier.size(12.dp))
        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search loaded live rooms") },
            leadingIcon = { Icon(Icons.Outlined.Search, null) },
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
        Spacer(Modifier.size(12.dp))
        if (query.isNotBlank()) {
            Text("Local search • " + rooms.size + " loaded matches", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        } else if (refreshing) {
            Text("Refreshing catalogue in the background…", color = TextSecondary, style = MaterialTheme.typography.bodySmall)
        }
        if (error != null) {
            Card(Modifier.fillMaxWidth().padding(vertical = 8.dp), colors = CardDefaults.cardColors(containerColor = Color(0xFF160D0D))) {
                Column(Modifier.padding(12.dp)) {
                    Text(error, color = Color(0xFFFFB4AB))
                    Text("Retry", color = Accent, modifier = Modifier.clickable(onClick = onRetry).padding(top = 8.dp))
                    Text("Diagnostics", color = Accent, modifier = Modifier.clickable(onClick = onDiagnostics).padding(top = 8.dp))
                }
            }
        }
        when {
            loading && rooms.isEmpty() -> CircularProgressIndicator(Modifier.padding(20.dp))
            rooms.isEmpty() -> Text("No live rooms are currently loaded.", color = TextSecondary, modifier = Modifier.padding(12.dp))
            else -> LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(rooms, key = { it.username.lowercase() }) { room -> RoomCard(room) { onRoomClick(room) } }
            }
        }
    }
}

@Composable
private fun RoomCard(room: ApiRoom, onClick: () -> Unit) {
    Card(Modifier.fillMaxWidth().clickable(onClick = onClick), colors = CardDefaults.cardColors(containerColor = OledCard)) {
        Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Surface(Modifier.size(64.dp), color = Color(0xFF151515), shape = RoundedCornerShape(14.dp)) {
                if (room.imageUrl.startsWith("https://")) {
                    AsyncImage(
                        model = room.imageUrl,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Spacer(Modifier.size(12.dp))
            Column(Modifier.weight(1f)) {
                Text(room.username, fontWeight = FontWeight.SemiBold)
                Text(room.subject.ifBlank { "Live" }, color = TextSecondary, maxLines = 1)
                val meta = listOf(room.gender, room.country).filter(String::isNotBlank).joinToString(" • ")
                if (meta.isNotBlank()) Text(meta, color = TextSecondary, style = MaterialTheme.typography.bodySmall)
            }
            Text(room.viewers.toString() + " viewers", color = TextSecondary)
        }
    }
}

@Composable
private fun FavoritesScreen(favorites: Set<String>, onRoomClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxSize().background(OledBlack).padding(18.dp)) {
        Text("Favorites", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.size(6.dp))
        Text(if (favorites.isEmpty()) "Rooms you favorite will appear here." else favorites.size.toString() + " saved rooms", color = TextSecondary)
        Spacer(Modifier.size(20.dp))
        favorites.forEach { name ->
            Card(Modifier.fillMaxWidth().padding(bottom = 10.dp).clickable { onRoomClick(name) }, colors = CardDefaults.cardColors(containerColor = OledCard), shape = RoundedCornerShape(18.dp)) {
                Row(Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Outlined.Star, null, tint = Accent)
                    Spacer(Modifier.size(12.dp))
                    Text(name, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
