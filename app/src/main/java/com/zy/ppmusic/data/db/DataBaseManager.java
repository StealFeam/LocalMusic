package com.zy.ppmusic.data.db;

import android.content.Context;

import com.zy.ppmusic.data.db.dao.DaoMaster;
import com.zy.ppmusic.data.db.dao.DaoSession;
import com.zy.ppmusic.data.db.dao.MusicDbEntityDao;
import com.zy.ppmusic.entity.MusicDbEntity;

import java.util.List;

/**
 * @author stealfeam
 */
public class DataBaseManager {
    private static final String TABLE_NAME = "local_db";
    private static final String TABLE_PATH_LIST = "path_list";
    private DaoMaster.DevOpenHelper mOpenHelper;
    private DaoSession mSession;

    private DataBaseManager() {

    }

    private static class InstanceHolder{
        private static final DataBaseManager INSTANCE = new DataBaseManager();
    }

    public static DataBaseManager getInstance() {
        return InstanceHolder.INSTANCE;
    }

    public DataBaseManager initDb(Context context) {
        if (mOpenHelper == null) {
            mOpenHelper = new DaoMaster.DevOpenHelper(context, TABLE_NAME);
        }
        if (mSession != null) {
            mSession.clear();
            mSession = null;
        }
        mSession = new DaoMaster(mOpenHelper.getWritableDb()).newSession();
        return this;
    }

    public void insetEntity(MusicDbEntity entity) {
        checkSession();
        if(entity == null){
            return;
        }
        MusicDbEntityDao musicDbEntityDao = mSession.getMusicDbEntityDao();
        musicDbEntityDao.insertOrReplace(entity);
    }

    public List<MusicDbEntity> getEntity() {
        checkSession();
        return mSession.loadAll(MusicDbEntity.class);
    }

    public void deleteAll() {
        checkSession();
        MusicDbEntityDao musicDbEntityDao = mSession.getMusicDbEntityDao();
        musicDbEntityDao.deleteAll();
    }

    private void checkSession() {
        if (mSession == null) {
            if(mOpenHelper == null){
                System.err.println("please call initDb first...");
                throw new NullPointerException("please call initDb first...");
            }
            mSession = new DaoMaster(mOpenHelper.getWritableDb()).newSession();
        }
    }

    public void closeConn() {
        if (mOpenHelper == null) {
            return;
        }
        mSession.clear();
        mSession = null;
    }
}
