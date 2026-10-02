package com.example.chaturbateclient

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaturbateclient.data.Room

private val OledBlack = Color.Black\nprivate val OledCard = Color(0xFF0A0A0A)\nprivate val OledElevated = Color(0xFF111111)
private val OledSurface = Color(0xFF080808)
private val TextPrimary = Color(0xFFF5F5F5)
private val TextSecondary = Color(0xFF9A9A9A)\nprivate val Accent = Color(0xFFD8B4FE)

private val demoRooms = listOf(
    Room("Room preview", 0, "API pending"),
    Room("Room preview 2", 0, "API pending"),
    Room("Room preview 3", 0, "API pending")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ClientTheme {
                ClientApp()
            }
        }
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
    var selectedTab by remember { mutableStateOf(0) }
    var query by remember { mutableStateOf("") }\n    var selectedRoom by remember { mutableStateOf<Room?>(null) }\n    var favorites by remember { mutableStateOf(setOf<String>()) }

    Scaffold(
        containerColor = OledBlack,
        bottomBar = {
            NavigationBar(
                modifier = Modifier.navigationBarsPadding(),
                containerColor = OledBlack
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Outlined.Home, contentDescription = "Home") },
                    label = { Text("Home") }
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Outlined.FavoriteBorder, contentDescription = "Favorites") },
                    label = { Text("Favorites") }
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Outlined.Settings, contentDescription = "Settings") },
                    label = { Text("Settings") }
                )
            }
        }
    ) { padding ->
        if (selectedRoom != null) {\n            com.example.chaturbateclient.ui.player.PlayerScreen(\n                username = selectedRoom!!.username,\n                onBack = { selectedRoom = null },\n                isFavorite = favorites.contains(selectedRoom!!.username),\n                onFavorite = {\n                    val name = selectedRoom!!.username\n                    favorites = if (favorites.contains(name)) favorites - name else favorites + name\n                }\n            )\n        } else when (selectedTab) {
            0 -> HomeScreen(query, { query = it }, Modifier.padding(padding)) { selectedRoom = it }
            1 -> FavoritesScreen(favorites, Modifier.padding(padding))
            else -> PlaceholderScreen("Settings", Modifier.padding(padding))
        }
    }
}

@Composable
private fun HomeScreen(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    onRoomClick: (Room) -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(OledBlack)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        Text(
            text = "Discover",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(Modifier.height(12.dp))

        TextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            placeholder = { Text("Search rooms") },
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
        Text(
            text = "Live rooms",
            color = TextSecondary
        )
        Spacer(Modifier.height(8.dp))

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(demoRooms.filter { query.isBlank() || it.username.contains(query, ignoreCase = true) }) { room -> RoomCard(room, onClick = { onRoomClick(room) }) }
        }
    }
}

@Composable
private fun RoomCard(room: Room, onClick: () -> Unit = {}) {
    Card(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = OledCard)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                modifier = Modifier.size(58.dp),
                color = Color(0xFF151515),
                shape = RoundedCornerShape(16.dp)
            ) {}

            Spacer(Modifier.size(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(room.username, fontWeight = FontWeight.SemiBold)
                Text(room.category, color = TextSecondary)
            }

            Text(
                text = if (room.viewers > 0) room.viewers.toString() + " viewers" else "—",
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun PlaceholderScreen(title: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize().background(OledBlack).padding(20.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "This section is part of the initial client scaffold.",
            color = TextSecondary
        )
    }
}
\n@Composable
private fun FavoritesScreen(
    favorites: Set<String>,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxSize().background(OledBlack).padding(18.dp)
    ) {
        Text("Favorites", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text(
            if (favorites.isEmpty()) "Rooms you favorite will appear here."
            else favorites.size.toString() + " saved rooms",
            color = TextSecondary
        )
        Spacer(Modifier.height(20.dp))
        favorites.forEach { name ->
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp),
                colors = CardDefaults.cardColors(containerColor = OledCard),
                shape = RoundedCornerShape(18.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Star, contentDescription = null, tint = Accent)
                    Spacer(Modifier.size(12.dp))
                    Text(name, style = MaterialTheme.typography.titleMedium)
                }
            }
        }
    }
}
