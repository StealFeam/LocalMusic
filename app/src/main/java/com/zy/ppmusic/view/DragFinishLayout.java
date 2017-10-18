package com.zy.ppmusic.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class DragFinishLayout extends LinearLayout {
    private static final String TAG = "DragFinishLayout";
    private DragFinishHelper helper;

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
        return helper.onTouchEvent(ev) || super.dispatchTouchEvent(ev);

    }
}
