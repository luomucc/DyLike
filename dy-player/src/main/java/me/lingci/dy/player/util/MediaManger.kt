package me.lingci.dy.player.util

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.lingci.dy.player.entity.VideoData
import me.lingci.lib.base.util.Log
import me.lingci.lib.base.util.createNew
import me.lingci.lib.base.util.deleteExists
import me.lingci.lib.base.util.isLocal
import java.io.File
import java.io.FileOutputStream
import androidx.core.graphics.scale


/**
 *   @author : happyc
 *   time    : 2025/01/20
 *   desc    :
 *   version : 1.0
 */
object MediaManger {

    fun getVideoFrame(
        videoPath: String,
        timeUs: Long,
        option: Int = MediaMetadataRetriever.OPTION_CLOSEST
    ): Bitmap? {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(videoPath)
            retriever.getFrameAtTime(timeUs, option)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        } finally {
            retriever.release()
        }
    }

    suspend fun getMediaInfo(filePath: String) = withContext(Dispatchers.IO) {
        val retriever = MediaMetadataRetriever()
        try {
            // 设置数据源为指定的文件路径
            retriever.setDataSource(filePath)

            // 获取不同的媒体标签信息
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
            val width = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH)
            val height = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT)
            val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
            val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)

            // 打印获取到的媒体信息
            println("Duration: $duration Width: $width Height: $height Artist: $artist Title: $title")
            println("${retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE)}")
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            // 确保释放资源
            retriever.release()
        }
    }

    fun getVideoFirstFrame(videoPath: String): Bitmap? {
        return getVideoFrame(videoPath, 0, MediaMetadataRetriever.OPTION_CLOSEST_SYNC)
    }

    private fun scaleBitmap(bitmap: Bitmap, maxWidth: Int): Bitmap {
        if (bitmap.width <= maxWidth) {
            return bitmap
        }
        val ratio = bitmap.width.toFloat() / bitmap.height.toFloat()
        val newHeight = (maxWidth / ratio).toInt()
        return bitmap.scale(maxWidth, newHeight)
    }

    suspend fun scanVideoThumb(context: Context, videos: List<VideoData>) =
        withContext(Dispatchers.IO) {
            var count = 0
            for (video in videos) {
                if (video.videoUrl.isLocal().not()) {
                    continue
                }
                val file = File(context.externalCacheDir, ".thumb/${video.md5()}.${AppUtil.THUMB_TYPE}")
                if (file.exists().not()) {
                    file.createNew()
                    try {
                        FileOutputStream(file).use { fos ->
                            val originalBitmap = getVideoFirstFrame(video.videoUrl)
                            if (originalBitmap != null) {
                                try {
                                    val scaledBitmap = scaleBitmap(originalBitmap, 480)
                                    try {
                                        scaledBitmap.compress(AppUtil.COMPRESS_FORMAT, 70, fos)
                                    } finally {
                                        if (scaledBitmap !== originalBitmap) {
                                            scaledBitmap.recycle()
                                        }
                                    }
                                } finally {
                                    originalBitmap.recycle()
                                }
                            } else {
                                file.deleteExists()
                            }
                        }
                        count++
                    } catch (e: Exception) {
                        file.deleteExists()
                    }
                }
            }
            Log.d(this, "scanVideoThumb", count)
        }

}
