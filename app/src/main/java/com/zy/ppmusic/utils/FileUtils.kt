package com.zy.ppmusic.utils

import android.content.Context
import android.os.Build
import android.os.Environment
import android.os.storage.StorageManager
import android.text.TextUtils
import android.util.Log
import com.zy.ppmusic.App
import java.io.*
import java.lang.reflect.Array

/**
 * 1.获取手机目录所有的音乐文件
 * 2.通过首页展示
 * 3.设计首页加载的框架
 *
 * @author stealfeam
 */

object FileUtils {
    private val TAG = "FileUtils"

    val downloadFile: String
        get() {
            val downloadDir = App.appBaseContext.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            return downloadDir?.absolutePath ?: ""
        }

    /**
     * @param isExternalStorage false 获取内置存储路径
     * true  获取外置存储路径
     */
    fun getStoragePath(mContext: Context, isExternalStorage: Boolean): String? {
        val mStorageManager = mContext.getSystemService(Context.STORAGE_SERVICE) as StorageManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            for (volume in mStorageManager.storageVolumes) {
                if (volume.isRemovable == isExternalStorage) {
                    return volume.directory?.absolutePath
                }
            }
        } else {
            val storageVolumeClazz: Class<*>
            try {
                storageVolumeClazz = Class.forName("android.os.storage.StorageVolume")
                val getVolumeList = mStorageManager.javaClass.getMethod("getVolumeList")
                val getPath = storageVolumeClazz.getMethod("getPath")
                val isRemovable = storageVolumeClazz.getMethod("isRemovable")
                val result = getVolumeList.invoke(mStorageManager) ?: Any()
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

    fun saveObject(obj: Any?, path: String) {
        val file = File(path)
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

    fun readObject(path: String): Any? {
        val file = File(path)
        PrintLog.e("缓存文件的路径----$path")
        if (!file.exists()) {
            PrintLog.e("该文件不存在")
            return null
        }
        var inputStream: ObjectInputStream? = null
        return try {
            inputStream = ObjectInputStream(FileInputStream(file))
            PrintLog.d("read object success")
            inputStream.readObject()
        } catch (e: IOException) {
            PrintLog.e("read cache has a error :" + e.message)
            null
        } catch (e: ClassNotFoundException) {
            PrintLog.e("read cache has a error :" + e.message)
            null
        } finally {
            StreamUtils.closeIo(inputStream!!)
        }
    }

    fun getPathFile(path: String): File {
        return createFileIfNotExits(path)
    }

    private fun createFileIfNotExits(path: String): File {
        if (!isExits(path)) {
            try {
                val result = File(path)
                if (result.createNewFile()) {
                    PrintLog.print("创建缓存文件成功")
                } else {
                    PrintLog.print("创建缓存文件失败")
                }
                return result
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
        return File("")
    }

    fun isExits(path: String?): Boolean {
        return path != null && File(path).exists()
    }

    fun deleteFile(path: String?): Boolean {
        if (path.isNullOrEmpty()) {
            System.err.println("删除文件失败，路径为空")
            return false
        }
        File(path).apply {
            if (this.exists()) {
                return this.delete()
            }
        }
        return false
    }
}
