package com.zzy.lib_combine

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.zzy.common.router.PageContainer
import com.zzy.common.router_api.PageConstant

class LauncherReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        PageContainer.putPageClazz(PageConstant.Positing.NAME,
                PageConstant.Positing.COMBINE_PAGE, CombineActivity::class.java)
    }
}