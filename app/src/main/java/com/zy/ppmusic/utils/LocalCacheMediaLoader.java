package com.zy.ppmusic.utils;

import com.zy.ppmusic.App;
import com.zy.ppmusic.entity.MusicInfoEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author y-slience
 * @date 2018/5/8
 */

public class LocalCacheMediaLoader implements IMediaLoader{

    @Override
    public List<String> getMediaPaths() {
        String localPath = App.getInstance().getCacheDir().getAbsolutePath();
        Object localCache = FileUtils.INSTANCE.readObject(localPath+"/cache.obj");
        if (localCache != null) {
            ArrayList<MusicInfoEntity> entities = (ArrayList<MusicInfoEntity>) localCache;
            DataTransform.Companion.get().transFormData(entities);
            return DataTransform.Companion.get().getPathList();
        }
        return null;
    }
}
