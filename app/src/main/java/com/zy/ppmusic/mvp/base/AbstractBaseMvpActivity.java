package com.zy.ppmusic.mvp.base;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

import com.zy.ppmusic.App;
import com.zy.ppmusic.utils.PrintLog;

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
        App.getInstance().createActivity(this);
        featureBeforeCreate();
        setContentView(getContentViewId());
        mPresenter = createPresenter();
        initViews();
    }

    @Override
    public Resources getResources() {
        return App.getInstance().getResources();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mPresenter != null) {
            mPresenter.detachViewAndModel();
        }
        App.getInstance().destroyActivity(this);
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
