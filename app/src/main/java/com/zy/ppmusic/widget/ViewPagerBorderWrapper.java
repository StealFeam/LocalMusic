package com.zy.ppmusic.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import com.zy.ppmusic.R;
import com.zy.ppmusic.utils.UiUtils;

/**
 * @author y-slience
 * @date 2018/3/22
 * 只支持横向，还欠缺的是多指判断，耦合性相对高
 */
public class ViewPagerBorderWrapper extends ViewGroup {
    private static final String TAG = "ViewPagerBorderWrapper";
    /**
     * 中间的View
     */
    private ViewPager mViewPager;
    /**
     * 左侧的View
     */
    private SimpleVerticalTextView mLeftView;
    /**
     * 右侧的View
     */
    private SimpleVerticalTextView mRightView;
    /**
     * 最大的移动距离
     */
    private int maxChangedWidth;
    /**
     * 上次事件的点横坐标
     */
    private int mLastTouchX = 0;
    /**
     * 上次拦截事件的初始按下时间
     */
    private long mInterceptDownTime = 0L;
    /**
     * 上次是否拦截了事件
     */
    private boolean isLastIntercepted = false;
    /**
     * 是否满足跳转的条件
     */
    private boolean isSkip = false;

    public ViewPagerBorderWrapper(Context context) {
        this(context, null);
    }

    public ViewPagerBorderWrapper(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public ViewPagerBorderWrapper(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    private void attachViewPager(ViewPager viewPager) {
        this.mViewPager = viewPager;
        if (mLeftView == null) {
            mLeftView = new SimpleVerticalTextView(getContext());
            mLeftView.setGravity(Gravity.CENTER);
        }
        if (mRightView == null) {
            mRightView = new SimpleVerticalTextView(getContext());
            mRightView.setGravity(Gravity.CENTER);
        }
        mLeftView.setTextColor(UiUtils.getColor(R.color.colorBlack));
        mRightView.setTextColor(UiUtils.getColor(R.color.colorBlack));
        removeAllViews();
        addView(mLeftView, 0,new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT));
        addView(mViewPager, 1);
        addView(mRightView, 2,new LayoutParams(LayoutParams.WRAP_CONTENT,LayoutParams.MATCH_PARENT));
    }


    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        attachViewPager((ViewPager) getChildAt(0));
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        measureChildren(widthMeasureSpec, heightMeasureSpec);
        int childCount = getChildCount();
        int measureWidth = 0;
        int measureHeight = 0;
        for (int i = 0; i < childCount; i++) {
            View childAt = getChildAt(i);
            measureWidth += childAt.getMeasuredWidth();
            if (measureHeight < childAt.getMeasuredHeight()) {
                measureHeight = childAt.getMeasuredHeight();
            }
        }
        setMeasuredDimension(measureWidth, measureHeight);
        //仅在首次时将左右两侧的View宽度设置为0
        if (maxChangedWidth == 0) {
            maxChangedWidth = mViewPager.getMeasuredWidth() / 4;
            LayoutParams layoutParams = mLeftView.getLayoutParams();
            layoutParams.width = 0;
            mLeftView.setLayoutParams(layoutParams);


            Log.e(TAG, "onMeasure: width==" + getMeasuredWidth());
            System.out.println("left====" + mLeftView.getMeasuredWidth());
            System.out.println("content====" + mViewPager.getMeasuredWidth());
            System.out.println("right======" + mRightView.getMeasuredWidth());
        }
    }


    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = (int) ev.getRawX();
                mInterceptDownTime = ev.getDownTime();
                isLastIntercepted = false;
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (ev.getRawX() - mLastTouchX);
                if (mViewPager.getCurrentItem() == 0) {
                    if (deltaX > 0) {
                        isLastIntercepted = true;
                        Log.e(TAG, "diapatchTouchEvent：这是最左侧触发：");
                        return true;
                    }
                    //上次的事件序列并没有结束
                    return isLastIntercepted && mInterceptDownTime == ev.getDownTime();
                }
                if (mViewPager.getAdapter() == null) {
                    Log.d(TAG, "dispatchTouchEvent: this viewpager's adapter is null");
                    return false;
                }

                if (mViewPager.getCurrentItem() == mViewPager.getAdapter().getCount() - 1) {
                    if (deltaX < 0) {
                        isLastIntercepted = true;
                        Log.e(TAG, "dispatchTouchEvent: 这是到最右侧时触发");
                        return true;
                    }
                    return isLastIntercepted && mInterceptDownTime == ev.getDownTime();
                }
                break;
            case MotionEvent.ACTION_UP:
                mLastTouchX = (int) ev.getRawX();
                isLastIntercepted = false;
                mInterceptDownTime = ev.getDownTime();
                break;
            default:
                break;
        }
        return super.onInterceptTouchEvent(ev);
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        if (mViewPager.getAdapter() == null) {
            return false;
        }
        switch (ev.getAction()) {
            case MotionEvent.ACTION_DOWN:
                mLastTouchX = (int) ev.getRawX();
                if (getAnimation() != null) {
                    getAnimation().cancel();
                }
                clearAnimation();
                break;
            case MotionEvent.ACTION_MOVE:
                int deltaX = (int) (ev.getRawX() - mLastTouchX);
                //第一个条目，并且是向右滑动
                if (mViewPager.getCurrentItem() == 0) {
                    LayoutParams layoutParams = mLeftView.getLayoutParams();
                    layoutParams.width += deltaX;
                    layoutParams.width = Math.max(0, layoutParams.width);
                    if (layoutParams.width > maxChangedWidth) {
                        mLeftView.setText("释放跳转到最后一首");
                        isSkip = true;
                    } else {
                        isSkip = false;
                        mLeftView.setText("继续滑动");
                    }
                    mLeftView.setLayoutParams(layoutParams);
                    //最后一个条目，并且是向左滑动
                } else if (mViewPager.getCurrentItem() == mViewPager.getAdapter().getCount() - 1) {
                    LayoutParams layoutParams = mRightView.getLayoutParams();
                    layoutParams.width -= deltaX;
                    layoutParams.width = Math.max(0, layoutParams.width);
                    if (layoutParams.width > maxChangedWidth) {
                        mRightView.setText("释放跳转到第一首");
                        isSkip = true;
                    } else {
                        isSkip = false;
                        mRightView.setText("继续滑动");
                    }
                    mRightView.setLayoutParams(layoutParams);
                    scrollBy(-deltaX, 0);
                    invalidate();
                }
                mLastTouchX = (int) ev.getRawX();
                break;
            case MotionEvent.ACTION_UP:
                final LayoutParams leftParams = mLeftView.getLayoutParams();
                final LayoutParams rightParams = mRightView.getLayoutParams();
                if (rightParams.width > 0) {
                    restoreView(rightParams, mRightView, 0);
                } else {
                    int endPosition = -1;
                    if (mViewPager.getAdapter() != null) {
                        endPosition = mViewPager.getAdapter().getCount() - 1;
                    }
                    restoreView(leftParams, mLeftView, endPosition);
                }
                break;
            default:
                break;
        }
        return true;
    }

    private void restoreView(final LayoutParams params, final View view, final int itemPosition) {
        ValueAnimator valueAnimator = ValueAnimator.ofInt(params.width, 0);
        valueAnimator.setDuration(80);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                params.width = (int) animation.getAnimatedValue();
                //当前仅有右侧滑动会改动scrollX
                if(getScrollX() != 0){
                    scrollTo(params.width,0);
                    invalidate();
                }
                view.setLayoutParams(params);
                if (params.width == 0) {
                    animation.removeAllUpdateListeners();
                    if (isSkip) {
                        if (itemPosition == -1) {
                            System.out.println("-1位置错误");
                            return;
                        }
                        mViewPager.setCurrentItem(itemPosition, false);
                    }
                }
            }
        });
        valueAnimator.setInterpolator(new LinearInterpolator());
        valueAnimator.start();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = l;
        int right = 0;
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            right += childAt.getMeasuredWidth();
            if (right > r) {
                right = r;
            }
            childAt.layout(left, 0, right, getMeasuredHeight());
            left += childAt.getMeasuredWidth();
        }
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (int i = 0; i < getChildCount(); i++) {
            View childAt = getChildAt(i);
            childAt.draw(canvas);
        }
    }

}
