package com.zzy.common.util

/**
 * create by zuyuan on 2021/3/4
 */

fun String.isAllNumberAndNoNull(): Boolean {
    if (length == 0) {
        return false
    }
    forEach {
        if (it < '0' || it > '9') {
            return false
        }
    }
    return true
}