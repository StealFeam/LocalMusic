package com.zy.ppmusic.adapter;

import android.view.Gravity;
import android.widget.TextView;

import com.zy.ppmusic.R;
import com.zy.ppmusic.adapter.base.AbstractSingleTypeAdapter;
import com.zy.ppmusic.adapter.base.ExpandableViewHolder;
import com.zy.ppmusic.adapter.base.OnItemViewClickListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
/**
 * @author ZY
 */
public class TimeClockAdapter extends AbstractSingleTypeAdapter {
    private List<Integer> array;
    private boolean isTicking = false;
    private OnItemViewClickListener mClickListener;

    public TimeClockAdapter() {
        this.array = new ArrayList<>();
        int mCount = 5;
        for (int i = 0; i < mCount; i++) {
            this.array.add((i + 1) * 15);
        }
        this.array.add(120);
    }

    /**
     * 是否正在倒计时
     */
    public void setTicking(boolean flag) {
        if (isTicking == flag) {
            return;
        }
        isTicking = flag;
        notifyDataSetChanged();
    }

    public boolean isTick() {
        return isTicking;
    }

    public int getItem(int position) {
        return array.get(position);
    }

    public void setOnItemClickListener(OnItemViewClickListener l){
        this.mClickListener = l;
    }

    @Override
    public void bindHolderData(ExpandableViewHolder holder, int viewType) {
        super.bindHolderData(holder, viewType);
        holder.attachOnClickListener(mClickListener,holder.itemView);
    }

    @Override
    public void setupItemData(ExpandableViewHolder holder, int position) {
        TextView tvTime = holder.getView(R.id.item_normal_text);
        tvTime.setGravity(Gravity.CENTER);
        if (isTicking) {
            if (position == 0) {
                tvTime.setText("关闭定时");
            } else {
                tvTime.setText(String.format(Locale.CHINA, "%d分钟", array.get(position - 1)));
            }
        } else {
            tvTime.setText(String.format(Locale.CHINA, "%d分钟", array.get(position)));
        }
    }

    @Override
    public int itemCount() {
        if (array == null) {
            return 0;
        }
        return isTicking ? array.size() + 1 : array.size();
    }

    @Override
    public int getItemLayoutId() {
        return R.layout.item_list_normal;
    }

}
