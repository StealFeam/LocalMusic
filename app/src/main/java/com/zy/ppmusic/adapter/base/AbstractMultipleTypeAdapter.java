package com.zy.ppmusic.adapter.base;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author stealfeam
 * @date 2018/1/4
 */
public abstract class AbstractMultipleTypeAdapter extends AbstractExpandableAdapter {

    @Override
    public ExpandableViewHolder setupViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).
                inflate(getItemLayoutIdByType(viewType),
                parent, false);
        return new ExpandableViewHolder(itemView);
    }

    @Override
    public int getItemViewType(int position) {
        return getItemTypeByPosition(position);
    }

    /**
     * 通过position判断item的布局类型
     *
     * @param position item位置
     * @return 布局id
     */
    public abstract int getItemTypeByPosition(int position);

    /**
     * 通过viewType获取item布局id
     *
     * @param viewType ~~
     * @return item布局id
     */
    public abstract int getItemLayoutIdByType(int viewType);

}
