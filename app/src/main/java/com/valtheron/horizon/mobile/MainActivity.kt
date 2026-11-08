package com.valtheron.horizon.mobile

import android.content.Context
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.graphics.Brush
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

private val Ink = Color(0xFF030915)
private val Panel = Color(0xFF0B192D)
private val Cyan = Color(0xFF6FE7F6)
private val Muted = Color(0xFF88A5C4)
private val White = Color(0xFFE8F6FF)
private val Purple = Color(0xFFA48AFE)
private val MobileColors = darkColorScheme(
    primary = Cyan, onPrimary = Ink, background = Ink, onBackground = White,
    surface = Panel, onSurface = White, secondary = Muted
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(3, 9, 21)
        window.navigationBarColor = android.graphics.Color.rgb(5, 11, 21)
        setContent { MaterialTheme(colorScheme = MobileColors) { HorizonMobileApp() } }
    }
}

private data class ChatEntry(val id: String, val isUser: Boolean, val text: String)

@Composable
private fun HorizonMobileApp() {
    val context = LocalContext.current
    var page by rememberSaveable { mutableStateOf(0) }
    var connected by remember { mutableStateOf<Boolean?>(null) }
    val tabs = listOf("Chat", "Neuron", "Memori", "Setelan")
    val symbols = listOf("◈", "✧", "▤", "⚙")

    LaunchedEffect(page) {
        connected = withContext(Dispatchers.IO) {
            runCatching { HorizonBridge.health(context).startsWith("Tersambung") }
                .getOrDefault(false)
        }
    }
    Scaffold(
        containerColor = Ink,
        topBar = {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Brush.horizontalGradient(listOf(Color(0xFF07172A), Ink)))
                    .padding(horizontal = 16.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(11.dp)
            ) {
                Box(
                    modifier = Modifier.size(42.dp)
                        .border(1.dp, Color(0xFF265877), RoundedCornerShape(14.dp))
                        .background(Brush.linearGradient(listOf(Color(0xFF142A49), Color(0xFF0A1529))), RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) { Text("✧", color = Cyan, style = MaterialTheme.typography.headlineMedium) }
                Column(modifier = Modifier.weight(1f)) {
                    Text("HORIZON MOBILE", color = White, fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium)
                    Text("ONE CANONICAL BRAIN", color = Cyan,
                        style = MaterialTheme.typography.labelSmall)
                }
                Box(
                    modifier = Modifier
                        .border(1.dp, if (connected == true) Color(0xFF287566) else Color(0xFF35516A), CircleShape)
                        .background(Color(0xFF091D30), CircleShape)
                        .padding(horizontal = 10.dp, vertical = 7.dp)
                ) {
                    Text(
                        if (connected == true) "● LIVE" else if (connected == false) "○ OFFLINE" else "○ CEK",
                        color = if (connected == true) Color(0xFF8AF3CD) else Muted,
                        style = MaterialTheme.typography.labelSmall
                    )
                }
            }
        },
        bottomBar = {
            Row(
                modifier = Modifier.fillMaxWidth()
                    .background(Color(0xFF081529))
                    .padding(horizontal = 10.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                tabs.forEachIndexed { index, label ->
                    Column(
                        modifier = Modifier.weight(1f)
                            .background(
                                if (page == index) Color(0xFF153451) else Color.Transparent,
                                RoundedCornerShape(14.dp)
                            )
                            .clickable { page = index }
                            .padding(vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(symbols[index], color = if (page == index) Cyan else Muted,
                            style = MaterialTheme.typography.titleMedium)
                        Text(label, color = if (page == index) White else Muted,
                            style = MaterialTheme.typography.labelSmall, maxLines = 1)
                    }
                }
            }
        }
    ) { pad ->
        Box(
            Modifier.fillMaxSize().padding(pad)
                .background(Brush.verticalGradient(listOf(Color(0xFF061426), Ink, Color(0xFF040915))))
        ) {
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
        Row(
            Modifier.fillMaxWidth().padding(top = 11.dp, bottom = 8.dp)
                .background(Color(0xFF0C2035), RoundedCornerShape(13.dp))
                .padding(horizontal = 13.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Text("◆", color = Purple)
            Column {
                Text("Horizon Native Interface", color = White,
                    style = MaterialTheme.typography.labelLarge)
                Text("Respons numerik asli · tanpa model percakapan eksternal",
                    color = Muted, style = MaterialTheme.typography.labelSmall)
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (messages.isEmpty() && !busy) item { ChatWelcome() }
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
                label = { Text("Tulis observasi ke Horizon...") },
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
private fun ChatWelcome() {
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 45.dp, bottom = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            Modifier.size(100.dp)
                .border(1.dp, Color(0xFF326987), CircleShape)
                .background(
                    Brush.radialGradient(listOf(Color(0xFF205B80), Color(0xFF0E2039), Ink)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) { Text("✧", color = Cyan, style = MaterialTheme.typography.displayLarge) }
        Text("ONE NEURAL BRAIN", color = Cyan, style = MaterialTheme.typography.labelSmall)
        Text("Selamat datang\ndi Horizon.", color = White,
            style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Text("Satu jaringan kecerdasan. Pengamatan dan respons diproses\n" +
            "oleh runtime neural lokal yang sama.",
            color = Muted, style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Row(
            modifier = Modifier.padding(top = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MiniChip("GO NATIVE", Cyan)
            MiniChip("LOCAL ONLY", Purple)
        }
    }
}

@Composable
private fun MiniChip(label: String, tint: Color) {
    Box(
        Modifier.border(1.dp, tint.copy(alpha = .45f), RoundedCornerShape(40.dp))
            .background(Color(0xFF0B1A30), RoundedCornerShape(40.dp))
            .padding(horizontal = 11.dp, vertical = 7.dp)
    ) { Text(label, color = tint, style = MaterialTheme.typography.labelSmall) }
}

@Composable
private fun ChatBubble(message: ChatEntry) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.90f),
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) Color(0xFF193D5C) else Color(0xFF101B34)
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
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 17.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Neural Field", color = White,
                    style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Text("Graf 3D · satu canonical Brain · read-only",
                    color = Muted, style = MaterialTheme.typography.labelSmall)
            }
            Text("CANVAS 3D", color = Cyan, style = MaterialTheme.typography.labelSmall)
        }
        AndroidView(
            factory = { context -> NeuralMonitor.create(context) },
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
        Text("Canonical Memory", color = White, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        Text("Snapshot inspeksi read-only dari runtime Go.", color = Muted,
            style = MaterialTheme.typography.bodySmall)
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
        Text("Horizon Settings", color = White, style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold)
        InfoTile("Horizon Bridge", "127.0.0.1:8765")
        InfoTile("Monitor Neuron", "Canvas terpasang di aplikasi · langsung dari Bridge Go")
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
