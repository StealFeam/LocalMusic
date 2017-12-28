package com.zy.ppmusic.adapter;

import android.support.v4.view.PagerAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.zy.ppmusic.R;

import java.lang.ref.WeakReference;
import java.util.List;

/**
 * @author ZhiTouPC
 * @date 2017/12/27
 */

public class MediaHeadPageAdapter extends PagerAdapter {
    private WeakReference<View> pageViewReference;
    private List<String> pathList;

    public MediaHeadPageAdapter(List<String> pathList) {
        this.pathList = pathList;
    }

    public void setPathList(List<String> pathList) {
        this.pathList = pathList;
    }

    @Override
    public Object instantiateItem(ViewGroup container, int position) {
        View root = View.inflate(container.getContext(), R.layout.fragment_media_head, container);
        ImageView imageView = root.findViewById(R.id.iv_media_head);
        imageView.setImageResource(R.mipmap.ic_music_launcher_round);
        return imageView;
    }

    @Override
    public void destroyItem(ViewGroup container, int position, Object object) {
        View rootView = (View) ((ImageView) object).getParent();
        container.removeView(rootView);
    }

    @Override
    public int getCount() {
        return pathList.size();
    }

    @Override
    public boolean isViewFromObject(View view, Object object) {
        return view == object;
    }
}
