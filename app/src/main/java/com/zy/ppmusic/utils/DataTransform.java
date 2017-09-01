package com.zy.ppmusic.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.provider.MediaStore;
import android.support.v4.media.MediaBrowserCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.ArrayMap;
import android.util.Log;

import com.zy.ppmusic.entity.MusicInfoEntity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * 数据转换
 */
public class DataTransform {
    private static final String TAG = "DataTransform";
    private volatile List<MusicInfoEntity> musicInfoEntities;//可存放本地的数据
    private volatile List<MediaSessionCompat.QueueItem> queueItemList;
    private volatile ArrayList<MediaBrowserCompat.MediaItem> mediaItemList;
    private volatile ArrayMap<String, MediaMetadataCompat> mapMetadataArray;
    private volatile ArrayMap<Integer, String> indexMediaArray;
    private volatile ArrayList<String> pathList;
    private List<String> mediaIdList;

    private static class Inner {
        private static DataTransform transform = new DataTransform();
    }

    private DataTransform() {
        pathList = new ArrayList<>();
        musicInfoEntities = new ArrayList<>();
        mapMetadataArray = new ArrayMap<>();
        indexMediaArray = new ArrayMap<>();
        queueItemList = new ArrayList<>();
        mediaItemList = new ArrayList<>();
        mediaIdList = new ArrayList<>();
    }

    public static DataTransform getInstance() {
        return Inner.transform;
    }

    /**
     * 从本地扫描得到数据转换
     */
    public void transFormData(Context context, ArrayList<String> pathList) {
        clearData();
        queryMedia(context, pathList);
    }

    public void moveTo(int from, int to) {
        swap(pathList.get(from), pathList.get(to));
        swap(from, to);
        Collections.swap(pathList, from, to);
        Collections.swap(musicInfoEntities, from, to);
        Collections.swap(queueItemList, from, to);
        Collections.swap(mediaItemList, from, to);
        Collections.swap(mediaIdList, from, to);
    }

    private void swap(String fromMedia, String toMedia) {
        MediaMetadataCompat fromMeta = mapMetadataArray.get(fromMedia);
        MediaMetadataCompat toMeta = mapMetadataArray.get(toMedia);
        mapMetadataArray.put(fromMedia, toMeta);
        mapMetadataArray.put(toMedia, fromMeta);
    }

    private void swap(int from, int to) {
        String fromMedia = indexMediaArray.get(from);
        String toMedia = indexMediaArray.get(to);
        indexMediaArray.put(from, toMedia);
        indexMediaArray.put(to, fromMedia);
    }

    private void clearData() {
        if (this.pathList.size() > 0) {
            this.pathList.clear();
            musicInfoEntities.clear();
            mapMetadataArray.clear();
            indexMediaArray.clear();
            queueItemList.clear();
            mediaIdList.clear();
            mediaItemList.clear();
        }
    }

    /**
     * 测试耗时比较长，废弃
     */
    private void queryMedia(List<String> localList) {
        MediaMetadataCompat.Builder builder;
        MediaMetadataRetriever retriever = new MediaMetadataRetriever();
        int index = 0;
        for (String itemPath : localList) {
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
            //唯一id
            builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(itemPath.hashCode()));
            //文件路径
            builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, itemPath);
            //显示名称
            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, titleS);
            //作者
            builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artistS);
            //作者
            builder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, artistS);
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
                    titleS, artistS, itemPath, 0l, d);
            musicInfoEntities.add(infoEntity);
            pathList.add(itemPath);
            indexMediaArray.put(index, String.valueOf(itemPath.hashCode()));
            mediaIdList.add(String.valueOf(itemPath.hashCode()));
            index++;
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
        int index = 0;
        MediaMetadataCompat.Builder builder;
        boolean isNeedRe = false;
        for (String itemPath : localList) {
            if (mediaIdList.contains(String.valueOf(itemPath.hashCode()))) {
                continue;
            } else {
                System.out.println(itemPath);
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
                        Log.w(TAG, "queryMedia: first...");
                        String name = query.getString(query.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                        String title = query.getString(query.getColumnIndex(MediaStore.Audio.Media.TITLE));
                        String artist = query.getString(query.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                        long duration = query.getLong(query.getColumnIndex(MediaStore.Audio.Media.DURATION));
                        String size = query.getString(query.getColumnIndex(MediaStore.Audio.Media.SIZE));
                        String queryPath = query.getString(query.getColumnIndex(MediaStore.Audio.Media.DATA));
                        Log.w(TAG, "queryMedia: path===" + queryPath);
                        //过滤小于20s的文件
                        if (duration < 20 * 1000) {
                            continue;
                        }

                        if (!new File(queryPath).exists()) {
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
                        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, artist);
                        //作者
                        builder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, artist);
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
                                title, artist, queryPath, Long.parseLong(size), duration);
                        musicInfoEntities.add(infoEntity);
                        pathList.add(queryPath);
                        indexMediaArray.put(index, String.valueOf(queryPath.hashCode()));
                        mediaIdList.add(String.valueOf(queryPath.hashCode()));
                        index++;
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
     *
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
                builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, (String) StringUtils.Companion.ifEmpty(titleS, musicName));

                //作者
                builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, (String) StringUtils.Companion.ifEmpty(artistS));
                //作者
                builder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR, (String) StringUtils.Companion.ifEmpty(artistS));
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
                        musicName, artistS, itemPath, 0, d);
                musicInfoEntities.add(infoEntity);
                System.out.println("put else index=" + i + ",path=" + itemPath);
                pathList.add(itemPath);
                indexMediaArray.put(i, String.valueOf(itemPath.hashCode()));
                mediaIdList.add(String.valueOf(itemPath.hashCode()));
            }
        }
    }

    /**
     * 从本地缓存得到的数据转换
     *
     * @param localList
     */
    public void transFormData(List<MusicInfoEntity> localList) {
        clearData();
        this.musicInfoEntities = localList;
        MediaMetadataCompat metadataCompatItem;
        for (int i = 0; i < musicInfoEntities.size(); i++) {
            MusicInfoEntity itemEntity = musicInfoEntities.get(i);
            pathList.add(itemEntity.getQueryPath());
            indexMediaArray.put(i, itemEntity.getMediaId());
            mediaIdList.add(itemEntity.getMediaId());

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
        indexMediaArray.remove(index);
        queueItemList.remove(index);
        mediaIdList.remove(index);
        mediaItemList.remove(index);
    }

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
        System.out.println("mediaId=" + mediaId + ",data=" + mapMetadataArray.toString());
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
                ", indexMediaArray=" + indexMediaArray.size() +
                ", pathList=" + pathList.size() +
                ", mediaIdList=" + mediaIdList.size() +
                '}';
    }
}
