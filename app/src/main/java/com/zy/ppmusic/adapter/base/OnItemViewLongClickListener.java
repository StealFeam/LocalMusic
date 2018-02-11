package com.zy.ppmusic.adapter.base;

import android.view.View;

/**
 * @author ZhiTouPC
 * @date 2018/2/11
 */

public interface OnItemViewLongClickListener {
    /**
     * itemView点击事件
     *
     * @param v        触发事件的View
     * @param position 在adapter中的位置
     * @return 是否拦截此次事件
     */
    boolean onItemViewLongClick(View v, int position);
}
