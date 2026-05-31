package me.lingci.dy.player.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import me.lingci.dy.player.entity.MediaData
import me.lingci.dy.player.entity.MediaLibType
import me.lingci.dy.player.entity.MediaTypeEntity

/**
 * MediaData DAO接口
 */
@Dao
interface MediaDataDao {

    @Query("SELECT count(*) FROM media_data")
    fun count(): Int

    @Query("SELECT * FROM media_data order by `index` desc")
    fun list(): List<MediaData>

    @Query("SELECT t1.* FROM media_data t1 WHERE :count >= ( SELECT COUNT(*) FROM media_data t2 WHERE t2.media_type = t1.media_type AND t2.`index` >= t1.`index` ) ORDER BY t1.media_type, t1.`index` DESC")
    fun listByTypeAll(count: Int): List<MediaData>

    @Query("SELECT * FROM media_data WHERE media_type = :mediaType order by `index` desc")
    fun listByType(mediaType: MediaLibType): List<MediaData>

    @Query("SELECT * FROM media_data WHERE id = :id")
    fun getById(id: String): MediaData?

    @Insert
    fun insert(mediaData: MediaData)

    @Update
    fun update(mediaData: MediaData)

    @Delete
    fun delete(mediaData: MediaData)

    @Query("select media_type as type, storage_id as storageId, count(*) as size from media_data group by media_type, storage_id")
    fun listMediaType(): List<MediaTypeEntity>

}