package com.zy.ppmusic.entity

import androidx.room.Entity

/**
 * @author stealfeam
 */
@Entity(tableName = "music", primaryKeys = ["queryPath"])
data class MusicInfoEntity(
    val mediaId: String?,
    val musicName: String?,
    val artist: String?,
    val queryPath: String,
    val size: Long = 0, // 文件大小
    val duration: Long = 0, // 时长
    val iconData: ByteArray? // 专辑图片数据
) {

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as MusicInfoEntity
        return queryPath == other.queryPath
    }

    override fun hashCode(): Int {
        return queryPath.hashCode()
    }
}