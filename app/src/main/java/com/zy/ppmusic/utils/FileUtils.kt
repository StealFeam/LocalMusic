package com.zy.ppmusic.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.os.storage.StorageVolume
import android.text.TextUtils
import android.util.Log

import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.io.OutputStream
import java.lang.reflect.Array
import java.lang.reflect.Method

/**
 * 1.获取手机目录所有的音乐文件
 * 2.通过首页展示
 * 3.设计首页加载的框架
 *
 * @author ZY
 */

object FileUtils {
    private val TAG = "FileUtils"

    val downloadFile: String
        get() {
            val downloadDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
            return downloadDir.absolutePath
        }

    /**
     * @param isExternalStorage false 获取内置存储路径
     * true  获取外置存储路径
     */
    fun getStoragePath(mContext: Context, isExternalStorage: Boolean): String? {
        val mStorageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
                ?: return null
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val storageVolumes = mStorageManager.storageVolumes
            for (storageVolume in storageVolumes) {
                storageVolume.getDescription(mContext)
            }
        }
        val storageVolumeClazz: Class<*>
        try {
            storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
            val getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
            val getPath = storageVolumeClazz.getMethod("getPath")
            val isRemovable = storageVolumeClazz.getMethod("isRemovable")
            val result = getVolumeList.invoke(mStorageManager)
            val length = Array.getLength(result)
            for (i in 0 until length) {
                val storageVolumeElement = Array.get(result, i)
                val path = getPath.invoke(storageVolumeElement) as String
                val removable = isRemovable.invoke(storageVolumeElement) as Boolean
                if (isExternalStorage == removable) {
                    return path
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        return null
    }

    /**
     * 遍历文件目录下的所有文件
     *
     * @param file 需要扫描的文件目录
     */
    private fun searchFile(file: File) {
        if (file.isDirectory) {
            Log.e(TAG, "" + file.absolutePath)
            val items = file.listFiles()
            for (item in items) {
                searchFile(item)
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
    fun writeObjectToFile(`object`: Any, path: String, name: String) {
        val dir = File(path)
        if (!dir.exists()) {
            val result = dir.mkdirs()
            if (!result) {
                System.err.println("文件目录创建失败")
                return
            }
        }
        val saveFile = File(dir.absolutePath + File.separator + name)
        var objectOutputStream: ObjectOutputStream? = null
        var outputStream: OutputStream? = null
        try {
            outputStream = FileOutputStream(saveFile)
            objectOutputStream = ObjectOutputStream(outputStream)
            objectOutputStream.writeObject(`object`)
            objectOutputStream.flush()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            StreamUtils.closeIo(objectOutputStream!!)
            StreamUtils.closeIo(outputStream!!)
        }
    }

    fun writeStringToFile(str: String, path: String) {
        if (StringUtils.ifEmpty(str) || StringUtils.ifEmpty(path)) {
            return
        }
        val file = File(path)
        if (!file.exists()) {
            try {
                val newFile = file.createNewFile()
                if (!newFile) {
                    println("创建文件失败")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        var ous: BufferedOutputStream? = null
        var fOus: FileOutputStream? = null
        try {
            fOus = FileOutputStream(file)
            ous = BufferedOutputStream(fOus)
            val buffered = str.toByteArray(charset("utf-8"))
            ous.write(buffered, 0, buffered.size)
            ous.flush()
        } catch (e: FileNotFoundException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            StreamUtils.closeIo(ous!!)
            StreamUtils.closeIo(fOus!!)
        }
    }

    /**
     * 从本地读取数据
     *
     * @param path 文件存放的位置
     * @return data
     */
    fun readObjectFromFile(path: String): Any? {
        val saveFile = File(path)
        if (!saveFile.exists()) {
            return null
        }
        var objectInputStream: ObjectInputStream? = null
        var inputStream: InputStream? = null
        try {
            inputStream = FileInputStream(saveFile)
            objectInputStream = ObjectInputStream(inputStream)
            return objectInputStream.readObject()
        } catch (e: IOException) {
            e.printStackTrace()
        } catch (e: ClassNotFoundException) {
            e.printStackTrace()
        } finally {
            StreamUtils.closeIo(objectInputStream!!)
            StreamUtils.closeIo(inputStream!!)
        }
        return null
    }

    fun saveObject(obj: Any, dir: String) {
        val file = File("$dir/cache.obj")
        if (!file.exists()) {
            try {
                val createFileResult = file.createNewFile()
                if (createFileResult) {
                    PrintLog.print("创建缓存文件成功")
                } else {
                    PrintLog.print("创建缓存文件失败")
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }

        }
        var outputStream: ObjectOutputStream? = null
        try {
            outputStream = ObjectOutputStream(FileOutputStream(file))
            outputStream.writeObject(obj)
            outputStream.flush()
            PrintLog.print("write object success")
        } catch (e: IOException) {
            e.printStackTrace()
        } finally {
            StreamUtils.closeIo(outputStream!!)
        }
    }


    fun readObject(dir: String): Any? {
        val file = File("$dir/cache.obj")
        if (!file.exists()) {
            return null
        }
        var inputStream: ObjectInputStream? = null
        try {
            inputStream = ObjectInputStream(FileInputStream(file))
            PrintLog.print("read object success")
            return inputStream.readObject()
        } catch (e: IOException) {
            PrintLog.e("read cache has a error :" + e.message)
            return null
        } catch (e: ClassNotFoundException) {
            PrintLog.e("read cache has a error :" + e.message)
            return null
        } finally {
            StreamUtils.closeIo(inputStream!!)
        }
    }

    fun isExits(path: String?): Boolean {
        return path != null && File(path).exists()
    }

    fun deleteFile(path: String?): Boolean {
        if (TextUtils.isEmpty(path)) {
            System.err.println("删除文件失败，路径为空")
            return false
        }
        val file = File(path)
        if (!file.exists()) {
            System.err.println("删除文件失败，找不到该文件")
            return false
        }
        return file.delete()
    }


}
