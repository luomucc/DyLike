package me.lingci.dy.player.core

/**
 * 短视频标题展示策略。
 *
 * Numeric values are storage ABI: do not reorder or reuse them.
 *
 * - [RAW]: 原始显示（仅去扩展名，等价于改动前的默认行为）。
 * - [SPLIT_ALL]: 按分隔符拆分全部为多行。
 * - [FIRST_LINE]: 分隔符前作为首行，剩余按分隔符继续换行。
 * - [REGEX_FIRST]: 首行用正则提取，剩余按分隔符换行。
 */
enum class ShortTitleStrategy(val value: Int, val displayName: String) {
    RAW(0, "原始显示"),
    SPLIT_ALL(1, "分隔符换行"),
    FIRST_LINE(2, "首行提取"),
    REGEX_FIRST(3, "正则首行+分隔符");

    companion object {
        /** Unknown restored/future values fall back to RAW (safest default). */
        fun fromValue(value: Int): ShortTitleStrategy {
            return entries.firstOrNull { it.value == value } ?: RAW
        }
    }
}
