package me.lingci.dy.player.room

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import me.lingci.dy.player.entity.MediaData
import me.lingci.dy.player.entity.SourceData
import me.lingci.dy.player.entity.VideoData
import me.lingci.dy.player.room.dao.MediaDataDao
import me.lingci.dy.player.room.dao.SourceDataDao
import me.lingci.dy.player.room.dao.VideoDataDao

/**
 * Room数据库抽象类
 */
@Database(
    entities = [
        MediaData::class,
        SourceData::class,
        VideoData::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(MediaLibTypeConverter::class, StorageTypeConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun mediaDataDao(): MediaDataDao
    abstract fun sourceDataDao(): SourceDataDao
    abstract fun videoDataDao(): VideoDataDao
}