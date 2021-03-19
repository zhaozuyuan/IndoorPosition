package com.zzy.common.bean

import java.io.Serializable

/**
 * create by zuyuan on 2021/2/21
 * Pair<ssid, bssid>
 * 记录本地指定的几个wifi
 */

data class WiFiAppointBean(val wifi_list: List<WifiTag>) : Serializable