package me.lingci.dy.player.util

import android.annotation.SuppressLint
import android.app.Activity
import android.app.ActivityOptions
import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.graphics.Bitmap
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import androidx.core.app.ActivityCompat
import androidx.core.app.ActivityOptionsCompat
import androidx.core.net.toUri
import androidx.core.view.WindowCompat
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import me.lingci.dy.player.PlayerApp
import me.lingci.dy.player.R
import me.lingci.lib.base.util.AppFile
import me.lingci.lib.base.util.logD
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.text.SimpleDateFormat
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Date
import java.util.Locale

/**
 *   @author : happyc
 *   time    : 2024/09/21
 *   desc    :
 *   version : 1.0
 */
object AppUtil {

    const val THUMB_TYPE = "webp"
    val COMPRESS_FORMAT = Bitmap.CompressFormat.WEBP

    fun buildThumbFile(context: Context, hash: String): File {
        return AppFile(context).buildCache(".thumb/${hash}.${THUMB_TYPE}")
    }

    fun today(): String {
        return SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(Date())
    }

    var mediaOptions: RequestOptions = RequestOptions()
        .placeholder(R.drawable.ic_media_default)
        .fallback(R.drawable.ic_media_default)
        .error(R.drawable.ic_media_default)

    /**
     * 将View从父控件中移除
     */
    fun removeViewFormParent(v: View?) {
        if (v == null) return
        val parent = v.parent
        if (parent is FrameLayout) {
            parent.removeView(v)
        }
    }

    fun isUrl(url: String): Boolean {
        val pattern = "https?://[-a-zA-Z0-9+&@#/%?=~_|!:,.;]*[-a-zA-Z0-9+&@#/%=~_|]"
        val regex = Regex(pattern)
        return regex.matches(url)
    }

    fun startWithAnim(activity: Activity, targetActivity: Class<out Activity>) {
        val intent = Intent(activity, targetActivity)
        val bundle = ActivityOptions.makeCustomAnimation(
            activity,
            R.anim.circular_enter_animation, R.anim.circular_exit_animation
        ).toBundle()
        activity.startActivity(intent, bundle)
    }

    // 使用共享元素动画
    fun startWithScene(
        activity: Activity,
        sharedView: View,
        bundle: Bundle,
        targetActivity: Class<out Activity>
    ) {
        val intent = Intent(activity, targetActivity)
        intent.putExtras(bundle)
        val options: ActivityOptionsCompat = ActivityOptionsCompat
            .makeSceneTransitionAnimation(
                activity,
                sharedView,
                bundle.getString("view_name")!!
            )
        ActivityCompat.startActivity(activity, intent, options.toBundle())
    }

    fun setStatusBarTransparent(activity: Activity) {
        WindowCompat.setDecorFitsSystemWindows(activity.window, false)
        activity.window.statusBarColor = Color.TRANSPARENT
        activity.window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            activity.window.isNavigationBarContrastEnforced = false
            activity.window.isStatusBarContrastEnforced = false
        }
    }

    fun resetViewPagerLayout(viewPager2: ViewPager2) {
        viewPager2.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                val layoutParams = viewPager2.layoutParams
                layoutParams.height = ViewGroup.LayoutParams.MATCH_PARENT
                viewPager2.layoutParams = layoutParams
                viewPager2.viewTreeObserver.removeOnGlobalLayoutListener(this)
            }
        })
    }

    val videoRegex = Regex("\\.(mp4|avi|mkv|flv|m4v|mov)$", RegexOption.IGNORE_CASE)
    
    fun isVideo(name:String): Boolean {
        return videoRegex.containsMatchIn(name)
    }

    fun parsePathFromIntent(intent: Intent): String? {
        val filepath = when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let { resolveUri(it) }
            Intent.ACTION_SEND -> intent.getStringExtra(Intent.EXTRA_TEXT)?.let {
                val uri = it.trim().toUri()
                if (uri.isHierarchical && !uri.isRelative) resolveUri(uri) else null
            }
            else -> intent.getStringExtra("filepath")
        }
        return filepath
    }

    private fun resolveUri(data: Uri): String? {
        val filepath = when (data.scheme) {
            "file" -> data.path
            "content" -> translateContentUri(data)
            // mpv supports data URIs but needs data:// to pass it through correctly
            "data" -> "data://${data.schemeSpecificPart}"
            "http", "https", "rtmp", "rtmps", "rtp", "rtsp", "mms", "mmst", "mmsh",
            "tcp", "udp", "lavf", "ftp"
                -> data.toString()
            else -> null
        }

        if (filepath == null) {
            logD("unknown scheme: ${data.scheme}")
        }
        return filepath
    }

    private fun translateContentUri(uri: Uri): String {
        logD("Resolving content URI: $uri")
        val resolver = PlayerApp.getAppContext().contentResolver
        val projection = arrayOf(MediaStore.Video.Media.DATA)
        var cursor: Cursor? = null
        try {
            cursor = resolver.query(uri, projection, null, null, null)
            if (cursor != null && cursor.moveToFirst()) {
                val columnIndex = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
                return cursor.getString(columnIndex)
            }
        } catch (e: Exception) {
            logD("Failed to open content fd: $e")
        } finally {
            cursor?.close()
        }
        try {
            resolver.openFileDescriptor(uri, "r")?.use { pfd ->
                // See if we can skip the indirection and read the real file directly
                val path = findRealPath(pfd.fd)
                if (path != null) {
                    logD("Found real file path: $path")
                    return path
                }
            }
        } catch(e: Exception) {
            logD("Failed to open content fd: $e")
        }

        // Otherwise, just let mpv open the content URI directly via ffmpeg
        return uri.toString()
    }

    fun findRealPath(fd: Int): String? {
        var ins: InputStream? = null
        try {
            val path = File("/proc/self/fd/${fd}").canonicalPath
            if (!path.startsWith("/proc") && File(path).canRead()) {
                // Double check that we can read it
                ins = FileInputStream(path)
                ins.read()
                return path
            }
        } catch(e: Exception) { } finally { ins?.close() }
        return null
    }

    @SuppressLint("SimpleDateFormat")
    fun formatNow(pattern: String = "yyyyMMddHHmmss"): String {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            LocalDateTime.now().format(DateTimeFormatter.ofPattern(pattern))
        } else {
            SimpleDateFormat(pattern).format(Date())
        }
    }

    fun pauseImages(context: Context) {
        Glide.with(context).pauseRequests()
    }

    fun resumeImages(context: Context) {
        Glide.with(context).resumeRequests()
    }


}
