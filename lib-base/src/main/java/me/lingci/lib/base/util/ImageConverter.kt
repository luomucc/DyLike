package me.lingci.lib.base.util

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * 图片转换
 */
class ImageConverter() {

    val options = BitmapFactory.Options().apply { inSampleSize = 2 } // 降低采样率

    enum class CompressFormat {
        JPEG,
        PNG,
        WEBP
    }

    private var sourceFile: File = File("")
    private var sourcePaths: List<String> = emptyList()
    private var compressFormat: CompressFormat = CompressFormat.JPEG
    private var lossless: Boolean = false
    private var quality: Int = 85
    private var targetFile: File = File("")

    fun setSourceFile(file: File): ImageConverter {
        this.sourceFile = file
        return this
    }

    fun setSourcePath(paths: List<String>): ImageConverter {
        this.sourcePaths = paths
        return this
    }

    fun setCompressFormat(format: CompressFormat): ImageConverter {
        this.compressFormat = format
        return this
    }

    fun isLossless(): ImageConverter {
        this.lossless = true
        return this
    }

    fun setQuality(quality: Int): ImageConverter {
        this.quality = quality
        return this
    }

    fun setTargetFile(file: File): ImageConverter {
        this.targetFile = file
        return this
    }

    private fun getQuality(): Int {
        if (lossless) {
            return 100
        }
        return quality
    }

    private fun getBitmapCompressFormat(): Bitmap.CompressFormat {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && compressFormat == CompressFormat.WEBP) {
            return if (lossless) {
                Bitmap.CompressFormat.WEBP_LOSSLESS
            } else {
                Bitmap.CompressFormat.WEBP_LOSSY
            }
        }
        return when (compressFormat) {
            CompressFormat.PNG -> Bitmap.CompressFormat.PNG
            else -> Bitmap.CompressFormat.JPEG
        }
    }

    private fun getFileType(): String {
        return when (compressFormat) {
            CompressFormat.PNG -> ".png"
            CompressFormat.WEBP -> ".webp"
            else -> ".jpg"
        }
    }

    suspend fun convert() {
        if (sourceFile.path.isBlank() || targetFile.path.isBlank()) {
            return
        }
        targetFile.createNew()
        loadBitmap(sourceFile.path)?.let { bitmap ->
            FileOutputStream(targetFile).use { fos ->
                bitmap.compress(getBitmapCompressFormat(), quality, fos)
            }
        }
    }

    suspend fun converts(): Result<Boolean> = withContext(Dispatchers.IO) {
        if (sourcePaths.isEmpty() || targetFile.path.isBlank()) {
            Result.success(false)
        } else {
            try {
                sourcePaths.forEach { path ->
                    val bitmap = loadBitmap(path) ?: return@forEach
                    val target = File(targetFile, "${path.subFileName()}${getFileType()}")
                    target.createNew()
                    FileOutputStream(target).use { fos ->
                        bitmap.compress(getBitmapCompressFormat(), quality, fos)
                    }
                }
                Result.success(true)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    suspend fun bitmapToFile(bitmap: Bitmap, outputFile: File): Boolean =
        withContext(Dispatchers.IO) {
            return@withContext try {
                if (outputFile.path.isBlank()) {
                    false
                } else {
                    outputFile.createNew()
                    FileOutputStream(outputFile).use {
                        bitmap.compress(getBitmapCompressFormat(), getQuality(), it)
                    }
                    true
                }
            } catch (e: Exception) {
                outputFile.deleteExists()
                false
            }
        }

    suspend fun saveImage(bitmap: Bitmap, outputFile: File): Boolean = withContext(Dispatchers.IO) {
        try {
            if (bitmap.width < 0 && bitmap.height < 0) {
                return@withContext false
            }
            if (outputFile.path.isBlank()) {
                return@withContext false
            }

            val isHdr = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                bitmap.config == Bitmap.Config.RGBA_F16
            } else {
                false
            }
            Log.d(this, "bitmap", bitmap.width, bitmap.height, "isHdr", isHdr, outputFile.path)
            outputFile.createNew()
            // 降级为 SDR 并保存为 PNG
            // bitmap.copy(Bitmap.Config.ARGB_8888, false)
            FileOutputStream(outputFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }
            bitmap.recycle()
            return@withContext true
        } catch (e: Exception) {
            Log.d(this, "save", e)
            outputFile.deleteExists()
            bitmap.recycle()
            return@withContext false
        }
    }

    suspend fun saveImageToMediaStore(
        context: Context, bitmap: Bitmap, folder: String, displayName: String
    ): Uri? = withContext(Dispatchers.IO) {
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/png")
            put(
                MediaStore.MediaColumns.RELATIVE_PATH,
                "${Environment.DIRECTORY_PICTURES}/${folder}"
            )
        }

        val resolver = context.contentResolver
        var uri: Uri? = null

        try {
            // 在 MediaStore 中创建一个新的条目
            uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentValues)
            uri?.let { imageUri ->
                // 打开输出流，将 Bitmap 写入该 URI 指向的位置
                resolver.openOutputStream(imageUri)?.use { outputStream ->
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
                }
                // 文件已写入，并且自动添加到了 MediaStore！
            }
        } catch (e: Exception) {
            e.printStackTrace()
            uri?.let { failedUri ->
                // 如果写入失败，尝试删除已创建的空条目
                try {
                    resolver.delete(failedUri, null, null)
                } catch (deleteException: Exception) {
                    deleteException.printStackTrace()
                }
            }
            return@withContext null
        }
        return@withContext uri
    }

    private suspend fun loadBitmap(uri: String): Bitmap? = withContext(Dispatchers.IO) {
        return@withContext try {
            BitmapFactory.decodeFile(uri)
        } catch (e: Exception) {
            Log.d(this@ImageConverter, "load bitmap failed", e)
            null
        }
    }

    private fun bitmapToByteArray(bitmap: Bitmap): ByteArray {
        ByteArrayOutputStream().use { bos ->
            // 压缩质量，范围 0 - 100，值越小质量越低，文件越小
            bitmap.compress(getBitmapCompressFormat(), getQuality(), bos)
            return bos.toByteArray()
        }
    }

    private fun isWebpFile(file: File): Boolean {
        // 验证 WebP 头标识
        FileInputStream(file).use {
            val header = ByteArray(4)
            it.read(header)
            return (String(header) == "RIFF")
        }
    }

    /**
     * 辅助函数：替换文件扩展名
     */
    private fun changeExtension(file: File, newExtension: String): File {
        val name = file.name
        val lastDot = name.lastIndexOf('.')
        val baseName = if (lastDot >= 0) name.take(lastDot) else name
        return File(file.parent, "$baseName$newExtension")
    }

}