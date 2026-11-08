package com.valtheron.horizon.mobile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Read-only bounded projection of the ONE canonical Horizon Brain.
 * Spatial coordinates are created by the Canvas viewer for visualization only.
 * No neural weights, checkpoints, or canonical state are mutated.
 */
internal class NeuralGraphGateway(private val context: Context) {
    private val base = "http://127.0.0.1:8765"
    private val brainIdentity = "horizon-primary-brain"
    private var cached: String? = null
    private var cachedAtMs: Long = 0L

    @Synchronized
    fun load(force: Boolean = false): String {
        val now = System.currentTimeMillis()
        if (!force && cached != null && now - cachedAtMs < 10_000) return cached!!
        val units = LinkedHashMap<String, JSONObject>()
        val edgesRaw = ArrayList<JSONObject>()
        val related = ArrayList<JSONObject>()
        var identity: String? = null
        var epoch: String? = null
        var revision: Long? = null
        var counts = JSONObject()

        fun read(section: String, order: String, maxPages: Int) {
            var cursor = ""
            repeat(maxPages) {
                val params = "section=$section&limit=256&order=$order" +
                    (if (cursor.isNotEmpty()) "&cursor=" + java.net.URLEncoder.encode(cursor, "UTF-8") else "")
                val page = fetch("/v1/brain/inspection?$params")
                require(page.optString("type") == "brain.inspection.page" &&
                    page.optInt("schema_version") == 1) { "Skema inspeksi Horizon tidak kompatibel" }
                val pageIdentity = page.optString("brain_identity")
                val pageEpoch = page.optString("inspection_epoch")
                val pageRevision = page.optLong("state_revision", -1L)
                require(pageIdentity == brainIdentity) { "Identitas canonical Brain tidak sesuai" }
                if (identity == null) {
                    identity = pageIdentity
                    epoch = pageEpoch
                    revision = pageRevision
                }
                require(identity == pageIdentity && epoch == pageEpoch &&
                    revision == pageRevision) { "Revisi neural berubah; coba segarkan" }

                val data = page.getJSONObject("data")
                data.optJSONObject("counts")?.let { counts = it }
                val items = data.optJSONArray("items") ?: JSONArray()
                for (i in 0 until items.length()) {
                    val item = items.optJSONObject(i) ?: continue
                    when (section) {
                        "neural_units" -> {
                            val id = item.opt("id")?.toString() ?: continue
                            if (units.size < 760 || units.containsKey(id)) units[id] = item
                        }
                        "synapses" -> edgesRaw.add(item)
                    }
                }
                if (section == "neural_units") {
                    val links = data.optJSONArray("related_synapses") ?: JSONArray()
                    for (i in 0 until links.length()) links.optJSONObject(i)?.let(related::add)
                }
                cursor = page.optString("next_cursor")
                if (cursor.isEmpty()) return
            }
        }

        read("neural_units", "activity", 2)
        read("neural_units", "id", 1)
        read("synapses", "id", 5)

        val nodes = JSONArray()
        for ((id, u) in units) {
            val v = JSONArray()
            val representation = u.optJSONArray("representation") ?: JSONArray()
            for (i in 0 until minOf(representation.length(), 48))
                v.put(finite(representation.optDouble(i)))
            nodes.put(JSONObject()
                .put("id", id)
                .put("activation", finite(u.optDouble("activation")))
                .put("importance", finite(u.optDouble("importance")))
                .put("plasticity", finite(u.optDouble("plasticity")))
                .put("representation", v))
        }

        val edges = JSONArray()
        val seen = HashSet<String>()
        for (u in edgesRaw + related) {
            val source = u.opt("source_id")?.toString() ?: continue
            val target = u.opt("target_id")?.toString() ?: continue
            if (source == target || source !in units || target !in units) continue
            val weight = finite(u.optDouble("weight"))
            val conf = finite(u.optDouble("confidence"))
            val inhibitory = u.optBoolean("inhibitory")
            val signature = "$source:$target:$weight:$conf:$inhibitory"
            if (!seen.add(signature)) continue
            edges.put(JSONObject().put("s", source).put("t", target)
                .put("w", weight).put("conf", conf).put("inhibitory", inhibitory))
            if (edges.length() >= 2200) break
        }

        val result = JSONObject()
            .put("status", "ok")
            .put("mode", "BRIDGE • REAL")
            .put("brain_identity", identity ?: brainIdentity)
            .put("state_revision", revision ?: -1L)
            .put("counts", JSONObject()
                .put("neural_units", counts.optInt("neural_units", units.size))
                .put("synapses", counts.optInt("synapses", edges.length())))
            .put("nodes", nodes).put("edges", edges)
            .put("coverage", "Jendela inspeksi terbatas; jumlah ditampilkan berbeda dari total.")
            .put("rendering", "Proyeksi 3D dari representasi dan konektivitas; bukan koordinat fisik.")
            .put("retrieved_at", System.currentTimeMillis() / 1000.0)

        val json = result.toString()
        cached = json
        cachedAtMs = now
        return json
    }

    private fun finite(value: Double): Double = if (value.isFinite()) value else 0.0

    private fun fetch(path: String): JSONObject {
        val connection = URL(base + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 8000
            connection.setRequestProperty("Accept", "application/json")
            context.getSharedPreferences("horizon_settings", Context.MODE_PRIVATE)
                .getString("bridge_token", null)?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { connection.setRequestProperty("Authorization", "Bearer $it") }
            val status = connection.responseCode
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val sink = ByteArrayOutputStream()
            stream?.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val n = input.read(buffer)
                    if (n == -1) break
                    if (sink.size() + n > 600_000) error("Halaman inspeksi terlalu besar")
                    sink.write(buffer, 0, n)
                }
            }
            val raw = sink.toString("UTF-8")
            if (status !in 200..299) {
                error(if (status == 409) "Revisi Horizon berubah; segarkan data"
                    else "Bridge HTTP $status: " + raw.take(170))
            }
            return JSONObject(raw)
        } finally {
            connection.disconnect()
        }
    }
}
