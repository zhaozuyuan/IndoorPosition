package com.zzy.common.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * create by zuyuan on 2021/2/10
 * desc: 利用系统自带的融合计步传感器
 */
class SysStepSensorHandler(private val manager: SensorManager)
    : SensorEventListener, ISensorHandler, IStepHandler {

    private val isRegistered = AtomicBoolean(false)

    private var onNewStep: (() -> Unit)? = null

    companion object {
        private const val TAG = "SysStepHandler"
    }

    override fun setCallback(onNewStep: () -> Unit) {
        this.onNewStep= onNewStep
    }

    override fun startListen() {
        val canRegister: Boolean = isRegistered.compareAndSet(false, true)
        if (!canRegister) {
            throw RuntimeException("SysStepSensorHandler has be registered !!!")
        }
        val sensor: Sensor = manager.getDefaultSensor(Sensor.TYPE_STEP_DETECTOR)
        manager.registerListener(this, sensor, SensorManager.SENSOR_DELAY_FASTEST)
    }

    override fun stopListen() {
        if (isRegistered.compareAndSet(true, false)) {
            manager.unregisterListener(this)
        }
    }

    override fun onAccuracyChanged(p0: Sensor?, p1: Int) {

    }

    override fun onSensorChanged(p0: SensorEvent?) {
        Log.d(TAG, "新的一步")
        onNewStep?.invoke()
    }
}