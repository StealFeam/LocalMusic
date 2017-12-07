package com.zy.ppmusic.widget;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.AppCompatTextView;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.FrameLayout;

import com.zy.ppmusic.R;

import java.util.Locale;

/**
 * @author ZhiTouPC
 */
public class EasyTintView extends AppCompatTextView {
    private static final String TAG = "EasyTintView";
    public static final int TINT_LONG = 3000;
    public static final int TINT_SHORT = 1000;
    private Handler mDelayHandler;
    private int delayDuration;
    private int showGravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
    private boolean isVisible = false;
    private ViewGroup parent;
    private int topMargin;
    private float[] screenParams;

    public EasyTintView(Context context) {
        super(context);
        mDelayHandler = new Handler();
        setBackgroundColor(ContextCompat.getColor(context, R.color.colorTheme));
        setTextColor(Color.WHITE);
        setGravity(Gravity.CENTER);
        setTag(TAG);
    }

    public static EasyTintView makeText(@NonNull View anchorView, String msg, int duration) {
        ViewGroup parent = findSuitableParent(anchorView);
        View tagView = null;
        int topMargin = 0;
        if (parent != null) {
            tagView = parent.findViewWithTag(TAG);
            Rect rect = new Rect();
            //获取android.R.id.content根布局的内容区域
            parent.getGlobalVisibleRect(rect);
            //有Toolbar和状态栏高度
            int topWithToolBar = rect.top;

            anchorView.getWindowVisibleDisplayFrame(rect);
            //只有状态栏高度
            int topWithTranslateBar = rect.top;
            if (topWithToolBar > 0) {
                topMargin = 0;
            } else {
                topMargin = topWithTranslateBar;
            }
        }
        EasyTintView tintView;
        if (tagView != null) {
            tintView = (EasyTintView) tagView;
        } else {
            tintView = new EasyTintView(anchorView.getContext());
        }
        tintView.setText(String.valueOf(msg));
        tintView.topMargin = topMargin;
        tintView.parent = parent;
        tintView.delayDuration = duration;
        return tintView;
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        int width = getDefaultSize((int) getScreenParams(0), widthMeasureSpec);
        FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) getLayoutParams();
        int defaultHeight = (int) (params.height + 10f * getScreenParams(2));
        int height = getDefaultSize(defaultHeight, heightMeasureSpec);
        height = height > defaultHeight ? height : defaultHeight;
        setMeasuredDimension(width, height);
    }

    private float getScreenParams(int posi) {
        if (posi < 0 || posi > 2) {
            return 0;
        }
        if (screenParams == null) {
            screenParams = new float[3];
            WindowManager manager = (WindowManager) getContext().getSystemService(Context.WINDOW_SERVICE);
            if (manager != null) {
                DisplayMetrics metrics = new DisplayMetrics();
                Display defaultDisplay = manager.getDefaultDisplay();
                defaultDisplay.getMetrics(metrics);
                System.out.println(String.format(Locale.CHINA, "%f,%d,%f",
                        metrics.density, metrics.densityDpi, metrics.scaledDensity));
                screenParams[0] = metrics.widthPixels;
                screenParams[1] = metrics.heightPixels;
                screenParams[2] = metrics.scaledDensity;
            } else {
                return 0;
            }
        }
        return screenParams[posi];
    }

    private void setShowGravity(int gravity) {
        this.showGravity = gravity;
    }

    public void show() {
        if (isVisible) {
            mDelayHandler.removeCallbacksAndMessages(null);
            mDelayHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    hideAnim();
                }
            }, delayDuration);
        } else {
            showAnim();
        }
    }

    private void showAnim() {
        if (parent == null) {
            Log.e(TAG, "showAnim: parent is null");
            return;
        }
        if (isVisible) {
            System.out.println("view is already showing in screen");
            return;
        }

        if (this.getParent() == null) {
            FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT,
                    getResources().getDimensionPixelOffset(R.dimen.dp45));
            params.topMargin = this.topMargin;
            params.gravity = showGravity;
            parent.addView(this, params);
        } else {
            FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) this.getLayoutParams();
            params.gravity = showGravity;
            this.setLayoutParams(params);
        }

        if (this.getVisibility() != View.VISIBLE) {
            this.setVisibility(View.VISIBLE);
        }

        Animation showAnim = AnimationUtils.loadAnimation(getContext(), R.anim.tint_show_anim);
        showAnim.setDuration(300);
        showAnim.setFillAfter(true);
        this.startAnimation(showAnim);
        showAnim.setAnimationListener(new Animation.AnimationListener() {
            @Override
            public void onAnimationStart(Animation animation) {
            }

            @Override
            public void onAnimationEnd(Animation animation) {
                isVisible = true;
                mDelayHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        hideAnim();
                    }
                }, delayDuration);
            }

            @Override
            public void onAnimationRepeat(Animation animation) {
            }
        });
    }

    private void hideAnim() {
        if (isVisible) {
            Animation hideAnim = AnimationUtils.loadAnimation(getContext(), R.anim.tint_hide_anim);
            hideAnim.setDuration(300);
            hideAnim.setFillAfter(true);
            this.startAnimation(hideAnim);
            hideAnim.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    removeFromParent();
                    setVisibility(View.GONE);
                    isVisible = false;
                    mDelayHandler.removeCallbacksAndMessages(null);
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }

    }

    private void removeFromParent() {
        if (parent != null && this.getParent() != null) {
            parent.removeView(this);
        }
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

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mDelayHandler.removeCallbacksAndMessages(null);
    }

}
