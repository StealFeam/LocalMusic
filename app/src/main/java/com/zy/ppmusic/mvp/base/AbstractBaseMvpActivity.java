package com.zy.ppmusic.mvp.base;

import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LayerDrawable;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatImageView;
import androidx.appcompat.widget.AppCompatSeekBar;
import androidx.appcompat.widget.AppCompatTextView;
import androidx.core.view.LayoutInflaterCompat;

import com.zy.ppmusic.App;
import com.zy.ppmusic.R;
import com.zy.ppmusic.ViewOpt;
import com.zy.ppmusic.utils.UIUtilsKt;

import java.lang.ref.SoftReference;
import java.util.ArrayList;
import java.util.List;

/**
 * @author stealfeam
 * @date 2018/1/12
 */
public abstract class AbstractBaseMvpActivity<P extends AbstractBasePresenter> extends AppCompatActivity {
    private static final String TAG = "AbstractBaseMvpActivity";
    protected P mPresenter;
    protected View contentView;

    private static final List<SoftReference<View>> availableModifyThemeColorView = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
        View view = ViewOpt.createView(name, context, attrs);
        if (view != null) {
            return view;
        }
        return super.onCreateView(name, context, attrs);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        LayoutInflaterCompat.setFactory2(getLayoutInflater(), new LayoutInflater.Factory2() {
            @Nullable
            @Override
            public View onCreateView(@Nullable View parent, @NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
                View result = getDelegate().createView(parent, name, context, attrs);

                boolean availableModify = result instanceof TextView || result instanceof ImageView;
                if (result == null) {
                    result = createView(name, context, attrs);
                    availableModify = result != null;
                }
                if (availableModify && result.getTag(R.id.ignore_theme_color) == null) {
                    availableModifyThemeColorView.add(new SoftReference<>(result));
                }
                return result;
            }

            @Nullable
            @Override
            public View onCreateView(@NonNull String name, @NonNull Context context, @NonNull AttributeSet attrs) {
                View result = getDelegate().createView(null, name, context, attrs);
                boolean availableModify = result instanceof TextView || result instanceof ImageView;
                if (result == null) {
                    result = createView(name, context, attrs);
                    availableModify = result != null;
                }
                if (availableModify) {
                    availableModifyThemeColorView.add(new SoftReference<>(result));
                }
                return result;
            }

            @Nullable
            private View createView(String name, Context context, AttributeSet attrs) {
                View result = null;
                if ("androidx.appcompat.widget.AppCompatTextView".equals(name)) {
                    result = new AppCompatTextView(context, attrs);
                } else if ("androidx.appcompat.widget.AppCompatImageView".equals(name)) {
                    result = new AppCompatImageView(context, attrs);
                } else if ("androidx.appcompat.widget.AppCompatSeekBar".equals(name)) {
                    result = new AppCompatSeekBar(context, attrs);
                }
                return result;
            }
        });
        super.onCreate(savedInstanceState);
        App.setCustomDensity(this);
        featureBeforeCreate();
        contentView = LayoutInflater.from(this).inflate(getContentViewId(), null);
        setContentView(contentView);
        mPresenter = createPresenter();
        initViews();
    }

    protected void modifyThemeColor(int themeColor) {
        UIUtilsKt.setThemeColor(themeColor);
        int size = availableModifyThemeColorView.size();
        for (int index = size - 1; index >= 0; index --) {
            final View view = availableModifyThemeColorView.get(index).get();
            if (view == null || view.getTag(R.id.ignore_theme_color) != null) {
                availableModifyThemeColorView.remove(index).clear();
                continue;
            }
            if (view instanceof TextView) {
                ((TextView)view).setTextColor(UIUtilsKt.getThemeColor());
            } else if (view instanceof ImageView) {
                if (view instanceof AppCompatImageView) {
                    AppCompatImageView imageView = ((AppCompatImageView) view);
                    if (imageView.getImageTintMode() == PorterDuff.Mode.SRC_IN) {
                        continue;
                    }
                    imageView.setImageTintList(ColorStateList.valueOf(UIUtilsKt.getThemeColor()));
                } else {
                    Drawable drawable = ((ImageView) view).getDrawable();
                    if (drawable != null) {
                        drawable.setTint(UIUtilsKt.getThemeColor());
                    }
                }
            } else if (view instanceof AppCompatSeekBar) {
                AppCompatSeekBar seekBar = ((AppCompatSeekBar)view);
                seekBar.getThumb().setTint(themeColor);
                ((LayerDrawable)seekBar.getProgressDrawable()).getDrawable(2).setTint(themeColor);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        for (SoftReference<View> viewSoftReference : availableModifyThemeColorView) {
            viewSoftReference.clear();
        }
        if (mPresenter != null) {
            mPresenter.detachViewAndModel();
        }
    }

    /**
     * 做些初始化操作
     */
    protected void featureBeforeCreate() {

    }

    /**
     * 初始化view
     */
    protected void initViews() {

    }

    /**
     * 获取主页面的Id
     *
     * @return id
     */
    protected abstract int getContentViewId();


    /**
     * 创建Presenter逻辑代理
     *
     * @return 返回presenter实例
     */
    protected abstract P createPresenter();
}
