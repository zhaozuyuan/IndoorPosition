package com.zzy.common.util

import android.app.Application

object Global {

    lateinit var app: Application
        private set

    fun init(app: Application) {
        this.app = app
    }
}