package com.zy.ppmusic.utils

import android.content.Context
import android.content.SharedPreferences
import com.zy.ppmusic.App

/**
 * @author stealfeam
 * @since 2018/9/6
 */
class SpUtils {
    private val spName = "cache_share"
    private val DEFAULT_STRING_VALUE = "default"
    private val DEFAULT_INT_VALUE = 0
    private val DEFAULT_LONG_VALUE = 0L

    private object Holder {
        val instance = SpUtils()
    }

    companion object {
        @JvmStatic
        fun get(): SpUtils {
            return Holder.instance
        }
    }

    private val spInstance: SharedPreferences = App.getAppBaseContext().getSharedPreferences(spName, Context.MODE_PRIVATE)

    fun putString(key: String, value: String) {
        spInstance.edit().apply {
            putString(key, value)
        }.apply()
    }

    fun getString(key: String): String {
        return spInstance.getString(key, DEFAULT_STRING_VALUE)!!
    }

    fun putInt(key: String, value: Int) {
        spInstance.edit().apply {
            putInt(key, value)
        }.apply()
    }

    fun getInt(key: String): Int {
        return spInstance.getInt(key, DEFAULT_INT_VALUE)
    }

    fun putOperator(operator: (SharedPreferences.Editor) -> Unit) {
        spInstance.edit().apply {
            operator.invoke(this)
        }.apply()
    }

    fun getOperator(operator: (SharedPreferences) -> Any): Any {
        return operator.invoke(spInstance)
    }
}