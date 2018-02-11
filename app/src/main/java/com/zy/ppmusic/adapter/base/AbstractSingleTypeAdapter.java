package com.zy.ppmusic.adapter.base;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author ZhiTouPC
 * @date 2018/1/4
 */

public abstract class AbstractSingleTypeAdapter extends AbstractExpandableAdapter{

    @Override
    public ExpandableViewHolder setupViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext())
                .inflate(getItemLayoutId(),parent,false);
        return new ExpandableViewHolder(itemView);
    }

    /**
     * 获取itemLayout的Id
     * @return id
     */
    public abstract int getItemLayoutId();
}
