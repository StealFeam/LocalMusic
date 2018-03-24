package com.zy.ppmusic.widget;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.support.v7.widget.AppCompatTextView;
import android.text.Layout;
import android.text.StaticLayout;
import android.util.AttributeSet;
import android.view.Gravity;

/**
 * @author y-slience
 * @date 2018/3/23
 * 纵向的TextView
 * 目前第二列不能实现
 */
public class VerticalTextView extends AppCompatTextView {
    private StaticLayout mDrawLayout;

    public VerticalTextView(Context context) {
        this(context, null);
    }

    public VerticalTextView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public VerticalTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @SuppressLint("DrawAllocation")
    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        float width = getPaint().measureText("字");

        mDrawLayout = new StaticLayout(getText(), getPaint(), (int) width,
                Layout.Alignment.ALIGN_CENTER, 1.0f, 0.0f, false);

        int minDrawHeight = mDrawLayout.getHeight();

        if (widthMode == MeasureSpec.EXACTLY) {
            width = MeasureSpec.getSize(widthMeasureSpec);
        }

        if(heightMode == MeasureSpec.EXACTLY){
            minDrawHeight = MeasureSpec.getSize(heightMeasureSpec);
        }

        setMeasuredDimension(Math.max((int) width,getMeasuredWidth()), Math.max(minDrawHeight,getMeasuredHeight()));
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getGravity() == Gravity.CENTER) {
            int drawHeight = canvas.getHeight();
            int drawWidth = canvas.getWidth();
            int drawLayoutHeight = mDrawLayout.getHeight();
            int drawLayoutWidth = mDrawLayout.getWidth();

            int deltaHeight = drawHeight - drawLayoutHeight;
            int deltaWidth = drawWidth - drawLayoutWidth;

            canvas.translate(deltaWidth / 2, deltaHeight / 2);
        }
        mDrawLayout.draw(canvas);
    }
}
