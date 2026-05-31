package me.lingci.lib.player.mpv

import `is`.xyz.mpv.MPV
import `is`.xyz.mpv.Utils.AudioMetadata
import kotlin.math.ceil
import kotlin.math.roundToInt

/**
 * MPV播放器工具类
 */
object MpvUtil {

    /**
     * 播放状态缓存类，用于管理和更新播放器状态
     */
    class PlaybackStateCache {
        // 音频元数据
        val meta = AudioMetadata()
        
        // 缓存暂停状态
        var cachePause = false
            private set

        // seek 进行中状态
        var seeking = false
            private set
        
        // 播放暂停状态
        var pause = false
            private set
        
        // 播放位置（毫秒）
        var position = -1L
            private set
        
        // 总时长（毫秒）
        var duration = 0L
            private set
        
        // 播放列表位置
        var playlistPos = 0
            private set
        
        // 播放列表数量
        var playlistCount = 0
            private set
        
        // 播放速度
        var speed = 1f
            private set
        
        // 视频宽度
        var videoWidth = 0
            private set
        
        // 视频高度
        var videoHeight = 0
            private set
        
        // 视频旋转角度
        var videoRotation = 0
            private set

        // 视频显示宽高比（DAR）
        var videoAspect = 0.0
            private set

        // 是否已收到 rotate 属性（用于延迟首帧尺寸回调，避免旋转信息缺失导致闪烁）
        var rotateReceived = false
            private set

        // 缓冲百分比
        var bufferedPercentage = 0
            private set
        
        // 缓存时间（秒）
        var cacheTime = 0.0
            private set
        
        /** 获取播放位置（秒） */
        val positionSec get() = (position / 1000).toInt()
        
        /** 获取总时长（秒） */
        val durationSec get() = if (duration > 0) (duration / 1000f).roundToInt() else 0
        
        /** 清空缓存的状态 */
        fun clear() {
            position = -1L
            duration = 0L
            cachePause = false
            pause = false
            seeking = false
            playlistPos = 0
            playlistCount = 0
            speed = 1f
            videoWidth = 0
            videoHeight = 0
            videoRotation = 0
            videoAspect = 0.0
            rotateReceived = false
            bufferedPercentage = 0
            cacheTime = 0.0
        }

        /** 处理类型为 <code>MPV_FORMAT_NONE</code> 的属性更新 */
        fun update(property: String, mpv: MPV): Boolean {
            return meta.update(property, mpv)
        }

        /** 处理类型为 <code>MPV_FORMAT_STRING</code> 的属性更新 */
        fun update(property: String, value: String): Boolean {
            if (meta.update(property, value)) {
                return true
            }
            
            when (property) {
                "speed" -> {
                    try {
                        speed = value.toFloat()
                    } catch (e: NumberFormatException) {
                        // 处理转换错误
                    }
                }
                else -> return false
            }
            return true
        }

        /** 处理类型为 <code>MPV_FORMAT_FLAG</code> 的属性更新 */
        fun update(property: String, value: Boolean): Boolean {
            when (property) {
                "pause" -> pause = value
                "paused-for-cache" -> cachePause = value
                "seeking" -> seeking = value
                else -> return false
            }
            return true
        }

        /** 处理类型为 <code>MPV_FORMAT_INT64</code> 的属性更新 */
        fun update(property: String, value: Long): Boolean {
            when (property) {
                "time-pos" -> position = value * 1000 // 转换为毫秒
                "duration" -> duration = value * 1000 // 转换为毫秒
                "playlist-pos" -> playlistPos = value.toInt()
                "playlist-count" -> playlistCount = value.toInt()
                "width", "video-params/w" -> videoWidth = value.toInt()
                "height", "video-params/h" -> videoHeight = value.toInt()
                "video-params/rotate" -> {
                    videoRotation = value.toInt()
                    rotateReceived = true
                }
                "cache-buffering-state" -> bufferedPercentage = value.toInt()
                else -> return false
            }
            return true
        }

        /** 处理类型为 <code>MPV_FORMAT_DOUBLE</code> 的属性更新 */
        fun update(property: String, value: Double): Boolean {
            when (property) {
                "duration/full", "duration" -> duration = ceil(value * 1000.0).coerceAtLeast(0.0).toLong()
                "time-pos" -> position = (value * 1000).toLong()
                "video-params/rotate" -> {
                    videoRotation = value.toInt()
                    rotateReceived = true
                }
                "speed" -> speed = value.toFloat()
                "demuxer-cache-time" -> {
                    cacheTime = value
                    // 根据缓存时间计算缓冲百分比
                    if (duration > 0) {
                        bufferedPercentage = ((cacheTime * 100) / (duration / 1000.0)).toInt().coerceIn(0, 100)
                    }
                }
                "video-params/aspect" -> {
                    videoAspect = value
                }
                else -> return false
            }
            return true
        }
    }
    
    /**
     * 检查是否是有效的媒体URL
     */
    fun isValidMediaUrl(url: String?): Boolean {
        if (url.isNullOrEmpty()) return false
        
        return url.startsWith("http://") || 
               url.startsWith("https://") || 
               url.startsWith("file://") ||
               url.startsWith("rtmp://") ||
               url.startsWith("rtsp://") ||
               url.startsWith("m3u8") ||
               url.startsWith("mp4") ||
               url.contains(".mp4") ||
               url.contains(".m3u8") ||
               url.contains(".mkv") ||
               url.contains(".avi") ||
               url.contains(".flv")
    }
    
    /**
     * 获取默认的MPV选项配置
     */
    fun getDefaultOptions(): Map<String, String> {
        return mapOf(
            "profile" to "fast",
            "vo" to "gpu-next",
            "hwdec" to "mediacodec,mediacodec-copy",
            "ao" to "audiotrack,opensles",
            "video-sync" to "audio",
            "cache" to "yes"
        )
    }
}
