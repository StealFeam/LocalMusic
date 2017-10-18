package com.zy.ppmusic.data.db;

import android.content.Context;

import com.zy.ppmusic.data.db.dao.DaoMaster;
import com.zy.ppmusic.data.db.dao.DaoSession;
import com.zy.ppmusic.data.db.dao.MusicDbEntityDao;
import com.zy.ppmusic.entity.MusicDbEntity;

import org.greenrobot.greendao.identityscope.IdentityScopeType;

import java.util.List;

public class DBManager {
    private static final String TABLE_NAME = "local_db";
    private DaoMaster mMaster;
    private DaoMaster.DevOpenHelper mOpenHelper;
    private DaoSession mSession;

    private static volatile DBManager manager = new DBManager();

    public static DBManager getInstance() {
        return manager;
    }

    private DBManager() {

    }

    public DBManager initDb(Context context) {
        mOpenHelper = new DaoMaster.DevOpenHelper(context, TABLE_NAME);
        mMaster = new DaoMaster(mOpenHelper.getWritableDb());
        mSession = mMaster.newSession();
        return this;
    }

    public void insetEntity(MusicDbEntity entity) {
        checkSession();
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

    private void checkSession(){
        if (mSession == null) {
            System.err.println("please call initDb first...");
            throw new NullPointerException("please call initDb first...");
        }
    }


}
