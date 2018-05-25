package com.zy.ppmusic.utils;

import android.util.Log;

import com.zy.ppmusic.BuildConfig;

import java.util.Locale;

/**
 * @author ZhiTouPC
 * @date 2017/12/27
 */

public class PrintLog {
    private static String mClassName;
    private static String mMethodName;
    private static int mLineNumber;
    private static String mDetailInfo;


    private static void generateInfo(StackTraceElement[] elements) {
        mClassName = elements[1].getClassName();
        mMethodName = elements[1].getMethodName();
        mLineNumber = elements[1].getLineNumber();
        mDetailInfo = String.format(Locale.CHINA, "%s : %s : %s \n", mClassName, mMethodName, mLineNumber);
        System.out.println("--**--");
    }

    public static void print(Object msg) {
        generateInfo(new Throwable().getStackTrace());
        System.out.println(mDetailInfo + msg);
    }

    public static void e(String msg) {
        generateInfo(new Throwable().getStackTrace());
        Log.e(mClassName, buildLogMsg(msg));
    }

    public static void w(String msg) {
        generateInfo(new Throwable().getStackTrace());
        Log.w(mClassName, buildLogMsg(msg));
    }

    public static void i(String msg) {
        generateInfo(new Throwable().getStackTrace());
        Log.i(mClassName, buildLogMsg(msg));
    }

    public static void d(String msg) {
        generateInfo(new Throwable().getStackTrace());
        Log.d(mClassName, buildLogMsg(msg));
    }

    private static String buildLogMsg(String msg) {
        if (BuildConfig.DEBUG) {
            return String.format(Locale.CHINA, "%s -> %d \n %s", mMethodName, mLineNumber, msg == null?"empty":msg);
        } else {
            return "-------------------------------";
        }
    }
}
