package com.zy.ppmusic;

import android.content.Context;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import com.zy.viewopt.IViewCreator;
import com.zy.viewopt.ViewOptHost;

@ViewOptHost
public class ViewOpt {
    private static volatile IViewCreator sIViewCreator;

    static {
        try {
            String ifsName = ViewOpt.class.getName();
            String proxyClassName = String.format("%s__ViewCreator__Proxy", ifsName);
            Class proxyClass = Class.forName(proxyClassName);
            Object proxyInstance = proxyClass.newInstance();
            if (proxyInstance instanceof IViewCreator) {
                sIViewCreator = (IViewCreator) proxyInstance;
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }

    public static View createView(String name, Context context, AttributeSet attrs) {
        try {
            if (sIViewCreator != null) {
                View view = sIViewCreator.createView(name, context, attrs);
                if (view != null) {
                    Log.d("Opt", name + " 拦截生成");
                }
                return view;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
