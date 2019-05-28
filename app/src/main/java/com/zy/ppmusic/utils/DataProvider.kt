package com.zy.ppmusic.utils

import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.collection.ArrayMap
import android.text.TextUtils
import android.util.Log
import com.zy.ppmusic.App
import com.zy.ppmusic.entity.MusicInfoEntity
import java.util.*

/**
 * 数据转换
 * @author stealfeam
 */
class DataProvider private constructor() {
    @Volatile
    var musicInfoEntities: ArrayList<MusicInfoEntity>? = null
    @Volatile
    var queueItemList: ArrayList<MediaSessionCompat.QueueItem>
    val mediaItemList: ArrayList<MediaBrowserCompat.MediaItem>
    private val mapMetadataArray: androidx.collection.ArrayMap<String, MediaMetadataCompat>
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

    private val loadPicOption = BitmapFactory.Options().apply {
        inSampleSize = 2
    }

    private object Inner {
        val INSTANCE = DataProvider()
    }

    init {
        musicInfoEntities = ArrayList()
        mapMetadataArray = androidx.collection.ArrayMap()
        queueItemList = ArrayList()
        mediaItemList = ArrayList()
        mediaIdList = ArrayList()
    }

    fun transFormStringData(pathList: ArrayList<String>) {
        clearData()
        queryMedia(App.getInstance(), pathList)
    }

    private fun isMemoryHasData():Boolean{
        return pathList.size > 0
    }

    fun loadData(forceCache:Boolean){
        if(!forceCache){
            //返回内存中数据
            PrintLog.e("开始检查内存缓存")
            if(isMemoryHasData()){
                return
            }
            PrintLog.e("开始检查文件缓存")
            FileUtils.readObject(Constant.CACHE_FILE_PATH)?.apply {
                PrintLog.e("读取到本地缓存列表------")
                transFormData(this as ArrayList<MusicInfoEntity>)
                return
            }
        }
        PrintLog.e("未检查到本地缓存-----$forceCache")
        ScanMusicFile.get().startScan(App.getInstance(),object:ScanMusicFile.OnScanCompleteListener{
            override fun onComplete(paths: ArrayList<String>) {
                transFormStringData(paths)
                //缓存到本地
                FileUtils.saveObject(musicInfoEntities,Constant.CACHE_FILE_PATH)
            }
        })
    }

    interface OnLoadCompleteListener{
        fun onLoadComplete()
    }

    private fun clearData() {
        if (this.pathList.size > 0) {
            this.pathList.clear()
            musicInfoEntities?.clear()
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
            val query = contentResolver.query(audioUri, null,
                    MediaStore.Audio.Media.IS_MUSIC + "=?", arrayOf("1"), null)
            if (query != null) {
                //判断如果是上次扫描的uri则跳过，系统分为内部存储uri的音频和外部存储的uri
                if (oldUri != null && oldUri == audioUri) {
                    StreamUtils.closeIo(query)
                    continue
                } else {
                    queryContent(query, mediaMetadataRetriever)
                    StreamUtils.closeIo(query)
                }
            }

            oldUri = audioUri
        }

        PrintLog.d("扫描的数量-----${queueItemList.size}")

        if (localList.size > pathList.size) {
            reQueryList(localList,mediaMetadataRetriever)
        }

        mediaMetadataRetriever.release()

        Log.d(TAG, "queryResolver() called with: context = [$context]")
    }

    private fun queryContent(query: Cursor, picLoader: MediaMetadataRetriever) {
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
            val format = queryPath.substring(queryPath.lastIndexOf("."), queryPath.length)
            if (!SupportMediaType.SUPPORT_TYPE.contains(format)) {
                continue
            }
            PrintLog.e("扫描到的名称----$title------$artist")
            PrintLog.e("实际地址-------$queryPath")

            picLoader.setDataSource(queryPath)

            val infoEntity = MusicInfoEntity(queryPath.hashCode().toString(), title, artist, queryPath, size.toLong(),
                    duration, picLoader.embeddedPicture)

            mapMetadataArray[infoEntity.mediaId] = buildMetadataCompat(infoEntity)

            queueItemList.add(buildQueueItem(mapMetadataArray[infoEntity.mediaId]!!.description))

            mediaItemList.add(buildMediaItem(mapMetadataArray[infoEntity.mediaId]!!.description))

            musicInfoEntities?.add(infoEntity)
            pathList.add(queryPath)
            mediaIdList.add(infoEntity.mediaId!!)
        }
    }

    /**
     * 重新对数据遍历，筛选出系统媒体库中不存在的媒体
     * @param list 扫描到的数据列表
     */
    private fun reQueryList(list: List<String>,retriever: MediaMetadataRetriever) {
        for (itemPath in list) {
            if (!this.pathList.contains(itemPath)) {
                //过滤本地不存在的媒体文件
                if (!FileUtils.isExits(itemPath)) {
                    continue
                }
                retriever.setDataSource(itemPath)
                //METADATA_KEY_ALBUM 专辑
                val mediaName = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                val mediaAuthor = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                val mediaDuration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                var length: Long = 0
                if (!TextUtils.isEmpty(mediaDuration)) {
                    length = mediaDuration.toLong()
                    if (length < 20 * 1000) {
                        continue
                    }
                }

                val infoEntity = MusicInfoEntity(itemPath.hashCode().toString(),
                        StringUtils.ifEmpty(mediaName, getMusicName(itemPath)), mediaAuthor,
                        itemPath, 0, length, retriever.embeddedPicture)

                mapMetadataArray[infoEntity.mediaId] = buildMetadataCompat(infoEntity)

                queueItemList.add(buildQueueItem(mapMetadataArray[infoEntity.mediaId]!!.description))

                mediaItemList.add(buildMediaItem(mapMetadataArray[infoEntity.mediaId]!!.description))

                musicInfoEntities?.add(infoEntity)
                pathList.add(infoEntity.queryPath!!)
                mediaIdList.add(infoEntity.mediaId!!)
            }
        }
    }

    private fun buildMetadataCompat(infoEntity: MusicInfoEntity):MediaMetadataCompat{
       return MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, infoEntity.mediaId)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, infoEntity.queryPath)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                        StringUtils.ifEmpty(infoEntity.musicName, getMusicName(infoEntity.queryPath)))
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, infoEntity.iconData?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size, loadPicOption)
                })
                //作者
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                        StringUtils.ifEmpty(infoEntity.artist, "<未知作者>"))
                .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR,
                        StringUtils.ifEmpty(infoEntity.artist, "<未知作者>"))
                //时长
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, infoEntity.duration)
                .build()
    }

    private fun buildQueueItem(descriptionCompat: MediaDescriptionCompat): MediaSessionCompat.QueueItem {
        return MediaSessionCompat.QueueItem(descriptionCompat, descriptionCompat.mediaId?.hashCode()?.toLong()
                ?: UUID.randomUUID().hashCode().toLong())
    }

    private fun buildMediaItem(descriptionCompat: MediaDescriptionCompat): MediaBrowserCompat.MediaItem {
        return MediaBrowserCompat.MediaItem(descriptionCompat, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    /**
     * 从本地缓存得到的数据转换
     *
     * @param localList 本地缓存数据
     */
    private fun transFormData(localList: ArrayList<MusicInfoEntity>) {
        clearData()
        this.musicInfoEntities = localList
        for (itemEntity in localList) {
            pathList.add(itemEntity.queryPath!!)
            mediaIdList.add(itemEntity.mediaId!!)

            mapMetadataArray[itemEntity.mediaId] = buildMetadataCompat(itemEntity)

            queueItemList.add(buildQueueItem(mapMetadataArray[itemEntity.mediaId]!!.description))

            mediaItemList.add(buildMediaItem(mapMetadataArray[itemEntity.mediaId]!!.description))
        }
    }

    fun removeItem(index: Int) {
        pathList.removeAt(index)
        musicInfoEntities?.removeAt(index)
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

    fun getMediaIndex(mediaId: String?): Int {
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
        return "DataProvider{" +
                "musicInfoEntities=" + musicInfoEntities!!.size +
                ", queueItemList=" + queueItemList.size +
                ", mediaItemList=" + mediaItemList.size +
                ", mapMetadataArray=" + mapMetadataArray.size +
                ", pathList=" + pathList.size +
                ", mediaIdList=" + mediaIdList.size +
                '}'.toString()
    }

    companion object {
        private const val TAG = "DataProvider"

        @JvmStatic
        fun get(): DataProvider {
            return Inner.INSTANCE
        }
    }
}
