package com.zzy.common.sensor

/**
 * create by zuyuan on 2021/2/10
 */
interface IStepHandler: ISensorHandler {
    fun setCallback(onNewStep: () -> Unit)
}