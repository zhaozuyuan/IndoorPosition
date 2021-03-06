package com.zzy.common.bean

import android.os.Parcel
import android.os.Parcelable
import java.io.Serializable

/**
 * create by zuyuan on 2021/2/26
 *
 * 需要上传和下载的rssi数据
 * task:RSSITaskBean
 * 一次扫描产生的数据:RSSIData
 *
 * @param task_name 任务名称，唯一
 * @param scan_count 一个坐标下扫描的次数
 * @param wifi_count 一个坐标下会扫描几个固定的wifi
 *
 * {
        "task_name":"task1",
        "scan_count":6,
        "wifi_count":3,
        "unit_length":100,
        "wifi_tags":[{"ssid":"xxx","bssid:"xxx"}]
        "rssi_data":
        [
            {
            "wifi_ssid":"wifi1",
            "wifi_bssid":"xx:xx:xx",
            "x":1,
            "y":1,
            "levels":[-100,-1]
            }
        ]
    }
 */
data class RSSITaskBean(
    var task_name: String,
    var scan_count: Int,
    var wifi_count: Int,
    var unit_length: Int,
    var wifi_tags: List<WifiTag> = emptyList(),
    var rssi_data: List<RSSIData> = emptyList(),
) : Serializable {
    constructor(scan_count: Int, wifi_count: Int) : this("", scan_count, wifi_count,0)
}

data class RSSIData(
    var wifi_bssid: String,
    var wifi_ssid: String,
    var x: Int,
    var y: Int,
    var levels: List<Int>,
) : Serializable