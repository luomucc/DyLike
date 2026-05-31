package me.lingci.lib.player.chapter

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.lingci.lib.base.util.Log

/**
 *   @author : happyc
 *   time    : 2026/04/17
 *   desc    :
 *   version : 1.0
 */
object ChapterHelper {


    /**
     * 从视频文件提取章节信息
     * 自动检测格式并调用对应的解析器
     *
     * @param context 上下文
     * @param uri 视频文件 URI
     * @return 章节列表
     */
    suspend fun extractChapters(context: Context, uri: Uri): List<ChapterNode> = withContext(Dispatchers.IO) {
        try {
            val mimeType = context.contentResolver.getType(uri) ?: ""
            val path = uri.path ?: ""

            val chapters = when {
                // MKV/MKA 格式
                mimeType.contains("matroska", ignoreCase = true) ||
                        mimeType.contains("x-matroska", ignoreCase = true) ||
                        path.endsWith(".mkv", ignoreCase = true) ||
                        path.endsWith(".mka", ignoreCase = true) -> {
                    MkChapterParser.parse(context, uri)
                }

                // MP4/M4A/M4B 格式
                mimeType.contains("mp4", ignoreCase = true) ||
                        mimeType.contains("x-m4a", ignoreCase = true) ||
                        mimeType.contains("x-m4b", ignoreCase = true) ||
                        path.endsWith(".mp4", ignoreCase = true) ||
                        path.endsWith(".m4a", ignoreCase = true) ||
                        path.endsWith(".m4b", ignoreCase = true) -> {
                    Mp4ChapterParser.parse(context, uri)
                }

                // 尝试解析所有格式
                else -> {
                    // 先尝试 MKV
                    val mkvChapters = MkChapterParser.parse(context, uri)
                    if (mkvChapters.isNotEmpty()) return@withContext mkvChapters

                    // 再尝试 MP4
                    val mp4Chapters = Mp4ChapterParser.parse(context, uri)
                    if (mp4Chapters.isNotEmpty()) return@withContext mp4Chapters

                    emptyList()
                }
            }

            Log.d(this@ChapterHelper, "extractChapters", "Found ${chapters.size} chapters")
            return@withContext chapters
        } catch (e: Exception) {
            Log.d(this@ChapterHelper, "extractChapters", "Failed to extract chapters", e)
            return@withContext emptyList()
        }
    }

    /**
     * 查找当前播放位置对应的章节
     */
    fun findCurrentChapter(chapters: List<ChapterNode>, positionMs: Long): ChapterNode? {
        if (chapters.isEmpty()) return null

        // 从后往前查找，找到第一个开始时间 <= 当前位置的章节
        for (i in chapters.size - 1 downTo 0) {
            if (chapters[i].startTimeMs <= positionMs) {
                return chapters[i]
            }
        }
        return chapters.firstOrNull()
    }

}