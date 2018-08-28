package com.zy.ppmusic.adapter.base;

import android.content.Context;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;

/**
 * @author ZhiTouPC
 * @date 2018/1/3
 */
public abstract class AbstractExpandableAdapter extends RecyclerView.Adapter<ExpandableViewHolder> {
    private WeakReference<Context> mContextWeak;

    @NonNull
    @Override
    public ExpandableViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (mContextWeak != null) {
            mContextWeak.clear();
        }
        mContextWeak = new WeakReference<>(parent.getContext());
        ExpandableViewHolder holder = setupViewHolder(parent, viewType);
        bindHolderData(holder, viewType);
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ExpandableViewHolder holder, int position) {
        setupItemData(holder, position);
    }

    @Override
    public int getItemCount() {
        return itemCount();
    }

    @Nullable
    protected Context getContext(){
        if(mContextWeak == null){
            return null;
        }
        return mContextWeak.get();
    }

    /**
     * 为每个item设置holder
     *
     * @param parent   父布局
     * @param viewType 布局类型
     * @return 所属holder
     */
    public abstract ExpandableViewHolder setupViewHolder(ViewGroup parent, int viewType);

    /**
     * 设置item的数据
     *
     * @param holder   item所属holder
     * @param position item位置
     */
    public abstract void setupItemData(ExpandableViewHolder holder, int position);

    /**
     * 绑定一些事件
     *
     * @param holder   对应的holder
     * @param viewType 对应的布局
     */
    public void bindHolderData(ExpandableViewHolder holder, int viewType) {

    }

    /**
     * item数量
     *
     * @return 数量
     */
    public abstract int itemCount();


}
