package me.lingci.lib.base.storage.impl

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import jcifs.CIFSContext
import jcifs.context.BaseContext
import jcifs.context.SingletonContext
import jcifs.smb.NtlmPasswordAuthenticator
import jcifs.smb.SmbFile
import me.lingci.lib.base.storage.IStorage
import me.lingci.lib.base.storage.entity.FileEntity
import me.lingci.lib.base.storage.entity.StorageConfig
import me.lingci.lib.base.util.md5
import java.io.File
import java.util.*

/**
 * SMB文件系统实现
 * 依赖库：jcifs-ng (https://github.com/AgNO3/jcifs-ng)
 */

class SmbStorage(
    private val config: StorageConfig.SmbStorageConfig,
    private val localCacheDir: String // 本地缓存目录
) : IStorage {

    // SMB上下文
    private val cifsContext: CIFSContext by lazy {
        val baseCtx = SingletonContext.getInstance()

        if (config.username.isNullOrBlank() || config.password.isNullOrBlank()) {
            baseCtx
        } else {
            val auth = NtlmPasswordAuthenticator(
                config.domain,
                config.username,
                config.password
            )
            baseCtx.withCredentials(auth)
        }
    }

    // SMB根路径
    private val rootPath: String by lazy {
        "smb://${config.server}:${config.port}/${config.share}/"
    }

    override fun rootFile(): FileEntity {
        TODO("Not yet implemented")
    }

    override fun fullPath(path: String): String {
        TODO("Not yet implemented")
    }

    override fun getToken(): String {
        TODO("Not yet implemented")
    }

    override suspend fun testConnect(): Boolean {
        TODO("Not yet implemented")
    }

    override suspend fun listFiles(path: String, refresh: Boolean): Flow<List<FileEntity>> = flow {
        val smbPath = getSmbPath(path)

        val fileList = mutableListOf<FileEntity>()
        try {
            val smbFile = SmbFile(smbPath, cifsContext)

            if (smbFile.exists() && smbFile.isDirectory) {
                val files = smbFile.listFiles()?.mapNotNull { smbFileToEntity(it) }?.filter { true } ?: emptyList()
                emit(files)
            } else {
                emit(emptyList())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        emit(fileList)
    }.flowOn(Dispatchers.IO)

    override suspend fun listFile(
        path: String,
        refresh: Boolean
    ): Flow<FileEntity> {
        TODO("Not yet implemented")
    }

    override suspend fun getFileInfo(path: String): FileEntity? = withContext(Dispatchers.IO) {
        return@withContext try {
            val smbFile = SmbFile(getSmbPath(path), cifsContext)
            if (smbFile.exists()) smbFileToEntity(smbFile) else null
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    override suspend fun createDirectory(path: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val smbFile = SmbFile(getSmbPath(path), cifsContext)
            if (smbFile.exists()) {
                smbFile.isDirectory
            } else {
                smbFile.mkdirs()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun rename(oldPath: String, newName: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val oldFile = SmbFile(getSmbPath(oldPath), cifsContext)
            if (!oldFile.exists()) return@withContext false

            val parentPath = oldFile.parent ?: return@withContext false
            val newFile = SmbFile("$parentPath/$newName", cifsContext)

            oldFile.renameTo(newFile)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun delete(path: String): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val smbFile = SmbFile(getSmbPath(path), cifsContext)
            if (!smbFile.exists()) return@withContext true

            if (smbFile.isDirectory) {
                deleteDirectory(smbFile)
            } else {
                smbFile.delete()
                true
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun copy(
        sourcePath: String,
        targetPath: String,
        progressCallback: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val sourceFile = SmbFile(getSmbPath(sourcePath), cifsContext)
            val targetFile = SmbFile(getSmbPath(targetPath), cifsContext)

            if (!sourceFile.exists()) return@withContext false

            if (sourceFile.isDirectory) {
                copyDirectory(sourceFile, targetFile, progressCallback)
            } else {
                copyFile(sourceFile, targetFile, progressCallback)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            false
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
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val remoteFile = SmbFile(getSmbPath(remotePath), cifsContext)
            val localFile = File(localPath)

            if (!remoteFile.exists() || remoteFile.isDirectory) return@withContext false

            // 确保父目录存在
            localFile.parentFile?.mkdirs()

            copyFromSmbToLocal(remoteFile, localFile, progressCallback)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override suspend fun upload(
        localPath: String,
        remotePath: String,
        progressCallback: (Float) -> Unit
    ): Boolean = withContext(Dispatchers.IO) {
        return@withContext try {
            val localFile = File(localPath)
            val remoteFile = SmbFile(getSmbPath(remotePath), cifsContext)

            if (!localFile.exists() || localFile.isDirectory) return@withContext false

            copyFromLocalToSmb(localFile, remoteFile, progressCallback)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    override fun searchFiles(query: String, rootPath: String): Flow<List<FileEntity>> = flow {
        val results = mutableListOf<FileEntity>()
        searchSmbDirectory(getSmbPath(rootPath), query, results)
        emit(results)
    }.flowOn(Dispatchers.IO)

    override fun release() {
        // 释放SMB资源
        try {
            (cifsContext as? BaseContext)?.close()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    // 辅助方法：将SmbFile转换为FileEntity
    private fun smbFileToEntity(smbFile: SmbFile): FileEntity? {
        return try {
            FileEntity(
                id = smbFile.path.md5(),
                name = smbFile.name,
                path = getRelativePath(smbFile),
                isFile = smbFile.isDirectory.not(),
                size = if (smbFile.isDirectory) 0 else smbFile.length(),
                lastModified = smbFile.lastModified(),
                type = config.type,
                storageId = config.id,
                mimeType = "${getMimeType(smbFile.name)}"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    // 获取SMB路径
    private fun getSmbPath(relativePath: String): String {
        return if (relativePath.isEmpty() || relativePath == "/") {
            rootPath
        } else {
            val adjustedPath = relativePath.trimStart('/')
            "$rootPath$adjustedPath"
        }.replace("//", "/").replace("/", "/")
    }

    // 获取相对路径
    private fun getRelativePath(smbFile: SmbFile): String {
        val url = smbFile.path.replace(rootPath, "")
        return url.ifEmpty { "/" }
    }

    // 获取MIME类型
    private fun getMimeType(fileName: String): String? {
        val extension = fileName.substringAfterLast('.', "").lowercase()
        return when (extension) {
            "txt" -> "text/plain"
            "pdf" -> "application/pdf"
            "doc", "docx" -> "application/msword"
            "xls", "xlsx" -> "application/vnd.ms-excel"
            "ppt", "pptx" -> "application/vnd.ms-powerpoint"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "mp3" -> "audio/mpeg"
            "mp4" -> "video/mp4"
            else -> null
        }
    }

    // 删除目录（递归）
    private fun deleteDirectory(dir: SmbFile): Boolean {
        return try {
            if (!dir.isDirectory) return false

            val files = dir.listFiles() ?: return true
            for (file in files) {
                if (file.isDirectory) {
                    val success = deleteDirectory(file)
                    if (!success) return false
                } else {
                    file.delete()
                }
            }
            dir.delete()
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 复制目录（递归）
    private fun copyDirectory(
        source: SmbFile,
        target: SmbFile,
        progressCallback: (Float) -> Unit
    ): Boolean {
        return try {
            if (!source.isDirectory) return false
            if (!target.exists()) return false

            val files = source.listFiles() ?: return true
            var total = files.size.toFloat()
            var copied = 0f

            for (file in files) {
                val targetFile = SmbFile(target.path + "/" + file.name, cifsContext)

                if (file.isDirectory) {
                    if (!copyDirectory(file, targetFile, progressCallback)) return false
                } else {
                    if (!copyFile(file, targetFile) {}) return false
                }

                copied++
                progressCallback(copied / total)
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    // 复制SMB文件
    private fun copyFile(
        source: SmbFile,
        target: SmbFile,
        progressCallback: (Float) -> Unit = {}
    ): Boolean {
        return try {
            source.inputStream.use { input ->
                target.outputStream.use { output ->
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
            e.printStackTrace()
            false
        }
    }

    // 从SMB复制到本地
    private fun copyFromSmbToLocal(
        source: SmbFile,
        target: File,
        progressCallback: (Float) -> Unit
    ) {
        source.inputStream.use { input ->
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
    }

    // 从本地复制到SMB
    private fun copyFromLocalToSmb(
        source: File,
        target: SmbFile,
        progressCallback: (Float) -> Unit
    ) {
        source.inputStream().use { input ->
            target.outputStream.use { output ->
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
    }

    // 搜索SMB目录
    private fun searchSmbDirectory(smbPath: String, query: String, results: MutableList<FileEntity>) {
        try {
            val dir = SmbFile(smbPath, cifsContext)
            if (!dir.exists() || !dir.isDirectory) return

            val files = dir.listFiles() ?: return
            for (file in files) {
                if (file.name.contains(query, ignoreCase = true)) {
                    smbFileToEntity(file)?.let { results.add(it) }
                }

                if (file.isDirectory) {
                    searchSmbDirectory(file.path, query, results)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}