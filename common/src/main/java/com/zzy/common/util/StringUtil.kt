package com.zzy.common.util

/**
 * create by zuyuan on 2021/3/4
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