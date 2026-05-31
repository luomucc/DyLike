package me.lingci.dy.player.room.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import me.lingci.dy.player.entity.SourceData

/**
 * SourceData DAO接口
 */
@Dao
interface SourceDataDao {

    @Query("SELECT count(*) FROM source_data")
    fun count(): Int

    @Query("SELECT * FROM source_data")
    fun list(): List<SourceData>

    @Query("SELECT * FROM source_data WHERE id = :id")
    fun getById(id: String): SourceData?

    @Insert
    fun insert(sourceData: SourceData)

    @Update
    fun update(sourceData: SourceData)

    @Delete
    fun delete(sourceData: SourceData)

}