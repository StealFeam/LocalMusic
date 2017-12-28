package com.zy.ppmusic.widget;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.WindowManager;

import com.zy.ppmusic.utils.PrintOut;

import java.util.ArrayList;

/**
 * @author ZhiTouPC
 */
public class WaveRefreshView extends View {
    private static final String TAG = "WaveRefreshView";
    private final float LINE_AREA = 3f / 5f;
    private final float OTHER_AREA = 2f / 5f;
    /**
     * 最小宽度占屏幕的比例
     */
    private final float MIN_WIDTH_PERCENT = 1F / 6F;
    private Paint mCirclePaint;
    private int minWidth = 20;
    private float lineWidth;
    private float whiteWidth;
    private int maxChange;
    private ArrayList<Animator> mAnimatorList = new ArrayList<>();
    private RectF[] lineRectF = new RectF[5];
    private float[] screenParams;
    private AnimatorHandler handler;
    private float lastPercent;

    public WaveRefreshView(Context context) {
        this(context, null);
    }

    public WaveRefreshView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public WaveRefreshView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        mCirclePaint = new Paint();
        mCirclePaint.setColor(Color.WHITE);
        mCirclePaint.setAntiAlias(true);
        mCirclePaint.setStyle(Paint.Style.FILL);
        mCirclePaint.setStrokeWidth(5);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        maxChange = h / 3;
        //线区域总宽
        lineWidth = getMeasuredWidth() * LINE_AREA;
        //空白区总宽
        whiteWidth = getMeasuredWidth() * OTHER_AREA;

        lineWidth /= lineRectF.length;
        whiteWidth /= lineRectF.length;

        minWidth = (int) ((getScreenParams(0) * MIN_WIDTH_PERCENT) / getScreenParams(2));
        PrintOut.print("最小宽度计算值：" + minWidth);

        for (int i = 0; i < lineRectF.length; i++) {
            lineRectF[i] = new RectF((lineWidth + whiteWidth) * i, maxChange,
                    (lineWidth + whiteWidth) * i + lineWidth, getMeasuredHeight() - maxChange);
            Log.w(TAG, "onSizeChanged: " + lineRectF[i].toString());
        }
        startAnim();
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

    private float getScreenParams(int posi) {
        if (posi < 0 || posi > 2) {
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
        return screenParams[posi];
    }

    public void startAnim() {
        for (Animator animator : mAnimatorList) {
            animator.cancel();
        }
        mAnimatorList.clear();
        for (int i = 0; i < lineRectF.length; i++) {
            final int index = i;
            ValueAnimator animator = ValueAnimator.ofInt(0, maxChange);
            animator.setDuration(lineRectF.length * 100);
            animator.setRepeatMode(ValueAnimator.REVERSE);
            animator.setRepeatCount(-1);
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator animation) {
                    float percent = animation.getAnimatedFraction();
                    lineRectF[index].top = maxChange * percent;
                    lineRectF[index].bottom = getMeasuredHeight() - maxChange * percent;
                    postInvalidate();
                }
            });
            mAnimatorList.add(animator);
        }
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        } else {
            handler = new AnimatorHandler(Looper.getMainLooper(), mAnimatorList);
        }
        for (int i = 0; i < mAnimatorList.size(); i++) {
            handler.sendEmptyMessageDelayed(i, (i + 1) * 80);
        }
    }

    private static class AnimatorHandler extends Handler {
        private ArrayList<Animator> animatorArrayList;

        private AnimatorHandler(Looper looper, ArrayList<Animator> animatorArrayList) {
            super(looper);
            this.animatorArrayList = animatorArrayList;
        }

        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int currentStartIndex = msg.what;
            if (animatorArrayList != null && currentStartIndex < animatorArrayList.size()) {
                Animator animator = animatorArrayList.get(currentStartIndex);
                animator.start();
            }
        }
    }

}
