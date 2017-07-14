package com.zy.ppmusic.data.db;

import android.database.sqlite.SQLiteDatabase;

public class SqlBuilder {
    private SQLiteDatabase mDatabase;

    public SqlBuilder(SQLiteDatabase mDatabase) {
        this.mDatabase = mDatabase;
    }

}
