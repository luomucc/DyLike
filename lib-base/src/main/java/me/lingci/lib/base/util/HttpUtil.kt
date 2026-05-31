package me.lingci.lib.base.util

import me.lingci.lib.base.okhttp.RedirectResolver
import me.lingci.lib.base.okhttp.httpGet
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlin.system.measureTimeMillis

/**
 * @author : happyc
 * time    : 2022/05/14
 * desc    :
 * version : 1.0
 */
object HttpUtil {

    const val IPHONE_UA =
        "Mozilla/5.0 (iPhone; CPU iPhone OS 9_1 like Mac OS X) AppleWebKit/601.1.46 (KHTML, like Gecko) Version/9.0 Mobile/13B143 Safari/601.1"
    const val ANDROID_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.124 Safari/537.36"
    const val EDGE_UA =
        "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36 Edg/115.0.1901.183"

    @JvmStatic
    fun testUrl(url: String): Boolean {
        return try {
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", EDGE_UA)
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            val responseCode = connection.responseCode
            connection.responseCode >= 200 && responseCode < 300
        } catch (e: IOException) {
            logD("test url", e)
            false
        }
    }

    suspend fun testUrlByOk(url: String): Boolean {
        return try {
            httpGet(url).unsafe().webUa().checkPreflight()
        } catch (e: Exception) {
            false
        }
    }

    @JvmStatic
    fun getRedirectUrl(openurl: String): String {
        try {
            val connection = URL(openurl).openConnection() as HttpURLConnection
            connection.setRequestProperty("User-Agent", EDGE_UA)
            connection.requestMethod = "GET"
            connection.connectTimeout = 2000
            connection.readTimeout = 2000
            connection.instanceFollowRedirects = false
            connection.connect()
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                val redirectUrl = connection.getHeaderField("Location")
                if (redirectUrl.isNotEmpty()) {
                    connection.disconnect()
                    return redirectUrl
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            logD("get url", e)
        }
        return openurl
    }

    @JvmStatic
    fun getRedirectUrl(openurl: String, headers: Map<String, String>): String {
        try {
            val connection = URL(openurl).openConnection() as HttpURLConnection
            connection.allowUserInteraction = true
            headers.forEach { (t, u) -> connection.setRequestProperty(t, u) }
            val responseCode = connection.responseCode
            if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || responseCode == HttpURLConnection.HTTP_MOVED_TEMP) {
                val redirectUrl = connection.getHeaderField("Location")
                if (redirectUrl.isNotEmpty()) {
                    Log.d(this@HttpUtil, redirectUrl)
                    connection.disconnect()
                    return redirectUrl
                }
            }
            connection.disconnect()
        } catch (e: Exception) {
            Log.d(this@HttpUtil, e)
        }
        return openurl
    }

    @JvmStatic
    fun getFinalRedirectUrl(shortUrl: String): String {
        Log.d(this, "url", shortUrl)
        HttpURLConnection.setFollowRedirects(false)
        val con = URL(shortUrl).openConnection() as HttpURLConnection
        con.requestMethod = "GET"
        con.setRequestProperty("User-Agent", EDGE_UA)
        try {
            return when (con.responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    shortUrl
                }
                HttpURLConnection.HTTP_MOVED_PERM, HttpURLConnection.HTTP_MOVED_TEMP -> {
                    var redirectUrl = con.getHeaderField("Location")
                    redirectUrl = redirectUrl.replace(" ", "%20")
                    getRedirectUrl(redirectUrl)
                }
                else -> {
                    shortUrl
                }
            }
        } catch (e: Exception) {
            return shortUrl
        } finally {
            con.disconnect()
        }
    }

}

fun main() {
    val url = "https://lmmzx.com"
    measureTimeMillis {
        println(HttpUtil.getRedirectUrl(url))
    }.let {
        println(it)
    }
    measureTimeMillis {
        println(HttpUtil.getFinalRedirectUrl(url))
    }.let {
        println(it)
    }
    measureTimeMillis {
        println(RedirectResolver.getRedirectUrl(url, emptyMap()))
    }.let {
        println(it)
    }
    measureTimeMillis {
        println(RedirectResolver.getFinalRedirectUrl(url))
    }.let {
        println(it)
    }

    //println(RedirectResolver.getRedirectUrl(
    //    "https://api.ihappyc.eu.org:10514/dav/凡人修仙传/06【海绵小站+www.haimianxz.com】凡人修仙传.The.Immortal.Ascension.S01E06.2025.2160p.WEB-DL.H265.HQ.60fps.AA.mp4",
    //    mapOf(Pair("Authorization", Credentials.basic("wouser", "wouser")))
    //))
}