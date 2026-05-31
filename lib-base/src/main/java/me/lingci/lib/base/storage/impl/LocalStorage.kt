package me.lingci.lib.base.storage.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import me.lingci.lib.base.storage.IStorage
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.storage.entity.StorageConfig
import me.lingci.lib.base.storage.entity.StorageType
import me.lingci.lib.base.util.notExists
import java.io.File

/**
 * 本地文件系统实现
 */
class LocalStorage(
    private val config: StorageConfig.LocalStorageConfig
) : IStorage {

    private val rootFile = File(config.rootPath)

    override fun rootFile(): FileEntity {
        return FileEntity(
            name = "主页",
            path = config.rootPath,
            type = StorageType.LOCAL_STORAGE
        )
    }

    override fun fullPath(path: String): String {
        return path
    }

    override fun getToken(): String {
        return ""
    }

    override suspend fun testConnect(): Boolean = withContext(Dispatchers.IO) {
        return@withContext true
    }

    override suspend fun listFiles(path: String, refresh: Boolean): Flow<List<FileEntity>> = flow {
        val targetFile = if (path.isEmpty() || path == "/") rootFile else File(path)

        if (targetFile.exists() && targetFile.isDirectory) {
            val files = targetFile.listFiles()?.mapNotNull { fileToEntity(it) } ?: emptyList()
            emit(files)
        } else {
            emit(emptyList())
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun listFile(path: String, refresh: Boolean): Flow<FileEntity> = flow {
        val targetFile = if (path.isEmpty() || path == "/") rootFile else File(path)

        if (targetFile.exists() && targetFile.isDirectory) {
            targetFile.listFiles()?.forEach { emit(fileToEntity(it)) }
        }
    }.flowOn(Dispatchers.IO)

    override suspend fun getFileInfo(path: String): FileEntity? = withContext(Dispatchers.IO) {
        val file = File(path)
        return@withContext if (file.exists()) fileToEntity(file) else null
    }

    override suspend fun createDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        val dir = File(path)
        return@withContext if (dir.exists()) dir.isDirectory else dir.mkdirs()
    }

    override suspend fun rename(oldPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        val oldFile = File(oldPath)
        if (!oldFile.exists()) return@withContext false

        val parent = oldFile.parentFile ?: return@withContext false
        val newFile = File(parent, newName)

        return@withContext oldFile.renameTo(newFile)
    }

    override suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        val file = File(path)
        if (file.notExists()) return@withContext true
        return@withContext if (file.isDirectory) deleteDirectory(file) else file.delete()
    }

    override suspend fun copy(
        sourcePath: String,
        targetPath: String,
        progressCallback: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        val sourceFile = File(sourcePath)
        val targetFile = File(targetPath)

        if (!sourceFile.exists()) return@withContext false

        return@withContext if (sourceFile.isDirectory) {
            copyDirectory(sourceFile, targetFile, progressCallback)
        } else {
            copyFile(sourceFile, targetFile, progressCallback)
        }
    }

    override suspend fun move(
        sourcePath: String,
        targetPath: String,
        progressCallback: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        // 先复制再删除实现移动
        val copySuccess = copy(sourcePath, targetPath, progressCallback)
        if (copySuccess) {
            return@withContext delete(sourcePath)
        }
        return@withContext false
    }

    override suspend fun download(
        remotePath: String,
        localPath: String,
        progressCallback: (Float) -> Unit
    ): Boolean {
        return true
    }

    override suspend fun upload(
        localPath: String,
        remotePath: String,
        progressCallback: (Float) -> Unit
    ): Boolean {
        return true
    }

    override fun searchFiles(query: String, rootPath: String): Flow<List<FileEntity>> = flow {
        val root = if (rootPath.isEmpty() || rootPath == "/") rootFile else File(rootFile, rootPath)
        val results = mutableListOf<FileEntity>()

        searchDirectory(root, query, results)
        emit(results)
    }.flowOn(Dispatchers.IO)

    override fun release() {
        // 本地文件系统不需要释放特殊资源
    }

    // 辅助方法：将File转换为FileEntity
    private fun fileToEntity(file: File): FileEntity {
        return FileEntity(
            id = file.absolutePath,
            name = file.name,
            path = file.path,
            fullPath = file.path,
            isFile = file.isFile,
            size = if (file.isDirectory) 0 else file.length(),
            lastModified = file.lastModified(),
            type = config.type,
            storageId = config.id,
            mimeType = getMimeType(file.absolutePath)
        )
    }

    // 获取相对路径
    private fun getRelativePath(file: File): String {
        return rootFile.toURI().relativize(file.toURI()).path
    }

    // 获取MIME类型
    private fun getMimeType(filePath: String): String {
        return try {
            ""
        } catch (e: Exception) {
            ""
        }
    }

    // 删除目录（递归）
    private fun deleteDirectory(dir: File): Boolean {
        if (dir.isDirectory) {
            val children = dir.listFiles() ?: return true
            for (child in children) {
                val success = deleteDirectory(child)
                if (!success) return false
            }
        }
        return dir.delete()
    }

    // 复制目录（递归）
    private fun copyDirectory(
        source: File,
        target: File,
        progressCallback: (Float) -> Unit
    ): Boolean {
        if (!source.isDirectory) return false
        if (!target.exists() && !target.mkdirs()) return false

        val files = source.listFiles() ?: return true
        var total = files.size.toFloat()
        var copied = 0f

        for (file in files) {
            val sourceFile = file
            val targetFile = File(target, file.name)

            if (sourceFile.isDirectory) {
                if (!copyDirectory(sourceFile, targetFile, progressCallback)) return false
            } else {
                if (!copyFile(sourceFile, targetFile) {}) return false
            }

            copied++
            progressCallback(copied / total)
        }
        return true
    }

    // 复制文件
    private fun copyFile(
        source: File,
        target: File,
        progressCallback: (Float) -> Unit = {}
    ): Boolean {
        return try {
            source.inputStream().use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    val total = source.length()
                    var copied = 0L

                    var bytesRead: Int
                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        copied += bytesRead
                        progressCallback((copied.toFloat() / total).coerceAtMost(1.0f))
                    }
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }

    // 搜索目录
    private fun searchDirectory(dir: File, query: String, results: MutableList<FileEntity>) {
        if (!dir.exists() || !dir.isDirectory) return

        val files = dir.listFiles() ?: return
        for (file in files) {
            if (file.name.contains(query, ignoreCase = true)) {
                fileToEntity(file).let { results.add(it) }
            }

            if (file.isDirectory) {
                searchDirectory(file, query, results)
            }
        }
    }

}
