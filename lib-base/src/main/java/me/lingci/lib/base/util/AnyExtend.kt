package me.lingci.lib.base.util

import android.annotation.SuppressLint
import android.content.Intent
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.util.TypedValue
import androidx.core.graphics.ColorUtils
import java.io.File
import java.nio.file.Files
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.regex.Pattern
import kotlin.io.path.fileStore
import kotlin.time.DurationUnit
import kotlin.time.toDuration

fun Any.logD(vararg messages: Any?) {
    Log.dd(this, messages.toList())
}

fun String.md5(): String {
    return CodeUtil.md5(this)
}

fun String.isUrl(): Boolean {
    return CodeUtil.isURL(this)
}

fun String.isLocal(): Boolean {
    return this.startsWith("/")
}

fun String.newFile(): File = File(this)

fun String.suffix(): String {
    val index = lastIndexOf(".")
    return if (index == -1) "" else substring(index).lowercase()
}

fun String.isVideo(): Boolean {
    return FileOperator.VIDEO_EXTENSIONS.contains(suffix())
}

fun String.isImage(): Boolean {
    return FileOperator.IMAGE_EXTENSIONS.contains(suffix())
}

fun String.isText(): Boolean {
    return FileOperator.TEXT_EXTENSIONS.contains(suffix())
}

fun String.isFont(): Boolean {
    return FileOperator.FONT_EXTENSIONS.contains(suffix())
}

fun String.findInt(): Int {
    return try {
        Pattern.compile("\\d+").firstMatchGroup(this).toInt()
    } catch (e: Exception) {
        -1
    }
}

fun String.toIntOrDefault(): Int {
    return try {
        toInt()
    } catch (e: Exception) {
        -1
    }
}

// 编码特殊字符
fun String.encodeDm(): String {
    return this.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        //.replace("'", "&apos;")
        .replace("\"", "&quot;")
}

// 解码特殊字符
fun String.decodeDm(): String {
    return this.replace("&amp;", "&")
        .replace("&lt;", "<")
        .replace("&gt;", ">")
        .replace("&apos;", "'")
        .replace("&quot;", "\"")
}

val xmlInvalidChars = Regex("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F\\x7F]")
fun String.replaceDm(): String {
    return xmlInvalidChars.replace(this, "")
        .replace('\b', ' ')
        .replace('\uFFFE', ' ')
        .replace('\uFFFF', ' ')
}

fun String?.isJsonEmpty(): Boolean {
    if (this.isNullOrBlank()) {
        return true
    }
    if (this == "{}" || this == "[]") {
        return true
    }
    return false
}

// 文件是否满足扩展类型
fun File.extensionFile(extensions: List<String>): Boolean {
    if (isDirectory) {
        return false
    }
    if (startDot()) {
        return false
    }
    if (extensions.isEmpty()) {
        return true
    }
    return extensions.any { extension -> name.lowercase().endsWith(extension) }
}

fun File.notExists(): Boolean {
    return !this.exists()
}

fun File.modifiedIn24H(): Boolean {
    return isUpdatedWithin(24)
}

fun File.isUpdatedWithin(duration: Long, unit: DurationUnit = DurationUnit.HOURS): Boolean {
    return (System.currentTimeMillis() - this.lastModified()) <= duration.toDuration(unit).inWholeMilliseconds
}

fun File.createNew() {
    this.parentFile?.let {
        if (it.notExists()) {
            it.mkdirs()
        }
    }
    if (this.notExists()) {
        this.createNewFile()
    }
}

fun File.deleteExists() {
    if (this.exists()) {
        this.delete()
    }
}

fun File.suffix(): String {
    return name.suffix()
}

fun File.isVideo(): Boolean {
    if (isDirectory) {
        return false
    }
    return FileOperator.VIDEO_EXTENSIONS.contains(suffix())
}

fun File.isVideoOrDir(): Boolean {
    if (isDirectory) {
        return notStartDot()
    }
    return isVideo()
}

fun File.isImage(): Boolean {
    if (isDirectory || notExists() || canRead().not() || length() <= 0) {
        return false
    }
    return FileOperator.IMAGE_EXTENSIONS.contains(suffix())
}

fun String.startDot(): Boolean {
    return this.startsWith(".")
}

fun String.notStartDot(): Boolean {
    return startDot().not()
}

fun File.startDot(): Boolean {
    return name.startDot()
}

fun File.notStartDot(): Boolean {
    return startDot().not()
}

fun File.isHome(): Boolean {
    return path == FileOperator.rootFolder.path
}

fun File.notHome(): Boolean {
    return !isHome()
}

fun File.isMovieFolder(): Boolean {
    return path == FileOperator.movieFolder.path
}

fun File.notMovieFolder(): Boolean {
    return !isMovieFolder()
}

fun File.pathNoRoot(): String {
    return path.replace(FileOperator.rootFolder.path, "")
}

fun File.hasChild(): Boolean {
    return !noChild()
}

fun File.noChild(): Boolean {
    return try {
        if (isFile) {
            true
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.list(toPath()).use { it.count() } == 0L
            } else {
                list().isNullOrEmpty()
            }
        }
    } catch (e: Exception) {
        true
    }
}

fun File.childSize(): Int {
    return try {
        if (isFile) {
            0
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                Files.list(toPath()).use { it.count() }.toInt()
            } else {
                list()?.size?: 0
            }
        }
    } catch (e: Exception) {
        0
    }
}

fun File.mediaChildSize(): Int {
    return FileOperator.getChildFileSize(this, FileOperator.VIDEO_EXTENSIONS)
}

@SuppressLint("DefaultLocale")
fun File.sizeFormat(): String {
    return when {
        length() < 1024 -> "${length()} B"
        length() < 1024 * 1024 -> String.format("%.2f kB", length() / 1024f)
        length() < 1024 * 1024 * 1024 -> String.format("%.2f MB", length() / 1024 / 1024f)
        else -> String.format("%.2f GB", length() / 1024 / 1024 / 1024f)
    }
}

// 扩展方法用于正则匹配
fun Pattern.firstMatchGroup(input: String): String {
    val matcher = this.matcher(input)
    return if (matcher.find()) {
        if (matcher.groupCount() >= 1) {
            matcher.group(1) ?: ""
        } else {
            ""
        }
    } else {
        ""
    }
}

fun Pattern.allMatchGroup(input: String): String {
    val matcher = this.matcher(input)
    return if (matcher.find()) {
        matcher.group() ?: ""
    } else {
        ""
    }
}

fun String.toColor(): Int {
    return (0x00000000ff000000L or this.toLong()).toInt()
}

fun String.toRgbDecimals(): Int {
    // 1. 去掉 # 前缀，统一转为大写
    val cleanHex = this.removePrefix("#").uppercase()

    // 2. 补全 Alpha 通道（若缺失）
    @Suppress("SpellCheckingInspection")
    val fullHex = when (cleanHex.length) {
        3 -> "FF${cleanHex.map { it.toString().repeat(2) }.joinToString("")}" // #F00 → FF000000
        4 -> cleanHex.map { it.toString().repeat(2) }.joinToString("") // #FF00 → FFFF0000
        6 -> "FF$cleanHex" // #FF0000 → FFFF0000
        8 -> cleanHex // #FFFF0000 → 原样
        else -> "FFFFFFFF"
    }

    // 3. 解析十六进制字符串为十进制 Int（radix=16 表示十六进制）
    return fullHex.substring(2).toInt(16)
}

fun Int.toRgbDecimal(): Int {
    return this and 0x00FFFFFF
}

fun Int.strokeColor(): Int {
    val luminance = ColorUtils.calculateLuminance(this)
    return if (luminance > 0.5) Color.BLACK else Color.WHITE
}

fun Int.colorLightness(amount: Float): Int {
    val hsl = FloatArray(3)
    ColorUtils.colorToHSL(this, hsl)
    // 调整明度(Lightness)分量
    hsl[2] = (hsl[2] + amount).coerceIn(0f, 1f)
    return ColorUtils.HSLToColor(hsl)
}

fun Int.isWhite(): Boolean {
    return this == 16777215 || this == Color.WHITE
}

fun Int.unWhite(): Boolean {
    return isWhite().not()
}

fun Int.isCloseWhite(): Boolean {
    return CodeUtil.isCloseToWhiteRgbInt(this)
}

fun Int.formatTime(): String {
    return CodeUtil.stringForTime(this)
}

fun Long.formatSendTime(): String {
    return CodeUtil.formatTime(this, "yyyy/MM/dd HH:mm")
}

fun Long.formatTimeS(): String {
    return CodeUtil.formatTime(this, "yyyy MM dd HH mm ss")
}

fun String.isDateTime(): Boolean {
    return CodeUtil.matchDateString(this)
}

fun String.toTimestamp(): Long {
    return try {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(this)!!.time
    } catch (e: Exception) {
        0
    }
}

fun String.toUnixTimestamp(): Long {
    return try {
        SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).parse(this)!!.time / 1000
    } catch (e: Exception) {
        0
    }
}

fun String.fileName(): String {
    return this.substringBefore("?").substringAfterLast("/")
}

fun String.subFileName(): String {
    return this.fileName().substringBeforeLast(".")
}

fun String.formatName(): String {
    if (this.isBlank()) return this
    return this.replace("\t", " ")
        .replace("\r", "")
        .replace("\b", "")
        .replace("\n", "")
        .replace("\\\\", "")
        .replace("/", "")
        .replace(">", "")
        .replace("<", "")
        .replace(":", "")
        .replace("\"", "")
        .replace("?", "")
        .replace("*", "")
        .replace("|", "")
}

fun Int.toMinutes(): Pair<Int, Int> {
    val seconds: Int = this % 60
    val minutes: Int = this / 60
    return Pair(minutes, seconds)
}

fun Int.toHours(): Triple<Int, Int, Int> {
    val seconds: Int = this % 60
    val minutes: Int = (this / 60) % 60
    val hours: Int = this / 3600
    return Triple(hours, minutes, seconds)
}

fun Int.toMinuteTimes(): String {
    val seconds: Int = this % 60
    val minutes: Int = this / 60
    return String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
}

fun Int.toHourTimes(): String {
    val seconds: Int = this % 60
    val minutes: Int = (this / 60) % 60
    val hours: Int = this / 3600
    return if (hours > 0) {
        String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
    }
}

inline fun <reified T : Parcelable> Bundle.safeGetParcelable(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        getParcelable(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        getParcelable(key)
    }
}

inline fun <reified T : Parcelable> Intent.safeGetParcelable(key: String): T? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getParcelableExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        this.getParcelableExtra(key)
    }
}

inline fun <reified T: Parcelable> Intent.safeGetParcelableArrayList(key: String): ArrayList<T>? {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this.getParcelableArrayListExtra(key, T::class.java)
    } else {
        @Suppress("DEPRECATION")
        this.getParcelableArrayListExtra(key)
    }
}

val Float.dp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, this, Resources.getSystem().displayMetrics
    )
