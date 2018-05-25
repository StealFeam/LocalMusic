package com.zy.ppmusic.utils;

import com.zy.ppmusic.App;
import com.zy.ppmusic.entity.MusicInfoEntity;

import java.util.List;

/**
 * @author y-slience
 * @date 2018/5/8
 */

public class LocalCacheMediaLoader implements IMediaLoader{

    @Override
    public List<String> getMediaPaths() {
        String localPath = App.getInstance().getCacheDir().getAbsolutePath();
        Object localCache = FileUtils.readObject(localPath+"/cache.obj");
        if (localCache != null) {
            List<MusicInfoEntity> entities = (List<MusicInfoEntity>) localCache;
            DataTransform.getInstance().transFormData(entities);
            return DataTransform.getInstance().getPathList();
        }
        return null;
    }
}
