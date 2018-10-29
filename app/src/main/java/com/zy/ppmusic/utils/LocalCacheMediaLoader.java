package com.zy.ppmusic.utils;

import android.support.annotation.NonNull;

import com.zy.ppmusic.entity.MusicInfoEntity;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * @author y-slience
 */
public class LocalCacheMediaLoader implements IMediaLoader {

    @NonNull
    @Override
    public List<String> getMediaPaths() {
        Object localCache = FileUtils.INSTANCE.readObject(Constant.INSTANCE.getCACHE_FILE_PATH());
        if (localCache != null) {
            ArrayList<MusicInfoEntity> entities = (ArrayList<MusicInfoEntity>) localCache;
            DataTransform.get().transFormData(entities);
            return DataTransform.get().getPathList();
        }
        return new ArrayList<>();
    }
}
