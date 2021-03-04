package com.zzy.common.util

import android.widget.Toast

fun toastShort(msg: String) {
    runUIThread {
        Toast.makeText(Global.app, msg, Toast.LENGTH_SHORT).show()
    }
}

fun toastLong(msg: String) {
    runUIThread {
        Toast.makeText(Global.app, msg, Toast.LENGTH_LONG).show()
    }
}