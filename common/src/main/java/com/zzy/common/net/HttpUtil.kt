package com.zzy.common.net

import okhttp3.*
import java.lang.Exception

/**
 * create by zuyuan on 2021/2/26
 */
object HttpUtil {
    private const val BASE_URL = "http://t3780l3454.zicp.vip"

    private const val TEST = "/text"

    private val okClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OkHttpClient.Builder().addInterceptor(HttpLoggingInterceptor()).build()
    }

    fun testRequest(): String? {
        val request = getNormalRequest(TEST)
        return try {
            okClient.newCall(request).execute().body.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    private fun getNormalRequest(path: String): Request {
        return Request.Builder().url("$BASE_URL$path").build()
    }
}