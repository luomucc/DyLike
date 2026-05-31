package me.lingci.dy.player.room

import me.lingci.dy.player.entity.CoverType
import me.lingci.dy.player.entity.MediaLibType
import me.lingci.lib.base.storage.entity.StorageType


/**
 * MediaLibType类型转换器
 */
class MediaLibTypeConverter {
    @androidx.room.TypeConverter
    fun fromMediaLibType(type: MediaLibType): Int {
        return type.value
    }

    @androidx.room.TypeConverter
    fun toMediaLibType(value: Int): MediaLibType {
        return MediaLibType.fromValue(value)
    }
}

/**
 * StorageType类型转换器
 */
class StorageTypeConverter {
    @androidx.room.TypeConverter
    fun fromStorageType(type: StorageType): String {
        return type.name
    }

    @androidx.room.TypeConverter
    fun toStorageType(value: String): StorageType {
        return try {
            StorageType.valueOf(value)
        } catch (e: IllegalArgumentException) {
            StorageType.LOCAL_STORAGE
        }
    }
}

/**
 * CoverType类型转换器
 */
class CoverTypeConverter {
    @androidx.room.TypeConverter
    fun fromCoverType(type: CoverType): Int {
        return type.value
    }

    @androidx.room.TypeConverter
    fun toCoverType(value: Int): CoverType {
        return CoverType.fromValue(value)
    }
}