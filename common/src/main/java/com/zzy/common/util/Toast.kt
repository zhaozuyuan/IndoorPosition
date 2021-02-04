package com.zzy.common.util

import android.widget.Toast

fun toastShort(msg: String) {
    Toast.makeText(Global.app, msg, Toast.LENGTH_SHORT).show()
}

fun toastLong(msg: String) {
    Toast.makeText(Global.app, msg, Toast.LENGTH_LONG).show()
}