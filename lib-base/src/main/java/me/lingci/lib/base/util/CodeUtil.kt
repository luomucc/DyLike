package me.lingci.lib.base.util

import android.annotation.SuppressLint
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Color
import android.os.Build
import android.text.Html
import android.text.Spanned
import java.math.BigInteger
import java.net.URLEncoder
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.regex.Pattern
import kotlin.math.pow

/**
 *   @author : happyc
 *   time    : 2025/02/14
 *   desc    :
 *   version : 1.0
 */
object CodeUtil {

    /**
     * dp2px
     */
    fun dp2px(dpValue: Float): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (dpValue * scale + 0.5f).toInt()
    }

    /**
     * px2dp
     */
    fun px2dp(pxValue: Float): Int {
        val scale = Resources.getSystem().displayMetrics.density
        return (pxValue / scale + 0.5f).toInt()
    }

    /**
     * 屏幕宽度
     */
    fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    fun md5(input: String): String {
        val digest = MessageDigest.getInstance("MD5")
        digest.update(input.toByteArray())
        return BigInteger(1, digest.digest()).toString(16)
    }

    @Suppress("DEPRECATION")
    fun urlEncode(value: String): String {
        return try {
            URLEncoder.encode(value, "utf-8")
        } catch (e: Exception) {
            URLEncoder.encode(value)
        }
    }

    fun findUrl(text: String, find: (url: String) -> Unit) {
        val reg = "(https?://[^\\s/$.?#].[^\\s]*)"
        val matcher = Pattern.compile(reg).matcher(text)
        while (matcher.find()) {
            find(matcher.group())
        }
    }

    /**
     * isURL
     */
    fun isURL(str: String): Boolean {
        val regex = "[a-zA-z]+://[^\\s]*"
        val r = Pattern.compile(regex)
        val m = r.matcher(str)
        return m.matches()
    }

    fun copyToClipboard(context: Context, text: String): Boolean {
        if (text.isBlank()) {
            return false
        }
        try {
            val clipboardManager: ClipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clipData = ClipData.newPlainText("Label", text)
            clipboardManager.setPrimaryClip(clipData)
            return true
        } catch (e: Exception) {
            return false
        }
    }

    fun fromHtml(string: String): Spanned {
        return Html.fromHtml(string, Html.FROM_HTML_MODE_LEGACY);
    }

    @JvmStatic
    fun packageInfo(context: Context): PackageInfo {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
    }

    fun versionCode(context: Context):Long {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo(context).longVersionCode
        } else {
            @Suppress("DEPRECATION")
            packageInfo(context).versionCode.toLong()
        }
    }

    fun stringForTime(timeMs: Int): String {
        val totalSeconds = timeMs / 1000
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        return if (hours > 0) {
            String.format(Locale.getDefault(), "%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.getDefault(), "%02d:%02d", minutes, seconds)
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun formatTime(time: Long, pattern: String): String {
        return SimpleDateFormat(pattern).format(Date(time))
    }

    @SuppressLint("SimpleDateFormat")
    fun parseDate(time: String, pattern: String): Date? {
        return try {
            SimpleDateFormat(pattern).parse(time)
        } catch (e: Exception) {
            Log.d(this, "parseDate", e)
            null
        }
    }

    // 预编译正则，提升性能
    private val datePatterns = listOf(
        // 1. 完整日期：YYYY-MM-DD, YY/MM/DD, YYYY年M月D日 等
        """\b\d{2,4}[-/.年。]\d{1,2}[-/.月。]\d{1,2}(日)?\b""",

        // 2. 年月格式：YYYY-MM, YY/MM, 2023年10月
        """\b\d{2,4}[-/.年。]\d{1,2}(月)?\b""",

        // 3. 月日格式（无年份）：MM-DD, M/D, 10月25日
        """\b\d{1,2}[-/.月。]\d{1,2}(日)?\b""",

        // 4. 四位或两位纯年份（独立出现，避免匹配 ID）
        """\b(19|20)\d{2}\b""",      // 1900-2099
        """\b[0-9]{2}\b(?!\d)""",    // 两位年（如 23），但不匹配 123 中的 23

        // 5. 合法时间格式：HH:mm, H:m （0～23 小时，0～59 分钟）
        """\b([01]?[0-9]|2[0-3]):[0-5]?[0-9]\b""",

        // 6. ★ 新增：6 位 YYMMDD 格式（如 170110 = 2017-01-10）
        """\b\d{6}\b"""
    ).map { Pattern.compile(it, Pattern.CASE_INSENSITIVE) }

    fun matchDateString(input: String): Boolean {
        if (input.isBlank()) return false

        for (pattern in datePatterns) {
            if (pattern.matcher(input).find()) {
                return true
            }
        }
        return false
    }

    fun isCloseToWhite(color: Int, threshold: Int = 30): Boolean {
        val r = Color.red(color)
        val g = Color.green(color)
        val b = Color.blue(color)

        // 计算与白色的 RGB 距离（白色是 (255, 255, 255)）
        val distance = kotlin.math.sqrt(
            (255 - r).toDouble().pow(2) +
                    (255 - g).toDouble().pow(2) +
                    (255 - b).toDouble().pow(2)
        )

        return distance <= threshold
    }

    fun isCloseToWhiteByLuminance(color: Int, threshold: Float = 0.1f): Boolean {
        val r = Color.red(color) / 255f
        val g = Color.green(color) / 255f
        val b = Color.blue(color) / 255f

        // 计算相对亮度（ITU-R BT.709 标准）
        val luminance = 0.2126 * r + 0.7152 * g + 0.0722 * b

        return luminance >= (1.0f - threshold)
    }

    fun isCloseToWhiteRgbInt(rgbInt: Int, threshold: Int = 30): Boolean {
        val r = (rgbInt shr 16) and 0xFF
        val g = (rgbInt shr 8) and 0xFF
        val b = rgbInt and 0xFF

        val distance = kotlin.math.sqrt(
            (255 - r).toDouble().pow(2) +
                    (255 - g).toDouble().pow(2) +
                    (255 - b).toDouble().pow(2)
        )

        return distance <= threshold
    }

}