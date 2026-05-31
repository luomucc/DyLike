package me.lingci.dy.player.room

import androidx.room.Room
import me.lingci.dy.player.PlayerApp
import me.lingci.dy.player.room.dao.MediaDataDao
import me.lingci.dy.player.room.dao.SourceDataDao
import me.lingci.dy.player.room.dao.VideoDataDao


/**
 * 数据库管理类，用于初始化和获取数据库实例
 */
class DatabaseManager() {

    companion object {

        @Volatile
        private var INSTANCE: AppDatabase = Holder.dbManager.db

    }

    private object Holder {
        val dbManager = DatabaseManager()
    }

    private val db: AppDatabase by lazy {
        Room.databaseBuilder(
            PlayerApp.getAppContext(),
            AppDatabase::class.java,
            "dy_like_db"
        )
            .fallbackToDestructiveMigration() // 数据库升级时数据会丢失
            .build()
    }

    fun mediaDataDao(): MediaDataDao = db.mediaDataDao()
    fun sourceDataDao(): SourceDataDao = db.sourceDataDao()
    fun videoDataDao(): VideoDataDao = db.videoDataDao()

}