package com.zzy.common.bean

import android.net.wifi.ScanResult

/**
 * create by zuyuan on 2021/2/26
 */
data class WifiBean(val ssid: String, val bassid: String, val level: Int) {

    companion object {
        fun toWifiBean(result: ScanResult): WifiBean = WifiBean(result.SSID ?: "", result.BSSID, result.level)
    }
}