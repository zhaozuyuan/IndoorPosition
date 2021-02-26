package com.zzy.common.bean

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
 */
data class RSSITaskBean(
    val rssi_data: List<RSSIData>,
    val scan_count: Int,
    val task_name: String,
    val wifi_count: Int
)

data class RSSIData(
    val level: Int,
    val wifi_bassid: String,
    val wifi_ssid: String,
    val x: Int,
    val y: Int
)