package me.lingci.lib.base.okhttp

import me.lingci.lib.base.util.Log
import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.net.URI

object RedirectResolver {

    fun getFinalUrl(url: String, headers: Map<String, String> = emptyMap(), count: Int = 9): String {
        if (count >= 10) return url
        val request = Request.Builder().url(url).headers(headers.toHeaders()).head().build()
        return try {
            OkUtil.unsafeClient.newCall(request).execute().use { res ->
                if (res.code in 300..399) {
                    val nextUrl = res.header("Location") ?: return url
                    getFinalUrl(nextUrl, headers, count + 1)
                } else url
            }
        } catch (e: Exception) { url }
    }

    // 定义User-Agent
    private const val EDGE_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36 Edg/91.0.864.59"

    // 最大重定向次数，防止无限循环
    private const val MAX_REDIRECTS = 10

    private val client = OkHttpClient.Builder()
        .sslSocketFactory(SslManager.sslContext.socketFactory, SslManager.trustManager)
        .hostnameVerifier { _, _ -> true }
        .followRedirects(false) // 禁用自动跟随重定向
        .followSslRedirects(false)
        .build()

    @JvmStatic
    fun getFinalRedirectUrl(shortUrl: String): String {
        return getFinalRedirectUrl(shortUrl, mapOf(), 0)
    }

    @JvmStatic
    fun getRedirectUrl(shortUrl: String, headers: Map<String, String>): String {
        return getFinalRedirectUrl(shortUrl, headers, 7)
    }

    /**
     * 递归获取最终重定向URL
     * @param url 当前URL
     * @param redirectCount 已重定向次数，用于防止无限循环
     */
    private fun getFinalRedirectUrl(url: String, headers: Map<String, String>, redirectCount: Int): String {
        Log.d(this, "count", redirectCount, "url", url)
        // 检查是否超过最大重定向次数
        if (redirectCount >= MAX_REDIRECTS) {
            return url
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", EDGE_UA).apply {
                if (headers.isNotEmpty()) {
                    headers.forEach { (t, u) -> header(t, u) }
                }
            }
            .build()

        try {
            client.newCall(request).execute().use { response ->
                // 处理响应
                return when {
                    // 2xx状态码，返回当前URL
                    response.isSuccessful -> url

                    // 3xx重定向状态码
                    response.code in 300..399 -> {
                        val location = response.header("Location") ?: return url

                        // 处理相对路径
                        val resolvedUrl = resolveUrl(url, location)

                        // 递归处理下一次重定向
                        getFinalRedirectUrl(resolvedUrl, headers, redirectCount + 1)
                    }

                    // 其他状态码，返回当前URL
                    else -> url
                }
            }
        } catch (e: IOException) {
            // 只捕获IO相关异常，避免掩盖其他问题
            Log.d(this, "getFinalRedirectUrl", e.message)
            return url
        }
    }

    /**
     * 解析相对路径的URL
     */
    private fun resolveUrl(baseUrl: String, relativeUrl: String): String {
        return try {
            val baseUri = URI(baseUrl)
            val resolvedUri = baseUri.resolve(relativeUrl)
            resolvedUri.toString()
        } catch (e: Exception) {
            // 解析失败时返回相对URL
            relativeUrl
        }
    }

}
