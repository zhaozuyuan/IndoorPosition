package com.zzy.common.sensor

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import androidx.annotation.MainThread
import java.lang.RuntimeException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlin.math.asin

/**
 * create by zuyuan on 2021/2/4
 * desc: 监听和处理方向的逻辑类
 */
class RotationSensorHandler(private val manager: SensorManager) : SensorEventListener,ISensorHandler {

    private val isRegistered = AtomicBoolean(false)

    @Volatile
    private var callback: ((FloatArray) -> Unit) ?= null

    //加速度传感器数据
    private var accelerometerValues = FloatArray(3) { 0f }
    //磁场传感器数据
    private var magneticFieldValues  = FloatArray(3) { 0f }

    private var changedCount = 0

    companion object {
        private const val TAG = "rotationHandler"
    }

    /**
     * 虽然是向量，但是场景较简单，就把模看作1即可，那么 z=sin(O/2)
     * @param callback floatArray: x,y,z
     *
     *
     *
     * .... 哎，最后还是采用了过时的TYPE_ORIENTATION，结果就是0 ~ 360
     */
    fun setCallback(callback: (FloatArray) -> Unit) {
        this.callback = callback
    }

    override fun startListen() {
        val canRegister: Boolean = isRegistered.compareAndSet(false, true)
        if (!canRegister) {
            throw RuntimeException("SensorEventListener has be registered !!!")
        }
        //地磁传感器
//        val magneticSensor: Sensor = manager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        //加速度传感器
//        val accelerometerSensor : Sensor = manager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
//        manager.registerListener(this, magneticSensor, SensorManager.SENSOR_DELAY_NORMAL)
//        manager.registerListener(this, accelerometerSensor, SensorManager.SENSOR_DELAY_NORMAL)
        manager.registerListener(this, manager.getDefaultSensor(Sensor.TYPE_ORIENTATION), SensorManager.SENSOR_DELAY_NORMAL)
    }


    override fun stopListen() {
        if (isRegistered.compareAndSet(true, false)) {
            manager.unregisterListener(this)
        }
    }

    override fun onAccuracyChanged(p0: Sensor, p1: Int) {
        Log.i(TAG, "精度改变 value=$p1")
    }

    @MainThread
    override fun onSensorChanged(event: SensorEvent) {
//        if (event.sensor.type == Sensor.TYPE_ACCELEROMETER) {
//            accelerometerValues = event.values
//            ++changedCount
//        } else if (event.sensor.type == Sensor.TYPE_MAGNETIC_FIELD) {
//            magneticFieldValues = event.values
//            ++changedCount
//        }
//        if (changedCount == 2) {
//            changedCount = 0
//            calculateOrientation()
//        }
        Log.i(TAG, "绕Z=${event.values[0]}")
        callback?.invoke(event.values)
    }

    private fun calculateOrientation() {
        val values = FloatArray(3) { 0f }
        val r = FloatArray(9){ 0f }
        //得到旋转矩阵r
        SensorManager.getRotationMatrix(r, null, accelerometerValues, magneticFieldValues)
        //得到设备的方向
        SensorManager.getOrientation(r, values)
        //   0 -> 1 -> -1 -> 0
        Log.i(TAG, "围绕z旋转=${values[0]}, " +
                "围绕x旋转=${values[1]} " +
                "围绕y旋转=${values[2]} " )
        callback?.invoke(values)
    }
}