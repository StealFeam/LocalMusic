package com.zy.ppmusic

import dalvik.system.PathClassLoader
import java.lang.Exception

class InjectClassLoader(dexPath: String, parentClassLoader: ClassLoader) : PathClassLoader(dexPath, parentClassLoader) {

    companion object {
        fun hook(classLoader: ClassLoader) {
            try {
                val loader = InjectClassLoader("", classLoader.parent)
                val field = ClassLoader::class.java.getDeclaredField("parent")
                field.isAccessible = true
                field.set(classLoader, loader)
                Thread.currentThread().contextClassLoader = loader
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun findClass(name: String?): Class<*> {
        // print class name
        return super.findClass(name)
    }
}
