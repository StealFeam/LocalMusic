package com.zy.ppmusic.view;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.support.v7.widget.AppCompatTextView;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.TranslateAnimation;
import android.widget.FrameLayout;

import com.zy.ppmusic.R;

public class EasyTintView extends AppCompatTextView {
    private static final String TAG = "EasyTintView";
    public static final int TINT_LONG = 3000;
    public static final int TINT_SHORT = 1000;
    private Handler mDelayHandler;
    private int delayDuration;
    private int showGravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    private ViewGroup parent;

    public EasyTintView(Context context) {
        super(context);
        mDelayHandler = new Handler();
        setPadding(20, 10, 20, 10);
        setBackgroundResource(R.drawable.normal_tint_shape);
        getBackground().setAlpha(130);
        setTextColor(Color.WHITE);
        setTag(TAG);
    }

    public static EasyTintView makeText(View anchorView, String msg, int duration) {
        ViewGroup parent = findSuitableParent(anchorView);
        View tagView = parent.findViewWithTag(TAG);
        EasyTintView tintView;
        if (tagView != null) {
            tintView = (EasyTintView) tagView;
        } else {
            tintView = new EasyTintView(anchorView.getContext());
        }
        tintView.setText(msg);
        tintView.parent = parent;
        tintView.delayDuration = duration;
        return tintView;
    }

    private void setShowGravity(int gravity) {
        this.showGravity = gravity;
    }

    public void show() {
        showAnim();
    }

    private void showAnim() {
        if (parent == null) {
            Log.e(TAG, "showAnim: parent is null");
            return;
        }
        if (this.getParent() == null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            params.gravity = showGravity;
            parent.addView(this, params);
        } else {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) this.getLayoutParams();
            params.gravity = showGravity;
            this.setLayoutParams(params);
        }
        Animation showAnim = AnimationUtils.loadAnimation(getContext(), R.anim.tint_show_anim);
        showAnim.setDuration(200);
        showAnim.setFillAfter(true);
        this.setAnimation(showAnim);
        showAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {}
            @Override
            public void onAnimationEnd(Animation animation) {
                mDelayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hideAnim();
                    }
                }, delayDuration);
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        showAnim.start();
    }

    private void hideAnim() {
        removeFromParent();
        Animation hideAnim = AnimationUtils.loadAnimation(getContext(), R.anim.tint_hide_anim);
        hideAnim.setDuration(200);
        hideAnim.setFillAfter(true);
        this.setAnimation(hideAnim);
        hideAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }
            @Override
            public void onAnimationEnd(Animation animation) {
                mDelayHandler.removeCallbacksAndMessages(null);
                removeFromParent();
            }
            @Override
            public void onAnimationRepeat(Animation animation) {}
        });
        hideAnim.start();
        Log.e(TAG, "hideAnim: start");
    }

    private void removeFromParent() {
        if (parent != null && this.getParent() != null) {
            parent.removeView(this);
        }
    }

    private static ViewGroup findSuitableParent(View view) {
        ViewGroup fallback = null;
        do {
            if (view instanceof FrameLayout) {
                if (view.getId() == android.R.id.content) {
                    return (ViewGroup) view;
                }
                fallback = (ViewGroup) view;
            }
            if (view != null) {
                ViewParent parent = view.getParent();
                view = parent instanceof View ? (View) parent : null;
            }
        } while (view != null);

        return fallback;
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mDelayHandler.removeCallbacksAndMessages(null);
    }

}
