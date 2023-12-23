package com.zy.ppmusic.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.zy.ppmusic.data.db.dao.MusicDao
import com.zy.ppmusic.entity.MusicInfoEntity

@Database(entities = [MusicInfoEntity::class], version = 1, exportSchema = false)
abstract class MusicDatabase : RoomDatabase() {
    abstract fun musicDao(): MusicDao
}