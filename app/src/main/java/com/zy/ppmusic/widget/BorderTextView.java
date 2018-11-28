package com.zy.ppmusic.widget;

import android.content.Context;
import android.graphics.Color;
import android.support.v7.widget.AppCompatTextView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.widget.FrameLayout;

import com.zy.ppmusic.R;

import org.jetbrains.annotations.Nullable;


/**
 * @author stealfeam
 */
public class BorderTextView extends AppCompatTextView implements ViewGroup.OnHierarchyChangeListener {
    private static final String TAG = "BorderTextView";
    private FrameLayout mRootView;
    private boolean isAdded;

    public BorderTextView(Context context) {
        super(context);
    }

    public void show(@Nullable View v, String text) {
        if (mRootView == null) {
            findSuitParent(v);
            mRootView.setOnHierarchyChangeListener(this);
        }
        if (this.getVisibility() != VISIBLE) {
            setVisibility(VISIBLE);
        }
        if (mRootView != null && !isAdded) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = Gravity.END | Gravity.CENTER_VERTICAL;
            mRootView.addView(this, params);

            this.setBackgroundResource(R.drawable.layout_shape_round);
            this.getBackground().setAlpha(80);
            getBackground().setBounds(0, 0, getBackground().getIntrinsicWidth() - 10, getBackground().getIntrinsicHeight());
            this.setTextColor(Color.WHITE);
            this.setPadding(10, 10, 0, 10);
        }
        setText(text);
    }

    public void hide() {
        this.setVisibility(GONE);
    }

    private void findSuitParent(View v) {
        while (v != null) {
            if (v.getId() == android.R.id.content) {
                mRootView = (FrameLayout) v;
                return;
            }
            ViewParent parent = v.getParent();
            v = parent instanceof View ? (View) parent : v;
        }
    }

    @Override
    public void onChildViewAdded(View parent, View child) {
        if (child instanceof BorderTextView) {
            invalidate();
            isAdded = true;
        }
    }

    @Override
    public void onChildViewRemoved(View parent, View child) {
        if (child instanceof BorderTextView) {
            isAdded = false;
        }
    }
}
