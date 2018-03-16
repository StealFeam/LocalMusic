package com.zy.ppmusic.widget;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.annotation.IntRange;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

/**
 * @author ZhiTouPC
 */
public class TimBackGroundDrawable extends Drawable {
    public static final int TOP = 1;
    public static final int MIDDLE = 2;
    public static final int BOTTOM = 3;

    /**
     * 尖角在左侧
     */
    public static final int LEFT = 1001;
    /**
     * 尖角在右侧
     */
    public static final int RIGHT = 1002;

    private Paint mPaint;
    private Path mPath;
    private int mPaintColor = Color.BLUE;
    private int mLinePercent = MIDDLE;
    private int mCorner = RIGHT;

    public TimBackGroundDrawable() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        mPaint.setColor(mPaintColor);
        mPaint.setStrokeWidth(1);
        mPath = new Path();
    }

    public void setDrawableColor(int color) {
        this.mPaintColor = color;
        this.mPaint.setColor(mPaintColor);
        invalidateSelf();
    }

    public void setPercent(int mode) {
        this.mLinePercent = mode;
        invalidateSelf();
    }

    public void setCorner(int corner){
        this.mCorner = corner;
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        if(mCorner == RIGHT){
            clipRightPath(canvas, bounds);
        }else{
            clipLeftPath(canvas,bounds);
        }
        canvas.drawPath(mPath, mPaint);
    }

    private void clipRightPath(Canvas canvas, Rect area) {
        mPath.rewind();
        mPath.moveTo(area.left, 0);
        mPath.lineTo(area.width(), 0);
        switch (mLinePercent) {
            case TOP:
                mPath.lineTo(area.width(), 0);
                break;
            case MIDDLE:
                mPath.lineTo(area.width(), area.height() / 2);
                break;
            case BOTTOM:
                mPath.lineTo(area.width(), area.height());
                break;
            default:
                mPath.lineTo(area.width(), area.height() / 3);
                break;
        }

        mPath.lineTo(0, area.height());
        mPath.close();
        canvas.clipPath(mPath);
    }

    private void clipLeftPath(Canvas canvas, Rect area) {
        mPath.rewind();
        switch (mLinePercent) {
            case TOP:
                mPath.moveTo(area.left, area.height()*3/5);
                break;
            case MIDDLE:
                mPath.moveTo(area.left,area.height()/2);
                break;
            case BOTTOM:
                mPath.moveTo(area.left,area.height());
                break;
            default:
                break;
        }
        mPath.lineTo(area.width(),0);
        mPath.lineTo(area.width(),area.height());
        mPath.lineTo(0, area.height());
        mPath.close();
        canvas.clipPath(mPath);
    }

    @Override
    public void setAlpha(@IntRange(from = 0, to = 255) int alpha) {
        mPaint.setAlpha(alpha);
        invalidateSelf();
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        mPaint.setColorFilter(colorFilter);
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
