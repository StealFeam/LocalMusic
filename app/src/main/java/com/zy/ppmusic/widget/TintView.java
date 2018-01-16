package com.zy.ppmusic.widget;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;

import com.zy.ppmusic.R;

import org.jetbrains.annotations.NotNull;

/**
 * @author ZhiTouPC
 * @date 2017/12/5
 */
public class TintView extends AppCompatTextView {
    public static final int TINT_LONG = 3000;
    public static final int TINT_MIDDLE = 1500;
    public static final int TINT_SHORT = 500;
    private static final String TAG = "TintView";
    private int showDuration = TINT_SHORT;
    private int showGravity;
    private Handler mDelayHandler;
    private ViewGroup parent;

    public TintView(Context context) {
        this(context, null);
    }

    public TintView(Context context, AttributeSet attrs) {
        super(context, attrs);
        mDelayHandler = new Handler();
        setBackgroundDrawable(ContextCompat.getDrawable(context, R.drawable.tint_shape));
        setTextColor(Color.WHITE);
    }

    public static TintView makeView(@NonNull View anchorView, String msg) {
        ViewGroup parent = findSuitableParent(anchorView);
        View tagView = null;
        if (parent != null) {
            tagView = parent.findViewWithTag(TAG);
        }
        TintView tintView;
        if (tagView != null) {
            tintView = (TintView) tagView;
        } else {
            tintView = new TintView(anchorView.getContext());
            tintView.setTag(TAG);
        }
        tintView.parent = parent;
        tintView.setText(msg);
        return tintView;
    }

    public static TintView findView(@NotNull View anchorView) {
        ViewGroup parent = findSuitableParent(anchorView);
        TintView view = null;
        if (parent != null) {
            view = parent.findViewWithTag(TAG);
        }
        if (view == null) {
            return new TintView(anchorView.getContext());
        }
        return view;
    }

    private static ViewGroup findSuitableParent(View view) {
        do {
            if (view instanceof FrameLayout) {
                if (view.getId() == android.R.id.content) {
                    return (ViewGroup) view;
                }
            }
            if (view != null) {
                ViewParent parent = view.getParent();
                view = parent instanceof View ? (View) parent : null;
            }
        } while (view != null);
        return null;
    }

    public TintView showGravity(int gravity) {
        this.showGravity = gravity;
        setGravity(showGravity);
        return this;
    }

    public void show(int duration) {
        showDuration = duration;
        if (parent != null && getParent() == null) {
            parent.addView(this, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT, showGravity));
        }
    }

    public void hide() {
        Animation animation = getAnimation();
        if (animation != null) {
            animation.cancel();
        }
        animation = new AlphaAnimation(1, 0);
        animation.setDuration(showDuration);
        animation.setFillAfter(true);
        this.startAnimation(animation);
        animation.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {

            }

            @Override
            public void onAnimationEnd(Animation animation) {
                setVisibility(GONE);
                if (parent != null) {
                    parent.removeView(TintView.this);
                }
            }

            @Override
            public void onAnimationRepeat(Animation animation) {

            }
        });
    }
}
