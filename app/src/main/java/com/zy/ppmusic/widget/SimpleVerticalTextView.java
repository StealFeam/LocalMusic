package com.zy.ppmusic.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import androidx.collection.SimpleArrayMap;
import androidx.appcompat.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.widget.TextView;

/**
 * @author stealfeam
 * @date 2018/3/29
 */
public class SimpleVerticalTextView extends AppCompatTextView {
    private static final String TAG = "SimpleVerticalTextView";
    private char[] mTexts;
    private int mCharWidth;
    private int mCharHeight;
    private SimpleArrayMap<Integer, Integer> mColumnHeightMap;
    /**
     * 为了实现横向居中
     */
    private int mPaddingLeft;

    public SimpleVerticalTextView(Context context) {
        this(context, null);
    }

    public SimpleVerticalTextView(Context context, AttributeSet attrs) {
        this(context, attrs, -1);
    }

    public SimpleVerticalTextView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        getPaint().setAntiAlias(true);
    }

    @Override
    public void setText(CharSequence text, TextView.BufferType type) {
        super.setText(text, type);
        mTexts = text.toString().toCharArray();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        Paint.FontMetricsInt metrics = getPaint().getFontMetricsInt();
        //descent为正，ascent为负，leading为声调高度，没有则为0
        mCharHeight = metrics.descent - metrics.ascent + metrics.leading;
        for (char mText : mTexts) {
            float charMeasureWidth = getPaint().measureText(String.valueOf(mText));
            if (mCharWidth < charMeasureWidth) {
                mCharWidth = (int) charMeasureWidth;
            }
        }
        //如果高度为精确模式，按照指定高度，否则按照lines来确定高度
        int textHeight = 0;
        //以当前高度计算需要多少列，以增加宽度
        int columnCount;
        int measureHeightMode = MeasureSpec.getMode(heightMeasureSpec);
        if (measureHeightMode == MeasureSpec.EXACTLY) {
            textHeight += MeasureSpec.getSize(heightMeasureSpec);
        } else {
            int line = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
                line = getMinLines();
            }
            line = Math.max(1, line);
            textHeight += line * mCharHeight + getPaddingTop() + getPaddingBottom();
        }
        columnCount = getColumnCountByHeight(textHeight);
        int finalMeasureWidth = mCharWidth * columnCount + getPaddingLeft() + getPaddingRight();
        int finalMeasureHeight = textHeight;
        if (MeasureSpec.getMode(widthMeasureSpec) == MeasureSpec.EXACTLY) {
            setMeasuredDimension(MeasureSpec.getSize(widthMeasureSpec), Math.max(finalMeasureHeight,
                    getMeasuredHeight()));
        } else {
            setMeasuredDimension(Math.max(finalMeasureWidth, getMeasuredWidth()),
                    Math.max(finalMeasureHeight, getMeasuredHeight()));
        }
        mPaddingLeft = Math.max(0, (getMeasuredWidth() - finalMeasureWidth) / 2);
    }

    /**
     * 根据测量出来的宽度和高度计算出需要多少列
     *
     * @param textHeight 当前最大的高度，当大于这个高度需要增加宽度
     * @return 计算出来的列的数量
     */
    private int getColumnCountByHeight(int textHeight) {
        int currentHeight = getPaddingTop();
        int columnCount = 1;
        if (mColumnHeightMap == null) {
            mColumnHeightMap = new SimpleArrayMap<>();
        } else {
            mColumnHeightMap.clear();
        }
        for (char mText : mTexts) {
            currentHeight += mCharHeight;
            mColumnHeightMap.put(columnCount, currentHeight);
            if (currentHeight + getPaddingBottom() >= textHeight) {
                currentHeight = getPaddingTop() + mCharHeight;
                columnCount++;
            }
        }
        return columnCount;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        int y = getPaddingTop();
        int x = getPaddingLeft() + mPaddingLeft;
        Paint.FontMetricsInt fontMetricsInt = getPaint().getFontMetricsInt();
        int padding = fontMetricsInt.ascent - fontMetricsInt.top;
        int currentColumn = 1;
        int topMargin;
        //计算高度差，达到垂直居中
        if (mColumnHeightMap.get(currentColumn) != null && mColumnHeightMap.get(currentColumn) < getMeasuredHeight()) {
            topMargin = Math.max((getMeasuredHeight() - mColumnHeightMap.get(currentColumn)) / 2, 0);
            y += topMargin;
        }
        for (char itemText : mTexts) {
            y += mCharHeight;
            if (y + getPaddingBottom() > getMeasuredHeight()) {
                y = getPaddingTop() + mCharHeight;
                x += mCharWidth;
                currentColumn++;
                if (mColumnHeightMap.get(currentColumn) < getMeasuredHeight()) {
                    topMargin = Math.max((getMeasuredHeight() - mColumnHeightMap.get(currentColumn)) / 2, 0);
                    y += topMargin;
                }
            }
            canvas.save();
            canvas.translate(x, (y - padding));
            canvas.drawText(String.valueOf(itemText), 0, 0, getPaint());
            canvas.restore();
        }
    }
}
