package com.zy.ppmusic.utils

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.util.ArrayMap
import android.text.TextUtils
import android.util.Log
import com.zy.ppmusic.entity.MusicInfoEntity
import java.io.File

/**
 * 数据转换
 * @author ZhiTouPC
 */
class DataTransform private constructor() {
    /**
     * 可存放本地的数据
     */
    @Volatile
    var musicInfoEntities: ArrayList<MusicInfoEntity>? = null
    val queueItemList: ArrayList<MediaSessionCompat.QueueItem>
    val mediaItemList: ArrayList<MediaBrowserCompat.MediaItem>
    private val mapMetadataArray: ArrayMap<String, MediaMetadataCompat>
    /**
     * 扫描到的路径
     */
    val pathList: ArrayList<String> = ArrayList()
    /**
     * 用于获取mediaId的位置
     */
    val mediaIdList: ArrayList<String>

    val metadataCompatList: Map<String, MediaMetadataCompat>
        get() = mapMetadataArray

    private object Inner {
        val INSTANCE = DataTransform()
    }

    init {
        musicInfoEntities = ArrayList()
        mapMetadataArray = ArrayMap()
        queueItemList = ArrayList()
        mediaItemList = ArrayList()
        mediaIdList = ArrayList()
    }

    /**
     * 从本地扫描得到数据转换
     */
    fun transFormData(context: Context, pathList: ArrayList<String>) {
        clearData()
        queryMedia(context, pathList)
    }

    private fun clearData() {
        if (this.pathList.size > 0) {
            this.pathList.clear()
            musicInfoEntities!!.clear()
            mapMetadataArray.clear()
            queueItemList.clear()
            mediaIdList.clear()
            mediaItemList.clear()
        }
    }

    /**
     * 从系统媒体库获取信息
     * @param context   context
     * @param localList 路径列表
     */
    private fun queryMedia(context: Context, localList: List<String>) {
        val contentResolver = context.contentResolver
        var oldUri: Uri? = null
        var builder: MediaMetadataCompat.Builder
        var isNeedRe = false

        val mediaMetadataRetriever = MediaMetadataRetriever()

        for (itemPath in localList) {
            if (mediaIdList.contains(itemPath.hashCode().toString())) {
                continue
            } else {
                PrintLog.print(itemPath)
            }
            //根据音频地址获取uri，区分为内部存储和外部存储
            val audioUri = MediaStore.Audio.Media.getContentUriForPath(itemPath)
            //仅查询是音乐的文件
            val query = contentResolver.query(audioUri, null,  MediaStore.Audio.Media.IS_MUSIC+"=?", arrayOf("1"), null)
            if (query != null) {
                //判断如果是上次扫描的uri则跳过，系统分为内部存储uri的音频和外部存储的uri
                if (oldUri != null && oldUri == audioUri) {
                    query.close()
                    continue
                } else {
                    //遍历得到内部或者外部存储的所有媒体文件的信息
                    while (query.moveToNext()) {
                        val title = query.getString(query.getColumnIndex(MediaStore.Audio.Media.TITLE))
                        val artist = query.getString(query.getColumnIndex(MediaStore.Audio.Media.ARTIST))
                        val duration = query.getLong(query.getColumnIndex(MediaStore.Audio.Media.DURATION))
                        val size = query.getString(query.getColumnIndex(MediaStore.Audio.Media.SIZE))
                        val queryPath = query.getString(query.getColumnIndex(MediaStore.Audio.Media.DATA))
                        //过滤本地不存在的媒体文件
                        if (!FileUtils.isExits(queryPath)) {
                            continue
                        }
                        //过滤小于30s的文件
                        if (duration < 30L * 1000L) {
                            continue
                        }

                        //过滤系统媒体库中其他类型的媒体文件
                        if (!isSupportMediaType(queryPath)) {
                            continue
                        }

                        mediaMetadataRetriever.setDataSource(queryPath)
                        val mBitmap: Bitmap? = mediaMetadataRetriever.embeddedPicture?.let {
                            BitmapFactory.decodeByteArray(it, 0, it.size)
                        }

                        builder = MediaMetadataCompat.Builder()
                        //唯一id
                        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, queryPath.hashCode().toString())
                        //文件路径
                        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, queryPath)
                        //显示名称
                        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title)
                        //作者
                        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, StringUtils
                                .ifEmpty(artist, "<未知作者>"))
                        //作者
                        builder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, StringUtils
                                .ifEmpty(artist, "<未知作者>"))

                        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, mBitmap)

                        //时长
                        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)

                        val metadataCompatItem = builder.build()

                        mapMetadataArray[queryPath.hashCode().toString()] = metadataCompatItem

                        val queueItem = MediaSessionCompat.QueueItem(
                                metadataCompatItem.description, queryPath.hashCode().toLong())
                        queueItemList.add(queueItem)

                        val mediaItem = MediaBrowserCompat.MediaItem(
                                metadataCompatItem.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
                        mediaItemList.add(mediaItem)

                        val infoEntity = MusicInfoEntity(queryPath.hashCode().toString(), title,
                                artist, queryPath, size.toLong(), duration, mediaMetadataRetriever.embeddedPicture)
                        musicInfoEntities!!.add(infoEntity)
                        pathList.add(queryPath)
                        mediaIdList.add(queryPath.hashCode().toString())
                    }
                    //去除媒体库中不存在的
                    if (!mediaIdList.contains(itemPath.hashCode().toString())) {
                        pathList.remove(itemPath)
                        isNeedRe = true
                    }
                }

            } else {
                isNeedRe = true
            }
            StreamUtils.closeIo(query)
            oldUri = audioUri
        }

        if (isNeedRe) {
            reQueryList(localList)
            Log.e(TAG, "queryMedia: isNeedRe " + pathList.size)
        } else if (localList.size > pathList.size) {
            reQueryList(localList)
        }
        Log.d(TAG, "queryResolver() called with: context = [$context]")
    }

    private fun isSupportMediaType(queryPath: String): Boolean {
        for (type in SupportMediaType.SUPPORT_TYPE) {
            if (queryPath.endsWith(type)) {
                return true
            }
        }
        return false
    }

    /**
     * 重新对数据遍历，筛选出系统ContentProvider中不存在的媒体
     * @param list 扫描到的数据列表
     */
    private fun reQueryList(list: List<String>) {
        var builder: MediaMetadataCompat.Builder
        val retriever = MediaMetadataRetriever()
        val listSize = list.size
        for (i in 0 until listSize) {
            val itemPath = list[i]
            if (!this.pathList.contains(itemPath)) {
                retriever.setDataSource(itemPath)
                //METADATA_KEY_ALBUM 专辑
                val titleS = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val artistS = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val durationS = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                var length: Long = 0
                if (!TextUtils.isEmpty(durationS)) {
                    length = durationS.toLong()
                    if (length < 20 * 1000) {
                        continue
                    }
                }
                builder = MediaMetadataCompat.Builder()
                val musicName = getMusicName(itemPath)
                builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, itemPath.hashCode().toString())
                builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, itemPath)
                builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                        StringUtils.ifEmpty(titleS, musicName))

                val bitmap: Bitmap? = retriever.embeddedPicture?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size)
                }

                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap)

                //作者
                builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                        StringUtils.ifEmpty(artistS, "<未知作者>"))
                //作者
                builder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR,
                        StringUtils.ifEmpty(artistS, "<未知作者>"))
                //时长
                builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, length)

                val metadataCompatItem = builder.build()

                mapMetadataArray[itemPath.hashCode().toString()] = metadataCompatItem

                val queueItem = MediaSessionCompat.QueueItem(
                        metadataCompatItem.description, itemPath.hashCode().toLong())
                queueItemList.add(queueItem)

                val mediaItem = MediaBrowserCompat.MediaItem(
                        metadataCompatItem.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
                mediaItemList.add(mediaItem)

                val infoEntity = MusicInfoEntity(itemPath.hashCode().toString(),
                        musicName!!, artistS, itemPath, 0, length, retriever.embeddedPicture)
                musicInfoEntities!!.add(infoEntity)
                pathList.add(itemPath)
                mediaIdList.add(itemPath.hashCode().toString())
            }
        }
    }

    /**
     * 从本地缓存得到的数据转换
     *
     * @param localList 本地缓存数据
     */
    fun transFormData(localList: ArrayList<MusicInfoEntity>) {
        clearData()
        this.musicInfoEntities = localList
        var metadataCompatItem: MediaMetadataCompat
        val dataSize = musicInfoEntities!!.size
        for (i in 0 until dataSize) {
            val itemEntity = musicInfoEntities!![i]
            pathList.add(itemEntity.queryPath!!)
            mediaIdList.add(itemEntity.mediaId!!)

            itemEntity.isExits = FileUtils.isExits(itemEntity.queryPath)

            val itemBuilder = MediaMetadataCompat.Builder()
            //唯一id
            itemBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, itemEntity.mediaId)
            //文件路径
            itemBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, itemEntity.queryPath)
            //显示名称
            itemBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, itemEntity.musicName)
            //作者
            itemBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, itemEntity.artist)
            //作者
            itemBuilder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, itemEntity.artist)

            itemBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, itemEntity.duration)

            itemBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, if (itemEntity.iconData == null)
                null
            else
                BitmapFactory.decodeByteArray(itemEntity.iconData, 0, itemEntity.iconData!!.size))

            metadataCompatItem = itemBuilder.build()

            mapMetadataArray[itemEntity.mediaId] = metadataCompatItem

            val queueItem = MediaSessionCompat.QueueItem(
                    metadataCompatItem.description, java.lang.Long.parseLong(itemEntity.mediaId))
            queueItemList.add(queueItem)

            val mediaItem = MediaBrowserCompat.MediaItem(
                    metadataCompatItem.description, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
            mediaItemList.add(mediaItem)
        }
    }

    fun removeItem(context: Context, index: Int) {
        val path = pathList[index]
        val file = File(path)
        file.deleteOnExit()
        if (!file.exists()) {
            val contentResolver = context.contentResolver
            val audioUri = MediaStore.Audio.Media.getContentUriForPath(path)
            val where = MediaStore.Images.Media.DATA + "='" + path + "'"
            contentResolver.delete(audioUri, where, null)
        }
        this.pathList.removeAt(index)
        musicInfoEntities!!.removeAt(index)
        mapMetadataArray.remove(this.mediaIdList[index])
        queueItemList.removeAt(index)
        mediaIdList.removeAt(index)
        mediaItemList.removeAt(index)
    }

    /**
     * 获取文件媒体名称，仅在媒体文件中信息丢失时使用
     * @param path 文件路径
     * @return 媒体显示名称
     */
    private fun getMusicName(path: String?): String? {
        return path?.substring(path.lastIndexOf("/") + 1, path.lastIndexOf("."))
    }

    fun getMusicInfoEntities(): List<MusicInfoEntity>? {
        return musicInfoEntities
    }

    fun getQueueItemList(): List<MediaSessionCompat.QueueItem> {
        return queueItemList
    }

    fun getMediaIdList(): List<String> {
        return mediaIdList
    }

    fun getPath(position: Int): String? {
        return if (position >= 0 && position < pathList.size) {
            pathList[position]
        } else {
            null
        }
    }

    fun getMediaIndex(mediaId: String): Int {
        return if (mediaIdList.contains(mediaId)) {
            mediaIdList.indexOf(mediaId)
        } else 0
    }

    fun getMetadataItem(mediaId: String): MediaMetadataCompat? {
        return if (mapMetadataArray.containsKey(mediaId)) {
            mapMetadataArray[mediaId]
        } else null
    }

    override fun toString(): String {
        return "DataTransform{" +
                "musicInfoEntities=" + musicInfoEntities!!.size +
                ", queueItemList=" + queueItemList.size +
                ", mediaItemList=" + mediaItemList.size +
                ", mapMetadataArray=" + mapMetadataArray.size +
                ", pathList=" + pathList.size +
                ", mediaIdList=" + mediaIdList.size +
                '}'.toString()
    }

    companion object {
        private const val TAG = "DataTransform"


        fun get(): DataTransform {
            return Inner.INSTANCE
        }
    }
}
