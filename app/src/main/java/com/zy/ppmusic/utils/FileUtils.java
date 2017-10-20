package com.zy.ppmusic.utils;

import android.content.Context;
import android.os.storage.StorageManager;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.lang.reflect.Array;
import java.lang.reflect.Method;

/**
 * 1.获取手机目录所有的音乐文件
 * 2.通过首页展示
 * 3.设计首页加载的框架
 * @author ZY
 */

public class FileUtils {
    private static final String TAG = "FileUtils";

    /**
     * @param isExternalStorage false 获取内置存储路径
     *                          true  获取外置存储路径
     */
    public static String getStoragePath(Context mContext, boolean isExternalStorage) {
        StorageManager mStorageManager = (StorageManager) mContext.getSystemService(Context.STORAGE_SERVICE);
        Class<?> storageVolumeClazz;
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume");
            Method getVolumeList = mStorageManager.getClass().getMethod("getVolumeList");
            Method getPath = storageVolumeClazz.getMethod("getPath");
            Method isRemovable = storageVolumeClazz.getMethod("isRemovable");
            Object result = getVolumeList.invoke(mStorageManager);
            final int length = Array.getLength(result);
            for (int i = 0; i < length; i++) {
                Object storageVolumeElement = Array.get(result, i);
                String path = (String) getPath.invoke(storageVolumeElement);
                boolean removable = (Boolean) isRemovable.invoke(storageVolumeElement);
                if (isExternalStorage == removable) {
                    return path;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 遍历文件目录下的所有文件
     *
     * @param file 需要扫描的文件目录
     */
    private static void searchFile(File file) {
        if (file.isDirectory()) {
            Log.e(TAG, "" + file.getAbsolutePath());
            File[] items = file.listFiles();
            for (File item : items) {
                searchFile(item);
            }
        }
    }

    /**
     * 存储对象到本地
     *
     * @param object 需要存储的数据对象
     * @param path   存储文件目录
     * @param name   存储文件名称
     */
    public static void writeDataToFile(Object object, String path, String name) {
        File dir = new File(path);
        File saveFile = null;
        if (!dir.exists()) {
            boolean result = dir.mkdirs();
            if (!result) {
                System.err.println("文件目录创建失败");
                return;
            }
        }
        saveFile = new File(dir.getAbsolutePath() + File.separator + name);
        ObjectOutputStream objectOutputStream = null;
        OutputStream outputStream = null;
        try {
            outputStream = new FileOutputStream(saveFile);
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeObject(object);
            objectOutputStream.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            StreamUtils.closeIo(objectOutputStream);
            StreamUtils.closeIo(outputStream);
        }
    }

    /**
     * 从本地读取数据
     *
     * @param path 文件存放的位置
     * @return data
     */
    public static Object readDataFromFile(String path) {
        File saveFile = new File(path);
        if (!saveFile.exists()) {
            return null;
        }
        ObjectInputStream objectInputStream = null;
        InputStream inputStream = null;
        try {
            inputStream = new FileInputStream(saveFile);
            objectInputStream = new ObjectInputStream(inputStream);
            return objectInputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            StreamUtils.closeIo(objectInputStream);
            StreamUtils.closeIo(inputStream);
        }
        return null;
    }

    public static void saveObject(Object obj,String dir){
        File file = new File(dir+"/cache.obj");
        if(!file.exists()){
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        ObjectOutputStream outputStream = null;
        try {
            outputStream = new ObjectOutputStream(new FileOutputStream(file));
            outputStream.writeObject(obj);
            outputStream.flush();
            System.out.println("write object success");
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            StreamUtils.closeIo(outputStream);
        }
    }


    public static Object readObject(String dir){
        File file = new File(dir+"/cache.obj");
        if(!file.exists()){
            return null;
        }
        ObjectInputStream inputStream = null;
        try {
            inputStream = new ObjectInputStream(new FileInputStream(file));
            System.out.println("read object success");
            return inputStream.readObject();
        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            StreamUtils.closeIo(inputStream);
        }
        return null;
    }


}
