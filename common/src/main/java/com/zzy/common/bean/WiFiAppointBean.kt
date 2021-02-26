package com.zzy.common.bean

/**
 * create by zuyuan on 2021/2/21
 * Pair<ssid, bssid>
 */
data class WiFiAppointBean(val wifi_list: List<WifiTag>) {

    companion object {
        const val SP_KEY = "wifi_appoint_list"
    }
}

data class WifiTag(val ssid: String, val bssid: String)