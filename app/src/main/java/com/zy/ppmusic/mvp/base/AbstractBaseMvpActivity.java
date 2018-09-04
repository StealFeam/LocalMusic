package com.zy.ppmusic.mvp.base;

import android.os.Bundle;
import android.os.StrictMode;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.zy.ppmusic.App;
import com.zy.ppmusic.BuildConfig;

/**
 * @author ZhiTouPC
 * @date 2018/1/12
 */
public abstract class AbstractBaseMvpActivity<P extends AbstractBasePresenter> extends AppCompatActivity {
    private static final String TAG = "AbstractBaseMvpActivity";
    protected P mPresenter;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (BuildConfig.IS_DEBUG) {
            StrictMode.setThreadPolicy(new StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .detectCustomSlowCalls()
                    .permitAll()
                    .penaltyDeath()
                    .penaltyDialog()
                    .penaltyLog()
                    .build());
            StrictMode.setVmPolicy(new StrictMode.VmPolicy.Builder()
                    .detectLeakedSqlLiteObjects()
                    .detectLeakedClosableObjects()
                    .detectAll()
                    .penaltyLog()
                    .penaltyDeath()
                    .build());
        }
        App.setCustomDensity(this);
        featureBeforeCreate();
        setContentView(getContentViewId());
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
