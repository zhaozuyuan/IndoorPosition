package com.zzy.common.util

import android.view.View
import kotlin.math.roundToInt

/**
 * create by zuyuan on 2021/3/4
 */


/**
 * FLoat取小数点后几位
 */
fun Float.round(i: Int): Float {
    var mul = 1
    var varI = i
    while (varI > 0) {
        mul *= 10
        --varI
    }
    return (this * mul).roundToInt().toFloat() / mul.toFloat()
}

/**
 * 判断整数
 */
fun String.isAllNumberAndNoNull(): Boolean {
    if (length == 0) {
        return false
    }
    var index = 0
    forEach {
        if (index == 0 && it == '-') {
            return@forEach
        }
        if (it < '0' || it > '9') {
            return false
        }
        index++
    }
    return true
}

/**
 * 防止暴击
 */
fun View.setOnClickListenerSafely(listener: View.OnClickListener, invalidTime: Long = 300L) {
    setOnClickListener(object : View.OnClickListener {
        var preClickTime = System.currentTimeMillis()
        override fun onClick(v: View?) {
            val curTime = System.currentTimeMillis()
            if (curTime - preClickTime > invalidTime) {
                listener.onClick(v)
                preClickTime = curTime
            }
        }
    })
}