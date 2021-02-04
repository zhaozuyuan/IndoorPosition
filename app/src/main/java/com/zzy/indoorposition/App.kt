package com.zzy.indoorposition

import android.app.Application
import android.content.Intent
import com.zzy.common.util.Global

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        Global.init(this)
        sendBroadcast(Intent().setAction("com.zzy.receiver.LAUNCHER").setPackage(packageName))
    }
}