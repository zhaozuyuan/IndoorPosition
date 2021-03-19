package com.zzy.common.bean

import java.lang.NullPointerException

/**
 * 储存rssi坐标信息
 */
data class RSSIPointBean(val x: Int, val y: Int, val wifiList: MutableList<WifiBean> = mutableListOf()) {

    fun findWifiBeanNoNull(bssid: String): WifiBean {
        wifiList.forEach {
            if (it.bssid == bssid) {
                return it
            }
        }
        throw NullPointerException()
    }
}