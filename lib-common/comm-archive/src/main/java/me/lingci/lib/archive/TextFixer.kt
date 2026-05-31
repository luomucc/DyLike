package me.lingci.lib.archive

import java.nio.charset.Charset

object TextFixer {

    private fun containsChinese(text: String): Boolean {
        return text.count { it in '\u4e00'..'\u9fff' } >= 2 // 至少两个汉字
    }

    private fun tryFixAsUtf8Mojibake(s: String): String {
        return String(s.toByteArray(Charsets.ISO_8859_1), Charsets.UTF_8)
    }

    private fun tryFixAsGbkMojibake(s: String): String {
        return String(s.toByteArray(Charsets.ISO_8859_1), Charset.forName("GBK"))
    }

    fun fixGarbledFilename(input: String): String {
        // 如果已经是正常中文，直接返回
        if (containsChinese(input)) return input

        // 尝试两种修复
        var candidate1 = ""
        var candidate2 = ""

        try { candidate1 = tryFixAsUtf8Mojibake(input) } catch (_: Exception) {}
        try { candidate2 = tryFixAsGbkMojibake(input) } catch (_: Exception) {}

        // 优先选择包含更多中文字符的结果
        val score1 = candidate1.count { it in '\u4e00'..'\u9fff' }
        val score2 = candidate2.count { it in '\u4e00'..'\u9fff' }

        return when {
            score1 > score2 -> candidate1
            score2 > 0 -> candidate2
            else -> input // 都不行，返回原串
        }
    }
}