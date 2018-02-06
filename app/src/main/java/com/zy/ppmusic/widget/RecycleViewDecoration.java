package com.zy.ppmusic.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;

/**
 * RecyclerView Item分割线，横向和纵向
 *
 * @author ZhiTouPC
 */
public class RecycleViewDecoration extends RecyclerView.ItemDecoration {
    private static final String TAG = "RecycleViewDecoration";
    private int mOrientation;
    private Drawable mDivider;
    private int[] padding;

    private RecycleViewDecoration(Context context, int orientation, int dividerDrawable) {
        mDivider = ContextCompat.getDrawable(context, dividerDrawable);
        if (orientation != LinearLayoutManager.HORIZONTAL &&
                orientation != LinearLayoutManager.VERTICAL) {
            Log.e(TAG, "RecycleViewDecoration: orientation is error");
            return;
        }
        mOrientation = orientation;
        padding = new int[4];
    }


    /**
     * padding为可变参数，顺序为left,top,right,bottom
     */
    public RecycleViewDecoration(Context context, int orientation, int dividerDrawable, int... padding) {
        this(context, orientation, dividerDrawable);
        this.padding = padding;
    }

    @Override
    public void onDraw(Canvas c, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == LinearLayoutManager.HORIZONTAL) {
            drawHorizontalLine(c, parent, state);
        } else {
            drawVerticalLine(c, parent, state);
        }
    }

    /**
     * 绘制横向列表的分割线
     */
    private void drawHorizontalLine(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int top = parent.getPaddingTop() + (padding.length > 1 ? padding[1] : 0);
        int bottom = parent.getChildAt(0).getBottom() - (padding.length > 3 ? padding[3] : 0);
        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);

            final int left = child.getRight();
            final int right = left + mDivider.getIntrinsicWidth();
            mDivider.setBounds(left+getPaddingSafe(0), top+getPaddingSafe(1), right+getPaddingSafe(2),
                    bottom+getPaddingSafe(3));
            mDivider.draw(c);
        }
    }

    /**
     * 绘制纵向列表的分割线
     */
    private void drawVerticalLine(Canvas c, RecyclerView parent, RecyclerView.State state) {
        int left = parent.getPaddingLeft() + (padding.length > 0 ? padding[0] : 0);
        int right = parent.getWidth() - parent.getPaddingRight() - (padding.length > 2 ? padding[2] : 0);
        final int childCount = parent.getChildCount();
        for (int i = 0; i < childCount; i++) {
            final View child = parent.getChildAt(i);

            final RecyclerView.LayoutParams params = (RecyclerView.LayoutParams) child.getLayoutParams();
            final int top = params.bottomMargin + child.getBottom();
            final int bottom = top + mDivider.getIntrinsicHeight();
            mDivider.setBounds(left+getPaddingSafe(0), top+getPaddingSafe(1), right+getPaddingSafe(2),
                    bottom+getPaddingSafe(3));
            mDivider.draw(c);
        }
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent, RecyclerView.State state) {
        if (mOrientation == LinearLayoutManager.HORIZONTAL) {
            outRect.set(mDivider.getIntrinsicWidth(), 0, mDivider.getIntrinsicWidth(), 0);
        } else {
            outRect.set(0, mDivider.getIntrinsicHeight(), 0, mDivider.getIntrinsicHeight());
        }
    }

    private int getPaddingSafe(int position) {
        if (position >= padding.length) {
            return 0;
        }
        return padding[position];
    }
}
