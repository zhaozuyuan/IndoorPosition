package com.zzy.common.router

import android.app.Activity
import androidx.annotation.MainThread

object PageContainer {

    private val map = mutableMapOf<String, Class<*>>()

    fun getPageClazzNoNull(url: String) : Class<*> {
        return map[url]!!
    }

    @MainThread
    fun putPageClazz(module: String, page: String, clazz: Class<*>) {
        putPageClazz(Router.assembleUrl(module, page), clazz)
    }

    @MainThread
    fun putPageClazz(url: String, clazz: Class<*>) {
        if (Activity::class.java.isAssignableFrom(clazz)) {
            if (map[url] != null) {
                throw RuntimeException("can't not add repeat.")
            } else {
                map[url] = clazz
            }
        }
    }
}