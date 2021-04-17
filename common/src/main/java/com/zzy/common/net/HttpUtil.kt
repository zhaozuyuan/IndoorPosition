package com.zzy.common.net

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.zzy.common.bean.NetResult
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

    private const val BASE_URL_REMOTE = "http://t3780l3454.zicp.vip"
    private const val BASE_URL_LOCAL = "http://192.168.43.163:8080"

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
            .connectTimeout(8, TimeUnit.SECONDS)  //socket建立连接，握手+SSL
            .callTimeout(20, TimeUnit.SECONDS)    //socket连接时长
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .addNetworkInterceptor(HttpLoggingInterceptor())
            .build()
    }

    private val jsonHelper = Gson()

    private var currentUrl = BASE_URL_REMOTE

    private var otherUrl = BASE_URL_LOCAL
        get() {
            if (currentUrl == BASE_URL_REMOTE) {
                currentUrl = BASE_URL_LOCAL
                otherUrl = BASE_URL_REMOTE
            } else {
                currentUrl = BASE_URL_REMOTE
                otherUrl = BASE_URL_LOCAL
            }
            return field
        }

    fun pushRSSITaskData(data: RSSITaskBean): NetResult<Unit> {
        var result = realPushRSSITaskData(data, currentUrl)
        if (result.code == 500) {
            result = realPushRSSITaskData(data, otherUrl)
        }
        return result
    }

    fun getRSSITaskData(taskName: String): NetResult<RSSITaskBean> {
        var result = realGetRSSITaskData(taskName, currentUrl)
        if (result.code == 500) {
            result = realGetRSSITaskData(taskName, otherUrl)
        }
        return result
    }

    fun getAllTaskData(): NetResult<List<RSSITaskBean>> {
        var result = realGetAllTaskData(currentUrl)
        if (result.code == 500) {
            result = realGetAllTaskData(otherUrl)
        }
        return result
    }

    private fun realPushRSSITaskData(data: RSSITaskBean, baseUrl: String): NetResult<Unit> {
        val body = jsonHelper.toJson(data)
                .toRequestBody("application/json;charset=utf-8".toMediaTypeOrNull())
        val request = Request.Builder().url("$baseUrl${Path.PUSH_RSSI_DATA}").post(body).build()
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

    private fun realGetRSSITaskData(taskName: String, baseUrl: String): NetResult<RSSITaskBean> {
        val request = Request.Builder()
                .url("$baseUrl${Path.GET_RSSI_DATA}?${Params.PARAM_TASK_NAME}=$taskName")
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

    private fun realGetAllTaskData(baseUrl: String): NetResult<List<RSSITaskBean>> {
        val request = Request.Builder()
                .url("$baseUrl${Path.GET_ALL_TASK}")
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