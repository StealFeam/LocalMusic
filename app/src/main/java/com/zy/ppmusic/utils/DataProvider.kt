package com.zy.ppmusic.utils

import android.content.Context
import android.database.Cursor
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import androidx.collection.ArrayMap
import android.text.TextUtils
import android.util.Log
import androidx.core.content.ContentResolverCompat
import com.zy.ppmusic.App
import com.zy.ppmusic.entity.MusicInfoEntity
import kotlinx.coroutines.*
import java.util.*
import java.util.concurrent.atomic.AtomicReference
import kotlin.collections.ArrayList
import kotlin.coroutines.resume
import kotlin.math.max

/**
 * 数据转换
 * @author stealfeam
 */
class DataProvider private constructor() {
    var musicInfoEntities: AtomicReference<ArrayList<MusicInfoEntity>> = AtomicReference(ArrayList())
    val queueItemList: AtomicReference<ArrayList<MediaSessionCompat.QueueItem>> = AtomicReference(ArrayList())
    val mediaItemList: AtomicReference<ArrayList<MediaBrowserCompat.MediaItem>> = AtomicReference(ArrayList())
    private val mapMetadataArray: AtomicReference<ArrayMap<String, MediaMetadataCompat>> = AtomicReference(ArrayMap())

    /**
     * 扫描到的路径
     */
    val pathList: AtomicReference<ArrayList<String>> = AtomicReference(ArrayList())
    /**
     * 用于获取mediaId的位置
     */
    val mediaIdList: AtomicReference<ArrayList<String>> = AtomicReference(ArrayList())

    val metadataCompatList: Map<String, MediaMetadataCompat>
        get() = mapMetadataArray.get()

    private val loadPicOption = BitmapFactory.Options().apply {
        inSampleSize = 2
    }

    private object Inner {
        val INSTANCE = DataProvider()
    }

    fun transformData(pathList: ArrayList<String>) {
        clearData()
        queryMedia(App.appBaseContext, pathList)
    }

    private fun isMemoryHasData():Boolean {
        return pathList.get().size > 0
    }

    suspend fun loadData(forceCache: Boolean) = suspendCancellableCoroutine<Void> { cont ->
        if (!forceCache) {
            // 返回内存中数据
            PrintLog.e("开始检查内存缓存")
            if (isMemoryHasData()) {
                cont.resume(Void)
                return@suspendCancellableCoroutine
            }
            PrintLog.e("开始检查文件缓存")
            FileUtils.readObject(Constant.CACHE_FILE_PATH)?.apply {
                PrintLog.e("读取到本地缓存列表------")
                transFormData(this as ArrayList<MusicInfoEntity>)
                cont.resume(Void)
                return@suspendCancellableCoroutine
            }
        }
        PrintLog.e("未检查到本地缓存-----$forceCache")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            CoroutineScope(Job() + Dispatchers.IO).launch {
                ScanMediaFile.get().scanInternalMedia(App.instance!!)
                ScanMediaFile.get().scanExternalMedia(App.instance!!)
                clearData()
                val projection = arrayOf(
                    MediaStore.Audio.Media.ALBUM_ID,
                    MediaStore.Audio.Media.DISPLAY_NAME,
                    MediaStore.Audio.Media.DURATION,
                    MediaStore.Audio.Media.SIZE,
                    MediaStore.Audio.Media.TITLE,
                    MediaStore.Audio.Media.ARTIST,
                    MediaStore.Audio.Media.AUTHOR,
                    MediaStore.Audio.Media.DATA,
                    MediaStore.Audio.Media.RELATIVE_PATH
                )
                val externalQuery = ContentResolverCompat.query(
                    App.instance?.contentResolver,
                    MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Audio.Media.IS_MUSIC + "=?",
                    arrayOf("1"),
                    null,
                    null
                )
                externalQuery?.use { cursor ->
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    queryContent(cursor, mediaMetadataRetriever)
                    mediaMetadataRetriever.release()
                }
                val internalQuery = ContentResolverCompat.query(
                    App.instance?.contentResolver,
                    MediaStore.Audio.Media.INTERNAL_CONTENT_URI,
                    projection,
                    MediaStore.Audio.Media.IS_MUSIC + "=?",
                    arrayOf("1"),
                    null,
                    null
                )
                internalQuery?.use { cursor ->
                    val mediaMetadataRetriever = MediaMetadataRetriever()
                    queryContent(cursor, mediaMetadataRetriever)
                    mediaMetadataRetriever.release()
                }
                //缓存到本地
                FileUtils.saveObject(musicInfoEntities.get(), Constant.CACHE_FILE_PATH)
                cont.resume(Void)
            }
        } else {
            ScanMediaFile.get().startScan(App.appBaseContext, object: ScanMediaFile.OnScanCompleteListener{
                override fun onComplete(paths: ArrayList<String>) {
                    transformData(paths)
                    //缓存到本地
                    FileUtils.saveObject(musicInfoEntities.get(), Constant.CACHE_FILE_PATH)
                    cont.resume(Void)
                }
            })
        }
    }

    private fun clearData() {
        if (this.pathList.get().size > 0) {
            this.pathList.get().clear()
            musicInfoEntities.get().clear()
            mapMetadataArray.get().clear()
            queueItemList.get().clear()
            mediaIdList.get().clear()
            mediaItemList.get().clear()
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

        val endIndex = localList.size - 1
        for (index in endIndex downTo 0) {
            val itemPath = localList[index]
            if (mediaIdList.get().contains(itemPath.hashCode().toString())) {
                continue
            } else {
                PrintLog.print(itemPath)
            }
            //根据音频地址获取uri，区分为内部存储和外部存储
            val audioUri = MediaStore.Audio.Media.getContentUriForPath(itemPath) ?: continue
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

        PrintLog.d("扫描的数量-----${queueItemList.get().size}")

        if (localList.size > pathList.get().size) {
            reQueryList(localList,mediaMetadataRetriever)
        }

        mediaMetadataRetriever.release()

        Log.d(TAG, "queryResolver() called with: context = [$context]")
    }

    private fun queryContent(query: Cursor, picLoader: MediaMetadataRetriever) {
        //遍历得到内部或者外部存储的所有媒体文件的信息
        while (query.moveToNext()) {
            val title = query.getString(max(query.getColumnIndex(MediaStore.Audio.Media.TITLE), 0))
            val artist = query.getString(max(query.getColumnIndex(MediaStore.Audio.Media.ARTIST), 0))
            val duration = query.getLong(max(query.getColumnIndex(MediaStore.Audio.Media.DURATION), 0))
            val size = query.getString(max(query.getColumnIndex(MediaStore.Audio.Media.SIZE), 0))
            // RELATIVE_PATH
            val queryPath = query.getString(max(query.getColumnIndex(MediaStore.Audio.Media.DATA), 0))
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

            mapMetadataArray.get()[infoEntity.mediaId] = buildMetadataCompat(infoEntity)

            queueItemList.get().add(buildQueueItem(mapMetadataArray.get()[infoEntity.mediaId]!!.description))

            mediaItemList.get().add(buildMediaItem(mapMetadataArray.get()[infoEntity.mediaId]!!.description))

            musicInfoEntities.get()?.add(infoEntity)
            pathList.get().add(queryPath)
            mediaIdList.get().add(infoEntity.mediaId!!)
        }
    }

    /**
     * 重新对数据遍历，筛选出系统媒体库中不存在的媒体
     * @param list 扫描到的数据列表
     */
    private fun reQueryList(list: List<String>,retriever: MediaMetadataRetriever) {
        for (itemPath in list) {
            if (!this.pathList.get().contains(itemPath)) {
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
                    length = mediaDuration?.toLong() ?: continue
                    if (length < 20 * 1000) {
                        continue
                    }
                }

                val infoEntity = MusicInfoEntity(itemPath.hashCode().toString(),
                        StringUtils.ifEmpty(mediaName, getMusicName(itemPath)), mediaAuthor,
                        itemPath, 0, length, retriever.embeddedPicture)

                mapMetadataArray.get()[infoEntity.mediaId] = buildMetadataCompat(infoEntity)

                queueItemList.get().add(buildQueueItem(mapMetadataArray.get()[infoEntity.mediaId]!!.description))

                mediaItemList.get().add(buildMediaItem(mapMetadataArray.get()[infoEntity.mediaId]!!.description))

                musicInfoEntities.get().add(infoEntity)
                pathList.get().add(infoEntity.queryPath!!)
                mediaIdList.get().add(infoEntity.mediaId!!)
            }
        }
    }

    private fun buildMetadataCompat(infoEntity: MusicInfoEntity):MediaMetadataCompat{
       return MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, infoEntity.mediaId)
                .putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, infoEntity.queryPath)
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, StringUtils.ifEmpty(infoEntity.musicName, getMusicName(infoEntity.queryPath)))
                .putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, infoEntity.iconData?.let {
                    BitmapFactory.decodeByteArray(it, 0, it.size, loadPicOption)
                })
                .putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, StringUtils.ifEmpty(infoEntity.artist, "<未知作者>")) // 作者
                .putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, StringUtils.ifEmpty(infoEntity.artist, "<未知作者>"))
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, infoEntity.duration) // 时长
                .build()
    }

    private fun buildQueueItem(descriptionCompat: MediaDescriptionCompat): MediaSessionCompat.QueueItem {
        return MediaSessionCompat.QueueItem(descriptionCompat, descriptionCompat.mediaId?.hashCode()?.toLong() ?: UUID.randomUUID().hashCode().toLong())
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
        this.musicInfoEntities.set(localList)
        for (itemEntity in localList) {
            pathList.get().add(itemEntity.queryPath!!)
            mediaIdList.get().add(itemEntity.mediaId!!)

            mapMetadataArray.get()[itemEntity.mediaId] = buildMetadataCompat(itemEntity)

            queueItemList.get().add(buildQueueItem(mapMetadataArray.get()[itemEntity.mediaId]!!.description))

            mediaItemList.get().add(buildMediaItem(mapMetadataArray.get()[itemEntity.mediaId]!!.description))
        }
    }

    private fun removeItem(index: Int) {
        pathList.get().removeAt(index)
        musicInfoEntities.get().removeAt(index)
        mapMetadataArray.get().remove(this.mediaIdList.get()[index])
        mediaIdList.get().removeAt(index)
        mediaItemList.get().removeAt(index)
        queueItemList.get().removeAt(index)
    }

    suspend fun removeItemIncludeFile(index: Int) = withContext(Dispatchers.IO) {
        removeItem(index)
        // 更新缓存
        FileUtils.saveObject(musicInfoEntities.get(), Constant.CACHE_FILE_PATH)
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
        return musicInfoEntities.get()
    }

    fun getQueueItemList(): List<MediaSessionCompat.QueueItem> {
        return queueItemList.get()
    }

    fun getMediaIdList(): List<String> {
        return mediaIdList.get()
    }

    fun getPath(position: Int): String? {
        return if (position >= 0 && position < pathList.get().size) {
            pathList.get()[position]
        } else {
            null
        }
    }

    fun getPathList(): List<String> {
        return pathList.get()
    }

    fun getMediaIndex(mediaId: String?): Int {
        return if (mediaId.isNullOrEmpty()) {
            -1
        } else {
            mediaIdList.get().indexOf(mediaId)
        }
    }

    fun getMetadataItem(mediaId: String): MediaMetadataCompat? {
        return mapMetadataArray.get()[mediaId]
    }

    override fun toString(): String {
        return "DataProvider{" +
                "musicInfoEntities=" + musicInfoEntities.get().size +
                ", queueItemList=" + queueItemList.get().size +
                ", mediaItemList=" + mediaItemList.get().size +
                ", mapMetadataArray=" + mapMetadataArray.get().size +
                ", pathList=" + pathList.get().size +
                ", mediaIdList=" + mediaIdList.get().size +
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
