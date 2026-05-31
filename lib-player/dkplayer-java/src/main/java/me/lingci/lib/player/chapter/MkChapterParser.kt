package me.lingci.lib.player.chapter

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * MKV/Matroska 章节解析器
 * 解析 EBML 格式的 Chapters 元素
 *
 * EBML 元素 ID 参考:
 * - Chapters: 0x1043A770
 * - EditionEntry: 0x45B9
 * - ChapterAtom: 0xB6
 * - ChapterTimeStart: 0x91
 * - ChapterTimeEnd: 0x92
 * - ChapterDisplay: 0x80
 * - ChapString: 0x85
 * - ChapLanguage: 0x437C
 */
object MkChapterParser {

    private const val TAG_CHAPTERS = 0x1043A770L
    private const val TAG_EDITION_ENTRY = 0x45B9L
    private const val TAG_CHAPTER_ATOM = 0xB6L
    private const val TAG_CHAPTER_TIME_START = 0x91L
    private const val TAG_CHAPTER_TIME_END = 0x92L
    private const val TAG_CHAPTER_DISPLAY = 0x80L
    private const val TAG_CHAP_STRING = 0x85L
    private const val TAG_CHAP_LANGUAGE = 0x437CL

    private const val NANOS_PER_MS = 1_000_000L

    /**
     * 从 MKV 文件解析章节信息
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
        val chapters = mutableListOf<ChapterNode>()

        try {
            val allData = stream.readBytes()
            val buffer = ByteBuffer.wrap(allData).order(ByteOrder.BIG_ENDIAN)

            // 跳过 EBML Header
            skipEbmlHeader(buffer)

            // 查找 Segment 元素
            while (buffer.hasRemaining()) {
                val elementId = readVarInt(buffer)
                val elementSize = readVarIntSize(buffer)

                when (elementId) {
                    0x18538067L -> { // Segment
                        parseSegment(buffer, elementSize, chapters)
                        break
                    }
                    else -> {
                        if (elementSize > 0 && elementSize <= buffer.remaining()) {
                            buffer.position(buffer.position() + elementSize.toInt())
                        } else {
                            break
                        }
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return chapters
    }

    private fun parseSegment(buffer: ByteBuffer, segmentSize: Long, chapters: MutableList<ChapterNode>) {
        val startPos = buffer.position()
        val endPos = if (segmentSize == -1L) buffer.limit() else (startPos + segmentSize).toInt().coerceAtMost(buffer.limit())

        while (buffer.position() < endPos && buffer.hasRemaining()) {
            val elementId = readVarInt(buffer)
            if (elementId == -1L) break
            val elementSize = readVarIntSize(buffer)
            if (elementSize == -1L) break

            when (elementId) {
                TAG_CHAPTERS -> {
                    parseChapters(buffer, elementSize, chapters)
                }
                else -> {
                    val skip = elementSize.toInt()
                    if (skip > 0 && skip <= buffer.remaining()) {
                        buffer.position(buffer.position() + skip)
                    } else {
                        break
                    }
                }
            }
        }
    }

    private fun parseChapters(buffer: ByteBuffer, size: Long, chapters: MutableList<ChapterNode>) {
        val endPos = buffer.position() + size.toInt()
        var chapterIndex = 0

        while (buffer.position() < endPos && buffer.hasRemaining()) {
            val elementId = readVarInt(buffer)
            if (elementId == -1L) break
            val elementSize = readVarIntSize(buffer)
            if (elementSize == -1L) break

            when (elementId) {
                TAG_EDITION_ENTRY -> {
                    chapterIndex = parseEditionEntry(buffer, elementSize, chapters, chapterIndex)
                }
                else -> {
                    val skip = elementSize.toInt()
                    if (skip > 0 && skip <= buffer.remaining()) {
                        buffer.position(buffer.position() + skip)
                    } else {
                        break
                    }
                }
            }
        }
    }

    private fun parseEditionEntry(buffer: ByteBuffer, size: Long, chapters: MutableList<ChapterNode>, startIndex: Int): Int {
        val endPos = buffer.position() + size.toInt()
        var chapterIndex = startIndex

        while (buffer.position() < endPos && buffer.hasRemaining()) {
            val elementId = readVarInt(buffer)
            if (elementId == -1L) break
            val elementSize = readVarIntSize(buffer)
            if (elementSize == -1L) break

            when (elementId) {
                TAG_CHAPTER_ATOM -> {
                    val chapter = parseChapterAtom(buffer, elementSize, chapterIndex)
                    if (chapter != null) {
                        chapters.add(chapter)
                        chapterIndex++
                    }
                }
                else -> {
                    val skip = elementSize.toInt()
                    if (skip > 0 && skip <= buffer.remaining()) {
                        buffer.position(buffer.position() + skip)
                    } else {
                        break
                    }
                }
            }
        }
        return chapterIndex
    }

    private fun parseChapterAtom(buffer: ByteBuffer, size: Long, index: Int): ChapterNode? {
        val endPos = buffer.position() + size.toInt()
        var timeStartNs: Long? = null
        var timeEndNs: Long? = null
        var title: String? = null
        var language: String? = null

        while (buffer.position() < endPos && buffer.hasRemaining()) {
            val elementId = readVarInt(buffer)
            if (elementId == -1L) break
            val elementSize = readVarIntSize(buffer)
            if (elementSize == -1L) break

            when (elementId) {
                TAG_CHAPTER_TIME_START -> {
                    timeStartNs = readUnsignedInt(buffer, elementSize.toInt())
                }
                TAG_CHAPTER_TIME_END -> {
                    timeEndNs = readUnsignedInt(buffer, elementSize.toInt())
                }
                TAG_CHAPTER_DISPLAY -> {
                    val display = parseChapterDisplay(buffer, elementSize)
                    title = display.first
                    language = display.second
                }
                TAG_CHAPTER_ATOM -> {
                    // 嵌套章节，跳过
                    val skip = elementSize.toInt()
                    if (skip > 0 && skip <= buffer.remaining()) {
                        buffer.position(buffer.position() + skip)
                    }
                }
                else -> {
                    val skip = elementSize.toInt()
                    if (skip > 0 && skip <= buffer.remaining()) {
                        buffer.position(buffer.position() + skip)
                    } else {
                        break
                    }
                }
            }
        }

        if (timeStartNs == null) return null

        val startTimeMs = timeStartNs / NANOS_PER_MS
        val endTimeMs = timeEndNs?.let { it / NANOS_PER_MS }

        return ChapterNode(
            index,
            title ?: "Chapter ${index + 1}",
            startTimeMs,
            endTimeMs,
            language
        )
    }

    private fun parseChapterDisplay(buffer: ByteBuffer, size: Long): Pair<String?, String?> {
        val endPos = buffer.position() + size.toInt()
        var title: String? = null
        var language: String? = null

        while (buffer.position() < endPos && buffer.hasRemaining()) {
            val elementId = readVarInt(buffer)
            if (elementId == -1L) break
            val elementSize = readVarIntSize(buffer)
            if (elementSize == -1L) break

            when (elementId) {
                TAG_CHAP_STRING -> {
                    val bytes = ByteArray(elementSize.toInt())
                    buffer.get(bytes)
                    title = String(bytes, Charsets.UTF_8)
                }
                TAG_CHAP_LANGUAGE -> {
                    val bytes = ByteArray(elementSize.toInt())
                    buffer.get(bytes)
                    language = String(bytes, Charsets.UTF_8)
                }
                else -> {
                    val skip = elementSize.toInt()
                    if (skip > 0 && skip <= buffer.remaining()) {
                        buffer.position(buffer.position() + skip)
                    } else {
                        break
                    }
                }
            }
        }
        return Pair(title, language)
    }

    /**
     * 跳过 EBML Header
     */
    private fun skipEbmlHeader(buffer: ByteBuffer) {
        // EBML Header ID: 0x1A45DFA3
        if (buffer.remaining() >= 4) {
            val headerId = buffer.int
            if (headerId == 0x1A45DFA3.toInt()) {
                val headerSize = readVarIntSize(buffer)
                if (headerSize > 0 && headerSize <= buffer.remaining()) {
                    buffer.position(buffer.position() + headerSize.toInt())
                }
            } else {
                buffer.position(buffer.position() - 4)
            }
        }
    }

    /**
     * 读取 EBML Variable Size Integer
     */
    private fun readVarIntSize(buffer: ByteBuffer): Long {
        if (!buffer.hasRemaining()) return -1
        val firstByte = buffer.get().toInt() and 0xFF

        val (length, mask) = when {
            firstByte and 0x80 != 0 -> Pair(1, 0x7F)
            firstByte and 0x40 != 0 -> Pair(2, 0x3F)
            firstByte and 0x20 != 0 -> Pair(3, 0x1F)
            firstByte and 0x10 != 0 -> Pair(4, 0x0F)
            firstByte and 0x08 != 0 -> Pair(5, 0x07)
            firstByte and 0x04 != 0 -> Pair(6, 0x03)
            firstByte and 0x02 != 0 -> Pair(7, 0x01)
            firstByte and 0x01 != 0 -> Pair(8, 0x00)
            else -> return -1
        }

        if (length == 1) return (firstByte and mask).toLong()

        var result = (firstByte and mask).toLong()
        for (i in 1 until length) {
            if (!buffer.hasRemaining()) return -1
            result = (result shl 8) or (buffer.get().toInt() and 0xFF).toLong()
        }
        return result
    }

    /**
     * 读取 EBML Element ID (VINT)
     */
    private fun readVarInt(buffer: ByteBuffer): Long {
        if (!buffer.hasRemaining()) return -1
        val firstByte = buffer.get().toInt() and 0xFF

        val length = when {
            firstByte and 0x80 != 0 -> 1
            firstByte and 0x40 != 0 -> 2
            firstByte and 0x20 != 0 -> 3
            firstByte and 0x10 != 0 -> 4
            firstByte and 0x08 != 0 -> 5
            firstByte and 0x04 != 0 -> 6
            firstByte and 0x02 != 0 -> 7
            firstByte and 0x01 != 0 -> 8
            else -> return -1
        }

        if (length == 1) return firstByte.toLong()

        var result = firstByte.toLong()
        for (i in 1 until length) {
            if (!buffer.hasRemaining()) return -1
            result = (result shl 8) or (buffer.get().toInt() and 0xFF).toLong()
        }
        return result
    }

    /**
     * 读取无符号整数
     */
    private fun readUnsignedInt(buffer: ByteBuffer, size: Int): Long {
        if (size <= 0 || size > 8) return 0
        var result = 0L
        for (i in 0 until size) {
            if (!buffer.hasRemaining()) break
            result = (result shl 8) or (buffer.get().toInt() and 0xFF).toLong()
        }
        return result
    }
}
