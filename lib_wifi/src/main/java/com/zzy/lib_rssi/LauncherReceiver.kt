package com.zzy.lib_rssi

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zzy.common.router.PageContainer
import com.zzy.common.router.Router
import com.zzy.common.router_api.PageConstant

class LauncherReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context?, intent: Intent?) {
        PageContainer.putPageClazz(Router.assembleUrl(PageConstant.Wifi.NAME, PageConstant.Wifi
                .WIFI_RSSI_PAGE), RSSICollectActivity::class.java)
        PageContainer.putPageClazz(Router.assembleUrl(PageConstant.Positing.NAME, PageConstant.Positing
            .WIFI_PAGE), WiFiPositionActivity::class.java)
    }
}