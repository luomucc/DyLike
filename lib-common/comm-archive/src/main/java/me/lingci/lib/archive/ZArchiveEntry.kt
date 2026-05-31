package me.lingci.lib.archive

/**
 *   @author : happyc
 *   time    : 2025/12/14
 *   desc    : 压缩文件
 *   version : 1.0
 */
data class ZArchiveEntry(
    var name: String,
    var isDirectory: Boolean = false,
    var size: Long = 0
) {
}

