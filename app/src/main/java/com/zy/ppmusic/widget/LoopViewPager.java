package com.zy.ppmusic.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewpager.widget.ViewPager;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.view.MotionEvent;
import android.view.WindowManager;

/**
 * @author stealfeam
 * @date 2018/2/7
 */
public class LoopViewPager extends ViewPager {
    private int mLastX;
    private int maxTranslation = 0;
    private boolean mSkipToFirst = false;
    private boolean mSkipToEnd = false;
    private float damp = 1;
    private OnDragListener mDragListener;

    public LoopViewPager(@NonNull Context context) {
        this(context, null);
    }

    public LoopViewPager(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public void setDragListener(OnDragListener listener) {
        this.mDragListener = listener;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        if (maxTranslation == 0) {
            WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                DisplayMetrics displayMetrics = new DisplayMetrics();
                manager.getDefaultDisplay().getMetrics(displayMetrics);
                maxTranslation = displayMetrics.widthPixels / 3;
                System.out.println("max=====" + maxTranslation);
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
                    if (!intercepted && getTranslationX() > 0) {
                        intercepted = true;
                    }
                }
                if (getCurrentItem() == getAdapter().getCount() - 1) {
                    intercepted = deltaX < 0 && !canScrollHorizontally(1);
                }
                if (intercepted) {
                    damp += 0.3;
                    float currentTranslationX = getTranslationX() + deltaX / damp;
                    if (Math.abs(currentTranslationX) > maxTranslation) {
                        currentTranslationX = currentTranslationX > 0 ? maxTranslation : -maxTranslation;
                    }
                    setTranslationX(currentTranslationX);
                    if (mDragListener != null) {
                        mDragListener.onDrag(getTranslationX());
                    }
                    if (Math.abs(deltaX) > maxTranslation) {
                        if (deltaX > 0) {
                            mSkipToEnd = true;
                            mSkipToFirst = false;
                        } else {
                            mSkipToFirst = true;
                            mSkipToEnd = false;
                        }
                        return true;
                    } else {
                        mSkipToEnd = false;
                        mSkipToFirst = false;
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
                //当滑动到第一个item时，且向右滑动，跳转到最后一个item
                //当滑动到最后一个item时，且向左滑动，跳转到第一个item
                damp = 1;
                setTranslationX(0);
                if (mDragListener != null) {
                    mDragListener.onDrag(0);
                }
                if (mSkipToEnd) {
                    setCurrentItem(getAdapter().getCount(), false);
                    mSkipToEnd = false;
                    mSkipToFirst = false;
                    return true;
                }
                if (mSkipToFirst) {
                    setCurrentItem(0, false);
                    mSkipToFirst = false;
                    return true;
                }
                break;
            default:
                break;
        }
        return intercepted || super.onTouchEvent(ev);
    }
    @Keep
    public interface OnDragListener {
        /**
         * x方向移动距离变化时
         *
         * @param distance 移动的距离
         */
        void onDrag(float distance);
    }
}
