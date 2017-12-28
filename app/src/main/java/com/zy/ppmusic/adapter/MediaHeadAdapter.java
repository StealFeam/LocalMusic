package com.zy.ppmusic.adapter;

import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.media.MediaMetadataCompat;

import com.zy.ppmusic.mvp.view.frag.MediaHeadFragment;
import com.zy.ppmusic.utils.DataTransform;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZhiTouPC
 * @date 2017/12/26
 */

public class MediaHeadAdapter extends FragmentStatePagerAdapter {
    private List<String> mPathList;

    public MediaHeadAdapter(FragmentManager fm, List<String> pathList) {
        super(fm);
        this.mPathList = new ArrayList<>();
        setPathList(pathList);
    }

    public void setPathList(List<String> pathList) {
        if (pathList == null) {
            return;
        }

        if (pathList.size() == this.mPathList.size()) {
            return;
        }

        this.mPathList.clear();
        this.mPathList.addAll(pathList);
        notifyDataSetChanged();
    }

    @Override
    public Fragment getItem(int position) {
        int zero = 0;
        if (this.mPathList.size() == zero) {
            return MediaHeadFragment.createInstance(null);
        }
        String mediaId = DataTransform.getInstance().getMediaIdList().get(position);
        MediaMetadataCompat metadataCompat = DataTransform.getInstance().getMetadataItem(mediaId);
        return MediaHeadFragment.createInstance(metadataCompat);
    }

    @Override
    public int getCount() {
        return this.mPathList.size();
    }
}
