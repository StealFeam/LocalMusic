package com.zy.ppmusic.utils;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Handler;
import android.os.Message;
import android.provider.MediaStore;
import android.support.v4.media.MediaMetadataCompat;
import android.util.Log;

import java.io.File;
import java.lang.ref.WeakReference;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 扫描本地音乐文件
 */
public class ScanMusicFile {
    private static final String TAG = "ScanMusicFile";
    private static final int COUNT_CHANGE = 0X001;//扫描的数量发生了变化
    private static final int SCAN_COMPLETE = 0X000;//扫描完成

    private String[] mSupportMedia = {".mp3", ".wav", ".wma"};//所支持的音乐格式
    private static ExecutorService executor = Executors.newSingleThreadExecutor();//单一线程池
    private ArrayList<OnScanComplete> callBackList = new ArrayList<>();
    private ArrayList<String> mPathList = new ArrayList<>();//扫描到的音乐路径集合
    private ScanHandler mHandler = new ScanHandler(this);//线程回调的handler
    private NumberFormat format = NumberFormat.getInstance(Locale.CHINA);//数字格式化工具
    private String mInnerStoragePath;//内部存储路径
    private String mExternalStoragePath;//外部存储路径
    private Runnable mScanTask;

    private static LinkedHashMap<String, MediaMetadataCompat> mMusicListById;

    private static class ScanInstance {
        private static ScanMusicFile scanMusicFile = new ScanMusicFile();
    }

    private ScanMusicFile() {
        format.setMaximumIntegerDigits(2);
        format.setMaximumFractionDigits(2);
        mMusicListById = new LinkedHashMap<>();
    }

    public static ScanMusicFile getInstance() {
        return ScanInstance.scanMusicFile;
    }

    public ScanMusicFile setOnScanComplete(OnScanComplete complete) {
        if (!callBackList.contains(complete)) {
            callBackList.add(complete);
        }
        return this;
    }

    public MediaMetadataCompat getMusicById(String musicId) {
        return mMusicListById.containsKey(musicId) ? mMusicListById.get(musicId) : null;
    }

    public String getMediaIdByPosition(int position) {
        if(position>=0 && position < mPathList.size()){
            return String.valueOf(mPathList.get(position).hashCode());
        }else{
            return null;
        }
    }

    public int getPositionFromMediaId(String mediaId){
        int index = 0;
        Set<String> keys = mMusicListById.keySet();
        for (String key : keys) {
            if(String.valueOf(key).equals(mediaId)){
                return index;
            }
            index ++;
        }
        Log.e(TAG, "mediaId not found");
        return 0;
    }

    public String getMusicName(String path) {
        if (path != null) {
            return path.substring((path.lastIndexOf("/") + 1), path.lastIndexOf("."));
        } else {
            return null;
        }
    }


    public ScanMusicFile scanMusicFile(final Context c) {
        final Context context = c.getApplicationContext();
        if(mInnerStoragePath == null){
            mInnerStoragePath = FileUtils.getStoragePath(context, false);
            mExternalStoragePath = FileUtils.getStoragePath(context, true);
        }
        Log.d(TAG, "scanMusicFile() called with: context = [" + context + "]" + mMusicListById + "," + this);
        synchronized (this) {
            if (mScanTask == null) {
                mScanTask = new Runnable() {
                    @Override
                    public void run() {
                        Log.d(TAG, "run: 扫描开始");
                        searchFile(new File(mInnerStoragePath));
                        Log.e(TAG, "run: 扫描内部存储结束");
                        if (mExternalStoragePath != null) {
                            searchFile(new File(mExternalStoragePath));
                            Log.e(TAG, "run: 扫描外部存储结束");
                        }
                        Log.d(TAG, "run: 扫描结束");
                        for (int i = (mPathList.size() - 1); i >= 0; i--) {
                            File file = new File(mPathList.get(i));
                            double size = ((double) file.length() / 1024d) / 1024d;
                            if (size > 1d) {
                                Log.w(TAG, file.getAbsolutePath() + "," + format.format(size) + "M");
                            } else {
                                mPathList.remove(i);
                            }
                        }
                        ContentResolver contentResolver = context.getContentResolver();
                        Uri oldUri = null;
                        for (String s : mPathList) {
                            //根据音频地址获取uri，区分为内部存储和外部存储
                            Uri audioUri = MediaStore.Audio.Media.getContentUriForPath(s);
                            Cursor query = contentResolver.query(audioUri,null, null, null, null);
                            if(query != null){
                                //判断如果是上次扫描的uri则跳过，系统分为内部存储uri的音频和外部存储的uri
                                if(oldUri != null && oldUri.equals(audioUri)){
                                    continue;
                                }else{
                                    //遍历得到内部或者外部存储的所有媒体文件的信息
                                    while(query.moveToNext()){
                                        String name = query.getString(query.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME));
                                        String title = query.getString(query.getColumnIndex(MediaStore.Audio.Media.TITLE));
                                        String artist = query.getString(query.getColumnIndex(MediaStore.Audio.Media.ARTIST));
                                        String duration = query.getString(query.getColumnIndex(MediaStore.Audio.Media.DURATION));
                                        String size = query.getString(query.getColumnIndex(MediaStore.Audio.Media.SIZE));
                                        String path = query.getString(query.getColumnIndex(MediaStore.Audio.Media.DATA));

                                        MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
                                        //唯一id
                                        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(s.hashCode()));
                                        //文件路径
                                        builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, path);
                                        //显示名称
                                        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,title);
                                        //作者
                                        builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE,artist);
                                        //作者
                                        builder.putString(MediaMetadataCompat.METADATA_KEY_AUTHOR,artist);
                                        mMusicListById.put(String.valueOf(path.hashCode()), builder.build());
                                    }
                                    query.close();
                                }
                            }else {//如果本地媒体库未发现文件则创建默认的
                                MediaMetadataCompat.Builder builder = new MediaMetadataCompat.Builder();
                                builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, String.valueOf(s.hashCode()));
                                builder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_URI, s);
                                builder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE,getMusicName(s));
                                mMusicListById.put(String.valueOf(s.hashCode()), builder.build());
                            }
                            oldUri = audioUri;
                        }
                        mHandler.sendEmptyMessage(SCAN_COMPLETE);
                    }
                };
                executor.submit(mScanTask);
            }
        }
        return this;
    }

    private static class ScanHandler extends Handler {
        private WeakReference<ScanMusicFile> weak;

        private ScanHandler(ScanMusicFile scanMusicFile) {
            this.weak = new WeakReference<>(scanMusicFile);
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (weak.get() != null) {
                ScanMusicFile scanMusicFile = weak.get();
                switch (msg.what) {
                    case SCAN_COMPLETE:
                        if (scanMusicFile.callBackList.size() > 0) {
                            for (OnScanComplete onScanComplete : scanMusicFile.callBackList) {
                                onScanComplete.onComplete(scanMusicFile.mPathList);
                            }
                        }
                        break;
                    case COUNT_CHANGE:
                        if (scanMusicFile.callBackList.size() > 0) {
                            for (OnScanComplete onScanComplete : scanMusicFile.callBackList) {
                                onScanComplete.onCountChange(scanMusicFile.mPathList.size());
                            }
                        }
                        break;
                    default:
                        break;
                }
            }
        }
    }

    /**
     * 遍历文件目录下的所有文件
     *
     * @param file 需要扫描的文件目录
     */
    private void searchFile(File file) {
        if (file.isDirectory() && file.listFiles() != null) {
            File[] items = file.listFiles();
            for (File item : items) {
                searchFile(item);
            }
            return;
        }
        //过滤没有后缀名的文件
        if (!file.getName().contains(".")) {
            return;
        }
        //判断文件的类型是否支持
        for (String format : mSupportMedia) {
            if (file.getName().endsWith(format)) {
//                Log.e(TAG, "searchFile: "+file.getAbsolutePath());
                mPathList.add(file.getAbsolutePath());
                mHandler.sendEmptyMessage(COUNT_CHANGE);
                return;
            }
        }
    }

    public abstract static class OnScanComplete {
        public OnScanComplete() {

        }

        protected abstract void onComplete(ArrayList<String> paths);

        void onCountChange(int size) {

        }
    }
}
