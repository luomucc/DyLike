package me.lingci.lib.base.storage

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.util.JsonUtil
import me.lingci.lib.base.util.createNew
import me.lingci.lib.base.util.writeJsonEntity
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * 文件系统缓存管理器
 * 用于缓存文件列表和文件信息，提高查询速度
 */
class StorageCacheManager(private val context: Context) {

    // 缓存目录
    private val cacheDir by lazy {
        File(context.cacheDir, "file_system_cache").apply {
            if (!exists()) mkdirs()
        }
    }

    // 缓存过期时间（默认10分钟）
    private val cacheExpirationTime = TimeUnit.MINUTES.toMillis(10)

    /**
     * 缓存文件列表
     */
    suspend fun cacheFileList(
        storageId: String,
        path: String,
        files: List<FileEntity>
    ) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile(storageId, path, "list")
            val cacheData = CacheData(
                timestamp = System.currentTimeMillis(),
                data = files
            )
            // 写法1
            cacheFile.createNew()
            cacheFile.outputStream().use { steam ->
                JsonUtil.writeTo(cacheData, steam)
            }
            // 写法2
            cacheFile.writeJsonEntity(cacheData)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取缓存的文件列表
     * 如果缓存不存在或已过期，返回null
     */
    suspend fun getCachedFileList(
        storageId: String,
        path: String
    ): List<FileEntity>? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile(storageId, path, "list")
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                return@withContext null
            }

            // 检查缓存是否过期
            val lastModified = cacheFile.lastModified()
            if (System.currentTimeMillis() - lastModified > cacheExpirationTime) {
                cacheFile.delete()
                return@withContext null
            }
            cacheFile.inputStream().use { steam ->
                val cacheData = JsonUtil.toEntity<CacheData<List<FileEntity>>>(steam)
                cacheData.data
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 缓存单个文件信息
     */
    suspend fun cacheFileInfo(
        storageId: String,
        path: String,
        file: FileEntity
    ) = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile(storageId, path, "info")
            val cacheData = CacheData(
                timestamp = System.currentTimeMillis(),
                data = file
            )
            cacheFile.createNew()
            cacheFile.outputStream().use { steam ->
                JsonUtil.writeTo(cacheData, steam)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * 获取缓存的文件信息
     * 如果缓存不存在或已过期，返回null
     */
    suspend fun getCachedFileInfo(
        storageId: String,
        path: String
    ): FileEntity? = withContext(Dispatchers.IO) {
        try {
            val cacheFile = getCacheFile(storageId, path, "info")
            if (!cacheFile.exists() || cacheFile.length() == 0L) {
                return@withContext null
            }

            // 检查缓存是否过期
            val lastModified = cacheFile.lastModified()
            if (System.currentTimeMillis() - lastModified > cacheExpirationTime) {
                cacheFile.delete()
                return@withContext null
            }
            cacheFile.inputStream().use { steam ->
                val cacheData = JsonUtil.toEntity<CacheData<FileEntity>>(steam)
                cacheData.data
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 当文件被修改时，更新相关缓存
     */
    suspend fun invalidateCache(
        storageId: String,
        path: String,
        isDirectory: Boolean
    ) = withContext(Dispatchers.IO) {
        // 删除当前路径的缓存
        getCacheFile(storageId, path, "list").delete()
        getCacheFile(storageId, path, "info").delete()

        // 如果是文件，删除其父目录的列表缓存
        if (!isDirectory) {
            val parentPath = if (path.contains("/")) {
                path.substring(0, path.lastIndexOf("/"))
            } else {
                "/"
            }
            getCacheFile(storageId, parentPath, "list").delete()
        }
    }

    /**
     * 清除指定存储的所有缓存
     */
    suspend fun clearStorageCache(storageId: String) = withContext(Dispatchers.IO) {
        val storageDir = File(cacheDir, storageId)
        if (storageDir.exists() && storageDir.isDirectory) {
            storageDir.deleteRecursively()
        }
    }

    /**
     * 清除所有缓存
     */
    suspend fun clearAllCache() = withContext(Dispatchers.IO) {
        if (cacheDir.exists() && cacheDir.isDirectory) {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
        }
    }

    /**
     * 获取缓存文件
     */
    private fun getCacheFile(storageId: String, path: String, type: String): File {
        // 处理路径，将 '/' 替换为 '_' 以避免创建多级目录
        val safePath = path.replace("/", "_").takeIf { it.isNotEmpty() } ?: "root"
        val storageDir = File(cacheDir, storageId).apply {
            if (!exists()) mkdirs()
        }
        return File(storageDir, "${safePath}_$type.cache")
    }

    /**
     * 缓存数据结构
     */
    @OptIn(InternalSerializationApi::class)
    @Serializable
    private data class CacheData<T>(
        val timestamp: Long,
        val data: T
    )

}
