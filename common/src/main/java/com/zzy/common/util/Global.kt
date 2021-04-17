package com.zzy.common.util

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity

object Global {

    lateinit var app: Application
        private set

    private var curActivity: AppCompatActivity? = null

    const val APP_DIR_NAME = "IndoorPosition"

    fun init(app: Application) {
        this.app = app
        app.registerActivityLifecycleCallbacks(object : Application.ActivityLifecycleCallbacks {
            override fun onActivityPaused(p0: Activity) {}

            override fun onActivityStarted(p0: Activity) {}

            override fun onActivityDestroyed(p0: Activity) {
                curActivity = p0 as? AppCompatActivity
            }

            override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {}

            override fun onActivityStopped(p0: Activity) {}

            override fun onActivityCreated(p0: Activity, p1: Bundle?) {
                curActivity = p0 as? AppCompatActivity
            }

            override fun onActivityResumed(p0: Activity) {}
        })
    }

    fun curActivity() : AppCompatActivity? = curActivity
}