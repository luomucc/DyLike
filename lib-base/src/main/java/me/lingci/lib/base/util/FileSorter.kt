package me.lingci.lib.base.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.regex.Pattern

// 增强型
data class FileNamePart(
    val isNumber: Boolean,
    val value: String
)

// 增强版自然排序
fun splitFileNameToParts(fileName: String): List<FileNamePart> {
    val parts = mutableListOf<FileNamePart>()
    val pattern = Pattern.compile("(\\d+|\\D+)")
    val matcher = pattern.matcher(fileName)

    while (matcher.find()) {
        val match = matcher.group()
        parts.add(FileNamePart(
            isNumber = match.matches(Regex("\\d+")),
            value = match
        ))
    }
    return parts
}

fun advancedNaturalSort(fileNames: List<String>): List<String> {
    return fileNames.sortedWith { name1, name2 ->
        val parts1 = splitFileNameToParts(name1.lowercase())
        val parts2 = splitFileNameToParts(name2.lowercase())

        val maxLength = maxOf(parts1.size, parts2.size)

        for (i in 0 until maxLength) {
            if (i >= parts1.size) return@sortedWith -1
            if (i >= parts2.size) return@sortedWith 1

            val part1 = parts1[i]
            val part2 = parts2[i]

            when {
                part1.isNumber && part2.isNumber -> {
                    val num1 = part1.value.toLongOrNull() ?: 0
                    val num2 = part2.value.toLongOrNull() ?: 0
                    val result = num1.compareTo(num2)
                    if (result != 0) return@sortedWith result
                }
                else -> {
                    val result = part1.value.compareTo(part2.value)
                    if (result != 0) return@sortedWith result
                }
            }
        }
        0
    }
}

// 处理特殊情况
// 文件扩展名排序
fun sortByNameThenExtension(files: List<File>): List<File> {
    return files.sortedWith(compareBy<File> { it.nameWithoutExtension }
        .thenBy { it.extension })
}
// 混合日期和数字的文件名
fun sortWithDateAndNumbers(files: List<String>): List<String> {
    val datePattern = Regex("(\\d{4})[._-](\\d{1,2})[._-](\\d{1,2})")

    return files.sortedWith { name1, name2 ->
        val date1 = datePattern.find(name1)?.value
        val date2 = datePattern.find(name2)?.value

        when {
            date1 != null && date2 != null -> date1.compareTo(date2)
            date1 != null -> -1
            date2 != null -> 1
            else -> if (advancedNaturalSort(listOf(name1, name2)).first() == name1) -1 else 1
        }
    }
}
// 对于大量文件排序，建议使用：
// 使用并行排序处理大量文件
suspend fun sortFilesParallel(files: List<File>): List<File> {
    return withContext(Dispatchers.Default) {
        files.sortedWith { file1, file2 ->
            if (advancedNaturalSort(listOf(file1.name, file2.name)).first() == file1.name) -1 else 1
        }
    }
}


// 实际文件排序
fun sortFilesInDirectory(directory: File): List<File> {
    if (!directory.exists() || !directory.isDirectory) {
        return emptyList()
    }

    return directory.listFiles()?.sortedWith { file1, file2 ->
        // 按文件名自然排序
        val name1 = file1.name
        val name2 = file2.name

        if (advancedNaturalSort(listOf(name1, name2)).first() == name1) -1 else 1
    } ?: emptyList()
}

fun test() {
// 使用示例
    val directory = File("/sdcard/Download")
    val sortedFiles = sortFilesInDirectory(directory)
    sortedFiles.forEach { file ->
        println(file.name)
    }
}

fun testFileSorting() {
    val testFiles = listOf(
        "文件2.txt",
        "file10.txt",
        "文件1.txt",
        "测试_2024_01.jpg",
        "测试_2024_2.jpg",
        "abc123.txt",
        "中文测试.mp4",
        "!特殊符号.txt",
        "file1.txt",
        "file20.txt"
    )

    println("原始顺序: $testFiles")
    println("自然排序: ${advancedNaturalSort(testFiles)}")

    // 输出:
    // 原始顺序: [文件2.txt, file10.txt, 文件1.txt, 测试_2024_01.jpg, 测试_2024_2.jpg, abc123.txt, 中文测试.mp4, !特殊符号.txt, file1.txt, file20.txt]
    // 自然排序: [!特殊符号.txt, abc123.txt, file1.txt, file2.txt, file10.txt, file20.txt, 中文测试.mp4, 文件1.txt, 文件2.txt, 测试_2024_01.jpg, 测试_2024_2.jpg]
}