package com.zy.ppmusic.widget;

import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
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
    private Paint mPaint;
    private Path mPath;
    private int mPaintColor = Color.BLUE;

    public TimBackGroundDrawable() {
        mPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        mPaint.setAntiAlias(true);
        mPaint.setColor(mPaintColor);
        mPaint.setStrokeWidth(1);
        mPath = new Path();
    }

    public void setDrawableColor(int color) {
        this.mPaintColor = color;
        mPaint.setColor(mPaintColor);
        invalidateSelf();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        Rect bounds = getBounds();
        clipPath(canvas, bounds);
        canvas.drawPath(mPath, mPaint);
    }

    private void clipPath(Canvas canvas, Rect area) {
        mPath.rewind();
        mPath.moveTo(area.left, 0);
        mPath.lineTo(area.width(), 0);
        mPath.lineTo(area.width(), area.height() / 3);
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
        if (mPaint.getAlpha() == 255) {
            return PixelFormat.OPAQUE;
        }
        return PixelFormat.TRANSLUCENT;
    }
}
