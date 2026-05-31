package me.lingci.lib.player.mpv

import android.content.Context
import `is`.xyz.mpv.MPV
import xyz.doikki.videoplayer.util.L
import java.util.concurrent.ConcurrentHashMap

class MpvMediaSourceHelper(private val context: Context) {
    
    // 存储不同URL的HTTP头信息
    private val urlHeadersMap: ConcurrentHashMap<String, String> = ConcurrentHashMap()

    /**
     * 设置HTTP请求头
     * @param headers HTTP请求头映射
     */
    fun setHeaders(mpv: MPV, headers: MutableMap<String, String>) {
        if (headers.isNotEmpty()) {
            try {
                // 处理User-Agent
                if (headers.containsKey("User-Agent")) {
                    val value = headers.remove("User-Agent")
                    if (!value.isNullOrBlank()) {
                        mpv.setPropertyString("user-agent", value)
                        L.d("Set user-agent: $value")
                    }
                }

                // 处理其他HTTP头
                if (headers.isNotEmpty()) {
                    val headersString = headers.map {
                        "${it.key}: ${it.value.replace(",", "\\,")}"
                    }.joinToString(",")
                    mpv.setPropertyString("http-header-fields", headersString)
                    L.d("Set HTTP headers: $headersString")
                }
            } catch (e: Exception) {
                L.e("Failed to set HTTP headers: ${e.message}")
            }
        } else {
            mpv.setPropertyString("http-header-fields", "")
        }
    }
    
    /**
     * 为特定URL设置HTTP请求头
     * @param url 媒体URL
     * @param headers HTTP请求头映射
     */
    fun setHeadersForUrl(mpv: MPV, url: String, headers: MutableMap<String, String>) {
        if (url.isEmpty() || headers.isEmpty()) return
        
        val headersString = headers.map {
            "${it.key}: ${it.value.replace(",", "\\,")}"
        }.joinToString(",")
        urlHeadersMap[url] = headersString
        
        // 如果是当前正在加载的URL，立即应用
        try {
            try {
                val currentUrl = mpv.getPropertyString("stream-open-filename")
                if (currentUrl == url) {
                    setHeaders(mpv, headers)
                }
            } catch (e: Exception) {
                L.e("Failed to check current URL for headers: ${e.message}")
            }
        } catch (e: Exception) {
            L.e("Failed to setup network settings: ${e.message}")
        }
    }
    
    /**
     * 配置缓存设置
     * @param cacheEnabled 是否启用缓存
     * @param cacheSize 缓存大小（MB）
     */
    fun setupCache(mpv: MPV, cacheEnabled: Boolean, cacheSize: Int = 64) {
        try {
            try {
                if (cacheEnabled) {
                    // 设置缓存大小
                    val cacheBytes = cacheSize * 1024 * 1024L
                    mpv.setOptionString("demuxer-max-bytes", cacheBytes.toString())
                    mpv.setOptionString("demuxer-max-back-bytes", (cacheBytes / 2).toString())
                    
                    // 开启网络缓存
                    mpv.setOptionString("cache", "yes")
                    mpv.setOptionString("cache-on-disk", "yes")
                    
                    L.d("Cache enabled, size: ${cacheSize}MB")
                } else {
                    // 禁用缓存
                    mpv.setOptionString("cache", "no")
                    mpv.setOptionString("cache-on-disk", "no")
                    L.d("Cache disabled")
                }
            } catch (e: Exception) {
                L.e("Failed to setup cache: ${e.message}")
            }
        } catch (e: Exception) {
            L.e("Failed to setup network settings: ${e.message}")
        }
    }
    
    /**
     * 配置网络设置
     * @param timeout 超时时间（秒）
     * @param lowLatency 是否低延迟模式
     */
    fun setupNetworkSettings(mpv: MPV, timeout: Int = 10, lowLatency: Boolean = false) {
        try {
            try {
                // 设置网络超时
                mpv.setOptionString("network-timeout", (timeout * 1000).toString())
                
                if (lowLatency) {
                    // 低延迟模式设置
                    mpv.setOptionString("demuxer-readahead-secs", "0.5")
                    mpv.setOptionString("hr-seek-framedrop", "no")
                    mpv.setOptionString("cache-secs", "1")
                    L.d("Low latency mode enabled")
                } else {
                    // 正常模式设置
                    mpv.setOptionString("demuxer-readahead-secs", "5")
                    mpv.setOptionString("cache-secs", "30")
                }
            } catch (e: Exception) {
                L.e("Failed to setup network settings: ${e.message}")
            }
        } catch (e: Exception) {
            L.e("Failed to setup network settings: ${e.message}")
        }
    }
    
    /**
     * 清理资源
     */
    fun release() {
        urlHeadersMap.clear()
    }

}