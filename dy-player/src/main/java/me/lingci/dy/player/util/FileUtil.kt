package me.lingci.dy.player.util

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import me.lingci.lib.base.storage.entity.FileEntity
import java.io.*
import java.util.Locale


object FileUtil {

    private const val PERMISSION_REQUEST_CODE = 1

    private val VIDEO_EXTENSIONS: List<String> = mutableListOf(
        ".mp4", ".avi", ".mov", ".mkv", ".flv", ".wmv", ".3gp", ".m4a", ".m4v"
    )

    private val IMAGE_EXTENSIONS = listOf(".jpg", ".jpeg", ".png", ".gif", ".webp")

    /**
     * 检查并请求读写权限
     */
    @SuppressLint("ObsoleteSdkInt")
    fun checkAndRequestPermissions(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val readPermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.READ_EXTERNAL_STORAGE
            )
            val writePermission = ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE
            )

            if (readPermission != PackageManager.PERMISSION_GRANTED || writePermission != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    context as Activity,
                    arrayOf(
                        android.Manifest.permission.READ_EXTERNAL_STORAGE,
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ),
                    PERMISSION_REQUEST_CODE
                )
                return false
            }
        }
        return true
    }

    /**
     * 写入文件
     */
    fun writeFile(context: Context, fileName: String, content: String): Boolean {
        if (!checkAndRequestPermissions(context)) {
            return false
        }
        return try {
            val file = getExternalFile(fileName)
            val writer = FileWriter(file)
            writer.write(content)
            writer.close()
            true
        } catch (e: IOException) {
            e.printStackTrace()
            false
        }
    }

    /**
     * 读取文件
     */
    fun readFile(context: Context, fileName: String): String? {
        if (!checkAndRequestPermissions(context)) {
            return null
        }
        return try {
            val file = getExternalFile(fileName)
            if (!file.exists()) {
                return null
            }
            val reader = BufferedReader(FileReader(file))
            val stringBuilder = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                stringBuilder.append(line)
            }
            reader.close()
            stringBuilder.toString()
        } catch (e: IOException) {
            e.printStackTrace()
            null
        }
    }

    /**
     * 获取外部存储的文件
     */
    @JvmStatic
    fun getExternalFile(fileName: String): File {
        return File(Environment.getExternalStorageDirectory(), fileName)
    }

    fun getExternalPath(fileName: String): String {
        return getExternalFile(fileName).path
    }

    fun getAppFiles(context: Context, fileName: String): File {
        return File(context.getExternalFilesDir(null), fileName)
    }

    fun videoFileSize(folderBean: FileEntity): Int {
        folderBean.path.let { path ->
            File(path).listFiles { file ->
                isVideoFile(file)
            }?.let {
                return it.size
            }
        }
        return 0
    }

    fun listFolder(folderBean: FileEntity): ArrayList<FileEntity> {
        folderBean.path.let { path ->
            File(path).listFiles { file -> file.isDirectory && !file.name.startsWith(".") }
                ?.map { file ->
                    FileEntity(file)
                }?.let {
                    return ArrayList(it)
                }
        }
        return ArrayList()
    }

    @JvmStatic
    fun isVideoFile(file: File?): Boolean {
        if (file == null || !file.exists() || file.isDirectory) {
            return false
        }
        val dotIndex = file.name.lastIndexOf('.')
        if (dotIndex == -1) {
            return false
        }
        val extension = file.name.substring(dotIndex).lowercase(Locale.getDefault())
        return VIDEO_EXTENSIONS.contains(extension)
    }

    @JvmStatic
    fun isImageFile(file: File?): Boolean {
        if (file == null || !file.exists() || file.isDirectory) {
            return false
        }
        val dotIndex = file.name.lastIndexOf('.')
        if (dotIndex == -1) {
            return false
        }
        return IMAGE_EXTENSIONS.any { ext ->
            file.name.lowercase().endsWith(ext)
        }
    }

}