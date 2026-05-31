package me.lingci.lib.base.storage.entity

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import java.io.File
import kotlinx.serialization.Serializable
import me.lingci.lib.base.util.md5
import java.text.Collator
import java.util.Locale
import java.util.regex.Pattern

/**
 *   @author : happyc
 *   time    : 2024/09/21
 *   desc    :
 *   version : 1.0
 */
@OptIn(InternalSerializationApi::class)
@Serializable
@Parcelize
data class FileEntity(

    var id: String = "",
    var title: String = "",
    var name: String = "",
    var path: String = "",
    var fullPath: String = "",
    var isFile: Boolean = true,
    var childSize: Int = 0,
    var returnParent: Boolean = false,
    var type: StorageType = StorageType.LOCAL_STORAGE,
    var selected: Boolean = false,
    var size: Long = 0,
    var lastModified: Long = 0,
    var storageId: String = "",
    var mimeType: String = ""

    ) : Parcelable {

    constructor(file: File) : this(
        title = file.name,
        name = file.name,
        path = file.path,
        isFile = file.isFile,
        type = StorageType.LOCAL_STORAGE,
        size = file.length()
    )

    fun thumbName(): String? {
        if (id.isNotBlank()) return id
        if (fullPath.isBlank()) return null
        return fullPath.md5()
    }

}

val FileEntity.parent get() = path.substringBeforeLast("/")


/**
 * 文件实体类，代表一个文件或目录
 */
data class FileEntity2(
    val id: String, // 唯一标识
    val name: String, // 文件名
    val path: String, // 文件路径
    val isDirectory: Boolean, // 是否为目录
    val size: Long, // 文件大小（字节）
    val lastModified: Long, // 最后修改时间
    val storageType: StorageType, // 存储类型
    val storageId: String, // 所属存储的ID
    val mimeType: String? = null, // MIME类型
    val thumbnailUrl: String? = null // 缩略图URL（如果有）
)