package me.lingci.lib.player.chapter

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MP4/M4A/M4B 章节解析器
 * 支持 QuickTime 和 Nero 两种章节格式
 *
 * QuickTime 章节位置: moov.udta.chpl
 * Nero 章节位置: meta 或自定义 atom
 *
 * Atom 结构:
 * - size (4 bytes): atom 大小
 * - type (4 bytes): atom 类型 (ASCII)
 * - 如果 size == 1: extended size (8 bytes)
 * - 如果 size == 0: 到文件末尾
 */
object Mp4ChapterParser {

    // Atom 类型标识
    private val ATOM_MOOV = "moov".toByteArray()
    private val ATOM_UDTA = "udta".toByteArray()
    private val ATOM_CHPL = "chpl".toByteArray()
    private val ATOM_META = "meta".toByteArray()
    private val ATOM_ILST = "ilst".toByteArray()
    private val ATOM_CHAP = "chap".toByteArray()
    private val ATOM_TRAK = "trak".toByteArray()
    private val ATOM_TKHD = "tkhd".toByteArray()

    private const val HUNDRED_NANOS_PER_MS = 10_000L

    /**
     * 从 MP4 文件解析章节信息
     */
    suspend fun parse(context: Context, uri: Uri): List<ChapterNode> = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.openInputStream(uri)?.use { stream ->
                return@withContext parseFromStream(stream)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext emptyList()
    }

    /**
     * 从输入流解析章节
     */
    fun parseFromStream(stream: InputStream): List<ChapterNode> {
        // 先尝试 QuickTime 章节
        val qtChapters = parseQuickTimeChapters(stream)
        if (qtChapters.isNotEmpty()) return qtChapters

        // 重置流，尝试 Nero 章节
        try {
            stream.reset()
        } catch (e: Exception) {
            // 流不支持 reset，返回已解析的结果
        }

        return qtChapters
    }

    /**
     * 解析 QuickTime 章节 (moov.udta.chpl)
     */
    private fun parseQuickTimeChapters(stream: InputStream): List<ChapterNode> {
        val chapters = mutableListOf<ChapterNode>()

        try {
            val data = stream.readBytes()
            val buffer = ByteBuffer.wrap(data).order(ByteOrder.BIG_ENDIAN)

            // 遍历顶层 atoms
            while (buffer.remaining() >= 8) {
                val atomStart = buffer.position()
                val atomSize = buffer.int.toLong() and 0xFFFFFFFFL
                val atomType = ByteArray(4)
                buffer.get(atomType)

                if (atomSize < 8) break

                if (atomType.contentEquals(ATOM_MOOV)) {
                    parseMoovAtom(buffer, atomStart + atomSize, chapters)
                    if (chapters.isNotEmpty()) return chapters
                    buffer.position(atomStart + atomSize.toInt())
                } else {
                    val skip = (atomSize - 8).toInt()
                    if (skip > 0 && skip <= buffer.remaining()) {
                        buffer.position(buffer.position() + skip)
                    } else {
                        break
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return chapters
    }

    /**
     * 解析 moov atom
     */
    private fun parseMoovAtom(buffer: ByteBuffer, moovEnd: Long, chapters: MutableList<ChapterNode>) {
        while (buffer.position() < moovEnd && buffer.remaining() >= 8) {
            val atomStart = buffer.position()
            val atomSize = buffer.int.toLong() and 0xFFFFFFFFL
            val atomType = ByteArray(4)
            buffer.get(atomType)

            if (atomSize < 8) break

            when {
                atomType.contentEquals(ATOM_UDTA) -> {
                    parseUdtaAtom(buffer, atomStart + atomSize, chapters)
                    if (chapters.isNotEmpty()) return
                    buffer.position(atomStart + atomSize.toInt())
                }
                else -> {
                    val skip = (atomSize - 8).toInt()
                    if (skip > 0 && skip <= buffer.remaining()) {
                        buffer.position(buffer.position() + skip)
                    } else {
                        break
                    }
                }
            }
        }
    }

    /**
     * 解析 udta atom
     */
    private fun parseUdtaAtom(buffer: ByteBuffer, udtaEnd: Long, chapters: MutableList<ChapterNode>) {
        while (buffer.position() < udtaEnd && buffer.remaining() >= 8) {
            val atomStart = buffer.position()
            val atomSize = buffer.int.toLong() and 0xFFFFFFFFL
            val atomType = ByteArray(4)
            buffer.get(atomType)

            if (atomSize < 8) break

            when {
                atomType.contentEquals(ATOM_CHPL) -> {
                    parseChplAtom(buffer, atomStart + atomSize, chapters)
                    return
                }
                atomType.contentEquals(ATOM_META) -> {
                    parseMetaForChapters(buffer, atomStart + atomSize, chapters)
                    if (chapters.isNotEmpty()) return
                    buffer.position(atomStart + atomSize.toInt())
                }
                else -> {
                    val skip = (atomSize - 8).toInt()
                    if (skip > 0 && skip <= buffer.remaining()) {
                        buffer.position(buffer.position() + skip)
                    } else {
                        break
                    }
                }
            }
        }
    }

    /**
     * 解析 chpl atom (QuickTime 章节)
     *
     * chpl 结构:
     * - version (1 byte)
     * - flags (3 bytes)
     * - reserved (4 bytes) 或 chapter count
     * - chapter entries:
     *   - timestamp (8 bytes, in 100ns units)
     *   - name length (1 byte)
     *   - name (UTF-8)
     */
    private fun parseChplAtom(buffer: ByteBuffer, chplEnd: Long, chapters: MutableList<ChapterNode>) {
        if (buffer.remaining() < 8) return

        // version + flags
        buffer.get() // version
        buffer.get() // flags[0]
        buffer.get() // flags[1]
        buffer.get() // flags[2]

        // chapter count or reserved
        val countOrReserved = buffer.int

        var chapterCount = 0
        var index = 0

        // 尝试读取章节
        while (buffer.position() < chplEnd && buffer.remaining() >= 9) {
            // 时间戳 (8 bytes, 100ns 单位)
            val timestamp100ns = buffer.long
            val timestampMs = timestamp100ns / HUNDRED_NANOS_PER_MS

            // 名称长度 (1 byte)
            if (!buffer.hasRemaining()) break
            val nameLength = buffer.get().toInt() and 0xFF

            if (nameLength <= 0 || nameLength > buffer.remaining()) break

            // 名称
            val nameBytes = ByteArray(nameLength)
            buffer.get(nameBytes)
            val name = String(nameBytes, Charsets.UTF_8)

            chapters.add(ChapterNode(index, name, timestampMs))
            index++
            chapterCount++
        }
    }

    /**
     * 解析 meta atom 查找章节
     */
    private fun parseMetaForChapters(buffer: ByteBuffer, metaEnd: Long, chapters: MutableList<ChapterNode>) {
        // meta atom 有 4 bytes 的 version/flags
        if (buffer.remaining() < 4) return
        buffer.int // skip version/flags

        while (buffer.position() < metaEnd && buffer.remaining() >= 8) {
            val atomStart = buffer.position()
            val atomSize = buffer.int.toLong() and 0xFFFFFFFFL
            val atomType = ByteArray(4)
            buffer.get(atomType)

            if (atomSize < 8) break

            when {
                atomType.contentEquals(ATOM_ILST) -> {
                    parseIlstForChapters(buffer, atomStart + atomSize, chapters)
                    if (chapters.isNotEmpty()) return
                    buffer.position(atomStart + atomSize.toInt())
                }
                else -> {
                    val skip = (atomSize - 8).toInt()
                    if (skip > 0 && skip <= buffer.remaining()) {
                        buffer.position(buffer.position() + skip)
                    } else {
                        break
                    }
                }
            }
        }
    }

    /**
     * 解析 ilst atom 查找章节列表
     */
    private fun parseIlstForChapters(buffer: ByteBuffer, ilstEnd: Long, chapters: MutableList<ChapterNode>) {
        // 查找 chap atom (章节列表)
        while (buffer.position() < ilstEnd && buffer.remaining() >= 8) {
            val atomStart = buffer.position()
            val atomSize = buffer.int.toLong() and 0xFFFFFFFFL
            val atomType = ByteArray(4)
            buffer.get(atomType)

            if (atomSize < 8) break

            if (atomType.contentEquals(ATOM_CHAP)) {
                parseChapList(buffer, atomStart + atomSize, chapters)
                return
            } else {
                val skip = (atomSize - 8).toInt()
                if (skip > 0 && skip <= buffer.remaining()) {
                    buffer.position(buffer.position() + skip)
                } else {
                    break
                }
            }
        }
    }

    /**
     * 解析 chap atom 中的章节列表
     */
    private fun parseChapList(buffer: ByteBuffer, chapEnd: Long, chapters: MutableList<ChapterNode>) {
        // chap atom 内部结构: data atom
        while (buffer.position() < chapEnd && buffer.remaining() >= 8) {
            val atomStart = buffer.position()
            val atomSize = buffer.int.toLong() and 0xFFFFFFFFL
            val atomType = ByteArray(4)
            buffer.get(atomType)

            if (atomSize < 8) break

            if (atomType.contentEquals("data".toByteArray())) {
                // data atom: version (1) + flags (3) + reserved (4) + data
                if (buffer.remaining() >= 8) {
                    buffer.int // version + flags
                    buffer.int // reserved

                    // 解析章节时间戳列表
                    val dataEnd = atomStart + atomSize
                    var index = 0
                    while (buffer.position() < dataEnd && buffer.remaining() >= 8) {
                        val timestampMs = buffer.long
                        chapters.add(ChapterNode(index, "Chapter ${index + 1}", timestampMs))
                        index++
                    }
                }
            } else {
                val skip = (atomSize - 8).toInt()
                if (skip > 0 && skip <= buffer.remaining()) {
                    buffer.position(buffer.position() + skip)
                } else {
                    break
                }
            }
        }
    }
}
