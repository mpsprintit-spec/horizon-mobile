package com.valtheron.horizon.mobile

import android.content.Context
import android.os.Bundle
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.UUID

private val Ink = Color(0xFF050B15)
private val Panel = Color(0xFF0F2032)
private val Cyan = Color(0xFF73E4EF)
private val Muted = Color(0xFF91AFC8)
private val White = Color(0xFFE5F6FD)
private val MobileColors = darkColorScheme(
    primary = Cyan, onPrimary = Ink, background = Ink, onBackground = White,
    surface = Panel, onSurface = White, secondary = Muted
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(5, 11, 21)
        window.navigationBarColor = android.graphics.Color.rgb(5, 11, 21)
        setContent { MaterialTheme(colorScheme = MobileColors) { HorizonMobileApp() } }
    }
}

private data class ChatEntry(val id: String, val isUser: Boolean, val text: String)

@Composable
private fun HorizonMobileApp() {
    var page by rememberSaveable { mutableStateOf(0) }
    val tabs = listOf("Percakapan", "Neuron", "Memori", "Pengaturan")
    Scaffold(
        containerColor = Ink,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth().background(Ink).padding(18.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("HORIZON MOBILE", color = White, fontWeight = FontWeight.Bold)
                    Text("ONE CANONICAL BRAIN", color = Cyan, style = MaterialTheme.typography.labelSmall)
                }
                Text("LOCAL", color = Muted, style = MaterialTheme.typography.labelSmall)
            }
        },
        bottomBar = {
            NavigationBar(containerColor = Panel) {
                tabs.forEachIndexed { index, label ->
                    NavigationBarItem(
                        selected = page == index, onClick = { page = index },
                        icon = { Text(listOf("◈", "✧", "▤", "⚙")[index]) },
                        label = { Text(label, maxLines = 1) }
                    )
                }
            }
        }
    ) { pad ->
        Box(Modifier.fillMaxSize().padding(pad)) {
            when (page) {
                0 -> ChatPage()
                1 -> NeuronPage()
                2 -> MemoryPage()
                else -> SettingsPage()
            }
        }
    }
}

@Composable
private fun ChatPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val messages = remember { mutableStateListOf<ChatEntry>() }
    val listState = rememberLazyListState()
    var prompt by rememberSaveable { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        messages.clear()
        messages.addAll(withContext(Dispatchers.IO) { History.load(context) })
    }
    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            withContext(Dispatchers.IO) { History.save(context, messages.toList()) }
            listState.animateScrollToItem(messages.lastIndex)
        }
    }
    Column(Modifier.fillMaxSize().padding(horizontal = 14.dp)) {
        Text(
            "Percakapan ini diproses oleh Horizon. Respons berupa hasil native neural, " +
                "bukan teks yang direka oleh model lain.",
            color = Muted,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 12.dp)
        )
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { message -> ChatBubble(message) }
            if (busy) {
                item {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Cyan)
                        Text("  Horizon memproses observasi...", color = Muted)
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.Bottom,
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = prompt, onValueChange = { prompt = it },
                modifier = Modifier.weight(1f),
                label = { Text("Pesan untuk Horizon") },
                maxLines = 5,
                enabled = !busy
            )
            Button(
                enabled = prompt.isNotBlank() && !busy,
                modifier = Modifier.height(56.dp),
                onClick = {
                    val text = prompt.trim()
                    if (text.isEmpty()) return@Button
                    prompt = ""
                    messages.add(ChatEntry(UUID.randomUUID().toString(), true, text))
                    busy = true
                    scope.launch {
                        val response = runCatching { HorizonBridge.observe(context, text) }
                            .getOrElse { "Koneksi Horizon gagal: " + (it.message ?: "tidak diketahui") }
                        messages.add(ChatEntry(UUID.randomUUID().toString(), false, response))
                        busy = false
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Cyan)
            ) { Text("Kirim") }
        }
    }
}

@Composable
private fun ChatBubble(message: ChatEntry) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.90f),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) Color(0xFF163B57) else Panel
            )
        ) {
            Text(
                message.text, color = White,
                modifier = Modifier.padding(14.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun NeuronPage() {
    Column(Modifier.fillMaxSize()) {
        Text(
            "Jaringan neural asli · WebView lokal · tanpa bentuk biologis",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
            color = Muted, style = MaterialTheme.typography.bodySmall
        )
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    webViewClient = WebViewClient()
                    setBackgroundColor(android.graphics.Color.rgb(5, 11, 21))
                    loadUrl("http://127.0.0.1:8897/")
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
private fun MemoryPage() {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var data by remember { mutableStateOf<JSONObject?>(null) }
    var status by remember { mutableStateOf("Belum diperiksa") }
    val refresh: () -> Unit = {
        scope.launch {
            status = "Membaca canonical Brain..."
            val result = runCatching { HorizonBridge.inspect(context) }
            result.onSuccess {
                data = it
                status = "Terhubung · read-only"
            }.onFailure {
                data = null
                status = "Tidak terhubung: " + (it.message ?: "error")
            }
        }
    }
    LaunchedEffect(Unit) { refresh() }
    Column(Modifier.fillMaxSize().padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Canonical Brain", color = White, style = MaterialTheme.typography.headlineSmall)
        Text(status, color = Muted)
        if (data != null) {
            val counts = data!!.optJSONObject("data")?.optJSONObject("counts")
            InfoTile("Neuron total", counts?.optString("neural_units") ?: "—")
            InfoTile("Sinaps total", counts?.optString("synapses") ?: "—")
            InfoTile("Revisi canonical", data!!.optString("state_revision", "—"))
            InfoTile("Identitas Brain", data!!.optString("brain_identity", "—"))
        } else {
            Text("Jalankan runtime Horizon Go di Termux untuk melihat metrik nyata.", color = Muted)
        }
        Button(onClick = refresh) { Text("Perbarui") }
        Text(
            "Panel ini tidak menulis bobot, tidak membuat checkpoint, " +
                "dan tidak menyimpan salinan neural substrate.",
            color = Muted, style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun InfoTile(title: String, value: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Panel)
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(title, color = Muted, style = MaterialTheme.typography.labelMedium)
            Text(
                value, color = White,
                style = MaterialTheme.typography.titleMedium,
                maxLines = 2, overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun SettingsPage() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("horizon_settings", Context.MODE_PRIVATE) }
    var token by remember { mutableStateOf(prefs.getString("bridge_token", "") ?: "") }
    var testResult by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    Column(
        Modifier.fillMaxSize().padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(15.dp)
    ) {
        Text("Pengaturan Lokal", color = White, style = MaterialTheme.typography.headlineSmall)
        InfoTile("Horizon Bridge", "127.0.0.1:8765")
        InfoTile("Monitor Neuron Canvas", "127.0.0.1:8897")
        OutlinedTextField(
            value = token,
            onValueChange = {
                token = it
                prefs.edit().putString("bridge_token", it).apply()
            },
            label = { Text("Token Bridge (opsional)") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        Button(onClick = {
            scope.launch {
                testResult = runCatching { HorizonBridge.health(context) }
                    .getOrElse { "Tidak terhubung: " + (it.message ?: "error") }
            }
        }) { Text("Uji koneksi Horizon") }
        Text(testResult, color = Muted)
        Text(
            "Aplikasi hanya menggunakan layanan loopback di perangkat ini. " +
                "Tidak membutuhkan akun atau API model eksternal.",
            color = Muted, style = MaterialTheme.typography.bodySmall
        )
    }
}

private object HorizonBridge {
    private const val BASE = "http://127.0.0.1:8765"

    private suspend fun call(context: Context, path: String, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val connection = URL(BASE + path).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = if (body == null) "GET" else "POST"
                connection.connectTimeout = 5000
                connection.readTimeout = 15000
                connection.setRequestProperty("Accept", "application/json")
                val token = context.getSharedPreferences("horizon_settings", Context.MODE_PRIVATE)
                    .getString("bridge_token", "")?.trim().orEmpty()
                if (token.isNotEmpty()) connection.setRequestProperty("Authorization", "Bearer " + token)
                if (body != null) {
                    connection.doOutput = true
                    connection.setRequestProperty("Content-Type", "application/json; charset=utf-8")
                    connection.outputStream.use { it.write(body.toString().toByteArray(Charsets.UTF_8)) }
                }
                val status = connection.responseCode
                val stream = if (status in 200..299) connection.inputStream else connection.errorStream
                val raw = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
                if (status !in 200..299) error("HTTP " + status + ": " + raw.take(250))
                JSONObject(raw)
            } finally {
                connection.disconnect()
            }
        }

    suspend fun health(context: Context): String {
        val result = call(context, "/health")
        return if (result.optString("status") == "ok") {
            "Tersambung ke " + result.optString("brain_identity")
        } else "Bridge belum siap"
    }

    suspend fun inspect(context: Context): JSONObject =
        call(context, "/v1/brain/inspection?section=neural_units&limit=1")

    suspend fun observe(context: Context, prompt: String): String {
        val tokens = JSONArray()
        prompt.split(Regex("\\s+")).filter { it.isNotBlank() }.forEach { tokens.put(it) }
        val source = "horizon-mobile"
        val modality = "language"
        val payload = JSONObject()
            .put("schema_version", 1)
            .put("brain_identity", "horizon-primary-brain")
            .put("event_id", UUID.randomUUID().toString())
            .put("timestamp", Instant.now().toString())
            .put("source", source)
            .put("modality", modality)
            .put("provenance", JSONObject()
                .put("source", source)
                .put("modality", modality)
                .put("adapter", "android-local")
                .put("synthetic", false))
            .put("tokens", tokens)
        val data = call(context, "/v1/observations", payload)
        val answer = data.optJSONObject("answer")
        val state = answer?.optString("status", "unknown") ?: "unknown"
        val confidence = answer?.optDouble("confidence", 0.0) ?: 0.0
        val nodeIds = answer?.optJSONArray("node_ids")
        val ids = if (nodeIds != null) {
            (0 until minOf(nodeIds.length(), 8)).joinToString(", ") { nodeIds.optString(it) }
        } else ""
        return "Status neural: " + state +
            "\nConfidence: " + String.format(java.util.Locale.US, "%.3f", confidence) +
            "\nState revision: " + data.optString("state_revision", "—") +
            "\nNode respons: " + (if (ids.isEmpty()) "tidak tersedia" else ids) +
            "\n\nHorizon belum menyediakan teks jawaban pada endpoint ini."
    }
}

private object History {
    fun load(context: Context): List<ChatEntry> = runCatching {
        val file = File(context.filesDir, "horizon_chat.json")
        if (!file.exists()) return@runCatching emptyList()
        val rows = JSONArray(file.readText())
        (0 until rows.length()).mapNotNull { idx ->
            val record = rows.optJSONObject(idx) ?: return@mapNotNull null
            ChatEntry(
                record.optString("id", UUID.randomUUID().toString()),
                record.optBoolean("user"),
                record.optString("text")
            )
        }
    }.getOrDefault(emptyList())

    fun save(context: Context, entries: List<ChatEntry>) {
        val rows = JSONArray()
        entries.takeLast(300).forEach { msg ->
            rows.put(JSONObject()
                .put("id", msg.id)
                .put("user", msg.isUser)
                .put("text", msg.text))
        }
        val file = File(context.filesDir, "horizon_chat.json")
        val temp = File(context.filesDir, "horizon_chat.tmp")
        temp.writeText(rows.toString())
        if (!temp.renameTo(file)) {
            temp.copyTo(file, overwrite = true)
            temp.delete()
        }
    }
}
