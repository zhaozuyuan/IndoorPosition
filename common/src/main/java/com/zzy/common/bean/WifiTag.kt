package com.zzy.common.bean

import java.io.Serializable

class WifiTag(val ssid: String, val bssid: String) : Serializable {

    override fun hashCode(): Int {
        return bssid.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        return other != null && other as WifiTag? != null && other.bssid == this.bssid
    }

    override fun toString(): String {
        return "WifiTag(ssid=$ssid bssid=$bssid)"
    }
}