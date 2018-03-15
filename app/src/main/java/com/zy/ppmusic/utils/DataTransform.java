package com.zy.ppmusic.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.util.ArrayMap;
import android.text.TextUtils;
import android.util.Log;

import com.zy.ppmusic.entity.MusicInfoEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * 数据转换
 * @author ZhiTouPC
 */
public class DataTransform {
    private static final String TAG = "DataTransform";
    /**
     * 可存放本地的数据
     */
    private volatile List<MusicInfoEntity> musicInfoEntities;
    private final List<MediaSessionCompat.QueueItem> queueItemList;
    private final ArrayList<MediaBrowserCompat.MediaItem> mediaItemList;
    private final ArrayMap<String, MediaMetadataCompat> mapMetadataArray;
    /**
     * 扫描到的路径
     */
    private final ArrayList<String> pathList;
    /**
     * 用于获取mediaId的位置
     */
    private final List<String> mediaIdList;

    private static class Inner {
        private static final DataTransform INSTANCE = new DataTransform();
    }

    private DataTransform() {
        pathList = new ArrayList<>();
        musicInfoEntities = new ArrayList<>();
        mapMetadataArray = new ArrayMap<>();
        queueItemList = new ArrayList<>();
        mediaItemList = new ArrayList<>();
        mediaIdList = new ArrayList<>();
    }

    public static DataTransform getInstance() {
        return Inner.INSTANCE;
    }

    /**
     * 从本地扫描得到数据转换
     */
    public void transFormData(Context context, ArrayList<String> pathList) {
        clearData();
        queryMedia(context, pathList);
    }

    private void clearData() {
        if (this.pathList.size() > 0) {
            this.pathList.clear();
            musicInfoEntities.clear();
            mapMetadataArray.clear();
            queueItemList.clear();
            mediaIdList.clear();
            mediaItemList.clear();
        }
    }
    /**
     * 从系统媒体库获取信息
     *
     * @param context   context
     * @param localList 路径列表
     */
    private void queryMedia(Context context, List<String> localList) {
        ContentResolver contentResolver = context.getContentResolver();
        Uri oldUri = null;
        MediaMetadataCompat.Builder builder;
        boolean isNeedRe = false;

        MediaMetadataRetriever mediaMetadataRetriever = new MediaMetadataRetriever();

        for (String itemPath : localList) {
            if (mediaIdList.contains(String.valueOf(itemPath.hashCode()))) {
                continue;
            } else {
                PrintOut.print(itemPath);
            }
            //根据音频地址获取uri，区分为内部存储和外部存储
            Uri audioUri = MediaStore.Audio.Media.getContentUriForPath(itemPath);
            Cursor query = contentResolver.query(audioUri, null, null, null, null);
            if (query != null) {
                //判断如果是上次扫描的uri则跳过，系统分为内部存储uri的音频和外部存储的uri
                if (oldUri != null && oldUri.equals(audioUri)) {
                    query.close();
                    continue;
                } else {
                    //遍历得到内部或者外部存储的所有媒体文件的信息
                    while (query.moveToNext()) {
                        String title = query.getString(query.getColumnIndex(MediaStore.Audio.Media.TITLE));
                        String artist = query.getString(query.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                        long duration = query.getLong(query.getColumnIndex(MediaStore.Audio.Media.DURATION));
                        String size = query.getString(query.getColumnIndex(MediaStore.Audio.Media.SIZE));
                        String queryPath = query.getString(query.getColumnIndex(MediaStore.Audio.Media.DATA));
                        mediaMetadataRetriever.setDataSource(queryPath);
                        Bitmap mBitmap = null;
                        if (mediaMetadataRetriever.getEmbeddedPicture() != null) {
                            mBitmap = BitmapFactory.decodeByteArray(mediaMetadataRetriever.getEmbeddedPicture(),
                                    0, mediaMetadataRetriever.getEmbeddedPicture().length);
                        }

                        //过滤小于20s的文件
                        if (duration < 20 * 1000) {
                            continue;
                        }
                        //过滤本地不存在的媒体文件
                        if (!FileUtils.isExits(queryPath)) {
                            continue;
                        }
                        builder = new MediaMetadataCompat.Builder();
                        //唯一id
                        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(queryPath.hashCode()));
                        //文件路径
                        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, queryPath);
                        //显示名称
                        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, title);
                        //作者
                        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, StringUtils.Companion
                                .ifEmpty(artist,"<未知作者>"));
                        //作者
                        builder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, StringUtils.Companion
                                .ifEmpty(artist,"<未知作者>"));

                        builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, mBitmap);

                        //时长
                        builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);

                        MediaMetadataCompat metadataCompatItem = builder.build();

                        mapMetadataArray.put(String.valueOf(queryPath.hashCode()), metadataCompatItem);

                        MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(
                                metadataCompatItem.getDescription(), queryPath.hashCode());
                        queueItemList.add(queueItem);

                        MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                                metadataCompatItem.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
                        mediaItemList.add(mediaItem);

                        MusicInfoEntity infoEntity = new MusicInfoEntity(String.valueOf(queryPath.hashCode()),
                                title, artist, queryPath, Long.parseLong(size), duration, mediaMetadataRetriever.getEmbeddedPicture());
                        musicInfoEntities.add(infoEntity);
                        pathList.add(queryPath);
                        mediaIdList.add(String.valueOf(queryPath.hashCode()));
                    }
                    //去除媒体库中不存在的
                    if (!mediaIdList.contains(String.valueOf(itemPath.hashCode()))) {
                        pathList.remove(itemPath);
                        isNeedRe = true;
                    }
                    query.close();
                }
            } else {
                isNeedRe = true;
            }
            oldUri = audioUri;
        }

        if (isNeedRe) {
            reQueryList(localList);
            Log.e(TAG, "queryMedia: isNeedRe " + pathList.size());
        } else if (localList.size() > pathList.size()) {
            reQueryList(localList);
        }
        Log.d(TAG, "queryResolver() called with: context = [" + context + "]");
    }

    /**
     * 重新对数据遍历，筛选出系统ContentProvider中不存在的媒体
     * @param list 扫描到的数据列表
     */
    private void reQueryList(List<String> list) {
        MediaMetadataCompat.Builder builder;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        for (int i = 0; i < list.size(); i++) {
            String itemPath = list.get(i);
            if (!this.pathList.contains(itemPath)) {
                retriever.setDataSource(itemPath);
                //METADATA_KEY_ALBUM 专辑
                String titleS = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
                String artistS = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
                String durationS = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
                long d = 0;
                if (!TextUtils.isEmpty(durationS)) {
                    d = Long.parseLong(durationS);
                    if (d < 20 * 1000) {
                        continue;
                    }
                }
                builder = new MediaMetadataCompat.Builder();
                String musicName = getMusicName(itemPath);
                builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(itemPath.hashCode()));
                builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, itemPath);
                builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,
                        StringUtils.Companion.ifEmpty(titleS, musicName));
                byte[] embeddedPicture = retriever.getEmbeddedPicture();
                Bitmap bitmap = null;
                if (embeddedPicture != null) {
                    bitmap = BitmapFactory.decodeByteArray(embeddedPicture, 0, embeddedPicture.length);
                }

                builder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, bitmap);

                //作者
                builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,
                        StringUtils.Companion.ifEmpty(artistS, "<未知作者>"));
                //作者
                builder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR,
                        StringUtils.Companion.ifEmpty(artistS, "<未知作者>"));
                //时长
                builder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, d);

                MediaMetadataCompat metadataCompatItem = builder.build();

                mapMetadataArray.put(String.valueOf(itemPath.hashCode()), metadataCompatItem);

                MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(
                        metadataCompatItem.getDescription(), itemPath.hashCode());
                queueItemList.add(queueItem);

                MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                        metadataCompatItem.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
                mediaItemList.add(mediaItem);

                MusicInfoEntity infoEntity = new MusicInfoEntity(String.valueOf(itemPath.hashCode()),
                        musicName, artistS, itemPath, 0, d, embeddedPicture);
                musicInfoEntities.add(infoEntity);
                PrintOut.print("put else index=" + i + ",path=" + itemPath);
                pathList.add(itemPath);
                mediaIdList.add(String.valueOf(itemPath.hashCode()));
            }
        }
    }

    /**
     * 从本地缓存得到的数据转换
     *
     * @param localList 本地缓存数据
     */
    public void transFormData(List<MusicInfoEntity> localList) {
        clearData();
        this.musicInfoEntities = localList;
        MediaMetadataCompat metadataCompatItem;
        for (int i = 0; i < musicInfoEntities.size(); i++) {
            MusicInfoEntity itemEntity = musicInfoEntities.get(i);
            pathList.add(itemEntity.getQueryPath());
            mediaIdList.add(itemEntity.getMediaId());

            itemEntity.setExits(FileUtils.isExits(itemEntity.getQueryPath()));

            MediaMetadataCompat.Builder itemBuilder = new MediaMetadataCompat.Builder();
            //唯一id
            itemBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, itemEntity.getMediaId());
            //文件路径
            itemBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, itemEntity.getQueryPath());
            //显示名称
            itemBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, itemEntity.getMusicName());
            //作者
            itemBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, itemEntity.getArtist());
            //作者
            itemBuilder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, itemEntity.getArtist());

            itemBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, itemEntity.getDuration());

            itemBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_DISPLAY_ICON, itemEntity.getIconData() == null
                    ? null : BitmapFactory.decodeByteArray(itemEntity.getIconData(), 0, itemEntity.getIconData().length));

            metadataCompatItem = itemBuilder.build();

            mapMetadataArray.put(itemEntity.getMediaId(), metadataCompatItem);

            MediaSessionCompat.QueueItem queueItem = new MediaSessionCompat.QueueItem(
                    metadataCompatItem.getDescription(), Long.parseLong(itemEntity.getMediaId()));
            queueItemList.add(queueItem);

            MediaBrowserCompat.MediaItem mediaItem = new MediaBrowserCompat.MediaItem(
                    metadataCompatItem.getDescription(), MediaBrowserCompat.MediaItem.FLAG_PLAYABLE);
            mediaItemList.add(mediaItem);
        }
    }

    public void removeItem(Context context, int index) {
        String path = pathList.get(index);
        File file = new File(path);
        file.deleteOnExit();
        if (!file.exists()) {
            ContentResolver contentResolver = context.getContentResolver();
            Uri audioUri = MediaStore.Audio.Media.getContentUriForPath(path);
            String where = MediaStore.Images.Media.DATA + "='" + path + "'";
            contentResolver.delete(audioUri, where, null);
        }
        this.pathList.remove(index);
        musicInfoEntities.remove(index);
        mapMetadataArray.remove(this.mediaIdList.get(index));
        queueItemList.remove(index);
        mediaIdList.remove(index);
        mediaItemList.remove(index);
    }

    /**
     * 获取文件媒体名称，仅在媒体文件中信息丢失时使用
     * @param path 文件路径
     * @return 媒体显示名称
     */
    private String getMusicName(String path) {
        if (path != null) {
            return path.substring((path.lastIndexOf("/") + 1), path.lastIndexOf("."));
        } else {
            return null;
        }
    }

    public List<MusicInfoEntity> getMusicInfoEntities() {
        return musicInfoEntities;
    }

    public List<MediaSessionCompat.QueueItem> getQueueItemList() {
        return queueItemList;
    }

    public ArrayList<MediaBrowserCompat.MediaItem> getMediaItemList() {
        return mediaItemList;
    }

    public ArrayList<String> getPathList() {
        return pathList;
    }

    public List<String> getMediaIdList() {
        return mediaIdList;
    }

    public String getPath(int position) {
        if (position >= 0 && position < pathList.size()) {
            return pathList.get(position);
        } else {
            return null;
        }
    }

    public int getMediaIndex(String mediaId) {
        if (mediaIdList.contains(mediaId)) {
            return mediaIdList.indexOf(mediaId);
        }
        return 0;
    }

    public MediaMetadataCompat getMetadataItem(String mediaId) {
        if (mapMetadataArray.containsKey(mediaId)) {
            return mapMetadataArray.get(mediaId);
        }
        return null;
    }

    public Map<String, MediaMetadataCompat> getMetadataCompatList() {
        return mapMetadataArray;
    }

    @Override
    public String toString() {
        return "DataTransform{" +
                "musicInfoEntities=" + musicInfoEntities.size() +
                ", queueItemList=" + queueItemList.size() +
                ", mediaItemList=" + mediaItemList.size() +
                ", mapMetadataArray=" + mapMetadataArray.size() +
                ", pathList=" + pathList.size() +
                ", mediaIdList=" + mediaIdList.size() +
                '}';
    }
}
