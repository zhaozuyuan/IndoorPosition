package com.zzy.common.util

import android.net.wifi.ScanResult
import android.util.Log
import com.zzy.common.bean.*
import java.lang.RuntimeException
import kotlin.math.roundToInt

/**
 * create by zuyuan on 2021/3/13
 */
object WifiRSSIUtil {

    const val INVALID_LEVEL = -999

    private const val TAG = "rssi_util"

    /**
     * 转换rssi指纹库数据
     */
    fun parseDataOfRSSI(bean: RSSITaskBean): List<RSSIPointBean> {
        //转换数据
        val map = mutableMapOf<String, MutableList<RSSIData>>()
        bean.rssi_data.forEach {
            val xyName = "${it.x}*${it.y}"
            if (map[xyName] == null) {
                map[xyName] = mutableListOf()
            }
            val list = map[xyName]!!
            list.add(it)
        }
        val beans = mutableListOf<RSSIPointBean>()
        map.forEach { entry ->
            val oneBean = entry.value[0]
            val rssiPointBean =
                    RSSIPointBean(oneBean.x, oneBean.y)
            entry.value.forEach { data ->
                var level = 0
                var count = 0
                data.levels.forEach {
                    //抛去无效值
                    if (it != INVALID_LEVEL) {
                        level += it
                        ++count
                    }
                }
                if (count == 0) {
                    return emptyList()
                }
                level = (level.toFloat() / count).roundToInt()
                val wifiBean = WifiBean(data.wifi_ssid, data.wifi_bssid, level)
                rssiPointBean.wifiList.add(wifiBean)
            }
            beans.add(rssiPointBean)
        }
        return beans
    }

    /**
     * 解析扫描结果
     */
    fun parseScanResult(targetWifiList: List<WifiTag>, data: List<List<ScanResult>>): List<List<WifiBean>> {
        val targetResult: MutableList<List<WifiBean>> = mutableListOf()
        data.forEach { list ->
            val onceList = list.filter { result ->
                targetWifiList.contains(WifiTag(result.SSID, result.BSSID))
            }.sortedBy { result ->
                //根据名字简单排个序
                result.SSID
            }.map { WifiBean.toWifiBean(it)
            }.toMutableList()
            //有的wifi没扫描到
            if (onceList.size != targetWifiList.size) {
                val copyTargetWifiList = targetWifiList.toMutableList()
                onceList.forEach {
                    val tag = WifiTag(it.ssid, it.bssid)
                    if (copyTargetWifiList.contains(tag)) {
                        copyTargetWifiList.remove(tag)
                    }
                }
                copyTargetWifiList.forEach {
                    onceList.add(WifiBean(it.ssid, it.bssid, INVALID_LEVEL))
                }
            }
            targetResult.add(onceList)
        }
        return targetResult
    }

    /**
     * 根据RSSI指纹库和扫描结果，得到当前的坐标
     */
    fun getCurrentXY(rssiPointBeans: List<RSSIPointBean>, scanResult: List<WifiBean>): Pair<Float, Float> {
        var minLevel = Int.MAX_VALUE
        var minLevelBean =  RSSIPointBean(0,0)
        rssiPointBeans.forEach { rssiPointBean ->
            if (scanResult.size != rssiPointBean.wifiList.size) {
                throw RuntimeException()
            }
            var level2 = 0
            scanResult.forEach { wifiBean ->
                val findBean = rssiPointBean.findWifiBeanNoNull(wifiBean.bssid)
                val diffLevel = findBean.level - wifiBean.level
                level2 += (diffLevel * diffLevel)
            }
            if (minLevel >= level2) {
                minLevel = level2
                minLevelBean = rssiPointBean
            }
        }
        Log.d(TAG, "cur xy = $minLevelBean")
        return Pair(minLevelBean.x.toFloat(), minLevelBean.y.toFloat())
    }
}