package me.lingci.lib.base.util

import me.lingci.lib.base.storage.entity.FileEntity
import java.math.BigInteger
import java.text.Collator
import java.util.Locale
import java.util.regex.Pattern

/**
 *   @author : gpt
 *   time    : 2026/02/04
 *   desc    :
 *   version : 1.0
 */
object Comparators {

    // ========= 自然排序器 =========
    private val defaultSorter = FastNaturalSorter(
        ignoreSymbols = true,
        numberPrefixFirst = true
    )

    // ========= 字符串自然排序比较器 =========
    fun stringComparator(): Comparator<String> {
        return Comparator { a, b -> defaultSorter.compare(a, b) }
    }

    // ========= 文件名自然排序比较器 =========
    fun fileEntityNameComparator(): Comparator<FileEntity> {
        val cmp = stringComparator()
        return Comparator { a, b -> cmp.compare(a.name, b.name) }
    }

    // ========= 文件夹优先比较器 =========
    fun fileEntityFolderComparator(): Comparator<FileEntity> {
        val cmp = stringComparator()
        return Comparator { a, b ->
            when {
                !a.isFile && b.isFile -> -1
                a.isFile && !b.isFile -> 1
                else -> cmp.compare(a.name, b.name)
            }
        }
    }

    // ========= 内部自然排序器实现 =========
    private class FastNaturalSorter(
        private val ignoreSymbols: Boolean = true,
        private val numberPrefixFirst: Boolean = true
    ) {

        private val collator = Collator.getInstance(Locale.CHINA).apply {
            strength = Collator.PRIMARY
        }

        private val tokenPattern = Pattern.compile("(\\d+)|(\\D+)")
        private val cache = HashMap<String, List<Token>>(1024)

        // ========= 公共 compare 方法 =========
        fun compare(a: String, b: String): Int {
            val t1 = tokenize(a)
            val t2 = tokenize(b)

            // 1️⃣ 前缀数字优先
            if (numberPrefixFirst) {
                val p1 = t1.firstOrNull() as? Token.Number
                val p2 = t2.firstOrNull() as? Token.Number
                if (p1 != null && p2 != null) {
                    val cmp = p1.value.compareTo(p2.value)
                    if (cmp != 0) return cmp
                } else if (p1 != null) return -1
                else if (p2 != null) return 1
            }

            // 2️⃣ Token 逐段比较
            val size = minOf(t1.size, t2.size)
            for (i in 0 until size) {
                val cmp = t1[i].compareTo(t2[i], collator)
                if (cmp != 0) return cmp
            }

            // 3️⃣ 长度比较
            return t1.size.compareTo(t2.size)
        }

        // ========= Token 解析 =========
        private fun tokenize(raw: String): List<Token> {
            return cache.getOrPut(raw) {
                val processed = preprocess(raw)
                val matcher = tokenPattern.matcher(processed)
                val tokens = mutableListOf<Token>()
                while (matcher.find()) {
                    val part = matcher.group()
                    if (part.all { it.isDigit() }) {
                        tokens.add(Token.Number(BigInteger(part)))
                    } else {
                        tokens.add(Token.Text(part))
                    }
                }
                tokens
            }
        }

        private fun preprocess(input: String): String {
            return if (!ignoreSymbols) input
            else input.replace(Regex("[\\s._\\-()\\[\\]{}]+"), "")
        }

        // ========= Token 定义 =========
        private sealed class Token {
            data class Number(val value: BigInteger) : Token()
            data class Text(val value: String) : Token()

            fun compareTo(other: Token, collator: Collator): Int {
                return when {
                    this is Number && other is Number -> this.value.compareTo(other.value)
                    this is Text && other is Text -> collator.compare(this.value, other.value)
                    this is Number -> -1  // 数字排在文本前
                    else -> 1             // 文本排在数字后
                }
            }
        }
    }

}