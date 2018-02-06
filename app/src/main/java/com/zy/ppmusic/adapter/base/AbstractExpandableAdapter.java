package com.zy.ppmusic.adapter.base;

import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.view.ViewGroup;

/**
 * @author ZhiTouPC
 * @date 2018/1/3
 */
public abstract class AbstractExpandableAdapter extends
        RecyclerView.Adapter<AbstractExpandableAdapter.ExpandableViewHolder>{

    @Override
    public ExpandableViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return setupViewHolder(parent,viewType);
    }

    @Override
    public void onBindViewHolder(ExpandableViewHolder holder, int position) {
        setupItemData(holder,position);
    }

    @Override
    public int getItemCount() {
        return itemCount();
    }

    /**
     * 为每个item设置holder
     * @param parent 父布局
     * @param viewType 布局类型
     * @return 所属holder
     */
    public abstract ExpandableViewHolder setupViewHolder(ViewGroup parent,int viewType);

    /**
     * 设置item的数据
     * @param holder item所属holder
     * @param position item位置
     */
    public abstract void setupItemData(ExpandableViewHolder holder,int position);

    /**
     * item数量
     * @return 数量
     */
    public abstract int itemCount();

    protected static class ExpandableViewHolder extends RecyclerView.ViewHolder {
        private final SparseArrayCompat<View> mItemViews;

        protected ExpandableViewHolder(View itemView) {
            super(itemView);
            mItemViews = new SparseArrayCompat<>();
        }

        @SuppressWarnings("unchecked")
        public <T extends View> T getView(int id) {
            if (mItemViews.get(id) != null) {
                return (T) mItemViews.get(id);
            }
            View view = itemView.findViewById(id);
            if (view != null) {
                mItemViews.put(id, view);
                return (T) view;
            }
            return null;
        }
    }
}
