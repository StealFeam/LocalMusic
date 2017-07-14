package com.zy.ppmusic.data.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import org.greenrobot.greendao.database.DatabaseOpenHelper;

public class LocalDataHelper extends DatabaseOpenHelper{

    public LocalDataHelper(Context context, String name, int version) {
        super(context, name, version);
    }

    public LocalDataHelper(Context context, String name, SQLiteDatabase.CursorFactory factory, int version) {
        super(context, name, factory, version);
    }


}
