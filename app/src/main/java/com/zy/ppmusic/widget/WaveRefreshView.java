package com.zy.ppmusic.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentActivity;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.zy.ppmusic.utils.PrintLog;

import java.util.ArrayList;
import java.util.List;

/**
 * @author ZhiTouPC
 */
public class WaveRefreshView extends View implements LifecycleObserver {
    private static final String TAG = "WaveRefreshView";
    /**
     * 最小宽度占屏幕的比例
     */
    private final float MIN_WIDTH_PERCENT = 1F / 6F;
    private Paint mCirclePaint;
    private int minWidth = 20;
    private int maxChange;
    private volatile ArrayList<Animator> mAnimatorList = new ArrayList<>();
    private RectF[] lineRectF = new RectF[5];
    private float[] screenParams;
    private DelayHandler mDelayHandler;
    private Runnable callback = new Runnable() {
        @Override
        public void run() {
            int count = mAnimatorList.size();
            int end = 0;
            while (end < count) {
                mDelayHandler.sendEmptyMessage(end);
                try {
                    Thread.sleep((end + 1) * 80);
                } catch (InterruptedException e) {
                    Log.e(TAG, "thread is interrupted" + this.getClass().getName());
                    break;
                }
                end++;
            }
        }
    };

    public WaveRefreshView(Context context) {
        this(context, null);
    }

    public WaveRefreshView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public WaveRefreshView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        ((FragmentActivity) context).getLifecycle().addObserver(this);
        mCirclePaint = new Paint();
        mCirclePaint.setColor(Color.WHITE);
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setStrokeWidth(5);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float lineArea = 3f / 5f;
        float otherArea = 2f / 5f;
        maxChange = h / 3;
        //线区域总宽
        float lineWidth = getMeasuredWidth() * lineArea;
        //空白区总宽
        float whiteWidth = getMeasuredWidth() * otherArea;

        lineWidth /= lineRectF.length;
        whiteWidth /= lineRectF.length;

        minWidth = (int) ((getScreenParams(0) * MIN_WIDTH_PERCENT) / getScreenParams(2));
        PrintLog.print("最小宽度计算值：" + minWidth);

        for (int i = 0; i < lineRectF.length; i++) {
            lineRectF[i] = new RectF((lineWidth + whiteWidth) * i, maxChange,
                    (lineWidth + whiteWidth) * i + lineWidth, getMeasuredHeight() - maxChange);
            Log.w(TAG, "onSizeChanged: " + lineRectF[i].toString());
        }
        startAnim();
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onActivityStop() {
        stopAnim();
        clearReference();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnim();
        clearReference();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);
        minWidth = (int) ((getScreenParams(0) * MIN_WIDTH_PERCENT) / getScreenParams(2));
        int lastWidthAndHeight = Math.max(Math.max(width, height), minWidth);
        setMeasuredDimension(lastWidthAndHeight, lastWidthAndHeight);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        for (RectF aLineRectF : lineRectF) {
            canvas.drawRoundRect(aLineRectF, 20, 20, mCirclePaint);
        }
    }

    private boolean checkPositionRange(int pos) {
        int minPos = 0;
        int maxPos = 2;
        return pos < minPos || pos > maxPos;
    }

    private float getScreenParams(int pos) {
        if (checkPositionRange(pos)) {
            return 0;
        }
        if (screenParams == null) {
            screenParams = new float[3];
            WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                Display defaultDisplay = manager.getDefaultDisplay();
                defaultDisplay.getMetrics(metrics);
                screenParams[0] = metrics.widthPixels;
                screenParams[1] = metrics.heightPixels;
                screenParams[2] = metrics.scaledDensity;
            } else {
                return 0;
            }
        }
        return screenParams[pos];
    }

    public void startAnim() {
        for (Animator animator : mAnimatorList) {
            animator.cancel();
        }
        mAnimatorList.clear();
        for (int i = 0; i < lineRectF.length; i++) {
            final int index = i;
            ValueAnimator animator = ValueAnimator.ofInt(0, maxChange);
            animator.setDuration(1000);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(-1);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    if (lineRectF[index] == null) {
                        return;
                    }
                    float percent = animation.getAnimatedFraction();
                    lineRectF[index].top = maxChange * percent;
                    lineRectF[index].bottom = getMeasuredHeight() - maxChange * percent;
                    postInvalidate();
                }
            });
            mAnimatorList.add(animator);
        }
        if (mDelayHandler == null) {
            mDelayHandler = new DelayHandler(mAnimatorList);
        }
        mDelayHandler.removeCallbacksAndMessages(null);
        post(callback);
    }

    public void clearReference() {
        removeCallbacks(callback);
        mDelayHandler.sendEmptyMessage(-1);
        mDelayHandler.removeCallbacksAndMessages(null);
        mDelayHandler = null;
        mAnimatorList.clear();
    }

    public void stopAnim() {
        if (mDelayHandler != null) {
            mDelayHandler.sendEmptyMessage(-1);
            mDelayHandler.removeCallbacksAndMessages(null);
        }
        mAnimatorList.clear();
    }

    private static class DelayHandler extends Handler {
        private List<Animator> animators;

        private DelayHandler(List<Animator> list) {
            super(Looper.getMainLooper());
            this.animators = list;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int currentStartIndex = msg.what;
            if (currentStartIndex == -1) {
                if (animators != null) {
                    for (Animator animator : animators) {
                        animator.end();
                        animator.cancel();
                    }
                }
                return;
            }
            if (animators != null && currentStartIndex < animators.size()) {
                Animator animator = animators.get(currentStartIndex);
                animator.start();
            }
        }
    }

}
