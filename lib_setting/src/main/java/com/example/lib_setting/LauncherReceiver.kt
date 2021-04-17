package com.example.lib_setting

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zzy.common.router.PageContainer
import com.zzy.common.router_api.PageConstant

class LauncherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        PageContainer.putPageClazz(PageConstant.Setting.NAME,
                PageConstant.Setting.SETTING_HOME_PAGE, SettingActivity::class.java)
        PageContainer.putPageClazz(PageConstant.Setting.NAME,
            PageConstant.Setting.WIFI_APPOINT_PAGE, WiFiAppointActivity::class.java)
        PageContainer.putPageClazz(PageConstant.Setting.NAME,
            PageConstant.Setting.PDR_DIRECTION_PAGE, PDRDirectionActivity::class.java)
    }
}