package com.valtheron.horizon.mobile

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Read-only, revision-consistent neural inspection.
 *
 * The live Horizon Brain changes revisions continuously. Never combine
 * separate pages or sections: the first bounded neural_units page already
 * includes related_synapses, counts, and a single consistent revision.
 */
internal class NeuralGraphGateway(private val context: Context) {
    private val base = "http://127.0.0.1:8765"
    private var cached: String? = null
    private var cachedAtMs = 0L

    @Synchronized
    fun load(force: Boolean = false): String {
        val now = System.currentTimeMillis()
        if (!force && cached != null && now - cachedAtMs < 10_000L) return cached!!

        // The server caps pages at 512 KiB. If a page exceeds the contract,
        // retry a smaller independent first page, never merge revisions.
        var last: Exception? = null
        for (limit in intArrayOf(256, 128, 64, 16, 1)) {
            try {
                val page = fetch(
                    "/v1/brain/inspection?section=neural_units&order=activity&limit=$limit"
                )
                val result = inspectionPageToGraph(page).toString()
                cached = result
                cachedAtMs = now
                return result
            } catch (e: OversizedInspection) {
                last = e
            }
        }
        throw last ?: IllegalStateException("Halaman neural tidak tersedia")
    }

    private class OversizedInspection(message: String) : RuntimeException(message)

    private fun fetch(path: String): JSONObject {
        val connection = URL(base + path).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "GET"
            connection.connectTimeout = 4000
            connection.readTimeout = 12000
            connection.setRequestProperty("Accept", "application/json")
            context.getSharedPreferences("horizon_settings", Context.MODE_PRIVATE)
                .getString("bridge_token", null)?.trim()
                ?.takeIf { it.isNotEmpty() }
                ?.let { connection.setRequestProperty("Authorization", "Bearer $it") }

            val status = connection.responseCode
            if (status == 413) throw OversizedInspection("Halaman terlalu besar untuk API inspeksi")
            if (status == 401 || status == 403) {
                throw IllegalStateException("Token Bridge diperlukan atau salah. Isi di Setelan Horizon Mobile.")
            }
            if (status == 409) throw IllegalStateException("Revisi berubah. Muat ulang halaman inspeksi.")
            if (status == 404) throw IllegalStateException("Endpoint inspeksi tidak tersedia pada runtime Go ini.")
            val stream = if (status in 200..299) connection.inputStream else connection.errorStream
            val sink = ByteArrayOutputStream()
            stream?.use { input ->
                val buffer = ByteArray(8192)
                while (true) {
                    val bytes = input.read(buffer)
                    if (bytes < 0) break
                    if (sink.size() + bytes > 600_000) {
                        throw OversizedInspection("Halaman inspeksi melampaui 600 KB")
                    }
                    sink.write(buffer, 0, bytes)
                }
            }
            if (status !in 200..299) {
                throw IllegalStateException("Bridge HTTP $status: " + sink.toString("UTF-8").take(160))
            }
            return JSONObject(sink.toString("UTF-8"))
        } finally {
            connection.disconnect()
        }
    }
}

/**
 * Pure transformation of one Horizon Bridge inspection page into Canvas format.
 * No extra reads, local neural state, or neural memory are created.
 */
internal fun inspectionPageToGraph(page: JSONObject): JSONObject {
    require(page.optString("type") == "brain.inspection.page") {
        "Respons bukan halaman inspeksi neuron"
    }
    require(page.optInt("schema_version") == 1) {
        "Versi skema inspeksi belum didukung"
    }
    require(page.optString("brain_identity") == "horizon-primary-brain") {
        "Identitas canonical Brain tidak sesuai"
    }
    require(page.optString("inspection_epoch").isNotBlank()) {
        "Inspection epoch tidak tersedia"
    }
    require(page.optLong("state_revision", -1L) >= 0L) {
        "State revision tidak valid"
    }
    val data = page.getJSONObject("data")
    require(data.optString("section") == "neural_units") {
        "Halaman inspeksi bukan neural_units"
    }
    val items = data.optJSONArray("items")
        ?: throw IllegalArgumentException("Daftar neuron tidak tersedia")
    val rawEdges = data.optJSONArray("related_synapses") ?: JSONArray()
    val rawCounts = data.optJSONObject("counts")
        ?: throw IllegalArgumentException("Jumlah total jaringan tidak tersedia")

    val nodes = JSONArray()
    val ids = HashSet<String>()
    for (i in 0 until items.length()) {
        val unit = items.optJSONObject(i) ?: continue
        val id = unit.opt("id")?.toString() ?: continue
        if (!ids.add(id)) continue
        val vector = JSONArray()
        val representation = unit.optJSONArray("representation") ?: JSONArray()
        for (j in 0 until minOf(representation.length(), 48)) {
            vector.put(safeFinite(representation.optDouble(j)))
        }
        nodes.put(
            JSONObject().put("id", id)
                .put("activation", safeFinite(unit.optDouble("activation")))
                .put("importance", safeFinite(unit.optDouble("importance")))
                .put("plasticity", safeFinite(unit.optDouble("plasticity")))
                .put("representation", vector)
        )
    }

    val edges = JSONArray()
    val distinct = HashSet<String>()
    for (i in 0 until rawEdges.length()) {
        val edge = rawEdges.optJSONObject(i) ?: continue
        val source = edge.opt("source_id")?.toString() ?: continue
        val target = edge.opt("target_id")?.toString() ?: continue
        if (source == target || source !in ids || target !in ids) continue
        val weight = safeFinite(edge.optDouble("weight"))
        val confidence = safeFinite(edge.optDouble("confidence"))
        val inhibitory = edge.optBoolean("inhibitory")
        val key = "$source|$target|$weight|$confidence|$inhibitory"
        if (!distinct.add(key)) continue
        edges.put(
            JSONObject().put("s", source).put("t", target)
                .put("w", weight).put("conf", confidence)
                .put("inhibitory", inhibitory)
        )
    }

    val totalUnits = rawCounts.optInt("neural_units", data.optInt("total"))
    val totalSynapses = rawCounts.optInt("synapses", data.optInt("related_total"))
    val shownUnits = nodes.length()
    val shownEdges = edges.length()
    val revision = page.getLong("state_revision")
    return JSONObject()
        .put("status", "ok")
        .put("mode", "BRIDGE · REAL")
        .put("brain_identity", page.getString("brain_identity"))
        .put("state_revision", revision)
        .put("inspection_epoch", page.getString("inspection_epoch"))
        .put("counts", JSONObject()
            .put("neural_units", totalUnits)
            .put("synapses", totalSynapses))
        .put("nodes", nodes)
        .put("edges", edges)
        .put("coverage", "Terlihat $shownUnits/$totalUnits neuron, $shownEdges/$totalSynapses sinaps · revisi $revision")
        .put("rendering", "Proyeksi 3D berdasarkan representasi; bukan posisi fisik.")
        .put("retrieved_at", System.currentTimeMillis() / 1000.0)
}
private fun safeFinite(value: Double): Double = if (value.isFinite()) value else 0.0
