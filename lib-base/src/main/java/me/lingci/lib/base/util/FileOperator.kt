package me.lingci.lib.base.util

import android.os.Build
import android.os.Environment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.lingci.lib.base.storage.entity.FileEntity
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStreamWriter
import java.nio.charset.Charset
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.Collator
import java.util.Locale
import java.util.regex.Pattern

/**
 *   @author : happyc
 *   time    : 2025/02/27
 *   desc    :
 *   version : 1.0
 */
object FileOperator {

    private const val XML_EXTENSION: String = ".xml"
    val VIDEO_EXTENSIONS = arrayListOf(
        ".mp4", ".avi", ".mov", ".mkv", ".flv", ".wmv", ".3gp", ".m4a", ".m4v", ".m4s"
    )
    val IMAGE_EXTENSIONS = arrayListOf(".jpg", ".jpeg", ".png", ".gif", ".webp")
    val XML_EXTENSIONS = arrayListOf(XML_EXTENSION)
    val DM_EXTENSIONS = arrayListOf(XML_EXTENSION, ".zip", ".7z", ".rar")
    val JSON_EXTENSIONS = arrayListOf(".json")
    val TEXT_EXTENSIONS = XML_EXTENSIONS + JSON_EXTENSIONS
    val FONT_EXTENSIONS = arrayListOf(".ttf", ".ttc")
    val PROTOBUF_EXTENSIONS = arrayListOf(".so", ".pb")

    private const val LOCAL_BASE = "/storage/emulated/0"

    @JvmStatic
    val rootFolder: File
        get() {
            return try {
                Environment.getExternalStorageDirectory()
            } catch (e: Exception) {
                File(LOCAL_BASE)
            }
        }

    @JvmStatic
    val downloadFolder: File get() = File(rootFolder, Environment.DIRECTORY_DOWNLOADS)

    @JvmStatic
    val movieFolder: File get() = File(rootFolder, Environment.DIRECTORY_MOVIES)

    @JvmStatic
    val pictureFolder: File get() = File(rootFolder, Environment.DIRECTORY_PICTURES)

    @JvmStatic
    fun buildDownFile(name: String): File {
        return File(downloadFolder, name)
    }

    @JvmStatic
    fun buildDownFile(parent: String, name: String): File {
        return File(File(downloadFolder, parent), name)
    }

    @JvmStatic
    fun buildDownXmlFile(parent: String, name: String): File {
        return buildDownFile(parent, name + XML_EXTENSION)
    }

    @JvmStatic
    fun buildPictureFile(parent: String, name: String): File {
        return File(File(pictureFolder, parent), name)
    }

    @JvmStatic
    fun buildFile(parent: String, name: String): File {
        return File(parent, name)
    }

    @JvmStatic
    fun buildXmlFile(parent: String, name: String): File {
        return buildFile(parent, name + XML_EXTENSION)
    }

    fun relativePath(path: String): String {
        return path.substringAfter(LOCAL_BASE)
    }

    @JvmStatic
    fun readText(filePath: String): String {
        return readText(File(filePath))
    }

    @JvmStatic
    fun readText(file: File): String {
        if (!file.exists() || !file.isFile) {
            Log.d(this@FileOperator, "readText not found", file.path)
            return ""
        }
        return file.readText(Charsets.UTF_8)
    }

    @JvmStatic
    fun readText(inputStream: InputStream): String {
        try {
            ByteArrayOutputStream().use { out ->
                inputStream.use { input ->
                    input.copyTo(out)
                }
                return out.toString(Charset.defaultCharset().name())
            }
        } catch (e: Exception) {
            Log.d(this, "read is failed", e)
            return ""
        }
    }

    @JvmStatic
    fun writeText(filePath: String, content: String): Boolean {
        return writeText(File(filePath), content)
    }

    @JvmStatic
    fun writeText(file: File, content: String): Boolean {
        return try {
            file.createNew()
            //file.writeText(content)
            BufferedWriter(
                OutputStreamWriter(
                    FileOutputStream(file),
                    Charsets.UTF_8
                )
            ).use { writer ->
                writer.write(content)
            }
            true
        } catch (e: Exception) {
            Log.d(this, "write file failed", file.path, e)
            false
        }
    }

    @JvmStatic
    fun writeText(
        scope: CoroutineScope,
        file: File,
        content: String,
        callback: (Boolean) -> Unit = {}
    ) {
        scope.launch(Dispatchers.IO) {
            writeText(file, content).let {
                withContext(Dispatchers.Main) {
                    callback.invoke(it)
                }
            }
        }
    }

    @JvmStatic
    fun getChildFileSize(folder: File, extensions: ArrayList<String>): Int {
        if (folder.canRead().not() || folder.isFile) {
            return 0
        }
        folder.listFiles { file -> file.extensionFile(extensions) }?.let {
            return it.size
        }
        return 0
    }

    @JvmStatic
    fun getSortedFolders(folder: File): List<File> {
        if (folder.canRead().not() || folder.isFile) {
            return emptyList()
        }
        return folder.listFiles {
            it.isDirectory && !it.startDot() && !it.noChild()
        }?.sortedWith(fileNameComparator())?.toList() ?: emptyList()
    }

    @JvmStatic
    fun getSortedFiles(folder: File, extensions: ArrayList<String>): List<File> {
        if (folder.canRead().not() || folder.isFile) {
            return emptyList()
        }
        folder.listFiles { file -> file.extensionFile(extensions) }?.let { list ->
            return list.sortedWith(fileNameComparator())
        }
        return emptyList()
    }

    @JvmStatic
    fun getSortedFiles(
        folder: File,
        folderTop: Boolean,
        extensions: ArrayList<String>
    ): List<File> {
        if (folder.canRead().not() || folder.isFile) {
            return emptyList()
        }
        val allFiles = folder.listFiles()?.toList() ?: emptyList()

        // 筛选出指定文件
        val files = allFiles.filter { it.extensionFile(extensions) }
            .sortedWith(fileNameComparator())

        val folders =
            allFiles.filter { it.isDirectory && !it.startDot() && !it.noChild() }.sortedWith(
                fileNameComparator()
            )

        return if (folderTop) {
            folders + files;
        } else {
            files + folders
        }
    }

    @JvmStatic
    fun getSortedFiles(
        folder: File,
        folderTop: Boolean,
        extensions: ArrayList<String>,
        callback: (List<FileEntity>) -> Unit
    ) {
        getSortedFiles(folder, folderTop, extensions).map { FileEntity(it) }.let {
            callback.invoke(it)
        }
    }

    @JvmStatic
    fun fileTimeComparator(): Comparator<File> {
        return Comparator{file1, file2 ->
            file1.lastModified().compareTo(file2.lastModified())
        }
    }

    @JvmStatic
    fun fileSizeComparator(): Comparator<File> {
        return Comparator{file1, file2 ->
            file1.length().compareTo(file2.length())
        }
    }

    @JvmStatic
    fun fileNameComparator(): Comparator<File> {
        @Suppress("RegExpSimplifiable")
        val pattern = Pattern.compile("(\\d+)|([^\\d]+)")
        val collator = Collator.getInstance(Locale.CHINA).apply {
            // 忽略大小写和重音符号的差异
            strength = Collator.PRIMARY
        }
        return Comparator{file1, file2 ->
            // 分割两个文件名成数字和非数字片段
            val matcher1 = pattern.matcher(file1.name)
            val matcher2 = pattern.matcher(file2.name)

            while (matcher1.find() && matcher2.find()) {
                val segment1 = matcher1.group()
                val segment2 = matcher2.group()

                // 比较当前片段
                val comparison = if (segment1[0].isDigit() && segment2[0].isDigit()) {
                    // 都是数字，按数值大小比较
                    try {
                        segment1.toLong().compareTo(segment2.toLong())
                    } catch (e: Exception) {
                        // 超过数字长度，用文字匹配
                        collator.compare(segment1, segment2)
                    }
                } else {
                    // 非数字，按文字规则比较
                    collator.compare(segment1, segment2)
                }

                if (comparison != 0) {
                    return@Comparator comparison
                }
            }

            // 处理一个字符串是另一个字符串前缀的情况
            matcher1.find().compareTo(matcher2.find())
            //collator.compare(file1.name, file2.name)
        }
    }

    @JvmStatic
    fun parentPathFileNameComparator(): Comparator<File> {
        val nameComparator = fileNameComparator()
        return Comparator { file1, file2 ->
            val parent1 = file1.parentFile?.name ?: ""
            val parent2 = file2.parentFile?.name ?: ""
            val parentCompare = nameComparator.compare(File(parent1), File(parent2))
            if (parentCompare != 0) {
                parentCompare
            } else {
                nameComparator.compare(file1, file2)
            }
        }
    }

    fun copyFile(source: File, dest: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Files.copy(source.toPath(), dest.toPath(), StandardCopyOption.REPLACE_EXISTING)
        } else {
            source.copyTo(dest, true)
        }
    }



}