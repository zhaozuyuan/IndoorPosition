package com.zzy.common.util

import android.content.Context
import com.google.gson.Gson
import java.lang.Exception

/**
 * create by zuyuan on 2021/2/20
 */
object SPUtil {

    private const val SP_NAME = "my_sp"

    private val spObj by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Global.app.getSharedPreferences(SP_NAME, Context.MODE_PRIVATE)
    }

    private val jsonHandler by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        Gson()
    }

    fun putJsonString(key: String, value: Any) {
        val str = if (value is String && value.isEmpty()) {
            value
        } else {
            jsonHandler.toJson(value)
        }
        val editor = spObj.edit()
        editor.putString(key, str)
        editor.apply()
    }

    fun <T> readJsonObj(key: String, clazz: Class<T>): T? {
        val str = spObj.getString(key, "")
        if (str.isNullOrEmpty()) {
            return null
        }
        //强报错
        return try {
            jsonHandler.fromJson(str, clazz)
        } catch (e: Exception) {
            putJsonString(key, "")
            throw e
        }
    }
}