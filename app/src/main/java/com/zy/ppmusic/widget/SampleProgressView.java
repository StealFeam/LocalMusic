package com.zy.ppmusic.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.support.annotation.Nullable;
import android.text.TextPaint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import com.zy.ppmusic.R;

/**
 * @author stealfeam
 * @since 2018/10/23
 * 1.实现SeekBar触摸功能
 * 2.点击原点滑动上方有进度值显示
 * 3.扩展更多方向
 */
public class SampleProgressView extends View {
    private final int STATE_NORMAL = 1;
    private final int STATE_TOUCH = 2;
    private final int STATE_TRACKING = 3;
    private final int STATE_RELEASE = 4;

    private int minWidth = 400;
    private int minHeight = 300;
    private int lineMinHeight = 20;
    //进度条区域
    private Rect mLineArea;
    //当前进度值
    private float progress;
    //当前状态
    private int currentState;
    private Paint linePaint;
    private TextPaint textPaint;

    private int colorProgress;
    private int colorUnReachProgress;
    private int colorPoint;
    private ITouchShowType showType = new ProgressType();

    public SampleProgressView(Context context) {
        this(context, null);
    }

    public SampleProgressView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SampleProgressView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        linePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        linePaint.setColor(Color.RED);

        mLineArea = new Rect();
        currentState = STATE_NORMAL;

        TypedArray typedArray = context.obtainStyledAttributes(attrs, R.styleable.SampleProgressView);
        colorProgress = typedArray.getColor(R.styleable.SampleProgressView_colorProgress, Color.RED);
        colorPoint = typedArray.getColor(R.styleable.SampleProgressView_colorPoint, Color.BLUE);
        colorUnReachProgress = typedArray.getColor(R.styleable.SampleProgressView_colorUnReachProgress, Color.DKGRAY);
        progress = typedArray.getFloat(R.styleable.SampleProgressView_progress, 0);
        progress /= 100f;
        float textSize = typedArray.getDimensionPixelSize(R.styleable.SampleProgressView_textSize, 18);
        int textColor = typedArray.getColor(R.styleable.SampleProgressView_textColor, Color.BLACK);

        typedArray.recycle();

        textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setTextSize(textSize);
        textPaint.setColor(textColor);
    }

    public void setColorProgress(int color) {
        this.colorProgress = color;
        invalidate();
    }

    public void setColorUnReachProgress(int color) {
        this.colorUnReachProgress = color;
        invalidate();
    }

    public void setColorPoint(int color) {
        this.colorPoint = color;
        invalidate();
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mLineArea.set(getPaddingLeft(), (int) (getTextHeight() + getPaddingTop()),
                getMeasuredWidth() - getPaddingRight(), getMeasuredHeight() - getPaddingBottom());
    }

    public int getWidthWithoutPadding() {
        return getMeasuredWidth() - getPaddingLeft() - getPaddingRight();
    }

    public int getHeightWithoutPadding() {
        return getMeasuredHeight() - getPaddingTop() - getPaddingBottom();
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int x = (int) event.getX();
                int y = (int) event.getY();
                boolean result = mLineArea.contains(x, y);
                if (result) {
                    currentState = STATE_TOUCH;
                    invalidate();
                }
                return result;
            default:
                break;
        }
        return super.dispatchTouchEvent(event);
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_MOVE:
                currentState = STATE_TRACKING;
                float touchProgress = event.getX() / getWidth();
                setProgress(touchProgress);
                break;
            case MotionEvent.ACTION_UP:
                currentState = STATE_RELEASE;
                float touchUpProgress = event.getX() / getWidth();
                setProgress(touchUpProgress);
                break;
            default:
                break;
        }
        return true;
    }

    private synchronized void setProgress(float progress) {
        this.progress = Math.max(0, Math.min(1, progress));
        invalidate();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        minHeight = (int) (lineMinHeight + getTextHeight());

        int width = getSuggestedSize(minWidth, widthMeasureSpec);
        width += getPaddingLeft() + getPaddingRight();

        int height = getSuggestedSize(minHeight, heightMeasureSpec);
        height += getPaddingTop() + getPaddingBottom();

        setMeasuredDimension(width, height);

        mLineArea.set(getPaddingLeft(), (int) (getTextHeight() + getPaddingTop()),
                getMeasuredWidth() - getPaddingRight(), getMeasuredHeight() - getPaddingBottom());
    }

    private float getTextHeight() {
        Paint.FontMetrics fontMetrics = textPaint.getFontMetrics();
        return fontMetrics.descent - fontMetrics.ascent;
    }

    private int getSuggestedSize(int _size, int measureSpec) {
        int mode = MeasureSpec.getMode(measureSpec);
        int size = MeasureSpec.getSize(measureSpec);
        switch (mode) {
            case MeasureSpec.EXACTLY:
                return size;
            case MeasureSpec.AT_MOST:
            case MeasureSpec.UNSPECIFIED:
            default:
                return _size;
        }
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);
        drawProgress(canvas);
        drawBg(canvas);
        switch (currentState) {
            case STATE_NORMAL:
            case STATE_TOUCH:
                drawPoint(canvas);
                break;
            case STATE_TRACKING:
                drawText(canvas);
                break;
            case STATE_RELEASE:
                drawText(canvas);
                postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        currentState = STATE_NORMAL;
                        invalidate();
                    }
                }, 200);
                break;
            default:
                break;
        }
    }

    //可以扩展成其他的文本
    private void drawText(Canvas canvas) {
        float x = getWidthWithoutPadding() * progress - getTextWidth()/2 + getPaddingLeft();
        if (x - getTextWidth()/2 < getPaddingLeft()) {
            x += getTextWidth()/2;
        } else if (x + getTextWidth() >= getWidth() - getPaddingRight()) {
            x -= getTextWidth()/2;
        }
        canvas.drawText(showType.getDrawContent(progress), x, mLineArea.top - textPaint.descent(), textPaint);
    }

    private float getTextWidth(){
        return textPaint.measureText(showType.getDrawContent(progress));
    }

    /**
     * 绘制未达背景
     */
    private void drawBg(Canvas canvas) {
        linePaint.setColor(colorUnReachProgress);
        float left = (getWidthWithoutPadding() * progress) + getPaddingLeft();
        if (left == mLineArea.right) {
            return;
        }
        canvas.drawRect(left, mLineArea.top, mLineArea.right, mLineArea.bottom, linePaint);
    }

    /**
     * 绘制临界点
     */
    private void drawPoint(Canvas canvas) {
        float centerX = (getWidthWithoutPadding() * progress) + getPaddingLeft();
        float centerY = mLineArea.height() / 2 + mLineArea.top;
        linePaint.setColor(colorPoint);
        canvas.drawCircle(centerX, centerY, mLineArea.height() / 2 + mLineArea.height() * 0.2f, linePaint);
    }

    /**
     * 绘制进度
     */
    private void drawProgress(Canvas canvas) {
        float right = (getWidthWithoutPadding() * progress) + getPaddingLeft();
        if (right == mLineArea.left) {
            return;
        }
        System.out.println("得到的进度值：" + progress + "，right=" + right + ",end=" + mLineArea.right);
        linePaint.setColor(colorProgress);
        canvas.drawRect(mLineArea.left, mLineArea.top, right, mLineArea.bottom, linePaint);
    }
}
