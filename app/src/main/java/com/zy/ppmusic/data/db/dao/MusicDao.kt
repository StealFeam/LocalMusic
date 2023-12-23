package com.zy.ppmusic.data.db.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import com.zy.ppmusic.entity.MusicInfoEntity

@Dao
interface MusicDao {

    @Transaction
    @Query("SELECT * FROM music")
    fun all(): List<MusicInfoEntity>

    @Query("SELECT * FROM music WHERE queryPath LIKE :path")
    fun queryByPath(path: String): List<MusicInfoEntity>

    @Transaction
    @Insert
    fun insert(vararg musics: MusicInfoEntity)

    @Transaction
    @Delete
    fun delete(musics: MusicInfoEntity)
}