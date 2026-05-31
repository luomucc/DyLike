package me.lingci.lib.base.util

import java.io.File
import java.io.RandomAccessFile
import java.security.MessageDigest

/**
 *   @author : happyc
 *   time    : 2026/02/03
 *   desc    :
 *   version : 1.0
 */
object HashHelper {

    /**
     * 获取文件的动态采样唯一标识
     * 策略：根据文件大小动态决定采样点数量和块大小
     */
    fun getDynamicHash(filePath: String): String? {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return null
        val fileSize = file.length()
        // 1. 如果文件小于 1MB，直接计算全量 MD5，既快又准
        if (fileSize < 1024 * 1024) {
            return getFullFileMd5(file)
        }
        return try {
            val digest = MessageDigest.getInstance("MD5")
            val raf = RandomAccessFile(file, "r")
            // 2. 动态计算采样参数 根据文件大小决定：每个块读多大 (chunkSize)，读多少个点 (pointCount)
            val (chunkSize, pointCount) = when {
                fileSize < 100 * 1024 * 1024 -> 32 * 1024 to 3   // <100MB: 32KB * 3点
                fileSize < 1024 * 1024 * 1024 -> 64 * 1024 to 5  // <1GB: 64KB * 5点
                else -> 128 * 1024 to 7                          // >1GB: 128KB * 7点
            }
            val buffer = ByteArray(chunkSize)
            // 3. 多点均匀采样
            for (i in 0 until pointCount) {
                // 计算采样点位置：0%, 25%, 50%...
                val pos = (fileSize - chunkSize) * i / (pointCount - 1)
                raf.seek(pos)
                val readBytes = raf.read(buffer)
                if (readBytes > 0) {
                    digest.update(buffer, 0, readBytes)
                }
            }
            // 4. 关键：将文件大小和最后修改时间也混入计算，防止“人造碰撞”
            digest.update(fileSize.toString().toByteArray())
            //digest.update(file.lastModified().toString().toByteArray())
            raf.close()
            // 转换为 16 进制
            digest.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getFullFileMd5(file: File): String {
        val digest = MessageDigest.getInstance("MD5")
        file.inputStream().use { input ->
            val buffer = ByteArray(8192)
            var bytesRead: Int
            while (input.read(buffer).also { bytesRead = it } != -1) {
                digest.update(buffer, 0, bytesRead)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

}