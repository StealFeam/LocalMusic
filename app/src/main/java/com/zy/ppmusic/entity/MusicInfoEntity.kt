package com.zy.ppmusic.entity

import com.zy.ppmusic.utils.FileUtils
import java.io.Serializable
import java.util.Arrays

/**
 * @author ZhiTouPC
 */
class MusicInfoEntity : Serializable {
    var mediaId: String? = null
    var musicName: String? = null
    var artist: String? = null
    var queryPath: String? = null
    /**
     * 文件大小
     */
    var size: Long = 0
    /**
     * 时长
     */
    var duration: Long = 0
    /**
     * 专辑图片数据
     */
    var iconData: ByteArray? = null

    var isExits: Boolean = false
            get(){
            return FileUtils.isExits(queryPath)
        }

    constructor() {}

    constructor(mediaId: String?, musicName: String?, artist: String?, queryPath: String?,
                size: Long, duration: Long, iconData: ByteArray?) {
        this.mediaId = mediaId
        this.musicName = musicName
        this.artist = artist
        this.queryPath = queryPath
        this.size = size
        this.duration = duration
        this.iconData = iconData
    }

    override fun toString(): String {
        return "MusicInfoEntity{" +
                "mediaId='" + mediaId + '\''.toString() +
                ", musicName='" + musicName + '\''.toString() +
                ", artist='" + artist + '\''.toString() +
                ", queryPath='" + queryPath + '\''.toString() +
                ", size=" + size +
                ", duration=" + duration +
                ", iconData=" + Arrays.toString(iconData) +
                '}'.toString()
    }

    companion object {
        private const val serialVersionUID = -1016970004377532215L
    }


}
