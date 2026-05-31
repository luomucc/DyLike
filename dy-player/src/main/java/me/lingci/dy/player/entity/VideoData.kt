package me.lingci.dy.player.entity

import android.os.Parcelable
import androidx.core.net.toUri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import me.lingci.dy.player.room.StorageTypeConverter
import me.lingci.lib.base.okhttp.RedirectResolver
import me.lingci.lib.base.storage.entity.StorageType
import me.lingci.lib.base.util.CodeUtil
import me.lingci.lib.base.util.Log
import me.lingci.lib.base.util.md5
import java.io.File

/**
 *   @author : happyc
 *   time    : 2024/09/21
 *   desc    :
 *   version : 1.0
 */
@OptIn(InternalSerializationApi::class)
@Serializable
@Parcelize
@Entity(tableName = "video_data")
@TypeConverters(StorageTypeConverter::class)
data class VideoData(

    @PrimaryKey
    var id: String = "",
    var name: String = "",
    @ColumnInfo(name = "media_id")
    var mediaId: String = "",
    @ColumnInfo(name = "video_url")
    var videoUrl: String = "",
    var dmUrl: String = "",
    @Ignore
    var headers: MutableMap<String, String> = mutableMapOf(),
    var preview: String = "",
    @ColumnInfo(name = "storage_type", typeAffinity = ColumnInfo.INTEGER)
    var type: StorageType = StorageType.WEBDAV,
    @ColumnInfo(name = "last_play")
    var lastPlay: Boolean = false,
    @Ignore
    var like: Boolean = false,
    @Ignore
    var selected: Boolean = false,
    @ColumnInfo(name = "parent_path")
    var parentPath: String = "",

    ) : Parcelable {

    companion object {
        const val TOKEN_KEY: String = "Authorization"
    }

    constructor() : this(id = "")

    constructor(file: File) : this(
        name = file.name,
        videoUrl = file.path,
        type = StorageType.LOCAL_STORAGE,
        parentPath = file.parentFile?.name ?: ""
    )

    fun putToken(token: String) {
        headers[TOKEN_KEY] = token
    }

    fun getToken(): String? {
        return headers[TOKEN_KEY]
    }

    fun md5(): String {
        if (id.isNotBlank()) {
            return id
        }
        return CodeUtil.md5(videoUrl)
    }

    fun beanId(): String {
        if (id.isBlank()) {
            return videoUrl.md5()
        }
        return id
    }

    /**
     * 获取播放URL。当类型为WEBDAV且URL以http开头时，会同步执行网络请求解析重定向。
     * 注意：此方法可能阻塞，禁止在主线程调用，必须在IO线程执行。
     */
    fun playUrl(): String {
        Log.d(this@VideoData, this)
        return if (type == StorageType.WEBDAV && videoUrl.startsWith("http")) {
            RedirectResolver.getRedirectUrl(videoUrl, headers)
        } else {
            videoUrl
        }
    }

    fun is302(url: String): Boolean {
        if (url.startsWith("http")) {
            return try {
                url.toUri().host != videoUrl.toUri().host
            } catch (e: Exception) {
                false
            }
        }
        return false
    }

    val dmLink: String
        get() = "${videoUrl.substringBeforeLast(".")}.xml"

    val dmSubLink: String
        get() = "${videoUrl.substringBeforeLast(".")}_合并.xml"

    val srtLink: String
        get() = "${videoUrl.substringBeforeLast(".")}.srt"

    val assLink: String
        get() = "${videoUrl.substringBeforeLast(".")}.ass"

}