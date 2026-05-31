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
import java.io.InputStream
import java.io.RandomAccessFile

/**
 * 最终优化版
 * 解决 RAR 无法随机读取第二个文件的问题
 * 原理：利用 Extract 回调机制，按解压器的顺序“拦截”数据流，而不是强制寻址。
 */
object RarFile {

    private fun isPasswordError(error: Throwable): Boolean {
        val message = error.message?.lowercase().orEmpty()
        return message.contains("password") || message.contains("encrypted")
    }

    /**
     * 一次性打开 Archive，自动管理资源生命周期
     */
    private inline fun <T> withArchive(path: String, password: String?, block: (IInArchive) -> T?): T? {
        return try {
            // 注意：这里不能在 block 执行完就 close，必须等 extract 结束
            // 但为了防止内存泄漏，这里使用 RAII 模式，block 内部必须同步执行完 extract
            val raf = RandomAccessFile(path, "r")
            val inStream = RandomAccessFileInStream(raf)
            val archive = SevenZip.openInArchive(null, inStream, password)

            try {
                block(archive)
            } finally {
                // 确保 block 执行完（解压结束）后再关闭
                try { archive.close() } catch (e: Exception) { Log.d(this, "close archive error", e) }
                try { raf.close() } catch (e: Exception) { Log.d(this, "close raf error", e) }
            }
        } catch (e: Exception) {
            Log.d(this, "withArchive failed", e)
            null
        }
    }

    fun listZEntry(zPath: String, password: String): List<ZArchiveEntry> {
        return withArchive(zPath, password.ifBlank { null }) { archive ->
            val list = mutableListOf<ZArchiveEntry>()
            for (i in 0 until archive.numberOfItems) {
                list.add(
                    ZArchiveEntry(
                        name = TextFixer.fixGarbledFilename(archive.getStringProperty(i, PropID.PATH)),
                        isDirectory = archive.getProperty(i, PropID.IS_FOLDER) as? Boolean ?: false,
                        size = archive.getStringProperty(i, PropID.SIZE)?.toLongOrNull() ?: 0L
                    )
                )
            }
            list
        } ?: emptyList()
    }

    fun checkPassword(zPath: String): Boolean {
        return withArchive(zPath, null) { archive ->
            var isEncrypted = false
            // 找第一个非文件夹文件进行测试
            val testIndex = (0 until archive.numberOfItems).find {
                archive.getProperty(it, PropID.IS_FOLDER) as? Boolean == false
            } ?: return@withArchive false

            val callback = object : IArchiveExtractCallback {
                override fun getStream(index: Int, extractAskMode: ExtractAskMode?) = ISequentialOutStream { it?.size ?: 0 }
                override fun setOperationResult(result: ExtractOperationResult?) {
                    if (result == ExtractOperationResult.WRONG_PASSWORD) isEncrypted = true
                }
                override fun prepareOperation(extractAskMode: ExtractAskMode?) {}
                override fun setTotal(total: Long) {}
                override fun setCompleted(completed: Long) {}
            }

            try {
                archive.extract(intArrayOf(testIndex), false, callback)
            } catch (e: Exception) {
                if (isPasswordError(e)) isEncrypted = true
            }
            isEncrypted
        } ?: false
    }

    fun verifyPassword(zPath: String, password: String): ArchivePasswordVerifyResult {
        return withArchive(zPath, password.ifBlank { null }) { archive ->
            val testIndex = (0 until archive.numberOfItems).find {
                archive.getProperty(it, PropID.IS_FOLDER) as? Boolean == false
            } ?: return@withArchive ArchivePasswordVerifyResult.ENTRY_NOT_FOUND

            var status = ArchivePasswordVerifyResult.ERROR
            val callback = object : IArchiveExtractCallback {
                override fun getStream(index: Int, extractAskMode: ExtractAskMode?) =
                    ISequentialOutStream { data -> data?.size ?: 0 }

                override fun setOperationResult(result: ExtractOperationResult?) {
                    status = when (result) {
                        ExtractOperationResult.OK -> ArchivePasswordVerifyResult.VALID
                        ExtractOperationResult.WRONG_PASSWORD -> ArchivePasswordVerifyResult.WRONG_PASSWORD
                        ExtractOperationResult.UNSUPPORTEDMETHOD -> ArchivePasswordVerifyResult.UNSUPPORTED_METHOD
                        else -> ArchivePasswordVerifyResult.ERROR
                    }
                }

                override fun prepareOperation(extractAskMode: ExtractAskMode?) {}
                override fun setTotal(total: Long) {}
                override fun setCompleted(completed: Long) {}
            }

            try {
                archive.extract(intArrayOf(testIndex), false, callback)
            } catch (e: Exception) {
                return@withArchive if (isPasswordError(e)) {
                    ArchivePasswordVerifyResult.WRONG_PASSWORD
                } else {
                    Log.d(this, "verify password failed", e)
                    ArchivePasswordVerifyResult.ERROR
                }
            }

            status
        } ?: ArchivePasswordVerifyResult.ERROR
    }

    fun openZEntry(zPath: String, password: String, entryName: String): InputStream? {
        return when (val result = openZEntryResult(zPath, password, entryName)) {
            is ArchiveEntryOpenResult.Success -> result.stream
            else -> null
        }
    }

    fun openZEntryResult(zPath: String, password: String, entryName: String): ArchiveEntryOpenResult {
        return withArchive(zPath, password.ifBlank { null }) { archive ->
            var result: ArchiveEntryOpenResult = ArchiveEntryOpenResult.Error

            // 1. 查找目标索引
            val targetIndex = (0 until archive.numberOfItems).find { i ->
                TextFixer.fixGarbledFilename(archive.getStringProperty(i, PropID.PATH)) == entryName
            }

            if (targetIndex == null) {
                Log.d(this, "Entry not found: $entryName")
                return@withArchive ArchiveEntryOpenResult.EntryNotFound
            }

            // 2. 关键：准备拦截数据的 ByteArrayOutputStream
            // 避免内存溢出建议：此处可替换为 FileOutputStream 写入临时文件
            val buffer = ByteArrayOutputStream()

            // 3. 定义回调：当解压器读到数据时，写入 buffer
            val callback = object : IArchiveExtractCallback {
                override fun getStream(index: Int, extractAskMode: ExtractAskMode?): ISequentialOutStream {
                    return ISequentialOutStream { data ->
                        if (index == targetIndex) {
                            data?.let { buffer.write(it) }
                        }
                        data?.size ?: 0
                    }
                }

                override fun setOperationResult(operationResult: ExtractOperationResult?) {
                    if (operationResult == ExtractOperationResult.OK && buffer.size() > 0) {
                        result = ArchiveEntryOpenResult.Success(ByteArrayInputStream(buffer.toByteArray()))
                    } else if (operationResult == ExtractOperationResult.WRONG_PASSWORD) {
                        Log.d(this, "Wrong password for $entryName")
                        result = ArchiveEntryOpenResult.WrongPassword
                    } else if (operationResult == ExtractOperationResult.UNSUPPORTEDMETHOD) {
                        Log.d(this, "Unsupported method for $entryName")
                        result = ArchiveEntryOpenResult.UnsupportedMethod
                    } else {
                        result = ArchiveEntryOpenResult.Error
                    }
                }

                override fun prepareOperation(extractAskMode: ExtractAskMode?) {}
                override fun setTotal(total: Long) {}
                override fun setCompleted(completed: Long) {}
            }

            // 4. 执行解压
            // extract 方法会处理 RAR 的复杂指针跳动，我们只需静静等待回调结果
            try {
                archive.extract(intArrayOf(targetIndex), false, callback)
            } catch (e: Exception) {
                Log.d(this, "Extract failed", e)
                result = if (isPasswordError(e)) {
                    ArchiveEntryOpenResult.WrongPassword
                } else {
                    ArchiveEntryOpenResult.Error
                }
            }

            result
        } ?: ArchiveEntryOpenResult.Error
    }

}
