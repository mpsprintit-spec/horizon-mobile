package com.valtheron.horizon.mobile

import android.annotation.SuppressLint
import android.content.Context
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.webkit.WebViewAssetLoader
import java.io.ByteArrayInputStream

/**
 * Secure same-origin host for the approved offline Canvas neural viewer.
 * The only dynamic endpoint delegates to the read-only native Horizon gateway.
 */
internal object NeuralMonitor {
    private const val HOST = "appassets.androidplatform.net"
    private const val URL = "https://appassets.androidplatform.net/assets/neural/index.html"

    @SuppressLint("SetJavaScriptEnabled")
    fun create(context: Context): WebView {
        val graph = NeuralGraphGateway(context.applicationContext)
        val loader = WebViewAssetLoader.Builder()
            .addPathHandler("/assets/", WebViewAssetLoader.AssetsPathHandler(context))
            .addPathHandler("/api/", WebViewAssetLoader.PathHandler { path ->
                if (path != "graph") {
                    return@PathHandler jsonResponse(404, """{"status":"not_found"}""")
                }
                try {
                    jsonResponse(200, graph.load(force = true))
                } catch (exception: Exception) {
                    val message = org.json.JSONObject()
                        .put("status", "unavailable")
                        .put("message", exception.message ?: "Bridge Horizon tidak tersedia")
                    jsonResponse(503, message.toString())
                }
            })
            .build()
        return WebView(context).apply {
            settings.javaScriptEnabled = true
            settings.domStorageEnabled = true
            settings.allowFileAccess = false
            settings.allowContentAccess = false
            settings.javaScriptCanOpenWindowsAutomatically = false
            settings.setSupportMultipleWindows(false)
            settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
            setBackgroundColor(android.graphics.Color.rgb(3, 9, 21))
            webViewClient = object : WebViewClient() {
                override fun shouldInterceptRequest(
                    view: WebView,
                    request: WebResourceRequest
                ): WebResourceResponse? {
                    if (request.url.host != HOST) {
                        return jsonResponse(403, """{"status":"blocked"}""")
                    }
                    return loader.shouldInterceptRequest(request.url)
                        ?: jsonResponse(404, """{"status":"not_found"}""")
                }
                override fun shouldOverrideUrlLoading(
                    view: WebView,
                    request: WebResourceRequest
                ): Boolean = request.url.host != HOST
            }
            loadUrl(URL)
        }
    }

    private fun jsonResponse(status: Int, content: String): WebResourceResponse =
        WebResourceResponse(
            "application/json",
            "UTF-8",
            ByteArrayInputStream(content.toByteArray(Charsets.UTF_8))
        ).apply {
            setStatusCodeAndReasonPhrase(
                status,
                when (status) {
                    200 -> "OK"
                    403 -> "Forbidden"
                    404 -> "Not Found"
                    else -> "Service Unavailable"
                }
            )
            responseHeaders = mapOf(
                "Cache-Control" to "no-store",
                "X-Content-Type-Options" to "nosniff"
            )
        }
}
