package com.zy.ppmusic.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

/**
 * @author ZhiTouPC
 */
public class DragFinishLayout extends LinearLayout {
    private static final String TAG = "DragFinishLayout";
    private final DragFinishHelper helper;

    public DragFinishLayout(Context context) {
        this(context,null);
    }

    public DragFinishLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        helper = new DragFinishHelper();
        helper.attachView(this);
    }

    public void setDragFinishListener(DragFinishHelper.DragFinishListener l){
        this.helper.setListener(l);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
        return super.dispatchTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        return helper.onTouchEvent(event) || super.onTouchEvent(event);
    }
}
