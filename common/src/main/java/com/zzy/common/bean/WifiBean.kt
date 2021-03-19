package com.zzy.common.bean

import android.net.wifi.ScanResult

/**
 * create by zuyuan on 2021/2/26
 *
 * 处理业务逻辑时的中间转换bean
 */
open class WifiBean(val ssid: String, val bssid: String, val level: Int) {

    companion object {
        fun toWifiBean(result: ScanResult): WifiBean = WifiBean(result.SSID ?: "", result.BSSID, result.level)
    }

    override fun toString(): String {
        return "(ssid:$ssid,level:$level)"
    }
}