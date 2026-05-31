package me.lingci.dy.player.entity

import android.os.Parcelable
import androidx.core.net.toUri
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import me.lingci.dy.player.room.StorageTypeConverter
import me.lingci.lib.base.storage.IStorage
import me.lingci.lib.base.storage.entity.StorageConfig
import me.lingci.lib.base.storage.entity.StorageType
import me.lingci.lib.base.storage.impl.LocalStorage
import me.lingci.lib.base.storage.impl.WebDavStorage

/**
 *   @author : happyc
 *   time    : 2024/09/21
 *   desc    :
 *   version : 1.0
 */
@OptIn(InternalSerializationApi::class)
@Serializable
@Parcelize
@Entity(tableName = "source_data")
@TypeConverters(StorageTypeConverter::class)
data class SourceData(

    @PrimaryKey
    var id: String = "",
    var title: String = "",
    @ColumnInfo(name = "site_url")
    var siteUrl: String = "",
    var username: String = "guest",
    var password: String = "guest",
    var schema: String = "",
    var port: String = "",
    var index: Int = 0,
    @ColumnInfo(name = "storage_type", typeAffinity = ColumnInfo.INTEGER)
    var type: StorageType? = null

) : Parcelable {

    constructor() : this(id = "")

    fun baseUrl(): String {
        if (siteUrl.isBlank()) {
            return ""
        }
        val uri = siteUrl.toUri()
        return uri.scheme + "://" + uri.authority
    }

    fun storageType(): StorageType {
        if (type == null) {
            return StorageType.WEBDAV
        }
        return type!!
    }

    fun isCustomType(): Boolean {
        return type != StorageType.LOCAL_STORAGE && type != StorageType.STREAM_LINK
    }

    fun toStorage(): IStorage? {
        return when (type) {
            StorageType.LOCAL_STORAGE -> {
                LocalStorage(
                    StorageConfig.LocalStorageConfig(
                        id = id,
                        name = title,
                        rootPath = siteUrl
                    )
                )
            }

            StorageType.WEBDAV -> {
                WebDavStorage(
                    StorageConfig.WebDavStorageConfig(
                        id = id,
                        name = title,
                        url = siteUrl,
                        username = username,
                        password = password
                    ), ""
                )
            }

            else -> null
        }
    }

}