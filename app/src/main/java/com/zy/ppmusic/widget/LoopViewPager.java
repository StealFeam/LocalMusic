package com.zy.ppmusic.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;

/**
 * @author ZhiTouPC
 * @date 2018/2/7
 */
public class LoopViewPager extends ViewPager {

    public LoopViewPager(@NonNull Context context) {
        this(context, null);
    }

    public LoopViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    private int mLastX;
    private int maxTranslation = 0;
    private boolean mSkipToFirst = false;
    private boolean mSkipToEnd = false;

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (maxTranslation == 0) {
            WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                manager.getDefaultDisplay().getMetrics(displayMetrics);
                maxTranslation = displayMetrics.widthPixels / 3;
            }
        }
    }


    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (getAdapter() == null) {
            return super.onTouchEvent(ev);
        }
        boolean intercepted = false;
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastX = (int) ev.getX();
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (ev.getX() - mLastX);
                if (getCurrentItem() == 0) {
                    //从左往右
                    intercepted = deltaX > 0 && getScrollX() == 0;
                }
                if (getCurrentItem() == getAdapter().getCount() - 1) {
                    intercepted = deltaX < 0 && !canScrollHorizontally(1);
                }
                if (intercepted) {
                    if (Math.abs(deltaX) > maxTranslation) {
                        if (deltaX > 0) {
                            mSkipToEnd = true;
                            mSkipToFirst = false;
                        } else {
                            mSkipToFirst = true;
                            mSkipToEnd = false;
                        }
                        return true;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                //当滑动到第一个item时，且向右滑动，跳转到最后一个item
                //当滑动到最后一个item时，且向左滑动，跳转到第一个item
                if (mSkipToEnd) {
                    setCurrentItem(getAdapter().getCount(), false);
                    mSkipToEnd = false;
                    mSkipToFirst = false;
                    return true;
                }
                if (mSkipToFirst) {
                    setCurrentItem(0, false);
                    mSkipToFirst = false;
                    mSkipToFirst = false;
                    return true;
                }
                break;
            default:
                break;
        }
        return super.onTouchEvent(ev);
    }

}
