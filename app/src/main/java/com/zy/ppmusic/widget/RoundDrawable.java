package com.zy.ppmusic.widget;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * @author stealfeam
 * @date 2018/1/15
 */
public class RoundDrawable extends Drawable {
    private Paint mPaint;
    private int mRadius;

    public RoundDrawable(int mRadius, int color) {
        this.mRadius = mRadius;
        initPaint();
        this.mPaint.setColor(color);
    }

    private void initPaint() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
    }

    public void setRadius(int mRadius) {
        this.mRadius = mRadius;
        invalidateSelf();
    }

    public void setRoundColor(int mRoundColor) {
        mPaint.setColor(mRoundColor);
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        int height = bounds.bottom - bounds.top;
        int width = bounds.right - bounds.left;
        canvas.drawCircle(width / 2, height / 2, mRadius, mPaint);
    }

    @Override
    public void setAlpha(int alpha) {
        this.mPaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        this.mPaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        int maxAlpha = 255;
        if (mPaint.getAlpha() == maxAlpha) {
            return PixelFormat.OPAQUE;
        }
        return PixelFormat.TRANSLUCENT;
    }
}
