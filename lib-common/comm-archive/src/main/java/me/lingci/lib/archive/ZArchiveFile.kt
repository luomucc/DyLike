package me.lingci.lib.archive

import me.lingci.lib.base.util.Log
import net.sf.sevenzipjbinding.ExtractAskMode
import net.sf.sevenzipjbinding.ExtractOperationResult
import net.sf.sevenzipjbinding.IArchiveExtractCallback
import net.sf.sevenzipjbinding.IInArchive
import net.sf.sevenzipjbinding.ISequentialOutStream
import net.sf.sevenzipjbinding.PropID
import net.sf.sevenzipjbinding.SevenZip
import net.sf.sevenzipjbinding.impl.RandomAccessFileInStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.Closeable
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * 封装类，用于管理 RandomAccessFile 和 IInArchive 的生命周期
 * 解决过早关闭流导致的 Native Crash 问题
 */
private class ArchiveHandle(
    private val randomAccessFile: RandomAccessFile,
    val archive: IInArchive
) : Closeable {

    override fun close() {
        try {
            archive.close()
        } catch (e: Exception) {
            Log.d(this, "Error closing archive", e)
        }
        try {
            randomAccessFile.close()
        } catch (e: Exception) {
            Log.d(this, "Error closing RAF", e)
        }
    }
}

object ZArchiveFile {

    private fun isPasswordError(error: Throwable): Boolean {
        val message = error.message?.lowercase().orEmpty()
        return message.contains("password") || message.contains("encrypted")
    }

    fun listZEntry(zPath: String, password: String): List<ZArchiveEntry> {
        val lowerPath = zPath.lowercase()
        return when {
            lowerPath.endsWith(".zip") -> listSevenZEntry(zPath, password)
            lowerPath.endsWith(".7z") -> listSevenZEntry(zPath, password)
            lowerPath.endsWith(".rar") -> RarFile.listZEntry(zPath, password)
            else -> emptyList()
        }
    }

    private fun openArchiveHandle(path: String, password: String?): ArchiveHandle? {
        return try {
            // 使用 "rw" 模式以适应某些格式的需求，尽管通常 "r" 也可
            val raf = RandomAccessFile(path, "rw")
            val inStream = RandomAccessFileInStream(raf)
            // 将密码逻辑统一放在 openInArchive 中
            val archive = SevenZip.openInArchive(null, inStream, password)
            ArchiveHandle(raf, archive)
        } catch (e: Exception) {
            Log.d(this, "openArchiveHandle failed", e)
            null
        }
    }

    fun listSevenZEntry(zipPath: String, password: String): List<ZArchiveEntry> {
        val list = mutableListOf<ZArchiveEntry>()
        // 使用自定义的 Handle 管理生命周期，确保流在读取完毕后才关闭
        openArchiveHandle(zipPath, password.ifBlank { null })?.use { handle ->
            val inArchive = handle.archive
            for (i in 0 until inArchive.numberOfItems) {
                // 安全获取 Size，防止空指针崩溃
                val sizeStr = inArchive.getStringProperty(i, PropID.SIZE)
                val size = sizeStr?.toLongOrNull() ?: 0L

                list.add(
                    ZArchiveEntry(
                        name = TextFixer.fixGarbledFilename(
                            inArchive.getStringProperty(i, PropID.PATH)
                        ),
                        isDirectory = inArchive.getProperty(i, PropID.IS_FOLDER) as? Boolean ?: false,
                        size = size
                    )
                )
            }
        }
        return list
    }

    fun checkPassword(zPath: String): Boolean {
        val lowerPath = zPath.lowercase()
        if (lowerPath.endsWith(".rar")) {
            return RarFile.checkPassword(zPath)
        }
        return try {
            var isEncrypted = false
            openArchiveHandle(zPath, null)?.use { handle ->
                val inArchive = handle.archive
                // 检查压缩包是否加密
                // 方法1：尝试获取属性 (部分格式支持，更高效)
                // 方法2：尝试解压一个小文件 (通用兜底)

                // 优先检查文件属性，部分格式直接支持判断
                // 如果属性判断不准，再尝试解压第一个文件

                var fileIndex = -1
                for (i in 0 until inArchive.numberOfItems) {
                    val isFolder = inArchive.getProperty(i, PropID.IS_FOLDER) as? Boolean ?: false
                    if (!isFolder) {
                        fileIndex = i
                        break
                    }
                }

                if (fileIndex == -1) {
                    // 空压缩包，认为不需要密码
                    return false
                }

                try {
                    // 尝试不带密码解压，通过回调捕获错误
                    val callback = object : IArchiveExtractCallback {
                        override fun getStream(index: Int, extractAskMode: ExtractAskMode?): ISequentialOutStream {
                            // 丢弃数据流，仅做测试
                            return ISequentialOutStream { data -> data?.size ?: 0 }
                        }

                        override fun prepareOperation(extractAskMode: ExtractAskMode?) {}
                        override fun setOperationResult(extractOperationResult: ExtractOperationResult?) {
                            if (extractOperationResult == ExtractOperationResult.WRONG_PASSWORD) {
                                // 明确的密码错误标志
                                isEncrypted = true
                            }
                        }
                        override fun setTotal(total: Long) {}
                        override fun setCompleted(completed: Long) {}
                    }
                    inArchive.extract(intArrayOf(fileIndex), false, callback)
                } catch (e: Exception) {
                    // 捕获 Native 层抛出的密码异常
                    if (isPasswordError(e)) {
                        isEncrypted = true
                    } else {
                        // 如果是其他异常（如 CRC 错误、文件损坏），不应视为需要密码
                        // 这里的逻辑取决于业务需求，但通常文件损坏应抛出异常或返回 false
                        Log.d(this, "Check password error (corrupted?)", e)
                        return false
                    }
                }
            }
            isEncrypted
        } catch (e: Exception) {
            Log.d(this, "checkPassword failed", e)
            false
        }
    }

    fun verifyPassword(zPath: String, password: String): ArchivePasswordVerifyResult {
        val lowerPath = zPath.lowercase()
        return when {
            lowerPath.endsWith(".zip") -> verifySevenZPassword(zPath, password)
            lowerPath.endsWith(".7z") -> verifySevenZPassword(zPath, password)
            lowerPath.endsWith(".rar") -> RarFile.verifyPassword(zPath, password)
            else -> ArchivePasswordVerifyResult.ERROR
        }
    }

    fun openZEntryResult(zPath: String, password: String, entryName: String): ArchiveEntryOpenResult {
        val lowerPath = zPath.lowercase()
        return when {
            lowerPath.endsWith(".zip") -> openSevenZEntryResult(zPath, password, entryName)
            lowerPath.endsWith(".7z") -> openSevenZEntryResult(zPath, password, entryName)
            lowerPath.endsWith(".rar") -> RarFile.openZEntryResult(zPath, password, entryName)
            else -> ArchiveEntryOpenResult.Error
        }
    }

    fun openZEntry(zPath: String, password: String, entryName: String): InputStream? {
        return when (val result = openZEntryResult(zPath, password, entryName)) {
            is ArchiveEntryOpenResult.Success -> result.stream
            else -> null
        }
    }

    private fun verifySevenZPassword(
        zipPath: String,
        password: String
    ): ArchivePasswordVerifyResult {
        return openArchiveHandle(zipPath, password.ifBlank { null })?.use { handle ->
            val inArchive = handle.archive
            val fileIndex = (0 until inArchive.numberOfItems).firstOrNull {
                (inArchive.getProperty(it, PropID.IS_FOLDER) as? Boolean) == false
            } ?: return@use ArchivePasswordVerifyResult.ENTRY_NOT_FOUND

            var status = ArchivePasswordVerifyResult.ERROR
            val callback = object : IArchiveExtractCallback {
                override fun getStream(index: Int, extractAskMode: ExtractAskMode?) =
                    ISequentialOutStream { data -> data?.size ?: 0 }

                override fun prepareOperation(extractAskMode: ExtractAskMode?) {}

                override fun setOperationResult(extractOperationResult: ExtractOperationResult?) {
                    status = when (extractOperationResult) {
                        ExtractOperationResult.OK -> ArchivePasswordVerifyResult.VALID
                        ExtractOperationResult.WRONG_PASSWORD -> ArchivePasswordVerifyResult.WRONG_PASSWORD
                        ExtractOperationResult.UNSUPPORTEDMETHOD -> ArchivePasswordVerifyResult.UNSUPPORTED_METHOD
                        else -> ArchivePasswordVerifyResult.ERROR
                    }
                }

                override fun setTotal(total: Long) {}
                override fun setCompleted(completed: Long) {}
            }

            try {
                inArchive.extract(intArrayOf(fileIndex), false, callback)
            } catch (e: Exception) {
                return@use if (isPasswordError(e)) {
                    ArchivePasswordVerifyResult.WRONG_PASSWORD
                } else {
                    Log.d(this, "verify password failed", e)
                    ArchivePasswordVerifyResult.ERROR
                }
            }

            status
        } ?: ArchivePasswordVerifyResult.ERROR
    }

    private fun openSevenZEntryResult(
        zipPath: String,
        password: String,
        entryName: String
    ): ArchiveEntryOpenResult {
        Log.d(this, "open", zipPath, entryName)

        return openArchiveHandle(zipPath, password.ifBlank { null })?.use { handle ->
            val inArchive = handle.archive
            var targetIndex = -1

            // 查找目标文件
            for (i in 0 until inArchive.numberOfItems) {
                val path = TextFixer.fixGarbledFilename(inArchive.getStringProperty(i, PropID.PATH))
                if (path == entryName) {
                    targetIndex = i
                    break
                }
            }

            if (targetIndex == -1) {
                Log.d(this, "Entry not found", entryName)
                return@use ArchiveEntryOpenResult.EntryNotFound
            }

            // 警告：对于大文件，ByteArrayOutputStream 会消耗大量内存
            // 生产环境建议改为解压到临时文件，返回 FileInputStream
            val output = ByteArrayOutputStream()
            var operationResult: ExtractOperationResult? = null

            val callback = object : IArchiveExtractCallback {
                override fun getStream(index: Int, extractAskMode: ExtractAskMode?): ISequentialOutStream {
                    return ISequentialOutStream { data ->
                        data?.let {
                            output.write(it)
                        }
                        data?.size ?: 0
                    }
                }

                override fun prepareOperation(extractAskMode: ExtractAskMode?) {}
                override fun setOperationResult(extractOperationResult: ExtractOperationResult?) {
                    operationResult = extractOperationResult
                    if (extractOperationResult == ExtractOperationResult.OK) {
                        Log.d(this, "Extract success, size: ${output.size()}")
                    } else if (extractOperationResult == ExtractOperationResult.WRONG_PASSWORD) {
                        Log.d(this, "Wrong password")
                    } else {
                        Log.d(this, "Extract failed: ${extractOperationResult?.name}")
                    }
                }

                override fun setTotal(total: Long) {}
                override fun setCompleted(complete: Long) {}
            }

            try {
                inArchive.extract(intArrayOf(targetIndex), false, callback)
            } catch (e: Exception) {
                Log.d(this, "Extract exception", e)
                return@use if (isPasswordError(e)) {
                    ArchiveEntryOpenResult.WrongPassword
                } else {
                    ArchiveEntryOpenResult.Error
                }
            }

            when (operationResult) {
                ExtractOperationResult.OK -> {
                    if (output.size() > 0) {
                        ArchiveEntryOpenResult.Success(ByteArrayInputStream(output.toByteArray()))
                    } else {
                        ArchiveEntryOpenResult.Error
                    }
                }

                ExtractOperationResult.WRONG_PASSWORD -> ArchiveEntryOpenResult.WrongPassword
                ExtractOperationResult.UNSUPPORTEDMETHOD -> ArchiveEntryOpenResult.UnsupportedMethod
                else -> ArchiveEntryOpenResult.Error
            }
        } ?: ArchiveEntryOpenResult.Error
    }

}
