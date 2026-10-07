package com.example.chaturbateclient.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.chaturbateclient.data.ApiRoom
import com.example.chaturbateclient.data.normalizedImageUrl
import com.example.chaturbateclient.data.summarizeImageUrls
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

private val DiagSecondary = Color(0xFF9A9A9A)

private data class ProbeResult(val plain: String, val withReferer: String)

/**
 * Read-only diagnostics for the room thumbnails.
 *
 * Shows the raw image URLs actually present in the loaded catalogue and probes
 * each one to report the HTTP status the device receives, with and without a
 * browser Referer. This is how we tell a bad URL apart from hotlink blocking
 * without guessing.
 */
@Composable
fun DiagnosticsScreen(rooms: List<ApiRoom>, modifier: Modifier = Modifier) {
    val stats = remember(rooms) { summarizeImageUrls(rooms) }
    val sample = remember(rooms) { rooms.mapNotNull { r -> normalizedImageUrl(r.imageUrl)?.let { r.username to it } }.take(8) }
    val scope = rememberCoroutineScope()

    var results by remember { mutableStateOf<Map<String, ProbeResult>>(emptyMap()) }
    var running by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 18.dp)
    ) {
        Spacer(Modifier.height(18.dp))
        Text("Image diagnostics", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(6.dp))
        Text("Read-only. Nothing here changes your data.", color = DiagSecondary)
        Spacer(Modifier.height(18.dp))

        Text("Loaded rooms: ${stats.total}", fontWeight = FontWeight.SemiBold)
        Text("Image URL present (usable): ${stats.usable}", color = DiagSecondary)
        Text("Blank: ${stats.blank}  ·  https: ${stats.https}", color = DiagSecondary)
        Text("http (cleartext): ${stats.cleartextHttp}  ·  // protocol-relative: ${stats.protocolRelative}", color = DiagSecondary)
        Text("Other/unrecognised: ${stats.other}", color = DiagSecondary)

        Spacer(Modifier.height(16.dp))
        HorizontalDivider()
        Spacer(Modifier.height(16.dp))

        Button(
            enabled = !running && sample.isNotEmpty(),
            onClick = {
                scope.launch {
                    running = true
                    val out = LinkedHashMap<String, ProbeResult>()
                    for ((_, url) in sample) {
                        val plain = probe(url, withReferer = false)
                        val ref = probe(url, withReferer = true)
                        out[url] = ProbeResult(plain, ref)
                    }
                    results = out
                    running = false
                }
            }
        ) {
            Text(if (running) "Probing…" else "Probe first ${sample.size} images")
        }

        Spacer(Modifier.height(16.dp))

        if (sample.isEmpty()) {
            Text("No usable image URLs in the loaded catalogue.", color = DiagSecondary)
        } else {
            sample.forEach { (username, url) ->
                Text(username, fontWeight = FontWeight.SemiBold)
                Text(url, color = DiagSecondary, fontFamily = FontFamily.Monospace)
                val r = results[url]
                Text(
                    text = when {
                        r == null -> "not probed yet"
                        else -> "no referer → ${r.plain}\nwith referer → ${r.withReferer}"
                    },
                    color = Color(0xFFB0B0B0),
                    fontFamily = FontFamily.Monospace
                )
                Spacer(Modifier.height(14.dp))
            }
        }

        Spacer(Modifier.height(24.dp))
    }
}

private suspend fun probe(url: String, withReferer: Boolean): String = withContext(Dispatchers.IO) {
    try {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "HEAD"
            instanceFollowRedirects = false
            connectTimeout = 10_000
            readTimeout = 10_000
            setRequestProperty(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 13) AppleWebKit/537.36 Chrome/120 Mobile Safari/537.36"
            )
            if (withReferer) setRequestProperty("Referer", "https://chaturbate.com/")
        }
        val code = connection.responseCode
        val type = connection.contentType ?: "?"
        connection.disconnect()
        "HTTP $code · $type"
    } catch (e: Exception) {
        "error: ${e.javaClass.simpleName}: ${e.message}"
    }
}
