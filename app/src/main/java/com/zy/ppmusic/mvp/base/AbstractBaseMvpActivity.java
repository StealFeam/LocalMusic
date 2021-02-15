package com.zy.ppmusic.mvp.base;

import android.os.Bundle;
import android.os.StrictMode;
import android.view.LayoutInflater;
import android.view.View;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.zy.ppmusic.App;
import com.zy.ppmusic.BuildConfig;

/**
 * @author stealfeam
 * @date 2018/1/12
 */
public abstract class AbstractBaseMvpActivity<P extends AbstractBasePresenter> extends AppCompatActivity {
    private static final String TAG = "AbstractBaseMvpActivity";
    protected P mPresenter;
    protected View contentView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        App.setCustomDensity(this);
        featureBeforeCreate();
        contentView = LayoutInflater.from(this).inflate(getContentViewId(), null);
        setContentView(contentView);
        mPresenter = createPresenter();
        initViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
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
