package com.zzy.common.bean

/**
 * create by zuyuan on 2021/3/3
 */
data class NetResult<T> (val code: Int, val msg: String, val data: T? = null) {

    companion object {
        const val SUCCESS_CODE = 200
    }
}