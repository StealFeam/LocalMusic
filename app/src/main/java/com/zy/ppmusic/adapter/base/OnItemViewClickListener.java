package com.zy.ppmusic.adapter.base;

import android.view.View;

/**
 * @author ZhiTouPC
 * @date 2018/2/11
 */

public interface OnItemViewClickListener {
    /**
     * itemView点击事件
     * @param v 触发事件的View
     * @param position 在adapter中的位置
     */
    void onItemViewClick(View v, int position);
}
