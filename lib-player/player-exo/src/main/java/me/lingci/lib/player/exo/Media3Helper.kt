package me.lingci.lib.player.exo

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import androidx.media3.common.Format
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.SeekParameters
import androidx.media3.inspector.FrameExtractor
import androidx.media3.inspector.MediaExtractorCompat
import androidx.media3.inspector.MetadataRetriever
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.lingci.lib.base.util.Log
import java.nio.ByteBuffer

/**
 *   @author : gemini
 *   time    : 2025/11/30
 *   desc    : https://developer.android.google.cn/media/media3/inspector?hl=zh-cn
 *   version : 1.0
 */
object Media3Helper {

    @UnstableApi
    suspend fun retrieveMetadata(context: Context, uri: Uri) = withContext(Dispatchers.IO) {
        try {
            // 1. Build the retriever.
            // `MetadataRetriever` implements `AutoCloseable`, so wrap it in
            // a Kotlin `.use` block, which calls `close()` automatically.
            MetadataRetriever.Builder(context, MediaItem.fromUri(uri)).build().use { retriever ->
                // 2. Retrieve metadata asynchronously.
                val trackGroups = retriever.retrieveTrackGroups().get()
                val timeline = retriever.retrieveTimeline().get()
                val durationUs = retriever.retrieveDurationUs().get()
                0.until(trackGroups.length)
                    .asSequence()
                    .map { trackGroups[it].getFormat(0) }
                    .filter { format -> format.containerMimeType.isNullOrBlank().not() }
                    .forEach(::handleMetaFormat)

                for (i in 0 until trackGroups.length) {
                    trackGroups.get(i).let {
                        println("${it.id} ${it.type} ${it.length}")
                    }
                }
                timeline?.let {
                    println("${it.periodCount}")
                }
                //handleMetadata(trackGroups, timeline, durationUs)
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }

    @UnstableApi
    fun handleMetaFormat(format: Format) {
        Log.d(this, "format", format)
    }

    @UnstableApi
    suspend fun getThumbnailFromVideo(
        context: Context,
        videoUri: Uri
    ): Bitmap? = withContext(Dispatchers.IO) {
        FrameExtractor.Builder(context, MediaItem.fromUri(videoUri))
            .setSeekParameters(SeekParameters.EXACT)
            .build().use { frameExtractor ->
                try {
                    return@withContext frameExtractor.thumbnail.get().bitmap
                } catch (e: Exception) {
                    Log.d(this, "FrameExtractor", "Failed to extract thumbnail", e)
                    return@withContext null
                }
            }
    }

    @UnstableApi
    suspend fun getFrameFromVideo(
        context: Context,
        videoUri: Uri,
        headerMap: Map<String, String>,
        timeMs: Long
    ): Bitmap? = withContext(Dispatchers.IO) {
        // 1. 创建带 Header 的 DataSource.Factory
        val dataSourceFactory = DefaultHttpDataSource.Factory().apply {
            if (headerMap.isNotEmpty()) {
                setDefaultRequestProperties(headerMap)
            }
        }
        // 2. 构建 MediaItem
        val mediaItem = MediaItem.fromUri(videoUri)

        // 3. 配置 FrameExtractor
        FrameExtractor.Builder(context, mediaItem)
            .setSeekParameters(SeekParameters.EXACT)
            .build().use { frameExtractor ->
                try {
                    // 异步获取帧（get() 会阻塞直到完成）
                    val frame = frameExtractor.getFrame(timeMs).get()
                    return@withContext frame?.bitmap
                } catch (e: Exception) {
                    Log.d(this, "FrameExtractor", "Failed to extract frame at $timeMs ms", e)
                    return@withContext null
                }
            }
    }

    @UnstableApi
    fun extractSamples(context: Context, mediaPath: String) {
        val extractor = MediaExtractorCompat(context)
        try {
            // 1. Setup the extractor
            extractor.setDataSource(mediaPath)

            // Find and select available tracks
            for (i in 0 until extractor.trackCount) {
                val format = extractor.getTrackFormat(i)
                extractor.selectTrack(i)
            }

            // 2. Process samples
            val buffer = ByteBuffer.allocate(10 * 1024 * 1024)
            while (true) {
                // Read an encoded sample into the buffer.
                val bytesRead = extractor.readSampleData(buffer, 0)
                if (bytesRead < 0) break

                // Access sample metadata
                val trackIndex = extractor.sampleTrackIndex
                val presentationTimeUs = extractor.sampleTime
                val sampleSize = extractor.sampleSize

                extractor.advance()
            }
        } catch (e: Exception) {
            throw RuntimeException(e)
        } finally {
            // 3. Release the extractor
            extractor.release()
        }

    }

}