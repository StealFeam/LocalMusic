package com.zy.ppmusic.widget;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SparseBooleanArray;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;
import android.widget.LinearLayout;

import com.zy.ppmusic.R;
import com.zy.ppmusic.utils.PrintLog;


/**
 * @author stealfeam
 */
public class WaveRefreshView extends LinearLayout {
    private static final String TAG = "WaveRefreshView";
    /**
     * 最小宽度占屏幕的比例
     */
    private final float MIN_WIDTH_PERCENT = 1F / 6F;
    private Paint mCirclePaint;
    private int minWidth = 20;
    private int maxChange;
    private RectF[] lineRectF = new RectF[5];
    private float[] screenParams;

    public WaveRefreshView(Context context) {
        this(context, null);
    }

    public WaveRefreshView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public WaveRefreshView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mCirclePaint = new Paint();
        mCirclePaint.setColor(ContextCompat.getColor(context, R.color.colorTheme));
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
        PrintLog.INSTANCE.print("最小宽度计算值：" + minWidth);

        for (int i = 0; i < lineRectF.length; i++) {
            lineRectF[i] = new RectF((lineWidth + whiteWidth) * i, maxChange,
                    (lineWidth + whiteWidth) * i + lineWidth, getMeasuredHeight() - maxChange);
            Log.w(TAG, "onSizeChanged: " + lineRectF[i].toString());
        }
        startAnim();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        startAnim();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopRunningAnim();
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

    /**
     * 判断是否到达最小或最大值
     */
    private SparseBooleanArray mDirectionArray = new SparseBooleanArray(lineRectF.length);

    @Override
    protected void onDraw(Canvas canvas) {
        if (isStopped) {
            return;
        }
        for (int i = 0; i < lineRectF.length; i++) {
            RectF itemRect = lineRectF[i];
            if (mDirectionArray.get(i)) {
                itemRect.top = itemRect.top + maxChange * 0.02f * (i + 1);
            } else {
                itemRect.top = itemRect.top - maxChange * 0.02f * (i + 1);
            }
            if (itemRect.top <= 0) {
                itemRect.top = 0;
                mDirectionArray.put(i, true);
            } else if (itemRect.top >= maxChange) {
                itemRect.top = maxChange;
                mDirectionArray.put(i, false);
            }
            canvas.save();
            if (canvas.clipRect(itemRect)) {
                canvas.drawRoundRect(itemRect, 20, 20, mCirclePaint);
            }
            canvas.restore();
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

    private ValueAnimator animator;
    private boolean isStopped = true;

    public void startAnim() {
        stopRunningAnim();
        animator = ValueAnimator.ofFloat(0, maxChange);
        animator.setDuration(1000);
        animator.setRepeatCount(-1);
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                invalidate();
            }
        });
        animator.start();
        isStopped = false;
    }

    public void clearReference() {

    }

    public void stopRunningAnim() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
            animator = null;
            isStopped = true;
        }
    }

}
