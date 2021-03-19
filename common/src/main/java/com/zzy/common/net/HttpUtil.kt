package com.zzy.common.net

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zzy.common.bean.NetResult
import com.zzy.common.bean.RSSIData
import com.zzy.common.bean.RSSITaskBean
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit

/**
 * create by zuyuan on 2021/2/26
 */
object HttpUtil {
    private const val BASE_URL = "http://t3780l3454.zicp.vip"

    object Path {
        const val PUSH_RSSI_DATA = "/pushRSSIData"
        const val GET_RSSI_DATA = "/rssiData"
        const val GET_ALL_TASK = "/allTaskData"
    }

    object Params {
        const val PARAM_TASK_NAME = "taskName"
    }

    private val okClient by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        OkHttpClient.Builder()
            .connectTimeout(10000L, TimeUnit.MILLISECONDS)
            .callTimeout(3000L, TimeUnit.MILLISECONDS)
            .readTimeout(5000L, TimeUnit.MILLISECONDS)
            .writeTimeout(5000L, TimeUnit.MILLISECONDS)
            .addNetworkInterceptor(HttpLoggingInterceptor())
            .build()
    }

    private val jsonHelper = Gson()

    fun pushRSSITaskData(data: RSSITaskBean): NetResult<Unit> {
        val body = jsonHelper.toJson(data)
                .toRequestBody("application/json;charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url("$BASE_URL${Path.PUSH_RSSI_DATA}").post(body).build()
        return try {
            jsonHelper.fromJson<NetResult<Unit>>(
                    okClient.newCall(request).execute().body?.string(),
                    NetResult::class.java
            )
        } catch (e: Exception) {
            e.printStackTrace()
            getNetErrorResult()
        }
    }

    fun getRSSITaskData(taskName: String): NetResult<RSSITaskBean> {
        val request = Request.Builder()
                .url("$BASE_URL${Path.GET_RSSI_DATA}?${Params.PARAM_TASK_NAME}=$taskName")
                .build()
        return try {
            val resultType: Type = object : TypeToken<NetResult<RSSITaskBean>>(){}.type
            jsonHelper.fromJson<NetResult<RSSITaskBean>>(
                    okClient.newCall(request).execute().body?.string(),
                    resultType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            getNetErrorResult()
        }
    }

    fun getAllTaskData(): NetResult<List<RSSITaskBean>> {
        val request = Request.Builder()
            .url("$BASE_URL${Path.GET_ALL_TASK}")
            .build()
        return try {
            val resultType: Type = object : TypeToken<NetResult<List<RSSITaskBean>>>(){}.type
            jsonHelper.fromJson<NetResult<List<RSSITaskBean>>>(
                okClient.newCall(request).execute().body?.string(),
                resultType
            )
        } catch (e: Exception) {
            e.printStackTrace()
            getNetErrorResult()
        }
    }

    private fun <T> getNetErrorResult() : NetResult<T> = NetResult(500, "服务器异常")
}