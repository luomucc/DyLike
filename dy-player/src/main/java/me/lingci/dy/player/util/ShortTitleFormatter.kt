package me.lingci.dy.player.util

import me.lingci.dy.player.core.ShortTitleStrategy
import me.lingci.lib.player.danmaku.PlayerInitializer

/**
 * 短视频标题格式化器。
 *
 * 处理流程：
 * 1. 去扩展名
 * 2. 按 strategy 处理首行 / 剩余
 * 3. 按分隔符拆分剩余为多行
 * 4. 应用最大行数限制
 *
 * 所有策略对异常输入（无分隔符、非法正则等）均回退为「去扩展名原样显示」，
 * 保证任何文件名都能安全展示。
 */
object ShortTitleFormatter {

    /** 默认分隔符，非法分隔符回退使用。 */
    private const val DEFAULT_DELIMITER = "-"

    /**
     * 便捷重载：直接从 [PlayerInitializer.Player] 读取运行时策略参数格式化标题。
     * 供 Adapter / Activity 等业务侧统一调用，避免重复样板代码。
     */
    fun format(rawName: String): String {
        return format(
            rawName = rawName,
            strategy = ShortTitleStrategy.fromValue(PlayerInitializer.Player.shortTitleStrategy),
            delimiter = PlayerInitializer.Player.shortTitleDelimiter,
            regex = PlayerInitializer.Player.shortTitleRegex,
            maxLines = PlayerInitializer.Player.shortTitleMaxLines
        )
    }

    /**
     * 把原始文件名格式化为展示标题（多行用 \n 分隔）。
     *
     * @param rawName 原始文件名（VideoData.name，可能含扩展名）
     * @param strategy 策略
     * @param delimiter 分隔符（单字符，如 "-" "_"；非法则回退 "-"）
     * @param regex 首行正则（仅 REGEX_FIRST 用；空则回退为 FIRST_LINE 语义）
     * @param maxLines 最大行数（0 = 不限制）
     */
    fun format(
        rawName: String,
        strategy: ShortTitleStrategy,
        delimiter: String,
        regex: String,
        maxLines: Int
    ): String {
        if (rawName.isBlank()) return rawName
        // 1. 去扩展名
        val base = rawName.substringBeforeLast(".")
        if (strategy == ShortTitleStrategy.RAW) return base

        val delim = sanitizeDelimiter(delimiter)
        return when (strategy) {
            ShortTitleStrategy.SPLIT_ALL -> joinLines(base.split(delim), maxLines)
            ShortTitleStrategy.FIRST_LINE -> splitByFirstDelimiter(base, delim, maxLines)
            ShortTitleStrategy.REGEX_FIRST -> extractFirstByRegex(base, delim, regex, maxLines)
            ShortTitleStrategy.RAW -> base
        }
    }

    private fun sanitizeDelimiter(delimiter: String): String {
        return if (delimiter.length == 1) delimiter else DEFAULT_DELIMITER
    }

    /** 分隔符前作首行，剩余按分隔符换行。无分隔符则原样返回。 */
    private fun splitByFirstDelimiter(base: String, delim: String, maxLines: Int): String {
        val idx = base.indexOf(delim)
        if (idx < 0) return base
        val first = base.substring(0, idx).trim()
        val rest = base.substring(idx + delim.length)
        val restLines = rest.split(delim).map { it.trim() }.filter { it.isNotEmpty() }
        return joinLines(listOf(first) + restLines, maxLines)
    }

    /**
     * 正则提取首行，剩余按分隔符换行。
     * - 正则为空 / 非法 / 无匹配 → 回退 [splitByFirstDelimiter]。
     * - 优先取第一个捕获组，否则取整个匹配。
     */
    private fun extractFirstByRegex(
        base: String,
        delim: String,
        regex: String,
        maxLines: Int
    ): String {
        if (regex.isBlank()) return splitByFirstDelimiter(base, delim, maxLines)
        val match = try {
            Regex(regex).find(base)
        } catch (e: Exception) {
            // 非法正则，回退首行提取
            null
        } ?: return splitByFirstDelimiter(base, delim, maxLines)

        val first = (match.groups[1]?.value ?: match.value).trim().ifEmpty {
            return splitByFirstDelimiter(base, delim, maxLines)
        }
        val rest = base.removeRange(match.range).trim()
        val restLines = rest.split(delim).map { it.trim() }.filter { it.isNotEmpty() }
        return joinLines(listOf(first) + restLines, maxLines)
    }

    /** 拼接多行；超过最大行数时末行合并剩余内容。 */
    private fun joinLines(lines: List<String>, maxLines: Int): String {
        val filtered = lines.filter { it.isNotEmpty() }
        if (filtered.isEmpty()) return ""
        if (maxLines in 1 until filtered.size) {
            val head = filtered.take(maxLines - 1)
            val tail = filtered.drop(maxLines - 1).joinToString("")
            return (head + tail).joinToString("\n")
        }
        return filtered.joinToString("\n")
    }

}
