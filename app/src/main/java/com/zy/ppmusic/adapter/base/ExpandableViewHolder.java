package com.zy.ppmusic.adapter.base;

import android.support.v4.util.SparseArrayCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * @author ZhiTouPC
 */
public class ExpandableViewHolder extends RecyclerView.ViewHolder
        implements View.OnClickListener, View.OnLongClickListener {
    private SparseArrayCompat<View> mItemChildViews;
    private OnItemViewClickListener mItemClickListener;
    private OnItemViewLongClickListener mItemLongClickListener;

    public ExpandableViewHolder(View itemView) {
        super(itemView);
        mItemChildViews = new SparseArrayCompat<>();
    }

    public void attachOnClickListener(OnItemViewClickListener l,View... views) {
        if(mItemClickListener != l){
            this.mItemClickListener = l;
        }
        for (View itemView : views) {
            itemView.setOnClickListener(this);
        }
    }

    public void attachOnLongClickListener(OnItemViewLongClickListener l,View... views) {
        this.mItemLongClickListener = l;
        for (View itemView : views) {
            itemView.setOnClickListener(this);
        }
    }

    @Override
    public boolean onLongClick(View v) {
        return mItemLongClickListener != null &&
                mItemLongClickListener.onItemViewLongClick(v, getAdapterPosition());
    }

    @Override
    public void onClick(View v) {
        if (mItemClickListener != null) {
            mItemClickListener.onItemViewClick(v, getAdapterPosition());
        }
    }

    @SuppressWarnings("unchecked")
    public <T extends View> T getView(int id) {
        if (mItemChildViews.get(id) != null) {
            return (T) mItemChildViews.get(id);
        }
        View view = itemView.findViewById(id);
        if (view != null) {
            mItemChildViews.put(id, view);
            return (T) view;
        }
        return null;
    }


}