package com.zy.ppmusic.data.db;

import android.content.Context;

import com.zy.ppmusic.data.db.dao.DaoMaster;
import com.zy.ppmusic.data.db.dao.DaoSession;
import com.zy.ppmusic.entity.MusicDbEntity;

import java.util.List;

public class DBManager {
    private static final String TABLE_NAME = "local_db";
    private DaoMaster mMaster;
    private DaoMaster.DevOpenHelper mOpenHelper;
    private DaoSession mSession;

    private static volatile DBManager manager = new DBManager();

    public static DBManager getInstance(){
        return manager;
    }

    private DBManager() {

    }

    public DBManager initDb(Context context){
        mOpenHelper = new DaoMaster.DevOpenHelper(context,TABLE_NAME);
        mMaster = new DaoMaster(mOpenHelper.getWritableDb());
        mSession = mMaster.newSession();
        return this;
    }

    public void insetEntity(MusicDbEntity entity){
        if (mSession == null) {
            System.err.println("please call initDb first...");
            return;
        }
        mSession = mMaster.newSession();
        mSession.insert(entity);
        mSession.clear();
    }

    public List<MusicDbEntity> getEntity(){
        if (mSession == null) {
            System.err.println("please call initDb first...");
            return null;
        }
        return mSession.loadAll(MusicDbEntity.class);
    }

    public void deleteAll(){
        if (mSession == null) {
            System.err.println("please call initDb first...");
            return;
        }
        mSession = mMaster.newSession();
        mSession.deleteAll(MusicDbEntity.class);
    }


}
