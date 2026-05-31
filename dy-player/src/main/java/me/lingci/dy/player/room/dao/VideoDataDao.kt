package me.lingci.dy.player.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import me.lingci.dy.player.entity.VideoData

/**
 * VideoData DAO接口
 */
@Dao
interface VideoDataDao {

    @Query("SELECT count(*) FROM video_data")
    fun count(): Int

    @Query("SELECT * FROM video_data WHERE media_id = :mediaId")
    fun listByMedia(mediaId: String): List<VideoData>

    @Query("SELECT * FROM video_data order by id limit :ps offset ((:pn -1) * :ps)")
    fun page(pn: Int, ps: Int): List<VideoData>

    @Query("SELECT * FROM video_data WHERE id = :id")
    fun getById(id: String): VideoData?

    @Insert
    fun insertVideoData(videoData: VideoData)

    @Update
    fun updateVideoData(videoData: VideoData)

    @Delete
    fun deleteVideoData(videoData: VideoData)

}